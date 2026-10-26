package com.luojiajun.chat.model;

import com.luojiajun.chat.config.ChatProperties;

import java.util.Map;

/**
 * 选谁：会话里记的 provider → 有 Key 用它，没 Key 落 mock；首字节前允许跨 provider 降级。
 */
public class ModelRouter {

    public static final String DEEPSEEK = "deepseek";
    public static final String QWEN = "qwen";

    private final Map<String, ChatModel> models;
    private final ChatModel mock;
    private final ChatProperties properties;

    public ModelRouter(Map<String, ChatModel> models, ChatModel mock, ChatProperties properties) {
        this.models = models;
        this.mock = mock;
        this.properties = properties;
    }

    public ChatModel resolve(String provider) {
        String normalized = normalize(provider);
        if (mockActive()) {
            return mock;
        }
        if (!hasKey(normalized)) {
            if (properties.getMock().isEnabled()) {
                return mock;
            }
            throw ModelException.notConfigured(normalized);
        }
        ChatModel model = models.get(normalized);
        if (model == null) {
            throw ModelException.notConfigured(normalized);
        }
        return model;
    }

    /**
     * 只在"还没吐字"时使用：DeepSeek 挂了切通义，反之亦然。
     */
    public String fallbackProvider(String provider) {
        if (mockActive()) {
            return null;
        }
        String other = DEEPSEEK.equals(normalize(provider)) ? QWEN : DEEPSEEK;
        return hasKey(other) ? other : null;
    }

    public boolean mockActive() {
        return properties.getMock().isEnabled() && !hasKey(DEEPSEEK) && !hasKey(QWEN);
    }

    public boolean hasKey(String provider) {
        ChatProperties.Provider config = properties.getProviders().get(provider);
        return config != null && config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    public String defaultProvider() {
        return DEEPSEEK;
    }

    public String defaultModel(String provider) {
        ChatProperties.Provider config = properties.getProviders().get(normalize(provider));
        return config == null || config.getDefaultModel() == null ? "unknown" : config.getDefaultModel();
    }

    /**
     * 实际生效的 provider：没有 Key 时落到 mock，就要如实说 mock，
     * 否则前端会以为在跑真实模型（调用日志里也会对不上）。
     */
    public String effectiveProvider(String requested) {
        return mockActive() ? "mock" : normalize(requested);
    }

    public String effectiveModel(String requested, String model) {
        if (mockActive()) {
            return "mock-model";
        }
        return model == null || model.isBlank() ? defaultModel(requested) : model;
    }

    public String normalize(String provider) {
        return provider == null || provider.isBlank() ? DEEPSEEK : provider.trim().toLowerCase();
    }
}
