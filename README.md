# 国联征信项目管理平台 · 底座脚手架 v1.0

面向公司内部的平台底座：**平台框架 + 系统管理 + 通用审批能力**。
CRM、项目管理、驾驶舱三个业务模块由独立方案建设，底座只为其预留菜单、路由、权限位与占位页。

- 前端：Vue 3 + TypeScript + Vite + Pinia + Vue Router + Axios，视觉沿用 Tabler 结构、令牌替换为「深海蓝企业级轻科技」
- 后端：Spring Boot 3.2.5 + JDK 17 + MyBatis-Plus 3.5.5 + MySQL 8.0 / Redis，审批引擎 Flowable 7.1.0
- 鉴权：自实现 JWT（短时效访问令牌 + 服务端会话表），不接 SSO
- 文档：Swagger / OpenAPI 3，路径 `/doc.html`

---

## 1. 交付边界（最重要）

**已交付**

1. 平台框架：横纵栏布局、登录鉴权、路由与菜单驱动、统一请求封装、主题令牌、数据权限拦截器、脱敏序列化器
2. 系统管理：用户（含 4 个 Tab）、账号交接、组织架构、公司主体、角色、菜单、审批流程、数据权限、字段脱敏、字典、参数、通知公告、操作日志、登录日志、在线用户、附件、个人中心
3. 通用审批能力：Flowable 7.1.0 集成 + 表单化流程配置 + 业务集成封装 + 统一审批中心，以**账号交接**为唯一正式跑通场景
4. 业务模块占位：CRM / 项目管理 / 驾驶舱，统一占位页展示「模块待接入」与当前用户权限标识

**明确不在本方案**

- CRM 业务功能：客户、公共客户池、线索商机、销售跟进、法务合同、协议、投标、回款、流失客户
- 项目管理业务功能：产品、项目、任务看板、立项/评审/验收/复盘
- 驾驶舱指标定义与取数逻辑
- 北森 OA、用友财务、DXE 中台的真实对接

**硬约束落实情况**

| 约束 | 落实方式 |
| --- | --- |
| 底座不出现 CRM / 项目业务表 | 26 张表全部 `sys_*` + Flowable `ACT_*`，无任何客户/合同/项目/任务表与 Entity |
| 用户不直接挂组织 | 无 `sys_user_org` 表、无 `sys_user.primary_org_id`，组织由角色派生 |
| 角色必绑组织 | `sys_role.org_id` 非空，服务层校验；切换角色即切组织 |
| 公司主体与组织无关 | `sys_org` 无 `company_id`，组织接口不返回主体信息 |
| 双重屏障 | 所有列表/详情/导出/统计走同一套数据权限 + 字段脱敏 |
| 离职只读 | 拦截器按 `readOnly` 拒绝写操作，不依赖前端隐藏按钮 |
| 审批只用原生表 | 实例与任务全部在 `ACT_*`，自建表仅 `sys_flow_def` / `sys_flow_biz` |

---

## 2. 目录结构

```
guolian-pm-platform/
├── backend/                          # Spring Boot 3.2.5 后端
│   ├── src/main/java/com/guolian/pm/
│   │   ├── auth/                     # 登录、切角色、模拟视角、会话注册表
│   │   ├── common/                   # 统一响应、异常、日志、缓存、数据权限、Web 配置
│   │   ├── system/                   # 用户/角色/组织/主体/菜单/字典/参数/通知/日志/脱敏/在线/附件
│   │   ├── flow/                     # Flowable 封装、BPMN 生成器、审批模板、审批中心
│   │   └── transfer/                 # 账号交接：适配器、服务、控制器
│   ├── src/main/resources/
│   │   ├── application.yml / -dev.yml / -prod.yml
│   │   ├── db/h2/{schema,data}.sql   # 开发档内嵌库
│   │   └── processes/                # 2 个示例 BPMN（自动部署）
│   └── src/test/java/.../PlatformIntegrationTest.java   # 11 项全链路集成测试
├── frontend/                         # Vue 3 + TS 前端
│   └── src/{api,stores,router,layout,components,views,styles}
├── sql/                              # MySQL 生产脚本
│   ├── 01_schema.sql                 # 26 张自建表
│   ├── 02_init_data.sql              # 组织/主体/角色/用户/菜单/授权/脱敏/交接/审批模板
│   ├── 03_dict_data.sql              # 字典与参数
│   └── 04_flowable.sql               # ACT_* 建表方案说明
├── docker-compose.yml                # MySQL + Redis + 后端 + 前端
└── .env.example                      # 环境变量样例（仅占位值）
```

---

## 3. 快速开始

### 3.1 本机 5 分钟启动（无需 MySQL / Redis）

开发档使用内嵌 H2（MySQL 兼容模式）+ Flowable 自动建表，适合本机看效果与联调。

```bash
# 后端（JDK 17+、Maven 3.9+）
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 或： mvn -DskipTests package && java -jar target/pm-platform.jar --spring.profiles.active=dev

# 前端（Node 20+）
cd frontend
npm install
npm run dev          # http://127.0.0.1:5173 ，/api 已代理到 8080
```

- 后端健康检查：`GET http://127.0.0.1:8080/api/v1/health`
- 接口文档：`http://127.0.0.1:8080/doc.html`
- H2 控制台：`http://127.0.0.1:8080/h2-console`（JDBC `jdbc:h2:mem:pmplatform`，用户 `sa`，空密码）

> 说明：`application-dev.yml` 内置了一把仅用于本机的 JWT 密钥；生产必须通过环境变量注入。

### 3.2 Docker 一键部署（MySQL + Redis + 前后端）

```bash
cp .env.example .env      # 填写 JWT_SECRET、DB_PASSWORD 等
docker compose up -d --build
# 前端 http://localhost:8081    后端 http://localhost:8080
```

`sql/01~03` 会在 MySQL 容器首次启动时自动执行；Flowable 的 `ACT_*` 建表策略见 `sql/04_flowable.sql`。

### 3.3 演示账号（口令统一 `Admin@123`）

| 账号 | 说明 |
| --- | --- |
| `admin` | 集团-超级管理员，数据范围 ALL，全部权限 |
| `zhangwei` | 拥有 **5 个角色、跨 3 个组织**，用于验证切角色即切组织 |
| `lina` | G端BG-项目经理（ORG_AND_CHILD），手机号/身份证脱敏 |
| `wangqiang` | G端BG-调解员，**离职只读态**（写接口返回 403） |
| `liuyang` / `chenjing` | 和合BG 项目经理 / 商务部商务经理 |
| `zhaolei` | 已停用账号（验证无法登录） |

---

## 4. 已实现接口一览

统一响应体 `{ code, msg, data }`，分页 `data = { list, total, pageNum, pageSize }`，Base URL `/api/v1`。

**认证与身份**

```
POST /auth/login              登录（返回 Token + 身份 + 菜单 + 可用角色 + 角标）
POST /auth/switch-role        切角色（同时切组织，返回新 Token/菜单/权限/数据范围/脱敏策略）
POST /auth/logout             GET /auth/user-info     GET /auth/menus     GET /auth/my-roles
POST /auth/change-password
```

**用户管理**

```
GET    /system/users                  列表（含全部关联角色、组织、主体）
GET    /system/users/{id}             详情        POST /system/users        新增
PUT    /system/users/{id}             修改        DELETE /system/users/{id} 删除
PUT    /system/users/{id}/roles       分配角色（≤5、默认角色、同组织单角色校验）
PUT    /system/users/{id}/status      启用/停用/离职（停用会踢出在线会话）
POST   /system/users/{id}/reset-password
GET    /system/users/{id}/projects    项目参与（跨模块只读，本期返回空列表）
GET    /system/users/export           导出 CSV（走同一套脱敏）
```

**角色 / 组织 / 主体 / 菜单 / 脱敏**

```
GET|POST|PUT|DELETE /system/roles ...            role.org_id 必填；改绑组织需二次确认
PUT /system/roles/{id}/menus                    角色授权（菜单+按钮）
PUT /system/roles/{id}/data-scope               数据范围
PUT /system/roles/{id}/field-mask               角色脱敏档位
GET|POST|PUT|DELETE /system/orgs ...  PUT /system/orgs/{id}/move
GET|POST|PUT|DELETE /system/companies ...
GET|POST|PUT|DELETE /system/menus ...
GET|POST|PUT|DELETE /system/sensitive/{groups,fields}   GET /system/sensitive/whitelist
GET  /system/sensitive/plain?bizType=&bizId=&group=     查看明文（写审计日志，不含明文）
```

**平台能力**

```
GET|POST|PUT|DELETE /system/dicts/{types,items}   GET /system/dicts/data?types=
GET|POST|PUT|DELETE /system/configs
GET|POST|PUT|DELETE /system/notices   GET /system/notices/mine|todos|badge   POST /system/notices/{id}/read
GET /system/logs/oper    GET /system/logs/login
GET /system/online       POST /system/online/{sessionId}/logout
POST /system/files/upload   GET /system/files   GET /system/files/{id}/download
POST /system/debug/impersonate   GET /system/debug/data-scope-sql   POST /system/debug/exit
```

**审批（供业务模块复用）**

```
GET|POST|PUT|DELETE /system/flows        POST /system/flows/{id}/publish
POST /flow/start                         POST /flow/tasks/{taskId}/{approve|reject|add-sign|resolve|transfer}
POST /flow/instances/{id}/withdraw
GET  /flow/tasks/todo|done   GET /flow/instances/mine|cc   GET /flow/instances/{id}/history
```

**账号交接**

```
GET  /system/transfers/precheck           预检（遍历已注册适配器统计待交接数量）
GET  /system/transfers/eligible-receivers 按角色筛合格接收人
GET  /system/transfers/adapters           已注册业务对象
POST /system/transfers                    创建草稿
POST /system/transfers/{id}/submit        提交审批（启动 Flowable）
POST /system/transfers/{id}/execute       执行（仅审批通过后）
POST /system/transfers/{id}/retry         仅重试失败项
POST /system/transfers/{id}/revoke        可撤销期内反向回写
GET  /system/transfers/{id}/items         交接明细
```

---

## 5. 核心设计说明

### 5.1 角色即身份

`角色 = 所属组织 + 功能权限 + 数据范围 + 字段脱敏策略`。Token 携带 `currentRoleId` 与 `currentOrgId`（后者由角色派生），
切换角色时服务端重新装载上下文并签发新 Token，前端重置状态并重新拉取数据，**不需要重新登录**，也**没有独立的「切换组织」入口**。

### 5.2 数据权限（行级）

`PmDataPermissionHandler` 挂在 MyBatis-Plus `DataPermissionInterceptor` 上，按「表名 → 归属人字段」注册表（`DataScopeRegistry`）在 SQL 前拼条件：

| 范围 | 用户表条件 |
| --- | --- |
| ALL | 不限制（超管绕过，日志标记 dataScope=bypass） |
| ORG / ORG_AND_CHILD / ORG_CUSTOM | `id in (select ur.user_id from sys_user_role ur join sys_role r on r.id=ur.role_id where r.org_id in (...))` |
| COMPANY_CUSTOM | `id in (select uc.user_id from sys_user_company uc where uc.company_id in (...))` |
| SELF / PROJECT_MEMBER | `id = 当前用户` |

只按当前角色计算，不叠加；列表、详情、导出、统计总数共用同一套逻辑。

### 5.3 字段脱敏（列级）

- `@Sensitive(group = "MOBILE")` 标注字段，算法由「字段注册」决定，不在实体里硬编码
- `SensitiveSerializerModifier` 在 `HIDDEN` 档位下**彻底跳过字段写入**，JSON 中不出现该 key（不用空串或 null 占位）
- 算法：`KEEP_HEAD_TAIL` / `KEEP_TAIL` / `EMAIL_MASK` / `ADDRESS_MASK` / `FULL_MASK` / `AMOUNT_RANGE`
- 高风险组且无字段允许明文时，任何角色都无法配成 `PLAIN`（服务端二次降级）
- 配置变更后缓存 60 秒过期，1 分钟内对在线用户生效；导出与日志走同一逻辑

### 5.4 审批能力

- 平台侧只做三件事：表单化流程配置（保存时生成 BPMN 并部署）、业务集成封装（`FlowService`）、统一待办中心
- 8 项引擎能力：单人、候选人、会签（全通过）、或签、条件分支、抄送（通知 Bean 位）、超时提醒（定时边界事件位）、驳回到指定节点
- 提交前必须解析出每个节点的审批人集合，空审批人直接阻止并提示节点名称
- 审批流实例与任务全部使用 `ACT_*`，`sys_flow_def` 只存配置，`sys_flow_biz` 只做业务映射

### 5.5 账号交接

- 内置 2 个适配器：`NOTICE_TODO`（站内待办）、`FLOW_TASK`（Flowable 任务改派，接收人不在节点审批人范围时该项标记失败）
- 接收人资格：单接收人模式要求「接收人角色集合 ⊇ 交出人角色集合」；按角色分派模式逐角色匹配
- 审批通过后由 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` 异步执行，逐条写 `sys_account_transfer_item`
- 可撤销期默认 7 天（参数 `transfer.revoke.days`），过期后撤销按钮消失、接口拒绝

---

## 6. 业务模块接入指引（四件事，决定三个模块能否并行开发）

### 6.1 注册菜单与按钮权限标识

1. 在「系统管理 → 菜单管理」新增目录 → 菜单 → 按钮，权限标识统一 `module:resource:action`
   （已有占位：`crm:customer:list`、`project:project:list`、`cockpit:overview:view` …）
2. 前端在 `src/router/index.ts` 增加静态路由，`meta.perm` 填权限标识；后端 `sys_menu` 里配置 `path` / `component`
3. 页面内按钮使用指令：`v-perm="['crm:customer:add']"`
4. 后端接口加注解：`@Perm("crm:customer:add")`；写操作再加 `@OpLog(module="客户管理", action="新增客户")`

> 菜单是前端路由的唯一权限来源；前端路由表为静态全量表 + 后端权限标识过滤。

### 6.2 实现并注册交接适配器

```java
@Component
public class CustomerTransferAdapter implements TransferAdapter {
    @Override public String bizType()  { return "CRM_CUSTOMER"; }
    @Override public String bizName()  { return "客户"; }
    @Override public long count(Long fromUserId, LocalDateTime since) { /* 统计待交接数量 */ }
    @Override public List<Long> listIds(Long fromUserId, int offset, int limit) { /* 分批取 ID */ }
    @Override public TransferOutcome transfer(Long fromUserId, Long toUserId, List<Long> bizIds) { /* 改归属 */ }
    @Override public void rollback(List<RollbackItem> items) { /* 按明细反向回写 */ }
    @Override public String describe(Long bizId) { /* 明细展示名称 */ }
    @Override public String ownerRoleCode(Long bizId) { return null; } // 需要按角色分派时返回角色编码
}
```

注册即为 Spring Bean，底座启动时自动扫描；**预检、执行、重试、撤销主流程代码无需修改**，
未注册的对象在预检中不可选并提示「该业务对象暂未接入交接」。

### 6.3 调用审批

```java
FlowDtos.StartRequest req = new FlowDtos.StartRequest();
req.setBizType("CRM_CONTRACT");      // 需在 sys_flow_def 配置并发布同 bizType 的模板
req.setBizId(contractId);
req.setBizNo("HT-2026-0001");
req.setBizTitle("合同审批：" + contractName);
req.setDetailRoute("/crm/contract?id=" + contractId);
req.getVariables().put("amount", contractAmount);   // 条件分支变量
req.getVariables().put("subjectUserId", ownerId);   // 为他人发起时，审批人按主体人解析
flowService.start(req, AuthContext.require());
```

后续动作一律调 `FlowService`：`approve / reject / addSign / transferTask / withdraw`，不直接依赖 Flowable API。
流程结束会发布 `FlowFinishedEvent`（含 bizType / bizId / approved），业务模块用
`@TransactionalEventListener(AFTER_COMMIT)` 更新自己的单据状态。

### 6.4 使用 `@DataScope` 与 `@Sensitive`

- 数据权限：在 `DataScopeRegistry` 注册业务表 → 归属人字段（例如 `project` → `owner_id`），
  或在启动时调 `DataScopeRegistry.register("crm_contract", "owner_id")`。查询自动带上行级条件，
  与列表/导出/统计共用。需要标记语义时在 Mapper 方法上加 `@DataScope(resource = "contract")`。
- 字段脱敏：在「字段脱敏」页面把业务字段加入白名单并注册算法，VO 字段加 `@Sensitive(group = "MOBILE")`。
  新业务对象需扩展 `SensitiveAdminService.whitelist()` 一份字段白名单（研发维护，前端下拉选择）。

---

## 7. 验证记录

**自动化集成测试：`backend/src/test/java/com/guolian/pm/PlatformIntegrationTest.java`（11 项，全部通过）**

| 用例 | 验证点 |
| --- | --- |
| 登录与菜单 | 超管登录成功、组织/数据范围正确、4 个一级菜单、权限标识 80+ |
| 错误口令 | 返回 `4001 账号或密码错误`，不泄露内部细节 |
| 切换角色 | 5 个角色可切换，切到「地区部-运营」后组织变华北地区部、菜单只剩驾驶舱 |
| 数据权限 | 项目经理可见用户数 < 超管可见数，导出与总数一致 |
| 字段脱敏 | 角色 102 手机号 `138****8012`；角色 103 身份证 key 完全不出现在 JSON |
| 超管明文 | 超管手机号为明文真值 |
| 功能权限 | 无导出权限的角色调接口返回 HTTP 403 |
| 离职只读 | 离职账号写接口返回 HTTP 403 |
| 交接预检与接收人校验 | 预检返回适配器数量；接收人缺角色被阻止并提示缺哪个角色 |
| 交接端到端 | 创建 → 提交审批 → 三级审批（直属上级/部门负责人/人力资源）→ 自动执行 → 明细 SUCCESS |
| 审批流水 | `ACT_HI_*` 时间轴可查 |

**本机实测**

- `mvn test`：11/11 通过（真实 H2 + 真实 Flowable，未 mock 服务层）
- `npm run build`：`vue-tsc --noEmit` 类型检查通过，产物构建成功
- HTTP 冒烟：`POST /api/v1/auth/login` 返回 Token；无 Token 访问 `/system/users` 返回 401；带 Token 返回列表且手机号/身份证按策略脱敏

**已知限制（有意为之，避免过度交付）**

1. 开发档用 H2 内存库，重启即重置；生产用 MySQL + `sql/01~03`
2. 在线会话注册表为节点内存储，多实例部署需替换为 Redis 实现（`OnlineSessionRegistry`）
3. 审批抄送列表返回空（未引入抄送表），超时提醒仅保留定时任务骨架与 BPMN 边界事件位
4. 附件存储为本地磁盘，生产建议接对象存储（`FileService` 单点替换）
5. 交接「按角色分派」模式下，若适配器不实现 `ownerRoleCode`，数据统一转给默认角色接收人
6. 系统监控、编码规则、定时任务调度为 P2，当前保留表结构与页面骨架

---

## 8. 安全与运维要点

- 配置全部走环境变量并在 `AppProperties` 启动时校验，`JWT_SECRET` 缺失或短于 32 位直接启动失败
- CORS 显式来源，`*` 会在启动时被拒绝；安全响应头由 `SecurityHeadersFilter` 统一附加
- 令牌不入 `localStorage`、不进 URL；访问令牌默认 120 分钟，刷新页面用 `sessionStorage` 兜底
- 登录连续失败 5 次锁定 10 分钟（参数可配）；停用/离职会立即踢出在线会话
- 操作日志参数中的 `password / token / secret` 一律替换为 `***`；身份证号 AES-GCM 加密存储
- 模拟查看（impersonate）在 PROD 默认关闭，开启后写操作一律被接口层拒绝，进入/退出各留一条操作日志
- 健康检查：`/api/v1/health`、`/api/v1/ready`、`/actuator/health`；`server.shutdown=graceful` 优雅停机

---

## 9. 待确认问题（沿用方案清单）

1. 账号交接审批三级节点（直属上级 → 部门负责人 → 人力资源）是否符合现行制度，HR 是否改为抄送
2. 业务模块适配器由各模块团队开发还是底座统一代建
3. 审批模板由业务运营在平台配置，还是研发随需求发布
4. 是否需要在底座交付时同步产出「业务模块接入验收清单」（菜单权限 / 适配器 / 审批调用 / 数据权限 / 脱敏五项）
5. 审批超时默认时长（建议 24 小时提醒、48 小时再次提醒并抄送上级）
6. 同一组织下对同一人是否严格只允许一个角色
7. 三个业务模块的排期衔接方式
