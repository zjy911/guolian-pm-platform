package com.guolian.pm.crm.common.service;

import com.guolian.pm.system.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * CRM 参数读取统一入口，键全部落在底座 sys_config（参数配置），不硬编码。
 */
@Component
@RequiredArgsConstructor
public class CrmConfig {

    private final ConfigService configService;

    /** 客户无活动进公共池天数（默认 90） */
    public int poolTimeoutDays() {
        return configService.getInt("crm.pool.timeout.days", 90);
    }

    /** 线索回收天数（默认 30） */
    public int leadPoolTimeoutDays() {
        return configService.getInt("crm.lead.pool.timeout.days", 30);
    }

    /** 认领申诉时长（小时，默认 24） */
    public int claimAppealHours() {
        return configService.getInt("crm.claim.appeal.hours", 24);
    }

    /** 合同到期提醒提前天数（默认 30） */
    public int contractExpireNotifyDays() {
        return configService.getInt("crm.contract.expire.notify.days", 30);
    }

    /** 开标提醒提前天数（默认 3） */
    public int bidOpenNotifyDays() {
        return configService.getInt("crm.bid.open.notify.days", 3);
    }

    /** 单次导入上限（默认 5000） */
    public int importMaxRows() {
        return configService.getInt("crm.import.max.rows", 5000);
    }

    /** 认领审批原负责人未处理是否自动同意（默认开） */
    public boolean claimAutoApprove() {
        return configService.getBoolean("crm.claim.auto.approve", true);
    }

    /** 金额阈值（元），供条件分支示例使用 */
    public BigDecimal amountThreshold() {
        return new BigDecimal(configService.get("crm.approval.amount.threshold", "1000000"));
    }
}
