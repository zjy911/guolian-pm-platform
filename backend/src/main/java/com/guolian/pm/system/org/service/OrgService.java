package com.guolian.pm.system.org.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.common.scope.DataScopeService;
import com.guolian.pm.system.org.dto.OrgDtos;
import com.guolian.pm.system.org.entity.SysOrg;
import com.guolian.pm.system.org.mapper.SysOrgMapper;
import com.guolian.pm.system.org.vo.OrgVO;
import com.guolian.pm.system.role.entity.SysRole;
import com.guolian.pm.system.role.mapper.SysRoleMapper;
import com.guolian.pm.system.user.entity.SysUser;
import com.guolian.pm.system.user.entity.SysUserRole;
import com.guolian.pm.system.user.mapper.SysUserMapper;
import com.guolian.pm.system.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 组织架构：只有一棵树，节点不挂公司主体，但会被角色绑定。
 */
@Service
@RequiredArgsConstructor
public class OrgService {

    private final SysOrgMapper orgMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMapper userMapper;
    private final DataScopeService dataScopeService;

    public List<OrgVO> tree(Boolean onlyEnabled) {
        List<SysOrg> all = orgMapper.selectList(Wrappers.<SysOrg>lambdaQuery()
                .orderByAsc(SysOrg::getSortNo));
        Map<Long, Long> roleCount = countsByOrg();
        Map<Long, OrgVO> nodes = new LinkedHashMap<>();
        for (SysOrg org : all) {
            if (Boolean.TRUE.equals(onlyEnabled) && !Integer.valueOf(1).equals(org.getStatus())) {
                continue;
            }
            OrgVO vo = toVO(org, roleCount.getOrDefault(org.getId(), 0L));
            nodes.put(org.getId(), vo);
        }
        List<OrgVO> roots = new ArrayList<>();
        for (SysOrg org : all) {
            OrgVO node = nodes.get(org.getId());
            if (node == null) {
                continue;
            }
            OrgVO parent = nodes.get(org.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            } else if (org.getParentId() == null || org.getParentId() == 0) {
                roots.add(node);
            }
        }
        sortRecursively(roots);
        return roots;
    }

    public OrgVO detail(Long id) {
        SysOrg org = orgMapper.selectById(id);
        if (org == null) {
            throw BizException.notFound("组织不存在");
        }
        OrgVO vo = toVO(org, countsByOrg().getOrDefault(id, 0L));
        List<SysRole> roles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getOrgId, id));
        List<Map<String, Object>> roleItems = new ArrayList<>();
        Set<Long> userIds = new HashSet<>();
        for (SysRole role : roles) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", role.getId());
            item.put("roleCode", role.getRoleCode());
            item.put("roleName", role.getRoleName());
            item.put("dataScope", role.getDataScope());
            item.put("status", role.getStatus());
            List<SysUserRole> relations = userRoleMapper.selectList(
                    Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getRoleId, role.getId()));
            item.put("userCount", relations.size());
            relations.forEach(r -> userIds.add(r.getUserId()));
            roleItems.add(item);
        }
        vo.setRoles(roleItems);
        List<Map<String, Object>> userItems = new ArrayList<>();
        if (!userIds.isEmpty()) {
            List<SysUser> users = userMapper.selectBatchIds(userIds);
            for (SysUser user : users) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", user.getId());
                item.put("username", user.getUsername());
                item.put("realName", user.getRealName());
                item.put("status", user.getStatus());
                item.put("employeeNo", user.getEmployeeNo());
                userItems.add(item);
            }
        }
        vo.setUsers(userItems);
        vo.setUserCount(userItems.size());
        return vo;
    }

    @Transactional
    public Long create(OrgDtos.SaveRequest request) {
        Long codeExists = orgMapper.selectCount(Wrappers.<SysOrg>lambdaQuery()
                .eq(SysOrg::getOrgCode, request.getOrgCode().trim()));
        if (codeExists != null && codeExists > 0) {
            throw BizException.biz("组织编码「" + request.getOrgCode() + "」已存在");
        }
        Long parentId = request.getParentId() == null ? 0L : request.getParentId();
        SysOrg parent = null;
        if (parentId != 0L) {
            parent = orgMapper.selectById(parentId);
            if (parent == null) {
                throw BizException.badRequest("上级组织不存在");
            }
        }
        checkSameLevelName(request.getOrgName(), parentId, null);

        SysOrg org = new SysOrg();
        copy(request, org);
        org.setParentId(parentId);
        org.setAncestors(parent == null ? "0" : parent.getAncestors() + "," + parent.getId());
        orgMapper.insert(org);
        dataScopeService.evictOrgCache();
        return org.getId();
    }

    @Transactional
    public void update(OrgDtos.SaveRequest request) {
        SysOrg exists = orgMapper.selectById(request.getId());
        if (exists == null) {
            throw BizException.notFound("组织不存在");
        }
        Long codeCount = orgMapper.selectCount(Wrappers.<SysOrg>lambdaQuery()
                .eq(SysOrg::getOrgCode, request.getOrgCode().trim())
                .ne(SysOrg::getId, request.getId()));
        if (codeCount != null && codeCount > 0) {
            throw BizException.biz("组织编码「" + request.getOrgCode() + "」已存在");
        }
        checkSameLevelName(request.getOrgName(), exists.getParentId(), request.getId());

        SysOrg org = new SysOrg();
        org.setId(request.getId());
        copy(request, org);
        org.setParentId(exists.getParentId());
        org.setAncestors(exists.getAncestors());
        orgMapper.updateById(org);
        dataScopeService.evictOrgCache();
    }

    /**
     * 移动节点：校验目标父节点不是自身或其后代，并重算全部子孙 ancestors。
     */
    @Transactional
    public void move(Long id, Long newParentId) {
        SysOrg node = orgMapper.selectById(id);
        if (node == null) {
            throw BizException.notFound("组织不存在");
        }
        if (newParentId == null) {
            newParentId = 0L;
        }
        if (newParentId.equals(id)) {
            throw BizException.biz("不能把组织移动到自身下");
        }
        SysOrg newParent = null;
        if (newParentId != 0L) {
            newParent = orgMapper.selectById(newParentId);
            if (newParent == null) {
                throw BizException.badRequest("目标上级组织不存在");
            }
            if (isDescendant(node.getId(), newParent)) {
                throw BizException.biz("不能把组织移动到自己的下级组织中");
            }
        }
        String ancestors = newParent == null ? "0" : newParent.getAncestors() + "," + newParent.getId();
        SysOrg update = new SysOrg();
        update.setId(id);
        update.setParentId(newParentId);
        update.setAncestors(ancestors);
        orgMapper.updateById(update);
        rebuildDescendants(id, ancestors);
        dataScopeService.evictOrgCache();
    }

    @Transactional
    public void delete(Long id) {
        SysOrg org = orgMapper.selectById(id);
        if (org == null) {
            throw BizException.notFound("组织不存在");
        }
        Long children = orgMapper.selectCount(Wrappers.<SysOrg>lambdaQuery().eq(SysOrg::getParentId, id));
        if (children != null && children > 0) {
            throw BizException.biz("该组织下仍有 " + children + " 个子组织，请先处理");
        }
        Long roles = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getOrgId, id));
        if (roles != null && roles > 0) {
            throw BizException.biz("该组织下仍有 " + roles + " 个角色，请先调整角色的绑定组织");
        }
        orgMapper.deleteById(id);
        dataScopeService.evictOrgCache();
    }

    // ---------------- 内部 ----------------

    private void rebuildDescendants(Long parentId, String parentAncestors) {
        List<SysOrg> children = orgMapper.selectList(Wrappers.<SysOrg>lambdaQuery()
                .eq(SysOrg::getParentId, parentId));
        for (SysOrg child : children) {
            String ancestors = parentAncestors + "," + parentId;
            SysOrg update = new SysOrg();
            update.setId(child.getId());
            update.setAncestors(ancestors);
            orgMapper.updateById(update);
            rebuildDescendants(child.getId(), ancestors);
        }
    }

    private boolean isDescendant(Long nodeId, SysOrg candidate) {
        if (candidate.getAncestors() == null) {
            return false;
        }
        for (String part : candidate.getAncestors().split(",")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            if (Long.valueOf(part.trim()).equals(nodeId)) {
                return true;
            }
        }
        return false;
    }

    private void checkSameLevelName(String orgName, Long parentId, Long excludeId) {
        Long count = orgMapper.selectCount(Wrappers.<SysOrg>lambdaQuery()
                .eq(SysOrg::getOrgName, orgName)
                .eq(SysOrg::getParentId, parentId)
                .ne(excludeId != null, SysOrg::getId, excludeId));
        if (count != null && count > 0) {
            throw BizException.biz("同一上级下已存在名为「" + orgName + "」的组织");
        }
    }

    private Map<Long, Long> countsByOrg() {
        Map<Long, Long> map = new HashMap<>();
        for (SysRole role : roleMapper.selectList(Wrappers.emptyWrapper())) {
            map.merge(role.getOrgId(), 1L, Long::sum);
        }
        return map;
    }

    private OrgVO toVO(SysOrg org, long roleCount) {
        OrgVO vo = new OrgVO();
        vo.setId(org.getId());
        vo.setOrgCode(org.getOrgCode());
        vo.setOrgName(org.getOrgName());
        vo.setOrgType(org.getOrgType());
        vo.setParentId(org.getParentId());
        vo.setAncestors(org.getAncestors());
        vo.setLeaderId(org.getLeaderId());
        vo.setCostCenterCode(org.getCostCenterCode());
        vo.setSortNo(org.getSortNo());
        vo.setStatus(org.getStatus());
        vo.setEffectiveDate(org.getEffectiveDate());
        vo.setExpireDate(org.getExpireDate());
        vo.setRoleCount(roleCount);
        return vo;
    }

    private void sortRecursively(List<OrgVO> nodes) {
        nodes.sort(Comparator.comparing(v -> v.getSortNo() == null ? 0 : v.getSortNo()));
        for (OrgVO node : nodes) {
            sortRecursively(node.getChildren());
        }
    }

    private void copy(OrgDtos.SaveRequest request, SysOrg org) {
        org.setOrgCode(request.getOrgCode().trim());
        org.setOrgName(request.getOrgName());
        org.setOrgType(request.getOrgType());
        org.setLeaderId(request.getLeaderId());
        org.setCostCenterCode(request.getCostCenterCode());
        org.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        org.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        org.setEffectiveDate(request.getEffectiveDate());
        org.setExpireDate(request.getExpireDate());
    }
}
