package com.mdt.auth.rest;

import com.mdt.auth.security.BadCredentialsException;
import com.mdt.auth.security.JwtUtil;
import com.mdt.auth.security.UserRecord;
import com.mdt.auth.security.UserService;
import com.mdt.common.audit.AuditLogger;
import io.jsonwebtoken.Claims;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** REST：登录(JWT) + 当前用户。网关统一路由 /api/auth/** */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JwtUtil jwt;
    private final UserService users;
    private final AuditLogger audit;

    public AuthController(JwtUtil jwt, UserService users, AuditLogger audit) {
        this.jwt = jwt;
        this.users = users;
        this.audit = audit;
    }

    /** 登录：经内存用户表 + BCrypt 校验（生产接医院统一认证） */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        UserRecord u = users.authenticate(username, password);   // 失败抛 BadCredentialsException(401)
        String token = jwt.issue(u.username(), u.tenantId(), u.role());
        audit.log(u.tenantId(), u.username(), null, "LOGIN", "user=" + u.username());
        return Map.of("token", token, "username", u.username(), "tenantId", u.tenantId(), "role", u.role());
    }

    /** 当前用户（需 Bearer Token） */
    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BadCredentialsException();
        }
        Claims c = jwt.parse(auth.substring(7));
        return Map.of("username", c.getSubject(), "tenantId", c.get("tenantId"), "role", c.get("role"));
    }
}
