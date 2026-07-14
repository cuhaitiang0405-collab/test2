package com.mdt.ext;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ⑥ 外部扩展层（M1 占位骨架）。
 * M6 将落地：SPI 云影像 WADO + 负载均衡策略（轮询/加权轮询/最少连接/源地址散列）。
 */
@SpringBootApplication
public class ExtApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExtApplication.class, args);
    }
}
