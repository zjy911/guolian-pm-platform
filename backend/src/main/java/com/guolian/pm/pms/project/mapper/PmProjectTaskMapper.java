package com.guolian.pm.pms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.pms.project.entity.PmProjectTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PmProjectTaskMapper extends BaseMapper<PmProjectTask> {

    /** 某阶段未完成的阻塞任务（R-08：阻塞则禁止推进） */
    @Select("SELECT * FROM pm_project_task WHERE project_id = #{projectId} AND phase = #{phase} "
            + "AND blocking = 1 AND status NOT IN ('DONE') AND is_deleted = 0 ORDER BY sort_no")
    List<PmProjectTask> selectBlockingIncomplete(@Param("projectId") Long projectId, @Param("phase") Integer phase);

    @Select("SELECT COUNT(1) FROM pm_project_task WHERE assignee_id = #{userId} "
            + "AND status NOT IN ('DONE') AND is_deleted = 0")
    int countOpenByAssignee(@Param("userId") Long userId);

    @Select("SELECT t.* FROM pm_project_task t JOIN pm_project p ON p.id = t.project_id "
            + "JOIN pm_project_member m ON m.project_id = p.id AND m.user_id = #{userId} "
            + "WHERE t.status NOT IN ('DONE') AND t.is_deleted = 0 AND m.status = 'ACTIVE' ORDER BY t.due_date LIMIT 50")
    List<PmProjectTask> selectOpenByMember(@Param("userId") Long userId);
}
