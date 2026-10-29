package com.guolian.pm.pms.project.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageQuery;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.pms.common.dto.PmDtos;
import com.guolian.pm.pms.form.entity.PmFormInstance;
import com.guolian.pm.pms.form.service.FormInstanceService;
import com.guolian.pm.pms.project.entity.PmProject;
import com.guolian.pm.pms.project.entity.PmProjectMember;
import com.guolian.pm.pms.project.service.ProjectMemberService;
import com.guolian.pm.pms.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 项目管理（运行态）：立项 / 工作台 / 环节推进 / 表单任务（PRD 5.1、5.2）。
 */
@RestController
@RequestMapping("/api/v1/project/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMemberService memberService;
    private final FormInstanceService formInstanceService;

    @GetMapping
    @Perm("project:project:list")
    public ApiResponse<PageResult<PmProject>> page(PageQuery query,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Integer phase,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String healthLight) {
        return ApiResponse.ok(projectService.page(query, keyword, phase, status, healthLight));
    }

    /** 新建立项申请（生成项目 + 立项表单草稿） */
    @PostMapping("/initiations")
    @Perm("project:project:list")
    public ApiResponse<Map<String, Long>> createInitiation(@Valid @RequestBody PmDtos.InitiationRequest request) {
        return ApiResponse.ok(projectService.createInitiation(request));
    }

    @GetMapping("/{id}")
    @Perm("project:project:list")
    public ApiResponse<PmProject> detail(@PathVariable Long id) {
        return ApiResponse.ok(projectService.detail(id));
    }

    /** 单项目工作台聚合（顶部看板条 + 右侧手风琴数据源） */
    @GetMapping("/{id}/workspace")
    @Perm("project:project:list")
    public ApiResponse<Map<String, Object>> workspace(@PathVariable Long id) {
        return ApiResponse.ok(projectService.workspace(id));
    }

    /** 环节推进（R-08：阻塞任务未完成则 4xx 阻断） */
    @PostMapping("/{id}/advance")
    @Perm("project:project:list")
    public ApiResponse<PmProject> advance(@PathVariable Long id) {
        return ApiResponse.ok(projectService.advancePhase(id));
    }

    @GetMapping("/{id}/phase-progress")
    @Perm("project:project:list")
    public ApiResponse<Map<String, Integer>> phaseProgress(@PathVariable Long id, @RequestParam Integer phase) {
        return ApiResponse.ok(projectService.phaseProgress(id, phase));
    }

    // ---------------- 成员 ----------------

    @GetMapping("/{id}/members")
    @Perm("project:project:list")
    public ApiResponse<List<PmProjectMember>> members(@PathVariable Long id) {
        return ApiResponse.ok(memberService.byProject(id));
    }

    @PostMapping("/{id}/members/{userId}")
    @Perm("project:project:list")
    public ApiResponse<Map<String, Long>> assignMember(@PathVariable Long id, @PathVariable Long userId,
                                                       @Valid @RequestBody PmDtos.MemberAssignRequest request) {
        request.setProjectId(id);
        Long memberId = memberService.assign(userId, request);
        return ApiResponse.ok(Map.of("memberId", memberId));
    }

    @PutMapping("/{id}/members/{memberId}")
    @Perm("project:project:list")
    public ApiResponse<Void> updateMember(@PathVariable Long id, @PathVariable Long memberId,
                                          @RequestBody PmDtos.MemberUpdateRequest request) {
        memberService.updateMember(memberId, request);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Perm("project:project:list")
    public ApiResponse<Void> releaseMember(@PathVariable Long id, @PathVariable Long memberId,
                                           @RequestParam(required = false) String evaluation) {
        memberService.release(memberId, evaluation);
        return ApiResponse.ok(null);
    }

    // ---------------- 表单任务 ----------------

    /** 打开任务的表单（无草稿则新建） */
    @GetMapping("/tasks/{taskId}/form")
    @Perm("project:project:list")
    public ApiResponse<PmFormInstance> taskForm(@PathVariable Long taskId) {
        return ApiResponse.ok(formInstanceService.draftForTask(taskId));
    }

    @PutMapping("/forms/{formId}")
    @Perm("project:project:list")
    public ApiResponse<Void> saveForm(@PathVariable Long formId, @RequestBody PmDtos.FormSaveRequest request) {
        formInstanceService.saveDraft(formId, request.getFormData());
        return ApiResponse.ok(null);
    }

    @PostMapping("/forms/{formId}/submit")
    @Perm("project:project:list")
    public ApiResponse<Map<String, String>> submitForm(@PathVariable Long formId) {
        String instanceId = formInstanceService.submit(formId);
        return ApiResponse.ok(Map.of("flowInstanceId", instanceId));
    }
}
