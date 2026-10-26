package com.luojiajun.chat.entity;

import lombok.Data;

@Data
public class ChatChunk {
    private Long id;
    private Long documentId;
    private Long userId;
    private Integer seq;
    private String content;
    private byte[] embedding;
    private Integer tokenEst;
}
