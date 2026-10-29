package com.guolian.pm.pms.form.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 表单实例：38 类表单对象的通用承载，扩展字段以 JSON 存储（PRD 七）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_form_instance")
public class PmFormInstance extends BaseEntity {

    private Long projectId;
    private Long taskId;
    private String formCode;
    private String formData;
    private String status;
    private String flowInstanceId;
    private Long submitUserId;
    private String submitUserName;
    private LocalDateTime submitTime;
    private LocalDateTime approveTime;
    private String approveComment;
}
