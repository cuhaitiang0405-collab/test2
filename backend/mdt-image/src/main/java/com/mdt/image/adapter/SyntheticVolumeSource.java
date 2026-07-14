package com.mdt.image.adapter;

import com.mdt.image.domain.ImageVolume;
import com.mdt.image.port.VolumeSource;
import org.springframework.stereotype.Component;

/**
 * 程序化合成体数据适配器（研发态）。
 * 同一 studyUid 确定性生成同一段体数据（xorshift 种子由 studyUid 哈希），
 * 使"任意 study 都能稳定出图、且刷新不变"，便于联调与演示。
 * 形态：椭球体表 + 骨壳 + 偏心占位灶 + 同心壳纹理 + 轻噪声；CT 用 HU 量程，MRI 重映射亮度。
 */
@Component
public class SyntheticVolumeSource implements VolumeSource {

    private static final int[] DEFAULT_DIMS = {128, 128, 96};
    private static final double[] DEFAULT_SPACING = {0.7, 0.7, 1.5};

    @Override
    public ImageVolume load(String studyUid, String modality) {
        int[] dims = DEFAULT_DIMS;
        double[] spacing = DEFAULT_SPACING;
        String mod = (modality == null || modality.isBlank()) ? "CT" : modality.toUpperCase();
        boolean mri = "MRI".equals(mod);

        int xN = dims[0], yN = dims[1], zN = dims[2];
        byte[] voxels = new byte[xN * yN * zN * 2];

        long seed = hashSeed(studyUid + "|" + mod);
        long s = seed;

        double cx = xN / 2.0, cy = yN / 2.0, cz = zN / 2.0;
        double rx = xN * 0.42, ry = yN * 0.42, rz = zN * 0.40;
        double lx = cx + rx * 0.25, ly = cy - ry * 0.20, lz = cz;
        double lr = Math.min(xN, yN) * 0.12;

        int idx = 0;
        for (int z = 0; z < zN; z++) {
            double nz = (z - cz) / rz;
            for (int y = 0; y < yN; y++) {
                double ny = (y - cy) / ry;
                for (int x = 0; x < xN; x++) {
                    double nx = (x - cx) / rx;
                    double r = Math.sqrt(nx * nx + ny * ny + nz * nz);

                    short val;
                    if (r > 1.0) {
                        val = mri ? 0 : (short) -1000;            // 体外：CT 空气 / MRI 黑
                    } else {
                        int base = 30 + (int) (20 * (1 - r));      // 软组织基底 + 径向渐变
                        if (r > 0.86 && r < 0.95) base = 900;     // 骨壳
                        double dl = Math.sqrt(Math.pow((x - lx) / lr, 2)
                                + Math.pow((y - ly) / lr, 2) + Math.pow((z - lz) / lr, 2));
                        if (dl < 1.0) base = 130;                 // 偏心占位灶
                        base += (int) (Math.abs(Math.sin(r * 18.0)) * 15); // 同心壳纹理
                        s = xorshift(s);
                        base += (int) (((s & 0xff) / 255.0 - 0.5) * 30); // 轻噪声
                        if (mri) {
                            base = (int) (200 + 600 * (1 - r) + (base % 50));   // MRI 类亮度重映射
                        }
                        val = (short) clamp(base, -1024, 3071);
                    }
                    voxels[idx * 2] = (byte) (val & 0xff);
                    voxels[idx * 2 + 1] = (byte) ((val >> 8) & 0xff);
                    idx++;
                }
            }
        }

        int ww = mri ? 800 : 400;
        int wl = mri ? 400 : 40;
        return new ImageVolume(dims, spacing, mod, voxels, ww, wl);
    }

    /** studyUid 字符串 -> 64 位种子（FNV-1a 变体） */
    private long hashSeed(String key) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < key.length(); i++) {
            h ^= key.charAt(i);
            h *= 0x100000001b3L;
        }
        return h == 0 ? 0x9e3779b97f4a7c15L : h;
    }

    /** xorshift64 伪随机，保证同种子同序列 */
    private long xorshift(long x) {
        x ^= (x << 13);
        x ^= (x >>> 7);
        x ^= (x << 17);
        return x;
    }

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
