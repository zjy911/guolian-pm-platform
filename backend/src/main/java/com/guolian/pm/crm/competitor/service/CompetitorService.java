package com.guolian.pm.crm.competitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.crm.competitor.entity.CrmCompetitor;
import com.guolian.pm.crm.competitor.mapper.CrmCompetitorMapper;
import com.guolian.pm.crm.opportunity.entity.CrmOpportunity;
import com.guolian.pm.crm.opportunity.mapper.CrmOpportunityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 竞对服务：挂在商机下，提供对比矩阵数据。竞对是商机子资源，权限复用商机权限。
 */
@Service
@RequiredArgsConstructor
public class CompetitorService {

    private final CrmCompetitorMapper competitorMapper;
    private final CrmOpportunityMapper opportunityMapper;
    private final CrmOwnerSupport ownerSupport;

    public List<CrmCompetitor> list(Long opportunityId) {
        return competitorMapper.selectList(Wrappers.<CrmCompetitor>lambdaQuery()
                .eq(CrmCompetitor::getOpportunityId, opportunityId)
                .orderByAsc(CrmCompetitor::getCreatedTime));
    }

    @Transactional
    public Long create(CrmCompetitor request) {
        if (request.getOpportunityId() == null) {
            throw BizException.biz("竞对必须挂在商机下");
        }
        if (!StringUtils.hasText(request.getCompetitorName())) {
            throw BizException.biz("请填写竞对名称");
        }
        CrmOpportunity opportunity = opportunityMapper.selectById(request.getOpportunityId());
        if (opportunity == null) {
            throw BizException.notFound("所属商机不存在或无权维护");
        }
        long sameName = competitorMapper.selectCount(Wrappers.<CrmCompetitor>lambdaQuery()
                .eq(CrmCompetitor::getOpportunityId, request.getOpportunityId())
                .eq(CrmCompetitor::getCompetitorName, request.getCompetitorName()));
        if (sameName > 0) {
            throw BizException.biz("同一商机下竞对名称不得重复");
        }
        request.setId(null);
        request.setOwnerUserId(ownerSupport.currentUserId());
        request.setOwnerOrgId(ownerSupport.createOrgId());
        request.setOwnerUserName(ownerSupport.currentUserName());
        competitorMapper.insert(request);
        return request.getId();
    }

    @Transactional
    public void update(Long id, CrmCompetitor request) {
        CrmCompetitor exist = competitorMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("竞对不存在或无权修改");
        }
        request.setId(id);
        request.setOpportunityId(exist.getOpportunityId());
        request.setOwnerUserId(exist.getOwnerUserId());
        request.setOwnerOrgId(exist.getOwnerOrgId());
        request.setOwnerUserName(exist.getOwnerUserName());
        competitorMapper.updateById(request);
    }

    @Transactional
    public void delete(Long id) {
        competitorMapper.deleteById(id);
    }

    /** 对比矩阵：我方 + 各竞对，按同一组维度横向铺开 */
    public Map<String, Object> compare(Long opportunityId) {
        CrmOpportunity opportunity = opportunityMapper.selectById(opportunityId);
        if (opportunity == null) {
            throw BizException.notFound("商机不存在或无权查看");
        }
        Map<String, Object> ours = new LinkedHashMap<>();
        ours.put("name", "我方");
        ours.put("staffSize", null);
        ours.put("coverageScope", null);
        ours.put("techAbility", null);
        ours.put("pricingMode", opportunity.getPricingMode());
        ours.put("pricingDetail", opportunity.getPricingDetail());
        ours.put("operationEfficiency", null);
        ours.put("operationQuality", null);
        ours.put("estServiceVolume", opportunity.getEstServiceVolume());

        List<Map<String, Object>> competitors = list(opportunityId).stream().map(c -> {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", c.getId());
            row.put("name", c.getCompetitorName());
            row.put("staffSize", c.getStaffSize());
            row.put("coverageScope", c.getCoverageScope());
            row.put("techAbility", c.getTechAbility());
            row.put("pricingMode", c.getPricingMode());
            row.put("pricingDetail", c.getPricingDetail());
            row.put("operationEfficiency", c.getOperationEfficiency());
            row.put("operationQuality", c.getOperationQuality());
            return row;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("opportunityId", opportunityId);
        result.put("opportunityName", opportunity.getName());
        result.put("ours", ours);
        result.put("competitors", competitors);
        return result;
    }
}
