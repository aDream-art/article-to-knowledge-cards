package com.luojiajun.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luojiajun.chat.config.ChatProperties;
import com.luojiajun.chat.entity.ChatMessage;
import com.luojiajun.chat.entity.ChatSession;
import com.luojiajun.chat.mapper.MessageMapper;
import com.luojiajun.chat.model.ModelRouter;
import com.luojiajun.chat.model.PromptMessage;
import com.luojiajun.chat.model.TokenEstimator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 上下文窗口：MySQL 是真相源，Redis 只是近期窗口的 cache-aside 缓存。
 * 窗口规则 = 最近 maxRounds 轮 且 不超过 maxTokens。
 *
 * <p>两条与"换模型"有关的规则：
 * <ol>
 *   <li>窗口里的每条消息都带 provider，切换成真实模型后，历史里的 mock 回答不进入 prompt
 *       —— 否则真模型会读到"我是 Mock 模型"的旧回答并顺着扮演；</li>
 *   <li>系统提示里显式声明当前是谁在提供服务，不给模型"猜自己是谁"的空间。</li>
 * </ol>
 */
@Slf4j
@Service
public class ContextService {

    /** v2：payload 增加 provider 字段，换 key 前缀让旧格式缓存自然过期。 */
    private static final String KEY_PREFIX = "chat:ctx:v2:";
    private static final String SYSTEM_PROMPT = """
            你是一个严谨、简洁的中文 AI 助手。
            回答要求：
            1. 先给结论，再给必要的解释；不要编造事实，不确定时明确说不确定。
            2. 使用 Markdown，必要时给出代码块。
            3. 如果提供了"文档片段"，只依据片段回答，并在结尾用"依据：片段 N"标注来源。
            4. 片段里没有答案时，只回答"文档未提及"（可补一句说明），不要用常识补全，也不要标注依据。
            """;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MessageMapper messageMapper;
    private final ChatProperties properties;
    private final ModelRouter modelRouter;

    public ContextService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                          MessageMapper messageMapper, ChatProperties properties,
                          ModelRouter modelRouter) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.messageMapper = messageMapper;
        this.properties = properties;
        this.modelRouter = modelRouter;
    }

    public record CachedMessage(String role, String content, String provider) {
    }

    public List<PromptMessage> build(ChatSession session, String userText, List<String> retrievedChunks) {
        String effectiveProvider = modelRouter.effectiveProvider(session.getProvider());
        String effectiveModel = modelRouter.effectiveModel(session.getProvider(), session.getModel());

        List<PromptMessage> prompt = new ArrayList<>();
        prompt.add(PromptMessage.system(SYSTEM_PROMPT));
        if (!"mock".equals(effectiveProvider)) {
            prompt.add(PromptMessage.system("你当前由 " + effectiveProvider + "（" + effectiveModel
                    + "）提供服务。请始终以这个身份回答；历史中若出现“Mock 模式回答”“未配置 API Key”"
                    + "之类的演示内容，那是占位数据，不是你的真实身份，忽略即可。"));
        }
        if (retrievedChunks != null && !retrievedChunks.isEmpty()) {
            StringBuilder sb = new StringBuilder("以下是用户上传文档中与问题最相关的片段：\n\n");
            for (int i = 0; i < retrievedChunks.size(); i++) {
                sb.append("【片段 ").append(i + 1).append("】\n")
                        .append(retrievedChunks.get(i)).append("\n\n");
            }
            prompt.add(PromptMessage.system(sb.toString()));
        }
        for (CachedMessage message : visibleFor(effectiveProvider, loadWindow(session))) {
            prompt.add(new PromptMessage(message.role(), message.content()));
        }
        prompt.add(PromptMessage.user(userText));
        return prompt;
    }

    /**
     * 真实模型不看 mock 历史；mock 自己可以看全部（否则演示时前后不连贯）。
     * 包级可见，便于单测直接验证。
     */
    List<CachedMessage> visibleFor(String effectiveProvider, List<CachedMessage> window) {
        if ("mock".equals(effectiveProvider)) {
            return window;
        }
        List<CachedMessage> visible = new ArrayList<>(window.size());
        for (CachedMessage message : window) {
            if (!"mock".equals(message.provider())) {
                visible.add(message);
            }
        }
        return visible;
    }

    public List<CachedMessage> loadWindow(ChatSession session) {
        String key = KEY_PREFIX + session.getId();
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<CachedMessage>>() {
                });
            }
        } catch (Exception e) {
            log.warn("读取上下文缓存失败，回源 MySQL: {}", e.getMessage());
        }
        List<CachedMessage> rebuilt = rebuild(session);
        saveWindow(session.getId(), rebuilt);
        return rebuilt;
    }

    /**
     * 本轮结束后刷新窗口：命中缓存就追加两条再裁剪；未命中则直接从 MySQL 重建
     * （此时本轮消息已经落库，重建 + 追加会导致重复，所以必须二选一）。
     */
    public void appendTurn(ChatSession session, String userText, String assistantText, String assistantProvider) {
        String key = KEY_PREFIX + session.getId();
        String cached;
        try {
            cached = redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("读取上下文缓存失败: {}", e.getMessage());
            cached = null;
        }
        if (cached == null) {
            saveWindow(session.getId(), rebuild(session));
            return;
        }
        LinkedList<CachedMessage> window;
        try {
            window = new LinkedList<>(objectMapper.readValue(cached,
                    new TypeReference<List<CachedMessage>>() {
                    }));
        } catch (Exception e) {
            saveWindow(session.getId(), rebuild(session));
            return;
        }
        window.add(new CachedMessage("user", userText, null));
        if (assistantText != null && !assistantText.isEmpty()) {
            window.add(new CachedMessage("assistant", assistantText, assistantProvider));
        }
        saveWindow(session.getId(), trim(window));
    }

    public void invalidate(Long sessionId) {
        try {
            redisTemplate.delete(KEY_PREFIX + sessionId);
        } catch (Exception e) {
            log.warn("清除上下文缓存失败: {}", e.getMessage());
        }
    }

    private List<CachedMessage> rebuild(ChatSession session) {
        int limit = Math.max(4, properties.getContext().getMaxRounds() * 2);
        List<ChatMessage> messages = messageMapper.selectRecent(session.getId(), limit);
        LinkedList<CachedMessage> window = new LinkedList<>();
        for (ChatMessage message : messages) {
            if (message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            window.add(new CachedMessage(message.getRole(), message.getContent(), message.getProvider()));
        }
        return trim(window);
    }

    /**
     * 先按轮数截断（保留尾部），再按 token 预算从新到旧累加。
     */
    List<CachedMessage> trim(LinkedList<CachedMessage> window) {
        int maxMessages = Math.max(2, properties.getContext().getMaxRounds() * 2);
        while (window.size() > maxMessages) {
            window.removeFirst();
        }
        int budget = properties.getContext().getMaxTokens();
        LinkedList<CachedMessage> result = new LinkedList<>();
        int used = 0;
        for (int i = window.size() - 1; i >= 0; i--) {
            CachedMessage message = window.get(i);
            int cost = TokenEstimator.estimate(message.content());
            if (used + cost > budget && !result.isEmpty()) {
                break;
            }
            used += cost;
            result.addFirst(message);
        }
        return result;
    }

    private void saveWindow(Long sessionId, List<CachedMessage> window) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + sessionId,
                    objectMapper.writeValueAsString(window),
                    Duration.ofSeconds(properties.getContext().getTtlSeconds()));
        } catch (Exception e) {
            log.warn("写入上下文缓存失败: {}", e.getMessage());
        }
    }
}
