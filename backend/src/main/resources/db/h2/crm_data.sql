-- ============================================================
-- CRM 模块种子数据（开发档 H2）
-- 执行顺序：schema.sql → crm_schema.sql → data.sql → crm_data.sql
-- 依赖底座已有的组织(1-30)、角色(101-107)、用户(1-7)、公司主体(1-2)
--
-- 说明：演示数据的 mobile / email 以明文落库，mobile_hash / email_hash 留空。
-- 生产写入一律走 CrmCryptoService.encrypt + hash（见 LeadService / ContactService），
-- 解密失败时 CrmCryptoService.decrypt 会回落原值，因此演示数据仍可正常展示。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 字典（CRM 业务枚举全部字典化，前端不再硬编码文案）
-- ------------------------------------------------------------
INSERT INTO sys_dict_type (id, dict_type, dict_name, status, remark) VALUES
 (16,'crm_lead_status','线索状态',1,'CRM'),
 (17,'crm_lead_source','线索来源',1,'CRM'),
 (18,'crm_lead_grade','线索等级',1,'CRM'),
 (19,'crm_account_nature','客户性质',1,'CRM'),
 (20,'crm_account_type','客户分类',1,'CRM'),
 (21,'crm_industry','所属行业',1,'CRM'),
 (22,'crm_pool_status','客户池状态',1,'CRM'),
 (23,'crm_pool_trigger','池流转触发方式',1,'CRM'),
 (24,'crm_claim_status','认领审批状态',1,'CRM'),
 (25,'crm_sales_stage','商机阶段',1,'CRM'),
 (26,'crm_pricing_mode','计价口径',1,'CRM'),
 (27,'crm_activity_type','活动类型',1,'CRM'),
 (28,'crm_activity_level','活动级别',1,'CRM'),
 (29,'crm_activity_status','活动状态',1,'CRM'),
 (30,'crm_contract_status','合同状态',1,'CRM'),
 (31,'crm_contract_type','合同类型',1,'CRM'),
 (32,'crm_agreement_status','协议状态',1,'CRM'),
 (33,'crm_agreement_type','协议类型',1,'CRM'),
 (34,'crm_bid_status','投标状态',1,'CRM'),
 (35,'crm_tender_form','招标形式',1,'CRM'),
 (36,'crm_deposit_status','保证金状态',1,'CRM'),
 (37,'crm_target_type','跟进对象类型',1,'CRM'),
 (38,'crm_import_status','导入批次状态',1,'CRM');

INSERT INTO sys_dict_item (id, dict_type, dict_label, dict_value, sort_no, status) VALUES
 (1601,'crm_lead_status','新建','NEW',1,1),(1602,'crm_lead_status','跟进中','FOLLOWING',2,1),
 (1603,'crm_lead_status','已转化','CONVERTED',3,1),(1604,'crm_lead_status','已作废','INVALID',4,1),
 (1701,'crm_lead_source','官网/线上','WEB',1,1),(1702,'crm_lead_source','电话呼入','PHONE',2,1),
 (1703,'crm_lead_source','老客户推荐','REFERRAL',3,1),(1704,'crm_lead_source','展会活动','EXHIBITION',4,1),
 (1705,'crm_lead_source','渠道合作','PARTNER',5,1),(1706,'crm_lead_source','招投标信息','TENDER',6,1),
 (1707,'crm_lead_source','其他','OTHER',7,1),
 (1801,'crm_lead_grade','A 级（高意向）','A',1,1),(1802,'crm_lead_grade','B 级（中意向）','B',2,1),
 (1803,'crm_lead_grade','C 级（低意向）','C',3,1),(1804,'crm_lead_grade','D 级（待验证）','D',4,1),
 (1901,'crm_account_nature','To B（企业）','TO_B',1,1),(1902,'crm_account_nature','To C（个人）','TO_C',2,1),
 (1903,'crm_account_nature','To G（政府）','TO_G',3,1),
 (2001,'crm_account_type','普通客户','NORMAL',1,1),(2002,'crm_account_type','重点客户','KEY',2,1),
 (2003,'crm_account_type','战略客户','STRATEGIC',3,1),
 (2101,'crm_industry','银行','BANK',1,1),(2102,'crm_industry','证券','SECURITIES',2,1),
 (2103,'crm_industry','保险','INSURANCE',3,1),(2104,'crm_industry','政府机关','GOV',4,1),
 (2105,'crm_industry','司法机构','JUDICIAL',5,1),(2106,'crm_industry','一般企业','ENTERPRISE',6,1),
 (2107,'crm_industry','融资租赁','LEASING',7,1),(2108,'crm_industry','金融科技','FINTECH',8,1),
 (2201,'crm_pool_status','私有','PRIVATE',1,1),(2202,'crm_pool_status','公共池','POOL',2,1),
 (2203,'crm_pool_status','已流失','CHURNED',3,1),
 (2301,'crm_pool_trigger','手工释放','MANUAL',1,1),(2302,'crm_pool_trigger','无活动超期','AUTO_TIMEOUT',2,1),
 (2303,'crm_pool_trigger','认领转移','CLAIM',3,1),(2304,'crm_pool_trigger','流失','CHURN',4,1),
 (2401,'crm_claim_status','审批中','APPROVING',1,1),(2402,'crm_claim_status','已通过','APPROVED',2,1),
 (2403,'crm_claim_status','已驳回','REJECTED',3,1),(2404,'crm_claim_status','已撤销','CANCELLED',4,1),
 (2501,'crm_sales_stage','寻找潜在客户','PROSPECT',1,1),(2502,'crm_sales_stage','提案报价','PROPOSAL',2,1),
 (2503,'crm_sales_stage','谈判','NEGOTIATION',3,1),(2504,'crm_sales_stage','成交','WON',4,1),
 (2505,'crm_sales_stage','流失','LOST',5,1),
 (2601,'crm_pricing_mode','免费','FREE',1,1),(2602,'crm_pricing_mode','总价包干','LUMP_SUM',2,1),
 (2603,'crm_pricing_mode','单价计量','UNIT_PRICE',3,1),(2604,'crm_pricing_mode','按工时','TIME_BASED',4,1),
 (2605,'crm_pricing_mode','按人月','HEAD_COUNT',5,1),(2606,'crm_pricing_mode','佣金分成','COMMISSION',6,1),
 (2607,'crm_pricing_mode','按效果','EFFECT_BASED',7,1),
 (2701,'crm_activity_type','拜访','VISIT',1,1),(2702,'crm_activity_type','电话','CALL',2,1),
 (2703,'crm_activity_type','邮件','EMAIL',3,1),(2704,'crm_activity_type','会议','MEETING',4,1),
 (2705,'crm_activity_type','即时沟通','IM',5,1),(2706,'crm_activity_type','其他','OTHER',6,1),
 (2801,'crm_activity_level','A 级（关键）','A',1,1),(2802,'crm_activity_level','B 级（重要）','B',2,1),
 (2803,'crm_activity_level','C 级（一般）','C',3,1),
 (2901,'crm_activity_status','有效','VALID',1,1),(2902,'crm_activity_status','已作废','VOIDED',2,1),
 (3001,'crm_contract_status','草稿','DRAFT',1,1),(3002,'crm_contract_status','审批中','APPROVING',2,1),
 (3003,'crm_contract_status','生效中','ACTIVE',3,1),(3004,'crm_contract_status','已到期','EXPIRED',4,1),
 (3005,'crm_contract_status','已终止','TERMINATED',5,1),
 (3101,'crm_contract_type','首签','FIRST',1,1),(3102,'crm_contract_type','续签','RENEW',2,1),
 (3103,'crm_contract_type','补充协议','SUPPLEMENT',3,1),
 (3201,'crm_agreement_status','草稿','DRAFT',1,1),(3202,'crm_agreement_status','审批中','APPROVING',2,1),
 (3203,'crm_agreement_status','生效中','EFFECTIVE',3,1),(3204,'crm_agreement_status','已失效','EXPIRED',4,1),
 (3205,'crm_agreement_status','已终止','TERMINATED',5,1),
 (3301,'crm_agreement_type','框架协议','FRAMEWORK',1,1),(3302,'crm_agreement_type','补充协议','SUPPLEMENT',2,1),
 (3303,'crm_agreement_type','保密协议','NDA',3,1),(3304,'crm_agreement_type','服务协议','SERVICE',4,1),
 (3401,'crm_bid_status','跟踪中','TRACKING',1,1),(3402,'crm_bid_status','已报名','REGISTERED',2,1),
 (3403,'crm_bid_status','已投标','SUBMITTED',3,1),(3404,'crm_bid_status','已中标','WON',4,1),
 (3405,'crm_bid_status','未中标','LOST',5,1),(3406,'crm_bid_status','已放弃','ABANDONED',6,1),
 (3501,'crm_tender_form','公开招标','OPEN',1,1),(3502,'crm_tender_form','邀请招标','INVITE',2,1),
 (3503,'crm_tender_form','竞争性磋商','NEGOTIATED',3,1),(3504,'crm_tender_form','单一来源','SINGLE',4,1),
 (3505,'crm_tender_form','询价','INQUIRY',5,1),
 (3601,'crm_deposit_status','无需缴纳','NONE',1,1),(3602,'crm_deposit_status','已缴纳','PAID',2,1),
 (3603,'crm_deposit_status','已退还','REFUNDED',3,1),(3604,'crm_deposit_status','已没收','FORFEITED',4,1),
 (3701,'crm_target_type','线索','LEAD',1,1),(3702,'crm_target_type','客户','ACCOUNT',2,1),
 (3703,'crm_target_type','联系人','CONTACT',3,1),(3704,'crm_target_type','商机','OPPORTUNITY',4,1),
 (3705,'crm_target_type','合同','CONTRACT',5,1),(3706,'crm_target_type','投标','BID',6,1),
 (3801,'crm_import_status','待处理','PENDING',1,1),(3802,'crm_import_status','处理中','RUNNING',2,1),
 (3803,'crm_import_status','全部成功','SUCCESS',3,1),(3804,'crm_import_status','部分失败','PARTIAL_FAILED',4,1),
 (3805,'crm_import_status','全部失败','FAILED',5,1);

-- ------------------------------------------------------------
-- 2. 参数配置（CrmConfig 读取的全部键，禁止代码内硬编码天数）
-- ------------------------------------------------------------
INSERT INTO sys_config (id, config_key, config_value, config_name, config_type, builtin, remark) VALUES
 (12,'crm.pool.timeout.days','90','客户无活动自动入公共池天数','NUMBER',1,'定时任务依据日'),
 (13,'crm.lead.pool.timeout.days','30','线索未跟进回收天数','NUMBER',1,'定时任务依据'),
 (14,'crm.claim.appeal.hours','24','客户认领申诉时长（小时）','NUMBER',1,'原负责人可申诉窗口'),
 (15,'crm.contract.expire.notify.days','30','合同到期提前提醒天数','NUMBER',1,'到期扫描任务'),
 (16,'crm.bid.open.notify.days','3','投标开标提前提醒天数','NUMBER',1,'开标提醒任务'),
 (17,'crm.import.max.rows','5000','单次导入最大行数','NUMBER',1,'超过则整体拒绝'),
 (18,'crm.claim.auto.approve','true','认领审批原负责人超时自动同意','BOOLEAN',1,'未处理时是否放行'),
 (19,'crm.approval.amount.threshold','1000000','合同审批金额条件分支阈值（元）','NUMBER',1,'流程条件表达式使用'),
 (20,'crm.stats.cache.ttl.seconds','300','CRM 统计缓存有效期（秒）','NUMBER',1,'按角色+组织缓存'),
 (21,'crm.crypto.secret','','CRM 手机号/邮箱加密密钥','STRING',1,'生产必须改为环境变量注入，留空则用应用配置');

-- ------------------------------------------------------------
-- 3. 脱敏字段白名单扩容（CRM 主体字段注册，未注册字段不会被脱敏）
-- ------------------------------------------------------------
INSERT INTO sys_sensitive_field (id, group_code, biz_object, field_name, field_label, mask_rule, mask_param, allow_plain, status) VALUES
 (8,'MOBILE','lead','mobile','线索手机号','KEEP_HEAD_TAIL','3,4',1,1),
 (9,'EMAIL','lead','email','线索邮箱','EMAIL_MASK',NULL,1,1),
 (10,'MOBILE','contact','mobile','联系人手机号','KEEP_HEAD_TAIL','3,4',1,1),
 (11,'EMAIL','contact','email','联系人邮箱','EMAIL_MASK',NULL,1,1),
 (12,'MOBILE','account','phone','客户联系电话','KEEP_HEAD_TAIL','3,4',1,1),
 (13,'ADDRESS','account','address','客户详细地址','ADDRESS_MASK',NULL,1,1),
 (14,'ADDRESS','contact','address','联系人地址','ADDRESS_MASK',NULL,1,1);

-- ------------------------------------------------------------
-- 4. 编码规则（合同号 / 协议号，流水号原子自增 + 按月重置）
-- ------------------------------------------------------------
INSERT INTO sys_code_rule (id, rule_code, rule_name, prefix, date_format, seq_length, reset_cycle, current_seq, current_period, sample, status, remark) VALUES
 (1,'CRM_CONTRACT','合同编号','HT','yyyyMM',4,'MONTH',0,NULL,'HT202610-0001',1,'CRM 合同按签年月编号'),
 (2,'CRM_AGREEMENT','协议编号','XY','yyyyMM',4,'MONTH',0,NULL,'XY202610-0001',1,'CRM 协议按签年月编号');

-- ------------------------------------------------------------
-- 5. 审批模板（合同 / 协议 / 客户认领，复用底座 Flowable 通用能力）
-- ------------------------------------------------------------
INSERT INTO sys_flow_def (id, flow_code, flow_name, biz_type, org_scope, process_definition_key, deployed_version, status, form_code, node_config) VALUES
 (3,'CRM_CONTRACT','CRM-合同审批','CRM_CONTRACT',NULL,'CRM_CONTRACT',1,1,'CRM_CONTRACT',
  '{"nodes":[{"key":"start","name":"发起人","type":"START"},{"key":"directLeader","name":"直属上级","type":"SINGLE","approverType":"LEADER","level":1},{"key":"amountGateway","name":"金额判断","type":"CONDITION","expression":"winAmount > 1000000"},{"key":"finance","name":"财务审核","type":"SINGLE","approverType":"ROLE","roleCode":"ROLE_GROUP_ADMIN"},{"key":"legal","name":"法务审核","type":"CANDIDATE","approverType":"ROLE","roleCode":"ROLE_GROUP_ADMIN"},{"key":"end","name":"结束","type":"END"}]}'),
 (4,'CRM_AGREEMENT','CRM-协议审批','CRM_AGREEMENT',NULL,'CRM_AGREEMENT',1,1,'CRM_AGREEMENT',
  '{"nodes":[{"key":"start","name":"发起人","type":"START"},{"key":"directLeader","name":"直属上级","type":"SINGLE","approverType":"LEADER","level":1},{"key":"legal","name":"法务审核","type":"SINGLE","approverType":"ROLE","roleCode":"ROLE_GROUP_ADMIN"},{"key":"end","name":"结束","type":"END"}]}'),
 (5,'CRM_CLAIM','CRM-客户认领审批','CRM_CLAIM',NULL,'CRM_CLAIM',1,1,'CRM_CLAIM',
  '{"nodes":[{"key":"start","name":"发起人","type":"START"},{"key":"oldOwner","name":"原负责人确认","type":"SINGLE","approverType":"INITIATOR_VAR","variable":"oldOwnerId"},{"key":"deptLeader","name":"部门负责人","type":"SINGLE","approverType":"ORG_LEADER"},{"key":"end","name":"结束","type":"END"}]}');

-- ------------------------------------------------------------
-- 6. 菜单与权限位
--    data.sql 里的 CRM 菜单是占位（component=placeholder/index），此处修正为真实页面，
--    并补充 6 个特殊视图、9 个列表页对应的按钮权限位。
-- ------------------------------------------------------------
UPDATE sys_menu SET component = 'crm/account/index',     perms = 'crm:account:list',     sort_no = 1  WHERE id = 1101;
UPDATE sys_menu SET component = 'crm/pool/index',        perms = 'crm:account:pool',     sort_no = 2  WHERE id = 1102;
UPDATE sys_menu SET menu_name = '线索管理', component = 'crm/lead/index', perms = 'crm:lead:list', sort_no = 5 WHERE id = 1103;
UPDATE sys_menu SET menu_name = '销售活动', component = 'crm/activity/index',    perms = 'crm:activity:list',    sort_no = 9  WHERE id = 1104;
UPDATE sys_menu SET component = 'crm/contract/index',    perms = 'crm:contract:list',    sort_no = 11 WHERE id = 1105;
UPDATE sys_menu SET component = 'crm/agreement/index',   perms = 'crm:agreement:list',   sort_no = 13 WHERE id = 1106;
UPDATE sys_menu SET component = 'crm/bid/index',         perms = 'crm:bid:list',         sort_no = 14 WHERE id = 1107;
UPDATE sys_menu SET component = 'crm/payment/index',     perms = 'crm:payment:list',     sort_no = 16 WHERE id = 1108;
UPDATE sys_menu SET component = 'crm/churn/index',       perms = 'crm:account:list',     sort_no = 17 WHERE id = 1109;

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_no, visible, status) VALUES
 (1121,100,'客户认领审批',2,'/crm/claim','crm/pool/claim','crm:account:claim','user-check',3,1,1),
 (1122,100,'联系人管理',2,'/crm/contact','crm/contact/index','crm:contact:list','address-card',4,1,1),
 (1123,100,'线索转化工作台',2,'/crm/lead-convert','crm/lead/convert','crm:lead:convert','transform',6,1,1),
 (1120,100,'商机管理',2,'/crm/opportunity','crm/opportunity/index','crm:opportunity:list','briefcase',7,1,1),
 (1124,100,'商机漏斗',2,'/crm/opportunity-funnel','crm/opportunity/funnel','crm:opportunity:list','chart-funnel',8,1,1),
 (1125,100,'活动时间线',2,'/crm/activity-timeline','crm/activity/timeline','crm:activity:list','timeline',10,1,1),
 (1126,100,'合同到期预警',2,'/crm/contract-expire','crm/contract/expire','crm:contract:list','alarm',12,1,1),
 (1127,100,'投标日历',2,'/crm/bid-calendar','crm/bid/calendar','crm:bid:list','calendar-event',15,1,1);

-- CRM 按钮权限位
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_no, visible, status) VALUES
 (9201,1101,'查看客户详情',3,NULL,NULL,'crm:account:query',NULL,1,1,1),
 (9202,1101,'新增客户',3,NULL,NULL,'crm:account:add',NULL,2,1,1),
 (9203,1101,'修改客户',3,NULL,NULL,'crm:account:edit',NULL,3,1,1),
 (9204,1101,'删除客户',3,NULL,NULL,'crm:account:remove',NULL,4,1,1),
 (9205,1101,'转移负责人',3,NULL,NULL,'crm:account:assign',NULL,5,1,1),
 (9210,1102,'认领客户',3,NULL,NULL,'crm:account:claim',NULL,1,1,1),
 (9211,1102,'公共池查询',3,NULL,NULL,'crm:account:pool',NULL,2,1,1),
 (9220,1121,'认领审批处理',3,NULL,NULL,'crm:account:claim',NULL,1,1,1),
 (9230,1122,'查看联系人',3,NULL,NULL,'crm:contact:query',NULL,1,1,1),
 (9231,1122,'新增联系人',3,NULL,NULL,'crm:contact:add',NULL,2,1,1),
 (9232,1122,'修改联系人',3,NULL,NULL,'crm:contact:edit',NULL,3,1,1),
 (9233,1122,'删除联系人',3,NULL,NULL,'crm:contact:remove',NULL,4,1,1),
 (9240,1103,'查看线索',3,NULL,NULL,'crm:lead:query',NULL,1,1,1),
 (9241,1103,'新增线索',3,NULL,NULL,'crm:lead:add',NULL,2,1,1),
 (9242,1103,'修改线索',3,NULL,NULL,'crm:lead:edit',NULL,3,1,1),
 (9243,1103,'删除线索',3,NULL,NULL,'crm:lead:remove',NULL,4,1,1),
 (9244,1103,'线索转化',3,NULL,NULL,'crm:lead:convert',NULL,5,1,1),
 (9245,1103,'导入线索',3,NULL,NULL,'crm:lead:import',NULL,6,1,1),
 (9246,1103,'分配线索',3,NULL,NULL,'crm:lead:assign',NULL,7,1,1),
 (9250,1120,'查看商机',3,NULL,NULL,'crm:opportunity:query',NULL,1,1,1),
 (9251,1120,'新增商机',3,NULL,NULL,'crm:opportunity:add',NULL,2,1,1),
 (9252,1120,'修改商机',3,NULL,NULL,'crm:opportunity:edit',NULL,3,1,1),
 (9253,1120,'删除商机',3,NULL,NULL,'crm:opportunity:remove',NULL,4,1,1),
 (9254,1120,'阶段推进',3,NULL,NULL,'crm:opportunity:stage',NULL,5,1,1),
 (9255,1120,'置流失',3,NULL,NULL,'crm:opportunity:lost',NULL,6,1,1),
 (9260,1104,'新增跟进',3,NULL,NULL,'crm:activity:add',NULL,1,1,1),
 (9261,1104,'修改跟进',3,NULL,NULL,'crm:activity:edit',NULL,2,1,1),
 (9262,1104,'作废跟进',3,NULL,NULL,'crm:activity:void',NULL,3,1,1),
 (9270,1105,'查看合同',3,NULL,NULL,'crm:contract:query',NULL,1,1,1),
 (9271,1105,'新增合同',3,NULL,NULL,'crm:contract:add',NULL,2,1,1),
 (9272,1105,'修改合同',3,NULL,NULL,'crm:contract:edit',NULL,3,1,1),
 (9273,1105,'删除合同',3,NULL,NULL,'crm:contract:remove',NULL,4,1,1),
 (9274,1105,'提交合同审批',3,NULL,NULL,'crm:contract:submit',NULL,5,1,1),
 (9275,1105,'合同归档',3,NULL,NULL,'crm:contract:archive',NULL,6,1,1),
 (9276,1105,'合同续签',3,NULL,NULL,'crm:contract:renew',NULL,7,1,1),
 (9277,1105,'合同终止',3,NULL,NULL,'crm:contract:terminate',NULL,8,1,1),
 (9280,1106,'查看协议',3,NULL,NULL,'crm:agreement:query',NULL,1,1,1),
 (9281,1106,'新增协议',3,NULL,NULL,'crm:agreement:add',NULL,2,1,1),
 (9282,1106,'修改协议',3,NULL,NULL,'crm:agreement:edit',NULL,3,1,1),
 (9283,1106,'删除协议',3,NULL,NULL,'crm:agreement:remove',NULL,4,1,1),
 (9284,1106,'提交协议审批',3,NULL,NULL,'crm:agreement:submit',NULL,5,1,1),
 (9285,1106,'协议归档',3,NULL,NULL,'crm:agreement:archive',NULL,6,1,1),
 (9286,1106,'协议续签',3,NULL,NULL,'crm:agreement:renew',NULL,7,1,1),
 (9290,1107,'查看投标',3,NULL,NULL,'crm:bid:query',NULL,1,1,1),
 (9291,1107,'新增投标',3,NULL,NULL,'crm:bid:add',NULL,2,1,1),
 (9292,1107,'修改投标',3,NULL,NULL,'crm:bid:edit',NULL,3,1,1),
 (9293,1107,'删除投标',3,NULL,NULL,'crm:bid:remove',NULL,4,1,1),
 (9294,1107,'投标状态流转',3,NULL,NULL,'crm:bid:status',NULL,5,1,1);

-- 角色授权：超管补齐全部 CRM 菜单（data.sql 授权时新菜单尚未插入）
-- uk_role_menu(role_id, menu_id) 唯一，必须 NOT EXISTS 幂等插入
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 300000 + m.id, 101, m.id FROM sys_menu m
WHERE (m.id IN (100,1101,1102,1103,1104,1105,1106,1107,1108,1109,1120,1121,1122,1123,1124,1125,1126,1127)
    OR (m.id BETWEEN 9201 AND 9294))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 101 AND rm.menu_id = m.id);

-- G端BG 项目经理：CRM 全功能
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 310000 + m.id, 102, m.id FROM sys_menu m
WHERE (m.id IN (1120,1121,1122,1123,1124,1125,1126,1127)
    OR (m.id BETWEEN 9201 AND 9294))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 102 AND rm.menu_id = m.id);

-- G端BG 调解员：客户 / 公共池 / 认领 / 联系人 / 线索 / 跟进
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
 (320001,103,1121),(320002,103,1122),
 (320011,103,9201),(320012,103,9203),(320013,103,9210),(320014,103,9220),
 (320015,103,9230),(320016,103,9231),(320017,103,9232),
 (320018,103,9240),(320019,103,9241),(320020,103,9242),(320021,103,9260),(320022,103,9261);

-- 地区部运营：客户 / 公共池 / 线索 / 跟进（只读 + 认领）
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
 (330001,105,1122),(330011,105,9201),(330012,105,9210),(330013,105,9230),(330014,105,9240);

-- 商务经理：客户 / 联系人 / 合同 / 协议 / 投标 / 回款 + 到期预警 + 投标日历
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
 (340001,106,1122),(340002,106,1126),(340003,106,1127),
 (340011,106,9201),(340012,106,9230),(340013,106,9231),
 (340020,106,9270),(340021,106,9271),(340022,106,9272),(340023,106,9274),
 (340024,106,9275),(340025,106,9276),(340026,106,9277),
 (340030,106,9280),(340031,106,9281),(340032,106,9282),(340033,106,9284),
 (340040,106,9290),(340041,106,9291),(340042,106,9292),(340043,106,9294);

-- 只读访客：客户 / 线索列表（无任何操作位）
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
 (350001,107,100),(350002,107,1101),(350003,107,1103),(350004,107,9201),(350005,107,9240);

-- ------------------------------------------------------------
-- 7. 演示数据
--    组织归属分布（用于验证「按组织归属」行级数据权限）：
--      org 2  G端BG  → 张伟(2) / 李娜(3)
--      org 3  和合BG  → 刘洋(5)
--      org 14 商务部  → 陈静(6)
-- ------------------------------------------------------------

-- ---------- 7.1 客户（20） ----------
INSERT INTO crm_account (id, company_name, credit_code, institution_code, industry, account_nature, account_type,
    website, phone, employee_scale, annual_revenue, owner_user_id, owner_org_id, owner_user_name,
    parent_account_id, primary_contact_id, province, city, district, address, pool_status, pool_in_time,
    pool_reason, churn_reason, is_prospect, last_activity_time, remark, created_time) VALUES
 (1001,'中国工商银行股份有限公司北京市分行','91110000100003962T','ICBC-BJ','BANK','TO_B','STRATEGIC','www.icbc.com.cn','010-66410000','1000+',860000000.0000,2,2,'张伟',NULL,2001,'北京市','北京市','西城区','复兴门内大街 55 号','PRIVATE',NULL,NULL,NULL,0,'2026-10-22 15:20:00','总行级战略客户，年度框架采购','2025-11-10 09:00:00'),
 (1002,'中国建设银行股份有限公司上海市分行','91310000100005905J','CCB-SH','BANK','TO_B','KEY','www.ccb.com','021-58880000','1000+',720000000.0000,2,2,'张伟',NULL,2003,'上海市','上海市','浦东新区','陆家嘴环路 900 号','PRIVATE',NULL,NULL,NULL,0,'2026-10-20 11:00:00','风控数据服务续签意向明确','2025-12-02 14:10:00'),
 (1003,'北京市政务服务管理局','11110000000017676W','GOV-BJ-ZWFW','GOV','TO_G','STRATEGIC','zwfwj.beijing.gov.cn','010-55529000','200-500',NULL,2,2,'张伟',NULL,2005,'北京市','北京市','通州区','运河东大街 57 号','PRIVATE',NULL,NULL,NULL,0,'2026-10-18 10:30:00','一网通办信用核查场景','2026-01-15 09:20:00'),
 (1004,'江苏省人力资源和社会保障厅','11320000014000201A','GOV-JS-HRSS','GOV','TO_G','KEY','jshrss.jiangsu.gov.cn','025-83276000','200-500',NULL,2,2,'张伟',NULL,2007,'江苏省','南京市','鼓楼区','北京西路 16 号','PRIVATE',NULL,NULL,NULL,0,'2026-10-16 14:00:00','劳动仲裁数据核验','2026-02-08 10:00:00'),
 (1005,'招商银行股份有限公司深圳分行','91440300192471224K','CMB-SZ','BANK','TO_B','NORMAL','www.cmbchina.com','0755-83077000','500-1000',310000000.0000,3,2,'李娜',NULL,NULL,'广东省','深圳市','福田区','深南大道 7088 号','POOL','2026-10-18 09:00:00','原负责人离职，自动释放至公共池',NULL,0,'2026-07-10 16:00:00','待重新认领','2026-03-12 15:30:00'),
 (1006,'中信证券股份有限公司','91110000101725017N','CITICS','SECURITIES','TO_B','KEY','www.cs.ecitic.com','010-60838888','1000+',450000000.0000,2,2,'张伟',NULL,2009,'北京市','北京市','朝阳区','亮马桥路 48 号','PRIVATE',NULL,NULL,NULL,0,'2026-10-21 09:40:00','投行尽调数据服务','2026-04-01 09:00:00'),
 (1007,'中国人寿保险股份有限公司','91110000100004396F','CHINALIFE','INSURANCE','TO_B','NORMAL','www.e-chinalife.com','010-63633333','1000+',260000000.0000,2,2,'张伟',NULL,NULL,'北京市','北京市','西城区','金融大街 16 号','CHURNED',NULL,NULL,'2026 年度预算冻结，暂停合作，转二期跟进',0,'2026-06-28 10:00:00','流失客户池演示数据','2026-05-06 11:00:00'),
 (1008,'国网北京市电力公司','91110000100006752H','SGCC-BJ','ENTERPRISE','TO_G','KEY','www.bj.sgcc.com.cn','010-63128000','1000+',180000000.0000,2,2,'张伟',NULL,NULL,'北京市','北京市','西城区','前门西大街 41 号','PRIVATE',NULL,NULL,NULL,0,'2026-10-19 15:00:00','供应商信用评价','2026-05-20 14:00:00'),
 (1009,'交通银行股份有限公司北京市分行','91110000100005913L','BOCOM-BJ','BANK','TO_B','NORMAL','www.bankcomm.com','010-66500000','500-1000',210000000.0000,3,2,'李娜',NULL,2011,'北京市','北京市','西城区','金融大街 33 号','PRIVATE',NULL,NULL,NULL,0,'2026-10-17 10:20:00','贷前核验标品采购','2026-06-15 09:30:00'),
 (1010,'上海市大数据中心','12310000MB2F00051K','GOV-SH-DATA','GOV','TO_G','KEY','data.sh.gov.cn','021-23111234','200-500',NULL,3,2,'李娜',NULL,2013,'上海市','上海市','黄浦区','人民大道 200 号','PRIVATE',NULL,NULL,NULL,0,'2026-10-15 14:40:00','公共数据授权运营合作','2026-07-02 10:10:00'),
 (1011,'中国邮政储蓄银行股份有限公司','91110000710934600C','PSBC','BANK','TO_B','NORMAL','www.psbc.com','010-68858000','1000+',150000000.0000,3,2,'李娜',NULL,NULL,'北京市','北京市','西城区','金融大街 3 号','POOL','2026-09-30 09:00:00','超过 90 天无有效跟进，自动入池',NULL,0,'2026-06-20 09:00:00','自动入池演示数据','2026-03-28 10:00:00'),
 (1012,'平安银行股份有限公司上海分行','91310000100003281F','PAB-SH','BANK','TO_B','NORMAL','bank.pingan.com','021-38880000','500-1000',190000000.0000,3,2,'李娜',NULL,NULL,'上海市','上海市','浦东新区','陆家嘴环路 1333 号','PRIVATE',NULL,NULL,NULL,0,'2026-10-12 16:30:00','零售风控数据试点','2026-07-18 09:00:00'),
 (1013,'广州市中级人民法院','11440100MB2C00001X','GZ-COURT','JUDICIAL','TO_G','KEY','www.gzcourt.gov.cn','020-83210000','200-500',NULL,5,3,'刘洋',NULL,2015,'广东省','广州市','越秀区','中山四路 246 号','PRIVATE',NULL,NULL,NULL,0,'2026-10-23 09:30:00','失信被执行人数据核查','2026-02-20 10:00:00'),
 (1014,'深圳市南山区人民法院','11440305MB2D00002Y','SZ-NS-COURT','JUDICIAL','TO_G','NORMAL','www.nsqcourt.gov.cn','0755-26650000','50-200',NULL,5,3,'刘洋',NULL,NULL,'广东省','深圳市','南山区','东滨路 128 号','PRIVATE',NULL,NULL,NULL,0,'2026-10-11 11:00:00','诉前调解辅助服务','2026-04-12 09:00:00'),
 (1015,'杭州互联网法院','11330100MB1A00003Z','HZ-INTERNET-COURT','JUDICIAL','TO_G','STRATEGIC','www.hznetcourt.gov.cn','0571-86000000','50-200',NULL,5,3,'刘洋',NULL,2017,'浙江省','杭州市','西湖区','转塘街道象山 352 号','PRIVATE',NULL,NULL,NULL,0,'2026-10-22 10:00:00','电子证据链核验战略合作','2026-05-08 10:30:00'),
 (1016,'中国移动通信集团广东有限公司','91440000100010001M','CMCC-GD','ENTERPRISE','TO_B','KEY','www.10086.cn','020-38688888','1000+',980000000.0000,5,3,'刘洋',NULL,NULL,'广东省','广州市','天河区','珠江新城珠江西路 11 号','PRIVATE',NULL,NULL,NULL,1,'2026-10-09 14:00:00','潜在客户，待完成资质核验','2026-08-20 15:00:00'),
 (1017,'顺丰速运有限公司','91440300100010002N','SF-EXPRESS','ENTERPRISE','TO_B','NORMAL','www.sf-express.com','0755-95338','1000+',640000000.0000,5,3,'刘洋',NULL,NULL,'广东省','深圳市','宝安区','福永街道机场物流园','POOL','2026-10-20 09:00:00','负责人转岗释放',NULL,1,'2026-07-25 10:00:00','潜在客户，物流行业首单','2026-08-28 09:00:00'),
 (1018,'中国银行股份有限公司江苏省分行','91320000100010003P','BOC-JS','BANK','TO_B','NORMAL','www.boc.cn','025-84710000','500-1000',230000000.0000,6,14,'陈静',NULL,NULL,'江苏省','南京市','秦淮区','中山南路 148 号','PRIVATE',NULL,NULL,NULL,0,'2026-10-14 10:00:00','商务部直管客户','2026-03-05 09:00:00'),
 (1019,'南京银行股份有限公司','91320100100010004Q','NJCB','BANK','TO_B','KEY','www.njcb.com.cn','025-86770000','200-500',120000000.0000,6,14,'陈静',NULL,NULL,'江苏省','南京市','建邺区','中山路 288 号','PRIVATE',NULL,NULL,NULL,0,'2026-10-21 15:30:00','小微信贷数据合作','2026-04-18 10:00:00'),
 (1020,'苏州银行股份有限公司','91320500100010005R','SUZBANK','BANK','TO_B','NORMAL','www.suzhoubank.com','0512-69860000','200-500',98000000.0000,6,14,'陈静',NULL,NULL,'江苏省','苏州市','工业园区','苏州大道东 289 号','PRIVATE',NULL,NULL,NULL,0,'2026-10-08 09:30:00','区域性银行试点','2026-06-22 10:00:00');

-- 主联系人回填（客户与联系人互指，先插客户再补 primary_contact_id）

-- ---------- 7.2 联系人（30） ----------
INSERT INTO crm_contact (id, name, account_id, mobile, email, position_name, department, birthday,
    address, owner_user_id, owner_org_id, owner_user_name, last_activity_time, remark, created_time) VALUES
 (2001,'王建国',1001,'13910000001','wangjianguo@icbc.com.cn','部门主任','办公室','1972-03-12','北京市西城区复兴门内大街 55 号',2,2,'张伟','2026-10-22 15:20:00','关键决策人','2025-11-10 09:30:00'),
 (2002,'李慧',1001,'13910000002','lihui@icbc.com.cn','总经理','信息科技部','1980-07-25','北京市西城区复兴门内大街 55 号',2,2,'张伟','2026-10-22 15:20:00','技术对接负责人','2025-11-10 09:35:00'),
 (2003,'张晨',1002,'13910000003','zhangchen@ccb.com','副总经理','风险管理部','1978-11-02','上海市浦东新区陆家嘴环路 900 号',2,2,'张伟','2026-10-20 11:00:00','续签决策人','2025-12-02 14:20:00'),
 (2004,'刘敏',1002,'13910000004','liumin@ccb.com','高级经理','数据管理部','1986-05-19','上海市浦东新区陆家嘴环路 900 号',2,2,'张伟','2026-10-20 11:00:00','日常接口人','2025-12-02 14:25:00'),
 (2005,'陈国强',1003,'13910000005','chenguoqiang@zwfwj.gov.cn','处长','政务服务数据处','1975-09-08','北京市通州区运河东大街 57 号',2,2,'张伟','2026-10-18 10:30:00','项目主管','2026-01-15 10:00:00'),
 (2006,'赵雪',1003,'13910000006','zhaoxue@zwfwj.gov.cn','副处长','信息化推进处','1983-02-14','北京市通州区运河东大街 57 号',2,2,'张伟','2026-10-18 10:30:00',NULL,'2026-01-15 10:05:00'),
 (2007,'周文斌',1004,'13910000007','zhouwenbin@jshrss.gov.cn','主任','信息中心','1976-12-30','江苏省南京市鼓楼区北京西路 16 号',2,2,'张伟','2026-10-16 14:00:00',NULL,'2026-02-08 10:10:00'),
 (2008,'吴静',1004,'13910000008','wujing@jshrss.gov.cn','科长','劳动关系处','1988-06-11','江苏省南京市鼓楼区北京西路 16 号',2,2,'张伟','2026-10-16 14:00:00',NULL,'2026-02-08 10:15:00'),
 (2009,'郑浩',1006,'13910000009','zhenghao@citics.com','总监','信息技术部','1979-04-22','北京市朝阳区亮马桥路 48 号',2,2,'张伟','2026-10-21 09:40:00','技术决策人','2026-04-01 09:10:00'),
 (2010,'孙丽',1006,'13910000010','sunli@citics.com','高级经理','合规部','1985-08-16','北京市朝阳区亮马桥路 48 号',2,2,'张伟','2026-10-21 09:40:00',NULL,'2026-04-01 09:15:00'),
 (2011,'冯磊',1009,'13910000011','fenglei@bankcomm.com','总经理','风险管理部','1977-01-09','北京市西城区金融大街 33 号',3,2,'李娜','2026-10-17 10:20:00','标品采购发起人','2026-06-15 09:40:00'),
 (2012,'韩雪',1009,'13910000012','hanxue@bankcomm.com','经理','采购部','1990-10-05','北京市西城区金融大街 33 号',3,2,'李娜','2026-10-17 10:20:00',NULL,'2026-06-15 09:45:00'),
 (2013,'曹阳',1010,'13910000013','caoyang@shdata.gov.cn','部长','数据运营部','1974-07-18','上海市黄浦区人民大道 200 号',3,2,'李娜','2026-10-15 14:40:00','授权运营对接人','2026-07-02 10:20:00'),
 (2014,'沈梦',1010,'13910000014','shenmeng@shdata.gov.cn','副处长','综合处','1987-03-27','上海市黄浦区人民大道 200 号',3,2,'李娜','2026-10-15 14:40:00',NULL,'2026-07-02 10:25:00'),
 (2015,'谢东',1013,'13910000015','xiedong@gzcourt.gov.cn','处长','技术处','1975-11-11','广东省广州市越秀区中山四路 246 号',5,3,'刘洋','2026-10-23 09:30:00','技术验收负责人','2026-02-20 10:10:00'),
 (2016,'罗琳',1013,'13910000016','luolin@gzcourt.gov.cn','副庭长','立案庭','1981-09-02','广东省广州市越秀区中山四路 246 号',5,3,'刘洋','2026-10-23 09:30:00',NULL,'2026-02-20 10:15:00'),
 (2017,'蒋伟',1015,'13910000017','jiangwei@hznetcourt.gov.cn','主任','信息技术中心','1978-05-06','浙江省杭州市西湖区转塘街道象山 352 号',5,3,'刘洋','2026-10-22 10:00:00','战略合作对接人','2026-05-08 10:40:00'),
 (2018,'潘婷',1015,'13910000018','panting@hznetcourt.gov.cn','副主任','审判管理办公室','1989-12-19','浙江省杭州市西湖区转塘街道象山 352 号',5,3,'刘洋','2026-10-22 10:00:00',NULL,'2026-05-08 10:45:00'),
 (2019,'唐磊',1008,'13910000019','tanglei@bj.sgcc.com.cn','主任','物资部','1976-02-28','北京市西城区前门西大街 41 号',2,2,'张伟','2026-10-19 15:00:00',NULL,'2026-05-20 14:10:00'),
 (2020,'邓洁',1008,'13910000020','dengjie@bj.sgcc.com.cn','会计','财务部','1991-04-17','北京市西城区前门西大街 41 号',2,2,'张伟','2026-10-19 15:00:00',NULL,'2026-05-20 14:15:00'),
 (2021,'汪洋',1012,'13910000021','wangyang@pab.com','总经理','零售风控部','1980-08-23','上海市浦东新区陆家嘴环路 1333 号',3,2,'李娜','2026-10-12 16:30:00',NULL,'2026-07-18 09:10:00'),
 (2022,'顾芳',1012,'13910000022','gufang@pab.com','经理','科技部','1986-01-30','上海市浦东新区陆家嘴环路 1333 号',3,2,'李娜','2026-10-12 16:30:00',NULL,'2026-07-18 09:15:00'),
 (2023,'蒋晨',1014,'13910000023','jiangchen@nsqcourt.gov.cn','庭长','立案庭','1973-06-14','广东省深圳市南山区东滨路 128 号',5,3,'刘洋','2026-10-11 11:00:00',NULL,'2026-04-12 09:10:00'),
 (2024,'白露',1014,'13910000024','bailu@nsqcourt.gov.cn','主任','综合办公室','1988-09-09','广东省深圳市南山区东滨路 128 号',5,3,'刘洋','2026-10-11 11:00:00',NULL,'2026-04-12 09:15:00'),
 (2025,'高鹏',1016,'13910000025','gaopeng@gd.chinamobile.com','总监','政企事业部','1979-10-21','广东省广州市天河区珠江西路 11 号',5,3,'刘洋','2026-10-09 14:00:00','潜在客户接口人','2026-08-20 15:10:00'),
 (2026,'林娜',1016,'13910000026','linna@gd.chinamobile.com','经理','采购中心','1992-03-03','广东省广州市天河区珠江西路 11 号',5,3,'刘洋','2026-10-09 14:00:00',NULL,'2026-08-20 15:15:00'),
 (2027,'徐磊',1018,'13910000027','xulei@boc.cn','总经理','公司金融部','1977-07-07','江苏省南京市秦淮区中山南路 148 号',6,14,'陈静','2026-10-14 10:00:00',NULL,'2026-03-05 09:10:00'),
 (2028,'何雯',1019,'13910000028','hewen@njcb.com.cn','总经理','小企业部','1982-11-25','江苏省南京市建邺区中山路 288 号',6,14,'陈静','2026-10-21 15:30:00','小微信贷业务负责人','2026-04-18 10:10:00'),
 (2029,'曹敏',1020,'13910000029','caomin@suzhoubank.com','总经理','风险管理部','1984-02-08','江苏省苏州市工业园区苏州大道东 289 号',6,14,'陈静','2026-10-08 09:30:00',NULL,'2026-06-22 10:10:00'),
 (2030,'邱阳',1005,'13910000030','qiuyang@cmbchina.com','经理','风险管理部','1989-05-12','广东省深圳市福田区深南大道 7088 号',3,2,'李娜','2026-07-10 16:00:00','原负责人离职前接口人','2026-03-12 15:40:00');

-- ---------- 7.3 线索（15，含 3 条已转化、1 条作废、1 条疑似重复） ----------
INSERT INTO crm_lead (id, name, company_name, mobile, email, position_name, industry, source_channel, grade, status,
    account_nature, owner_user_id, owner_org_id, owner_user_name, last_follow_time,
    converted_account_id, converted_contact_id, converted_opportunity_id, converted_time,
    invalid_reason, dup_reason, remark, created_time) VALUES
 (3001,'王建国','中国工商银行股份有限公司北京市分行','13910000001','wangjianguo@icbc.com.cn','部门主任','BANK','REFERRAL','A','CONVERTED','TO_B',2,2,'张伟','2025-11-09 16:00:00',1001,2001,4001,'2025-11-09 17:20:00',NULL,NULL,'老客户转介绍，直接转化','2025-11-06 09:00:00'),
 (3002,'张晨','中国建设银行股份有限公司上海市分行','13910000003','zhangchen@ccb.com','副总经理','BANK','EXHIBITION','A','CONVERTED','TO_B',2,2,'张伟','2025-12-01 15:30:00',1002,2003,4002,'2025-12-01 16:10:00',NULL,NULL,'金融展现场获取','2025-11-28 10:00:00'),
 (3003,'陈国强','北京市政务服务管理局','13910000005','chenguoqiang@zwfwj.gov.cn','处长','GOV','TENDER','A','CONVERTED','TO_G',2,2,'张伟','2026-01-14 11:00:00',1003,2005,4003,'2026-01-14 14:00:00',NULL,NULL,'招标公告线索转化','2026-01-08 09:30:00'),
 (3004,'徐鹏','深圳前海微众银行股份有限公司','13910000031','xupeng@webank.com','数据总监','BANK','WEB','A','FOLLOWING','TO_B',2,2,'张伟','2026-10-21 14:00:00',NULL,NULL,NULL,NULL,NULL,NULL,'已发送方案待评审','2026-10-08 09:00:00'),
 (3005,'钱蕾','中国银联股份有限公司','13910000032','qianlei@unionpay.com','高级经理','FINTECH','PHONE','B','FOLLOWING','TO_B',2,2,'张伟','2026-10-18 10:00:00',NULL,NULL,NULL,NULL,NULL,NULL,'电话呼入咨询接口服务','2026-10-10 11:00:00'),
 (3006,'马超','上海浦东发展银行股份有限公司','13910000033','machao@spdb.com.cn','风险经理','BANK','WEB','C','NEW','TO_B',3,2,'李娜',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'官网表单留资，待首次联系','2026-10-19 15:00:00'),
 (3007,'卢敏','广东省政务服务数据管理局','13910000034','lumin@gd.gov.cn','副处长','GOV','TENDER','A','FOLLOWING','TO_G',5,3,'刘洋','2026-10-23 09:00:00',NULL,NULL,NULL,NULL,NULL,NULL,'已递交标书，等待开标','2026-09-25 10:00:00'),
 (3008,'任杰','宁波银行股份有限公司','13910000035','renjie@nbcb.cn','部门总经理','BANK','PARTNER','B','NEW','TO_B',3,2,'李娜',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'渠道伙伴推荐','2026-10-20 14:00:00'),
 (3009,'柳青','泰康人寿保险有限责任公司','13910000036','liuqing@taikang.com','数据经理','INSURANCE','WEB','C','NEW','TO_B',2,2,'张伟',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'保险反欺诈场景咨询','2026-10-15 09:00:00'),
 (3010,'范伟','成都银行股份有限公司','13910000037','fanwei@cdb.cn','小企业部经理','BANK','REFERRAL','B','FOLLOWING','TO_B',5,3,'刘洋','2026-10-17 16:00:00',NULL,NULL,NULL,NULL,NULL,NULL,'老客户推荐，正在做需求确认','2026-10-09 10:00:00'),
 (3011,'未知来电','不详','00000000000',NULL,NULL,NULL,'PHONE','D','INVALID',NULL,2,2,'张伟',NULL,NULL,NULL,NULL,NULL,'号码为空号，无法联系',NULL,'呼叫中心转入的无效线索','2026-10-11 11:20:00'),
 (3012,'李慧','中国工商银行股份有限公司北京市分行','13910000001','lihui@icbc.com.cn','总经理','BANK','WEB','B','NEW','TO_B',3,2,'李娜',NULL,NULL,NULL,NULL,NULL,NULL,'手机号与已有线索 3001 重复','同一客户不同联系人再次留资，待合并','2026-10-21 09:00:00'),
 (3013,'钱进','徽商银行股份有限公司','13910000038','qianjin@hsbank.com.cn','风险总监','BANK','EXHIBITION','C','NEW','TO_B',3,2,'李娜',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'展会扫码获取','2026-10-16 13:00:00'),
 (3014,'苏珊','长沙银行股份有限公司','13910000039','sushan@cscb.cn','数据部经理','BANK','TENDER','B','FOLLOWING','TO_B',5,3,'刘洋','2026-10-22 15:00:00',NULL,NULL,NULL,NULL,NULL,NULL,'已报名投标，同步跟进','2026-10-13 09:30:00'),
 (3015,'贺强','重庆农村商业银行股份有限公司','13910000040','heqiang@cqrcb.com','部门经理','BANK','OTHER','C','NEW','TO_B',5,3,'刘洋',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'行业会议交流获取','2026-10-20 10:30:00');

-- ---------- 7.4 线索转化日志（幂等最后防线：uk_convert_lead 唯一索引） ----------
INSERT INTO crm_lead_convert_log (id, lead_id, account_id, contact_id, opportunity_id, convert_mode, operator_id, operator_org_id, remark, created_time) VALUES
 (1,3001,1001,2001,4001,'FULL',2,2,'线索转客户+联系人+商机','2025-11-09 17:20:00'),
 (2,3002,1002,2003,4002,'FULL',2,2,'线索转客户+联系人+商机','2025-12-01 16:10:00'),
 (3,3003,1003,2005,4003,'FULL',2,2,'线索转客户+联系人+商机','2026-01-14 14:00:00');

-- ---------- 7.5 商机（12） ----------
INSERT INTO crm_opportunity (id, name, account_id, product_id, expected_amount, expected_close_date, sales_stage,
    win_probability, est_revenue, currency, est_cost, est_profit_rate, est_service_volume, pricing_mode, pricing_detail,
    owner_user_id, owner_org_id, owner_user_name, source_lead_id, is_won, contract_id, address, lost_reason,
    stage_change_time, last_activity_time, remark, created_time) VALUES
 (4001,'工行北京分行-2026 年度企业征信数据服务',1001,NULL,4600000.0000,'2025-11-30','WON',100.00,4600000.0000,'CNY',2760000.0000,40.00,'12 个月',  'LUMP_SUM','{"totalAmount":4600000,"note":"年度包干，含 4 次现场支持"}',2,2,'张伟',3001,1,7001,'北京市西城区复兴门内大街 55 号',NULL,'2025-11-09 17:20:00','2026-10-22 15:20:00',NULL,'2025-11-09 17:20:00'),
 (4002,'建行上海分行-风控数据平台续签',1002,NULL,3200000.0000,'2026-12-15','NEGOTIATION',70.00,3200000.0000,'CNY',1920000.0000,40.00,'12 个月','HEAD_COUNT','{"pricePerPersonMonth":16000,"personMonths":200}',2,2,'张伟',3002,0,NULL,'上海市浦东新区陆家嘴环路 900 号',NULL,'2026-09-01 10:00:00','2026-10-20 11:00:00','续签谈判中，客户关注交付人力投入','2025-12-01 16:10:00'),
 (4003,'北京市政务服务局-一网通办信用核查',1003,NULL,2800000.0000,'2026-12-31','PROPOSAL',50.00,2800000.0000,'CNY',1680000.0000,40.00,'12 个月','UNIT_PRICE','{"unitPrice":28,"quantity":100000,"note":"按核查次数计费"}',2,2,'张伟',3003,0,NULL,'北京市通州区运河东大街 57 号',NULL,'2026-09-20 09:00:00','2026-10-18 10:30:00',NULL,'2026-01-14 14:00:00'),
 (4004,'江苏人社厅-劳动仲裁数据核验',1004,NULL,1500000.0000,'2027-03-31','PROSPECT',20.00,1500000.0000,'CNY',900000.0000,40.00,'12 个月','TIME_BASED','{"hourlyRate":1200,"hours":750}',2,2,'张伟',NULL,0,NULL,'江苏省南京市鼓楼区北京西路 16 号',NULL,'2026-08-15 09:00:00','2026-10-16 14:00:00',NULL,'2026-08-15 09:00:00'),
 (4005,'中信证券-投行尽调数据服务',1006,NULL,2100000.0000,'2026-11-30','PROPOSAL',50.00,2100000.0000,'CNY',1260000.0000,40.00,'12 个月','COMMISSION','{"commissionRate":0.08,"baseAmount":26250000,"note":"按尽调项目金额分成"}',2,2,'张伟',NULL,0,NULL,'北京市朝阳区亮马桥路 48 号',NULL,'2026-09-10 14:00:00','2026-10-21 09:40:00',NULL,'2026-04-01 09:00:00'),
 (4006,'国网北京电力-供应商信用评价',1008,NULL,960000.0000,'2026-11-15','NEGOTIATION',70.00,960000.0000,'CNY',576000.0000,40.00,'12 个月','EFFECT_BASED','{"unitPrice":320,"quantity":3000,"note":"按评价供应商家数结算"}',2,2,'张伟',NULL,0,NULL,'北京市西城区前门西大街 41 号',NULL,'2026-09-05 09:00:00','2026-10-19 15:00:00',NULL,'2026-05-20 14:00:00'),
 (4007,'交通银行北京分行-贷前核验标品',1009,NULL,780000.0000,'2026-12-31','PROSPECT',20.00,780000.0000,'CNY',468000.0000,40.00,'12 个月','FREE',NULL,3,2,'李娜',NULL,0,NULL,'北京市西城区金融大街 33 号',NULL,'2026-09-28 10:00:00','2026-10-17 10:20:00','先用免费试用版验证效果','2026-06-15 09:30:00'),
 (4008,'上海大数据中心-公共数据授权运营',1010,NULL,3500000.0000,'2027-01-31','PROPOSAL',50.00,3500000.0000,'CNY',2100000.0000,40.00,'24 个月','LUMP_SUM','{"totalAmount":3500000,"note":"两年期，分两期付款"}',3,2,'李娜',NULL,0,NULL,'上海市黄浦区人民大道 200 号',NULL,'2026-09-25 09:00:00','2026-10-15 14:40:00',NULL,'2026-07-02 10:10:00'),
 (4009,'广州中院-失信被执行人数据核查',1013,NULL,1200000.0000,'2026-11-30','NEGOTIATION',70.00,1200000.0000,'CNY',720000.0000,40.00,'12 个月','UNIT_PRICE','{"unitPrice":15,"quantity":80000}',5,3,'刘洋',NULL,0,NULL,'广东省广州市越秀区中山四路 246 号',NULL,'2026-09-12 09:00:00','2026-10-23 09:30:00',NULL,'2026-02-20 10:00:00'),
 (4010,'杭州互联网法院-电子证据链核验',1015,NULL,2600000.0000,'2026-10-14','WON',100.00,2600000.0000,'CNY',1560000.0000,40.00,'12 个月','LUMP_SUM','{"totalAmount":2600000,"note":"含系统对接与培训"}',5,3,'刘洋',NULL,1,7002,'浙江省杭州市西湖区转塘街道象山 352 号',NULL,'2026-10-10 09:00:00','2026-10-22 10:00:00',NULL,'2026-05-08 10:30:00'),
 (4011,'南京银行-小微信贷数据合作',1019,NULL,890000.0000,'2027-02-28','PROPOSAL',50.00,890000.0000,'CNY',534000.0000,40.00,'12 个月','UNIT_PRICE','{"unitPrice":9,"quantity":100000}',6,14,'陈静',NULL,0,NULL,'江苏省南京市建邺区中山路 288 号',NULL,'2026-10-01 09:00:00','2026-10-21 15:30:00',NULL,'2026-04-18 10:00:00'),
 (4012,'中国人寿-保险反欺诈数据服务',1007,NULL,1700000.0000,'2026-09-30','LOST',0.00,1700000.0000,'CNY',1020000.0000,40.00,'12 个月','LUMP_SUM','{"totalAmount":1700000}',2,2,'张伟',NULL,0,NULL,'北京市西城区金融大街 16 号','2026 年度预算冻结，项目延期至下一年度','2026-08-30 16:00:00','2026-06-28 10:00:00',NULL,'2026-05-06 11:00:00');

-- ---------- 7.6 竞争对手（6，用于商机对比矩阵） ----------
INSERT INTO crm_competitor (id, opportunity_id, competitor_name, staff_size, coverage_scope, tech_ability,
    pricing_mode, pricing_detail, operation_efficiency, operation_quality, owner_user_id, owner_org_id, owner_user_name, remark, created_time) VALUES
 (5001,4002,'上海资信有限公司',320,'华东',  'MEDIUM','HEAD_COUNT','{"pricePerPersonMonth":14000}','MEDIUM','MEDIUM',2,2,'张伟','本地服务响应快，价格低 12%','2026-09-15 10:00:00'),
 (5002,4002,'鹏元征信有限公司',450,'全国',  'HIGH',  'LUMP_SUM','{"totalAmount":2980000}','MEDIUM','HIGH',2,2,'张伟','数据维度接近，报价略低','2026-09-15 10:05:00'),
 (5003,4005,'中诚信征信有限公司',600,'全国',  'HIGH',  'COMMISSION','{"commissionRate":0.075}','HIGH','HIGH',2,2,'张伟','券商渠道关系强','2026-09-18 14:00:00'),
 (5004,4006,'国网信通产业集团',1200,'全国', 'HIGH',  'EFFECT_BASED','{"unitPrice":300}','HIGH','HIGH',2,2,'张伟','集团内部关联方，天然优势','2026-09-20 09:00:00'),
 (5005,4009,'华宇软件股份有限公司',2000,'全国','HIGH',  'UNIT_PRICE','{"unitPrice":13}','HIGH','HIGH',5,3,'刘洋','法院行业存量客户多','2026-09-22 10:00:00'),
 (5006,4010,'共道网络科技有限公司',180,'华东',  'MEDIUM','LUMP_SUM','{"totalAmount":2450000}','MEDIUM','MEDIUM',5,3,'刘洋','价格战报价，交付人力不足','2026-10-08 10:00:00');

-- ---------- 7.7 销售跟进活动（40，多态挂接线索/客户/联系人/商机/合同/投标） ----------
INSERT INTO crm_activity (id, subject, type, level, start_time, end_time, duration_days, target_type, target_id,
    owner_user_id, owner_org_id, owner_user_name, description, file_ids, status, void_reason, remark, created_time) VALUES
 (6001,'工行年度服务启动会',      'MEETING','A','2026-10-08 09:00:00','2026-10-08 11:30:00',1,'ACCOUNT',1001,2,2,'张伟','确认年度交付计划与对接人','','VALID',NULL,NULL,'2026-10-08 09:00:00'),
 (6002,'工行信息科技部需求复盘',  'VISIT','A','2026-10-12 14:00:00','2026-10-12 16:00:00',1,'CONTACT',2002,2,2,'张伟','梳理接口改造需求','','VALID',NULL,NULL,'2026-10-12 14:00:00'),
 (6003,'工行季度交付质量回访',    'CALL','B','2026-10-20 10:00:00','2026-10-20 10:30:00',1,'ACCOUNT',1001,2,2,'张伟','客户满意度 4.6 分','','VALID',NULL,NULL,'2026-10-20 10:00:00'),
 (6004,'建行续签方案讲解',        'MEETING','A','2026-10-09 09:30:00','2026-10-09 12:00:00',1,'OPPORTUNITY',4002,2,2,'张伟','讲解人力投入模型','','VALID',NULL,NULL,'2026-10-09 09:30:00'),
 (6005,'建行风险部价格谈判',      'MEETING','A','2026-10-20 11:00:00','2026-10-20 13:00:00',1,'OPPORTUNITY',4002,2,2,'张伟','客户要求降价 8%，待内部审批','','VALID',NULL,NULL,'2026-10-20 11:00:00'),
 (6006,'建行数据管理部对接',      'EMAIL','C','2026-10-14 15:00:00','2026-10-14 15:15:00',1,'CONTACT',2004,2,2,'张伟','发送数据字典与接口说明','','VALID',NULL,NULL,'2026-10-14 15:00:00'),
 (6007,'政务服务局方案汇报',      'VISIT','A','2026-10-10 10:00:00','2026-10-10 12:00:00',1,'OPPORTUNITY',4003,2,2,'张伟','向数据处汇报信用核查方案','','VALID',NULL,NULL,'2026-10-10 10:00:00'),
 (6008,'政务服务局信息化处沟通',  'CALL','B','2026-10-18 10:30:00','2026-10-18 11:00:00',1,'CONTACT',2006,2,2,'张伟','确认安全评估要求','','VALID',NULL,NULL,'2026-10-18 10:30:00'),
 (6009,'政务服务局预算摸底',      'CALL','B','2026-09-28 14:00:00','2026-09-28 14:30:00',1,'ACCOUNT',1003,2,2,'张伟','2027 年预算盘子约 300 万','','VALID',NULL,NULL,'2026-09-28 14:00:00'),
 (6010,'江苏人社厅需求调研',      'VISIT','B','2026-10-16 14:00:00','2026-10-16 17:00:00',1,'ACCOUNT',1004,2,2,'张伟','调研劳动仲裁数据核验场景','','VALID',NULL,NULL,'2026-10-16 14:00:00'),
 (6011,'江苏人社厅技术交流',      'MEETING','C','2026-10-16 14:30:00','2026-10-16 15:30:00',1,'CONTACT',2007,2,2,'张伟','确认数据接口规范','','VALID',NULL,NULL,'2026-10-16 14:30:00'),
 (6012,'中信证券合规评审会',      'MEETING','A','2026-10-21 09:40:00','2026-10-21 11:00:00',1,'OPPORTUNITY',4005,2,2,'张伟','合规部关注数据来源合规性','','VALID',NULL,NULL,'2026-10-21 09:40:00'),
 (6013,'中信证券技术方案确认',    'EMAIL','B','2026-10-15 16:00:00','2026-10-15 16:20:00',1,'CONTACT',2009,2,2,'张伟','发送尽调数据服务技术方案','','VALID',NULL,NULL,'2026-10-15 16:00:00'),
 (6014,'国网电力供应商评价沟通',  'VISIT','A','2026-10-19 15:00:00','2026-10-19 17:00:00',1,'OPPORTUNITY',4006,2,2,'张伟','确认 3000 家供应商评价范围','','VALID',NULL,NULL,'2026-10-19 15:00:00'),
 (6015,'国网电力物资部对接',      'CALL','C','2026-10-13 09:30:00','2026-10-13 10:00:00',1,'CONTACT',2019,2,2,'张伟','确认招采流程','','VALID',NULL,NULL,'2026-10-13 09:30:00'),
 (6016,'交行标品试用反馈',        'CALL','B','2026-10-17 10:20:00','2026-10-17 10:50:00',1,'OPPORTUNITY',4007,3,2,'李娜','试用期核查通过率 92%','','VALID',NULL,NULL,'2026-10-17 10:20:00'),
 (6017,'交行采购部报价沟通',      'EMAIL','C','2026-10-11 11:00:00','2026-10-11 11:15:00',1,'CONTACT',2012,3,2,'李娜','发送标品报价单','','VALID',NULL,NULL,'2026-10-11 11:00:00'),
 (6018,'上海大数据中心运营方案',  'MEETING','A','2026-10-15 14:40:00','2026-10-15 16:40:00',1,'OPPORTUNITY',4008,3,2,'李娜','讨论授权运营分成机制','','VALID',NULL,NULL,'2026-10-15 14:40:00'),
 (6019,'上海大数据中心综合处沟通','CALL','C','2026-10-09 10:00:00','2026-10-09 10:20:00',1,'CONTACT',2014,3,2,'李娜','确认立项材料清单','','VALID',NULL,NULL,'2026-10-09 10:00:00'),
 (6020,'平安银行试点启动会',      'MEETING','B','2026-10-12 16:30:00','2026-10-12 17:30:00',1,'ACCOUNT',1012,3,2,'李娜','零售风控数据试点启动','','VALID',NULL,NULL,'2026-10-12 16:30:00'),
 (6021,'招行公共池认领前沟通',    'CALL','C','2026-07-10 16:00:00','2026-07-10 16:20:00',1,'ACCOUNT',1005,3,2,'李娜','离职前最后一次跟进','','VALID',NULL,NULL,'2026-07-10 16:00:00'),
 (6022,'广州中院失信数据演示',    'VISIT','A','2026-10-23 09:30:00','2026-10-23 11:30:00',1,'OPPORTUNITY',4009,5,3,'刘洋','现场演示被执行人数据核查','','VALID',NULL,NULL,'2026-10-23 09:30:00'),
 (6023,'广州中院立案庭沟通',      'MEETING','B','2026-10-16 15:00:00','2026-10-16 16:00:00',1,'CONTACT',2016,5,3,'刘洋','确认数据更新频率','','VALID',NULL,NULL,'2026-10-16 15:00:00'),
 (6024,'杭州互联网法院验收会',    'MEETING','A','2026-10-22 10:00:00','2026-10-22 12:00:00',1,'CONTRACT',7002,5,3,'刘洋','电子证据链系统初验通过','','VALID',NULL,NULL,'2026-10-22 10:00:00'),
 (6025,'杭州互联网法院技术对接',  'VISIT','A','2026-10-13 14:00:00','2026-10-13 17:00:00',1,'CONTACT',2017,5,3,'刘洋','联调证据哈希校验接口','','VALID',NULL,NULL,'2026-10-13 14:00:00'),
 (6026,'南山法院诉前调解沟通',    'CALL','B','2026-10-11 11:00:00','2026-10-11 11:40:00',1,'ACCOUNT',1014,5,3,'刘洋','确认诉前调解辅助服务范围','','VALID',NULL,NULL,'2026-10-11 11:00:00'),
 (6027,'广东移动资质核验',        'EMAIL','C','2026-10-09 14:00:00','2026-10-09 14:20:00',1,'ACCOUNT',1016,5,3,'刘洋','发送资质材料清单','','VALID',NULL,NULL,'2026-10-09 14:00:00'),
 (6028,'广东移动政企部交流',      'VISIT','B','2026-10-09 15:00:00','2026-10-09 17:00:00',1,'CONTACT',2025,5,3,'刘洋','了解政企数据服务需求','','VALID',NULL,NULL,'2026-10-09 15:00:00'),
 (6029,'顺丰物流信用场景沟通',    'CALL','C','2026-07-25 10:00:00','2026-07-25 10:30:00',1,'ACCOUNT',1017,5,3,'刘洋','客户预算未落实','','VALID',NULL,NULL,'2026-07-25 10:00:00'),
 (6030,'成都银行需求确认',        'CALL','B','2026-10-17 16:00:00','2026-10-17 16:30:00',1,'LEAD',3010,5,3,'刘洋','确认小微风控数据需求','','VALID',NULL,NULL,'2026-10-17 16:00:00'),
 (6031,'长沙银行投标跟进',        'EMAIL','C','2026-10-22 15:00:00','2026-10-22 15:20:00',1,'LEAD',3014,5,3,'刘洋','发送投标资料清单','','VALID',NULL,NULL,'2026-10-22 15:00:00'),
 (6032,'微众银行方案评审',        'MEETING','A','2026-10-21 14:00:00','2026-10-21 16:00:00',1,'LEAD',3004,2,2,'张伟','方案待客户内部评审','','VALID',NULL,NULL,'2026-10-21 14:00:00'),
 (6033,'银联接口咨询答复',        'CALL','B','2026-10-18 10:00:00','2026-10-18 10:30:00',1,'LEAD',3005,2,2,'张伟','答复接口并发与计费方式','','VALID',NULL,NULL,'2026-10-18 10:00:00'),
 (6034,'南京银行小微信贷方案',    'VISIT','A','2026-10-21 15:30:00','2026-10-21 17:30:00',1,'OPPORTUNITY',4011,6,14,'陈静','讲解按笔计费模型','','VALID',NULL,NULL,'2026-10-21 15:30:00'),
 (6035,'南京银行框架协议续签',    'MEETING','B','2026-10-14 09:00:00','2026-10-14 10:00:00',1,'ACCOUNT',1019,6,14,'陈静','原框架协议已失效，需重新签署','','VALID',NULL,NULL,'2026-10-14 09:00:00'),
 (6036,'中国银行江苏分行年度回访', 'VISIT','B','2026-10-14 10:00:00','2026-10-14 12:00:00',1,'ACCOUNT',1018,6,14,'陈静','年度合作满意度回访','','VALID',NULL,NULL,'2026-10-14 10:00:00'),
 (6037,'苏州银行试点沟通',        'CALL','C','2026-10-08 09:30:00','2026-10-08 10:00:00',1,'ACCOUNT',1020,6,14,'陈静','确认试点范围','','VALID',NULL,NULL,'2026-10-08 09:30:00'),
 (6038,'广东政务数据局投标准备',  'MEETING','A','2026-10-20 09:00:00','2026-10-20 12:00:00',1,'LEAD',3007,5,3,'刘洋','标书定稿会议','','VALID',NULL,NULL,'2026-10-20 09:00:00'),
 (6039,'国寿反欺诈项目复盘',      'MEETING','B','2026-06-28 10:00:00','2026-06-28 11:00:00',1,'ACCOUNT',1007,2,2,'张伟','客户预算冻结，转二期跟进','','VALID',NULL,NULL,'2026-06-28 10:00:00'),
 (6040,'作废演示：误建拜访记录',  'VISIT','C','2026-10-05 10:00:00','2026-10-05 11:00:00',1,'ACCOUNT',1002,2,2,'张伟','误操作创建的记录','','VOIDED','重复录入，已作废',NULL,'2026-10-05 10:00:00');

-- ---------- 7.8 合同（10） ----------
INSERT INTO crm_contract (id, contract_no, name, account_id, company_id, business_group_id, product_id,
    sign_date, start_date, end_date, duration_days, contract_type, first_contract_id, status,
    source_type, source_id, win_amount, owner_user_id, owner_org_id, owner_user_name,
    is_archived, archive_time, paper_box_no, paper_copies, activate_time, terminate_time, terminate_reason,
    flow_instance_id, remark, created_time) VALUES
 (7001,'HT202511-0001','工行北京分行 2026 年度企业征信数据服务合同',1001,1,2,NULL,'2025-11-25','2025-12-01','2026-11-30',364,'FIRST',NULL,'ACTIVE','OPPORTUNITY',4001,4600000.0000,2,2,'张伟',1,'2025-12-05 10:00:00','A-2025-0118',2,'2025-12-01 00:00:00',NULL,NULL,NULL,'年度最大单，已归档纸质件','2025-11-20 09:00:00'),
 (7002,'HT202610-0002','杭州互联网法院电子证据链核验服务合同',1015,2,3,NULL,'2026-10-10','2026-10-15','2027-10-14',364,'FIRST',NULL,'ACTIVE','OPPORTUNITY',4010,2600000.0000,5,3,'刘洋',0,NULL,'B-2026-0091',2,'2026-10-15 00:00:00',NULL,NULL,NULL,'战略合作首个落地合同','2026-10-08 10:00:00'),
 (7003,'HT202609-0003','中信证券投行尽调数据服务合同',1006,1,2,NULL,'2026-09-18','2026-10-01','2027-09-30',364,'FIRST',NULL,'ACTIVE','MANUAL',NULL,1980000.0000,2,2,'张伟',0,NULL,'A-2026-0142',1,'2026-10-01 00:00:00',NULL,NULL,NULL,'按项目分成模式结算','2026-09-15 14:00:00'),
 (7004,'HT202608-0004','国网北京电力供应商信用评价合同',1008,1,2,NULL,'2026-08-20','2026-09-01','2027-08-31',364,'FIRST',NULL,'ACTIVE','MANUAL',NULL,960000.0000,2,2,'张伟',0,NULL,'A-2026-0138',1,'2026-09-01 00:00:00',NULL,NULL,NULL,NULL,'2026-08-18 10:00:00'),
 (7005,'HT202607-0005','交通银行北京分行贷前核验标品合同',1009,1,2,NULL,'2026-07-15','2026-08-01','2027-07-31',364,'FIRST',NULL,'ACTIVE','MANUAL',NULL,760000.0000,3,2,'李娜',0,NULL,NULL,1,'2026-08-01 00:00:00',NULL,NULL,NULL,'标品标准合同模板','2026-07-12 09:00:00'),
 (7006,'HT202606-0006','建设银行上海分行风控数据服务合同',1002,1,2,NULL,'2026-06-20','2026-07-01','2027-06-30',364,'FIRST',NULL,'ACTIVE','MANUAL',NULL,3000000.0000,2,2,'张伟',1,'2026-07-02 10:00:00','A-2026-0121',2,'2026-07-01 00:00:00',NULL,NULL,NULL,'续签谈判的基准合同','2026-06-18 09:00:00'),
 (7007,'HT202610-0007','南京银行小微信贷数据合作合同',1019,1,14,NULL,'2026-10-20','2026-11-01','2027-10-31',364,'FIRST',NULL,'DRAFT','OPPORTUNITY',4011,890000.0000,6,14,'陈静',0,NULL,NULL,1,NULL,NULL,NULL,NULL,'草稿待提交审批','2026-10-20 15:00:00'),
 (7008,'HT202609-0008','中国人寿保险反欺诈数据服务合同',1007,1,2,NULL,'2025-09-25','2025-10-01','2026-09-30',364,'FIRST',NULL,'EXPIRED','MANUAL',NULL,1700000.0000,2,2,'张伟',1,'2026-10-08 10:00:00','A-2025-0095',1,'2025-10-01 00:00:00',NULL,NULL,NULL,'已到期未续签，客户预算冻结','2025-09-22 09:00:00'),
 (7009,'HT202608-0009','深圳南山法院诉前调解辅助服务合同',1014,2,3,NULL,'2026-08-25','2026-09-01','2027-08-31',364,'FIRST',NULL,'APPROVING','MANUAL',NULL,680000.0000,5,3,'刘洋',0,NULL,NULL,1,NULL,NULL,NULL,'wf-crm-contract-0009','审批中，等待法务审核','2026-08-25 09:00:00'),
 (7010,'HT202512-0010','江苏人社厅劳动仲裁数据核验合同',1004,1,2,NULL,'2025-12-15','2026-01-01','2026-12-31',364,'FIRST',NULL,'TERMINATED','MANUAL',NULL,1400000.0000,2,2,'张伟',1,'2026-03-10 10:00:00','A-2025-0126',1,'2026-01-01 00:00:00','2026-03-05 10:00:00','客户机构改革，项目终止',NULL,'机构改革后项目不再重启','2025-12-12 09:00:00');

-- ---------- 7.9 协议（6，含 2 条长期协议 end_date 为空） ----------
INSERT INTO crm_agreement (id, agreement_no, name, agreement_type, account_id, company_id, opportunity_id, product_id,
    business_group_id, sign_date, start_date, end_date, duration_days, status, sign_type, first_agreement_id,
    win_amount, owner_user_id, owner_org_id, owner_user_name, is_archived, archive_time, paper_box_no, paper_copies,
    activate_time, flow_instance_id, terminate_reason, remark, created_time) VALUES
 (8001,'XY202511-0001','工行总对总框架合作协议','FRAMEWORK',1001,1,4001,NULL,2,'2025-11-20','2025-12-01','2028-11-30',1095,'EFFECTIVE','FIRST',NULL,4600000.0000,2,2,'张伟',1,'2025-12-05 10:30:00','A-2025-0119',2,'2025-12-01 00:00:00',NULL,NULL,'总对总三年框架，年度据实下单','2025-11-20 10:00:00'),
 (8002,'XY202603-0002','中信证券数据保密协议','NDA',1006,1,NULL,NULL,2,'2026-03-25','2026-04-01',NULL,NULL,'EFFECTIVE','FIRST',NULL,NULL,2,2,'张伟',0,NULL,NULL,1,'2026-04-01 00:00:00',NULL,NULL,'长期有效，无到期日','2026-03-25 10:00:00'),
 (8003,'XY202605-0003','杭州互联网法院战略合作框架协议','FRAMEWORK',1015,2,4010,NULL,3,'2026-05-15','2026-06-01','2029-05-31',1095,'EFFECTIVE','FIRST',NULL,2600000.0000,5,3,'刘洋',0,NULL,'B-2026-0085',2,'2026-06-01 00:00:00',NULL,NULL,'战略级合作，含联合实验室','2026-05-12 10:00:00'),
 (8004,'XY202606-0004','建设银行上海分行服务协议','SERVICE',1002,1,4002,NULL,2,'2026-06-18','2026-07-01','2027-06-30',364,'EFFECTIVE','FIRST',NULL,3000000.0000,2,2,'张伟',0,NULL,NULL,1,'2026-07-01 00:00:00',NULL,NULL,NULL,'2026-06-18 10:00:00'),
 (8005,'XY202609-0005','上海大数据中心公共数据授权运营协议','FRAMEWORK',1010,1,4008,NULL,2,'2026-09-28','2026-11-01','2029-10-31',1095,'APPROVING','FIRST',NULL,3500000.0000,3,2,'李娜',0,NULL,NULL,1,NULL,'wf-crm-agreement-0005',NULL,'授权运营模式需法务重点审核','2026-09-28 10:00:00'),
 (8006,'XY202607-0006','南京银行框架协议（首期）','FRAMEWORK',1019,1,4011,NULL,14,'2026-07-10','2026-08-01','2026-07-31',NULL,'EXPIRED','FIRST',NULL,600000.0000,6,14,'陈静',1,'2026-08-05 10:00:00','A-2026-0130',1,'2026-08-01 00:00:00',NULL,NULL,'已失效，需重新签署框架','2026-07-08 10:00:00');

-- ---------- 7.10 投标（15） ----------
INSERT INTO crm_bid (id, account_id, tender_name, tender_no, tender_form, bid_entity, province, city, region_org_id,
    price_cap, open_date, deadline, deposit, deposit_status, bid_status, win_amount, win_result, bid_plan,
    tender_url, win_url, is_won, owner_user_id, owner_org_id, owner_user_name, lost_reason, abandon_reason,
    gen_opportunity_id, gen_contract_id, remark, created_time) VALUES
 (9001,1013,'广东省政务数据治理服务采购项目','GD-2026-ZWSJ-018','OPEN',1,'广东省','广州市',22,5000000.0000,'2026-11-05','2026-10-28',100000.0000,'PAID','REGISTERED',NULL,NULL,'技术标+商务标，已完成标书初稿','http://www.ccgp-guangdong.gov.cn/notice/018',NULL,0,5,3,'刘洋',NULL,NULL,NULL,NULL,'重点项目，竞争激烈','2026-10-05 09:00:00'),
 (9002,1018,'人民银行征信中心数据接口服务采购','PBC-2026-JK-002','OPEN',1,'北京市','北京市',20,8000000.0000,'2026-11-12','2026-11-01',200000.0000,'PAID','SUBMITTED',NULL,NULL,'已递交，等待开标','http://www.ccgp.gov.cn/notice/002',NULL,0,6,14,'陈静',NULL,NULL,4004,NULL,'体量最大的公开标','2026-09-28 09:00:00'),
 (9003,1016,'中国银联数据服务框架采购','UP-2026-FW-031','INVITE',1,'上海市','上海市',21,3000000.0000,'2026-10-30','2026-10-24',50000.0000,'PAID','TRACKING',NULL,NULL,'受邀投标，材料准备中','http://www.unionpay.com/tender/031',NULL,0,6,14,'陈静',NULL,NULL,NULL,NULL,'邀请招标，需资格预审','2026-10-02 09:00:00'),
 (9004,1019,'浦发银行风控数据服务竞争性磋商','SPDB-2026-CS-014','NEGOTIATED',1,'上海市','上海市',21,2400000.0000,'2026-11-20','2026-11-10',NULL,'NONE','TRACKING',NULL,NULL,'磋商文件已获取','http://www.spdb.com.cn/tender/014',NULL,0,6,14,'陈静',NULL,NULL,4011,NULL,'竞争性磋商，两轮报价','2026-10-12 09:00:00'),
 (9005,1015,'杭州互联网法院电子证据核验服务','HZ-2026-DZ-007','SINGLE',2,'浙江省','杭州市',21,2800000.0000,'2026-11-02','2026-10-26',NULL,'NONE','REGISTERED',NULL,NULL,'单一来源采购，准备响应文件','http://www.hznetcourt.gov.cn/notice/007',NULL,0,5,3,'刘洋',NULL,NULL,4010,7002,'已在框架协议下走单一来源','2026-10-09 09:00:00'),
 (9006,1007,'泰康人寿保险反欺诈数据服务询价','TK-2026-XJ-021','INQUIRY',1,'北京市','北京市',20,1800000.0000,'2026-10-26','2026-10-22',NULL,'NONE','SUBMITTED',NULL,NULL,'已报价，等待询价结果','http://www.taikang.com/procure/021',NULL,0,2,2,'张伟',NULL,NULL,4012,NULL,'询价采购，最低价优先','2026-10-10 09:00:00'),
 (9007,1010,'成都银行小微风控数据采购','CDB-2026-ZB-009','OPEN',1,'四川省','成都市',24,1500000.0000,'2026-09-28','2026-09-20',30000.0000,'REFUNDED','WON',1350000.0000,'综合评分第一中标','技术分 68，商务分 26','http://www.cdb.cn/tender/009','http://www.cdb.cn/win/009',1,5,3,'刘洋',NULL,NULL,4011,NULL,'已中标，待签合同','2026-08-20 09:00:00'),
 (9008,1019,'徽商银行企业征信数据服务招标','HS-2026-ZB-005','INVITE',1,'安徽省','合肥市',23,1200000.0000,'2026-09-25','2026-09-15',20000.0000,'REFUNDED','LOST',NULL,'第二名，价格劣势',NULL,'http://www.hsbank.com.cn/tender/005',NULL,0,6,14,'陈静','竞争对手报价低 15%',NULL,NULL,NULL,'已失标，复盘报价策略','2026-08-15 09:00:00'),
 (9009,1014,'长沙银行信贷数据服务采购','CSCB-2026-ZB-011','OPEN',2,'湖南省','长沙市',23,1000000.0000,'2026-11-08','2026-10-30',20000.0000,'PAID','REGISTERED',NULL,NULL,'标书编制中','http://www.cscb.cn/tender/011',NULL,0,5,3,'刘洋',NULL,NULL,NULL,NULL,NULL,'2026-10-13 09:00:00'),
 (9010,1015,'重庆农商行农户信用数据服务','CQRCB-2026-CS-003','NEGOTIATED',2,'重庆市','重庆市',24,900000.0000,'2026-12-01','2026-11-20',NULL,'NONE','TRACKING',NULL,NULL,'磋商文件待领取','http://www.cqrcb.com/tender/003',NULL,0,5,3,'刘洋',NULL,NULL,NULL,NULL,NULL,'2026-10-18 09:00:00'),
 (9011,1003,'北京市政务服务信用核查服务采购','BJ-2026-ZB-076','OPEN',1,'北京市','北京市',20,3200000.0000,'2026-11-15','2026-11-05',60000.0000,'PAID','SUBMITTED',NULL,NULL,'已递交，进入评标阶段','http://www.ccgp-beijing.gov.cn/notice/076',NULL,0,2,2,'张伟',NULL,NULL,4003,NULL,'对应一网通办信用核查商机','2026-10-02 09:00:00'),
 (9012,1008,'国网北京电力供应商信用评价服务','SGCC-2026-CS-012','NEGOTIATED',1,'北京市','北京市',20,1200000.0000,'2026-10-29','2026-10-20',NULL,'NONE','WON',960000.0000,'磋商后确定为成交供应商','两轮磋商，最终价 96 万','http://www.bj.sgcc.com.cn/tender/012','http://www.bj.sgcc.com.cn/win/012',1,2,2,'张伟',NULL,NULL,4006,7004,'已转合同 7004','2026-09-20 09:00:00'),
 (9013,1016,'微众银行数据服务采购项目','WEBANK-2026-ZB-004','OPEN',1,'广东省','深圳市',22,2000000.0000,'2026-11-22','2026-11-12',40000.0000,'NONE','TRACKING',NULL,NULL,'等待资格预审结果','http://www.webank.com/procure/004',NULL,0,2,2,'张伟',NULL,NULL,NULL,NULL,NULL,'2026-10-16 09:00:00'),
 (9014,1016,'广东移动政企数据服务邀请招标','CMCC-GD-2026-IB-008','INVITE',1,'广东省','广州市',22,4000000.0000,'2026-10-24','2026-10-16',NULL,'NONE','ABANDONED',NULL,NULL,NULL,'http://www.10086.cn/tender/008',NULL,0,5,3,'刘洋',NULL,'资质要求不满足（需等保三级），主动放弃',NULL,NULL,NULL,'2026-09-30 09:00:00'),
 (9015,1017,'顺丰速运物流信用数据询价','SF-2026-XJ-002','INQUIRY',2,'广东省','深圳市',22,600000.0000,'2026-11-30','2026-11-18',NULL,'NONE','LOST',NULL,'客户选择内部自建方案',NULL,'http://www.sf-express.com/procure/002',NULL,0,5,3,'刘洋','客户决定自建',NULL,NULL,NULL,NULL,'2026-10-11 09:00:00');

-- ---------- 7.11 客户池流转日志 ----------
INSERT INTO crm_account_pool_log (id, account_id, from_status, to_status, trigger_type, reason,
    operator_id, operator_org_id, remark, created_time) VALUES
 (1,1005,'PRIVATE','POOL','MANUAL','原负责人离职，手工释放至公共池',3,2,'李娜操作','2026-10-18 09:00:00'),
 (2,1011,'PRIVATE','POOL','AUTO_TIMEOUT','超过 90 天无有效跟进，定时任务自动入池',NULL,NULL,'客户池回收定时任务','2026-09-30 09:00:00'),
 (3,1017,'PRIVATE','POOL','MANUAL','负责人转岗释放',5,3,'刘洋操作','2026-10-20 09:00:00'),
 (4,1007,'PRIVATE','CHURNED','CHURN','2026 年度预算冻结，客户流失',2,2,'张伟置流失','2026-06-28 10:00:00');

-- ---------- 7.12 认领申请（1 条审批中，1 条已通过） ----------
INSERT INTO crm_claim_apply (id, account_id, applicant_id, applicant_org_id, old_owner_id, status,
    flow_instance_id, appeal_deadline, appeal_status, apply_reason, remark, created_time) VALUES
 (1,1005,3,2,3,'APPROVING','wf-crm-claim-0001','2026-10-20 09:00:00','NONE','原为我名下客户，离职交接后希望重新认领','等待部门负责人审批','2026-10-18 10:00:00'),
 (2,1017,6,14,5,'APPROVED',NULL,NULL,'NONE','商务部拟承接该潜在客户','已通过','2026-10-21 09:00:00');

-- ---------- 7.13 导入批次（演示两段式导入结果） ----------
INSERT INTO crm_import_batch (id, biz_type, file_id, error_file_id, total_count, success_count, fail_count,
    status, error_detail, operator_id, remark, created_time) VALUES
 (1,'crm_lead',1,NULL,50,47,3,'PARTIAL_FAILED','第 12 行：手机号格式不合法；第 27 行：公司名称为空；第 41 行：手机号与已有线索重复',2,'10 月展会线索批量导入','2026-10-16 16:00:00');

-- ---------- 7.14 埋点（漏斗与转化统计的数据来源） ----------
INSERT INTO crm_event_log (id, event_code, biz_id, operator_id, operator_org_id, props, created_time) VALUES
 (1,'crm_lead_create',3004,2,2,'{"sourceChannel":"WEB","grade":"A"}','2026-10-08 09:00:00'),
 (2,'crm_lead_create',3005,2,2,'{"sourceChannel":"PHONE","grade":"B"}','2026-10-10 11:00:00'),
 (3,'crm_lead_convert',3001,2,2,'{"accountId":1001,"opportunityId":4001}','2025-11-09 17:20:00'),
 (4,'crm_lead_convert',3002,2,2,'{"accountId":1002,"opportunityId":4002}','2025-12-01 16:10:00'),
 (5,'crm_lead_convert',3003,2,2,'{"accountId":1003,"opportunityId":4003}','2026-01-14 14:00:00'),
 (6,'crm_opp_stage_change',4002,2,2,'{"fromStage":"PROPOSAL","toStage":"NEGOTIATION"}','2026-09-01 10:00:00'),
 (7,'crm_opp_stage_change',4001,2,2,'{"fromStage":"NEGOTIATION","toStage":"WON"}','2025-11-09 17:20:00'),
 (8,'crm_contract_activate',7001,2,2,'{"winAmount":4600000}','2025-12-01 00:00:00'),
 (9,'crm_contract_activate',7002,5,3,'{"winAmount":2600000}','2026-10-15 00:00:00'),
 (10,'crm_bid_status_change',9012,2,2,'{"fromStatus":"SUBMITTED","toStatus":"WON"}','2026-10-29 09:00:00');

-- ------------------------------------------------------------
-- 8. 定时任务（CRM 7 个后台任务，由底座调度器统一驱动，均为幂等操作）
-- ------------------------------------------------------------
INSERT INTO sys_job (id, job_name, job_code, cron, status, remark) VALUES
 (3,'客户池自动回收','CRM_ACCOUNT_POOL_RECYCLE','0 30 2 * * ?',1,'无活动超过 crm.pool.timeout.days 的客户自动入公共池'),
 (4,'线索未跟进回收','CRM_LEAD_RECYCLE','0 0 3 * * ?',1,'超过 crm.lead.pool.timeout.days 未跟进的线索置无效'),
 (5,'合同到期扫描','CRM_CONTRACT_EXPIRE','0 0 8 * * ?',1,'到期前 crm.contract.expire.notify.days 推送提醒，到期后置 EXPIRED'),
 (6,'协议到期扫描','CRM_AGREEMENT_EXPIRE','0 10 8 * * ?',1,'长协 end_date 为空必须跳过'),
 (7,'投标开标提醒','CRM_BID_OPENING_REMIND','0 20 8 * * ?',1,'开标前 crm.bid.open.notify.days 提醒负责人'),
 (8,'跟进任务逾期提醒','CRM_ACTIVITY_OVERDUE','0 40 8 * * ?',1,'任务型活动到期未完成生成底座待办，按活动去重'),
 (9,'认领申诉超时处理','CRM_CLAIM_APPEAL_TIMEOUT','0 0 9 * * ?',1,'超过 crm.claim.appeal.hours 未申诉自动关闭申诉窗口');


