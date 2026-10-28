package com.guolian.pm.crm.account.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户池流转日志：私有 / 公共池 / 流失每次变更必须留痕。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_account_pool_log")
public class CrmAccountPoolLog extends BaseEntity {

    private Long accountId;
    private String fromStatus;
    private String toStatus;
    /** MANUAL / AUTO_TIMEOUT / CLAIM / CHURN */
    private String triggerType;
    private String reason;
    private Long operatorId;
    private Long operatorOrgId;
    private String remark;
}
