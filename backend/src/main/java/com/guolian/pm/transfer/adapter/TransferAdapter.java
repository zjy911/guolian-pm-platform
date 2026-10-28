package com.guolian.pm.transfer.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 账号交接业务对象适配器。
 * 底座不建业务表，业务模块各自实现本接口并注册为 Spring Bean，交接主流程代码不改。
 */
public interface TransferAdapter {

    /** 业务对象编码，如 NOTICE_TODO / FLOW_TASK */
    String bizType();

    /** 业务对象名称 */
    String bizName();

    /** 统计待交接数量（since 可为空） */
    long count(Long fromUserId, LocalDateTime since);

    /** 待交接数据 ID 列表（分批执行时使用） */
    List<Long> listIds(Long fromUserId, int offset, int limit);

    /** 执行迁移 */
    TransferOutcome transfer(Long fromUserId, Long toUserId, List<Long> bizIds);

    /**
     * 执行迁移（带接收人组织）。
     *
     * <p>业务表同时存 owner_user_id 与 owner_org_id 时，两者必须同一次 update 一起改，
     * 否则数据会跑到错误的组织范围里。因此底座把「接收人在该角色的 org_id」一并传入，
     * 业务模块不要去查接收人的默认角色自己推算。
     *
     * <p>默认实现忽略 toOrgId，向后兼容只按人归属的适配器。
     */
    default TransferOutcome transfer(Long fromUserId, Long toUserId, Long toOrgId, List<Long> bizIds) {
        return transfer(fromUserId, toUserId, bizIds);
    }

    /** 按明细反向回写（撤销） */
    void rollback(List<RollbackItem> items);

    /** 单条数据名称，用于明细展示 */
    String describe(Long bizId);

    /**
     * 该条数据归属的角色编码，用于「按角色分派」模式精确路由。
     * 返回 null 时回落到默认角色对应的接收人。业务模块可按需覆写。
     */
    default String ownerRoleCode(Long bizId) {
        return null;
    }

    record TransferOutcome(List<Long> succeeded, Map<Long, String> failed) {
    }

    record RollbackItem(Long bizId, Long beforeOwnerId, Long afterOwnerId) {
    }
}
