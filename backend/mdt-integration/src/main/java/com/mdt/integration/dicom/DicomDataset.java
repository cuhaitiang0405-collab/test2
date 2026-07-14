package com.mdt.integration.dicom;

import java.time.LocalDate;

/**
 * 合成 DICOM 数据集（纯 Java，零 DICOM 网络依赖）。
 * 仅建模接入层关心的最小元数据 + 报告，足以驱动 SCP/SCU 入库链路与统一索引。
 */
public record DicomDataset(
        String studyInstanceUid, String patientId, String accessionNumber, String tenantId,
        String modality, LocalDate studyDate, String objectKey, int instanceCount,
        String reportContent, String pathologyConclusion) {

    /** 约定模态集：覆盖项目要求的多模态 PACS（放射/超声/内镜/病理/心电） */
    public static final String[] MODALITIES = {"CT", "MRI", "US", "ENDOSCOPY", "PATH", "ECG"};
}
