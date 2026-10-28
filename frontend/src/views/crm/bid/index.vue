<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import Pagination from '@/components/Pagination.vue'
import { accountApi, bidApi } from '@/api/crm'
import type { CrmAccount, CrmBid } from '@/api/crm'
import { companyApi } from '@/api'
import type { CompanyVO } from '@/api/types'
import { dateText, money, useCrmDict } from '../shared/useCrmDict'
import { useCrmList } from '../shared/useCrmList'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const dict = useCrmDict(['crm_bid_status', 'crm_tender_form', 'crm_deposit_status'])

const { list, total, loading, loaded, pageNum, pageSize, query, load, reset } = useCrmList<CrmBid>(
  bidApi.page,
  { keyword: '', status: '', tenderForm: '', accountId: null }
)

const canAdd = computed(() => userStore.hasPerm('crm:bid:add'))
const canEdit = computed(() => userStore.hasPerm('crm:bid:edit'))
const canStatus = computed(() => userStore.hasPerm('crm:bid:status'))
const canRemove = computed(() => userStore.hasPerm('crm:bid:remove'))
const readOnly = computed(() => userStore.profile?.readOnly ?? false)

const accountOptions = ref<CrmAccount[]>([])
const companies = ref<CompanyVO[]>([])

async function loadOptions() {
  try {
    const [accountData, companyList] = await Promise.all([accountApi.page({ pageSize: 50 }), companyApi.options()])
    accountOptions.value = accountData.list
    companies.value = companyList
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function accountName(id?: number) {
  if (!id) return '—'
  return accountOptions.value.find((item) => item.id === id)?.companyName ?? `#${id}`
}

const formOpen = ref(false)
const saving = ref(false)
const form = reactive<Record<string, any>>({})

function resetForm() {
  Object.assign(form, {
    id: null,
    tenderName: '',
    tenderNo: '',
    accountId: null,
    tenderForm: 'OPEN',
    bidEntity: null,
    province: '',
    city: '',
    priceCap: null,
    openDate: '',
    deadline: '',
    deposit: null,
    depositStatus: '',
    bidPlan: '',
    tenderUrl: '',
    remark: ''
  })
}

function openCreate() {
  resetForm()
  formOpen.value = true
}

function openEdit(row: CrmBid) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    tenderName: row.tenderName,
    tenderNo: row.tenderNo ?? '',
    accountId: row.accountId ?? null,
    tenderForm: row.tenderForm ?? 'OPEN',
    bidEntity: row.bidEntity ?? null,
    province: row.province ?? '',
    city: row.city ?? '',
    priceCap: row.priceCap ?? null,
    openDate: row.openDate ?? '',
    deadline: row.deadline ?? '',
    deposit: row.deposit ?? null,
    depositStatus: row.depositStatus ?? '',
    bidPlan: row.bidPlan ?? '',
    tenderUrl: row.tenderUrl ?? '',
    remark: row.remark ?? ''
  })
  formOpen.value = true
}

async function submit() {
  if (!form.tenderName || !form.accountId || !form.tenderForm || !form.bidEntity || !form.openDate || !form.deadline) {
    appStore.toast('项目名称、招标人、招标形式、投标主体、开标日、截止日为必填项', 'error')
    return
  }
  saving.value = true
  try {
    const payload: Record<string, unknown> = {
      tenderName: form.tenderName,
      tenderNo: form.tenderNo || undefined,
      accountId: form.accountId,
      tenderForm: form.tenderForm,
      bidEntity: form.bidEntity,
      province: form.province || undefined,
      city: form.city || undefined,
      priceCap: form.priceCap ?? undefined,
      openDate: form.openDate,
      deadline: form.deadline,
      deposit: form.deposit ?? undefined,
      depositStatus: form.depositStatus || undefined,
      bidPlan: form.bidPlan || undefined,
      tenderUrl: form.tenderUrl || undefined,
      remark: form.remark || undefined
    }
    if (form.id) {
      await bidApi.update(form.id as number, payload)
      appStore.toast('投标项目已更新')
    } else {
      await bidApi.create(payload)
      appStore.toast('投标项目已创建')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

// ---------------- 状态推进 ----------------
const statusDialog = ref<{
  open: boolean
  row: CrmBid | null
  targetStatus: string
  winAmount: number | null
  reason: string
}>({ open: false, row: null, targetStatus: '', winAmount: null, reason: '' })

const NEXT_STATUS: Record<string, string[]> = {
  TRACKING: ['REGISTERED', 'ABANDONED'],
  REGISTERED: ['SUBMITTED', 'ABANDONED'],
  SUBMITTED: ['OPENED', 'ABANDONED'],
  OPENED: ['WON', 'LOST'],
  WON: [],
  LOST: [],
  ABANDONED: []
}

function nextStatuses(row: CrmBid) {
  return NEXT_STATUS[row.bidStatus ?? 'TRACKING'] ?? []
}

function openStatus(row: CrmBid) {
  const options = nextStatuses(row)
  if (options.length === 0) {
    appStore.toast('该投标已进入终态，无后续状态', 'error')
    return
  }
  statusDialog.value = { open: true, row, targetStatus: options[0], winAmount: null, reason: '' }
}

async function submitStatus() {
  const row = statusDialog.value.row
  if (!row || !statusDialog.value.targetStatus) return
  try {
    await bidApi.status(row.id, {
      targetStatus: statusDialog.value.targetStatus,
      winAmount: statusDialog.value.winAmount ?? undefined,
      reason: statusDialog.value.reason || undefined
    })
    appStore.toast('状态已更新')
    statusDialog.value.open = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function genOpportunity(row: CrmBid) {
  if (row.genOpportunityId) {
    appStore.toast('该投标已生成商机，无需重复创建', 'error')
    return
  }
  try {
    await bidApi.genOpportunity(row.id)
    appStore.toast('已生成商机')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function genContract(row: CrmBid) {
  if (row.genContractId) {
    appStore.toast('该投标已生成合同，无需重复创建', 'error')
    return
  }
  try {
    await bidApi.genContract(row.id, {})
    appStore.toast('已生成合同草稿')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function remove(row: CrmBid) {
  if (!window.confirm(`确认删除投标「${row.tenderName}」？`)) return
  try {
    await bidApi.remove(row.id)
    appStore.toast('投标已删除')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function statusTag(status?: string) {
  if (status === 'WON') return 'is-success'
  if (status === 'LOST' || status === 'ABANDONED') return 'is-danger'
  if (status === 'OPENED' || status === 'SUBMITTED') return 'is-warning'
  return 'is-muted'
}

onMounted(async () => {
  await dict.loadDicts()
  await loadOptions()
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="投标管理" desc="截止日不得晚于开标日；赢单可一键生成商机 / 合同">
      <template #actions>
        <button class="pm-btn" @click="$router.push('/crm/bid-calendar')">
          <i class="ti ti-calendar-event" /> 投标日历
        </button>
        <button v-if="canAdd && !readOnly" class="pm-btn is-primary" @click="openCreate">
          <i class="ti ti-plus" /> 新增投标
        </button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="query.keyword" class="pm-input" placeholder="项目名称 / 招标编号" @keyup.enter="load" />
        </div>
        <div class="pm-field">
          <label>状态</label>
          <select v-model="query.status" class="pm-select">
            <option value="">全部</option>
            <option v-for="item in dict.options('crm_bid_status')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-field">
          <label>招标形式</label>
          <select v-model="query.tenderForm" class="pm-select">
            <option value="">全部</option>
            <option v-for="item in dict.options('crm_tender_form')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-field">
          <label>招标人</label>
          <select v-model="query.accountId" class="pm-select">
            <option :value="null">全部</option>
            <option v-for="item in accountOptions" :key="item.id" :value="item.id">{{ item.companyName }}</option>
          </select>
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
              <th>项目名称</th>
              <th>招标人</th>
              <th>形式</th>
              <th>状态</th>
              <th>开标日</th>
              <th>截止日</th>
              <th>限价</th>
              <th>中标金额</th>
              <th style="width: 290px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td>
                <div style="font-weight: 600">{{ row.tenderName }}</div>
                <div style="font-size: 12px; color: var(--pm-neutral-500)">{{ row.tenderNo || '—' }}</div>
              </td>
              <td>{{ accountName(row.accountId) }}</td>
              <td>{{ dict.label('crm_tender_form', row.tenderForm) }}</td>
              <td>
                <span class="pm-tag" :class="statusTag(row.bidStatus)">
                  {{ dict.label('crm_bid_status', row.bidStatus) }}
                </span>
                <div v-if="row.lostReason" style="font-size: 12px; color: var(--pm-danger)">{{ row.lostReason }}</div>
                <div v-if="row.abandonReason" style="font-size: 12px; color: var(--pm-danger)">
                  {{ row.abandonReason }}
                </div>
              </td>
              <td>{{ dateText(row.openDate) }}</td>
              <td>{{ dateText(row.deadline) }}</td>
              <td>{{ money(row.priceCap) }}</td>
              <td>{{ money(row.winAmount) }}</td>
              <td>
                <div class="pm-actions">
                  <button v-if="canEdit && !readOnly" class="pm-btn is-ghost is-sm" @click="openEdit(row)">编辑</button>
                  <button v-if="canStatus && !readOnly" class="pm-btn is-ghost is-sm" @click="openStatus(row)">
                    状态
                  </button>
                  <button v-if="canEdit && !readOnly" class="pm-btn is-ghost is-sm" @click="genOpportunity(row)">
                    转商机
                  </button>
                  <button v-if="canEdit && !readOnly" class="pm-btn is-ghost is-sm" @click="genContract(row)">
                    转合同
                  </button>
                  <button v-if="canRemove && !readOnly" class="pm-btn is-ghost is-sm" @click="remove(row)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="loaded && list.length === 0">
              <td colspan="9" class="pm-empty">没有符合条件的投标项目</td>
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
      :title="form.id ? '编辑投标' : '新增投标'"
      :wide="true"
      :loading="saving"
      @close="formOpen = false"
      @submit="submit"
    >
      <div class="pm-form-grid">
        <div class="pm-form-item is-full">
          <label><span class="pm-required">*</span>招标项目名称</label>
          <input v-model="form.tenderName" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>招标编号</label>
          <input v-model="form.tenderNo" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>招标人（客户）</label>
          <select v-model="form.accountId" class="pm-select">
            <option :value="null">请选择</option>
            <option v-for="item in accountOptions" :key="item.id" :value="item.id">{{ item.companyName }}</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>招标形式</label>
          <select v-model="form.tenderForm" class="pm-select">
            <option v-for="item in dict.options('crm_tender_form')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>我方投标主体</label>
          <select v-model="form.bidEntity" class="pm-select">
            <option :value="null">请选择</option>
            <option v-for="item in companies" :key="item.id" :value="item.id">
              {{ item.shortName || item.companyName }}
            </option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>省份</label>
          <input v-model="form.province" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>城市</label>
          <input v-model="form.city" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>最高限价（元）</label>
          <input v-model.number="form.priceCap" type="number" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>开标日期</label>
          <input v-model="form.openDate" type="date" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>投标截止日期</label>
          <input v-model="form.deadline" type="date" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>保证金（元）</label>
          <input v-model.number="form.deposit" type="number" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>保证金状态</label>
          <select v-model="form.depositStatus" class="pm-select">
            <option value="">未填写</option>
            <option v-for="item in dict.options('crm_deposit_status')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-form-item is-full">
          <label>投标方案</label>
          <textarea v-model="form.bidPlan" class="pm-input" rows="3" />
        </div>
        <div class="pm-form-item is-full">
          <label>招标公告链接</label>
          <input v-model="form.tenderUrl" class="pm-input" />
        </div>
        <div class="pm-form-item is-full">
          <label>备注</label>
          <textarea v-model="form.remark" class="pm-input" />
        </div>
      </div>
    </FormDrawer>

    <div v-if="statusDialog.open" class="pm-drawer-mask" @click.self="statusDialog.open = false">
      <div class="pm-card" style="width: 460px; margin: auto">
        <h3 style="margin: 0 0 12px; font-size: 15px">状态推进 · {{ statusDialog.row?.tenderName }}</h3>
        <div class="pm-form-item">
          <label>目标状态</label>
          <select v-model="statusDialog.targetStatus" class="pm-select">
            <option v-for="status in nextStatuses(statusDialog.row as CrmBid)" :key="status" :value="status">
              {{ dict.label('crm_bid_status', status) }}
            </option>
          </select>
        </div>
        <div v-if="statusDialog.targetStatus === 'WON'" class="pm-form-item" style="margin-top: 10px">
          <label>中标金额（元）</label>
          <input v-model.number="statusDialog.winAmount" type="number" class="pm-input" />
        </div>
        <div
          v-if="statusDialog.targetStatus === 'LOST' || statusDialog.targetStatus === 'ABANDONED'"
          class="pm-form-item"
          style="margin-top: 10px"
        >
          <label>原因</label>
          <textarea v-model="statusDialog.reason" class="pm-input" rows="2" placeholder="未中标 / 放弃原因" />
        </div>
        <div class="pm-actions" style="justify-content: flex-end; margin-top: 12px">
          <button class="pm-btn" @click="statusDialog.open = false">取消</button>
          <button class="pm-btn is-primary" @click="submitStatus">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>
