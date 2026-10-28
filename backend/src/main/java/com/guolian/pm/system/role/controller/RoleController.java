package com.guolian.pm.system.role.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.system.role.dto.RoleDtos;
import com.guolian.pm.system.role.service.RoleService;
import com.guolian.pm.system.role.vo.RoleVO;
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

/**
 * 角色管理接口。org_id 必填，切换角色即切换组织。
 */
@RestController
@RequestMapping("/api/v1/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Perm("system:role:list")
    public ApiResponse<PageResult<RoleVO>> page(RoleDtos.Query query) {
        return ApiResponse.ok(roleService.page(query));
    }

    /** 下拉用：全量启用角色，可按组织过滤 */
    @GetMapping("/options")
    public ApiResponse<List<RoleVO>> options(@RequestParam(required = false) Long orgId) {
        return ApiResponse.ok(roleService.listAll(orgId));
    }

    @GetMapping("/{id}")
    @Perm("system:role:list")
    public ApiResponse<RoleVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(roleService.detail(id));
    }

    @PostMapping
    @Perm("system:role:add")
    @OpLog(module = "角色管理", action = "新增角色")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody RoleDtos.SaveRequest request) {
        return ApiResponse.ok(Map.of("id", roleService.create(request)));
    }

    @PutMapping("/{id}")
    @Perm("system:role:edit")
    @OpLog(module = "角色管理", action = "修改角色")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody RoleDtos.SaveRequest request) {
        request.setId(id);
        roleService.update(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Perm("system:role:remove")
    @OpLog(module = "角色管理", action = "删除角色")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/menus")
    @Perm("system:role:list")
    public ApiResponse<List<Long>> menus(@PathVariable Long id) {
        return ApiResponse.ok(roleService.menuIds(id));
    }

    @PutMapping("/{id}/menus")
    @Perm("system:role:auth")
    @OpLog(module = "角色管理", action = "角色授权")
    public ApiResponse<Void> saveMenus(@PathVariable Long id, @RequestBody RoleDtos.MenuAuthRequest request) {
        roleService.saveMenus(id, request.getMenuIds());
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/data-scope")
    @Perm("system:role:data-scope")
    @OpLog(module = "角色管理", action = "配置数据权限")
    public ApiResponse<Void> saveDataScope(@PathVariable Long id,
                                           @Valid @RequestBody RoleDtos.DataScopeRequest request) {
        roleService.saveDataScope(id, request);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/field-mask")
    @Perm("system:role:list")
    public ApiResponse<Map<String, String>> fieldMasks(@PathVariable Long id) {
        return ApiResponse.ok(roleService.fieldMasks(id));
    }

    @PutMapping("/{id}/field-mask")
    @Perm("system:role:edit")
    @OpLog(module = "角色管理", action = "配置字段脱敏")
    public ApiResponse<Void> saveFieldMasks(@PathVariable Long id,
                                            @RequestBody RoleDtos.FieldMaskRequest request) {
        roleService.saveFieldMasks(id, request.getMasks());
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/users")
    @Perm("system:role:list")
    public ApiResponse<List<Map<String, Object>>> users(@PathVariable Long id) {
        return ApiResponse.ok(roleService.users(id));
    }
}
