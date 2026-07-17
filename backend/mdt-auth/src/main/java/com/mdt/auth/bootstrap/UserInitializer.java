package com.mdt.auth.bootstrap;

import com.mdt.auth.domain.PermissionEntity;
import com.mdt.auth.domain.PermissionRepository;
import com.mdt.auth.domain.RolePermissionEntity;
import com.mdt.auth.domain.RolePermissionRepository;
import com.mdt.auth.domain.UserEntity;
import com.mdt.auth.domain.UserRepository;
import com.mdt.auth.security.UserService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
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
    private final PermissionRepository permRepo;
    private final RolePermissionRepository rpRepo;

    public UserInitializer(UserRepository userRepo, PermissionRepository permRepo, RolePermissionRepository rpRepo) {
        this.userRepo = userRepo; this.permRepo = permRepo; this.rpRepo = rpRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("doctor",      "doctor123", "T001", "DOCTOR",      "张医生");
        seed("pathologist", "path123",   "T001", "PATHOLOGIST", "赵敏 · 病理科主治医师");
        seed("ecg_tech",    "ecg123",    "T001", "ECG_TECH",    "心电技师");
        seed("admin",       "admin123",  "T001", "ADMIN",       "系统管理员");
        seedPermissions(permRepo, rpRepo);
    }

    private void seed(String username, String rawPwd, String tenantId, String role, String displayName) {
        if (userRepo.existsById(username)) return;
        userRepo.save(new UserEntity(username, UserService.encode(rawPwd), tenantId, role, displayName));
    }
    private void seedPermissions(PermissionRepository pr, RolePermissionRepository rr) {
        String[][] perms = {{"USER_MANAGE","用户管理","user","ALL"},
            {"ROLE_MANAGE","角色权限管理","role","ALL"},
            {"CONSULT_MANAGE","会诊管理","consultation","ALL"},
            {"SYSTEM_ADMIN","系统管理","system","ALL"}};
        for (String[] p : perms) {
            if (!pr.existsById(p[0])) pr.save(new PermissionEntity(p[0],p[1],p[2],p[3],true));
            rr.save(new RolePermissionEntity("ADMIN", p[0]));
        }
    }
}
