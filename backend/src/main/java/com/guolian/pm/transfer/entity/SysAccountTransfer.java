package com.guolian.pm.transfer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 账号交接单。审批状态与执行状态是两条独立状态线。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_account_transfer")
public class SysAccountTransfer extends BaseEntity {

    private String transferNo;
    private Long fromUserId;
    /** JSON 数组 */
    private String toUserIds;
    /** 1 单接收人 / 2 按角色分派 */
    private Integer assignMode;
    /** JSON 数组：交接业务范围（适配器 bizType 列表） */
    private String scope;
    /** 按角色分派时保存 roleCode → 接收人 userId 的 JSON 映射 */
    private String roleAssignments;
    private String remark;
    private LocalDateTime effectiveTime;
    /** DRAFT / RUNNING / APPROVED / REJECTED / WITHDRAWN */
    private String flowStatus;
    /** NOT_EXECUTED / EXECUTING / DONE / PARTIAL_FAILED / REVOKED */
    private String execStatus;
    private String flowInstanceId;
    private Integer itemCount;
    private Integer failCount;
    private LocalDateTime revokeDeadline;
}
