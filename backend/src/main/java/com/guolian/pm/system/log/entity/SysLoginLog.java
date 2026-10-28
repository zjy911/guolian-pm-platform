package com.guolian.pm.system.log.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 登录日志：登录 / 登出 / 切换角色 / 模拟登录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_login_log")
public class SysLoginLog extends BaseEntity {

    private String username;
    private Long userId;
    private String loginType;
    private String ip;
    private String location;
    private String browser;
    private String os;
    private Integer success;
    private String failReason;
}
