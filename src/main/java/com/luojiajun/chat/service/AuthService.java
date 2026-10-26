package com.luojiajun.chat.service;

import com.luojiajun.chat.common.ApiException;
import com.luojiajun.chat.common.Asserts;
import com.luojiajun.chat.common.ResultCode;
import com.luojiajun.chat.dto.LoginParam;
import com.luojiajun.chat.dto.LoginVO;
import com.luojiajun.chat.dto.RegisterParam;
import com.luojiajun.chat.entity.User;
import com.luojiajun.chat.mapper.UserMapper;
import com.luojiajun.chat.security.JwtTokenUtil;
import com.luojiajun.chat.security.TokenBlacklistService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final TokenBlacklistService blacklistService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                       JwtTokenUtil jwtTokenUtil, TokenBlacklistService blacklistService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.blacklistService = blacklistService;
    }

    @Transactional
    public LoginVO register(RegisterParam param) {
        if (userMapper.selectByUsername(param.getUsername()) != null) {
            throw new ApiException(ResultCode.VALIDATE_FAILED, "用户名已存在");
        }
        User user = new User();
        user.setUsername(param.getUsername());
        user.setPasswordHash(passwordEncoder.encode(param.getPassword()));
        user.setNickname(param.getNickname() == null || param.getNickname().isBlank()
                ? param.getUsername() : param.getNickname());
        user.setStatus(1);
        userMapper.insert(user);
        return toLoginVO(user);
    }

    public LoginVO login(LoginParam param) {
        User user = userMapper.selectByUsername(param.getUsername());
        if (user == null || !passwordEncoder.matches(param.getPassword(), user.getPasswordHash())) {
            throw new ApiException(ResultCode.VALIDATE_FAILED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new ApiException(ResultCode.FORBIDDEN, "账号已停用");
        }
        return toLoginVO(user);
    }

    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }
        JwtTokenUtil.JwtClaims claims = jwtTokenUtil.parse(authorizationHeader.substring(7).trim());
        long remaining = claims.exp() - java.time.Instant.now().getEpochSecond();
        blacklistService.blacklist(claims.jti(), remaining);
    }

    public User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        Asserts.notNull(user, "用户不存在");
        return user;
    }

    private LoginVO toLoginVO(User user) {
        String token = jwtTokenUtil.generate(user.getId(), user.getUsername());
        return new LoginVO(token, user.getId(), user.getUsername(), user.getNickname());
    }
}
