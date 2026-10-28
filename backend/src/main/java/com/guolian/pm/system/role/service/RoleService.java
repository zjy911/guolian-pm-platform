package com.guolian.pm.system.role.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guolian.pm.auth.RoleContextService;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.cache.CacheService;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.common.scope.DataScopeService;
import com.guolian.pm.system.menu.entity.SysMenu;
import com.guolian.pm.system.menu.mapper.SysMenuMapper;
import com.guolian.pm.system.org.entity.SysOrg;
import com.guolian.pm.system.org.mapper.SysOrgMapper;
import com.guolian.pm.system.role.dto.RoleDtos;
import com.guolian.pm.system.role.entity.SysDataScopeDept;
import com.guolian.pm.system.role.entity.SysRole;
import com.guolian.pm.system.role.entity.SysRoleFieldMask;
import com.guolian.pm.system.role.entity.SysRoleMenu;
import com.guolian.pm.system.role.mapper.SysDataScopeDeptMapper;
import com.guolian.pm.system.role.mapper.SysRoleFieldMaskMapper;
import com.guolian.pm.system.role.mapper.SysRoleMapper;
import com.guolian.pm.system.role.mapper.SysRoleMenuMapper;
import com.guolian.pm.system.role.vo.RoleVO;
import com.guolian.pm.system.sensitive.entity.SysSensitiveGroup;
import com.guolian.pm.system.sensitive.service.SensitivePolicyService;
import com.guolian.pm.system.user.entity.SysUser;
import com.guolian.pm.system.user.entity.SysUserRole;
import com.guolian.pm.system.user.mapper.SysUserMapper;
import com.guolian.pm.system.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色管理：角色 = 组织 + 功能权限 + 数据范围 + 脱敏策略。
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private static final Map<String, String> DATA_SCOPE_LABELS = Map.of(
            "ALL", "全部数据",
            "ORG_AND_CHILD", "本组织及下级",
            "ORG", "本组织",
            "ORG_CUSTOM", "自定义组织",
            "COMPANY_CUSTOM", "自定义公司主体",
            "SELF", "仅本人",
            "PROJECT_MEMBER", "我参与的项目");

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleFieldMaskMapper roleFieldMaskMapper;
    private final SysDataScopeDeptMapper dataScopeDeptMapper;
    private final SysMenuMapper menuMapper;
    private final SysOrgMapper orgMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SensitivePolicyService sensitivePolicyService;
    private final DataScopeService dataScopeService;
    private final RoleContextService roleContextService;
    private final CacheService cache;
    private final ObjectMapper objectMapper;

    public PageResult<RoleVO> page(RoleDtos.Query query) {
        LambdaQueryWrapper<SysRole> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysRole::getRoleName, kw).or().like(SysRole::getRoleCode, kw));
        }
        if (query.getOrgId() != null) {
            wrapper.eq(SysRole::getOrgId, query.getOrgId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysRole::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getDataScope())) {
            wrapper.eq(SysRole::getDataScope, query.getDataScope());
        }
        wrapper.orderByAsc(SysRole::getSortNo);
        Page<SysRole> page = roleMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(toVOList(page.getRecords()), page.getTotal(), query.safePageNum(), query.safePageSize());
    }

    public List<RoleVO> listAll(Long orgId) {
        LambdaQueryWrapper<SysRole> wrapper = Wrappers.<SysRole>lambdaQuery().eq(SysRole::getStatus, 1);
        if (orgId != null) {
            wrapper.eq(SysRole::getOrgId, orgId);
        }
        wrapper.orderByAsc(SysRole::getSortNo);
        return toVOList(roleMapper.selectList(wrapper));
    }

    public RoleVO detail(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw BizException.notFound("角色不存在");
        }
        RoleVO vo = toVOList(List.of(role)).get(0);
        vo.setMenuIds(menuMapper.selectMenuIdsByRole(id));
        return vo;
    }

    @Transactional
    public Long create(RoleDtos.SaveRequest request) {
        Long exists = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, request.getRoleCode().trim()));
        if (exists != null && exists > 0) {
            throw BizException.biz("角色编码「" + request.getRoleCode() + "」已存在");
        }
        SysOrg org = requireEnabledOrg(request.getOrgId());
        SysRole role = new SysRole();
        copy(request, role);
        role.setOrgId(org.getId());
        role.setScopeValue(toScopeJson(request));
        roleMapper.insert(role);
        return role.getId();
    }

    @Transactional
    public void update(RoleDtos.SaveRequest request) {
        SysRole exists = roleMapper.selectById(request.getId());
        if (exists == null) {
            throw BizException.notFound("角色不存在");
        }
        Long codeCount = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, request.getRoleCode().trim())
                .ne(SysRole::getId, request.getId()));
        if (codeCount != null && codeCount > 0) {
            throw BizException.biz("角色编码「" + request.getRoleCode() + "」已存在");
        }
        SysOrg org = requireEnabledOrg(request.getOrgId());
        if (!org.getId().equals(exists.getOrgId())) {
            int users = roleMapper.countRoleUsers(exists.getId());
            if (users > 0 && !Boolean.TRUE.equals(request.getConfirmOrgChange())) {
                throw BizException.biz("该角色下仍有 " + users + " 名用户，修改绑定组织会同时改变其组织归属与数据可见范围，请二次确认");
            }
        }
        SysRole role = new SysRole();
        role.setId(request.getId());
        copy(request, role);
        role.setOrgId(org.getId());
        role.setScopeValue(toScopeJson(request));
        roleMapper.updateById(role);
        evictRoleCaches(request.getId());
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw BizException.notFound("角色不存在");
        }
        if (Integer.valueOf(1).equals(role.getRoleType())) {
            throw BizException.biz("内置角色不允许删除");
        }
        int users = roleMapper.countRoleUsers(id);
        if (users > 0) {
            throw BizException.biz("该角色仍被 " + users + " 名用户引用，请先解除关联");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
        roleFieldMaskMapper.delete(Wrappers.<SysRoleFieldMask>lambdaQuery().eq(SysRoleFieldMask::getRoleId, id));
        dataScopeDeptMapper.delete(Wrappers.<SysDataScopeDept>lambdaQuery().eq(SysDataScopeDept::getRoleId, id));
        evictRoleCaches(id);
    }

    public List<Long> menuIds(Long roleId) {
        return menuMapper.selectMenuIdsByRole(roleId);
    }

    @Transactional
    public void saveMenus(Long roleId, List<Long> menuIds) {
        requireRole(roleId);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds == null) {
            return;
        }
        Set<Long> unique = new HashSet<>(menuIds);
        if (unique.isEmpty()) {
            return;
        }
        Set<Long> valid = menuMapper.selectBatchIds(unique).stream()
                .map(SysMenu::getId).collect(Collectors.toSet());
        for (Long menuId : unique) {
            if (!valid.contains(menuId)) {
                continue;
            }
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        }
        evictRoleCaches(roleId);
    }

    @Transactional
    public void saveDataScope(Long roleId, RoleDtos.DataScopeRequest request) {
        requireRole(roleId);
        SysRole update = new SysRole();
        update.setId(roleId);
        update.setDataScope(request.getDataScope());
        String json = null;
        if ("ORG_CUSTOM".equals(request.getDataScope())) {
            json = toJson(request.getOrgIds());
        } else if ("COMPANY_CUSTOM".equals(request.getDataScope())) {
            json = toJson(request.getCompanyIds());
        }
        update.setScopeValue(json);
        roleMapper.updateById(update);

        dataScopeDeptMapper.delete(Wrappers.<SysDataScopeDept>lambdaQuery().eq(SysDataScopeDept::getRoleId, roleId));
        if ("ORG_CUSTOM".equals(request.getDataScope()) && request.getOrgIds() != null) {
            for (Long orgId : new HashSet<>(request.getOrgIds())) {
                SysDataScopeDept dept = new SysDataScopeDept();
                dept.setRoleId(roleId);
                dept.setOrgId(orgId);
                dataScopeDeptMapper.insert(dept);
            }
        }
        dataScopeService.evictRoleScope(roleId);
        cache.evictByPrefix("rolectx:");
    }

    public Map<String, String> fieldMasks(Long roleId) {
        Map<String, String> result = new HashMap<>();
        for (SysRoleFieldMask mask : roleFieldMaskMapper.selectList(
                Wrappers.<SysRoleFieldMask>lambdaQuery().eq(SysRoleFieldMask::getRoleId, roleId))) {
            result.put(mask.getGroupCode(), mask.getMaskLevel());
        }
        return result;
    }

    @Transactional
    public void saveFieldMasks(Long roleId, List<RoleDtos.MaskItem> items) {
        requireRole(roleId);
        Map<String, SysSensitiveGroup> groups = sensitivePolicyService.groups();
        if (items != null) {
            for (RoleDtos.MaskItem item : items) {
                if (!groups.containsKey(item.getGroupCode())) {
                    throw BizException.badRequest("字段组不存在：" + item.getGroupCode());
                }
                if (!List.of("PLAIN", "MASKED", "HIDDEN").contains(item.getMaskLevel())) {
                    throw BizException.badRequest("非法档位：" + item.getMaskLevel());
                }
                if ("PLAIN".equals(item.getMaskLevel()) && !sensitivePolicyService.plainAllowed(item.getGroupCode())) {
                    throw BizException.biz("字段组「" + groups.get(item.getGroupCode()).getGroupName()
                            + "」为高风险且不允许明文，不能配置为明文可见");
                }
            }
        }
        roleFieldMaskMapper.delete(Wrappers.<SysRoleFieldMask>lambdaQuery()
                .eq(SysRoleFieldMask::getRoleId, roleId));
        if (items != null) {
            for (RoleDtos.MaskItem item : items) {
                SysRoleFieldMask entity = new SysRoleFieldMask();
                entity.setRoleId(roleId);
                entity.setGroupCode(item.getGroupCode());
                entity.setMaskLevel(item.getMaskLevel());
                roleFieldMaskMapper.insert(entity);
            }
        }
        sensitivePolicyService.evictRole(roleId);
        cache.evictByPrefix("rolectx:");
    }

    public List<Map<String, Object>> users(Long roleId) {
        List<Long> userIds = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getRoleId, roleId))
                .stream().map(SysUserRole::getUserId).toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUser user : userMapper.selectBatchIds(userIds)) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("username", user.getUsername());
            item.put("realName", user.getRealName());
            item.put("status", user.getStatus());
            result.add(item);
        }
        return result;
    }

    // ---------------- 内部 ----------------

    private List<RoleVO> toVOList(List<SysRole> roles) {
        List<RoleVO> result = new ArrayList<>();
        if (roles.isEmpty()) {
            return result;
        }
        Map<Long, Long> userCounts = new HashMap<>();
        for (Map<String, Object> row : userRoleMapper.selectCountGroupByRole()) {
            Long roleId = ((Number) row.get("role_id")).longValue();
            Long cnt = ((Number) row.get("cnt")).longValue();
            userCounts.put(roleId, cnt);
        }
        for (SysRole role : roles) {
            RoleVO vo = new RoleVO();
            vo.setId(role.getId());
            vo.setRoleCode(role.getRoleCode());
            vo.setRoleName(role.getRoleName());
            vo.setOrgId(role.getOrgId());
            vo.setOrgName(roleContextService.orgName(role.getOrgId()));
            vo.setOrgPath(roleContextService.buildOrgPath(role.getOrgId()));
            vo.setDescription(role.getDescription());
            vo.setRoleType(role.getRoleType());
            vo.setDataScope(role.getDataScope());
            vo.setDataScopeLabel(DATA_SCOPE_LABELS.getOrDefault(role.getDataScope(), "仅本人"));
            vo.setScopeValue(role.getScopeValue());
            vo.setCanBeProjectMember(role.getCanBeProjectMember());
            vo.setCanBeApprover(role.getCanBeApprover());
            vo.setSortNo(role.getSortNo());
            vo.setStatus(role.getStatus());
            vo.setRemark(role.getRemark());
            vo.setUserCount(userCounts.getOrDefault(role.getId(), 0L));
            result.add(vo);
        }
        return result;
    }

    private void copy(RoleDtos.SaveRequest request, SysRole role) {
        role.setRoleCode(request.getRoleCode().trim());
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setRoleType(request.getRoleType() == null ? 2 : request.getRoleType());
        role.setDataScope(request.getDataScope());
        role.setCanBeProjectMember(request.getCanBeProjectMember() == null ? 0 : request.getCanBeProjectMember());
        role.setCanBeApprover(request.getCanBeApprover() == null ? 0 : request.getCanBeApprover());
        role.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        role.setRemark(request.getRemark());
    }

    private String toScopeJson(RoleDtos.SaveRequest request) {
        if ("ORG_CUSTOM".equals(request.getDataScope())) {
            return toJson(request.getScopeOrgIds());
        }
        return null;
    }

    private String toJson(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids == null ? List.of() : ids);
        } catch (Exception e) {
            return "[]";
        }
    }

    private SysOrg requireEnabledOrg(Long orgId) {
        if (orgId == null) {
            throw BizException.biz("角色必须绑定一个组织");
        }
        SysOrg org = orgMapper.selectById(orgId);
        if (org == null) {
            throw BizException.badRequest("组织不存在");
        }
        if (!Integer.valueOf(1).equals(org.getStatus())) {
            throw BizException.biz("组织「" + org.getOrgName() + "」已停用，不能绑定角色");
        }
        return org;
    }

    private void requireRole(Long roleId) {
        if (roleMapper.selectById(roleId) == null) {
            throw BizException.notFound("角色不存在");
        }
    }

    private void evictRoleCaches(Long roleId) {
        cache.evictByPrefix("rolectx:");
        dataScopeService.evictRoleScope(roleId);
        sensitivePolicyService.evictRole(roleId);
    }
}
