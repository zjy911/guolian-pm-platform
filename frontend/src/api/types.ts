/**
 * 统一响应体与分页结构。
 */
export interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface PageQuery {
  pageNum?: number
  pageSize?: number
}

export interface MenuNode {
  id: number
  parentId: number
  name: string
  path?: string
  component?: string
  icon?: string
  type: number
  perms?: string
  children: MenuNode[]
}

export interface RoleOption {
  roleId: number
  roleCode: string
  roleName: string
  orgId: number
  orgName: string
  dataScope: string
  dataScopeLabel: string
  defaultRole: boolean
  current: boolean
}

export interface Profile {
  userId: number
  username: string
  realName: string
  avatar?: string
  roleId: number
  roleCode: string
  roleName: string
  orgId: number
  orgName: string
  orgPath: string
  companyNames: string
  dataScope: string
  dataScopeLabel: string
  impersonate: boolean
  impersonateBy?: string
  readOnly: boolean
  firstLogin: boolean
  perms: string[]
}

export interface AuthResult {
  token: string
  profile: Profile
  menus: MenuNode[]
  roles: RoleOption[]
  todoCount: number
  unreadCount: number
}

export interface UserRoleBrief {
  userId: number
  roleId: number
  roleCode: string
  roleName: string
  orgId: number
  orgName: string
  isDefault: number
  dataScope: string
}

export interface UserVO {
  id: number
  username: string
  realName: string
  employeeNo?: string
  mobile?: string
  email?: string
  idCard?: string
  gender?: number
  employeeType?: string
  employmentForm?: string
  position?: string
  jobLevel?: string
  joinDate?: string
  leaveDate?: string
  status: number
  defaultRoleId?: number
  orgId?: number
  orgName?: string
  orgPath?: string
  companyNames?: string
  lastLoginTime?: string
  remark?: string
  roles: UserRoleBrief[]
  companies: { companyId: number; companyName: string; relationType: number }[]
}

export interface RoleVO {
  id: number
  roleCode: string
  roleName: string
  orgId: number
  orgName: string
  orgPath: string
  description?: string
  roleType: number
  dataScope: string
  dataScopeLabel: string
  scopeValue?: string
  canBeProjectMember: number
  canBeApprover: number
  sortNo: number
  status: number
  remark?: string
  userCount: number
  menuIds: number[]
}

export interface OrgVO {
  id: number
  orgCode: string
  orgName: string
  orgType: string
  parentId: number
  ancestors?: string
  leaderId?: number
  costCenterCode?: string
  sortNo: number
  status: number
  roleCount: number
  userCount: number
  children: OrgVO[]
  roles: { id: number; roleCode: string; roleName: string; userCount: number; dataScope?: string }[]
  users: { id: number; realName: string; username: string; employeeNo?: string; status?: number }[]
}

export interface CompanyVO {
  id: number
  companyCode: string
  companyName: string
  shortName?: string
  companyType?: string
  legalPerson?: string
  creditCode?: string
  regAddress?: string
  officeAddress?: string
  taxNo?: string
  bankName?: string
  bankAccount?: string
  status: number
  userCount: number
}

export interface NoticeVO {
  id: number
  title: string
  noticeType: number
  content?: string
  receiverId?: number
  pinned: number
  status: number
  publishTime?: string
  todoStatus?: number
}

export interface TransferVO {
  id: number
  transferNo: string
  fromUserId: number
  fromUserName: string
  toUserIds: number[]
  toUserNames: string
  assignMode: number
  scope: string[]
  remark?: string
  flowStatus: string
  execStatus: string
  flowInstanceId?: string
  itemCount: number
  failCount: number
  revocable: boolean
  revokeDeadline?: string
  createdTime?: string
  effectiveTime?: string
}

export interface TransferItemVO {
  id: number
  bizType: string
  bizId: number
  bizName: string
  beforeOwnerName?: string
  afterOwnerName?: string
  result: string
  failReason?: string
}

export interface FlowTaskVO {
  taskId: string
  taskName: string
  processInstanceId: string
  bizType: string
  bizNo: string
  bizTitle: string
  initiatorName?: string
  startTime?: string
  detailRoute?: string
}

export interface FlowHistoryVO {
  taskId: string
  taskName: string
  assigneeName?: string
  startTime?: string
  endTime?: string
  duration?: string
  comment?: string
  finished: boolean
}

export interface FlowInstanceVO {
  processInstanceId: string
  bizType: string
  bizNo: string
  bizTitle: string
  status: string
  currentNodeName: string
  initiatorName?: string
  startTime?: string
  endTime?: string
  detailRoute?: string
}

export interface SensitiveGroup {
  id: number
  groupCode: string
  groupName: string
  defaultLevel: string
  riskLevel: string
  sortNo: number
  status: number
  remark?: string
}

export interface SensitiveField {
  id: number
  groupCode: string
  bizObject: string
  fieldName: string
  fieldLabel: string
  maskRule: string
  maskParam?: string
  allowPlain: number
  status: number
}

export interface DictType {
  id: number
  dictType: string
  dictName: string
  status: number
  remark?: string
}

export interface DictItem {
  id: number
  dictType: string
  dictLabel: string
  dictValue: string
  sortNo: number
  status: number
}

export interface SysConfigItem {
  id: number
  configKey: string
  configValue: string
  configName: string
  configType: string
  builtin: number
  remark?: string
}

export interface OnlineSession {
  sessionId: string
  userId: number
  username: string
  realName: string
  roleName: string
  orgName: string
  ip: string
  loginTime: string
  lastActiveTime: string
  impersonate: boolean
}
