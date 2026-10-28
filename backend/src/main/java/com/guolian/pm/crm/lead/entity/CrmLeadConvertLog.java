package com.guolian.pm.crm.lead.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 线索转化日志：uk_convert_lead 唯一索引是转化幂等的最后一道防线。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_lead_convert_log")
public class CrmLeadConvertLog extends BaseEntity {

    private Long leadId;
    private Long accountId;
    private Long contactId;
    private Long opportunityId;
    /** NEW_ACCOUNT / EXIST_ACCOUNT */
    private String convertMode;
    private Long operatorId;
    private Long operatorOrgId;
    private String remark;
}
