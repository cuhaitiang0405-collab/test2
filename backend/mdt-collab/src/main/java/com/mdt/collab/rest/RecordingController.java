package com.mdt.collab.rest;

import com.mdt.collab.adapter.RecordingRepository;
import com.mdt.collab.domain.RecordingEntity;
import com.mdt.common.audit.AuditLogger;
import com.mdt.common.security.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * M7 · GAP-2 会诊录制服务 REST。
 * 录制媒体(WebM)经前端 MediaRecorder 采集后上传；
 * 本控制器管理录制元数据（起止/时长/状态）。
 */
@RestController
@RequestMapping("/api/collab/recordings")
public class RecordingController {

    private final RecordingRepository repo;
    private final AuditLogger audit;

    public RecordingController(RecordingRepository repo, AuditLogger audit) {
        this.repo = repo;
        this.audit = audit;
    }

    @PostMapping
    public Map<String, Object> start(@RequestBody Map<String, String> body) {
        String cid = body.get("consultationId");
        String rid = "REC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        RecordingEntity e = new RecordingEntity(rid, cid, System.currentTimeMillis());
        repo.save(e);
        audit.log(TenantContext.getTenantId(), TenantContext.getOperatorId(), null,
                "RECORDING_START", "id=" + rid);
        return Map.of("recordingId", rid, "status", "RECORDING");
    }

    @PutMapping("/{recordingId}/stop")
    public Map<String, Object> stop(@PathVariable String recordingId, @RequestBody Map<String, Object> body) {
        RecordingEntity e = repo.findById(recordingId)
                .orElseThrow(() -> new IllegalArgumentException("录制不存在: " + recordingId));
        e.setStatus("COMPLETED");
        e.setStoppedAt(System.currentTimeMillis());
        e.setDuration(body.containsKey("duration") ? ((Number) body.get("duration")).intValue() : 0);
        if (body.containsKey("objectKey")) e.setObjectKey(String.valueOf(body.get("objectKey")));
        repo.save(e);
        audit.log(TenantContext.getTenantId(), TenantContext.getOperatorId(), null, "RECORDING_STOP", "id=" + recordingId + " duration=" + e.getDuration());
        return Map.of("recordingId", recordingId, "status", "COMPLETED", "duration", e.getDuration());
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(defaultValue = "") String consultationId) {
        List<RecordingEntity> list = (consultationId.isBlank())
                ? repo.findAll()
                : repo.findByConsultationIdOrderByStartedAtDesc(consultationId);
        return list.stream().map(this::toMap).toList();
    }

    private Map<String, Object> toMap(RecordingEntity e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("recordingId", e.getRecordingId());
        m.put("consultationId", e.getConsultationId());
        m.put("status", e.getStatus());
        m.put("duration", e.getDuration());
        m.put("objectKey", e.getObjectKey());
        m.put("startedAt", e.getStartedAt());
        m.put("stoppedAt", e.getStoppedAt());
        return m;
    }
}
