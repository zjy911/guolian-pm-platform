-- ============================================================
-- 国联政信项目管理平台 · 项目管理模块种子数据（演示）
-- ============================================================

-- 1. 编码规则：项目编号 PRJ-G-yyyyMM-NNN
INSERT INTO sys_code_rule (id, rule_code, rule_name, prefix, date_format, seq_length, reset_cycle, current_seq, current_period, sample, status, remark) VALUES
 (3,'PM_PROJECT','项目编号','PRJ-G-','yyyyMM',3,'MONTH',0,NULL,'PRJ-G-202610-001',1,'项目管理模块按立项年月编号');

-- 2. 审批流定义：立项单 + 通用表单
INSERT INTO sys_flow_def (id, flow_code, flow_name, biz_type, org_scope, process_definition_key, deployed_version, status, form_code, node_config) VALUES
 (6,'PM_INITIATION','PM-立项审批','PM_INITIATION',NULL,'PM_INITIATION',1,1,'PM_INITIATION',
  '{"nodes":[{"key":"start","name":"发起人","type":"START"},{"key":"directLeader","name":"直属上级","type":"SINGLE","approverType":"LEADER","level":1},{"key":"amountGateway","name":"金额判断","type":"CONDITION","expression":"estimatedAmount > 5000000"},{"key":"finance","name":"财务审核","type":"SINGLE","approverType":"ROLE","roleCode":"ROLE_GROUP_ADMIN"},{"key":"bgLeader","name":"BG负责人","type":"SINGLE","approverType":"ORG_LEADER"},{"key":"end","name":"结束","type":"END"}]}'),
 (7,'PM_FORM','PM-表单通用审批','PM_FORM',NULL,'PM_FORM',1,1,'PM_FORM',
  '{"nodes":[{"key":"start","name":"发起人","type":"START"},{"key":"directLeader","name":"直属上级","type":"SINGLE","approverType":"LEADER","level":1},{"key":"deptLeader","name":"部门负责人","type":"SINGLE","approverType":"ORG_LEADER"},{"key":"end","name":"结束","type":"END"}]}');

-- 3. 产品与任务模板
INSERT INTO pm_product (id, product_code, product_name, bg_org_id, bg_org_name, pricing_mode, template_version, status, remark, owner_user_id, owner_org_id, created_time) VALUES
 (1,'PM-PROD-001','法院全域诉源治理调解服务',2,'G端BG','UNIT_PRICE','v1.0','ON_SALE','面向基层法院的诉前调解驻场服务',2,2,'2026-09-01 10:00:00'),
 (2,'PM-PROD-002','金融纠纷多元调解平台服务',2,'G端BG','COMMISSION','v1.0','ON_SALE','面向银行信用卡/个贷不良的云端调解',2,2,'2026-09-05 10:00:00'),
 (3,'PM-PROD-003','政务热线数据治理服务',6,'数据资产BG','TOTAL_PACKAGE','v1.0','ON_SALE','12345 热线工单治理与数据分析',5,6,'2026-09-10 10:00:00');

INSERT INTO pm_task_template (id, product_id, version, template_json, status, publish_time, created_time) VALUES
 (11,1,1,'[{"phase":1,"phaseName":"项目立项","category":"立项申报","tasks":[{"name":"立项申请单","type":"FORM","formCode":"pm_form_initiation","blocking":true},{"name":"立项变更单","type":"FORM","formCode":"pm_form_init_change","blocking":false}]},{"phase":2,"phaseName":"项目筹备","category":"预算与编制","tasks":[{"name":"项目预算单","type":"FORM","formCode":"pm_form_budget","blocking":true},{"name":"编制申请单","type":"FORM","formCode":"pm_form_headcount","blocking":false}]},{"phase":2,"phaseName":"项目筹备","category":"资源与标准","tasks":[{"name":"资源申请单","type":"FORM","formCode":"pm_form_resource_apply","blocking":false},{"name":"目标责任书","type":"FORM","formCode":"pm_form_target_responsibility","blocking":false}]},{"phase":2,"phaseName":"项目筹备","category":"计划","tasks":[{"name":"项目计划单","type":"FORM","formCode":"pm_form_project_plan","blocking":true}]},{"phase":3,"phaseName":"项目实施","category":"进场","tasks":[{"name":"进场确认单","type":"FORM","formCode":"pm_form_site_entry","blocking":false}]},{"phase":3,"phaseName":"项目实施","category":"分案","tasks":[{"name":"分案机制确认单","type":"FORM","formCode":"pm_form_case_dispatch","blocking":false}]},{"phase":3,"phaseName":"项目实施","category":"人员","tasks":[{"name":"人员上岗认证单","type":"FORM","formCode":"pm_form_staff_certification","blocking":false},{"name":"人员任命/调拨单","type":"FORM","formCode":"pm_form_staff_transfer","blocking":false}]},{"phase":4,"phaseName":"项目交接","category":"验收","tasks":[{"name":"验收单","type":"FORM","formCode":"pm_form_acceptance","blocking":true},{"name":"整改/复验单","type":"FORM","formCode":"pm_form_rectification","blocking":false}]},{"phase":4,"phaseName":"项目交接","category":"交接","tasks":[{"name":"项目交接单","type":"FORM","formCode":"pm_form_project_handover","blocking":true},{"name":"复盘报告","type":"FORM","formCode":"pm_form_debrief_report","blocking":false}]},{"phase":5,"phaseName":"项目运营","category":"爬坡","tasks":[{"name":"爬坡期跟踪","type":"DATA","formCode":"pm_form_ramp_up","blocking":false}]},{"phase":5,"phaseName":"项目运营","category":"分级","tasks":[{"name":"项目分级","type":"FORM","formCode":"pm_form_project_grade","blocking":false}]},{"phase":5,"phaseName":"项目运营","category":"财务","tasks":[{"name":"回款台账","type":"DATA","formCode":"pm_form_payment_ledger","blocking":true}]},{"phase":5,"phaseName":"项目运营","category":"质量","tasks":[{"name":"质量抽检与投诉","type":"FORM","formCode":"pm_form_quality_inspection","blocking":false},{"name":"合规检查","type":"FORM","formCode":"pm_form_compliance_check","blocking":false}]},{"phase":5,"phaseName":"项目运营","category":"客户","tasks":[{"name":"客户满意度","type":"FORM","formCode":"pm_form_client_satisfaction","blocking":false}]},{"phase":5,"phaseName":"项目运营","category":"健康度","tasks":[{"name":"健康度评分卡","type":"DATA","formCode":"pm_form_health_scorecard","blocking":false}]},{"phase":6,"phaseName":"项目撤场","category":"申请","tasks":[{"name":"撤场申请单","type":"FORM","formCode":"pm_form_closing_apply","blocking":true}]},{"phase":6,"phaseName":"项目撤场","category":"执行","tasks":[{"name":"撤场方案","type":"FORM","formCode":"pm_form_closing_plan","blocking":false}]},{"phase":6,"phaseName":"项目撤场","category":"移交","tasks":[{"name":"卷宗移交清单","type":"COLLAB","formCode":"pm_form_case_file_transfer","blocking":false},{"name":"资产处置单","type":"COLLAB","formCode":"pm_form_asset_disposal","blocking":false}]},{"phase":6,"phaseName":"项目撤场","category":"收口","tasks":[{"name":"撤场清算表","type":"FORM","formCode":"pm_form_final_settlement","blocking":true},{"name":"档案封存","type":"FORM","formCode":"pm_form_archive_seal","blocking":false}]}]','PUBLISHED','2026-09-01 10:00:00','2026-09-01 09:00:00');
INSERT INTO pm_task_template (id, product_id, version, template_json, status, publish_time, created_time) VALUES
 (12,2,1,'[{"phase":1,"phaseName":"项目立项","category":"立项申报","tasks":[{"name":"立项申请单","type":"FORM","formCode":"pm_form_initiation","blocking":true}]},{"phase":2,"phaseName":"项目筹备","category":"预算与编制","tasks":[{"name":"项目预算单","type":"FORM","formCode":"pm_form_budget","blocking":true},{"name":"编制申请单","type":"FORM","formCode":"pm_form_headcount","blocking":false}]},{"phase":2,"phaseName":"项目筹备","category":"资源与标准","tasks":[{"name":"资源申请单","type":"FORM","formCode":"pm_form_resource_apply","blocking":false},{"name":"目标责任书","type":"FORM","formCode":"pm_form_target_responsibility","blocking":false}]},{"phase":2,"phaseName":"项目筹备","category":"计划","tasks":[{"name":"项目计划单","type":"FORM","formCode":"pm_form_project_plan","blocking":true}]},{"phase":3,"phaseName":"项目实施","category":"进场","tasks":[{"name":"进场确认单","type":"FORM","formCode":"pm_form_site_entry","blocking":false}]},{"phase":3,"phaseName":"项目实施","category":"分案","tasks":[{"name":"分案机制确认单","type":"FORM","formCode":"pm_form_case_dispatch","blocking":false}]},{"phase":3,"phaseName":"项目实施","category":"人员","tasks":[{"name":"人员上岗认证单","type":"FORM","formCode":"pm_form_staff_certification","blocking":false},{"name":"人员任命/调拨单","type":"FORM","formCode":"pm_form_staff_transfer","blocking":false}]},{"phase":4,"phaseName":"项目交接","category":"验收","tasks":[{"name":"验收单","type":"FORM","formCode":"pm_form_acceptance","blocking":true}]},{"phase":4,"phaseName":"项目交接","category":"交接","tasks":[{"name":"项目交接单","type":"FORM","formCode":"pm_form_project_handover","blocking":true},{"name":"复盘报告","type":"FORM","formCode":"pm_form_debrief_report","blocking":false}]},{"phase":5,"phaseName":"项目运营","category":"爬坡","tasks":[{"name":"爬坡期跟踪","type":"DATA","formCode":"pm_form_ramp_up","blocking":false}]},{"phase":5,"phaseName":"项目运营","category":"分级","tasks":[{"name":"项目分级","type":"FORM","formCode":"pm_form_project_grade","blocking":false}]},{"phase":5,"phaseName":"项目运营","category":"财务","tasks":[{"name":"回款台账","type":"DATA","formCode":"pm_form_payment_ledger","blocking":true}]},{"phase":5,"phaseName":"项目运营","category":"质量","tasks":[{"name":"质量抽检与投诉","type":"FORM","formCode":"pm_form_quality_inspection","blocking":false},{"name":"合规检查","type":"FORM","formCode":"pm_form_compliance_check","blocking":false}]},{"phase":5,"phaseName":"项目运营","category":"客户","tasks":[{"name":"客户满意度","type":"FORM","formCode":"pm_form_client_satisfaction","blocking":false}]},{"phase":5,"phaseName":"项目运营","category":"健康度","tasks":[{"name":"健康度评分卡","type":"DATA","formCode":"pm_form_health_scorecard","blocking":false}]},{"phase":6,"phaseName":"项目撤场","category":"申请","tasks":[{"name":"撤场申请单","type":"FORM","formCode":"pm_form_closing_apply","blocking":true}]},{"phase":6,"phaseName":"项目撤场","category":"执行","tasks":[{"name":"撤场方案","type":"FORM","formCode":"pm_form_closing_plan","blocking":false}]},{"phase":6,"phaseName":"项目撤场","category":"移交","tasks":[{"name":"卷宗移交清单","type":"COLLAB","formCode":"pm_form_case_file_transfer","blocking":false},{"name":"资产处置单","type":"COLLAB","formCode":"pm_form_asset_disposal","blocking":false}]},{"phase":6,"phaseName":"项目撤场","category":"收口","tasks":[{"name":"撤场清算表","type":"FORM","formCode":"pm_form_final_settlement","blocking":true},{"name":"档案封存","type":"FORM","formCode":"pm_form_archive_seal","blocking":false}]}]','PUBLISHED','2026-09-02 10:00:00','2026-09-02 09:00:00');
INSERT INTO pm_task_template (id, product_id, version, template_json, status, publish_time, created_time) VALUES
 (13,3,1,'[{"phase":1,"phaseName":"项目立项","category":"立项申报","tasks":[{"name":"立项申请单","type":"FORM","formCode":"pm_form_initiation","blocking":true}]},{"phase":2,"phaseName":"项目筹备","category":"预算与编制","tasks":[{"name":"项目预算单","type":"FORM","formCode":"pm_form_budget","blocking":true},{"name":"编制申请单","type":"FORM","formCode":"pm_form_headcount","blocking":false}]},{"phase":2,"phaseName":"项目筹备","category":"资源与标准","tasks":[{"name":"资源申请单","type":"FORM","formCode":"pm_form_resource_apply","blocking":false},{"name":"目标责任书","type":"FORM","formCode":"pm_form_target_responsibility","blocking":false}]},{"phase":2,"phaseName":"项目筹备","category":"计划","tasks":[{"name":"项目计划单","type":"FORM","formCode":"pm_form_project_plan","blocking":true}]},{"phase":3,"phaseName":"项目实施","category":"进场","tasks":[{"name":"进场确认单","type":"FORM","formCode":"pm_form_site_entry","blocking":false}]},{"phase":3,"phaseName":"项目实施","category":"分案","tasks":[{"name":"分案机制确认单","type":"FORM","formCode":"pm_form_case_dispatch","blocking":false}]},{"phase":3,"phaseName":"项目实施","category":"人员","tasks":[{"name":"人员上岗认证单","type":"FORM","formCode":"pm_form_staff_certification","blocking":false},{"name":"人员任命/调拨单","type":"FORM","formCode":"pm_form_staff_transfer","blocking":false}]},{"phase":4,"phaseName":"项目交接","category":"验收","tasks":[{"name":"验收单","type":"FORM","formCode":"pm_form_acceptance","blocking":true},{"name":"整改/复验单","type":"FORM","formCode":"pm_form_rectification","blocking":false}]},{"phase":4,"phaseName":"项目交接","category":"交接","tasks":[{"name":"项目交接单","type":"FORM","formCode":"pm_form_project_handover","blocking":true},{"name":"复盘报告","type":"FORM","formCode":"pm_form_debrief_report","blocking":false}]},{"phase":5,"phaseName":"项目运营","category":"爬坡","tasks":[{"name":"爬坡期跟踪","type":"DATA","formCode":"pm_form_ramp_up","blocking":false}]},{"phase":5,"phaseName":"项目运营","category":"分级","tasks":[{"name":"项目分级","type":"FORM","formCode":"pm_form_project_grade","blocking":false}]},{"phase":5,"phaseName":"项目运营","category":"财务","tasks":[{"name":"回款台账","type":"DATA","formCode":"pm_form_payment_ledger","blocking":true}]},{"phase":5,"phaseName":"项目运营","category":"质量","tasks":[{"name":"质量抽检与投诉","type":"FORM","formCode":"pm_form_quality_inspection","blocking":false},{"name":"合规检查","type":"FORM","formCode":"pm_form_compliance_check","blocking":false}]},{"phase":5,"phaseName":"项目运营","category":"客户","tasks":[{"name":"客户满意度","type":"FORM","formCode":"pm_form_client_satisfaction","blocking":false}]},{"phase":5,"phaseName":"项目运营","category":"健康度","tasks":[{"name":"健康度评分卡","type":"DATA","formCode":"pm_form_health_scorecard","blocking":false}]},{"phase":6,"phaseName":"项目撤场","category":"申请","tasks":[{"name":"撤场申请单","type":"FORM","formCode":"pm_form_closing_apply","blocking":true}]},{"phase":6,"phaseName":"项目撤场","category":"执行","tasks":[{"name":"撤场方案","type":"FORM","formCode":"pm_form_closing_plan","blocking":false}]},{"phase":6,"phaseName":"项目撤场","category":"移交","tasks":[{"name":"卷宗移交清单","type":"COLLAB","formCode":"pm_form_case_file_transfer","blocking":false},{"name":"资产处置单","type":"COLLAB","formCode":"pm_form_asset_disposal","blocking":false}]},{"phase":6,"phaseName":"项目撤场","category":"收口","tasks":[{"name":"撤场清算表","type":"FORM","formCode":"pm_form_final_settlement","blocking":true},{"name":"档案封存","type":"FORM","formCode":"pm_form_archive_seal","blocking":false}]}]','PUBLISHED','2026-09-03 10:00:00','2026-09-03 09:00:00');

-- 4. 演示项目（3 个，覆盖运营/筹备/实施三个阶段）
INSERT INTO pm_project (id, project_code, project_name, product_id, product_name, template_version, customer_id, customer_name, contract_no, sign_entity, estimated_amount, annual_cases, pricing_mode, rating, phase, status, health_score, health_light, gross_margin, org_id, owner_user_id, owner_org_id, owner_user_name, initiation_form_id, created_time) VALUES
 (1,'PRJ-G-202609-001','海淀区法院诉源治理调解服务项目',1,'法院全域诉源治理调解服务','v1.0',1003,'北京市政务服务管理局','HT202512-0010','国联政信有限公司',4800000,12000,'UNIT_PRICE','A',5,'OPERATING',87.5,'GREEN',0.2834,2,2,2,'张伟',5001,'2026-09-20 10:00:00'),
 (2,'PRJ-G-202610-002','工商银行北京分行个贷调解项目',2,'金融纠纷多元调解平台服务','v1.0',1001,'中国工商银行股份有限公司北京市分行','HT202512-0003','国联政信有限公司',6500000,30000,'COMMISSION','B',2,'PREPARING',74.0,'YELLOW',0.2200,2,2,2,'张伟',5002,'2026-10-11 10:00:00'),
 (3,'PRJ-G-202610-003','12345 热线工单数据治理项目',3,'政务热线数据治理服务','v1.0',1003,'北京市政务服务管理局',NULL,'国联政信有限公司',1200000,8000,'TOTAL_PACKAGE','C',3,'IMPLEMENTING',66.0,'RED',0.1500,6,4,6,'王强',5003,'2026-10-18 10:00:00');

-- 5. 项目成员（投入比例：张伟 30+30=60，王强 40+30=70，均在 100 内）
INSERT INTO pm_project_member (id, project_id, user_id, user_name, project_role, input_ratio, join_date, expected_leave_date, status, evaluation, created_time) VALUES
 (101,1,2,'张伟','PROJECT_MANAGER',30,'2026-09-20','2027-09-19','ACTIVE',NULL,'2026-09-20 10:00:00'),
 (102,1,3,'李娜','IMPLEMENT_LEADER',40,'2026-09-25','2027-09-19','ACTIVE',NULL,'2026-09-25 10:00:00'),
 (103,1,4,'王强','FIELD_MEDIATOR',30,'2026-10-01',NULL,'ACTIVE',NULL,'2026-10-01 10:00:00'),
 (104,1,5,'刘洋','QC',20,'2026-10-08','2026-12-31','ACTIVE',NULL,'2026-10-08 10:00:00'),
 (105,2,2,'张伟','PROJECT_MANAGER',30,'2026-10-11','2027-10-10','ACTIVE',NULL,'2026-10-11 10:00:00'),
 (106,2,6,'陈静','BUSINESS_REP',30,'2026-10-15',NULL,'ACTIVE',NULL,'2026-10-15 10:00:00'),
 (107,3,4,'王强','PROJECT_MANAGER',40,'2026-10-18','2026-12-31','ACTIVE',NULL,'2026-10-18 10:00:00'),
 (108,3,7,'赵磊','FIELD_MEDIATOR',30,'2026-10-20',NULL,'ACTIVE',NULL,'2026-10-20 10:00:00');

-- 6. 项目任务实例（按模板快照实例化）
INSERT INTO pm_project_task (id, project_id, phase, category, task_name, task_type, form_code, blocking, depends_task_id, status, assignee_id, assignee_name, due_date, sort_no, completed_time, created_time, updated_time) VALUES
 (9010000,1,1,'立项申报','立项申请单','FORM','pm_form_initiation',1,NULL,'DONE',2,'张伟',NULL,0,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9010001,1,1,'立项申报','立项变更单','FORM','pm_form_init_change',0,NULL,'DONE',2,'张伟',NULL,1,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9010002,1,2,'预算与编制','项目预算单','FORM','pm_form_budget',1,NULL,'DONE',2,'张伟',NULL,2,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9010003,1,2,'预算与编制','编制申请单','FORM','pm_form_headcount',0,NULL,'DONE',2,'张伟',NULL,3,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9010004,1,2,'资源与标准','资源申请单','FORM','pm_form_resource_apply',0,NULL,'DONE',2,'张伟',NULL,4,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9010005,1,2,'资源与标准','目标责任书','FORM','pm_form_target_responsibility',0,NULL,'DONE',2,'张伟',NULL,5,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9010006,1,2,'计划','项目计划单','FORM','pm_form_project_plan',1,NULL,'DONE',2,'张伟',NULL,6,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9010007,1,3,'进场','进场确认单','FORM','pm_form_site_entry',0,NULL,'DONE',2,'张伟',NULL,7,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9010008,1,3,'分案','分案机制确认单','FORM','pm_form_case_dispatch',0,NULL,'DONE',2,'张伟',NULL,8,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9010009,1,3,'人员','人员上岗认证单','FORM','pm_form_staff_certification',0,NULL,'DONE',2,'张伟',NULL,9,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9010010,1,3,'人员','人员任命/调拨单','FORM','pm_form_staff_transfer',0,NULL,'DONE',2,'张伟',NULL,10,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9010011,1,4,'验收','验收单','FORM','pm_form_acceptance',1,NULL,'DONE',2,'张伟',NULL,11,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9010012,1,4,'验收','整改/复验单','FORM','pm_form_rectification',0,NULL,'DONE',2,'张伟',NULL,12,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9010013,1,4,'交接','项目交接单','FORM','pm_form_project_handover',1,NULL,'DONE',2,'张伟',NULL,13,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9010014,1,4,'交接','复盘报告','FORM','pm_form_debrief_report',0,NULL,'DONE',2,'张伟',NULL,14,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9010015,1,5,'爬坡','爬坡期跟踪','DATA','pm_form_ramp_up',0,NULL,'IN_PROGRESS',2,'张伟','2026-05-15',15,NULL,'2026-10-01 09:00:00',NULL),
 (9010016,1,5,'分级','项目分级','FORM','pm_form_project_grade',0,NULL,'IN_PROGRESS',2,'张伟','2026-05-15',16,NULL,'2026-10-01 09:00:00',NULL),
 (9010017,1,5,'财务','回款台账','DATA','pm_form_payment_ledger',1,NULL,'IN_PROGRESS',2,'张伟','2026-05-15',17,NULL,'2026-10-01 09:00:00',NULL),
 (9010018,1,5,'质量','质量抽检与投诉','FORM','pm_form_quality_inspection',0,NULL,'IN_PROGRESS',2,'张伟','2026-05-15',18,NULL,'2026-10-01 09:00:00',NULL),
 (9010019,1,5,'质量','合规检查','FORM','pm_form_compliance_check',0,NULL,'IN_PROGRESS',2,'张伟','2026-05-15',19,NULL,'2026-10-01 09:00:00',NULL),
 (9010020,1,5,'客户','客户满意度','FORM','pm_form_client_satisfaction',0,NULL,'IN_PROGRESS',2,'张伟','2026-05-15',20,NULL,'2026-10-01 09:00:00',NULL),
 (9010021,1,5,'健康度','健康度评分卡','DATA','pm_form_health_scorecard',0,NULL,'IN_PROGRESS',2,'张伟','2026-05-15',21,NULL,'2026-10-01 09:00:00',NULL),
 (9010022,1,6,'申请','撤场申请单','FORM','pm_form_closing_apply',1,NULL,'NOT_STARTED',2,'张伟','2026-06-15',22,NULL,'2026-10-01 09:00:00',NULL),
 (9010023,1,6,'执行','撤场方案','FORM','pm_form_closing_plan',0,NULL,'NOT_STARTED',2,'张伟','2026-06-15',23,NULL,'2026-10-01 09:00:00',NULL),
 (9010024,1,6,'移交','卷宗移交清单','COLLAB','pm_form_case_file_transfer',0,NULL,'NOT_STARTED',2,'张伟','2026-06-15',24,NULL,'2026-10-01 09:00:00',NULL),
 (9010025,1,6,'移交','资产处置单','COLLAB','pm_form_asset_disposal',0,NULL,'NOT_STARTED',2,'张伟','2026-06-15',25,NULL,'2026-10-01 09:00:00',NULL),
 (9010026,1,6,'收口','撤场清算表','FORM','pm_form_final_settlement',1,NULL,'NOT_STARTED',2,'张伟','2026-06-15',26,NULL,'2026-10-01 09:00:00',NULL),
 (9010027,1,6,'收口','档案封存','FORM','pm_form_archive_seal',0,NULL,'NOT_STARTED',2,'张伟','2026-06-15',27,NULL,'2026-10-01 09:00:00',NULL),
 (9020000,2,1,'立项申报','立项申请单','FORM','pm_form_initiation',1,NULL,'DONE',2,'张伟',NULL,0,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9020001,2,2,'预算与编制','项目预算单','FORM','pm_form_budget',1,NULL,'APPROVING',2,'张伟','2026-02-15',1,NULL,'2026-10-01 09:00:00',NULL),
 (9020002,2,2,'预算与编制','编制申请单','FORM','pm_form_headcount',0,NULL,'IN_PROGRESS',2,'张伟','2026-02-15',2,NULL,'2026-10-01 09:00:00',NULL),
 (9020003,2,2,'资源与标准','资源申请单','FORM','pm_form_resource_apply',0,NULL,'IN_PROGRESS',2,'张伟','2026-02-15',3,NULL,'2026-10-01 09:00:00',NULL),
 (9020004,2,2,'资源与标准','目标责任书','FORM','pm_form_target_responsibility',0,NULL,'IN_PROGRESS',2,'张伟','2026-02-15',4,NULL,'2026-10-01 09:00:00',NULL),
 (9020005,2,2,'计划','项目计划单','FORM','pm_form_project_plan',1,NULL,'IN_PROGRESS',2,'张伟','2026-02-15',5,NULL,'2026-10-01 09:00:00',NULL),
 (9020006,2,3,'进场','进场确认单','FORM','pm_form_site_entry',0,NULL,'NOT_STARTED',2,'张伟','2026-03-15',6,NULL,'2026-10-01 09:00:00',NULL),
 (9020007,2,3,'分案','分案机制确认单','FORM','pm_form_case_dispatch',0,NULL,'NOT_STARTED',2,'张伟','2026-03-15',7,NULL,'2026-10-01 09:00:00',NULL),
 (9020008,2,3,'人员','人员上岗认证单','FORM','pm_form_staff_certification',0,NULL,'NOT_STARTED',2,'张伟','2026-03-15',8,NULL,'2026-10-01 09:00:00',NULL),
 (9020009,2,3,'人员','人员任命/调拨单','FORM','pm_form_staff_transfer',0,NULL,'NOT_STARTED',2,'张伟','2026-03-15',9,NULL,'2026-10-01 09:00:00',NULL),
 (9020010,2,4,'验收','验收单','FORM','pm_form_acceptance',1,NULL,'NOT_STARTED',2,'张伟','2026-04-15',10,NULL,'2026-10-01 09:00:00',NULL),
 (9020011,2,4,'交接','项目交接单','FORM','pm_form_project_handover',1,NULL,'NOT_STARTED',2,'张伟','2026-04-15',11,NULL,'2026-10-01 09:00:00',NULL),
 (9020012,2,4,'交接','复盘报告','FORM','pm_form_debrief_report',0,NULL,'NOT_STARTED',2,'张伟','2026-04-15',12,NULL,'2026-10-01 09:00:00',NULL),
 (9020013,2,5,'爬坡','爬坡期跟踪','DATA','pm_form_ramp_up',0,NULL,'NOT_STARTED',2,'张伟','2026-05-15',13,NULL,'2026-10-01 09:00:00',NULL),
 (9020014,2,5,'分级','项目分级','FORM','pm_form_project_grade',0,NULL,'NOT_STARTED',2,'张伟','2026-05-15',14,NULL,'2026-10-01 09:00:00',NULL),
 (9020015,2,5,'财务','回款台账','DATA','pm_form_payment_ledger',1,NULL,'NOT_STARTED',2,'张伟','2026-05-15',15,NULL,'2026-10-01 09:00:00',NULL),
 (9020016,2,5,'质量','质量抽检与投诉','FORM','pm_form_quality_inspection',0,NULL,'NOT_STARTED',2,'张伟','2026-05-15',16,NULL,'2026-10-01 09:00:00',NULL),
 (9020017,2,5,'质量','合规检查','FORM','pm_form_compliance_check',0,NULL,'NOT_STARTED',2,'张伟','2026-05-15',17,NULL,'2026-10-01 09:00:00',NULL),
 (9020018,2,5,'客户','客户满意度','FORM','pm_form_client_satisfaction',0,NULL,'NOT_STARTED',2,'张伟','2026-05-15',18,NULL,'2026-10-01 09:00:00',NULL),
 (9020019,2,5,'健康度','健康度评分卡','DATA','pm_form_health_scorecard',0,NULL,'NOT_STARTED',2,'张伟','2026-05-15',19,NULL,'2026-10-01 09:00:00',NULL),
 (9020020,2,6,'申请','撤场申请单','FORM','pm_form_closing_apply',1,NULL,'NOT_STARTED',2,'张伟','2026-06-15',20,NULL,'2026-10-01 09:00:00',NULL),
 (9020021,2,6,'执行','撤场方案','FORM','pm_form_closing_plan',0,NULL,'NOT_STARTED',2,'张伟','2026-06-15',21,NULL,'2026-10-01 09:00:00',NULL),
 (9020022,2,6,'移交','卷宗移交清单','COLLAB','pm_form_case_file_transfer',0,NULL,'NOT_STARTED',2,'张伟','2026-06-15',22,NULL,'2026-10-01 09:00:00',NULL),
 (9020023,2,6,'移交','资产处置单','COLLAB','pm_form_asset_disposal',0,NULL,'NOT_STARTED',2,'张伟','2026-06-15',23,NULL,'2026-10-01 09:00:00',NULL),
 (9020024,2,6,'收口','撤场清算表','FORM','pm_form_final_settlement',1,NULL,'NOT_STARTED',2,'张伟','2026-06-15',24,NULL,'2026-10-01 09:00:00',NULL),
 (9020025,2,6,'收口','档案封存','FORM','pm_form_archive_seal',0,NULL,'NOT_STARTED',2,'张伟','2026-06-15',25,NULL,'2026-10-01 09:00:00',NULL),
 (9030000,3,1,'立项申报','立项申请单','FORM','pm_form_initiation',1,NULL,'DONE',4,'王强',NULL,0,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9030001,3,2,'预算与编制','项目预算单','FORM','pm_form_budget',1,NULL,'DONE',4,'王强',NULL,1,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9030002,3,2,'预算与编制','编制申请单','FORM','pm_form_headcount',0,NULL,'DONE',4,'王强',NULL,2,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9030003,3,2,'资源与标准','资源申请单','FORM','pm_form_resource_apply',0,NULL,'DONE',4,'王强',NULL,3,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9030004,3,2,'资源与标准','目标责任书','FORM','pm_form_target_responsibility',0,NULL,'DONE',4,'王强',NULL,4,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9030005,3,2,'计划','项目计划单','FORM','pm_form_project_plan',1,NULL,'DONE',4,'王强',NULL,5,'2026-10-15 10:00:00','2026-10-01 09:00:00','2026-10-15 10:00:00'),
 (9030006,3,3,'进场','进场确认单','FORM','pm_form_site_entry',0,NULL,'IN_PROGRESS',4,'王强','2026-03-15',6,NULL,'2026-10-01 09:00:00',NULL),
 (9030007,3,3,'分案','分案机制确认单','FORM','pm_form_case_dispatch',0,NULL,'IN_PROGRESS',4,'王强','2026-03-15',7,NULL,'2026-10-01 09:00:00',NULL),
 (9030008,3,3,'人员','人员上岗认证单','FORM','pm_form_staff_certification',0,NULL,'IN_PROGRESS',4,'王强','2026-03-15',8,NULL,'2026-10-01 09:00:00',NULL),
 (9030009,3,3,'人员','人员任命/调拨单','FORM','pm_form_staff_transfer',0,NULL,'IN_PROGRESS',4,'王强','2026-03-15',9,NULL,'2026-10-01 09:00:00',NULL),
 (9030010,3,4,'验收','验收单','FORM','pm_form_acceptance',1,NULL,'NOT_STARTED',4,'王强','2026-04-15',10,NULL,'2026-10-01 09:00:00',NULL),
 (9030011,3,4,'验收','整改/复验单','FORM','pm_form_rectification',0,NULL,'NOT_STARTED',4,'王强','2026-04-15',11,NULL,'2026-10-01 09:00:00',NULL),
 (9030012,3,4,'交接','项目交接单','FORM','pm_form_project_handover',1,NULL,'NOT_STARTED',4,'王强','2026-04-15',12,NULL,'2026-10-01 09:00:00',NULL),
 (9030013,3,4,'交接','复盘报告','FORM','pm_form_debrief_report',0,NULL,'NOT_STARTED',4,'王强','2026-04-15',13,NULL,'2026-10-01 09:00:00',NULL),
 (9030014,3,5,'爬坡','爬坡期跟踪','DATA','pm_form_ramp_up',0,NULL,'NOT_STARTED',4,'王强','2026-05-15',14,NULL,'2026-10-01 09:00:00',NULL),
 (9030015,3,5,'分级','项目分级','FORM','pm_form_project_grade',0,NULL,'NOT_STARTED',4,'王强','2026-05-15',15,NULL,'2026-10-01 09:00:00',NULL),
 (9030016,3,5,'财务','回款台账','DATA','pm_form_payment_ledger',1,NULL,'NOT_STARTED',4,'王强','2026-05-15',16,NULL,'2026-10-01 09:00:00',NULL),
 (9030017,3,5,'质量','质量抽检与投诉','FORM','pm_form_quality_inspection',0,NULL,'NOT_STARTED',4,'王强','2026-05-15',17,NULL,'2026-10-01 09:00:00',NULL),
 (9030018,3,5,'质量','合规检查','FORM','pm_form_compliance_check',0,NULL,'NOT_STARTED',4,'王强','2026-05-15',18,NULL,'2026-10-01 09:00:00',NULL),
 (9030019,3,5,'客户','客户满意度','FORM','pm_form_client_satisfaction',0,NULL,'NOT_STARTED',4,'王强','2026-05-15',19,NULL,'2026-10-01 09:00:00',NULL),
 (9030020,3,5,'健康度','健康度评分卡','DATA','pm_form_health_scorecard',0,NULL,'NOT_STARTED',4,'王强','2026-05-15',20,NULL,'2026-10-01 09:00:00',NULL),
 (9030021,3,6,'申请','撤场申请单','FORM','pm_form_closing_apply',1,NULL,'NOT_STARTED',4,'王强','2026-06-15',21,NULL,'2026-10-01 09:00:00',NULL),
 (9030022,3,6,'执行','撤场方案','FORM','pm_form_closing_plan',0,NULL,'NOT_STARTED',4,'王强','2026-06-15',22,NULL,'2026-10-01 09:00:00',NULL),
 (9030023,3,6,'移交','卷宗移交清单','COLLAB','pm_form_case_file_transfer',0,NULL,'NOT_STARTED',4,'王强','2026-06-15',23,NULL,'2026-10-01 09:00:00',NULL),
 (9030024,3,6,'移交','资产处置单','COLLAB','pm_form_asset_disposal',0,NULL,'NOT_STARTED',4,'王强','2026-06-15',24,NULL,'2026-10-01 09:00:00',NULL),
 (9030025,3,6,'收口','撤场清算表','FORM','pm_form_final_settlement',1,NULL,'NOT_STARTED',4,'王强','2026-06-15',25,NULL,'2026-10-01 09:00:00',NULL),
 (9030026,3,6,'收口','档案封存','FORM','pm_form_archive_seal',0,NULL,'NOT_STARTED',4,'王强','2026-06-15',26,NULL,'2026-10-01 09:00:00',NULL);

-- 7. 表单实例（立项单已批、预算单部分在批）
INSERT INTO pm_form_instance (id, project_id, task_id, form_code, form_data, status, flow_instance_id, submit_user_id, submit_user_name, submit_time, approve_time, approve_comment, created_time) VALUES
 (5001,1,NULL,'pm_form_initiation','{"projectName":"海淀区法院诉源治理调解服务项目","estimatedAmount":4800000,"annualCases":12000,"pricingMode":"UNIT_PRICE","rating":"A","risks":[{"dim":"亏损","score":2},{"dim":"政策","score":3},{"dim":"竞争","score":2},{"dim":"用工合规","score":2}]}','APPROVED',NULL,2,'张伟','2026-09-18 10:00:00','2026-09-20 09:30:00','同意立项，按 A 级管理','2026-09-18 10:00:00'),
 (5002,2,NULL,'pm_form_initiation','{"projectName":"工商银行北京分行个贷调解项目","estimatedAmount":6500000,"annualCases":30000,"pricingMode":"COMMISSION","rating":"B"}','APPROVED',NULL,2,'张伟','2026-10-09 10:00:00','2026-10-11 09:00:00','同意，注意爬坡期人力成本','2026-10-09 10:00:00'),
 (5003,3,NULL,'pm_form_initiation','{"projectName":"12345 热线工单数据治理项目","estimatedAmount":1200000,"annualCases":8000,"pricingMode":"TOTAL_PACKAGE","rating":"C"}','APPROVED',NULL,4,'王强','2026-10-16 10:00:00','2026-10-18 09:00:00','同意','2026-10-16 10:00:00'),
 (5004,1,NULL,'pm_form_budget','{"income":[{"period":"2026-Q4","amount":1600000}],"cost":[{"item":"调解员薪酬","amount":820000},{"item":"场地","amount":120000}]}','APPROVED',NULL,2,'张伟','2026-09-22 10:00:00','2026-09-24 09:00:00','预算基线确认','2026-09-22 10:00:00'),
 (5005,2,NULL,'pm_form_budget','{"income":[{"period":"2026-Q4","amount":1800000}],"cost":[{"item":"调解员薪酬","amount":1100000}]}','APPROVING',NULL,2,'张伟','2026-10-26 10:00:00',NULL,NULL,'2026-10-26 10:00:00');

-- 8. 项目日志
INSERT INTO pm_project_log (id, project_id, action, detail, operator_id, operator_name, created_time) VALUES
 (8001,1,'INITIATION_APPROVED','立项审批通过，按模板 v1.0 实例化 6 环节 28 项任务',2,'张伟','2026-09-20 09:30:00'),
 (8002,1,'PHASE_ADVANCE','项目阶段推进：项目交接 → 项目运营',2,'张伟','2026-10-10 15:00:00'),
 (8003,2,'INITIATION_APPROVED','立项审批通过，按模板 v1.0 实例化 6 环节 26 项任务',2,'张伟','2026-10-11 09:00:00'),
 (8004,3,'INITIATION_APPROVED','立项审批通过，按模板 v1.0 实例化 6 环节 26 项任务',4,'王强','2026-10-18 09:00:00'),
 (8005,3,'MEMBER_ASSIGNED','指派成员：赵磊（驻场调解员，投入 30%）',4,'王强','2026-10-20 10:00:00');
