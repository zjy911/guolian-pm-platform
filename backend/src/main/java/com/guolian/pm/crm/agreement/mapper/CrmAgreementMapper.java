package com.guolian.pm.crm.agreement.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.crm.agreement.entity.CrmAgreement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CrmAgreementMapper extends BaseMapper<CrmAgreement> {

    @Update("update crm_agreement set status = 'EFFECTIVE', activate_time = now() "
            + "where id = #{id} and status = 'APPROVING' and is_deleted = 0")
    int activate(@Param("id") Long id);

    @Update("update crm_agreement set status = 'DRAFT', flow_instance_id = null "
            + "where id = #{id} and status = 'APPROVING' and is_deleted = 0")
    int backToDraft(@Param("id") Long id);

    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_agreement where owner_user_id = #{uid} and is_deleted = 0 "
            + "and status <> 'EXPIRED' order by id limit #{limit} offset #{offset}")
    List<Long> selectTransferIds(@Param("uid") Long uid, @Param("offset") int offset, @Param("limit") int limit);

    @InterceptorIgnore(dataPermission = "1")
    @Select("select count(1) from crm_agreement where owner_user_id = #{uid} and is_deleted = 0 "
            + "and status <> 'EXPIRED'")
    long countTransfer(@Param("uid") Long uid);

    /** 到期扫描：end_date 为空的长期协议必须排除 */
    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_agreement where is_deleted = 0 and status = 'EFFECTIVE' "
            + "and end_date is not null and end_date < #{today} limit #{limit}")
    List<Long> selectExpiredIds(@Param("today") LocalDate today, @Param("limit") int limit);
}
