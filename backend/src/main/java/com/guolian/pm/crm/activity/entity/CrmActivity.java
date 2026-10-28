package com.guolian.pm.crm.activity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 活动：多态挂接线索 / 客户 / 联系人 / 商机，不物理删除只作废。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_activity")
public class CrmActivity extends BaseEntity {

    private String subject;
    /** PHONE / EMAIL / MEETING / TASK / VISIT / PROPOSAL */
    private String type;
    /** HIGH / MEDIUM / LOW */
    private String level;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationDays;
    /** LEAD / ACCOUNT / CONTACT / OPPORTUNITY */
    private String targetType;
    private Long targetId;
    private Long ownerUserId;
    private Long ownerOrgId;
    private String ownerUserName;
    private String description;
    private String fileIds;
    /** VALID / VOIDED */
    private String status;
    private String voidReason;
    private String remark;
}
