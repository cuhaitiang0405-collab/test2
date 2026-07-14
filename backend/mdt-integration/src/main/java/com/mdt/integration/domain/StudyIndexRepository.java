package com.mdt.integration.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 检查索引仓储（DDD：XxxRepository） */
@Repository
public interface StudyIndexRepository extends JpaRepository<StudyIndex, String> {
}
