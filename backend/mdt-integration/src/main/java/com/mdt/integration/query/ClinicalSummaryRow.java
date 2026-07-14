package com.mdt.integration.query;

import java.time.LocalDate;

/** V_CLINICAL_SUMMARY 视图行（HIS 人口学 + EMR 就诊，脱敏维度）。 */
public record ClinicalSummaryRow(
        String patientVisitUid, String tenantId, String patientId, String accessionNumber,
        String gender, LocalDate birthDate, String dept, String attending,
        String chiefComplaint, String diagnosis, String allergy, String medication) {
}
