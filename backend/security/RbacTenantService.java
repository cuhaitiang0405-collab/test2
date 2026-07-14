package com.mdt.security;

import java.util.*;

/**
 * ⑤ 权限租户管理 — 字段级 RBAC + 多租户隔离 + 日志脱敏。
 * - 多租户：所有查询必须带 TenantId 行级过滤。
 * - 字段级 RBAC：角色对资源字段细粒度授权（如心电技士不可见病理结论）。
 * - 脱敏：日志/审计禁止出现姓名、身份证，仅保留 PatientID + 检查号。
 */
public class RbacTenantService {

    /** 角色 -> (资源 -> (字段 -> 可读)) */
    private final Map<String, Map<String, Set<String>>> fieldAcl = new HashMap<>();

    public RbacTenantService() {
        // 示例：心电技士可看心电数据，但不可看病理结论字段
        grant("ECG_TECH", "study.ecg", Set.of("waveform", "heart_rate"));
        grant("ECG_TECH", "study.pathology", Set.of()); // 病理字段全不可见
        grant("PATHOLOGIST", "study.pathology", Set.of("diagnosis", "conclusion"));
    }

    private void grant(String role, String resource, Set<String> fields) {
        fieldAcl.computeIfAbsent(role, k -> new HashMap()).put(resource, fields);
    }

    /** 字段级权限校验 */
    public boolean canReadField(String role, String resource, String field) {
        Map<String, Set<String>> res = fieldAcl.get(role);
        if (res == null) return false;
        Set<String> allowed = res.get(resource);
        return allowed != null && allowed.contains(field);
    }

    /**
     * 多租户行级过滤：在 SQL 之外再兜一层断言，
     * 防止越权跨机构访问（区域会诊需显式授权白名单）。
     */
    public void assertTenantVisible(String callerTenant, String dataTenant,
                                    boolean regionAllowed) {
        if (!callerTenant.equals(dataTenant) && !regionAllowed) {
            throw new SecurityException("跨租户访问被拒绝：" + callerTenant + " -> " + dataTenant);
        }
    }

    // ----------------------- 日志脱敏 -----------------------
    /** 脱敏器：姓名/身份证替换为掩码，仅保留 PatientID + 检查号 */
    public static class Desensitizer {
        public static String mask(String rawLog) {
            if (rawLog == null) return null;
            // 身份证：18 位，掩码中间
            String s = rawLog.replaceAll("(\\d{6})\\d{8}(\\d{4})", "$1********$2");
            // 姓名（2-4 个汉字）替换为 **
            s = s.replaceAll("(?<=[^\\u4e00-\\u9fa5])([\\u4e00-\\u9fa5]{2,4})(?=[^\\u4e00-\\u9fa5])", "**");
            return s;
        }
    }

    /** 统一日志门面：append 前自动脱敏 */
    public static class MdtLogger {
        public static void info(String traceId, String msg) {
            System.out.printf("[%s] %s%n", traceId, Desensitizer.mask(msg));
        }
    }

    // ============================ 单元/压测用例建议 ============================
    // 1) canReadField：ECG_TECH 读 study.ecg.heart_rate=true；读 study.pathology.* =false。
    // 2) assertTenantVisible：不同租户且 regionAllowed=false 抛异常；true 通过。
    // 3) Desensitizer：含身份证/姓名的日志输出后仅剩 PatientID+检查号，断言无明文敏感信息。
    // 4) 压测：字段级校验在高并发下 P99 < 100μs（内存 Map 读取）。
}
