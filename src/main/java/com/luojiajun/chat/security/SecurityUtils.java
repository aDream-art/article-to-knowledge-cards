package com.luojiajun.chat.security;

import com.luojiajun.chat.common.ApiException;
import com.luojiajun.chat.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new ApiException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long currentUserId() {
        return current().getUserId();
    }
}
