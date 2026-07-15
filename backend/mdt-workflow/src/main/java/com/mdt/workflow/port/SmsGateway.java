package com.mdt.workflow.port;

/**
 * 短信网关端口（SPI 风格）：M4 用 Mock 实现模拟 MQ 异步队列投递；
 * 生产期替换为真实短信网关（阿里云/腾讯云等），对上层 ConsultationService 透明。
 */
public interface SmsGateway {
    /** 向指定专家发送会诊通知短信（异步） */
    void sendNotification(String expertName, String consultationTitle);
}
