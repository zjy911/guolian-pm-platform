package com.guolian.pm.system.org.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 组织树节点。故意不返回任何公司主体字段（主体与组织无关系）。
 */
@Data
public class OrgVO {

    private Long id;
    private String orgCode;
    private String orgName;
    private String orgType;
    private Long parentId;
    private String ancestors;
    private Long leaderId;
    private String leaderName;
    private String costCenterCode;
    private Integer sortNo;
    private Integer status;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private long roleCount;
    private long userCount;
    private List<OrgVO> children = new ArrayList<>();
    private List<Map<String, Object>> roles = new ArrayList<>();
    private List<Map<String, Object>> users = new ArrayList<>();
}
