package com.mdt.auth.domain;

import jakarta.persistence.*;

/** 用户实体（替代硬编码 UserService） */
@Entity
@Table(name = "md_user")
public class UserEntity {

    @Id @Column(length = 64)
    private String username;

    @Column(name = "password_hash", length = 128, nullable = false)
    private String passwordHash;

    @Column(name = "tenant_id", length = 32, nullable = false)
    private String tenantId;

    @Column(length = 32, nullable = false)
    private String role;

    @Column(name = "display_name", length = 64)
    private String displayName;

    public UserEntity() {}
    public UserEntity(String username, String passwordHash, String tenantId, String role, String displayName) {
        this.username = username; this.passwordHash = passwordHash;
        this.tenantId = tenantId; this.role = role; this.displayName = displayName;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getTenantId() { return tenantId; }
    public String getRole() { return role; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v) { this.displayName = v; }
    public void setRole(String v) { this.role = v; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
}
