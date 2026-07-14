package com.mdt.auth.rest;

import com.mdt.auth.security.AuthException;
import com.mdt.common.audit.AuditLogger;
import com.mdt.common.trace.GrpcTraceInterceptor;
import com.mdt.common.trace.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局 REST 异常处理器：
 * - 业务异常(AuthException) → 映射对应 HTTP 状态码 + 合规错误体 {code,message,traceId}
 * - 未捕获异常 → 500 + 通用文案，绝不回传堆栈/内部细节
 * - 失败操作统一落审计（脱敏），TraceId 回写响应体便于全链路排障
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final AuditLogger audit;

    public GlobalExceptionHandler(AuditLogger audit) {
        this.audit = audit;
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorBody> handleBiz(AuthException e) {
        String tid = resolveTrace();
        log.warn("[ERR] code={} status={} trace={}", e.getCode(), e.getHttpStatus(), tid);
        audit.log("UNKNOWN", "-", null, "AUTH_EXCEPTION", "code=" + e.getCode());
        return ResponseEntity.status(e.getHttpStatus())
                .body(ErrorBody.of(e.getCode(), e.getMessage(), tid));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleOther(Exception e) {
        String tid = resolveTrace();
        log.error("[ERR] uncaught trace={} type={}", tid, e.getClass().getSimpleName(), e);
        audit.log("UNKNOWN", "-", null, "INTERNAL_ERROR", "type=" + e.getClass().getSimpleName());
        // 不泄露原始异常信息给调用方
        return ResponseEntity.status(500)
                .body(ErrorBody.of("INTERNAL_ERROR", "服务器内部错误，请稍后重试", tid));
    }

    /** 优先取 REST 线程 MDC 的 TraceId，回退 gRPC Context */
    private String resolveTrace() {
        String t = TraceContext.get();
        if (t == null || t.isBlank()) t = GrpcTraceInterceptor.currentTraceId();
        return (t == null || t.isBlank()) ? TraceContext.newTraceId() : t;
    }
}
