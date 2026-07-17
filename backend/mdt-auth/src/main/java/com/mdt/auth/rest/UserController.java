package com.mdt.auth.rest;

import com.mdt.auth.domain.UserEntity;
import com.mdt.auth.domain.UserRepository;
import com.mdt.auth.security.UserService;
import com.mdt.common.audit.AuditLogger;
import com.mdt.common.security.TenantContext;
import com.mdt.auth.security.RequirePermission;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** Admin 用户管理 CRUD（仅 ADMIN 可操作） */
@RestController
@RequestMapping("/api/patient-visit")
public class UserController {
    private final UserRepository repo;
    private final AuditLogger audit;

    public UserController(UserRepository repo, AuditLogger audit) {
        this.repo = repo; this.audit = audit;
    }

    @RequirePermission("USER_MANAGE")
    @GetMapping("/users")
    public List<Map<String,Object>> list() {
        return repo.findAll().stream().map(this::toMap).toList();
    }

    @RequirePermission("USER_MANAGE")
    @PostMapping("/users")
    public Map<String,Object> create(@RequestBody Map<String,String> body) {
        String username = body.get("username"), password = body.get("password"),
               role = body.getOrDefault("role","USER"), displayName = body.getOrDefault("displayName",username);
        if (username == null || username.isBlank() || password == null || password.isBlank())
            throw new IllegalArgumentException("用户名和密码必填");
        UserEntity u = new UserEntity(username, UserService.encode(password), TenantContext.getTenantId(), role, displayName);
        repo.save(u);
        audit.log(TenantContext.getTenantId(), TenantContext.getOperatorId(), null, "USER_CREATE", "user=" + username);
        return toMap(u);
    }

    @RequirePermission("USER_MANAGE")
    @PutMapping("/users/{username}")
    public Map<String,Object> update(@PathVariable String username, @RequestBody Map<String,String> body) {
        UserEntity u = require(username);
        if ("ADMIN".equals(u.getRole())) throw new IllegalArgumentException("超级管理员不可编辑");
        if (body.containsKey("displayName")) u.setDisplayName(body.get("displayName"));
        if (body.containsKey("role") && !"ADMIN".equals(u.getRole())) u.setRole(body.get("role"));
        if (body.containsKey("password")) u.setPasswordHash(UserService.encode(body.get("password")));
        repo.save(u);
        audit.log(TenantContext.getTenantId(), TenantContext.getOperatorId(), null, "USER_UPDATE", "user=" + username);
        return toMap(u);
    }

    @RequirePermission("USER_MANAGE")
    @DeleteMapping("/users/{username}")
    public Map<String,Object> delete(@PathVariable String username) {
        UserEntity u = require(username);
        if ("ADMIN".equals(u.getRole())) throw new IllegalArgumentException("超级管理员不可删除");
        repo.delete(u);
        audit.log(TenantContext.getTenantId(), TenantContext.getOperatorId(), null, "USER_DELETE", "user=" + username);
        return Map.of("success", true);
    }

    @RequirePermission("USER_MANAGE")
    @PutMapping("/users/{username}/password")
    public Map<String,Object> resetPwd(@PathVariable String username, @RequestBody Map<String,String> body) {
        UserEntity u = require(username);
        u.setPasswordHash(UserService.encode(body.getOrDefault("password","123456")));
        repo.save(u);
        return Map.of("success", true);
    }

    private UserEntity require(String username) {
        return repo.findById(username).orElseThrow(() -> new IllegalArgumentException("用户不存在: " + username));
    }
    private Map<String,Object> toMap(UserEntity u) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("username",u.getUsername());m.put("role",u.getRole());
        m.put("displayName",u.getDisplayName());m.put("tenantId",u.getTenantId());
        return m;
    }
}
