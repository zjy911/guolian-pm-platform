package com.guolian.pm.pms.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guolian.pm.common.api.PageQuery;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.pms.common.dto.PmDtos;
import com.guolian.pm.pms.form.entity.PmFormInstance;
import com.guolian.pm.pms.form.mapper.PmFormInstanceMapper;
import com.guolian.pm.pms.product.entity.PmProduct;
import com.guolian.pm.pms.product.entity.PmTaskTemplate;
import com.guolian.pm.pms.product.mapper.PmProductMapper;
import com.guolian.pm.pms.product.service.ProductService;
import com.guolian.pm.pms.project.entity.PmProject;
import com.guolian.pm.pms.project.entity.PmProjectLog;
import com.guolian.pm.pms.project.entity.PmProjectTask;
import com.guolian.pm.pms.project.mapper.PmProjectLogMapper;
import com.guolian.pm.pms.project.mapper.PmProjectMapper;
import com.guolian.pm.pms.project.mapper.PmProjectTaskMapper;
import com.guolian.pm.system.code.service.CodeRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 项目运行态：立项建档、模板实例化、环节推进（阻塞校验）、健康度。
 *
 * <p>关键规则落点：
 * <ul>
 *   <li>R-01/R-02：6 环节固定串联，实例化时按模板快照生成全部任务（AC-03）</li>
 *   <li>R-08：阻塞任务未完成禁止推进环节（AC-05）</li>
 *   <li>R-05：审批中锁定由表单状态机承载</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final PmProjectMapper projectMapper;
    private final PmProjectTaskMapper taskMapper;
    private final PmProjectLogMapper logMapper;
    private final PmFormInstanceMapper formMapper;
    private final PmProductMapper productMapper;
    private final ProductService productService;
    private final CrmOwnerSupport owner;
    private final CodeRuleService codeRuleService;
    private final ObjectMapper objectMapper;

    private static final String[] PHASE_STATUS = {
            "INITIATING", "PREPARING", "IMPLEMENTING", "PENDING_HANDOVER", "OPERATING", "CLOSING"
    };
    private static final String[] PHASE_NAMES = {
            "项目立项", "项目筹备", "项目实施", "项目交接", "项目运营", "项目撤场"
    };

    public PageResult<PmProject> page(PageQuery query, String keyword, Integer phase, String status, String healthLight) {
        var wrapper = Wrappers.<PmProject>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w.like(PmProject::getProjectName, keyword)
                        .or().like(PmProject::getProjectCode, keyword)
                        .or().like(PmProject::getCustomerName, keyword))
                .eq(phase != null, PmProject::getPhase, phase)
                .eq(StringUtils.hasText(status), PmProject::getStatus, status)
                .eq(StringUtils.hasText(healthLight), PmProject::getHealthLight, healthLight)
                .orderByDesc(PmProject::getCreatedTime);
        Page<PmProject> page = projectMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), query.safePageNum(), query.safePageSize());
    }

    public PmProject detail(Long id) {
        PmProject project = projectMapper.selectById(id);
        if (project == null) {
            throw BizException.notFound("项目不存在");
        }
        return project;
    }

    /** 项目详情聚合：任务按环节分组 + 成员 + 表单 + 日志 */
    public Map<String, Object> workspace(Long id) {
        PmProject project = detail(id);
        List<PmProjectTask> tasks = taskMapper.selectList(Wrappers.<PmProjectTask>lambdaQuery()
                .eq(PmProjectTask::getProjectId, id).orderByAsc(PmProjectTask::getPhase)
                .orderByAsc(PmProjectTask::getSortNo));
        List<PmProjectLog> logs = logMapper.selectList(Wrappers.<PmProjectLog>lambdaQuery()
                .eq(PmProjectLog::getProjectId, id).orderByDesc(PmProjectLog::getCreatedTime).last("limit 50"));
        return Map.of(
                "project", project,
                "tasks", tasks,
                "logs", logs
        );
    }

    /** 新建立项申请：生成项目编号、建档（立项中）、挂立项表单草稿 */
    @Transactional
    public Map<String, Long> createInitiation(PmDtos.InitiationRequest request) {
        PmProduct product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw BizException.notFound("服务产品不存在");
        }
        PmTaskTemplate template = productService.latestTemplate(product.getId());
        if (template == null) {
            throw BizException.biz("产品「" + product.getProductName() + "」尚未发布任务模板，请先在产品管理中配置");
        }
        PmProject project = new PmProject();
        project.setProjectCode(codeRuleService.generate("PM_PROJECT"));
        project.setProjectName(request.getProjectName());
        project.setProductId(product.getId());
        project.setProductName(product.getProductName());
        project.setTemplateVersion(product.getTemplateVersion());
        project.setCustomerId(request.getCustomerId());
        project.setCustomerName(request.getCustomerName());
        project.setContractNo(request.getContractNo());
        project.setSignEntity(request.getSignEntity());
        project.setEstimatedAmount(request.getEstimatedAmount());
        project.setAnnualCases(request.getAnnualCases());
        project.setPricingMode(StringUtils.hasText(request.getPricingMode()) ? request.getPricingMode() : product.getPricingMode());
        project.setRating(request.getRating());
        project.setPhase(1);
        project.setStatus("INITIATING");
        project.setHealthScore(BigDecimal.ZERO);
        project.setHealthLight("GRAY");
        project.setOrgId(owner.createOrgId());
        project.setOwnerUserId(owner.currentUserId());
        project.setOwnerOrgId(owner.createOrgId());
        project.setOwnerUserName(owner.currentUserName());
        projectMapper.insert(project);

        PmFormInstance form = new PmFormInstance();
        form.setProjectId(project.getId());
        form.setFormCode("pm_form_initiation");
        form.setFormData(StringUtils.hasText(request.getFormData()) ? request.getFormData() : "{}");
        form.setStatus("DRAFT");
        form.setSubmitUserId(owner.currentUserId());
        form.setSubmitUserName(owner.currentUserName());
        formMapper.insert(form);

        PmProject link = new PmProject();
        link.setId(project.getId());
        link.setInitiationFormId(form.getId());
        projectMapper.updateById(link);

        log(project.getId(), "INITIATION_CREATED", "新建立项申请（" + project.getProjectCode() + "），待提交审批");
        return Map.of("projectId", project.getId(), "formId", form.getId());
    }

    /** 立项审批通过回调：按模板快照实例化 6 环节与全部任务（AC-03），进入筹备 */
    @Transactional
    public int onInitiationApproved(Long projectId) {
        PmProject project = detail(projectId);
        PmProduct product = productMapper.selectById(project.getProductId());
        PmTaskTemplate template = productService.templateFor(product, project.getTemplateVersion());
        if (template == null) {
            throw BizException.biz("产品模板缺失，无法实例化项目任务");
        }
        int sort = 0;
        int count = 0;
        try {
            List<Map<String, Object>> sections = objectMapper.readValue(
                    template.getTemplateJson(), new TypeReference<>() {
                    });
            for (Map<String, Object> section : sections) {
                Integer phase = ((Number) section.get("phase")).intValue();
                Object tasksObj = section.get("tasks");
                if (!(tasksObj instanceof List<?> tasks)) {
                    continue;
                }
                for (Object t : tasks) {
                    Map<String, Object> task = (Map<String, Object>) t;
                    PmProjectTask entity = new PmProjectTask();
                    entity.setProjectId(projectId);
                    entity.setPhase(phase);
                    entity.setCategory((String) section.get("category"));
                    entity.setTaskName((String) task.get("name"));
                    entity.setTaskType((String) task.getOrDefault("type", "FORM"));
                    entity.setFormCode((String) task.get("formCode"));
                    entity.setBlocking(Boolean.TRUE.equals(task.get("blocking")) ? 1 : 0);
                    entity.setStatus("NOT_STARTED");
                    entity.setAssigneeId(project.getOwnerUserId());
                    entity.setAssigneeName(project.getOwnerUserName());
                    entity.setSortNo(sort++);
                    taskMapper.insert(entity);
                    count++;
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("模板实例化失败 projectId={}", projectId, e);
            throw BizException.biz("任务模板解析失败，请联系管理员检查模板 JSON");
        }
        PmProject update = new PmProject();
        update.setId(projectId);
        update.setPhase(2);
        update.setStatus("PREPARING");
        update.setHealthScore(new BigDecimal("85.00"));
        update.setHealthLight("GREEN");
        projectMapper.updateById(update);
        log(projectId, "INITIATION_APPROVED",
                "立项审批通过，按模板 " + product.getTemplateVersion() + " 实例化 6 环节 " + count + " 项任务");
        return count;
    }

    /** 环节推进：阻塞任务未完成则阻断（R-08 / AC-05） */
    @Transactional
    public PmProject advancePhase(Long projectId) {
        PmProject project = detail(projectId);
        if (project.getPhase() == null || project.getPhase() < 1 || project.getPhase() >= 6) {
            throw BizException.biz("当前环节不可推进（撤场收口由撤场流程推进）");
        }
        List<PmProjectTask> blocking = taskMapper.selectBlockingIncomplete(projectId, project.getPhase());
        if (!blocking.isEmpty()) {
            List<String> names = blocking.stream().map(PmProjectTask::getTaskName).toList();
            throw BizException.biz("存在未完成的阻塞任务，禁止推进：" + String.join("、", names));
        }
        int next = project.getPhase() + 1;
        PmProject update = new PmProject();
        update.setId(projectId);
        update.setPhase(next);
        update.setStatus(PHASE_STATUS[next - 1]);
        projectMapper.updateById(update);
        log(projectId, "PHASE_ADVANCE", "项目阶段推进：" + PHASE_NAMES[project.getPhase() - 1] + " → " + PHASE_NAMES[next - 1]);
        return detail(projectId);
    }

    /** 环节完成度：某阶段 已完成任务数 / 总数 */
    public Map<String, Integer> phaseProgress(Long projectId, int phase) {
        List<PmProjectTask> tasks = taskMapper.selectList(Wrappers.<PmProjectTask>lambdaQuery()
                .eq(PmProjectTask::getProjectId, projectId).eq(PmProjectTask::getPhase, phase));
        int done = (int) tasks.stream().filter(t -> "DONE".equals(t.getStatus())).count();
        return Map.of("done", done, "total", tasks.size());
    }

    /** 健康度回写：来自健康度评分卡表单数据（五维加权，PRD 7.5） */
    @Transactional
    public void applyHealthScorecard(Long projectId, Map<String, Object> scores) {
        double biz = num(scores.get("business")) * 0.4;
        double quality = num(scores.get("quality")) * 0.2;
        double compliance = num(scores.get("compliance")) * 0.15;
        double client = num(scores.get("client")) * 0.15;
        double team = num(scores.get("team")) * 0.1;
        double total = biz + quality + compliance + client + team;
        PmProject update = new PmProject();
        update.setId(projectId);
        update.setHealthScore(BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP));
        update.setHealthLight(total >= 85 ? "GREEN" : total >= 70 ? "YELLOW" : "RED");
        projectMapper.updateById(update);
    }

    /** 任务状态流转（表单提交/审批回调联动） */
    @Transactional
    public void updateTaskStatus(Long taskId, String status) {
        PmProjectTask update = new PmProjectTask();
        update.setId(taskId);
        update.setStatus(status);
        if ("DONE".equals(status)) {
            update.setCompletedTime(LocalDateTime.now());
        }
        taskMapper.updateById(update);
    }

    public void log(Long projectId, String action, String detail) {
        PmProjectLog entry = new PmProjectLog();
        entry.setProjectId(projectId);
        entry.setAction(action);
        entry.setDetail(detail);
        entry.setOperatorId(owner.currentUserId());
        entry.setOperatorName(owner.currentUserName());
        entry.setCreatedTime(LocalDateTime.now());
        logMapper.insert(entry);
    }

    private double num(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s && StringUtils.hasText(s)) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}
