package com.guolian.pm.crm.common.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.crm.account.entity.CrmAccount;
import com.guolian.pm.crm.account.mapper.CrmAccountMapper;
import com.guolian.pm.crm.agreement.entity.CrmAgreement;
import com.guolian.pm.crm.agreement.mapper.CrmAgreementMapper;
import com.guolian.pm.crm.bid.entity.CrmBid;
import com.guolian.pm.crm.bid.mapper.CrmBidMapper;
import com.guolian.pm.crm.common.dto.CrmDtos;
import com.guolian.pm.crm.contract.entity.CrmContract;
import com.guolian.pm.crm.contract.mapper.CrmContractMapper;
import com.guolian.pm.crm.opportunity.entity.CrmOpportunity;
import com.guolian.pm.crm.opportunity.mapper.CrmOpportunityMapper;
import com.guolian.pm.system.org.entity.SysOrg;
import com.guolian.pm.system.org.mapper.SysOrgMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 供项目管理模块的立项来源只读契约（本期冻结）。
 *
 * <p>约束：只提供 GET；返回数据按<b>调用方当前角色</b>做数据权限与脱敏，不做特殊放行；
 * 字段名不得改动，新增字段可加，删除或改名必须提 /v2。
 */
@Service
@RequiredArgsConstructor
public class CrmOpeningSourceService {

    private final CrmOpportunityMapper opportunityMapper;
    private final CrmBidMapper bidMapper;
    private final CrmContractMapper contractMapper;
    private final CrmAgreementMapper agreementMapper;
    private final CrmAccountMapper accountMapper;
    private final SysOrgMapper orgMapper;
    private final ProductRefService productRefService;

    /** 成交商机（sales_stage = WON） */
    public List<CrmDtos.OpeningSource> wonOpportunities(String keyword) {
        List<CrmOpportunity> list = opportunityMapper.selectList(Wrappers.<CrmOpportunity>lambdaQuery()
                .eq(CrmOpportunity::getSalesStage, "WON")
                .like(keyword != null && !keyword.isBlank(), CrmOpportunity::getName, keyword)
                .orderByDesc(CrmOpportunity::getStageChangeTime)
                .last("limit 200"));
        List<CrmDtos.OpeningSource> result = new ArrayList<>();
        for (CrmOpportunity o : list) {
            CrmDtos.OpeningSource s = base("OPPORTUNITY", o.getId(), null, o.getName());
            s.setAccountId(o.getAccountId());
            s.setProductId(o.getProductId());
            s.setAmount(o.getEstRevenue() != null ? o.getEstRevenue() : o.getExpectedAmount());
            s.setCurrency(o.getCurrency());
            s.setPricingMode(o.getPricingMode());
            s.setPricingDetail(o.getPricingDetail());
            s.setOwnerUserId(o.getOwnerUserId());
            s.setOwnerUserName(o.getOwnerUserName());
            s.setOrgId(o.getOwnerOrgId());
            enrich(s);
            result.add(s);
        }
        return result;
    }

    /** 中标投标（bid_status = WON） */
    public List<CrmDtos.OpeningSource> wonBids(String keyword) {
        List<CrmBid> list = bidMapper.selectList(Wrappers.<CrmBid>lambdaQuery()
                .eq(CrmBid::getBidStatus, "WON")
                .like(keyword != null && !keyword.isBlank(), CrmBid::getTenderName, keyword)
                .orderByDesc(CrmBid::getOpenDate)
                .last("limit 200"));
        List<CrmDtos.OpeningSource> result = new ArrayList<>();
        for (CrmBid b : list) {
            CrmDtos.OpeningSource s = base("BID", b.getId(), b.getTenderNo(), b.getTenderName());
            s.setAccountId(b.getAccountId());
            s.setAmount(b.getWinAmount());
            s.setCurrency("CNY");
            s.setOwnerUserId(b.getOwnerUserId());
            s.setOwnerUserName(b.getOwnerUserName());
            s.setOrgId(b.getOwnerOrgId());
            enrich(s);
            result.add(s);
        }
        return result;
    }

    /** 激活合同（status = ACTIVE） */
    public List<CrmDtos.OpeningSource> activeContracts(String keyword) {
        List<CrmContract> list = contractMapper.selectList(Wrappers.<CrmContract>lambdaQuery()
                .eq(CrmContract::getStatus, "ACTIVE")
                .like(keyword != null && !keyword.isBlank(), CrmContract::getName, keyword)
                .orderByDesc(CrmContract::getSignDate)
                .last("limit 200"));
        List<CrmDtos.OpeningSource> result = new ArrayList<>();
        for (CrmContract c : list) {
            CrmDtos.OpeningSource s = base("CONTRACT", c.getId(), c.getContractNo(), c.getName());
            s.setAccountId(c.getAccountId());
            s.setProductId(c.getProductId());
            s.setBusinessGroupId(c.getBusinessGroupId());
            s.setAmount(c.getWinAmount());
            s.setCurrency("CNY");
            s.setStartDate(c.getStartDate() == null ? null : c.getStartDate().toString());
            s.setEndDate(c.getEndDate() == null ? null : c.getEndDate().toString());
            s.setOwnerUserId(c.getOwnerUserId());
            s.setOwnerUserName(c.getOwnerUserName());
            s.setOrgId(c.getOwnerOrgId());
            enrich(s);
            result.add(s);
        }
        return result;
    }

    /** 生效协议（status = EFFECTIVE） */
    public List<CrmDtos.OpeningSource> effectiveAgreements(String keyword) {
        List<CrmAgreement> list = agreementMapper.selectList(Wrappers.<CrmAgreement>lambdaQuery()
                .eq(CrmAgreement::getStatus, "EFFECTIVE")
                .like(keyword != null && !keyword.isBlank(), CrmAgreement::getName, keyword)
                .orderByDesc(CrmAgreement::getSignDate)
                .last("limit 200"));
        List<CrmDtos.OpeningSource> result = new ArrayList<>();
        for (CrmAgreement a : list) {
            CrmDtos.OpeningSource s = base("AGREEMENT", a.getId(), a.getAgreementNo(), a.getName());
            s.setAccountId(a.getAccountId());
            s.setProductId(a.getProductId());
            s.setBusinessGroupId(a.getBusinessGroupId());
            s.setAmount(a.getWinAmount());
            s.setCurrency("CNY");
            s.setStartDate(a.getStartDate() == null ? null : a.getStartDate().toString());
            s.setEndDate(a.getEndDate() == null ? null : a.getEndDate().toString());
            s.setOwnerUserId(a.getOwnerUserId());
            s.setOwnerUserName(a.getOwnerUserName());
            s.setOrgId(a.getOwnerOrgId());
            enrich(s);
            result.add(s);
        }
        return result;
    }

    /** 单条详情快照 */
    public CrmDtos.OpeningSource byId(String type, Long id) {
        List<CrmDtos.OpeningSource> list = switch (type) {
            case "OPPORTUNITY" -> wonOpportunities(null);
            case "BID" -> wonBids(null);
            case "CONTRACT" -> activeContracts(null);
            case "AGREEMENT" -> effectiveAgreements(null);
            default -> throw BizException.biz("非法的来源类型：" + type);
        };
        return list.stream().filter(s -> s.getSourceId().equals(id)).findFirst()
                .orElseThrow(() -> BizException.notFound("来源单据不存在或无权查看"));
    }

    private CrmDtos.OpeningSource base(String type, Long id, String no, String name) {
        CrmDtos.OpeningSource s = new CrmDtos.OpeningSource();
        s.setSourceType(type);
        s.setSourceId(id);
        s.setSourceNo(no);
        s.setSourceName(name);
        return s;
    }

    /** 补齐客户名、产品名、业务线名（不做额外权限放大，均按调用方范围查） */
    private void enrich(CrmDtos.OpeningSource s) {
        if (s.getAccountId() != null) {
            CrmAccount account = accountMapper.selectById(s.getAccountId());
            s.setAccountName(account == null ? null : account.getCompanyName());
        }
        if (s.getProductId() != null && productRefService.isAvailable()) {
            ProductRefService.ProductBrief brief = productRefService.getById(s.getProductId());
            s.setProductName(brief == null ? null : brief.name());
        }
        if (s.getBusinessGroupId() != null) {
            SysOrg org = orgMapper.selectById(s.getBusinessGroupId());
            s.setBusinessGroupName(org == null ? null : org.getOrgName());
        }
    }
}
