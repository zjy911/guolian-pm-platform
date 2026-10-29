package com.guolian.pm.pms.common.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.pms.project.entity.PmProject;
import com.guolian.pm.pms.project.mapper.PmProjectMapper;
import com.guolian.pm.pms.project.mapper.PmProjectTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目驾驶舱（只读分析态）：环节分布 / 健康度灯分布 / 金额汇总 / 预警清单（PRD 7.5 驾驶舱）。
 */
@Service
@RequiredArgsConstructor
public class CockpitService {

    private static final String[] PHASE_NAMES = {
            "项目立项", "项目筹备", "项目实施", "项目交接", "项目运营", "项目撤场"
    };

    private final PmProjectMapper projectMapper;
    private final PmProjectTaskMapper taskMapper;

    public Map<String, Object> summary() {
        List<PmProject> all = projectMapper.selectList(Wrappers.<PmProject>lambdaQuery().eq(PmProject::getIsDeleted, 0));
        Map<String, Object> result = new HashMap<>();
        result.put("total", all.size());

        int[] byPhase = new int[6];
        Map<String, Integer> byLight = new HashMap<>();
        byLight.put("GREEN", 0);
        byLight.put("YELLOW", 0);
        byLight.put("RED", 0);
        double amount = 0;
        double healthSum = 0;
        int healthCount = 0;
        for (PmProject p : all) {
            if (p.getPhase() != null && p.getPhase() >= 1 && p.getPhase() <= 6) {
                byPhase[p.getPhase() - 1]++;
            }
            if (p.getHealthLight() != null) {
                byLight.merge(p.getHealthLight(), 1, Integer::sum);
            }
            if (p.getEstimatedAmount() != null) {
                amount += p.getEstimatedAmount().doubleValue();
            }
            if (p.getHealthScore() != null && p.getHealthScore().doubleValue() > 0) {
                healthSum += p.getHealthScore().doubleValue();
                healthCount++;
            }
        }
        result.put("byPhase", byPhase);
        result.put("phaseNames", PHASE_NAMES);
        result.put("byLight", byLight);
        result.put("estimatedAmountTotal", amount);
        result.put("avgHealth", healthCount == 0 ? 0 : Math.round(healthSum / healthCount * 100.0) / 100.0);
        // 红黄灯预警清单
        result.put("alerts", all.stream()
                .filter(p -> "RED".equals(p.getHealthLight()) || "YELLOW".equals(p.getHealthLight()))
                .sorted((a, b) -> Double.compare(
                        a.getHealthScore() == null ? 0 : a.getHealthScore().doubleValue(),
                        b.getHealthScore() == null ? 0 : b.getHealthScore().doubleValue()))
                .limit(10).toList());
        result.put("openTaskCount", taskMapper.selectCount(Wrappers.<com.guolian.pm.pms.project.entity.PmProjectTask>lambdaQuery()
                .ne(com.guolian.pm.pms.project.entity.PmProjectTask::getStatus, "DONE")));
        return result;
    }
}
