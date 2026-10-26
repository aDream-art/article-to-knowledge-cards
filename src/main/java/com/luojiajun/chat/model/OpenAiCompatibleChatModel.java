package com.luojiajun.chat.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OpenAI 兼容协议的客户端。DeepSeek 与通义（DashScope compatible-mode）共用这一份实现，
 * 差异只有 baseUrl / key / model —— 这就是"统一调用层"的具体含义。
 */
public class OpenAiCompatibleChatModel implements ChatModel {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String provider;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleChatModel(String provider, String baseUrl, String apiKey,
                                     String defaultModel, OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.provider = provider;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String provider() {
        return provider;
    }

    @Override
    public ChatResult stream(ChatRequest request, Consumer<String> onDelta) {
        long start = System.currentTimeMillis();
        Integer firstTokenMs = null;
        StringBuilder content = new StringBuilder();
        Integer tokenIn = null;
        Integer tokenOut = null;

        Map<String, Object> body = baseBody(request);
        body.put("stream", true);
        body.put("stream_options", Map.of("include_usage", true));

        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(toJson(body), JSON))
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw ModelException.upstream(provider, response.code(), bodyString(response));
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw ModelException.parse(provider, null);
            }
            BufferedSource source = responseBody.source();
            String line;
            while ((line = source.readUtf8Line()) != null) {
                if (line.isEmpty() || line.startsWith(":")) {
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                JsonNode node = objectMapper.readTree(data);
                JsonNode usage = node.path("usage");
                if (!usage.isMissingNode() && !usage.isNull()) {
                    tokenIn = usage.path("prompt_tokens").asInt(tokenIn == null ? 0 : tokenIn);
                    tokenOut = usage.path("completion_tokens").asInt(tokenOut == null ? 0 : tokenOut);
                }
                JsonNode choices = node.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    JsonNode delta = choices.get(0).path("delta");
                    JsonNode textNode = delta.get("content");
                    if (textNode != null && !textNode.isNull()) {
                        String text = textNode.asText();
                        if (!text.isEmpty()) {
                            if (firstTokenMs == null) {
                                firstTokenMs = (int) (System.currentTimeMillis() - start);
                            }
                            content.append(text);
                            onDelta.accept(text);
                        }
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            throw ModelException.timeout(provider);
        } catch (IOException e) {
            throw new ModelException("IO_ERROR", "调用 " + provider + " 失败: " + e.getMessage(),
                    true, null, e);
        } catch (ModelException e) {
            throw e;
        } catch (RuntimeException e) {
            // 业务侧的中断信号（客户端断开）原样上抛，不要包成模型解析错误
            throw e;
        } catch (Exception e) {
            throw ModelException.parse(provider, e);
        }

        String text = content.toString();
        return ChatResult.builder()
                .content(text)
                .provider(provider)
                .model(modelOf(request))
                .tokenIn(tokenIn != null ? tokenIn : TokenEstimator.estimate(joinMessages(request.getMessages())))
                .tokenOut(tokenOut != null ? tokenOut : TokenEstimator.estimate(text))
                .firstTokenMs(firstTokenMs)
                .latencyMs((int) (System.currentTimeMillis() - start))
                .build();
    }

    @Override
    public ChatResult complete(ChatRequest request) {
        long start = System.currentTimeMillis();
        Map<String, Object> body = baseBody(request);
        body.put("stream", false);

        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(toJson(body), JSON))
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw ModelException.upstream(provider, response.code(), bodyString(response));
            }
            String text = bodyString(response);
            JsonNode root = objectMapper.readTree(text);
            JsonNode choices = root.path("choices");
            String content = choices.isEmpty() ? "" : choices.get(0).path("message").path("content").asText();
            JsonNode usage = root.path("usage");
            return ChatResult.builder()
                    .content(content)
                    .provider(provider)
                    .model(modelOf(request))
                    .tokenIn(usage.path("prompt_tokens").asInt(TokenEstimator.estimate(joinMessages(request.getMessages()))))
                    .tokenOut(usage.path("completion_tokens").asInt(TokenEstimator.estimate(content)))
                    .firstTokenMs(null)
                    .latencyMs((int) (System.currentTimeMillis() - start))
                    .build();
        } catch (SocketTimeoutException e) {
            throw ModelException.timeout(provider);
        } catch (IOException e) {
            throw new ModelException("IO_ERROR", "调用 " + provider + " 失败: " + e.getMessage(),
                    true, null, e);
        } catch (ModelException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw ModelException.parse(provider, e);
        }
    }

    private Map<String, Object> baseBody(ChatRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        for (PromptMessage message : request.getMessages()) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("role", message.role());
            m.put("content", message.content());
            messages.add(m);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelOf(request));
        body.put("messages", messages);
        if (request.getTemperature() > 0) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() > 0) {
            body.put("max_tokens", request.getMaxTokens());
        }
        return body;
    }

    private String modelOf(ChatRequest request) {
        return request.getModel() == null || request.getModel().isBlank() ? defaultModel : request.getModel();
    }

    private String joinMessages(List<PromptMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (PromptMessage m : messages) {
            sb.append(m.content()).append('\n');
        }
        return sb.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化请求体失败", e);
        }
    }

    private String bodyString(Response response) {
        try {
            ResponseBody body = response.body();
            return body == null ? "" : body.string();
        } catch (IOException e) {
            return "";
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
