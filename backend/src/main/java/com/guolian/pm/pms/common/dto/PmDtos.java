package com.guolian.pm.pms.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目管理模块请求/响应 DTO。
 */
public class PmDtos {

    private PmDtos() {
    }

    /** 产品保存（含任务模板 JSON，发布后版本快照只增不改） */
    @Data
    public static class ProductSaveRequest {
        private Long id;
        @NotBlank(message = "产品编码不能为空")
        private String productCode;
        @NotBlank(message = "产品名称不能为空")
        private String productName;
        private Long bgOrgId;
        private String bgOrgName;
        private String pricingMode;
        private String remark;
        /** 6 环节任务模板 JSON，发布时生成新版本 */
        private String templateJson;
    }

    /** 立项申请：提交后生成项目（INITIATING）+ 立项表单实例 */
    @Data
    public static class InitiationRequest {
        @NotNull(message = "请选择服务产品")
        private Long productId;
        @NotBlank(message = "项目名称不能为空")
        private String projectName;
        @NotNull(message = "请选择客户主体")
        private Long customerId;
        private String customerName;
        private String contractNo;
        private String signEntity;
        @NotNull(message = "请填写预估合同额")
        private BigDecimal estimatedAmount;
        private Integer annualCases;
        private String pricingMode;
        private String rating;
        /** 四维风险评估等扩展字段 JSON */
        private String formData;
    }

    /** 人员指派进项目（PRD 6.3 关联项目字段） */
    @Data
    public static class MemberAssignRequest {
        @NotNull(message = "请选择项目")
        private Long projectId;
        @NotBlank(message = "请选择项目角色")
        private String projectRole;
        @NotNull(message = "请填写投入比例")
        private Integer inputRatio;
        private LocalDate joinDate;
        private LocalDate expectedLeaveDate;
    }

    /** 项目成员变更（变更角色/投入比例） */
    @Data
    public static class MemberUpdateRequest {
        private String projectRole;
        private Integer inputRatio;
        private LocalDate expectedLeaveDate;
    }

    /** 表单数据保存/提交 */
    @Data
    public static class FormSaveRequest {
        @NotBlank(message = "表单数据不能为空")
        private String formData;
    }
}
