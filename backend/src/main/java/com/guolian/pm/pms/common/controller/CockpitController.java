package com.guolian.pm.pms.common.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.pms.common.service.CockpitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 项目驾驶舱（只读分析态，PRD 5.1）。
 */
@RestController
@RequestMapping("/api/v1/project/cockpit")
@RequiredArgsConstructor
public class CockpitController {

    private final CockpitService cockpitService;

    @GetMapping("/summary")
    @Perm("project:cockpit:view")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok(cockpitService.summary());
    }
}
