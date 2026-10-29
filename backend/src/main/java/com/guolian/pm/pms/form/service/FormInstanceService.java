package com.guolian.pm.pms.form.service;

import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.flow.dto.FlowDtos;
import com.guolian.pm.flow.service.FlowService;
import com.guolian.pm.pms.form.entity.PmFormInstance;
import com.guolian.pm.pms.form.mapper.PmFormInstanceMapper;
import com.guolian.pm.pms.project.entity.PmProject;
import com.guolian.pm.pms.project.entity.PmProjectTask;
import com.guolian.pm.pms.project.mapper.PmProjectMapper;
import com.guolian.pm.pms.project.mapper.PmProjectTaskMapper;
import com.guolian.pm.pms.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 表单任务通用引擎：38 类表单对象共用一张实例表，扩展字段 JSON 存储（PRD 七）。
 * 提交走底座 Flowable 审批：立项单走 PM_INITIATION（金额条件分支），其余走 PM_FORM。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormInstanceService {

    public static final String FORM_INITIATION = "pm_form_initiation";

    private final PmFormInstanceMapper formMapper;
    private final PmProjectTaskMapper taskMapper;
    private final PmProjectMapper projectMapper;
    private final ProjectService projectService;
    private final FlowService flowService;
    private final CrmOwnerSupport owner;

    /** 按任务取最新实例（无则新建草稿） */
    @Transactional
    public PmFormInstance draftForTask(Long taskId) {
        PmProjectTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw BizException.notFound("任务不存在");
        }
        PmFormInstance latest = formMapper.selectLatestByTask(taskId);
        if (latest != null && !"REJECTED".equals(latest.getStatus())) {
            return latest;
        }
        PmFormInstance form = new PmFormInstance();
        form.setProjectId(task.getProjectId());
        form.setTaskId(taskId);
        form.setFormCode(task.getFormCode());
        form.setFormData(latest != null ? latest.getFormData() : "{}");
        form.setStatus("DRAFT");
        formMapper.insert(form);
        return form;
    }

    /** 暂存：仅草稿/驳回可改 */
    @Transactional
    public void saveDraft(Long formId, String formData) {
        PmFormInstance form = requireForm(formId);
        if (!"DRAFT".equals(form.getStatus()) && !"REJECTED".equals(form.getStatus())) {
            throw BizException.biz("当前状态不可修改（" + statusLabel(form.getStatus()) + "）");
        }
        PmFormInstance update = new PmFormInstance();
        update.setId(formId);
        update.setFormData(formData);
        if ("REJECTED".equals(form.getStatus())) {
            update.setStatus("DRAFT");
        }
        formMapper.updateById(update);
    }

    /** 提交审批：状态置 APPROVING，任务联动 APPROVING，启动流程 */
    @Transactional
    public String submit(Long formId) {
        PmFormInstance form = requireForm(formId);
        if (!"DRAFT".equals(form.getStatus()) && !"REJECTED".equals(form.getStatus())) {
            throw BizException.biz("当前状态不可提交（" + statusLabel(form.getStatus()) + "）");
        }
        if (!StringUtils.hasText(form.getFormData()) || "{}".equals(form.getFormData().trim())) {
            throw BizException.biz("表单内容为空，无法提交");
        }
        boolean initiation = FORM_INITIATION.equals(form.getFormCode());
        PmProject project = form.getProjectId() == null ? null : projectMapper.selectById(form.getProjectId());
        FlowDtos.StartRequest start = new FlowDtos.StartRequest();
        start.setBizType(initiation ? "PM_INITIATION" : "PM_FORM");
        start.setBizId(form.getId());
        start.setBizNo(project == null ? null : project.getProjectCode());
        start.setBizTitle((project == null ? form.getFormCode() : project.getProjectName())
                + " · " + displayName(form.getFormCode()));
        start.setDetailRoute("/project/workspace/" + (project == null ? "" : project.getId()));
        if (project != null && project.getEstimatedAmount() != null) {
            start.getVariables().put("estimatedAmount", project.getEstimatedAmount());
            start.getVariables().put("estimatedAmountWan", project.getEstimatedAmount()
                    .divide(new java.math.BigDecimal("10000"), 2, java.math.RoundingMode.HALF_UP));
        }
        start.getVariables().put("formCode", form.getFormCode());
        String instanceId = flowService.start(start, owner.current());

        PmFormInstance update = new PmFormInstance();
        update.setId(formId);
        update.setStatus("APPROVING");
        update.setFlowInstanceId(instanceId);
        update.setSubmitTime(LocalDateTime.now());
        formMapper.updateById(update);
        if (form.getTaskId() != null) {
            projectService.updateTaskStatus(form.getTaskId(), "APPROVING");
        }
        if (project != null) {
            projectService.log(project.getId(), "FORM_SUBMITTED",
                    displayName(form.getFormCode()) + " 提交审批");
        }
        return instanceId;
    }

    /** 审批回调（PM_INITIATION / PM_FORM） */
    @Transactional
    public void onFlowFinished(Long formId, boolean approved, String comment) {
        PmFormInstance form = formMapper.selectById(formId);
        if (form == null) {
            log.warn("PM 表单审批回调：实例不存在 formId={}", formId);
            return;
        }
        PmFormInstance update = new PmFormInstance();
        update.setId(formId);
        update.setApproveTime(LocalDateTime.now());
        update.setApproveComment(comment);
        if (approved) {
            update.setStatus("EFFECTIVE");
            formMapper.updateById(update);
            if (form.getTaskId() != null) {
                projectService.updateTaskStatus(form.getTaskId(), "DONE");
            }
            afterApproved(form);
        } else {
            update.setStatus("REJECTED");
            formMapper.updateById(update);
            if (form.getTaskId() != null) {
                projectService.updateTaskStatus(form.getTaskId(), "REJECTED");
            }
        }
        if (form.getProjectId() != null) {
            projectService.log(form.getProjectId(), approved ? "FORM_APPROVED" : "FORM_REJECTED",
                    displayName(form.getFormCode()) + (approved ? " 审批通过" : " 审批驳回")
                            + (StringUtils.hasText(comment) ? "：" + comment : ""));
        }
    }

    /** 审批通过后的业务联动 */
    private void afterApproved(PmFormInstance form) {
        try {
            switch (form.getFormCode() == null ? "" : form.getFormCode()) {
                case FORM_INITIATION -> {
                    if (form.getProjectId() != null) {
                        projectService.onInitiationApproved(form.getProjectId());
                    }
                }
                case "pm_form_budget" -> projectService.log(form.getProjectId(), "BUDGET_BASELINE", "预算基线锁定");
                case "pm_form_health_scorecard" -> projectService.applyHealthScorecard(
                        form.getProjectId(), parseData(form.getFormData()));
                default -> {
                    // 其余表单通过即闭环，无额外联动
                }
            }
        } catch (Exception e) {
            log.error("表单审批联动失败 formId={} code={}", form.getId(), form.getFormCode(), e);
        }
    }

    private Map<String, Object> parseData(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json == null ? "{}" : json, new com.fasterxml.jackson.core.type.TypeReference<>() {
                    });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private PmFormInstance requireForm(Long formId) {
        PmFormInstance form = formMapper.selectById(formId);
        if (form == null) {
            throw BizException.notFound("表单实例不存在");
        }
        return form;
    }

    public String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "DRAFT" -> "草稿";
            case "APPROVING" -> "审批中";
            case "APPROVED", "EFFECTIVE" -> "已生效";
            case "REJECTED" -> "已驳回";
            default -> status == null ? "" : status;
        };
    }

    /** 38 类对象编码 → 中文名（前端容器标题用） */
    public static String displayName(String formCode) {
        return switch (formCode == null ? "" : formCode) {
            case "pm_form_initiation" -> "立项申请单";
            case "pm_form_init_change" -> "立项变更单";
            case "pm_form_budget" -> "项目预算单";
            case "pm_form_headcount" -> "编制申请单";
            case "pm_form_resource_apply" -> "资源申请单";
            case "pm_form_target_responsibility" -> "目标责任书";
            case "pm_form_project_plan" -> "项目计划单";
            case "pm_form_site_entry" -> "进场确认单";
            case "pm_form_case_dispatch" -> "分案机制确认单";
            case "pm_form_staff_certification" -> "人员上岗认证单";
            case "pm_form_staff_transfer" -> "人员任命/调拨单";
            case "pm_form_acceptance" -> "验收单";
            case "pm_form_rectification" -> "整改/复验单";
            case "pm_form_project_handover" -> "项目交接单";
            case "pm_form_debrief_report" -> "复盘报告";
            case "pm_form_ramp_up" -> "爬坡期跟踪";
            case "pm_form_project_grade" -> "项目分级";
            case "pm_form_payment_ledger" -> "回款台账";
            case "pm_form_quality_inspection" -> "质量抽检与投诉";
            case "pm_form_compliance_check" -> "合规检查";
            case "pm_form_client_satisfaction" -> "客户满意度";
            case "pm_form_health_scorecard" -> "健康度评分卡";
            case "pm_form_closing_apply" -> "撤场申请单";
            case "pm_form_closing_plan" -> "撤场方案";
            case "pm_form_case_file_transfer" -> "卷宗移交清单";
            case "pm_form_asset_disposal" -> "资产处置单";
            case "pm_form_final_settlement" -> "撤场清算表";
            case "pm_form_archive_seal" -> "档案封存";
            case "pm_form_knowledge_asset" -> "案例/知识资产";
            case "pm_form_tf_task" -> "TF 专项任务";
            case "pm_form_metric_dictionary" -> "指标字典";
            case "pm_form_product_spec" -> "产品管理";
            case "pm_form_project_scope_change" -> "项目变更单";
            case "pm_form_staff_movement" -> "人员异动";
            case "pm_form_meeting_record" -> "项目会议";
            case "pm_form_feedback_refinement" -> "经验回流";
            default -> formCode == null ? "" : formCode;
        };
    }
}
