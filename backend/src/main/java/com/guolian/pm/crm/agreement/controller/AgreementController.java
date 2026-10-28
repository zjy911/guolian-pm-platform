package com.guolian.pm.crm.agreement.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.crm.agreement.entity.CrmAgreement;
import com.guolian.pm.crm.agreement.service.AgreementService;
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

import java.util.Map;

/**
 * CRM 协议接口。
 */
@RestController
@RequestMapping("/api/v1/crm/agreements")
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService agreementService;

    @GetMapping
    @Perm("crm:agreement:list")
    public ApiResponse<PageResult<CrmAgreement>> page(CrmQuery query) {
        return ApiResponse.ok(agreementService.page(query));
    }

    @GetMapping("/{id}")
    @Perm("crm:agreement:query")
    public ApiResponse<CrmAgreement> detail(@PathVariable Long id) {
        return ApiResponse.ok(agreementService.get(id));
    }

    @PostMapping
    @Perm("crm:agreement:add")
    @OpLog(module = "CRM-协议", action = "新增协议")
    public ApiResponse<Map<String, Long>> create(@RequestBody CrmAgreement request) {
        return ApiResponse.ok(Map.of("id", agreementService.create(request)));
    }

    @PutMapping("/{id}")
    @Perm("crm:agreement:edit")
    @OpLog(module = "CRM-协议", action = "修改协议")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody CrmAgreement request) {
        agreementService.update(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Perm("crm:agreement:remove")
    @OpLog(module = "CRM-协议", action = "删除协议")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        agreementService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/submit")
    @Perm("crm:agreement:submit")
    @OpLog(module = "CRM-协议", action = "提交审批")
    public ApiResponse<Map<String, String>> submit(@PathVariable Long id) {
        return ApiResponse.ok(Map.of("processInstanceId", agreementService.submit(id)));
    }

    @PostMapping("/{id}/withdraw")
    @Perm("crm:agreement:submit")
    @OpLog(module = "CRM-协议", action = "撤回审批")
    public ApiResponse<Void> withdraw(@PathVariable Long id, @RequestParam(required = false) String reason) {
        agreementService.withdraw(id, reason);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/archive")
    @Perm("crm:agreement:archive")
    @OpLog(module = "CRM-协议", action = "归档登记")
    public ApiResponse<Void> archive(@PathVariable Long id, @RequestBody ArchiveRequest request) {
        agreementService.archive(id, request.getPaperBoxNo(), request.getPaperCopies());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/renew")
    @Perm("crm:agreement:renew")
    @OpLog(module = "CRM-协议", action = "发起续签")
    public ApiResponse<Map<String, Long>> renew(@PathVariable Long id, @RequestBody CrmAgreement request) {
        return ApiResponse.ok(Map.of("id", agreementService.renew(id, request)));
    }

    @Data
    public static class ArchiveRequest {
        private String paperBoxNo;
        private Integer paperCopies;
    }
}
