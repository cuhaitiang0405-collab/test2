package com.mdt.loadbalancer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 影像云运算负载均衡器。
 * 支持：轮询(ROUND_ROBIN)、加权轮询(WEIGHTED_ROUND_ROBIN)、
 *       最少连接(LEAST_CONNECTIONS)、源地址散列(SOURCE_HASH)。
 * 用于 ⑥ 外部扩展层 并发调度云影像引擎（CT/MRI/CR/DR/DSA/RF/US 即时运算）。
 */
public class LoadBalancer {

    /** 负载均衡策略枚举 */
    public enum Strategy {
        ROUND_ROBIN,
        WEIGHTED_ROUND_ROBIN,
        LEAST_CONNECTIONS,
        SOURCE_HASH
    }

    /** 后端节点（影像云运算单元） */
    public static class Node {
        final InetSocketAddress addr;
        final int weight;                 // 加权轮询权重（>0）
        final AtomicInteger activeConns;  // 最少连接计数

        public Node(String host, int port, int weight) {
            this.addr = new InetSocketAddress(host, port);
            this.weight = Math.max(1, weight);
            this.activeConns = new AtomicInteger(0);
        }
    }

    private final List<Node> nodes;
    private final Strategy strategy;
    private final AtomicInteger rrCursor = new AtomicInteger(0);
    // 加权轮询：平滑加权（当前权重累加后选最大，再减总权重）
    private final ConcurrentHashMap<Node, Integer> currentWeight = new ConcurrentHashMap<>();
    private final AtomicInteger wrrCursor = new AtomicInteger(0);

    public LoadBalancer(List<Node> nodes, Strategy strategy) {
        this.nodes = nodes;
        this.strategy = strategy;
        nodes.forEach(n -> currentWeight.put(n, 0));
    }

    /**
     * 选择一个节点处理本次请求。
     * @param clientKey 用于源地址散列的客户端标识（IP 或 PatientVisitUID）
     */
    public Node select(String clientKey) {
        switch (strategy) {
            case ROUND_ROBIN:          return roundRobin();
            case WEIGHTED_ROUND_ROBIN: return weightedRoundRobin();
            case LEAST_CONNECTIONS:    return leastConnections();
            case SOURCE_HASH:          return sourceHash(clientKey);
            default:                   return roundRobin();
        }
    }

    /** 轮询：简单取模游标 */
    private Node roundRobin() {
        int idx = Math.floorMod(rrCursor.getAndIncrement(), nodes.size());
        return nodes.get(idx);
    }

    /**
     * 加权轮询（平滑加权轮询 / Nginx 算法）：
     * 每轮把所有节点当期权重 += 配置权重，选当期权重最大者，选后减掉总权重。
     * 优点：权重高的被选中更频繁，但请求分布平滑不突兀。
     */
    private Node weightedRoundRobin() {
        int totalWeight = nodes.stream().mapToInt(n -> n.weight).sum();
        Node best = null;
        int bestWeight = Integer.MIN_VALUE;
        synchronized (nodes) {
            for (Node n : nodes) {
                int cw = currentWeight.get(n) + n.weight;
                currentWeight.put(n, cw);
                if (cw > bestWeight) { bestWeight = cw; best = n; }
            }
            currentWeight.put(best, bestWeight - totalWeight);
        }
        return best;
    }

    /** 最少连接：挑 activeConns 最小者（并发安全） */
    private Node leastConnections() {
        Node best = nodes.get(0);
        int min = best.activeConns.get();
        for (Node n : nodes) {
            int c = n.activeConns.get();
            if (c < min) { min = c; best = n; }
        }
        best.activeConns.incrementAndGet(); // 占用一个连接
        return best;
    }

    /** 源地址散列：同一 clientKey 始终命中同一节点（会话粘滞，利于缓存命中） */
    private Node sourceHash(String clientKey) {
        int h = (clientKey == null ? 0 : clientKey.hashCode());
        int idx = Math.floorMod(h, nodes.size());
        return nodes.get(idx);
    }

    /** 释放连接（与 leastConnections 配对） */
    public void release(Node node) {
        node.activeConns.decrementAndGet();
    }

    // ============================ 单元/压测用例建议 ============================
    // 1) 轮询：连续 select 1000 次，统计各节点命中次数应≈均分。
    // 2) 加权轮询：权重 1:3，万次抽样命中比应≈1:3。
    // 3) 最少连接：预占用某节点，select 优先返回连接数最小的节点；release 后计数回落。
    // 4) 源地址散列：同一 clientKey 连续 select 必返回同一节点；不同 key 分布均衡。
    // 5) 压测：4 节点、10k QPS 下 select P99 < 50μs（无锁路径）；加权/最少连接走同步块需注意竞争。
}
