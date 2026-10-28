package com.guolian.pm.crm.opportunity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.crm.account.entity.CrmAccount;
import com.guolian.pm.crm.account.mapper.CrmAccountMapper;
import com.guolian.pm.crm.common.dto.CrmDtos;
import com.guolian.pm.crm.common.dto.CrmQuery;
import com.guolian.pm.crm.common.service.CrmEventService;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.crm.common.service.ProductRefService;
import com.guolian.pm.crm.opportunity.entity.CrmOpportunity;
import com.guolian.pm.crm.opportunity.mapper.CrmOpportunityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 商机服务：阶段单向推进、利润率服务端计算、赢单由合同/协议激活反推（幂等）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunityService {

    /** 阶段顺序（单向，允许跳段不允许回退） */
    private static final List<String> STAGES = List.of("PROSPECT", "PROPOSAL", "NEGOTIATION", "WON");
    private static final Map<String, String> STAGE_LABEL = Map.of(
            "PROSPECT", "寻找潜在客户", "PROPOSAL", "提案报价",
            "NEGOTIATION", "谈判", "WON", "成交", "LOST", "流失");

    private final CrmOpportunityMapper opportunityMapper;
    private final CrmAccountMapper accountMapper;
    private final CrmOwnerSupport ownerSupport;
    private final ProductRefService productRefService;
    private final CrmEventService eventService;
    private final ObjectMapper objectMapper;

    public PageResult<CrmOpportunity> page(CrmQuery q) {
        Page<CrmOpportunity> page = new Page<>(q.safePageNum(), q.safePageSize());
        var wrapper = Wrappers.<CrmOpportunity>lambdaQuery()
                .eq(StringUtils.hasText(q.getStage()), CrmOpportunity::getSalesStage, q.getStage())
                .eq(q.getAccountId() != null, CrmOpportunity::getAccountId, q.getAccountId())
                .eq(q.getOwnerUserId() != null, CrmOpportunity::getOwnerUserId, q.getOwnerUserId())
                .ge(q.getStartDate() != null, CrmOpportunity::getCreatedTime,
                        q.getStartDate() == null ? null : q.getStartDate().atStartOfDay())
                .le(q.getEndDate() != null, CrmOpportunity::getCreatedTime,
                        q.getEndDate() == null ? null : q.getEndDate().atTime(23, 59, 59))
                .like(StringUtils.hasText(q.getKeyword()), CrmOpportunity::getName, q.getKeyword())
                .orderByDesc(CrmOpportunity::getCreatedTime);
        Page<CrmOpportunity> result = opportunityMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), q.safePageNum(), q.safePageSize());
    }

    public CrmOpportunity get(Long id) {
        CrmOpportunity opportunity = opportunityMapper.selectById(id);
        if (opportunity == null) {
            throw BizException.notFound("商机不存在或无权查看");
        }
        return opportunity;
    }

    @Transactional
    public Long create(CrmOpportunity request) {
        if (!StringUtils.hasText(request.getName())) {
            throw BizException.biz("请填写商机名称");
        }
        if (request.getAccountId() == null) {
            throw BizException.biz("商机必须归属一个客户");
        }
        CrmAccount account = accountMapper.selectById(request.getAccountId());
        if (account == null) {
            throw BizException.notFound("所属客户不存在或无权维护");
        }
        validateProduct(request.getProductId());
        validatePricing(request.getPricingMode(), request.getPricingDetail());
        request.setId(null);
        request.setSalesStage(StringUtils.hasText(request.getSalesStage()) ? request.getSalesStage() : "PROSPECT");
        request.setCurrency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency() : "CNY");
        request.setIsWon("WON".equals(request.getSalesStage()) ? 1 : 0);
        request.setOwnerUserId(ownerSupport.currentUserId());
        request.setOwnerOrgId(ownerSupport.createOrgId());
        request.setOwnerUserName(ownerSupport.currentUserName());
        request.setStageChangeTime(LocalDateTime.now());
        recalcProfitRate(request);
        opportunityMapper.insert(request);
        return request.getId();
    }

    @Transactional
    public void update(Long id, CrmOpportunity request) {
        CrmOpportunity exist = opportunityMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("商机不存在或无权修改");
        }
        if (Integer.valueOf(1).equals(exist.getIsWon())) {
            throw BizException.biz("已成交商机不可编辑金额与阶段");
        }
        validateProduct(request.getProductId());
        validatePricing(StringUtils.hasText(request.getPricingMode()) ? request.getPricingMode() : exist.getPricingMode(),
                StringUtils.hasText(request.getPricingDetail()) ? request.getPricingDetail() : exist.getPricingDetail());
        request.setId(id);
        request.setOwnerUserId(exist.getOwnerUserId());
        request.setOwnerOrgId(exist.getOwnerOrgId());
        request.setOwnerUserName(exist.getOwnerUserName());
        request.setSalesStage(exist.getSalesStage());
        request.setIsWon(exist.getIsWon());
        request.setContractId(exist.getContractId());
        recalcProfitRate(request);
        opportunityMapper.updateById(request);
    }

    @Transactional
    public void delete(Long id) {
        CrmOpportunity exist = opportunityMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("商机不存在或无权删除");
        }
        if (Integer.valueOf(1).equals(exist.getIsWon())) {
            throw BizException.biz("已成交商机不可删除");
        }
        opportunityMapper.deleteById(id);
    }

    /** 阶段推进：只进不退；回退必须填原因并留痕 */
    @Transactional
    public void advance(Long id, String targetStage, String reason) {
        CrmOpportunity exist = opportunityMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("商机不存在或无权推进");
        }
        if (!STAGES.contains(targetStage) && !"LOST".equals(targetStage)) {
            throw BizException.biz("非法的目标阶段：" + targetStage);
        }
        if ("WON".equals(targetStage)) {
            throw BizException.biz("成交状态只能由合同或协议生效反推，不可手工置");
        }
        if ("LOST".equals(exist.getSalesStage())) {
            throw BizException.biz("已流失商机需先恢复到谈判阶段再推进");
        }
        int from = STAGES.indexOf(exist.getSalesStage());
        int to = STAGES.indexOf(targetStage);
        if (to >= 0 && from >= 0 && to < from) {
            if (!StringUtils.hasText(reason)) {
                throw BizException.biz("回退阶段必须填写原因");
            }
            log.info("商机 {} 阶段回退 {} -> {}，原因：{}", id, exist.getSalesStage(), targetStage, reason);
        }
        CrmOpportunity update = new CrmOpportunity();
        update.setId(id);
        update.setSalesStage(targetStage);
        update.setStageChangeTime(LocalDateTime.now());
        opportunityMapper.updateById(update);
        eventService.track(CrmEventService.OPP_STAGE_CHANGE, id, CrmEventService.props(
                "fromStage", exist.getSalesStage(), "toStage", targetStage,
                "expectedAmount", exist.getExpectedAmount()));
    }

    @Transactional
    public void markLost(Long id, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw BizException.biz("置流失必须填写原因");
        }
        CrmOpportunity exist = opportunityMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("商机不存在或无权操作");
        }
        if (Integer.valueOf(1).equals(exist.getIsWon())) {
            throw BizException.biz("已成交商机不可置流失");
        }
        CrmOpportunity update = new CrmOpportunity();
        update.setId(id);
        update.setSalesStage("LOST");
        update.setLostReason(reason);
        update.setStageChangeTime(LocalDateTime.now());
        opportunityMapper.updateById(update);
    }

    @Transactional
    public void changeOwner(Long id, Long toUserId) {
        CrmOpportunity exist = opportunityMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("商机不存在或无权转移");
        }
        ownerSupport.requireActiveUser(toUserId);
        Long toOrgId = ownerSupport.orgOf(toUserId);
        if (toOrgId == null) {
            throw BizException.biz("接收人未绑定角色组织，无法转移");
        }
        CrmOpportunity update = new CrmOpportunity();
        update.setId(id);
        update.setOwnerUserId(toUserId);
        update.setOwnerOrgId(toOrgId);
        update.setOwnerUserName(ownerSupport.nameOf(toUserId));
        opportunityMapper.updateById(update);
    }

    /**
     * 赢单反推：合同/协议生效时调用。条件更新保证幂等，重复调用不产生副作用。
     */
    @Transactional
    public void onContractActivated(Long opportunityId, Long contractId) {
        if (opportunityId == null) {
            return;
        }
        int rows = opportunityMapper.markWon(opportunityId, contractId);
        if (rows == 0) {
            // 已成交（可能是首签之后的续签）：只记录合同关联，不再改状态
            opportunityMapper.linkContract(opportunityId, contractId);
            log.info("商机 {} 已成交，合同 {} 仅记录关联", opportunityId, contractId);
        }
    }

    /** 漏斗聚合：数量 + 金额 + 阶段转化率 */
    public List<CrmDtos.FunnelStage> funnel() {
        List<Map<String, Object>> rows = opportunityMapper.funnel();
        List<CrmDtos.FunnelStage> stages = new ArrayList<>();
        for (String stage : List.of("PROSPECT", "PROPOSAL", "NEGOTIATION", "WON", "LOST")) {
            CrmDtos.FunnelStage fs = new CrmDtos.FunnelStage();
            fs.setStage(stage);
            fs.setStageLabel(STAGE_LABEL.getOrDefault(stage, stage));
            for (Map<String, Object> row : rows) {
                Object s = row.get("stage") == null ? row.get("STAGE") : row.get("stage");
                if (stage.equals(String.valueOf(s))) {
                    fs.setCount(toLong(row.get("cnt") == null ? row.get("CNT") : row.get("cnt")));
                    fs.setAmount(toDecimal(row.get("amount") == null ? row.get("AMOUNT") : row.get("amount")));
                }
            }
            stages.add(fs);
        }
        // 相邻阶段转化率
        for (int i = 1; i < stages.size(); i++) {
            long prev = stages.get(i - 1).getCount();
            if (prev > 0) {
                stages.get(i).setConvertRate(BigDecimal.valueOf(stages.get(i).getCount())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(prev), 2, RoundingMode.HALF_UP));
            }
        }
        return stages;
    }

    /** 利润率 =（预估收入 - 预估成本）/ 预估收入 * 100，分母为空或 0 时置空 */
    public void recalcProfitRate(CrmOpportunity opportunity) {
        BigDecimal revenue = opportunity.getEstRevenue();
        BigDecimal cost = opportunity.getEstCost();
        if (revenue == null || revenue.compareTo(BigDecimal.ZERO) == 0) {
            opportunity.setEstProfitRate(null);
            return;
        }
        BigDecimal profit = revenue.subtract(cost == null ? BigDecimal.ZERO : cost);
        opportunity.setEstProfitRate(profit.multiply(BigDecimal.valueOf(100))
                .divide(revenue, 2, RoundingMode.HALF_UP));
    }

    /** 计价口径结构化校验：7 种口径的必填项由服务端把关，不依赖前端 */
    private void validatePricing(String mode, String detailJson) {
        if (!StringUtils.hasText(mode)) {
            throw BizException.biz("请选择计价口径");
        }
        if ("FREE".equals(mode)) {
            return;
        }
        if (!StringUtils.hasText(detailJson)) {
            throw BizException.biz("计价口径为「" + mode + "」时必须填写计价明细");
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(detailJson);
        } catch (Exception e) {
            throw BizException.biz("计价明细不是合法 JSON");
        }
        String required = switch (mode) {
            case "LUMP_SUM" -> "totalAmount";
            case "UNIT_PRICE" -> "unitPrice";
            case "TIME_BASED" -> "hourlyRate";
            case "HEAD_COUNT" -> "pricePerPersonMonth";
            case "COMMISSION" -> "commissionRate";
            case "EFFECT_BASED" -> "unitPrice";
            default -> null;
        };
        if (required != null && (node.get(required) == null || node.get(required).isNull())) {
            throw BizException.biz("计价口径「" + mode + "」缺少必填项 " + required);
        }
    }

    private void validateProduct(Long productId) {
        if (productId == null) {
            return;
        }
        if (productRefService.isAvailable() && productRefService.getById(productId) == null) {
            throw BizException.biz("服务产品不存在或已停用");
        }
    }

    private long toLong(Object v) {
        return v == null ? 0L : Long.parseLong(String.valueOf(v).split("\\.")[0]);
    }

    private BigDecimal toDecimal(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(v));
    }

    /** 报表：区间内新增商机数 */
    public long countCreated(LocalDate from, LocalDate to) {
        return opportunityMapper.selectCount(Wrappers.<CrmOpportunity>lambdaQuery()
                .ge(from != null, CrmOpportunity::getCreatedTime, from == null ? null : from.atStartOfDay())
                .le(to != null, CrmOpportunity::getCreatedTime, to == null ? null : to.atTime(23, 59, 59)));
    }
}
