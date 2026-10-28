package com.guolian.pm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDtos {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "请输入登录账号")
        private String username;

        @NotBlank(message = "请输入密码")
        private String password;
    }

    @Data
    public static class SwitchRoleRequest {
        @NotNull(message = "请选择要切换的角色")
        private Long roleId;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "请输入原密码")
        private String oldPassword;

        @NotBlank(message = "请输入新密码")
        @Size(min = 8, max = 32, message = "新密码长度需为 8~32 位")
        private String newPassword;
    }

    @Data
    public static class ImpersonateRequest {
        @NotNull(message = "请选择要模拟的用户")
        private Long userId;
    }
}
