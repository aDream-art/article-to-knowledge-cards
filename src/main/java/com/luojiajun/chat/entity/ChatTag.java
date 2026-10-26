package com.luojiajun.chat.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatTag {
    private Long id;
    private Long userId;
    private String name;
    private LocalDateTime createdAt;

    private Integer count;

    /** 关联查询时带出所属收藏，非表字段 */
    private Long favoriteId;
}
