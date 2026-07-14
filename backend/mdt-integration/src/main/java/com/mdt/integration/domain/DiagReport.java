package com.mdt.integration.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 诊断报告（放射/超声/内镜/病理/心电）。报告正文与病理结论为字段级权限管控对象。 */
@Entity
@Table(name = "DIAG_REPORT")
public class DiagReport {
    @Id
    @Column(name = "REPORT_ID", length = 64)
    private String reportId;

    @Column(name = "PATIENT_VISIT_UID", length = 64, nullable = false)
    private String patientVisitUid;

    @Column(name = "TENANT_ID", length = 32, nullable = false)
    private String tenantId;

    @Column(name = "MODALITY", length = 16, nullable = false)
    private String modality;

    @Column(name = "REPORT_CONTENT", length = 4000)
    private String reportContent;

    @Column(name = "PATHOLOGY_CONCLUSION", length = 4000)   // 病理结论（受限字段示例）
    private String pathologyConclusion;

    @Column(name = "PUBLISH_TIME")
    private LocalDateTime publishTime;

    public DiagReport() {}
    public DiagReport(String reportId, String patientVisitUid, String tenantId, String modality,
                      String reportContent, String pathologyConclusion, LocalDateTime publishTime) {
        this.reportId = reportId; this.patientVisitUid = patientVisitUid; this.tenantId = tenantId;
        this.modality = modality; this.reportContent = reportContent;
        this.pathologyConclusion = pathologyConclusion; this.publishTime = publishTime;
    }

    public String getReportId() { return reportId; }
    public void setReportId(String v) { this.reportId = v; }
    public String getPatientVisitUid() { return patientVisitUid; }
    public void setPatientVisitUid(String v) { this.patientVisitUid = v; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String v) { this.tenantId = v; }
    public String getModality() { return modality; }
    public void setModality(String v) { this.modality = v; }
    public String getReportContent() { return reportContent; }
    public void setReportContent(String v) { this.reportContent = v; }
    public String getPathologyConclusion() { return pathologyConclusion; }
    public void setPathologyConclusion(String v) { this.pathologyConclusion = v; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime v) { this.publishTime = v; }
}
