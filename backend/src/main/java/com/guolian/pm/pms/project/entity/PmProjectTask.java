package com.guolian.pm.pms.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目任务实例：立项通过后按模板快照一次性实例化（AC-03）。
 * 任务类型：FORM 表单 / DATA 数据 / COLLAB 协同（PRD 5.4）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_project_task")
public class PmProjectTask extends BaseEntity {

    private Long projectId;
    private Integer phase;
    private String category;
    private String taskName;
    private String taskType;
    private String formCode;
    private Integer blocking;
    private Long dependsTaskId;
    private String status;
    private Long assigneeId;
    private String assigneeName;
    private LocalDate dueDate;
    private Integer sortNo;
    private LocalDateTime completedTime;
}
