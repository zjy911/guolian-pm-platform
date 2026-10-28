package com.guolian.pm.transfer.adapter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.system.notice.entity.SysNotice;
import com.guolian.pm.system.notice.mapper.SysNoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 内置适配器 1：站内待办（sys_notice 中 notice_type = 3 且未完成的待办）。
 */
@Component
@RequiredArgsConstructor
public class NoticeTodoTransferAdapter implements TransferAdapter {

    private final SysNoticeMapper noticeMapper;

    @Override
    public String bizType() {
        return "NOTICE_TODO";
    }

    @Override
    public String bizName() {
        return "站内待办";
    }

    @Override
    public long count(Long fromUserId, LocalDateTime since) {
        var wrapper = Wrappers.<SysNotice>lambdaQuery()
                .eq(SysNotice::getNoticeType, 3)
                .eq(SysNotice::getReceiverId, fromUserId)
                .eq(SysNotice::getTodoStatus, 0);
        if (since != null) {
            wrapper.ge(SysNotice::getCreatedTime, since);
        }
        return noticeMapper.selectCount(wrapper);
    }

    @Override
    public List<Long> listIds(Long fromUserId, int offset, int limit) {
        return noticeMapper.selectList(Wrappers.<SysNotice>lambdaQuery()
                        .eq(SysNotice::getNoticeType, 3)
                        .eq(SysNotice::getReceiverId, fromUserId)
                        .eq(SysNotice::getTodoStatus, 0)
                        .orderByAsc(SysNotice::getId)
                        .last("limit " + limit + " offset " + offset))
                .stream().map(SysNotice::getId).toList();
    }

    @Override
    public TransferOutcome transfer(Long fromUserId, Long toUserId, List<Long> bizIds) {
        List<Long> succeeded = new ArrayList<>();
        Map<Long, String> failed = new java.util.LinkedHashMap<>();
        for (Long bizId : bizIds) {
            SysNotice notice = noticeMapper.selectById(bizId);
            if (notice == null) {
                failed.put(bizId, "待办不存在");
                continue;
            }
            if (!fromUserId.equals(notice.getReceiverId())) {
                failed.put(bizId, "待办归属已变更");
                continue;
            }
            SysNotice update = new SysNotice();
            update.setId(bizId);
            update.setReceiverId(toUserId);
            noticeMapper.updateById(update);
            succeeded.add(bizId);
        }
        return new TransferOutcome(succeeded, failed);
    }

    @Override
    public void rollback(List<RollbackItem> items) {
        for (RollbackItem item : items) {
            SysNotice update = new SysNotice();
            update.setId(item.bizId());
            update.setReceiverId(item.beforeOwnerId());
            noticeMapper.updateById(update);
        }
    }

    @Override
    public String describe(Long bizId) {
        SysNotice notice = noticeMapper.selectById(bizId);
        return notice == null ? "待办 #" + bizId : "待办：" + notice.getTitle();
    }
}
