package com.guolian.pm.transfer.adapter;

import com.guolian.pm.system.role.entity.SysRole;
import com.guolian.pm.system.role.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.identitylink.api.IdentityLink;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 内置适配器 2：审批任务（Flowable taskService.setAssignee 改派）。
 * 接收人需在节点审批人范围内，否则该项标记失败。
 */
@Component
@RequiredArgsConstructor
public class FlowTaskTransferAdapter implements TransferAdapter {

    private final TaskService taskService;
    private final SysRoleMapper roleMapper;

    @Override
    public String bizType() {
        return "FLOW_TASK";
    }

    @Override
    public String bizName() {
        return "审批任务";
    }

    @Override
    public long count(Long fromUserId, LocalDateTime since) {
        var query = taskService.createTaskQuery().taskAssignee(String.valueOf(fromUserId));
        if (since != null) {
            query.taskCreatedAfter(java.sql.Timestamp.valueOf(since));
        }
        return query.count();
    }

    @Override
    public List<Long> listIds(Long fromUserId, int offset, int limit) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(String.valueOf(fromUserId))
                .orderByTaskCreateTime().asc()
                .listPage(offset, limit);
        return tasks.stream()
                .map(Task::getId)
                .filter(id -> id.matches("\\d+"))
                .map(Long::valueOf)
                .toList();
    }

    @Override
    public TransferOutcome transfer(Long fromUserId, Long toUserId, List<Long> bizIds) {
        List<Long> succeeded = new ArrayList<>();
        Map<Long, String> failed = new LinkedHashMap<>();
        Set<String> receiverRoles = roleMapper.selectEnabledRolesByUser(toUserId).stream()
                .map(SysRole::getRoleCode).collect(Collectors.toSet());
        for (Long bizId : bizIds) {
            Task task = taskService.createTaskQuery().taskId(String.valueOf(bizId)).singleResult();
            if (task == null) {
                failed.put(bizId, "任务不存在或已办结");
                continue;
            }
            if (!String.valueOf(fromUserId).equals(task.getAssignee())) {
                failed.put(bizId, "任务归属已变更");
                continue;
            }
            List<IdentityLink> links = taskService.getIdentityLinksForTask(task.getId());
            Set<String> candidateGroups = links.stream()
                    .filter(l -> "candidate".equals(l.getType()) && l.getGroupId() != null)
                    .map(IdentityLink::getGroupId)
                    .collect(Collectors.toSet());
            if (!candidateGroups.isEmpty() && candidateGroups.stream().noneMatch(receiverRoles::contains)) {
                failed.put(bizId, "接收人不在该节点审批人范围内");
                continue;
            }
            taskService.setAssignee(task.getId(), String.valueOf(toUserId));
            succeeded.add(bizId);
        }
        return new TransferOutcome(succeeded, failed);
    }

    @Override
    public void rollback(List<RollbackItem> items) {
        for (RollbackItem item : items) {
            Task task = taskService.createTaskQuery().taskId(String.valueOf(item.bizId())).singleResult();
            if (task == null) {
                continue;
            }
            taskService.setAssignee(task.getId(), String.valueOf(item.beforeOwnerId()));
        }
    }

    @Override
    public String describe(Long bizId) {
        Task task = taskService.createTaskQuery().taskId(String.valueOf(bizId)).singleResult();
        return task == null ? "审批任务 #" + bizId : "审批：" + task.getName();
    }
}
