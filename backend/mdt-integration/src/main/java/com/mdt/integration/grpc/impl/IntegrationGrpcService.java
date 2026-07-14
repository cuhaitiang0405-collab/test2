package com.mdt.integration.grpc.impl;

import com.mdt.common.audit.AuditLogger;
import com.mdt.common.trace.GrpcTraceInterceptor;
import com.mdt.integration.adapter.ClinicalDataAdapter;
import com.mdt.integration.dicom.DicomAdapter;
import com.mdt.integration.dicom.IngestCommand;
import com.mdt.integration.grpc.*;
import com.mdt.integration.service.IntegrationService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDate;

/**
 * ① 数据接入层 gRPC 实现：SCU 拉取 / SCP 入库 / 临床数据拉取。
 * 经 GrpcTraceInterceptor 透传 TraceId（与 REST、跨域一致）。
 */
@GrpcService(interceptors = GrpcTraceInterceptor.class)
public class IntegrationGrpcService extends IntegrationServiceGrpc.IntegrationServiceImplBase {

    private final DicomAdapter dicom;
    private final IntegrationService svc;
    private final ClinicalDataAdapter clinical;
    private final AuditLogger audit;

    public IntegrationGrpcService(DicomAdapter dicom, IntegrationService svc,
                                  ClinicalDataAdapter clinical, AuditLogger audit) {
        this.dicom = dicom; this.svc = svc; this.clinical = clinical; this.audit = audit;
    }

    /** SCU 拉取：C-FIND -> C-MOVE -> 落库 */
    @Override
    public void pullStudyFromPacs(PullStudyRequest req, StreamObserver<PullStudyResponse> obs) {
        String tenant = req.getTrace().getTenantId();
        String operator = req.getTrace().getOperatorId();
        IngestCommand cmd = dicom.pullViaScu(req.getPatientId(), req.getAccessionNumber());
        IntegrationService.IngestResult r = svc.ingest(cmd, operator);
        obs.onNext(PullStudyResponse.newBuilder()
                .setSuccess(true).setPatientVisitUid(r.patientVisitUid())
                .setInstanceCount(r.instanceCount())
                .setMessage("SCU 拉取入库成功 modality=" + cmd.modality()).build());
        obs.onCompleted();
    }

    /** SCP 推送落库：携带完整 DICOM 元数据 */
    @Override
    public void ingestStudy(IngestStudyRequest req, StreamObserver<Ack> obs) {
        String operator = req.getTrace().getOperatorId();
        IngestCommand cmd = new IngestCommand(
                req.getStudyInstanceUid(), req.getPatientId(), req.getAccessionNumber(),
                req.getTenantId(), req.getModality(),
                req.getStudyDate().isBlank() ? LocalDate.now() : LocalDate.parse(req.getStudyDate()),
                req.getObjectKey(), req.getInstanceCount(),
                req.getReportContent().isBlank() ? null : req.getReportContent(),
                req.getPathologyConclusion().isBlank() ? null : req.getPathologyConclusion());
        svc.ingest(cmd, operator);
        obs.onNext(Ack.newBuilder().setOk(true).setMessage("ingested").build());
        obs.onCompleted();
    }

    /** SCP 存储到达通知（回调钩子）：仅审计，实际落库由 IngestStudy 完成 */
    @Override
    public void onStudyReceived(StudyReceivedEvent req, StreamObserver<Ack> obs) {
        audit.log(req.getTrace().getTenantId(), req.getTrace().getOperatorId(),
                req.getPatientVisitUid(), "SCP_STORE_RECEIVED", "studyUid=" + req.getStudyInstanceUid());
        obs.onNext(Ack.newBuilder().setOk(true).setMessage("notified").build());
        obs.onCompleted();
    }

    /** 临床数据拉取：HIS/EMR/LIS 脱敏聚合 */
    @Override
    public void fetchClinicalData(ClinicalDataRequest req, StreamObserver<ClinicalDataResponse> obs) {
        String uid = req.getPatientVisitUid();
        audit.log(req.getTrace().getTenantId(), req.getTrace().getOperatorId(),
                uid, "FETCH_CLINICAL", "sources=" + req.getSourceList());
        String emrJson = clinical.fetch(uid);
        obs.onNext(ClinicalDataResponse.newBuilder().setSuccess(true).setEmrJson(emrJson).build());
        obs.onCompleted();
    }
}
