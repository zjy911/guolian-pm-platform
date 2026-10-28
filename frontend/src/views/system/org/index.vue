<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { orgApi } from '@/api'
import type { OrgVO } from '@/api/types'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const tree = ref<OrgVO[]>([])
const current = ref<OrgVO | null>(null)
const keyword = ref('')
const formOpen = ref(false)
const saving = ref(false)
const form = reactive<Record<string, unknown>>({})

const canEdit = userStore.hasPerm('system:org:edit')

async function load() {
  try {
    tree.value = await orgApi.tree()
    if (!current.value && tree.value.length) {
      await select(tree.value[0].id)
    }
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function select(id: number) {
  current.value = await orgApi.detail(id)
}

function openCreate(parent?: OrgVO) {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, {
    id: null,
    orgCode: '',
    orgName: '',
    orgType: 'DEPT',
    parentId: parent?.id ?? 0,
    leaderId: null,
    costCenterCode: '',
    sortNo: 0,
    status: 1
  })
  formOpen.value = true
}

function openEdit(node: OrgVO) {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, {
    id: node.id,
    orgCode: node.orgCode,
    orgName: node.orgName,
    orgType: node.orgType,
    parentId: node.parentId,
    leaderId: node.leaderId,
    costCenterCode: node.costCenterCode,
    sortNo: node.sortNo,
    status: node.status
  })
  formOpen.value = true
}

async function submit() {
  saving.value = true
  try {
    if (form.id) {
      await orgApi.update(form.id as number, { ...form })
      appStore.toast('组织已更新')
    } else {
      await orgApi.create({ ...form })
      appStore.toast('组织已创建，祖级路径已重算')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

async function remove(node: OrgVO) {
  if (!window.confirm(`确认删除组织「${node.orgName}」？`)) {
    return
  }
  try {
    await orgApi.remove(node.id)
    appStore.toast('组织已删除')
    current.value = null
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function renderNodes(nodes: OrgVO[], level = 0): OrgVO[] {
  return nodes.flatMap((node) => [node, ...renderNodes(node.children || [], level + 1)])
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="组织架构" desc="只有一棵树，节点不挂公司主体">
      <template #actions>
        <button v-if="canEdit" class="pm-btn is-primary" @click="openCreate()"><i class="ti ti-plus" /> 新增根组织</button>
      </template>
    </PageHeader>

    <div style="display: grid; grid-template-columns: 320px 1fr; gap: 16px">
      <div class="pm-card" style="margin: 0">
        <input v-model="keyword" class="pm-input" placeholder="搜索组织" style="margin-bottom: 10px" />
        <div style="max-height: 560px; overflow: auto">
          <div v-for="node in renderNodes(tree)" :key="node.id">
            <div
              class="pm-dropdown-item"
              :class="{ 'is-active': current?.id === node.id }"
              :style="{ paddingLeft: `${8 + (node.ancestors?.split(',').length || 1) * 10}px` }"
              v-show="!keyword || node.orgName.includes(keyword)"
              @click="select(node.id)"
            >
              <span>{{ node.orgName }}</span>
              <span class="pm-tag is-muted">{{ node.roleCount }} 角色</span>
            </div>
          </div>
        </div>
      </div>

      <div class="pm-card" style="margin: 0">
        <template v-if="current">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px">
            <div>
              <h2 style="margin: 0 0 4px; font-size: 18px">{{ current.orgName }}</h2>
              <div style="font-size: 12px; color: var(--pm-neutral-500)">
                编码 {{ current.orgCode }} · 类型 {{ current.orgType }} · 祖级 {{ current.ancestors }}
              </div>
            </div>
            <div class="pm-actions">
              <button v-if="canEdit" class="pm-btn is-sm" @click="openCreate(current)">新增子级</button>
              <button v-if="canEdit" class="pm-btn is-sm" @click="openEdit(current)">编辑</button>
              <button v-if="canEdit" class="pm-btn is-sm is-danger" @click="remove(current)">删除</button>
            </div>
          </div>

          <h3 style="font-size: 14px; margin: 16px 0 8px">该组织下的角色（{{ current.roles.length }}）</h3>
          <table class="pm-table">
            <thead><tr><th>角色</th><th>编码</th><th>数据范围</th><th>人数</th></tr></thead>
            <tbody>
              <tr v-for="role in current.roles" :key="role.id">
                <td>{{ role.roleName }}</td>
                <td>{{ role.roleCode }}</td>
                <td>{{ role.dataScope }}</td>
                <td>{{ role.userCount }}</td>
              </tr>
              <tr v-if="current.roles.length === 0"><td colspan="4" class="pm-empty">该组织尚未定义角色</td></tr>
            </tbody>
          </table>

          <h3 style="font-size: 14px; margin: 16px 0 8px">组织人员（{{ current.users.length }}）</h3>
          <table class="pm-table">
            <thead><tr><th>姓名</th><th>账号</th><th>工号</th><th>状态</th></tr></thead>
            <tbody>
              <tr v-for="user in current.users" :key="user.id">
                <td>{{ user.realName }}</td>
                <td>{{ user.username }}</td>
                <td>{{ user.employeeNo || '—' }}</td>
                <td>{{ user.status }}</td>
              </tr>
              <tr v-if="current.users.length === 0"><td colspan="4" class="pm-empty">暂无人员</td></tr>
            </tbody>
          </table>
        </template>
        <div v-else class="pm-empty">请在左侧选择组织</div>
      </div>
    </div>

    <FormDrawer :open="formOpen" :title="form.id ? '编辑组织' : '新增组织'" :loading="saving" @close="formOpen = false" @submit="submit">
      <div class="pm-form-grid">
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>组织名称</label>
          <input v-model="form.orgName" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>组织编码</label>
          <input v-model="form.orgCode" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>组织类型</label>
          <select v-model="form.orgType" class="pm-select">
            <option value="GROUP">集团</option>
            <option value="BG">事业群</option>
            <option value="DEPT">职能部门</option>
            <option value="REGION">地区部</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>排序</label>
          <input v-model.number="form.sortNo" type="number" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>负责人 ID</label>
          <input v-model.number="form.leaderId" type="number" class="pm-input" placeholder="用于审批「组织负责人」节点" />
        </div>
        <div class="pm-form-item">
          <label>成本中心</label>
          <input v-model="form.costCenterCode" class="pm-input" />
        </div>
      </div>
      <p style="font-size: 12px; color: var(--pm-neutral-500); margin-top: 12px">
        提示：组织不直接关联公司主体，员工主体在用户管理「公司主体」Tab 维护。
      </p>
    </FormDrawer>
  </div>
</template>
