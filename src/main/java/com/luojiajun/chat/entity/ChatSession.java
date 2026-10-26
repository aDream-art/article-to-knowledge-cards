package com.luojiajun.chat.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSession {
    private Long id;
    private Long userId;
    private String title;
    private String provider;
    private String model;
    private Long documentId;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 列表页展示用的最近一条消息摘要（子查询填充，非表字段） */
    private String lastMessage;
}
