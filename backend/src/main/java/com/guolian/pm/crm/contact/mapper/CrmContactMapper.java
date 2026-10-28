package com.guolian.pm.crm.contact.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.crm.contact.entity.CrmContact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmContactMapper extends BaseMapper<CrmContact> {

    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_contact where owner_user_id = #{uid} and is_deleted = 0 "
            + "order by id limit #{limit} offset #{offset}")
    List<Long> selectTransferIds(@Param("uid") Long uid, @Param("offset") int offset, @Param("limit") int limit);

    @InterceptorIgnore(dataPermission = "1")
    @Select("select count(1) from crm_contact where owner_user_id = #{uid} and is_deleted = 0")
    long countTransfer(@Param("uid") Long uid);

    /** 同客户下「同名 + 同手机号」重复校验 */
    @InterceptorIgnore(dataPermission = "1")
    @Select("select count(1) from crm_contact where account_id = #{accountId} and name = #{name} "
            + "and mobile_hash = #{hash} and is_deleted = 0")
    long countSameInAccount(@Param("accountId") Long accountId, @Param("name") String name,
                            @Param("hash") String hash);

    /** 生日关怀：按生日月日筛选（跨范围，任务态） */
    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_contact where is_deleted = 0 "
            + "and month(birthday) = #{month} and day(birthday) = #{day}")
    List<Long> selectByBirthday(@Param("month") int month, @Param("day") int day);
}
