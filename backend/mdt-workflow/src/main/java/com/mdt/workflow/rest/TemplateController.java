package com.mdt.workflow.rest;

import com.mdt.common.audit.AuditLogger;
import com.mdt.common.security.TenantContext;
import com.mdt.workflow.domain.TemplateEntity;
import com.mdt.workflow.domain.TemplateRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * M7 · GAP-1 模板引擎 REST 入口（检查/讨论/病历三类模板 CRUD）。
 * 路由经网关 /api/workflow/templates/**。
 * 研发态预置 3 套种子模板（见 DataSeeder），Admin 角色可编辑。
 */
@RestController
@RequestMapping("/api/workflow/templates")
public class TemplateController {

    private final TemplateRepository repo;
    private final AuditLogger audit;

    public TemplateController(TemplateRepository repo, AuditLogger audit) {
        this.repo = repo;
        this.audit = audit;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(defaultValue = "") String type) {
        String tenant = TenantContext.getTenantId();
        List<TemplateEntity> list;
        if (type.isBlank()) list = repo.findByTenantId(tenant);
        else list = repo.findByTenantIdAndType(tenant, type.toUpperCase());
        return list.stream().map(this::toMap).toList();
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        String tid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        TemplateEntity e = new TemplateEntity(
                tid,
                String.valueOf(body.getOrDefault("type", "DISCUSSION")).toUpperCase(),
                TenantContext.getTenantId(),
                String.valueOf(body.getOrDefault("name", "未命名")),
                body.containsKey("sections") ? String.valueOf(body.get("sections")) : "[]"
        );
        repo.save(e);
        audit.log(TenantContext.getTenantId(), TenantContext.getOperatorId(), null, "TEMPLATE_CREATE", "id=" + tid);
        return toMap(e);
    }

    @PutMapping("/{templateId}")
    public Map<String, Object> update(@PathVariable String templateId, @RequestBody Map<String, Object> body) {
        TemplateEntity e = jpaFindByTemplateId(templateId);
        if (body.containsKey("name")) e.setName(String.valueOf(body.get("name")));
        if (body.containsKey("type")) e.setType(String.valueOf(body.get("type")).toUpperCase());
        if (body.containsKey("sections")) e.setSections(String.valueOf(body.get("sections")));
        repo.save(e);
        audit.log(TenantContext.getTenantId(), TenantContext.getOperatorId(), null, "TEMPLATE_UPDATE", "id=" + templateId);
        return toMap(e);
    }

    @DeleteMapping("/{templateId}")
    public Map<String, Object> delete(@PathVariable String templateId) {
        jpaFindByTemplateId(templateId);
        repo.deleteByTemplateId(templateId);
        audit.log(TenantContext.getTenantId(), TenantContext.getOperatorId(), null, "TEMPLATE_DELETE", "id=" + templateId);
        return Map.of("success", true);
    }

    private TemplateEntity jpaFindByTemplateId(String templateId) {
        return repo.findAll().stream()
                .filter(e -> e.getTemplateId().equals(templateId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + templateId));
    }

    private Map<String, Object> toMap(TemplateEntity e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("templateId", e.getTemplateId());
        m.put("type", e.getType());
        m.put("tenantId", e.getTenantId());
        m.put("name", e.getName());
        m.put("sections", e.getSections());
        return m;
    }
}
