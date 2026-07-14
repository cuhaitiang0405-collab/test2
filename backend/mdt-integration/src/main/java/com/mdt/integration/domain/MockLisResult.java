package com.mdt.integration.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Mock LIS 检验结果中间表（研发态）。一对多，按 PATIENT_VISIT_UID 归集。 */
@Entity
@Table(name = "MOCK_LIS_RESULT")
public class MockLisResult {
    @Id
    @Column(name = "RESULT_ID", length = 64)
    private String resultId;

    @Column(name = "PATIENT_VISIT_UID", length = 64, nullable = false)
    private String patientVisitUid;

    @Column(name = "TENANT_ID", length = 32, nullable = false)
    private String tenantId;

    @Column(name = "ITEM", length = 100)
    private String item;

    @Column(name = "RESULT_VALUE", length = 100)
    private String resultValue;

    @Column(name = "UNIT", length = 20)
    private String unit;

    @Column(name = "REF_RANGE", length = 60)
    private String refRange;

    @Column(name = "TS")
    private LocalDateTime ts;

    public MockLisResult() {}
    public MockLisResult(String resultId, String patientVisitUid, String tenantId, String item,
                         String resultValue, String unit, String refRange, LocalDateTime ts) {
        this.resultId = resultId; this.patientVisitUid = patientVisitUid; this.tenantId = tenantId;
        this.item = item; this.resultValue = resultValue; this.unit = unit;
        this.refRange = refRange; this.ts = ts;
    }

    public String getResultId() { return resultId; }
    public void setResultId(String v) { this.resultId = v; }
    public String getPatientVisitUid() { return patientVisitUid; }
    public void setPatientVisitUid(String v) { this.patientVisitUid = v; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String v) { this.tenantId = v; }
    public String getItem() { return item; }
    public void setItem(String v) { this.item = v; }
    public String getResultValue() { return resultValue; }
    public void setResultValue(String v) { this.resultValue = v; }
    public String getUnit() { return unit; }
    public void setUnit(String v) { this.unit = v; }
    public String getRefRange() { return refRange; }
    public void setRefRange(String v) { this.refRange = v; }
    public LocalDateTime getTs() { return ts; }
    public void setTs(LocalDateTime v) { this.ts = v; }
}
