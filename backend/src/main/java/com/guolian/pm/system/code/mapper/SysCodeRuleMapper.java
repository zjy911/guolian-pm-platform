package com.guolian.pm.system.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.system.code.entity.SysCodeRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysCodeRuleMapper extends BaseMapper<SysCodeRule> {

    /** 同周期内原子自增；返回 0 表示周期已切换，需要重置 */
    @Update("update sys_code_rule set current_seq = current_seq + 1 "
            + "where rule_code = #{code} and current_period = #{period} and is_deleted = 0")
    int increase(@Param("code") String code, @Param("period") String period);

    @Update("update sys_code_rule set current_seq = 1, current_period = #{period} "
            + "where rule_code = #{code} and is_deleted = 0")
    int reset(@Param("code") String code, @Param("period") String period);

    @Update("update sys_code_rule set sample = #{sample} where id = #{id}")
    int updateSample(@Param("id") Long id, @Param("sample") String sample);
}
