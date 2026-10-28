package com.guolian.pm.flow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 业务单据映射：把 Flowable 任务/实例映射回业务标题与跳转路由。
 * 实例与任务本身使用 ACT_* 原生表，这里不重复造。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_flow_biz")
public class SysFlowBiz extends BaseEntity {

    private String bizType;
    private Long bizId;
    private String bizNo;
    private String bizTitle;
    private String processInstanceId;
    private String flowCode;
    private Integer flowVersion;
    private Long initiatorId;
    private Long initiatorOrgId;
    /** RUNNING / APPROVED / REJECTED / WITHDRAWN / TERMINATED */
    private String status;
    private String currentNodeName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String detailRoute;
}
