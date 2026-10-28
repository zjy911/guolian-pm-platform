package com.guolian.pm.system.menu.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.system.menu.entity.SysMenu;
import com.guolian.pm.system.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    public ApiResponse<List<MenuService.Node>> tree() {
        return ApiResponse.ok(menuService.tree());
    }

    @PostMapping
    @Perm("system:menu:add")
    @OpLog(module = "菜单管理", action = "新增菜单")
    public ApiResponse<Map<String, Long>> create(@RequestBody SysMenu menu) {
        menu.setId(null);
        return ApiResponse.ok(Map.of("id", menuService.save(menu)));
    }

    @PutMapping("/{id}")
    @Perm("system:menu:edit")
    @OpLog(module = "菜单管理", action = "修改菜单")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody SysMenu menu) {
        menu.setId(id);
        menuService.save(menu);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Perm("system:menu:remove")
    @OpLog(module = "菜单管理", action = "删除菜单")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ApiResponse.ok();
    }
}
