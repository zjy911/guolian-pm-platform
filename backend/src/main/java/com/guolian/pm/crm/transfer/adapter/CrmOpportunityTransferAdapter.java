package com.guolian.pm.crm.transfer.adapter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.crm.opportunity.entity.CrmOpportunity;
import com.guolian.pm.crm.opportunity.mapper.CrmOpportunityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 商机交接。
 *
 * <p>商机不限状态：赢单/输单的商机仍可能被查阅与复用，所以全量参与交接，
 * 与合同（排除 TERMINATED）、协议（排除 EXPIRED）的语义不同。
 */
@Component
@RequiredArgsConstructor
public class CrmOpportunityTransferAdapter extends AbstractCrmTransferAdapter {

    private final CrmOpportunityMapper opportunityMapper;
    private final CrmOwnerSupport ownerSupport;

    @Override
    protected CrmOwnerSupport ownerSupport() {
        return ownerSupport;
    }

    @Override
    public String bizType() {
        return "crm_opportunity";
    }

    @Override
    public String bizName() {
        return "商机";
    }

    @Override
    protected long countByOwner(Long fromUserId) {
        return opportunityMapper.countTransfer(fromUserId);
    }

    @Override
    protected List<Long> pageIds(Long fromUserId, int offset, int limit) {
        return opportunityMapper.selectTransferIds(fromUserId, offset, limit);
    }

    @Override
    protected int updateOwner(List<Long> bizIds, Long toUserId, Long toOrgId, String toUserName) {
        CrmOpportunity update = new CrmOpportunity();
        update.setOwnerUserId(toUserId);
        update.setOwnerOrgId(toOrgId);
        update.setOwnerUserName(toUserName);
        return opportunityMapper.update(update, Wrappers.<CrmOpportunity>lambdaUpdate().in(CrmOpportunity::getId, bizIds));
    }

    @Override
    protected String describeById(Long bizId) {
        CrmOpportunity opp = opportunityMapper.selectById(bizId);
        return opp == null ? null : opp.getName();
    }
}
