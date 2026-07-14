package com.mdt.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.*;

/** 验证内存用户表 + BCrypt 真实加解密链路（无 Mock）。 */
class UserServiceTest {

    private final UserService service = new UserService();

    @Test
    void doctorLoginOk() {
        UserRecord u = service.authenticate("doctor", "doctor123");
        assertThat(u.username()).isEqualTo("doctor");
        assertThat(u.role()).isEqualTo("DOCTOR");
        assertThat(u.tenantId()).isEqualTo("T001");
        // 哈希落地、非空、且确为 BCrypt 格式
        assertThat(u.passwordHash()).startsWith("$2a$");
    }

    @Test
    void wrongPasswordRejected() {
        assertThatThrownBy(() -> service.authenticate("doctor", "wrong"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void unknownUserRejected() {
        assertThatThrownBy(() -> service.authenticate("nobody", "x"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void ecgTechAndPathologistSeeded() {
        assertThat(service.authenticate("ecg_tech", "ecg123").role()).isEqualTo("ECG_TECH");
        assertThat(service.authenticate("pathologist", "path123").role()).isEqualTo("PATHOLOGIST");
    }

    @Test
    void bcryptSaltMakesDistinctHashes() {
        String a = new BCryptPasswordEncoder().encode("doctor123");
        String b = new BCryptPasswordEncoder().encode("doctor123");
        assertThat(a).isNotEqualTo(b);                 // 同明文不同盐
        assertThat(new BCryptPasswordEncoder().matches("doctor123", a)).isTrue();
    }
}
