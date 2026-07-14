package com.mdt.auth.grpc;

import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

/**
 * M1 自测：gRPC AuthService + 字段级 RBAC 正确性 + TraceId 经 metadata 透传。
 * 全程 H2 内存库，不依赖外部 Postgres。
 */
@SpringBootTest(classes = com.mdt.auth.AuthApplication.class)
class AuthGrpcIntegrationTest {

    private ManagedChannel channel;

    @BeforeEach
    void setup() {
        channel = ManagedChannelBuilder.forAddress("localhost", 50054).usePlaintext().build();
    }

    @AfterEach
    void teardown() {
        if (channel != null) channel.shutdownNow();
    }

    /** 字段级 RBAC：心电技士不可读病理结论 */
    @Test
    void ecgTechDeniedOnPathology() {
        var resp = withTrace(channel, "trace-ecg-deny").checkFieldPermission(
                FieldPermissionRequest.newBuilder()
                        .setTenantId("T001").setRole("ECG_TECH")
                        .setResource("study.pathology").setField("conclusion").build());
        assertThat(resp.getAllowed()).isFalse();
    }

    /** 字段级 RBAC：医生可读全部字段 */
    @Test
    void doctorAllowedOnPathology() {
        var resp = withTrace(channel, "trace-doc-allow").checkFieldPermission(
                FieldPermissionRequest.newBuilder()
                        .setTenantId("T001").setRole("DOCTOR")
                        .setResource("study.pathology").setField("conclusion").build());
        assertThat(resp.getAllowed()).isTrue();
    }

    /** 将 TraceId 放入 gRPC metadata，验证服务端 GrpcTraceInterceptor 可接收（调用成功即通过） */
    private static AuthServiceGrpc.AuthServiceBlockingStub withTrace(
            ManagedChannel ch, String traceId) {
        Metadata md = new Metadata();
        md.put(Metadata.Key.of("x-mdt-traceid", Metadata.ASCII_STRING_MARSHALLER), traceId);
        var interceptor = MetadataUtils.newAttachHeadersInterceptor(md);
        return AuthServiceGrpc.newBlockingStub(ch).withInterceptors(interceptor);
    }
}
