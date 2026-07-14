package com.mdt.workflow.domain;

import java.util.*;

/**
 * ④ 业务流程层 — 会诊状态机（DDD 领域模型）。
 * 闭环：申请(APPLIED) → 通知(NOTIFIED) → 确认(CONFIRMED)
 *       → 进行中(IN_PROGRESS) → 总结(COMPLETED)，外加 已取消(CANCELLED)。
 * 规则：进入 IN_PROGRESS 前必须全员确认（confirmedCount == expertTotal）。
 */
public class ConsultationStateMachine {

    public enum Status {
        APPLIED, NOTIFIED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
    }

    /** 合法迁移表：源状态 -> 允许的目标状态集合 */
    private static final Map<Status, Set<Status>> TRANSITIONS = new EnumMap<>(Status.class);
    static {
        TRANSITIONS.put(Status.APPLIED,     EnumSet.of(Status.NOTIFIED, Status.CANCELLED));
        TRANSITIONS.put(Status.NOTIFIED,    EnumSet.of(Status.CONFIRMED, Status.CANCELLED));
        TRANSITIONS.put(Status.CONFIRMED,   EnumSet.of(Status.IN_PROGRESS, Status.CANCELLED));
        TRANSITIONS.put(Status.IN_PROGRESS, EnumSet.of(Status.COMPLETED, Status.CANCELLED));
        TRANSITIONS.put(Status.COMPLETED,   EnumSet.noneOf(Status.class));   // 终态
        TRANSITIONS.put(Status.CANCELLED,   EnumSet.noneOf(Status.class));   // 终态
    }

    private Status status = Status.APPLIED;
    private int confirmedCount = 0;
    private int expertTotal = 0;

    public Status getStatus() { return status; }

    /** 受守卫的状态迁移；非法迁移抛异常 */
    public void transitionTo(Status target) {
        Set<Status> allowed = TRANSITIONS.get(status);
        if (allowed == null || !allowed.contains(target)) {
            throw new IllegalStateTransitionException(status, target);
        }
        // 进入进行中前必须全员确认
        if (target == Status.IN_PROGRESS && confirmedCount < expertTotal) {
            throw new IllegalStateException("会诊未获全部专家确认，不能开始");
        }
        this.status = target;
    }

    /** 专家确认（仅 NOTIFIED 状态下累计） */
    public void confirmByExpert() {
        if (status != Status.NOTIFIED) {
            throw new IllegalStateTransitionException(status, Status.CONFIRMED);
        }
        confirmedCount++;
        if (confirmedCount >= expertTotal) {
            transitionTo(Status.CONFIRMED);
        }
    }

    public void setExpertTotal(int total) { this.expertTotal = total; }

    // ----------------------- 应用服务层（XxxService 调用边界） -----------------------
    /** 申请会诊 */
    public void apply(int expertTotal) {
        this.expertTotal = expertTotal;
        transitionTo(Status.NOTIFIED); // 申请后自动进入通知（触发短信异步队列）
    }

    public static class IllegalStateTransitionException extends RuntimeException {
        public IllegalStateTransitionException(Status from, Status to) {
            super("非法状态迁移: " + from + " -> " + to);
        }
    }

    // ============================ 单元/压测用例建议 ============================
    // 1) 合法链：apply -> confirm*N -> IN_PROGRESS -> COMPLETED 全程通过。
    // 2) 全员门控：confirmedCount < expertTotal 时 transitionTo(IN_PROGRESS) 抛异常。
    // 3) 非法迁移：APPLIED 直接 COMPLETED 抛 IllegalStateTransitionException。
    // 4) 取消：任意非终态可 CANCELLED，且 CANCELLED 后不可再迁移。
    // 5) 并发：多专家同时 confirmByExpert，confirmedCount 需原子累加（并发压测无丢失）。
}
