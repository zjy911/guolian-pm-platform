package com.guolian.pm.crm.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * CRM 跨对象共享 DTO。
 */
public class CrmDtos {

    /** 查重命中项 */
    @Data
    public static class DupHit {
        /** LEAD / ACCOUNT / CONTACT */
        private String type;
        private Long id;
        private String name;
        private String companyName;
        private String ownerUserName;
        private String poolStatus;
        private String mobileMasked;
    }

    @Data
    public static class DupCheckResult {
        private boolean duplicated;
        private List<DupHit> hits = new ArrayList<>();
    }

    /** 线索转化入参 */
    @Data
    public static class ConvertRequest {
        /** NEW_ACCOUNT 新建客户 / EXIST_ACCOUNT 关联已有客户 */
        private String convertMode = "NEW_ACCOUNT";
        private Long accountId;
        /** 转化表单必选：服务产品（跨模块只读引用，模块未接入时可为空） */
        private Long productId;
        private String opportunityName;
        private BigDecimal expectedAmount;
    }

    /** 转化结果（idempotent=true 表示本次未新建，返回的是既有结果） */
    public record ConvertResultHolder(boolean idempotent, Long accountId, Long contactId, Long opportunityId) {
    }

    /** 通用状态推进入参 */
    @Data
    public static class StageRequest {
        private String targetStage;
        private String reason;
    }

    /** 负责人转移入参 */
    @Data
    public static class OwnerRequest {
        private Long ownerUserId;
    }

    /** 金额/数量统计项 */
    @Data
    public static class StatItem {
        private String key;
        private String label;
        private long count;
        private BigDecimal amount = BigDecimal.ZERO;

        public StatItem(String key, String label) {
            this.key = key;
            this.label = label;
        }
    }

    /** 漏斗阶段 */
    @Data
    public static class FunnelStage {
        private String stage;
        private String stageLabel;
        private long count;
        private BigDecimal amount = BigDecimal.ZERO;
        /** 相对上一阶段转化率（%） */
        private BigDecimal convertRate = BigDecimal.ZERO;
    }

    /** 供项目管理模块的立项来源快照（字段名冻结，不得改名） */
    @Data
    public static class OpeningSource {
        private String sourceType;
        private Long sourceId;
        private String sourceNo;
        private String sourceName;
        private Long accountId;
        private String accountName;
        private Long productId;
        private String productName;
        private Long businessGroupId;
        private String businessGroupName;
        private BigDecimal amount;
        private String currency;
        private String pricingMode;
        private String pricingDetail;
        private String startDate;
        private String endDate;
        private Long ownerUserId;
        private String ownerUserName;
        private Long orgId;
    }
}
