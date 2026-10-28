import { del, download, get, post, put } from './request'
import type { PageQuery, PageResult } from './types'

/**
 * CRM 模块 API 客户端。
 * 9 个业务对象共用一套筛选字段（对应后端 CrmQuery），避免为每个对象各建一个 Query。
 */
export interface CrmQuery extends PageQuery {
  keyword?: string
  status?: string
  ownerUserId?: number | null
  startDate?: string
  endDate?: string
  accountId?: number | null
  poolStatus?: string
  accountType?: string
  accountNature?: string
  industry?: string
  province?: string
  channel?: string
  grade?: string
  stage?: string
  tenderForm?: string
  regionOrgId?: number | null
  month?: string
  targetType?: string
  targetId?: number | null
  activityType?: string
  businessGroupId?: number | null
  includeProspect?: boolean
}

export interface CrmLead {
  id: number
  name: string
  companyName?: string
  mobile?: string
  email?: string
  positionName?: string
  industry?: string
  sourceChannel?: string
  grade?: string
  status?: string
  accountNature?: string
  ownerUserId?: number
  ownerOrgId?: number
  ownerUserName?: string
  lastFollowTime?: string
  convertedAccountId?: number
  convertedContactId?: number
  convertedOpportunityId?: number
  convertedTime?: string
  invalidReason?: string
  dupReason?: string
  remark?: string
  createdTime?: string
}

export interface CrmAccount {
  id: number
  companyName: string
  creditCode?: string
  institutionCode?: string
  industry?: string
  accountNature?: string
  accountType?: string
  website?: string
  phone?: string
  employeeScale?: string
  annualRevenue?: number
  ownerUserId?: number
  ownerOrgId?: number
  ownerUserName?: string
  parentAccountId?: number
  primaryContactId?: number
  province?: string
  city?: string
  district?: string
  address?: string
  poolStatus?: string
  poolInTime?: string
  poolReason?: string
  churnReason?: string
  isProspect?: number
  lastActivityTime?: string
  remark?: string
  createdTime?: string
}

export interface CrmContact {
  id: number
  name: string
  accountId?: number
  mobile?: string
  email?: string
  positionName?: string
  department?: string
  birthday?: string
  address?: string
  ownerUserId?: number
  ownerOrgId?: number
  ownerUserName?: string
  lastActivityTime?: string
  remark?: string
  createdTime?: string
}

export interface CrmOpportunity {
  id: number
  name: string
  accountId?: number
  productId?: number
  expectedAmount?: number
  expectedCloseDate?: string
  salesStage?: string
  winProbability?: number
  estRevenue?: number
  currency?: string
  estCost?: number
  estProfitRate?: number
  estServiceVolume?: string
  pricingMode?: string
  pricingDetail?: string
  ownerUserId?: number
  ownerOrgId?: number
  ownerUserName?: string
  sourceLeadId?: number
  isWon?: number
  contractId?: number
  address?: string
  lostReason?: string
  stageChangeTime?: string
  lastActivityTime?: string
  remark?: string
  createdTime?: string
}

export interface CrmCompetitor {
  id: number
  opportunityId?: number
  competitorName: string
  staffSize?: number
  coverageScope?: string
  techAbility?: string
  pricingMode?: string
  pricingDetail?: string
  operationEfficiency?: string
  operationQuality?: string
  remark?: string
}

export interface CrmActivity {
  id: number
  subject: string
  type?: string
  level?: string
  startTime?: string
  endTime?: string
  durationDays?: number
  targetType?: string
  targetId?: number
  ownerUserId?: number
  ownerOrgId?: number
  ownerUserName?: string
  description?: string
  fileIds?: string
  status?: string
  voidReason?: string
  remark?: string
  createdTime?: string
}

export interface CrmContract {
  id: number
  contractNo?: string
  name: string
  accountId?: number
  companyId?: number
  businessGroupId?: number
  productId?: number
  signDate?: string
  startDate?: string
  endDate?: string
  durationDays?: number
  contractType?: string
  firstContractId?: number
  status?: string
  sourceType?: string
  sourceId?: number
  winAmount?: number
  ownerUserId?: number
  ownerOrgId?: number
  ownerUserName?: string
  isArchived?: number
  archiveTime?: string
  paperBoxNo?: string
  paperCopies?: number
  activateTime?: string
  terminateTime?: string
  terminateReason?: string
  flowInstanceId?: string
  remark?: string
  createdTime?: string
}

export interface CrmAgreement {
  id: number
  agreementNo?: string
  name: string
  agreementType?: string
  accountId?: number
  companyId?: number
  opportunityId?: number
  productId?: number
  businessGroupId?: number
  signDate?: string
  startDate?: string
  endDate?: string
  durationDays?: number
  status?: string
  signType?: string
  firstAgreementId?: number
  winAmount?: number
  ownerUserId?: number
  ownerOrgId?: number
  ownerUserName?: string
  isArchived?: number
  archiveTime?: string
  paperBoxNo?: string
  paperCopies?: number
  activateTime?: string
  flowInstanceId?: string
  terminateReason?: string
  remark?: string
  createdTime?: string
}

export interface CrmBid {
  id: number
  accountId?: number
  tenderName: string
  tenderNo?: string
  tenderForm?: string
  bidEntity?: number
  province?: string
  city?: string
  regionOrgId?: number
  priceCap?: number
  openDate?: string
  deadline?: string
  deposit?: number
  depositStatus?: string
  bidStatus?: string
  winAmount?: number
  winResult?: string
  bidPlan?: string
  tenderUrl?: string
  winUrl?: string
  isWon?: number
  ownerUserId?: number
  ownerOrgId?: number
  ownerUserName?: string
  lostReason?: string
  abandonReason?: string
  genOpportunityId?: number
  genContractId?: number
  remark?: string
  createdTime?: string
}

export interface CrmClaimVO {
  id: number
  accountId: number
  accountName?: string
  applicantId?: number
  applicantName?: string
  applicantOrgId?: number
  oldOwnerId?: number
  oldOwnerName?: string
  status?: string
  flowInstanceId?: string
  appealDeadline?: string
  appealStatus?: string
  applyReason?: string
  remark?: string
  createdTime?: string
  poolStatus?: string
}

export interface FunnelStage {
  stage: string
  stageLabel: string
  count: number
  amount: number
  convertRate: number
}

export interface DupHit {
  type: string
  id: number
  name?: string
  companyName?: string
  ownerUserName?: string
  poolStatus?: string
  mobileMasked?: string
}

export interface DupCheckResult {
  duplicated: boolean
  hits: DupHit[]
}

export interface OpeningSource {
  sourceType: string
  sourceId: number
  sourceNo?: string
  sourceName?: string
  accountId?: number
  accountName?: string
  productId?: number
  productName?: string
  businessGroupId?: number
  businessGroupName?: string
  amount?: number
  currency?: string
  pricingMode?: string
  pricingDetail?: string
  startDate?: string
  endDate?: string
  ownerUserId?: number
  ownerUserName?: string
  orgId?: number
}

const q = (params: object) => params as Record<string, unknown>

// ---------------- 线索 ----------------
export const leadApi = {
  page: (params: CrmQuery) => get<PageResult<CrmLead>>('/crm/leads', q(params)),
  detail: (id: number) => get<CrmLead>(`/crm/leads/${id}`),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/crm/leads', data),
  update: (id: number, data: Record<string, unknown>) => put<void>(`/crm/leads/${id}`, data),
  remove: (id: number) => del<void>(`/crm/leads/${id}`),
  convert: (id: number, data: Record<string, unknown>) =>
    post<{ idempotent: boolean; accountId: number; contactId: number; opportunityId: number }>(
      `/crm/leads/${id}/convert`,
      data
    ),
  invalid: (id: number, reason: string) => put<void>(`/crm/leads/${id}/invalid?reason=${encodeURIComponent(reason)}`),
  changeOwner: (id: number, ownerUserId: number) => put<void>(`/crm/leads/${id}/owner`, { ownerUserId })
}

// ---------------- 客户 ----------------
export const accountApi = {
  page: (params: CrmQuery) => get<PageResult<CrmAccount>>('/crm/accounts', q(params)),
  detail: (id: number) => get<CrmAccount>(`/crm/accounts/${id}`),
  tabCounts: (id: number) => get<Record<string, number>>(`/crm/accounts/${id}/tab-counts`),
  relations: (id: number) => get<Record<string, unknown>>(`/crm/accounts/${id}/relations`),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/crm/accounts', data),
  update: (id: number, data: Record<string, unknown>) => put<void>(`/crm/accounts/${id}`, data),
  remove: (id: number) => del<void>(`/crm/accounts/${id}`),
  pool: (params: CrmQuery) => get<PageResult<CrmAccount>>('/crm/accounts/pool', q(params)),
  toPool: (id: number, reason?: string) =>
    put<void>(`/crm/accounts/${id}/to-pool${reason ? `?reason=${encodeURIComponent(reason)}` : ''}`),
  churn: (id: number, reason: string) =>
    put<void>(`/crm/accounts/${id}/churn?reason=${encodeURIComponent(reason)}`),
  revive: (id: number, reason?: string) =>
    put<void>(`/crm/accounts/${id}/revive${reason ? `?reason=${encodeURIComponent(reason)}` : ''}`),
  claim: (id: number, reason?: string) =>
    post<{ applyId: number }>(`/crm/accounts/${id}/claim${reason ? `?reason=${encodeURIComponent(reason)}` : ''}`),
  changeOwner: (id: number, ownerUserId: number) => put<void>(`/crm/accounts/${id}/owner`, { ownerUserId })
}

// ---------------- 联系人 ----------------
export const contactApi = {
  page: (params: CrmQuery) => get<PageResult<CrmContact>>('/crm/contacts', q(params)),
  detail: (id: number) => get<CrmContact>(`/crm/contacts/${id}`),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/crm/contacts', data),
  update: (id: number, data: Record<string, unknown>) => put<void>(`/crm/contacts/${id}`, data),
  remove: (id: number) => del<void>(`/crm/contacts/${id}`),
  setPrimary: (id: number) => put<void>(`/crm/contacts/${id}/primary`)
}

// ---------------- 商机 ----------------
export const opportunityApi = {
  page: (params: CrmQuery) => get<PageResult<CrmOpportunity>>('/crm/opportunities', q(params)),
  detail: (id: number) => get<CrmOpportunity>(`/crm/opportunities/${id}`),
  funnel: () => get<FunnelStage[]>('/crm/opportunities/funnel'),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/crm/opportunities', data),
  update: (id: number, data: Record<string, unknown>) => put<void>(`/crm/opportunities/${id}`, data),
  remove: (id: number) => del<void>(`/crm/opportunities/${id}`),
  stage: (id: number, targetStage: string, reason?: string) =>
    put<void>(`/crm/opportunities/${id}/stage`, { targetStage, reason }),
  lost: (id: number, reason: string) =>
    put<void>(`/crm/opportunities/${id}/lost?reason=${encodeURIComponent(reason)}`),
  changeOwner: (id: number, ownerUserId: number) => put<void>(`/crm/opportunities/${id}/owner`, { ownerUserId }),
  competitors: (id: number) => get<CrmCompetitor[]>(`/crm/opportunities/${id}/competitors`),
  compare: (id: number) => get<Record<string, unknown>>(`/crm/opportunities/${id}/competitors/compare`),
  addCompetitor: (id: number, data: Record<string, unknown>) =>
    post<{ id: number }>(`/crm/opportunities/${id}/competitors`, data),
  updateCompetitor: (opId: number, id: number, data: Record<string, unknown>) =>
    put<void>(`/crm/opportunities/${opId}/competitors/${id}`, data),
  removeCompetitor: (opId: number, id: number) => del<void>(`/crm/opportunities/${opId}/competitors/${id}`)
}

// ---------------- 活动 ----------------
export const activityApi = {
  page: (params: CrmQuery) => get<PageResult<CrmActivity>>('/crm/activities', q(params)),
  mine: (params?: { startDate?: string; endDate?: string }) =>
    get<CrmActivity[]>('/crm/activities/mine', params ? q(params) : undefined),
  timeline: (targetType: string, targetId: number) =>
    get<CrmActivity[]>('/crm/activities/timeline', { targetType, targetId }),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/crm/activities', data),
  update: (id: number, data: Record<string, unknown>) => put<void>(`/crm/activities/${id}`, data),
  void: (id: number, reason: string) =>
    put<void>(`/crm/activities/${id}/void?reason=${encodeURIComponent(reason)}`)
}

// ---------------- 合同 ----------------
export const contractApi = {
  page: (params: CrmQuery) => get<PageResult<CrmContract>>('/crm/contracts', q(params)),
  detail: (id: number) => get<CrmContract>(`/crm/contracts/${id}`),
  expiring: (days = 30) => get<CrmContract[]>('/crm/contracts/expiring', { days }),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/crm/contracts', data),
  update: (id: number, data: Record<string, unknown>) => put<void>(`/crm/contracts/${id}`, data),
  remove: (id: number) => del<void>(`/crm/contracts/${id}`),
  submit: (id: number) => post<Record<string, string>>(`/crm/contracts/${id}/submit`),
  withdraw: (id: number, reason?: string) =>
    post<void>(`/crm/contracts/${id}/withdraw${reason ? `?reason=${encodeURIComponent(reason)}` : ''}`),
  archive: (id: number, data: { paperBoxNo?: string; paperCopies?: number }) =>
    post<void>(`/crm/contracts/${id}/archive`, data),
  renew: (id: number, data: Record<string, unknown>) => post<{ id: number }>(`/crm/contracts/${id}/renew`, data),
  terminate: (id: number, reason: string) =>
    post<void>(`/crm/contracts/${id}/terminate?reason=${encodeURIComponent(reason)}`)
}

// ---------------- 协议 ----------------
export const agreementApi = {
  page: (params: CrmQuery) => get<PageResult<CrmAgreement>>('/crm/agreements', q(params)),
  detail: (id: number) => get<CrmAgreement>(`/crm/agreements/${id}`),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/crm/agreements', data),
  update: (id: number, data: Record<string, unknown>) => put<void>(`/crm/agreements/${id}`, data),
  remove: (id: number) => del<void>(`/crm/agreements/${id}`),
  submit: (id: number) => post<Record<string, string>>(`/crm/agreements/${id}/submit`),
  withdraw: (id: number, reason?: string) =>
    post<void>(`/crm/agreements/${id}/withdraw${reason ? `?reason=${encodeURIComponent(reason)}` : ''}`),
  archive: (id: number, data: { paperBoxNo?: string; paperCopies?: number }) =>
    post<void>(`/crm/agreements/${id}/archive`, data),
  renew: (id: number, data: Record<string, unknown>) => post<{ id: number }>(`/crm/agreements/${id}/renew`, data)
}

// ---------------- 投标 ----------------
export const bidApi = {
  page: (params: CrmQuery) => get<PageResult<CrmBid>>('/crm/bids', q(params)),
  detail: (id: number) => get<CrmBid>(`/crm/bids/${id}`),
  calendar: (month?: string) => get<CrmBid[]>('/crm/bids/calendar', month ? { month } : undefined),
  openingSoon: (days = 3) => get<CrmBid[]>('/crm/bids/opening-soon', { days }),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/crm/bids', data),
  update: (id: number, data: Record<string, unknown>) => put<void>(`/crm/bids/${id}`, data),
  remove: (id: number) => del<void>(`/crm/bids/${id}`),
  status: (id: number, data: { targetStatus: string; winAmount?: number; reason?: string }) =>
    put<void>(`/crm/bids/${id}/status`, data),
  genOpportunity: (id: number) => post<{ id: number }>(`/crm/bids/${id}/gen-opportunity`),
  genContract: (id: number, data: { companyId?: number; productId?: number }) =>
    post<{ id: number }>(`/crm/bids/${id}/gen-contract`, data)
}

// ---------------- 认领审批 ----------------
export const claimApi = {
  page: (params: { status?: string; keyword?: string; pageNum?: number; pageSize?: number }) =>
    get<PageResult<CrmClaimVO>>('/crm/claims', q(params)),
  pendingCount: () => get<{ count: number }>('/crm/claims/pending-count')
}

// ---------------- 公共能力 ----------------
export const crmCommonApi = {
  checkDuplicate: (params: {
    type?: string
    mobile?: string
    email?: string
    creditCode?: string
    companyName?: string
    province?: string
  }) => get<DupCheckResult>('/crm/common/check-duplicate', q(params)),
  importTemplate: (bizType: string) => download('/crm/common/import-template', `${bizType}-template.xlsx`, { bizType }),
  import: (bizType: string, fileId: string) => post<Record<string, unknown>>('/crm/common/import', { bizType, fileId }),
  importBatch: (batchId: number) => get<Record<string, unknown>>(`/crm/common/imports/${batchId}`),
  summary: () =>
    get<{ myLeads: number; opportunityAmount: number; pendingContracts: number; monthBids: number }>(
      '/crm/common/stats/summary'
    ),
  funnel: () => get<{ newLeads: number; convertedLeads: number; leadConvertRate: number; stages: FunnelStage[] }>(
    '/crm/common/stats/funnel'
  ),
  products: () => get<{ degraded: boolean; list: unknown[]; message?: string }>('/crm/common/products')
}

export const openingSourceApi = {
  opportunities: () => get<OpeningSource[]>('/crm/opening-sources/opportunities'),
  bids: () => get<OpeningSource[]>('/crm/opening-sources/bids'),
  contracts: () => get<OpeningSource[]>('/crm/opening-sources/contracts'),
  agreements: () => get<OpeningSource[]>('/crm/opening-sources/agreements'),
  byId: (sourceType: string, sourceId: number) =>
    get<OpeningSource>('/crm/opening-sources/by-id', { sourceType, sourceId })
}
