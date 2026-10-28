package com.guolian.pm.crm.transfer.adapter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.crm.contract.entity.CrmContract;
import com.guolian.pm.crm.contract.mapper.CrmContractMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 合同交接：已终止（TERMINATED）合同不参与交接 —— 归档性质，责任人已无实际职责。
 *
 * <p>在途审批中的合同照常交接，审批流不因负责人变更而中断（Flowable 中 assignee 为变量，
 * 只影响后续待办接收人）。
 */
@Component
@RequiredArgsConstructor
public class CrmContractTransferAdapter extends AbstractCrmTransferAdapter {

    private final CrmContractMapper contractMapper;
    private final CrmOwnerSupport ownerSupport;

    @Override
    protected CrmOwnerSupport ownerSupport() {
        return ownerSupport;
    }

    @Override
    public String bizType() {
        return "crm_contract";
    }

    @Override
    public String bizName() {
        return "合同";
    }

    @Override
    protected long countByOwner(Long fromUserId) {
        return contractMapper.countTransfer(fromUserId);
    }

    @Override
    protected List<Long> pageIds(Long fromUserId, int offset, int limit) {
        return contractMapper.selectTransferIds(fromUserId, offset, limit);
    }

    @Override
    protected int updateOwner(List<Long> bizIds, Long toUserId, Long toOrgId, String toUserName) {
        CrmContract update = new CrmContract();
        update.setOwnerUserId(toUserId);
        update.setOwnerOrgId(toOrgId);
        update.setOwnerUserName(toUserName);
        return contractMapper.update(update, Wrappers.<CrmContract>lambdaUpdate().in(CrmContract::getId, bizIds));
    }

    @Override
    protected String describeById(Long bizId) {
        CrmContract contract = contractMapper.selectById(bizId);
        if (contract == null) {
            return null;
        }
        return contract.getContractNo() + " / " + contract.getName();
    }
}
