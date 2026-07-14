package com.mdt.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ④ 业务流程层（M1 占位骨架）。
 * M4 将落地：会诊状态机（申请→通知→确认→进行中→总结）+ 短信异步队列。
 */
@SpringBootApplication
public class WorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }
}
