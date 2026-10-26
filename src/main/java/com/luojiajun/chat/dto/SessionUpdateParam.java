package com.luojiajun.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SessionUpdateParam {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最多 100 个字符")
    private String title;
}
