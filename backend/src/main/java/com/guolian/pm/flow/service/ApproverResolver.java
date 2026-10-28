package com.guolian.pm.flow.service;

import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.common.scope.DataScopeService;
import com.guolian.pm.flow.dto.FlowDtos;
import com.guolian.pm.common.web.LoginUser;
import com.guolian.pm.system.org.entity.SysOrg;
import com.guolian.pm.system.role.entity.SysRole;
import com.guolian.pm.system.role.mapper.SysRoleMapper;
import com.guolian.pm.system.user.entity.SysUser;
import com.guolian.pm.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 审批人规则求值：启动前解析成具体用户 ID 集合，放入流程变量。
 * 空审批人必须在启动前拦截，不能让 Flowable 产生孤儿任务。
 *
 * 说明：账号交接这类「为他人发起」的场景，直属上级/组织负责人应按单据主体人（subjectUserId）取，
 * 而不是按操作人取，否则会派给错误的人。
 */
@Component
@RequiredArgsConstructor
public class ApproverResolver {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final DataScopeService dataScopeService;

    public List<Long> resolve(FlowDtos.NodeConfig node, LoginUser initiator, Map<String, Object> variables) {
        String type = node.getApproverType() == null ? "USER" : node.getApproverType();
        SysUser subject = subjectUser(initiator, variables);
        List<Long> result = new ArrayList<>();
        switch (type) {
            case "LEADER" -> {
                if (subject.getLeaderId() != null) {
                    result.add(subject.getLeaderId());
                }
            }
            case "ORG_LEADER" -> {
                Long orgId = resolveOrgId(subject, initiator);
                SysOrg org = dataScopeService.allOrgs().stream()
                        .filter(o -> o.getId().equals(orgId))
                        .findFirst().orElse(null);
                if (org != null && org.getLeaderId() != null) {
                    result.add(org.getLeaderId());
                }
            }
            case "ROLE" -> result.addAll(roleMapper.selectApproverIdsByRoleCode(node.getRoleCode()));
            case "INITIATOR" -> result.add(initiator.getUserId());
            default -> {
                if (node.getUserIds() != null) {
                    result.addAll(node.getUserIds());
                }
            }
        }
        return result.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }

    public void requireNotEmpty(FlowDtos.NodeConfig node, List<Long> approvers) {
        if (approvers.isEmpty()) {
            throw BizException.biz("节点「" + node.getName() + "」未匹配到审批人，请检查审批人规则配置");
        }
    }

    private SysUser subjectUser(LoginUser initiator, Map<String, Object> variables) {
        Object subjectId = variables == null ? null : variables.get("subjectUserId");
        if (subjectId != null) {
            try {
                SysUser subject = userMapper.selectById(Long.valueOf(String.valueOf(subjectId)));
                if (subject != null) {
                    return subject;
                }
            } catch (NumberFormatException ignored) {
                // 交由调用方兜底
            }
        }
        return userMapper.selectById(initiator.getUserId());
    }

    private Long resolveOrgId(SysUser subject, LoginUser initiator) {
        if (subject != null && subject.getDefaultRoleId() != null) {
            SysRole role = roleMapper.selectById(subject.getDefaultRoleId());
            if (role != null) {
                return role.getOrgId();
            }
        }
        return initiator.getOrgId();
    }
}
