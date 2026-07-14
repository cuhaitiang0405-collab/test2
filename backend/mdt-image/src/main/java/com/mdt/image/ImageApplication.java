package com.mdt.image;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * ② 影像核心引擎（M3 落地）。
 * 自研轻量 WebGL2 体积渲染的服务端：按 studyUid 确定性生成合成体数据并流式下发；
 * 前端 GPU 完成 MPR / 窗宽窗位 / 缩放平移。生产期把 VolumeSource 换成真实对象存储 / DICOM 解码即可。
 */
@SpringBootApplication(scanBasePackages = "com.mdt")
@EntityScan("com.mdt.common")
@EnableJpaRepositories(basePackages = "com.mdt.common")
public class ImageApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImageApplication.class, args);
    }
}
