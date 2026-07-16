package com.mdt.integration.rest;

import com.mdt.common.audit.AuditLogger;
import com.mdt.common.security.TenantContext;
import com.mdt.common.trace.ClientTraceInterceptor;
import com.mdt.integration.dicom.DicomAdapter;
import com.mdt.integration.dicom.DicomSimulator;
import com.mdt.integration.grpc.*;
import com.mdt.integration.query.ClinicalViewRepository;
import com.mdt.integration.query.PatientStudyRow;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据接入层 REST 入口（网关路由 /api/integration/**）。
 * 经 @GrpcClient 调本域 gRPC 服务（带 ClientTraceInterceptor 透传 TraceId），
 * 保证「网关 REST → 域内 gRPC」链路闭环 + 审计可追溯。
 */
@RestController
@RequestMapping("/api/integration")
public class IntegrationController {

    @GrpcClient(value = "integration", interceptors = ClientTraceInterceptor.class)
    private IntegrationServiceGrpc.IntegrationServiceBlockingStub integrationStub;

    private final DicomAdapter dicom;
    private final DicomSimulator simulator;
    private final ClinicalViewRepository viewRepo;
    private final AuditLogger audit;

    // GAP-7 跨机构影像发布：研发期用内存登记（生产替换为区域影像平台 MQ/接口）
    private final Map<String, Object> published = new ConcurrentHashMap<>();

    public IntegrationController(DicomAdapter dicom, DicomSimulator simulator,
                                 ClinicalViewRepository viewRepo, AuditLogger audit) {
        this.dicom = dicom; this.simulator = simulator; this.viewRepo = viewRepo; this.audit = audit;
    }

    private static Trace trace() {
        return Trace.newBuilder().setTenantId(TenantContext.getTenantId()).setOperatorId(TenantContext.getOperatorId()).build();
    }

    /** 模拟一次接入：mode=scu（SCU 拉取）/ scp（SCP 推送），均落 STUDY_INDEX */
    @PostMapping("/simulate")
    public Map<String, Object> simulate(@RequestParam(defaultValue = "scu") String mode) {
        if ("scp".equalsIgnoreCase(mode)) {
            // SCP 推送：模拟设备 C-STORE 推送一份随机模态检查（落到已登记患者 P1002）
            var ds = simulator.generate(simulator.randomModality(), "P1002", "A202407002", "T001");
            var cmd = dicom.receiveViaScp(ds);
            integrationStub.ingestStudy(IngestStudyRequest.newBuilder()
                    .setTrace(trace())
                    .setStudyInstanceUid(cmd.studyInstanceUid()).setPatientId(cmd.patientId())
                    .setAccessionNumber(cmd.accessionNumber()).setTenantId(cmd.tenantId())
                    .setModality(cmd.modality()).setStudyDate(cmd.studyDate().toString())
                    .setObjectKey(cmd.objectKey()).setInstanceCount(cmd.instanceCount())
                    .setReportContent(cmd.reportContent() == null ? "" : cmd.reportContent())
                    .setPathologyConclusion(cmd.pathologyConclusion() == null ? "" : cmd.pathologyConclusion())
                    .build());
            return Map.of("mode", "scp", "success", true, "patientVisitUid",
                    "PV-T001-P1002-A202407002", "modality", cmd.modality());
        }
        if ("scu".equalsIgnoreCase(mode)) {
            // SCU 拉取：从模拟 PACS 拉取患者 P1001 的检查
            var resp = integrationStub.pullStudyFromPacs(PullStudyRequest.newBuilder()
                    .setTrace(trace()).setPatientId("P1001").setAccessionNumber("A202407001").build());
            return Map.of("mode", "scu", "success", resp.getSuccess(),
                    "patientVisitUid", resp.getPatientVisitUid(), "instanceCount", resp.getInstanceCount(),
                    "message", resp.getMessage());
        }
        throw new IllegalArgumentException("mode 仅支持 scu 或 scp");
    }

    /** 拉取某患者的脱敏临床数据（HIS/EMR/LIS） */
    @PostMapping("/clinical")
    public Map<String, Object> clinical(@RequestBody Map<String, String> body) {
        String uid = body.get("patientVisitUid");
        if (uid == null || uid.isBlank()) throw new IllegalArgumentException("patientVisitUid 必填");
        var resp = integrationStub.fetchClinicalData(ClinicalDataRequest.newBuilder()
                .setTrace(trace()).setPatientVisitUid(uid).addSource("HIS").addSource("EMR").addSource("LIS").build());
        return Map.of("success", resp.getSuccess(), "emrJson", resp.getEmrJson());
    }

    /** 数据接入看板：患者全量影像+报告统一视图（脱敏） */
    @GetMapping("/studies")
    public List<Map<String, Object>> studies() {
        return viewRepo.listPatientStudies(TenantContext.getTenantId()).stream().map(this::toMap).toList();
    }

    private Map<String, Object> toMap(PatientStudyRow r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("patientVisitUid", r.patientVisitUid());
        m.put("patientId", r.patientId());
        m.put("accessionNumber", r.accessionNumber());
        m.put("studyInstanceUid", r.studyInstanceUid());
        m.put("modality", r.modality());
        m.put("studyDate", r.studyDate() == null ? null : r.studyDate().toString());
        m.put("reportId", r.reportId());
        m.put("hasPathology", r.pathologyConclusion() != null);
        return m;
    }

    /**
     * GAP-7 跨机构影像发布：将检查发布到区域影像平台（研发期内存登记 + 审计）。
     * 生产期替换为对区域平台的鉴权发布调用（同一方法签名，零侵入）。
     */
    @PostMapping("/publish")
    public Map<String, Object> publish(@RequestBody Map<String, String> body) {
        String studyUid = body.get("studyInstanceUid");
        String patientId = body.getOrDefault("patientId", "");
        String accession = body.getOrDefault("accessionNumber", "");
        String tenantId = TenantContext.getTenantId();
        if (studyUid == null || studyUid.isBlank()) throw new IllegalArgumentException("studyInstanceUid 必填");

        String publishId = "PUB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("publishId", publishId);
        rec.put("studyInstanceUid", studyUid);
        rec.put("patientId", patientId);
        rec.put("accessionNumber", accession);
        rec.put("tenantId", tenantId);
        rec.put("target", "REGIONAL-IMAGE-HUB");
        rec.put("publishedAt", java.time.Instant.now().toString());
        published.put(publishId, rec);

        audit.log(tenantId, "WEB", patientId, "IMAGE_PUBLISH",
                "publishId=" + publishId + " studyUid=" + studyUid);
        return Map.of("success", true, "publishId", publishId, "target", "REGIONAL-IMAGE-HUB");
    }
}
