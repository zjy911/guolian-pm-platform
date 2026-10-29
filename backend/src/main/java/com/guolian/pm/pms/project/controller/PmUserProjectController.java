package com.guolian.pm.pms.project.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.pms.common.dto.PmDtos;
import com.guolian.pm.pms.project.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统管理·用户管理「关联项目」能力（PRD 六）：指派 / 变更 / 释放 / 履历。
 * 与底座 UserController 的 GET /system/users/{id}/projects 共同构成双向可查。
 */
@RestController
@RequestMapping("/api/v1/system/users/{userId}/projects")
@RequiredArgsConstructor
public class PmUserProjectController {

    private final ProjectMemberService memberService;

    /** 指派进项目（投入比例 >100% 或未认证时阻断） */
    @PostMapping
    @Perm("system:user:list")
    public ApiResponse<Map<String, Long>> assign(@PathVariable Long userId,
                                                 @Valid @RequestBody PmDtos.MemberAssignRequest request) {
        Long memberId = memberService.assign(userId, request);
        return ApiResponse.ok(Map.of("memberId", memberId));
    }

    /** 在办项目投入比例合计（表单实时提示） */
    @GetMapping("/ratio")
    @Perm("system:user:list")
    public ApiResponse<Map<String, Integer>> ratio(@PathVariable Long userId) {
        return ApiResponse.ok(Map.of("activeRatio", memberService.activeRatioOf(userId)));
    }
}
