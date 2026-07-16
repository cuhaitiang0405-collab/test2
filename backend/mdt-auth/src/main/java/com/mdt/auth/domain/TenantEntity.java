package com.mdt.auth.domain;

import jakarta.persistence.*;

/**
 * GAP-10 医疗单位实体（多租户管理机构信息）。
 * 各机构经机构 ID（tenantId）隔离，Admin 角色可 CRUD。
 */
@Entity
@Table(name = "md_tenant")
public class TenantEntity {

    @Id
    @Column(name = "tenant_id", nullable = false, length = 32)
    private String tenantId;

    @Column(name = "tenant_name", nullable = false)
    private String tenantName;

    @Column(name = "region")
    private String region;

    @Column(name = "type")
    private String type; // 综合 / 专科 / 社区

    @Column(name = "status")
    private String status = "ACTIVE"; // ACTIVE / INACTIVE

    public TenantEntity() {}
    public TenantEntity(String tenantId, String tenantName, String region, String type) {
        this.tenantId = tenantId; this.tenantName = tenantName; this.region = region; this.type = type;
    }

    public String getTenantId() { return tenantId; }
    public String getTenantName() { return tenantName; }
    public String getRegion() { return region; }
    public String getType() { return type; }
    public String getStatus() { return status; }

    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public void setRegion(String region) { this.region = region; }
    public void setType(String type) { this.type = type; }
    public void setStatus(String status) { this.status = status; }
}
