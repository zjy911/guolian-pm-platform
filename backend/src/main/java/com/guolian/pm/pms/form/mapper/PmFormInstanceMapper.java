package com.guolian.pm.pms.form.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.pms.form.entity.PmFormInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PmFormInstanceMapper extends BaseMapper<PmFormInstance> {

    /** 任务当前表单实例（同一任务重复提交取最新一条） */
    @Select("SELECT * FROM pm_form_instance WHERE task_id = #{taskId} AND is_deleted = 0 "
            + "ORDER BY created_time DESC LIMIT 1")
    PmFormInstance selectLatestByTask(@Param("taskId") Long taskId);
}
