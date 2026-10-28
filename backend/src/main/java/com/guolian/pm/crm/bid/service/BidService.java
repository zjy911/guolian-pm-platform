package com.guolian.pm.crm.bid.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.crm.account.entity.CrmAccount;
import com.guolian.pm.crm.account.mapper.CrmAccountMapper;
import com.guolian.pm.crm.bid.entity.CrmBid;
import com.guolian.pm.crm.bid.mapper.CrmBidMapper;
import com.guolian.pm.crm.common.dto.CrmQuery;
import com.guolian.pm.crm.common.service.CrmConfig;
import com.guolian.pm.crm.common.service.CrmEventService;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.crm.common.service.NoticePublisher;
import com.guolian.pm.crm.contract.entity.CrmContract;
import com.guolian.pm.crm.contract.service.ContractService;
import com.guolian.pm.crm.opportunity.entity.CrmOpportunity;
import com.guolian.pm.crm.opportunity.mapper.CrmOpportunityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * 投标服务：替代商务部线下 Excel，状态机 + 中标联动（一键生成商机/合同，幂等）。
 */
@Service
@RequiredArgsConstructor
public class BidService {

    private static final List<String> FLOW = List.of("TRACKING", "REGISTERED", "SUBMITTED", "WON", "LOST", "ABANDONED");

    private final CrmBidMapper bidMapper;
    private final CrmAccountMapper accountMapper;
    private final CrmOpportunityMapper opportunityMapper;
    private final ContractService contractService;
    private final CrmOwnerSupport ownerSupport;
    private final CrmEventService eventService;
    private final CrmConfig crmConfig;
    private final NoticePublisher noticePublisher;

    public PageResult<CrmBid> page(CrmQuery q) {
        Page<CrmBid> page = new Page<>(q.safePageNum(), q.safePageSize());
        var wrapper = Wrappers.<CrmBid>lambdaQuery()
                .eq(StringUtils.hasText(q.getStatus()), CrmBid::getBidStatus, q.getStatus())
                .eq(StringUtils.hasText(q.getTenderForm()), CrmBid::getTenderForm, q.getTenderForm())
                .eq(q.getRegionOrgId() != null, CrmBid::getRegionOrgId, q.getRegionOrgId())
                .eq(q.getAccountId() != null, CrmBid::getAccountId, q.getAccountId())
                .eq(q.getOwnerUserId() != null, CrmBid::getOwnerUserId, q.getOwnerUserId())
                .ge(q.getStartDate() != null, CrmBid::getOpenDate, q.getStartDate())
                .le(q.getEndDate() != null, CrmBid::getOpenDate, q.getEndDate())
                .and(StringUtils.hasText(q.getKeyword()), w -> w
                        .like(CrmBid::getTenderName, q.getKeyword())
                        .or().like(CrmBid::getTenderNo, q.getKeyword()))
                .orderByDesc(CrmBid::getOpenDate);
        Page<CrmBid> result = bidMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), q.safePageNum(), q.safePageSize());
    }

    /** 开标日历：按月取 */
    public List<CrmBid> calendar(String month) {
        YearMonth ym = StringUtils.hasText(month) ? YearMonth.parse(month) : YearMonth.now();
        return bidMapper.selectCalendar(ym.atDay(1), ym.atEndOfMonth());
    }

    public CrmBid get(Long id) {
        CrmBid bid = bidMapper.selectById(id);
        if (bid == null) {
            throw BizException.notFound("投标记录不存在或无权查看");
        }
        return bid;
    }

    @Transactional
    public Long create(CrmBid request) {
        if (request.getAccountId() == null) {
            throw BizException.biz("请选择招标人（客户库无此人时先建潜客）");
        }
        if (!StringUtils.hasText(request.getTenderName())) {
            throw BizException.biz("请填写招标项目名称");
        }
        if (!StringUtils.hasText(request.getTenderForm())) {
            throw BizException.biz("请选择招标形式");
        }
        if (request.getBidEntity() == null) {
            throw BizException.biz("请选择我方投标主体");
        }
        if (request.getOpenDate() == null || request.getDeadline() == null) {
            throw BizException.biz("请填写开标日期与投标截止日期");
        }
        if (request.getDeadline().isAfter(request.getOpenDate())) {
            throw BizException.biz("投标截止日期不得晚于开标日期");
        }
        CrmAccount account = accountMapper.selectById(request.getAccountId());
        if (account == null) {
            throw BizException.notFound("招标人不存在或无权维护");
        }
        if (request.getDeposit() != null && !StringUtils.hasText(request.getDepositStatus())) {
            throw BizException.biz("填写保证金后必须选择保证金状态");
        }
        request.setId(null);
        request.setBidStatus(StringUtils.hasText(request.getBidStatus()) ? request.getBidStatus() : "TRACKING");
        request.setIsWon(0);
        request.setOwnerUserId(ownerSupport.currentUserId());
        request.setOwnerOrgId(ownerSupport.createOrgId());
        request.setOwnerUserName(ownerSupport.currentUserName());
        if (request.getRegionOrgId() == null) {
            request.setRegionOrgId(ownerSupport.createOrgId());
        }
        bidMapper.insert(request);
        return request.getId();
    }

    @Transactional
    public void update(Long id, CrmBid request) {
        CrmBid exist = bidMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("投标记录不存在或无权修改");
        }
        request.setId(id);
        request.setBidStatus(exist.getBidStatus());
        request.setIsWon(exist.getIsWon());
        request.setOwnerUserId(exist.getOwnerUserId());
        request.setOwnerOrgId(exist.getOwnerOrgId());
        request.setOwnerUserName(exist.getOwnerUserName());
        request.setGenOpportunityId(exist.getGenOpportunityId());
        request.setGenContractId(exist.getGenContractId());
        bidMapper.updateById(request);
    }

    @Transactional
    public void delete(Long id) {
        bidMapper.deleteById(id);
    }

    /** 状态推进：置 WON 必须填中标金额；LOST/ABANDONED 必须填原因 */
    @Transactional
    public void changeStatus(Long id, String targetStatus, BigDecimal winAmount, String reason) {
        CrmBid bid = bidMapper.selectById(id);
        if (bid == null) {
            throw BizException.notFound("投标记录不存在或无权操作");
        }
        if (!FLOW.contains(targetStatus)) {
            throw BizException.biz("非法的投标状态：" + targetStatus);
        }
        if (targetStatus.equals(bid.getBidStatus())) {
            return;
        }
        CrmBid update = new CrmBid();
        update.setId(id);
        update.setBidStatus(targetStatus);
        switch (targetStatus) {
            case "WON" -> {
                BigDecimal amount = winAmount == null ? bid.getWinAmount() : winAmount;
                if (amount == null) {
                    throw BizException.biz("置为已中标必须填写中标金额（万元）");
                }
                update.setWinAmount(amount);
                update.setIsWon(1);
            }
            case "LOST" -> {
                if (!StringUtils.hasText(reason)) {
                    throw BizException.biz("未中标必须填写原因");
                }
                update.setLostReason(reason);
                update.setIsWon(0);
            }
            case "ABANDONED" -> {
                if (!StringUtils.hasText(reason)) {
                    throw BizException.biz("放弃投标必须填写原因");
                }
                update.setAbandonReason(reason);
                update.setIsWon(0);
            }
            default -> update.setIsWon(0);
        }
        bidMapper.updateById(update);
        eventService.track(CrmEventService.BID_STATUS_CHANGE, id, CrmEventService.props(
                "fromStatus", bid.getBidStatus(), "toStatus", targetStatus,
                "winAmount", update.getWinAmount(), "tenderForm", bid.getTenderForm()));
    }

    /** 中标后一键生成商机（谈判阶段），已生成过则提示跳转，不重复创建 */
    @Transactional
    public Long genOpportunity(Long id) {
        CrmBid bid = bidMapper.selectById(id);
        if (bid == null) {
            throw BizException.notFound("投标记录不存在或无权操作");
        }
        if (!"WON".equals(bid.getBidStatus())) {
            throw BizException.biz("仅已中标投标可生成商机");
        }
        if (bid.getGenOpportunityId() != null) {
            throw BizException.biz("该投标已生成商机（ID:" + bid.getGenOpportunityId() + "），请直接查看");
        }
        CrmOpportunity opportunity = new CrmOpportunity();
        opportunity.setName(bid.getTenderName());
        opportunity.setAccountId(bid.getAccountId());
        opportunity.setProductId(null);
        opportunity.setSalesStage("NEGOTIATION");
        opportunity.setExpectedAmount(bid.getWinAmount());
        opportunity.setEstRevenue(bid.getWinAmount());
        opportunity.setCurrency("CNY");
        opportunity.setPricingMode("LUMP_SUM");
        opportunity.setPricingDetail("{\"mode\":\"LUMP_SUM\",\"totalAmount\":" + bid.getWinAmount() + "}");
        opportunity.setOwnerUserId(bid.getOwnerUserId());
        opportunity.setOwnerOrgId(bid.getOwnerOrgId());
        opportunity.setOwnerUserName(bid.getOwnerUserName());
        opportunity.setIsWon(0);
        opportunity.setStageChangeTime(java.time.LocalDateTime.now());
        opportunityMapper.insert(opportunity);

        CrmBid update = new CrmBid();
        update.setId(id);
        update.setGenOpportunityId(opportunity.getId());
        bidMapper.updateById(update);
        return opportunity.getId();
    }

    /** 中标后生成合同草稿（source_type=BID），已生成过则提示 */
    @Transactional
    public Long genContract(Long id, Long companyId, Long productId) {
        CrmBid bid = bidMapper.selectById(id);
        if (bid == null) {
            throw BizException.notFound("投标记录不存在或无权操作");
        }
        if (!"WON".equals(bid.getBidStatus())) {
            throw BizException.biz("仅已中标投标可生成合同");
        }
        if (bid.getGenContractId() != null) {
            throw BizException.biz("该投标已生成合同（ID:" + bid.getGenContractId() + "），请直接查看");
        }
        if (companyId == null) {
            throw BizException.biz("请选择我方签约主体");
        }
        CrmContract contract = new CrmContract();
        contract.setAccountId(bid.getAccountId());
        contract.setCompanyId(companyId);
        contract.setProductId(productId);
        contract.setBusinessGroupId(bid.getRegionOrgId() == null ? bid.getOwnerOrgId() : bid.getRegionOrgId());
        contract.setSignDate(LocalDate.now());
        contract.setStartDate(LocalDate.now());
        contract.setEndDate(LocalDate.now().plusYears(1).minusDays(1));
        contract.setName(bid.getTenderName() + "服务合同");
        contract.setWinAmount(bid.getWinAmount());
        contract.setSourceType("BID");
        contract.setSourceId(bid.getId());
        Long contractId = contractService.create(contract);

        CrmBid update = new CrmBid();
        update.setId(id);
        update.setGenContractId(contractId);
        bidMapper.updateById(update);
        return contractId;
    }

    /** 开标提醒：未来 N 天开标 */
    public List<CrmBid> openingSoon(int days) {
        LocalDate today = LocalDate.now();
        return bidMapper.selectOpeningSoon(today, today.plusDays(days <= 0 ? 3 : days));
    }

    /**
     * 定时任务：开标前推送待办给负责人。
     * 幂等由 NoticePublisher 的 (bizType, bizId, receiverId) 去重保证。
     */
    @Transactional
    public int remindOpeningSoon() {
        int days = crmConfig.bidOpenNotifyDays();
        int sent = 0;
        for (CrmBid bid : openingSoon(days)) {
            boolean created = noticePublisher.publishTodo(bid.getOwnerUserId(),
                    "投标即将开标：" + bid.getTenderName(),
                    "项目「" + bid.getTenderName() + "」将于 " + bid.getOpenDate() + " 开标，请确认投标文件与保证金状态。",
                    "CRM_BID_OPENING", bid.getId(), "/crm/bid-calendar");
            if (created) {
                sent++;
            }
        }
        return sent;
    }

    /** 报表：区间内投标数与中标数 */
    public Map<String, Long> stats(LocalDate from, LocalDate to) {
        List<CrmBid> list = bidMapper.selectList(Wrappers.<CrmBid>lambdaQuery()
                .ge(from != null, CrmBid::getOpenDate, from)
                .le(to != null, CrmBid::getOpenDate, to));
        long total = list.size();
        long won = list.stream().filter(b -> "WON".equals(b.getBidStatus())).count();
        return Map.of("total", total, "won", won);
    }
}
