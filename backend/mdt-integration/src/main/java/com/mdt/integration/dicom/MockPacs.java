package com.mdt.integration.dicom;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 内存态模拟 PACS（研发态）：建模 C-FIND 匹配 + C-MOVE 交付两条 DIMSE。
 * 不绑端口、零网络依赖；每次查询按 (PatientID, AccessionNumber) 返回一份合成 CT 检查，
 * 模拟「SCU 拉取」链路（真实环境换成 dcm4che 连接远端 PACS，接口不变）。
 */
@Component
@Profile("!prod")
public class MockPacs {

    private final AtomicLong seq = new AtomicLong(System.nanoTime());
    // 已知患者目录（演示 SCU 拉取命中）；未知患者抛出未找到
    private final Map<String, Boolean> known = new ConcurrentHashMap<>();

    public void registerKnownPatient(String patientId, String accessionNumber) {
        known.put(key(patientId, accessionNumber), Boolean.TRUE);
    }

    /** 模拟 C-FIND 匹配 + C-MOVE 交付：返回该患者的合成 CT 检查 */
    public DicomDataset retrieve(String patientId, String accessionNumber) {
        if (!known.containsKey(key(patientId, accessionNumber))) {
            throw new IllegalStateException("PACS 未找到检查：patientId=" + patientId
                    + ", accession=" + accessionNumber);
        }
        String uid = "1.2.840.113619.2.1.PACS-" + seq.incrementAndGet();
        return new DicomDataset(uid, patientId, accessionNumber, "T001", "CT",
                LocalDate.now(), "hot://pacs/" + uid, 300,
                "C-FIND/C-MOVE 拉取：CT 上腹部增强，动脉期可见强化灶。", null);
    }

    private String key(String pid, String acc) {
        return pid + "|" + acc;
    }
}
