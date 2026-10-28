package com.guolian.pm.crm.contract.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import com.guolian.pm.system.sensitive.annotation.Sensitive;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 合同：DRAFT → APPROVING → ACTIVE → EXPIRED / TERMINATED。
 * 激活触发赢单反推，必须幂等。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_contract")
public class CrmContract extends BaseEntity {

    private String contractNo;
    private String name;
    private Long accountId;
    /** 我方签约主体 sys_company */
    private Long companyId;
    /** 业务线，org_type=BG 的组织节点 */
    private Long businessGroupId;
    private Long productId;
    private LocalDate signDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;
    /** FIRST 首签 / RENEW 续签 */
    private String contractType;
    private Long firstContractId;
    /** DRAFT / APPROVING / ACTIVE / EXPIRED / TERMINATED */
    private String status;
    /** OPPORTUNITY / BID */
    private String sourceType;
    private Long sourceId;

    /** 合同金额（万元），用于审批条件分支与脱敏 */
    @Sensitive(group = "CRM_AMOUNT", bizObject = "contract")
    private BigDecimal winAmount;

    private Long ownerUserId;
    private Long ownerOrgId;
    private String ownerUserName;
    private Integer isArchived;
    private LocalDateTime archiveTime;
    private Long archiveUserId;
    private String paperBoxNo;
    private Integer paperCopies;
    private LocalDateTime activateTime;
    private LocalDateTime terminateTime;
    private String terminateReason;
    private String flowInstanceId;
    private String remark;
}
