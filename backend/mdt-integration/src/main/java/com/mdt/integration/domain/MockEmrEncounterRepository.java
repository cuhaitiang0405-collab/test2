package com.mdt.integration.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Mock EMR 就诊仓储 */
@Repository
public interface MockEmrEncounterRepository extends JpaRepository<MockEmrEncounter, String> {
}
