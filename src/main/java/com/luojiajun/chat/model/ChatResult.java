package com.luojiajun.chat.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResult {
    private String content;
    private String provider;
    private String model;
    private Integer tokenIn;
    private Integer tokenOut;
    private Integer firstTokenMs;
    private Integer latencyMs;

    public int totalTokens() {
        return (tokenIn == null ? 0 : tokenIn) + (tokenOut == null ? 0 : tokenOut);
    }
}
