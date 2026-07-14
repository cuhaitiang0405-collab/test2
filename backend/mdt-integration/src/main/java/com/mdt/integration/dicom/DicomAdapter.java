package com.mdt.integration.dicom;

/**
 * DICOM 适配器（适配器模式）：屏蔽 PACS 异构接口差异。
 * 建模 SCP（接收 C-STORE 推送）与 SCU（C-FIND→C-MOVE 拉取）两条路径，
 * 二者统一产出 IngestCommand 交 IntegrationService 落库。
 * 生产替换为真实 dcm4che 实现时，仅需替换本接口实现，调用方无感。
 */
public interface DicomAdapter {

    /** 启动 SCP 监听（研发态为内存态，无端口；生产替换为 device.bindConnections()） */
    void startScp();

    /** SCU 拉取：C-FIND 匹配 -> C-MOVE 交付，返回入库指令 */
    IngestCommand pullViaScu(String patientId, String accessionNumber);

    /** SCP 接收：设备 C-STORE 推送到达，返回入库指令 */
    IngestCommand receiveViaScp(DicomDataset ds);
}
