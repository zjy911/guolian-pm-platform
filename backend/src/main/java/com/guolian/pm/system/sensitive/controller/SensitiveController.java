package com.guolian.pm.system.sensitive.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.system.sensitive.entity.SysSensitiveField;
import com.guolian.pm.system.sensitive.entity.SysSensitiveGroup;
import com.guolian.pm.system.sensitive.service.SensitiveAdminService;
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
 * 字段脱敏配置。字段名只能从白名单选择。
 */
@RestController
@RequestMapping("/api/v1/system/sensitive")
@RequiredArgsConstructor
public class SensitiveController {

    private final SensitiveAdminService service;

    @GetMapping("/groups")
    @Perm("system:sensitive:list")
    public ApiResponse<List<SysSensitiveGroup>> groups() {
        return ApiResponse.ok(service.groups());
    }

    @PostMapping("/groups")
    @Perm("system:sensitive:add")
    @OpLog(module = "字段脱敏", action = "新增字段组")
    public ApiResponse<Map<String, Long>> createGroup(@RequestBody SysSensitiveGroup group) {
        group.setId(null);
        return ApiResponse.ok(Map.of("id", service.saveGroup(group)));
    }

    @PutMapping("/groups/{id}")
    @Perm("system:sensitive:edit")
    @OpLog(module = "字段脱敏", action = "修改字段组")
    public ApiResponse<Void> updateGroup(@PathVariable Long id, @RequestBody SysSensitiveGroup group) {
        group.setId(id);
        service.saveGroup(group);
        return ApiResponse.ok();
    }

    @DeleteMapping("/groups/{id}")
    @Perm("system:sensitive:remove")
    @OpLog(module = "字段脱敏", action = "删除字段组")
    public ApiResponse<Void> deleteGroup(@PathVariable Long id) {
        service.deleteGroup(id);
        return ApiResponse.ok();
    }

    @GetMapping("/fields")
    @Perm("system:sensitive:list")
    public ApiResponse<List<SysSensitiveField>> fields(@RequestParam(required = false) String groupCode,
                                                       @RequestParam(required = false) String bizObject) {
        return ApiResponse.ok(service.fields(groupCode, bizObject));
    }

    @GetMapping("/whitelist")
    @Perm("system:sensitive:list")
    public ApiResponse<Map<String, List<Map<String, String>>>> whitelist() {
        return ApiResponse.ok(service.whitelist());
    }

    @PostMapping("/fields")
    @Perm("system:sensitive:add")
    @OpLog(module = "字段脱敏", action = "新增脱敏字段")
    public ApiResponse<Map<String, Long>> createField(@RequestBody SysSensitiveField field) {
        field.setId(null);
        return ApiResponse.ok(Map.of("id", service.saveField(field)));
    }

    @PutMapping("/fields/{id}")
    @Perm("system:sensitive:edit")
    @OpLog(module = "字段脱敏", action = "修改脱敏字段")
    public ApiResponse<Void> updateField(@PathVariable Long id, @RequestBody SysSensitiveField field) {
        field.setId(id);
        service.saveField(field);
        return ApiResponse.ok();
    }

    @DeleteMapping("/fields/{id}")
    @Perm("system:sensitive:remove")
    @OpLog(module = "字段脱敏", action = "删除脱敏字段")
    public ApiResponse<Void> deleteField(@PathVariable Long id) {
        service.deleteField(id);
        return ApiResponse.ok();
    }

    /** 查看明文（写审计日志，独立接口） */
    @GetMapping("/plain")
    public ApiResponse<Map<String, String>> plain(@RequestParam String bizType,
                                                  @RequestParam Long bizId,
                                                  @RequestParam String group) {
        return ApiResponse.ok(Map.of("value", service.plain(bizType, bizId, group)));
    }
}
