package com.guolian.pm.crm.activity.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.crm.activity.entity.CrmActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CrmActivityMapper extends BaseMapper<CrmActivity> {

    /** 目标对象最近活动时间（写入后用同一事务刷新快照） */
    @Select("select max(start_time) from crm_activity where target_type = #{targetType} "
            + "and target_id = #{targetId} and status = 'VALID' and is_deleted = 0")
    LocalDateTime maxStartTime(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    /** 任务型活动到期未完成 → 生成底座待办，全库扫描 */
    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_activity where is_deleted = 0 and status = 'VALID' and type = 'TASK' "
            + "and end_time < #{now} limit #{limit}")
    List<Long> selectOverdueTasks(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
