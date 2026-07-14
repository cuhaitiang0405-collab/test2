package com.mdt.auth.security;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 字段级 RBAC（DDD 应用服务）：角色 -> 资源 -> 可读字段集合。
 * 多租户隔离在 Gateway/拦截器层兜底（M6 强化）。
 */
@Service
public class RbacService {
    // role -> (resource -> 可读字段)
    private final Map<String, Map<String, Set<String>>> acl = new HashMap<>();

    public RbacService() {
        // 示例：心电技士可看心电字段，不可看病理结论
        grant("ECG_TECH", "study.ecg", Set.of("waveform", "heart_rate"));
        grant("ECG_TECH", "study.pathology", Set.of());
        grant("PATHOLOGIST", "study.pathology", Set.of("diagnosis", "conclusion"));
        grant("DOCTOR", "study.*", Set.of("*"));   // 医生可读全部
    }

    private void grant(String role, String resource, Set<String> fields) {
        acl.computeIfAbsent(role, k -> new HashMap<>()).put(resource, new HashSet<>(fields));
    }

    /** 字段级读权限校验（支持 study.* 通配前缀） */
    public boolean canReadField(String role, String resource, String field) {
        Map<String, Set<String>> res = acl.get(role);
        if (res == null) return false;
        // 精确匹配
        Set<String> exact = res.get(resource);
        if (exact != null && (exact.contains("*") || exact.contains(field))) return true;
        // 通配前缀匹配（如 study.* 覆盖 study.pathology / study.ecg）
        for (var e : res.entrySet()) {
            String key = e.getKey();
            if (key.endsWith(".*") && resource.startsWith(key.substring(0, key.length() - 2))) {
                Set<String> f = e.getValue();
                if (f.contains("*") || f.contains(field)) return true;
            }
        }
        return false;
    }
}
