package com.guolian.pm.flow.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.flow.dto.FlowDtos;
import com.guolian.pm.flow.entity.SysFlowDef;
import com.guolian.pm.flow.service.FlowDefService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 审批模板配置：表单化配置节点，发布时生成 BPMN 并部署。
 */
@RestController
@RequestMapping("/api/v1/system/flows")
@RequiredArgsConstructor
public class FlowDefController {

    private final FlowDefService flowDefService;

    @GetMapping
    @Perm("system:flow:list")
    public ApiResponse<List<SysFlowDef>> list(@RequestParam(required = false) String bizType,
                                              @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(flowDefService.list(bizType, status));
    }

    @GetMapping("/{id}")
    @Perm("system:flow:list")
    public ApiResponse<SysFlowDef> get(@PathVariable Long id) {
        return ApiResponse.ok(flowDefService.get(id));
    }

    @PostMapping
    @Perm("system:flow:add")
    @OpLog(module = "审批流程", action = "新增审批模板")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody FlowDtos.DefRequest request) {
        request.setId(null);
        return ApiResponse.ok(Map.of("id", flowDefService.save(request)));
    }

    @PutMapping("/{id}")
    @Perm("system:flow:edit")
    @OpLog(module = "审批流程", action = "修改审批模板")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody FlowDtos.DefRequest request) {
        request.setId(id);
        flowDefService.save(request);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/publish")
    @Perm("system:flow:publish")
    @OpLog(module = "审批流程", action = "发布审批模板")
    public ApiResponse<SysFlowDef> publish(@PathVariable Long id) {
        return ApiResponse.ok(flowDefService.publish(id));
    }

    @DeleteMapping("/{id}")
    @Perm("system:flow:remove")
    @OpLog(module = "审批流程", action = "删除审批模板")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        flowDefService.delete(id);
        return ApiResponse.ok();
    }
}
