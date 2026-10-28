package com.guolian.pm.flow.event;

/**
 * 审批流程结束事件（通过 / 驳回 / 撤回）。
 */
public record FlowFinishedEvent(String bizType, Long bizId, String processInstanceId, boolean approved) {
}
