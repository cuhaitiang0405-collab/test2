package com.mdt.workflow.domain;

/** 会诊状态机节点：申请 → 通知 → 确认 → 进行中 → 总结（另含 已取消）。 */
public enum ConsultationStatus {
    APPLIED,      // 已申请（待通知）
    NOTIFIED,     // 已通知专家（待全员确认）
    CONFIRMED,    // 全员确认（待开始）
    IN_PROGRESS,  // 进行中
    COMPLETED,    // 已总结（结论已落库）
    CANCELLED     // 已取消
}
