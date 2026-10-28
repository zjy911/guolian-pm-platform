package com.guolian.pm.crm.transfer.adapter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.crm.lead.entity.CrmLead;
import com.guolian.pm.crm.lead.mapper.CrmLeadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 线索交接：已转化线索不再参与交接（转化后归属跟着客户走）。
 */
@Component
@RequiredArgsConstructor
public class CrmLeadTransferAdapter extends AbstractCrmTransferAdapter {

    private final CrmLeadMapper leadMapper;
    private final CrmOwnerSupport ownerSupport;

    @Override
    protected CrmOwnerSupport ownerSupport() {
        return ownerSupport;
    }

    @Override
    public String bizType() {
        return "crm_lead";
    }

    @Override
    public String bizName() {
        return "线索";
    }

    @Override
    protected long countByOwner(Long fromUserId) {
        return leadMapper.countTransfer(fromUserId);
    }

    @Override
    protected List<Long> pageIds(Long fromUserId, int offset, int limit) {
        return leadMapper.selectTransferIds(fromUserId, offset, limit);
    }

    @Override
    protected int updateOwner(List<Long> bizIds, Long toUserId, Long toOrgId, String toUserName) {
        CrmLead update = new CrmLead();
        update.setOwnerUserId(toUserId);
        update.setOwnerOrgId(toOrgId);
        update.setOwnerUserName(toUserName);
        return leadMapper.update(update, Wrappers.<CrmLead>lambdaUpdate().in(CrmLead::getId, bizIds));
    }

    @Override
    protected String describeById(Long bizId) {
        CrmLead lead = leadMapper.selectById(bizId);
        return lead == null ? null : lead.getCompanyName() + " / " + lead.getName();
    }
}
