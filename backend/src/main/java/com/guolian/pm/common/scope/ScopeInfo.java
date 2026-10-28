package com.guolian.pm.common.scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 解析后的数据范围快照。只按当前角色计算，不叠加其他角色。
 */
public class ScopeInfo {

    public enum Type {
        /** 全部数据（仅高管类角色） */
        ALL,
        /** 仅本人 */
        SELF,
        /** 按组织集合过滤 */
        ORG
    }

    private final Type type;
    private final List<Long> orgIds;
    private final List<Long> companyIds;

    public ScopeInfo(Type type, List<Long> orgIds, List<Long> companyIds) {
        this.type = type;
        this.orgIds = orgIds == null ? Collections.emptyList() : orgIds;
        this.companyIds = companyIds == null ? Collections.emptyList() : companyIds;
    }

    public static ScopeInfo all() {
        return new ScopeInfo(Type.ALL, Collections.emptyList(), Collections.emptyList());
    }

    public static ScopeInfo self() {
        return new ScopeInfo(Type.SELF, Collections.emptyList(), Collections.emptyList());
    }

    public Type getType() {
        return type;
    }

    public List<Long> getOrgIds() {
        return orgIds;
    }

    public List<Long> getCompanyIds() {
        return companyIds;
    }

    public boolean byCompany() {
        return type == Type.ORG && orgIds.isEmpty() && !companyIds.isEmpty();
    }

    public String orgIdCsv() {
        List<String> parts = new ArrayList<>();
        for (Long id : orgIds) {
            parts.add(String.valueOf(id));
        }
        return String.join(",", parts);
    }

    public String companyIdCsv() {
        List<String> parts = new ArrayList<>();
        for (Long id : companyIds) {
            parts.add(String.valueOf(id));
        }
        return String.join(",", parts);
    }
}
