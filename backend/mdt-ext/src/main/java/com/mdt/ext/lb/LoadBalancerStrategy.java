package com.mdt.ext.lb;

import java.util.List;

/**
 * ⑥ 外部扩展层 — 负载均衡策略接口（SPI）。
 * 后端实例由配置注入，策略在 application.yml (lb.strategy=ROUND_ROBIN/RANDOM/... ) 控制。
 */
public interface LoadBalancerStrategy {
    /** 从候选列表中选择一个后端索引 */
    int select(List<String> backends, String requestKey);
    /** 策略名称（用于配置匹配） */
    String name();
}
