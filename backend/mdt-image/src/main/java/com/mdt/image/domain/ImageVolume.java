package com.mdt.image.domain;

/**
 * 影像体数据值对象（DDD 领域对象）。
 * 体素以 Int16 little-endian 原始字节承载，前端上传 WebGL2 3D 纹理后做 MPR / 窗宽窗位。
 */
public record ImageVolume(
        int[] dims,        // [x, y, z] 体素维度
        double[] spacing,  // [sx, sy, sz] 层厚/间距（mm）
        String modality,   // CT / MRI
        byte[] voxels,     // Int16 LE，长度 = x*y*z*2
        int recommendedWw, // 推荐窗宽
        int recommendedWl  // 推荐窗位
) {
}
