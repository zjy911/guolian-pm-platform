package com.guolian.pm.auth;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 在线会话（节点内存储；生产多实例时替换为 Redis 实现）。
 */
@Data
public class OnlineSession {

    private String sessionId;
    private Long userId;
    private String username;
    private String realName;
    private Long roleId;
    private String roleName;
    private String orgName;
    private String ip;
    private String browser;
    private String os;
    private LocalDateTime loginTime;
    private LocalDateTime lastActiveTime;
    private boolean impersonate;
}
