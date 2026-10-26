package com.luojiajun.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatStreamParam {

    @NotNull(message = "sessionId 不能为空")
    private Long sessionId;

    @NotBlank(message = "提问内容不能为空")
    @Size(max = 4000, message = "单次提问最多 4000 个字符")
    private String content;
}
