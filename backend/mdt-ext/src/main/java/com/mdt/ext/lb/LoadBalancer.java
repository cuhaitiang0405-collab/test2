package com.mdt.ext.lb;

import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ⑥ 外部扩展层 — 负载均衡器。
 * 聚合策略 + 后端列表，供云影像 SPI 等组件调用 selectBackend() 选择实例。
 * 配置驱动：application.yml → lb.strategy / lb.backends
 */
@Component
public class LoadBalancer {

    private final LoadBalancerStrategy strategy;
    private final List<String> backends;

    public LoadBalancer(@Value("${lb.strategy:ROUND_ROBIN}") String strategyName,
                        @Value("${lb.backends:http://localhost:8083}") String backendsList) {
        this.backends = Arrays.asList(backendsList.split("\\s*,\\s*"));
        this.strategy = switch (strategyName.toUpperCase()) {
            case "RANDOM" -> new RandomStrategy();
            case "WEIGHTED_ROUND_ROBIN" -> new WeightedRoundRobinStrategy(this.backends, new int[]{2, 1, 1});
            case "LEAST_CONNECTIONS" -> new LeastConnectionsStrategy();
            case "SOURCE_HASH" -> new SourceHashStrategy();
            default -> new RoundRobinStrategy();
        };
    }

    public String selectBackend(String requestKey) {
        int idx = strategy.select(backends, requestKey);
        return backends.get(idx);
    }

    public List<String> getBackends() { return backends; }
    public LoadBalancerStrategy getStrategy() { return strategy; }
}
