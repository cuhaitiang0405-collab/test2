package com.mdt.collab.signal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdt.collab.adapter.AnnotationRepository;
import com.mdt.collab.adapter.AnnotationSerializer;
import com.mdt.collab.domain.AnnotationEntity;
import com.mdt.collab.domain.RoomRegistry;
import com.mdt.common.audit.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;

/**
 * ③ 协同通讯层 WebSocket 信令处理器。
 * 职责：房间管理、SDP/ICE 中继（Mesh）、白板标注实时广播 + 持久化、桌面共享状态广播。
 * 媒体本身不经本服务（浏览器间直连）；本服务只转发信令与标注增量。
 */
@Component
public class CollabWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CollabWebSocketHandler.class);
    private final ObjectMapper om = new ObjectMapper();
    private final RoomRegistry registry;
    private final AnnotationRepository annotations;
    private final AuditLogger audit;

    public CollabWebSocketHandler(RoomRegistry registry, AnnotationRepository annotations, AuditLogger audit) {
        this.registry = registry;
        this.annotations = annotations;
        this.audit = audit;
    }

    // —— 连接建立：从 query 取身份并登记房间 ——
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        var q = parseQuery(session.getUri().getQuery());
        String token = q.get("token");
        String user = q.getOrDefault("user", "anon");
        String room = q.getOrDefault("room", "default");
        String pvuid = q.getOrDefault("patientVisitUid", "");
        String tenant = q.getOrDefault("tenant", "T001");

        if (token == null || token.isBlank()) {
            try { session.close(CloseStatus.NOT_ACCEPTABLE.withReason("missing token")); } catch (Exception ignored) {}
            return;
        }
        session.getAttributes().put("user", user);
        session.getAttributes().put("room", room);
        session.getAttributes().put("patientVisitUid", pvuid);
        session.getAttributes().put("tenant", tenant);

        registry.join(room, user, session);
        audit.log(tenant, user, pvuid.isEmpty() ? null : pvuid, "COLLAB_JOIN", "room=" + room);
        log.info("[WS] {} 加入房间 {} (在线 {})", user, room, registry.users(room));

        // 通知自己：当前房间内已有对端（用于主动发起 offer）
        send(session, Map.of("type", "welcome", "you", user, "room", room, "peers", new ArrayList<>(registry.users(room))));
        // 通知他人：有新对端加入
        broadcast(room, user, Map.of("type", "peer-join", "user", user, "peers", new ArrayList<>(registry.users(room))));
    }

    // —— 消息路由 ——
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode root = om.readTree(message.getPayload());
        String type = root.path("type").asText("");
        String room = (String) session.getAttributes().get("room");
        String from = (String) session.getAttributes().get("user");

        switch (type) {
            case "offer":
            case "answer":
            case "ice": {
                // 定向中继给 to 指定对端（from 以服务端登记身份为准，防伪造）
                String to = root.path("to").asText("");
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("type", type);
                out.put("from", from);
                out.put("to", to);
                if (root.has("sdp")) out.put("sdp", root.get("sdp"));
                if (root.has("candidate")) out.put("candidate", root.get("candidate"));
                relayTo(room, to, out);
                break;
            }
            case "draw": {
                // 实时广播 + 持久化（服务端回填版本号，保证回放顺序）
                JsonNode op = root.path("op");
                int version = annotations.countByConsultationId(room) + 1;
                String payload = om.writeValueAsString(op);
                AnnotationEntity saved = annotations.save(
                        new AnnotationEntity(room, version, payload, from, System.currentTimeMillis()));
                audit.log((String) session.getAttributes().get("tenant"), from,
                        (String) session.getAttributes().get("patientVisitUid"), "COLLAB_ANNOTATE",
                        "room=" + room + " version=" + version);
                // 回放载荷带服务端版本
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("type", "draw");
                out.put("op", om.readTree(saved.getPayload()));
                out.put("version", version);
                out.put("author", from);
                broadcast(room, from, out);
                break;
            }
            case "cursor": {
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("type", "cursor");
                out.put("from", from);
                out.put("x", root.path("x").asDouble());
                out.put("y", root.path("y").asDouble());
                broadcast(room, from, out);
                break;
            }
            case "share": {
                boolean on = root.path("on").asBoolean();
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("type", "share");
                out.put("from", from);
                out.put("on", on);
                broadcast(room, from, out);
                break;
            }
            case "join": {
                // 连接时已登记；此处幂等，仅广播当前成员
                broadcast(room, from, Map.of("type", "peer-join", "user", from, "peers", new ArrayList<>(registry.users(room))));
                break;
            }
            default:
                log.warn("[WS] 未知消息类型 {}", type);
        }
    }

    // —— 断开 / 异常：退房并通知他人 ——
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        leave(session);
    }
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        leave(session);
    }
    private void leave(WebSocketSession session) {
        String room = (String) session.getAttributes().get("room");
        String user = (String) session.getAttributes().get("user");
        if (room == null || user == null) return;
        registry.leave(room, user);
        log.info("[WS] {} 离开房间 {}", user, room);
        broadcast(room, user, Map.of("type", "peer-leave", "user", user, "peers", new ArrayList<>(registry.users(room))));
    }

    // —— 工具 ——
    private void broadcast(String room, String exceptUser, Map<String, Object> msg) {
        for (WebSocketSession s : registry.others(room, exceptUser)) {
            send(s, msg);
        }
    }
    private void relayTo(String room, String toUser, Map<String, Object> msg) {
        WebSocketSession s = registry.get(room, toUser);
        if (s != null && s.isOpen()) send(s, msg);
    }
    private void send(WebSocketSession s, Object msg) {
        try { s.sendMessage(new TextMessage(om.writeValueAsString(msg))); }
        catch (Exception e) { log.debug("[WS] 发送失败 {}", e.getMessage()); }
    }
    private Map<String, String> parseQuery(String query) {
        Map<String, String> m = new LinkedHashMap<>();
        if (query == null) return m;
        for (String kv : query.split("&")) {
            String[] p = kv.split("=", 2);
            m.put(p[0], p.length > 1 ? p[1] : "");
        }
        return m;
    }
}
