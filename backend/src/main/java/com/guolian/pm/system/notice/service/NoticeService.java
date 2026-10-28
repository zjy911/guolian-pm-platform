package com.guolian.pm.system.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolian.pm.common.api.PageQuery;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.common.web.LoginUser;
import com.guolian.pm.system.notice.entity.SysNotice;
import com.guolian.pm.system.notice.entity.SysNoticeRead;
import com.guolian.pm.system.notice.mapper.SysNoticeMapper;
import com.guolian.pm.system.notice.mapper.SysNoticeReadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知 / 公告 / 站内待办。
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final SysNoticeMapper noticeMapper;
    private final SysNoticeReadMapper noticeReadMapper;
    private final BadgeService badgeService;

    public PageResult<SysNotice> page(PageQuery query, String keyword, Integer noticeType, Integer status) {
        LambdaQueryWrapper<SysNotice> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysNotice::getTitle, keyword);
        }
        if (noticeType != null) {
            wrapper.eq(SysNotice::getNoticeType, noticeType);
        }
        if (status != null) {
            wrapper.eq(SysNotice::getStatus, status);
        }
        wrapper.orderByDesc(SysNotice::getPinned).orderByDesc(SysNotice::getPublishTime);
        Page<SysNotice> page = noticeMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), query.safePageNum(), query.safePageSize());
    }

    /** 当前用户可见的通知公告（全员 + 指定本人） */
    public List<SysNotice> myNotices(Integer noticeType) {
        LoginUser user = AuthContext.require();
        LambdaQueryWrapper<SysNotice> wrapper = Wrappers.<SysNotice>lambdaQuery()
                .eq(SysNotice::getStatus, 1)
                .and(w -> w.eq(SysNotice::getReceiverScope, "ALL")
                        .or().isNull(SysNotice::getReceiverId)
                        .or().eq(SysNotice::getReceiverId, user.getUserId()));
        if (noticeType != null) {
            wrapper.eq(SysNotice::getNoticeType, noticeType);
        } else {
            wrapper.in(SysNotice::getNoticeType, 1, 2);
        }
        wrapper.orderByDesc(SysNotice::getPinned).orderByDesc(SysNotice::getPublishTime);
        List<SysNotice> list = noticeMapper.selectList(wrapper);
        list.forEach(n -> n.setTodoStatus(readFlag(n.getId(), user.getUserId())));
        return list;
    }

    public List<SysNotice> myTodos() {
        LoginUser user = AuthContext.require();
        return noticeMapper.selectList(Wrappers.<SysNotice>lambdaQuery()
                .eq(SysNotice::getNoticeType, 3)
                .eq(SysNotice::getTodoStatus, 0)
                .and(w -> w.eq(SysNotice::getReceiverId, user.getUserId()).or().isNull(SysNotice::getReceiverId))
                .orderByDesc(SysNotice::getPublishTime));
    }

    @Transactional
    public Long save(SysNotice notice) {
        if (!StringUtils.hasText(notice.getTitle())) {
            throw BizException.badRequest("请输入标题");
        }
        if (notice.getNoticeType() == null) {
            notice.setNoticeType(1);
        }
        if (notice.getStatus() == null) {
            notice.setStatus(1);
        }
        if (notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        if (notice.getId() == null) {
            noticeMapper.insert(notice);
        } else {
            noticeMapper.updateById(notice);
        }
        return notice.getId();
    }

    @Transactional
    public void delete(Long id) {
        noticeMapper.deleteById(id);
    }

    public void read(Long noticeId) {
        LoginUser user = AuthContext.require();
        badgeService.markRead(noticeId, user.getUserId());
    }

    @Transactional
    public void readAll() {
        LoginUser user = AuthContext.require();
        for (SysNotice notice : myNotices(null)) {
            badgeService.markRead(notice.getId(), user.getUserId());
        }
    }

    public void completeTodo(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw BizException.notFound("待办不存在");
        }
        SysNotice update = new SysNotice();
        update.setId(id);
        update.setTodoStatus(1);
        noticeMapper.updateById(update);
    }

    private int readFlag(Long noticeId, Long userId) {
        Long count = noticeReadMapper.selectCount(Wrappers.<SysNoticeRead>lambdaQuery()
                .eq(SysNoticeRead::getNoticeId, noticeId)
                .eq(SysNoticeRead::getUserId, userId));
        return count != null && count > 0 ? 1 : 0;
    }
}
