package com.guolian.pm.crm.common.dto;

import com.guolian.pm.common.api.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * CRM 通用列表查询入参：9 个对象共用一套筛选字段，避免为每个对象各建一个 Query 类。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmQuery extends PageQuery {

    /** 关键字：名称 / 公司名 / 单号 */
    private String keyword;
    /** 通用状态（对象各自的 status / bidStatus 由 status 承载） */
    private String status;
    private Long ownerUserId;
    private LocalDate startDate;
    private LocalDate endDate;

    /** 客户 */
    private Long accountId;
    private String poolStatus;
    private String accountType;
    private String accountNature;
    private String industry;
    private String province;

    /** 线索 */
    private String channel;
    private String grade;

    /** 商机 */
    private String stage;

    /** 投标 */
    private String tenderForm;
    private Long regionOrgId;
    /** yyyy-MM，开标日历用 */
    private String month;

    /** 活动 */
    private String targetType;
    private Long targetId;
    private String activityType;

    /** 合同 / 协议 */
    private Long businessGroupId;

    /** 是否只统计潜客（null = 默认排除潜客） */
    private Boolean includeProspect;
}
