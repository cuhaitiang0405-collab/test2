package com.mdt.collab.adapter;

import com.mdt.collab.domain.AnnotationEntity;
import com.mdt.collab.grpc.*;
import com.mdt.common.audit.AuditLogger;
import com.mdt.common.trace.GrpcTraceInterceptor;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ③ 协同通讯层 gRPC 实现（与 mdt.proto CollabService 对齐）。
 * JoinRoom 返回 WS 信令地址（生产替换为 SFU 媒体地址）；PushAnnotation 落库；ReportQuality 上报。
 * 经 GrpcTraceInterceptor 透传 TraceId，与 REST/WS 全链路一致。
 */
@GrpcService(interceptors = GrpcTraceInterceptor.class)
public class CollabGrpcService extends CollabServiceGrpc.CollabServiceImplBase {

    private final AnnotationRepository annotations;
    private final AuditLogger audit;

    public CollabGrpcService(AnnotationRepository annotations, AuditLogger audit) {
        this.annotations = annotations;
        this.audit = audit;
    }

    // 研发态：质量上报仅内存累计（生产入时序库）
    private final ConcurrentHashMap<String, QualityReport> lastQuality = new ConcurrentHashMap<>();

    @Override
    public void joinRoom(JoinRoomRequest req, StreamObserver<JoinRoomResponse> obs) {
        String token = "ROOM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        // gRPC 不直接携带 PatientVisitUID，审计留空（PVUID 经 REST/WS 入房时记录）
        audit.log(req.getTrace().getTenantId().isEmpty() ? "T001" : req.getTrace().getTenantId(),
                req.getTrace().getOperatorId(), null, "COLLAB_JOIN",
                "room=" + req.getConsultationId() + " token=" + token);
        obs.onNext(JoinRoomResponse.newBuilder()
                .setSfuEndpoint("/ws/collab")   // 生产替换为真实 SFU 媒体地址
                .setRoomToken(token)
                .build());
        obs.onCompleted();
    }

    @Override
    public void pushAnnotation(AnnotationMessage req, StreamObserver<Ack> obs) {
        String payload = new String(req.getPayload().toByteArray(), StandardCharsets.UTF_8);
        int version = annotations.countByConsultationId(req.getConsultationId()) + 1;
        String author = req.getTrace().getOperatorId();
        annotations.save(new AnnotationEntity(req.getConsultationId(), version, payload, author, System.currentTimeMillis()));
        audit.log(req.getTrace().getTenantId().isEmpty() ? "T001" : req.getTrace().getTenantId(),
                author, null, "COLLAB_ANNOTATE",
                "room=" + req.getConsultationId() + " version=" + version);
        obs.onNext(Ack.newBuilder().setOk(true).setMessage("v" + version).build());
        obs.onCompleted();
    }

    @Override
    public void reportQuality(QualityReport req, StreamObserver<Ack> obs) {
        lastQuality.put(req.getConsultationId(), req);
        obs.onNext(Ack.newBuilder().setOk(true).setMessage("recorded").build());
        obs.onCompleted();
    }
}
