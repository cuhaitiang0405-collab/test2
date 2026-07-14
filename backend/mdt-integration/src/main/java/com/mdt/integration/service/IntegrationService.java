package com.mdt.integration.service;

import com.mdt.common.audit.AuditLogger;
import com.mdt.common.jpa.PatientVisit;
import com.mdt.common.jpa.PatientVisitRepository;
import com.mdt.integration.domain.DiagReport;
import com.mdt.integration.domain.DiagReportRepository;
import com.mdt.integration.domain.StudyIndex;
import com.mdt.integration.domain.StudyIndexRepository;
import com.mdt.integration.dicom.IngestCommand;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 集成应用服务（DDD：XxxService）：编排「适配器产出 → 统一索引解析 → 落库 → 审计」。
 * 统一就诊标识 PatientVisitUID 由 (tenant, patientId, accession) 确定性组合，
 * PATIENT_VISIT 幂等 upsert（首次到院自动建主索引），STUDY_INDEX 引用之。
 */
@Service
public class IntegrationService {

    private final PatientVisitRepository pvRepo;
    private final StudyIndexRepository studyRepo;
    private final DiagReportRepository reportRepo;
    private final AuditLogger audit;

    public IntegrationService(PatientVisitRepository pvRepo, StudyIndexRepository studyRepo,
                              DiagReportRepository reportRepo, AuditLogger audit) {
        this.pvRepo = pvRepo; this.studyRepo = studyRepo; this.reportRepo = reportRepo; this.audit = audit;
    }

    /** 执行一次入库（SCU 拉取或 SCP 推送结果），返回统一就诊标识与实例数 */
    public IngestResult ingest(IngestCommand cmd, String operatorId) {
        String uid = resolveUid(cmd.tenantId(), cmd.patientId(), cmd.accessionNumber());

        // 1) 幂等 upsert PATIENT_VISIT（首次到院自动建主索引）
        if (!pvRepo.existsById(uid)) {
            pvRepo.save(new PatientVisit(uid, cmd.tenantId(), cmd.patientId(),
                    cmd.accessionNumber(), "OUTPATIENT"));
        }
        // 2) 写 STUDY_INDEX（有则更新）
        studyRepo.save(new StudyIndex(cmd.studyInstanceUid(), uid, cmd.tenantId(), cmd.modality(),
                cmd.studyDate(), cmd.objectKey(), cmd.instanceCount()));
        // 3) 写 DIAG_REPORT（存在报告/病理结论时）
        if (cmd.reportContent() != null || cmd.pathologyConclusion() != null) {
            reportRepo.save(new DiagReport("R-" + cmd.studyInstanceUid(), uid, cmd.tenantId(),
                    cmd.modality(), cmd.reportContent(), cmd.pathologyConclusion(), LocalDateTime.now()));
        }
        // 4) 审计（脱敏：仅统一标识）
        audit.log(cmd.tenantId(), operatorId, uid, "INGEST_STUDY",
                "modality=" + cmd.modality() + " studyUid=" + cmd.studyInstanceUid());
        return new IngestResult(uid, cmd.instanceCount());
    }

    /** 解析统一就诊唯一标识：PV-{tenant}-{patientId}-{accession}（生产替换为真实主数据服务） */
    public String resolveUid(String tenantId, String patientId, String accessionNumber) {
        return "PV-" + tenantId + "-" + patientId + "-" + accessionNumber;
    }

    public record IngestResult(String patientVisitUid, int instanceCount) {}
}
