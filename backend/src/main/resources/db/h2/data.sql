-- ============================================================
-- 种子数据（开发档 H2；sql/02_init_data.sql 为 MySQL 同源版本）
-- 口令统一为 Admin@123（BCrypt），演示账号仅用于本地验证
-- ============================================================

-- ---------- 组织架构：集团 → 8 BG + 10 职能部门 + 6 地区部 ----------
INSERT INTO sys_org (id, org_code, org_name, org_type, parent_id, ancestors, leader_id, sort_no, status) VALUES
 (1,'GL','国联政信','GROUP',0,'0',NULL,1,1),
 (2,'GL-BG-G','G端BG','BG',1,'0,1',NULL,2,1),
 (3,'GL-BG-HH','和合BG','BG',1,'0,1',NULL,3,1),
 (4,'GL-BG-SL','丝路BG','BG',1,'0,1',NULL,4,1),
 (5,'GL-BG-JY','解忧BG','BG',1,'0,1',NULL,5,1),
 (6,'GL-BG-DA','数据资产BG','BG',1,'0,1',NULL,6,1),
 (7,'GL-BG-ZK','智库BG','BG',1,'0,1',NULL,7,1),
 (8,'GL-BG-PX','培训BG','BG',1,'0,1',NULL,8,1),
 (9,'GL-BG-DXE','DXE平台BG','BG',1,'0,1',NULL,9,1),
 (10,'GL-FN-001','集团办公室','DEPT',1,'0,1',NULL,10,1),
 (11,'GL-FN-002','人力资源部','DEPT',1,'0,1',NULL,11,1),
 (12,'GL-FN-003','财务部','DEPT',1,'0,1',NULL,12,1),
 (13,'GL-FN-004','法务部','DEPT',1,'0,1',NULL,13,1),
 (14,'GL-FN-005','商务部','DEPT',1,'0,1',NULL,14,1),
 (15,'GL-FN-006','信息技术部','DEPT',1,'0,1',NULL,15,1),
 (16,'GL-FN-007','质量管理部','DEPT',1,'0,1',NULL,16,1),
 (17,'GL-FN-008','战略投资部','DEPT',1,'0,1',NULL,17,1),
 (18,'GL-FN-009','品牌市场部','DEPT',1,'0,1',NULL,18,1),
 (19,'GL-FN-010','审计监察部','DEPT',1,'0,1',NULL,19,1),
 (20,'GL-RG-001','华北地区部','REGION',1,'0,1',NULL,20,1),
 (21,'GL-RG-002','华东地区部','REGION',1,'0,1',NULL,21,1),
 (22,'GL-RG-003','华南地区部','REGION',1,'0,1',NULL,22,1),
 (23,'GL-RG-004','华中地区部','REGION',1,'0,1',NULL,23,1),
 (24,'GL-RG-005','西南地区部','REGION',1,'0,1',NULL,24,1),
 (25,'GL-RG-006','西北地区部','REGION',1,'0,1',NULL,25,1),
 (26,'GL-BG-G-01','G端BG-交付一部','DEPT',2,'0,1,2',NULL,1,1),
 (27,'GL-BG-G-02','G端BG-交付二部','DEPT',2,'0,1,2',NULL,2,1),
 (28,'GL-BG-HH-01','和合BG-调解一部','DEPT',3,'0,1,3',NULL,1,1),
 (29,'GL-BG-HH-02','和合BG-调解二部','DEPT',3,'0,1,3',NULL,2,1),
 (30,'GL-BG-SL-01','丝路BG-业务一部','DEPT',4,'0,1,4',NULL,1,1);

-- ---------- 公司主体（与组织无直接关系） ----------
INSERT INTO sys_company (id, company_code, company_name, short_name, company_type, legal_person, credit_code, reg_address, office_address, tax_no, bank_name, bank_account, status) VALUES
 (1,'GL-CREDIT','国联政信有限公司','国联政信','HEAD','张建国','91110108MA01ABCD1X','北京市海淀区中关村南大街 1 号','北京市海淀区中关村南大街 1 号 A 座 12 层','91110108MA01ABCD1X','中国银行北京海淀支行','6217 8501 0000 1234 567',1),
 (2,'GL-TECH','国联政信科技（北京）有限公司','国联政信科技','SUB','李明','91110108MA02EFGH2Y','北京市朝阳区建国路 88 号','北京市朝阳区建国路 88 号 B 座 8 层','91110108MA02EFGH2Y','招商银行北京朝阳支行','6225 8801 0000 9876 543',1);

-- ---------- 角色（每个角色必绑一个组织） ----------
INSERT INTO sys_role (id, role_code, role_name, org_id, description, role_type, data_scope, can_be_project_member, can_be_approver, sort_no, status) VALUES
 (101,'ROLE_GROUP_ADMIN','集团-超级管理员',1,'平台最高权限，可配置全部系统参数','1','ALL',1,1,1,1),
 (102,'ROLE_GBG_PM','G端BG-项目经理',2,'G端BG项目负责人，可见本组织及下级数据',1,'ORG_AND_CHILD',1,1,2,1),
 (103,'ROLE_GBG_MEDIATOR','G端BG-调解员',26,'交付一部调解员，仅本组织数据',2,'ORG',1,1,3,1),
 (104,'ROLE_HHBG_PM','和合BG-项目经理',3,'和合BG项目负责人',2,'ORG_AND_CHILD',1,1,4,1),
 (105,'ROLE_REGION_OPS','地区部-运营',20,'华北地区部运营，仅本组织数据',2,'ORG',0,1,5,1),
 (106,'ROLE_BIZ_MANAGER','商务-商务经理',14,'商务部商务经理',2,'ORG',0,1,6,1),
 (107,'ROLE_VIEWER','只读访客',1,'仅本人数据，全字段高敏脱敏',1,'SELF',0,0,7,1);

-- ---------- 用户（口令 Admin@123） ----------
INSERT INTO sys_user (id, username, password, real_name, employee_no, mobile, email, gender, id_card, employee_type, employment_form, position_name, job_level, join_date, status, default_role_id, leader_id, first_login, remark) VALUES
 (1,'admin','$2b$10$/f941xLiBvjE1UB5XFQVr.n6e0w/cVa/S5TazxCwtCWB2rCFYQhrG','平台管理员','GL00001','13800138000','admin@guolian.com',1,'110108199001011234','FULLTIME','LABOR','系统管理员','P8','2020-01-06',1,101,NULL,0,'内置超管账号'),
 (2,'zhangwei','$2b$10$/f941xLiBvjE1UB5XFQVr.n6e0w/cVa/S5TazxCwtCWB2rCFYQhrG','张伟','GL00012','13800138012','zhangwei@guolian.com',1,'110108198805124321','FULLTIME','LABOR','高级项目经理','P7','2021-03-15',1,102,1,0,'跨 3 个组织共 5 个角色'),
 (3,'lina','$2b$10$/f941xLiBvjE1UB5XFQVr.n6e0w/cVa/S5TazxCwtCWB2rCFYQhrG','李娜','GL00023','13800138023','lina@guolian.com',2,'110108199203056789','FULLTIME','LABOR','项目经理','P6','2022-06-01',1,102,2,0,'G端BG 项目经理'),
 (4,'wangqiang','$2b$10$/f941xLiBvjE1UB5XFQVr.n6e0w/cVa/S5TazxCwtCWB2rCFYQhrG','王强','GL00031','13800138031','wangqiang@guolian.com',1,'110108198707159876','FULLTIME','LABOR','调解员','P5','2020-09-01',3,103,2,0,'已离职，只读态'),
 (5,'liuyang','$2b$10$/f941xLiBvjE1UB5XFQVr.n6e0w/cVa/S5TazxCwtCWB2rCFYQhrG','刘洋','GL00042','13800138042','liuyang@guolian.com',1,'110108199109234561','FULLTIME','LABOR','项目经理','P6','2023-02-20',1,104,2,0,'和合BG 项目经理'),
 (6,'chenjing','$2b$10$/f941xLiBvjE1UB5XFQVr.n6e0w/cVa/S5TazxCwtCWB2rCFYQhrG','陈静','GL00055','13800138055','chenjing@guolian.com',2,'110108199405067890','FULLTIME','LABOR','商务经理','P6','2022-11-07',1,106,1,0,'商务部商务经理'),
 (7,'zhaolei','$2b$10$/f941xLiBvjE1UB5XFQVr.n6e0w/cVa/S5TazxCwtCWB2rCFYQhrG','赵磊','GL00066','13800138066','zhaolei@guolian.com',1,'110108198906112345','PARTTIME','CONSULT','外聘顾问','P4','2024-04-01',2,105,1,0,'已停用演示账号');

-- ---------- 用户角色 ----------
INSERT INTO sys_user_role (id, user_id, role_id, is_default, position_name, job_level) VALUES
 (1001,1,101,1,'系统管理员','P8'),
 (1002,2,102,1,'高级项目经理','P7'),
 (1003,2,103,0,'调解员','P5'),
 (1004,2,104,0,'项目经理','P6'),
 (1005,2,105,0,'运营','P5'),
 (1006,2,106,0,'商务经理','P6'),
 (1007,3,102,1,'项目经理','P6'),
 (1008,4,103,1,'调解员','P5'),
 (1009,5,104,1,'项目经理','P6'),
 (1010,6,106,1,'商务经理','P6'),
 (1011,7,105,1,'运营','P4');

-- ---------- 用户公司主体 ----------
INSERT INTO sys_user_company (id, user_id, company_id, relation_type, is_primary) VALUES
 (2001,1,1,1,1),(2002,1,1,2,1),(2003,1,1,4,1),
 (2004,2,1,1,1),(2005,2,1,2,1),(2006,2,2,4,0),
 (2007,3,2,1,1),(2008,3,2,2,1),(2009,3,1,3,0),
 (2010,4,1,1,1),(2011,5,2,1,1),(2012,5,1,2,0),(2013,6,1,1,1);

-- ---------- 菜单：4 个一级 + 全部二级 + 系统管理按钮 ----------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_no, visible, status) VALUES
 (100,0,'CRM',1,'/crm','Layout',NULL,'users',1,1,1),
 (1101,100,'客户管理',2,'/crm/customer','placeholder/index','crm:customer:list','address-book',1,1,1),
 (1102,100,'公共客户池',2,'/crm/pool','placeholder/index','crm:pool:list','users-group',2,1,1),
 (1103,100,'线索与商机',2,'/crm/lead','placeholder/index','crm:lead:list','bulb',3,1,1),
 (1104,100,'销售跟进',2,'/crm/follow','placeholder/index','crm:follow:list','progress',4,1,1),
 (1105,100,'法务合同',2,'/crm/contract','placeholder/index','crm:contract:list','file-text',5,1,1),
 (1106,100,'协议管理',2,'/crm/agreement','placeholder/index','crm:agreement:list','file-certificate',6,1,1),
 (1107,100,'投标管理',2,'/crm/bid','placeholder/index','crm:bid:list','gavel',7,1,1),
 (1108,100,'回款管理',2,'/crm/payment','placeholder/index','crm:payment:list','cash',8,1,1),
 (1109,100,'流失客户',2,'/crm/churn','placeholder/index','crm:churn:list','user-off',9,1,1),
 (101,0,'项目管理',1,'/project','Layout',NULL,'briefcase',2,1,1),
 (1201,101,'产品管理',2,'/project/product','project/product/index','project:product:list','box',1,1,1),
 (1202,101,'项目管理',2,'/project/list','project/list/index','project:project:list','list',2,1,1),
 (1203,101,'项目驾驶舱',2,'/project/cockpit','project/cockpit/index','project:cockpit:view','dashboard',3,1,1),
 (1204,101,'（预留）立项看板',2,'/project/board','placeholder/index','project:task:list','layout-kanban',4,1,0),
 (1205,101,'（预留）项目评审',2,'/project/review','placeholder/index','project:review:list','checks',5,1,0),
 (1206,101,'（预留）项目验收',2,'/project/acceptance','placeholder/index','project:acceptance:list','circle-check',6,1,0),
 (1207,101,'（预留）项目复盘',2,'/project/retro','placeholder/index','project:retro:list','history',7,1,0),
 (102,0,'驾驶舱管理',1,'/cockpit','Layout',NULL,'chart-dots',3,1,1),
 (1301,102,'经营总览',2,'/cockpit/overview','placeholder/index','cockpit:overview:view','dashboard',1,1,1),
 (1302,102,'销售看板',2,'/cockpit/sales','placeholder/index','cockpit:sales:view','chart-bar',2,1,1),
 (1303,102,'地区部经营看板',2,'/cockpit/region','placeholder/index','cockpit:region:view','map-2',3,1,1),
 (1304,102,'商务部看板',2,'/cockpit/business','placeholder/index','cockpit:business:view','chart-pie',4,1,1),
 (1305,102,'人事看板',2,'/cockpit/hr','placeholder/index','cockpit:hr:view','users',5,1,1),
 (103,0,'系统管理',1,'/system','Layout',NULL,'settings',4,1,1),
 (1401,103,'用户管理',2,'/system/user','system/user/index','system:user:list','user',1,1,1),
 (1402,103,'账号交接',2,'/system/transfer','system/transfer/index','system:transfer:list','swap',2,1,1),
 (1403,103,'组织架构',2,'/system/org','system/org/index','system:org:list','sitemap',3,1,1),
 (1404,103,'公司主体',2,'/system/company','system/company/index','system:company:list','building',4,1,1),
 (1405,103,'角色管理',2,'/system/role','system/role/index','system:role:list','shield-lock',5,1,1),
 (1406,103,'菜单管理',2,'/system/menu','system/menu/index','system:menu:list','list-details',6,1,1),
 (1407,103,'审批流程',2,'/system/flow','system/flow/index','system:flow:list','git-branch',7,1,1),
 (1408,103,'数据权限',2,'/system/data-scope','system/data-scope/index','system:role:data-scope','database',8,1,1),
 (1409,103,'字段脱敏',2,'/system/sensitive','system/sensitive/index','system:sensitive:list','eye-off',9,1,1),
 (1410,103,'字典管理',2,'/system/dict','system/dict/index','system:dict:list','book',10,1,1),
 (1411,103,'参数配置',2,'/system/config','system/config/index','system:config:list','adjustments',11,1,1),
 (1412,103,'通知公告',2,'/system/notice','system/notice/index','system:notice:list','bell',12,1,1),
 (1413,103,'操作日志',2,'/system/oper-log','system/log/oper/index','system:log:oper','list',13,1,1),
 (1414,103,'登录日志',2,'/system/login-log','system/log/login/index','system:log:login','login',14,1,1),
 (1415,103,'在线用户',2,'/system/online','system/online/index','system:online:list','users-group',15,1,1),
 (1416,103,'附件管理',2,'/system/file','system/file/index','system:file:list','paperclip',16,1,1),
 (9001,1401,'新增用户',3,NULL,NULL,'system:user:add',NULL,1,1,1),
 (9002,1401,'修改用户',3,NULL,NULL,'system:user:edit',NULL,2,1,1),
 (9003,1401,'删除用户',3,NULL,NULL,'system:user:remove',NULL,3,1,1),
 (9004,1401,'导出用户',3,NULL,NULL,'system:user:export',NULL,4,1,1),
 (9005,1401,'导入用户',3,NULL,NULL,'system:user:import',NULL,5,1,1),
 (9006,1401,'重置密码',3,NULL,NULL,'system:user:reset-pwd',NULL,6,1,1),
 (9007,1401,'分配角色',3,NULL,NULL,'system:user:assign-role',NULL,7,1,1),
 (9008,1401,'状态变更',3,NULL,NULL,'system:user:status',NULL,8,1,1),
 (9009,1401,'模拟查看视角',3,NULL,NULL,'system:debug:impersonate',NULL,9,1,1),
 (9010,1402,'新增交接单',3,NULL,NULL,'system:transfer:add',NULL,1,1,1),
 (9011,1402,'提交审批',3,NULL,NULL,'system:transfer:submit',NULL,2,1,1),
 (9012,1402,'执行交接',3,NULL,NULL,'system:transfer:execute',NULL,3,1,1),
 (9013,1402,'撤销交接',3,NULL,NULL,'system:transfer:revoke',NULL,4,1,1),
 (9020,1405,'新增角色',3,NULL,NULL,'system:role:add',NULL,1,1,1),
 (9021,1405,'修改角色',3,NULL,NULL,'system:role:edit',NULL,2,1,1),
 (9022,1405,'删除角色',3,NULL,NULL,'system:role:remove',NULL,3,1,1),
 (9023,1405,'角色授权',3,NULL,NULL,'system:role:auth',NULL,4,1,1),
 (9030,1403,'新增组织',3,NULL,NULL,'system:org:add',NULL,1,1,1),
 (9031,1403,'修改组织',3,NULL,NULL,'system:org:edit',NULL,2,1,1),
 (9032,1403,'删除组织',3,NULL,NULL,'system:org:remove',NULL,3,1,1),
 (9040,1404,'新增主体',3,NULL,NULL,'system:company:add',NULL,1,1,1),
 (9041,1404,'修改主体',3,NULL,NULL,'system:company:edit',NULL,2,1,1),
 (9042,1404,'删除主体',3,NULL,NULL,'system:company:remove',NULL,3,1,1),
 (9050,1407,'新增流程',3,NULL,NULL,'system:flow:add',NULL,1,1,1),
 (9051,1407,'发布流程',3,NULL,NULL,'system:flow:publish',NULL,2,1,1),
 (9052,1407,'修改流程',3,NULL,NULL,'system:flow:edit',NULL,3,1,1),
 (9053,1407,'删除流程',3,NULL,NULL,'system:flow:remove',NULL,4,1,1),
 (9060,1409,'新增字段组',3,NULL,NULL,'system:sensitive:add',NULL,1,1,1),
 (9061,1409,'修改字段组',3,NULL,NULL,'system:sensitive:edit',NULL,2,1,1),
 (9062,1409,'删除字段组',3,NULL,NULL,'system:sensitive:remove',NULL,3,1,1),
 (9070,1410,'新增字典',3,NULL,NULL,'system:dict:add',NULL,1,1,1),
 (9071,1410,'修改字典',3,NULL,NULL,'system:dict:edit',NULL,2,1,1),
 (9072,1410,'删除字典',3,NULL,NULL,'system:dict:remove',NULL,3,1,1),
 (9080,1411,'新增参数',3,NULL,NULL,'system:config:add',NULL,1,1,1),
 (9081,1411,'修改参数',3,NULL,NULL,'system:config:edit',NULL,2,1,1),
 (9082,1411,'删除参数',3,NULL,NULL,'system:config:remove',NULL,3,1,1),
 (9090,1412,'新增公告',3,NULL,NULL,'system:notice:add',NULL,1,1,1),
 (9091,1412,'修改公告',3,NULL,NULL,'system:notice:edit',NULL,2,1,1),
 (9092,1412,'删除公告',3,NULL,NULL,'system:notice:remove',NULL,3,1,1),
 (9100,1406,'新增菜单',3,NULL,NULL,'system:menu:add',NULL,1,1,1),
 (9101,1406,'修改菜单',3,NULL,NULL,'system:menu:edit',NULL,2,1,1),
 (9102,1406,'删除菜单',3,NULL,NULL,'system:menu:remove',NULL,3,1,1);

-- ---------- 角色菜单授权 ----------
-- 超管：全部
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 100000 + m.id, 101, m.id FROM sys_menu m;
-- G端BG 项目经理：CRM + 项目管理 + 用户管理（只读）
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
 (201100,102,100),(201101,102,1101),(201102,102,1102),(201103,102,1103),(201104,102,1104),
 (201105,102,1105),(201106,102,1106),(201107,102,1107),(201108,102,1108),(201109,102,1109),
 (201120,102,101),(201121,102,1201),(201122,102,1202),(201123,102,1203),(201124,102,1204),
 (201125,102,1205),(201126,102,1206),(201127,102,1207),
 (201140,102,103),(201141,102,1401);
-- G端BG 调解员：CRM 客户/池/线索/跟进
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
 (201200,103,100),(201201,103,1101),(201202,103,1102),(201203,103,1103),(201204,103,1104),
 (201205,103,103),(201206,103,1401);
-- 和合BG 项目经理：项目管理
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
 (201300,104,101),(201301,104,1201),(201302,104,1202),(201303,104,1203);
-- 地区部运营：驾驶舱
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
 (201400,105,102),(201401,105,1301),(201402,105,1302),(201403,105,1303);
-- 商务经理：CRM 合同/协议/投标/回款
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
 (201500,106,100),(201501,106,1105),(201502,106,1106),(201503,106,1107),(201504,106,1108);
-- 只读访客：用户管理列表
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
 (201600,107,103),(201601,107,1401);

-- ---------- 脱敏：字段组 ----------
INSERT INTO sys_sensitive_group (id, group_code, group_name, default_level, risk_level, sort_no, status, remark) VALUES
 (1,'MOBILE','手机号','MASKED','MEDIUM',1,1,'用户联系方式'),
 (2,'ID_CARD','身份证号','MASKED','HIGH',2,1,'高风险，禁止配置明文'),
 (3,'BANK_ACCOUNT','银行账号','HIDDEN','HIGH',3,1,'公司主体收款账号'),
 (4,'EMAIL','邮箱','MASKED','LOW',4,1,'用户邮箱'),
 (5,'ADDRESS','地址','MASKED','LOW',5,1,'公司注册与办公地址'),
 (6,'SALARY','薪资','HIDDEN','HIGH',6,1,'预留字段组，薪酬模块接入后启用');

-- ---------- 脱敏：字段注册（字段名限白名单） ----------
INSERT INTO sys_sensitive_field (id, group_code, biz_object, field_name, field_label, mask_rule, mask_param, allow_plain, status) VALUES
 (1,'MOBILE','user','mobile','手机号','KEEP_HEAD_TAIL','3,4',0,1),
 (2,'ID_CARD','user','idCard','身份证号','KEEP_TAIL','4',0,1),
 (3,'BANK_ACCOUNT','company','bankAccount','银行账号','KEEP_TAIL','4',0,1),
 (4,'EMAIL','user','email','邮箱','EMAIL_MASK',NULL,1,1),
 (5,'ADDRESS','company','regAddress','注册地址','ADDRESS_MASK',NULL,1,1),
 (6,'ADDRESS','company','officeAddress','办公地址','ADDRESS_MASK',NULL,1,1),
 (7,'SALARY','user','salaryAmount','月薪','AMOUNT_RANGE','1000',0,1);

-- ---------- 脱敏：角色档位 ----------
INSERT INTO sys_role_field_mask (id, role_id, group_code, mask_level) VALUES
 (1,101,'MOBILE','PLAIN'),(2,101,'ID_CARD','MASKED'),(3,101,'BANK_ACCOUNT','HIDDEN'),
 (4,101,'EMAIL','PLAIN'),(5,101,'ADDRESS','PLAIN'),(6,101,'SALARY','HIDDEN'),
 (7,102,'MOBILE','MASKED'),(8,102,'ID_CARD','MASKED'),(9,102,'BANK_ACCOUNT','HIDDEN'),
 (10,102,'EMAIL','MASKED'),(11,102,'ADDRESS','MASKED'),(12,102,'SALARY','HIDDEN'),
 (13,103,'MOBILE','MASKED'),(14,103,'ID_CARD','HIDDEN'),(15,103,'BANK_ACCOUNT','HIDDEN'),
 (16,103,'EMAIL','MASKED'),(17,103,'ADDRESS','HIDDEN'),(18,103,'SALARY','HIDDEN'),
 (19,107,'MOBILE','MASKED'),(20,107,'ID_CARD','HIDDEN'),(21,107,'BANK_ACCOUNT','HIDDEN'),
 (22,107,'EMAIL','HIDDEN'),(23,107,'ADDRESS','HIDDEN'),(24,107,'SALARY','HIDDEN');

-- ---------- 字典 ----------
INSERT INTO sys_dict_type (id, dict_type, dict_name, status, remark) VALUES
 (1,'user_status','用户状态',1,NULL),
 (2,'gender','性别',1,NULL),
 (3,'employee_type','员工类型',1,NULL),
 (4,'employment_form','用工形式',1,NULL),
 (5,'org_type','组织类型',1,NULL),
 (6,'company_type','主体类型',1,NULL),
 (7,'data_scope','数据范围类型',1,NULL),
 (8,'mask_level','脱敏档位',1,NULL),
 (9,'mask_rule','脱敏算法',1,NULL),
 (10,'flow_status','审批状态',1,NULL),
 (11,'exec_status','交接执行状态',1,NULL),
 (12,'node_type','审批节点类型',1,NULL),
 (13,'approve_action','审批动作',1,NULL),
 (14,'notice_type','通知类型',1,NULL),
 (15,'assign_mode','交接分配模式',1,NULL);

INSERT INTO sys_dict_item (id, dict_type, dict_label, dict_value, sort_no, status) VALUES
 (101,'user_status','正常','1',1,1),(102,'user_status','停用','2',2,1),(103,'user_status','离职','3',3,1),(104,'user_status','锁定','4',4,1),
 (201,'gender','男','1',1,1),(202,'gender','女','2',2,1),(203,'gender','未知','0',3,1),
 (301,'employee_type','全职','FULLTIME',1,1),(302,'employee_type','兼职','PARTTIME',2,1),(303,'employee_type','退休返聘','REHIRE',3,1),(304,'employee_type','实习','INTERN',4,1),(305,'employee_type','外包','OUTSOURCE',5,1),
 (401,'employment_form','劳动合同','LABOR',1,1),(402,'employment_form','劳务派遣','DISPATCH',2,1),(403,'employment_form','实习协议','INTERN_AGREE',3,1),(404,'employment_form','顾问协议','CONSULT',4,1),
 (501,'org_type','集团','GROUP',1,1),(502,'org_type','事业群','BG',2,1),(503,'org_type','职能部门','DEPT',3,1),(504,'org_type','地区部','REGION',4,1),
 (601,'company_type','总公司','HEAD',1,1),(602,'company_type','分公司','BRANCH',2,1),(603,'company_type','子公司','SUB',3,1),
 (701,'data_scope','全部数据','ALL',1,1),(702,'data_scope','本组织及下级','ORG_AND_CHILD',2,1),(703,'data_scope','本组织','ORG',3,1),
 (704,'data_scope','自定义组织','ORG_CUSTOM',4,1),(705,'data_scope','自定义公司主体','COMPANY_CUSTOM',5,1),(706,'data_scope','仅本人','SELF',6,1),(707,'data_scope','我参与的项目','PROJECT_MEMBER',7,1),
 (801,'mask_level','明文可见','PLAIN',1,1),(802,'mask_level','脱敏','MASKED',2,1),(803,'mask_level','隐藏','HIDDEN',3,1),
 (901,'mask_rule','保留首尾','KEEP_HEAD_TAIL',1,1),(902,'mask_rule','仅保留末位','KEEP_TAIL',2,1),(903,'mask_rule','邮箱脱敏','EMAIL_MASK',3,1),
 (904,'mask_rule','保留省市','ADDRESS_MASK',4,1),(905,'mask_rule','全掩码','FULL_MASK',5,1),(906,'mask_rule','区间显示','AMOUNT_RANGE',6,1),
 (1001,'flow_status','草稿','DRAFT',1,1),(1002,'flow_status','审批中','RUNNING',2,1),(1003,'flow_status','已通过','APPROVED',3,1),
 (1004,'flow_status','已驳回','REJECTED',4,1),(1005,'flow_status','已撤回','WITHDRAWN',5,1),
 (1101,'exec_status','未执行','NOT_EXECUTED',1,1),(1102,'exec_status','执行中','EXECUTING',2,1),(1103,'exec_status','已完成','DONE',3,1),
 (1104,'exec_status','部分失败','PARTIAL_FAILED',4,1),(1105,'exec_status','已撤销','REVOKED',5,1),
 (1201,'node_type','单人审批','SINGLE',1,1),(1202,'node_type','候选审批','CANDIDATE',2,1),(1203,'node_type','会签','ALL_SIGN',3,1),
 (1204,'node_type','或签','OR_SIGN',4,1),(1205,'node_type','条件分支','CONDITION',5,1),(1206,'node_type','抄送','CC',6,1),
 (1301,'approve_action','同意','APPROVE',1,1),(1302,'approve_action','驳回','REJECT',2,1),(1303,'approve_action','加签','ADD_SIGN',3,1),
 (1304,'approve_action','转办','TRANSFER',4,1),(1305,'approve_action','撤回','WITHDRAW',5,1),
 (1401,'notice_type','通知','1',1,1),(1402,'notice_type','公告','2',2,1),(1403,'notice_type','待办','3',3,1),
 (1501,'assign_mode','单接收人','1',1,1),(1502,'assign_mode','按角色分派','2',2,1);

-- ---------- 参数配置 ----------
INSERT INTO sys_config (id, config_key, config_value, config_name, config_type, builtin, remark) VALUES
 (1,'user.init.password','Init@123456','用户初始密码','STRING',1,'新增用户与重置密码默认值'),
 (2,'jwt.access.minutes','120','Token 有效期（分钟）','NUMBER',1,NULL),
 (3,'file.upload.max.mb','20','附件上传上限（MB）','NUMBER',1,NULL),
 (4,'login.fail.lock.count','5','登录失败锁定次数','NUMBER',1,NULL),
 (5,'login.fail.lock.minutes','10','登录失败锁定时长（分钟）','NUMBER',1,NULL),
 (6,'transfer.revoke.days','7','交接可撤销天数','NUMBER',1,'默认 7 天'),
 (7,'role.max.per.user','5','单用户角色上限','NUMBER',1,NULL),
 (8,'transfer.revoke.need.approval','false','交接撤销是否需审批','BOOLEAN',1,NULL),
 (9,'debug.impersonate.enabled','true','调试模拟开关','BOOLEAN',0,'PROD 默认关闭'),
 (10,'transfer.max.items.per.type','5000','单类交接上限','NUMBER',1,'超限分批执行'),
 (11,'flow.default.timeout.hours','24','审批默认超时（小时）','NUMBER',1,NULL);

-- ---------- 通知公告 + 站内待办 ----------
INSERT INTO sys_notice (id, title, notice_type, content, receiver_scope, receiver_id, pinned, status, publish_time, todo_status, biz_type, biz_id) VALUES
 (1,'平台底座 v1.0 上线公告',2,'<p>项目管理平台底座已完成部署，包含账号交接审批、数据权限与字段脱敏能力。业务模块（CRM / 项目管理 / 驾驶舱）将按独立方案接入。</p>', 'ALL', NULL, 1, 1, '2026-10-08 09:00:00', 0, NULL, NULL),
 (2,'请完成 10 月销售例会材料准备',1,'<p>请各 BG 项目经理于本周五前提交例会材料。</p>', 'ALL', NULL, 0, 1, '2026-10-20 10:30:00', 0, NULL, NULL),
 (3,'【待办】账号交接待接收',3,'<p>李娜发起的账号交接已审批通过，请接收并确认。</p>', 'USER', 2, 0, 1, '2026-10-25 14:00:00', 0, 'ACCOUNT_TRANSFER', 1);

-- ---------- 审批模板 ----------
INSERT INTO sys_flow_def (id, flow_code, flow_name, biz_type, org_scope, process_definition_key, deployed_version, status, form_code, node_config) VALUES
 (1,'ACCOUNT_TRANSFER','账号交接审批','ACCOUNT_TRANSFER',NULL,'ACCOUNT_TRANSFER',1,1,'ACCOUNT_TRANSFER',
  '{"nodes":[{"key":"start","name":"发起人","type":"START"},{"key":"leader","name":"直属上级","type":"SINGLE","approverType":"LEADER","level":1},{"key":"deptLeader","name":"部门负责人","type":"SINGLE","approverType":"ORG_LEADER"},{"key":"hr","name":"人力资源","type":"CANDIDATE","approverType":"ROLE","roleCode":"ROLE_GROUP_ADMIN"},{"key":"end","name":"结束","type":"END"}]}'),
 (2,'_DEMO_CONDITION','引擎能力自证（条件分支+会签）','DEMO',NULL,'_DEMO_CONDITION',1,1,NULL,
  '{"nodes":[{"key":"start","name":"发起人","type":"START"},{"key":"sign3","name":"三人会签","type":"ALL_SIGN","approverType":"ROLE","roleCode":"ROLE_GBG_PM"},{"key":"amountGateway","name":"金额判断","type":"CONDITION","expression":"amount > 1000000"},{"key":"finance","name":"财务审批","type":"SINGLE","approverType":"ROLE","roleCode":"ROLE_GROUP_ADMIN"},{"key":"end","name":"结束","type":"END"}]}');

-- ---------- 账号交接演示数据 ----------
INSERT INTO sys_account_transfer (id, transfer_no, from_user_id, to_user_ids, assign_mode, scope, remark, effective_time, flow_status, exec_status, flow_instance_id, item_count, fail_count, revoke_deadline) VALUES
 (1,'HO-20261025-0001',3,'[2]',1,'["NOTICE_TODO","FLOW_TASK"]','李娜转岗，名下待办与审批任务移交张伟','2026-10-25 15:00:00','APPROVED','DONE',NULL,8,0,'2026-11-01 15:00:00'),
 (2,'HO-20261027-0002',5,'[2]',1,'["NOTICE_TODO"]','临时交接站内待办','2026-10-27 09:00:00','RUNNING','NOT_EXECUTED',NULL,0,0,NULL);

INSERT INTO sys_account_transfer_item (id, transfer_id, biz_type, biz_id, biz_name, before_owner_id, after_owner_id, result, fail_reason) VALUES
 (1,1,'NOTICE_TODO',901,'待办：客户资料复核',3,2,'SUCCESS',NULL),
 (2,1,'NOTICE_TODO',902,'待办：合同用印申请',3,2,'SUCCESS',NULL),
 (3,1,'NOTICE_TODO',903,'待办：周报提交',3,2,'SUCCESS',NULL),
 (4,1,'NOTICE_TODO',904,'待办：项目例会纪要',3,2,'SUCCESS',NULL),
 (5,1,'FLOW_TASK',905,'审批：G端BG 项目立项',3,2,'SUCCESS',NULL),
 (6,1,'FLOW_TASK',906,'审批：合同评审',3,2,'SUCCESS',NULL),
 (7,1,'FLOW_TASK',907,'审批：费用报销',3,2,'SUCCESS',NULL),
 (8,1,'FLOW_TASK',908,'审批：用印申请',3,2,'SUCCESS',NULL);

INSERT INTO sys_flow_biz (id, biz_type, biz_id, biz_no, biz_title, process_instance_id, flow_code, flow_version, initiator_id, initiator_org_id, status, current_node_name, start_time, end_time, detail_route) VALUES
 (1,'ACCOUNT_TRANSFER',1,'HO-20261025-0001','账号交接：李娜 → 张伟',NULL,'ACCOUNT_TRANSFER',1,3,2,'APPROVED','结束','2026-10-25 14:10:00','2026-10-25 15:00:00','/system/transfer?id=1');

-- ---------- 定时任务（P2 骨架） ----------
INSERT INTO sys_job (id, job_name, job_code, cron, status, remark) VALUES
 (1,'交接单定时生效','TRANSFER_EFFECTIVE','0 0/5 * * * ?',0,'扫描到期交接单并触发执行'),
 (2,'审批超时提醒','FLOW_TIMEOUT_REMIND','0 0 * * * ?',0,'按节点配置推送超时提醒');

-- ---------- 组织负责人（Flowable「组织负责人」节点的取值来源） ----------
UPDATE sys_org SET leader_id = 1 WHERE id = 1;
UPDATE sys_org SET leader_id = 2 WHERE id IN (2, 26, 27);
UPDATE sys_org SET leader_id = 5 WHERE id IN (3, 28, 29);
UPDATE sys_org SET leader_id = 6 WHERE id = 14;
UPDATE sys_org SET leader_id = 7 WHERE id = 20;
