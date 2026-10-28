package com.guolian.pm.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.system.user.dto.UserCompanyBrief;
import com.guolian.pm.system.user.entity.SysUserCompany;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserCompanyMapper extends BaseMapper<SysUserCompany> {

    @Select("""
            <script>
            select uc.user_id as userId, uc.company_id as companyId, uc.relation_type as relationType,
                   uc.is_primary as isPrimary, c.company_name as companyName
            from sys_user_company uc
            left join sys_company c on c.id = uc.company_id and c.is_deleted = 0
            where uc.is_deleted = 0 and uc.user_id in
            <foreach collection="userIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<UserCompanyBrief> selectCompanyBriefs(@Param("userIds") List<Long> userIds);
}
