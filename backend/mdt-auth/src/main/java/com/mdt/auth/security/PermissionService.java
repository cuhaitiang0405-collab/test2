package com.mdt.auth.security;

import com.mdt.auth.domain.PermissionRepository;
import com.mdt.auth.domain.RolePermissionRepository;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.stream.Collectors;

/** 权限校验服务——admin 角色拥有所有权限 */
@Service
public class PermissionService {
    private final RolePermissionRepository rpRepo;
    private final PermissionRepository permRepo;

    public PermissionService(RolePermissionRepository rpRepo, PermissionRepository permRepo) {
        this.rpRepo = rpRepo; this.permRepo = permRepo;
    }

    public boolean hasPermission(String username, String role, String permissionCode) {
        if ("ADMIN".equals(role)) return true; // 超级管理员全部放行
        return rpRepo.findByRoleName(role).stream()
                .anyMatch(rp -> rp.getPermissionCode().equals(permissionCode));
    }

    public Set<String> getPermissions(String role) {
        if ("ADMIN".equals(role)) return permRepo.findAll().stream().map(p->p.getCode()).collect(Collectors.toSet());
        return rpRepo.findByRoleName(role).stream().map(r -> r.getPermissionCode()).collect(Collectors.toSet());
    }
}
