package com.guolian.pm.system.role.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色自定义数据范围-组织明细（data_scope = ORG_CUSTOM 时生效）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_data_scope_dept")
public class SysDataScopeDept extends BaseEntity {

    private Long roleId;
    private Long orgId;
}
