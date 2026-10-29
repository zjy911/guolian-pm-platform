package com.guolian.pm.system.user.service;

import java.util.List;
import java.util.Map;

/**
 * 人员 ↔ 项目双向可查的桥梁（PRD 6.1：用户是唯一人员来源，反向用户详情可看参与的全部项目）。
 * 由项目管理模块实现，底座只依赖本接口，避免 system → pms 的反向编译依赖。
 */
public interface UserProjectProvider {

    /** 某用户的关联项目列表：在办 + 历史，附在办任务数与投入比例合计 */
    List<Map<String, Object>> projectsOf(Long userId);

    /** 某用户在办项目投入比例合计（供表单实时提示） */
    int activeRatioOf(Long userId);
}
