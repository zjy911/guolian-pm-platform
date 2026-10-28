package com.guolian.pm.system.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户-公司主体关联。relation_type：1 任职 / 2 劳动合同 / 3 社保 / 4 发薪。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_company")
public class SysUserCompany extends BaseEntity {

    private Long userId;
    private Long companyId;
    private Integer relationType;
    private Integer isPrimary;
}
