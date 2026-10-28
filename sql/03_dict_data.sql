-- 字典与参数数据（MySQL 8.0）

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

