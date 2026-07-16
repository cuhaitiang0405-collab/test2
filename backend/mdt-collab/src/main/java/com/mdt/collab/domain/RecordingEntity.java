package com.mdt.collab.domain;

import jakarta.persistence.*;

/** M7 · GAP-2 会诊录制元数据 */
@Entity
@Table(name = "collab_recording")
public class RecordingEntity {

    @Id @Column(name = "recording_id", length = 64)
    private String recordingId;

    @Column(name = "consultation_id", length = 64, nullable = false)
    private String consultationId;

    @Column(name = "object_key", length = 512)
    private String objectKey;

    @Column(nullable = false)
    private int duration;

    @Column(length = 16)
    private String status = "RECORDING";

    @Column(name = "started_at")
    private long startedAt;

    @Column(name = "stopped_at")
    private Long stoppedAt;

    public RecordingEntity() {}
    public RecordingEntity(String recordingId, String consultationId, long startedAt) {
        this.recordingId = recordingId; this.consultationId = consultationId; this.startedAt = startedAt;
    }

    public String getRecordingId() { return recordingId; }
    public String getConsultationId() { return consultationId; }
    public String getObjectKey() { return objectKey; }
    public int getDuration() { return duration; }
    public String getStatus() { return status; }
    public long getStartedAt() { return startedAt; }
    public Long getStoppedAt() { return stoppedAt; }
    public void setObjectKey(String v) { this.objectKey = v; }
    public void setDuration(int v) { this.duration = v; }
    public void setStatus(String v) { this.status = v; }
    public void setStoppedAt(Long v) { this.stoppedAt = v; }
}
