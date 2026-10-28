package com.guolian.pm.system.log.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志。参数已脱敏，不含密码/令牌；模拟登录行为强制留痕。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oper_log")
public class SysOperLog extends BaseEntity {

    private String module;
    private String action;
    private String method;
    private String requestUri;
    @TableField("params")
    private String params;
    private String result;
    private Long operatorId;
    private String operatorName;
    private String roleName;
    private String ip;
    private Long costMs;
    private Integer success;
    private String errorMsg;
}
