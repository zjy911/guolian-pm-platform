/**
 * 演示模式（GitHub Pages 静态部署专用）：
 * - 通过 VITE_DEMO=1 构建（vite build --mode demo）启用
 * - 在 request.ts 的 get/post/put/del 入口短路，按 URL 路由返回模拟数据
 * - 数据为一次性生成的确定性数据集，与后端 crm_data.sql 的字典/取值保持一致
 * - 任何写操作返回成功但不持久化（刷新即还原）
 */
import type {
  AuthResult,
  DictItem,
  MenuNode,
  PageResult,
  Profile,
  RoleOption
} from './types'
import type {
  CrmAccount,
  CrmActivity,
  CrmAgreement,
  CrmBid,
  CrmClaimVO,
  CrmContact,
  CrmContract,
  CrmCompetitor,
  CrmLead,
  CrmOpportunity,
  FunnelStage,
  OpeningSource
} from './crm'

export const DEMO = import.meta.env.VITE_DEMO === '1'

// ==================== 基础数据 ====================

interface DemoUser {
  id: number
  username: string
  realName: string
  orgId: number
  orgName: string
  roleId: number
  roleCode: string
  roleName: string
}

const USERS: DemoUser[] = [
  { id: 1, username: 'admin', realName: '王建国', orgId: 1, orgName: '总公司', roleId: 101, roleCode: 'SUPER_ADMIN', roleName: '超级管理员' },
  { id: 2, username: 'lina', realName: '李娜', orgId: 5, orgName: 'G端BG', roleId: 102, roleCode: 'G_PM', roleName: 'G端BG项目经理' },
  { id: 3, username: 'zhangwei', realName: '张伟', orgId: 2, orgName: '华北地区部', roleId: 103, roleCode: 'BIZ_SPECIALIST', roleName: '商务专员' },
  { id: 4, username: 'liuyang', realName: '刘洋', orgId: 3, orgName: '华东地区部', roleId: 103, roleCode: 'BIZ_SPECIALIST', roleName: '商务专员' },
  { id: 5, username: 'chenjing', realName: '陈静', orgId: 4, orgName: '商务部', roleId: 104, roleCode: 'BIZ_DIRECTOR', roleName: '商务总监' },
  { id: 6, username: 'zhaolei', realName: '赵磊', orgId: 2, orgName: '华北地区部', roleId: 103, roleCode: 'BIZ_SPECIALIST', roleName: '商务专员' }
]

const userById = (id?: number | null) => USERS.find((u) => u.id === id) ?? USERS[0]
const ownerOf = (id?: number | null) => {
  const u = userById(id)
  return { ownerUserId: u.id, ownerOrgId: u.orgId, ownerUserName: u.realName }
}

const COMPANIES = [
  { id: 1, companyCode: 'GL-C001', companyName: '国联政信股份有限公司', shortName: '国联政信总部' },
  { id: 2, companyCode: 'GL-C002', companyName: '国联政信（上海）有限公司', shortName: '上海分公司' }
]

const ORG_TREE = [
  {
    id: 1, orgCode: 'HQ', orgName: '总公司', orgType: 'COMPANY', parentId: 0, sortNo: 1, status: 1,
    roleCount: 2, userCount: 2, children: [
      {
        id: 5, orgCode: 'GBG', orgName: 'G端BG', orgType: 'BUSINESS_GROUP', parentId: 1, sortNo: 2, status: 1,
        roleCount: 1, userCount: 1, children: [], roles: [], users: [{ id: 2, realName: '李娜', username: 'lina' }]
      },
      {
        id: 2, orgCode: 'NORTH', orgName: '华北地区部', orgType: 'REGION', parentId: 1, sortNo: 3, status: 1,
        roleCount: 1, userCount: 2, children: [], roles: [], users: [{ id: 3, realName: '张伟', username: 'zhangwei' }, { id: 6, realName: '赵磊', username: 'zhaolei' }]
      },
      {
        id: 3, orgCode: 'EAST', orgName: '华东地区部', orgType: 'REGION', parentId: 1, sortNo: 4, status: 1,
        roleCount: 1, userCount: 1, children: [], roles: [], users: [{ id: 4, realName: '刘洋', username: 'liuyang' }]
      },
      {
        id: 4, orgCode: 'BIZ', orgName: '商务部', orgType: 'DEPARTMENT', parentId: 1, sortNo: 5, status: 1,
        roleCount: 1, userCount: 1, children: [], roles: [], users: [{ id: 5, realName: '陈静', username: 'chenjing' }]
      }
    ],
    roles: [
      { id: 101, roleCode: 'SUPER_ADMIN', roleName: '超级管理员', userCount: 1, dataScope: 'ALL' }
    ],
    users: [{ id: 1, realName: '王建国', username: 'admin' }]
  }
]

const ROLES = [
  { id: 101, roleCode: 'SUPER_ADMIN', roleName: '超级管理员', orgId: 1, orgName: '总公司', dataScope: 'ALL', dataScopeLabel: '全部数据', roleType: 1, userCount: 1, menuIds: [1] },
  { id: 102, roleCode: 'G_PM', roleName: 'G端BG项目经理', orgId: 5, orgName: 'G端BG', dataScope: 'ORG', dataScopeLabel: '本组织', roleType: 2, userCount: 1, menuIds: [] },
  { id: 103, roleCode: 'BIZ_SPECIALIST', roleName: '商务专员', orgId: 2, orgName: '华北地区部', dataScope: 'SELF', dataScopeLabel: '仅本人', roleType: 2, userCount: 3, menuIds: [] },
  { id: 104, roleCode: 'BIZ_DIRECTOR', roleName: '商务总监', orgId: 4, orgName: '商务部', dataScope: 'ORG_AND_CHILD', dataScopeLabel: '本组织及以下', roleType: 2, userCount: 1, menuIds: [] }
]

// ==================== CRM 数据 ====================

const INDUSTRIES = ['BANK', 'GOV', 'INSURANCE', 'SECURITIES', 'ENTERPRISE', 'JUDICIAL', 'LEASING', 'FINTECH']
const PROVINCES = ['北京市', '上海市', '广东省', '浙江省', '江苏省', '四川省']
const ACCOUNT_NAMES = [
  '中国工商银行股份有限公司北京市分行', '中国人民银行营业管理部', '中国平安财产保险股份有限公司', '国泰君安证券股份有限公司',
  '华为技术有限公司', '北京市第一中级人民法院', '平安国际融资租赁有限公司', '京东金融科技控股有限公司',
  '中国建设银行股份有限公司上海市分行', '上海市浦东新区人民政府', '中国太平洋人寿保险股份有限公司', '招商证券股份有限公司',
  '腾讯科技（深圳）有限公司', '杭州市中级人民法院', '光大金融租赁股份有限公司', '蚂蚁科技集团股份有限公司',
  '中国农业银行股份有限公司深圳市分行', '广州市人民政府办公厅', '中国人民财产保险股份有限公司', '中信证券股份有限公司'
]

let seed = 20261028
function rnd(): number {
  seed = (seed * 9301 + 49297) % 233280
  return seed / 233280
}
const pick = <T>(arr: T[], i: number): T => arr[i % arr.length]
const dayStr = (offset: number): string => {
  const d = new Date(Date.UTC(2026, 9, 28) + offset * 86400000)
  return d.toISOString().slice(0, 10)
}

export const accounts: CrmAccount[] = ACCOUNT_NAMES.map((companyName, i) => {
  const id = 2001 + i
  const poolStatus = i < 12 ? 'PRIVATE' : i < 17 ? 'POOL' : 'CHURNED'
  return {
    id,
    companyName,
    creditCode: `91110000MA0${String(100000 + i * 7).slice(0, 6)}X${i}`,
    industry: pick(INDUSTRIES, i),
    accountNature: i % 4 === 3 ? 'TO_G' : 'TO_B',
    accountType: pick(['NORMAL', 'KEY', 'STRATEGIC'], i),
    phone: `010-6${String(5000000 + i * 137).slice(0, 7)}`,
    employeeScale: pick(['50-100人', '100-500人', '500-1000人', '1000人以上'], i),
    annualRevenue: [5.2, 18.6, 42.3, 8.9, 120.5, 66.1][i % 6] * 10000,
    ...ownerOf(i < 12 ? pick([2, 3, 4, 6], i) : null),
    province: pick(PROVINCES, i),
    city: '市区',
    address: `${pick(PROVINCES, i).replace(/[省市]$/, '')}市金融街${i + 1}号`,
    poolStatus,
    poolInTime: poolStatus !== 'PRIVATE' ? dayStr(-40 - i) : undefined,
    poolReason: poolStatus === 'POOL' ? '无活动超期自动回收' : poolStatus === 'CHURNED' ? '客户机构改革，项目终止' : undefined,
    churnReason: poolStatus === 'CHURNED' ? '预算削减，项目无限期搁置' : undefined,
    isProspect: 0,
    lastActivityTime: poolStatus === 'PRIVATE' ? dayStr(-3 - i) : undefined,
    remark: '',
    createdTime: dayStr(-200 + i * 3)
  }
})

const CONTACT_NAMES = ['陈明', '周红', '吴刚', '郑爽', '冯军', '褚燕', '卫东', '蒋丽', '沈涛', '韩雪', '杨帆', '朱琳', '秦峰', '许倩', '何超', '吕婷', '施磊', '张凯', '孔薇', '曹阳', '严华', '华鑫', '金晶', '魏晨', '陶勇', '姜怡', '戚悦', '谢宇', '邹娜', '喻明']
export const contacts: CrmContact[] = CONTACT_NAMES.map((name, i) => ({
  id: 3001 + i,
  name,
  accountId: accounts[i % accounts.length].id,
  mobile: `139${String(10000000 + i * 111111).slice(0, 8)}`,
  email: `${name.toLowerCase().replace(/[^a-z]/g, 'x') || 'user'}${i}@example.com`,
  positionName: pick(['总经理', '信息科技部负责人', '采购部经理', '风控总监', '办公室副主任'], i),
  department: pick(['信息科技部', '办公室', '采购中心', '风险合规部'], i),
  ...ownerOf(accounts[i % accounts.length].ownerUserId),
  lastActivityTime: dayStr(-5 - i),
  remark: '',
  createdTime: dayStr(-180 + i * 2)
}))

const LEAD_NAMES = ['李慧', '孙浩', '钱进', '孟子涵', '郑小龙', '王芳', '马腾', '朱丽叶', '胡军', '林依晨', '高翔', '罗成', '梁咏', '宋佳', '唐磊']
export const leads: CrmLead[] = LEAD_NAMES.map((name, i) => {
  const status = i < 8 ? 'FOLLOWING' : i < 11 ? 'NEW' : i < 13 ? 'CONVERTED' : 'INVALID'
  return {
    id: 4001 + i,
    name,
    companyName: ACCOUNT_NAMES[(i + 5) % ACCOUNT_NAMES.length],
    mobile: `138${String(20000000 + i * 121212).slice(0, 8)}`,
    email: `lead${i}@example.com`,
    positionName: pick(['信息中心主任', '副总经理', '科技处科长', '风控经理'], i),
    industry: pick(INDUSTRIES, i + 2),
    sourceChannel: pick(['WEB', 'REFERRAL', 'EXHIBITION', 'TENDER'], i),
    grade: pick(['A', 'B', 'C'], i),
    status,
    accountNature: 'TO_B',
    ...ownerOf(pick([2, 3, 4, 6], i)),
    lastFollowTime: status === 'FOLLOWING' ? dayStr(-2 - i) : undefined,
    convertedAccountId: status === 'CONVERTED' ? accounts[i].id : undefined,
    convertedTime: status === 'CONVERTED' ? dayStr(-20 - i) : undefined,
    invalidReason: status === 'INVALID' ? '资质要求不满足，主动放弃' : undefined,
    remark: '',
    createdTime: dayStr(-60 + i * 2)
  }
})

const STAGES = ['PROSPECT', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST']
const OPP_NAMES = ACCOUNT_NAMES.slice(0, 12).map((c) => `${c.slice(0, 12)}征信系统采购项目`)
export const opportunities: CrmOpportunity[] = OPP_NAMES.map((name, i) => {
  const stage = STAGES[i % 5]
  const expectedAmount = [86, 132, 45, 210, 68, 95, 156, 73, 240, 58, 118, 88][i] * 10000
  return {
    id: 5001 + i,
    name,
    accountId: accounts[i].id,
    productId: undefined,
    expectedAmount,
    expectedCloseDate: dayStr(30 + i * 7),
    salesStage: stage,
    winProbability: { PROSPECT: 20, PROPOSAL: 45, NEGOTIATION: 70, WON: 100, LOST: 0 }[stage],
    estRevenue: Math.round(expectedAmount * 0.9),
    currency: 'CNY',
    estCost: Math.round(expectedAmount * 0.55),
    estProfitRate: 45.0,
    estServiceVolume: `${(expectedAmount / 10000).toFixed(1)}万`,
    pricingMode: pick(['LUMP_SUM', 'UNIT_PRICE', 'HEAD_COUNT', 'TIME_BASED'], i),
    pricingDetail: '按方案约定',
    ...ownerOf(accounts[i].ownerUserId),
    isWon: stage === 'WON' ? 1 : 0,
    address: '',
    lostReason: stage === 'LOST' ? '客户选择自建，价格竞争激烈' : undefined,
    stageChangeTime: dayStr(-10 - i),
    lastActivityTime: dayStr(-4 - i),
    remark: '',
    createdTime: dayStr(-90 + i * 3)
  }
})

export const competitors: CrmCompetitor[] = [
  { id: 6001, opportunityId: 5001, competitorName: '百行征信有限公司', staffSize: 500, coverageScope: '全国', techAbility: '强', pricingMode: 'LUMP_SUM', pricingDetail: '总价 90 万', operationEfficiency: '中', operationQuality: '高', remark: '' },
  { id: 6002, opportunityId: 5002, competitorName: '朴道征信有限公司', staffSize: 300, coverageScope: '华北', techAbility: '中', pricingMode: 'UNIT_PRICE', pricingDetail: '按查询次数计费', operationEfficiency: '高', operationQuality: '中', remark: '' }
]

const ACT_SUBJECTS = ['首访客户信息科技部', '产品方案宣讲', '报价沟通会', '高层关系维护拜访', '合同条款谈判', '售后回访', '项目启动会对接', '季度业务复盘会']
export const activities: CrmActivity[] = Array.from({ length: 36 }, (_, i) => ({
  id: 7001 + i,
  subject: `${ACT_SUBJECTS[i % ACT_SUBJECTS.length]}（${accounts[i % accounts.length].companyName.slice(0, 10)}）`,
  type: pick(['VISIT', 'CALL', 'MEETING', 'EMAIL'], i),
  level: pick(['A', 'B', 'C'], i),
  startTime: `${dayStr(-i * 2)} 09:00:00`,
  endTime: `${dayStr(-i * 2)} 11:00:00`,
  durationDays: undefined,
  targetType: pick(['ACCOUNT', 'OPPORTUNITY', 'CONTACT'], i),
  targetId: [accounts[i % accounts.length].id, opportunities[i % opportunities.length].id, contacts[i % contacts.length].id][i % 3],
  ...ownerOf(accounts[i % accounts.length].ownerUserId),
  description: '演示数据：与客户就征信平台建设方案进行沟通，客户关注数据接入与等保合规。',
  status: 'VALID',
  remark: '',
  createdTime: dayStr(-i * 2 - 1)
}))

const now = new Date('2026-10-28T00:00:00')
export const contracts: CrmContract[] = Array.from({ length: 10 }, (_, i) => {
  const start = dayStr(-150 + i * 12)
  const endOffset = [-5, 3, 12, 25, 45, 80, 120, 200, 300, 400][i]
  const status = endOffset < 0 ? 'EXPIRED' : i === 3 ? 'APPROVING' : i === 0 ? 'DRAFT' : 'ACTIVE'
  return {
    id: 8001 + i,
    contractNo: `HT2026${String(101 + i).padStart(4, '0')}`,
    name: `${accounts[i].companyName.slice(0, 12)}征信服务合同${i > 4 ? '（续签）' : ''}`,
    accountId: accounts[i].id,
    companyId: COMPANIES[i % 2].id,
    businessGroupId: i % 2 === 0 ? 5 : 2,
    productId: undefined,
    signDate: start,
    startDate: start,
    endDate: dayStr(endOffset),
    contractType: i > 4 ? 'RENEW' : 'FIRST',
    status,
    sourceType: 'OPPORTUNITY',
    sourceId: opportunities[i].id,
    winAmount: [86, 132, 45, 210, 68, 95, 156, 73, 240, 58][i] * 10000,
    ...ownerOf(accounts[i].ownerUserId),
    isArchived: 0,
    paperBoxNo: '',
    paperCopies: 2,
    flowInstanceId: status === 'APPROVING' ? 'demo-flow-8004' : undefined,
    remark: '',
    createdTime: start
  }
})

export const agreements: CrmAgreement[] = Array.from({ length: 6 }, (_, i) => ({
  id: 8101 + i,
  agreementNo: `XY2026${String(201 + i).padStart(4, '0')}`,
  name: `${accounts[i + 3].companyName.slice(0, 10)}${pick(['框架协议', '保密协议', '服务协议'], i)}`,
  agreementType: pick(['FRAMEWORK', 'NDA', 'SERVICE'], i),
  accountId: accounts[i + 3].id,
  companyId: COMPANIES[i % 2].id,
  opportunityId: opportunities[i].id,
  businessGroupId: 5,
  signDate: dayStr(-100 + i * 15),
  startDate: dayStr(-100 + i * 15),
  endDate: i === 1 ? null : dayStr(200 + i * 30),
  durationDays: undefined,
  status: pick(['EFFECTIVE', 'DRAFT', 'APPROVING'], i),
  signType: 'ONLINE',
  winAmount: [50, 30, 88, 120, 66, 42][i] * 10000,
  ...ownerOf(accounts[i + 3].ownerUserId),
  isArchived: 0,
  remark: '',
  createdTime: dayStr(-100 + i * 15)
})) as CrmAgreement[]

const BID_NAMES = ACCOUNT_NAMES.slice(4, 16).map((c) => `${c.slice(0, 10)}信用平台招标`)
export const bids: CrmBid[] = BID_NAMES.map((tenderName, i) => {
  const openOffset = [-30, -10, -3, 0, 1, 2, 3, 5, 8, 12, 20, 35][i]
  const bidStatus = openOffset < -5 ? pick(['WON', 'LOST'], i) : openOffset < 0 ? 'SUBMITTED' : i % 3 === 0 ? 'REGISTERED' : 'TRACKING'
  return {
    id: 9001 + i,
    accountId: accounts[i + 4].id,
    tenderName,
    tenderNo: `TB2026-${3000 + i}`,
    tenderForm: pick(['OPEN', 'INVITE', 'NEGOTIATED'], i),
    bidEntity: 1,
    province: pick(PROVINCES, i + 1),
    city: '市区',
    regionOrgId: pick([2, 3], i),
    priceCap: [120, 80, 200, 150, 90, 66, 180, 140, 95, 210, 75, 130][i] * 10000,
    openDate: dayStr(openOffset),
    deadline: dayStr(openOffset - 7),
    deposit: 50000,
    depositStatus: pick(['PAID', 'NONE', 'REFUNDED'], i),
    bidStatus,
    winAmount: bidStatus === 'WON' ? [120, 80, 200, 150, 90, 66][i % 6] * 10000 * 0.95 : undefined,
    winResult: bidStatus === 'WON' ? '第一中标候选人' : undefined,
    isWon: bidStatus === 'WON' ? 1 : 0,
    ...ownerOf(pick([2, 3, 4, 6], i)),
    bidPlan: '演示数据：联合生态伙伴投标，本地化服务团队驻场。',
    remark: '',
    createdTime: dayStr(openOffset - 20)
  }
})

export const claims: CrmClaimVO[] = [
  { id: 9501, accountId: 2013, accountName: accounts[12].companyName, applicantId: 3, applicantName: '张伟', applicantOrgId: 2, oldOwnerId: undefined, oldOwnerName: undefined, status: 'APPROVING', flowInstanceId: 'demo-flow-9501', applyReason: '长期跟进客户，拥有成熟对接渠道', createdTime: dayStr(-6), poolStatus: 'POOL' },
  { id: 9502, accountId: 2014, accountName: accounts[13].companyName, applicantId: 4, applicantName: '刘洋', applicantOrgId: 3, oldOwnerId: undefined, oldOwnerName: undefined, status: 'APPROVING', flowInstanceId: 'demo-flow-9502', applyReason: '区域客户，跟进成本低', createdTime: dayStr(-3), poolStatus: 'POOL' },
  { id: 9503, accountId: 2015, accountName: accounts[14].companyName, applicantId: 6, applicantName: '赵磊', applicantOrgId: 2, oldOwnerId: undefined, oldOwnerName: undefined, status: 'APPROVED', appealDeadline: dayStr(5), appealStatus: 'NONE', applyReason: '原客户经理离职，由我接手', createdTime: dayStr(-15), poolStatus: 'POOL' },
  { id: 9504, accountId: 2016, accountName: accounts[15].companyName, applicantId: 3, applicantName: '张伟', applicantOrgId: 2, oldOwnerId: undefined, oldOwnerName: undefined, status: 'REJECTED', applyReason: '客户明确拒绝对接', createdTime: dayStr(-25), poolStatus: 'POOL' }
]

// ==================== 字典 ====================

const DICT_MAP: Record<string, [string, string][]> = {
  crm_account_nature: [['TO_B', 'To B（企业）'], ['TO_C', 'To C（个人）'], ['TO_G', 'To G（政府）']],
  crm_account_type: [['NORMAL', '普通客户'], ['KEY', '重点客户'], ['STRATEGIC', '战略客户']],
  crm_activity_level: [['A', 'A 级（关键）'], ['B', 'B 级（重要）'], ['C', 'C 级（一般）']],
  crm_activity_status: [['VALID', '有效'], ['VOIDED', '已作废']],
  crm_activity_type: [['VISIT', '拜访'], ['CALL', '电话'], ['EMAIL', '邮件'], ['MEETING', '会议'], ['IM', '即时沟通'], ['OTHER', '其他']],
  crm_agreement_status: [['DRAFT', '草稿'], ['APPROVING', '审批中'], ['EFFECTIVE', '生效中'], ['EXPIRED', '已失效'], ['TERMINATED', '已终止']],
  crm_agreement_type: [['FRAMEWORK', '框架协议'], ['SUPPLEMENT', '补充协议'], ['NDA', '保密协议'], ['SERVICE', '服务协议']],
  crm_bid_status: [['TRACKING', '跟踪中'], ['REGISTERED', '已报名'], ['SUBMITTED', '已投标'], ['WON', '已中标'], ['LOST', '未中标'], ['ABANDONED', '已放弃']],
  crm_claim_status: [['APPROVING', '审批中'], ['APPROVED', '已通过'], ['REJECTED', '已驳回'], ['CANCELLED', '已撤销']],
  crm_contract_status: [['DRAFT', '草稿'], ['APPROVING', '审批中'], ['ACTIVE', '生效中'], ['EXPIRED', '已到期'], ['TERMINATED', '已终止']],
  crm_contract_type: [['FIRST', '首签'], ['RENEW', '续签'], ['SUPPLEMENT', '补充协议']],
  crm_deposit_status: [['NONE', '无需缴纳'], ['PAID', '已缴纳'], ['REFUNDED', '已退还'], ['FORFEITED', '已没收']],
  crm_industry: [['BANK', '银行'], ['SECURITIES', '证券'], ['INSURANCE', '保险'], ['GOV', '政府机关'], ['JUDICIAL', '司法机构'], ['ENTERPRISE', '一般企业'], ['LEASING', '融资租赁'], ['FINTECH', '金融科技']],
  crm_lead_grade: [['A', 'A 级（高意向）'], ['B', 'B 级（中意向）'], ['C', 'C 级（低意向）'], ['D', 'D 级（待验证）']],
  crm_lead_source: [['WEB', '官网/线上'], ['PHONE', '电话呼入'], ['REFERRAL', '老客户推荐'], ['EXHIBITION', '展会活动'], ['PARTNER', '渠道合作'], ['TENDER', '招投标信息'], ['OTHER', '其他']],
  crm_lead_status: [['NEW', '新建'], ['FOLLOWING', '跟进中'], ['CONVERTED', '已转化'], ['INVALID', '已作废']],
  crm_pool_status: [['PRIVATE', '私有'], ['POOL', '公共池'], ['CHURNED', '已流失']],
  crm_pricing_mode: [['FREE', '免费'], ['LUMP_SUM', '总价包干'], ['UNIT_PRICE', '单价计量'], ['TIME_BASED', '按工时'], ['HEAD_COUNT', '按人月'], ['COMMISSION', '佣金分成'], ['EFFECT_BASED', '按效果']],
  crm_sales_stage: [['PROSPECT', '寻找潜在客户'], ['PROPOSAL', '提案报价'], ['NEGOTIATION', '谈判'], ['WON', '成交'], ['LOST', '流失']],
  crm_target_type: [['LEAD', '线索'], ['ACCOUNT', '客户'], ['CONTACT', '联系人'], ['OPPORTUNITY', '商机'], ['CONTRACT', '合同'], ['BID', '投标']],
  crm_tender_form: [['OPEN', '公开招标'], ['INVITE', '邀请招标'], ['NEGOTIATED', '竞争性磋商'], ['SINGLE', '单一来源'], ['INQUIRY', '询价']],
  sys_user_status: [['1', '在职'], ['0', '停用']],
  sys_common_yes_no: [['1', '是'], ['0', '否']]
}

const DICT_ITEMS: Record<string, DictItem[]> = Object.fromEntries(
  Object.entries(DICT_MAP).map(([type, pairs]) => [
    type,
    pairs.map(([dictValue, dictLabel], i) => ({ id: i + 1, dictType: type, dictValue, dictLabel, sortNo: i + 1, status: 1 }))
  ])
)

// ==================== 菜单 / 认证 ====================

const m = (id: number, parentId: number, name: string, path: string, icon?: string, perms?: string): MenuNode => ({
  id, parentId, name, path, type: 2, icon, perms, children: []
})

const CRM_MENUS: MenuNode[] = [
  m(1101, 100, '客户管理', '/crm/customer', 'address-book', 'crm:account:list'),
  m(1102, 100, '公共客户池', '/crm/pool', 'users-group', 'crm:account:pool'),
  m(1110, 100, '客户认领审批', '/crm/claim', 'user-check', 'crm:account:claim'),
  m(1109, 100, '流失客户', '/crm/churn', 'user-off', 'crm:account:list'),
  m(1111, 100, '联系人管理', '/crm/contact', 'users', 'crm:contact:list'),
  m(1103, 100, '线索管理', '/crm/lead', 'bulb', 'crm:lead:list'),
  m(1112, 100, '线索转化工作台', '/crm/lead-convert', 'transform', 'crm:lead:convert'),
  m(1104, 100, '商机管理', '/crm/opportunity', 'progress', 'crm:opportunity:list'),
  m(1113, 100, '商机漏斗', '/crm/opportunity-funnel', 'chart-funnel', 'crm:opportunity:list'),
  m(1105, 100, '销售活动', '/crm/follow', 'message', 'crm:activity:list'),
  m(1114, 100, '活动时间线', '/crm/activity-timeline', 'timeline', 'crm:activity:list'),
  m(1106, 100, '法务合同', '/crm/contract', 'file-text', 'crm:contract:list'),
  m(1115, 100, '合同到期预警', '/crm/contract-expire', 'alert-triangle', 'crm:contract:list'),
  m(1107, 100, '协议管理', '/crm/agreement', 'file-certificate', 'crm:agreement:list'),
  m(1108, 100, '投标管理', '/crm/bid', 'gavel', 'crm:bid:list'),
  m(1116, 100, '投标日历', '/crm/bid-calendar', 'calendar', 'crm:bid:list')
]

const SYSTEM_MENUS: MenuNode[] = [
  m(101, 100, '用户管理', '/system/user', 'user', 'system:user:list'),
  m(102, 100, '组织架构', '/system/org', 'sitemap', 'system:org:list'),
  m(103, 100, '公司主体', '/system/company', 'building', 'system:company:list'),
  m(104, 100, '角色管理', '/system/role', 'rosette-discount-check', 'system:role:list'),
  m(105, 100, '菜单管理', '/system/menu', 'list', 'system:menu:list'),
  m(106, 100, '字典管理', '/system/dict', 'book', 'system:dict:list'),
  m(107, 100, '参数配置', '/system/config', 'settings', 'system:config:list'),
  m(108, 100, '通知公告', '/system/notice', 'bell', 'system:notice:list'),
  m(109, 100, '审批流程', '/system/flow', 'git-merge', 'system:flow:list'),
  m(110, 100, '字段脱敏', '/system/sensitive', 'eye-off', 'system:sensitive:list'),
  m(111, 100, '账号交接', '/system/transfer', 'arrows-right-left', 'system:transfer:list'),
  m(112, 100, '操作日志', '/system/oper-log', 'history', 'system:log:oper'),
  m(113, 100, '登录日志', '/system/login-log', 'login', 'system:log:login'),
  m(114, 100, '在线用户', '/system/online', 'wifi', 'system:online:list')
]

const MENUS: MenuNode[] = [
  { id: 100, parentId: 0, name: 'CRM', path: '/crm', type: 1, icon: 'users', children: CRM_MENUS },
  { id: 90, parentId: 0, name: '系统管理', path: '/system', type: 1, icon: 'settings', children: SYSTEM_MENUS },
  {
    id: 91, parentId: 0, name: '审批中心', path: '/flow/center', type: 1, icon: 'git-merge', children: [],
    perms: undefined
  },
  {
    id: 101, parentId: 0, name: '项目管理', path: '/project', type: 1, icon: 'briefcase', children: [
      m(1201, 101, '产品管理', '/project/product', 'box'),
      m(1202, 101, '项目列表', '/project/list', 'list'),
      m(1203, 101, '任务看板', '/project/board', 'layout-kanban'),
      m(1204, 101, '项目立项', '/project/initiation', 'flag'),
      m(1205, 101, '项目评审', '/project/review', 'checklist'),
      m(1206, 101, '项目验收', '/project/acceptance', 'circle-check'),
      m(1207, 101, '项目复盘', '/project/retro', 'refresh')
    ]
  },
  {
    id: 102, parentId: 0, name: '驾驶舱', path: '/cockpit', type: 1, icon: 'dashboard', children: [
      m(1301, 102, '经营总览', '/cockpit/overview', 'chart-area'),
      m(1302, 102, '销售看板', '/cockpit/sales', 'chart-bar'),
      m(1303, 102, '地区部经营看板', '/cockpit/region', 'map'),
      m(1304, 102, '商务部看板', '/cockpit/business', 'building-bank'),
      m(1305, 102, '人事看板', '/cockpit/hr', 'users-group')
    ]
  }
]

function profileOf(): Profile {
  return {
    userId: 1, username: 'admin', realName: '王建国', roleId: 101, roleCode: 'SUPER_ADMIN', roleName: '超级管理员',
    orgId: 1, orgName: '总公司', orgPath: '总公司', companyNames: '国联政信股份有限公司', dataScope: 'ALL',
    dataScopeLabel: '全部数据', impersonate: false, readOnly: false, firstLogin: false, perms: ['*:*:*']
  }
}

function authResult(): AuthResult {
  const profile = profileOf()
  return { token: 'demo-token', profile, menus: MENUS, roles: roleOptions(), todoCount: 3, unreadCount: 2 }
}

function roleOptions(): RoleOption[] {
  return ROLES.map((r) => ({
    roleId: r.id, roleCode: r.roleCode, roleName: r.roleName, orgId: r.orgId, orgName: r.orgName,
    dataScope: r.dataScope, dataScopeLabel: r.dataScopeLabel, defaultRole: r.id === 101, current: r.id === 101
  }))
}

// ==================== 分页与路由 ====================

function page<T>(list: T[], q: Record<string, unknown>, match?: (item: T) => boolean): PageResult<T> {
  let rows = match ? list.filter(match) : list
  const keyword = String(q.keyword ?? '').trim()
  if (keyword) {
    rows = rows.filter((row) =>
      JSON.stringify(row).toLowerCase().includes(keyword.toLowerCase())
    )
  }
  const status = q.status
  if (status) rows = rows.filter((row) => (row as { status?: unknown }).status === status)
  const pageNum = Math.max(1, Number(q.pageNum ?? 1))
  const pageSize = Math.max(1, Number(q.pageSize ?? 20))
  return { list: rows.slice((pageNum - 1) * pageSize, pageNum * pageSize), total: rows.length, pageNum, pageSize }
}

let nextId = 99001

interface Ctx {
  query: Record<string, string>
  body: Record<string, unknown>
}

type Handler = (ctx: Ctx, ...rest: string[]) => unknown

const routes: [string, RegExp, Handler][] = [
  // ---- 认证 ----
  ['POST', /^\/auth\/login$/, () => authResult()],
  ['POST', /^\/auth\/switch-role$/, () => authResult()],
  ['POST', /^\/auth\/logout$/, () => null],
  ['GET', /^\/auth\/user-info$/, () => authResult()],
  ['GET', /^\/auth\/menus$/, () => MENUS],
  ['GET', /^\/auth\/my-roles$/, () => roleOptions()],
  ['POST', /^\/auth\/change-password$/, () => null],
  ['GET', /^\/system\/debug\/data-scope-sql$/, () => ({ sql: 'SELECT 1' })],

  // ---- CRM 列表与详情 ----
  ['GET', /^\/crm\/leads$/, ({ query }) => page(leads, query)],
  ['GET', /^\/crm\/leads\/(\d+)$/, (_c, id) => leads.find((l) => l.id === Number(id)) ?? leads[0]],
  ['POST', /^\/crm\/leads$/, () => ({ id: nextId++ })],
  ['PUT', /^\/crm\/leads\/\d+/, () => null],
  ['DELETE', /^\/crm\/leads\/\d+$/, () => null],
  ['POST', /^\/crm\/leads\/(\d+)\/convert$/, (_c, id) => {
    const lead = leads.find((l) => l.id === Number(id))
    if (lead) {
      lead.status = 'CONVERTED'
      lead.convertedAccountId = accounts[0].id
      lead.convertedTime = dayStr(0)
    }
    return { idempotent: false, accountId: accounts[0].id, contactId: contacts[0].id, opportunityId: opportunities[0].id }
  }],

  ['GET', /^\/crm\/accounts\/pool$/, ({ query }) => page(accounts, query, (a) => a.poolStatus === 'POOL')],
  ['GET', /^\/crm\/accounts$/, ({ query }) => page(accounts, query, (a) => a.poolStatus !== 'CHURNED')],
  ['GET', /^\/crm\/accounts\/(\d+)\/tab-counts$/, (_c, id) => ({
    contacts: contacts.filter((c) => c.accountId === Number(id)).length,
    opportunities: opportunities.filter((o) => o.accountId === Number(id)).length,
    contracts: contracts.filter((c) => c.accountId === Number(id)).length,
    agreements: agreements.filter((a) => a.accountId === Number(id)).length,
    bids: bids.filter((b) => b.accountId === Number(id)).length
  })],
  ['GET', /^\/crm\/accounts\/(\d+)\/relations$/, () => ({ deletable: false, total: 3 })],
  ['GET', /^\/crm\/accounts\/(\d+)$/, (_c, id) => accounts.find((a) => a.id === Number(id)) ?? accounts[0]],
  ['POST', /^\/crm\/accounts$/, () => ({ id: nextId++ })],
  ['PUT', /^\/crm\/accounts\/\d+/, () => null],
  ['DELETE', /^\/crm\/accounts\/\d+$/, () => null],
  ['POST', /^\/crm\/accounts\/(\d+)\/claim$/, () => ({ applyId: nextId++ })],

  ['GET', /^\/crm\/contacts$/, ({ query }) => page(contacts, query)],
  ['GET', /^\/crm\/contacts\/(\d+)$/, (_c, id) => contacts.find((c) => c.id === Number(id)) ?? contacts[0]],
  ['POST', /^\/crm\/contacts$/, () => ({ id: nextId++ })],
  ['PUT', /^\/crm\/contacts\/\d+/, () => null],
  ['DELETE', /^\/crm\/contacts\/\d+$/, () => null],

  ['GET', /^\/crm\/opportunities\/funnel$/, () => funnelStages()],
  ['GET', /^\/crm\/opportunities$/, ({ query }) => page(opportunities, query)],
  ['GET', /^\/crm\/opportunities\/(\d+)$/, (_c, id) => opportunities.find((o) => o.id === Number(id)) ?? opportunities[0]],
  ['GET', /^\/crm\/opportunities\/(\d+)\/competitors\/compare$/, (_c, id) => compareMatrix(Number(id))],
  ['GET', /^\/crm\/opportunities\/(\d+)\/competitors$/, () => competitors],
  ['POST', /^\/crm\/opportunities$/, () => ({ id: nextId++ })],
  ['PUT', /^\/crm\/opportunities\/\d+/, () => null],
  ['DELETE', /^\/crm\/opportunities\/\d+$/, () => null],
  ['POST', /^\/crm\/opportunities\/(\d+)\/competitors$/, () => ({ id: nextId++ })],
  ['PUT', /^\/crm\/opportunities\/\d+\/competitors\/\d+/, () => null],
  ['DELETE', /^\/crm\/opportunities\/\d+\/competitors\/\d+$/, () => null],

  ['GET', /^\/crm\/activities\/timeline$/, () => activities.slice(0, 12)],
  ['GET', /^\/crm\/activities\/mine$/, () => activities.slice(0, 8)],
  ['GET', /^\/crm\/activities$/, ({ query }) => page(activities, query)],
  ['POST', /^\/crm\/activities$/, () => ({ id: nextId++ })],
  ['PUT', /^\/crm\/activities\/\d+/, () => null],

  ['GET', /^\/crm\/contracts\/expiring$/, ({ query }) => {
    const days = Number(query.days ?? 30)
    return contracts.filter((c) => c.status === 'ACTIVE' && c.endDate && daysUntil(c.endDate) >= 0 && daysUntil(c.endDate) <= days)
  }],
  ['GET', /^\/crm\/contracts$/, ({ query }) => page(contracts, query)],
  ['GET', /^\/crm\/contracts\/(\d+)$/, (_c, id) => contracts.find((c) => c.id === Number(id)) ?? contracts[0]],
  ['POST', /^\/crm\/contracts$/, () => ({ id: nextId++ })],
  ['PUT', /^\/crm\/contracts\/\d+/, () => null],
  ['DELETE', /^\/crm\/contracts\/\d+$/, () => null],
  ['POST', /^\/crm\/contracts\/(\d+)\/(submit|withdraw|archive|renew|terminate)/, () => ({ flowInstanceId: `demo-flow-${nextId++}` })],

  ['GET', /^\/crm\/agreements$/, ({ query }) => page(agreements, query)],
  ['GET', /^\/crm\/agreements\/(\d+)$/, (_c, id) => agreements.find((a) => a.id === Number(id)) ?? agreements[0]],
  ['POST', /^\/crm\/agreements$/, () => ({ id: nextId++ })],
  ['PUT', /^\/crm\/agreements\/\d+/, () => null],
  ['DELETE', /^\/crm\/agreements\/\d+$/, () => null],
  ['POST', /^\/crm\/agreements\/(\d+)\/(submit|withdraw|archive|renew)/, () => ({ flowInstanceId: `demo-flow-${nextId++}` })],

  ['GET', /^\/crm\/bids\/calendar$/, ({ query }) => {
    const month = query.month
    return month ? bids.filter((b) => (b.openDate ?? '').startsWith(month)) : bids
  }],
  ['GET', /^\/crm\/bids\/opening-soon$/, ({ query }) => {
    const days = Number(query.days ?? 3)
    return bids.filter((b) => b.openDate && daysUntil(b.openDate) >= 0 && daysUntil(b.openDate) <= days)
  }],
  ['GET', /^\/crm\/bids$/, ({ query }) => page(bids, query)],
  ['GET', /^\/crm\/bids\/(\d+)$/, (_c, id) => bids.find((b) => b.id === Number(id)) ?? bids[0]],
  ['POST', /^\/crm\/bids$/, () => ({ id: nextId++ })],
  ['PUT', /^\/crm\/bids\/\d+/, () => null],
  ['DELETE', /^\/crm\/bids\/\d+$/, () => null],
  ['POST', /^\/crm\/bids\/(\d+)\/gen-opportunity$/, () => ({ id: opportunities[0].id })],
  ['POST', /^\/crm\/bids\/(\d+)\/gen-contract$/, () => ({ id: contracts[0].id })],

  ['GET', /^\/crm\/claims\/pending-count$/, () => ({ count: claims.filter((c) => c.status === 'APPROVING').length })],
  ['GET', /^\/crm\/claims$/, ({ query }) => page(claims, query)],

  ['GET', /^\/crm\/common\/stats\/summary$/, () => ({
    myLeads: leads.filter((l) => l.status !== 'INVALID').length,
    opportunityAmount: opportunities.filter((o) => o.salesStage !== 'LOST').reduce((s, o) => s + (o.expectedAmount ?? 0), 0),
    pendingContracts: contracts.filter((c) => c.status === 'APPROVING').length,
    monthBids: bids.filter((b) => (b.openDate ?? '').startsWith('2026-10')).length
  })],
  ['GET', /^\/crm\/common\/stats\/funnel$/, () => ({
    newLeads: leads.filter((l) => l.status === 'NEW' || l.status === 'FOLLOWING').length,
    convertedLeads: leads.filter((l) => l.status === 'CONVERTED').length,
    leadConvertRate: Math.round((leads.filter((l) => l.status === 'CONVERTED').length / Math.max(1, leads.length)) * 100),
    stages: funnelStages()
  })],
  ['GET', /^\/crm\/common\/products$/, () => ({ degraded: true, list: [], message: '演示模式：产品中心未接入，引用服务已降级' })],
  ['GET', /^\/crm\/common\/check-duplicate$/, () => ({ duplicated: false, hits: [] })],

  ['GET', /^\/crm\/opening-sources\/opportunities$/, () => openingFrom('OPPORTUNITY')],
  ['GET', /^\/crm\/opening-sources\/bids$/, () => openingFrom('BID')],
  ['GET', /^\/crm\/opening-sources\/contracts$/, () => openingFrom('CONTRACT')],
  ['GET', /^\/crm\/opening-sources\/agreements$/, () => openingFrom('AGREEMENT')],
  ['GET', /^\/crm\/opening-sources\/by-id$/, () => openingFrom('OPPORTUNITY')[0]],

  // ---- 字典 ----
  ['GET', /^\/system\/dicts\/data$/, ({ query }) => {
    const types = String(query.types ?? '').split(',').filter(Boolean)
    const out: Record<string, DictItem[]> = {}
    types.forEach((t) => { if (DICT_ITEMS[t]) out[t] = DICT_ITEMS[t] })
    return out
  }],
  ['GET', /^\/system\/dicts\/types$/, ({ query }) => {
    const types = Object.keys(DICT_ITEMS).map((dictType, i) => ({ id: 9000 + i, dictType, dictName: dictType, status: 1 }))
    return page(types, query)
  }],
  ['GET', /^\/system\/dicts\/types\/([a-z_]+)\/items$/, (_c, type) => DICT_ITEMS[type] ?? []],
  ['GET', /^\/system\/configs$/, () => [
    { id: 1, configKey: 'crm.pool.timeout.days', configValue: '30', configName: '客户池无活动超期天数', configType: 'crm', builtin: 1 },
    { id: 2, configKey: 'crm.claim.appeal.hours', configValue: '72', configName: '认领申诉窗口时长', configType: 'crm', builtin: 1 },
    { id: 3, configKey: 'crm.contract.expire.notify.days', configValue: '30', configName: '合同到期提前提醒天数', configType: 'crm', builtin: 1 }
  ]],
  ['PUT', /^\/system\/configs\/\d+/, () => null],

  // ---- 系统管理 ----
  ['GET', /^\/system\/users$/, ({ query }) => page(usersVo(), query)],
  ['GET', /^\/system\/users\/(\d+)$/, (_c, id) => usersVo().find((u) => u.id === Number(id)) ?? usersVo()[0]],
  ['POST', /^\/system\/users$/, () => ({ id: nextId++ })],
  ['PUT', /^\/system\/users\/\d+/, () => null],
  ['DELETE', /^\/system\/users\/\d+$/, () => null],
  ['POST', /^\/system\/users\/(\d+)\/reset-password$/, () => ({ password: 'Demo@1234' })],
  ['GET', /^\/system\/roles$/, ({ query }) => page(roleVo(), query)],
  ['GET', /^\/system\/roles\/options$/, () => roleVo()],
  ['GET', /^\/system\/roles\/(\d+)$/, (_c, id) => roleVo().find((r) => r.id === Number(id)) ?? roleVo()[0]],
  ['GET', /^\/system\/roles\/(\d+)\/field-mask$/, () => ({ CRM_CONTACT: 'KEEP_HEAD_TAIL' })],
  ['GET', /^\/system\/roles\/(\d+)\/users$/, () => usersVo().slice(0, 3)],
  ['POST', /^\/system\/roles$/, () => ({ id: nextId++ })],
  ['PUT', /^\/system\/roles\/\d+/, () => null],
  ['DELETE', /^\/system\/roles\/\d+$/, () => null],
  ['GET', /^\/system\/orgs\/tree$/, () => ORG_TREE],
  ['POST', /^\/system\/orgs$/, () => ({ id: nextId++ })],
  ['PUT', /^\/system\/orgs\/\d+/, () => null],
  ['DELETE', /^\/system\/orgs\/\d+$/, () => null],
  ['GET', /^\/system\/companies$/, ({ query }) => page(companiesVo(), query)],
  ['GET', /^\/system\/companies\/options$/, () => companiesVo()],
  ['POST', /^\/system\/companies$/, () => ({ id: nextId++ })],
  ['PUT', /^\/system\/companies\/\d+/, () => null],
  ['DELETE', /^\/system\/companies\/\d+$/, () => null],
  ['GET', /^\/system\/menus\/tree$/, () => MENUS],
  ['POST', /^\/system\/menus$/, () => ({ id: nextId++ })],
  ['PUT', /^\/system\/menus\/\d+/, () => null],
  ['DELETE', /^\/system\/menus\/\d+$/, () => null],

  ['GET', /^\/system\/notices\/badge$/, () => ({ todoCount: 3, unreadCount: 2 })],
  ['GET', /^\/system\/notices\/todos$/, () => notices().filter((n) => n.noticeType === 2)],
  ['GET', /^\/system\/notices\/mine$/, () => notices()],
  ['GET', /^\/system\/notices$/, ({ query }) => page(notices(), query)],
  ['POST', /^\/system\/notices/, () => ({ id: nextId++ })],
  ['PUT', /^\/system\/notices\/\d+/, () => null],
  ['DELETE', /^\/system\/notices\/\d+$/, () => null],
  ['GET', /^\/system\/logs\/oper$/, ({ query }) => page(operLogs(), query)],
  ['GET', /^\/system\/logs\/login$/, ({ query }) => page(loginLogs(), query)],
  ['GET', /^\/system\/online$/, () => [
    { sessionId: 'demo-s1', userId: 1, username: 'admin', realName: '王建国', roleName: '超级管理员', orgName: '总公司', ip: '10.0.0.8', loginTime: '2026-10-28 09:12:00', lastActiveTime: '2026-10-28 16:40:00', impersonate: false }
  ]],
  ['GET', /^\/system\/sensitive\/groups$/, () => [
    { id: 1, groupCode: 'CRM_CONTACT', groupName: 'CRM 联系人', defaultLevel: 'KEEP_HEAD_TAIL', riskLevel: 'HIGH', sortNo: 1, status: 1 },
    { id: 2, groupCode: 'CRM_AMOUNT', groupName: 'CRM 金额', defaultLevel: 'RANGE', riskLevel: 'MEDIUM', sortNo: 2, status: 1 }
  ]],
  ['GET', /^\/system\/sensitive\/fields$/, () => [
    { id: 1, groupCode: 'CRM_CONTACT', bizObject: 'CrmContact', fieldName: 'mobile', fieldLabel: '手机号', maskRule: 'KEEP_HEAD_TAIL', allowPlain: 0, status: 1 },
    { id: 2, groupCode: 'CRM_CONTACT', bizObject: 'CrmLead', fieldName: 'mobile', fieldLabel: '手机号', maskRule: 'KEEP_HEAD_TAIL', allowPlain: 0, status: 1 },
    { id: 3, groupCode: 'CRM_AMOUNT', bizObject: 'CrmAccount', fieldName: 'annualRevenue', fieldLabel: '年营收', maskRule: 'RANGE', allowPlain: 0, status: 1 }
  ]],
  ['GET', /^\/system\/sensitive\/whitelist$/, () => ({})],
  ['POST', /^\/system\/sensitive/, () => ({ id: nextId++ })],
  ['PUT', /^\/system\/sensitive\/[a-z]+\/\d+/, () => null],
  ['DELETE', /^\/system\/sensitive\/[a-z]+\/\d+$/, () => null],

  ['GET', /^\/system\/flows$/, () => [
    { id: 1, bizType: 'CRM_CONTRACT', bizName: '合同审批', flowKey: 'crm-contract', status: 1, nodeCount: 2 },
    { id: 2, bizType: 'CRM_AGREEMENT', bizName: '协议审批', flowKey: 'crm-agreement', status: 1, nodeCount: 2 },
    { id: 3, bizType: 'CRM_CLAIM', bizName: '客户认领审批', flowKey: 'crm-claim', status: 1, nodeCount: 2 }
  ]],
  ['POST', /^\/system\/flows$/, () => ({ id: nextId++ })],
  ['PUT', /^\/system\/flows\/\d+/, () => null],
  ['POST', /^\/system\/flows\/(\d+)\/publish$/, () => ({})],
  ['DELETE', /^\/system\/flows\/\d+$/, () => null],
  ['GET', /^\/flow\/tasks\/todo$/, () => [
    { taskId: 'demo-task-1', taskName: '部门经理审批', processInstanceId: 'demo-flow-8004', bizType: 'CRM_CONTRACT', bizNo: 'HT2026104', bizTitle: '中国工商银行北京市分行征信服务合同', initiatorName: '张伟', startTime: dayStr(-1), detailRoute: '/crm/contract' },
    { taskId: 'demo-task-2', taskName: '部门经理审批', processInstanceId: 'demo-flow-9501', bizType: 'CRM_CLAIM', bizNo: 'RL2026-9501', bizTitle: '认领申请：中国平安财产保险股份有限公司', initiatorName: '张伟', startTime: dayStr(-2), detailRoute: '/crm/claim' }
  ]],
  ['GET', /^\/flow\/tasks\/done$/, () => [
    { taskId: 'demo-task-9', taskName: '部门经理审批', processInstanceId: 'demo-flow-9503', bizType: 'CRM_CLAIM', bizNo: 'RL2026-9503', bizTitle: '认领申请：光大金融租赁股份有限公司', initiatorName: '赵磊', startTime: dayStr(-6), detailRoute: '/crm/claim' }
  ]],
  ['GET', /^\/flow\/instances\/mine$/, () => [
    { processInstanceId: 'demo-flow-8004', bizType: 'CRM_CONTRACT', bizNo: 'HT2026104', bizTitle: '征信服务合同审批', status: 'RUNNING', currentNodeName: '部门经理审批', startTime: dayStr(-1), detailRoute: '/crm/contract' }
  ]],
  ['GET', /^\/flow\/instances\/cc$/, () => []],
  ['GET', /^\/flow\/instances\/[^/]+\/history$/, () => [
    { taskId: 't1', taskName: '发起申请', assigneeName: '张伟', startTime: dayStr(-2), endTime: dayStr(-2), finished: true, comment: '提交审批' }
  ]],
  ['POST', /^\/flow\/tasks\/[^/]+\/(approve|reject|transfer)/, () => null],
  ['POST', /^\/flow\/instances\/[^/]+\/withdraw/, () => null],

  ['GET', /^\/system\/transfers\/adapters$/, () => [
    { bizType: 'CRM_LEAD', bizName: '线索', count: leads.length, available: true },
    { bizType: 'CRM_ACCOUNT', bizName: '客户', count: accounts.length, available: true },
    { bizType: 'CRM_CONTACT', bizName: '联系人', count: contacts.length, available: true },
    { bizType: 'CRM_OPPORTUNITY', bizName: '商机', count: opportunities.length, available: true },
    { bizType: 'CRM_CONTRACT', bizName: '合同', count: contracts.length, available: true },
    { bizType: 'CRM_AGREEMENT', bizName: '协议', count: agreements.length, available: true },
    { bizType: 'CRM_BID', bizName: '投标', count: bids.length, available: true }
  ]],
  ['GET', /^\/system\/transfers\/precheck$/, () => ({
    fromUserId: 3, fromUserName: '张伟',
    roles: [{ roleId: 103, roleCode: 'BIZ_SPECIALIST', roleName: '商务专员', orgName: '华北地区部', default: true }],
    adapters: [
      { bizType: 'CRM_LEAD', bizName: '线索', count: 5, available: true },
      { bizType: 'CRM_ACCOUNT', bizName: '客户', count: 8, available: true }
    ],
    totalCount: 13
  })],
  ['GET', /^\/system\/transfers\/eligible-receivers$/, () => [
    { userId: 6, realName: '赵磊', username: 'zhaolei', orgName: '华北地区部' }
  ]],
  ['GET', /^\/system\/transfers$/, ({ query }) => page(transferVo(), query)],
  ['GET', /^\/system\/transfers\/(\d+)\/items$/, () => [
    { id: 1, bizType: 'CRM_LEAD', bizId: 4001, bizName: '李慧', beforeOwnerName: '张伟', afterOwnerName: '赵磊', result: 'SUCCESS' },
    { id: 2, bizType: 'CRM_ACCOUNT', bizId: 2001, bizName: '中国工商银行北京市分行', beforeOwnerName: '张伟', afterOwnerName: '赵磊', result: 'SUCCESS' }
  ]],
  ['GET', /^\/system\/transfers\/(\d+)$/, () => transferVo()[0]],
  ['POST', /^\/system\/transfers$/, () => ({ id: nextId++ })],
  ['POST', /^\/system\/transfers\/(\d+)\/(submit|execute|retry|revoke)/, () => ({ processInstanceId: `demo-flow-${nextId++}` })]
]

// ==================== 派生数据 ====================

function daysUntil(dateStr: string): number {
  return Math.round((new Date(dateStr + 'T00:00:00').getTime() - now.getTime()) / 86400000)
}

function funnelStages(): FunnelStage[] {
  const amounts = (stage: string) =>
    opportunities.filter((o) => o.salesStage === stage).reduce((s, o) => s + (o.expectedAmount ?? 0), 0)
  const defs: [string, string, number][] = [
    ['PROSPECT', '寻找潜在客户', 100], ['PROPOSAL', '提案报价', 60], ['NEGOTIATION', '谈判', 35], ['WON', '成交', 20]
  ]
  return defs.map(([stage, stageLabel, rate], i) => {
    const count = opportunities.filter((o) => o.salesStage === stage).length
    return {
      stage, stageLabel, count,
      amount: amounts(stage),
      convertRate: Math.round(rate - i * 2)
    }
  })
}

function compareMatrix(id: number) {
  const opp = opportunities.find((o) => o.id === id) ?? opportunities[0]
  const rows = competitors.filter((c) => c.opportunityId === id)
  const ours = {
    name: '国联政信', staffSize: 800, coverageScope: '全国', techAbility: '自研数据底座，覆盖 3 亿企业画像',
    pricingMode: opp.pricingMode, pricingDetail: opp.pricingDetail, operationEfficiency: '高（7×24 响应）', operationQuality: '通过等保三级'
  }
  const theirs = rows.map((c) => ({ ...c }))
  const dims = ['staffSize', 'coverageScope', 'techAbility', 'pricingMode', 'pricingDetail', 'operationEfficiency', 'operationQuality']
  const labels: Record<string, string> = {
    staffSize: '人员规模', coverageScope: '覆盖范围', techAbility: '技术能力', pricingMode: '计价模式',
    pricingDetail: '报价明细', operationEfficiency: '运营效率', operationQuality: '运营质量'
  }
  return { ours, theirs, dims: dims.map((d) => ({ key: d, label: labels[d] })) }
}

function openingFrom(sourceType: string): OpeningSource[] {
  if (sourceType === 'OPPORTUNITY') {
    return opportunities.filter((o) => o.salesStage !== 'LOST').map((o) => ({
      sourceType, sourceId: o.id, sourceName: o.name, accountId: o.accountId,
      accountName: accounts.find((a) => a.id === o.accountId)?.companyName,
      amount: o.expectedAmount, currency: 'CNY', pricingMode: o.pricingMode, pricingDetail: o.pricingDetail,
      startDate: o.expectedCloseDate, ownerUserId: o.ownerUserId, ownerUserName: o.ownerUserName, orgId: o.ownerOrgId
    }))
  }
  if (sourceType === 'BID') {
    return bids.filter((b) => b.isWon === 1).map((b) => ({
      sourceType, sourceId: b.id, sourceNo: b.tenderNo, sourceName: b.tenderName, accountId: b.accountId,
      accountName: accounts.find((a) => a.id === b.accountId)?.companyName,
      amount: b.winAmount, currency: 'CNY', startDate: b.openDate, ownerUserId: b.ownerUserId, ownerUserName: b.ownerUserName, orgId: b.ownerOrgId
    }))
  }
  if (sourceType === 'CONTRACT') {
    return contracts.filter((c) => c.status === 'ACTIVE').map((c) => ({
      sourceType, sourceId: c.id, sourceNo: c.contractNo, sourceName: c.name, accountId: c.accountId,
      accountName: accounts.find((a) => a.id === c.accountId)?.companyName,
      amount: c.winAmount, currency: 'CNY', startDate: c.startDate, endDate: c.endDate,
      ownerUserId: c.ownerUserId, ownerUserName: c.ownerUserName, orgId: c.ownerOrgId
    }))
  }
  return agreements.map((a) => ({
    sourceType, sourceId: a.id, sourceNo: a.agreementNo, sourceName: a.name, accountId: a.accountId,
    accountName: accounts.find((x) => x.id === a.accountId)?.companyName,
    amount: a.winAmount, currency: 'CNY', startDate: a.startDate, endDate: a.endDate,
    ownerUserId: a.ownerUserId, ownerUserName: a.ownerUserName, orgId: a.ownerOrgId
  }))
}

function usersVo() {
  return USERS.map((u) => ({
    id: u.id, username: u.username, realName: u.realName, employeeNo: `E00${u.id}`,
    mobile: `1360000000${u.id}`, email: `${u.username}@guolian.com`, status: 1,
    employeeType: 'FORMAL', position: u.roleName, orgId: u.orgId, orgName: u.orgName,
    companyNames: '国联政信股份有限公司', lastLoginTime: '2026-10-28 09:00:00',
    roles: [{ userId: u.id, roleId: u.roleId, roleCode: u.roleCode, roleName: u.roleName, orgId: u.orgId, orgName: u.orgName, isDefault: 1, dataScope: 'ALL' }],
    companies: [{ companyId: 1, companyName: '国联政信股份有限公司', relationType: 1 }]
  }))
}

function roleVo() {
  return ROLES.map((r) => ({
    ...r, orgPath: r.orgName, scopeValue: r.dataScope === 'ALL' ? '' : String(r.orgId),
    canBeProjectMember: 1, canBeApprover: 1, sortNo: r.id, status: 1, description: ''
  }))
}

function companiesVo() {
  return COMPANIES.map((c) => ({ ...c, companyType: 'SELF', status: 1, userCount: 3 }))
}

function notices() {
  return [
    { id: 1, title: '平台上线通知：CRM 模块已发布', noticeType: 1, content: '演示数据', pinned: 1, status: 1, publishTime: '2026-10-20 10:00:00' },
    { id: 2, title: '待办：合同 HT2026104 待审批', noticeType: 2, content: '演示数据', pinned: 0, status: 0, publishTime: dayStr(-1), todoStatus: 0 },
    { id: 3, title: '待办：客户认领申请待审批', noticeType: 2, content: '演示数据', pinned: 0, status: 0, publishTime: dayStr(-2), todoStatus: 0 },
    { id: 4, title: '季度商务例会通知', noticeType: 1, content: '演示数据', pinned: 0, status: 1, publishTime: dayStr(-4) }
  ]
}

function operLogs() {
  return Array.from({ length: 8 }, (_, i) => ({
    id: i + 1, module: 'CRM', operation: pick(['查询客户列表', '新建线索', '提交合同审批', '更新商机阶段'], i),
    operatorName: USERS[i % USERS.length].realName, ip: '10.0.0.9', success: 1, costMs: 120 + i * 30,
    createdTime: `${dayStr(-i)} 10:${String(10 + i).padStart(2, '0')}:00`
  }))
}

function loginLogs() {
  return USERS.map((u, i) => ({
    id: i + 1, username: u.username, realName: u.realName, loginType: 'PASSWORD',
    ip: '10.0.0.8', browser: 'Chrome 120', os: 'Windows', success: 1,
    message: '登录成功', createdTime: `2026-10-28 0${i % 9}:1${i}:00`
  }))
}

function transferVo() {
  return [{
    id: 1, transferNo: 'JY2026-001', fromUserId: 3, fromUserName: '张伟', toUserIds: [6], toUserNames: '赵磊',
    assignMode: 1, scope: ['CRM_LEAD', 'CRM_ACCOUNT'], flowStatus: 'APPROVED', execStatus: 'SUCCESS',
    itemCount: 13, failCount: 0, revocable: false, createdTime: dayStr(-7), effectiveTime: dayStr(-6), remark: '演示数据'
  }]
}

// ==================== 入口 ====================

function splitUrl(url: string): { path: string; query: Record<string, string> } {
  const [path, qs = ''] = url.split('?')
  const query: Record<string, string> = {}
  new URLSearchParams(qs).forEach((v, k) => { query[k] = v })
  return { path, query }
}

/**
 * 演示模式请求入口：返回解包后的 data（与 get/post/put/del 的返回约定一致）。
 * 未匹配的路由：GET 返回空分页 / null，写操作返回成功，保证页面不白屏。
 */
export async function demoRequest<T>(method: string, url: string, params?: Record<string, unknown>, body?: unknown): Promise<T> {
  const { path, query } = splitUrl(url)
  const m = method.toUpperCase()
  for (const [routeMethod, pattern, handler] of routes) {
    if (routeMethod !== m) continue
    const match = pattern.exec(path)
    if (match) {
      const result = handler({ query, body: (body ?? {}) as Record<string, unknown> }, ...match.slice(1))
      // 模拟网络延迟，让加载态可见
      await new Promise((r) => setTimeout(r, 120))
      return result as T
    }
  }
  await new Promise((r) => setTimeout(r, 60))
  if (m === 'GET') {
    if (path.startsWith('/system/') || path.startsWith('/flow/')) return [] as T
    return { list: [], total: 0, pageNum: 1, pageSize: 20 } as T
  }
  return null as T
}
