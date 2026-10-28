package com.guolian.pm.system.sensitive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字段组：脱敏配置的三层结构之一。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sensitive_group")
public class SysSensitiveGroup extends BaseEntity {

    private String groupCode;
    private String groupName;
    /** PLAIN / MASKED / HIDDEN */
    private String defaultLevel;
    /** LOW / MEDIUM / HIGH */
    private String riskLevel;
    private Integer sortNo;
    private Integer status;
    private String remark;
}
