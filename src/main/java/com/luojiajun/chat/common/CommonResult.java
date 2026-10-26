package com.luojiajun.chat.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回体。流式接口不使用它（SSE 用自己的 error 事件）。
 */
@Data
@Schema(description = "统一返回体")
public class CommonResult<T> implements Serializable {

    @Schema(description = "业务码，200 表示成功")
    private long code;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "业务数据")
    private T data;

    public CommonResult() {
    }

    public CommonResult(long code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> CommonResult<T> success(T data, String message) {
        return new CommonResult<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> CommonResult<T> failed() {
        return new CommonResult<>(ResultCode.FAILED.getCode(), ResultCode.FAILED.getMessage(), null);
    }

    public static <T> CommonResult<T> failed(String message) {
        return new CommonResult<>(ResultCode.FAILED.getCode(), message, null);
    }

    public static <T> CommonResult<T> failed(IErrorCode errorCode) {
        return new CommonResult<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> CommonResult<T> failed(IErrorCode errorCode, String message) {
        return new CommonResult<>(errorCode.getCode(), message, null);
    }

    public static <T> CommonResult<T> validateFailed(String message) {
        return new CommonResult<>(ResultCode.VALIDATE_FAILED.getCode(), message, null);
    }

    public static <T> CommonResult<T> unauthorized(T data) {
        return new CommonResult<>(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage(), data);
    }

    public static <T> CommonResult<T> forbidden(T data) {
        return new CommonResult<>(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage(), data);
    }
}
