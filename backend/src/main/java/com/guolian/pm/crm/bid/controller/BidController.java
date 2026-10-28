package com.guolian.pm.crm.bid.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.crm.bid.entity.CrmBid;
import com.guolian.pm.crm.bid.service.BidService;
import com.guolian.pm.crm.common.dto.CrmQuery;
import lombok.Data;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * CRM 投标接口（含开标日历、中标联动）。
 */
@RestController
@RequestMapping("/api/v1/crm/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @GetMapping
    @Perm("crm:bid:list")
    public ApiResponse<PageResult<CrmBid>> page(CrmQuery query) {
        return ApiResponse.ok(bidService.page(query));
    }

    @GetMapping("/{id}")
    @Perm("crm:bid:query")
    public ApiResponse<CrmBid> detail(@PathVariable Long id) {
        return ApiResponse.ok(bidService.get(id));
    }

    /** 开标日历：month=yyyy-MM */
    @GetMapping("/calendar")
    @Perm("crm:bid:list")
    public ApiResponse<List<CrmBid>> calendar(@RequestParam(required = false) String month) {
        return ApiResponse.ok(bidService.calendar(month));
    }

    /** 开标提醒（未来 N 天） */
    @GetMapping("/opening-soon")
    @Perm("crm:bid:list")
    public ApiResponse<List<CrmBid>> openingSoon(@RequestParam(defaultValue = "3") int days) {
        return ApiResponse.ok(bidService.openingSoon(days));
    }

    @PostMapping
    @Perm("crm:bid:add")
    @OpLog(module = "CRM-投标", action = "新增投标")
    public ApiResponse<Map<String, Long>> create(@RequestBody CrmBid request) {
        return ApiResponse.ok(Map.of("id", bidService.create(request)));
    }

    @PutMapping("/{id}")
    @Perm("crm:bid:edit")
    @OpLog(module = "CRM-投标", action = "修改投标")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody CrmBid request) {
        bidService.update(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Perm("crm:bid:remove")
    @OpLog(module = "CRM-投标", action = "删除投标")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bidService.delete(id);
        return ApiResponse.ok();
    }

    /** 状态推进（置已中标必须填中标金额） */
    @PutMapping("/{id}/status")
    @Perm("crm:bid:status")
    @OpLog(module = "CRM-投标", action = "推进状态")
    public ApiResponse<Void> status(@PathVariable Long id, @RequestBody StatusRequest request) {
        bidService.changeStatus(id, request.getTargetStatus(), request.getWinAmount(), request.getReason());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/gen-opportunity")
    @Perm("crm:bid:status")
    @OpLog(module = "CRM-投标", action = "生成商机")
    public ApiResponse<Map<String, Long>> genOpportunity(@PathVariable Long id) {
        return ApiResponse.ok(Map.of("opportunityId", bidService.genOpportunity(id)));
    }

    @PostMapping("/{id}/gen-contract")
    @Perm("crm:bid:status")
    @OpLog(module = "CRM-投标", action = "生成合同草稿")
    public ApiResponse<Map<String, Long>> genContract(@PathVariable Long id, @RequestBody GenContractRequest request) {
        return ApiResponse.ok(Map.of("contractId",
                bidService.genContract(id, request.getCompanyId(), request.getProductId())));
    }

    @Data
    public static class StatusRequest {
        private String targetStatus;
        private BigDecimal winAmount;
        private String reason;
    }

    @Data
    public static class GenContractRequest {
        private Long companyId;
        private Long productId;
    }
}
