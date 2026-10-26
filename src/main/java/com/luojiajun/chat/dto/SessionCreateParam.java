package com.luojiajun.chat.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SessionCreateParam {

    @Size(max = 100, message = "标题最多 100 个字符")
    private String title;

    @Size(max = 20)
    private String provider;

    @Size(max = 50)
    private String model;

    private Long documentId;
}
