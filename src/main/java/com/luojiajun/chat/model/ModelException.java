package com.luojiajun.chat.model;

import lombok.Getter;

/**
 * 模型层异常统一形状：是否可重试决定调用层的重试策略。
 */
@Getter
public class ModelException extends RuntimeException {

    private final String code;
    private final boolean retryable;
    private final Integer httpStatus;

    public ModelException(String code, String message, boolean retryable, Integer httpStatus, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.retryable = retryable;
        this.httpStatus = httpStatus;
    }

    public static ModelException notConfigured(String provider) {
        return new ModelException("PROVIDER_NOT_CONFIGURED",
                "模型 " + provider + " 未配置 API Key，且 mock 未开启", false, null, null);
    }

    public static ModelException timeout(String provider) {
        return new ModelException("TIMEOUT", "模型 " + provider + " 响应超时", true, null, null);
    }

    public static ModelException upstream(String provider, int httpStatus, String body) {
        boolean retryable = httpStatus >= 500 || httpStatus == 429;
        String trimmed = body == null ? "" : body.substring(0, Math.min(body.length(), 200));
        return new ModelException("UPSTREAM_" + httpStatus,
                "模型 " + provider + " 返回 " + httpStatus + ": " + trimmed, retryable, httpStatus, null);
    }

    public static ModelException parse(String provider, Throwable cause) {
        return new ModelException("PARSE_ERROR", "解析模型 " + provider + " 响应失败", false, null, cause);
    }
}
