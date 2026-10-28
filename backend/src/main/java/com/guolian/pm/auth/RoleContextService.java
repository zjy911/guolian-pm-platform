package com.guolian.pm.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.common.scope.DataScopeService;
import com.guolian.pm.common.scope.ScopeInfo;
import com.guolian.pm.system.company.entity.SysCompany;
import com.guolian.pm.system.company.mapper.SysCompanyMapper;
import com.guolian.pm.system.menu.mapper.SysMenuMapper;
import com.guolian.pm.system.org.entity.SysOrg;
import com.guolian.pm.system.role.entity.SysRole;
import com.guolian.pm.system.role.mapper.SysRoleMapper;
import com.guolian.pm.system.user.entity.SysUserCompany;
import com.guolian.pm.system.user.mapper.SysUserCompanyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色上下文装载：角色 → 组织 / 权限标识 / 数据范围 / 所属公司主体。
 */
@Service
@RequiredArgsConstructor
public class RoleContextService {

    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysCompanyMapper companyMapper;
    private final SysUserCompanyMapper userCompanyMapper;
    private final DataScopeService dataScopeService;

    public RoleContext load(Long userId, SysRole role) {
        ScopeInfo scope = dataScopeService.resolve(role);
        Set<String> perms = new HashSet<>(menuMapper.selectPermsByRole(role.getId()));
        List<String> roleCodes = roleMapper.selectEnabledRolesByUser(userId).stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());
        String orgPath = buildOrgPath(role.getOrgId());
        return new RoleContext(
                role.getId(), role.getRoleCode(), role.getRoleName(),
                role.getOrgId(), orgName(role.getOrgId()), orgPath,
                companyNames(userId),
                role.getDataScope(), scope.getOrgIds(), scope.getCompanyIds(),
                perms, roleCodes);
    }

    public String orgName(Long orgId) {
        return dataScopeService.allOrgs().stream()
                .filter(o -> o.getId().equals(orgId))
                .map(SysOrg::getOrgName)
                .findFirst()
                .orElse("未分配组织");
    }

    public String buildOrgPath(Long orgId) {
        List<SysOrg> all = dataScopeService.allOrgs();
        Map<Long, SysOrg> map = new LinkedHashMap<>();
        for (SysOrg org : all) {
            map.put(org.getId(), org);
        }
        List<String> names = new ArrayList<>();
        SysOrg current = map.get(orgId);
        int guard = 0;
        while (current != null && guard++ < 20) {
            names.add(0, current.getOrgName());
            current = map.get(current.getParentId());
        }
        return String.join(" / ", names);
    }

    private String companyNames(Long userId) {
        List<SysUserCompany> relations = userCompanyMapper.selectList(Wrappers.<SysUserCompany>lambdaQuery()
                .eq(SysUserCompany::getUserId, userId)
                .eq(SysUserCompany::getRelationType, 1));
        if (relations.isEmpty()) {
            return "";
        }
        List<Long> ids = relations.stream().map(SysUserCompany::getCompanyId).toList();
        return companyMapper.selectBatchIds(ids).stream()
                .map(SysCompany::getShortName)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.joining("、"));
    }
}
