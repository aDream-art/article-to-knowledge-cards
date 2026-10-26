package com.luojiajun.chat.model;

import com.luojiajun.chat.config.ChatProperties;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 这个测试守的是"前端看到的 provider 必须是实际生效的那家"：
 * 没有 Key 时必须如实报 mock，不能拿用户选的 deepseek 糊弄。
 */
class ModelRouterTest {

    private ChatProperties properties(String deepseekKey, String qwenKey) {
        ChatProperties properties = new ChatProperties();
        properties.getMock().setEnabled(true);
        Map<String, ChatProperties.Provider> providers = new LinkedHashMap<>();

        ChatProperties.Provider deepseek = new ChatProperties.Provider();
        deepseek.setBaseUrl("https://api.deepseek.com");
        deepseek.setApiKey(deepseekKey);
        deepseek.setDefaultModel("deepseek-chat");
        providers.put("deepseek", deepseek);

        ChatProperties.Provider qwen = new ChatProperties.Provider();
        qwen.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        qwen.setApiKey(qwenKey);
        qwen.setDefaultModel("qwen-plus");
        providers.put("qwen", qwen);

        properties.setProviders(providers);
        return properties;
    }

    private ModelRouter router(ChatProperties properties, ChatModel mock) {
        return new ModelRouter(Map.of("deepseek", mock, "qwen", mock), mock, properties);
    }

    @Test
    void noKeysMeansMockIsActive() {
        ModelRouter router = router(properties("", ""), new MockChatModel(0));
        assertThat(router.mockActive()).isTrue();
        assertThat(router.effectiveProvider("deepseek")).isEqualTo("mock");
        assertThat(router.effectiveModel("deepseek", "deepseek-chat")).isEqualTo("mock-model");
        assertThat(router.fallbackProvider("deepseek")).isNull();
    }

    @Test
    void withKeyTheRealProviderIsReported() {
        ModelRouter router = router(properties("sk-test", ""), new MockChatModel(0));
        assertThat(router.mockActive()).isFalse();
        assertThat(router.effectiveProvider("deepseek")).isEqualTo("deepseek");
        assertThat(router.effectiveModel("deepseek", "deepseek-chat")).isEqualTo("deepseek-chat");
        assertThat(router.effectiveModel("deepseek", null)).isEqualTo("deepseek-chat");
    }

    @Test
    void fallbackPointsToTheOtherConfiguredProvider() {
        ModelRouter router = router(properties("sk-test", "sk-qwen"), new MockChatModel(0));
        assertThat(router.fallbackProvider("deepseek")).isEqualTo("qwen");
        assertThat(router.fallbackProvider("qwen")).isEqualTo("deepseek");
    }

    @Test
    void fallbackIsNullWhenOnlyOneProviderHasKey() {
        ModelRouter router = router(properties("sk-test", ""), new MockChatModel(0));
        assertThat(router.fallbackProvider("deepseek")).isNull();
    }
}
