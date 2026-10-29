package com.guolian.pm.pms.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 产品：BG 面向某类客户的标准化服务方案，任务模板的载体（PRD 7.5 产品管理）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_product")
public class PmProduct extends BaseEntity {

    private String productCode;
    private String productName;
    private Long bgOrgId;
    private String bgOrgName;
    private String pricingMode;
    private String templateVersion;
    private String status;
    private String remark;
    private Long ownerUserId;
    private Long ownerOrgId;
}
