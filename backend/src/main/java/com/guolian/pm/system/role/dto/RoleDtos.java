package com.guolian.pm.system.role.dto;

import com.guolian.pm.common.api.PageQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

public class RoleDtos {

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Query extends PageQuery {
        private String keyword;
        private Long orgId;
        private Integer status;
        private String dataScope;
    }

    @Data
    public static class SaveRequest {
        private Long id;

        @NotBlank(message = "请输入角色编码")
        @Size(max = 50, message = "角色编码不超过 50 位")
        private String roleCode;

        @NotBlank(message = "请输入角色名称")
        private String roleName;

        @NotNull(message = "角色必须绑定一个组织")
        private Long orgId;

        private String description;
        private Integer roleType = 2;

        @NotBlank(message = "请选择数据范围")
        private String dataScope = "SELF";

        private List<Long> scopeOrgIds = new ArrayList<>();
        private Integer canBeProjectMember = 0;
        private Integer canBeApprover = 0;
        private Integer sortNo = 0;
        private Integer status = 1;
        private String remark;

        /** 修改绑定组织时的二次确认标记 */
        private Boolean confirmOrgChange = Boolean.FALSE;
    }

    @Data
    public static class MenuAuthRequest {
        private List<Long> menuIds = new ArrayList<>();
    }

    @Data
    public static class DataScopeRequest {
        @NotBlank(message = "请选择数据范围类型")
        private String dataScope;
        private List<Long> orgIds = new ArrayList<>();
        private List<Long> companyIds = new ArrayList<>();
    }

    @Data
    public static class MaskItem {
        @NotBlank(message = "字段组不能为空")
        private String groupCode;
        @NotBlank(message = "档位不能为空")
        private String maskLevel;
    }

    @Data
    public static class FieldMaskRequest {
        private List<MaskItem> masks = new ArrayList<>();
    }
}
