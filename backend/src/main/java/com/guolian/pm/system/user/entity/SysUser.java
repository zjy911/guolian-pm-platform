package com.guolian.pm.system.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户。组织不落在用户表上——由角色派生（角色必绑组织）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    private String realName;
    private String employeeNo;
    private String mobile;
    private String email;
    private Integer gender;
    private String avatar;
    /** 身份证号：加密存储，展示时按脱敏策略处理 */
    private String idCard;
    private String employeeType;
    private String employmentForm;
    @TableField("position_name")
    private String position;
    private String jobLevel;
    private LocalDate joinDate;
    private LocalDate leaveDate;
    /** 上岗认证状态：NONE 未认证 / CERTIFIED 已认证 / EXPIRED 已过期（PRD 6.2） */
    private String certStatus;
    private String certItem;
    private LocalDate certExpireDate;
    /** 1 正常 / 2 停用 / 3 离职 / 4 锁定 */
    private Integer status;
    private Long defaultRoleId;
    private Long leaderId;
    private Integer firstLogin;
    private LocalDateTime lastLoginTime;
    private String remark;
}
