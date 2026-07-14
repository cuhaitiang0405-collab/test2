package com.mdt.image.port;

import com.mdt.image.domain.ImageVolume;

/**
 * 体数据来源（适配器缝 / SPI）。
 * 研发态由 {@code SyntheticVolumeSource} 程序化生成；生产期替换为真实实现：
 *  - 对象存储拉取（MINIO/S3/OBS）+ WADO-RS 取帧
 *  - 或 DICOM 解码（dcm4che3 / WASM 解码器）后归一化为 Int16 体数据
 * 调用方（VolumeService）仅依赖本接口，切换真实源零侵入。
 */
public interface VolumeSource {
    /** 按统一检查标识加载体数据；modality 用于塑形（CT/MRI 不同对比） */
    ImageVolume load(String studyUid, String modality);
}
