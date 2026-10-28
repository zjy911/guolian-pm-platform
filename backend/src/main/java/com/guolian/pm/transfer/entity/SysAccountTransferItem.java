package com.guolian.pm.transfer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 交接明细：每条数据迁移的逐项结果，支持仅重试失败项与按明细反向回写。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_account_transfer_item")
public class SysAccountTransferItem extends BaseEntity {

    private Long transferId;
    private String bizType;
    private Long bizId;
    private String bizName;
    private Long beforeOwnerId;
    private Long afterOwnerId;
    /** SUCCESS / FAILED / ROLLED_BACK */
    private String result;
    private String failReason;
}
