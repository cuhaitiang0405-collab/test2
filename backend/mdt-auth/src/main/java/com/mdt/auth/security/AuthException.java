package com.mdt.auth.security;

/** 业务异常基类：携带 HTTP 状态码与错误码，供全局异常处理器映射。 */
public class AuthException extends RuntimeException {
    private final int httpStatus;
    private final String code;

    public AuthException(int httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public int getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
}
