package com.guolian.pm.crm.transfer.adapter;

import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.common.web.LoginUser;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.transfer.adapter.TransferAdapter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CRM 交接适配器基类：7 个业务对象只差异在「查哪些 ID」「改哪些表」，因此抽出来。
 *
 * <p>三条实现要求（方案 11.2）：
 * <ol>
 *   <li>公共池客户不参与交接 —— 由子类的 SQL 保证（计数与执行同时排除）</li>
 *   <li>owner_org_id 必须随 owner_user_id 一起改 —— 基类统一处理</li>
 *   <li>rollback 按明细逐条反向回写 —— 基类统一处理，不用「全部重置为交出人」的粗暴做法</li>
 * </ol>
 */
public abstract class AbstractCrmTransferAdapter implements TransferAdapter {

    /** 子类注入，用于取接收人/交出人的默认角色组织 */
    protected abstract CrmOwnerSupport ownerSupport();

    protected abstract long countByOwner(Long fromUserId);

    protected abstract List<Long> pageIds(Long fromUserId, int offset, int limit);

    /** 同一次 update 改负责人 + 归属组织 + 姓名快照 */
    protected abstract int updateOwner(List<Long> bizIds, Long toUserId, Long toOrgId, String toUserName);

    protected abstract String describeById(Long bizId);

    /** 该条数据归属角色编码，用于「按角色分派」精确路由；默认不区分 */
    protected String roleCodeOf(Long bizId) {
        return null;
    }

    @Override
    public long count(Long fromUserId, LocalDateTime since) {
        return countByOwner(fromUserId);
    }

    @Override
    public List<Long> listIds(Long fromUserId, int offset, int limit) {
        return pageIds(fromUserId, offset, limit);
    }

    @Override
    public TransferOutcome transfer(Long fromUserId, Long toUserId, List<Long> bizIds) {
        return transfer(fromUserId, toUserId, null, bizIds);
    }

    @Override
    public TransferOutcome transfer(Long fromUserId, Long toUserId, Long toOrgId, List<Long> bizIds) {
        List<Long> succeeded = new ArrayList<>();
        Map<Long, String> failed = new LinkedHashMap<>();
        if (bizIds == null || bizIds.isEmpty()) {
            return new TransferOutcome(succeeded, failed);
        }
        if (toUserId == null) {
            bizIds.forEach(id -> failed.put(id, "未指定接收人"));
            return new TransferOutcome(succeeded, failed);
        }
        // 接收人组织优先用底座传入的「该角色 org_id」，取不到时回落默认角色组织
        Long orgId = toOrgId != null ? toOrgId : ownerSupport().orgOf(toUserId);
        if (orgId == null) {
            bizIds.forEach(id -> failed.put(id, "接收人未绑定角色组织，无法确定数据归属"));
            return new TransferOutcome(succeeded, failed);
        }
        String userName = ownerSupport().nameOf(toUserId);
        // 分批调用，避免一次性 UPDATE 全量
        for (Long bizId : bizIds) {
            try {
                int rows = updateOwner(List.of(bizId), toUserId, orgId, userName);
                if (rows > 0) {
                    succeeded.add(bizId);
                } else {
                    failed.put(bizId, "数据不存在或已变更，未更新");
                }
            } catch (Exception e) {
                failed.put(bizId, "更新失败：" + e.getMessage());
            }
        }
        return new TransferOutcome(succeeded, failed);
    }

    @Override
    public void rollback(List<RollbackItem> items) {
        LoginUser user = AuthContext.get();
        for (RollbackItem item : items) {
            if (item.bizId() == null || item.beforeOwnerId() == null) {
                continue;
            }
            Long orgId = ownerSupport().orgOf(item.beforeOwnerId());
            String userName = ownerSupport().nameOf(item.beforeOwnerId());
            updateOwner(List.of(item.bizId()), item.beforeOwnerId(),
                    orgId == null && user != null ? user.getOrgId() : orgId, userName);
        }
    }

    @Override
    public String describe(Long bizId) {
        try {
            String name = describeById(bizId);
            return name == null ? bizName() + "#" + bizId : name;
        } catch (Exception e) {
            return bizName() + "#" + bizId;
        }
    }
}
