package com.guolian.pm.common.scope;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guolian.pm.common.cache.CacheService;
import com.guolian.pm.common.config.AppProperties;
import com.guolian.pm.common.web.LoginUser;
import com.guolian.pm.system.org.entity.SysOrg;
import com.guolian.pm.system.org.mapper.SysOrgMapper;
import com.guolian.pm.system.role.entity.SysRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据范围解析。基准始终是 sys_role.org_id，只按当前角色计算，不叠加。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataScopeService {

    private static final String ORG_ALL_KEY = "org:all";
    private static final String SCOPE_ORG_KEY = "scope:org:";
    private static final String SCOPE_COMPANY_KEY = "scope:company:";

    private final SysOrgMapper orgMapper;
    private final CacheService cache;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    @SuppressWarnings("unchecked")
    public ScopeInfo resolve(SysRole role) {
        if (role == null) {
            return ScopeInfo.self();
        }
        String scope = StringUtils.hasText(role.getDataScope()) ? role.getDataScope() : "SELF";
        switch (scope) {
            case "ALL":
                return ScopeInfo.all();
            case "SELF":
            case "PROJECT_MEMBER":
                return ScopeInfo.self();
            case "ORG":
                return new ScopeInfo(ScopeInfo.Type.ORG, List.of(role.getOrgId()), List.of());
            case "ORG_AND_CHILD":
                return new ScopeInfo(ScopeInfo.Type.ORG, orgWithChildren(List.of(role.getOrgId())), List.of());
            case "ORG_CUSTOM": {
                List<Long> configured = parseIds(role.getScopeValue());
                if (configured.isEmpty()) {
                    return ScopeInfo.self();
                }
                return new ScopeInfo(ScopeInfo.Type.ORG, orgWithChildren(configured), List.of());
            }
            case "COMPANY_CUSTOM": {
                List<Long> companies = parseIds(role.getScopeValue());
                if (companies.isEmpty()) {
                    return ScopeInfo.self();
                }
                return new ScopeInfo(ScopeInfo.Type.ORG, List.of(), companies);
            }
            default:
                return ScopeInfo.self();
        }
    }

    public ScopeInfo resolve(LoginUser user) {
        if (user == null) {
            return ScopeInfo.self();
        }
        String key = SCOPE_ORG_KEY + user.getRoleId();
        @SuppressWarnings("unchecked")
        List<Long> cached = cache.get(key, List.class);
        if (cached != null) {
            return new ScopeInfo(ScopeInfo.Type.ORG, cached, List.of());
        }
        SysRole role = new SysRole();
        role.setOrgId(user.getOrgId());
        role.setDataScope(user.getDataScope());
        role.setScopeValue(null);
        ScopeInfo info = resolve(role);
        if (info.getType() == ScopeInfo.Type.ORG) {
            cache.put(key, info.getOrgIds(), properties.getCache().getTtlSeconds());
        }
        return info;
    }

    /**
     * 把组织及其全部下级展开成 ID 集合。
     */
    public List<Long> orgWithChildren(List<Long> roots) {
        List<SysOrg> all = allOrgs();
        Set<Long> result = new LinkedHashSet<>(roots);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (SysOrg org : all) {
                if (result.contains(org.getParentId()) && result.add(org.getId())) {
                    changed = true;
                }
            }
        }
        return new ArrayList<>(result);
    }

    @SuppressWarnings("unchecked")
    public List<SysOrg> allOrgs() {
        List<SysOrg> cached = cache.get(ORG_ALL_KEY, List.class);
        if (cached != null) {
            return cached;
        }
        List<SysOrg> list = orgMapper.selectList(Wrappers.<SysOrg>lambdaQuery().orderByAsc(SysOrg::getSortNo));
        cache.put(ORG_ALL_KEY, list, properties.getCache().getTtlSeconds());
        return list;
    }

    public void evictOrgCache() {
        cache.evict(ORG_ALL_KEY);
        cache.evictByPrefix(SCOPE_ORG_KEY);
        cache.evictByPrefix(SCOPE_COMPANY_KEY);
    }

    public void evictRoleScope(Long roleId) {
        cache.evict(SCOPE_ORG_KEY + roleId);
        cache.evict(SCOPE_COMPANY_KEY + roleId);
    }

    private List<Long> parseIds(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<Long> ids = objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
            return ids == null ? List.of() : ids;
        } catch (Exception e) {
            log.warn("数据范围配置解析失败，按仅本人处理：{}", json);
            return List.of();
        }
    }
}
