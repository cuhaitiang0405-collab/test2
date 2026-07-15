package com.mdt.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/** 异步执行器：供 Mock 短信网关 @Async("smsExecutor") 使用，隔离短信投递与会诊主流程。 */
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean("smsExecutor")
    public Executor smsExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(50);
        ex.setThreadNamePrefix("sms-");
        ex.initialize();
        return ex;
    }
}
