package com.mdt.ext.lb;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * 源地址散列（Source Hash）策略。
 * 按 requestKey（如源 IP 或请求标识）取 SHA-256 哈希后 mod backends.size()，
 * 保证同一源始终路由到同一后端（会话亲和），直到实例列表变化。
 */
public class SourceHashStrategy implements LoadBalancerStrategy {

    @Override
    public int select(List<String> backends, String requestKey) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((requestKey != null ? requestKey : "").getBytes(StandardCharsets.UTF_8));
            return (hash[0] & 0xFF) % backends.size();
        } catch (Exception e) {
            return Math.abs(requestKey != null ? requestKey.hashCode() : 0) % backends.size();
        }
    }

    @Override public String name() { return "SOURCE_HASH"; }
}
