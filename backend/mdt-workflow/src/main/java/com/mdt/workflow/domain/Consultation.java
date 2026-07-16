package com.mdt.workflow.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 会诊聚合根（CONSULTATION 表）。
 * 通过 patientVisitUid 与数据接入层/影像层共用统一就诊唯一标识，满足合规关联要求。
 */
@Entity
@Table(name = "CONSULTATION")
public class Consultation {

    @Id
    @Column(name = "CONSULTATION_ID", length = 40)
    private String consultationId;

    /** 统一就诊标识（合规关联影像/临床数据，审计脱敏仅显示 PatientID+检查号） */
    @Column(name = "PATIENT_VISIT_UID", length = 64, nullable = false)
    private String patientVisitUid;

    @Column(name = "PATIENT_ID", length = 32, nullable = false)
    private String patientId;

    @Column(name = "ACCESSION_NUMBER", length = 32)
    private String accessionNumber;

    /** 申请人（医生） */
    @Column(name = "APPLICANT", length = 64, nullable = false)
    private String applicant;

    /** 租户（医疗机构），预留多租户行级隔离 */
    @Column(name = "TENANT_ID", length = 32, nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 16, nullable = false)
    private ConsultationStatus status;

    @Column(name = "TITLE", length = 200, nullable = false)
    private String title;

    /** 申请理由/病情摘要 */
    @Column(name = "REASON", length = 1000)
    private String reason;
    @Column(name = "TIER")
    private Integer tier = 1;

    /** 结论（COMPLETED 时落库），支持多方专家意见 */
    @Column(name = "SUMMARY_TEXT", length = 4000)
    private String summaryText;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    public Consultation() {}

    public Consultation(String consultationId, String patientVisitUid, String patientId,
                        String accessionNumber, String applicant, String tenantId,
                        ConsultationStatus status, String title, String reason) {
        this.consultationId = consultationId;
        this.patientVisitUid = patientVisitUid;
        this.patientId = patientId;
        this.accessionNumber = accessionNumber;
        this.applicant = applicant;
        this.tenantId = tenantId;
        this.status = status;
        this.title = title;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // —— Getter / Setter ——
    public String getConsultationId() { return consultationId; }
    public void setConsultationId(String v) { this.consultationId = v; }
    public String getPatientVisitUid() { return patientVisitUid; }
    public void setPatientVisitUid(String v) { this.patientVisitUid = v; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String v) { this.patientId = v; }
    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String v) { this.accessionNumber = v; }
    public String getApplicant() { return applicant; }
    public void setApplicant(String v) { this.applicant = v; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String v) { this.tenantId = v; }
    public ConsultationStatus getStatus() { return status; }
    public void setStatus(ConsultationStatus v) { this.status = v; this.updatedAt = LocalDateTime.now(); }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public Integer getTier() { return tier; }
    public void setTier(Integer v) { this.tier = v; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String v) { this.summaryText = v; this.updatedAt = LocalDateTime.now(); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
