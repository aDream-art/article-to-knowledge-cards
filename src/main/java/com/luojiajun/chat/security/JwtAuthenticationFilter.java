package com.luojiajun.chat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenUtil jwtTokenUtil;
    private final TokenBlacklistService blacklistService;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, TokenBlacklistService blacklistService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.blacklistService = blacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                JwtTokenUtil.JwtClaims claims = jwtTokenUtil.parse(token);
                if (!blacklistService.isBlacklisted(claims.jti())) {
                    LoginUser loginUser = new LoginUser(claims.userId(), claims.username());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            loginUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.debug("token 解析失败: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
