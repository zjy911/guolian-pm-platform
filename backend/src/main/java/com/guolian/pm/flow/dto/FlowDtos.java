package com.guolian.pm.flow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowDtos {

    /** 表单化流程配置：nodes 为节点 JSON 快照 */
    @Data
    public static class DefRequest {
        private Long id;

        @NotBlank(message = "请输入流程编码")
        private String flowCode;

        @NotBlank(message = "请输入流程名称")
        private String flowName;

        @NotBlank(message = "请选择业务类型")
        private String bizType;

        private String orgScope;
        private String processDefinitionKey;
        private String formCode;
        private Integer status = 0;

        @NotBlank(message = "请配置流程节点")
        private String nodeConfig;
    }

    @Data
    public static class StartRequest {
        @NotBlank(message = "业务类型不能为空")
        private String bizType;

        @NotNull(message = "业务单据 ID 不能为空")
        private Long bizId;

        private String bizNo;
        private String bizTitle;
        private String detailRoute;
        private Map<String, Object> variables = new HashMap<>();
    }

    @Data
    public static class ActionRequest {
        @NotBlank(message = "请填写审批意见")
        private String comment;

        /** 驳回到指定节点（可选） */
        private String targetNodeKey;

        /** 加签/转办的目标用户 */
        private Long userId;

        private String reason;
    }

    @Data
    public static class TaskVO {
        private String taskId;
        private String taskName;
        private String processInstanceId;
        private String bizType;
        private String bizNo;
        private String bizTitle;
        private String initiatorName;
        private String startTime;
        private String detailRoute;
        private String nodeKey;
    }

    @Data
    public static class HistoryVO {
        private String taskId;
        private String taskName;
        private String assigneeName;
        private String startTime;
        private String endTime;
        private String duration;
        private String comment;
        private boolean finished;
    }

    @Data
    public static class InstanceVO {
        private String processInstanceId;
        private String bizType;
        private String bizNo;
        private String bizTitle;
        private String status;
        private String currentNodeName;
        private String initiatorName;
        private String startTime;
        private String endTime;
        private String detailRoute;
        private List<NodeConfig> nodes = new ArrayList<>();
    }

    @Data
    public static class NodeConfig {
        private String key;
        private String name;
        /** START / SINGLE / CANDIDATE / ALL_SIGN / OR_SIGN / CONDITION / CC / END */
        private String type;
        /** LEADER / ORG_LEADER / ROLE / USER / INITIATOR */
        private String approverType;
        private Integer level;
        private String roleCode;
        private List<Long> userIds = new ArrayList<>();
        private String expression;
    }

    @Data
    public static class NodeConfigWrapper {
        private List<NodeConfig> nodes = new ArrayList<>();
    }
}
