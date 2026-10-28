package com.guolian.pm.system.sensitive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查看明文审计日志。绝不记录明文值本身。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sensitive_log")
public class SysSensitiveLog extends BaseEntity {

    private Long userId;
    private Long roleId;
    private String bizType;
    private Long bizId;
    private String fieldName;
    private String groupCode;
    private String ip;
}
