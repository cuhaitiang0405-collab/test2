package com.mdt.image.rest;

import com.mdt.common.trace.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 影像核心引擎全局异常处理。
 * <p>
 * 遵循安全合规：不向客户端暴露内部堆栈；TraceId 附带响应供运维串联。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 参数非法 → 400，附带业务错误信息 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        log.warn("[IMAGE] 请求参数非法: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
    }

    /** 其他未预期异常 → 500，仅暴露 TraceId，不泄露内部细节 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleInternal(Exception e) {
        String traceId = TraceContext.get();
        log.error("[IMAGE] 未预期异常 traceId={}", traceId, e);
        return ResponseEntity.internalServerError()
                .body(Map.of("error",
                        "服务器内部异常，traceId=" + (traceId != null ? traceId : "")));
    }
}
