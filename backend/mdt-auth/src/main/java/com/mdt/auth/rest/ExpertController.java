package com.mdt.auth.rest;

import com.mdt.auth.domain.UserEntity;
import com.mdt.auth.domain.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 专家名录 REST（从 md_user 表加载非 ADMIN 用户）。
 * 替代前端硬编码 MOCK_EXPERTS。
 */
@RestController
@RequestMapping("/api/patient-visit")
public class ExpertController {

    private final UserRepository userRepo;

    public ExpertController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("/experts")
    public List<Map<String, Object>> list() {
        return userRepo.findByRoleNot("ADMIN").stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getUsername());
            m.put("name", (u.getDisplayName() != null) ? u.getDisplayName() : u.getUsername());
            m.put("role", u.getRole());
            m.put("tenantId", u.getTenantId());
            m.put("institution", u.getTenantId()); // 默认机构=租户
            return m;
        }).toList();
    }
}
