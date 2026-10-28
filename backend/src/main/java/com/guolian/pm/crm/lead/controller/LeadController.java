package com.guolian.pm.crm.lead.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.crm.common.dto.CrmDtos;
import com.guolian.pm.crm.common.dto.CrmQuery;
import com.guolian.pm.crm.lead.entity.CrmLead;
import com.guolian.pm.crm.lead.service.LeadService;
import jakarta.validation.constraints.NotBlank;
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
 * CRM 线索接口。
 */
@RestController
@RequestMapping("/api/v1/crm/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    @Perm("crm:lead:list")
    public ApiResponse<PageResult<CrmLead>> page(CrmQuery query) {
        return ApiResponse.ok(leadService.page(query));
    }

    @GetMapping("/{id}")
    @Perm("crm:lead:query")
    public ApiResponse<CrmLead> detail(@PathVariable Long id) {
        return ApiResponse.ok(leadService.get(id));
    }

    @PostMapping
    @Perm("crm:lead:add")
    @OpLog(module = "CRM-线索", action = "新增线索")
    public ApiResponse<Map<String, Long>> create(@RequestBody CrmLead request) {
        return ApiResponse.ok(Map.of("id", leadService.create(request)));
    }

    @PutMapping("/{id}")
    @Perm("crm:lead:edit")
    @OpLog(module = "CRM-线索", action = "修改线索")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody CrmLead request) {
        leadService.update(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Perm("crm:lead:remove")
    @OpLog(module = "CRM-线索", action = "删除线索")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        leadService.delete(id);
        return ApiResponse.ok();
    }

    /** 转化：客户 + 联系人 + 商机一次派生，幂等 */
    @PostMapping("/{id}/convert")
    @Perm("crm:lead:convert")
    @OpLog(module = "CRM-线索", action = "线索转化")
    public ApiResponse<CrmDtos.ConvertResultHolder> convert(@PathVariable Long id,
                                                            @RequestBody CrmDtos.ConvertRequest request) {
        return ApiResponse.ok(leadService.convert(id, request));
    }

    @PutMapping("/{id}/invalid")
    @Perm("crm:lead:edit")
    @OpLog(module = "CRM-线索", action = "置无效")
    public ApiResponse<Void> invalid(@PathVariable Long id, @RequestParam @NotBlank String reason) {
        leadService.invalid(id, reason);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/owner")
    @Perm("crm:lead:assign")
    @OpLog(module = "CRM-线索", action = "转移负责人")
    public ApiResponse<Void> changeOwner(@PathVariable Long id, @RequestBody CrmDtos.OwnerRequest request) {
        leadService.changeOwner(id, request.getOwnerUserId());
        return ApiResponse.ok();
    }
}
