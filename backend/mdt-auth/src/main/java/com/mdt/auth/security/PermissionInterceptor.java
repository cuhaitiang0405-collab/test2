package com.mdt.auth.security;

import com.mdt.common.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 权限拦截器：对标注 @RequirePermission 的方法进行权限校验。
 * 从 TenantContext 读取当前用户 role，经 PermissionService 判断。
 * ADMIN 角色直接放行。
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionService permSvc;

    public PermissionInterceptor(PermissionService permSvc) {
        this.permSvc = permSvc;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;
        RequirePermission ann = hm.getMethodAnnotation(RequirePermission.class);
        if (ann == null) return true; // 无标注 → 放行

        String role = TenantContext.getRole();
        String username = TenantContext.getOperatorId();

        if (!permSvc.hasPermission(username, role, ann.value())) {
            try {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"权限不足: " + ann.value() + "\",\"code\":403}");
            } catch (Exception ignored) {}
            return false;
        }
        return true;
    }
}
