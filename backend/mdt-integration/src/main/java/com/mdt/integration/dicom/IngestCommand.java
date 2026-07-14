package com.mdt.integration.dicom;

import java.time.LocalDate;

/**
 * 入库指令：适配器把 DICOM 元数据转换为与存储无关的落库命令。
 * 由 IntegrationService 负责解析 PatientVisitUID + 幂等 upsert + 审计。
 */
public record IngestCommand(
        String studyInstanceUid, String patientId, String accessionNumber, String tenantId,
        String modality, LocalDate studyDate, String objectKey, int instanceCount,
        String reportContent, String pathologyConclusion) {

    public static IngestCommand from(DicomDataset ds) {
        return new IngestCommand(ds.studyInstanceUid(), ds.patientId(), ds.accessionNumber(),
                ds.tenantId(), ds.modality(), ds.studyDate(), ds.objectKey(),
                ds.instanceCount(), ds.reportContent(), ds.pathologyConclusion());
    }
}
