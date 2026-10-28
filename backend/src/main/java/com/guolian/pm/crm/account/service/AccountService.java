package com.guolian.pm.crm.account.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.common.web.LoginUser;
import com.guolian.pm.crm.account.entity.CrmAccount;
import com.guolian.pm.crm.account.entity.CrmAccountPoolLog;
import com.guolian.pm.crm.account.entity.CrmClaimApply;
import com.guolian.pm.crm.account.mapper.CrmAccountMapper;
import com.guolian.pm.crm.account.mapper.CrmAccountPoolLogMapper;
import com.guolian.pm.crm.account.mapper.CrmClaimApplyMapper;
import com.guolian.pm.crm.account.vo.CrmClaimVO;
import com.guolian.pm.crm.common.dto.CrmDtos;
import com.guolian.pm.crm.common.dto.CrmQuery;
import com.guolian.pm.crm.common.service.CrmConfig;
import com.guolian.pm.crm.common.service.CrmCryptoService;
import com.guolian.pm.crm.common.service.CrmDupCheckService;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.crm.contract.mapper.CrmContractMapper;
import com.guolian.pm.crm.agreement.mapper.CrmAgreementMapper;
import com.guolian.pm.crm.bid.mapper.CrmBidMapper;
import com.guolian.pm.crm.contact.entity.CrmContact;
import com.guolian.pm.crm.contact.mapper.CrmContactMapper;
import com.guolian.pm.crm.opportunity.mapper.CrmOpportunityMapper;
import com.guolian.pm.flow.dto.FlowDtos;
import com.guolian.pm.flow.service.FlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户服务：主数据 + 私有 / 公共池 / 流失三态 + 认领审批。
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final CrmAccountMapper accountMapper;
    private final CrmAccountPoolLogMapper poolLogMapper;
    private final CrmClaimApplyMapper claimApplyMapper;
    private final CrmContactMapper contactMapper;
    private final CrmOpportunityMapper opportunityMapper;
    private final CrmContractMapper contractMapper;
    private final CrmAgreementMapper agreementMapper;
    private final CrmBidMapper bidMapper;
    private final CrmCryptoService cryptoService;
    private final CrmDupCheckService dupCheckService;
    private final CrmOwnerSupport ownerSupport;
    private final CrmConfig crmConfig;
    private final FlowService flowService;

    // ---------------- 查询 ----------------

    public PageResult<CrmAccount> page(CrmQuery q) {
        Page<CrmAccount> page = new Page<>(q.safePageNum(), q.safePageSize());
        boolean includeProspect = Boolean.TRUE.equals(q.getIncludeProspect());
        var wrapper = Wrappers.<CrmAccount>lambdaQuery()
                .eq(!includeProspect, CrmAccount::getIsProspect, 0)
                .eq(StringUtils.hasText(q.getPoolStatus()), CrmAccount::getPoolStatus, q.getPoolStatus())
                .eq(StringUtils.hasText(q.getAccountType()), CrmAccount::getAccountType, q.getAccountType())
                .eq(StringUtils.hasText(q.getAccountNature()), CrmAccount::getAccountNature, q.getAccountNature())
                .eq(StringUtils.hasText(q.getIndustry()), CrmAccount::getIndustry, q.getIndustry())
                .eq(StringUtils.hasText(q.getProvince()), CrmAccount::getProvince, q.getProvince())
                .eq(q.getOwnerUserId() != null, CrmAccount::getOwnerUserId, q.getOwnerUserId())
                .ge(q.getStartDate() != null, CrmAccount::getCreatedTime,
                        q.getStartDate() == null ? null : q.getStartDate().atStartOfDay())
                .le(q.getEndDate() != null, CrmAccount::getCreatedTime,
                        q.getEndDate() == null ? null : q.getEndDate().atTime(23, 59, 59))
                .like(StringUtils.hasText(q.getKeyword()), CrmAccount::getCompanyName, q.getKeyword())
                .orderByDesc(CrmAccount::getCreatedTime);
        Page<CrmAccount> result = accountMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::decryptInPlace);
        return PageResult.of(result.getRecords(), result.getTotal(), q.safePageNum(), q.safePageSize());
    }

    /** 公共客户池：显式跨范围只读（豁免声明在 Mapper 上，不写在业务 SQL 里） */
    public PageResult<CrmAccount> poolPage(CrmQuery q) {
        LoginUser user = AuthContext.require();
        if (!user.hasPerm("crm:account:pool") && !user.hasPerm("*:*:*")) {
            throw BizException.forbidden("无公共客户池查看权限");
        }
        int offset = (int) ((q.safePageNum() - 1) * q.safePageSize());
        List<CrmAccount> list = accountMapper.selectPoolPage(q.getKeyword(), offset, (int) q.safePageSize());
        long total = accountMapper.countPool(q.getKeyword());
        list.forEach(this::decryptInPlace);
        return PageResult.of(list, total, q.safePageNum(), q.safePageSize());
    }

    public CrmAccount get(Long id) {
        CrmAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw BizException.notFound("客户不存在或无权查看");
        }
        decryptInPlace(account);
        return account;
    }

    /** 详情页五个 Tab 的计数，避免前端逐个接口拉 */
    public Map<String, Object> tabCounts(Long id) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("contacts", contactMapper.selectCount(Wrappers.<CrmContact>lambdaQuery()
                .eq(CrmContact::getAccountId, id)));
        map.put("opportunities", opportunityMapper.countByAccount(id));
        map.put("contracts", contractMapper.selectCount(Wrappers.<com.guolian.pm.crm.contract.entity.CrmContract>lambdaQuery()
                .eq(com.guolian.pm.crm.contract.entity.CrmContract::getAccountId, id)));
        map.put("agreements", agreementMapper.selectCount(Wrappers.<com.guolian.pm.crm.agreement.entity.CrmAgreement>lambdaQuery()
                .eq(com.guolian.pm.crm.agreement.entity.CrmAgreement::getAccountId, id)));
        map.put("bids", bidMapper.selectCount(Wrappers.<com.guolian.pm.crm.bid.entity.CrmBid>lambdaQuery()
                .eq(com.guolian.pm.crm.bid.entity.CrmBid::getAccountId, id)));
        return map;
    }

    /** 删除前置引用检查 */
    public Map<String, Object> relations(Long id) {
        long count = accountMapper.countReferences(id);
        return Map.of("total", count, "deletable", count == 0);
    }

    // ---------------- 写入 ----------------

    @Transactional
    public Long create(CrmAccount request) {
        if (!StringUtils.hasText(request.getCompanyName())) {
            throw BizException.biz("请填写客户全称");
        }
        if (!StringUtils.hasText(request.getIndustry())) {
            throw BizException.biz("请选择所属行业");
        }
        boolean toG = "TO_G".equals(request.getAccountNature());
        if (toG && !StringUtils.hasText(request.getInstitutionCode())) {
            throw BizException.biz("toG 客户必须填写机构编码（法院/机关无统一社会信用代码）");
        }
        if (!toG && StringUtils.hasText(request.getCreditCode())
                && !request.getCreditCode().matches("^[0-9A-Z]{18}$")) {
            throw BizException.biz("统一社会信用代码须为 18 位大写字母或数字");
        }
        CrmDtos.DupCheckResult dup = dupCheckService.check("ACCOUNT", null, null,
                request.getCreditCode(), request.getCompanyName(), request.getProvince());
        if (dup.isDuplicated()) {
            throw BizException.biz("客户已存在：" + dup.getHits().get(0).getName() + "，请直接关联已有客户");
        }
        request.setId(null);
        request.setPoolStatus(StringUtils.hasText(request.getPoolStatus()) ? request.getPoolStatus() : "PRIVATE");
        request.setAccountType(StringUtils.hasText(request.getAccountType()) ? request.getAccountType() : "NORMAL");
        request.setIsProspect(request.getIsProspect() == null ? 0 : request.getIsProspect());
        request.setOwnerUserId(ownerSupport.currentUserId());
        request.setOwnerOrgId(ownerSupport.createOrgId());
        request.setOwnerUserName(ownerSupport.currentUserName());
        request.setPhone(cryptoService.encrypt(request.getPhone()));
        request.setLastActivityTime(LocalDateTime.now());
        validateParent(request.getId(), request.getParentAccountId());
        accountMapper.insert(request);
        return request.getId();
    }

    @Transactional
    public void update(Long id, CrmAccount request) {
        CrmAccount exist = accountMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("客户不存在或无权修改");
        }
        validateParent(id, request.getParentAccountId());
        request.setId(id);
        request.setOwnerUserId(exist.getOwnerUserId());
        request.setOwnerOrgId(exist.getOwnerOrgId());
        request.setOwnerUserName(exist.getOwnerUserName());
        request.setPoolStatus(exist.getPoolStatus());
        if (StringUtils.hasText(request.getPhone())) {
            request.setPhone(cryptoService.encrypt(request.getPhone()));
        }
        accountMapper.updateById(request);
    }

    @Transactional
    public void delete(Long id) {
        CrmAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw BizException.notFound("客户不存在或无权删除");
        }
        long refs = accountMapper.countReferences(id);
        if (refs > 0) {
            throw BizException.biz("该客户已被 " + refs + " 条商机/合同/协议/投标/联系人引用，禁止删除，仅可置流失");
        }
        accountMapper.deleteById(id);
    }

    /** 手动放入公共池 */
    @Transactional
    public void toPool(Long id, String reason) {
        CrmAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw BizException.notFound("客户不存在或无权操作");
        }
        if ("POOL".equals(account.getPoolStatus())) {
            return;
        }
        CrmAccount update = new CrmAccount();
        update.setId(id);
        update.setPoolStatus("POOL");
        update.setPoolInTime(LocalDateTime.now());
        update.setPoolReason(StringUtils.hasText(reason) ? reason : "手动放入");
        accountMapper.updateById(update);
        writePoolLog(id, account.getPoolStatus(), "POOL", "MANUAL", update.getPoolReason());
    }

    /** 置流失（须填原因） */
    @Transactional
    public void churn(Long id, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw BizException.biz("置流失必须填写原因");
        }
        CrmAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw BizException.notFound("客户不存在或无权操作");
        }
        CrmAccount update = new CrmAccount();
        update.setId(id);
        update.setPoolStatus("CHURNED");
        update.setChurnReason(reason);
        accountMapper.updateById(update);
        writePoolLog(id, account.getPoolStatus(), "CHURNED", "CHURN", reason);
    }

    /** 流失恢复为私有 */
    @Transactional
    public void revive(Long id, String reason) {
        CrmAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw BizException.notFound("客户不存在或无权操作");
        }
        CrmAccount update = new CrmAccount();
        update.setId(id);
        update.setPoolStatus("PRIVATE");
        update.setChurnReason(null);
        accountMapper.updateById(update);
        writePoolLog(id, account.getPoolStatus(), "PRIVATE", "MANUAL",
                StringUtils.hasText(reason) ? reason : "流失恢复");
    }

    /** 转移负责人：负责人与归属组织同步变更 */
    @Transactional
    public void changeOwner(Long id, Long toUserId) {
        CrmAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw BizException.notFound("客户不存在或无权转移");
        }
        ownerSupport.requireActiveUser(toUserId);
        Long toOrgId = ownerSupport.orgOf(toUserId);
        if (toOrgId == null) {
            throw BizException.biz("接收人未绑定角色组织，无法转移");
        }
        CrmAccount update = new CrmAccount();
        update.setId(id);
        update.setOwnerUserId(toUserId);
        update.setOwnerOrgId(toOrgId);
        update.setOwnerUserName(ownerSupport.nameOf(toUserId));
        accountMapper.updateById(update);
    }

    /**
     * 认领申请分页（审批视角）。crm_claim_apply 不在行级权限注册表内：
     * 审批人需要看到跨组织的申请，因此这里按权限位 crm:account:claim 把关。
     */
    public PageResult<CrmClaimVO> pageClaims(String status, String keyword, long pageNum, long pageSize) {
        long pn = pageNum < 1 ? 1 : pageNum;
        long ps = pageSize < 1 ? 20 : Math.min(pageSize, 100);
        long total = claimApplyMapper.countClaim(status, keyword);
        if (total == 0) {
            return PageResult.empty(pn, ps);
        }
        List<CrmClaimVO> list = claimApplyMapper.selectClaimPage(status, keyword,
                (int) ((pn - 1) * ps), (int) ps);
        return PageResult.of(list, total, pn, ps);
    }

    /**
     * 定时任务：申诉期到期关闭。审批通过后原负责人可在 appeal_deadline 前申诉，
     * 超时未申诉则把申诉状态置 EXPIRED（幂等：只扫 NONE 状态）。
     */
    @Transactional
    public int processAppealTimeout() {
        List<CrmClaimApply> list = claimApplyMapper.selectList(Wrappers.<CrmClaimApply>lambdaQuery()
                .eq(CrmClaimApply::getStatus, "APPROVED")
                .eq(CrmClaimApply::getAppealStatus, "NONE")
                .lt(CrmClaimApply::getAppealDeadline, LocalDateTime.now())
                .last("limit 500"));
        for (CrmClaimApply apply : list) {
            CrmClaimApply update = new CrmClaimApply();
            update.setId(apply.getId());
            update.setAppealStatus("EXPIRED");
            claimApplyMapper.updateById(update);
        }
        return list.size();
    }

    /** 待审批数量：列表页顶部角标 */
    public long countPendingClaims() {
        return claimApplyMapper.countApproving();
    }

    /**
     * 发起认领：生成认领单并走 CRM_CLAIM 审批，审批通过后才改负责人与池状态。
     */
    @Transactional
    public Long claim(Long id, String reason) {
        CrmAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw BizException.notFound("客户不存在或无权认领");
        }
        if (!"POOL".equals(account.getPoolStatus())) {
            throw BizException.biz("仅公共池客户可认领");
        }
        long exists = claimApplyMapper.selectCount(Wrappers.<CrmClaimApply>lambdaQuery()
                .eq(CrmClaimApply::getAccountId, id)
                .eq(CrmClaimApply::getApplicantId, ownerSupport.currentUserId())
                .eq(CrmClaimApply::getStatus, "APPROVING"));
        if (exists > 0) {
            throw BizException.biz("你已提交过该客户的认领申请，请等待审批");
        }
        CrmClaimApply apply = new CrmClaimApply();
        apply.setAccountId(id);
        apply.setApplicantId(ownerSupport.currentUserId());
        apply.setApplicantOrgId(ownerSupport.createOrgId());
        apply.setOldOwnerId(account.getOwnerUserId());
        apply.setStatus("APPROVING");
        apply.setAppealDeadline(LocalDateTime.now().plusHours(crmConfig.claimAppealHours()));
        apply.setAppealStatus("NONE");
        apply.setApplyReason(reason);
        claimApplyMapper.insert(apply);

        FlowDtos.StartRequest start = new FlowDtos.StartRequest();
        start.setBizType("CRM_CLAIM");
        start.setBizId(apply.getId());
        start.setBizNo("RL-" + apply.getId());
        start.setBizTitle("公共池认领：" + account.getCompanyName());
        start.setDetailRoute("/crm/pool");
        start.getVariables().put("accountId", id);
        start.getVariables().put("applicantId", ownerSupport.currentUserId());
        start.getVariables().put("oldOwnerId", account.getOwnerUserId());
        start.getVariables().put("subjectUserId", account.getOwnerUserId());
        String instanceId = flowService.start(start, ownerSupport.current());
        CrmClaimApply update = new CrmClaimApply();
        update.setId(apply.getId());
        update.setFlowInstanceId(instanceId);
        claimApplyMapper.updateById(update);
        return apply.getId();
    }

    /** 审批通过后执行认领（由审批回调调用，事务内完成改负责人 + 改池状态 + 写日志） */
    @Transactional
    public void applyClaimApproved(Long applyId) {
        CrmClaimApply apply = claimApplyMapper.selectById(applyId);
        if (apply == null || !"APPROVING".equals(apply.getStatus())) {
            return;
        }
        CrmAccount account = accountMapper.selectById(apply.getAccountId());
        if (account == null) {
            return;
        }
        Long toOrgId = ownerSupport.orgOf(apply.getApplicantId());
        CrmAccount update = new CrmAccount();
        update.setId(account.getId());
        update.setOwnerUserId(apply.getApplicantId());
        update.setOwnerOrgId(toOrgId == null ? account.getOwnerOrgId() : toOrgId);
        update.setOwnerUserName(ownerSupport.nameOf(apply.getApplicantId()));
        update.setPoolStatus("PRIVATE");
        update.setPoolInTime(null);
        update.setPoolReason(null);
        accountMapper.updateById(update);

        CrmClaimApply applyUpdate = new CrmClaimApply();
        applyUpdate.setId(applyId);
        applyUpdate.setStatus("APPROVED");
        claimApplyMapper.updateById(applyUpdate);
        writePoolLog(account.getId(), "POOL", "PRIVATE", "CLAIM", "认领审批通过");
    }

    /** 审批驳回/撤回后回滚认领单状态 */
    @Transactional
    public void applyClaimRejected(Long applyId, boolean withdrawn) {
        CrmClaimApply apply = claimApplyMapper.selectById(applyId);
        if (apply == null || !"APPROVING".equals(apply.getStatus())) {
            return;
        }
        CrmClaimApply update = new CrmClaimApply();
        update.setId(applyId);
        update.setStatus(withdrawn ? "WITHDRAWN" : "REJECTED");
        claimApplyMapper.updateById(update);
    }

    // ---------------- 内部 ----------------

    private void validateParent(Long selfId, Long parentId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw BizException.biz("父客户不能指向自身");
        }
        CrmAccount parent = accountMapper.selectById(parentId);
        if (parent == null) {
            throw BizException.notFound("父客户不存在");
        }
        if (parent.getParentAccountId() != null) {
            throw BizException.biz("子客户不得再设子客户（仅支持一层）");
        }
    }

    private void decryptInPlace(CrmAccount account) {
        account.setPhone(cryptoService.decrypt(account.getPhone()));
    }

    private void writePoolLog(Long accountId, String from, String to, String trigger, String reason) {
        CrmAccountPoolLog log = new CrmAccountPoolLog();
        log.setAccountId(accountId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setTriggerType(trigger);
        log.setReason(reason);
        LoginUser user = AuthContext.get();
        if (user != null) {
            log.setOperatorId(user.getUserId());
            log.setOperatorOrgId(user.getOrgId());
        }
        poolLogMapper.insert(log);
    }

    /** 定时任务：超期回收进公共池（幂等，重复执行结果一致） */
    @Transactional
    public int autoRecycle() {
        LocalDateTime before = LocalDateTime.now().minusDays(crmConfig.poolTimeoutDays());
        List<Long> ids = accountMapper.selectTimeoutIds(before, 500);
        for (Long id : ids) {
            CrmAccount account = accountMapper.selectById(id);
            if (account == null || !"PRIVATE".equals(account.getPoolStatus())) {
                continue;
            }
            CrmAccount update = new CrmAccount();
            update.setId(id);
            update.setPoolStatus("POOL");
            update.setPoolInTime(LocalDateTime.now());
            update.setPoolReason("超期无活动自动回收");
            accountMapper.updateById(update);
            writePoolLog(id, "PRIVATE", "POOL", "AUTO_TIMEOUT", "超期无活动自动回收");
        }
        return ids.size();
    }
}
