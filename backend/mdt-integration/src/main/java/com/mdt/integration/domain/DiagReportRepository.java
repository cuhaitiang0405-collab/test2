package com.mdt.integration.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 诊断报告仓储（DDD：XxxRepository） */
@Repository
public interface DiagReportRepository extends JpaRepository<DiagReport, String> {
}
