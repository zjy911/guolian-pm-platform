package com.guolian.pm.auth;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线会话注册表：支持在线用户列表与强制下线。
 */
@Component
public class OnlineSessionRegistry {

    private final Map<String, OnlineSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userSessionIndex = new ConcurrentHashMap<>();

    public void register(OnlineSession session) {
        sessions.put(session.getSessionId(), session);
        userSessionIndex.put(session.getSessionId(), String.valueOf(session.getUserId()));
    }

    public void touch(String sessionId) {
        OnlineSession session = sessions.get(sessionId);
        if (session != null) {
            session.setLastActiveTime(LocalDateTime.now());
        }
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
        userSessionIndex.remove(sessionId);
    }

    /** 停用/离职/强制下线：踢出该用户全部会话 */
    public List<String> removeByUser(Long userId) {
        List<String> removed = new ArrayList<>();
        sessions.values().removeIf(s -> {
            if (s.getUserId().equals(userId)) {
                removed.add(s.getSessionId());
                return true;
            }
            return false;
        });
        removed.forEach(userSessionIndex::remove);
        return removed;
    }

    public List<OnlineSession> list() {
        return new ArrayList<>(sessions.values());
    }
}
