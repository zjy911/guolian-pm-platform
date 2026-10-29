package com.guolian.pm;

import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.pms.common.dto.PmDtos;
import com.guolian.pm.pms.form.entity.PmFormInstance;
import com.guolian.pm.pms.form.mapper.PmFormInstanceMapper;
import com.guolian.pm.pms.form.service.FormInstanceService;
import com.guolian.pm.pms.product.entity.PmProduct;
import com.guolian.pm.pms.product.mapper.PmProductMapper;
import com.guolian.pm.pms.project.entity.PmProject;
import com.guolian.pm.pms.project.mapper.PmProjectMapper;
import com.guolian.pm.pms.project.mapper.PmProjectMemberMapper;
import com.guolian.pm.pms.project.mapper.PmProjectTaskMapper;
import com.guolian.pm.pms.project.service.ProjectMemberService;
import com.guolian.pm.pms.project.service.ProjectService;
import com.guolian.pm.system.user.entity.SysUser;
import com.guolian.pm.system.user.mapper.SysUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.common.web.LoginUser;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 项目管理模块集成测试：模板实例化 / 阻塞推进 / 投入比例 / 认证校验（AC-03/05/07/08）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PmIntegrationTest {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private ProjectMemberService memberService;
    @Autowired
    private FormInstanceService formInstanceService;
    @Autowired
    private PmProjectMapper projectMapper;
    @Autowired
    private PmProjectTaskMapper taskMapper;
    @Autowired
    private PmProjectMemberMapper memberMapper;
    @Autowired
    private PmProductMapper productMapper;
    @Autowired
    private PmFormInstanceMapper formMapper;
    @Autowired
    private SysUserMapper userMapper;

    @org.junit.jupiter.api.BeforeEach
    void loginAsAdmin() {
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setUsername("admin");
        user.setRealName("平台管理员");
        user.setRoleId(101L);
        user.setRoleCode("ROLE_GROUP_ADMIN");
        user.setOrgId(1L);
        user.setDataScope("ALL");
        user.setScopeOrgIds(java.util.List.of(1L));
        AuthContext.set(user);
    }

    @org.junit.jupiter.api.AfterEach
    void clearContext() {
        AuthContext.clear();
    }

    @Test
    @DisplayName("AC-03 立项审批通过后按模板实例化 6 环节任务并进入筹备")
    void instantiateOnApproval() {
        PmDtos.InitiationRequest request = new PmDtos.InitiationRequest();
        request.setProductId(1L);
        request.setProjectName("集成测试-诉源治理项目");
        request.setCustomerId(1003L);
        request.setCustomerName("北京市政务服务管理局");
        request.setEstimatedAmount(new BigDecimal("3600000"));
        request.setAnnualCases(9000);
        Map<String, Long> created = projectService.createInitiation(request);
        Long projectId = created.get("projectId");
        Long formId = created.get("formId");

        PmProject project = projectMapper.selectById(projectId);
        assertThat(project.getStatus()).isEqualTo("INITIATING");
        assertThat(project.getProjectCode()).startsWith("PRJ-G-");

        // 模拟审批通过回调
        formInstanceService.onFlowFinished(formId, true, null);

        PmFormInstance form = formMapper.selectById(formId);
        assertThat(form.getStatus()).isEqualTo("EFFECTIVE");
        PmProject after = projectMapper.selectById(projectId);
        assertThat(after.getStatus()).isEqualTo("PREPARING");
        assertThat(after.getPhase()).isEqualTo(2);
        List<?> tasks = taskMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<com.guolian.pm.pms.project.entity.PmProjectTask>lambdaQuery()
                        .eq(com.guolian.pm.pms.project.entity.PmProjectTask::getProjectId, projectId));
        assertThat(tasks).isNotEmpty();
        // 模板含 6 个环节
        long phases = tasks.stream()
                .map(t -> ((com.guolian.pm.pms.project.entity.PmProjectTask) t).getPhase())
                .distinct().count();
        assertThat(phases).isEqualTo(6);
    }

    @Test
    @DisplayName("AC-05 阻塞任务未完成时禁止推进环节")
    void advanceBlockedByBlockingTask() {
        PmProject project = projectMapper.selectById(2L); // 筹备期，预算单 APPROVING（阻塞）
        assertThat(project.getPhase()).isEqualTo(2);
        assertThatThrownBy(() -> projectService.advancePhase(2L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("阻塞");
    }

    @Test
    @DisplayName("AC-07 投入比例累计超 100% 阻断并列冲突项目")
    void inputRatioBlocked() {
        // 张伟（id=2）在办合计 60%
        PmDtos.MemberAssignRequest request = new PmDtos.MemberAssignRequest();
        request.setProjectId(3L);
        request.setProjectRole("QC");
        request.setInputRatio(50);
        assertThatThrownBy(() -> memberService.assign(2L, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("100%")
                .hasMessageContaining("冲突项目");
    }

    @Test
    @DisplayName("AC-08 未认证人员不可承担执行类角色")
    void executionRoleRequiresCertification() {
        // 刘洋（id=5）种子未认证
        PmDtos.MemberAssignRequest request = new PmDtos.MemberAssignRequest();
        request.setProjectId(3L);
        request.setProjectRole("FIELD_MEDIATOR");
        request.setInputRatio(20);
        assertThatThrownBy(() -> memberService.assign(5L, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("上岗认证");

        // 认证通过后可指派
        SysUser user = userMapper.selectById(5L);
        SysUser update = new SysUser();
        update.setId(5L);
        update.setCertStatus("CERTIFIED");
        update.setCertItem("调解员上岗认证");
        update.setCertExpireDate(java.time.LocalDate.now().plusYears(1));
        userMapper.updateById(update);
        try {
            Long memberId = memberService.assign(5L, request);
            assertThat(memberId).isNotNull();
            assertThat(memberService.activeRatioOf(5L)).isGreaterThan(0);
        } finally {
            SysUser restore = new SysUser();
            restore.setId(5L);
            restore.setCertStatus("NONE");
            restore.setCertItem(null);
            userMapper.updateById(restore);
        }
    }

    @Test
    @DisplayName("用户关联项目双向可查（R-09）")
    void userProjectBidirectional() {
        List<java.util.Map<String, Object>> projects = memberService.projectsOf(2L);
        assertThat(projects).extracting(p -> p.get("projectId")).contains(1L, 2L);
        assertThat(memberService.activeRatioOf(2L)).isEqualTo(60);
    }
}
