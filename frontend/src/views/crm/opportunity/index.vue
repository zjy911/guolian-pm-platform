<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import Pagination from '@/components/Pagination.vue'
import { accountApi, activityApi, opportunityApi } from '@/api/crm'
import type { CrmAccount, CrmActivity, CrmCompetitor, CrmOpportunity } from '@/api/crm'
import { dateText, dateTimeText, money, wan, useCrmDict } from '../shared/useCrmDict'
import { useCrmList } from '../shared/useCrmList'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const dict = useCrmDict(['crm_sales_stage', 'crm_pricing_mode', 'crm_activity_type'])

const STAGE_ORDER = ['PROSPECT', 'PROPOSAL', 'NEGOTIATION', 'WON']

/** 计价口径 → 必填明细项（与后端 validatePricing 一一对应） */
const PRICING_FIELD: Record<string, { key: string; label: string }> = {
  LUMP_SUM: { key: 'totalAmount', label: '总包金额（元）' },
  UNIT_PRICE: { key: 'unitPrice', label: '单价（元/次或元/条）' },
  TIME_BASED: { key: 'hourlyRate', label: '人时费率（元/小时）' },
  HEAD_COUNT: { key: 'pricePerPersonMonth', label: '人月单价（元/人月）' },
  COMMISSION: { key: 'commissionRate', label: '佣金比例（%）' },
  EFFECT_BASED: { key: 'unitPrice', label: '效果单价（元/单位）' }
}

const { list, total, loading, loaded, pageNum, pageSize, query, load, reset } = useCrmList<CrmOpportunity>(
  opportunityApi.page,
  { keyword: '', stage: '', accountId: null }
)

const canAdd = computed(() => userStore.hasPerm('crm:opportunity:add'))
const canEdit = computed(() => userStore.hasPerm('crm:opportunity:edit'))
const canStage = computed(() => userStore.hasPerm('crm:opportunity:stage'))
const canLost = computed(() => userStore.hasPerm('crm:opportunity:lost'))
const canRemove = computed(() => userStore.hasPerm('crm:opportunity:remove'))
const readOnly = computed(() => userStore.profile?.readOnly ?? false)

const accountOptions = ref<CrmAccount[]>([])
const accountKeyword = ref('')

async function searchAccounts() {
  try {
    const data = await accountApi.page({ keyword: accountKeyword.value || undefined, pageSize: 20 })
    accountOptions.value = data.list
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function accountName(id?: number) {
  if (!id) return '—'
  return accountOptions.value.find((item) => item.id === id)?.companyName ?? `#${id}`
}

// ---------------- 新增 / 编辑 ----------------
const formOpen = ref(false)
const saving = ref(false)
const form = reactive<Record<string, any>>({})
const pricingValue = ref<number | null>(null)

function resetForm() {
  Object.assign(form, {
    id: null,
    name: '',
    accountId: null,
    expectedAmount: null,
    expectedCloseDate: '',
    pricingMode: 'LUMP_SUM',
    estCost: null,
    estServiceVolume: '',
    address: '',
    remark: ''
  })
  pricingValue.value = null
}

function openCreate() {
  resetForm()
  formOpen.value = true
  if (accountOptions.value.length === 0) searchAccounts()
}

function openEdit(row: CrmOpportunity) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    name: row.name,
    accountId: row.accountId ?? null,
    expectedAmount: row.expectedAmount ?? null,
    expectedCloseDate: row.expectedCloseDate ?? '',
    pricingMode: row.pricingMode ?? 'LUMP_SUM',
    estCost: row.estCost ?? null,
    estServiceVolume: row.estServiceVolume ?? '',
    address: row.address ?? '',
    remark: row.remark ?? ''
  })
  const field = PRICING_FIELD[row.pricingMode ?? '']
  if (field && row.pricingDetail) {
    try {
      pricingValue.value = JSON.parse(row.pricingDetail)[field.key] ?? null
    } catch {
      pricingValue.value = null
    }
  }
  formOpen.value = true
}

function buildPricingDetail(): string | undefined {
  const mode = form.pricingMode as string
  if (!mode || mode === 'FREE') return undefined
  const field = PRICING_FIELD[mode]
  if (!field || pricingValue.value === null || pricingValue.value === undefined) return undefined
  return JSON.stringify({ [field.key]: pricingValue.value })
}

async function submit() {
  if (!form.name || !form.accountId) {
    appStore.toast('商机名称与客户为必填项', 'error')
    return
  }
  saving.value = true
  try {
    const payload: Record<string, unknown> = {
      name: form.name,
      accountId: form.accountId,
      expectedAmount: form.expectedAmount ?? undefined,
      expectedCloseDate: form.expectedCloseDate || undefined,
      pricingMode: form.pricingMode,
      pricingDetail: buildPricingDetail(),
      estCost: form.estCost ?? undefined,
      estServiceVolume: form.estServiceVolume || undefined,
      address: form.address || undefined,
      remark: form.remark || undefined
    }
    if (form.id) {
      await opportunityApi.update(form.id as number, payload)
      appStore.toast('商机已更新')
    } else {
      await opportunityApi.create(payload)
      appStore.toast('商机已创建')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

// ---------------- 阶段推进 / 流失 ----------------
const stageDialog = ref<{ open: boolean; row: CrmOpportunity | null; target: string; reason: string }>({
  open: false,
  row: null,
  target: '',
  reason: ''
})

function nextStages(row: CrmOpportunity) {
  const index = STAGE_ORDER.indexOf(row.salesStage ?? '')
  if (index < 0) return []
  return STAGE_ORDER.slice(index + 1)
}

function openStage(row: CrmOpportunity) {
  const options = nextStages(row)
  if (options.length === 0) {
    appStore.toast('该商机已成交，无后续阶段', 'error')
    return
  }
  stageDialog.value = { open: true, row, target: options[0], reason: '' }
}

async function submitStage() {
  const row = stageDialog.value.row
  if (!row || !stageDialog.value.target) return
  try {
    await opportunityApi.stage(row.id, stageDialog.value.target, stageDialog.value.reason || undefined)
    appStore.toast('阶段已推进')
    stageDialog.value.open = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

const lostDialog = ref<{ open: boolean; row: CrmOpportunity | null; reason: string }>({
  open: false,
  row: null,
  reason: ''
})

function openLost(row: CrmOpportunity) {
  lostDialog.value = { open: true, row, reason: '' }
}

async function submitLost() {
  const row = lostDialog.value.row
  if (!row) return
  if (!lostDialog.value.reason.trim()) {
    appStore.toast('请填写流失原因', 'error')
    return
  }
  try {
    await opportunityApi.lost(row.id, lostDialog.value.reason.trim())
    appStore.toast('已标记流失')
    lostDialog.value.open = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function remove(row: CrmOpportunity) {
  if (!window.confirm(`确认删除商机「${row.name}」？`)) return
  try {
    await opportunityApi.remove(row.id)
    appStore.toast('商机已删除')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

// ---------------- 详情：竞对 + 时间线 ----------------
const detailOpen = ref(false)
const detail = ref<CrmOpportunity | null>(null)
const detailTab = ref<'overview' | 'competitors' | 'timeline'>('overview')
const competitors = ref<CrmCompetitor[]>([])
const compareData = ref<Record<string, any> | null>(null)
const timeline = ref<CrmActivity[]>([])

async function openDetail(row: CrmOpportunity) {
  detailTab.value = 'overview'
  competitors.value = []
  compareData.value = null
  timeline.value = []
  try {
    detail.value = await opportunityApi.detail(row.id)
    detailOpen.value = true
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function switchTab(tab: typeof detailTab.value) {
  detailTab.value = tab
  if (!detail.value) return
  try {
    if (tab === 'competitors') {
      competitors.value = await opportunityApi.competitors(detail.value.id)
      compareData.value = await opportunityApi.compare(detail.value.id)
    } else if (tab === 'timeline') {
      timeline.value = await activityApi.timeline('OPPORTUNITY', detail.value.id)
    }
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

const competitorOpen = ref(false)
const competitorForm = reactive<Record<string, any>>({})

function openCompetitor() {
  Object.assign(competitorForm, {
    competitorName: '',
    staffSize: null,
    coverageScope: '',
    techAbility: '',
    pricingMode: '',
    pricingDetail: '',
    operationEfficiency: '',
    operationQuality: '',
    remark: ''
  })
  competitorOpen.value = true
}

async function submitCompetitor() {
  if (!detail.value) return
  if (!competitorForm.competitorName) {
    appStore.toast('请填写竞对名称', 'error')
    return
  }
  try {
    await opportunityApi.addCompetitor(detail.value.id, { ...competitorForm })
    appStore.toast('竞对已添加')
    competitorOpen.value = false
    await switchTab('competitors')
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function removeCompetitor(row: CrmCompetitor) {
  if (!detail.value) return
  if (!window.confirm(`确认删除竞对「${row.competitorName}」？`)) return
  try {
    await opportunityApi.removeCompetitor(detail.value.id, row.id)
    appStore.toast('竞对已删除')
    await switchTab('competitors')
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function stageTag(stage?: string) {
  if (stage === 'WON') return 'is-success'
  if (stage === 'LOST') return 'is-danger'
  if (stage === 'NEGOTIATION') return 'is-solid'
  if (stage === 'PROPOSAL') return 'is-warning'
  return 'is-muted'
}

onMounted(async () => {
  await dict.loadDicts()
  await searchAccounts()
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="商机管理" desc="阶段单向推进 · 利润率服务端计算">
      <template #actions>
        <button class="pm-btn" @click="$router.push('/crm/opportunity-funnel')">
          <i class="ti ti-chart-funnel" /> 漏斗视图
        </button>
        <button v-if="canAdd && !readOnly" class="pm-btn is-primary" @click="openCreate">
          <i class="ti ti-plus" /> 新增商机
        </button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="query.keyword" class="pm-input" placeholder="商机名称" @keyup.enter="load" />
        </div>
        <div class="pm-field">
          <label>阶段</label>
          <select v-model="query.stage" class="pm-select">
            <option value="">全部</option>
            <option v-for="item in dict.options('crm_sales_stage')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-field">
          <label>客户</label>
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
              <th>商机名称</th>
              <th>客户</th>
              <th>阶段</th>
              <th>预计金额</th>
              <th>预计成交</th>
              <th>利润率</th>
              <th>赢率</th>
              <th>负责人</th>
              <th style="width: 250px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td style="font-weight: 600">{{ row.name }}</td>
              <td>{{ accountName(row.accountId) }}</td>
              <td>
                <span class="pm-tag" :class="stageTag(row.salesStage)">
                  {{ dict.label('crm_sales_stage', row.salesStage) }}
                </span>
                <div v-if="row.lostReason" style="font-size: 12px; color: var(--pm-danger)">{{ row.lostReason }}</div>
              </td>
              <td>{{ money(row.expectedAmount) }}</td>
              <td>{{ dateText(row.expectedCloseDate) }}</td>
              <td>{{ row.estProfitRate ?? '—' }}%</td>
              <td>{{ row.winProbability ?? '—' }}%</td>
              <td>{{ row.ownerUserName || '—' }}</td>
              <td>
                <div class="pm-actions">
                  <button class="pm-btn is-ghost is-sm" @click="openDetail(row)">详情</button>
                  <button v-if="canEdit && !readOnly" class="pm-btn is-ghost is-sm" @click="openEdit(row)">编辑</button>
                  <button
                    v-if="canStage && !readOnly && row.salesStage !== 'WON' && row.salesStage !== 'LOST'"
                    class="pm-btn is-ghost is-sm"
                    @click="openStage(row)"
                  >
                    推进
                  </button>
                  <button
                    v-if="canLost && !readOnly && row.salesStage !== 'WON' && row.salesStage !== 'LOST'"
                    class="pm-btn is-ghost is-sm"
                    @click="openLost(row)"
                  >
                    流失
                  </button>
                  <button v-if="canRemove && !readOnly" class="pm-btn is-ghost is-sm" @click="remove(row)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="loaded && list.length === 0">
              <td colspan="9" class="pm-empty">没有符合条件的商机</td>
            </tr>
            <tr v-else-if="loading">
              <td colspan="9" class="pm-empty">加载中…</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>

    <!-- 新增 / 编辑 -->
    <FormDrawer
      :open="formOpen"
      :title="form.id ? '编辑商机' : '新增商机'"
      :wide="true"
      :loading="saving"
      @close="formOpen = false"
      @submit="submit"
    >
      <div class="pm-form-grid">
        <div class="pm-form-item is-full">
          <label><span class="pm-required">*</span>商机名称</label>
          <input v-model="form.name" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>客户</label>
          <select v-model="form.accountId" class="pm-select">
            <option :value="null">请选择</option>
            <option v-for="item in accountOptions" :key="item.id" :value="item.id">{{ item.companyName }}</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>预计金额（元）</label>
          <input v-model.number="form.expectedAmount" type="number" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>预计成交日期</label>
          <input v-model="form.expectedCloseDate" type="date" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>计价口径</label>
          <select v-model="form.pricingMode" class="pm-select">
            <option v-for="item in dict.options('crm_pricing_mode')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div v-if="PRICING_FIELD[form.pricingMode as string]" class="pm-form-item">
          <label><span class="pm-required">*</span>{{ PRICING_FIELD[form.pricingMode as string].label }}</label>
          <input v-model.number="pricingValue" type="number" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>预计成本（元）</label>
          <input v-model.number="form.estCost" type="number" class="pm-input" placeholder="利润率由服务端计算" />
        </div>
        <div class="pm-form-item">
          <label>预计服务量</label>
          <input v-model="form.estServiceVolume" class="pm-input" placeholder="如 12 万次查询" />
        </div>
        <div class="pm-form-item is-full">
          <label>服务地点</label>
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
      :title="`商机详情 · ${detail?.name ?? ''}`"
      :wide="true"
      @close="detailOpen = false"
      @submit="detailOpen = false"
    >
      <div class="pm-tabs">
        <div class="pm-tab" :class="{ 'is-active': detailTab === 'overview' }" @click="switchTab('overview')">基本信息</div>
        <div class="pm-tab" :class="{ 'is-active': detailTab === 'competitors' }" @click="switchTab('competitors')">
          竞对分析
        </div>
        <div class="pm-tab" :class="{ 'is-active': detailTab === 'timeline' }" @click="switchTab('timeline')">跟进记录</div>
      </div>

      <div v-if="detailTab === 'overview'">
        <table class="pm-table">
          <tbody>
            <tr><th style="width: 150px">客户</th><td>{{ accountName(detail?.accountId) }}</td></tr>
            <tr><th>阶段</th><td>{{ dict.label('crm_sales_stage', detail?.salesStage) }}</td></tr>
            <tr><th>预计金额</th><td>{{ money(detail?.expectedAmount) }}</td></tr>
            <tr><th>预计成本</th><td>{{ money(detail?.estCost) }}</td></tr>
            <tr><th>预计收入</th><td>{{ money(detail?.estRevenue) }}</td></tr>
            <tr><th>利润率</th><td>{{ detail?.estProfitRate ?? '—' }}%</td></tr>
            <tr><th>计价口径</th><td>{{ dict.label('crm_pricing_mode', detail?.pricingMode) }}</td></tr>
            <tr><th>计价明细</th><td>{{ detail?.pricingDetail || '—' }}</td></tr>
            <tr><th>预计服务量</th><td>{{ detail?.estServiceVolume || '—' }}</td></tr>
            <tr><th>预计成交</th><td>{{ dateText(detail?.expectedCloseDate) }}</td></tr>
            <tr><th>阶段变更时间</th><td>{{ dateTimeText(detail?.stageChangeTime) }}</td></tr>
            <tr><th>流失原因</th><td>{{ detail?.lostReason || '—' }}</td></tr>
          </tbody>
        </table>
      </div>

      <div v-else-if="detailTab === 'competitors'">
        <div class="pm-actions" style="justify-content: flex-end; margin-bottom: 8px">
          <button v-if="canEdit && !readOnly" class="pm-btn is-primary is-sm" @click="openCompetitor">添加竞对</button>
        </div>
        <div v-if="competitors.length === 0" class="pm-empty">尚未录入竞争对手</div>
        <table v-else class="pm-table">
          <thead>
            <tr><th>竞对</th><th>规模</th><th>覆盖</th><th>技术</th><th>价格</th><th>运营效率</th><th>运营质量</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in competitors" :key="row.id">
              <td style="font-weight: 600">{{ row.competitorName }}</td>
              <td>{{ row.staffSize ?? '—' }}</td>
              <td>{{ row.coverageScope || '—' }}</td>
              <td>{{ row.techAbility || '—' }}</td>
              <td>{{ row.pricingMode || '—' }}</td>
              <td>{{ row.operationEfficiency || '—' }}</td>
              <td>{{ row.operationQuality || '—' }}</td>
              <td>
                <button v-if="canEdit && !readOnly" class="pm-btn is-ghost is-sm" @click="removeCompetitor(row)">
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <h3 style="font-size: 14px; margin: 20px 0 8px">对比矩阵</h3>
        <div v-if="!compareData" class="pm-empty">加载中…</div>
        <table v-else class="pm-table">
          <thead>
            <tr>
              <th>维度</th>
              <th>我方</th>
              <th v-for="c in (compareData.competitors as Record<string, any>[])" :key="c.id">{{ c.name }}</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>人员规模</td>
              <td>{{ (compareData.ours as Record<string, any>).staffSize ?? '—' }}</td>
              <td v-for="c in (compareData.competitors as Record<string, any>[])" :key="`s-${c.id}`">
                {{ c.staffSize ?? '—' }}
              </td>
            </tr>
            <tr>
              <td>覆盖范围</td>
              <td>{{ (compareData.ours as Record<string, any>).coverageScope ?? '—' }}</td>
              <td v-for="c in (compareData.competitors as Record<string, any>[])" :key="`c-${c.id}`">
                {{ c.coverageScope ?? '—' }}
              </td>
            </tr>
            <tr>
              <td>技术能力</td>
              <td>{{ (compareData.ours as Record<string, any>).techAbility ?? '—' }}</td>
              <td v-for="c in (compareData.competitors as Record<string, any>[])" :key="`t-${c.id}`">
                {{ c.techAbility ?? '—' }}
              </td>
            </tr>
            <tr>
              <td>计价口径</td>
              <td>{{ (compareData.ours as Record<string, any>).pricingMode ?? '—' }}</td>
              <td v-for="c in (compareData.competitors as Record<string, any>[])" :key="`p-${c.id}`">
                {{ c.pricingMode ?? '—' }}
              </td>
            </tr>
            <tr>
              <td>运营效率</td>
              <td>{{ (compareData.ours as Record<string, any>).operationEfficiency ?? '—' }}</td>
              <td v-for="c in (compareData.competitors as Record<string, any>[])" :key="`e-${c.id}`">
                {{ c.operationEfficiency ?? '—' }}
              </td>
            </tr>
            <tr>
              <td>运营质量</td>
              <td>{{ (compareData.ours as Record<string, any>).operationQuality ?? '—' }}</td>
              <td v-for="c in (compareData.competitors as Record<string, any>[])" :key="`q-${c.id}`">
                {{ c.operationQuality ?? '—' }}
              </td>
            </tr>
            <tr>
              <td>预计服务量</td>
              <td>{{ (compareData.ours as Record<string, any>).estServiceVolume ?? '—' }}</td>
              <td v-for="c in (compareData.competitors as Record<string, any>[])" :key="`v-${c.id}`">—</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else>
        <div v-if="timeline.length === 0" class="pm-empty">暂无跟进记录</div>
        <div v-else class="pm-timeline">
          <div v-for="item in timeline" :key="item.id" class="pm-timeline-item">
            <div class="pm-timeline-dot" :class="{ 'is-done': item.status !== 'VOID' }" />
            <div>
              <div style="font-weight: 600">{{ item.subject }}</div>
              <div style="font-size: 12px; color: var(--pm-neutral-500)">
                {{ dict.label('crm_activity_type', item.type) }} · {{ dateTimeText(item.startTime) }}
              </div>
              <div v-if="item.description" style="font-size: 13px; margin-top: 4px">{{ item.description }}</div>
            </div>
          </div>
        </div>
      </div>
    </FormDrawer>

    <!-- 添加竞对 -->
    <FormDrawer
      :open="competitorOpen"
      title="添加竞争对手"
      :wide="true"
      @close="competitorOpen = false"
      @submit="submitCompetitor"
    >
      <div class="pm-form-grid">
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>竞对名称</label>
          <input v-model="competitorForm.competitorName" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>人员规模</label>
          <input v-model.number="competitorForm.staffSize" type="number" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>覆盖范围</label>
          <input v-model="competitorForm.coverageScope" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>技术能力</label>
          <input v-model="competitorForm.techAbility" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>计价口径</label>
          <input v-model="competitorForm.pricingMode" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>计价明细</label>
          <input v-model="competitorForm.pricingDetail" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>运营效率</label>
          <input v-model="competitorForm.operationEfficiency" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>运营质量</label>
          <input v-model="competitorForm.operationQuality" class="pm-input" />
        </div>
        <div class="pm-form-item is-full">
          <label>备注</label>
          <textarea v-model="competitorForm.remark" class="pm-input" />
        </div>
      </div>
    </FormDrawer>

    <!-- 阶段推进 -->
    <div v-if="stageDialog.open" class="pm-drawer-mask" @click.self="stageDialog.open = false">
      <div class="pm-card" style="width: 440px; margin: auto">
        <h3 style="margin: 0 0 12px; font-size: 15px">推进阶段 · {{ stageDialog.row?.name }}</h3>
        <div class="pm-form-item">
          <label>目标阶段</label>
          <select v-model="stageDialog.target" class="pm-select">
            <option v-for="stage in nextStages(stageDialog.row as CrmOpportunity)" :key="stage" :value="stage">
              {{ dict.label('crm_sales_stage', stage) }}
            </option>
          </select>
        </div>
        <div class="pm-form-item" style="margin-top: 10px">
          <label>推进说明</label>
          <textarea v-model="stageDialog.reason" class="pm-input" rows="2" />
        </div>
        <div class="pm-actions" style="justify-content: flex-end; margin-top: 12px">
          <button class="pm-btn" @click="stageDialog.open = false">取消</button>
          <button class="pm-btn is-primary" @click="submitStage">确定</button>
        </div>
      </div>
    </div>

    <!-- 流失 -->
    <div v-if="lostDialog.open" class="pm-drawer-mask" @click.self="lostDialog.open = false">
      <div class="pm-card" style="width: 440px; margin: auto">
        <h3 style="margin: 0 0 12px; font-size: 15px">标记流失 · {{ lostDialog.row?.name }}</h3>
        <textarea v-model="lostDialog.reason" class="pm-input" rows="3" placeholder="流失原因（必填）" />
        <div class="pm-actions" style="justify-content: flex-end; margin-top: 12px">
          <button class="pm-btn" @click="lostDialog.open = false">取消</button>
          <button class="pm-btn is-danger" @click="submitLost">确认流失</button>
        </div>
      </div>
    </div>
  </div>
</template>
