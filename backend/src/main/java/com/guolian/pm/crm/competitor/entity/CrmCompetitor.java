package com.guolian.pm.crm.competitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 竞对：挂在商机下，用于对比矩阵。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_competitor")
public class CrmCompetitor extends BaseEntity {

    private Long opportunityId;
    private String competitorName;
    private Integer staffSize;
    /** 大区 / 省份 / 城市 */
    private String coverageScope;
    /** STRONG / MEDIUM / WEAK */
    private String techAbility;
    private String pricingMode;
    private String pricingDetail;
    private String operationEfficiency;
    private String operationQuality;
    private Long ownerUserId;
    private Long ownerOrgId;
    private String ownerUserName;
    private String remark;
}
