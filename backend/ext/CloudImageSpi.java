package com.mdt.ext;

import java.util.ServiceLoader;

/**
 * ⑥ 外部扩展层 — 云影像 SPI（可插拔）。
 * 定义标准扩展点，影像云引擎（CT/MRI/CR/DR/DSA/RF/US 即时运算）
 * 与区域专家标记实现以 ServiceLoader 注册，业务域零耦合调用。
 */
public interface CloudImageProvider {

    /** 供应商标识，如 "tencent-health-cloud"、"region-expert-mark" */
    String vendor();

    /** 是否支持该模态（CT/MRI/US/...） */
    boolean supports(String modality);

    /** 经 WADO 取云端渲染图像 */
    WadoImage fetchViaWado(WadoRequest req);
}

/** WADO 请求/响应（SPI 内部模型，与 gRPC WadoRequest 对齐） */
class WadoRequest {
    String studyInstanceUid;
    String seriesInstanceUid;
    int instanceNumber;
}
class WadoImage {
    boolean success;
    String imageUrl;
}

/**
 * SPI 工厂：通过 ServiceLoader 加载所有已注册实现，按模态择优。
 * 结合 负载均衡策略（轮询/加权/最少连接/源地址散列）并发调度。
 */
public class CloudImageSpiFactory {
    private final java.util.List<CloudImageProvider> providers = new java.util.ArrayList<>();

    public CloudImageSpiFactory() {
        // 自动发现 classpath 下 META-INF/services/com.mdt.ext.CloudImageProvider 的实现
        ServiceLoader<CloudImageProvider> loader = ServiceLoader.load(CloudImageProvider.class);
        loader.forEach(providers::add);
    }

    public CloudImageProvider select(String modality) {
        return providers.stream()
                .filter(p -> p.supports(modality))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("无可用云影像供应商: " + modality));
    }

    public int providerCount() { return providers.size(); }

    // ============================ 单元/压测用例建议 ============================
    // 1) SPI 发现：classpath 放入实现 + META-INF/services 文件，select(modality) 命中对应供应商。
    // 2) 无实现：select 抛 IllegalStateException。
    // 3) 扩展隔离：新增供应商不修改本工厂（开闭原则）。
    // 4) 压测：并发 select 多模态，验证负载均衡策略在供应商间均分/加权分配。
}
