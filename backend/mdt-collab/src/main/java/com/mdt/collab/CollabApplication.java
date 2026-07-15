package com.mdt.collab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * ③ 协同通讯层（M5）：WebRTC 信令(Mesh) + 电子白板 + 标注序列化 + 桌面共享。
 * 复用 mdt-common 的审计/脱敏组件（scanBasePackages=com.mdt + EntityScan/JpaRepositories 含 common）。
 */
@SpringBootApplication(scanBasePackages = "com.mdt")
@EntityScan(basePackages = {"com.mdt.common", "com.mdt.collab"})
@EnableJpaRepositories(basePackages = {"com.mdt.common", "com.mdt.collab"})
public class CollabApplication {
    public static void main(String[] args) {
        SpringApplication.run(CollabApplication.class, args);
    }
}
