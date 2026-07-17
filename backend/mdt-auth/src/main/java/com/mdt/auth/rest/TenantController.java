package com.mdt.auth.rest;

import com.mdt.auth.domain.TenantEntity;
import com.mdt.auth.domain.TenantRepository;
import com.mdt.common.audit.AuditLogger;
import com.mdt.common.security.TenantContext;
import com.mdt.auth.security.RequirePermission;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * GAP-10 医疗单位管理 CRUD（Admin 角色可操作）。
 * 路由经 API 网关 /api/patient-visit/tenants/** 映射到 mdt-auth。
 */
@RestController
@RequestMapping("/api/patient-visit")
public class TenantController {

    private final TenantRepository repo;
    private final AuditLogger audit;

    public TenantController(TenantRepository repo, AuditLogger audit) {
        this.repo = repo;
        this.audit = audit;
    }

    /** 机构列表（含状态过滤） */
    @RequirePermission("USER_MANAGE")
    @GetMapping("/tenants")
    public List<Map<String, Object>> list(@RequestParam(defaultValue = "") String status) {
        List<TenantEntity> list = (status.isBlank())
                ? repo.findAll()
                : repo.findByStatus(status.toUpperCase());
        return list.stream().map(this::toMap).toList();
    }

    /** 新建机构 */
    @RequirePermission("USER_MANAGE")
    @PostMapping("/tenants")
    public Map<String, Object> create(@RequestBody Map<String, String> body) {
        String id = body.get("tenantId");
        String name = body.get("tenantName");
        if (id == null || id.isBlank() || name == null || name.isBlank())
            throw new IllegalArgumentException("tenantId / tenantName 必填");
        TenantEntity t = new TenantEntity(id.trim(), name.trim(),
                body.getOrDefault("region", ""), body.getOrDefault("type", "综合"));
        repo.save(t);
        audit.log(TenantContext.getTenantId(), TenantContext.getOperatorId(), null,
                "TENANT_CREATE", "tenantId=" + id);
        return toMap(t);
    }

    /** 更新机构 */
    @RequirePermission("USER_MANAGE")
    @PutMapping("/tenants/{tenantId}")
    public Map<String, Object> update(@PathVariable String tenantId, @RequestBody Map<String, String> body) {
        TenantEntity t = repo.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("机构不存在: " + tenantId));
        if (body.containsKey("tenantName")) t.setTenantName(body.get("tenantName"));
        if (body.containsKey("region")) t.setRegion(body.get("region"));
        if (body.containsKey("type")) t.setType(body.get("type"));
        if (body.containsKey("status")) t.setStatus(body.get("status"));
        repo.save(t);
        audit.log(TenantContext.getTenantId(), TenantContext.getOperatorId(), null, "TENANT_UPDATE", "tenantId=" + tenantId);
        return toMap(t);
    }

    /** 停用/启用机构 */
    @RequirePermission("USER_MANAGE")
    @PutMapping("/tenants/{tenantId}/status")
    public Map<String, Object> toggleStatus(@PathVariable String tenantId, @RequestBody Map<String, String> body) {
        TenantEntity t = repo.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("机构不存在: " + tenantId));
        t.setStatus(body.getOrDefault("status", "INACTIVE").toUpperCase());
        repo.save(t);
        audit.log(TenantContext.getTenantId(), TenantContext.getOperatorId(), null, "TENANT_TOGGLE", "tenantId=" + tenantId + " status=" + t.getStatus());
        return toMap(t);
    }

    private Map<String, Object> toMap(TenantEntity t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tenantId", t.getTenantId());
        m.put("tenantName", t.getTenantName());
        m.put("region", t.getRegion());
        m.put("type", t.getType());
        m.put("status", t.getStatus());
        return m;
    }
}
