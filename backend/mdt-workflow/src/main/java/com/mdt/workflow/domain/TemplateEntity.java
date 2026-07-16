package com.mdt.workflow.domain;

import jakarta.persistence.*;

/**
 * M7 · GAP-1 模板定义实体（检查/讨论/病历三类可定制模板）。
 * sections 为 JSON 数组，每 section 含 title + fields[]。
 */
@Entity
@Table(name = "template_definition")
public class TemplateEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", length = 64, nullable = false, unique = true)
    private String templateId;

    @Column(name = "type", length = 16, nullable = false)
    private String type; // IMAGING | DISCUSSION | EMR

    @Column(name = "tenant_id", length = 32, nullable = false)
    private String tenantId;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "sections", columnDefinition = "text")
    private String sections; // JSON array of {title, fields:[{key,label,type,required,options}]}

    public TemplateEntity() {}
    public TemplateEntity(String templateId, String type, String tenantId, String name, String sections) {
        this.templateId = templateId; this.type = type; this.tenantId = tenantId; this.name = name; this.sections = sections;
    }

    public Long getId() { return id; }
    public String getTemplateId() { return templateId; }
    public String getType() { return type; }
    public String getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getSections() { return sections; }
    public void setName(String v) { this.name = v; }
    public void setSections(String v) { this.sections = v; }
    public void setType(String v) { this.type = v; }
    public void setTenantId(String v) { this.tenantId = v; }
}
