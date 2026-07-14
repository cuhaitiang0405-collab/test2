package com.mdt.integration.dicom;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/** 纯 Java DICOM 适配器单元测试（无 Spring，验证 SCP/SCU 语义与多模态生成）。 */
class DicomAdapterTest {

    private final DicomSimulator simulator = new DicomSimulator();
    private final MockPacs pacs = new MockPacs();
    private final DicomAdapter adapter = new PureJavaDicomAdapter(simulator, pacs);

    @Test
    void simulatorCoversAllModalitiesWithContent() {
        for (String m : DicomDataset.MODALITIES) {
            DicomDataset ds = simulator.generate(m, "P9", "A9", "T001");
            assertThat(ds.modality()).isEqualTo(m);
            assertThat(ds.studyInstanceUid()).isNotBlank();
            assertThat(ds.objectKey()).startsWith("hot://pacs/");
            if ("PATH".equals(m)) {
                assertThat(ds.pathologyConclusion()).contains("腺癌");   // 病理结论存在
            } else {
                assertThat(ds.reportContent()).isNotBlank();            // 其余含报告正文
            }
        }
    }

    @Test
    void mockPacsReturnsCtForKnownPatient() {
        pacs.registerKnownPatient("P1001", "A202407001");
        DicomDataset ds = pacs.retrieve("P1001", "A202407001");
        assertThat(ds.modality()).isEqualTo("CT");
        assertThat(ds.patientId()).isEqualTo("P1001");
    }

    @Test
    void mockPacsThrowsForUnknownPatient() {
        assertThatThrownBy(() -> pacs.retrieve("NOPE", "X"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void scuPullProducesIngestCommand() {
        pacs.registerKnownPatient("P1001", "A202407001");
        IngestCommand cmd = adapter.pullViaScu("P1001", "A202407001");
        assertThat(cmd.patientId()).isEqualTo("P1001");
        assertThat(cmd.modality()).isEqualTo("CT");
        assertThat(cmd.studyInstanceUid()).isNotBlank();
    }

    @Test
    void scpPushConvertsDatasetToIngestCommand() {
        DicomDataset ds = simulator.generate("ECG", "P1002", "A202407002", "T001");
        IngestCommand cmd = adapter.receiveViaScp(ds);
        assertThat(cmd.modality()).isEqualTo("ECG");
        assertThat(cmd.patientId()).isEqualTo("P1002");
        assertThat(cmd).isEqualTo(IngestCommand.from(ds));
    }
}
