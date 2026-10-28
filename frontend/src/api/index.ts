import { del, download, get, post, put } from './request'
import type {
  AuthResult,
  CompanyVO,
  DictItem,
  DictType,
  FlowHistoryVO,
  FlowInstanceVO,
  FlowTaskVO,
  MenuNode,
  NoticeVO,
  OnlineSession,
  OrgVO,
  PageResult,
  PageQuery,
  RoleOption,
  RoleVO,
  SensitiveField,
  SensitiveGroup,
  SysConfigItem,
  TransferItemVO,
  TransferVO,
  UserVO
} from './types'

// ---------------- 认证 ----------------
export const authApi = {
  login: (username: string, password: string) =>
    post<AuthResult>('/auth/login', { username, password }),
  switchRole: (roleId: number) => post<AuthResult>('/auth/switch-role', { roleId }),
  logout: () => post<void>('/auth/logout'),
  userInfo: () => get<AuthResult>('/auth/user-info'),
  menus: () => get<MenuNode[]>('/auth/menus'),
  myRoles: () => get<RoleOption[]>('/auth/my-roles'),
  changePassword: (oldPassword: string, newPassword: string) =>
    post<void>('/auth/change-password', { oldPassword, newPassword }),
  impersonate: (userId: number) => post<AuthResult>('/system/debug/impersonate', { userId }),
  dataScopeSql: () => get<Record<string, unknown>>('/system/debug/data-scope-sql')
}

// ---------------- 用户 ----------------
export interface UserQuery extends PageQuery {
  keyword?: string
  orgId?: number | null
  roleIds?: number[]
  roleMatch?: string
  companyId?: number | null
  statuses?: number[]
  employeeType?: string
}

export const userApi = {
  page: (params: UserQuery) => get<PageResult<UserVO>>('/system/users', params as Record<string, unknown>),
  detail: (id: number) => get<UserVO>(`/system/users/${id}`),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/system/users', data),
  update: (id: number, data: Record<string, unknown>) => put<void>(`/system/users/${id}`, data),
  remove: (id: number) => del<void>(`/system/users/${id}`),
  assignRoles: (id: number, roleIds: number[], defaultRoleId: number) =>
    put<void>(`/system/users/${id}/roles`, { roleIds, defaultRoleId }),
  updateStatus: (id: number, status: number, leaveDate?: string) =>
    put<void>(`/system/users/${id}/status`, { status, leaveDate }),
  resetPassword: (id: number) => post<{ password: string }>(`/system/users/${id}/reset-password`),
  projects: (id: number) => get<Record<string, unknown>[]>(`/system/users/${id}/projects`),
  exportCsv: (params: UserQuery) => download('/system/users/export', 'users.csv', params as Record<string, unknown>)
}

// ---------------- 角色 ----------------
export const roleApi = {
  page: (params: PageQuery & { keyword?: string; orgId?: number | null; status?: number | null }) =>
    get<PageResult<RoleVO>>('/system/roles', params as Record<string, unknown>),
  options: (orgId?: number) => get<RoleVO[]>('/system/roles/options', orgId ? { orgId } : undefined),
  detail: (id: number) => get<RoleVO>(`/system/roles/${id}`),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/system/roles', data),
  update: (id: number, data: Record<string, unknown>) => put<void>(`/system/roles/${id}`, data),
  remove: (id: number) => del<void>(`/system/roles/${id}`),
  saveMenus: (id: number, menuIds: number[]) => put<void>(`/system/roles/${id}/menus`, { menuIds }),
  saveDataScope: (id: number, data: Record<string, unknown>) =>
    put<void>(`/system/roles/${id}/data-scope`, data),
  fieldMasks: (id: number) => get<Record<string, string>>(`/system/roles/${id}/field-mask`),
  saveFieldMasks: (id: number, masks: { groupCode: string; maskLevel: string }[]) =>
    put<void>(`/system/roles/${id}/field-mask`, { masks }),
  users: (id: number) => get<Record<string, unknown>[]>(`/system/roles/${id}/users`)
}

// ---------------- 组织 / 主体 / 菜单 ----------------
export const orgApi = {
  tree: (onlyEnabled?: boolean) => get<OrgVO[]>('/system/orgs/tree', onlyEnabled ? { onlyEnabled } : undefined),
  detail: (id: number) => get<OrgVO>(`/system/orgs/${id}`),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/system/orgs', data),
  update: (id: number, data: Record<string, unknown>) => put<void>(`/system/orgs/${id}`, data),
  move: (id: number, parentId: number) => put<void>(`/system/orgs/${id}/move`, { parentId }),
  remove: (id: number) => del<void>(`/system/orgs/${id}`)
}

export const companyApi = {
  page: (params: PageQuery & { keyword?: string; status?: number | null }) =>
    get<PageResult<CompanyVO>>('/system/companies', params as Record<string, unknown>),
  options: () => get<CompanyVO[]>('/system/companies/options'),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/system/companies', data),
  update: (id: number, data: Record<string, unknown>) => put<void>(`/system/companies/${id}`, data),
  remove: (id: number) => del<void>(`/system/companies/${id}`)
}

export const menuApi = {
  tree: () => get<MenuNode[]>('/system/menus/tree'),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/system/menus', data),
  update: (id: number, data: Record<string, unknown>) => put<void>(`/system/menus/${id}`, data),
  remove: (id: number) => del<void>(`/system/menus/${id}`)
}

// ---------------- 字典 / 参数 ----------------
export const dictApi = {
  types: (params: PageQuery & { keyword?: string }) =>
    get<PageResult<DictType>>('/system/dicts/types', params as Record<string, unknown>),
  items: (dictType: string) => get<DictItem[]>(`/system/dicts/types/${dictType}/items`),
  data: (types: string) => get<Record<string, DictItem[]>>('/system/dicts/data', { types }),
  createType: (data: Record<string, unknown>) => post<{ id: number }>('/system/dicts/types', data),
  updateType: (id: number, data: Record<string, unknown>) => put<void>(`/system/dicts/types/${id}`, data),
  removeType: (id: number) => del<void>(`/system/dicts/types/${id}`),
  createItem: (data: Record<string, unknown>) => post<{ id: number }>('/system/dicts/items', data),
  updateItem: (id: number, data: Record<string, unknown>) => put<void>(`/system/dicts/items/${id}`, data),
  removeItem: (id: number) => del<void>(`/system/dicts/items/${id}`)
}

export const configApi = {
  list: (keyword?: string) => get<SysConfigItem[]>('/system/configs', keyword ? { keyword } : undefined),
  update: (id: number, configValue: string, remark?: string) =>
    put<void>(`/system/configs/${id}`, { configValue, remark }),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/system/configs', data),
  remove: (id: number) => del<void>(`/system/configs/${id}`)
}

// ---------------- 通知 / 日志 / 在线 ----------------
export const noticeApi = {
  page: (params: PageQuery & { keyword?: string; noticeType?: number | null }) =>
    get<PageResult<NoticeVO>>('/system/notices', params as Record<string, unknown>),
  mine: (noticeType?: number) => get<NoticeVO[]>('/system/notices/mine', noticeType ? { noticeType } : undefined),
  todos: () => get<NoticeVO[]>('/system/notices/todos'),
  badge: () => get<{ todoCount: number; unreadCount: number }>('/system/notices/badge'),
  read: (id: number) => post<void>(`/system/notices/${id}/read`),
  readAll: () => post<void>('/system/notices/read-all'),
  completeTodo: (id: number) => post<void>(`/system/notices/todos/${id}/complete`),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/system/notices', data),
  update: (id: number, data: Record<string, unknown>) => put<void>(`/system/notices/${id}`, data),
  remove: (id: number) => del<void>(`/system/notices/${id}`)
}

export const logApi = {
  oper: (params: PageQuery & { keyword?: string; module?: string; success?: number | null }) =>
    get<PageResult<Record<string, unknown>>>('/system/logs/oper', params as Record<string, unknown>),
  login: (params: PageQuery & { keyword?: string; loginType?: string }) =>
    get<PageResult<Record<string, unknown>>>('/system/logs/login', params as Record<string, unknown>)
}

export const onlineApi = {
  list: () => get<OnlineSession[]>('/system/online'),
  forceLogout: (sessionId: string) => post<void>(`/system/online/${sessionId}/logout`)
}

export const fileApi = {
  page: (params: PageQuery & { keyword?: string }) =>
    get<PageResult<Record<string, unknown>>>('/system/files', params as Record<string, unknown>),
  remove: (id: number) => del<void>(`/system/files/${id}`),
  downloadUrl: (id: number) => `/api/v1/system/files/${id}/download`
}

// ---------------- 脱敏 ----------------
export const sensitiveApi = {
  groups: () => get<SensitiveGroup[]>('/system/sensitive/groups'),
  fields: (groupCode?: string) =>
    get<SensitiveField[]>('/system/sensitive/fields', groupCode ? { groupCode } : undefined),
  whitelist: () => get<Record<string, { fieldName: string; fieldLabel: string; groupCode: string; maskRule: string }[]>>(
    '/system/sensitive/whitelist'
  ),
  createGroup: (data: Record<string, unknown>) => post<{ id: number }>('/system/sensitive/groups', data),
  updateGroup: (id: number, data: Record<string, unknown>) => put<void>(`/system/sensitive/groups/${id}`, data),
  removeGroup: (id: number) => del<void>(`/system/sensitive/groups/${id}`),
  createField: (data: Record<string, unknown>) => post<{ id: number }>('/system/sensitive/fields', data),
  updateField: (id: number, data: Record<string, unknown>) => put<void>(`/system/sensitive/fields/${id}`, data),
  removeField: (id: number) => del<void>(`/system/sensitive/fields/${id}`),
  plain: (bizType: string, bizId: number, group: string) =>
    get<{ value: string }>('/system/sensitive/plain', { bizType, bizId, group })
}

// ---------------- 审批中心 ----------------
export const flowApi = {
  defs: (params?: { bizType?: string; status?: number }) =>
    get<Record<string, unknown>[]>('/system/flows', params),
  createDef: (data: Record<string, unknown>) => post<{ id: number }>('/system/flows', data),
  updateDef: (id: number, data: Record<string, unknown>) => put<void>(`/system/flows/${id}`, data),
  publishDef: (id: number) => post<Record<string, unknown>>(`/system/flows/${id}/publish`),
  removeDef: (id: number) => del<void>(`/system/flows/${id}`),
  todo: () => get<FlowTaskVO[]>('/flow/tasks/todo'),
  done: () => get<FlowTaskVO[]>('/flow/tasks/done'),
  mine: (status?: string) => get<FlowInstanceVO[]>('/flow/instances/mine', status ? { status } : undefined),
  cc: () => get<FlowInstanceVO[]>('/flow/instances/cc'),
  history: (instanceId: string) => get<FlowHistoryVO[]>(`/flow/instances/${instanceId}/history`),
  approve: (taskId: string, comment: string) => post<void>(`/flow/tasks/${taskId}/approve`, { comment }),
  reject: (taskId: string, comment: string, targetNodeKey?: string) =>
    post<void>(`/flow/tasks/${taskId}/reject`, { comment, targetNodeKey }),
  transfer: (taskId: string, userId: number, comment: string) =>
    post<void>(`/flow/tasks/${taskId}/transfer`, { userId, comment }),
  withdraw: (instanceId: string, reason: string) =>
    post<void>(`/flow/instances/${instanceId}/withdraw`, { reason })
}

// ---------------- 账号交接 ----------------
export const transferApi = {
  page: (params: PageQuery & { flowStatus?: string; execStatus?: string; keyword?: string }) =>
    get<PageResult<TransferVO>>('/system/transfers', params as Record<string, unknown>),
  detail: (id: number) => get<TransferVO>(`/system/transfers/${id}`),
  items: (id: number) => get<TransferItemVO[]>(`/system/transfers/${id}/items`),
  adapters: () =>
    get<{ bizType: string; bizName: string; count: number; available: boolean }[]>('/system/transfers/adapters'),
  precheck: (fromUserId: number) =>
    get<{
      fromUserId: number
      fromUserName: string
      roles: { roleId: number; roleCode: string; roleName: string; orgName: string; default: boolean }[]
      adapters: { bizType: string; bizName: string; count: number; available: boolean }[]
      totalCount: number
    }>('/system/transfers/precheck', { fromUserId }),
  receivers: (fromUserId: number, roleId: number) =>
    get<{ userId: number; realName: string; username: string; orgName: string }[]>(
      '/system/transfers/eligible-receivers',
      { fromUserId, roleId }
    ),
  create: (data: Record<string, unknown>) => post<{ id: number }>('/system/transfers', data),
  submit: (id: number) => post<{ processInstanceId: string }>(`/system/transfers/${id}/submit`),
  execute: (id: number) => post<void>(`/system/transfers/${id}/execute`),
  retry: (id: number) => post<void>(`/system/transfers/${id}/retry`),
  revoke: (id: number, reason: string) => post<void>(`/system/transfers/${id}/revoke`, { reason })
}
