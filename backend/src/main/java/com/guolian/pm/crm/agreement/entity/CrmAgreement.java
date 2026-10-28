package com.guolian.pm.crm.agreement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import com.guolian.pm.system.sensitive.annotation.Sensitive;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 协议：法院授权类合约。end_date 可为空表示长期有效（到期任务须排除 NULL）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_agreement")
public class CrmAgreement extends BaseEntity {

    private String agreementNo;
    private String name;
    private String agreementType;
    private Long accountId;
    private Long companyId;
    private Long opportunityId;
    private Long productId;
    private Long businessGroupId;
    private LocalDate signDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;
    /** DRAFT / APPROVING / EFFECTIVE / EXPIRED / RENEWING */
    private String status;
    /** FIRST / RENEW */
    private String signType;
    private Long firstAgreementId;

    @Sensitive(group = "CRM_AMOUNT", bizObject = "agreement")
    private BigDecimal winAmount;

    private Long ownerUserId;
    private Long ownerOrgId;
    private String ownerUserName;
    private Integer isArchived;
    private LocalDateTime archiveTime;
    private String paperBoxNo;
    private Integer paperCopies;
    private LocalDateTime activateTime;
    private String flowInstanceId;
    private String terminateReason;
    private String remark;
}
