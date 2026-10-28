package com.guolian.pm.flow.controller;

import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.flow.dto.FlowDtos;
import com.guolian.pm.flow.service.FlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 审批中心：待办 / 已办 / 我发起的 / 抄送我的 + 五个动作。
 * 不做一级菜单，通过 Topbar 待办入口进入。
 */
@RestController
@RequestMapping("/api/v1/flow")
@RequiredArgsConstructor
public class FlowController {

    private final FlowService flowService;

    @PostMapping("/start")
    @OpLog(module = "审批中心", action = "发起审批")
    public ApiResponse<Map<String, String>> start(@Valid @RequestBody FlowDtos.StartRequest request) {
        String instanceId = flowService.start(request, AuthContext.require());
        return ApiResponse.ok(Map.of("processInstanceId", instanceId));
    }

    @GetMapping("/tasks/todo")
    public ApiResponse<List<FlowDtos.TaskVO>> todo() {
        return ApiResponse.ok(flowService.todoTasks(AuthContext.require()));
    }

    @GetMapping("/tasks/done")
    public ApiResponse<List<FlowDtos.TaskVO>> done() {
        return ApiResponse.ok(flowService.doneTasks(AuthContext.require()));
    }

    @GetMapping("/instances/mine")
    public ApiResponse<List<FlowDtos.InstanceVO>> mine(@RequestParam(required = false) String status) {
        return ApiResponse.ok(flowService.myInstances(AuthContext.require(), status));
    }

    @GetMapping("/instances/cc")
    public ApiResponse<List<FlowDtos.InstanceVO>> cc() {
        return ApiResponse.ok(flowService.ccInstances(AuthContext.require()));
    }

    @GetMapping("/instances/{id}/history")
    public ApiResponse<List<FlowDtos.HistoryVO>> history(@PathVariable String id) {
        return ApiResponse.ok(flowService.history(id));
    }

    @PostMapping("/tasks/{taskId}/approve")
    @OpLog(module = "审批中心", action = "审批同意")
    public ApiResponse<Void> approve(@PathVariable String taskId,
                                     @RequestBody(required = false) FlowDtos.ActionRequest request) {
        flowService.approve(taskId, request == null ? "同意" : request.getComment(), AuthContext.require());
        return ApiResponse.ok();
    }

    @PostMapping("/tasks/{taskId}/reject")
    @OpLog(module = "审批中心", action = "审批驳回")
    public ApiResponse<Void> reject(@PathVariable String taskId,
                                    @RequestBody(required = false) FlowDtos.ActionRequest request) {
        FlowDtos.ActionRequest req = request == null ? new FlowDtos.ActionRequest() : request;
        flowService.reject(taskId, req.getComment(), req.getTargetNodeKey(), AuthContext.require());
        return ApiResponse.ok();
    }

    @PostMapping("/tasks/{taskId}/add-sign")
    @OpLog(module = "审批中心", action = "加签")
    public ApiResponse<Void> addSign(@PathVariable String taskId, @RequestBody FlowDtos.ActionRequest request) {
        flowService.addSign(taskId, request.getUserId(), request.getComment(), AuthContext.require());
        return ApiResponse.ok();
    }

    @PostMapping("/tasks/{taskId}/resolve")
    @OpLog(module = "审批中心", action = "加签处理完成")
    public ApiResponse<Void> resolve(@PathVariable String taskId,
                                     @RequestBody(required = false) FlowDtos.ActionRequest request) {
        flowService.resolveSign(taskId, request == null ? null : request.getComment(), AuthContext.require());
        return ApiResponse.ok();
    }

    @PostMapping("/tasks/{taskId}/transfer")
    @OpLog(module = "审批中心", action = "转办")
    public ApiResponse<Void> transfer(@PathVariable String taskId, @RequestBody FlowDtos.ActionRequest request) {
        flowService.transferTask(taskId, request.getUserId(), request.getComment(), AuthContext.require());
        return ApiResponse.ok();
    }

    @PostMapping("/instances/{id}/withdraw")
    @OpLog(module = "审批中心", action = "撤回审批")
    public ApiResponse<Void> withdraw(@PathVariable String id, @RequestBody FlowDtos.ActionRequest request) {
        flowService.withdraw(id, request.getReason(), AuthContext.require());
        return ApiResponse.ok();
    }
}
