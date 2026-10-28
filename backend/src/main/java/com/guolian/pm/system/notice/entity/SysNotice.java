package com.guolian.pm.system.notice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 通知 / 公告 / 站内待办。notice_type：1 通知 / 2 公告 / 3 待办。
 * 站内待办同时是账号交接的内置适配器业务对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class SysNotice extends BaseEntity {

    private String title;
    private Integer noticeType;
    @TableField("content")
    private String content;
    private String receiverScope;
    private Long receiverId;
    private Integer pinned;
    private Integer status;
    private LocalDateTime publishTime;
    /** 待办状态：0 未完成 / 1 已完成 */
    private Integer todoStatus;
    private String bizType;
    private Long bizId;
}
