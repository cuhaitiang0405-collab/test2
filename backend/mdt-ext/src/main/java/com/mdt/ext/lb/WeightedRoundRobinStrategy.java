package com.mdt.ext.lb;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 加权轮询（Weighted Round Robin）。
 * 每个后端附 weight（配置注入），按比例分配请求。
 * 简化实现：列表中权重为 N 的后端出现 N 次，再对展开列表做轮询。
 */
public class WeightedRoundRobinStrategy implements LoadBalancerStrategy {
    private final List<String> expanded = new CopyOnWriteArrayList<>();
    private final AtomicInteger idx = new AtomicInteger(0);

    /** backends 格式："host:port"；weights 为其对应权重，索引对齐 */
    public WeightedRoundRobinStrategy(List<String> backends, int[] weights) {
        for (int i = 0; i < backends.size(); i++) {
            int w = i < weights.length ? weights[i] : 1;
            for (int j = 0; j < w; j++) expanded.add(backends.get(i));
        }
    }

    @Override
    public int select(List<String> backends, String requestKey) {
        if (expanded.isEmpty()) return 0;
        return backends.indexOf(expanded.get(Math.abs(idx.getAndIncrement() % expanded.size())));
    }

    @Override public String name() { return "WEIGHTED_ROUND_ROBIN"; }
}
