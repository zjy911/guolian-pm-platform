package com.guolian.pm.system.role.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色字段脱敏策略：role_id + group_code 唯一，mask_level = PLAIN / MASKED / HIDDEN。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_field_mask")
public class SysRoleFieldMask extends BaseEntity {

    private Long roleId;
    private String groupCode;
    private String maskLevel;
}
