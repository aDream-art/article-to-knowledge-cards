package com.luojiajun.chat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luojiajun.chat.model.ChatModel;
import com.luojiajun.chat.model.EmbeddingRouter;
import com.luojiajun.chat.model.MockChatModel;
import com.luojiajun.chat.model.ModelRouter;
import com.luojiajun.chat.model.OpenAiCompatibleChatModel;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ModelConfig {

    @Bean
    public OkHttpClient modelHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(20))
                .retryOnConnectionFailure(false)
                .build();
    }

    @Bean
    public MockChatModel mockChatModel(ChatProperties properties) {
        return new MockChatModel(properties.getMock().getFailCall());
    }

    @Bean("deepseekChatModel")
    public ChatModel deepseekChatModel(ChatProperties properties, OkHttpClient modelHttpClient,
                                       ObjectMapper objectMapper) {
        ChatProperties.Provider provider = properties.getProviders().get(ModelRouter.DEEPSEEK);
        return new OpenAiCompatibleChatModel(ModelRouter.DEEPSEEK, provider.getBaseUrl(),
                provider.getApiKey(), provider.getDefaultModel(), modelHttpClient, objectMapper);
    }

    @Bean("qwenChatModel")
    public ChatModel qwenChatModel(ChatProperties properties, OkHttpClient modelHttpClient,
                                   ObjectMapper objectMapper) {
        ChatProperties.Provider provider = properties.getProviders().get(ModelRouter.QWEN);
        return new OpenAiCompatibleChatModel(ModelRouter.QWEN, provider.getBaseUrl(),
                provider.getApiKey(), provider.getDefaultModel(), modelHttpClient, objectMapper);
    }

    @Bean
    public ModelRouter modelRouter(@Qualifier("deepseekChatModel") ChatModel deepseek,
                                   @Qualifier("qwenChatModel") ChatModel qwen,
                                   MockChatModel mockChatModel,
                                   ChatProperties properties) {
        Map<String, ChatModel> models = new LinkedHashMap<>();
        models.put(ModelRouter.DEEPSEEK, deepseek);
        models.put(ModelRouter.QWEN, qwen);
        return new ModelRouter(models, mockChatModel, properties);
    }

    @Bean
    public EmbeddingRouter embeddingRouter(ChatProperties properties, OkHttpClient modelHttpClient,
                                           ObjectMapper objectMapper) {
        return new EmbeddingRouter(properties, modelHttpClient, objectMapper);
    }
}
