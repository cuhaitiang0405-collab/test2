package com.mdt.workflow.rest;

import com.mdt.workflow.domain.Consultation;
import com.mdt.common.security.TenantContext;
import com.mdt.workflow.domain.ConsultationExpert;
import com.mdt.workflow.domain.ConsultationStatus;
import com.mdt.workflow.service.ConsultationService;
import com.mdt.workflow.service.ConsultationService.ConsultationDetail;
import com.mdt.workflow.service.IllegalStateTransitionException;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ④ 业务流程层 REST 入口（网关路由 /api/workflow/**）。
 * 经 RestTraceFilter 透传 TraceId，关键操作由 ConsultationService 写脱敏审计。
 * operator / tenant 取自请求头（缺省回退 WEB/T001），与前端登录态对齐。
 */
@RestController
@RequestMapping("/api/workflow")
public class ConsultationController {

    private final ConsultationService svc;
    public ConsultationController(ConsultationService svc) { this.svc = svc; }

    private String op(java.util.Map<String, String> h) { return h.getOrDefault("X-Mdt-Operator", "WEB"); }
    private String ten(java.util.Map<String, String> h) { return h.getOrDefault("X-Mdt-Tenant", "T001"); }

    /** 申请会诊 */
    @PostMapping("/consultations")
    public Map<String, Object> apply(@RequestBody Map<String, Object> body,
                                     @RequestHeader Map<String, String> headers) {
        List<?> eIds = (List<?>) body.getOrDefault("expertIds", List.of());
        List<?> eNames = (List<?>) body.getOrDefault("expertNames", List.of());
        Consultation c = svc.apply(
                str(body, "patientVisitUid"), str(body, "patientId"), str(body, "accessionNumber"),
                str(body, "applicant", op(headers)), TenantContext.getTenantId(),
                str(body, "title"), str(body, "reason"),
                toStrings(eIds), toStrings(eNames));
        // 返回完整视图（与 notify/confirm/start/complete 一致），含 reason/applicant/专家明细
        return toView(svc.get(c.getConsultationId()));
    }

    /** 通知专家（触发短信异步队列） */
    @PostMapping("/consultations/{id}/notify")
    public Map<String, Object> notify(@PathVariable String id, @RequestHeader Map<String, String> h) {
        Consultation c = svc.notify(id, op(h));
        return toView(svc.get(id));
    }

    /** 某专家确认（全员确认后自动进入 CONFIRMED） */
    @PostMapping("/consultations/{id}/confirm")
    public Map<String, Object> confirm(@PathVariable String id,
                                       @RequestParam String expertId, @RequestHeader Map<String, String> h) {
        Consultation c = svc.confirm(id, expertId, op(h));
        return toView(svc.get(id));
    }

    /** 开始会诊 */
    @PostMapping("/consultations/{id}/start")
    public Map<String, Object> start(@PathVariable String id, @RequestHeader Map<String, String> h) {
        Consultation c = svc.start(id, op(h));
        return toView(svc.get(id));
    }

    /** 总结会诊（落库结论） */
    @PostMapping("/consultations/{id}/complete")
    public Map<String, Object> complete(@PathVariable String id,
                                        @RequestBody Map<String, String> body, @RequestHeader Map<String, String> h) {
        Consultation c = svc.complete(id, body.getOrDefault("summaryText", ""), op(h));
        return toView(svc.get(id));
    }

    /** 取消会诊 */
    @PostMapping("/consultations/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable String id, @RequestHeader Map<String, String> h) {
        Consultation c = svc.cancel(id, op(h));
        return toView(svc.get(id));
    }

    /** 列表（按状态可选过滤） */
    @GetMapping("/consultations")
    public List<Map<String, Object>> list(@RequestParam(required = false) String status,
                                          @RequestHeader Map<String, String> h) {
        ConsultationStatus st = status == null ? null : ConsultationStatus.valueOf(status);
        return svc.list(TenantContext.getTenantId(), st).stream().map(this::toCard).collect(Collectors.toList());
    }

    /** 详情（含专家确认明细） */
    @GetMapping("/consultations/{id}")
    public Map<String, Object> detail(@PathVariable String id) {
        return toView(svc.get(id));
    }

    /** 各状态计数（供工作台待办） */
    @GetMapping("/consultations/stats")
    public Map<String, Object> stats(@RequestHeader Map<String, String> h) {
        return Map.of("stats", svc.stats(TenantContext.getTenantId()));
    }

    /** 非法状态迁移 → 409 */
    @ExceptionHandler(IllegalStateTransitionException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.CONFLICT)
    public Map<String, String> onIllegal(IllegalStateTransitionException e) {
        return Map.of("error", e.getMessage());
    }

    // —— 视图映射（贴合前端 Consultation TS 接口） ——
    private Map<String, Object> toView(ConsultationDetail d) {
        Consultation c = d.consultation();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("consultationId", c.getConsultationId());
        m.put("patientVisitUid", c.getPatientVisitUid());
        m.put("patientId", c.getPatientId());
        m.put("status", c.getStatus().name());
        m.put("title", c.getTitle());
        m.put("reason", c.getReason());
        m.put("applicant", c.getApplicant());
        m.put("summaryText", c.getSummaryText());
        m.put("tier", c.getTier());
        m.put("createdAt", c.getCreatedAt() == null ? null : c.getCreatedAt().toString());
        m.put("experts", d.experts().stream().map(ex -> {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("expertId", ex.getExpertId());
            e.put("expertName", ex.getExpertName());
            e.put("confirmed", ex.isConfirmed());
            e.put("institution", ex.getInstitution());
            return e;
        }).collect(Collectors.toList()));
        return m;
    }

    private Map<String, Object> toCard(Consultation c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("consultationId", c.getConsultationId());
        m.put("patientVisitUid", c.getPatientVisitUid());
        m.put("patientId", c.getPatientId());
        m.put("status", c.getStatus().name());
        m.put("title", c.getTitle());
        m.put("applicant", c.getApplicant());
        m.put("createdAt", c.getCreatedAt() == null ? null : c.getCreatedAt().toString());
        return m;
    }

    private static String str(Map<String, Object> b, String k) { return str(b, k, ""); }
    private static String str(Map<String, Object> b, String k, String d) {
        Object v = b.get(k); return v == null ? d : String.valueOf(v);
    }
    private static List<String> toStrings(List<?> l) {
        return l.stream().map(String::valueOf).collect(Collectors.toList());
    }
}
