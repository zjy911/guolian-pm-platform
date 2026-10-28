package com.guolian.pm.system.user.dto;

import lombok.Data;

@Data
public class UserCompanyBrief {

    private Long userId;
    private Long companyId;
    private String companyName;
    private Integer relationType;
    private Integer isPrimary;
}
