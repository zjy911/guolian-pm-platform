package com.guolian.pm.crm.contract.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.crm.common.dto.CrmQuery;
import com.guolian.pm.crm.contract.entity.CrmContract;
import com.guolian.pm.crm.contract.service.ContractService;
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

import java.util.List;
import java.util.Map;

/**
 * CRM 合同接口：提交审批 / 撤回 / 归档 / 续签 / 终止。
 */
@RestController
@RequestMapping("/api/v1/crm/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    @Perm("crm:contract:list")
    public ApiResponse<PageResult<CrmContract>> page(CrmQuery query) {
        return ApiResponse.ok(contractService.page(query));
    }

    @GetMapping("/{id}")
    @Perm("crm:contract:query")
    public ApiResponse<CrmContract> detail(@PathVariable Long id) {
        return ApiResponse.ok(contractService.get(id));
    }

    /** 到期预警 */
    @GetMapping("/expiring")
    @Perm("crm:contract:list")
    public ApiResponse<List<CrmContract>> expiring(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(contractService.expiring(days));
    }

    @PostMapping
    @Perm("crm:contract:add")
    @OpLog(module = "CRM-合同", action = "新增合同")
    public ApiResponse<Map<String, Long>> create(@RequestBody CrmContract request) {
        return ApiResponse.ok(Map.of("id", contractService.create(request)));
    }

    @PutMapping("/{id}")
    @Perm("crm:contract:edit")
    @OpLog(module = "CRM-合同", action = "修改合同")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody CrmContract request) {
        contractService.update(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Perm("crm:contract:remove")
    @OpLog(module = "CRM-合同", action = "删除合同")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/submit")
    @Perm("crm:contract:submit")
    @OpLog(module = "CRM-合同", action = "提交审批")
    public ApiResponse<Map<String, String>> submit(@PathVariable Long id) {
        return ApiResponse.ok(Map.of("processInstanceId", contractService.submit(id)));
    }

    @PostMapping("/{id}/withdraw")
    @Perm("crm:contract:submit")
    @OpLog(module = "CRM-合同", action = "撤回审批")
    public ApiResponse<Void> withdraw(@PathVariable Long id, @RequestParam(required = false) String reason) {
        contractService.withdraw(id, reason);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/archive")
    @Perm("crm:contract:archive")
    @OpLog(module = "CRM-合同", action = "归档登记")
    public ApiResponse<Void> archive(@PathVariable Long id, @RequestBody ArchiveRequest request) {
        contractService.archive(id, request.getPaperBoxNo(), request.getPaperCopies());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/renew")
    @Perm("crm:contract:renew")
    @OpLog(module = "CRM-合同", action = "发起续签")
    public ApiResponse<Map<String, Long>> renew(@PathVariable Long id, @RequestBody CrmContract request) {
        return ApiResponse.ok(Map.of("id", contractService.renew(id, request)));
    }

    @PostMapping("/{id}/terminate")
    @Perm("crm:contract:terminate")
    @OpLog(module = "CRM-合同", action = "终止合同")
    public ApiResponse<Void> terminate(@PathVariable Long id, @RequestParam String reason) {
        contractService.terminate(id, reason);
        return ApiResponse.ok();
    }

    @Data
    public static class ArchiveRequest {
        private String paperBoxNo;
        private Integer paperCopies;
    }
}
