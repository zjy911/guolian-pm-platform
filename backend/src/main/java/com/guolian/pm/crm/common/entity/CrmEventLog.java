package com.guolian.pm.crm.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CRM 埋点事件。底座未提供独立埋点服务，按方案要求落本表并在此声明为临时方案。
 * 埋点一律服务端上报，禁止前端直报（避免越过数据权限泄露）。
 */
@Data
@TableName("crm_event_log")
public class CrmEventLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String eventCode;
    private Long bizId;
    private Long operatorId;
    private Long operatorOrgId;
    private String props;
    private LocalDateTime createdTime;
    private Integer isDeleted;
}
