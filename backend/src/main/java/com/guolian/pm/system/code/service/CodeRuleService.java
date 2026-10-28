package com.guolian.pm.system.code.service;

import com.guolian.pm.system.code.entity.SysCodeRule;
import com.guolian.pm.system.code.mapper.SysCodeRuleMapper;
import com.guolian.pm.common.exception.BizException;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 统一编号生成器（底座 P1 能力）。
 *
 * <p>业务模块只注册规则，不在代码里拼字符串。流水号通过条件更新原子自增，
 * 多实例并发下不会生成重号。
 */
@Service
@RequiredArgsConstructor
public class CodeRuleService {

    private final SysCodeRuleMapper codeRuleMapper;

    @Transactional
    public String generate(String ruleCode) {
        SysCodeRule rule = codeRuleMapper.selectOne(Wrappers.<SysCodeRule>lambdaQuery()
                .eq(SysCodeRule::getRuleCode, ruleCode).last("limit 1"));
        if (rule == null) {
            throw BizException.biz("编码规则未注册：" + ruleCode);
        }
        String period = period(rule);
        int updated = codeRuleMapper.increase(ruleCode, period);
        if (updated == 0) {
            // 周期切换或首次生成：重置流水
            codeRuleMapper.reset(ruleCode, period);
        }
        SysCodeRule latest = codeRuleMapper.selectById(rule.getId());
        int seq = latest.getCurrentSeq() == null ? 1 : latest.getCurrentSeq();
        String no = build(rule, period, seq);
        codeRuleMapper.updateSample(rule.getId(), no);
        return no;
    }

    private String build(SysCodeRule rule, String period, int seq) {
        StringBuilder sb = new StringBuilder();
        if (rule.getPrefix() != null) {
            sb.append(rule.getPrefix());
        }
        if (period != null && !period.isEmpty()) {
            sb.append(period).append('-');
        }
        String seqStr = String.valueOf(seq);
        int len = rule.getSeqLength() == null ? 4 : rule.getSeqLength();
        sb.append("0".repeat(Math.max(0, len - seqStr.length()))).append(seqStr);
        return sb.toString();
    }

    private String period(SysCodeRule rule) {
        String cycle = rule.getResetCycle() == null ? "MONTH" : rule.getResetCycle();
        String pattern = rule.getDateFormat();
        LocalDate now = LocalDate.now();
        if ("NONE".equals(cycle)) {
            return "";
        }
        if (pattern == null || pattern.isEmpty()) {
            pattern = switch (cycle) {
                case "DAY" -> "yyyyMMdd";
                case "YEAR" -> "yyyy";
                default -> "yyyyMM";
            };
        }
        return now.format(DateTimeFormatter.ofPattern(pattern));
    }
}
