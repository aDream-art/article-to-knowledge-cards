package com.luojiajun.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagCreateParam {

    @NotBlank(message = "标签名不能为空")
    @Size(max = 30, message = "标签最多 30 个字符")
    private String name;
}
