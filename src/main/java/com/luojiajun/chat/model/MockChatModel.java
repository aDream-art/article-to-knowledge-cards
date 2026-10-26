package com.luojiajun.chat.model;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 无 Key 时的演示/测试替身：首响 300ms、每 50ms 吐 10 字，可按配置让第 N 次调用失败以演示重试。
 */
@Slf4j
public class MockChatModel implements ChatModel {

    private static final int FIRST_DELAY_MS = 300;
    private static final int CHUNK_DELAY_MS = 50;
    private static final int CHUNK_SIZE = 10;

    private final int failCall;
    private final AtomicInteger calls = new AtomicInteger();

    public MockChatModel(int failCall) {
        this.failCall = failCall;
    }

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public ChatResult stream(ChatRequest request, Consumer<String> onDelta) {
        long start = System.currentTimeMillis();
        int call = calls.incrementAndGet();
        if (failCall > 0 && call == failCall) {
            throw new ModelException("MOCK_FAILURE",
                    "mock provider 第 " + call + " 次调用按配置失败（用于演示重试）", true, 500, null);
        }
        sleep(FIRST_DELAY_MS);
        String reply = render(request);
        int firstTokenMs = (int) (System.currentTimeMillis() - start);
        StringBuilder sent = new StringBuilder();
        for (int i = 0; i < reply.length(); i += CHUNK_SIZE) {
            String chunk = reply.substring(i, Math.min(reply.length(), i + CHUNK_SIZE));
            sent.append(chunk);
            onDelta.accept(chunk);
            sleep(CHUNK_DELAY_MS);
        }
        return ChatResult.builder()
                .content(sent.toString())
                .provider("mock")
                .model("mock-model")
                .tokenIn(TokenEstimator.estimate(lastUserContent(request)))
                .tokenOut(TokenEstimator.estimate(sent.toString()))
                .firstTokenMs(firstTokenMs)
                .latencyMs((int) (System.currentTimeMillis() - start))
                .build();
    }

    @Override
    public ChatResult complete(ChatRequest request) {
        long start = System.currentTimeMillis();
        return ChatResult.builder()
                .content(render(request))
                .provider("mock")
                .model("mock-model")
                .tokenIn(TokenEstimator.estimate(lastUserContent(request)))
                .tokenOut(120)
                .firstTokenMs(null)
                .latencyMs((int) (System.currentTimeMillis() - start))
                .build();
    }

    private String render(ChatRequest request) {
        String question = lastUserContent(request);
        String shorter = question.length() > 60 ? question.substring(0, 60) + "…" : question;
        return """
                **Mock 模式回答**（未配置真实 API Key）

                你问的是：`%s`

                这是一个本地生成的演示回答，用来验证整条链路（会话隔离 → SSE 流式 → 落库 → 收藏）是否通。
                配置 `DEEPSEEK_API_KEY` 或 `DASHSCOPE_API_KEY` 后，同一个入口会切到真实模型，代码路径完全一致。

                ```java
                ChatModel model = modelRouter.resolve("deepseek");
                model.stream(request, delta -> sseEmitter.send(delta));
                ```

                —— 以上内容不消耗任何 token。
                """.formatted(shorter);
    }

    private String lastUserContent(ChatRequest request) {
        List<PromptMessage> messages = request.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).role())) {
                return messages.get(i).content();
            }
        }
        return "";
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModelException("INTERRUPTED", "生成被中断", false, null, e);
        }
    }
}
