package com.guolian.pm.crm.common.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.common.cache.CacheService;
import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.common.web.LoginUser;
import com.guolian.pm.crm.account.entity.CrmAccount;
import com.guolian.pm.crm.account.mapper.CrmAccountMapper;
import com.guolian.pm.crm.bid.entity.CrmBid;
import com.guolian.pm.crm.bid.mapper.CrmBidMapper;
import com.guolian.pm.crm.common.dto.CrmDtos;
import com.guolian.pm.crm.contract.entity.CrmContract;
import com.guolian.pm.crm.contract.mapper.CrmContractMapper;
import com.guolian.pm.crm.lead.entity.CrmLead;
import com.guolian.pm.crm.lead.mapper.CrmLeadMapper;
import com.guolian.pm.crm.opportunity.entity.CrmOpportunity;
import com.guolian.pm.crm.opportunity.mapper.CrmOpportunityMapper;
import com.guolian.pm.crm.opportunity.service.OpportunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CRM 统计与看板供数：全部走数据权限（不看全公司数据），结果缓存 5 分钟，
 * 缓存 key 含 roleId + orgId，切角色后自然失效。
 */
@Service
@RequiredArgsConstructor
public class CrmStatsService {

    /** 结果缓存 5 分钟，key 含 roleId + orgId */
    private static final long TTL_SECONDS = 300;

    private final CrmLeadMapper leadMapper;
    private final CrmAccountMapper accountMapper;
    private final CrmOpportunityMapper opportunityMapper;
    private final CrmContractMapper contractMapper;
    private final CrmBidMapper bidMapper;
    private final OpportunityService opportunityService;
    private final CacheService cacheService;

    /** 首页四张小卡：我的线索 / 商机金额 / 待办合同 / 本月投标 */
    public Map<String, Object> summary() {
        LoginUser user = AuthContext.require();
        String key = cacheKey("summary", user);
        Map<String, Object> cached = cacheService.get(key, Map.class);
        if (cached != null) {
            return cached;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        long myLeads = leadMapper.selectCount(Wrappers.<CrmLead>lambdaQuery()
                .eq(CrmLead::getOwnerUserId, user.getUserId())
                .ne(CrmLead::getStatus, "CONVERTED"));
        BigDecimal oppAmount = nvl(opportunityMapper.selectList(Wrappers.<CrmOpportunity>lambdaQuery()
                        .notIn(CrmOpportunity::getSalesStage, List.of("LOST")))
                .stream().map(o -> o.getExpectedAmount() == null ? BigDecimal.ZERO : o.getExpectedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        long pendingContracts = contractMapper.selectCount(Wrappers.<CrmContract>lambdaQuery()
                .in(CrmContract::getStatus, List.of("DRAFT", "APPROVING")));
        long monthBids = bidMapper.selectCount(Wrappers.<CrmBid>lambdaQuery()
                .between(CrmBid::getOpenDate, monthStart, monthEnd));

        result.put("myLeads", myLeads);
        result.put("opportunityAmount", oppAmount);
        result.put("pendingContracts", pendingContracts);
        result.put("monthBids", monthBids);
        cacheService.put(key, result, TTL_SECONDS);
        return result;
    }

    /**
     * 最小看板接口：新增线索、转化率、各阶段商机、合同签约、投标中标率。
     */
    public Map<String, Object> funnel(LocalDate startDate, LocalDate endDate) {
        LoginUser user = AuthContext.require();
        String key = cacheKey("funnel:" + startDate + ":" + endDate, user);
        Map<String, Object> cached = cacheService.get(key, Map.class);
        if (cached != null) {
            return cached;
        }
        Map<String, Object> result = new LinkedHashMap<>();

        long newLeads = leadMapper.selectCount(Wrappers.<CrmLead>lambdaQuery()
                .ge(startDate != null, CrmLead::getCreatedTime,
                        startDate == null ? null : startDate.atStartOfDay())
                .le(endDate != null, CrmLead::getCreatedTime,
                        endDate == null ? null : endDate.atTime(23, 59, 59)));
        long convertedLeads = leadMapper.selectCount(Wrappers.<CrmLead>lambdaQuery()
                .eq(CrmLead::getStatus, "CONVERTED")
                .ge(startDate != null, CrmLead::getCreatedTime,
                        startDate == null ? null : startDate.atStartOfDay())
                .le(endDate != null, CrmLead::getCreatedTime,
                        endDate == null ? null : endDate.atTime(23, 59, 59)));
        result.put("newLeads", newLeads);
        result.put("convertedLeads", convertedLeads);
        result.put("leadConvertRate", newLeads == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(convertedLeads * 100.0 / newLeads).setScale(2, java.math.RoundingMode.HALF_UP));

        List<CrmDtos.FunnelStage> stages = opportunityService.funnel();
        result.put("stages", stages);

        List<CrmContract> contracts = contractMapper.selectList(Wrappers.<CrmContract>lambdaQuery()
                .eq(CrmContract::getStatus, "ACTIVE"));
        result.put("activeContracts", contracts.size());
        result.put("contractAmount", nvl(contracts.stream()
                .map(c -> c.getWinAmount() == null ? BigDecimal.ZERO : c.getWinAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add)));

        List<CrmBid> bids = bidMapper.selectList(Wrappers.<CrmBid>lambdaQuery()
                .ge(startDate != null, CrmBid::getOpenDate, startDate)
                .le(endDate != null, CrmBid::getOpenDate, endDate));
        long won = bids.stream().filter(b -> "WON".equals(b.getBidStatus())).count();
        result.put("bids", bids.size());
        result.put("wonBids", won);
        result.put("bidWinRate", bids.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(won * 100.0 / bids.size()).setScale(2, java.math.RoundingMode.HALF_UP));

        long customers = accountMapper.selectCount(Wrappers.<CrmAccount>lambdaQuery()
                .eq(CrmAccount::getIsProspect, 0)
                .ne(CrmAccount::getPoolStatus, "CHURNED"));
        result.put("customers", customers);
        cacheService.put(key, result, TTL_SECONDS);
        return result;
    }

    private String cacheKey(String biz, LoginUser user) {
        return "crm:stats:" + biz + ":" + user.getRoleId() + ":" + user.getOrgId();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
