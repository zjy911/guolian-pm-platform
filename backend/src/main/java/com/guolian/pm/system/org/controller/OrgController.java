package com.guolian.pm.system.org.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.system.org.dto.OrgDtos;
import com.guolian.pm.system.org.service.OrgService;
import com.guolian.pm.system.org.vo.OrgVO;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/system/orgs")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    @GetMapping("/tree")
    public ApiResponse<List<OrgVO>> tree(@RequestParam(required = false) Boolean onlyEnabled) {
        return ApiResponse.ok(orgService.tree(onlyEnabled));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrgVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(orgService.detail(id));
    }

    @PostMapping
    @Perm("system:org:add")
    @OpLog(module = "组织架构", action = "新增组织")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody OrgDtos.SaveRequest request) {
        return ApiResponse.ok(Map.of("id", orgService.create(request)));
    }

    @PutMapping("/{id}")
    @Perm("system:org:edit")
    @OpLog(module = "组织架构", action = "修改组织")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody OrgDtos.SaveRequest request) {
        request.setId(id);
        orgService.update(request);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/move")
    @Perm("system:org:edit")
    @OpLog(module = "组织架构", action = "移动组织")
    public ApiResponse<Void> move(@PathVariable Long id, @RequestBody OrgDtos.MoveRequest request) {
        orgService.move(id, request.getParentId());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Perm("system:org:remove")
    @OpLog(module = "组织架构", action = "删除组织")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        orgService.delete(id);
        return ApiResponse.ok();
    }
}
