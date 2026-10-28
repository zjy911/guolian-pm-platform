package com.guolian.pm.crm.opportunity.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.crm.common.dto.CrmDtos;
import com.guolian.pm.crm.common.dto.CrmQuery;
import com.guolian.pm.crm.competitor.entity.CrmCompetitor;
import com.guolian.pm.crm.competitor.service.CompetitorService;
import com.guolian.pm.crm.opportunity.entity.CrmOpportunity;
import com.guolian.pm.crm.opportunity.service.OpportunityService;
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
 * CRM 商机接口 + 竞对子资源。
 */
@RestController
@RequestMapping("/api/v1/crm/opportunities")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final CompetitorService competitorService;

    @GetMapping
    @Perm("crm:opportunity:list")
    public ApiResponse<PageResult<CrmOpportunity>> page(CrmQuery query) {
        return ApiResponse.ok(opportunityService.page(query));
    }

    @GetMapping("/{id}")
    @Perm("crm:opportunity:query")
    public ApiResponse<CrmOpportunity> detail(@PathVariable Long id) {
        return ApiResponse.ok(opportunityService.get(id));
    }

    /** 销售漏斗聚合 */
    @GetMapping("/funnel")
    @Perm("crm:opportunity:list")
    public ApiResponse<List<CrmDtos.FunnelStage>> funnel() {
        return ApiResponse.ok(opportunityService.funnel());
    }

    @PostMapping
    @Perm("crm:opportunity:add")
    @OpLog(module = "CRM-商机", action = "新增商机")
    public ApiResponse<Map<String, Long>> create(@RequestBody CrmOpportunity request) {
        return ApiResponse.ok(Map.of("id", opportunityService.create(request)));
    }

    @PutMapping("/{id}")
    @Perm("crm:opportunity:edit")
    @OpLog(module = "CRM-商机", action = "修改商机")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody CrmOpportunity request) {
        opportunityService.update(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Perm("crm:opportunity:remove")
    @OpLog(module = "CRM-商机", action = "删除商机")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        opportunityService.delete(id);
        return ApiResponse.ok();
    }

    /** 阶段推进（只进不退；回退须填原因） */
    @PutMapping("/{id}/stage")
    @Perm("crm:opportunity:stage")
    @OpLog(module = "CRM-商机", action = "推进阶段")
    public ApiResponse<Void> stage(@PathVariable Long id, @RequestBody CrmDtos.StageRequest request) {
        opportunityService.advance(id, request.getTargetStage(), request.getReason());
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/lost")
    @Perm("crm:opportunity:lost")
    @OpLog(module = "CRM-商机", action = "置流失")
    public ApiResponse<Void> lost(@PathVariable Long id, @RequestParam String reason) {
        opportunityService.markLost(id, reason);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/owner")
    @Perm("crm:opportunity:edit")
    @OpLog(module = "CRM-商机", action = "转移负责人")
    public ApiResponse<Void> changeOwner(@PathVariable Long id, @RequestBody CrmDtos.OwnerRequest request) {
        opportunityService.changeOwner(id, request.getOwnerUserId());
        return ApiResponse.ok();
    }

    // ---------------- 竞对（商机子资源，权限复用商机） ----------------

    @GetMapping("/{id}/competitors")
    @Perm("crm:opportunity:query")
    public ApiResponse<List<CrmCompetitor>> competitors(@PathVariable Long id) {
        return ApiResponse.ok(competitorService.list(id));
    }

    @GetMapping("/{id}/competitors/compare")
    @Perm("crm:opportunity:query")
    public ApiResponse<Map<String, Object>> compare(@PathVariable Long id) {
        return ApiResponse.ok(competitorService.compare(id));
    }

    @PostMapping("/{id}/competitors")
    @Perm("crm:opportunity:edit")
    @OpLog(module = "CRM-竞对", action = "新增竞对")
    public ApiResponse<Map<String, Long>> addCompetitor(@PathVariable Long id, @RequestBody CrmCompetitor request) {
        request.setOpportunityId(id);
        return ApiResponse.ok(Map.of("id", competitorService.create(request)));
    }

    @PutMapping("/{opId}/competitors/{id}")
    @Perm("crm:opportunity:edit")
    @OpLog(module = "CRM-竞对", action = "修改竞对")
    public ApiResponse<Void> updateCompetitor(@PathVariable Long opId, @PathVariable Long id,
                                             @RequestBody CrmCompetitor request) {
        competitorService.update(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{opId}/competitors/{id}")
    @Perm("crm:opportunity:edit")
    @OpLog(module = "CRM-竞对", action = "删除竞对")
    public ApiResponse<Void> deleteCompetitor(@PathVariable Long opId, @PathVariable Long id) {
        competitorService.delete(id);
        return ApiResponse.ok();
    }
}
