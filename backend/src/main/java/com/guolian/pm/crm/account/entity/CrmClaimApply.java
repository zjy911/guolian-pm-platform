package com.guolian.pm.crm.account.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 公共池认领申请：走 CRM_CLAIM 审批，原负责人 24h 内可申诉。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_claim_apply")
public class CrmClaimApply extends BaseEntity {

    private Long accountId;
    private Long applicantId;
    private Long applicantOrgId;
    private Long oldOwnerId;
    /** APPROVING / APPROVED / REJECTED / WITHDRAWN */
    private String status;
    private String flowInstanceId;
    private LocalDateTime appealDeadline;
    /** NONE / APPEALED / EXPIRED */
    private String appealStatus;
    private String applyReason;
    private String remark;
}
