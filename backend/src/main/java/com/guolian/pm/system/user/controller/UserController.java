package com.guolian.pm.system.user.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.system.user.dto.UserDtos;
import com.guolian.pm.system.user.service.UserService;
import com.guolian.pm.system.user.vo.UserVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 用户管理接口。
 */
@RestController
@RequestMapping("/api/v1/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @Perm("system:user:list")
    public ApiResponse<PageResult<UserVO>> page(UserDtos.Query query) {
        return ApiResponse.ok(userService.page(query));
    }

    @GetMapping("/{id}")
    @Perm("system:user:list")
    public ApiResponse<UserVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(userService.detail(id));
    }

    @PostMapping
    @Perm("system:user:add")
    @OpLog(module = "用户管理", action = "新增用户")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody UserDtos.SaveRequest request) {
        return ApiResponse.ok(Map.of("id", userService.create(request)));
    }

    @PutMapping("/{id}")
    @Perm("system:user:edit")
    @OpLog(module = "用户管理", action = "修改用户")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody UserDtos.SaveRequest request) {
        request.setId(id);
        userService.update(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Perm("system:user:remove")
    @OpLog(module = "用户管理", action = "删除用户")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/roles")
    @Perm("system:user:assign-role")
    @OpLog(module = "用户管理", action = "分配角色")
    public ApiResponse<Void> assignRoles(@PathVariable Long id,
                                         @Valid @RequestBody UserDtos.RoleAssignRequest request) {
        userService.assignRoles(id, request.getRoleIds(), request.getDefaultRoleId());
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/status")
    @Perm("system:user:status")
    @OpLog(module = "用户管理", action = "变更用户状态")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody UserDtos.StatusRequest request) {
        userService.updateStatus(id, request);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/reset-password")
    @Perm("system:user:reset-pwd")
    @OpLog(module = "用户管理", action = "重置密码")
    public ApiResponse<Map<String, String>> resetPassword(@PathVariable Long id) {
        return ApiResponse.ok(Map.of("password", userService.resetPassword(id)));
    }

    /**
     * 项目参与：数据由项目管理模块提供，本期返回空列表（结构已预留）。
     */
    @GetMapping("/{id}/projects")
    @Perm("system:user:list")
    public ApiResponse<List<Object>> projects(@PathVariable Long id) {
        return ApiResponse.ok(userService.projects(id));
    }

    @GetMapping("/export")
    @Perm("system:user:export")
    @OpLog(module = "用户管理", action = "导出用户")
    public void export(UserDtos.Query query, HttpServletResponse response) throws IOException {
        byte[] data = userService.exportCsv(query);
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=users.csv");
        response.getOutputStream().write(data);
    }
}
