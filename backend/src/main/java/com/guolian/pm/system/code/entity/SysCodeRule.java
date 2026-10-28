package com.guolian.pm.system.code.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 编码规则（底座 P1 能力）：统一编号生成器，业务模块注册自己的规则即可。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_code_rule")
public class SysCodeRule extends BaseEntity {

    private String ruleCode;
    private String ruleName;
    /** 前缀，如 HT- */
    private String prefix;
    /** 日期段格式，如 yyyyMM */
    private String dateFormat;
    /** 流水号位数 */
    private Integer seqLength;
    /** 流水重置周期：NONE / DAY / MONTH / YEAR */
    private String resetCycle;
    private Integer currentSeq;
    private String currentPeriod;
    private String sample;
    private Integer status;
    private String remark;
}
