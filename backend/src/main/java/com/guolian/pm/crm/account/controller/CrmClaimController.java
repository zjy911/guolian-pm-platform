package com.guolian.pm.crm.account.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.crm.account.service.AccountService;
import com.guolian.pm.crm.account.vo.CrmClaimVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 客户认领审批：只做查询与数量统计，审批动作统一在「审批中心」处理
 * （审批结果由 CrmFlowCallback 监听 FlowFinishedEvent 回写到业务）。
 */
@RestController
@RequestMapping("/api/v1/crm/claims")
@RequiredArgsConstructor
public class CrmClaimController {

    private final AccountService accountService;

    @GetMapping
    @Perm("crm:account:claim")
    public ApiResponse<PageResult<CrmClaimVO>> page(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(accountService.pageClaims(status, keyword, pageNum, pageSize));
    }

    @GetMapping("/pending-count")
    @Perm("crm:account:claim")
    public ApiResponse<Map<String, Long>> pendingCount() {
        return ApiResponse.ok(Map.of("count", accountService.countPendingClaims()));
    }
}
