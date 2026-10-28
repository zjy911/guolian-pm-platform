package com.guolian.pm.system.sensitive.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.common.cache.CacheService;
import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.common.web.LoginUser;
import com.guolian.pm.system.role.entity.SysRoleFieldMask;
import com.guolian.pm.system.role.mapper.SysRoleFieldMaskMapper;
import com.guolian.pm.system.sensitive.entity.SysSensitiveField;
import com.guolian.pm.system.sensitive.entity.SysSensitiveGroup;
import com.guolian.pm.system.sensitive.mapper.SysSensitiveFieldMapper;
import com.guolian.pm.system.sensitive.mapper.SysSensitiveGroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 脱敏策略中心。档位由「当前角色 + 字段组」决定，算法由字段注册决定，绝不硬编码在实体上。
 * 配置变更后刷新缓存，1 分钟内对在线用户生效（缓存 TTL = 60s）。
 */
@Service
@RequiredArgsConstructor
public class SensitivePolicyService {

    private static final long CONFIG_TTL = 60L;
    private static final String KEY_FIELDS = "mask:fields";
    private static final String KEY_GROUPS = "mask:groups";
    private static final String KEY_ROLE = "mask:role:";

    private final SysSensitiveGroupMapper groupMapper;
    private final SysSensitiveFieldMapper fieldMapper;
    private final SysRoleFieldMaskMapper roleFieldMaskMapper;
    private final CacheService cache;

    public static final String PLAIN = "PLAIN";
    public static final String MASKED = "MASKED";
    public static final String HIDDEN = "HIDDEN";

    /** 当前角色对某字段组的档位 */
    public String level(String groupCode) {
        LoginUser user = AuthContext.get();
        if (user == null || !StringUtils.hasText(groupCode)) {
            return MASKED;
        }
        String level = roleLevels(user.getRoleId()).get(groupCode);
        if (!StringUtils.hasText(level)) {
            SysSensitiveGroup group = groups().get(groupCode);
            level = group == null ? MASKED : group.getDefaultLevel();
        }
        if (PLAIN.equals(level) && !plainAllowed(groupCode)) {
            // 高风险组且无字段允许明文时，降级为脱敏，防止误配置导致泄露
            level = MASKED;
        }
        return level;
    }

    /** 按当前角色策略脱敏一个值（导出、日志同样走这里） */
    public String mask(String groupCode, String fieldName, String value) {
        String level = level(groupCode);
        if (PLAIN.equals(level)) {
            return value;
        }
        SysSensitiveField field = fields().get(key(groupCode, fieldName));
        String rule = field == null ? "FULL_MASK" : field.getMaskRule();
        String param = field == null ? null : field.getMaskParam();
        return MaskUtil.apply(rule, param, value);
    }

    public boolean isHidden(String groupCode) {
        return HIDDEN.equals(level(groupCode));
    }

    public boolean plainAllowed(String groupCode) {
        SysSensitiveGroup group = groups().get(groupCode);
        if (group == null) {
            return true;
        }
        if (!"HIGH".equals(group.getRiskLevel())) {
            return true;
        }
        return fields().values().stream()
                .anyMatch(f -> groupCode.equals(f.getGroupCode()) && Integer.valueOf(1).equals(f.getAllowPlain()));
    }

    public Map<String, SysSensitiveGroup> groups() {
        @SuppressWarnings("unchecked")
        Map<String, SysSensitiveGroup> cached = cache.get(KEY_GROUPS, Map.class);
        if (cached != null) {
            return cached;
        }
        Map<String, SysSensitiveGroup> map = new HashMap<>();
        for (SysSensitiveGroup g : groupMapper.selectList(Wrappers.emptyWrapper())) {
            map.put(g.getGroupCode(), g);
        }
        cache.put(KEY_GROUPS, map, CONFIG_TTL);
        return map;
    }

    private Map<String, SysSensitiveField> fields() {
        @SuppressWarnings("unchecked")
        Map<String, SysSensitiveField> cached = cache.get(KEY_FIELDS, Map.class);
        if (cached != null) {
            return cached;
        }
        Map<String, SysSensitiveField> map = new HashMap<>();
        List<SysSensitiveField> list = fieldMapper.selectList(Wrappers.<SysSensitiveField>lambdaQuery()
                .eq(SysSensitiveField::getStatus, 1));
        for (SysSensitiveField f : list) {
            map.put(key(f.getGroupCode(), f.getFieldName()), f);
        }
        cache.put(KEY_FIELDS, map, CONFIG_TTL);
        return map;
    }

    private Map<String, String> roleLevels(Long roleId) {
        String cacheKey = KEY_ROLE + roleId;
        @SuppressWarnings("unchecked")
        Map<String, String> cached = cache.get(cacheKey, Map.class);
        if (cached != null) {
            return cached;
        }
        Map<String, String> map = new HashMap<>();
        List<SysRoleFieldMask> masks = roleFieldMaskMapper.selectList(Wrappers.<SysRoleFieldMask>lambdaQuery()
                .eq(SysRoleFieldMask::getRoleId, roleId));
        for (SysRoleFieldMask m : masks) {
            map.put(m.getGroupCode(), m.getMaskLevel());
        }
        cache.put(cacheKey, map, CONFIG_TTL);
        return map;
    }

    public void evictAll() {
        cache.evict(KEY_FIELDS);
        cache.evict(KEY_GROUPS);
        cache.evictByPrefix(KEY_ROLE);
    }

    public void evictRole(Long roleId) {
        cache.evict(KEY_ROLE + roleId);
    }

    private String key(String group, String field) {
        return group + ":" + field;
    }
}
