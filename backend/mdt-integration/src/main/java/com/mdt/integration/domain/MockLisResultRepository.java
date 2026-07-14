package com.mdt.integration.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Mock LIS 检验仓储 */
@Repository
public interface MockLisResultRepository extends JpaRepository<MockLisResult, String> {
    java.util.List<MockLisResult> findByPatientVisitUid(String patientVisitUid);
}
