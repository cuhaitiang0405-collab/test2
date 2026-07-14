package com.mdt.integration.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Mock HIS 患者人口学中间表（研发态，生产替换为真实 HIS 视图/中间表）。
 * 仅存脱敏维度（PatientID + 检查号 + 人口学），不存姓名/身份证。
 */
@Entity
@Table(name = "MOCK_HIS_DEMO")
public class MockHisDemo {
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

    @Column(name = "GENDER", length = 4)
    private String gender;

    @Column(name = "BIRTH_DATE")
    private LocalDate birthDate;

    @Column(name = "DEPT", length = 64)
    private String dept;

    @Column(name = "ATTENDING", length = 64)
    private String attending;

    public MockHisDemo() {}
    public MockHisDemo(String patientVisitUid, String tenantId, String patientId, String accessionNumber,
                       String visitType, String gender, LocalDate birthDate, String dept, String attending) {
        this.patientVisitUid = patientVisitUid; this.tenantId = tenantId; this.patientId = patientId;
        this.accessionNumber = accessionNumber; this.visitType = visitType; this.gender = gender;
        this.birthDate = birthDate; this.dept = dept; this.attending = attending;
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
    public String getGender() { return gender; }
    public void setGender(String v) { this.gender = v; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate v) { this.birthDate = v; }
    public String getDept() { return dept; }
    public void setDept(String v) { this.dept = v; }
    public String getAttending() { return attending; }
    public void setAttending(String v) { this.attending = v; }
}
