package com.luojiajun.chat.common;

/**
 * 业务错误码：与前端约定的数字，前两位是 HTTP 语义、后三位是业务细分。
 */
public enum ResultCode implements IErrorCode {
    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(40001, "参数检验失败"),
    UNAUTHORIZED(40101, "暂未登录或 token 已过期"),
    FORBIDDEN(40301, "没有相关权限"),
    NOT_FOUND(40401, "资源不存在"),
    TOO_MANY_REQUESTS(42901, "请求过于频繁，请稍后再试"),
    QUOTA_EXCEEDED(42902, "今日 token 配额已用完"),
    MODEL_ERROR(50001, "模型服务异常");

    private final long code;
    private final String message;

    ResultCode(long code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public long getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
