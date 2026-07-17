package com.mdt.auth.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "md_permission")
public class PermissionEntity {
    @Id @Column(length = 64)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(length = 64)
    private String resource;
    @Column(length = 16)
    private String action; // READ/WRITE/DELETE/ALL
    @Column
    private boolean builtin; // 内置权限不可删除

    public PermissionEntity() {}
    public PermissionEntity(String code, String name, String resource, String action, boolean builtin) {
        this.code=code;this.name=name;this.resource=resource;this.action=action;this.builtin=builtin;
    }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getResource() { return resource; }
    public String getAction() { return action; }
    public boolean isBuiltin() { return builtin; }
}
