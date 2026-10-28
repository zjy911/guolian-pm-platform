<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import Pagination from '@/components/Pagination.vue'
import { companyApi, dictApi, orgApi, roleApi, userApi } from '@/api'
import { authApi } from '@/api'
import type { CompanyVO, DictItem, OrgVO, RoleVO, UserVO } from '@/api/types'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const route = useRoute()

const list = ref<UserVO[]>([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)

const query = reactive({
  keyword: '',
  orgId: null as number | null,
  statuses: [] as number[],
  employeeType: ''
})

const orgs = ref<OrgVO[]>([])
const flatOrgs = ref<{ id: number; name: string }[]>([])
const roles = ref<RoleVO[]>([])
const companies = ref<CompanyVO[]>([])
const employeeTypes = ref<DictItem[]>([])
const statuses = ref<DictItem[]>([])

const formOpen = ref(false)
const detailOpen = ref(false)
const saving = ref(false)
const detail = ref<UserVO | null>(null)
const detailTab = ref<'roles' | 'companies' | 'projects' | 'transfers'>('roles')
const resetPasswordVisible = ref(false)
const resetPasswordValue = ref('')

const form = reactive<Record<string, any>>({
  id: null as number | null,
  username: '',
  realName: '',
  employeeNo: '',
  mobile: '',
  email: '',
  gender: 1,
  idCard: '',
  employeeType: 'FULLTIME',
  employmentForm: 'LABOR',
  position: '',
  jobLevel: '',
  joinDate: '',
  leaveDate: '',
  status: 1,
  remark: '',
  roleIds: [] as number[],
  defaultRoleId: null as number | null,
  companies: [] as { companyId: number; relationType: number; isPrimary: number }[]
})

const canAdd = computed(() => userStore.hasPerm('system:user:add'))
const canEdit = computed(() => userStore.hasPerm('system:user:edit'))
const canReset = computed(() => userStore.hasPerm('system:user:reset-pwd'))
const canStatus = computed(() => userStore.hasPerm('system:user:status'))
const canImpersonate = computed(() => userStore.hasPerm('system:debug:impersonate'))
const isReadOnly = computed(() => userStore.profile?.readOnly ?? false)

function flatten(nodes: OrgVO[], prefix = '') {
  const result: { id: number; name: string }[] = []
  nodes.forEach((node) => {
    result.push({ id: node.id, name: `${prefix}${node.orgName}` })
    result.push(...flatten(node.children || [], `${prefix}　`))
  })
  return result
}

async function load() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: query.keyword || undefined,
      orgId: query.orgId || undefined,
      employeeType: query.employeeType || undefined,
      statuses: query.statuses.length ? query.statuses.join(',') : undefined
    }
    const data = await userApi.page(params)
    list.value = data.list
    total.value = data.total
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [orgTree, roleList, companyList, dictData] = await Promise.all([
    orgApi.tree(true),
    roleApi.options(),
    companyApi.options(),
    dictApi.data('employee_type,user_status')
  ])
  orgs.value = orgTree
  flatOrgs.value = flatten(orgTree)
  roles.value = roleList
  companies.value = companyList
  employeeTypes.value = dictData.employee_type || []
  statuses.value = dictData.user_status || []
}

function resetForm() {
  Object.assign(form, {
    id: null,
    username: '',
    realName: '',
    employeeNo: '',
    mobile: '',
    email: '',
    gender: 1,
    idCard: '',
    employeeType: 'FULLTIME',
    employmentForm: 'LABOR',
    position: '',
    jobLevel: '',
    joinDate: '',
    leaveDate: '',
    status: 1,
    remark: '',
    roleIds: [],
    defaultRoleId: null,
    companies: []
  })
}

function openCreate() {
  resetForm()
  formOpen.value = true
}

function openEdit(row: UserVO) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    username: row.username,
    realName: row.realName,
    employeeNo: row.employeeNo ?? '',
    mobile: row.mobile ?? '',
    email: row.email ?? '',
    gender: row.gender ?? 1,
    idCard: row.idCard ?? '',
    employeeType: row.employeeType ?? 'FULLTIME',
    employmentForm: row.employmentForm ?? 'LABOR',
    position: row.position ?? '',
    jobLevel: row.jobLevel ?? '',
    joinDate: row.joinDate ?? '',
    leaveDate: row.leaveDate ?? '',
    status: row.status,
    remark: row.remark ?? '',
    roleIds: row.roles.map((role) => role.roleId),
    defaultRoleId: row.defaultRoleId ?? row.roles[0]?.roleId ?? null,
    companies: row.companies.map((company) => ({
      companyId: company.companyId,
      relationType: company.relationType,
      isPrimary: 0
    }))
  })
  formOpen.value = true
}

function toggleRole(roleId: number) {
  const roleIds = form.roleIds as number[]
  const index = roleIds.indexOf(roleId)
  if (index >= 0) {
    roleIds.splice(index, 1)
    if (form.defaultRoleId === roleId) {
      form.defaultRoleId = roleIds[0] ?? null
    }
  } else {
    if (roleIds.length >= 5) {
      appStore.toast('单个用户最多关联 5 个角色', 'error')
      return
    }
    roleIds.push(roleId)
    if (!form.defaultRoleId) {
      form.defaultRoleId = roleId
    }
  }
}

function toggleCompany(companyId: number) {
  const items = form.companies as { companyId: number; relationType: number; isPrimary: number }[]
  const index = items.findIndex((item) => item.companyId === companyId)
  if (index >= 0) {
    items.splice(index, 1)
  } else {
    items.push({ companyId, relationType: 1, isPrimary: 0 })
  }
}

function hasCompany(companyId: number) {
  return (form.companies as { companyId: number }[]).some((item) => item.companyId === companyId)
}

async function submit() {
  saving.value = true
  try {
    const payload: Record<string, unknown> = {
      username: form.username,
      realName: form.realName,
      employeeNo: form.employeeNo || undefined,
      mobile: form.mobile,
      email: form.email || undefined,
      gender: form.gender,
      idCard: form.idCard || undefined,
      employeeType: form.employeeType,
      employmentForm: form.employmentForm,
      position: form.position || undefined,
      jobLevel: form.jobLevel || undefined,
      joinDate: form.joinDate || undefined,
      leaveDate: form.leaveDate || undefined,
      status: form.status,
      remark: form.remark || undefined,
      roleIds: form.roleIds,
      defaultRoleId: form.defaultRoleId,
      companies: (form.companies as { companyId: number; relationType: number; isPrimary: number }[]).length
        ? form.companies
        : []
    }
    if (form.id) {
      await userApi.update(form.id as number, payload)
      appStore.toast('用户已更新')
    } else {
      await userApi.create(payload)
      appStore.toast('用户已创建，初始密码为系统参数中的初始密码')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

async function openDetail(row: UserVO) {
  detailTab.value = 'roles'
  try {
    detail.value = await userApi.detail(row.id)
    detailOpen.value = true
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function resetPassword(row: UserVO) {
  try {
    const result = await userApi.resetPassword(row.id)
    resetPasswordValue.value = result.password
    resetPasswordVisible.value = true
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function toggleStatus(row: UserVO) {
  try {
    const next = row.status === 1 ? 2 : 1
    await userApi.updateStatus(row.id, next)
    appStore.toast(next === 1 ? '账号已启用' : '账号已停用，在线会话已踢出')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function impersonate(row: UserVO) {
  try {
    await userStore.impersonateUser(row.id)
    appStore.toast(`已进入 ${row.realName} 的只读视角`)
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function statusTag(status: number) {
  return status === 1 ? 'is-success' : status === 3 ? 'is-warning' : 'is-muted'
}

function statusLabel(status: number) {
  return { 1: '正常', 2: '停用', 3: '离职', 4: '锁定' }[status] ?? '未知'
}

function timeText(value?: string) {
  return value ?? '—'
}

watch(pageNum, load)
watch(pageSize, () => {
  pageNum.value = 1
  load()
})

onMounted(async () => {
  if (route.query.keyword) {
    query.keyword = String(route.query.keyword)
  }
  await loadOptions()
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="用户管理" desc="组织由角色派生">
      <template #actions>
        <button v-if="canAdd" class="pm-btn is-primary" @click="openCreate">
          <i class="ti ti-plus" /> 新增用户
        </button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="query.keyword" class="pm-input" placeholder="姓名 / 账号 / 手机号 / 工号" @keyup.enter="load" />
        </div>
        <div class="pm-field">
          <label>组织（含下级）</label>
          <select v-model="query.orgId" class="pm-select">
            <option :value="null">全部组织</option>
            <option v-for="org in flatOrgs" :key="org.id" :value="org.id">{{ org.name }}</option>
          </select>
        </div>
        <div class="pm-field">
          <label>员工类型</label>
          <select v-model="query.employeeType" class="pm-select">
            <option value="">全部</option>
            <option v-for="item in employeeTypes" :key="item.dictValue" :value="item.dictValue">{{ item.dictLabel }}</option>
          </select>
        </div>
        <div class="pm-actions">
          <button class="pm-btn is-primary" @click="load"><i class="ti ti-search" /> 查询</button>
          <button class="pm-btn" @click="query.keyword = ''; query.orgId = null; query.employeeType = ''; load()">重置</button>
        </div>
      </div>
    </div>

    <div class="pm-card">
      <div class="pm-table-wrap">
        <table class="pm-table">
          <thead>
            <tr>
              <th>姓名 / 账号</th>
              <th>工号</th>
              <th>组织（默认角色）</th>
              <th style="min-width: 220px">关联角色</th>
              <th>公司主体</th>
              <th>状态</th>
              <th>最近登录</th>
              <th style="width: 260px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td>
                <div style="display: flex; align-items: center; gap: 8px">
                  <div class="pm-avatar" style="width: 28px; height: 28px; font-size: 12px">
                    {{ row.realName?.slice(-1) }}
                  </div>
                  <div style="line-height: 1.3">
                    <div style="font-weight: 600">{{ row.realName }}</div>
                    <div style="font-size: 12px; color: var(--pm-neutral-500)">{{ row.username }}</div>
                  </div>
                </div>
              </td>
              <td>{{ row.employeeNo || '—' }}</td>
              <td>{{ row.orgName || '—' }}</td>
              <td>
                <span
                  v-for="(role, index) in row.roles.slice(0, 3)"
                  :key="role.roleId"
                  class="pm-tag"
                  :class="{ 'is-solid': role.roleId === row.defaultRoleId }"
                  :title="`${role.orgName} / ${role.roleName}${role.roleId === row.defaultRoleId ? '（默认）' : ''}`"
                >
                  {{ role.roleName }}
                </span>
                <span
                  v-if="row.roles.length > 3"
                  class="pm-tag is-muted"
                  :title="row.roles.map((r) => `${r.orgName} / ${r.roleName}`).join('；')"
                >
                  +{{ row.roles.length - 3 }}
                </span>
              </td>
              <td>{{ row.companyNames || '—' }}</td>
              <td><span class="pm-tag" :class="statusTag(row.status)">{{ statusLabel(row.status) }}</span></td>
              <td>{{ timeText(row.lastLoginTime) }}</td>
              <td>
                <div class="pm-actions">
                  <button class="pm-btn is-ghost is-sm" @click="openDetail(row)">详情</button>
                  <button v-if="canEdit && !isReadOnly" class="pm-btn is-ghost is-sm" @click="openEdit(row)">编辑</button>
                  <button v-if="canReset && !isReadOnly" class="pm-btn is-ghost is-sm" @click="resetPassword(row)">重置密码</button>
                  <button v-if="canStatus && !isReadOnly" class="pm-btn is-ghost is-sm" @click="toggleStatus(row)">
                    {{ row.status === 1 ? '停用' : '启用' }}
                  </button>
                  <button
                    v-if="canImpersonate && !isReadOnly"
                    class="pm-btn is-ghost is-sm"
                    @click="impersonate(row)"
                  >
                    模拟视角
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && list.length === 0">
              <td colspan="8" class="pm-empty">没有符合条件的数据</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>

    <!-- 新增 / 编辑 -->
    <FormDrawer :open="formOpen" :title="form.id ? '编辑用户' : '新增用户'" :wide="true" :loading="saving" @close="formOpen = false" @submit="submit">
      <h3 style="font-size: 14px; margin: 0 0 12px">基本信息</h3>
      <div class="pm-form-grid">
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>登录账号</label>
          <input v-model="form.username" class="pm-input" :disabled="Boolean(form.id)" placeholder="4~20 位字母数字下划线" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>姓名</label>
          <input v-model="form.realName" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>手机号</label>
          <input v-model="form.mobile" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>邮箱</label>
          <input v-model="form.email" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>工号</label>
          <input v-model="form.employeeNo" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>身份证号</label>
          <input v-model="form.idCard" class="pm-input" placeholder="加密存储，按脱敏策略展示" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>员工类型</label>
          <select v-model="form.employeeType" class="pm-select">
            <option v-for="item in employeeTypes" :key="item.dictValue" :value="item.dictValue">{{ item.dictLabel }}</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>用工形式</label>
          <select v-model="form.employmentForm" class="pm-select">
            <option value="LABOR">劳动合同</option>
            <option value="DISPATCH">劳务派遣</option>
            <option value="INTERN_AGREE">实习协议</option>
            <option value="CONSULT">顾问协议</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>岗位</label>
          <input v-model="form.position" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>职级</label>
          <input v-model="form.jobLevel" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>入职日期</label>
          <input v-model="form.joinDate" type="date" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>用户状态</label>
          <select v-model="form.status" class="pm-select">
            <option v-for="item in statuses" :key="item.dictValue" :value="Number(item.dictValue)">{{ item.dictLabel }}</option>
          </select>
        </div>
        <div v-if="form.status === 3" class="pm-form-item">
          <label><span class="pm-required">*</span>离职日期</label>
          <input v-model="form.leaveDate" type="date" class="pm-input" />
        </div>
        <div class="pm-form-item is-full">
          <label>备注</label>
          <textarea v-model="form.remark" class="pm-input" />
        </div>
      </div>

      <h3 style="font-size: 14px; margin: 20px 0 12px">
        角色与组织 <span class="pm-tag is-muted">最多 5 个，默认角色决定主组织</span>
      </h3>
      <div style="display: flex; flex-wrap: wrap; gap: 8px">
        <label
          v-for="role in roles"
          :key="role.id"
          class="pm-tag"
          :class="{ 'is-solid': (form.roleIds as number[]).includes(role.id) }"
          style="cursor: pointer; height: 28px"
          @click.prevent="toggleRole(role.id)"
        >
          {{ role.orgName }} / {{ role.roleName }}
        </label>
      </div>
      <div class="pm-form-item" style="margin-top: 12px; grid-template-columns: 110px 240px">
        <label>默认角色</label>
        <select v-model="form.defaultRoleId" class="pm-select">
          <option :value="null">请选择</option>
          <option v-for="roleId in form.roleIds as number[]" :key="roleId" :value="roleId">
            {{ roles.find((r) => r.id === roleId)?.roleName }}
          </option>
        </select>
      </div>

      <h3 style="font-size: 14px; margin: 20px 0 12px">公司主体</h3>
      <div style="display: flex; flex-wrap: wrap; gap: 8px">
        <label
          v-for="company in companies"
          :key="company.id"
          class="pm-tag"
          :class="{ 'is-solid': hasCompany(company.id) }"
          style="cursor: pointer; height: 28px"
          @click.prevent="toggleCompany(company.id)"
        >
          {{ company.shortName || company.companyName }}
        </label>
      </div>
    </FormDrawer>

    <!-- 详情 -->
    <FormDrawer :open="detailOpen" :title="`用户详情 · ${detail?.realName ?? ''}`" :wide="true" @close="detailOpen = false" @submit="detailOpen = false">
      <div class="pm-tabs">
        <div class="pm-tab" :class="{ 'is-active': detailTab === 'roles' }" @click="detailTab = 'roles'">角色与组织</div>
        <div class="pm-tab" :class="{ 'is-active': detailTab === 'companies' }" @click="detailTab = 'companies'">公司主体</div>
        <div class="pm-tab" :class="{ 'is-active': detailTab === 'projects' }" @click="detailTab = 'projects'">项目参与</div>
        <div class="pm-tab" :class="{ 'is-active': detailTab === 'transfers' }" @click="detailTab = 'transfers'">交接记录</div>
      </div>

      <div v-if="detailTab === 'roles'">
        <table class="pm-table">
          <thead>
            <tr><th>角色</th><th>所属组织</th><th>数据范围</th><th>默认</th></tr>
          </thead>
          <tbody>
            <tr v-for="role in detail?.roles ?? []" :key="role.roleId">
              <td>{{ role.roleName }}<span class="pm-tag is-muted" style="margin-left: 6px">{{ role.roleCode }}</span></td>
              <td>{{ role.orgName }}</td>
              <td>{{ role.dataScope }}</td>
              <td><span v-if="role.roleId === detail?.defaultRoleId" class="pm-tag is-solid">默认</span></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else-if="detailTab === 'companies'">
        <table class="pm-table">
          <thead><tr><th>公司主体</th><th>关联类型</th></tr></thead>
          <tbody>
            <tr v-for="company in detail?.companies ?? []" :key="company.companyId">
              <td>{{ company.companyName }}</td>
              <td>{{ { 1: '任职主体', 2: '劳动合同', 3: '社保缴纳', 4: '发薪主体' }[company.relationType] }}</td>
            </tr>
            <tr v-if="(detail?.companies ?? []).length === 0"><td colspan="2" class="pm-empty">未关联公司主体</td></tr>
          </tbody>
        </table>
      </div>

      <div v-else-if="detailTab === 'projects'">
        <div class="pm-tag is-muted">数据由项目管理模块提供</div>
        <div class="pm-empty">当前用户暂无参与项目（底座只做只读展示，不含项目新增/编辑入口）</div>
      </div>

      <div v-else>
        <div class="pm-empty">
          交接记录请在「系统管理 → 账号交接」中按交出人筛选查看
        </div>
      </div>
    </FormDrawer>

    <div v-if="resetPasswordVisible" class="pm-drawer-mask" @click.self="resetPasswordVisible = false">
      <div class="pm-card" style="width: 360px; margin: auto">
        <h3 style="margin: 0 0 8px; font-size: 15px">密码已重置</h3>
        <p style="font-size: 13px; color: var(--pm-neutral-500)">请将新密码线下告知用户，首次登录后需修改。</p>
        <div class="pm-card" style="background: var(--pm-primary-50); text-align: center; font-weight: 600">
          {{ resetPasswordValue }}
        </div>
        <div style="display: flex; justify-content: flex-end">
          <button class="pm-btn is-primary" @click="resetPasswordVisible = false">知道了</button>
        </div>
      </div>
    </div>
  </div>
</template>
