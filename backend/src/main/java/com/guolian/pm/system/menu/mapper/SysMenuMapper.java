package com.guolian.pm.system.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.system.menu.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 当前角色可见菜单（目录/菜单，不含按钮），由后端下发，前端只维护静态路由表。
     */
    @Select("""
            select m.* from sys_menu m
            join sys_role_menu rm on rm.menu_id = m.id and rm.is_deleted = 0
            where rm.role_id = #{roleId} and m.is_deleted = 0 and m.status = 1 and m.menu_type in (1, 2)
            order by m.sort_no asc
            """)
    List<SysMenu> selectMenuByRole(@Param("roleId") Long roleId);

    /**
     * 当前角色的按钮权限标识。
     */
    @Select("""
            select distinct m.perms from sys_menu m
            join sys_role_menu rm on rm.menu_id = m.id and rm.is_deleted = 0
            where rm.role_id = #{roleId} and m.is_deleted = 0 and m.status = 1
              and m.perms is not null and m.perms <> ''
            """)
    List<String> selectPermsByRole(@Param("roleId") Long roleId);

    @Select("select menu_id from sys_role_menu where role_id = #{roleId} and is_deleted = 0")
    List<Long> selectMenuIdsByRole(@Param("roleId") Long roleId);
}
