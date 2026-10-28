package com.guolian.pm.system.debug.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.auth.AuthService;
import com.guolian.pm.auth.Perm;
import com.guolian.pm.auth.dto.AuthDtos;
import com.guolian.pm.auth.dto.AuthResult;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.config.AppProperties;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.common.scope.DataScopeRegistry;
import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.common.web.LoginUser;
import com.guolian.pm.system.user.entity.SysUser;
import com.guolian.pm.system.user.mapper.SysUserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 超管调试：模拟用户视角、数据权限诊断。PROD 默认关闭。
 */
@RestController
@RequestMapping("/api/v1/system/debug")
@RequiredArgsConstructor
public class DebugController {

    private final AuthService authService;
    private final SysUserMapper userMapper;
    private final AppProperties properties;

    @PostMapping("/impersonate")
    @Perm("system:debug:impersonate")
    @OpLog(module = "调试", action = "模拟用户视角")
    public ApiResponse<AuthResult> impersonate(@Valid @RequestBody AuthDtos.ImpersonateRequest request) {
        return ApiResponse.ok(authService.impersonate(request.getUserId()));
    }

    /**
     * 退出模拟：拦截器放行该路径（模拟态下唯一允许的写操作），前端恢复原 Token 即可。
     */
    @PostMapping("/exit")
    public ApiResponse<Void> exit() {
        return ApiResponse.ok();
    }

    @GetMapping("/data-scope-sql")
    @Perm("system:debug:impersonate")
    public ApiResponse<Map<String, Object>> dataScopeSql() {
        LoginUser user = AuthContext.require();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roleId", user.getRoleId());
        result.put("roleName", user.getRoleName());
        result.put("dataScope", user.getDataScope());
        result.put("scopeOrgIds", user.getScopeOrgIds());
        result.put("scopeCompanyIds", user.getScopeCompanyIds());
        result.put("condition", buildCondition(user));
        Long visible = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery());
        result.put("visibleUserCount", visible);
        result.put("registeredTables", DataScopeRegistry.all());
        result.put("impersonateEnabled", properties.getDebug().isImpersonateEnabled());
        return ApiResponse.ok(result);
    }

    private String buildCondition(LoginUser user) {
        if ("ALL".equals(user.getDataScope())) {
            return "1 = 1（全部数据，超管绕过，日志标记 dataScope=bypass）";
        }
        if ("SELF".equals(user.getDataScope()) || "PROJECT_MEMBER".equals(user.getDataScope())) {
            return "id = " + user.getUserId();
        }
        if (user.getScopeOrgIds() != null && !user.getScopeOrgIds().isEmpty()) {
            return "id in (select ur.user_id from sys_user_role ur join sys_role r on r.id = ur.role_id"
                    + " where r.org_id in (" + user.getScopeOrgIds() + "))";
        }
        if (user.getScopeCompanyIds() != null && !user.getScopeCompanyIds().isEmpty()) {
            return "id in (select uc.user_id from sys_user_company uc where uc.company_id in ("
                    + user.getScopeCompanyIds() + "))";
        }
        return "1 = 0（未匹配到有效范围，默认不可见）";
    }
}
