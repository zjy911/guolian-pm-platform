<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import Pagination from '@/components/Pagination.vue'
import { menuApi, orgApi, roleApi, sensitiveApi } from '@/api'
import type { MenuNode, OrgVO, RoleVO, SensitiveGroup } from '@/api/types'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()

const list = ref<RoleVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const query = reactive({ keyword: '', orgId: null as number | null })
const orgs = ref<{ id: number; name: string; status: number }[]>([])
const flatOrgs = ref<{ id: number; name: string; status: number }[]>([])

const formOpen = ref(false)
const saving = ref(false)
const authOpen = ref(false)
const activeRole = ref<RoleVO | null>(null)
const menuTree = ref<MenuNode[]>([])
const checkedMenus = ref<number[]>([])
const maskGroups = ref<SensitiveGroup[]>([])
const masks = reactive<Record<string, string>>({})
const scopeForm = reactive({ dataScope: 'SELF', orgIds: [] as number[], companyIds: [] as number[] })

const form = reactive<Record<string, any>>({
  id: null as number | null,
  roleCode: '',
  roleName: '',
  orgId: null as number | null,
  description: '',
  roleType: 2,
  dataScope: 'SELF',
  canBeProjectMember: 0,
  canBeApprover: 1,
  sortNo: 0,
  status: 1,
  remark: '',
  confirmOrgChange: false
})

const canEdit = computed(() => userStore.hasPerm('system:role:edit'))

function flatten(nodes: OrgVO[], prefix = ''): { id: number; name: string; status: number }[] {
  const result: { id: number; name: string; status: number }[] = []
  nodes.forEach((node) => {
    result.push({ id: node.id, name: `${prefix}${node.orgName}`, status: node.status })
    result.push(...flatten(node.children || [], `${prefix}　`))
  })
  return result
}

async function load() {
  try {
    const data = await roleApi.page({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: query.keyword || undefined,
      orgId: query.orgId || undefined
    })
    list.value = data.list
    total.value = data.total
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function openCreate() {
  Object.assign(form, {
    id: null,
    roleCode: '',
    roleName: '',
    orgId: null,
    description: '',
    roleType: 2,
    dataScope: 'SELF',
    canBeProjectMember: 0,
    canBeApprover: 1,
    sortNo: 0,
    status: 1,
    remark: '',
    confirmOrgChange: false
  })
  formOpen.value = true
}

function openEdit(row: RoleVO) {
  Object.assign(form, {
    id: row.id,
    roleCode: row.roleCode,
    roleName: row.roleName,
    orgId: row.orgId,
    description: row.description ?? '',
    roleType: row.roleType,
    dataScope: row.dataScope,
    canBeProjectMember: row.canBeProjectMember,
    canBeApprover: row.canBeApprover,
    sortNo: row.sortNo,
    status: row.status,
    remark: row.remark ?? '',
    confirmOrgChange: false
  })
  formOpen.value = true
}

async function submit() {
  if (!form.orgId) {
    appStore.toast('角色必须绑定一个组织', 'error')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await roleApi.update(form.id as number, { ...form })
      appStore.toast('角色已更新')
    } else {
      await roleApi.create({ ...form })
      appStore.toast('角色已创建')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

async function removeRole(row: RoleVO) {
  if (!window.confirm(`确认删除角色「${row.roleName}」？`)) {
    return
  }
  try {
    await roleApi.remove(row.id)
    appStore.toast('角色已删除')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function openAuth(row: RoleVO) {
  activeRole.value = row
  const [tree, ids, groups, maskMap] = await Promise.all([
    menuApi.tree(),
    roleApi.detail(row.id).then((detail) => detail.menuIds),
    sensitiveApi.groups(),
    roleApi.fieldMasks(row.id)
  ])
  menuTree.value = tree
  checkedMenus.value = ids
  maskGroups.value = groups
  Object.keys(masks).forEach((key) => delete masks[key])
  groups.forEach((group) => {
    masks[group.groupCode] = maskMap[group.groupCode] ?? group.defaultLevel
  })
  scopeForm.dataScope = row.dataScope
  scopeForm.orgIds = []
  scopeForm.companyIds = []
  authOpen.value = true
}

function toggleMenu(node: MenuNode, checked: boolean) {
  const ids = new Set(checkedMenus.value)
  const walk = (item: MenuNode) => {
    if (checked) {
      ids.add(item.id)
    } else {
      ids.delete(item.id)
    }
    item.children?.forEach(walk)
  }
  walk(node)
  checkedMenus.value = Array.from(ids)
}

async function saveAuth() {
  if (!activeRole.value) {
    return
  }
  try {
    await roleApi.saveMenus(activeRole.value.id, checkedMenus.value)
    await roleApi.saveFieldMasks(
      activeRole.value.id,
      Object.entries(masks).map(([groupCode, maskLevel]) => ({ groupCode, maskLevel }))
    )
    await roleApi.saveDataScope(activeRole.value.id, { ...scopeForm })
    appStore.toast('授权已保存，权限缓存已刷新')
    authOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

onMounted(async () => {
  const tree = await orgApi.tree()
  flatOrgs.value = flatten(tree)
  orgs.value = flatOrgs.value
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="角色管理" desc="角色 = 组织 + 功能权限 + 数据范围 + 脱敏策略">
      <template #actions>
        <button v-if="canEdit" class="pm-btn is-primary" @click="openCreate"><i class="ti ti-plus" /> 新增角色</button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="query.keyword" class="pm-input" placeholder="角色名称 / 编码" @keyup.enter="load" />
        </div>
        <div class="pm-field">
          <label>所属组织</label>
          <select v-model="query.orgId" class="pm-select">
            <option :value="null">全部组织</option>
            <option v-for="org in orgs" :key="org.id" :value="org.id">{{ org.name }}</option>
          </select>
        </div>
        <div class="pm-actions">
          <button class="pm-btn is-primary" @click="load"><i class="ti ti-search" /> 查询</button>
        </div>
      </div>
    </div>

    <div class="pm-card">
      <div class="pm-table-wrap">
        <table class="pm-table">
          <thead>
            <tr>
              <th>角色</th><th>编码</th><th>所属组织</th><th>数据范围</th><th>可审批</th><th>人数</th><th>状态</th><th style="width: 220px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td style="font-weight: 600">{{ row.roleName }}</td>
              <td>{{ row.roleCode }}</td>
              <td>{{ row.orgName }}</td>
              <td><span class="pm-tag">{{ row.dataScopeLabel }}</span></td>
              <td>{{ row.canBeApprover === 1 ? '是' : '否' }}</td>
              <td>{{ row.userCount }}</td>
              <td>
                <span class="pm-tag" :class="row.status === 1 ? 'is-success' : 'is-muted'">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </span>
              </td>
              <td>
                <div class="pm-actions">
                  <button class="pm-btn is-ghost is-sm" @click="openAuth(row)">授权</button>
                  <button v-if="canEdit" class="pm-btn is-ghost is-sm" @click="openEdit(row)">编辑</button>
                  <button v-if="canEdit && row.roleType !== 1" class="pm-btn is-ghost is-sm is-danger" @click="removeRole(row)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>

    <FormDrawer :open="formOpen" :title="form.id ? '编辑角色' : '新增角色'" :loading="saving" @close="formOpen = false" @submit="submit">
      <div class="pm-form-grid">
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>角色名称</label>
          <input v-model="form.roleName" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>角色编码</label>
          <input v-model="form.roleCode" class="pm-input" placeholder="ROLE_组织缩写_职能" />
        </div>
        <div class="pm-form-item is-full">
          <label><span class="pm-required">*</span>所属组织</label>
          <select v-model="form.orgId" class="pm-select">
            <option :value="null">请选择组织（必填）</option>
            <option v-for="org in orgs.filter((item) => item.status === 1)" :key="org.id" :value="org.id">{{ org.name }}</option>
          </select>
        </div>
        <div class="pm-form-item is-full">
          <label>角色说明</label>
          <textarea v-model="form.description" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>数据范围</label>
          <select v-model="form.dataScope" class="pm-select">
            <option value="ALL">全部数据</option>
            <option value="ORG_AND_CHILD">本组织及下级</option>
            <option value="ORG">本组织</option>
            <option value="SELF">仅本人</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>可被分配审批</label>
          <select v-model="form.canBeApprover" class="pm-select">
            <option :value="1">是</option>
            <option :value="0">否</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>可被分配项目</label>
          <select v-model="form.canBeProjectMember" class="pm-select">
            <option :value="1">是</option>
            <option :value="0">否</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>排序</label>
          <input v-model.number="form.sortNo" type="number" class="pm-input" />
        </div>
        <div v-if="form.id" class="pm-form-item is-full">
          <label>组织变更确认</label>
          <label style="font-size: 12px; color: var(--pm-neutral-500)">
            <input v-model="form.confirmOrgChange" type="checkbox" />
            我已确认修改绑定组织会影响该角色下全部人员的组织归属与数据可见范围
          </label>
        </div>
      </div>
    </FormDrawer>

    <FormDrawer :open="authOpen" :title="`角色授权 · ${activeRole?.roleName ?? ''}`" :wide="true" @close="authOpen = false" @submit="saveAuth">
      <h3 style="font-size: 14px; margin: 0 0 10px">功能权限</h3>
      <div style="max-height: 280px; overflow: auto; border: 1px solid var(--pm-neutral-100); border-radius: 8px; padding: 8px">
        <div v-for="group in menuTree" :key="group.id" style="margin-bottom: 8px">
          <label style="display: flex; gap: 6px; font-weight: 600; font-size: 13px">
            <input
              type="checkbox"
              :checked="checkedMenus.includes(group.id)"
              @change="toggleMenu(group, ($event.target as HTMLInputElement).checked)"
            />
            {{ group.name }}
          </label>
          <div style="padding-left: 20px; display: flex; flex-direction: column; gap: 4px; margin-top: 4px">
            <label v-for="child in group.children" :key="child.id" style="display: flex; gap: 6px; font-size: 13px">
              <input
                type="checkbox"
                :checked="checkedMenus.includes(child.id)"
                @change="toggleMenu(child, ($event.target as HTMLInputElement).checked)"
              />
              {{ child.name }}
              <span v-if="child.perms" class="pm-tag is-muted">{{ child.perms }}</span>
            </label>
          </div>
        </div>
      </div>

      <h3 style="font-size: 14px; margin: 20px 0 10px">数据权限</h3>
      <div class="pm-form-item" style="grid-template-columns: 110px 240px">
        <label>数据范围</label>
        <select v-model="scopeForm.dataScope" class="pm-select">
          <option value="ALL">全部数据</option>
          <option value="ORG_AND_CHILD">本组织及下级</option>
          <option value="ORG">本组织</option>
          <option value="ORG_CUSTOM">自定义组织</option>
          <option value="SELF">仅本人</option>
          <option value="PROJECT_MEMBER">我参与的项目</option>
        </select>
      </div>
      <div v-if="scopeForm.dataScope === 'ORG_CUSTOM'" style="margin-top: 8px; display: flex; flex-wrap: wrap; gap: 6px">
        <label
          v-for="org in orgs"
          :key="org.id"
          class="pm-tag"
          :class="{ 'is-solid': scopeForm.orgIds.includes(org.id) }"
          style="cursor: pointer; height: 26px"
          @click.prevent="
            scopeForm.orgIds.includes(org.id)
              ? scopeForm.orgIds.splice(scopeForm.orgIds.indexOf(org.id), 1)
              : scopeForm.orgIds.push(org.id)
          "
        >
          {{ org.name }}
        </label>
      </div>

      <h3 style="font-size: 14px; margin: 20px 0 10px">字段脱敏档位</h3>
      <table class="pm-table">
        <thead><tr><th>字段组</th><th>风险</th><th>档位</th></tr></thead>
        <tbody>
          <tr v-for="group in maskGroups" :key="group.groupCode">
            <td>{{ group.groupName }}<span class="pm-tag is-muted" style="margin-left: 6px">{{ group.groupCode }}</span></td>
            <td>{{ group.riskLevel }}</td>
            <td>
              <select v-model="masks[group.groupCode]" class="pm-select" style="height: 30px">
                <option value="PLAIN" :disabled="group.riskLevel === 'HIGH'">明文可见</option>
                <option value="MASKED">脱敏</option>
                <option value="HIDDEN">隐藏</option>
              </select>
            </td>
          </tr>
        </tbody>
      </table>
    </FormDrawer>
  </div>
</template>
