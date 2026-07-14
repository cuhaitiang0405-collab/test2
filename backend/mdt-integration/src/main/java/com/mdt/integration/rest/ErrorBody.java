package com.mdt.integration.rest;

/** 统一错误响应体：不泄露堆栈/内部细节，仅回业务错误码 + 脱敏文案 + TraceId。 */
public record ErrorBody(String code, String message, String traceId) {
    public static ErrorBody of(String code, String message, String traceId) {
        return new ErrorBody(code, message, traceId);
    }
}
