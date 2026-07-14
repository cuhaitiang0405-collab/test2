package com.mdt.integration;

import com.mdt.integration.dicom.DicomAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** ① 数据接入层（M2）：DICOM 适配 + HIS/EMR 适配 + 统一索引 + 临床聚合 */
@SpringBootApplication(scanBasePackages = "com.mdt")
@EntityScan(basePackages = {"com.mdt.common", "com.mdt.integration"})
@EnableJpaRepositories(basePackages = {"com.mdt.common", "com.mdt.integration"})
public class IntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationApplication.class, args);
    }

    /** 启动 SCP 监听（研发态内存态；生产替换为 device.bindConnections()） */
    @Bean
    public ApplicationRunner dicomScpStarter(DicomAdapter dicom) {
        return args -> dicom.startScp();
    }
}
