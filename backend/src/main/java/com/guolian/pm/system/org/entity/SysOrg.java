package com.guolian.pm.system.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 组织架构（唯一一棵树）。故意不含 company_id —— 公司主体与组织无直接关系。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org")
public class SysOrg extends BaseEntity {

    private String orgCode;
    private String orgName;
    private String orgType;
    private Long parentId;
    /** 祖级路径，格式 0,1,12,135 */
    private String ancestors;
    private Long leaderId;
    private String costCenterCode;
    private Integer sortNo;
    private Integer status;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
}
