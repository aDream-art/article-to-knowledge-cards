package com.luojiajun.chat.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
