package com.mdt.common.audit;

import com.mdt.common.security.Desensitizer;
import com.mdt.common.trace.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 统一审计日志门面：关键操作调用此方法，异步/同步落库 AUDIT_LOG，
 * 同时输出脱敏后的日志（仅 PatientID + 检查号）。
 */
@Component
public class AuditLogger {
    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);
    private final AuditRepository repository;

    public AuditLogger(AuditRepository repository) {
        this.repository = repository;
    }

    public void log(String tenantId, String operatorId, String patientVisitUid,
                    String action, String detail) {
        String traceId = resolveTrace();
        // 1) 落库（detail 经脱敏，禁止明文姓名/身份证）
        String safeDetail = Desensitizer.mask(detail);
        try {
            repository.save(new AuditLog(
                    UUID.randomUUID().toString().replace("-", ""),
                    traceId, tenantId, operatorId, patientVisitUid, action, safeDetail));
        } catch (Exception e) {
            log.warn("[AUDIT] 落库失败 trace={} action={}", traceId, action);
        }
        // 2) 日志输出（脱敏）
        log.info("[AUDIT] trace={} tenant={} operator={} visitUid={} action={} detail={}",
                traceId, tenantId, operatorId, patientVisitUid, action, safeDetail);
    }

    /** TraceId 解析：gRPC Context -> REST MDC -> 生成兜底，保证审计必有 TraceId */
    private String resolveTrace() {
        return com.mdt.common.trace.GrpcTraceInterceptor.currentTraceId();
    }
}
