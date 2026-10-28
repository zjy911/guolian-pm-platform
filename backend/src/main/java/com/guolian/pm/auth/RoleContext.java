package com.guolian.pm.auth;

import java.util.List;
import java.util.Set;

/**
 * 当前角色的完整上下文：组织、权限标识、数据范围。切换角色即重新加载本对象。
 */
public record RoleContext(
        Long roleId,
        String roleCode,
        String roleName,
        Long orgId,
        String orgName,
        String orgPath,
        String companyNames,
        String dataScope,
        List<Long> scopeOrgIds,
        List<Long> scopeCompanyIds,
        Set<String> perms,
        List<String> roleCodes) {
}
