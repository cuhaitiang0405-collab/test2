package com.mdt.collab.config;

import com.mdt.collab.signal.CollabWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

/**
 * ③ 协同通讯层 WebSocket 配置：注册信令端点 /ws/collab。
 * 信令仅转发 SDP/ICE/标注增量，媒体走浏览器间 Mesh（生产可换 SFU，见 JoinRoomResponse.sfu_endpoint）。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CollabWebSocketHandler handler;

    public WebSocketConfig(CollabWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 研发态允许跨域；生产经 API 网关同源，且应强制 wss
        registry.addHandler(handler, "/ws/collab").setAllowedOrigins("*");
    }
}
