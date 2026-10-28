package com.guolian.pm.crm.lead.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.crm.lead.entity.CrmLead;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmLeadMapper extends BaseMapper<CrmLead> {

    /** 交接：按交出人取待交接 ID（跨数据范围，必须忽略行级权限） */
    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_lead where owner_user_id = #{uid} and is_deleted = 0 "
            + "and status <> 'CONVERTED' order by id limit #{limit} offset #{offset}")
    List<Long> selectTransferIds(@Param("uid") Long uid, @Param("offset") int offset, @Param("limit") int limit);

    @InterceptorIgnore(dataPermission = "1")
    @Select("select count(1) from crm_lead where owner_user_id = #{uid} and is_deleted = 0 and status <> 'CONVERTED'")
    long countTransfer(@Param("uid") Long uid);

    /** 查重：手机号/邮箱哈希在全库范围查（否则跨组织重复查不出来） */
    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_lead where mobile_hash = #{hash} and is_deleted = 0 limit 5")
    List<Long> selectIdsByMobileHash(@Param("hash") String hash);

    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_lead where email_hash = #{hash} and is_deleted = 0 limit 5")
    List<Long> selectIdsByEmailHash(@Param("hash") String hash);

    /** 转化前置行锁：同一线索并发转化时串行化，配合唯一索引双保险 */
    @Select("select * from crm_lead where id = #{id} and is_deleted = 0 for update")
    CrmLead selectForUpdate(@Param("id") Long id);
}
