package com.mdt.integration.dicom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 纯 Java DICOM 适配器实现（研发态，不依赖 dcm4che3）。
 * SCP/SCU 语义经内存态 MockPacs + DicomSimulator 跑通，接口与真实实现一致。
 */
// TODO(MOCK-SWITCH): 研发态 mock 实现；转生产替换为 dcm4che3 真实 DicomAdapterImpl（同一 DicomAdapter
//   接口），删除 MockPacs/DicomSimulator 与本类，调用方（IntegrationService/Controller）无感。
@Component
public class PureJavaDicomAdapter implements DicomAdapter {

    private static final Logger log = LoggerFactory.getLogger(PureJavaDicomAdapter.class);
    private final DicomSimulator simulator;
    private final MockPacs mockPacs;
    private final String defaultTenant;

    public PureJavaDicomAdapter(DicomSimulator simulator, MockPacs mockPacs) {
        this.simulator = simulator;
        this.mockPacs = mockPacs;
        this.defaultTenant = "T001";
        // 登记研发态已知患者，使 SCU 拉取可命中（与 DataInitializer 种子对齐）
        mockPacs.registerKnownPatient("P1001", "A202407001");
        mockPacs.registerKnownPatient("P1002", "A202407002");
    }

    @Override
    public void startScp() {
        // 内存态无端口；生产态此处 device.bindConnections() 监听 11112
        log.info("[DICOM] SCP 已就绪（内存态，模拟 11112）");
    }

    @Override
    public IngestCommand pullViaScu(String patientId, String accessionNumber) {
        DicomDataset ds = mockPacs.retrieve(patientId, accessionNumber);   // C-FIND + C-MOVE
        log.info("[DICOM] SCU 拉取完成 studyUid={} modality={}", ds.studyInstanceUid(), ds.modality());
        return IngestCommand.from(ds);
    }

    @Override
    public IngestCommand receiveViaScp(DicomDataset ds) {
        log.info("[DICOM] SCP 接收 studyUid={} modality={} pid={}", ds.studyInstanceUid(), ds.modality(), ds.patientId());
        return IngestCommand.from(ds);
    }

    public String getDefaultTenant() { return defaultTenant; }
}
