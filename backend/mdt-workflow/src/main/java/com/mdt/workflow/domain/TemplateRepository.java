package com.mdt.workflow.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<TemplateEntity, Long> {
    List<TemplateEntity> findByTenantIdAndType(String tenantId, String type);
    List<TemplateEntity> findByTenantId(String tenantId);
    @Modifying
    @Transactional
    void deleteByTemplateId(String templateId);
    boolean existsByTemplateId(String templateId);
}
