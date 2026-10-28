package com.guolian.pm.system.role.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RoleVO {

    private Long id;
    private String roleCode;
    private String roleName;
    private Long orgId;
    private String orgName;
    private String orgPath;
    private String description;
    private Integer roleType;
    private String dataScope;
    private String dataScopeLabel;
    private String scopeValue;
    private Integer canBeProjectMember;
    private Integer canBeApprover;
    private Integer sortNo;
    private Integer status;
    private String remark;
    private Long userCount;
    private List<Long> menuIds = new ArrayList<>();
}
