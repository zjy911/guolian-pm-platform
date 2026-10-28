package com.guolian.pm.crm.common.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.system.notice.entity.SysNotice;
import com.guolian.pm.system.notice.mapper.SysNoticeMapper;
import com.guolian.pm.system.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 通知投递实现：统一走底座 sys_notice，CRM 不自建通知表。
 * 待办按 (bizType, bizId) 去重，重复执行定时任务不会刷屏。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticePublisherImpl implements NoticePublisher {

    private static final int TYPE_TODO = 3;
    private static final int TYPE_NOTICE = 1;

    private final NoticeService noticeService;
    private final SysNoticeMapper noticeMapper;

    @Override
    public boolean publishTodo(Long receiverId, String title, String content,
                               String bizType, Long bizId, String route) {
        if (receiverId == null) {
            return false;
        }
        long exists = noticeMapper.selectCount(Wrappers.<SysNotice>lambdaQuery()
                .eq(SysNotice::getBizType, bizType)
                .eq(SysNotice::getBizId, bizId)
                .eq(SysNotice::getReceiverId, receiverId));
        if (exists > 0) {
            return false;
        }
        SysNotice notice = new SysNotice();
        notice.setTitle(trim(title, 120));
        notice.setNoticeType(TYPE_TODO);
        notice.setContent(content);
        notice.setReceiverScope("USER");
        notice.setReceiverId(receiverId);
        notice.setPinned(0);
        notice.setStatus(1);
        notice.setPublishTime(LocalDateTime.now());
        notice.setTodoStatus(0);
        notice.setBizType(bizType);
        notice.setBizId(bizId);
        noticeService.save(notice);
        log.debug("CRM 投递待办 receiver={} bizType={} bizId={} route={}", receiverId, bizType, bizId, route);
        return true;
    }

    @Override
    public void publishNotice(Long receiverId, String title, String content) {
        if (receiverId == null) {
            return;
        }
        SysNotice notice = new SysNotice();
        notice.setTitle(trim(title, 120));
        notice.setNoticeType(TYPE_NOTICE);
        notice.setContent(content);
        notice.setReceiverScope("USER");
        notice.setReceiverId(receiverId);
        notice.setPinned(0);
        notice.setStatus(1);
        notice.setPublishTime(LocalDateTime.now());
        notice.setTodoStatus(0);
        noticeService.save(notice);
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
