package com.luojiajun.chat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luojiajun.chat.common.ApiException;
import com.luojiajun.chat.config.ChatProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenUtilTest {

    private JwtTokenUtil util(long expireSeconds) {
        ChatProperties properties = new ChatProperties();
        properties.getJwt().setSecret("unit-test-secret-32-bytes-long-key");
        properties.getJwt().setExpireSeconds(expireSeconds);
        return new JwtTokenUtil(properties, new ObjectMapper());
    }

    @Test
    void roundTrip() {
        JwtTokenUtil util = util(3600);
        String token = util.generate(42L, "alice");
        JwtTokenUtil.JwtClaims claims = util.parse(token);
        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.username()).isEqualTo("alice");
        assertThat(claims.jti()).isNotBlank();
    }

    @Test
    void tamperedSignatureIsRejected() {
        JwtTokenUtil util = util(3600);
        String token = util.generate(1L, "bob");
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');
        assertThatThrownBy(() -> util.parse(tampered)).isInstanceOf(ApiException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtTokenUtil util = util(-10);
        String token = util.generate(1L, "carol");
        assertThatThrownBy(() -> util.parse(token)).isInstanceOf(ApiException.class);
    }

    @Test
    void garbageIsRejected() {
        assertThatThrownBy(() -> util(3600).parse("not-a-token")).isInstanceOf(ApiException.class);
    }
}
