package com.guolian.pm.crm.activity.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.crm.activity.entity.CrmActivity;
import com.guolian.pm.crm.activity.service.ActivityService;
import com.guolian.pm.crm.common.dto.CrmQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * CRM 活动接口（多态挂接 + 时间线 + 作废）。
 */
@RestController
@RequestMapping("/api/v1/crm/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    @Perm("crm:activity:list")
    public ApiResponse<PageResult<CrmActivity>> page(CrmQuery query) {
        return ApiResponse.ok(activityService.page(query));
    }

    /** 我的活动日历 */
    @GetMapping("/mine")
    @Perm("crm:activity:list")
    public ApiResponse<List<CrmActivity>> mine(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ApiResponse.ok(activityService.mine(start, end));
    }

    /** 目标对象的活动时间线 */
    @GetMapping("/timeline")
    @Perm("crm:activity:list")
    public ApiResponse<List<CrmActivity>> timeline(@RequestParam String targetType, @RequestParam Long targetId) {
        return ApiResponse.ok(activityService.timeline(targetType, targetId));
    }

    @PostMapping
    @Perm("crm:activity:add")
    @OpLog(module = "CRM-活动", action = "新增活动")
    public ApiResponse<Map<String, Long>> create(@RequestBody CrmActivity request) {
        return ApiResponse.ok(Map.of("id", activityService.create(request)));
    }

    @PutMapping("/{id}")
    @Perm("crm:activity:edit")
    @OpLog(module = "CRM-活动", action = "修改活动")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody CrmActivity request) {
        activityService.update(id, request);
        return ApiResponse.ok();
    }

    /** 作废（不提供物理删除） */
    @PutMapping("/{id}/void")
    @Perm("crm:activity:void")
    @OpLog(module = "CRM-活动", action = "作废活动")
    public ApiResponse<Void> voidActivity(@PathVariable Long id, @RequestParam String reason) {
        activityService.voidActivity(id, reason);
        return ApiResponse.ok();
    }
}
