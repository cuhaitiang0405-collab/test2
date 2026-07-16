package com.mdt.workflow.service;

import com.mdt.common.audit.AuditLogger;
import com.mdt.workflow.domain.*;
import com.mdt.workflow.port.SmsGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 会诊领域服务：编排状态机迁移、全员确认门控、短信异步通知与脱敏审计。
 * 状态机迁移一律经 assertTransition 守卫，非法迁移抛 IllegalStateTransitionException。
 */
@Service
public class ConsultationService {

    private final ConsultationRepository repo;
    private final ConsultationExpertRepository expertRepo;
    private final SmsGateway sms;
    private final AuditLogger audit;

    public ConsultationService(ConsultationRepository repo, ConsultationExpertRepository expertRepo,
                               SmsGateway sms, AuditLogger audit) {
        this.repo = repo; this.expertRepo = expertRepo; this.sms = sms; this.audit = audit;
    }

    /** 允许的状态迁移表（白名单） */
    private static final Map<ConsultationStatus, Set<ConsultationStatus>> ALLOWED = Map.of(
        ConsultationStatus.APPLIED,     Set.of(ConsultationStatus.NOTIFIED, ConsultationStatus.CANCELLED),
        ConsultationStatus.NOTIFIED,    Set.of(ConsultationStatus.CONFIRMED, ConsultationStatus.CANCELLED),
        ConsultationStatus.CONFIRMED,   Set.of(ConsultationStatus.IN_PROGRESS, ConsultationStatus.CANCELLED),
        ConsultationStatus.IN_PROGRESS, Set.of(ConsultationStatus.COMPLETED),
        ConsultationStatus.COMPLETED,   Set.of(),
        ConsultationStatus.CANCELLED,   Set.of()
    );

    private void assertTransition(ConsultationStatus from, ConsultationStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to))
            throw new IllegalStateTransitionException(from, to);
    }

    /** 申请会诊：创建 APPLIED 聚合，并挂接待确认专家（默认未确认） */
    @Transactional
    public Consultation apply(String patientVisitUid, String patientId, String accessionNumber,
                              String applicant, String tenantId, String title, String reason,
                              List<String> expertIds, List<String> expertNames, Integer tier) {
        String id = UUID.randomUUID().toString().replace("-", "");
        validateTierGate(tier, expertIds);
        Consultation c = new Consultation(id, patientVisitUid, patientId, accessionNumber,
                applicant, tenantId, ConsultationStatus.APPLIED, title, reason);
        c.setTier(tier);
        repo.save(c);
        for (int i = 0; i < expertIds.size(); i++) {
            expertRepo.save(new ConsultationExpert(id, expertIds.get(i),
                    i < expertNames.size() ? expertNames.get(i) : expertIds.get(i)));
        }
        audit.log(tenantId, applicant, patientVisitUid, "CONSULT_APPLIED",
                "consultationId=" + id + " experts=" + expertIds.size());
        return c;
    }
    public Consultation notify(String id, String operatorId) {
        Consultation c = require(id);
        assertTransition(c.getStatus(), ConsultationStatus.NOTIFIED);
        c.setStatus(ConsultationStatus.NOTIFIED);
        repo.save(c);
        // 向全部受邀专家投递短信（异步，不阻塞）
        for (ConsultationExpert ex : expertRepo.findByConsultationId(id))
            sms.sendNotification(ex.getExpertName(), c.getTitle());
        audit.log(c.getTenantId(), operatorId, c.getPatientVisitUid(), "CONSULT_NOTIFIED", id);
        return c;
    }

    /** 某专家确认：标记 confirmed；全员确认后 NOTIFIED 自动 → CONFIRMED（全员确认门控） */
    @Transactional
    public Consultation confirm(String id, String expertId, String operatorId) {
        Consultation c = require(id);
        ConsultationExpert ex = expertRepo.findByConsultationIdAndExpertId(id, expertId)
                .orElseThrow(() -> new IllegalArgumentException("专家不存在：" + expertId));
        ex.setConfirmed(true);
        ex.setConfirmedAt(LocalDateTime.now());
        expertRepo.save(ex);

        List<ConsultationExpert> all = expertRepo.findByConsultationId(id);
        boolean everyone = all.stream().allMatch(ConsultationExpert::isConfirmed);
        if (everyone && c.getStatus() == ConsultationStatus.NOTIFIED) {
            c.setStatus(ConsultationStatus.CONFIRMED);   // 全员确认门控放行
            repo.save(c);
            audit.log(c.getTenantId(), operatorId, c.getPatientVisitUid(), "CONSULT_CONFIRMED", id);
        }
        return c;
    }

    /** 开始会诊：CONFIRMED → IN_PROGRESS */
    @Transactional
    public Consultation start(String id, String operatorId) {
        Consultation c = require(id);
        assertTransition(c.getStatus(), ConsultationStatus.IN_PROGRESS);
        c.setStatus(ConsultationStatus.IN_PROGRESS);
        repo.save(c);
        audit.log(c.getTenantId(), operatorId, c.getPatientVisitUid(), "CONSULT_START", id);
        return c;
    }

    /** 总结会诊：IN_PROGRESS → COMPLETED，落库多方结论 */
    @Transactional
    public Consultation complete(String id, String summaryText, String operatorId) {
        Consultation c = require(id);
        assertTransition(c.getStatus(), ConsultationStatus.COMPLETED);
        c.setStatus(ConsultationStatus.COMPLETED);
        c.setSummaryText(summaryText);
        repo.save(c);
        audit.log(c.getTenantId(), operatorId, c.getPatientVisitUid(), "CONSULT_COMPLETED", id);
        return c;
    }

    /** 取消会诊：APPLIED/NOTIFIED/CONFIRMED → CANCELLED（进行中与已总结不可取消） */
    @Transactional
    public Consultation cancel(String id, String operatorId) {
        Consultation c = require(id);
        assertTransition(c.getStatus(), ConsultationStatus.CANCELLED);
        c.setStatus(ConsultationStatus.CANCELLED);
        repo.save(c);
        audit.log(c.getTenantId(), operatorId, c.getPatientVisitUid(), "CONSULT_CANCELLED", id);
        return c;
    }

    public ConsultationDetail get(String id) {
        Consultation c = require(id);
        return new ConsultationDetail(c, expertRepo.findByConsultationId(id));
    }

    public List<Consultation> list(String tenantId, ConsultationStatus status) {
        return status != null ? repo.findByTenantIdAndStatus(tenantId, status) : repo.findByTenantId(tenantId);
    }

    /** 各状态计数（供工作台待办统计） */
    public Map<String, Long> stats(String tenantId) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (ConsultationStatus s : ConsultationStatus.values()) m.put(s.name(), 0L);
        for (Object[] row : repo.countByStatus(tenantId))
            m.put(((ConsultationStatus) row[0]).name(), (Long) row[1]);
        return m;
    }

    private Consultation require(String id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("会诊不存在：" + id));
    }

    /** 聚合视图（会诊 + 专家确认明细） */
    public record ConsultationDetail(Consultation consultation, List<ConsultationExpert> experts) {}

    /** M7 GAP-4 分级门控：tier 1-2 需 ≥2 专家；tier 3 需 ≥3；tier 4 需 ≥4 */
    private void validateTierGate(Integer tier, List<String> expertIds) {
        int t = (tier != null) ? tier : 1;
        int minExperts = switch (t) {
            case 1, 2 -> 2;
            case 3 -> 3;
            case 4 -> 4;
            default -> 2;
        };
        if (expertIds.size() < minExperts)
            throw new IllegalArgumentException(
                String.format("Tier %d 需至少 %d 名专家，当前仅 %d 名", t, minExperts, expertIds.size()));
    }
}
