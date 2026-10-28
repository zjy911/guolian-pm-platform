package com.guolian.pm.crm.account.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.crm.account.entity.CrmAccount;
import com.guolian.pm.crm.account.service.AccountService;
import com.guolian.pm.crm.common.dto.CrmDtos;
import com.guolian.pm.crm.common.dto.CrmQuery;
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

import java.util.Map;

/**
 * CRM 客户接口（含公共池、流失、认领）。
 */
@RestController
@RequestMapping("/api/v1/crm/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Perm("crm:account:list")
    public ApiResponse<PageResult<CrmAccount>> page(CrmQuery query) {
        return ApiResponse.ok(accountService.page(query));
    }

    @GetMapping("/{id}")
    @Perm("crm:account:query")
    public ApiResponse<CrmAccount> detail(@PathVariable Long id) {
        return ApiResponse.ok(accountService.get(id));
    }

    /** 详情页五个 Tab 计数 */
    @GetMapping("/{id}/tab-counts")
    @Perm("crm:account:query")
    public ApiResponse<Map<String, Object>> tabCounts(@PathVariable Long id) {
        return ApiResponse.ok(accountService.tabCounts(id));
    }

    /** 删除前置引用检查 */
    @GetMapping("/{id}/relations")
    @Perm("crm:account:query")
    public ApiResponse<Map<String, Object>> relations(@PathVariable Long id) {
        return ApiResponse.ok(accountService.relations(id));
    }

    @PostMapping
    @Perm("crm:account:add")
    @OpLog(module = "CRM-客户", action = "新增客户")
    public ApiResponse<Map<String, Long>> create(@RequestBody CrmAccount request) {
        return ApiResponse.ok(Map.of("id", accountService.create(request)));
    }

    @PutMapping("/{id}")
    @Perm("crm:account:edit")
    @OpLog(module = "CRM-客户", action = "修改客户")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody CrmAccount request) {
        accountService.update(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Perm("crm:account:remove")
    @OpLog(module = "CRM-客户", action = "删除客户")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ApiResponse.ok();
    }

    /** 公共客户池（显式跨范围只读 + 脱敏） */
    @GetMapping("/pool")
    @Perm("crm:account:pool")
    public ApiResponse<PageResult<CrmAccount>> pool(CrmQuery query) {
        return ApiResponse.ok(accountService.poolPage(query));
    }

    @PutMapping("/{id}/to-pool")
    @Perm("crm:account:pool")
    @OpLog(module = "CRM-客户", action = "放入公共池")
    public ApiResponse<Void> toPool(@PathVariable Long id, @RequestParam(required = false) String reason) {
        accountService.toPool(id, reason);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/churn")
    @Perm("crm:account:edit")
    @OpLog(module = "CRM-客户", action = "置流失")
    public ApiResponse<Void> churn(@PathVariable Long id, @RequestParam String reason) {
        accountService.churn(id, reason);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/revive")
    @Perm("crm:account:edit")
    @OpLog(module = "CRM-客户", action = "流失恢复")
    public ApiResponse<Void> revive(@PathVariable Long id, @RequestParam(required = false) String reason) {
        accountService.revive(id, reason);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/claim")
    @Perm("crm:account:claim")
    @OpLog(module = "CRM-客户", action = "发起认领")
    public ApiResponse<Map<String, Long>> claim(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return ApiResponse.ok(Map.of("applyId", accountService.claim(id, reason)));
    }

    @PutMapping("/{id}/owner")
    @Perm("crm:account:assign")
    @OpLog(module = "CRM-客户", action = "转移负责人")
    public ApiResponse<Void> changeOwner(@PathVariable Long id, @RequestBody CrmDtos.OwnerRequest request) {
        accountService.changeOwner(id, request.getOwnerUserId());
        return ApiResponse.ok();
    }
}
