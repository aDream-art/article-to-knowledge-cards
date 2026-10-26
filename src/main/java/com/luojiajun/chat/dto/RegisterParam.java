package com.luojiajun.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterParam {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,30}$", message = "用户名只能是 3~30 位字母、数字或下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在 6~64 位之间")
    private String password;

    @Size(max = 30, message = "昵称最多 30 个字符")
    private String nickname;
}
