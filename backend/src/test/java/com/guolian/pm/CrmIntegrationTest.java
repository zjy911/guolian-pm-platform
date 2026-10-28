package com.guolian.pm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guolian.pm.crm.common.job.CrmJobScheduler;
import com.guolian.pm.system.job.entity.SysJob;
import com.guolian.pm.system.job.mapper.SysJobMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * CRM 模块集成测试：真实 H2 + 真实脱敏策略，不 mock 服务层。
 *
 * <p>重点覆盖三处易回归的地方：
 * 1. 金额类字段（BigDecimal）标了 @Sensitive 后仍能正常序列化；
 * 2. 线索转化幂等（重复提交返回既有结果，不重复建客户）；
 * 3. 未知路径返回 404 而不是伪装成 500。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CrmIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CrmJobScheduler scheduler;

    @Autowired
    private SysJobMapper jobMapper;

    private JsonNode call(String method, String url, String token, String body) throws Exception {
        var request = switch (method) {
            case "POST" -> org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url);
            case "PUT" -> org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url);
            default -> get(url);
        };
        if (!"GET".equals(method)) {
            request.contentType(MediaType.APPLICATION_JSON).content(body == null ? "{}" : body);
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String login() throws Exception {
        JsonNode node = call("POST", "/api/v1/auth/login", null,
                "{\"username\":\"admin\",\"password\":\"Admin@123\"}");
        assertThat(node.get("code").asInt()).withFailMessage(node.toString()).isZero();
        return node.get("data").get("token").asText();
    }

    @Test
    @DisplayName("CRM 列表接口：金额字段（BigDecimal + 脱敏）可正常序列化")
    void listEndpointsSerializeMoneyFields() throws Exception {
        String token = login();

        JsonNode accounts = call("GET", "/api/v1/crm/accounts?pageNum=1&pageSize=5", token, null);
        assertThat(accounts.get("code").asInt()).withFailMessage(accounts.toString()).isZero();
        assertThat(accounts.get("data").get("list").size()).isGreaterThan(0);

        // 演示数据里 annualRevenue 有值，且该字段标了 @Sensitive：必须仍是数值而不是报错/字符串通配
        boolean hasRevenue = false;
        for (JsonNode row : accounts.get("data").get("list")) {
            if (row.hasNonNull("annualRevenue") && row.get("annualRevenue").isNumber()) {
                hasRevenue = true;
                break;
            }
        }
        assertThat(hasRevenue).withFailMessage("annualRevenue 未被正确序列化：%s", accounts).isTrue();

        for (String url : new String[] {
                "/api/v1/crm/opportunities?pageNum=1&pageSize=5",
                "/api/v1/crm/contracts?pageNum=1&pageSize=5",
                "/api/v1/crm/agreements?pageNum=1&pageSize=5",
                "/api/v1/crm/bids?pageNum=1&pageSize=5",
                "/api/v1/crm/contacts?pageNum=1&pageSize=5",
                "/api/v1/crm/activities?pageNum=1&pageSize=5",
                "/api/v1/crm/leads?pageNum=1&pageSize=5"}) {
            JsonNode node = call("GET", url, token, null);
            assertThat(node.get("code").asInt()).withFailMessage(url + " -> " + node).isZero();
        }
    }

    @Test
    @DisplayName("公共池与认领审批：池列表可读，认领申请列表可读")
    void poolAndClaimEndpoints() throws Exception {
        String token = login();

        JsonNode pool = call("GET", "/api/v1/crm/accounts/pool?pageNum=1&pageSize=10", token, null);
        assertThat(pool.get("code").asInt()).withFailMessage(pool.toString()).isZero();

        JsonNode claims = call("GET", "/api/v1/crm/claims?pageNum=1&pageSize=10", token, null);
        assertThat(claims.get("code").asInt()).withFailMessage(claims.toString()).isZero();
        assertThat(claims.get("data").get("list").isArray()).isTrue();

        JsonNode pending = call("GET", "/api/v1/crm/claims/pending-count", token, null);
        assertThat(pending.get("code").asInt()).withFailMessage(pending.toString()).isZero();
        assertThat(pending.get("data").get("count").isNumber()).isTrue();
    }

    @Test
    @DisplayName("线索转化：同一线索重复提交返回既有结果（幂等）")
    void leadConvertIsIdempotent() throws Exception {
        String token = login();
        String mobile = "13900009999";

        JsonNode created = call("POST", "/api/v1/crm/leads", token,
                "{\"name\":\"集成测试线索\",\"mobile\":\"" + mobile + "\",\"companyName\":\"集成测试银行\""
                        + ",\"sourceChannel\":\"WEB\",\"grade\":\"A\",\"accountNature\":\"TO_B\"}");
        assertThat(created.get("code").asInt()).withFailMessage(created.toString()).isZero();
        long leadId = created.get("data").get("id").asLong();

        // 仅「跟进中」允许转化
        JsonNode updated = call("PUT", "/api/v1/crm/leads/" + leadId, token, "{\"status\":\"FOLLOWING\"}");
        assertThat(updated.get("code").asInt()).withFailMessage(updated.toString()).isZero();

        JsonNode first = call("POST", "/api/v1/crm/leads/" + leadId + "/convert", token,
                "{\"convertMode\":\"NEW_ACCOUNT\",\"opportunityName\":\"集成测试商机\",\"expectedAmount\":100000}");
        assertThat(first.get("code").asInt()).withFailMessage(first.toString()).isZero();
        assertThat(first.get("data").get("idempotent").asBoolean()).isFalse();
        long accountId = first.get("data").get("accountId").asLong();
        long opportunityId = first.get("data").get("opportunityId").asLong();

        JsonNode second = call("POST", "/api/v1/crm/leads/" + leadId + "/convert", token, "{}");
        assertThat(second.get("code").asInt()).withFailMessage(second.toString()).isZero();
        assertThat(second.get("data").get("idempotent").asBoolean()).isTrue();
        assertThat(second.get("data").get("accountId").asLong()).isEqualTo(accountId);
        assertThat(second.get("data").get("opportunityId").asLong()).isEqualTo(opportunityId);

        JsonNode account = call("GET", "/api/v1/crm/accounts/" + accountId, token, null);
        assertThat(account.get("code").asInt()).withFailMessage(account.toString()).isZero();
        assertThat(account.get("data").get("companyName").asText()).isEqualTo("集成测试银行");

        JsonNode opportunity = call("GET", "/api/v1/crm/opportunities/" + opportunityId, token, null);
        assertThat(opportunity.get("code").asInt()).withFailMessage(opportunity.toString()).isZero();
        assertThat(opportunity.get("data").get("salesStage").asText()).isEqualTo("PROSPECT");
    }

    @Test
    @DisplayName("统计与漏斗：汇总、阶段漏斗、立项来源均可读")
    void statsAndOpeningSources() throws Exception {
        String token = login();

        JsonNode summary = call("GET", "/api/v1/crm/common/stats/summary", token, null);
        assertThat(summary.get("code").asInt()).withFailMessage(summary.toString()).isZero();
        assertThat(summary.get("data").has("opportunityAmount")).isTrue();

        JsonNode funnel = call("GET", "/api/v1/crm/common/stats/funnel", token, null);
        assertThat(funnel.get("code").asInt()).withFailMessage(funnel.toString()).isZero();
        assertThat(funnel.get("data").get("stages").isArray()).isTrue();

        JsonNode stages = call("GET", "/api/v1/crm/opportunities/funnel", token, null);
        assertThat(stages.get("code").asInt()).withFailMessage(stages.toString()).isZero();

        for (String type : new String[] {"opportunities", "bids", "contracts", "agreements"}) {
            JsonNode node = call("GET", "/api/v1/crm/opening-sources/" + type, token, null);
            assertThat(node.get("code").asInt()).withFailMessage(type + " -> " + node).isZero();
        }
    }

    @Test
    @DisplayName("不存在的接口返回 404 而不是 500")
    void unknownEndpointReturns404() throws Exception {
        String token = login();
        MvcResult result = mockMvc.perform(get("/api/v1/crm/not-exists")
                .header("Authorization", "Bearer " + token)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(node.get("code").asInt()).isEqualTo(404);
    }

    @Test
    @DisplayName("7 个后台定时任务可执行且回写执行时间（幂等）")
    void scheduledJobsRun() {
        scheduler.accountPoolRecycle();
        scheduler.leadRecycle();
        scheduler.contractExpire();
        scheduler.agreementExpire();
        scheduler.bidOpeningRemind();
        scheduler.activityOverdue();
        scheduler.claimAppealTimeout();

        // 再跑一遍验证幂等（不抛异常、不产生重复数据）
        scheduler.contractExpire();
        scheduler.activityOverdue();

        SysJob job = jobMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<SysJob>lambdaQuery()
                        .eq(SysJob::getJobCode, "CRM_ACCOUNT_POOL_RECYCLE")
                        .last("limit 1"));
        assertThat(job).isNotNull();
        assertThat(job.getLastExecTime()).isNotNull();
    }
}
