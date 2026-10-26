package com.luojiajun.chat.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatRequest {
    private String provider;
    private String model;
    private List<PromptMessage> messages;
    private double temperature;
    private int maxTokens;
}
