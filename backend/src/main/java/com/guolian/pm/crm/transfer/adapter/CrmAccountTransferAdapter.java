package com.guolian.pm.crm.transfer.adapter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.crm.account.entity.CrmAccount;
import com.guolian.pm.crm.account.mapper.CrmAccountMapper;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 客户交接：公共池客户无负责人语义，计数与执行都排除。
 */
@Component
@RequiredArgsConstructor
public class CrmAccountTransferAdapter extends AbstractCrmTransferAdapter {

    private final CrmAccountMapper accountMapper;
    private final CrmOwnerSupport ownerSupport;

    @Override
    protected CrmOwnerSupport ownerSupport() {
        return ownerSupport;
    }

    @Override
    public String bizType() {
        return "crm_account";
    }

    @Override
    public String bizName() {
        return "客户";
    }

    @Override
    protected long countByOwner(Long fromUserId) {
        return accountMapper.countTransfer(fromUserId);
    }

    @Override
    protected List<Long> pageIds(Long fromUserId, int offset, int limit) {
        return accountMapper.selectTransferIds(fromUserId, offset, limit);
    }

    @Override
    protected int updateOwner(List<Long> bizIds, Long toUserId, Long toOrgId, String toUserName) {
        CrmAccount update = new CrmAccount();
        update.setOwnerUserId(toUserId);
        update.setOwnerOrgId(toOrgId);
        update.setOwnerUserName(toUserName);
        return accountMapper.update(update, Wrappers.<CrmAccount>lambdaUpdate().in(CrmAccount::getId, bizIds));
    }

    @Override
    protected String describeById(Long bizId) {
        CrmAccount account = accountMapper.selectById(bizId);
        return account == null ? null : account.getCompanyName();
    }
}
