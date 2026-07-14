package com.mdt.integration.dicom;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 多模态合成 DICOM 生成器（研发态，模拟设备产出）。
 * 按模态产出带报告的合成数据集；生产替换为真实 DICOM 解析（同 DicomAdapter 接口）。
 */
@Component
public class DicomSimulator {

    private final AtomicLong seq = new AtomicLong(System.nanoTime());

    /** 按模态生成一份合成检查（C-STORE 推送场景用） */
    public DicomDataset generate(String modality, String patientId, String accessionNumber, String tenantId) {
        String uid = "1.2.840.113619.2.1.SIM-" + modality + "-" + seq.incrementAndGet();
        Content c = CONTENT.getOrDefault(modality, new Content("模拟检查", null));
        int count = INSTANCE_COUNT.getOrDefault(modality, 100);
        return new DicomDataset(uid, patientId, accessionNumber, tenantId, modality,
                LocalDate.now(), "hot://pacs/" + uid, count, c.report(), c.pathology());
    }

    /** 随机模态（SCP 推送演示用，保证覆盖多模态） */
    public String randomModality() {
        String[] m = DicomDataset.MODALITIES;
        return m[(int) (seq.get() % m.length)];
    }

    private static final Map<String, Content> CONTENT = Map.of(
            "CT",       new Content("CT 上腹部增强，动脉期可见强化灶。", null),
            "MRI",      new Content("MRI 胰腺，胰头见占位信号。", null),
            "US",       new Content("超声内镜见黏膜下隆起，边界清。", null),
            "ENDOSCOPY",new Content("胃镜见胃窦溃疡，取活检。", null),
            "PATH",     new Content("镜下见腺体异型增生。", "结肠腺癌（中分化）"),
            "ECG",      new Content("窦性心律，未见明显异常。", null)
    );

    private static final Map<String, Integer> INSTANCE_COUNT = Map.of(
            "CT", 320, "MRI", 240, "US", 120, "ENDOSCOPY", 60, "PATH", 1, "ECG", 1
    );

    private record Content(String report, String pathology) {}
}
