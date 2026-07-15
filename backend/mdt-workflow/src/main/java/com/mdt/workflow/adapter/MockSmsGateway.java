package com.mdt.workflow.adapter;

import com.mdt.workflow.port.SmsGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Mock 短信网关：模拟「短信异步队列（MQ）」投递。
 * 用 @Async 异步执行，不阻塞会诊主流程；真实环境替换为经 MQ 的短信服务即可。
 */
@Component
public class MockSmsGateway implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(MockSmsGateway.class);

    @Async("smsExecutor")
    @Override
    public void sendNotification(String expertName, String consultationTitle) {
        // 模拟 MQ 投递：仅记日志（生产期改为调用短信服务商 API + 落 SMS_OUTBOX）
        log.info("[SMS-MQ] → {} : 您有新的多学科会诊待确认【{}】", expertName, consultationTitle);
    }
}
