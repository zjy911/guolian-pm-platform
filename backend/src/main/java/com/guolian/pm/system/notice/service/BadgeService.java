package com.guolian.pm.system.notice.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.system.notice.entity.SysNotice;
import com.guolian.pm.system.notice.mapper.SysNoticeMapper;
import com.guolian.pm.system.notice.mapper.SysNoticeReadMapper;
import com.guolian.pm.common.web.LoginUser;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角标聚合：待审批数 + 未读通知公告数。
 */
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final SysNoticeMapper noticeMapper;
    private final SysNoticeReadMapper noticeReadMapper;
    private final TaskService taskService;

    public record Badge(long todoCount, long unreadCount) {
    }

    public Badge badge(LoginUser user) {
        return new Badge(flowTodoCount(user), unreadCount(user));
    }

    public long unreadCount(LoginUser user) {
        Long uid = user.getUserId();
        return noticeMapper.selectCount(Wrappers.<SysNotice>lambdaQuery()
                .in(SysNotice::getNoticeType, 1, 2)
                .eq(SysNotice::getStatus, 1)
                .and(w -> w.eq(SysNotice::getReceiverScope, "ALL")
                        .or().isNull(SysNotice::getReceiverId)
                        .or().eq(SysNotice::getReceiverId, uid))
                .notInSql(SysNotice::getId,
                        "select notice_id from sys_notice_read where is_deleted = 0 and user_id = " + uid));
    }

    public long todoNoticeCount(LoginUser user) {
        Long uid = user.getUserId();
        return noticeMapper.selectCount(Wrappers.<SysNotice>lambdaQuery()
                .eq(SysNotice::getNoticeType, 3)
                .eq(SysNotice::getTodoStatus, 0)
                .and(w -> w.eq(SysNotice::getReceiverId, uid).or().isNull(SysNotice::getReceiverId)));
    }

    /**
     * Flowable 待办：同时覆盖 assignee 与 candidate（用户 / 角色组）。
     */
    public long flowTodoCount(LoginUser user) {
        String uid = String.valueOf(user.getUserId());
        List<String> groups = user.getRoleCodes();
        var query = taskService.createTaskQuery().or()
                .taskAssignee(uid)
                .taskCandidateUser(uid);
        if (groups != null && !groups.isEmpty()) {
            query.taskCandidateGroupIn(groups);
        }
        return query.endOr().count();
    }

    public long totalTodo(LoginUser user) {
        return todoNoticeCount(user) + flowTodoCount(user);
    }

    /** 标记单条通知已读 */
    public void markRead(Long noticeId, Long userId) {
        Long exists = noticeReadMapper.selectCount(Wrappers.<com.guolian.pm.system.notice.entity.SysNoticeRead>lambdaQuery()
                .eq(com.guolian.pm.system.notice.entity.SysNoticeRead::getNoticeId, noticeId)
                .eq(com.guolian.pm.system.notice.entity.SysNoticeRead::getUserId, userId));
        if (exists != null && exists > 0) {
            return;
        }
        com.guolian.pm.system.notice.entity.SysNoticeRead read =
                new com.guolian.pm.system.notice.entity.SysNoticeRead();
        read.setNoticeId(noticeId);
        read.setUserId(userId);
        read.setReadTime(java.time.LocalDateTime.now());
        noticeReadMapper.insert(read);
    }
}
