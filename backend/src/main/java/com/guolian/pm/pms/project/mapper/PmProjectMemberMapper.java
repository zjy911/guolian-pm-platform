package com.guolian.pm.pms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.pms.project.entity.PmProjectMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PmProjectMemberMapper extends BaseMapper<PmProjectMember> {

    /** 同一人在办项目投入比例合计（不含本人、不含已释放） */
    @Select("SELECT COALESCE(SUM(input_ratio), 0) FROM pm_project_member "
            + "WHERE user_id = #{userId} AND status = 'ACTIVE' AND is_deleted = 0 "
            + "AND (#{excludeMemberId} IS NULL OR id <> #{excludeMemberId})")
    Integer sumActiveRatio(@Param("userId") Long userId, @Param("excludeMemberId") Long excludeMemberId);

    /** 投入比例冲突项目清单（R：超出 100% 时阻断并提示冲突项目） */
    @Select("SELECT m.* FROM pm_project_member m WHERE m.user_id = #{userId} AND m.status = 'ACTIVE' AND m.is_deleted = 0")
    List<PmProjectMember> selectActiveByUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM pm_project_member WHERE project_id = #{projectId} AND user_id = #{userId} AND status = 'ACTIVE' AND is_deleted = 0")
    int countActive(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
