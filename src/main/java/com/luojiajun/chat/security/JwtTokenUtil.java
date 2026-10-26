package com.luojiajun.chat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luojiajun.chat.common.ApiException;
import com.luojiajun.chat.common.ResultCode;
import com.luojiajun.chat.config.ChatProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 手写 HS256 JWT：不引第三方 JWT 库，header.payload.signature 三段，签名用 constant-time 比较。
 */
@Component
public class JwtTokenUtil {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final ChatProperties properties;
    private final ObjectMapper objectMapper;
    private final byte[] key;

    public JwtTokenUtil(ChatProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.key = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
    }

    public String generate(Long userId, String username) {
        long now = Instant.now().getEpochSecond();
        long exp = now + properties.getJwt().getExpireSeconds();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("uid", userId);
        claims.put("name", username);
        claims.put("jti", UUID.randomUUID().toString().replace("-", ""));
        claims.put("iat", now);
        claims.put("exp", exp);
        try {
            String header = ENCODER.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
            String payload = ENCODER.encodeToString(objectMapper.writeValueAsBytes(claims));
            String signature = sign(header + "." + payload);
            return header + "." + payload + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("生成 token 失败", e);
        }
    }

    public JwtClaims parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw unauthorized();
            }
            String expected = sign(parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw unauthorized();
            }
            Map<String, Object> claims = objectMapper.readValue(DECODER.decode(parts[1]), Map.class);
            long exp = ((Number) claims.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) {
                throw unauthorized();
            }
            return new JwtClaims(
                    ((Number) claims.get("uid")).longValue(),
                    String.valueOf(claims.get("name")),
                    String.valueOf(claims.get("jti")),
                    exp);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw unauthorized();
        }
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return ENCODER.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private ApiException unauthorized() {
        return new ApiException(ResultCode.UNAUTHORIZED);
    }

    public record JwtClaims(Long userId, String username, String jti, long exp) {
    }
}
