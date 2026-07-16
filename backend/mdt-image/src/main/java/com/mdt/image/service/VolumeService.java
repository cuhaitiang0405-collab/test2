package com.mdt.image.service;

import com.mdt.common.audit.AuditLogger;
import com.mdt.image.domain.ImageVolume;
import com.mdt.common.security.TenantContext;
import com.mdt.image.port.VolumeSource;
import org.springframework.stereotype.Service;

/**
 * 影像核心服务（DDD Service 层）。
 * 不关心体数据来自合成还是真实源，仅经 {@link VolumeSource} 取数并审计；保持源无关，便于生产替换。
 */
@Service
public class VolumeService {

    private final VolumeSource source;
    private final AuditLogger audit;

    public VolumeService(VolumeSource source, AuditLogger audit) {
        this.source = source;
        this.audit = audit;
    }

    public ImageVolume getVolume(String studyUid, String modality) {
        if (studyUid == null || studyUid.isBlank()) {
            throw new IllegalArgumentException("studyUid 必填");
        }
        ImageVolume v = source.load(studyUid, modality);
        audit.log(TenantContext.getTenantId(), TenantContext.getOperatorId(), studyUid, "VOLUME_FETCH",
                "modality=" + v.modality() + " dims=" + v.dims()[0] + "x" + v.dims()[1] + "x" + v.dims()[2]);
        return v;
    }
}
