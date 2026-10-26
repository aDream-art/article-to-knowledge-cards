package com.luojiajun.chat.service;

import com.luojiajun.chat.common.ApiException;
import com.luojiajun.chat.common.ResultCode;
import com.luojiajun.chat.config.ChatProperties;
import com.luojiajun.chat.dto.ChatStreamParam;
import com.luojiajun.chat.entity.ChatMessage;
import com.luojiajun.chat.entity.ChatSession;
import com.luojiajun.chat.model.ChatRequest;
import com.luojiajun.chat.model.ChatResult;
import com.luojiajun.chat.model.ModelException;
import com.luojiajun.chat.model.ModelRouter;
import com.luojiajun.chat.model.PromptMessage;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * SSE 对话链路：同步做鉴权/限流校验，异步跑生成，线程内维护"已发内容 + 是否已落库"。
 * 语义见 docs/adr/0002-sse-no-resume.md：首字节前可重试/降级，之后只落 ABORTED 半截回答。
 */
@Slf4j
@Service
public class ChatService {

    private static final int MAX_RETRY = 2;
    private static final long HEARTBEAT_SECONDS = 15;

    private final SessionService sessionService;
    private final MessageService messageService;
    private final ContextService contextService;
    private final RateLimitService rateLimitService;
    private final ModelRouter modelRouter;
    private final RagService ragService;
    private final ChatProperties properties;
    private final ModelCallLogService callLogService;
    private final Executor chatExecutor;
    private final ScheduledExecutorService heartbeatExecutor;

    public ChatService(SessionService sessionService, MessageService messageService,
                       ContextService contextService, RateLimitService rateLimitService,
                       ModelRouter modelRouter, RagService ragService, ChatProperties properties,
                       ModelCallLogService callLogService,
                       @Qualifier("chatExecutor") Executor chatExecutor,
                       @Qualifier("heartbeatExecutor") ScheduledExecutorService heartbeatExecutor) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.contextService = contextService;
        this.rateLimitService = rateLimitService;
        this.modelRouter = modelRouter;
        this.ragService = ragService;
        this.properties = properties;
        this.callLogService = callLogService;
        this.chatExecutor = chatExecutor;
        this.heartbeatExecutor = heartbeatExecutor;
    }

    public SseEmitter stream(Long userId, ChatStreamParam param) {
        ChatSession session = sessionService.require(userId, param.getSessionId());
        rateLimitService.checkRequest(userId);
        rateLimitService.checkQuota(userId);

        SseEmitter emitter = new SseEmitter(0L);
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        AtomicBoolean aborted = new AtomicBoolean(false);
        emitter.onCompletion(() -> aborted.set(true));
        emitter.onTimeout(() -> {
            aborted.set(true);
            tryComplete(emitter);
        });
        emitter.onError(e -> aborted.set(true));

        String question = param.getContent().trim();
        chatExecutor.execute(() -> doStream(userId, session, question, emitter, requestId, aborted));
        return emitter;
    }

    private void doStream(Long userId, ChatSession session, String question, SseEmitter emitter,
                          String requestId, AtomicBoolean aborted) {
        long start = System.currentTimeMillis();
        StringBuilder answer = new StringBuilder();
        StreamState state = new StreamState(session.getProvider(), session.getModel());
        ScheduledFuture<?> heartbeat = null;
        boolean acquired = false;
        try {
            MDC.put("traceId", requestId);
            MDC.put("userId", String.valueOf(userId));
            MDC.put("sessionId", String.valueOf(session.getId()));

            acquired = rateLimitService.tryAcquireConcurrency(3);
            if (!acquired) {
                throw new ApiException(ResultCode.TOO_MANY_REQUESTS, "当前并发已满，请稍后重试");
            }

            messageService.save(userId, session.getId(), "user", question,
                    null, null, null, null, "COMPLETED", null);
            sessionService.autoTitleIfDefault(userId, session, question);
            ChatMessage assistant = messageService.save(userId, session.getId(), "assistant", "",
                    state.provider, state.model, null, null, "STREAMING", null);
            state.messageId = assistant.getId();

            heartbeat = heartbeatExecutor.scheduleAtFixedRate(() -> comment(emitter),
                    HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);

            List<String> chunks = session.getDocumentId() == null
                    ? List.of() : ragService.retrieve(session.getDocumentId(), question);
            List<PromptMessage> prompt = contextService.build(session, question, chunks);
            sendMeta(emitter, state, false);

            ChatRequest request = ChatRequest.builder()
                    .provider(state.provider)
                    .model(state.model)
                    .messages(prompt)
                    .temperature(0.7)
                    .maxTokens(2048)
                    .build();

            Consumer<String> onDelta = text -> {
                answer.append(text);
                state.firstByte = true;
                if (!send(emitter, "delta", Map.of("text", text))) {
                    throw new ClientDisconnectedException();
                }
            };

            ChatResult result = invoke(request, state, onDelta, emitter, aborted);

            int latency = (int) (System.currentTimeMillis() - start);
            messageService.updateResult(state.messageId, answer.toString(),
                    result.getTokenIn(), result.getTokenOut(), "COMPLETED", latency,
                    result.getProvider(), result.getModel());
            sessionService.touch(userId, session.getId());
            contextService.appendTurn(session, question, answer.toString(), result.getProvider());
            rateLimitService.addTokens(userId, result.totalTokens());
            callLogService.logSuccess(userId, session.getId(), state.messageId, requestId,
                    result.getProvider(), result.getModel(), result, state.retries);

            Map<String, Object> usage = new LinkedHashMap<>();
            usage.put("tokenIn", result.getTokenIn());
            usage.put("tokenOut", result.getTokenOut());
            usage.put("firstTokenMs", result.getFirstTokenMs() == null ? 0 : result.getFirstTokenMs());
            send(emitter, "usage", usage);
            send(emitter, "done", Map.of());
            tryComplete(emitter);
            log.info("会话 {} 一轮对话完成，provider={}，{}ms，{} 字",
                    session.getId(), state.provider, latency, answer.length());
        } catch (ClientDisconnectedException e) {
            aborted.set(true);
            persistAborted(userId, session, state, answer.toString(), start, requestId);
            tryComplete(emitter);
        } catch (ModelException e) {
            handleFailure(userId, session, state, answer.toString(), start, requestId, e, aborted, emitter);
        } catch (ApiException e) {
            sendError(emitter, e.getErrorCode().getCode(), e.getMessage());
            tryComplete(emitter);
        } catch (Exception e) {
            log.error("对话链路异常 sessionId={}", session.getId(), e);
            sendError(emitter, ResultCode.FAILED.getCode(), "服务内部错误");
            tryComplete(emitter);
        } finally {
            if (heartbeat != null) {
                heartbeat.cancel(false);
            }
            if (acquired) {
                rateLimitService.releaseConcurrency();
            }
            MDC.clear();
        }
    }

    private ChatResult invoke(ChatRequest request, StreamState state, Consumer<String> onDelta,
                              SseEmitter emitter, AtomicBoolean aborted) {
        while (true) {
            try {
                ChatRequest attempt = ChatRequest.builder()
                        .provider(state.provider)
                        .model(state.model)
                        .messages(request.getMessages())
                        .temperature(request.getTemperature())
                        .maxTokens(request.getMaxTokens())
                        .build();
                return modelRouter.resolve(state.provider).stream(attempt, onDelta);
            } catch (ModelException e) {
                if (aborted.get() || state.firstByte) {
                    throw e;
                }
                if (e.isRetryable() && state.retries < MAX_RETRY) {
                    state.retries++;
                    log.warn("模型调用失败，第 {} 次重试: {}", state.retries, e.getMessage());
                    sleep(500L * state.retries);
                    continue;
                }
                String fallback = state.fallback ? null : modelRouter.fallbackProvider(state.provider);
                if (fallback != null) {
                    log.warn("模型 {} 不可用（{}），首字节前降级到 {}", state.provider, e.getCode(), fallback);
                    state.provider = fallback;
                    state.model = modelRouter.defaultModel(fallback);
                    state.fallback = true;
                    state.retries = 0;
                    sendMeta(emitter, state, true);
                    continue;
                }
                throw e;
            }
        }
    }

    private void handleFailure(Long userId, ChatSession session, StreamState state, String partial,
                               long start, String requestId, ModelException e, AtomicBoolean aborted,
                               SseEmitter emitter) {
        int latency = (int) (System.currentTimeMillis() - start);
        String status = aborted.get() ? "ABORTED" : "FAILED";
        if (state.messageId != null) {
            try {
                messageService.updateResult(state.messageId, partial, null, null, status, latency,
                        modelRouter.effectiveProvider(state.provider),
                        modelRouter.effectiveModel(state.provider, state.model));
            } catch (Exception ex) {
                log.warn("落库失败回答失败: {}", ex.getMessage());
            }
        }
        callLogService.logFailure(userId, session.getId(), state.messageId, requestId,
                modelRouter.effectiveProvider(state.provider),
                modelRouter.effectiveModel(state.provider, state.model), e, state.retries, latency);
        if (!aborted.get()) {
            sendError(emitter, ResultCode.MODEL_ERROR.getCode(), e.getMessage());
        }
        contextService.invalidate(session.getId());
        tryComplete(emitter);
    }

    private void persistAborted(Long userId, ChatSession session, StreamState state,
                                String partial, long start, String requestId) {
        int latency = (int) (System.currentTimeMillis() - start);
        if (state.messageId != null) {
            try {
                messageService.updateResult(state.messageId, partial, null, null, "ABORTED", latency,
                        modelRouter.effectiveProvider(state.provider),
                        modelRouter.effectiveModel(state.provider, state.model));
            } catch (Exception e) {
                log.warn("落库已中断回答失败: {}", e.getMessage());
            }
        }
        callLogService.logFailure(userId, session.getId(), state.messageId, requestId,
                modelRouter.effectiveProvider(state.provider),
                modelRouter.effectiveModel(state.provider, state.model),
                new ModelException("CLIENT_ABORTED", "客户端中断", false, null, null),
                state.retries, latency);
        contextService.invalidate(session.getId());
    }

    private void sendMeta(SseEmitter emitter, StreamState state, boolean fallback) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("messageId", state.messageId);
        // 报"实际生效"的 provider/model：没有 Key 时就是 mock，不能拿会话里选的 deepseek 糊弄前端
        data.put("provider", modelRouter.effectiveProvider(state.provider));
        data.put("model", modelRouter.effectiveModel(state.provider, state.model));
        data.put("fallback", fallback);
        send(emitter, "meta", data);
    }

    private boolean send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
            return true;
        } catch (Exception e) {
            log.debug("SSE 发送失败（客户端可能已断开）: {}", e.getMessage());
            return false;
        }
    }

    private void sendError(SseEmitter emitter, long code, String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("message", message);
        send(emitter, "error", data);
    }

    private void comment(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("keep-alive"));
        } catch (Exception ignored) {
            // 客户端已断开，下一次 delta 写入会感知并走中断分支
        }
    }

    private void tryComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // 已经结束
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class StreamState {
        private String provider;
        private String model;
        private int retries;
        private boolean firstByte;
        private boolean fallback;
        private Long messageId;

        private StreamState(String provider, String model) {
            this.provider = provider;
            this.model = model;
        }
    }
}
