package com.guolian.pm.pms.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.pms.common.dto.PmDtos;
import com.guolian.pm.pms.project.entity.PmProject;
import com.guolian.pm.pms.project.entity.PmProjectMember;
import com.guolian.pm.pms.project.entity.PmProjectTask;
import com.guolian.pm.pms.project.mapper.PmProjectMapper;
import com.guolian.pm.pms.project.mapper.PmProjectMemberMapper;
import com.guolian.pm.pms.project.mapper.PmProjectTaskMapper;
import com.guolian.pm.system.user.entity.SysUser;
import com.guolian.pm.system.user.service.UserProjectProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目成员：人员与项目的双向关联（PRD 六）。
 *
 * <p>三条关键规则：
 * <ol>
 *   <li>投入比例：同一人跨在办项目累计 ≤ 100%，超出阻断并列冲突项目（PRD 6.4-1 / AC-07）</li>
 *   <li>认证校验：执行类角色（驻场调解员/实施主管）须已认证且在有效期（R-10 / AC-08）</li>
 *   <li>释放出场：置 RELEASED 并留痕，比例即时释放</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class ProjectMemberService implements UserProjectProvider {

    /** 需要上岗认证的执行类项目角色 */
    private static final List<String> EXECUTION_ROLES = List.of("FIELD_MEDIATOR", "IMPLEMENT_LEADER");

    private final PmProjectMemberMapper memberMapper;
    private final PmProjectTaskMapper taskMapper;
    private final PmProjectMapper projectMapper;
    private final ProjectService projectService;
    private final CrmOwnerSupport owner;

    /** 指派人员进项目 */
    @Transactional
    public Long assign(Long userId, PmDtos.MemberAssignRequest request) {
        SysUser user = owner.requireActiveUser(userId);
        if (request.getInputRatio() == null || request.getInputRatio() < 0 || request.getInputRatio() > 100) {
            throw BizException.biz("投入比例须在 0~100 之间");
        }
        PmProject project = projectMapper.selectById(request.getProjectId());
        if (project == null) {
            throw BizException.notFound("项目不存在");
        }
        if (memberMapper.countActive(request.getProjectId(), userId) > 0) {
            throw BizException.biz("该成员已在此项目中，请直接变更角色或投入比例");
        }
        checkCertification(user, request.getProjectRole());
        checkInputRatio(userId, user.getRealName(), request.getInputRatio(), null);
        if (request.getJoinDate() == null) {
            request.setJoinDate(LocalDate.now());
        }
        PmProjectMember member = new PmProjectMember();
        member.setProjectId(request.getProjectId());
        member.setUserId(userId);
        member.setUserName(user.getRealName());
        member.setProjectRole(request.getProjectRole());
        member.setInputRatio(request.getInputRatio());
        member.setJoinDate(request.getJoinDate());
        member.setExpectedLeaveDate(request.getExpectedLeaveDate());
        member.setStatus("ACTIVE");
        memberMapper.insert(member);
        projectService.log(request.getProjectId(), "MEMBER_ASSIGNED",
                "指派成员：" + user.getRealName() + "（" + roleLabel(request.getProjectRole())
                        + "，投入 " + request.getInputRatio() + "%）");
        return member.getId();
    }

    /** 变更角色 / 投入比例 / 预计出场 */
    @Transactional
    public void updateMember(Long memberId, PmDtos.MemberUpdateRequest request) {
        PmProjectMember member = requireMember(memberId);
        if (!"ACTIVE".equals(member.getStatus())) {
            throw BizException.biz("已释放的成员不可变更，请重新指派");
        }
        if (request.getInputRatio() != null) {
            checkInputRatio(member.getUserId(), member.getUserName(), request.getInputRatio(), memberId);
        }
        PmProjectMember update = new PmProjectMember();
        update.setId(memberId);
        update.setProjectRole(request.getProjectRole());
        update.setInputRatio(request.getInputRatio());
        update.setExpectedLeaveDate(request.getExpectedLeaveDate());
        memberMapper.updateById(update);
        projectService.log(member.getProjectId(), "MEMBER_UPDATED",
                "变更成员：" + member.getUserName() + "（角色 " + request.getProjectRole()
                        + "，投入 " + request.getInputRatio() + "%）");
    }

    /** 释放出场（PRD 6.3 操作：释放出场） */
    @Transactional
    public void release(Long memberId, String evaluation) {
        PmProjectMember member = requireMember(memberId);
        PmProjectMember update = new PmProjectMember();
        update.setId(memberId);
        update.setStatus("RELEASED");
        update.setReleaseTime(LocalDateTime.now());
        update.setEvaluation(evaluation);
        memberMapper.updateById(update);
        projectService.log(member.getProjectId(), "MEMBER_RELEASED",
                "释放成员：" + member.getUserName() + "（释放投入 " + member.getInputRatio() + "%）");
    }

    public List<PmProjectMember> byProject(Long projectId) {
        return memberMapper.selectList(Wrappers.<PmProjectMember>lambdaQuery()
                .eq(PmProjectMember::getProjectId, projectId)
                .orderByAsc(PmProjectMember::getStatus).orderByAsc(PmProjectMember::getUserId));
    }

    // ---------------- 用户侧（系统管理·用户管理 关联项目 Tab） ----------------

    /** 用户关联项目列表：在办 + 历史，附在办任务数（PRD 6.3） */
    @Override
    public List<Map<String, Object>> projectsOf(Long userId) {
        List<PmProjectMember> memberships = memberMapper.selectActiveByUser(userId);
        // 历史（已释放）成员记录
        List<PmProjectMember> released = memberMapper.selectList(Wrappers.<PmProjectMember>lambdaQuery()
                .eq(PmProjectMember::getUserId, userId)
                .eq(PmProjectMember::getStatus, "RELEASED")
                .orderByDesc(PmProjectMember::getReleaseTime).last("limit 20"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (PmProjectMember member : memberships) {
            result.add(toUserProjectView(member, true));
        }
        for (PmProjectMember member : released) {
            result.add(toUserProjectView(member, false));
        }
        return result;
    }

    @Override
    public int activeRatioOf(Long userId) {
        Integer sum = memberMapper.sumActiveRatio(userId, null);
        return sum == null ? 0 : sum;
    }

    private Map<String, Object> toUserProjectView(PmProjectMember member, boolean active) {
        PmProject project = projectMapper.selectById(member.getProjectId());
        Map<String, Object> view = new HashMap<>();
        view.put("memberId", member.getId());
        view.put("projectId", member.getProjectId());
        view.put("projectCode", project == null ? null : project.getProjectCode());
        view.put("projectName", project == null ? null : project.getProjectName());
        view.put("productName", project == null ? null : project.getProductName());
        view.put("orgId", project == null ? null : project.getOrgId());
        view.put("projectRole", member.getProjectRole());
        view.put("projectRoleLabel", roleLabel(member.getProjectRole()));
        view.put("inputRatio", member.getInputRatio());
        view.put("joinDate", member.getJoinDate());
        view.put("expectedLeaveDate", member.getExpectedLeaveDate());
        view.put("status", member.getStatus());
        view.put("active", active);
        view.put("evaluation", member.getEvaluation());
        view.put("openTaskCount", taskMapper.countOpenByAssignee(member.getUserId()));
        return view;
    }

    // ---------------- 校验 ----------------

    /** 投入比例校验：在办合计 + 本次 > 100 时阻断并列冲突项目（AC-07） */
    private void checkInputRatio(Long userId, String userName, int ratio, Long excludeMemberId) {
        Integer existing = memberMapper.sumActiveRatio(userId, excludeMemberId);
        int sum = (existing == null ? 0 : existing) + ratio;
        if (sum > 100) {
            StringBuilder conflicts = new StringBuilder();
            for (PmProjectMember member : memberMapper.selectActiveByUser(userId)) {
                if (member.getId() != null && member.getId().equals(excludeMemberId)) {
                    continue;
                }
                PmProject project = projectMapper.selectById(member.getProjectId());
                conflicts.append(project == null ? member.getProjectId() : project.getProjectName())
                        .append("（").append(member.getInputRatio()).append("%）");
            }
            throw BizException.biz("「" + userName + "」投入比例将达 " + sum + "%，超过 100% 上限。"
                    + "冲突项目：" + conflicts + "。请先释放某个项目的投入。");
        }
    }

    /** 认证校验：执行类角色须 CERTIFIED 且未过期（R-10 / AC-08） */
    private void checkCertification(SysUser user, String projectRole) {
        if (!EXECUTION_ROLES.contains(projectRole)) {
            return;
        }
        String status = user.getCertStatus() == null ? "NONE" : user.getCertStatus();
        if (!"CERTIFIED".equals(status)) {
            throw BizException.biz("「" + user.getRealName() + "」尚未完成上岗认证，不能承担执行类任务（"
                    + roleLabel(projectRole) + "）。请先在用户管理完成认证。");
        }
        if (user.getCertExpireDate() != null && user.getCertExpireDate().isBefore(LocalDate.now())) {
            throw BizException.biz("「" + user.getRealName() + "」上岗认证已于 "
                    + user.getCertExpireDate() + " 过期，请先安排复训认证。");
        }
    }

    private PmProjectMember requireMember(Long memberId) {
        PmProjectMember member = memberMapper.selectById(memberId);
        if (member == null) {
            throw BizException.notFound("项目成员记录不存在");
        }
        return member;
    }

    private String roleLabel(String role) {
        return switch (role == null ? "" : role) {
            case "PROJECT_MANAGER" -> "项目经理";
            case "IMPLEMENT_LEADER" -> "实施主管";
            case "FIELD_MEDIATOR" -> "驻场调解员";
            case "BUSINESS_REP" -> "商务代表";
            case "QC" -> "质检";
            case "OBSERVER" -> "只读观察者";
            default -> role == null ? "" : role;
        };
    }
}
