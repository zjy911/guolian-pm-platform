package com.guolian.pm.system.config.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.system.config.entity.SysConfig;
import com.guolian.pm.system.config.service.ConfigService;
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

@RestController
@RequestMapping("/api/v1/system/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    @Perm("system:config:list")
    public ApiResponse<List<SysConfig>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(configService.list(keyword));
    }

    @Data
    public static class ValueRequest {
        private String configValue;
        private String remark;
    }

    @PutMapping("/{id}")
    @Perm("system:config:edit")
    @OpLog(module = "参数配置", action = "修改参数")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody ValueRequest request) {
        configService.updateValue(id, request.getConfigValue(), request.getRemark());
        return ApiResponse.ok();
    }

    @PostMapping
    @Perm("system:config:add")
    @OpLog(module = "参数配置", action = "新增参数")
    public ApiResponse<Map<String, Long>> create(@RequestBody SysConfig config) {
        config.setId(null);
        return ApiResponse.ok(Map.of("id", configService.create(config)));
    }

    @DeleteMapping("/{id}")
    @Perm("system:config:remove")
    @OpLog(module = "参数配置", action = "删除参数")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return ApiResponse.ok();
    }
}
