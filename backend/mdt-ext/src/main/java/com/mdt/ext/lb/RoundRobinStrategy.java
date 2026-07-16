package com.mdt.ext.lb;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** 轮询（Round Robin）— 默认策略 */
public class RoundRobinStrategy implements LoadBalancerStrategy {
    private final AtomicInteger idx = new AtomicInteger(0);

    @Override
    public int select(List<String> backends, String requestKey) {
        return Math.abs(idx.getAndIncrement() % backends.size());
    }

    @Override public String name() { return "ROUND_ROBIN"; }
}
