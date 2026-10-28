<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import Pagination from '@/components/Pagination.vue'
import { transferApi, userApi } from '@/api'
import type { TransferItemVO, TransferVO, UserVO } from '@/api/types'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()

interface AdapterInfo { bizType: string; bizName: string; count: number; available: boolean }
interface RoleBrief { roleId: number; roleCode: string; roleName: string; orgName: string; default: boolean }

const list = ref<TransferVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const query = reactive({ keyword: '', flowStatus: '', execStatus: '' })

const createOpen = ref(false)
const saving = ref(false)
const detailOpen = ref(false)
const detail = ref<TransferVO | null>(null)
const items = ref<TransferItemVO[]>([])

const users = ref<UserVO[]>([])
const fromUserId = ref<number | null>(null)
const fromUserName = ref('')
const adapters = ref<AdapterInfo[]>([])
const fromRoles = ref<RoleBrief[]>([])
const assignMode = ref(1)
const scope = ref<string[]>([])
const remark = ref('')
const singleReceiver = ref<number | null>(null)
const roleReceivers = reactive<Record<number, number | null>>({})
const receiverOptions = reactive<Record<number, { userId: number; realName: string; orgName: string }[]>>({})

const canAdd = computed(() => userStore.hasPerm('system:transfer:add'))
const canSubmit = computed(() => userStore.hasPerm('system:transfer:submit'))
const canExecute = computed(() => userStore.hasPerm('system:transfer:execute'))
const canRevoke = computed(() => userStore.hasPerm('system:transfer:revoke'))
const readOnly = computed(() => userStore.profile?.readOnly ?? false)

const flowStatusLabel: Record<string, string> = {
  DRAFT: '草稿',
  RUNNING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回'
}
const execStatusLabel: Record<string, string> = {
  NOT_EXECUTED: '未执行',
  EXECUTING: '执行中',
  DONE: '已完成',
  PARTIAL_FAILED: '部分失败',
  REVOKED: '已撤销'
}

function flowTag(status: string) {
  if (status === 'APPROVED') return 'is-success'
  if (status === 'RUNNING') return 'is-warning'
  if (status === 'REJECTED') return 'is-danger'
  return 'is-muted'
}

function execTag(status: string) {
  if (status === 'DONE') return 'is-success'
  if (status === 'PARTIAL_FAILED') return 'is-danger'
  if (status === 'EXECUTING') return 'is-warning'
  return 'is-muted'
}

async function load() {
  try {
    const data = await transferApi.page({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: query.keyword || undefined,
      flowStatus: query.flowStatus || undefined,
      execStatus: query.execStatus || undefined
    })
    list.value = data.list
    total.value = data.total
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function openCreate() {
  const data = await userApi.page({ pageNum: 1, pageSize: 100 })
  users.value = data.list
  fromUserId.value = null
  fromUserName.value = ''
  adapters.value = []
  fromRoles.value = []
  assignMode.value = 1
  scope.value = []
  remark.value = ''
  singleReceiver.value = null
  Object.keys(roleReceivers).forEach((key) => delete roleReceivers[Number(key)])
  Object.keys(receiverOptions).forEach((key) => delete receiverOptions[Number(key)])
  createOpen.value = true
}

async function runPrecheck() {
  if (!fromUserId.value) {
    appStore.toast('请先选择交出人', 'error')
    return
  }
  try {
    const data = await transferApi.precheck(fromUserId.value)
    fromUserName.value = data.fromUserName
    adapters.value = data.adapters
    fromRoles.value = data.roles
    scope.value = data.adapters.filter((item) => item.available).map((item) => item.bizType)
    for (const role of data.roles) {
      const receivers = await transferApi.receivers(fromUserId.value, role.roleId)
      receiverOptions[role.roleId] = receivers
    }
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function toggleScope(bizType: string) {
  const index = scope.value.indexOf(bizType)
  if (index >= 0) {
    scope.value.splice(index, 1)
  } else {
    scope.value.push(bizType)
  }
}

async function submitCreate() {
  if (!fromUserId.value || scope.value.length === 0) {
    appStore.toast('请选择交出人与交接范围', 'error')
    return
  }
  const payload: Record<string, unknown> = {
    fromUserId: fromUserId.value,
    assignMode: assignMode.value,
    scope: scope.value,
    remark: remark.value || undefined
  }
  if (assignMode.value === 1) {
    if (!singleReceiver.value) {
      appStore.toast('请选择接收人', 'error')
      return
    }
    payload.toUserIds = [singleReceiver.value]
  } else {
    const entries = Object.entries(roleReceivers).filter(([, value]) => Boolean(value))
    if (entries.length !== fromRoles.value.length) {
      appStore.toast('请为每个角色指定接收人', 'error')
      return
    }
    payload.toUserIds = entries.map(([, value]) => value)
    const assignments: Record<number, number> = {}
    entries.forEach(([roleId, userId]) => {
      assignments[Number(roleId)] = Number(userId)
    })
    payload.roleAssignments = assignments
  }
  saving.value = true
  try {
    await transferApi.create(payload)
    appStore.toast('交接单已创建，请提交审批')
    createOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

async function submitFlow(row: TransferVO) {
  try {
    await transferApi.submit(row.id)
    appStore.toast('已提交审批，等待审批人处理')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function execute(row: TransferVO) {
  try {
    await transferApi.execute(row.id)
    appStore.toast('已触发异步执行，请稍后查看明细')
    setTimeout(load, 1500)
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function retry(row: TransferVO) {
  try {
    await transferApi.retry(row.id)
    appStore.toast('已重试失败项')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function revoke(row: TransferVO) {
  const reason = window.prompt('请输入撤销原因')
  if (reason === null) {
    return
  }
  try {
    await transferApi.revoke(row.id, reason)
    appStore.toast('已撤销并回写')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function openDetail(row: TransferVO) {
  detail.value = row
  items.value = await transferApi.items(row.id)
  detailOpen.value = true
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="账号交接" desc="唯一正式跑通的审批场景">
      <template #actions>
        <button v-if="canAdd && !readOnly" class="pm-btn is-primary" @click="openCreate">
          <i class="ti ti-plus" /> 发起交接
        </button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>交接单号</label>
          <input v-model="query.keyword" class="pm-input" placeholder="HO-YYYYMMDD-0001" @keyup.enter="load" />
        </div>
        <div class="pm-field">
          <label>审批状态</label>
          <select v-model="query.flowStatus" class="pm-select">
            <option value="">全部</option>
            <option value="DRAFT">草稿</option>
            <option value="RUNNING">审批中</option>
            <option value="APPROVED">已通过</option>
            <option value="REJECTED">已驳回</option>
          </select>
        </div>
        <div class="pm-field">
          <label>执行状态</label>
          <select v-model="query.execStatus" class="pm-select">
            <option value="">全部</option>
            <option value="NOT_EXECUTED">未执行</option>
            <option value="DONE">已完成</option>
            <option value="PARTIAL_FAILED">部分失败</option>
            <option value="REVOKED">已撤销</option>
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
              <th>交接单号</th><th>交出人</th><th>接收人</th><th>业务范围</th><th>审批</th><th>执行</th><th>明细</th><th>可撤销至</th><th style="width: 260px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td style="font-weight: 600">{{ row.transferNo }}</td>
              <td>{{ row.fromUserName }}</td>
              <td>{{ row.toUserNames }}</td>
              <td>
                <span v-for="item in row.scope" :key="item" class="pm-tag is-muted">{{ item }}</span>
              </td>
              <td><span class="pm-tag" :class="flowTag(row.flowStatus)">{{ flowStatusLabel[row.flowStatus] ?? row.flowStatus }}</span></td>
              <td><span class="pm-tag" :class="execTag(row.execStatus)">{{ execStatusLabel[row.execStatus] ?? row.execStatus }}</span></td>
              <td>{{ row.itemCount }} 条<span v-if="row.failCount > 0" style="color: var(--pm-danger)">（失败 {{ row.failCount }}）</span></td>
              <td>{{ row.revokeDeadline || '—' }}</td>
              <td>
                <div class="pm-actions">
                  <button v-if="canSubmit && !readOnly && (row.flowStatus === 'DRAFT' || row.flowStatus === 'REJECTED')" class="pm-btn is-ghost is-sm" @click="submitFlow(row)">提交审批</button>
                  <button v-if="canExecute && !readOnly && row.flowStatus === 'APPROVED' && row.execStatus === 'NOT_EXECUTED'" class="pm-btn is-ghost is-sm" @click="execute(row)">执行</button>
                  <button v-if="canExecute && !readOnly && row.execStatus === 'PARTIAL_FAILED'" class="pm-btn is-ghost is-sm" @click="retry(row)">重试失败项</button>
                  <button v-if="canRevoke && !readOnly && row.revocable" class="pm-btn is-ghost is-sm is-danger" @click="revoke(row)">撤销</button>
                  <button class="pm-btn is-ghost is-sm" @click="openDetail(row)">明细</button>
                </div>
              </td>
            </tr>
            <tr v-if="list.length === 0"><td colspan="9" class="pm-empty">暂无交接单</td></tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>

    <FormDrawer :open="createOpen" title="发起账号交接" :wide="true" :loading="saving" @close="createOpen = false" @submit="submitCreate">
      <h3 style="font-size: 14px; margin: 0 0 10px">1. 选择交出人</h3>
      <div style="display: flex; gap: 10px; align-items: center">
        <select v-model="fromUserId" class="pm-select" style="max-width: 320px" @change="runPrecheck">
          <option :value="null">请选择交出人</option>
          <option v-for="user in users" :key="user.id" :value="user.id">{{ user.realName }}（{{ user.username }}）</option>
        </select>
        <button class="pm-btn" @click="runPrecheck">预检</button>
      </div>

      <template v-if="adapters.length">
        <h3 style="font-size: 14px; margin: 18px 0 10px">2. 交接范围（预检结果）</h3>
        <div style="display: flex; flex-wrap: wrap; gap: 8px">
          <label
            v-for="adapter in adapters"
            :key="adapter.bizType"
            class="pm-tag"
            :class="{ 'is-solid': scope.includes(adapter.bizType), 'is-muted': !adapter.available }"
            style="height: 28px; cursor: pointer"
            @click.prevent="adapter.available && toggleScope(adapter.bizType)"
          >
            {{ adapter.bizName }} · {{ adapter.available ? adapter.count + ' 条' : '暂无可交接数据' }}
          </label>
        </div>

        <h3 style="font-size: 14px; margin: 18px 0 10px">3. 接收人</h3>
        <div style="display: flex; gap: 16px; margin-bottom: 10px">
          <label style="font-size: 13px"><input v-model="assignMode" type="radio" :value="1" /> 单接收人（需拥有交出人全部角色）</label>
          <label style="font-size: 13px"><input v-model="assignMode" type="radio" :value="2" /> 按角色分派</label>
        </div>

        <div v-if="assignMode === 1" class="pm-form-item" style="grid-template-columns: 110px 320px">
          <label>接收人</label>
          <select v-model="singleReceiver" class="pm-select">
            <option :value="null">请选择</option>
            <option v-for="option in receiverOptions[fromRoles[0]?.roleId] || []" :key="option.userId" :value="option.userId">
              {{ option.realName }}（{{ option.orgName }}）
            </option>
          </select>
        </div>

        <div v-else>
          <div v-for="role in fromRoles" :key="role.roleId" class="pm-form-item" style="grid-template-columns: 200px 320px; margin-bottom: 8px">
            <label style="text-align: left">{{ role.orgName }} / {{ role.roleName }}</label>
            <select v-model="roleReceivers[Number(role.roleId)]" class="pm-select">
              <option :value="null">请选择该角色的接收人</option>
              <option v-for="option in receiverOptions[role.roleId] || []" :key="option.userId" :value="option.userId">
                {{ option.realName }}
              </option>
            </select>
          </div>
        </div>

        <div class="pm-form-item is-full" style="grid-template-columns: 110px 1fr; margin-top: 12px">
          <label>交接说明</label>
          <textarea v-model="remark" class="pm-input" />
        </div>
        <p style="font-size: 12px; color: var(--pm-neutral-500)">
          提交后进入三级审批（直属上级 → 部门负责人 → 人力资源），审批通过后自动执行；默认 7 天内可撤销。
        </p>
      </template>
    </FormDrawer>

    <FormDrawer :open="detailOpen" :title="`交接明细 · ${detail?.transferNo ?? ''}`" :wide="true" @close="detailOpen = false" @submit="detailOpen = false">
      <div style="display: flex; gap: 10px; margin-bottom: 12px; flex-wrap: wrap">
        <span class="pm-tag">交出人：{{ detail?.fromUserName }}</span>
        <span class="pm-tag">接收人：{{ detail?.toUserNames }}</span>
        <span class="pm-tag" :class="flowTag(detail?.flowStatus ?? '')">审批 {{ flowStatusLabel[detail?.flowStatus ?? ''] }}</span>
        <span class="pm-tag" :class="execTag(detail?.execStatus ?? '')">执行 {{ execStatusLabel[detail?.execStatus ?? ''] }}</span>
      </div>
      <table class="pm-table">
        <thead><tr><th>业务对象</th><th>数据</th><th>原归属</th><th>新归属</th><th>结果</th><th>失败原因</th></tr></thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td>{{ item.bizType }}</td>
            <td>{{ item.bizName }}</td>
            <td>{{ item.beforeOwnerName }}</td>
            <td>{{ item.afterOwnerName }}</td>
            <td>
              <span class="pm-tag" :class="item.result === 'SUCCESS' ? 'is-success' : item.result === 'FAILED' ? 'is-danger' : 'is-muted'">
                {{ item.result }}
              </span>
            </td>
            <td>{{ item.failReason || '—' }}</td>
          </tr>
          <tr v-if="items.length === 0"><td colspan="6" class="pm-empty">暂无明细</td></tr>
        </tbody>
      </table>
    </FormDrawer>
  </div>
</template>
