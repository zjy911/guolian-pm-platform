package com.guolian.pm.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.auth.dto.AuthDtos;
import com.guolian.pm.auth.dto.AuthResult;
import com.guolian.pm.common.cache.CacheService;
import com.guolian.pm.common.config.AppProperties;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.common.web.LoginUser;
import com.guolian.pm.system.log.entity.SysLoginLog;
import com.guolian.pm.system.log.mapper.SysLoginLogMapper;
import com.guolian.pm.system.menu.entity.SysMenu;
import com.guolian.pm.system.menu.mapper.SysMenuMapper;
import com.guolian.pm.system.notice.service.BadgeService;
import com.guolian.pm.system.role.entity.SysRole;
import com.guolian.pm.system.role.mapper.SysRoleMapper;
import com.guolian.pm.system.user.entity.SysUser;
import com.guolian.pm.system.user.entity.SysUserRole;
import com.guolian.pm.system.user.mapper.SysUserMapper;
import com.guolian.pm.system.user.mapper.SysUserRoleMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 认证与身份：登录、切换角色（切组织）、模拟视角、改密、登出。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String LOGIN_FAIL_KEY = "login:fail:";

    private static final Map<String, String> DATA_SCOPE_LABELS = new LinkedHashMap<>();

    static {
        DATA_SCOPE_LABELS.put("ALL", "全部数据");
        DATA_SCOPE_LABELS.put("ORG_AND_CHILD", "本组织及下级");
        DATA_SCOPE_LABELS.put("ORG", "本组织");
        DATA_SCOPE_LABELS.put("ORG_CUSTOM", "自定义组织");
        DATA_SCOPE_LABELS.put("COMPANY_CUSTOM", "自定义公司主体");
        DATA_SCOPE_LABELS.put("SELF", "仅本人");
        DATA_SCOPE_LABELS.put("PROJECT_MEMBER", "我参与的项目");
    }

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final RoleContextService roleContextService;
    private final JwtService jwtService;
    private final CacheService cache;
    private final AppProperties properties;
    private final OnlineSessionRegistry sessionRegistry;
    private final SysLoginLogMapper loginLogMapper;
    private final BadgeService badgeService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResult login(AuthDtos.LoginRequest request, HttpServletRequest servletRequest) {
        String username = request.getUsername().trim();
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            writeLoginLog(username, null, "LOGIN", servletRequest, false, "账号不存在");
            throw BizException.biz("账号或密码错误");
        }
        long failCount = failCount(username);
        if (failCount >= properties.getSecurity().getLoginFailLockCount()) {
            writeLoginLog(username, user.getId(), "LOGIN", servletRequest, false, "账号已锁定");
            throw BizException.biz("密码连续错误 " + properties.getSecurity().getLoginFailLockCount()
                    + " 次，账号已锁定 " + properties.getSecurity().getLoginFailLockMinutes() + " 分钟");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            increaseFail(username);
            writeLoginLog(username, user.getId(), "LOGIN", servletRequest, false, "密码错误");
            throw BizException.biz("账号或密码错误");
        }
        if (Integer.valueOf(2).equals(user.getStatus())) {
            writeLoginLog(username, user.getId(), "LOGIN", servletRequest, false, "账号已停用");
            throw BizException.biz("账号已停用，请联系管理员");
        }
        if (Integer.valueOf(4).equals(user.getStatus())) {
            writeLoginLog(username, user.getId(), "LOGIN", servletRequest, false, "账号已锁定");
            throw BizException.biz("账号已锁定，请联系管理员");
        }
        clearFail(username);

        SysRole role = resolveLoginRole(user);
        LoginUser loginUser = new LoginUser();
        applyRole(loginUser, user, role, UUID.randomUUID().toString().replace("-", ""));
        loginUser.setReadOnly(Integer.valueOf(3).equals(user.getStatus()));

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        registerSession(loginUser, user, servletRequest);
        writeLoginLog(username, user.getId(), "LOGIN", servletRequest, true, null);
        return buildResult(loginUser, role, user);
    }

    /**
     * 切换角色 = 切组织 + 切全部权限，一步完成，无需重新登录。
     */
    public AuthResult switchRole(Long roleId) {
        LoginUser current = AuthContext.require();
        SysUserRole relation = userRoleMapper.selectOne(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, current.getUserId())
                .eq(SysUserRole::getRoleId, roleId));
        if (relation == null) {
            throw BizException.forbidden("当前账号未分配该角色");
        }
        SysRole role = roleMapper.selectById(roleId);
        if (role == null || !Integer.valueOf(1).equals(role.getStatus())) {
            throw BizException.biz("该角色已停用，无法切换");
        }
        SysUser user = userMapper.selectById(current.getUserId());
        LoginUser next = new LoginUser();
        applyRole(next, user, role, current.getSessionId());
        next.setReadOnly(current.isReadOnly());
        next.setImpersonate(current.isImpersonate());
        next.setImpersonateBy(current.getImpersonateBy());
        next.setImpersonateExpire(current.getImpersonateExpire());

        writeLoginLog(user.getUsername(), user.getId(), "SWITCH_ROLE", null, true,
                "切换为：" + role.getRoleName());
        return buildResult(next, role, user);
    }

    public AuthResult impersonate(Long targetUserId) {
        if (!properties.getDebug().isImpersonateEnabled()) {
            throw BizException.forbidden("当前环境未开启模拟查看");
        }
        LoginUser current = AuthContext.require();
        if (!current.hasPerm("system:debug:impersonate")) {
            throw BizException.forbidden("没有模拟查看权限");
        }
        if (current.isImpersonate()) {
            throw BizException.biz("已处于模拟查看状态，请先退出");
        }
        SysUser target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw BizException.notFound("目标用户不存在");
        }
        SysRole role = resolveLoginRole(target);
        LoginUser mock = new LoginUser();
        applyRole(mock, target, role, UUID.randomUUID().toString().replace("-", ""));
        mock.setImpersonate(true);
        mock.setImpersonateBy(current.getRealName());
        mock.setImpersonateExpire(LocalDateTime.now().plusHours(properties.getJwt().getImpersonateMaxHours()));
        mock.setReadOnly(true);
        writeLoginLog(target.getUsername(), target.getId(), "IMPERSONATE", null, true,
                "模拟人：" + current.getRealName());
        return buildResult(mock, role, target);
    }

    public AuthResult userInfo() {
        LoginUser current = AuthContext.require();
        SysUser user = userMapper.selectById(current.getUserId());
        SysRole role = roleMapper.selectById(current.getRoleId());
        return buildResult(current, role, user);
    }

    public List<AuthResult.MenuVO> menus() {
        LoginUser current = AuthContext.require();
        return buildMenus(current.getRoleId());
    }

    public List<AuthResult.RoleOptionVO> myRoles() {
        LoginUser current = AuthContext.require();
        return buildRoleOptions(current.getUserId(), current.getRoleId(), userMapper.selectById(current.getUserId()));
    }

    @Transactional
    public void changePassword(AuthDtos.ChangePasswordRequest request) {
        LoginUser current = AuthContext.require();
        SysUser user = userMapper.selectById(current.getUserId());
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw BizException.biz("原密码不正确");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw BizException.biz("新密码不能与原密码相同");
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        update.setFirstLogin(0);
        userMapper.updateById(update);
    }

    public void logout() {
        LoginUser current = AuthContext.get();
        if (current == null) {
            return;
        }
        sessionRegistry.remove(current.getSessionId());
        writeLoginLog(current.getUsername(), current.getUserId(), "LOGOUT", null, true, null);
    }

    @Transactional
    public void forceLogout(String sessionId) {
        sessionRegistry.remove(sessionId);
        cache.put("revoked:" + sessionId, Boolean.TRUE, properties.getJwt().getAccessTokenMinutes() * 60L);
    }

    public List<AuthResult.MenuVO> buildMenus(Long roleId) {
        List<SysMenu> flat = menuMapper.selectMenuByRole(roleId);
        Map<Long, AuthResult.MenuVO> nodes = new LinkedHashMap<>();
        for (SysMenu menu : flat) {
            nodes.put(menu.getId(), AuthResult.MenuVO.builder()
                    .id(menu.getId())
                    .parentId(menu.getParentId())
                    .name(menu.getMenuName())
                    .path(menu.getPath())
                    .component(menu.getComponent())
                    .icon(menu.getIcon())
                    .type(menu.getMenuType())
                    .perms(menu.getPerms())
                    .build());
        }
        List<AuthResult.MenuVO> roots = new ArrayList<>();
        flat.sort(Comparator.comparing(SysMenu::getSortNo));
        for (SysMenu menu : flat) {
            AuthResult.MenuVO node = nodes.get(menu.getId());
            AuthResult.MenuVO parent = nodes.get(menu.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            } else if (menu.getParentId() == null || menu.getParentId() == 0) {
                roots.add(node);
            }
        }
        return roots;
    }

    private void applyRole(LoginUser loginUser, SysUser user, SysRole role, String sessionId) {
        RoleContext ctx = roleContextService.load(user.getId(), role);
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setRoleId(ctx.roleId());
        loginUser.setRoleCode(ctx.roleCode());
        loginUser.setRoleName(ctx.roleName());
        loginUser.setOrgId(ctx.orgId());
        loginUser.setOrgName(ctx.orgName());
        loginUser.setOrgPath(ctx.orgPath());
        loginUser.setCompanyNames(ctx.companyNames());
        loginUser.setDataScope(ctx.dataScope());
        loginUser.setScopeOrgIds(ctx.scopeOrgIds());
        loginUser.setScopeCompanyIds(ctx.scopeCompanyIds());
        loginUser.setPerms(ctx.perms());
        loginUser.setRoleCodes(ctx.roleCodes());
        loginUser.setSessionId(sessionId);
    }

    private AuthResult buildResult(LoginUser loginUser, SysRole role, SysUser user) {
        List<AuthResult.MenuVO> menus = buildMenus(loginUser.getRoleId());
        List<AuthResult.RoleOptionVO> roles = buildRoleOptions(loginUser.getUserId(), loginUser.getRoleId(), user);
        BadgeService.Badge badge;
        try {
            badge = badgeService.badge(loginUser);
        } catch (Exception e) {
            log.warn("角标统计失败：{}", e.getMessage());
            badge = new BadgeService.Badge(0, 0);
        }
        AuthResult.Profile profile = AuthResult.Profile.builder()
                .userId(loginUser.getUserId())
                .username(loginUser.getUsername())
                .realName(loginUser.getRealName())
                .avatar(loginUser.getAvatar())
                .roleId(loginUser.getRoleId())
                .roleCode(loginUser.getRoleCode())
                .roleName(loginUser.getRoleName())
                .orgId(loginUser.getOrgId())
                .orgName(loginUser.getOrgName())
                .orgPath(loginUser.getOrgPath())
                .companyNames(loginUser.getCompanyNames())
                .dataScope(loginUser.getDataScope())
                .dataScopeLabel(DATA_SCOPE_LABELS.getOrDefault(loginUser.getDataScope(), "仅本人"))
                .impersonate(loginUser.isImpersonate())
                .impersonateBy(loginUser.getImpersonateBy())
                .readOnly(loginUser.isReadOnly())
                .firstLogin(user != null && Integer.valueOf(1).equals(user.getFirstLogin()))
                .perms(new ArrayList<>(loginUser.getPerms()))
                .build();
        return AuthResult.builder()
                .token(jwtService.createToken(loginUser))
                .profile(profile)
                .menus(menus)
                .roles(roles)
                .todoCount(badge.todoCount())
                .unreadCount(badge.unreadCount())
                .build();
    }

    private List<AuthResult.RoleOptionVO> buildRoleOptions(Long userId, Long currentRoleId, SysUser user) {
        List<SysRole> roles = roleMapper.selectEnabledRolesByUser(userId);
        List<AuthResult.RoleOptionVO> options = new ArrayList<>();
        for (SysRole role : roles) {
            options.add(AuthResult.RoleOptionVO.builder()
                    .roleId(role.getId())
                    .roleCode(role.getRoleCode())
                    .roleName(role.getRoleName())
                    .orgId(role.getOrgId())
                    .orgName(roleContextService.orgName(role.getOrgId()))
                    .dataScope(role.getDataScope())
                    .dataScopeLabel(DATA_SCOPE_LABELS.getOrDefault(role.getDataScope(), "仅本人"))
                    .defaultRole(user != null && role.getId().equals(user.getDefaultRoleId()))
                    .current(role.getId().equals(currentRoleId))
                    .build());
        }
        return options;
    }

    private SysRole resolveLoginRole(SysUser user) {
        List<SysRole> roles = roleMapper.selectEnabledRolesByUser(user.getId());
        if (roles.isEmpty()) {
            throw BizException.biz("该账号未分配可用角色，请联系管理员");
        }
        if (user.getDefaultRoleId() != null) {
            for (SysRole role : roles) {
                if (role.getId().equals(user.getDefaultRoleId())) {
                    return role;
                }
            }
        }
        // 默认角色已停用：回退到第一个可用角色
        return roles.get(0);
    }

    private void registerSession(LoginUser loginUser, SysUser user, HttpServletRequest request) {
        OnlineSession session = new OnlineSession();
        session.setSessionId(loginUser.getSessionId());
        session.setUserId(user.getId());
        session.setUsername(user.getUsername());
        session.setRealName(user.getRealName());
        session.setRoleId(loginUser.getRoleId());
        session.setRoleName(loginUser.getRoleName());
        session.setOrgName(loginUser.getOrgName());
        session.setLoginTime(LocalDateTime.now());
        session.setLastActiveTime(LocalDateTime.now());
        session.setImpersonate(loginUser.isImpersonate());
        if (request != null) {
            session.setIp(clientIp(request));
            session.setBrowser(request.getHeader("User-Agent"));
            session.setOs(request.getHeader("User-Agent"));
        }
        sessionRegistry.register(session);
    }

    private void writeLoginLog(String username, Long userId, String type, HttpServletRequest request,
                               boolean success, String reason) {
        SysLoginLog logEntity = new SysLoginLog();
        logEntity.setUsername(username);
        logEntity.setUserId(userId);
        logEntity.setLoginType(type);
        logEntity.setSuccess(success ? 1 : 0);
        logEntity.setFailReason(reason);
        if (request != null) {
            logEntity.setIp(clientIp(request));
            logEntity.setBrowser(shortAgent(request.getHeader("User-Agent")));
            logEntity.setOs("Web");
            logEntity.setLocation("内网");
        }
        try {
            loginLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.warn("登录日志写入失败：{}", e.getMessage());
        }
    }

    private long failCount(String username) {
        Integer count = cache.get(LOGIN_FAIL_KEY + username, Integer.class);
        return count == null ? 0 : count;
    }

    private void increaseFail(String username) {
        long count = failCount(username) + 1;
        cache.put(LOGIN_FAIL_KEY + username, (int) count,
                properties.getSecurity().getLoginFailLockMinutes() * 60L);
    }

    private void clearFail(String username) {
        cache.evict(LOGIN_FAIL_KEY + username);
    }

    public static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value) && !"unknown".equalsIgnoreCase(value)) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String shortAgent(String agent) {
        if (!StringUtils.hasText(agent)) {
            return "unknown";
        }
        return agent.length() > 80 ? agent.substring(0, 80) : agent;
    }
}
