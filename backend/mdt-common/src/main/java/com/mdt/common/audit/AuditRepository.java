package com.mdt.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 审计日志仓储（DDD：XxxRepository） */
@Repository
public interface AuditRepository extends JpaRepository<AuditLog, String> {
}
