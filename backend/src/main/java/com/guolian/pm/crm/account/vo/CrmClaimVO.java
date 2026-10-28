package com.guolian.pm.crm.account.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 认领申请列表视图：把申请单、客户名称、申请人与原负责人姓名一次查出来，
 * 避免前端为每行再发两次请求（N+1）。
 */
@Data
public class CrmClaimVO {

    private Long id;
    private Long accountId;
    private String accountName;
    private Long applicantId;
    private String applicantName;
    private Long applicantOrgId;
    private Long oldOwnerId;
    private String oldOwnerName;
    /** APPROVING / APPROVED / REJECTED / WITHDRAWN */
    private String status;
    private String flowInstanceId;
    private LocalDateTime appealDeadline;
    /** NONE / APPEALED / EXPIRED */
    private String appealStatus;
    private String applyReason;
    private String remark;
    private LocalDateTime createdTime;
    private String poolStatus;
}
