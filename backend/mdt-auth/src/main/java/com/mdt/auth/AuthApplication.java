package com.mdt.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** ⑤ 权限租户管理服务（M1 先行落地：登录/JWT/字段级RBAC/gRPC/审计） */
@SpringBootApplication(scanBasePackages = "com.mdt")
@EntityScan(basePackages = {"com.mdt.common", "com.mdt.auth"})
@EnableJpaRepositories(basePackages = {"com.mdt.common", "com.mdt.auth"})
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
