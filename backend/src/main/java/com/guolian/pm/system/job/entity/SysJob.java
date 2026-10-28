package com.guolian.pm.system.job.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job")
public class SysJob extends BaseEntity {

    private String jobName;
    private String jobCode;
    private String cron;
    private Integer status;
    private LocalDateTime lastExecTime;
    private String remark;
}
