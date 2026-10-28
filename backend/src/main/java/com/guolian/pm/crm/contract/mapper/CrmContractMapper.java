package com.guolian.pm.crm.contract.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.crm.contract.entity.CrmContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CrmContractMapper extends BaseMapper<CrmContract> {

    /** 激活：条件更新保证幂等（重复回调不产生副作用） */
    @Update("update crm_contract set status = 'ACTIVE', activate_time = now() "
            + "where id = #{id} and status = 'APPROVING' and is_deleted = 0")
    int activate(@Param("id") Long id);

    /** 驳回/撤回回草稿 */
    @Update("update crm_contract set status = 'DRAFT', flow_instance_id = null "
            + "where id = #{id} and status = 'APPROVING' and is_deleted = 0")
    int backToDraft(@Param("id") Long id);

    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_contract where owner_user_id = #{uid} and is_deleted = 0 "
            + "and status <> 'TERMINATED' order by id limit #{limit} offset #{offset}")
    List<Long> selectTransferIds(@Param("uid") Long uid, @Param("offset") int offset, @Param("limit") int limit);

    @InterceptorIgnore(dataPermission = "1")
    @Select("select count(1) from crm_contract where owner_user_id = #{uid} and is_deleted = 0 "
            + "and status <> 'TERMINATED'")
    long countTransfer(@Param("uid") Long uid);

    /** 到期扫描：ACTIVE 且 end_date 已过 */
    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_contract where is_deleted = 0 and status = 'ACTIVE' "
            + "and end_date is not null and end_date < #{today} limit #{limit}")
    List<Long> selectExpiredIds(@Param("today") LocalDate today, @Param("limit") int limit);

    /** 到期预警（供看板与提醒任务） */
    @Select("select * from crm_contract where is_deleted = 0 and status = 'ACTIVE' "
            + "and end_date between #{today} and #{until} order by end_date")
    List<CrmContract> selectExpiring(@Param("today") LocalDate today, @Param("until") LocalDate until);
}
