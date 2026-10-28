package com.guolian.pm.flow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.common.web.LoginUser;
import com.guolian.pm.flow.dto.FlowDtos;
import com.guolian.pm.flow.entity.SysFlowBiz;
import com.guolian.pm.flow.event.FlowFinishedEvent;
import com.guolian.pm.flow.mapper.SysFlowBizMapper;
import com.guolian.pm.system.user.entity.SysUser;
import com.guolian.pm.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flowable 统一封装：业务模块只调本类，不直接依赖 Flowable API。
 * 覆盖：提交、同意、驳回（可指定节点）、加签、转办、撤回、待办/已办/我发起/流水。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final SysFlowBizMapper flowBizMapper;
    private final FlowDefService flowDefService;
    private final BpmnGenerator bpmnGenerator;
    private final ApproverResolver approverResolver;
    private final SysUserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 通用发起。businessKey = 业务单号，实例与任务全部落在 Flowable 原生表。
     */
    @Transactional
    public String start(FlowDtos.StartRequest request, LoginUser initiator) {
        Long running = flowBizMapper.selectCount(Wrappers.<SysFlowBiz>lambdaQuery()
                .eq(SysFlowBiz::getBizType, request.getBizType())
                .eq(SysFlowBiz::getBizId, request.getBizId())
                .eq(SysFlowBiz::getStatus, "RUNNING"));
        if (running != null && running > 0) {
            throw BizException.biz("该单据已有进行中的审批流程，请勿重复发起");
        }
        var def = flowDefService.findByBizType(request.getBizType());
        if (def == null || !StringUtils.hasText(def.getProcessDefinitionKey())) {
            throw BizException.biz("该业务未配置已发布的审批流程");
        }
        List<FlowDtos.NodeConfig> nodes = bpmnGenerator.parseNodes(def.getNodeConfig());

        Map<String, Object> variables = new HashMap<>();
        if (request.getVariables() != null) {
            variables.putAll(request.getVariables());
        }
        variables.put("initiator", String.valueOf(initiator.getUserId()));
        variables.put("initiatorOrgId", initiator.getOrgId());

        for (FlowDtos.NodeConfig node : nodes) {
            if ("START".equals(node.getType()) || "END".equals(node.getType()) || "CONDITION".equals(node.getType())) {
                continue;
            }
            if ("CANDIDATE".equals(node.getType())) {
                if (!StringUtils.hasText(node.getRoleCode())) {
                    throw BizException.biz("节点「" + node.getName() + "」为候选审批，必须配置审批角色");
                }
                variables.put("grp_" + node.getKey(), node.getRoleCode());
                continue;
            }
            List<Long> approvers = approverResolver.resolve(node, initiator, variables);
            approverResolver.requireNotEmpty(node, approvers);
            if ("ALL_SIGN".equals(node.getType()) || "OR_SIGN".equals(node.getType())) {
                variables.put("users_" + node.getKey(), approvers.stream().map(String::valueOf).toList());
            } else {
                variables.put("apv_" + node.getKey(), String.valueOf(approvers.get(0)));
            }
        }

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                def.getProcessDefinitionKey(),
                StringUtils.hasText(request.getBizNo()) ? request.getBizNo() : String.valueOf(request.getBizId()),
                variables);

        SysFlowBiz biz = new SysFlowBiz();
        biz.setBizType(request.getBizType());
        biz.setBizId(request.getBizId());
        biz.setBizNo(request.getBizNo());
        biz.setBizTitle(request.getBizTitle());
        biz.setProcessInstanceId(instance.getId());
        biz.setFlowCode(def.getFlowCode());
        biz.setFlowVersion(instance.getProcessDefinitionVersion());
        biz.setInitiatorId(initiator.getUserId());
        biz.setInitiatorOrgId(initiator.getOrgId());
        biz.setStatus("RUNNING");
        biz.setCurrentNodeName(currentNodeName(instance.getId()));
        biz.setStartTime(LocalDateTime.now());
        biz.setDetailRoute(request.getDetailRoute());
        flowBizMapper.insert(biz);
        return instance.getId();
    }

    @Transactional
    public void approve(String taskId, String comment, LoginUser user) {
        Task task = requireTask(taskId, user);
        String pi = task.getProcessInstanceId();
        if (StringUtils.hasText(comment)) {
            taskService.addComment(taskId, pi, comment);
        }
        taskService.complete(taskId);
        refreshAfterChange(pi);
    }

    /**
     * 驳回：指定节点用 moveActivityIdTo（比删实例重走更规范）；未指定则终止流程并标记已驳回。
     */
    @Transactional
    public void reject(String taskId, String comment, String targetNodeKey, LoginUser user) {
        Task task = requireTask(taskId, user);
        String pi = task.getProcessInstanceId();
        taskService.addComment(taskId, pi, "驳回：" + (comment == null ? "" : comment));
        if (StringUtils.hasText(targetNodeKey)) {
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(pi)
                    .moveActivityIdTo(task.getTaskDefinitionKey(), targetNodeKey)
                    .changeState();
            refreshAfterChange(pi);
        } else {
            runtimeService.deleteProcessInstance(pi, "驳回：" + comment);
            finish(pi, "REJECTED", false);
        }
    }

    @Transactional
    public void addSign(String taskId, Long targetUserId, String comment, LoginUser user) {
        Task task = requireTask(taskId, user);
        taskService.addComment(taskId, task.getProcessInstanceId(), "加签给用户 " + targetUserId + "：" + comment);
        taskService.delegateTask(taskId, String.valueOf(targetUserId));
    }

    @Transactional
    public void resolveSign(String taskId, String comment, LoginUser user) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw BizException.notFound("待办任务不存在或已被处理");
        }
        taskService.addComment(taskId, task.getProcessInstanceId(), "加签处理完成：" + (comment == null ? "" : comment));
        taskService.resolveTask(taskId);
    }

    @Transactional
    public void transferTask(String taskId, Long targetUserId, String comment, LoginUser user) {
        Task task = requireTask(taskId, user);
        taskService.addComment(taskId, task.getProcessInstanceId(), "转办给用户 " + targetUserId + "：" + comment);
        taskService.setAssignee(taskId, String.valueOf(targetUserId));
    }

    /**
     * 撤回：仅首个节点未处理时允许。
     */
    @Transactional
    public void withdraw(String processInstanceId, String reason, LoginUser user) {
        SysFlowBiz biz = flowBizMapper.selectOne(Wrappers.<SysFlowBiz>lambdaQuery()
                .eq(SysFlowBiz::getProcessInstanceId, processInstanceId));
        if (biz == null) {
            throw BizException.notFound("流程记录不存在");
        }
        if (!biz.getInitiatorId().equals(user.getUserId())) {
            throw BizException.forbidden("只有发起人可以撤回");
        }
        if (!"RUNNING".equals(biz.getStatus())) {
            throw BizException.biz("流程已结束，无法撤回");
        }
        long done = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .count();
        if (done > 0) {
            throw BizException.biz("已有节点处理过，无法撤回");
        }
        runtimeService.deleteProcessInstance(processInstanceId, "发起人撤回：" + reason);
        finish(processInstanceId, "WITHDRAWN", false);
    }

    @Transactional
    public void terminate(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId, reason);
        finish(processInstanceId, "TERMINATED", false);
    }

    // ---------------- 查询 ----------------

    public List<FlowDtos.TaskVO> todoTasks(LoginUser user) {
        String uid = String.valueOf(user.getUserId());
        var query = taskService.createTaskQuery().or()
                .taskAssignee(uid)
                .taskCandidateUser(uid);
        if (user.getRoleCodes() != null && !user.getRoleCodes().isEmpty()) {
            query.taskCandidateGroupIn(user.getRoleCodes());
        }
        List<Task> tasks = query.endOr().orderByTaskCreateTime().desc().list();
        return toTaskVO(tasks);
    }

    public List<FlowDtos.TaskVO> doneTasks(LoginUser user) {
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(String.valueOf(user.getUserId()))
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();
        List<FlowDtos.TaskVO> result = new ArrayList<>();
        for (HistoricTaskInstance task : list) {
            FlowDtos.TaskVO vo = new FlowDtos.TaskVO();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getName());
            vo.setProcessInstanceId(task.getProcessInstanceId());
            vo.setStartTime(format(task.getStartTime()));
            enrich(vo, task.getProcessInstanceId());
            result.add(vo);
        }
        return result;
    }

    public List<FlowDtos.InstanceVO> myInstances(LoginUser user, String status) {
        var wrapper = Wrappers.<SysFlowBiz>lambdaQuery().eq(SysFlowBiz::getInitiatorId, user.getUserId());
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysFlowBiz::getStatus, status);
        }
        wrapper.orderByDesc(SysFlowBiz::getStartTime);
        List<FlowDtos.InstanceVO> result = new ArrayList<>();
        for (SysFlowBiz biz : flowBizMapper.selectList(wrapper)) {
            FlowDtos.InstanceVO vo = new FlowDtos.InstanceVO();
            vo.setProcessInstanceId(biz.getProcessInstanceId());
            vo.setBizType(biz.getBizType());
            vo.setBizNo(biz.getBizNo());
            vo.setBizTitle(biz.getBizTitle());
            vo.setStatus(biz.getStatus());
            vo.setCurrentNodeName(biz.getCurrentNodeName());
            vo.setInitiatorName(userName(biz.getInitiatorId()));
            vo.setStartTime(biz.getStartTime() == null ? null : biz.getStartTime().format(FMT));
            vo.setEndTime(biz.getEndTime() == null ? null : biz.getEndTime().format(FMT));
            vo.setDetailRoute(biz.getDetailRoute());
            result.add(vo);
        }
        return result;
    }

    /** 抄送：底座未引入抄送表，返回空列表（业务模块可扩展） */
    public List<FlowDtos.InstanceVO> ccInstances(LoginUser user) {
        return new ArrayList<>();
    }

    public List<FlowDtos.HistoryVO> history(String processInstanceId) {
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime().asc()
                .list();
        Map<String, String> comments = new HashMap<>();
        for (var comment : taskService.getProcessInstanceComments(processInstanceId)) {
            if (comment.getTaskId() != null && !comments.containsKey(comment.getTaskId())) {
                comments.put(comment.getTaskId(), comment.getFullMessage());
            }
        }
        List<FlowDtos.HistoryVO> result = new ArrayList<>();
        for (HistoricTaskInstance task : tasks) {
            FlowDtos.HistoryVO vo = new FlowDtos.HistoryVO();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getName());
            vo.setAssigneeName(userName(task.getAssignee()));
            vo.setStartTime(format(task.getStartTime()));
            vo.setEndTime(format(task.getEndTime()));
            vo.setDuration(task.getDurationInMillis() == null ? null
                    : (task.getDurationInMillis() / 60000) + " 分钟");
            vo.setComment(comments.get(task.getId()));
            vo.setFinished(task.getEndTime() != null);
            result.add(vo);
        }
        return result;
    }

    public SysFlowBiz bizByInstance(String processInstanceId) {
        return flowBizMapper.selectOne(Wrappers.<SysFlowBiz>lambdaQuery()
                .eq(SysFlowBiz::getProcessInstanceId, processInstanceId));
    }

    // ---------------- 内部 ----------------

    private Task requireTask(String taskId, LoginUser user) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw BizException.notFound("待办任务不存在或已被处理");
        }
        String uid = String.valueOf(user.getUserId());
        boolean assignee = uid.equals(task.getAssignee());
        boolean candidate = taskService.createTaskQuery().taskId(taskId)
                .taskCandidateUser(uid).count() > 0;
        boolean candidateGroup = user.getRoleCodes() != null && !user.getRoleCodes().isEmpty()
                && taskService.createTaskQuery().taskId(taskId)
                .taskCandidateGroupIn(user.getRoleCodes()).count() > 0;
        if (!assignee && !candidate && !candidateGroup) {
            throw BizException.forbidden("该任务不属于当前用户");
        }
        return task;
    }

    private void refreshAfterChange(String processInstanceId) {
        long running = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).count();
        SysFlowBiz biz = bizByInstance(processInstanceId);
        if (biz == null) {
            return;
        }
        if (running == 0) {
            finish(processInstanceId, "APPROVED", true);
            return;
        }
        SysFlowBiz update = new SysFlowBiz();
        update.setId(biz.getId());
        update.setCurrentNodeName(currentNodeName(processInstanceId));
        flowBizMapper.updateById(update);
    }

    private void finish(String processInstanceId, String status, boolean approved) {
        SysFlowBiz biz = bizByInstance(processInstanceId);
        if (biz == null) {
            return;
        }
        SysFlowBiz update = new SysFlowBiz();
        update.setId(biz.getId());
        update.setStatus(status);
        update.setEndTime(LocalDateTime.now());
        update.setCurrentNodeName("已结束");
        flowBizMapper.updateById(update);
        eventPublisher.publishEvent(new FlowFinishedEvent(biz.getBizType(), biz.getBizId(), processInstanceId, approved));
    }

    private String currentNodeName(String processInstanceId) {
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        if (tasks.isEmpty()) {
            return "已结束";
        }
        return tasks.stream().map(Task::getName).distinct()
                .collect(java.util.stream.Collectors.joining("、"));
    }

    private List<FlowDtos.TaskVO> toTaskVO(List<Task> tasks) {
        List<FlowDtos.TaskVO> result = new ArrayList<>();
        for (Task task : tasks) {
            FlowDtos.TaskVO vo = new FlowDtos.TaskVO();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getName());
            vo.setProcessInstanceId(task.getProcessInstanceId());
            vo.setNodeKey(task.getTaskDefinitionKey());
            vo.setStartTime(format(task.getCreateTime()));
            enrich(vo, task.getProcessInstanceId());
            result.add(vo);
        }
        return result;
    }

    private void enrich(FlowDtos.TaskVO vo, String processInstanceId) {
        SysFlowBiz biz = bizByInstance(processInstanceId);
        if (biz != null) {
            vo.setBizType(biz.getBizType());
            vo.setBizNo(biz.getBizNo());
            vo.setBizTitle(biz.getBizTitle());
            vo.setDetailRoute(biz.getDetailRoute());
            vo.setInitiatorName(userName(biz.getInitiatorId()));
        }
    }

    private String userName(String assignee) {
        if (!StringUtils.hasText(assignee)) {
            return null;
        }
        try {
            return userName(Long.valueOf(assignee));
        } catch (NumberFormatException e) {
            return assignee;
        }
    }

    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        return user == null ? String.valueOf(userId) : user.getRealName();
    }

    private String format(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(FMT);
    }
}
