package com.guolian.pm.system.online.controller;

import com.guolian.pm.auth.OnlineSession;
import com.guolian.pm.auth.OnlineSessionRegistry;
import com.guolian.pm.auth.AuthService;
import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.log.OpLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * 在线用户：会话编号、账号、当前角色与组织、IP、登录时间、最后活跃，支持强制下线。
 */
@RestController
@RequestMapping("/api/v1/system/online")
@RequiredArgsConstructor
public class OnlineController {

    private final OnlineSessionRegistry sessionRegistry;
    private final AuthService authService;

    @GetMapping
    @Perm("system:online:list")
    public ApiResponse<List<OnlineSession>> list() {
        List<OnlineSession> sessions = sessionRegistry.list();
        sessions.sort(Comparator.comparing(OnlineSession::getLoginTime).reversed());
        return ApiResponse.ok(sessions);
    }

    @PostMapping("/{sessionId}/logout")
    @Perm("system:online:list")
    @OpLog(module = "在线用户", action = "强制下线")
    public ApiResponse<Void> forceLogout(@PathVariable String sessionId) {
        authService.forceLogout(sessionId);
        return ApiResponse.ok();
    }
}
