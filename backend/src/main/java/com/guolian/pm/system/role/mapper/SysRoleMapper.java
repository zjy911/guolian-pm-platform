package com.guolian.pm.system.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.system.role.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询某用户全部启用角色（含组织信息），用于登录与角色切换下拉。
     */
    @Select("""
            select r.* from sys_role r
            join sys_user_role ur on ur.role_id = r.id and ur.is_deleted = 0
            where ur.user_id = #{userId} and r.is_deleted = 0 and r.status = 1
            order by ur.is_default desc, r.sort_no asc
            """)
    List<SysRole> selectEnabledRolesByUser(@Param("userId") Long userId);

    /**
     * 统计该用户在各组织下已占用的组织数，用于「同一组织只允许一个角色」校验。
     */
    @Select("""
            select count(1) from sys_user_role ur
            join sys_role r on r.id = ur.role_id and r.is_deleted = 0
            where ur.user_id = #{userId} and ur.is_deleted = 0 and r.org_id = #{orgId}
              and ur.role_id <> #{excludeRoleId}
            """)
    int countUserRoleInOrg(@Param("userId") Long userId, @Param("orgId") Long orgId,
                           @Param("excludeRoleId") Long excludeRoleId);

    @Select("select count(1) from sys_user_role where role_id = #{roleId} and is_deleted = 0")
    int countRoleUsers(@Param("roleId") Long roleId);

    /**
     * 按角色编码取可被分配审批的用户（can_be_approver = 1 且用户正常）。
     */
    @Select("""
            select ur.user_id from sys_user_role ur
            join sys_role r on r.id = ur.role_id and r.is_deleted = 0
            join sys_user u on u.id = ur.user_id and u.is_deleted = 0
            where r.role_code = #{roleCode} and r.status = 1 and r.can_be_approver = 1 and u.status = 1
            """)
    List<Long> selectApproverIdsByRoleCode(@Param("roleCode") String roleCode);
}
