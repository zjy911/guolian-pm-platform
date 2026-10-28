<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import Pagination from '@/components/Pagination.vue'
import { accountApi, activityApi, agreementApi, bidApi, contactApi, contractApi, opportunityApi } from '@/api/crm'
import type { CrmAccount, CrmActivity } from '@/api/crm'
import { useCrmDict, dateText, dateTimeText, money } from '../shared/useCrmDict'
import { useCrmList } from '../shared/useCrmList'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const dict = useCrmDict([
  'crm_industry',
  'crm_account_nature',
  'crm_account_type',
  'crm_pool_status',
  'crm_contact_position'
])

const { list, total, loading, loaded, pageNum, pageSize, query, load, reset } = useCrmList<CrmAccount>(
  accountApi.page,
  { keyword: '', industry: '', accountNature: '', accountType: '', poolStatus: '', includeProspect: undefined }
)

const canAdd = computed(() => userStore.hasPerm('crm:account:add'))
const canEdit = computed(() => userStore.hasPerm('crm:account:edit'))
const canRemove = computed(() => userStore.hasPerm('crm:account:remove'))
const canAssign = computed(() => userStore.hasPerm('crm:account:assign'))
const canQuery = computed(() => userStore.hasPerm('crm:account:query'))
const readOnly = computed(() => userStore.profile?.readOnly ?? false)

// ---------------- 新增 / 编辑 ----------------
const formOpen = ref(false)
const saving = ref(false)
const form = reactive<Record<string, any>>({})

function resetForm() {
  Object.assign(form, {
    id: null,
    companyName: '',
    creditCode: '',
    institutionCode: '',
    industry: '',
    accountNature: 'TO_B',
    accountType: 'NORMAL',
    website: '',
    phone: '',
    employeeScale: '',
    annualRevenue: null,
    province: '',
    city: '',
    district: '',
    address: '',
    isProspect: 0,
    remark: ''
  })
}

function openCreate() {
  resetForm()
  formOpen.value = true
}

function openEdit(row: CrmAccount) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    companyName: row.companyName,
    creditCode: row.creditCode ?? '',
    institutionCode: row.institutionCode ?? '',
    industry: row.industry ?? '',
    accountNature: row.accountNature ?? 'TO_B',
    accountType: row.accountType ?? 'NORMAL',
    website: row.website ?? '',
    phone: row.phone ?? '',
    employeeScale: row.employeeScale ?? '',
    annualRevenue: row.annualRevenue ?? null,
    province: row.province ?? '',
    city: row.city ?? '',
    district: row.district ?? '',
    address: row.address ?? '',
    isProspect: row.isProspect ?? 0,
    remark: row.remark ?? ''
  })
  formOpen.value = true
}

async function submit() {
  if (!form.companyName) {
    appStore.toast('请填写客户全称', 'error')
    return
  }
  saving.value = true
  try {
    const payload: Record<string, unknown> = {
      companyName: form.companyName,
      creditCode: form.creditCode || undefined,
      institutionCode: form.institutionCode || undefined,
      industry: form.industry,
      accountNature: form.accountNature,
      accountType: form.accountType,
      website: form.website || undefined,
      phone: form.phone || undefined,
      employeeScale: form.employeeScale || undefined,
      annualRevenue: form.annualRevenue ?? undefined,
      province: form.province || undefined,
      city: form.city || undefined,
      district: form.district || undefined,
      address: form.address || undefined,
      isProspect: form.isProspect,
      remark: form.remark || undefined
    }
    if (form.id) {
      await accountApi.update(form.id as number, payload)
      appStore.toast('客户已更新')
    } else {
      await accountApi.create(payload)
      appStore.toast('客户已创建')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

// ---------------- 详情 ----------------
const detailOpen = ref(false)
const detail = ref<CrmAccount | null>(null)
const detailTab = ref<'overview' | 'contacts' | 'opportunities' | 'contracts' | 'agreements' | 'bids' | 'timeline'>(
  'overview'
)
const tabCounts = ref<Record<string, number>>({})
const tabRows = ref<any[]>([])
const tabLoading = ref(false)
const timeline = ref<CrmActivity[]>([])

async function openDetail(row: CrmAccount) {
  detailTab.value = 'overview'
  tabRows.value = []
  timeline.value = []
  try {
    detail.value = await accountApi.detail(row.id)
    tabCounts.value = await accountApi.tabCounts(row.id)
    detailOpen.value = true
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function switchTab(tab: typeof detailTab.value) {
  detailTab.value = tab
  if (!detail.value) return
  const accountId = detail.value.id
  tabLoading.value = true
  try {
    if (tab === 'contacts') {
      tabRows.value = (await contactApi.page({ accountId, pageSize: 100 })).list
    } else if (tab === 'opportunities') {
      tabRows.value = (await opportunityApi.page({ accountId, pageSize: 100 })).list
    } else if (tab === 'contracts') {
      tabRows.value = (await contractApi.page({ accountId, pageSize: 100 })).list
    } else if (tab === 'agreements') {
      tabRows.value = (await agreementApi.page({ accountId, pageSize: 100 })).list
    } else if (tab === 'bids') {
      tabRows.value = (await bidApi.page({ accountId, pageSize: 100 })).list
    } else if (tab === 'timeline') {
      timeline.value = await activityApi.timeline('ACCOUNT', accountId)
    }
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    tabLoading.value = false
  }
}

// ---------------- 动作 ----------------
const reasonDialog = ref<{ open: boolean; title: string; value: string; action: ((reason: string) => Promise<void>) | null }>({
  open: false,
  title: '',
  value: '',
  action: null
})

function askReason(title: string, action: (reason: string) => Promise<void>) {
  reasonDialog.value = { open: true, title, value: '', action }
}

async function confirmReason() {
  const { value, action } = reasonDialog.value
  if (!value.trim()) {
    appStore.toast('请填写原因', 'error')
    return
  }
  try {
    await action?.(value.trim())
    reasonDialog.value.open = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function toPool(row: CrmAccount) {
  askReason('移入公共池', async (reason) => {
    await accountApi.toPool(row.id, reason)
    appStore.toast('已移入公共池')
  })
}

function churn(row: CrmAccount) {
  askReason('标记流失', async (reason) => {
    await accountApi.churn(row.id, reason)
    appStore.toast('已标记流失')
  })
}

async function revive(row: CrmAccount) {
  try {
    await accountApi.revive(row.id)
    appStore.toast('已恢复为私有客户')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function remove(row: CrmAccount) {
  try {
    const rel = await accountApi.relations(row.id)
    if (rel.deletable === false) {
      appStore.toast(`该客户存在 ${rel.total} 条关联数据，不能删除`, 'error')
      return
    }
    if (!window.confirm(`确认删除客户「${row.companyName}」？`)) return
    await accountApi.remove(row.id)
    appStore.toast('客户已删除')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function poolTagClass(status?: string) {
  if (status === 'PRIVATE') return 'is-success'
  if (status === 'POOL') return 'is-warning'
  if (status === 'CHURNED') return 'is-danger'
  return 'is-muted'
}

onMounted(async () => {
  await dict.loadDicts()
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="客户管理" desc="主数据 · 三态流转">
      <template #actions>
        <button v-if="canAdd && !readOnly" class="pm-btn is-primary" @click="openCreate">
          <i class="ti ti-plus" /> 新增客户
        </button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="query.keyword" class="pm-input" placeholder="客户全称 / 信用代码" @keyup.enter="load" />
        </div>
        <div class="pm-field">
          <label>所属行业</label>
          <select v-model="query.industry" class="pm-select">
            <option value="">全部</option>
            <option v-for="item in dict.options('crm_industry')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-field">
          <label>客户性质</label>
          <select v-model="query.accountNature" class="pm-select">
            <option value="">全部</option>
            <option v-for="item in dict.options('crm_account_nature')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-field">
          <label>客户分类</label>
          <select v-model="query.accountType" class="pm-select">
            <option value="">全部</option>
            <option v-for="item in dict.options('crm_account_type')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-field">
          <label>池状态</label>
          <select v-model="query.poolStatus" class="pm-select">
            <option value="">全部</option>
            <option v-for="item in dict.options('crm_pool_status')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
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
              <th>客户全称</th>
              <th>行业 / 性质</th>
              <th>分类</th>
              <th>池状态</th>
              <th>负责人</th>
              <th>年营收</th>
              <th>最近跟进</th>
              <th style="width: 240px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td>
                <div style="font-weight: 600">{{ row.companyName }}</div>
                <div style="font-size: 12px; color: var(--pm-neutral-500)">
                  {{ row.creditCode || row.institutionCode || '—' }}
                  <span v-if="row.province"> · {{ row.province }}{{ row.city }}</span>
                </div>
              </td>
              <td>
                {{ dict.label('crm_industry', row.industry) }}
                <div style="font-size: 12px; color: var(--pm-neutral-500)">
                  {{ dict.label('crm_account_nature', row.accountNature) }}
                </div>
              </td>
              <td>{{ dict.label('crm_account_type', row.accountType) }}</td>
              <td>
                <span class="pm-tag" :class="poolTagClass(row.poolStatus)">
                  {{ dict.label('crm_pool_status', row.poolStatus) }}
                </span>
              </td>
              <td>{{ row.ownerUserName || '—' }}</td>
              <td>{{ money(row.annualRevenue) }}</td>
              <td>{{ dateTimeText(row.lastActivityTime) }}</td>
              <td>
                <div class="pm-actions">
                  <button v-if="canQuery" class="pm-btn is-ghost is-sm" @click="openDetail(row)">详情</button>
                  <button v-if="canEdit && !readOnly" class="pm-btn is-ghost is-sm" @click="openEdit(row)">编辑</button>
                  <button
                    v-if="canAssign && !readOnly && row.poolStatus === 'PRIVATE'"
                    class="pm-btn is-ghost is-sm"
                    @click="toPool(row)"
                  >
                    移入池
                  </button>
                  <button
                    v-if="canEdit && !readOnly && row.poolStatus === 'PRIVATE'"
                    class="pm-btn is-ghost is-sm"
                    @click="churn(row)"
                  >
                    流失
                  </button>
                  <button
                    v-if="canEdit && !readOnly && row.poolStatus === 'CHURNED'"
                    class="pm-btn is-ghost is-sm"
                    @click="revive(row)"
                  >
                    恢复
                  </button>
                  <button v-if="canRemove && !readOnly" class="pm-btn is-ghost is-sm" @click="remove(row)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="loaded && list.length === 0">
              <td colspan="8" class="pm-empty">没有符合条件的数据</td>
            </tr>
            <tr v-else-if="loading">
              <td colspan="8" class="pm-empty">加载中…</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>

    <!-- 新增 / 编辑 -->
    <FormDrawer
      :open="formOpen"
      :title="form.id ? '编辑客户' : '新增客户'"
      :wide="true"
      :loading="saving"
      @close="formOpen = false"
      @submit="submit"
    >
      <div class="pm-form-grid">
        <div class="pm-form-item is-full">
          <label><span class="pm-required">*</span>客户全称</label>
          <input v-model="form.companyName" class="pm-input" placeholder="与营业执照一致" />
        </div>
        <div class="pm-form-item">
          <label>统一社会信用代码</label>
          <input v-model="form.creditCode" class="pm-input" placeholder="18 位大写字母或数字" />
        </div>
        <div class="pm-form-item">
          <label>机构编码</label>
          <input v-model="form.institutionCode" class="pm-input" placeholder="toG 客户必填" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>所属行业</label>
          <select v-model="form.industry" class="pm-select">
            <option value="">请选择</option>
            <option v-for="item in dict.options('crm_industry')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>客户性质</label>
          <select v-model="form.accountNature" class="pm-select">
            <option v-for="item in dict.options('crm_account_nature')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>客户分类</label>
          <select v-model="form.accountType" class="pm-select">
            <option v-for="item in dict.options('crm_account_type')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>潜客标识</label>
          <select v-model="form.isProspect" class="pm-select">
            <option :value="0">正式客户</option>
            <option :value="1">潜客</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>联系电话</label>
          <input v-model="form.phone" class="pm-input" placeholder="加密存储" />
        </div>
        <div class="pm-form-item">
          <label>官网</label>
          <input v-model="form.website" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>人员规模</label>
          <input v-model="form.employeeScale" class="pm-input" placeholder="如 1000-5000 人" />
        </div>
        <div class="pm-form-item">
          <label>年营收（元）</label>
          <input v-model.number="form.annualRevenue" type="number" class="pm-input" />
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
          <label>区县</label>
          <input v-model="form.district" class="pm-input" />
        </div>
        <div class="pm-form-item is-full">
          <label>详细地址</label>
          <input v-model="form.address" class="pm-input" placeholder="按脱敏策略展示" />
        </div>
        <div class="pm-form-item is-full">
          <label>备注</label>
          <textarea v-model="form.remark" class="pm-input" />
        </div>
      </div>
    </FormDrawer>

    <!-- 详情 -->
    <FormDrawer
      :open="detailOpen"
      :title="`客户详情 · ${detail?.companyName ?? ''}`"
      :wide="true"
      @close="detailOpen = false"
      @submit="detailOpen = false"
    >
      <div class="pm-tabs">
        <div class="pm-tab" :class="{ 'is-active': detailTab === 'overview' }" @click="switchTab('overview')">基本信息</div>
        <div class="pm-tab" :class="{ 'is-active': detailTab === 'contacts' }" @click="switchTab('contacts')">
          联系人（{{ tabCounts.contacts ?? 0 }}）
        </div>
        <div class="pm-tab" :class="{ 'is-active': detailTab === 'opportunities' }" @click="switchTab('opportunities')">
          商机（{{ tabCounts.opportunities ?? 0 }}）
        </div>
        <div class="pm-tab" :class="{ 'is-active': detailTab === 'contracts' }" @click="switchTab('contracts')">
          合同（{{ tabCounts.contracts ?? 0 }}）
        </div>
        <div class="pm-tab" :class="{ 'is-active': detailTab === 'agreements' }" @click="switchTab('agreements')">
          协议（{{ tabCounts.agreements ?? 0 }}）
        </div>
        <div class="pm-tab" :class="{ 'is-active': detailTab === 'bids' }" @click="switchTab('bids')">
          投标（{{ tabCounts.bids ?? 0 }}）
        </div>
        <div class="pm-tab" :class="{ 'is-active': detailTab === 'timeline' }" @click="switchTab('timeline')">跟进时间线</div>
      </div>

      <div v-if="detailTab === 'overview'">
        <table class="pm-table">
          <tbody>
            <tr><th style="width: 160px">客户全称</th><td>{{ detail?.companyName }}</td></tr>
            <tr><th>统一社会信用代码</th><td>{{ detail?.creditCode || '—' }}</td></tr>
            <tr><th>机构编码</th><td>{{ detail?.institutionCode || '—' }}</td></tr>
            <tr><th>行业 / 性质 / 分类</th>
              <td>
                {{ dict.label('crm_industry', detail?.industry) }} /
                {{ dict.label('crm_account_nature', detail?.accountNature) }} /
                {{ dict.label('crm_account_type', detail?.accountType) }}
              </td>
            </tr>
            <tr><th>池状态</th><td>{{ dict.label('crm_pool_status', detail?.poolStatus) }}</td></tr>
            <tr><th>负责人</th><td>{{ detail?.ownerUserName || '—' }}</td></tr>
            <tr><th>联系电话</th><td>{{ detail?.phone || '—' }}</td></tr>
            <tr><th>年营收</th><td>{{ money(detail?.annualRevenue) }}</td></tr>
            <tr><th>地址</th><td>{{ detail?.address || '—' }}</td></tr>
            <tr><th>入池时间</th><td>{{ dateTimeText(detail?.poolInTime) }}</td></tr>
            <tr><th>流失原因</th><td>{{ detail?.churnReason || '—' }}</td></tr>
            <tr><th>备注</th><td>{{ detail?.remark || '—' }}</td></tr>
          </tbody>
        </table>
      </div>

      <div v-else-if="detailTab === 'timeline'">
        <div v-if="timeline.length === 0" class="pm-empty">暂无跟进记录</div>
        <div v-else class="pm-timeline">
          <div v-for="item in timeline" :key="item.id" class="pm-timeline-item">
            <div class="pm-timeline-dot" :class="{ 'is-done': item.status !== 'VOID' }" />
            <div>
              <div style="font-weight: 600">{{ item.subject }}</div>
              <div style="font-size: 12px; color: var(--pm-neutral-500)">
                {{ dict.label('crm_activity_type', item.type) }} · {{ dateTimeText(item.startTime) }} ·
                {{ item.ownerUserName || '—' }}
                <span v-if="item.status === 'VOID'" class="pm-tag is-danger" style="margin-left: 6px">已作废</span>
              </div>
              <div v-if="item.description" style="font-size: 13px; margin-top: 4px">{{ item.description }}</div>
            </div>
          </div>
        </div>
      </div>

      <div v-else>
        <div v-if="tabLoading" class="pm-empty">加载中…</div>
        <div v-else-if="tabRows.length === 0" class="pm-empty">暂无关联数据</div>
        <table v-else class="pm-table">
          <thead v-if="detailTab === 'contacts'">
            <tr><th>姓名</th><th>职位</th><th>部门</th><th>手机</th><th>邮箱</th></tr>
          </thead>
          <thead v-else-if="detailTab === 'opportunities'">
            <tr><th>商机名称</th><th>阶段</th><th>预计金额</th><th>预计成交</th><th>负责人</th></tr>
          </thead>
          <thead v-else-if="detailTab === 'contracts'">
            <tr><th>合同编号</th><th>名称</th><th>状态</th><th>开始</th><th>到期</th><th>金额</th></tr>
          </thead>
          <thead v-else-if="detailTab === 'agreements'">
            <tr><th>协议编号</th><th>名称</th><th>状态</th><th>开始</th><th>到期</th></tr>
          </thead>
          <thead v-else>
            <tr><th>招标名称</th><th>编号</th><th>状态</th><th>开标日</th><th>截止日</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in tabRows" :key="row.id as number">
              <template v-if="detailTab === 'contacts'">
                <td>{{ row.name }}</td>
                <td>{{ row.positionName || '—' }}</td>
                <td>{{ row.department || '—' }}</td>
                <td>{{ row.mobile || '—' }}</td>
                <td>{{ row.email || '—' }}</td>
              </template>
              <template v-else-if="detailTab === 'opportunities'">
                <td>{{ row.name }}</td>
                <td>{{ dict.label('crm_sales_stage', row.salesStage as string) }}</td>
                <td>{{ money(row.expectedAmount as number) }}</td>
                <td>{{ dateText(row.expectedCloseDate as string) }}</td>
                <td>{{ row.ownerUserName || '—' }}</td>
              </template>
              <template v-else-if="detailTab === 'contracts'">
                <td>{{ row.contractNo || '—' }}</td>
                <td>{{ row.name }}</td>
                <td>{{ dict.label('crm_contract_status', row.status as string) }}</td>
                <td>{{ dateText(row.startDate as string) }}</td>
                <td>{{ dateText(row.endDate as string) }}</td>
                <td>{{ money(row.winAmount as number) }}</td>
              </template>
              <template v-else-if="detailTab === 'agreements'">
                <td>{{ row.agreementNo || '—' }}</td>
                <td>{{ row.name }}</td>
                <td>{{ dict.label('crm_agreement_status', row.status as string) }}</td>
                <td>{{ dateText(row.startDate as string) }}</td>
                <td>{{ dateText(row.endDate as string) }}</td>
              </template>
              <template v-else>
                <td>{{ row.tenderName }}</td>
                <td>{{ row.tenderNo || '—' }}</td>
                <td>{{ dict.label('crm_bid_status', row.bidStatus as string) }}</td>
                <td>{{ dateText(row.openDate as string) }}</td>
                <td>{{ dateText(row.deadline as string) }}</td>
              </template>
            </tr>
          </tbody>
        </table>
      </div>
    </FormDrawer>

    <!-- 原因输入 -->
    <div v-if="reasonDialog.open" class="pm-drawer-mask" @click.self="reasonDialog.open = false">
      <div class="pm-card" style="width: 420px; margin: auto">
        <h3 style="margin: 0 0 12px; font-size: 15px">{{ reasonDialog.title }}</h3>
        <textarea v-model="reasonDialog.value" class="pm-input" rows="3" placeholder="请填写原因（必填，会记入流转日志）" />
        <div class="pm-actions" style="justify-content: flex-end; margin-top: 12px">
          <button class="pm-btn" @click="reasonDialog.open = false">取消</button>
          <button class="pm-btn is-primary" @click="confirmReason">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>
