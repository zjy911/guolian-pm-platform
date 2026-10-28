package com.guolian.pm.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guolian.pm.auth.JwtService;
import com.guolian.pm.auth.OnlineSessionRegistry;
import com.guolian.pm.auth.Perm;
import com.guolian.pm.auth.RoleContext;
import com.guolian.pm.auth.RoleContextService;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.cache.CacheService;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.system.role.entity.SysRole;
import com.guolian.pm.system.role.mapper.SysRoleMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 鉴权 + 功能权限 + 只读/模拟态拦截。
 * 顺序：Token 解析 → 会话有效性 → 角色上下文装载 → 写操作守卫 → 按钮权限校验。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String ROLE_CTX_KEY = "rolectx:";
    private static final long ROLE_CTX_TTL = 300L;

    private final JwtService jwtService;
    private final SysRoleMapper roleMapper;
    private final RoleContextService roleContextService;
    private final CacheService cache;
    private final OnlineSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 防御性清理：容器复用线程，任何残留上下文都会串权限
        AuthContext.clear();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            return reject(response, 401, "未登录或登录已失效");
        }
        LoginUser base;
        try {
            base = jwtService.parse(header.substring(7));
        } catch (BizException e) {
            return reject(response, 401, e.getMessage());
        }
        if (Boolean.TRUE.equals(cache.get("revoked:" + base.getSessionId(), Boolean.class))) {
            return reject(response, 401, "账号已在其他设备被强制下线，请重新登录");
        }

        String key = ROLE_CTX_KEY + base.getUserId() + ":" + base.getRoleId();
        RoleContext ctx = cache.get(key, RoleContext.class);
        if (ctx == null) {
            SysRole role = roleMapper.selectById(base.getRoleId());
            if (role == null || !Integer.valueOf(1).equals(role.getStatus())) {
                return reject(response, 403, "当前角色已停用，请重新登录并选择其他角色");
            }
            ctx = roleContextService.load(base.getUserId(), role);
            cache.put(key, ctx, ROLE_CTX_TTL);
        }

        LoginUser user = new LoginUser();
        user.setUserId(base.getUserId());
        user.setUsername(base.getUsername());
        user.setRealName(base.getRealName());
        user.setAvatar(base.getAvatar());
        user.setSessionId(base.getSessionId());
        user.setRoleId(ctx.roleId());
        user.setRoleCode(ctx.roleCode());
        user.setRoleName(ctx.roleName());
        user.setOrgId(ctx.orgId());
        user.setOrgName(ctx.orgName());
        user.setOrgPath(ctx.orgPath());
        user.setCompanyNames(ctx.companyNames());
        user.setDataScope(ctx.dataScope());
        user.setScopeOrgIds(ctx.scopeOrgIds());
        user.setScopeCompanyIds(ctx.scopeCompanyIds());
        user.setPerms(ctx.perms());
        user.setRoleCodes(ctx.roleCodes());
        user.setReadOnly(base.isReadOnly() || base.isImpersonate());
        user.setImpersonate(base.isImpersonate());
        user.setImpersonateBy(base.getImpersonateBy());
        user.setImpersonateExpire(base.getImpersonateExpire());
        AuthContext.set(user);
        sessionRegistry.touch(base.getSessionId());

        boolean isWrite = !"GET".equalsIgnoreCase(request.getMethod());
        if (isWrite && user.isImpersonate() && !request.getRequestURI().endsWith("/system/debug/exit")) {
            return reject(response, 403, "模拟查看模式下不可执行写操作");
        }
        if (isWrite && base.isReadOnly()) {
            return reject(response, 403, "离职用户为只读状态，不可执行写操作");
        }

        if (handler instanceof HandlerMethod handlerMethod) {
            Perm perm = handlerMethod.getMethodAnnotation(Perm.class);
            if (perm != null) {
                boolean allowed = false;
                for (String code : perm.value()) {
                    if (user.hasPerm(code)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    return reject(response, 403, "没有权限执行该操作");
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContext.clear();
    }

    private boolean reject(HttpServletResponse response, int code, String message) throws Exception {
        // preHandle 返回 false 时容器不会再调用 afterCompletion，必须在这里清理
        AuthContext.clear();
        response.setStatus(code == 401 ? 401 : 403);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(code, message)));
        return false;
    }
}
