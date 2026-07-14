package com.mdt.integration.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/** 检查/影像索引（中间表，关联对象存储路径）。统一就诊标识 PatientVisitUID 关联主索引。 */
@Entity
@Table(name = "STUDY_INDEX")
public class StudyIndex {
    @Id
    @Column(name = "STUDY_INSTANCE_UID", length = 64)
    private String studyInstanceUid;

    @Column(name = "PATIENT_VISIT_UID", length = 64, nullable = false)
    private String patientVisitUid;

    @Column(name = "TENANT_ID", length = 32, nullable = false)
    private String tenantId;

    @Column(name = "MODALITY", length = 16, nullable = false)   // CT/MRI/US/ENDOSCOPY/PATH/ECG
    private String modality;

    @Column(name = "STUDY_DATE")
    private LocalDate studyDate;

    @Column(name = "OBJECT_KEY", length = 512)
    private String objectKey;

    @Column(name = "INSTANCE_COUNT")
    private Integer instanceCount;

    public StudyIndex() {}
    public StudyIndex(String studyInstanceUid, String patientVisitUid, String tenantId,
                      String modality, LocalDate studyDate, String objectKey, Integer instanceCount) {
        this.studyInstanceUid = studyInstanceUid; this.patientVisitUid = patientVisitUid;
        this.tenantId = tenantId; this.modality = modality; this.studyDate = studyDate;
        this.objectKey = objectKey; this.instanceCount = instanceCount;
    }

    public String getStudyInstanceUid() { return studyInstanceUid; }
    public void setStudyInstanceUid(String v) { this.studyInstanceUid = v; }
    public String getPatientVisitUid() { return patientVisitUid; }
    public void setPatientVisitUid(String v) { this.patientVisitUid = v; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String v) { this.tenantId = v; }
    public String getModality() { return modality; }
    public void setModality(String v) { this.modality = v; }
    public LocalDate getStudyDate() { return studyDate; }
    public void setStudyDate(LocalDate v) { this.studyDate = v; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String v) { this.objectKey = v; }
    public Integer getInstanceCount() { return instanceCount; }
    public void setInstanceCount(Integer v) { this.instanceCount = v; }
}
