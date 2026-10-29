package com.guolian.pm.pms.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目日志：立项/审批/推进/成员变更等履历留痕。
 */
@Data
@TableName("pm_project_log")
public class PmProjectLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private String action;
    private String detail;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createdTime;
}
