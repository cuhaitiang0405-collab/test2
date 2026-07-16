package com.mdt.collab.adapter;

import com.mdt.collab.domain.RecordingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecordingRepository extends JpaRepository<RecordingEntity, String> {
    List<RecordingEntity> findByConsultationIdOrderByStartedAtDesc(String consultationId);
}
