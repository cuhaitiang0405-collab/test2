package com.mdt.integration.adapter;

import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.AcceptedPresentationContext;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.net.service.DicomService;
import org.dcm4che3.net.service.DicomServiceException;

import java.io.IOException;
import java.security.KeyStore;

/**
 * ① 数据接入层 — DICOM 适配器（适配器模式）。
 * 同时扮演：SCP（接收设备 C-STORE 推送）+ SCU（主动 C-FIND / C-MOVE 拉取）。
 * 所有入库数据强制写入统一就诊唯一标识 PatientVisitUID。
 */
public class DicomAdapter {

    private final Device device;
    private final String localAeTitle;
    private final String remoteAeTitle;
    private final String remoteHost;
    private final int remotePort;

    public DicomAdapter(String localAeTitle, String remoteAeTitle,
                        String remoteHost, int remotePort) {
        this.localAeTitle = localAeTitle;
        this.remoteAeTitle = remoteAeTitle;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.device = buildDevice();
    }

    /** 构造 DICOM 设备：含 C-ECHO / C-STORE(SCP) / C-FIND+C-MOVE(SCU) */
    private Device buildDevice() {
        Device d = new Device("mdt-dicom-adapter");
        Connection conn = new Connection("dicom", null, 11112);
        conn.setTlsProtocols(new String[]{"TLSv1.2"});     // DICOM-TLS 强制
        d.addConnection(conn);
        ApplicationEntity ae = new ApplicationEntity(localAeTitle);
        ae.addConnection(conn);
        d.addApplicationEntity(ae);

        // SCP 侧：回应 Echo，并接收 C-STORE 推送
        ae.addRoleSelection(new RoleSelection(null, true, false));
        ae.registerService(new BasicCEchoSCP());
        ae.registerService(new BasicCStoreSCP("*") {
            @Override
            protected void store(Association as, PresentationContext pc,
                                 Attributes cmd, Attributes fmi, InputStream data)
                    throws IOException {
                // 设备推送来的全序列影像：落盘/落对象存储，并触发 OnStudyReceived 回调
                String studyUid = fmi.getString(Tag.StudyInstanceUID);
                String patientVisitUid = resolvePatientVisitUid(fmi);
                onStudyReceived(studyUid, patientVisitUid);
                super.store(as, pc, cmd, fmi, data);
            }
        });
        return d;
    }

    /** 启动 SCP 监听（接收设备推送） */
    public void startScp() throws Exception {
        device.bindConnections();
    }

    /**
     * SCU 拉取：C-FIND 查询 -> C-MOVE 拉取。
     * @param patientId    PatientID（脱敏后保留）
     * @param accessionNo  检查号 AccessionNumber
     * @return 拉取到的实例数
     */
    public int pullStudyViaScu(String patientId, String accessionNo) throws Exception {
        ApplicationEntity ae = device.getApplicationEntity(localAeTitle);
        Connection remote = new Connection("pacs", remoteHost, remotePort);
        Association as = ae.connect(remote, new ApplicationEntity(remoteAeTitle));

        // 1) C-FIND：按 PatientID + AccessionNumber 查询
        Attributes keys = new Attributes();
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
        keys.setString(Tag.PatientID, VR.LO, patientId);
        keys.setString(Tag.AccessionNumber, VR.SH, accessionNo);
        keys.setString(Tag.StudyInstanceUID, VR.UI, "");
        DimseRSP rsp = as.cfind(StudyRootQueryRetrieveInformationModelFind.uid, keys, null, 0);
        String studyUid = null;
        while (rsp.next()) {
            studyUid = rsp.getDataset().getString(Tag.StudyInstanceUID);
        }
        // 2) C-MOVE：把匹配影像拉到本 SCP（localAeTitle 作为 MoveDestination）
        if (studyUid != null) {
            Attributes moveKeys = new Attributes();
            moveKeys.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
            moveKeys.setString(Tag.StudyInstanceUID, VR.UI, studyUid);
            as.cmove(StudyRootQueryRetrieveInformationModelMove.uid,
                     Priority.NORMAL, moveKeys, null, localAeTitle);
        }
        as.release();
        return studyUid == null ? 0 : 1;
    }

    /** 设备推送回调：统一索引入库（PatientVisitUID 关联） */
    private void onStudyReceived(String studyUid, String patientVisitUid) {
        // 调用 IntegrationService.OnStudyReceived（gRPC）落库 + 索引
        System.out.printf("[SCP] study=%s visitUid=%s%n", studyUid, patientVisitUid);
    }

    /** 由 DICOM 元数据解析统一就诊唯一标识（业务规则：本院+PatientID+就诊号） */
    private String resolvePatientVisitUid(Attributes fmi) {
        String pid = fmi.getString(Tag.PatientID);
        String acc = fmi.getString(Tag.AccessionNumber);
        return "PV-" + pid + "-" + acc; // 真实环境应由主数据服务生成
    }

    // ============================ 单元/压测用例建议 ============================
    // 1) SCP：用 dcm4che 模拟器 C-STORE 推送，断言 onStudyReceived 被调用且 PatientVisitUID 非空。
    // 2) SCU：mock PACS，断言 C-FIND 返回 studyUid 后触发 C-MOVE。
    // 3) 异常：对端 AE Title 不匹配时 connect 抛异常，需重试/告警。
    // 4) 超时：C-MOVE 超过阈值（如 30s）应中断并回滚已落文件，避免脏数据。
    // 5) DICOM-TLS：未携带证书的连接必须被拒（握手失败）。
}
