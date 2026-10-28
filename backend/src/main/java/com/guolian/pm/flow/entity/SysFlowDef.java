package com.guolian.pm.flow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批模板配置（表单化配置，保存时生成 BPMN 并部署到 Flowable）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_flow_def")
public class SysFlowDef extends BaseEntity {

    private String flowCode;
    private String flowName;
    private String bizType;
    private String orgScope;
    private String processDefinitionKey;
    private Integer deployedVersion;
    /** 节点配置 JSON 快照 */
    @TableField("node_config")
    private String nodeConfig;
    private Integer status;
    private String formCode;
}
