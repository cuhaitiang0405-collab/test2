package com.mdt.auth.rest;

import com.mdt.auth.security.ResourceNotFoundException;
import com.mdt.common.jpa.PatientVisit;
import com.mdt.common.jpa.PatientVisitRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 患者就诊索引读写（供 M1 自测"字段能否更新"）：
 * POST 创建/更新 -> 落库；GET 回读 -> 断言一致。
 * 与影像/报告共用 PatientVisitUID（统一就诊唯一标识）。
 */
@RestController
@RequestMapping("/api/patient-visit")
public class PatientVisitController {
    private final PatientVisitRepository repo;

    public PatientVisitController(PatientVisitRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public PatientVisit upsert(@RequestBody PatientVisit pv) {
        return repo.save(pv);   // 有则更新，无则插入
    }

    @GetMapping("/{uid}")
    public PatientVisit get(@PathVariable String uid) {
        return repo.findById(uid).orElseThrow(() -> new ResourceNotFoundException("未找到 " + uid));
    }

    /** 仅返回脱敏字段（PatientID + 检查号），禁止回传姓名/身份证 */
    @GetMapping("/{uid}/masked")
    public Map<String, String> masked(@PathVariable String uid) {
        PatientVisit pv = get(uid);
        return Map.of("patientId", pv.getPatientId(), "accessionNumber", pv.getAccessionNumber());
    }
}
