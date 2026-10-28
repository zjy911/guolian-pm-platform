package com.guolian.pm.system.org.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

public class OrgDtos {

    @Data
    public static class SaveRequest {
        private Long id;

        @NotBlank(message = "请输入组织编码")
        private String orgCode;

        @NotBlank(message = "请输入组织名称")
        private String orgName;

        @NotBlank(message = "请选择组织类型")
        private String orgType;

        private Long parentId = 0L;
        private Long leaderId;
        private String costCenterCode;
        private Integer sortNo = 0;
        private Integer status = 1;
        private LocalDate effectiveDate;
        private LocalDate expireDate;
    }

    @Data
    public static class MoveRequest {
        private Long parentId;
    }
}
