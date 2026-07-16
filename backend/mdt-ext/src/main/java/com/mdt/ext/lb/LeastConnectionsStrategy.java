package com.mdt.ext.lb;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 最少连接数（Least Connections）策略。
 * 跟踪每个后端的活跃请求数，选择最少连接者。
 */
public class LeastConnectionsStrategy implements LoadBalancerStrategy {
    private final ConcurrentHashMap<String, Integer> conn = new ConcurrentHashMap<>();

    @Override
    public int select(List<String> backends, String requestKey) {
        String chosen = null;
        int min = Integer.MAX_VALUE;
        for (String b : backends) {
            int c = conn.getOrDefault(b, 0);
            if (c < min) { min = c; chosen = b; }
        }
        if (chosen != null) conn.merge(chosen, 1, Integer::sum);
        return backends.indexOf(chosen);
    }

    /** 请求完成后释放连接数 */
    public void release(String backend) { conn.merge(backend, -1, (a, b) -> Math.max(0, a + b)); }

    @Override public String name() { return "LEAST_CONNECTIONS"; }
}
