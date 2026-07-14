package com.mdt.integration.query;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** V_PATIENT_STUDIES 视图行（只读投影，脱敏：仅 PatientID + 检查号 + 影像/报告维度）。 */
public record PatientStudyRow(
        String patientVisitUid, String tenantId, String patientId, String accessionNumber,
        String studyInstanceUid, String modality, LocalDate studyDate, String objectKey,
        String reportId, String reportContent, String pathologyConclusion, LocalDateTime publishTime) {
}
