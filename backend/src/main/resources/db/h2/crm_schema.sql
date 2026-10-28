-- ============================================================
-- CRM 模块 Schema（开发档 H2 / MySQL 兼容模式）
-- 13 张 crm_* 表 + 埋点表；不含任何 product / project 表
-- 每张主表必须同时具备 owner_user_id 与 owner_org_id（数据权限依赖）
-- ============================================================

-- ---------- 1. 线索 ----------
CREATE TABLE IF NOT EXISTS crm_lead (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    company_name VARCHAR(128) NOT NULL,
    mobile VARCHAR(255) NOT NULL,
    mobile_hash CHAR(64),
    email VARCHAR(255),
    email_hash CHAR(64),
    position_name VARCHAR(64),
    industry VARCHAR(32),
    source_channel VARCHAR(32) NOT NULL,
    grade CHAR(1) NOT NULL DEFAULT 'C',
    status VARCHAR(16) NOT NULL DEFAULT 'NEW',
    account_nature VARCHAR(8),
    owner_user_id BIGINT NOT NULL,
    owner_org_id BIGINT NOT NULL,
    owner_user_name VARCHAR(50),
    last_follow_time DATETIME,
    converted_account_id BIGINT,
    converted_contact_id BIGINT,
    converted_opportunity_id BIGINT,
    converted_time DATETIME,
    invalid_reason VARCHAR(255),
    dup_reason VARCHAR(255),
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_lead_org_status ON crm_lead (owner_org_id, status);
CREATE INDEX IF NOT EXISTS idx_lead_owner ON crm_lead (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_lead_mobile_hash ON crm_lead (mobile_hash);
CREATE INDEX IF NOT EXISTS idx_lead_company ON crm_lead (company_name);
CREATE INDEX IF NOT EXISTS idx_lead_created ON crm_lead (created_time);

-- ---------- 2. 客户 ----------
CREATE TABLE IF NOT EXISTS crm_account (
    id BIGINT NOT NULL PRIMARY KEY,
    company_name VARCHAR(128) NOT NULL,
    credit_code VARCHAR(32),
    institution_code VARCHAR(64),
    industry VARCHAR(32) NOT NULL,
    account_nature VARCHAR(8) NOT NULL DEFAULT 'TO_B',
    account_type VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    website VARCHAR(255),
    phone VARCHAR(255),
    employee_scale VARCHAR(16),
    annual_revenue DECIMAL(18,4),
    owner_user_id BIGINT NOT NULL,
    owner_org_id BIGINT NOT NULL,
    owner_user_name VARCHAR(50),
    parent_account_id BIGINT,
    primary_contact_id BIGINT,
    province VARCHAR(32),
    city VARCHAR(32),
    district VARCHAR(32),
    address VARCHAR(255),
    pool_status VARCHAR(16) NOT NULL DEFAULT 'PRIVATE',
    pool_in_time DATETIME,
    pool_reason VARCHAR(255),
    churn_reason VARCHAR(255),
    is_prospect TINYINT NOT NULL DEFAULT 0,
    last_activity_time DATETIME,
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_account_credit ON crm_account (credit_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_account_inst ON crm_account (institution_code);
CREATE INDEX IF NOT EXISTS idx_account_org ON crm_account (owner_org_id, pool_status);
CREATE INDEX IF NOT EXISTS idx_account_owner ON crm_account (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_account_parent ON crm_account (parent_account_id);
CREATE INDEX IF NOT EXISTS idx_account_name_prov ON crm_account (company_name, province);

-- ---------- 3. 联系人 ----------
CREATE TABLE IF NOT EXISTS crm_contact (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    account_id BIGINT NOT NULL,
    mobile VARCHAR(255) NOT NULL,
    mobile_hash CHAR(64),
    email VARCHAR(255),
    email_hash CHAR(64),
    position_name VARCHAR(64),
    department VARCHAR(64),
    birthday DATE,
    address VARCHAR(255),
    owner_user_id BIGINT NOT NULL,
    owner_org_id BIGINT NOT NULL,
    owner_user_name VARCHAR(50),
    last_activity_time DATETIME,
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_contact_account ON crm_contact (account_id);
CREATE INDEX IF NOT EXISTS idx_contact_org ON crm_contact (owner_org_id);
CREATE INDEX IF NOT EXISTS idx_contact_owner ON crm_contact (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_contact_mobile_hash ON crm_contact (mobile_hash);

-- ---------- 4. 商机 ----------
CREATE TABLE IF NOT EXISTS crm_opportunity (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    account_id BIGINT NOT NULL,
    product_id BIGINT,
    expected_amount DECIMAL(18,4),
    expected_close_date DATE,
    sales_stage VARCHAR(16) NOT NULL DEFAULT 'PROSPECT',
    win_probability DECIMAL(5,2),
    est_revenue DECIMAL(18,4),
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
    est_cost DECIMAL(18,4),
    est_profit_rate DECIMAL(8,2),
    est_service_volume VARCHAR(64),
    pricing_mode VARCHAR(16) NOT NULL,
    pricing_detail VARCHAR(4000),
    owner_user_id BIGINT NOT NULL,
    owner_org_id BIGINT NOT NULL,
    owner_user_name VARCHAR(50),
    source_lead_id BIGINT,
    is_won TINYINT NOT NULL DEFAULT 0,
    contract_id BIGINT,
    address VARCHAR(255),
    lost_reason VARCHAR(255),
    stage_change_time DATETIME,
    last_activity_time DATETIME,
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_opp_account ON crm_opportunity (account_id);
CREATE INDEX IF NOT EXISTS idx_opp_org_stage ON crm_opportunity (owner_org_id, sales_stage);
CREATE INDEX IF NOT EXISTS idx_opp_owner ON crm_opportunity (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_opp_won ON crm_opportunity (is_won);

-- ---------- 5. 竞对 ----------
CREATE TABLE IF NOT EXISTS crm_competitor (
    id BIGINT NOT NULL PRIMARY KEY,
    opportunity_id BIGINT NOT NULL,
    competitor_name VARCHAR(128) NOT NULL,
    staff_size INT,
    coverage_scope VARCHAR(32),
    tech_ability VARCHAR(8),
    pricing_mode VARCHAR(16),
    pricing_detail VARCHAR(4000),
    operation_efficiency VARCHAR(8),
    operation_quality VARCHAR(8),
    owner_user_id BIGINT NOT NULL,
    owner_org_id BIGINT NOT NULL,
    owner_user_name VARCHAR(50),
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_comp_opp ON crm_competitor (opportunity_id);
CREATE INDEX IF NOT EXISTS idx_comp_org ON crm_competitor (owner_org_id);

-- ---------- 6. 活动（多态挂接） ----------
CREATE TABLE IF NOT EXISTS crm_activity (
    id BIGINT NOT NULL PRIMARY KEY,
    subject VARCHAR(128) NOT NULL,
    type VARCHAR(16) NOT NULL,
    level VARCHAR(8),
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    duration_days INT,
    target_type VARCHAR(16) NOT NULL,
    target_id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    owner_org_id BIGINT NOT NULL,
    owner_user_name VARCHAR(50),
    description VARCHAR(2000),
    file_ids VARCHAR(500),
    status VARCHAR(16) NOT NULL DEFAULT 'VALID',
    void_reason VARCHAR(255),
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_act_target ON crm_activity (target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_act_owner_time ON crm_activity (owner_user_id, start_time);
CREATE INDEX IF NOT EXISTS idx_act_org ON crm_activity (owner_org_id);

-- ---------- 7. 合同 ----------
CREATE TABLE IF NOT EXISTS crm_contract (
    id BIGINT NOT NULL PRIMARY KEY,
    contract_no VARCHAR(32) NOT NULL,
    name VARCHAR(200),
    account_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    business_group_id BIGINT NOT NULL,
    product_id BIGINT,
    sign_date DATE NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    duration_days INT,
    contract_type VARCHAR(8) NOT NULL DEFAULT 'FIRST',
    first_contract_id BIGINT,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    source_type VARCHAR(16) NOT NULL,
    source_id BIGINT,
    win_amount DECIMAL(18,4),
    owner_user_id BIGINT NOT NULL,
    owner_org_id BIGINT NOT NULL,
    owner_user_name VARCHAR(50),
    is_archived TINYINT NOT NULL DEFAULT 0,
    archive_time DATETIME,
    archive_user_id BIGINT,
    paper_box_no VARCHAR(64),
    paper_copies INT,
    activate_time DATETIME,
    terminate_time DATETIME,
    terminate_reason VARCHAR(255),
    flow_instance_id VARCHAR(64),
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_contract_no ON crm_contract (contract_no);
CREATE INDEX IF NOT EXISTS idx_contract_account ON crm_contract (account_id);
CREATE INDEX IF NOT EXISTS idx_contract_org_status ON crm_contract (owner_org_id, status);
CREATE INDEX IF NOT EXISTS idx_contract_bg ON crm_contract (business_group_id);
CREATE INDEX IF NOT EXISTS idx_contract_source ON crm_contract (source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_contract_end ON crm_contract (end_date);

-- ---------- 8. 协议 ----------
CREATE TABLE IF NOT EXISTS crm_agreement (
    id BIGINT NOT NULL PRIMARY KEY,
    agreement_no VARCHAR(32) NOT NULL,
    name VARCHAR(200),
    agreement_type VARCHAR(32) NOT NULL,
    account_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    opportunity_id BIGINT,
    product_id BIGINT,
    business_group_id BIGINT NOT NULL,
    sign_date DATE NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    duration_days INT,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    sign_type VARCHAR(8) NOT NULL DEFAULT 'FIRST',
    first_agreement_id BIGINT,
    win_amount DECIMAL(18,4),
    owner_user_id BIGINT NOT NULL,
    owner_org_id BIGINT NOT NULL,
    owner_user_name VARCHAR(50),
    is_archived TINYINT NOT NULL DEFAULT 0,
    archive_time DATETIME,
    paper_box_no VARCHAR(64),
    paper_copies INT,
    activate_time DATETIME,
    flow_instance_id VARCHAR(64),
    terminate_reason VARCHAR(255),
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_no ON crm_agreement (agreement_no);
CREATE INDEX IF NOT EXISTS idx_agree_account ON crm_agreement (account_id);
CREATE INDEX IF NOT EXISTS idx_agree_org_status ON crm_agreement (owner_org_id, status);
CREATE INDEX IF NOT EXISTS idx_agree_end ON crm_agreement (end_date);

-- ---------- 9. 投标 ----------
CREATE TABLE IF NOT EXISTS crm_bid (
    id BIGINT NOT NULL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    tender_name VARCHAR(255) NOT NULL,
    tender_no VARCHAR(64),
    tender_form VARCHAR(32) NOT NULL,
    bid_entity BIGINT NOT NULL,
    province VARCHAR(32),
    city VARCHAR(32),
    region_org_id BIGINT,
    price_cap DECIMAL(18,4),
    open_date DATE NOT NULL,
    deadline DATE NOT NULL,
    deposit DECIMAL(18,4),
    deposit_status VARCHAR(16),
    bid_status VARCHAR(16) NOT NULL DEFAULT 'TRACKING',
    win_amount DECIMAL(18,4),
    win_result VARCHAR(255),
    bid_plan VARCHAR(500),
    tender_url VARCHAR(512),
    win_url VARCHAR(512),
    is_won TINYINT NOT NULL DEFAULT 0,
    owner_user_id BIGINT NOT NULL,
    owner_org_id BIGINT NOT NULL,
    owner_user_name VARCHAR(50),
    lost_reason VARCHAR(255),
    abandon_reason VARCHAR(255),
    gen_opportunity_id BIGINT,
    gen_contract_id BIGINT,
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_bid_account ON crm_bid (account_id);
CREATE INDEX IF NOT EXISTS idx_bid_org_status ON crm_bid (owner_org_id, bid_status);
CREATE INDEX IF NOT EXISTS idx_bid_open_date ON crm_bid (open_date);
CREATE INDEX IF NOT EXISTS idx_bid_region ON crm_bid (region_org_id);

-- ---------- 10. 线索转化日志（幂等最后防线） ----------
CREATE TABLE IF NOT EXISTS crm_lead_convert_log (
    id BIGINT NOT NULL PRIMARY KEY,
    lead_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    contact_id BIGINT NOT NULL,
    opportunity_id BIGINT NOT NULL,
    convert_mode VARCHAR(16) NOT NULL,
    operator_id BIGINT NOT NULL,
    operator_org_id BIGINT NOT NULL,
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_convert_lead ON crm_lead_convert_log (lead_id);
CREATE INDEX IF NOT EXISTS idx_convert_lead ON crm_lead_convert_log (lead_id);

-- ---------- 11. 客户池流转日志 ----------
CREATE TABLE IF NOT EXISTS crm_account_pool_log (
    id BIGINT NOT NULL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    from_status VARCHAR(16) NOT NULL,
    to_status VARCHAR(16) NOT NULL,
    trigger_type VARCHAR(16) NOT NULL,
    reason VARCHAR(255),
    operator_id BIGINT,
    operator_org_id BIGINT,
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pool_account ON crm_account_pool_log (account_id, created_time);

-- ---------- 12. 认领申请 ----------
CREATE TABLE IF NOT EXISTS crm_claim_apply (
    id BIGINT NOT NULL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    applicant_org_id BIGINT NOT NULL,
    old_owner_id BIGINT,
    status VARCHAR(16) NOT NULL DEFAULT 'APPROVING',
    flow_instance_id VARCHAR(64),
    appeal_deadline DATETIME,
    appeal_status VARCHAR(16) DEFAULT 'NONE',
    apply_reason VARCHAR(255),
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_claim ON crm_claim_apply (account_id, applicant_id, status);
CREATE INDEX IF NOT EXISTS idx_claim_status ON crm_claim_apply (status);

-- ---------- 13. 导入批次 ----------
CREATE TABLE IF NOT EXISTS crm_import_batch (
    id BIGINT NOT NULL PRIMARY KEY,
    biz_type VARCHAR(32) NOT NULL,
    file_id BIGINT NOT NULL,
    error_file_id BIGINT,
    total_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    fail_count INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    error_detail VARCHAR(4000),
    operator_id BIGINT NOT NULL,
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_import_operator ON crm_import_batch (operator_id, created_time);

-- ---------- 埋点（底座无独立埋点服务时的落地表） ----------
CREATE TABLE IF NOT EXISTS crm_event_log (
    id BIGINT NOT NULL PRIMARY KEY,
    event_code VARCHAR(32) NOT NULL,
    biz_id BIGINT,
    operator_id BIGINT,
    operator_org_id BIGINT,
    props VARCHAR(2000),
    created_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_event_code ON crm_event_log (event_code, created_time);

-- ---------- 编码规则（底座 P1 能力，CRM 合同/协议号依赖） ----------
CREATE TABLE IF NOT EXISTS sys_code_rule (
    id BIGINT NOT NULL PRIMARY KEY,
    rule_code VARCHAR(50) NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    prefix VARCHAR(20),
    date_format VARCHAR(20),
    seq_length INT NOT NULL DEFAULT 4,
    reset_cycle VARCHAR(16) NOT NULL DEFAULT 'MONTH',
    current_seq INT NOT NULL DEFAULT 0,
    current_period VARCHAR(20),
    sample VARCHAR(100),
    status TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_code_rule ON sys_code_rule (rule_code);
