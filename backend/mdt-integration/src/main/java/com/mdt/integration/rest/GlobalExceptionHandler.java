package com.mdt.integration.rest;

import com.mdt.common.audit.AuditLogger;
import com.mdt.common.trace.GrpcTraceInterceptor;
import com.mdt.common.trace.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 数据接入层全局异常处理器：业务异常 → 对应 HTTP 状态码 + 合规错误体；
 * 未捕获异常 → 500 + 通用文案（绝不回传堆栈）；失败操作统一落审计。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final AuditLogger audit;

    public GlobalExceptionHandler(AuditLogger audit) {
        this.audit = audit;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorBody> handleBadRequest(IllegalArgumentException e) {
        String tid = resolveTrace();
        audit.log("UNKNOWN", "-", null, "INTEGRATION_EXCEPTION", "code=BAD_REQUEST");
        return ResponseEntity.status(400).body(ErrorBody.of("BAD_REQUEST", e.getMessage(), tid));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleOther(Exception e) {
        String tid = resolveTrace();
        log.error("[ERR] uncaught trace={} type={}", tid, e.getClass().getSimpleName(), e);
        audit.log("UNKNOWN", "-", null, "INTERNAL_ERROR", "type=" + e.getClass().getSimpleName());
        return ResponseEntity.status(500)
                .body(ErrorBody.of("INTERNAL_ERROR", "服务器内部错误，请稍后重试", tid));
    }

    private String resolveTrace() {
        String t = TraceContext.get();
        if (t == null || t.isBlank()) t = GrpcTraceInterceptor.currentTraceId();
        return (t == null || t.isBlank()) ? TraceContext.newTraceId() : t;
    }
}
