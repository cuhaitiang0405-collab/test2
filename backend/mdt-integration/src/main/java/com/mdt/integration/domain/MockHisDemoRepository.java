package com.mdt.integration.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Mock HIS 人口学仓储 */
@Repository
public interface MockHisDemoRepository extends JpaRepository<MockHisDemo, String> {
}
