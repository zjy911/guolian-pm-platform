package com.guolian.pm.common.web;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 当前登录上下文：由 Token + 当前角色共同决定。
 * 组织、菜单、按钮、数据范围、脱敏策略全部同源派生自 currentRoleId。
 */
@Data
public class LoginUser {

    private Long userId;
    private String username;
    private String realName;
    private String avatar;

    /** 当前角色（同时决定组织） */
    private Long roleId;
    private String roleCode;
    private String roleName;
    private Long orgId;
    private String orgName;
    private String orgPath;
    private String companyNames;

    /** 数据范围类型 + 已展开的组织 ID 集合（SELF/ALL 时为空） */
    private String dataScope;
    private List<Long> scopeOrgIds = new ArrayList<>();
    /** 自定义公司主体范围（COMPANY_CUSTOM 时生效） */
    private List<Long> scopeCompanyIds = new ArrayList<>();

    /** 按钮级权限标识 */
    private Set<String> perms = new HashSet<>();

    /** 该用户全部可用角色编码，用于 Flowable candidateGroups */
    private List<String> roleCodes = new ArrayList<>();

    /** 离职只读态 */
    private boolean readOnly;

    /** 超管模拟态 */
    private boolean impersonate;
    private String impersonateBy;
    private LocalDateTime impersonateExpire;

    private String sessionId;

    public boolean hasPerm(String perm) {
        return perms.contains("*:*:*") || perms.contains(perm);
    }
}
