package com.mdt.common.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 患者就诊仓储（DDD：XxxRepository） */
@Repository
public interface PatientVisitRepository extends JpaRepository<PatientVisit, String> {
}
