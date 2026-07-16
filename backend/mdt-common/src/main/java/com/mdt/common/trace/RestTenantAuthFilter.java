package com.mdt.common.trace;

import com.mdt.common.security.TenantContext;
import jakarta.servlet.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 多租户 + 操作人上下文注入过滤器（在各服务入口注册，配合 RestTraceFilter）。
 * 从 X-Mdt-Tenant / X-Mdt-Operator / X-Mdt-Role 头提取身份并填充 TenantContext。
 * 网关已从 JWT 中抽取并转发这些头；无头的请求回退默认 "WEB/T001/DOCTOR"。
 * 
 * 注意：仅 Spring MVC 服务；Spring Cloud Gateway（WebFlux/Netty）通过转发头实现等效行为。
 */
@Component
@Order(1) // 必须在 RestTraceFilter 之前，因为 AuditorLogger 依赖 OperatorId
public class RestTenantAuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        var request = (jakarta.servlet.http.HttpServletRequest) req;
        String tenant = headerOrDefault(request, TenantContext.HEADER_TENANT, "T001");
        String operator = headerOrDefault(request, TenantContext.HEADER_OPERATOR, "WEB");
        String role = headerOrDefault(request, TenantContext.HEADER_ROLE, "DOCTOR");
        TenantContext.set(tenant, operator, role);
        try {
            chain.doFilter(req, resp);
        } finally {
            TenantContext.clear();
        }
    }

    private String headerOrDefault(jakarta.servlet.http.HttpServletRequest req, String name, String def) {
        String v = req.getHeader(name);
        return (v != null && !v.isBlank()) ? v : def;
    }
}
