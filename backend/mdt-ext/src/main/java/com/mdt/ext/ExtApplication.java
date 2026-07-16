package com.mdt.ext;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * ⑥ 外部扩展层（M6 落地）：SPI 云影像 WADO + 5 种负载均衡策略。
 * 研发态：MockCloudImageProvider 返回本地影像端点；
 * 生产期：各云厂商实现 CloudImageProvider 并经 @Profile 装配。
 */
@SpringBootApplication(scanBasePackages = "com.mdt")
@EntityScan(basePackages = {"com.mdt.common"})
@EnableJpaRepositories(basePackages = {"com.mdt.common"})
public class ExtApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExtApplication.class, args);
    }
}
