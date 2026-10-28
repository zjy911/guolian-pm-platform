package com.guolian.pm.system.role.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色 = 所属组织 + 功能权限 + 数据权限 + 字段脱敏策略。
 * org_id 必填：切换角色即切换组织。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private String roleCode;
    private String roleName;
    private Long orgId;
    private String description;
    /** 1 内置 / 2 自定义 */
    private Integer roleType;
    /** ORG_AND_CHILD / ORG / ORG_CUSTOM / COMPANY_CUSTOM / SELF / ALL / PROJECT_MEMBER */
    private String dataScope;
    /** 自定义范围值（组织 ID 或公司主体 ID 的 JSON 数组） */
    private String scopeValue;
    private Integer canBeProjectMember;
    private Integer canBeApprover;
    private Integer sortNo;
    private Integer status;
    private String remark;
}
