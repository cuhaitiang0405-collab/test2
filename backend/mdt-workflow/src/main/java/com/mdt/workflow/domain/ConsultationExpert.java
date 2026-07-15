package com.mdt.workflow.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 会诊专家关联（CONSULTATION_EXPERT 表）：承载「全员确认门控」。
 * 所有专家 confirmed=true 后，NOTIFIED 才允许迁移至 CONFIRMED。
 */
@Entity
@Table(name = "CONSULTATION_EXPERT", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"CONSULTATION_ID", "EXPERT_ID"})
})
public class ConsultationExpert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CONSULTATION_ID", length = 40, nullable = false)
    private String consultationId;

    @Column(name = "EXPERT_ID", length = 64, nullable = false)
    private String expertId;

    @Column(name = "EXPERT_NAME", length = 64, nullable = false)
    private String expertName;

    @Column(name = "CONFIRMED", nullable = false)
    private boolean confirmed;

    @Column(name = "CONFIRMED_AT")
    private LocalDateTime confirmedAt;

    public ConsultationExpert() {}

    public ConsultationExpert(String consultationId, String expertId, String expertName) {
        this.consultationId = consultationId;
        this.expertId = expertId;
        this.expertName = expertName;
        this.confirmed = false;
    }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getConsultationId() { return consultationId; }
    public void setConsultationId(String v) { this.consultationId = v; }
    public String getExpertId() { return expertId; }
    public void setExpertId(String v) { this.expertId = v; }
    public String getExpertName() { return expertName; }
    public void setExpertName(String v) { this.expertName = v; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean v) { this.confirmed = v; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime v) { this.confirmedAt = v; }
}
