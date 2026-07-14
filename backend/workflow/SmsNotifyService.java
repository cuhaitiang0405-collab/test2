package com.mdt.workflow.service;

import com.mdt.workflow.domain.ConsultationStateMachine;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * ④ 业务流程层 — 会诊应用服务（DDD：XxxService）。
 * 编排状态机 + 短信异步队列（MQ），保证"通知"阶段专家及时收到申请。
 */
public class ConsultationService {

    private final ConsultationRepository repository;
    private final RabbitTemplate smsMq;          // 短信异步队列
    private final AuditLogger audit;

    public ConsultationService(ConsultationRepository repository,
                               RabbitTemplate smsMq, AuditLogger audit) {
        this.repository = repository;
        this.smsMq = smsMq;
        this.audit = audit;
    }

    /** 申请会诊：落库 -> 状态机 APPLIED->NOTIFIED -> 投递短信异步队列 */
    public String apply(String patientVisitUid, java.util.List<String> expertIds, String traceId) {
        ConsultationStateMachine m = new ConsultationStateMachine();
        m.setExpertTotal(expertIds.size());
        m.apply(expertIds.size());               // 进入 NOTIFIED
        String id = repository.save(m, patientVisitUid);

        // 异步通知：发到 MQ，短信网关消费（不阻塞主流程）
        for (String expert : expertIds) {
            smsMq.convertAndSend("mdt.sms.notify",
                new SmsNotifyEvent(id, expert, patientVisitUid, traceId));
        }
        audit.log(traceId, "APPLY_CONSULTATION", patientVisitUid);
        return id;
    }

    /** 专家确认：累计确认，全员到齐后状态机自动 CONFIRMED */
    public void confirm(String consultationId, String expertId, String traceId) {
        ConsultationStateMachine m = repository.load(consultationId);
        m.confirmByExpert();
        repository.save(m, null);
        audit.log(traceId, "CONFIRM", consultationId);
    }

    /** 开始会诊：要求已全员确认（状态机内部守卫） */
    public void start(String consultationId, String traceId) {
        ConsultationStateMachine m = repository.load(consultationId);
        m.transitionTo(ConsultationStateMachine.Status.IN_PROGRESS);
        repository.save(m, null);
        audit.log(traceId, "START", consultationId);
    }

    /** 持久化接口（DDD：XxxRepository） */
    public interface ConsultationRepository {
        String save(ConsultationStateMachine m, String patientVisitUid);
        ConsultationStateMachine load(String id);
    }

    /** 短信通知事件（经 MQ 异步投递） */
    public record SmsNotifyEvent(String consultationId, String expertId,
                                 String patientVisitUid, String traceId) {}
}

/**
 * 统一审计日志门面（含 TraceId，脱敏后仅 PatientVisitUID）。
 * 所有域的关键操作均经此落审计库。
 */
class AuditLogger {
    void log(String traceId, String action, String patientVisitUid) {
        // 异步批量落库；patientVisitUid 已是脱敏后的统一标识
        System.out.printf("[AUDIT] trace=%s action=%s visitUid=%s%n",
                traceId, action, patientVisitUid);
    }
}
