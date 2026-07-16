package com.mdt.auth.bootstrap;

import com.mdt.auth.domain.UserEntity;
import com.mdt.auth.domain.UserRepository;
import com.mdt.auth.security.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Dev 模式种子用户（生产移除此组件）。
 * 默认: doctor/doctor123(DOCTOR) | pathologist/path123(PATHOLOGIST) | ecg_tech/ecg123(ECG_TECH) | admin/admin123(ADMIN)
 */
@Component
@Profile("!prod")
public class UserInitializer implements ApplicationRunner {

    private final UserRepository userRepo;

    public UserInitializer(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("doctor",      "doctor123", "T001", "DOCTOR",      "张医生");
        seed("pathologist", "path123",   "T001", "PATHOLOGIST", "赵敏 · 病理科主治医师");
        seed("ecg_tech",    "ecg123",    "T001", "ECG_TECH",    "心电技师");
        seed("admin",       "admin123",  "T001", "ADMIN",       "系统管理员");
    }

    private void seed(String username, String rawPwd, String tenantId, String role, String displayName) {
        if (userRepo.existsById(username)) return;
        userRepo.save(new UserEntity(username, UserService.encode(rawPwd), tenantId, role, displayName));
    }
}
