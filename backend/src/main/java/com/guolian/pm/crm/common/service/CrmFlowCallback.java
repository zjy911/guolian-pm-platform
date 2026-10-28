package com.guolian.pm.crm.common.service;

import com.guolian.pm.crm.account.service.AccountService;
import com.guolian.pm.crm.agreement.service.AgreementService;
import com.guolian.pm.crm.contract.service.ContractService;
import com.guolian.pm.flow.event.FlowFinishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * CRM 审批回调：底座流程结束事件 → 业务状态流转。
 *
 * <p>CRM 不注入 RuntimeService/TaskService，也不建审批表；唯一入口就是本监听器。
 * 所有动作都幂等（条件更新），重复回调不产生副作用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmFlowCallback {

    private final ContractService contractService;
    private final AgreementService agreementService;
    private final AccountService accountService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFlowFinished(FlowFinishedEvent event) {
        String bizType = event.bizType();
        if (bizType == null) {
            return;
        }
        try {
            switch (bizType) {
                case "CRM_CONTRACT" -> contractService.onFlowFinished(event.bizId(), event.approved());
                case "CRM_AGREEMENT" -> agreementService.onFlowFinished(event.bizId(), event.approved());
                case "CRM_CLAIM" -> {
                    if (event.approved()) {
                        accountService.applyClaimApproved(event.bizId());
                    } else {
                        accountService.applyClaimRejected(event.bizId(), false);
                    }
                }
                default -> {
                    // 非 CRM 业务类型不处理
                }
            }
        } catch (Exception e) {
            log.error("CRM 审批回调处理失败 bizType={} bizId={}", bizType, event.bizId(), e);
        }
    }
}
