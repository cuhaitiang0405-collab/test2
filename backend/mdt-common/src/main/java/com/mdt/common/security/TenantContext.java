package com.mdt.common.security;

/**
 * 多租户 + 操作人上下文（ThreadLocal）。
 * 由 RestTenantAuthFilter 在请求入口注入，所有 Repository 查询以此替换硬编码 "T001"。
 * 请求结束时务必 clear()，避免线程复用串租户。
 */
public final class TenantContext {
    public static final String HEADER_TENANT = "X-Mdt-Tenant";
    public static final String HEADER_OPERATOR = "X-Mdt-Operator";
    public static final String HEADER_ROLE = "X-Mdt-Role";

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();
    private static final ThreadLocal<String> OPERATOR = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String tenant, String operator, String role) {
        TENANT.set(tenant);
        OPERATOR.set(operator);
        ROLE.set(role);
    }

    public static String getTenantId() { return TENANT.get() != null ? TENANT.get() : "T001"; }
    public static String getOperatorId() { return OPERATOR.get() != null ? OPERATOR.get() : "WEB"; }
    public static String getRole() { return ROLE.get() != null ? ROLE.get() : "DOCTOR"; }

    public static String getTenantIdOr(String fallback) { String v = TENANT.get(); return v != null ? v : fallback; }

    public static void clear() {
        TENANT.remove();
        OPERATOR.remove();
        ROLE.remove();
    }
}
