package com.luojiajun.chat.common;

public class ApiException extends RuntimeException {

    private final IErrorCode errorCode;

    public ApiException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ApiException(IErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApiException(String message) {
        super(message);
        this.errorCode = ResultCode.FAILED;
    }

    public IErrorCode getErrorCode() {
        return errorCode;
    }
}
