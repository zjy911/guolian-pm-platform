package com.guolian.pm.crm.opportunity.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.crm.opportunity.entity.CrmOpportunity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CrmOpportunityMapper extends BaseMapper<CrmOpportunity> {

    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_opportunity where owner_user_id = #{uid} and is_deleted = 0 "
            + "order by id limit #{limit} offset #{offset}")
    List<Long> selectTransferIds(@Param("uid") Long uid, @Param("offset") int offset, @Param("limit") int limit);

    @InterceptorIgnore(dataPermission = "1")
    @Select("select count(1) from crm_opportunity where owner_user_id = #{uid} and is_deleted = 0")
    long countTransfer(@Param("uid") Long uid);

    /**
     * 赢单反推：条件更新保证幂等，影响行数为 0 即已成交，直接返回。
     */
    @Update("update crm_opportunity set is_won = 1, sales_stage = 'WON', contract_id = #{contractId}, "
            + "stage_change_time = now() where id = #{id} and is_won = 0 and is_deleted = 0")
    int markWon(@Param("id") Long id, @Param("contractId") Long contractId);

    /** 续签等其他合同：仅记录关联，不改状态 */
    @Update("update crm_opportunity set contract_id = #{contractId} where id = #{id} and is_won = 1 and is_deleted = 0")
    int linkContract(@Param("id") Long id, @Param("contractId") Long contractId);

    /** 漏斗聚合（走数据权限） */
    @Select("select sales_stage as stage, count(1) as cnt, coalesce(sum(coalesce(est_revenue, expected_amount, 0)), 0) as amount "
            + "from crm_opportunity where is_deleted = 0 group by sales_stage")
    List<java.util.Map<String, Object>> funnel();

    /** 客户关联商机数 */
    @Select("select count(1) from crm_opportunity where account_id = #{accountId} and is_deleted = 0")
    long countByAccount(@Param("accountId") Long accountId);
}
