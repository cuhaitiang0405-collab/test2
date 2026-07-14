package com.mdt.integration.grpc;

import com.mdt.common.jpa.PatientVisit;
import com.mdt.common.jpa.PatientVisitRepository;
import com.mdt.integration.domain.StudyIndex;
import com.mdt.integration.domain.StudyIndexRepository;
import com.mdt.integration.query.ClinicalViewRepository;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

/**
 * M2 自测：gRPC 集成服务正确性 + TraceId 透传 + 统一索引落库 + 临床脱敏。
 * H2 内存库，不依赖外部 Postgres。
 */
@SpringBootTest(classes = com.mdt.integration.IntegrationApplication.class)
class IntegrationGrpcTest {

    private ManagedChannel channel;

    @Autowired StudyIndexRepository studyRepo;
    @Autowired PatientVisitRepository pvRepo;
    @Autowired ClinicalViewRepository viewRepo;

    @BeforeEach
    void setup() {
        channel = ManagedChannelBuilder.forAddress("localhost", 50055).usePlaintext().build();
    }

    @AfterEach
    void teardown() {
        if (channel != null) channel.shutdownNow();
    }

    private IntegrationServiceGrpc.IntegrationServiceBlockingStub stub(String traceId) {
        Metadata md = new Metadata();
        md.put(Metadata.Key.of("x-mdt-traceid", Metadata.ASCII_STRING_MARSHALLER), traceId);
        return IntegrationServiceGrpc.newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(md));
    }

    /** SCU 拉取：落 STUDY_INDEX 且幂等 upsert PATIENT_VISIT（统一索引 PV-T001-P1001-A202407001） */
    @Test
    void scuPullPersistsStudyAndIndex() {
        var resp = stub("trace-scu-1").pullStudyFromPacs(PullStudyRequest.newBuilder()
                .setTrace(Trace.newBuilder().setTenantId("T001").setOperatorId("WEB"))
                .setPatientId("P1001").setAccessionNumber("A202407001").build());

        assertThat(resp.getSuccess()).isTrue();
        String uid = "PV-T001-P1001-A202407001";
        assertThat(resp.getPatientVisitUid()).isEqualTo(uid);
        assertThat(pvRepo.existsById(uid)).isTrue();                 // 主索引已建
        assertThat(studyRepo.findAll()).anyMatch(
                s -> s.getPatientVisitUid().equals(uid) && "CT".equals(s.getModality()));
    }

    /** 临床数据拉取：脱敏（含 PatientID，不泄露姓名/身份证） */
    @Test
    void fetchClinicalIsDesensitized() {
        var resp = stub("trace-clin-1").fetchClinicalData(ClinicalDataRequest.newBuilder()
                .setTrace(Trace.newBuilder().setTenantId("T001").setOperatorId("WEB"))
                .setPatientVisitUid("PV-T001-P1001-A202407001")
                .addSource("HIS").addSource("EMR").addSource("LIS").build());

        assertThat(resp.getSuccess()).isTrue();
        String json = resp.getEmrJson();
        assertThat(json).contains("P1001");                          // 保留 PatientID
        assertThat(json).contains("diagnosis");                      // 临床字段可读
        assertThat(json).doesNotContain("姓名");                     // 无姓名
        assertThat(json).doesNotContain("身份证");                   // 无身份证
    }

    /** 统一视图可查：种子患者 P1001 在 V_PATIENT_STUDIES 中可见 */
    @Test
    void patientStudyViewQueryable() {
        var rows = viewRepo.listPatientStudies("T001");
        assertThat(rows).isNotEmpty();
        assertThat(rows).anyMatch(r -> "PV-T001-P1001-A202407001".equals(r.patientVisitUid()));
    }
}
