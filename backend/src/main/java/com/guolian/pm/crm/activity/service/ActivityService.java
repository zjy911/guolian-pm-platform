package com.guolian.pm.crm.activity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.crm.account.entity.CrmAccount;
import com.guolian.pm.crm.account.mapper.CrmAccountMapper;
import com.guolian.pm.crm.activity.entity.CrmActivity;
import com.guolian.pm.crm.activity.mapper.CrmActivityMapper;
import com.guolian.pm.crm.common.dto.CrmQuery;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.crm.contact.entity.CrmContact;
import com.guolian.pm.crm.contact.mapper.CrmContactMapper;
import com.guolian.pm.crm.lead.entity.CrmLead;
import com.guolian.pm.crm.lead.mapper.CrmLeadMapper;
import com.guolian.pm.crm.opportunity.entity.CrmOpportunity;
import com.guolian.pm.crm.opportunity.mapper.CrmOpportunityMapper;
import com.guolian.pm.crm.common.service.NoticePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动服务：多态挂接线索 / 客户 / 联系人 / 商机；不物理删除只作废；
 * 新增与作废都必须在同一事务内刷新目标对象的最近活动时间。
 */
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final CrmActivityMapper activityMapper;
    private final CrmLeadMapper leadMapper;
    private final CrmAccountMapper accountMapper;
    private final CrmContactMapper contactMapper;
    private final CrmOpportunityMapper opportunityMapper;
    private final CrmOwnerSupport ownerSupport;
    private final NoticePublisher noticePublisher;

    public PageResult<CrmActivity> page(CrmQuery q) {
        Page<CrmActivity> page = new Page<>(q.safePageNum(), q.safePageSize());
        var wrapper = Wrappers.<CrmActivity>lambdaQuery()
                .eq(StringUtils.hasText(q.getTargetType()), CrmActivity::getTargetType, q.getTargetType())
                .eq(q.getTargetId() != null, CrmActivity::getTargetId, q.getTargetId())
                .eq(StringUtils.hasText(q.getActivityType()), CrmActivity::getType, q.getActivityType())
                .eq(q.getOwnerUserId() != null, CrmActivity::getOwnerUserId, q.getOwnerUserId())
                .ge(q.getStartDate() != null, CrmActivity::getStartTime,
                        q.getStartDate() == null ? null : q.getStartDate().atStartOfDay())
                .le(q.getEndDate() != null, CrmActivity::getStartTime,
                        q.getEndDate() == null ? null : q.getEndDate().atTime(23, 59, 59))
                .like(StringUtils.hasText(q.getKeyword()), CrmActivity::getSubject, q.getKeyword())
                .orderByDesc(CrmActivity::getStartTime);
        Page<CrmActivity> result = activityMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), q.safePageNum(), q.safePageSize());
    }

    /** 我的活动日历 */
    public List<CrmActivity> mine(LocalDate start, LocalDate end) {
        return activityMapper.selectList(Wrappers.<CrmActivity>lambdaQuery()
                .eq(CrmActivity::getOwnerUserId, ownerSupport.currentUserId())
                .ge(CrmActivity::getStartTime, (start == null ? LocalDate.now().minusMonths(1) : start).atStartOfDay())
                .le(CrmActivity::getStartTime, (end == null ? LocalDate.now().plusMonths(1) : end).atTime(23, 59, 59))
                .orderByAsc(CrmActivity::getStartTime));
    }

    /** 活动时间线：按时间倒序，作废记录不参与展示 */
    public List<CrmActivity> timeline(String targetType, Long targetId) {
        return activityMapper.selectList(Wrappers.<CrmActivity>lambdaQuery()
                .eq(CrmActivity::getTargetType, targetType)
                .eq(CrmActivity::getTargetId, targetId)
                .eq(CrmActivity::getStatus, "VALID")
                .orderByDesc(CrmActivity::getStartTime));
    }

    @Transactional
    public Long create(CrmActivity request) {
        if (!StringUtils.hasText(request.getSubject())) {
            throw BizException.biz("请填写活动主题");
        }
        if (!StringUtils.hasText(request.getType())) {
            throw BizException.biz("请选择活动类型");
        }
        if (!StringUtils.hasText(request.getTargetType()) || request.getTargetId() == null) {
            throw BizException.biz("活动必须挂接到线索/客户/联系人/商机");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw BizException.biz("请填写活动起止时间");
        }
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw BizException.biz("结束时间不得早于开始时间");
        }
        requireTarget(request.getTargetType(), request.getTargetId());
        request.setId(null);
        request.setStatus("VALID");
        request.setDurationDays(durationDays(request.getStartTime(), request.getEndTime()));
        request.setOwnerUserId(ownerSupport.currentUserId());
        request.setOwnerOrgId(ownerSupport.createOrgId());
        request.setOwnerUserName(ownerSupport.currentUserName());
        activityMapper.insert(request);
        refreshTargetActivity(request.getTargetType(), request.getTargetId());
        return request.getId();
    }

    @Transactional
    public void update(Long id, CrmActivity request) {
        CrmActivity exist = activityMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("活动不存在或无权修改");
        }
        if ("VOIDED".equals(exist.getStatus())) {
            throw BizException.biz("已作废活动不可编辑");
        }
        request.setId(id);
        request.setTargetType(exist.getTargetType());
        request.setTargetId(exist.getTargetId());
        request.setOwnerUserId(exist.getOwnerUserId());
        request.setOwnerOrgId(exist.getOwnerOrgId());
        request.setOwnerUserName(exist.getOwnerUserName());
        if (request.getStartTime() != null && request.getEndTime() != null) {
            request.setDurationDays(durationDays(request.getStartTime(), request.getEndTime()));
        }
        activityMapper.updateById(request);
        refreshTargetActivity(exist.getTargetType(), exist.getTargetId());
    }

    /** 作废（不物理删除，保留审计痕迹） */
    @Transactional
    public void voidActivity(Long id, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw BizException.biz("作废必须填写原因");
        }
        CrmActivity exist = activityMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("活动不存在或无权操作");
        }
        if ("VOIDED".equals(exist.getStatus())) {
            return;
        }
        CrmActivity update = new CrmActivity();
        update.setId(id);
        update.setStatus("VOIDED");
        update.setVoidReason(reason);
        activityMapper.updateById(update);
        refreshTargetActivity(exist.getTargetType(), exist.getTargetId());
    }

    /** 任务型活动到期未完成 → 生成底座待办（幂等：同活动同待办只生成一次） */
    @Transactional
    public int remindOverdueTasks() {
        List<Long> ids = activityMapper.selectOverdueTasks(LocalDateTime.now(), 200);
        int created = 0;
        for (Long id : ids) {
            CrmActivity activity = activityMapper.selectById(id);
            if (activity == null) {
                continue;
            }
            boolean sent = noticePublisher.publishTodo(activity.getOwnerUserId(), "活动任务已到期：" + activity.getSubject(),
                    "活动任务「" + activity.getSubject() + "」已超过结束时间，请及时处理。",
                    "CRM_ACTIVITY_TASK", activity.getId(), "/crm/activity");
            if (sent) {
                created++;
            }
        }
        return created;
    }

    // ---------------- 内部 ----------------

    private void requireTarget(String targetType, Long targetId) {
        boolean exists = switch (targetType) {
            case "LEAD" -> {
                CrmLead lead = leadMapper.selectById(targetId);
                yield lead != null;
            }
            case "ACCOUNT" -> {
                CrmAccount account = accountMapper.selectById(targetId);
                yield account != null;
            }
            case "CONTACT" -> {
                CrmContact contact = contactMapper.selectById(targetId);
                yield contact != null;
            }
            case "OPPORTUNITY" -> {
                CrmOpportunity opportunity = opportunityMapper.selectById(targetId);
                yield opportunity != null;
            }
            default -> false;
        };
        if (!exists) {
            throw BizException.biz("目标对象不存在或已删除，无法创建活动");
        }
    }

    /** 刷新目标对象的最近活动时间（时间线冗余字段，禁止手工编辑） */
    private void refreshTargetActivity(String targetType, Long targetId) {
        LocalDateTime latest = activityMapper.maxStartTime(targetType, targetId);
        switch (targetType) {
            case "LEAD" -> {
                CrmLead lead = new CrmLead();
                lead.setId(targetId);
                lead.setLastFollowTime(latest);
                leadMapper.updateById(lead);
            }
            case "ACCOUNT" -> {
                CrmAccount account = new CrmAccount();
                account.setId(targetId);
                account.setLastActivityTime(latest == null ? LocalDateTime.now() : latest);
                accountMapper.updateById(account);
            }
            case "CONTACT" -> {
                CrmContact contact = new CrmContact();
                contact.setId(targetId);
                contact.setLastActivityTime(latest);
                contactMapper.updateById(contact);
            }
            case "OPPORTUNITY" -> {
                CrmOpportunity opportunity = new CrmOpportunity();
                opportunity.setId(targetId);
                opportunity.setLastActivityTime(latest);
                opportunityMapper.updateById(opportunity);
            }
            default -> {
                // 未知类型不处理
            }
        }
    }

    /** 不足一天按 1 天，向上取整 */
    private int durationDays(LocalDateTime start, LocalDateTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        long days = (minutes + (24 * 60 - 1)) / (24 * 60);
        return (int) Math.max(1, days);
    }
}
