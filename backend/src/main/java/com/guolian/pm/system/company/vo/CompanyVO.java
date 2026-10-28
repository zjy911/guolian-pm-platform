package com.guolian.pm.system.company.vo;

import com.guolian.pm.system.sensitive.annotation.Sensitive;
import lombok.Data;

@Data
public class CompanyVO {

    private Long id;
    private String companyCode;
    private String companyName;
    private String shortName;
    private String companyType;
    private Long parentCompanyId;
    private String legalPerson;
    private String creditCode;

    @Sensitive(group = "ADDRESS", bizObject = "company")
    private String regAddress;

    @Sensitive(group = "ADDRESS", bizObject = "company")
    private String officeAddress;

    private String taxNo;
    private String bankName;

    @Sensitive(group = "BANK_ACCOUNT", bizObject = "company")
    private String bankAccount;

    private Integer status;
    private Long userCount;
}
