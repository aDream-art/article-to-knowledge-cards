package com.luojiajun.chat.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public CommonResult<Void> handleApiException(ApiException e, HttpServletRequest request) {
        log.warn("业务异常 path={} msg={}", request.getRequestURI(), e.getMessage());
        return new CommonResult<>(e.getErrorCode().getCode(), e.getMessage(), null);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public CommonResult<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .orElse(ResultCode.VALIDATE_FAILED.getMessage());
        return new CommonResult<>(ResultCode.VALIDATE_FAILED.getCode(), message, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public CommonResult<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return new CommonResult<>(ResultCode.VALIDATE_FAILED.getCode(), "缺少参数 " + e.getParameterName(), null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public CommonResult<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return new CommonResult<>(ResultCode.VALIDATE_FAILED.getCode(), "请求体格式错误", null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public CommonResult<Void> handleUploadSize(MaxUploadSizeExceededException e) {
        return new CommonResult<>(ResultCode.VALIDATE_FAILED.getCode(), "文件超过大小限制（1MB）", null);
    }

    /**
     * SSE 客户端断开时 Tomcat 会抛这个，响应已提交，不能再写 JSON。
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncNotUsable(AsyncRequestNotUsableException e) {
        log.debug("SSE 客户端已断开: {}", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<Void> handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        log.error("未预期异常 path={}", request.getRequestURI(), e);
        if (response.isCommitted()) {
            return null;
        }
        return new CommonResult<>(ResultCode.FAILED.getCode(), "服务内部错误", null);
    }
}
