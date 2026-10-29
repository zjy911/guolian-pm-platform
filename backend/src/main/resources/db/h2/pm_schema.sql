-- ============================================================
-- 国联政信项目管理平台 · 项目管理模块 Schema（H2 开发档，MySQL 兼容）
-- 7 张业务表：产品 / 任务模板 / 项目 / 项目成员 / 项目任务 / 表单实例 / 项目日志
-- 依据：《项目管理模块 PRD v1.0》（环节定死、任务可配；模板快照隔离）
-- ============================================================

-- 产品：BG 面向某类客户的标准化服务方案，是任务模板的载体
CREATE TABLE IF NOT EXISTS pm_product (
    id BIGINT NOT NULL PRIMARY KEY,
    product_code VARCHAR(32) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    bg_org_id BIGINT,
    bg_org_name VARCHAR(100),
    pricing_mode VARCHAR(20),
    template_version VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE',
    remark VARCHAR(500),
    owner_user_id BIGINT,
    owner_org_id BIGINT,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_pm_product_code UNIQUE (product_code)
);

-- 任务模板：按产品维护的 6 环节任务定义（JSON 快照源），发布后版本只增不改
CREATE TABLE IF NOT EXISTS pm_task_template (
    id BIGINT NOT NULL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    template_json CLOB,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    publish_time DATETIME,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_tpl_product ON pm_task_template (product_id, version);

-- 项目：产品在具体客户上的一次落地实例
CREATE TABLE IF NOT EXISTS pm_project (
    id BIGINT NOT NULL PRIMARY KEY,
    project_code VARCHAR(32) NOT NULL,
    project_name VARCHAR(128) NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(128),
    template_version VARCHAR(20),
    customer_id BIGINT,
    customer_name VARCHAR(200),
    contract_no VARCHAR(64),
    sign_entity VARCHAR(200),
    estimated_amount DECIMAL(18,4),
    annual_cases INT,
    pricing_mode VARCHAR(20),
    rating VARCHAR(2),
    phase TINYINT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATING',
    health_score DECIMAL(5,2),
    health_light VARCHAR(10),
    gross_margin DECIMAL(9,4),
    org_id BIGINT,
    owner_user_id BIGINT,
    owner_org_id BIGINT,
    owner_user_name VARCHAR(50),
    initiation_form_id BIGINT,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_pm_project_code UNIQUE (project_code)
);
CREATE INDEX IF NOT EXISTS idx_pm_project_org ON pm_project (owner_org_id);
CREATE INDEX IF NOT EXISTS idx_pm_project_status ON pm_project (status);

-- 项目成员：人员主数据全部引用 sys_user（R-09），投入比例同一人跨项目累计 ≤100
CREATE TABLE IF NOT EXISTS pm_project_member (
    id BIGINT NOT NULL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(50),
    project_role VARCHAR(30) NOT NULL,
    input_ratio INT NOT NULL DEFAULT 0,
    join_date DATE,
    expected_leave_date DATE,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    evaluation VARCHAR(500),
    release_time DATETIME,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pm_member_user ON pm_project_member (user_id, status);
CREATE INDEX IF NOT EXISTS idx_pm_member_project ON pm_project_member (project_id);

-- 项目任务实例：立项通过后按模板快照一次性实例化（AC-03）
CREATE TABLE IF NOT EXISTS pm_project_task (
    id BIGINT NOT NULL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    phase TINYINT NOT NULL,
    category VARCHAR(64),
    task_name VARCHAR(128) NOT NULL,
    task_type VARCHAR(10) NOT NULL,
    form_code VARCHAR(64),
    blocking TINYINT NOT NULL DEFAULT 0,
    depends_task_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    assignee_id BIGINT,
    assignee_name VARCHAR(50),
    due_date DATE,
    sort_no INT,
    completed_time DATETIME,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pm_task_project ON pm_project_task (project_id, phase, sort_no);
CREATE INDEX IF NOT EXISTS idx_pm_task_assignee ON pm_project_task (assignee_id, status);

-- 表单实例：38 类表单对象的通用承载，扩展字段以 JSON 存储，不改主表结构
CREATE TABLE IF NOT EXISTS pm_form_instance (
    id BIGINT NOT NULL PRIMARY KEY,
    project_id BIGINT,
    task_id BIGINT,
    form_code VARCHAR(64) NOT NULL,
    form_data CLOB,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    flow_instance_id VARCHAR(64),
    submit_user_id BIGINT,
    submit_user_name VARCHAR(50),
    submit_time DATETIME,
    approve_time DATETIME,
    approve_comment VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pm_form_project ON pm_form_instance (project_id, form_code);
CREATE INDEX IF NOT EXISTS idx_pm_form_task ON pm_form_instance (task_id);

-- 项目日志：履历留痕（立项/审批/推进/变更/任命等）
CREATE TABLE IF NOT EXISTS pm_project_log (
    id BIGINT NOT NULL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    detail VARCHAR(1000),
    operator_id BIGINT,
    operator_name VARCHAR(50),
    created_time DATETIME
);
CREATE INDEX IF NOT EXISTS idx_pm_log_project ON pm_project_log (project_id, created_time);
