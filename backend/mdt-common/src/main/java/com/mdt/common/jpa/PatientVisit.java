package com.mdt.common.jpa;

import jakarta.persistence.*;

/**
 * 患者就诊主索引（统一就诊唯一标识 PatientVisitUID）。
 * 用于 M1 自测"字段能否更新"：经 API 改一条记录 -> 落库 -> 回读一致。
 */
@Entity
@Table(name = "PATIENT_VISIT")
public class PatientVisit {
    @Id
    @Column(name = "PATIENT_VISIT_UID", length = 64)
    private String patientVisitUid;

    @Column(name = "TENANT_ID", length = 32, nullable = false)
    private String tenantId;

    @Column(name = "PATIENT_ID", length = 64, nullable = false)
    private String patientId;

    @Column(name = "ACCESSION_NUMBER", length = 64)
    private String accessionNumber;

    @Column(name = "VISIT_TYPE", length = 16)
    private String visitType;

    public PatientVisit() {}
    public PatientVisit(String uid, String tenantId, String patientId, String accessionNumber, String visitType) {
        this.patientVisitUid = uid; this.tenantId = tenantId; this.patientId = patientId;
        this.accessionNumber = accessionNumber; this.visitType = visitType;
    }

    public String getPatientVisitUid() { return patientVisitUid; }
    public void setPatientVisitUid(String v) { this.patientVisitUid = v; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String v) { this.tenantId = v; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String v) { this.patientId = v; }
    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String v) { this.accessionNumber = v; }
    public String getVisitType() { return visitType; }
    public void setVisitType(String v) { this.visitType = v; }
}
