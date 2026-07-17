package com.mdt.auth.domain;

import jakarta.persistence.*;

/** 角色→权限映射（多对多关联表） */
@Entity
@Table(name = "md_role_permission")
public class RolePermissionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "role_name", length = 32, nullable = false)
    private String roleName;
    @Column(name = "permission_code", length = 64, nullable = false)
    private String permissionCode;

    public RolePermissionEntity() {}
    public RolePermissionEntity(String roleName, String permissionCode) {
        this.roleName=roleName;this.permissionCode=permissionCode;
    }
    public String getRoleName() { return roleName; }
    public String getPermissionCode() { return permissionCode; }
}
