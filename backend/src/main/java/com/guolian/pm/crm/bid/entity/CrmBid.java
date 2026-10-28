package com.guolian.pm.crm.bid.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import com.guolian.pm.system.sensitive.annotation.Sensitive;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 投标：1:1 替代商务部线下 Excel，保留 16 列语义。
 * 金额单位：拦标价与中标金额为万元，保证金为元（字段注释与前端必须标注）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_bid")
public class CrmBid extends BaseEntity {

    private Long accountId;
    private String tenderName;
    private String tenderNo;
    private String tenderForm;
    /** 我方投标主体 sys_company */
    private Long bidEntity;
    private String province;
    private String city;
    /** 所属地区部 sys_org */
    private Long regionOrgId;

    /** 拦标价，单位万元 */
    @Sensitive(group = "CRM_AMOUNT", bizObject = "bid")
    private BigDecimal priceCap;

    private LocalDate openDate;
    private LocalDate deadline;

    /** 保证金，单位元 */
    @Sensitive(group = "CRM_AMOUNT", bizObject = "bid")
    private BigDecimal deposit;

    /** UNPAID / PAID / REFUNDED */
    private String depositStatus;
    /** TRACKING / REGISTERED / SUBMITTED / WON / LOST / ABANDONED */
    private String bidStatus;

    /** 中标金额，单位万元 */
    @Sensitive(group = "CRM_AMOUNT", bizObject = "bid")
    private BigDecimal winAmount;

    private String winResult;
    private String bidPlan;
    private String tenderUrl;
    private String winUrl;
    private Integer isWon;
    private Long ownerUserId;
    private Long ownerOrgId;
    private String ownerUserName;
    private String lostReason;
    private String abandonReason;
    /** 一键生成商机/合同的幂等锚点 */
    private Long genOpportunityId;
    private Long genContractId;
    private String remark;
}
