-- ============================================================
-- 国联征信项目管理平台 · 开发档 H2 Schema（MySQL 兼容模式）
-- 与 sql/01_schema.sql（MySQL 8.0）保持一致，仅类型微调
-- 26 张自建表，不含任何 CRM / 项目管理业务表
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    employee_no VARCHAR(30),
    mobile VARCHAR(20),
    email VARCHAR(100),
    gender TINYINT,
    avatar VARCHAR(255),
    id_card VARCHAR(255),
    employee_type VARCHAR(20),
    employment_form VARCHAR(20),
    position_name VARCHAR(50),
    job_level VARCHAR(50),
    join_date DATE,
    leave_date DATE,
    status TINYINT NOT NULL DEFAULT 1,
    default_role_id BIGINT,
    leader_id BIGINT,
    first_login TINYINT DEFAULT 1,
    last_login_time DATETIME,
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_username UNIQUE (username)
);
CREATE INDEX IF NOT EXISTS idx_user_status ON sys_user (status);
CREATE INDEX IF NOT EXISTS idx_user_created ON sys_user (created_time);

CREATE TABLE IF NOT EXISTS sys_org (
    id BIGINT NOT NULL PRIMARY KEY,
    org_code VARCHAR(50) NOT NULL,
    org_name VARCHAR(100) NOT NULL,
    org_type VARCHAR(20) NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    ancestors VARCHAR(500),
    leader_id BIGINT,
    cost_center_code VARCHAR(50),
    sort_no INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    effective_date DATE,
    expire_date DATE,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_org_code UNIQUE (org_code)
);
CREATE INDEX IF NOT EXISTS idx_org_parent ON sys_org (parent_id);

CREATE TABLE IF NOT EXISTS sys_company (
    id BIGINT NOT NULL PRIMARY KEY,
    company_code VARCHAR(50) NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    short_name VARCHAR(100),
    company_type VARCHAR(20),
    parent_company_id BIGINT,
    legal_person VARCHAR(50),
    credit_code VARCHAR(30),
    reg_address VARCHAR(300),
    office_address VARCHAR(300),
    tax_no VARCHAR(50),
    bank_name VARCHAR(100),
    bank_account VARCHAR(100),
    status TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_company_code UNIQUE (company_code),
    CONSTRAINT uk_company_credit UNIQUE (credit_code)
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    org_id BIGINT NOT NULL,
    description VARCHAR(200),
    role_type TINYINT NOT NULL DEFAULT 2,
    data_scope VARCHAR(30) NOT NULL DEFAULT 'SELF',
    scope_value VARCHAR(2000),
    can_be_project_member TINYINT NOT NULL DEFAULT 0,
    can_be_approver TINYINT NOT NULL DEFAULT 0,
    sort_no INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_role_code UNIQUE (role_code)
);
CREATE INDEX IF NOT EXISTS idx_role_org ON sys_role (org_id);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    is_default TINYINT NOT NULL DEFAULT 0,
    position_name VARCHAR(50),
    job_level VARCHAR(50),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);
CREATE INDEX IF NOT EXISTS idx_ur_role ON sys_user_role (role_id);

CREATE TABLE IF NOT EXISTS sys_user_company (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    relation_type TINYINT NOT NULL DEFAULT 1,
    is_primary TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_uc_user ON sys_user_company (user_id);

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT NOT NULL PRIMARY KEY,
    parent_id BIGINT NOT NULL DEFAULT 0,
    menu_name VARCHAR(50) NOT NULL,
    menu_type TINYINT NOT NULL,
    path VARCHAR(200),
    component VARCHAR(200),
    perms VARCHAR(100),
    icon VARCHAR(50),
    sort_no INT NOT NULL DEFAULT 0,
    visible TINYINT NOT NULL DEFAULT 1,
    status TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_menu_parent ON sys_menu (parent_id);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT NOT NULL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_role_menu UNIQUE (role_id, menu_id)
);

CREATE TABLE IF NOT EXISTS sys_data_scope_dept (
    id BIGINT NOT NULL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    org_id BIGINT NOT NULL,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_dsd_role ON sys_data_scope_dept (role_id);

CREATE TABLE IF NOT EXISTS sys_role_field_mask (
    id BIGINT NOT NULL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    group_code VARCHAR(50) NOT NULL,
    mask_level VARCHAR(20) NOT NULL,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_role_group UNIQUE (role_id, group_code)
);

CREATE TABLE IF NOT EXISTS sys_sensitive_group (
    id BIGINT NOT NULL PRIMARY KEY,
    group_code VARCHAR(50) NOT NULL,
    group_name VARCHAR(50) NOT NULL,
    default_level VARCHAR(20) NOT NULL DEFAULT 'MASKED',
    risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    sort_no INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_sg_code UNIQUE (group_code)
);

CREATE TABLE IF NOT EXISTS sys_sensitive_field (
    id BIGINT NOT NULL PRIMARY KEY,
    group_code VARCHAR(50) NOT NULL,
    biz_object VARCHAR(30) NOT NULL,
    field_name VARCHAR(50) NOT NULL,
    field_label VARCHAR(50),
    mask_rule VARCHAR(30) NOT NULL,
    mask_param VARCHAR(50),
    allow_plain TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_sf_group ON sys_sensitive_field (group_code);

CREATE TABLE IF NOT EXISTS sys_sensitive_log (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT,
    role_id BIGINT,
    biz_type VARCHAR(30),
    biz_id BIGINT,
    field_name VARCHAR(50),
    group_code VARCHAR(50),
    ip VARCHAR(60),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGINT NOT NULL PRIMARY KEY,
    dict_type VARCHAR(50) NOT NULL,
    dict_name VARCHAR(50) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_dict_type UNIQUE (dict_type)
);

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id BIGINT NOT NULL PRIMARY KEY,
    dict_type VARCHAR(50) NOT NULL,
    dict_label VARCHAR(50) NOT NULL,
    dict_value VARCHAR(50) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    css_class VARCHAR(50),
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_di_type ON sys_dict_item (dict_type);

CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT NOT NULL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(500),
    config_name VARCHAR(100),
    config_type VARCHAR(20) DEFAULT 'STRING',
    builtin TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_config_key UNIQUE (config_key)
);

CREATE TABLE IF NOT EXISTS sys_notice (
    id BIGINT NOT NULL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    notice_type TINYINT NOT NULL DEFAULT 1,
    content CLOB,
    receiver_scope VARCHAR(50) DEFAULT 'ALL',
    receiver_id BIGINT,
    pinned TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    publish_time DATETIME,
    todo_status TINYINT DEFAULT 0,
    biz_type VARCHAR(30),
    biz_id BIGINT,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_notice_receiver ON sys_notice (receiver_id);
CREATE INDEX IF NOT EXISTS idx_notice_type ON sys_notice (notice_type);

CREATE TABLE IF NOT EXISTS sys_notice_read (
    id BIGINT NOT NULL PRIMARY KEY,
    notice_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_time DATETIME,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_nr_user ON sys_notice_read (user_id);

CREATE TABLE IF NOT EXISTS sys_file (
    id BIGINT NOT NULL PRIMARY KEY,
    file_name VARCHAR(200) NOT NULL,
    original_name VARCHAR(200),
    file_path VARCHAR(500),
    file_size BIGINT,
    content_type VARCHAR(100),
    biz_type VARCHAR(50),
    biz_id BIGINT,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_file_biz ON sys_file (biz_type, biz_id);

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id BIGINT NOT NULL PRIMARY KEY,
    module VARCHAR(50),
    action VARCHAR(50),
    method VARCHAR(200),
    request_uri VARCHAR(200),
    params CLOB,
    result CLOB,
    operator_id BIGINT,
    operator_name VARCHAR(50),
    role_name VARCHAR(50),
    ip VARCHAR(60),
    cost_ms BIGINT,
    success TINYINT DEFAULT 1,
    error_msg VARCHAR(1000),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ol_time ON sys_oper_log (created_time);

CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGINT NOT NULL PRIMARY KEY,
    username VARCHAR(50),
    user_id BIGINT,
    login_type VARCHAR(20),
    ip VARCHAR(60),
    location VARCHAR(100),
    browser VARCHAR(100),
    os VARCHAR(100),
    success TINYINT DEFAULT 1,
    fail_reason VARCHAR(200),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ll_time ON sys_login_log (created_time);

CREATE TABLE IF NOT EXISTS sys_job (
    id BIGINT NOT NULL PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL,
    job_code VARCHAR(50) NOT NULL,
    cron VARCHAR(50),
    status TINYINT NOT NULL DEFAULT 0,
    last_exec_time DATETIME,
    remark VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_job_code UNIQUE (job_code)
);

CREATE TABLE IF NOT EXISTS sys_flow_def (
    id BIGINT NOT NULL PRIMARY KEY,
    flow_code VARCHAR(50) NOT NULL,
    flow_name VARCHAR(100) NOT NULL,
    biz_type VARCHAR(50) NOT NULL,
    org_scope VARCHAR(100),
    process_definition_key VARCHAR(100),
    deployed_version INT,
    node_config CLOB,
    status TINYINT NOT NULL DEFAULT 0,
    form_code VARCHAR(50),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_flow_code UNIQUE (flow_code)
);

CREATE TABLE IF NOT EXISTS sys_flow_biz (
    id BIGINT NOT NULL PRIMARY KEY,
    biz_type VARCHAR(50) NOT NULL,
    biz_id BIGINT NOT NULL,
    biz_no VARCHAR(50),
    biz_title VARCHAR(200),
    process_instance_id VARCHAR(64),
    flow_code VARCHAR(50),
    flow_version INT,
    initiator_id BIGINT,
    initiator_org_id BIGINT,
    status VARCHAR(20),
    current_node_name VARCHAR(100),
    start_time DATETIME,
    end_time DATETIME,
    detail_route VARCHAR(200),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fb_biz ON sys_flow_biz (biz_type, biz_id);
CREATE INDEX IF NOT EXISTS idx_fb_pi ON sys_flow_biz (process_instance_id);
CREATE INDEX IF NOT EXISTS idx_fb_initiator ON sys_flow_biz (initiator_id);

CREATE TABLE IF NOT EXISTS sys_account_transfer (
    id BIGINT NOT NULL PRIMARY KEY,
    transfer_no VARCHAR(50) NOT NULL,
    from_user_id BIGINT NOT NULL,
    to_user_ids VARCHAR(500) NOT NULL,
    assign_mode TINYINT NOT NULL DEFAULT 1,
    scope VARCHAR(500) NOT NULL,
    role_assignments VARCHAR(1000),
    remark VARCHAR(500),
    effective_time DATETIME,
    flow_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    exec_status VARCHAR(20) NOT NULL DEFAULT 'NOT_EXECUTED',
    flow_instance_id VARCHAR(64),
    item_count INT DEFAULT 0,
    fail_count INT DEFAULT 0,
    revoke_deadline DATETIME,
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_transfer_no UNIQUE (transfer_no)
);
CREATE INDEX IF NOT EXISTS idx_at_from ON sys_account_transfer (from_user_id);
CREATE INDEX IF NOT EXISTS idx_at_flow ON sys_account_transfer (flow_status);

CREATE TABLE IF NOT EXISTS sys_account_transfer_item (
    id BIGINT NOT NULL PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    biz_type VARCHAR(50),
    biz_id BIGINT,
    biz_name VARCHAR(200),
    before_owner_id BIGINT,
    after_owner_id BIGINT,
    result VARCHAR(20),
    fail_reason VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME,
    updated_by BIGINT,
    updated_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ati_transfer ON sys_account_transfer_item (transfer_id);
