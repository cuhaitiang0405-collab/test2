package com.mdt.auth.grpc;

import com.mdt.auth.security.RbacService;
import com.mdt.common.audit.AuditLogger;
import com.mdt.common.trace.GrpcTraceInterceptor;
import com.mdt.common.trace.TraceContext;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * ⑤ 权限服务 gRPC 实现：字段级 RBAC 校验 + 审计写入。
 * 经 GrpcTraceInterceptor 透传 TraceId（gRPC 与 REST 全链路一致）。
 */
@GrpcService(interceptors = GrpcTraceInterceptor.class)
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private final RbacService rbac;
    private final AuditLogger audit;

    public AuthGrpcService(RbacService rbac, AuditLogger audit) {
        this.rbac = rbac;
        this.audit = audit;
    }

    @Override
    public void checkFieldPermission(FieldPermissionRequest req,
                                     StreamObserver<FieldPermissionResponse> obs) {
        boolean allowed = rbac.canReadField(req.getRole(), req.getResource(), req.getField());
        // 关键操作写审计（脱敏后仅统一标识）
        audit.log(req.getTenantId(), req.getRole(), null,
                "CHECK_FIELD_PERMISSION", "resource=" + req.getResource() + " field=" + req.getField());
        obs.onNext(FieldPermissionResponse.newBuilder().setAllowed(allowed).build());
        obs.onCompleted();
    }

    @Override
    public void writeAudit(AuditRecord req, StreamObserver<Ack> obs) {
        audit.log(req.getTenantId(), req.getOperatorId(), req.getPatientVisitUid(),
                req.getAction(), req.getDetail());
        obs.onNext(Ack.newBuilder().setOk(true).setMessage("audited").build());
        obs.onCompleted();
    }
}
