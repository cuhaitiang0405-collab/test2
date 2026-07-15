package com.mdt.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * ④ 业务流程层（M4 落地）：会诊状态机闭环。
 * 复用 mdt-common 的 RestTraceFilter（TraceId 透传）与 AuditLogger（脱敏审计），
 * 接线方式与 ② 影像核心引擎（mdt-image）完全一致：REST → DDD Service → JPA → 审计。
 * 本里程碑不引入内部 gRPC（gRPC 服务暴露留待 M6 多租户硬化阶段），对外经 API 网关 REST。
 */
@SpringBootApplication(scanBasePackages = "com.mdt")
@EntityScan({"com.mdt.workflow", "com.mdt.common"})
@EnableJpaRepositories(basePackages = {"com.mdt.workflow", "com.mdt.common"})
public class WorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }
}
