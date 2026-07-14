package com.mdt.image.rest;

import com.mdt.common.trace.TraceContext;
import com.mdt.image.domain.ImageVolume;
import com.mdt.image.service.VolumeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ② 影像核心引擎 — REST 出站口。
 * <p>
 * {@code GET /api/image/volume?studyUid=&modality=}
 * <p>
 * 返回 Int16 LE 体素流（~3MB），元数据置于响应头，前端 WebGL2 直接消费。
 * 研发期经网关 /api/image/** 路由到此，不暴露内部端口。
 * <p>
 * 【设计方案说明 200字】
 * 自研轻量 WebGL2 体渲染，不依赖 Cornerstone3D，保留 VolumeSource 缝便于生产切换。
 * 体数据由 SyntheticVolumeSource 确定性合成，同 studyUid 刷新不变。
 * 体素以原始 Int16 LE 字节流下发，前端在 GPU 端完成 MPR 重建 + 窗宽窗位 + 缩放平移。
 * 元数据（维度/层厚/推荐窗值）通过响应头传递，避免前端额外查询。
 * 核心指标：单次体数据约 3MB，带宽可控；WebGL2 TEXTURE_3D 直接上传显存，主线程无阻塞。
 * </p>
 */
@RestController
@RequestMapping("/api/image")
public class VolumeController {

    private final VolumeService volumeService;

    public VolumeController(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    /**
     * 获取体数据流。
     *
     * @param studyUid 统一检查标识（必填）
     * @param modality 模态（可选，默认 CT；CT/MRI 两类体数据）
     * @return 200 + Int16 LE voxel 流 + 元数据响应头；400 参数非法；500 内部异常
     */
    @GetMapping("/volume")
    public ResponseEntity<byte[]> getVolume(
            @RequestParam String studyUid,
            @RequestParam(defaultValue = "CT") String modality) {

        // 委托领域服务：校验 + 取数 + 审计
        ImageVolume vol = volumeService.getVolume(studyUid, modality);

        // 元数据 → 响应头（前端 WebGL2 初始化纹理需要）
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vol-Dims",
                vol.dims()[0] + "," + vol.dims()[1] + "," + vol.dims()[2]);
        headers.set("X-Vol-Spacing",
                vol.spacing()[0] + "," + vol.spacing()[1] + "," + vol.spacing()[2]);
        headers.set("X-Vol-Modality", vol.modality());
        headers.set("X-Vol-Ww", String.valueOf(vol.recommendedWw()));
        headers.set("X-Vol-Wl", String.valueOf(vol.recommendedWl()));

        // 透传 TraceId 便于前端/工具链串联
        String traceId = TraceContext.get();
        if (traceId != null) {
            headers.set(TraceContext.HEADER, traceId);
        }

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(vol.voxels().length)
                .body(vol.voxels());
    }
}
