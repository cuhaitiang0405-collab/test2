package com.mdt.integration.dicom;

import java.util.Random;

/**
 * M7 · GAP-3 DSA（数字减影血管造影）合成序列生成器。
 * 生成 512×512×32 帧的 Int16 体积：模拟血管树 + 造影剂 bolus 时序。
 * 种子数据：P1003 的 DSA 检查。
 */
public class DsaSimulator {

    private final Random rng = new Random(42);
    private final int size = 512;
    private final int frames = 32;

    /** 生成 DSA 体积(Int16 LE bytes)，每一帧 = 512×512×2 bytes */
    public byte[] generate(int width, int height, int frameCount) {
        int w = width > 0 ? width : size;
        int h = height > 0 ? height : size;
        int fc = frameCount > 0 ? frameCount : frames;
        byte[] data = new byte[w * h * fc * 2];

        // 背景：常量灰度 (~100)
        // 血管树：几条从中心辐射的曲线
        // 造影剂 bolus：中间帧最亮（模拟碘剂充盈）
        for (int f = 0; f < fc; f++) {
            double bolus = 1.0 - Math.abs((f - fc / 2.0) / (fc / 2.0)); // 0→1→0
            double contrastFactor = 1.0 + bolus * 8.0; // ×1～×9
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    double nx = (x - w / 2.0) / (w / 2.0);
                    double ny = (y - h / 2.0) / (h / 2.0);

                    // 血管结构：分叉形
                    double vessel = 0;
                    // 主干（主动脉弓）
                    vessel += gauss(nx + 0.1, ny, 0.02, 0.02, 0.03);
                    // 左分支
                    vessel += gauss(nx - 0.3, ny + 0.3, 0.06, 0.06, 0.03);
                    // 右分支
                    vessel += gauss(nx + 0.5, ny + 0.2, 0.06, 0.06, 0.03);
                    // 细支
                    vessel += gauss(nx - 0.1, ny - 0.5, 0.08, 0.08, 0.02);

                    double val = 100.0 + 800.0 * vessel * contrastFactor + rng.nextGaussian() * 5;
                    val = Math.max(0, Math.min(4095, val));
                    int idx = (f * h * w + y * w + x) * 2;
                    short sval = (short) ((int) val);
                    data[idx] = (byte) (sval & 0xFF);
                    data[idx + 1] = (byte) ((sval >> 8) & 0xFF);
                }
            }
        }
        return data;
    }

    public int defaultWidth() { return size; }
    public int defaultHeight() { return size; }
    public int defaultFrames() { return frames; }

    private double gauss(double dx, double dy, double sx, double sy, double amp) {
        return amp * Math.exp(-(dx * dx) / (2 * sx * sx) - (dy * dy) / (2 * sy * sy));
    }
}
