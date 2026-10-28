package com.guolian.pm.auth;

import com.guolian.pm.auth.dto.AuthDtos;
import com.guolian.pm.auth.dto.AuthResult;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.log.OpLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证与身份接口。控制器只做参数解析与响应封装，业务逻辑全部在 AuthService。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthResult> login(@Valid @RequestBody AuthDtos.LoginRequest request,
                                         HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.login(request, servletRequest));
    }

    @PostMapping("/switch-role")
    @OpLog(module = "认证", action = "切换角色")
    public ApiResponse<AuthResult> switchRole(@Valid @RequestBody AuthDtos.SwitchRoleRequest request) {
        return ApiResponse.ok(authService.switchRole(request.getRoleId()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.ok();
    }

    @GetMapping("/user-info")
    public ApiResponse<AuthResult> userInfo() {
        return ApiResponse.ok(authService.userInfo());
    }

    @GetMapping("/menus")
    public ApiResponse<List<AuthResult.MenuVO>> menus() {
        return ApiResponse.ok(authService.menus());
    }

    @GetMapping("/my-roles")
    public ApiResponse<List<AuthResult.RoleOptionVO>> myRoles() {
        return ApiResponse.ok(authService.myRoles());
    }

    @PostMapping("/change-password")
    @OpLog(module = "认证", action = "修改密码")
    public ApiResponse<Void> changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.ok();
    }
}
