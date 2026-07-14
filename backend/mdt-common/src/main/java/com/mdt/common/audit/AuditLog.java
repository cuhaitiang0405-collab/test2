package com.mdt.common.audit;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 审计日志实体（所有域关键操作落库）。
 * 仅存脱敏后的统一就诊标识，不存姓名/身份证。
 */
@Entity
@Table(name = "AUDIT_LOG")
public class AuditLog {
    @Id
    @Column(name = "AUDIT_ID", length = 64)
    private String auditId;

    @Column(name = "TRACE_ID", length = 64, nullable = false)
    private String traceId;

    @Column(name = "TENANT_ID", length = 32, nullable = false)
    private String tenantId;

    @Column(name = "OPERATOR_ID", length = 64, nullable = false)
    private String operatorId;

    @Column(name = "PATIENT_VISIT_UID", length = 64)
    private String patientVisitUid;

    @Column(name = "ACTION", length = 32)
    private String action;

    @Column(name = "DETAIL", length = 1000)
    private String detail;

    @Column(name = "TS")
    private Long ts = Instant.now().getEpochSecond();

    public AuditLog() {}
    public AuditLog(String auditId, String traceId, String tenantId,
                    String operatorId, String patientVisitUid, String action, String detail) {
        this.auditId = auditId; this.traceId = traceId; this.tenantId = tenantId;
        this.operatorId = operatorId; this.patientVisitUid = patientVisitUid;
        this.action = action; this.detail = detail;
    }

    // getters/setters
    public String getAuditId() { return auditId; }
    public void setAuditId(String v) { this.auditId = v; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String v) { this.traceId = v; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String v) { this.tenantId = v; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String v) { this.operatorId = v; }
    public String getPatientVisitUid() { return patientVisitUid; }
    public void setPatientVisitUid(String v) { this.patientVisitUid = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getDetail() { return detail; }
    public void setDetail(String v) { this.detail = v; }
    public Long getTs() { return ts; }
    public void setTs(Long v) { this.ts = v; }
}
