<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import Pagination from '@/components/Pagination.vue'
import { accountApi, activityApi, contactApi, leadApi, opportunityApi } from '@/api/crm'
import type { CrmActivity } from '@/api/crm'
import { dateTimeText, useCrmDict } from '../shared/useCrmDict'
import { useCrmList } from '../shared/useCrmList'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const dict = useCrmDict(['crm_activity_type', 'crm_activity_level', 'crm_activity_status', 'crm_target_type'])

const { list, total, loading, loaded, pageNum, pageSize, query, load, reset } = useCrmList<CrmActivity>(
  activityApi.page,
  { keyword: '', activityType: '', targetType: '', startDate: '', endDate: '' }
)

const canAdd = computed(() => userStore.hasPerm('crm:activity:add'))
const canEdit = computed(() => userStore.hasPerm('crm:activity:edit'))
const canVoid = computed(() => userStore.hasPerm('crm:activity:void'))
const readOnly = computed(() => userStore.profile?.readOnly ?? false)

// ---------------- 挂接对象选择 ----------------
interface TargetOption {
  id: number
  label: string
}

const targetOptions = ref<TargetOption[]>([])

async function loadTargets(targetType: string) {
  targetOptions.value = []
  if (!targetType) return
  try {
    if (targetType === 'ACCOUNT') {
      targetOptions.value = (await accountApi.page({ pageSize: 50 })).list.map((item) => ({
        id: item.id,
        label: item.companyName
      }))
    } else if (targetType === 'LEAD') {
      targetOptions.value = (await leadApi.page({ pageSize: 50 })).list.map((item) => ({
        id: item.id,
        label: `${item.name} · ${item.companyName ?? ''}`
      }))
    } else if (targetType === 'CONTACT') {
      targetOptions.value = (await contactApi.page({ pageSize: 50 })).list.map((item) => ({
        id: item.id,
        label: item.name
      }))
    } else if (targetType === 'OPPORTUNITY') {
      targetOptions.value = (await opportunityApi.page({ pageSize: 50 })).list.map((item) => ({
        id: item.id,
        label: item.name
      }))
    }
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

// ---------------- 新增 / 编辑 ----------------
const formOpen = ref(false)
const saving = ref(false)
const form = reactive<Record<string, any>>({})

function resetForm() {
  Object.assign(form, {
    id: null,
    subject: '',
    type: 'VISIT',
    level: 'NORMAL',
    targetType: 'ACCOUNT',
    targetId: null,
    startTime: '',
    endTime: '',
    description: '',
    remark: ''
  })
}

async function openCreate() {
  resetForm()
  formOpen.value = true
  await loadTargets('ACCOUNT')
}

async function openEdit(row: CrmActivity) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    subject: row.subject,
    type: row.type ?? 'VISIT',
    level: row.level ?? 'NORMAL',
    targetType: row.targetType ?? 'ACCOUNT',
    targetId: row.targetId ?? null,
    startTime: toLocalInput(row.startTime),
    endTime: toLocalInput(row.endTime),
    description: row.description ?? '',
    remark: row.remark ?? ''
  })
  formOpen.value = true
  await loadTargets(row.targetType ?? 'ACCOUNT')
}

/** 后端返回 "2026-10-23 09:30:00"，datetime-local 需要 "2026-10-23T09:30" */
function toLocalInput(value?: string) {
  if (!value) return ''
  return value.replace(' ', 'T').slice(0, 16)
}

watch(
  () => form.targetType,
  (value, old) => {
    if (formOpen.value && value !== old) {
      form.targetId = null
      loadTargets(value)
    }
  }
)

async function submit() {
  if (!form.subject || !form.targetId || !form.startTime || !form.endTime) {
    appStore.toast('主题、挂接对象、起止时间为必填项', 'error')
    return
  }
  saving.value = true
  try {
    const payload: Record<string, unknown> = {
      subject: form.subject,
      type: form.type,
      level: form.level,
      targetType: form.targetType,
      targetId: form.targetId,
      startTime: form.startTime,
      endTime: form.endTime,
      description: form.description || undefined,
      remark: form.remark || undefined
    }
    if (form.id) {
      await activityApi.update(form.id as number, payload)
      appStore.toast('活动已更新')
    } else {
      await activityApi.create(payload)
      appStore.toast('活动已创建')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

// ---------------- 作废 ----------------
const voidDialog = ref<{ open: boolean; row: CrmActivity | null; reason: string }>({
  open: false,
  row: null,
  reason: ''
})

function openVoid(row: CrmActivity) {
  voidDialog.value = { open: true, row, reason: '' }
}

async function submitVoid() {
  const row = voidDialog.value.row
  if (!row) return
  if (!voidDialog.value.reason.trim()) {
    appStore.toast('请填写作废原因', 'error')
    return
  }
  try {
    await activityApi.void(row.id, voidDialog.value.reason.trim())
    appStore.toast('活动已作废')
    voidDialog.value.open = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function statusTag(status?: string) {
  return status === 'VOID' ? 'is-danger' : 'is-success'
}

onMounted(async () => {
  await dict.loadDicts()
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="销售活动" desc="挂接线索 / 客户 / 联系人 / 商机，作废需填原因">
      <template #actions>
        <button class="pm-btn" @click="$router.push('/crm/activity-timeline')">
          <i class="ti ti-timeline" /> 时间线视图
        </button>
        <button v-if="canAdd && !readOnly" class="pm-btn is-primary" @click="openCreate">
          <i class="ti ti-plus" /> 新增活动
        </button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="query.keyword" class="pm-input" placeholder="活动主题" @keyup.enter="load" />
        </div>
        <div class="pm-field">
          <label>活动类型</label>
          <select v-model="query.activityType" class="pm-select">
            <option value="">全部</option>
            <option v-for="item in dict.options('crm_activity_type')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-field">
          <label>开始日期</label>
          <input v-model="query.startDate" type="date" class="pm-input" />
        </div>
        <div class="pm-field">
          <label>结束日期</label>
          <input v-model="query.endDate" type="date" class="pm-input" />
        </div>
        <div class="pm-actions">
          <button class="pm-btn is-primary" @click="load"><i class="ti ti-search" /> 查询</button>
          <button class="pm-btn" @click="reset">重置</button>
        </div>
      </div>
    </div>

    <div class="pm-card">
      <div class="pm-table-wrap">
        <table class="pm-table">
          <thead>
            <tr>
              <th>主题</th>
              <th>类型 / 级别</th>
              <th>挂接对象</th>
              <th>开始时间</th>
              <th>结束时间</th>
              <th>天数</th>
              <th>状态</th>
              <th>负责人</th>
              <th style="width: 190px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td style="font-weight: 600">
                {{ row.subject }}
                <div v-if="row.description" style="font-size: 12px; color: var(--pm-neutral-500); font-weight: 400">
                  {{ row.description }}
                </div>
              </td>
              <td>
                {{ dict.label('crm_activity_type', row.type) }}
                <div style="font-size: 12px; color: var(--pm-neutral-500)">
                  {{ dict.label('crm_activity_level', row.level) }}
                </div>
              </td>
              <td>
                {{ dict.label('crm_target_type', row.targetType) }}
                <span style="color: var(--pm-neutral-500)">#{{ row.targetId }}</span>
              </td>
              <td>{{ dateTimeText(row.startTime) }}</td>
              <td>{{ dateTimeText(row.endTime) }}</td>
              <td>{{ row.durationDays ?? '—' }}</td>
              <td>
                <span class="pm-tag" :class="statusTag(row.status)">
                  {{ dict.label('crm_activity_status', row.status) }}
                </span>
                <div v-if="row.voidReason" style="font-size: 12px; color: var(--pm-danger)">{{ row.voidReason }}</div>
              </td>
              <td>{{ row.ownerUserName || '—' }}</td>
              <td>
                <div class="pm-actions">
                  <button v-if="canEdit && !readOnly" class="pm-btn is-ghost is-sm" @click="openEdit(row)">编辑</button>
                  <button
                    v-if="canVoid && !readOnly && row.status !== 'VOID'"
                    class="pm-btn is-ghost is-sm"
                    @click="openVoid(row)"
                  >
                    作废
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="loaded && list.length === 0">
              <td colspan="9" class="pm-empty">没有符合条件的活动</td>
            </tr>
            <tr v-else-if="loading">
              <td colspan="9" class="pm-empty">加载中…</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>

    <FormDrawer
      :open="formOpen"
      :title="form.id ? '编辑活动' : '新增活动'"
      :wide="true"
      :loading="saving"
      @close="formOpen = false"
      @submit="submit"
    >
      <div class="pm-form-grid">
        <div class="pm-form-item is-full">
          <label><span class="pm-required">*</span>活动主题</label>
          <input v-model="form.subject" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>活动类型</label>
          <select v-model="form.type" class="pm-select">
            <option v-for="item in dict.options('crm_activity_type')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>活动级别</label>
          <select v-model="form.level" class="pm-select">
            <option v-for="item in dict.options('crm_activity_level')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>挂接对象类型</label>
          <select v-model="form.targetType" class="pm-select">
            <option v-for="item in dict.options('crm_target_type')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>挂接对象</label>
          <select v-model="form.targetId" class="pm-select">
            <option :value="null">请选择</option>
            <option v-for="item in targetOptions" :key="item.id" :value="item.id">{{ item.label }}</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>开始时间</label>
          <input v-model="form.startTime" type="datetime-local" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>结束时间</label>
          <input v-model="form.endTime" type="datetime-local" class="pm-input" />
        </div>
        <div class="pm-form-item is-full">
          <label>活动描述</label>
          <textarea v-model="form.description" class="pm-input" rows="3" />
        </div>
        <div class="pm-form-item is-full">
          <label>备注</label>
          <textarea v-model="form.remark" class="pm-input" />
        </div>
      </div>
    </FormDrawer>

    <div v-if="voidDialog.open" class="pm-drawer-mask" @click.self="voidDialog.open = false">
      <div class="pm-card" style="width: 440px; margin: auto">
        <h3 style="margin: 0 0 12px; font-size: 15px">作废活动 · {{ voidDialog.row?.subject }}</h3>
        <textarea v-model="voidDialog.reason" class="pm-input" rows="3" placeholder="作废原因（必填，保留审计痕迹）" />
        <div class="pm-actions" style="justify-content: flex-end; margin-top: 12px">
          <button class="pm-btn" @click="voidDialog.open = false">取消</button>
          <button class="pm-btn is-danger" @click="submitVoid">确认作废</button>
        </div>
      </div>
    </div>
  </div>
</template>
