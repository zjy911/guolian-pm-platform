package com.guolian.pm.system.sensitive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字段注册。field_name 只能从研发维护的白名单中选择，避免超管配错导致脱敏静默失效。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sensitive_field")
public class SysSensitiveField extends BaseEntity {

    private String groupCode;
    /** 本期仅 user / company */
    private String bizObject;
    private String fieldName;
    private String fieldLabel;
    /** KEEP_HEAD_TAIL / KEEP_TAIL / EMAIL_MASK / ADDRESS_MASK / FULL_MASK / AMOUNT_RANGE */
    private String maskRule;
    private String maskParam;
    private Integer allowPlain;
    private Integer status;
}
