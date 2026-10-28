package com.guolian.pm.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.system.user.dto.UserRoleBrief;
import com.guolian.pm.system.user.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 批量装配「用户 → 全部角色（含组织）」，避免列表 N+1。
     */
    @Select("""
            <script>
            select ur.user_id as userId, ur.role_id as roleId, ur.is_default as isDefault,
                   r.role_code as roleCode, r.role_name as roleName, r.org_id as orgId,
                   o.org_name as orgName, r.data_scope as dataScope
            from sys_user_role ur
            join sys_role r on r.id = ur.role_id and r.is_deleted = 0
            left join sys_org o on o.id = r.org_id and o.is_deleted = 0
            where ur.is_deleted = 0 and ur.user_id in
            <foreach collection="userIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            order by ur.is_default desc
            </script>
            """)
    List<UserRoleBrief> selectRoleBriefs(@Param("userIds") List<Long> userIds);

    @Select("select role_id from sys_user_role where user_id = #{userId} and is_deleted = 0")
    List<Long> selectRoleIds(@Param("userId") Long userId);

    @Select("select role_id, count(1) as cnt from sys_user_role where is_deleted = 0 group by role_id")
    List<java.util.Map<String, Object>> selectCountGroupByRole();
}
