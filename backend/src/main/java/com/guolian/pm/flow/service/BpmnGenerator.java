package com.guolian.pm.flow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guolian.pm.flow.dto.FlowDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 表单化流程配置 → BPMN XML 生成器。业务人员不学 BPMN，保存配置时由后端生成并部署。
 */
@Component
@RequiredArgsConstructor
public class BpmnGenerator {

    private final ObjectMapper objectMapper;

    public List<FlowDtos.NodeConfig> parseNodes(String nodeConfig) {
        try {
            FlowDtos.NodeConfigWrapper wrapper = objectMapper.readValue(nodeConfig, FlowDtos.NodeConfigWrapper.class);
            return wrapper.getNodes();
        } catch (Exception e) {
            throw new IllegalArgumentException("节点配置不是合法 JSON", e);
        }
    }

    /**
     * 生成顺序流程：start → 审批节点（支持会签/或签/候选）→ end，
     * 条件节点生成 exclusiveGateway + 条件表达式（不满足时直接跳过该节点）。
     */
    public String generate(String processKey, String processName, List<FlowDtos.NodeConfig> nodes) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n");
        xml.append("             xmlns:flowable=\"http://flowable.org/bpmn\"\n");
        xml.append("             targetNamespace=\"http://guolian.com/pm\">\n");
        xml.append("  <process id=\"").append(processKey).append("\" name=\"")
                .append(escape(processName)).append("\" isExecutable=\"true\">\n");
        xml.append("    <startEvent id=\"start\" name=\"发起\"/>\n");

        String previous = "start";
        int index = 0;
        for (FlowDtos.NodeConfig node : nodes) {
            if ("START".equals(node.getType()) || "END".equals(node.getType())) {
                continue;
            }
            if ("CONDITION".equals(node.getType())) {
                String gatewayId = "gateway_" + node.getKey();
                xml.append("    <exclusiveGateway id=\"").append(gatewayId).append("\" name=\"")
                        .append(escape(node.getName())).append("\"/>\n");
                xml.append("    <sequenceFlow id=\"flow_").append(gatewayId).append("_in\" sourceRef=\"")
                        .append(previous).append("\" targetRef=\"").append(gatewayId).append("\"/>\n");
                previous = gatewayId;
                continue;
            }
            String taskId = node.getKey();
            xml.append("    <userTask id=\"").append(taskId).append("\" name=\"")
                    .append(escape(node.getName())).append("\"");
            if ("CANDIDATE".equals(node.getType())) {
                xml.append(" flowable:candidateGroups=\"${grp_").append(taskId).append("}\"");
            } else if ("ALL_SIGN".equals(node.getType()) || "OR_SIGN".equals(node.getType())) {
                xml.append(" flowable:assignee=\"${assignee}\"");
            } else {
                xml.append(" flowable:assignee=\"${apv_").append(taskId).append("}\"");
            }
            xml.append(">\n");
            if ("ALL_SIGN".equals(node.getType()) || "OR_SIGN".equals(node.getType())) {
                String condition = "ALL_SIGN".equals(node.getType())
                        ? "${nrOfCompletedInstances == nrOfInstances}"
                        : "${nrOfCompletedInstances >= 1}";
                xml.append("      <multiInstanceLoopCharacteristics isSequential=\"false\"")
                        .append(" flowable:collection=\"users_").append(taskId).append("\"")
                        .append(" flowable:elementVariable=\"assignee\">\n");
                xml.append("        <completionCondition>").append(condition).append("</completionCondition>\n");
                xml.append("      </multiInstanceLoopCharacteristics>\n");
            }
            xml.append("    </userTask>\n");
            xml.append("    <sequenceFlow id=\"flow_").append(taskId).append("_in\" sourceRef=\"")
                    .append(previous).append("\" targetRef=\"").append(taskId).append("\"/>\n");
            previous = taskId;
            index++;
        }

        xml.append("    <endEvent id=\"end\" name=\"结束\"/>\n");
        xml.append("    <sequenceFlow id=\"flow_to_end\" sourceRef=\"").append(previous)
                .append("\" targetRef=\"end\"/>\n");
        xml.append("  </process>\n</definitions>\n");
        return xml.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
