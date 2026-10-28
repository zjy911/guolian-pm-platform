package com.guolian.pm.crm.common.service;

import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.common.web.LoginUser;
import com.guolian.pm.system.role.entity.SysRole;
import com.guolian.pm.system.role.mapper.SysRoleMapper;
import com.guolian.pm.system.user.entity.SysUser;
import com.guolian.pm.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 业务归属（负责人 / 归属组织）取值规则，CRM 全部写操作统一走这里。
 *
 * <p>三条硬规则：
 * <ol>
 *   <li>创建时 owner_org_id 取当前角色绑定的组织（由 Token 派生，<b>不接受前端传参</b>）</li>
 *   <li>转移负责人时 owner_user_id 与 owner_org_id 必须同一次 update 一起改</li>
 *   <li>姓名快照 owner_user_name 同步刷新，否则列表仍显示前任</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class CrmOwnerSupport {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    public LoginUser current() {
        return AuthContext.require();
    }

    public Long currentUserId() {
        return current().getUserId();
    }

    public Long currentOrgId() {
        return current().getOrgId();
    }

    public String currentUserName() {
        LoginUser user = current();
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    /** 创建时的归属组织：当前角色绑定的组织 */
    public Long createOrgId() {
        Long orgId = currentOrgId();
        if (orgId == null) {
            throw BizException.biz("当前角色未绑定组织，无法创建业务数据");
        }
        return orgId;
    }

    /** 指定负责人的姓名快照 */
    public String nameOf(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : user.getRealName();
    }

    /** 指定负责人的默认角色组织（转移负责人 / 交接时使用） */
    public Long orgOf(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getDefaultRoleId() == null) {
            return null;
        }
        SysRole role = roleMapper.selectById(user.getDefaultRoleId());
        return role == null ? null : role.getOrgId();
    }

    /** 查询负责人是否存在且状态正常 */
    public SysUser requireActiveUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("负责人不存在");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw BizException.biz("负责人「" + user.getRealName() + "」状态不正常");
        }
        return user;
    }
}
