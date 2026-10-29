import { del, get, post, put } from './request'
import type { PageResult } from './types'

// ---------------- 产品与模板（设计态） ----------------

export interface PmProduct {
  id: number
  productCode: string
  productName: string
  bgOrgId: number | null
  bgOrgName: string | null
  pricingMode: string
  templateVersion: string
  status: string
  remark: string | null
}

export interface PmTemplateSection {
  phase: number
  phaseName: string
  category: string
  tasks: { name: string; type: string; formCode: string; blocking: boolean }[]
}

export const productApi = {
  page: (params?: Record<string, unknown>) => get<PageResult<PmProduct>>('/project/products', params),
  detail: (id: number) =>
    get<{ product: PmProduct; template: { templateJson: string; version: number } | null }>(`/project/products/${id}`),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/project/products', data),
  update: (id: number, data: Record<string, unknown>) => put<void>(`/project/products/${id}`, data),
  publishTemplate: (id: number, templateJson: string) =>
    post<{ version: number }>(`/project/products/${id}/template/publish`, { templateJson })
}

// ---------------- 项目（运行态） ----------------

export interface PmProject {
  id: number
  projectCode: string
  projectName: string
  productId: number
  productName: string
  templateVersion: string
  customerId: number
  customerName: string
  contractNo: string | null
  signEntity: string | null
  estimatedAmount: number | null
  annualCases: number | null
  pricingMode: string
  rating: string
  phase: number
  status: string
  healthScore: number | null
  healthLight: string
  grossMargin: number | null
  ownerUserName: string
  createdTime?: string
}

export interface PmProjectTask {
  id: number
  projectId: number
  phase: number
  category: string
  taskName: string
  taskType: 'FORM' | 'DATA' | 'COLLAB'
  formCode: string | null
  blocking: number
  status: string
  assigneeName: string | null
  dueDate: string | null
  sortNo: number
}

export interface PmFormInstance {
  id: number
  projectId: number | null
  taskId: number | null
  formCode: string
  formData: string
  status: string
  submitUserName: string | null
  submitTime: string | null
  approveTime: string | null
  approveComment: string | null
}

export interface PmMember {
  id: number
  projectId: number
  userId: number
  userName: string
  projectRole: string
  inputRatio: number
  joinDate: string | null
  expectedLeaveDate: string | null
  status: string
  evaluation: string | null
}

export const projectApi = {
  page: (params?: Record<string, unknown>) => get<PageResult<PmProject>>('/project/projects', params),
  detail: (id: number) => get<PmProject>(`/project/projects/${id}`),
  workspace: (id: number) =>
    get<{ project: PmProject; tasks: PmProjectTask[]; logs: { action: string; detail: string; operatorName: string; createdTime: string }[] }>(
      `/project/projects/${id}/workspace`
    ),
  createInitiation: (data: Record<string, unknown>) =>
    post<{ projectId: number; formId: number }>('/project/projects/initiations', data),
  advance: (id: number) => post<PmProject>(`/project/projects/${id}/advance`),
  members: (id: number) => get<PmMember[]>(`/project/projects/${id}/members`),
  assignMember: (projectId: number, userId: number, data: Record<string, unknown>) =>
    post<{ memberId: number }>(`/project/projects/${projectId}/members/${userId}`, data),
  releaseMember: (projectId: number, memberId: number, evaluation?: string) =>
    del<void>(`/project/projects/${projectId}/members/${memberId}${evaluation ? `?evaluation=${encodeURIComponent(evaluation)}` : ''}`),
  taskForm: (taskId: number) => get<PmFormInstance>(`/project/projects/tasks/${taskId}/form`),
  saveForm: (formId: number, formData: string) => put<void>(`/project/projects/forms/${formId}`, { formData }),
  submitForm: (formId: number) => post<{ flowInstanceId: string }>(`/project/projects/forms/${formId}/submit`)
}

// ---------------- 用户关联项目（系统管理·用户管理） ----------------

export interface UserProjectView {
  memberId: number
  projectId: number
  projectCode: string
  projectName: string
  productName: string
  projectRole: string
  projectRoleLabel: string
  inputRatio: number
  joinDate: string | null
  expectedLeaveDate: string | null
  status: string
  active: boolean
  evaluation: string | null
  openTaskCount: number
}

export const userProjectApi = {
  list: (userId: number) => get<UserProjectView[]>(`/system/users/${userId}/projects`),
  ratio: (userId: number) => get<{ activeRatio: number }>(`/system/users/${userId}/projects/ratio`),
  assign: (userId: number, data: Record<string, unknown>) =>
    post<{ memberId: number }>(`/system/users/${userId}/projects`, data),
  updateMember: (projectId: number, memberId: number, data: Record<string, unknown>) =>
    put<void>(`/project/projects/${projectId}/members/${memberId}`, data),
  release: (projectId: number, memberId: number, evaluation?: string) =>
    del<void>(`/project/projects/${projectId}/members/${memberId}${evaluation ? `?evaluation=${encodeURIComponent(evaluation)}` : ''}`)
}

// ---------------- 驾驶舱 ----------------

export interface CockpitSummary {
  total: number
  byPhase: number[]
  phaseNames: string[]
  byLight: Record<string, number>
  estimatedAmountTotal: number
  avgHealth: number
  openTaskCount: number
  alerts: PmProject[]
}

export const cockpitApi = {
  summary: () => get<CockpitSummary>('/project/cockpit/summary')
}

// ---------------- 枚举文案 ----------------

export const PM_PHASES = ['项目立项', '项目筹备', '项目实施', '项目交接', '项目运营', '项目撤场']

export const PM_STATUS: Record<string, string> = {
  INITIATING: '立项中',
  PREPARING: '筹备中',
  IMPLEMENTING: '实施中',
  PENDING_HANDOVER: '交接中',
  OPERATING: '运营中',
  CLOSING: '撤场中',
  CLOSED: '已撤场',
  ARCHIVED: '已归档'
}

export const PM_TASK_STATUS: Record<string, string> = {
  NOT_STARTED: '未启动',
  IN_PROGRESS: '进行中',
  APPROVING: '审批中',
  DONE: '已完成',
  REJECTED: '已驳回'
}

export const PM_PROJECT_ROLE: Record<string, string> = {
  PROJECT_MANAGER: '项目经理',
  IMPLEMENT_LEADER: '实施主管',
  FIELD_MEDIATOR: '驻场调解员',
  BUSINESS_REP: '商务代表',
  QC: '质检',
  OBSERVER: '只读观察者'
}

export const PM_PRICING: Record<string, string> = {
  FREE: '免费',
  TOTAL_PACKAGE: '总额包干',
  UNIT_PRICE: '单价计数',
  BY_TIME: '按时间',
  BY_HEAD: '按人员个数',
  COMMISSION: '抽成',
  BY_EFFECT: '按效果'
}

export function healthTag(light?: string) {
  if (light === 'GREEN') return 'is-success'
  if (light === 'YELLOW') return 'is-warning'
  if (light === 'RED') return 'is-danger'
  return 'is-muted'
}

export function taskStatusTag(status?: string) {
  if (status === 'DONE') return 'is-success'
  if (status === 'APPROVING') return 'is-info'
  if (status === 'REJECTED') return 'is-danger'
  if (status === 'IN_PROGRESS') return 'is-warning'
  return 'is-muted'
}
