package com.mdt.collab.adapter;

import com.mdt.collab.domain.AnnotationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 标注持久化仓库 */
@Repository
public interface AnnotationRepository extends JpaRepository<AnnotationEntity, Long> {
    /** 按会诊回放（按版本升序） */
    List<AnnotationEntity> findByConsultationIdOrderByVersionAsc(String consultationId);
    /** 当前最大版本号（用于乐观锁自增） */
    int countByConsultationId(String consultationId);
}
