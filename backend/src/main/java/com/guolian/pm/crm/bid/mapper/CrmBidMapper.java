package com.guolian.pm.crm.bid.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.crm.bid.entity.CrmBid;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CrmBidMapper extends BaseMapper<CrmBid> {

    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_bid where owner_user_id = #{uid} and is_deleted = 0 "
            + "and bid_status not in ('WON','LOST','ABANDONED') order by id limit #{limit} offset #{offset}")
    List<Long> selectTransferIds(@Param("uid") Long uid, @Param("offset") int offset, @Param("limit") int limit);

    @InterceptorIgnore(dataPermission = "1")
    @Select("select count(1) from crm_bid where owner_user_id = #{uid} and is_deleted = 0 "
            + "and bid_status not in ('WON','LOST','ABANDONED')")
    long countTransfer(@Param("uid") Long uid);

    /** 开标提醒：未来 N 天内开标 */
    @InterceptorIgnore(dataPermission = "1")
    @Select("select * from crm_bid where is_deleted = 0 and bid_status in ('TRACKING','REGISTERED','SUBMITTED') "
            + "and open_date between #{from} and #{to}")
    List<CrmBid> selectOpeningSoon(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** 开标日历：按月取（走数据权限） */
    @Select("select * from crm_bid where is_deleted = 0 and open_date between #{from} and #{to} order by open_date")
    List<CrmBid> selectCalendar(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
