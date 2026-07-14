package com.mdt.integration.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdt.integration.domain.MockLisResult;
import com.mdt.integration.domain.MockLisResultRepository;
import com.mdt.integration.query.ClinicalSummaryRow;
import com.mdt.integration.query.ClinicalViewRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 临床数据适配器（适配器模式）：从 HIS/EMR/LIS 中间表/视图聚合临床数据。
 * 返回脱敏 JSON：仅 PatientID + 检查号 + 临床维度，绝不出现姓名/身份证。
 * 生产替换为真实 HIS/EMR 接口，本方法签名不变。
 */
@Component
public class ClinicalDataAdapter {

    private final ClinicalViewRepository viewRepo;
    // TODO(MOCK-SWITCH): 转生产须将 MockLisResultRepository 抽为 LisRepository 接口，本类仅依赖接口；
    //   mock/real 各一实现，经 @Profile 装配。当前直接依赖具体 mock 类属技术债（见 docs/tech-debt-mock-to-real.md）。
    private final MockLisResultRepository lisRepo;
    private final ObjectMapper om = new ObjectMapper();

    public ClinicalDataAdapter(ClinicalViewRepository viewRepo, MockLisResultRepository lisRepo) {
        this.viewRepo = viewRepo;
        this.lisRepo = lisRepo;
    }

    /** 按统一就诊标识拉取脱敏临床数据（HIS 人口学 + EMR 就诊 + LIS 检验） */
    public String fetch(String patientVisitUid) {
        ClinicalSummaryRow row = viewRepo.fetchClinicalSummary(patientVisitUid);
        if (row == null) return "{}";
        List<Map<String, String>> lis = lisRepo.findByPatientVisitUid(patientVisitUid).stream()
                .map(l -> Map.of(
                        "item", nz(l.getItem()), "value", nz(l.getResultValue()),
                        "unit", nz(l.getUnit()), "ref", nz(l.getRefRange())))
                .collect(Collectors.toList());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("patientVisitUid", row.patientVisitUid());
        m.put("patientId", row.patientId());
        m.put("accessionNumber", row.accessionNumber());
        m.put("gender", row.gender());
        m.put("birthDate", row.birthDate() == null ? null : row.birthDate().toString());
        m.put("dept", row.dept());
        m.put("attending", row.attending());
        m.put("chiefComplaint", row.chiefComplaint());
        m.put("diagnosis", row.diagnosis());
        m.put("allergy", row.allergy());
        m.put("medication", row.medication());
        m.put("lis", lis);
        try {
            return om.writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String nz(String s) { return s == null ? "" : s; }
}
