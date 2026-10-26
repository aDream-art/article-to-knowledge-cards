package com.luojiajun.chat.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatModelCallLog {
    private Long id;
    private Long userId;
    private Long sessionId;
    private Long messageId;
    private String requestId;
    private String provider;
    private String model;
    private String status;
    private Integer httpStatus;
    private Integer tokenIn;
    private Integer tokenOut;
    private Integer latencyMs;
    private Integer firstTokenMs;
    private Integer retryCount;
    private String errorCode;
    private String errorMsg;
    private LocalDateTime createdAt;
}
