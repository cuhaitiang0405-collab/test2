package com.mdt.common.security;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.*;

/**
 * 字段级 RBAC 响应拦截器（M6 核心能力）。
 * 在 @ResponseBody 写出前，按当前用户角色 strip 越权字段。
 * 当前规则（与 RbacService 对齐）：
 * - ECG_TECH：不可见 pathologyConclusion / pathology_conclusion / diagnosis
 * - 其余角色：全字段可见（M6 基础版，后续可扩展细粒度 ACL 配置中心）
 */
@RestControllerAdvice
public class FieldMaskAdvice implements ResponseBodyAdvice<Object> {

    // 角色 → 被禁止的字段名（Map key 匹配，大小写不敏感）
    private static final Map<String, Set<String>> BLOCKED = Map.of(
        "ECG_TECH", Set.of("pathologyconclusion", "pathology_conclusion", "conclusion", "diagnosis",
                           "pathologyconclusion_text", "pathology_diagnosis")
    );

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        String role = TenantContext.getRole();
        Set<String> blocked = BLOCKED.get(role);
        if (blocked == null || body == null) return body;

        if (body instanceof Map) {
            stripFields((Map<?, ?>) body, blocked);
        } else if (body instanceof List) {
            for (Object item : (List<?>) body) {
                if (item instanceof Map) stripFields((Map<?, ?>) item, blocked);
            }
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private void stripFields(Map<?, ?> map, Set<String> blocked) {
        Set<String> toRemove = new HashSet<>();
        for (Object key : map.keySet()) {
            String field = String.valueOf(key).toLowerCase().replace(" ", "").replace("_", "");
            if (blocked.contains(field)) toRemove.add(String.valueOf(key));
            // 递归子 Map
            Object val = map.get(key);
            if (val instanceof Map) stripFields((Map<?, ?>) val, blocked);
            else if (val instanceof List) {
                for (Object item : (List<?>) val) {
                    if (item instanceof Map) stripFields((Map<?, ?>) item, blocked);
                }
            }
        }
        ((Map<String, Object>) map).keySet().removeAll(toRemove);
    }
}
