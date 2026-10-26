package com.luojiajun.chat.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DashScope 原生 embedding 接口（DeepSeek 没有 embedding，所以向量化只能用通义）。
 */
public class DashScopeEmbeddingModel implements EmbeddingModel {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int dimension;
    private final int batchSize;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DashScopeEmbeddingModel(String baseUrl, String apiKey, String model, int dimension, int batchSize,
                                   OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.dimension = dimension;
        this.batchSize = batchSize;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> vectors = new java.util.ArrayList<>(texts.size());
        for (int from = 0; from < texts.size(); from += batchSize) {
            List<String> batch = texts.subList(from, Math.min(texts.size(), from + batchSize));
            vectors.addAll(callOnce(batch));
        }
        return vectors;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("序列化 embedding 请求失败", e);
        }
    }

    private List<float[]> callOnce(List<String> batch) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", Map.of("texts", batch));
        body.put("parameters", Map.of("text_type", "document", "dimension", dimension));

        Request request = new Request.Builder()
                .url(baseUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(toJson(body), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String raw = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw ModelException.upstream("dashscope-embedding", response.code(), raw);
            }
            JsonNode embeddings = objectMapper.readTree(raw).path("output").path("embeddings");
            List<float[]> result = new java.util.ArrayList<>(batch.size());
            for (JsonNode item : embeddings) {
                JsonNode vector = item.path("embedding");
                float[] values = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    values[i] = (float) vector.get(i).asDouble();
                }
                result.add(values);
            }
            return result;
        } catch (IOException e) {
            throw new ModelException("EMBEDDING_IO", "向量化失败: " + e.getMessage(), true, null, e);
        } catch (ModelException e) {
            throw e;
        } catch (Exception e) {
            throw ModelException.parse("dashscope-embedding", e);
        }
    }
}
