package com.mdt.workflow.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationExpertRepository extends JpaRepository<ConsultationExpert, Long> {

    List<ConsultationExpert> findByConsultationId(String consultationId);

    Optional<ConsultationExpert> findByConsultationIdAndExpertId(String consultationId, String expertId);
}
