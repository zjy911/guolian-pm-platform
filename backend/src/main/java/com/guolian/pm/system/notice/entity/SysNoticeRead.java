package com.guolian.pm.system.notice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice_read")
public class SysNoticeRead extends BaseEntity {

    private Long noticeId;
    private Long userId;
    private LocalDateTime readTime;
}
