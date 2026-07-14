package com.mdt.auth.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 内存用户表（M1 演示用，生产接医院统一认证/AD/LDAP）。
 * 密码以 BCrypt 加盐哈希存储，登录走 passwordEncoder.matches 比对，绝不明文比较。
 * 种子账户：doctor / ecg_tech / pathologist，均属租户 T001。
 */
@Service
public class UserService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Map<String, UserRecord> users = new HashMap<>();

    public UserService() {
        // 演示账户（生产移除，改为统一认证源）
        seed("doctor",      "doctor123", "T001", "DOCTOR");
        seed("ecg_tech",    "ecg123",    "T001", "ECG_TECH");
        seed("pathologist", "path123",   "T001", "PATHOLOGIST");
    }

    private void seed(String username, String rawPassword, String tenantId, String role) {
        users.put(username, new UserRecord(username, tenantId, role, encoder.encode(rawPassword)));
    }

    /**
     * 认证：命中且 BCrypt 比对通过返回用户记录；否则抛 BadCredentialsException(401)。
     * 不区分"用户不存在"与"密码错误"，避免账户枚举。
     */
    public UserRecord authenticate(String username, String rawPassword) {
        UserRecord u = users.get(username);
        if (u == null || rawPassword == null || !encoder.matches(rawPassword, u.passwordHash())) {
            throw new BadCredentialsException();
        }
        return u;
    }
}
