package com.guolian.pm.system.user.dto;

import lombok.Data;

/**
 * 用户角色摘要（列表角色列用，含组织名）。一次查询批量装配，避免 N+1。
 */
@Data
public class UserRoleBrief {

    private Long userId;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private Long orgId;
    private String orgName;
    private Integer isDefault;
    private String dataScope;
}
