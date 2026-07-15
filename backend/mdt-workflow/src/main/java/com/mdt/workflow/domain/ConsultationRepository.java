package com.mdt.workflow.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, String> {

    /** 按租户 + 状态过滤列表（多租户隔离预留：TenantId 必带） */
    List<Consultation> findByTenantIdAndStatus(String tenantId, ConsultationStatus status);

    List<Consultation> findByTenantId(String tenantId);

    /** 各状态计数（供工作台「待办会诊」统计） */
    @Query("""
        SELECT c.status, COUNT(c) FROM Consultation c
        WHERE c.tenantId = :tenant GROUP BY c.status
        """)
    List<Object[]> countByStatus(@Param("tenant") String tenant);
}
