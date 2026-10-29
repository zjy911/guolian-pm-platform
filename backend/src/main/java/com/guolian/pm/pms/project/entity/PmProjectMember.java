package com.guolian.pm.pms.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目成员：人员全部引用 sys_user 主数据（R-09）。
 * 项目角色：PROJECT_MANAGER / IMPLEMENT_LEADER / FIELD_MEDIATOR / BUSINESS_REP / QC / OBSERVER。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_project_member")
public class PmProjectMember extends BaseEntity {

    private Long projectId;
    private Long userId;
    private String userName;
    private String projectRole;
    /** 投入比例 %，同一人跨在办项目累计不得超过 100（PRD 6.4-1） */
    private Integer inputRatio;
    private LocalDate joinDate;
    private LocalDate expectedLeaveDate;
    private String status;
    private String evaluation;
    private LocalDateTime releaseTime;
}
