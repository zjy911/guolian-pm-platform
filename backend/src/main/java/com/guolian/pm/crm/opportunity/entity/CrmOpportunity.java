package com.guolian.pm.crm.opportunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import com.guolian.pm.system.sensitive.annotation.Sensitive;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商机：金额 / 阶段 / 概率 / 计价口径。成交由合同或协议生效反推，不可手工置。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_opportunity")
public class CrmOpportunity extends BaseEntity {

    private String name;
    private Long accountId;
    /** 跨模块只读引用（ProductRefService），CRM 不建产品表 */
    private Long productId;
    private BigDecimal expectedAmount;
    private LocalDate expectedCloseDate;
    /** PROSPECT / PROPOSAL / NEGOTIATION / WON / LOST */
    private String salesStage;
    private BigDecimal winProbability;
    private BigDecimal estRevenue;
    private String currency;

    @Sensitive(group = "CRM_COST", bizObject = "opportunity")
    private BigDecimal estCost;

    @Sensitive(group = "CRM_COST", bizObject = "opportunity")
    private BigDecimal estProfitRate;

    private String estServiceVolume;
    private String pricingMode;
    /** 计价结构化明细（JSON 字符串） */
    private String pricingDetail;

    private Long ownerUserId;
    private Long ownerOrgId;
    private String ownerUserName;

    private Long sourceLeadId;
    /** 成交标记，由合同激活反推 */
    private Integer isWon;
    /** 首份激活合同 */
    private Long contractId;
    private String address;
    private String lostReason;
    private LocalDateTime stageChangeTime;
    private LocalDateTime lastActivityTime;
    private String remark;
}
