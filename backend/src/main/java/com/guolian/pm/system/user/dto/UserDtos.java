package com.guolian.pm.system.user.dto;

import com.guolian.pm.common.api.PageQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户模块 DTO 集合。
 */
public class UserDtos {

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Query extends PageQuery {
        /** 姓名 / 账号 / 手机号 / 工号 */
        private String keyword;
        private Long orgId;
        private Boolean includeChild = Boolean.TRUE;
        private List<Long> roleIds = new ArrayList<>();
        /** ANY 拥有任一 / ALL 同时拥有全部 */
        private String roleMatch = "ANY";
        private Long companyId;
        private List<Integer> statuses = new ArrayList<>();
        private String employeeType;
        private LocalDate joinStart;
        private LocalDate joinEnd;
    }

    @Data
    public static class CompanyRelation {
        @NotNull(message = "请选择公司主体")
        private Long companyId;
        /** 1 任职 / 2 劳动合同 / 3 社保 / 4 发薪 */
        @NotNull(message = "请选择关联类型")
        private Integer relationType;
        private Integer isPrimary = 0;
    }

    @Data
    public static class SaveRequest {
        private Long id;

        @NotBlank(message = "请输入登录账号")
        @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "登录账号需为 4~20 位字母、数字或下划线")
        private String username;

        @NotBlank(message = "请输入姓名")
        @Size(min = 2, max = 20, message = "姓名长度需为 2~20 字")
        private String realName;

        private String employeeNo;

        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String mobile;

        private String email;
        private Integer gender;
        private String idCard;
        private String employeeType;
        private String employmentForm;
        private String position;
        private String jobLevel;
        private LocalDate joinDate;
        private LocalDate leaveDate;
        private String certStatus;
        private String certItem;
        private LocalDate certExpireDate;
        private Integer status = 1;
        private String remark;

        @NotEmpty(message = "请至少分配一个角色")
        private List<Long> roleIds = new ArrayList<>();

        @NotNull(message = "请指定默认角色")
        private Long defaultRoleId;

        private List<CompanyRelation> companies = new ArrayList<>();
    }

    @Data
    public static class RoleAssignRequest {
        @NotEmpty(message = "请至少分配一个角色")
        private List<Long> roleIds = new ArrayList<>();
        @NotNull(message = "请指定默认角色")
        private Long defaultRoleId;
    }

    @Data
    public static class StatusRequest {
        @NotNull(message = "请选择用户状态")
        private Integer status;
        private LocalDate leaveDate;
        private String remark;
    }
}
