package com.guolian.pm.crm.common.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.crm.common.dto.CrmDtos;
import com.guolian.pm.crm.common.service.CrmOpeningSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 供项目管理模块的立项来源只读契约（本期冻结，只允许 GET）。
 *
 * <p>字段名不得改动；新增字段可加，删除或改名必须提 /v2。
 */
@RestController
@RequestMapping("/api/v1/crm/opening-sources")
@RequiredArgsConstructor
public class CrmOpeningSourceController {

    private final CrmOpeningSourceService openingSourceService;

    @GetMapping("/opportunities")
    @Perm("crm:opportunity:list")
    public ApiResponse<List<CrmDtos.OpeningSource>> opportunities(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(openingSourceService.wonOpportunities(keyword));
    }

    @GetMapping("/bids")
    @Perm("crm:bid:list")
    public ApiResponse<List<CrmDtos.OpeningSource>> bids(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(openingSourceService.wonBids(keyword));
    }

    @GetMapping("/contracts")
    @Perm("crm:contract:list")
    public ApiResponse<List<CrmDtos.OpeningSource>> contracts(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(openingSourceService.activeContracts(keyword));
    }

    @GetMapping("/agreements")
    @Perm("crm:agreement:list")
    public ApiResponse<List<CrmDtos.OpeningSource>> agreements(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(openingSourceService.effectiveAgreements(keyword));
    }

    @GetMapping("/by-id")
    @Perm("crm:account:query")
    public ApiResponse<CrmDtos.OpeningSource> byId(@RequestParam String type, @RequestParam Long id) {
        return ApiResponse.ok(openingSourceService.byId(type, id));
    }
}
