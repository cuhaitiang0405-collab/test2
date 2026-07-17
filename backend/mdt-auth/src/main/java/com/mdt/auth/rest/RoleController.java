package com.mdt.auth.rest;

import com.mdt.auth.domain.*;
import com.mdt.common.audit.AuditLogger;
import com.mdt.common.security.TenantContext;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

/** 角色与权限管理（仅 ADMIN 可操作） */
@RestController
@RequestMapping("/api/patient-visit")
public class RoleController {
    private final PermissionRepository permRepo;
    private final RolePermissionRepository rpRepo;
    private final AuditLogger audit;

    public RoleController(PermissionRepository p, RolePermissionRepository rp, AuditLogger audit) {
        this.permRepo=p;this.rpRepo=rp;this.audit=audit;
    }

    @GetMapping("/permissions")
    public List<Map<String,Object>> listPermissions() {
        return permRepo.findAll().stream().map(p -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("code",p.getCode());m.put("name",p.getName());
            m.put("resource",p.getResource());m.put("action",p.getAction());
            m.put("builtin",p.isBuiltin());return m;
        }).toList();
    }

    @GetMapping("/roles")
    public List<Map<String,Object>> listRoles() {
        Set<String> roles = new LinkedHashSet<>();
        rpRepo.findAll().forEach(rp -> roles.add(rp.getRoleName()));
        return roles.stream().map(role -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("roleName", role);
            m.put("permissions", rpRepo.findByRoleName(role).stream().map(rp->rp.getPermissionCode()).toList());
            m.put("builtin", "ADMIN".equals(role));
            return m;
        }).collect(Collectors.toList());
    }

    @PutMapping("/roles/{roleName}/permissions")
    public Map<String,Object> assignPermissions(@PathVariable String roleName, @RequestBody Map<String,Object> body) {
        if ("ADMIN".equals(roleName)) throw new IllegalArgumentException("超级管理员权限不可修改");
        @SuppressWarnings("unchecked")
        List<String> codes = (List<String>) body.getOrDefault("permissionCodes", List.of());
        rpRepo.deleteByRoleName(roleName);
        codes.forEach(code -> rpRepo.save(new RolePermissionEntity(roleName, code)));
        audit.log(TenantContext.getTenantId(), TenantContext.getOperatorId(), null, "ROLE_PERM_UPDATE", "role=" + roleName + " perms=" + codes.size());
        return Map.of("success", true, "roleName", roleName, "count", codes.size());
    }
}
