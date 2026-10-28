package com.guolian.pm.crm.lead.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import com.guolian.pm.system.sensitive.annotation.Sensitive;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 线索：销售漏斗最前端，转化后派生客户 + 联系人 + 商机。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_lead")
public class CrmLead extends BaseEntity {

    private String name;
    private String companyName;

    @Sensitive(group = "MOBILE", bizObject = "lead")
    private String mobile;

    /** HMAC-SHA256，用于等值查重（允许强制继续，故不建唯一索引） */
    private String mobileHash;

    @Sensitive(group = "EMAIL", bizObject = "lead")
    private String email;

    private String emailHash;
    private String positionName;
    private String industry;
    private String sourceChannel;
    private String grade;

    /** NEW / FOLLOWING / CONVERTED / INVALID */
    private String status;
    private String accountNature;

    private Long ownerUserId;
    private Long ownerOrgId;
    private String ownerUserName;
    private LocalDateTime lastFollowTime;

    private Long convertedAccountId;
    private Long convertedContactId;
    private Long convertedOpportunityId;
    private LocalDateTime convertedTime;

    private String invalidReason;
    private String dupReason;
    private String remark;
}
