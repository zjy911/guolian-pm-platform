package com.guolian.pm.transfer.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TransferDtos {

    @Data
    public static class CreateRequest {
        @NotNull(message = "请选择交出人")
        private Long fromUserId;

        @NotEmpty(message = "请选择接收人")
        private List<Long> toUserIds = new ArrayList<>();

        /** 1 单接收人 / 2 按角色分派 */
        private Integer assignMode = 1;

        @NotEmpty(message = "请选择交接业务范围")
        private List<String> scope = new ArrayList<>();

        private String remark;
        private LocalDateTime effectiveTime;

        /** 按角色分派：roleId -> userId */
        private Map<Long, Long> roleAssignments = new LinkedHashMap<>();
    }

    @Data
    public static class AdapterInfo {
        private String bizType;
        private String bizName;
        private long count;
        private boolean available;
    }

    @Data
    public static class RoleBrief {
        private Long roleId;
        private String roleCode;
        private String roleName;
        private Long orgId;
        private String orgName;
        private boolean isDefault;
    }

    @Data
    public static class UserBrief {
        private Long userId;
        private String username;
        private String realName;
        private String orgName;
    }

    @Data
    public static class PrecheckVO {
        private Long fromUserId;
        private String fromUserName;
        private List<RoleBrief> roles = new ArrayList<>();
        private List<AdapterInfo> adapters = new ArrayList<>();
        private long totalCount;
    }

    @Data
    public static class DetailVO {
        private Long id;
        private String transferNo;
        private Long fromUserId;
        private String fromUserName;
        private List<Long> toUserIds = new ArrayList<>();
        private String toUserNames;
        private Integer assignMode;
        private List<String> scope = new ArrayList<>();
        private String remark;
        private String flowStatus;
        private String execStatus;
        private String flowInstanceId;
        private Integer itemCount;
        private Integer failCount;
        private boolean revocable;
        private String revokeDeadline;
        private String createdTime;
        private LocalDateTime effectiveTime;
    }

    @Data
    public static class ItemVO {
        private Long id;
        private String bizType;
        private Long bizId;
        private String bizName;
        private Long beforeOwnerId;
        private String beforeOwnerName;
        private Long afterOwnerId;
        private String afterOwnerName;
        private String result;
        private String failReason;
    }
}
