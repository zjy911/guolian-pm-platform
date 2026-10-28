package com.guolian.pm.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录 / 切换角色的统一返回体：Token + 身份 + 菜单 + 可用角色。
 */
@Data
@Builder
public class AuthResult {

    private String token;

    private Profile profile;

    @Builder.Default
    private List<MenuVO> menus = new ArrayList<>();

    @Builder.Default
    private List<RoleOptionVO> roles = new ArrayList<>();

    /** Topbar 待办角标 */
    private long todoCount;

    /** 顶栏铃铛未读数 */
    private long unreadCount;

    @Data
    @Builder
    public static class Profile {
        private Long userId;
        private String username;
        private String realName;
        private String avatar;
        private Long roleId;
        private String roleCode;
        private String roleName;
        private Long orgId;
        private String orgName;
        private String orgPath;
        private String companyNames;
        private String dataScope;
        private String dataScopeLabel;
        private boolean impersonate;
        private String impersonateBy;
        private boolean readOnly;
        private boolean firstLogin;
        private List<String> perms;
    }

    @Data
    @Builder
    public static class MenuVO {
        private Long id;
        private Long parentId;
        private String name;
        private String path;
        private String component;
        private String icon;
        private Integer type;
        private String perms;
        @Builder.Default
        private List<MenuVO> children = new ArrayList<>();
    }

    @Data
    @Builder
    public static class RoleOptionVO {
        private Long roleId;
        private String roleCode;
        private String roleName;
        private Long orgId;
        private String orgName;
        private String dataScope;
        private String dataScopeLabel;
        private boolean defaultRole;
        private boolean current;
    }
}
