package com.guolian.pm.pms.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 项目：产品在具体客户上的一次落地实例（PRD 三、术语）。
 * 阶段 phase 1-6 固定：立项/筹备/实施/交接/运营/撤场，不可自定义（R-01）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_project")
public class PmProject extends BaseEntity {

    private String projectCode;
    private String projectName;
    private Long productId;
    private String productName;
    private String templateVersion;
    private Long customerId;
    private String customerName;
    private String contractNo;
    private String signEntity;
    private BigDecimal estimatedAmount;
    private Integer annualCases;
    private String pricingMode;
    private String rating;
    /** 1 立项 2 筹备 3 实施 4 交接 5 运营 6 撤场 */
    private Integer phase;
    private String status;
    private BigDecimal healthScore;
    private String healthLight;
    private BigDecimal grossMargin;
    private Long orgId;
    private Long ownerUserId;
    private Long ownerOrgId;
    private String ownerUserName;
    private Long initiationFormId;
}
