package com.luojiajun.chat.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ChatFavorite {
    private Long id;
    private Long userId;
    private Long sessionId;
    private String question;
    private String answer;
    private String provider;
    private String model;
    private LocalDateTime createdAt;

    private List<ChatTag> tags = new ArrayList<>();
}
