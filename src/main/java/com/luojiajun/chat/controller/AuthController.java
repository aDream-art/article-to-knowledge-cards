package com.luojiajun.chat.controller;

import com.luojiajun.chat.common.CommonResult;
import com.luojiajun.chat.dto.LoginParam;
import com.luojiajun.chat.dto.LoginVO;
import com.luojiajun.chat.dto.RegisterParam;
import com.luojiajun.chat.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证", description = "注册 / 登录 / 登出")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "注册（成功后直接返回 token）")
    public CommonResult<LoginVO> register(@Valid @RequestBody RegisterParam param) {
        return CommonResult.success(authService.register(param));
    }

    @PostMapping("/login")
    @Operation(summary = "登录")
    public CommonResult<LoginVO> login(@Valid @RequestBody LoginParam param) {
        return CommonResult.success(authService.login(param));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出（把 jti 写进 Redis 黑名单）")
    public CommonResult<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return CommonResult.success(null);
    }
}
