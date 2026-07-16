package com.mdt.ext.spi;

import com.mdt.ext.lb.LoadBalancer;
import org.springframework.stereotype.Component;

/**
 * ⑥ 外部扩展层 — 云影像 Mock 实现（研发态）。
 * 返回本地影像引擎端点；负载均衡器选择后端实例。
 * 生产替换为 AlibabaCloudProvider / TencentCloudProvider 等并标注 @Profile("prod")。
 */
@Component
public class MockCloudImageProvider implements CloudImageProvider {

    private final LoadBalancer lb;

    public MockCloudImageProvider(LoadBalancer lb) {
        this.lb = lb;
    }

    @Override
    public String fetchImageUrl(String studyInstanceUid, String seriesInstanceUid, int instanceNumber) {
        String backend = lb.selectBackend(studyInstanceUid);
        return backend + "/api/image/frame?studyUid=" + studyInstanceUid +
                "&seriesUid=" + seriesInstanceUid + "&instance=" + instanceNumber;
    }

    @Override
    public boolean healthCheck() {
        return !lb.getBackends().isEmpty();
    }

    @Override
    public String providerName() {
        return "MOCK-" + lb.getBackends().size() + "nodes-" + lb.getStrategy().name();
    }
}
