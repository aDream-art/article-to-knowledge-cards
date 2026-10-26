package com.luojiajun.chat.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luojiajun.chat.config.ChatProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

/**
 * embedding 只能走通义；没有 Key 时退化为确定性 mock 向量，保证上传与召回链路可跑通。
 */
@Slf4j
public class EmbeddingRouter {

    private final EmbeddingModel model;
    private final boolean mock;

    public EmbeddingRouter(ChatProperties properties, OkHttpClient httpClient, ObjectMapper objectMapper) {
        ChatProperties.Embedding embedding = properties.getEmbedding();
        boolean hasKey = embedding.getApiKey() != null && !embedding.getApiKey().isBlank();
        if (hasKey) {
            this.model = new DashScopeEmbeddingModel(embedding.getBaseUrl(), embedding.getApiKey(),
                    embedding.getModel(), embedding.getDimension(), embedding.getBatchSize(),
                    httpClient, objectMapper);
            this.mock = false;
        } else {
            log.warn("未配置 DASHSCOPE_API_KEY，向量化退化为 mock 向量（语义检索不可用，仅链路可用）");
            this.model = new MockEmbeddingModel();
            this.mock = true;
        }
    }

    public EmbeddingModel model() {
        return model;
    }

    public boolean isMock() {
        return mock;
    }
}
