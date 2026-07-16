package com.mdt.integration.dicom;

import java.util.Random;

/**
 * M7 · GAP-3 腔镜（Endoscopy）合成帧序列生成器。
 * 生成 640×480 RGB 视频帧：模拟肠/支气管粘膜褶皱纹理 + 红色调 + 随机亮点（反射）。
 * 种子数据：P1004 的腔镜检查。
 */
public class EndoscopySimulator {

    private final Random rng = new Random(123);
    private final int width = 640;
    private final int height = 480;
    private int frameIndex = 0;

    /** 返回一帧 RGB bytes (width*height*3)，模拟内窥镜画面 */
    public byte[] nextFrame() {
        byte[] frame = new byte[width * height * 3];
        double centerX = width / 2.0 + Math.sin(frameIndex * 0.03) * 40;
        double centerY = height / 2.0 + Math.cos(frameIndex * 0.05) * 20;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double dx = (x - centerX) / (width / 3.0);
                double dy = (y - centerY) / (height / 3.0);
                double r = Math.sqrt(dx * dx + dy * dy) / 1.5;

                // 粘膜色调: 深红底 + 褶皱纹理
                double baseR = 80 + 50 * (1.0 - r) + rng.nextGaussian() * 5;
                double baseG = 15 + 20 * (1.0 - r) + Math.sin(dy * 20) * 8 + rng.nextGaussian() * 3;
                double baseB = 10 + 10 * (1.0 - r) + Math.cos(dx * 15) * 5 + rng.nextGaussian() * 2;

                // 暗角
                double vignette = Math.max(0, 1.2 - r);
                baseR *= vignette; baseG *= vignette; baseB *= vignette;

                // 随机镜面反射亮点
                double specular = (rng.nextDouble() < 0.008) ? rng.nextDouble() * 100 : 0;
                baseR += specular; baseG += specular; baseB += specular;

                int idx = (y * width + x) * 3;
                frame[idx] = (byte) Math.min(255, Math.max(0, (int) baseR));
                frame[idx + 1] = (byte) Math.min(255, Math.max(0, (int) baseG));
                frame[idx + 2] = (byte) Math.min(255, Math.max(0, (int) baseB));
            }
        }
        frameIndex++;
        return frame;
    }

    /** 生成连续 N 帧 */
    public byte[] generateFrames(int count) {
        byte[] all = new byte[width * height * 3 * count];
        for (int i = 0; i < count; i++) {
            byte[] f = nextFrame();
            System.arraycopy(f, 0, all, i * f.length, f.length);
        }
        return all;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
