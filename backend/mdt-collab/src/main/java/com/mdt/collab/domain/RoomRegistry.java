package com.mdt.collab.domain;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存房间注册表（研发态单实例）。
 * 生产扩容见 architecture.md：信令走 Redis Pub/Sub 分片，SFU 媒体独立扩容。
 * roomId -> (userId -> session)
 */
@Component
public class RoomRegistry {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    /** 用户加入房间（重复加入则替换会话） */
    public void join(String room, String user, WebSocketSession session) {
        rooms.computeIfAbsent(room, k -> new ConcurrentHashMap<>()).put(user, session);
    }

    /** 用户离开房间 */
    public void leave(String room, String user) {
        var users = rooms.get(room);
        if (users != null) {
            users.remove(user);
            if (users.isEmpty()) rooms.remove(room);
        }
    }

    /** 取某房间某用户的会话 */
    public WebSocketSession get(String room, String user) {
        var users = rooms.get(room);
        return users == null ? null : users.get(user);
    }

    /** 房间内除自己外的其它会话（信令/广播用） */
    public List<WebSocketSession> others(String room, String user) {
        var users = rooms.get(room);
        if (users == null) return List.of();
        List<WebSocketSession> list = new CopyOnWriteArrayList<>();
        users.forEach((u, s) -> { if (!u.equals(user) && s.isOpen()) list.add(s); });
        return list;
    }

    /** 房间内在线用户（含自己），供迟到加入者拉取已存在对端以发起 offer */
    public Set<String> users(String room) {
        var users = rooms.get(room);
        return users == null ? Set.of() : users.keySet();
    }
}
