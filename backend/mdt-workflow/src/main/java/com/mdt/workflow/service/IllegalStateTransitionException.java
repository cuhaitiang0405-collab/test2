package com.mdt.workflow.service;

import com.mdt.workflow.domain.ConsultationStatus;

/** 状态机非法迁移异常：由 ConsultationStateMachine 守卫抛出，控制器映射为 HTTP 409。 */
public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(ConsultationStatus from, ConsultationStatus to) {
        super("非法的会诊状态迁移：" + from + " → " + to);
    }
}
