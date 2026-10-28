package com.guolian.pm.crm.contract.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.crm.account.entity.CrmAccount;
import com.guolian.pm.crm.account.mapper.CrmAccountMapper;
import com.guolian.pm.crm.common.dto.CrmQuery;
import com.guolian.pm.crm.common.service.CrmConfig;
import com.guolian.pm.crm.common.service.CrmEventService;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.crm.common.service.NoticePublisher;
import com.guolian.pm.crm.common.service.ProductRefService;
import com.guolian.pm.crm.contract.entity.CrmContract;
import com.guolian.pm.crm.contract.mapper.CrmContractMapper;
import com.guolian.pm.crm.opportunity.service.OpportunityService;
import com.guolian.pm.flow.dto.FlowDtos;
import com.guolian.pm.flow.service.FlowService;
import com.guolian.pm.system.code.service.CodeRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 合同服务：DRAFT → APPROVING → ACTIVE → EXPIRED / TERMINATED。
 *
 * <p>三条要点：
 * <ul>
 *   <li>一期只允许一份进行中的流程实例（发起前校验 sys_flow_biz）</li>
 *   <li>激活动作用条件更新实现幂等，重复回调不产生副作用</li>
 *   <li>激活在同一事务内触发赢单反推</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final CrmContractMapper contractMapper;
    private final CrmAccountMapper accountMapper;
    private final OpportunityService opportunityService;
    private final CodeRuleService codeRuleService;
    private final FlowService flowService;
    private final CrmOwnerSupport ownerSupport;
    private final CrmConfig crmConfig;
    private final ProductRefService productRefService;
    private final CrmEventService eventService;
    private final NoticePublisher noticePublisher;

    // ---------------- 查询 ----------------

    public PageResult<CrmContract> page(CrmQuery q) {
        Page<CrmContract> page = new Page<>(q.safePageNum(), q.safePageSize());
        var wrapper = Wrappers.<CrmContract>lambdaQuery()
                .eq(StringUtils.hasText(q.getStatus()), CrmContract::getStatus, q.getStatus())
                .eq(q.getAccountId() != null, CrmContract::getAccountId, q.getAccountId())
                .eq(q.getBusinessGroupId() != null, CrmContract::getBusinessGroupId, q.getBusinessGroupId())
                .eq(q.getOwnerUserId() != null, CrmContract::getOwnerUserId, q.getOwnerUserId())
                .ge(q.getStartDate() != null, CrmContract::getSignDate, q.getStartDate())
                .le(q.getEndDate() != null, CrmContract::getSignDate, q.getEndDate())
                .and(StringUtils.hasText(q.getKeyword()), w -> w
                        .like(CrmContract::getContractNo, q.getKeyword())
                        .or().like(CrmContract::getName, q.getKeyword()))
                .orderByDesc(CrmContract::getCreatedTime);
        Page<CrmContract> result = contractMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), q.safePageNum(), q.safePageSize());
    }

    public CrmContract get(Long id) {
        CrmContract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw BizException.notFound("合同不存在或无权查看");
        }
        return contract;
    }

    /** 到期预警 */
    public List<CrmContract> expiring(int days) {
        LocalDate today = LocalDate.now();
        return contractMapper.selectExpiring(today, today.plusDays(days <= 0 ? 30 : days));
    }

    // ---------------- 写入 ----------------

    @Transactional
    public Long create(CrmContract request) {
        if (request.getAccountId() == null) {
            throw BizException.biz("请选择合同客户");
        }
        if (request.getCompanyId() == null) {
            throw BizException.biz("请选择我方签约主体");
        }
        if (request.getBusinessGroupId() == null) {
            throw BizException.biz("请选择业务线（BG）");
        }
        validateDates(request.getStartDate(), request.getEndDate());
        CrmAccount account = accountMapper.selectById(request.getAccountId());
        if (account == null) {
            throw BizException.notFound("合同客户不存在或无权维护");
        }
        if (request.getProductId() != null && productRefService.isAvailable()
                && productRefService.getById(request.getProductId()) == null) {
            throw BizException.biz("服务产品不存在或已停用");
        }
        boolean renew = "RENEW".equals(request.getContractType());
        if (renew) {
            if (request.getFirstContractId() == null) {
                throw BizException.biz("续签合同必须关联首签合同");
            }
            CrmContract first = contractMapper.selectById(request.getFirstContractId());
            if (first == null) {
                throw BizException.notFound("首签合同不存在");
            }
            if ("TERMINATED".equals(first.getStatus())) {
                throw BizException.biz("首签合同已终止，不可再续签");
            }
        }
        request.setId(null);
        request.setContractNo(codeRuleService.generate("CRM_CONTRACT"));
        request.setStatus("DRAFT");
        request.setContractType(renew ? "RENEW" : "FIRST");
        request.setIsArchived(0);
        request.setDurationDays((int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1);
        request.setSourceType(StringUtils.hasText(request.getSourceType()) ? request.getSourceType() : "OPPORTUNITY");
        request.setOwnerUserId(ownerSupport.currentUserId());
        request.setOwnerOrgId(ownerSupport.createOrgId());
        request.setOwnerUserName(ownerSupport.currentUserName());
        if (!StringUtils.hasText(request.getName())) {
            request.setName(account.getCompanyName() + "服务合同");
        }
        contractMapper.insert(request);
        return request.getId();
    }

    @Transactional
    public void update(Long id, CrmContract request) {
        CrmContract exist = contractMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("合同不存在或无权修改");
        }
        if (!"DRAFT".equals(exist.getStatus())) {
            throw BizException.biz("仅草稿状态可编辑，审批中请先撤回");
        }
        validateDates(request.getStartDate() == null ? exist.getStartDate() : request.getStartDate(),
                request.getEndDate() == null ? exist.getEndDate() : request.getEndDate());
        request.setId(id);
        request.setContractNo(exist.getContractNo());
        request.setStatus(exist.getStatus());
        request.setOwnerUserId(exist.getOwnerUserId());
        request.setOwnerOrgId(exist.getOwnerOrgId());
        request.setOwnerUserName(exist.getOwnerUserName());
        if (request.getStartDate() != null && request.getEndDate() != null) {
            request.setDurationDays((int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1);
        }
        contractMapper.updateById(request);
    }

    @Transactional
    public void delete(Long id) {
        CrmContract exist = contractMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("合同不存在或无权删除");
        }
        if (!"DRAFT".equals(exist.getStatus())) {
            throw BizException.biz("仅草稿状态可删除");
        }
        contractMapper.deleteById(id);
    }

    /** 提交审批：置审批中与回写实例 ID 必须同事务，避免「单据显示审批中但流程不存在」 */
    @Transactional
    public String submit(Long id) {
        CrmContract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw BizException.notFound("合同不存在或无权提交");
        }
        if (!"DRAFT".equals(contract.getStatus())) {
            throw BizException.biz("仅草稿状态可提交审批");
        }
        CrmAccount account = accountMapper.selectById(contract.getAccountId());
        FlowDtos.StartRequest start = new FlowDtos.StartRequest();
        start.setBizType("CRM_CONTRACT");
        start.setBizId(contract.getId());
        start.setBizNo(contract.getContractNo());
        start.setBizTitle(contract.getName() + "（" + contract.getContractNo() + "）");
        start.setDetailRoute("/crm/contract/detail/" + contract.getId());
        BigDecimal amount = contract.getWinAmount() == null ? BigDecimal.ZERO : contract.getWinAmount();
        start.getVariables().put("amount", amount);
        start.getVariables().put("contractNo", contract.getContractNo());
        start.getVariables().put("accountName", account == null ? null : account.getCompanyName());
        start.getVariables().put("businessGroupId", contract.getBusinessGroupId());
        start.getVariables().put("businessGroupName", null);
        start.getVariables().put("contractType", contract.getContractType());
        start.getVariables().put("amountThreshold", crmConfig.amountThreshold());
        String instanceId = flowService.start(start, ownerSupport.current());

        CrmContract update = new CrmContract();
        update.setId(id);
        update.setStatus("APPROVING");
        update.setFlowInstanceId(instanceId);
        contractMapper.updateById(update);
        return instanceId;
    }

    /** 撤回：流程撤回成功后状态回草稿 */
    @Transactional
    public void withdraw(Long id, String reason) {
        CrmContract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw BizException.notFound("合同不存在或无权撤回");
        }
        if (!"APPROVING".equals(contract.getStatus())) {
            throw BizException.biz("仅审批中的合同可撤回");
        }
        if (StringUtils.hasText(contract.getFlowInstanceId())) {
            flowService.withdraw(contract.getFlowInstanceId(), StringUtils.hasText(reason) ? reason : "发起人撤回",
                    ownerSupport.current());
        }
        contractMapper.backToDraft(id);
    }

    /** 归档登记：可多次补录，每次留痕 */
    @Transactional
    public void archive(Long id, String paperBoxNo, Integer paperCopies) {
        CrmContract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw BizException.notFound("合同不存在或无权操作");
        }
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw BizException.biz("仅已激活合同可归档登记");
        }
        if (paperCopies == null || paperCopies < 0) {
            throw BizException.biz("纸质份数必须大于等于 0");
        }
        CrmContract update = new CrmContract();
        update.setId(id);
        update.setIsArchived(1);
        update.setArchiveTime(LocalDateTime.now());
        update.setArchiveUserId(ownerSupport.currentUserId());
        update.setPaperBoxNo(paperBoxNo);
        update.setPaperCopies(paperCopies);
        contractMapper.updateById(update);
    }

    /** 发起续签：生成新草稿，first_contract_id 指向首签 */
    @Transactional
    public Long renew(Long id, CrmContract request) {
        CrmContract origin = contractMapper.selectById(id);
        if (origin == null) {
            throw BizException.notFound("合同不存在或无权续签");
        }
        if ("DRAFT".equals(origin.getStatus()) || "APPROVING".equals(origin.getStatus())) {
            throw BizException.biz("未生效合同不能发起续签");
        }
        if ("TERMINATED".equals(origin.getStatus())) {
            throw BizException.biz("已终止合同不可续签");
        }
        CrmContract renew = new CrmContract();
        renew.setAccountId(origin.getAccountId());
        renew.setCompanyId(origin.getCompanyId());
        renew.setBusinessGroupId(origin.getBusinessGroupId());
        renew.setProductId(origin.getProductId());
        renew.setSourceType(origin.getSourceType());
        renew.setSourceId(origin.getSourceId());
        renew.setWinAmount(request.getWinAmount() == null ? origin.getWinAmount() : request.getWinAmount());
        renew.setContractType("RENEW");
        renew.setFirstContractId(origin.getFirstContractId() == null ? origin.getId() : origin.getFirstContractId());
        renew.setSignDate(request.getSignDate() == null ? LocalDate.now() : request.getSignDate());
        renew.setStartDate(request.getStartDate() == null ? LocalDate.now() : request.getStartDate());
        renew.setEndDate(request.getEndDate() == null
                ? renew.getStartDate().plusYears(1).minusDays(1) : request.getEndDate());
        renew.setName(StringUtils.hasText(request.getName())
                ? request.getName() : origin.getName() + "（续签）");
        renew.setRemark(request.getRemark());
        return create(renew);
    }

    /** 终止：终态，须填原因 */
    @Transactional
    public void terminate(Long id, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw BizException.biz("终止必须填写原因");
        }
        CrmContract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw BizException.notFound("合同不存在或无权操作");
        }
        if ("TERMINATED".equals(contract.getStatus())) {
            return;
        }
        CrmContract update = new CrmContract();
        update.setId(id);
        update.setStatus("TERMINATED");
        update.setTerminateTime(LocalDateTime.now());
        update.setTerminateReason(reason);
        contractMapper.updateById(update);
    }

    /**
     * 审批完成回调：通过 → 激活并赢单反推；驳回/撤回 → 回草稿。
     * 全程幂等，重复回调不产生副作用。
     */
    @Transactional
    public void onFlowFinished(Long id, boolean approved) {
        if (!approved) {
            contractMapper.backToDraft(id);
            return;
        }
        int rows = contractMapper.activate(id);
        if (rows == 0) {
            log.info("合同 {} 激活回调重复到达，已忽略", id);
            return;
        }
        CrmContract contract = contractMapper.selectById(id);
        if (contract == null) {
            return;
        }
        if ("OPPORTUNITY".equals(contract.getSourceType()) && contract.getSourceId() != null) {
            opportunityService.onContractActivated(contract.getSourceId(), contract.getId());
        }
        eventService.track(CrmEventService.CONTRACT_ACTIVATE, id, CrmEventService.props(
                "contractId", id,
                "signCycleDays", contract.getCreatedTime() == null ? null
                        : ChronoUnit.DAYS.between(contract.getCreatedTime(), LocalDateTime.now()),
                "amount", contract.getWinAmount(),
                "businessGroupId", contract.getBusinessGroupId()));
        noticePublisher.publishNotice(contract.getOwnerUserId(), "合同已生效：" + contract.getContractNo(),
                "合同「" + contract.getName() + "」审批通过并已激活。");
    }

    /** 定时任务：到期置 EXPIRED（幂等） */
    @Transactional
    public int expireContracts() {
        List<Long> ids = contractMapper.selectExpiredIds(LocalDate.now(), 500);
        for (Long id : ids) {
            CrmContract update = new CrmContract();
            update.setId(id);
            update.setStatus("EXPIRED");
            contractMapper.updateById(update);
        }
        return ids.size();
    }

    /**
     * 定时任务：到期前提醒负责人。幂等由 NoticePublisher 的
     * (bizType, bizId, receiverId) 去重保证，重复执行不会刷屏。
     */
    @Transactional
    public int remindExpiring() {
        int days = crmConfig.contractExpireNotifyDays();
        LocalDate today = LocalDate.now();
        List<CrmContract> list = contractMapper.selectExpiring(today, today.plusDays(days));
        int sent = 0;
        for (CrmContract contract : list) {
            boolean created = noticePublisher.publishTodo(contract.getOwnerUserId(),
                    "合同即将到期：" + contract.getContractNo(),
                    "合同「" + contract.getName() + "」将于 " + contract.getEndDate() + " 到期，请及时续签或归档。",
                    "CRM_CONTRACT_EXPIRE", contract.getId(), "/crm/contract-expire");
            if (created) {
                sent++;
            }
        }
        return sent;
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw BizException.biz("请填写服务起止日期");
        }
        if (end.isBefore(start)) {
            throw BizException.biz("结束日期不得早于开始日期");
        }
    }
}
