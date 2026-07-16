package com.mdt.auth.security;

import com.mdt.auth.domain.UserEntity;
import com.mdt.auth.domain.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户认证服务（DB 驱动，JPA）。
 * 种子用户由 DataInitializer 在 dev 模式下写入 md_user 表。
 * 生产期对接 AD/LDAP/OAuth2 统一认证。
 */
@Service
public class UserService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    /** 登录：查 md_user 表做 BCrypt 比对 */
    public UserRecord authenticate(String username, String rawPassword) {
        UserEntity u = userRepo.findById(username)
                .orElseThrow(() -> new BadCredentialsException());
        if (rawPassword == null || !encoder.matches(rawPassword, u.getPasswordHash())) {
            throw new BadCredentialsException();
        }
        return new UserRecord(u.getUsername(), u.getTenantId(), u.getRole(), u.getPasswordHash());
    }

    /** dev 模式通过 DataInitializer 批量建种子用户 */
    public static String encode(String raw) {
        return new BCryptPasswordEncoder().encode(raw);
    }
}
