package com.guolian.pm.crm.agreement.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.crm.account.entity.CrmAccount;
import com.guolian.pm.crm.account.mapper.CrmAccountMapper;
import com.guolian.pm.crm.agreement.entity.CrmAgreement;
import com.guolian.pm.crm.agreement.mapper.CrmAgreementMapper;
import com.guolian.pm.crm.common.dto.CrmQuery;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.crm.common.service.NoticePublisher;
import com.guolian.pm.crm.common.service.ProductRefService;
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
 * 协议服务：法院授权类合约，链路比合同短；end_date 可为空表示长期有效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementService {

    private final CrmAgreementMapper agreementMapper;
    private final CrmAccountMapper accountMapper;
    private final OpportunityService opportunityService;
    private final CodeRuleService codeRuleService;
    private final FlowService flowService;
    private final CrmOwnerSupport ownerSupport;
    private final ProductRefService productRefService;
    private final NoticePublisher noticePublisher;

    public PageResult<CrmAgreement> page(CrmQuery q) {
        Page<CrmAgreement> page = new Page<>(q.safePageNum(), q.safePageSize());
        var wrapper = Wrappers.<CrmAgreement>lambdaQuery()
                .eq(StringUtils.hasText(q.getStatus()), CrmAgreement::getStatus, q.getStatus())
                .eq(q.getAccountId() != null, CrmAgreement::getAccountId, q.getAccountId())
                .eq(q.getBusinessGroupId() != null, CrmAgreement::getBusinessGroupId, q.getBusinessGroupId())
                .eq(q.getOwnerUserId() != null, CrmAgreement::getOwnerUserId, q.getOwnerUserId())
                .and(StringUtils.hasText(q.getKeyword()), w -> w
                        .like(CrmAgreement::getAgreementNo, q.getKeyword())
                        .or().like(CrmAgreement::getName, q.getKeyword()))
                .orderByDesc(CrmAgreement::getCreatedTime);
        Page<CrmAgreement> result = agreementMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), q.safePageNum(), q.safePageSize());
    }

    public CrmAgreement get(Long id) {
        CrmAgreement agreement = agreementMapper.selectById(id);
        if (agreement == null) {
            throw BizException.notFound("协议不存在或无权查看");
        }
        return agreement;
    }

    @Transactional
    public Long create(CrmAgreement request) {
        if (request.getAccountId() == null) {
            throw BizException.biz("请选择协议客户");
        }
        if (request.getCompanyId() == null) {
            throw BizException.biz("请选择我方签约主体");
        }
        if (request.getBusinessGroupId() == null) {
            throw BizException.biz("请选择业务线（BG）");
        }
        if (!StringUtils.hasText(request.getAgreementType())) {
            throw BizException.biz("请选择协议类型");
        }
        if (request.getStartDate() == null) {
            throw BizException.biz("请填写生效日期");
        }
        // end_date 允许为空（框架协议长期有效）
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw BizException.biz("结束日期不得早于生效日期");
        }
        CrmAccount account = accountMapper.selectById(request.getAccountId());
        if (account == null) {
            throw BizException.notFound("协议客户不存在或无权维护");
        }
        if (request.getProductId() != null && productRefService.isAvailable()
                && productRefService.getById(request.getProductId()) == null) {
            throw BizException.biz("服务产品不存在或已停用");
        }
        boolean renew = "RENEW".equals(request.getSignType());
        if (renew && request.getFirstAgreementId() == null) {
            throw BizException.biz("续签协议必须关联首签协议");
        }
        request.setId(null);
        request.setAgreementNo(codeRuleService.generate("CRM_AGREEMENT"));
        request.setStatus("DRAFT");
        request.setSignType(renew ? "RENEW" : "FIRST");
        request.setIsArchived(0);
        request.setDurationDays(request.getEndDate() == null ? null
                : (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1);
        request.setOwnerUserId(ownerSupport.currentUserId());
        request.setOwnerOrgId(ownerSupport.createOrgId());
        request.setOwnerUserName(ownerSupport.currentUserName());
        if (!StringUtils.hasText(request.getName())) {
            request.setName(account.getCompanyName() + "调解授权协议");
        }
        agreementMapper.insert(request);
        return request.getId();
    }

    @Transactional
    public void update(Long id, CrmAgreement request) {
        CrmAgreement exist = agreementMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("协议不存在或无权修改");
        }
        if (!"DRAFT".equals(exist.getStatus())) {
            throw BizException.biz("仅草稿状态可编辑，审批中请先撤回");
        }
        request.setId(id);
        request.setAgreementNo(exist.getAgreementNo());
        request.setStatus(exist.getStatus());
        request.setOwnerUserId(exist.getOwnerUserId());
        request.setOwnerOrgId(exist.getOwnerOrgId());
        request.setOwnerUserName(exist.getOwnerUserName());
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw BizException.biz("结束日期不得早于生效日期");
            }
            request.setDurationDays((int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1);
        }
        agreementMapper.updateById(request);
    }

    @Transactional
    public void delete(Long id) {
        CrmAgreement exist = agreementMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("协议不存在或无权删除");
        }
        if (!"DRAFT".equals(exist.getStatus())) {
            throw BizException.biz("仅草稿状态可删除");
        }
        agreementMapper.deleteById(id);
    }

    @Transactional
    public String submit(Long id) {
        CrmAgreement agreement = agreementMapper.selectById(id);
        if (agreement == null) {
            throw BizException.notFound("协议不存在或无权提交");
        }
        if (!"DRAFT".equals(agreement.getStatus())) {
            throw BizException.biz("仅草稿状态可提交审批");
        }
        CrmAccount account = accountMapper.selectById(agreement.getAccountId());
        FlowDtos.StartRequest start = new FlowDtos.StartRequest();
        start.setBizType("CRM_AGREEMENT");
        start.setBizId(agreement.getId());
        start.setBizNo(agreement.getAgreementNo());
        start.setBizTitle(agreement.getName() + "（" + agreement.getAgreementNo() + "）");
        start.setDetailRoute("/crm/agreement/detail/" + agreement.getId());
        start.getVariables().put("amount", agreement.getWinAmount() == null ? BigDecimal.ZERO : agreement.getWinAmount());
        start.getVariables().put("agreementNo", agreement.getAgreementNo());
        start.getVariables().put("accountName", account == null ? null : account.getCompanyName());
        start.getVariables().put("businessGroupId", agreement.getBusinessGroupId());
        String instanceId = flowService.start(start, ownerSupport.current());

        CrmAgreement update = new CrmAgreement();
        update.setId(id);
        update.setStatus("APPROVING");
        update.setFlowInstanceId(instanceId);
        agreementMapper.updateById(update);
        return instanceId;
    }

    @Transactional
    public void withdraw(Long id, String reason) {
        CrmAgreement agreement = agreementMapper.selectById(id);
        if (agreement == null) {
            throw BizException.notFound("协议不存在或无权撤回");
        }
        if (!"APPROVING".equals(agreement.getStatus())) {
            throw BizException.biz("仅审批中的协议可撤回");
        }
        if (StringUtils.hasText(agreement.getFlowInstanceId())) {
            flowService.withdraw(agreement.getFlowInstanceId(),
                    StringUtils.hasText(reason) ? reason : "发起人撤回", ownerSupport.current());
        }
        agreementMapper.backToDraft(id);
    }

    @Transactional
    public void archive(Long id, String paperBoxNo, Integer paperCopies) {
        CrmAgreement agreement = agreementMapper.selectById(id);
        if (agreement == null) {
            throw BizException.notFound("协议不存在或无权操作");
        }
        if (!"EFFECTIVE".equals(agreement.getStatus())) {
            throw BizException.biz("仅已生效协议可归档登记");
        }
        CrmAgreement update = new CrmAgreement();
        update.setId(id);
        update.setIsArchived(1);
        update.setArchiveTime(LocalDateTime.now());
        update.setPaperBoxNo(paperBoxNo);
        update.setPaperCopies(paperCopies);
        agreementMapper.updateById(update);
    }

    /** 续签：生成新草稿 */
    @Transactional
    public Long renew(Long id, CrmAgreement request) {
        CrmAgreement origin = agreementMapper.selectById(id);
        if (origin == null) {
            throw BizException.notFound("协议不存在或无权续签");
        }
        CrmAgreement renew = new CrmAgreement();
        renew.setAccountId(origin.getAccountId());
        renew.setCompanyId(origin.getCompanyId());
        renew.setOpportunityId(origin.getOpportunityId());
        renew.setProductId(origin.getProductId());
        renew.setBusinessGroupId(origin.getBusinessGroupId());
        renew.setAgreementType(origin.getAgreementType());
        renew.setWinAmount(request.getWinAmount() == null ? origin.getWinAmount() : request.getWinAmount());
        renew.setSignType("RENEW");
        renew.setFirstAgreementId(origin.getFirstAgreementId() == null ? origin.getId() : origin.getFirstAgreementId());
        renew.setSignDate(LocalDate.now());
        renew.setStartDate(request.getStartDate() == null ? LocalDate.now() : request.getStartDate());
        renew.setEndDate(request.getEndDate());
        renew.setName(StringUtils.hasText(request.getName())
                ? request.getName() : origin.getName() + "（续签）");
        renew.setRemark(request.getRemark());
        return create(renew);
    }

    /** 审批完成回调：通过 → 生效 + 赢单反推；驳回/撤回 → 回草稿 */
    @Transactional
    public void onFlowFinished(Long id, boolean approved) {
        if (!approved) {
            agreementMapper.backToDraft(id);
            return;
        }
        int rows = agreementMapper.activate(id);
        if (rows == 0) {
            log.info("协议 {} 生效回调重复到达，已忽略", id);
            return;
        }
        CrmAgreement agreement = agreementMapper.selectById(id);
        if (agreement == null) {
            return;
        }
        if (agreement.getOpportunityId() != null) {
            opportunityService.onContractActivated(agreement.getOpportunityId(), null);
        }
        noticePublisher.publishNotice(agreement.getOwnerUserId(), "协议已生效：" + agreement.getAgreementNo(),
                "协议「" + agreement.getName() + "」审批通过并已生效。");
    }

    /** 到期置 EXPIRED：end_date 为空的长期协议必须排除 */
    @Transactional
    public int expireAgreements() {
        List<Long> ids = agreementMapper.selectExpiredIds(LocalDate.now(), 500);
        for (Long id : ids) {
            CrmAgreement update = new CrmAgreement();
            update.setId(id);
            update.setStatus("EXPIRED");
            agreementMapper.updateById(update);
        }
        return ids.size();
    }
}
