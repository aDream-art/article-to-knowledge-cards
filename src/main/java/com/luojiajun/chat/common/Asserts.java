package com.luojiajun.chat.common;

public final class Asserts {

    private Asserts() {
    }

    public static void fail(String message) {
        throw new ApiException(message);
    }

    public static void fail(IErrorCode errorCode) {
        throw new ApiException(errorCode);
    }

    public static void fail(IErrorCode errorCode, String message) {
        throw new ApiException(errorCode, message);
    }

    public static void notNull(Object value, String message) {
        if (value == null) {
            throw new ApiException(ResultCode.NOT_FOUND, message);
        }
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new ApiException(ResultCode.VALIDATE_FAILED, message);
        }
    }

    public static void hasPermission(boolean condition, String message) {
        if (!condition) {
            throw new ApiException(ResultCode.FORBIDDEN, message);
        }
    }
}
