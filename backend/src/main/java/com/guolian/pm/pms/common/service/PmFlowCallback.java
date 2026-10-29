package com.guolian.pm.pms.common.service;

import com.guolian.pm.flow.event.FlowFinishedEvent;
import com.guolian.pm.pms.form.service.FormInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * PM 审批回调：立项单（PM_INITIATION）与通用表单（PM_FORM）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PmFlowCallback {

    private final FormInstanceService formInstanceService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFlowFinished(FlowFinishedEvent event) {
        String bizType = event.bizType();
        if (!"PM_INITIATION".equals(bizType) && !"PM_FORM".equals(bizType)) {
            return;
        }
        try {
            formInstanceService.onFlowFinished(event.bizId(), event.approved(), null);
        } catch (Exception e) {
            log.error("PM 审批回调处理失败 bizType={} bizId={}", bizType, event.bizId(), e);
        }
    }
}
