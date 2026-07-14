package com.mdt.integration.domain;

import jakarta.persistence.*;

/** Mock EMR 就诊记录中间表（研发态）。主诉/诊断/过敏/用药，脱敏维度。 */
@Entity
@Table(name = "MOCK_EMR_ENCOUNTER")
public class MockEmrEncounter {
    @Id
    @Column(name = "PATIENT_VISIT_UID", length = 64)
    private String patientVisitUid;

    @Column(name = "TENANT_ID", length = 32, nullable = false)
    private String tenantId;

    @Column(name = "CHIEF_COMPLAINT", length = 500)
    private String chiefComplaint;

    @Column(name = "DIAGNOSIS", length = 500)
    private String diagnosis;

    @Column(name = "ALLERGY", length = 200)
    private String allergy;

    @Column(name = "MEDICATION", length = 500)
    private String medication;

    public MockEmrEncounter() {}
    public MockEmrEncounter(String patientVisitUid, String tenantId, String chiefComplaint,
                            String diagnosis, String allergy, String medication) {
        this.patientVisitUid = patientVisitUid; this.tenantId = tenantId;
        this.chiefComplaint = chiefComplaint; this.diagnosis = diagnosis;
        this.allergy = allergy; this.medication = medication;
    }

    public String getPatientVisitUid() { return patientVisitUid; }
    public void setPatientVisitUid(String v) { this.patientVisitUid = v; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String v) { this.tenantId = v; }
    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String v) { this.chiefComplaint = v; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String v) { this.diagnosis = v; }
    public String getAllergy() { return allergy; }
    public void setAllergy(String v) { this.allergy = v; }
    public String getMedication() { return medication; }
    public void setMedication(String v) { this.medication = v; }
}
