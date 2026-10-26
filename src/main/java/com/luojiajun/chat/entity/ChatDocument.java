package com.luojiajun.chat.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatDocument {
    private Long id;
    private Long userId;
    private String filename;
    private Integer byteSize;
    private Integer charCount;
    private Integer chunkCount;
    private String status;
    private LocalDateTime createdAt;
}
