package com.guolian.pm.system.company.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公司主体。与组织无直接关系，只与用户关联。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_company")
public class SysCompany extends BaseEntity {

    private String companyCode;
    private String companyName;
    private String shortName;
    private String companyType;
    private Long parentCompanyId;
    private String legalPerson;
    private String creditCode;
    private String regAddress;
    private String officeAddress;
    private String taxNo;
    private String bankName;
    private String bankAccount;
    private Integer status;
}
