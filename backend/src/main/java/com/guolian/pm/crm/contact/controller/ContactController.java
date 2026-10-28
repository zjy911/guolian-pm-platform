package com.guolian.pm.crm.contact.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.crm.common.dto.CrmQuery;
import com.guolian.pm.crm.contact.entity.CrmContact;
import com.guolian.pm.crm.contact.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * CRM 联系人接口。
 */
@RestController
@RequestMapping("/api/v1/crm/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    @Perm("crm:contact:list")
    public ApiResponse<PageResult<CrmContact>> page(CrmQuery query) {
        return ApiResponse.ok(contactService.page(query));
    }

    @GetMapping("/{id}")
    @Perm("crm:contact:query")
    public ApiResponse<CrmContact> detail(@PathVariable Long id) {
        return ApiResponse.ok(contactService.get(id));
    }

    @PostMapping
    @Perm("crm:contact:add")
    @OpLog(module = "CRM-联系人", action = "新增联系人")
    public ApiResponse<Map<String, Long>> create(@RequestBody CrmContact request) {
        return ApiResponse.ok(Map.of("id", contactService.create(request)));
    }

    @PutMapping("/{id}")
    @Perm("crm:contact:edit")
    @OpLog(module = "CRM-联系人", action = "修改联系人")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody CrmContact request) {
        contactService.update(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Perm("crm:contact:remove")
    @OpLog(module = "CRM-联系人", action = "删除联系人")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        contactService.delete(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/primary")
    @Perm("crm:contact:edit")
    @OpLog(module = "CRM-联系人", action = "设为主要联系人")
    public ApiResponse<Void> setPrimary(@PathVariable Long id) {
        contactService.setPrimary(id);
        return ApiResponse.ok();
    }
}
