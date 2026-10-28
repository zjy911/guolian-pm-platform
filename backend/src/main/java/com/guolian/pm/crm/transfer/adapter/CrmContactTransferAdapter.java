package com.guolian.pm.crm.transfer.adapter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.crm.contact.entity.CrmContact;
import com.guolian.pm.crm.contact.mapper.CrmContactMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 联系人交接。
 */
@Component
@RequiredArgsConstructor
public class CrmContactTransferAdapter extends AbstractCrmTransferAdapter {

    private final CrmContactMapper contactMapper;
    private final CrmOwnerSupport ownerSupport;

    @Override
    protected CrmOwnerSupport ownerSupport() {
        return ownerSupport;
    }

    @Override
    public String bizType() {
        return "crm_contact";
    }

    @Override
    public String bizName() {
        return "联系人";
    }

    @Override
    protected long countByOwner(Long fromUserId) {
        return contactMapper.countTransfer(fromUserId);
    }

    @Override
    protected List<Long> pageIds(Long fromUserId, int offset, int limit) {
        return contactMapper.selectTransferIds(fromUserId, offset, limit);
    }

    @Override
    protected int updateOwner(List<Long> bizIds, Long toUserId, Long toOrgId, String toUserName) {
        CrmContact update = new CrmContact();
        update.setOwnerUserId(toUserId);
        update.setOwnerOrgId(toOrgId);
        update.setOwnerUserName(toUserName);
        return contactMapper.update(update, Wrappers.<CrmContact>lambdaUpdate().in(CrmContact::getId, bizIds));
    }

    @Override
    protected String describeById(Long bizId) {
        CrmContact contact = contactMapper.selectById(bizId);
        return contact == null ? null : contact.getName();
    }
}
