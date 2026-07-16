package com.mdt.ext.spi;

/**
 * ⑥ 外部扩展层 — 云影像提供者 SPI 接口。
 * 生产期：各云厂商（阿里云/腾讯云/华为云）实现本接口并经 @Profile 装配。
 * 研发期：MockCloudImageProvider 返回本地影像端点。
 */
public interface CloudImageProvider {
    /** 按 WADO 协议从云端调阅影像，返回可访问的图像 URL */
    String fetchImageUrl(String studyInstanceUid, String seriesInstanceUid, int instanceNumber);
    /** 验证连接与认证状态 */
    boolean healthCheck();
    /** 提供商名称 */
    String providerName();
}
