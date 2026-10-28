package com.guolian.pm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 底座全链路集成测试：真实 H2 + 真实 Flowable，不 mock 服务层。
 * 覆盖：登录、菜单下发、切角色（切组织）、数据权限、字段脱敏、
 * 功能权限 403、交接预检与审批端到端。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PlatformIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode post(String url, String token, String body) throws Exception {
        var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body == null ? "{}" : body);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode getJson(String url, String token) throws Exception {
        MvcResult result = mockMvc.perform(get(url).header("Authorization", "Bearer " + token)).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String login(String username, String password) throws Exception {
        JsonNode node = post("/api/v1/auth/login", null,
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertThat(node.get("code").asInt()).withFailMessage(node.toString()).isZero();
        return node.get("data").get("token").asText();
    }

    @Test
    @DisplayName("登录成功并下发 4 个一级菜单与按钮权限")
    void loginAndMenus() throws Exception {
        JsonNode node = post("/api/v1/auth/login", null, "{\"username\":\"admin\",\"password\":\"Admin@123\"}");
        assertThat(node.get("code").asInt()).isZero();
        JsonNode data = node.get("data");
        JsonNode profile = data.get("profile");
        assertThat(profile.get("orgName").asText()).isEqualTo("国联政信");
        assertThat(profile.get("dataScopeLabel").asText()).isEqualTo("全部数据");
        assertThat(data.get("menus")).hasSize(4);
        assertThat(data.get("menus").get(0).get("name").asText()).isEqualTo("CRM");
        assertThat(profile.get("perms").size()).isGreaterThan(50);
    }

    @Test
    @DisplayName("错误密码登录被拒绝且不泄露内部细节")
    void loginWithWrongPassword() throws Exception {
        JsonNode node = post("/api/v1/auth/login", null, "{\"username\":\"admin\",\"password\":\"bad\"}");
        assertThat(node.get("code").asInt()).isEqualTo(4001);
        assertThat(node.get("msg").asText()).isEqualTo("账号或密码错误");
    }

    @Test
    @DisplayName("切换角色同时切换组织，菜单与数据范围随之变化")
    void switchRoleSwitchesOrg() throws Exception {
        String token = login("zhangwei", "Admin@123");
        JsonNode info = getJson("/api/v1/auth/user-info", token);
        assertThat(info.get("data").get("profile").get("roleName").asText()).isEqualTo("G端BG-项目经理");
        assertThat(info.get("data").get("roles")).hasSize(5);

        JsonNode switched = post("/api/v1/auth/switch-role", token, "{\"roleId\":105}");
        assertThat(switched.get("code").asInt()).isZero();
        JsonNode profile = switched.get("data").get("profile");
        assertThat(profile.get("orgName").asText()).isEqualTo("华北地区部");
        assertThat(profile.get("roleName").asText()).isEqualTo("地区部-运营");
        assertThat(profile.get("dataScope").asText()).isEqualTo("ORG");
        // 新角色只有驾驶舱菜单
        assertThat(switched.get("data").get("menus")).hasSize(1);
        assertThat(switched.get("data").get("menus").get(0).get("name").asText()).isEqualTo("驾驶舱管理");
    }

    @Test
    @DisplayName("数据权限：本组织及下级只能看到范围内用户，超管可见全部")
    void dataScopeFilters() throws Exception {
        String adminToken = login("admin", "Admin@123");
        JsonNode adminUsers = getJson("/api/v1/system/users?pageNum=1&pageSize=20", adminToken);
        long adminTotal = adminUsers.get("data").get("total").asLong();
        assertThat(adminTotal).isGreaterThanOrEqualTo(7);

        String linaToken = login("lina", "Admin@123");
        JsonNode linaUsers = getJson("/api/v1/system/users?pageNum=1&pageSize=20", linaToken);
        long linaTotal = linaUsers.get("data").get("total").asLong();
        assertThat(linaTotal).isLessThan(adminTotal).isGreaterThan(0);
    }

    @Test
    @DisplayName("字段脱敏：手机号 MASKED、身份证 HIDDEN 时不出现该 key")
    void sensitiveMasking() throws Exception {
        // 角色 102：手机号与身份证为 MASKED
        String token = login("lina", "Admin@123");
        JsonNode users = getJson("/api/v1/system/users?keyword=zhangwei", token);
        JsonNode row = users.get("data").get("list").get(0);
        assertThat(row.get("mobile").asText()).contains("****");
        assertThat(row.get("idCard").asText()).contains("*");

        // 角色 103：身份证与邮箱为 HIDDEN，JSON 中应完全没有该 key
        String wangqiang = login("wangqiang", "Admin@123");
        JsonNode hidden = getJson("/api/v1/system/users?keyword=wangqiang", wangqiang);
        JsonNode hiddenRow = hidden.get("data").get("list").get(0);
        assertThat(hiddenRow.has("idCard")).isFalse();
        assertThat(hiddenRow.get("email").asText()).contains("***");
    }

    @Test
    @DisplayName("超管可见明文手机号（脱敏策略按角色生效）")
    void adminSeesPlainMobile() throws Exception {
        String token = login("admin", "Admin@123");
        JsonNode users = getJson("/api/v1/system/users?keyword=zhangwei", token);
        JsonNode row = users.get("data").get("list").get(0);
        assertThat(row.get("mobile").asText()).isEqualTo("13800138012");
    }

    @Test
    @DisplayName("功能权限：无导出权限的角色直接调接口返回 403")
    void permissionDenied() throws Exception {
        String token = login("lina", "Admin@123");
        MvcResult result = mockMvc.perform(get("/api/v1/system/users/export")
                .header("Authorization", "Bearer " + token)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("离职用户为只读态：写接口被接口层拒绝")
    void resignedUserIsReadOnly() throws Exception {
        String token = login("wangqiang", "Admin@123");
        MvcResult result = mockMvc.perform(put("/api/v1/system/users/3/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":2}")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("账号交接：预检返回已注册适配器数量，接收人缺角色被阻止")
    void transferPrecheckAndReceiverValidation() throws Exception {
        String token = login("admin", "Admin@123");
        JsonNode precheck = getJson("/api/v1/system/transfers/precheck?fromUserId=6", token);
        assertThat(precheck.get("code").asInt()).isZero();
        assertThat(precheck.get("data").get("adapters")).isNotEmpty();
        assertThat(precheck.get("data").get("roles")).isNotEmpty();

        // 接收人 5（刘洋）不拥有李娜的角色 102，单接收人模式应被阻止
        JsonNode failed = post("/api/v1/system/transfers", token,
                "{\"fromUserId\":6,\"toUserIds\":[3],\"assignMode\":1,\"scope\":[\"NOTICE_TODO\"]}");
        assertThat(failed.get("code").asInt()).isEqualTo(4001);
        assertThat(failed.get("msg").asText()).contains("缺少角色");
    }

    @Test
    @DisplayName("账号交接端到端：提交审批 → 三级审批 → 自动执行 → 明细落库")
    void transferEndToEnd() throws Exception {
        String adminToken = login("admin", "Admin@123");

        // 造一条待办，保证有可交接数据
        post("/api/v1/system/notices", adminToken,
                "{\"title\":\"交接测试待办\",\"noticeType\":3,\"content\":\"测试\",\"receiverId\":3}");

        JsonNode created = post("/api/v1/system/transfers", adminToken,
                "{\"fromUserId\":3,\"toUserIds\":[2],\"assignMode\":1,\"scope\":[\"NOTICE_TODO\"],\"remark\":\"集成测试\"}");
        assertThat(created.get("code").asInt()).withFailMessage(created.toString()).isZero();
        long transferId = created.get("data").get("id").asLong();

        JsonNode submitted = post("/api/v1/system/transfers/" + transferId + "/submit", adminToken, "{}");
        assertThat(submitted.get("code").asInt()).withFailMessage(submitted.toString()).isZero();
        String instanceId = submitted.get("data").get("processInstanceId").asText();
        assertThat(instanceId).isNotBlank();

        // 审批中不可执行
        JsonNode executeEarly = post("/api/v1/system/transfers/" + transferId + "/execute", adminToken, "{}");
        assertThat(executeEarly.get("code").asInt()).isEqualTo(4001);

        // 三级审批：直属上级(张伟) → 部门负责人(张伟) → 人力资源(超管候选)
        String zhangwei = login("zhangwei", "Admin@123");
        for (int i = 0; i < 2; i++) {
            JsonNode todo = getJson("/api/v1/flow/tasks/todo", zhangwei);
            assertThat(todo.get("data")).isNotEmpty();
            String taskId = todo.get("data").get(0).get("taskId").asText();
            JsonNode approved = post("/api/v1/flow/tasks/" + taskId + "/approve", zhangwei,
                    "{\"comment\":\"同意\"}");
            assertThat(approved.get("code").asInt()).isZero();
        }
        JsonNode adminTodo = getJson("/api/v1/flow/tasks/todo", adminToken);
        assertThat(adminTodo.get("data")).isNotEmpty();
        String hrTask = adminTodo.get("data").get(0).get("taskId").asText();
        assertThat(post("/api/v1/flow/tasks/" + hrTask + "/approve", adminToken, "{\"comment\":\"同意\"}")
                .get("code").asInt()).isZero();

        // 审批通过后异步执行，轮询执行状态
        String execStatus = "";
        for (int i = 0; i < 20; i++) {
            Thread.sleep(300);
            JsonNode detail = getJson("/api/v1/system/transfers/" + transferId, adminToken);
            execStatus = detail.get("data").get("execStatus").asText();
            if ("DONE".equals(execStatus) || "PARTIAL_FAILED".equals(execStatus)) {
                break;
            }
        }
        assertThat(execStatus).isEqualTo("DONE");
        JsonNode items = getJson("/api/v1/system/transfers/" + transferId + "/items", adminToken);
        assertThat(items.get("data")).isNotEmpty();
        assertThat(items.get("data").get(0).get("result").asText()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("审批流水可查：ACT_HI_* 完整留痕")
    void flowHistory() throws Exception {
        String token = login("admin", "Admin@123");
        JsonNode mine = getJson("/api/v1/flow/instances/mine", token);
        assertThat(mine.get("code").asInt()).isZero();
        if (mine.get("data").size() > 0) {
            String pi = mine.get("data").get(0).get("processInstanceId").asText();
            JsonNode history = getJson("/api/v1/flow/instances/" + pi + "/history", token);
            assertThat(history.get("data")).isNotEmpty();
        }
    }
}
