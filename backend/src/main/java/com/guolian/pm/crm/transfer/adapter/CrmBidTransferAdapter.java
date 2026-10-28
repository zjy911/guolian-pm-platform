package com.guolian.pm.crm.transfer.adapter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.crm.bid.entity.CrmBid;
import com.guolian.pm.crm.bid.mapper.CrmBidMapper;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 投标交接：已出结果（WON/LOST/ABANDONED）的投标不再参与交接。
 *
 * <p>在途投标（TRACKING/REGISTERED/SUBMITTED）必须交接，否则开标提醒会发给已经离职的人。
 */
@Component
@RequiredArgsConstructor
public class CrmBidTransferAdapter extends AbstractCrmTransferAdapter {

    private final CrmBidMapper bidMapper;
    private final CrmOwnerSupport ownerSupport;

    @Override
    protected CrmOwnerSupport ownerSupport() {
        return ownerSupport;
    }

    @Override
    public String bizType() {
        return "crm_bid";
    }

    @Override
    public String bizName() {
        return "投标";
    }

    @Override
    protected long countByOwner(Long fromUserId) {
        return bidMapper.countTransfer(fromUserId);
    }

    @Override
    protected List<Long> pageIds(Long fromUserId, int offset, int limit) {
        return bidMapper.selectTransferIds(fromUserId, offset, limit);
    }

    @Override
    protected int updateOwner(List<Long> bizIds, Long toUserId, Long toOrgId, String toUserName) {
        CrmBid update = new CrmBid();
        update.setOwnerUserId(toUserId);
        update.setOwnerOrgId(toOrgId);
        update.setOwnerUserName(toUserName);
        return bidMapper.update(update, Wrappers.<CrmBid>lambdaUpdate().in(CrmBid::getId, bizIds));
    }

    @Override
    protected String describeById(Long bizId) {
        CrmBid bid = bidMapper.selectById(bizId);
        if (bid == null) {
            return null;
        }
        return bid.getTenderNo() + " / " + bid.getTenderName();
    }
}
