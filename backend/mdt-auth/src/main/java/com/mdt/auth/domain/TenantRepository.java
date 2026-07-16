package com.mdt.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, String> {
    List<TenantEntity> findByStatus(String status);
}
