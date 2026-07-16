package com.mdt.collab.rest;

import com.mdt.collab.adapter.AnnotationRepository;
import com.mdt.common.security.TenantContext;
import com.mdt.collab.adapter.AnnotationSerializer;
import com.mdt.collab.domain.AnnotationEntity;
import com.mdt.common.audit.AuditLogger;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ③ 协同通讯层 REST 入口（网关路由 /api/collab/**）。
 * - 建/入房：返回 WS 信令地址（sfuEndpoint 字段保留生产 SFU 切换点）
 * - 标注回放：迟到加入 / 刷新恢复
 * - 标注落库：经 AnnotationSerializer 持久化（与 WS draw 同源）
 */
@RestController
@RequestMapping("/api/collab")
public class CollabController {

    private final AnnotationRepository annotations;
    private final AuditLogger audit;

    public CollabController(AnnotationRepository annotations, AuditLogger audit) {
        this.annotations = annotations;
        this.audit = audit;
    }

    /** 建/入房：返回信令地址与房间令牌 */
    @PostMapping("/rooms")
    public Map<String, Object> createRoom(@RequestBody Map<String, String> body) {
        String consultationId = body.getOrDefault("consultationId", "default");
        String user = body.getOrDefault("user", "anon");
        String tenant = TenantContext.getTenantId();
        String pvuid = body.getOrDefault("patientVisitUid", "");
        String roomToken = "ROOM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        audit.log(tenant, user, pvuid.isEmpty() ? null : pvuid, "COLLAB_JOIN", "room=" + consultationId + " token=" + roomToken);
        // sfu_endpoint 研发态指向 WS 信令地址；生产替换为真实 SFU 媒体地址（前端零改）
        return Map.of(
                "consultationId", consultationId,
                "roomToken", roomToken,
                "sfuEndpoint", "/ws/collab",
                "wsPath", "/ws/collab"
        );
    }

    /** 标注回放（迟到加入/刷新恢复）：按版本升序 */
    @GetMapping("/rooms/{consultationId}/annotations")
    public List<Map<String, Object>> listAnnotations(@PathVariable String consultationId) {
        return annotations.findByConsultationIdOrderByVersionAsc(consultationId).stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("version", e.getVersion());
            m.put("author", e.getAuthor());
            m.put("ts", e.getTs());
            m.put("payload", e.getPayload());
            return m;
        }).toList();
    }

    /** 标注落库（与 WS draw 同源；返回服务端回填的版本号） */
    @PostMapping("/rooms/{consultationId}/annotations")
    public Map<String, Object> pushAnnotation(@PathVariable String consultationId,
                                              @RequestBody Map<String, Object> body) {
        String author = (String) body.getOrDefault("author", "anon");
        Object op = body.get("op");
        int version = annotations.countByConsultationId(consultationId) + 1;
        String payload = (op instanceof String) ? (String) op : AnnotationSerializer.serialize(
                new AnnotationSerializer.AnnotationOp(null, null, null, null, author, System.currentTimeMillis(), version));
        if (!(op instanceof String)) {
            // op 是对象：用 Jackson 序列化为字符串
            try { payload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(op); } catch (Exception ignored) {}
        }
        AnnotationEntity saved = annotations.save(new AnnotationEntity(consultationId, version, payload, author, System.currentTimeMillis()));
        audit.log(String.valueOf(TenantContext.getTenantId()), author,
                (String) body.get("patientVisitUid"), "COLLAB_ANNOTATE",
                "room=" + consultationId + " version=" + version);
        return Map.of("success", true, "version", saved.getVersion(), "author", author);
    }
}
