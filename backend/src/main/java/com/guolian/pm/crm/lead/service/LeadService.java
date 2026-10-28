package com.guolian.pm.crm.lead.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.crm.account.entity.CrmAccount;
import com.guolian.pm.crm.account.mapper.CrmAccountMapper;
import com.guolian.pm.crm.common.dto.CrmDtos;
import com.guolian.pm.crm.common.dto.CrmQuery;
import com.guolian.pm.crm.common.service.CrmConfig;
import com.guolian.pm.crm.common.service.CrmCryptoService;
import com.guolian.pm.crm.common.service.CrmDupCheckService;
import com.guolian.pm.crm.common.service.CrmEventService;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.crm.contact.entity.CrmContact;
import com.guolian.pm.crm.contact.mapper.CrmContactMapper;
import com.guolian.pm.crm.lead.entity.CrmLead;
import com.guolian.pm.crm.lead.entity.CrmLeadConvertLog;
import com.guolian.pm.crm.lead.mapper.CrmLeadConvertLogMapper;
import com.guolian.pm.crm.lead.mapper.CrmLeadMapper;
import com.guolian.pm.crm.opportunity.entity.CrmOpportunity;
import com.guolian.pm.crm.opportunity.mapper.CrmOpportunityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 线索服务。核心难点是「转化」：一个事务内派生客户 + 联系人 + 商机并回写三 ID，
 * 通过「先查转化标记短路 + uk_convert_lead 唯一索引兜底」实现幂等。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final CrmLeadMapper leadMapper;
    private final CrmLeadConvertLogMapper convertLogMapper;
    private final CrmAccountMapper accountMapper;
    private final CrmContactMapper contactMapper;
    private final CrmOpportunityMapper opportunityMapper;
    private final CrmCryptoService cryptoService;
    private final CrmDupCheckService dupCheckService;
    private final CrmOwnerSupport ownerSupport;
    private final CrmEventService eventService;
    private final CrmConfig crmConfig;

    // ---------------- 查询 ----------------

    public PageResult<CrmLead> page(CrmQuery q) {
        Page<CrmLead> page = new Page<>(q.safePageNum(), q.safePageSize());
        var wrapper = Wrappers.<CrmLead>lambdaQuery()
                .eq(StringUtils.hasText(q.getStatus()), CrmLead::getStatus, q.getStatus())
                .eq(StringUtils.hasText(q.getGrade()), CrmLead::getGrade, q.getGrade())
                .eq(StringUtils.hasText(q.getChannel()), CrmLead::getSourceChannel, q.getChannel())
                .eq(q.getOwnerUserId() != null, CrmLead::getOwnerUserId, q.getOwnerUserId())
                .ge(q.getStartDate() != null, CrmLead::getCreatedTime,
                        q.getStartDate() == null ? null : q.getStartDate().atStartOfDay())
                .le(q.getEndDate() != null, CrmLead::getCreatedTime,
                        q.getEndDate() == null ? null : q.getEndDate().atTime(23, 59, 59))
                .and(StringUtils.hasText(q.getKeyword()), w -> w
                        .like(CrmLead::getName, q.getKeyword())
                        .or().like(CrmLead::getCompanyName, q.getKeyword()))
                .orderByDesc(CrmLead::getCreatedTime);
        Page<CrmLead> result = leadMapper.selectPage(page, wrapper);
        // 手机号在库内是密文，出参统一解密（展示层再由 @Sensitive 按角色脱敏）
        result.getRecords().forEach(this::decryptInPlace);
        return PageResult.of(result.getRecords(), result.getTotal(), q.safePageNum(), q.safePageSize());
    }

    public CrmLead get(Long id) {
        CrmLead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw BizException.notFound("线索不存在或无权查看");
        }
        decryptInPlace(lead);
        return lead;
    }

    private void decryptInPlace(CrmLead lead) {
        lead.setMobile(cryptoService.decrypt(lead.getMobile()));
        lead.setEmail(cryptoService.decrypt(lead.getEmail()));
        // 哈希不出接口
        lead.setMobileHash(null);
        lead.setEmailHash(null);
    }

    // ---------------- 写入 ----------------

    @Transactional
    public Long create(CrmLead request) {
        validate(request);
        CrmDtos.DupCheckResult dup = dupCheckService.check("LEAD", request.getMobile(),
                request.getEmail(), null, request.getCompanyName(), null);
        if (dup.isDuplicated() && !StringUtils.hasText(request.getDupReason())) {
            throw BizException.biz(buildDupMessage(dup) + "；如仍需建线，请填写重复原因后强制继续");
        }
        request.setId(null);
        request.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "NEW");
        request.setGrade(StringUtils.hasText(request.getGrade()) ? request.getGrade() : "C");
        request.setOwnerUserId(ownerSupport.currentUserId());
        request.setOwnerOrgId(ownerSupport.createOrgId());
        request.setOwnerUserName(ownerSupport.currentUserName());
        request.setMobileHash(cryptoService.hash(request.getMobile()));
        request.setEmailHash(cryptoService.hash(request.getEmail()));
        request.setMobile(cryptoService.encrypt(request.getMobile()));
        request.setEmail(cryptoService.encrypt(request.getEmail()));
        request.setConvertedAccountId(null);
        request.setConvertedContactId(null);
        request.setConvertedOpportunityId(null);
        request.setConvertedTime(null);
        leadMapper.insert(request);
        eventService.track(CrmEventService.LEAD_CREATE, request.getId(), CrmEventService.props(
                "channel", request.getSourceChannel(), "grade", request.getGrade(),
                "ownerOrgId", request.getOwnerOrgId(), "accountNature", request.getAccountNature()));
        return request.getId();
    }

    @Transactional
    public void update(Long id, CrmLead request) {
        CrmLead exist = leadMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("线索不存在或无权修改");
        }
        if ("CONVERTED".equals(exist.getStatus())) {
            throw BizException.biz("已转化线索全部字段只读");
        }
        if ("INVALID".equals(exist.getStatus())) {
            throw BizException.biz("已置无效线索不可编辑");
        }
        request.setId(id);
        // 归属与转化标记不接受前端传参
        request.setOwnerUserId(exist.getOwnerUserId());
        request.setOwnerOrgId(exist.getOwnerOrgId());
        request.setOwnerUserName(exist.getOwnerUserName());
        request.setConvertedAccountId(exist.getConvertedAccountId());
        request.setConvertedContactId(exist.getConvertedContactId());
        request.setConvertedOpportunityId(exist.getConvertedOpportunityId());
        if (StringUtils.hasText(request.getMobile())) {
            request.setMobileHash(cryptoService.hash(request.getMobile()));
            request.setMobile(cryptoService.encrypt(request.getMobile()));
        }
        if (StringUtils.hasText(request.getEmail())) {
            request.setEmailHash(cryptoService.hash(request.getEmail()));
            request.setEmail(cryptoService.encrypt(request.getEmail()));
        }
        leadMapper.updateById(request);
    }

    @Transactional
    public void delete(Long id) {
        CrmLead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw BizException.notFound("线索不存在或无权删除");
        }
        if (lead.getConvertedAccountId() != null) {
            throw BizException.biz("已转化线索不可删除，请改用置无效");
        }
        leadMapper.deleteById(id);
    }

    @Transactional
    public void invalid(Long id, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw BizException.biz("置无效必须填写原因");
        }
        CrmLead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw BizException.notFound("线索不存在或无权修改");
        }
        if ("CONVERTED".equals(lead.getStatus())) {
            throw BizException.biz("已转化线索不可置为无效");
        }
        CrmLead update = new CrmLead();
        update.setId(id);
        update.setStatus("INVALID");
        update.setInvalidReason(reason);
        leadMapper.updateById(update);
    }

    @Transactional
    public void changeOwner(Long id, Long toUserId) {
        CrmLead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw BizException.notFound("线索不存在或无权转移");
        }
        ownerSupport.requireActiveUser(toUserId);
        Long toOrgId = ownerSupport.orgOf(toUserId);
        if (toOrgId == null) {
            throw BizException.biz("接收人未绑定角色组织，无法转移");
        }
        CrmLead update = new CrmLead();
        update.setId(id);
        // 负责人与归属组织必须同一次 update 一起改
        update.setOwnerUserId(toUserId);
        update.setOwnerOrgId(toOrgId);
        update.setOwnerUserName(ownerSupport.nameOf(toUserId));
        leadMapper.updateById(update);
    }

    /**
     * 线索转化：客户 + 联系人 + 商机在一个事务内派生并回写线索三 ID。
     * 幂等：已有任一转化 ID 直接返回既有结果；唯一索引冲突同样返回既有结果。
     */
    @Transactional
    public CrmDtos.ConvertResultHolder convert(Long id, CrmDtos.ConvertRequest request) {
        CrmLead lead = leadMapper.selectForUpdate(id);
        if (lead == null) {
            throw BizException.notFound("线索不存在或无权转化");
        }
        if (lead.getConvertedAccountId() != null || lead.getConvertedContactId() != null
                || lead.getConvertedOpportunityId() != null) {
            return new CrmDtos.ConvertResultHolder(true, lead.getConvertedAccountId(),
                    lead.getConvertedContactId(), lead.getConvertedOpportunityId());
        }
        if (!"FOLLOWING".equals(lead.getStatus())) {
            throw BizException.biz("当前状态为「" + lead.getStatus() + "」，仅跟进中的线索允许转化");
        }
        String mode = StringUtils.hasText(request.getConvertMode()) ? request.getConvertMode() : "NEW_ACCOUNT";
        Long accountId;
        if ("EXIST_ACCOUNT".equals(mode)) {
            if (request.getAccountId() == null) {
                throw BizException.biz("关联已有客户时必须选择客户");
            }
            CrmAccount account = accountMapper.selectById(request.getAccountId());
            if (account == null) {
                throw BizException.notFound("目标客户不存在或无权关联");
            }
            accountId = account.getId();
        } else {
            CrmAccount account = new CrmAccount();
            account.setCompanyName(lead.getCompanyName());
            account.setIndustry(StringUtils.hasText(lead.getIndustry()) ? lead.getIndustry() : "OTHER");
            account.setAccountNature(StringUtils.hasText(lead.getAccountNature()) ? lead.getAccountNature() : "TO_B");
            account.setAccountType("NORMAL");
            account.setPoolStatus("PRIVATE");
            account.setIsProspect(0);
            // 归属取线索负责人，不是操作人
            account.setOwnerUserId(lead.getOwnerUserId());
            account.setOwnerOrgId(lead.getOwnerOrgId());
            account.setOwnerUserName(lead.getOwnerUserName());
            account.setPhone(cryptoService.encrypt(cryptoService.decrypt(lead.getMobile())));
            account.setLastActivityTime(LocalDateTime.now());
            accountMapper.insert(account);
            accountId = account.getId();
        }

        CrmContact contact = new CrmContact();
        contact.setName(lead.getName());
        contact.setAccountId(accountId);
        contact.setMobile(cryptoService.encrypt(cryptoService.decrypt(lead.getMobile())));
        contact.setMobileHash(lead.getMobileHash());
        contact.setEmail(lead.getEmail());
        contact.setEmailHash(lead.getEmailHash());
        contact.setPositionName(lead.getPositionName());
        contact.setOwnerUserId(lead.getOwnerUserId());
        contact.setOwnerOrgId(lead.getOwnerOrgId());
        contact.setOwnerUserName(lead.getOwnerUserName());
        contactMapper.insert(contact);

        CrmOpportunity opportunity = new CrmOpportunity();
        String name = StringUtils.hasText(request.getOpportunityName())
                ? request.getOpportunityName()
                : lead.getCompanyName() + "商机";
        opportunity.setName(name);
        opportunity.setAccountId(accountId);
        opportunity.setProductId(request.getProductId());
        opportunity.setSalesStage("PROSPECT");
        opportunity.setCurrency("CNY");
        opportunity.setPricingMode("LUMP_SUM");
        opportunity.setPricingDetail("{\"mode\":\"LUMP_SUM\"}");
        opportunity.setExpectedAmount(request.getExpectedAmount());
        opportunity.setEstRevenue(request.getExpectedAmount());
        opportunity.setSourceLeadId(lead.getId());
        opportunity.setIsWon(0);
        opportunity.setOwnerUserId(lead.getOwnerUserId());
        opportunity.setOwnerOrgId(lead.getOwnerOrgId());
        opportunity.setOwnerUserName(lead.getOwnerUserName());
        opportunity.setStageChangeTime(LocalDateTime.now());
        opportunityMapper.insert(opportunity);

        CrmLead update = new CrmLead();
        update.setId(lead.getId());
        update.setStatus("CONVERTED");
        update.setConvertedAccountId(accountId);
        update.setConvertedContactId(contact.getId());
        update.setConvertedOpportunityId(opportunity.getId());
        update.setConvertedTime(LocalDateTime.now());
        leadMapper.updateById(update);

        try {
            CrmLeadConvertLog convertLog = new CrmLeadConvertLog();
            convertLog.setLeadId(lead.getId());
            convertLog.setAccountId(accountId);
            convertLog.setContactId(contact.getId());
            convertLog.setOpportunityId(opportunity.getId());
            convertLog.setConvertMode(mode);
            convertLog.setOperatorId(ownerSupport.currentUserId());
            convertLog.setOperatorOrgId(ownerSupport.currentOrgId());
            convertLogMapper.insert(convertLog);
        } catch (DuplicateKeyException e) {
            // 并发重复转化：唯一索引兜底，事务回滚后由调用方重试读取既有结果
            log.warn("线索 {} 并发重复转化，唯一索引拦截", lead.getId());
            throw BizException.biz("该线索已被转化，请刷新后查看");
        }

        eventService.track(CrmEventService.LEAD_CONVERT, lead.getId(), CrmEventService.props(
                "leadId", lead.getId(),
                "durationHours", lead.getCreatedTime() == null ? null
                        : java.time.Duration.between(lead.getCreatedTime(), LocalDateTime.now()).toHours(),
                "createdObjects", List.of(accountId, contact.getId(), opportunity.getId())));
        return new CrmDtos.ConvertResultHolder(false, accountId, contact.getId(), opportunity.getId());
    }

    // ---------------- 内部 ----------------

    private void validate(CrmLead lead) {
        if (!StringUtils.hasText(lead.getName())) {
            throw BizException.biz("请填写联系人姓名");
        }
        if (!StringUtils.hasText(lead.getCompanyName())) {
            throw BizException.biz("请填写公司名称");
        }
        if (!StringUtils.hasText(lead.getMobile()) || !lead.getMobile().matches("^1\\d{10}$")) {
            throw BizException.biz("手机号格式不正确（需 11 位）");
        }
        if (!StringUtils.hasText(lead.getSourceChannel())) {
            throw BizException.biz("请选择来源渠道");
        }
    }

    private String buildDupMessage(CrmDtos.DupCheckResult dup) {
        StringBuilder sb = new StringBuilder("已存在相同联系方式：");
        for (CrmDtos.DupHit hit : dup.getHits()) {
            sb.append(hit.getType()).append("「").append(hit.getName()).append("」");
            if (hit.getCompanyName() != null) {
                sb.append("（").append(hit.getCompanyName()).append("）");
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    /** 供线索导入使用：跳过查重直接落库（导入侧先统一查重再批量落库） */
    @Transactional
    public Long createFromImport(CrmLead request, Long ownerUserId, Long ownerOrgId, String ownerName) {
        request.setId(null);
        request.setStatus("NEW");
        request.setGrade(StringUtils.hasText(request.getGrade()) ? request.getGrade() : "C");
        request.setOwnerUserId(ownerUserId);
        request.setOwnerOrgId(ownerOrgId);
        request.setOwnerUserName(ownerName);
        request.setMobileHash(cryptoService.hash(request.getMobile()));
        request.setEmailHash(cryptoService.hash(request.getEmail()));
        request.setMobile(cryptoService.encrypt(request.getMobile()));
        request.setEmail(cryptoService.encrypt(request.getEmail()));
        leadMapper.insert(request);
        return request.getId();
    }

    /** 报表用：按日期区间统计新增线索数（走数据权限） */
    public long countCreated(LocalDate from, LocalDate to) {
        return leadMapper.selectCount(Wrappers.<CrmLead>lambdaQuery()
                .ge(from != null, CrmLead::getCreatedTime, from == null ? null : from.atStartOfDay())
                .le(to != null, CrmLead::getCreatedTime, to == null ? null : to.atTime(23, 59, 59)));
    }

    /**
     * 定时任务：超过 N 天未跟进的线索置为无效。
     * 幂等：只处理状态仍为 NEW 的记录，重复执行不会重复改写。
     *
     * <p>注意这里显式忽略数据权限——定时任务以系统身份跑，不做「按人归属」过滤，
     * 否则超管之外的组织会漏扫。</p>
     */
    @Transactional
    public int recycleTimeoutLeads() {
        int days = crmConfig.leadPoolTimeoutDays();
        LocalDateTime before = LocalDateTime.now().minusDays(days);
        List<CrmLead> targets = leadMapper.selectList(Wrappers.<CrmLead>lambdaQuery()
                .eq(CrmLead::getStatus, "NEW")
                .and(w -> w.isNull(CrmLead::getLastFollowTime)
                        .lt(CrmLead::getCreatedTime, before)
                        .or()
                        .lt(CrmLead::getLastFollowTime, before))
                .last("limit 500"));
        int count = 0;
        for (CrmLead lead : targets) {
            CrmLead update = new CrmLead();
            update.setId(lead.getId());
            update.setStatus("INVALID");
            update.setInvalidReason("超过 " + days + " 天未跟进，系统自动置无效");
            leadMapper.updateById(update);
            count++;
        }
        if (count > 0) {
            log.info("线索超期回收完成，共 {} 条", count);
        }
        return count;
    }
}
