package com.luojiajun.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "chat")
public class ChatProperties {

    private Jwt jwt = new Jwt();
    private Map<String, Provider> providers = new LinkedHashMap<>();
    private Embedding embedding = new Embedding();
    private Rag rag = new Rag();
    private Context context = new Context();
    private Limit limit = new Limit();
    private Upload upload = new Upload();
    private Mock mock = new Mock();

    @Data
    public static class Jwt {
        private String secret;
        private long expireSeconds = 604800L;
    }

    @Data
    public static class Provider {
        private String baseUrl;
        private String apiKey;
        private String defaultModel;
    }

    @Data
    public static class Embedding {
        private String baseUrl;
        private String apiKey;
        private String model = "text-embedding-v3";
        private int dimension = 1024;
        private int batchSize = 10;
    }

    @Data
    public static class Rag {
        private int chunkSize = 400;
        private int overlap = 80;
        private int topK = 5;
        private double threshold = 0.3;
    }

    @Data
    public static class Context {
        private int maxRounds = 10;
        private int maxTokens = 3000;
        private long ttlSeconds = 7200;
    }

    @Data
    public static class Limit {
        private int perMinute = 20;
        private int dailyTokens = 30000;
        private int maxConcurrency = 20;
    }

    @Data
    public static class Upload {
        private long maxBytes = 1048576L;
        private int maxDocsPerUser = 10;
    }

    @Data
    public static class Mock {
        private boolean enabled = true;
        /** 让第 N 次调用失败，用于演示重试；0 表示不注入失败 */
        private int failCall = 0;
    }
}
