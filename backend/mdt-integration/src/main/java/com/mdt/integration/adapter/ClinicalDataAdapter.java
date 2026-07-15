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

        // GAP-9 诊疗数据细类：按业务维度结构化（入院病历 / 长期医嘱 / 临时医嘱），
        // 前端据此分类渲染；种子数据确定性生成，避免引入随机性。
        m.put("categories", buildCategories(row, lis));

        try {
            return om.writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * GAP-9：把扁平临床字段归并为三类诊疗细类。
     * 入院病历 = 人口学 + 主诉 + 诊断 + 过敏；长期医嘱 = 持续用药/处置；
     * 临时医嘱 = 单次检查/检验（由 LIS 结果派生）。
     */
    private Map<String, Object> buildCategories(ClinicalSummaryRow row, List<Map<String, String>> lis) {
        Map<String, Object> admission = new LinkedHashMap<>();
        admission.put("gender", row.gender());
        admission.put("birthDate", row.birthDate() == null ? null : row.birthDate().toString());
        admission.put("dept", row.dept());
        admission.put("attending", row.attending());
        admission.put("chiefComplaint", row.chiefComplaint());
        admission.put("diagnosis", row.diagnosis());
        admission.put("allergy", row.allergy());

        Map<String, Object> longTermOrder = new LinkedHashMap<>();
        longTermOrder.put("type", "长期医嘱");
        longTermOrder.put("items", List.of(
                Map.of("name", row.medication() == null || row.medication().isBlank() ? "无" : row.medication(),
                       "freq", "长期"),
                Map.of("name", "卧床休息", "freq", "长期"),
                Map.of("name", "吸氧 2L/min", "freq", "长期")));

        Map<String, Object> tempOrder = new LinkedHashMap<>();
        tempOrder.put("type", "临时医嘱");
        tempOrder.put("items", lis.isEmpty()
                ? List.of(Map.of("name", "血常规 + 生化全套", "freq", "临时"))
                : lis.stream().map(l -> Map.of(
                        "name", nz(l.get("item")) + "（" + nz(l.get("value")) + nz(l.get("unit")) + "）",
                        "freq", "临时")).collect(Collectors.toList()));

        Map<String, Object> cats = new LinkedHashMap<>();
        cats.put("admissionRecord", admission);
        cats.put("longTermOrders", longTermOrder);
        cats.put("tempOrders", tempOrder);
        return cats;
    }

    private String nz(String s) { return s == null ? "" : s; }
}
