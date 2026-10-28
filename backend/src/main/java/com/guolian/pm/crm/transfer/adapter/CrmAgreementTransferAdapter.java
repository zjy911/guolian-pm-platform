package com.guolian.pm.crm.transfer.adapter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.crm.agreement.entity.CrmAgreement;
import com.guolian.pm.crm.agreement.mapper.CrmAgreementMapper;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 协议交接：已失效（EXPIRED）协议不参与交接。
 *
 * <p>长期协议 end_date 为空也会被到期扫描排除，这里只需按状态过滤即可。
 */
@Component
@RequiredArgsConstructor
public class CrmAgreementTransferAdapter extends AbstractCrmTransferAdapter {

    private final CrmAgreementMapper agreementMapper;
    private final CrmOwnerSupport ownerSupport;

    @Override
    protected CrmOwnerSupport ownerSupport() {
        return ownerSupport;
    }

    @Override
    public String bizType() {
        return "crm_agreement";
    }

    @Override
    public String bizName() {
        return "协议";
    }

    @Override
    protected long countByOwner(Long fromUserId) {
        return agreementMapper.countTransfer(fromUserId);
    }

    @Override
    protected List<Long> pageIds(Long fromUserId, int offset, int limit) {
        return agreementMapper.selectTransferIds(fromUserId, offset, limit);
    }

    @Override
    protected int updateOwner(List<Long> bizIds, Long toUserId, Long toOrgId, String toUserName) {
        CrmAgreement update = new CrmAgreement();
        update.setOwnerUserId(toUserId);
        update.setOwnerOrgId(toOrgId);
        update.setOwnerUserName(toUserName);
        return agreementMapper.update(update, Wrappers.<CrmAgreement>lambdaUpdate().in(CrmAgreement::getId, bizIds));
    }

    @Override
    protected String describeById(Long bizId) {
        CrmAgreement agreement = agreementMapper.selectById(bizId);
        if (agreement == null) {
            return null;
        }
        return agreement.getAgreementNo() + " / " + agreement.getName();
    }
}
