package com.guolian.pm.crm.common.service;

/**
 * 通知/待办投递端口：CRM 不直接依赖底座通知实现，只声明「要发什么」。
 * 底座通知能力变更时只需替换实现，业务代码不动。
 */
public interface NoticePublisher {

    /**
     * 投递一条站内待办（幂等：同一 bizType + bizId 只生成一条未完成待办）。
     *
     * @return true 表示本次新建，false 表示已存在或接收人为空
     */
    boolean publishTodo(Long receiverId, String title, String content,
                        String bizType, Long bizId, String route);

    /** 投递一条普通通知 */
    void publishNotice(Long receiverId, String title, String content);
}
