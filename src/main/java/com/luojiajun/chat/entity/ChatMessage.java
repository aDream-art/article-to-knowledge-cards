package com.luojiajun.chat.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private Long id;
    private Long sessionId;
    private Long userId;
    private String role;
    private String content;
    private String provider;
    private String model;
    private Integer tokenIn;
    private Integer tokenOut;
    private String status;
    private Integer latencyMs;
    private LocalDateTime createdAt;
}
