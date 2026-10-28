package com.guolian.pm.crm.account.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import com.guolian.pm.system.sensitive.annotation.Sensitive;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户：toG / toB 统一管理，承载私有 / 公共池 / 流失三态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_account")
public class CrmAccount extends BaseEntity {

    private String companyName;
    /** 18 位统一社会信用代码，唯一（允许 NULL 多行） */
    private String creditCode;
    /** toG 客户无信用代码时使用机构编码 */
    private String institutionCode;
    private String industry;
    /** TO_G / TO_B */
    private String accountNature;
    /** KEY 大客户 / NORMAL 普通客户 */
    private String accountType;
    private String website;

    @Sensitive(group = "MOBILE", bizObject = "account")
    private String phone;

    private String employeeScale;
    /** 年营业额，单位万元 */
    @Sensitive(group = "CRM_REVENUE", bizObject = "account")
    private BigDecimal annualRevenue;

    private Long ownerUserId;
    private Long ownerOrgId;
    private String ownerUserName;

    private Long parentAccountId;
    private Long primaryContactId;
    private String province;
    private String city;
    private String district;

    @Sensitive(group = "ADDRESS", bizObject = "account")
    private String address;

    /** PRIVATE / POOL / CHURNED */
    private String poolStatus;
    private LocalDateTime poolInTime;
    private String poolReason;
    private String churnReason;
    /** 潜客标记：投标快速建档时为 1，不计入客户数统计 */
    private Integer isProspect;
    private LocalDateTime lastActivityTime;
    private String remark;
}
