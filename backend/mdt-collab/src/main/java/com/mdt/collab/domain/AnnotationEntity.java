package com.mdt.collab.domain;

import jakarta.persistence.*;

/**
 * 协同标注持久化实体（白板/标注增量）。
 * payload 为序列化的标注操作 JSON（见 AnnotationSerializer），支持迟到加入回放与刷新恢复。
 */
@Entity
@Table(name = "collab_annotation")
public class AnnotationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联会诊（房间）ID —— 与 consultationId 一致 */
    @Column(name = "consultation_id", nullable = false)
    private String consultationId;

    /** 乐观锁版本号（同一会诊内自增，保证回放顺序） */
    @Column(nullable = false)
    private int version;

    /** 序列化后的标注操作 JSON */
    @Column(name = "payload", columnDefinition = "text", nullable = false)
    private String payload;

    /** 作者（脱敏后仅用户标识） */
    @Column(name = "author")
    private String author;

    /** 时间戳（毫秒） */
    @Column(name = "ts")
    private long ts;

    public AnnotationEntity() {}

    public AnnotationEntity(String consultationId, int version, String payload, String author, long ts) {
        this.consultationId = consultationId;
        this.version = version;
        this.payload = payload;
        this.author = author;
        this.ts = ts;
    }

    public Long getId() { return id; }
    public String getConsultationId() { return consultationId; }
    public int getVersion() { return version; }
    public String getPayload() { return payload; }
    public String getAuthor() { return author; }
    public long getTs() { return ts; }
}
