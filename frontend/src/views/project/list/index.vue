<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import Pagination from '@/components/Pagination.vue'
import { productApi, projectApi, PM_PHASES, PM_PRICING, PM_STATUS, healthTag, type PmProject } from '@/api/pm'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const router = useRouter()
const list = ref<PmProject[]>([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const query = reactive({ keyword: '', phase: null as number | null, healthLight: '' })

const initOpen = ref(false)
const saving = ref(false)
const initForm = reactive({
  productId: null as number | null,
  projectName: '',
  customerId: null as number | null,
  customerName: '',
  contractNo: '',
  signEntity: '',
  estimatedAmount: null as number | null,
  annualCases: null as number | null,
  pricingMode: 'UNIT_PRICE',
  rating: 'B',
  risks: { 亏损: 2, 政策: 2, 竞争: 2, 用工合规: 2 } as Record<string, number>
})
const products = ref<{ id: number; productName: string; pricingMode: string }[]>([])

async function load() {
  loading.value = true
  try {
    const data = await projectApi.page({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: query.keyword || undefined,
      phase: query.phase ?? undefined,
      healthLight: query.healthLight || undefined
    })
    list.value = data.list
    total.value = data.total
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

async function openInitiation() {
  initForm.productId = null
  initForm.projectName = ''
  initForm.customerId = null
  initForm.customerName = ''
  initForm.contractNo = ''
  initForm.signEntity = ''
  initForm.estimatedAmount = null
  initForm.annualCases = null
  initForm.pricingMode = 'UNIT_PRICE'
  initForm.rating = 'B'
  initOpen.value = true
  void loadProductOptions()
}

async function loadProductOptions() {
  try {
    const data = await productApi.page({ pageNum: 1, pageSize: 100, status: 'ON_SALE' })
    products.value = data.list.map((p) => ({ id: p.id, productName: p.productName, pricingMode: p.pricingMode }))
  } catch {
    products.value = []
  }
}

function onProductChange() {
  const product = products.value.find((p) => p.id === initForm.productId)
  if (product) {
    initForm.pricingMode = product.pricingMode
  }
}

async function submitInitiation() {
  if (!initForm.productId || !initForm.projectName || !initForm.customerId || !initForm.estimatedAmount) {
    appStore.toast('请完整填写产品、项目名称、客户主体与预估合同额', 'error')
    return
  }
  saving.value = true
  try {
    const result = await projectApi.createInitiation({
      productId: initForm.productId,
      projectName: initForm.projectName,
      customerId: initForm.customerId,
      customerName: initForm.customerName || undefined,
      contractNo: initForm.contractNo || undefined,
      signEntity: initForm.signEntity || undefined,
      estimatedAmount: initForm.estimatedAmount,
      annualCases: initForm.annualCases || undefined,
      pricingMode: initForm.pricingMode,
      rating: initForm.rating,
      formData: JSON.stringify({ risks: initForm.risks })
    })
    appStore.toast(`立项申请已创建（${result.projectId}），请到工作台提交审批`)
    initOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

async function advance(row: PmProject) {
  if (!confirm(`确认将「${row.projectName}」推进到下一环节？阻塞任务未完成将被阻断。`)) return
  try {
    const updated = await projectApi.advance(row.id)
    appStore.toast(`已推进至 ${PM_PHASES[(updated.phase ?? 1) - 1]}`)
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function amountText(value?: number | null) {
  if (value == null) return '—'
  return `${(value / 10000).toLocaleString()} 万`
}

const userStore = useUserStore()
const canAdvance = () => userStore.hasPerm('project:project:list')

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="项目管理" desc="运行态：立项申请、项目列表与单项目工作台">
      <template #actions>
        <button class="pm-btn is-primary" @click="openInitiation"><i class="ti ti-rocket" /> 新建立项申请</button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="query.keyword" class="pm-input" placeholder="项目编号 / 名称 / 客户" @keyup.enter="load" />
        </div>
        <div class="pm-field">
          <label>环节</label>
          <select v-model="query.phase" class="pm-select">
            <option :value="null">全部环节</option>
            <option v-for="(name, index) in PM_PHASES" :key="index" :value="index + 1">{{ name }}</option>
          </select>
        </div>
        <div class="pm-field">
          <label>健康度</label>
          <select v-model="query.healthLight" class="pm-select">
            <option value="">全部</option>
            <option value="GREEN">绿灯</option>
            <option value="YELLOW">黄灯</option>
            <option value="RED">红灯</option>
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
              <th>项目编号</th><th>项目名称</th><th>产品</th><th>客户</th><th>预估合同额</th>
              <th>评级</th><th>环节</th><th>健康度</th><th>毛利率</th><th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td>{{ row.projectCode }}</td>
              <td>
                <a style="cursor: pointer; color: var(--pm-primary-600)" @click="router.push(`/project/workspace/${row.id}`)">
                  {{ row.projectName }}
                </a>
              </td>
              <td>{{ row.productName }}</td>
              <td>{{ row.customerName }}</td>
              <td>{{ amountText(row.estimatedAmount) }}</td>
              <td><span class="pm-tag is-solid">{{ row.rating ?? '—' }}</span></td>
              <td><span class="pm-tag is-muted">{{ PM_PHASES[(row.phase ?? 1) - 1] }} · {{ PM_STATUS[row.status] ?? row.status }}</span></td>
              <td>
                <span class="pm-tag" :class="healthTag(row.healthLight)">
                  {{ row.healthScore != null ? row.healthScore : '—' }}
                </span>
              </td>
              <td>{{ row.grossMargin != null ? `${(row.grossMargin * 100).toFixed(1)}%` : '—' }}</td>
              <td style="white-space: nowrap">
                <button class="pm-btn is-sm" @click="router.push(`/project/workspace/${row.id}`)">工作台</button>
                <button v-if="canAdvance() && (row.phase ?? 1) < 6" class="pm-btn is-sm" style="margin-left: 6px" @click="advance(row)">
                  推进环节
                </button>
              </td>
            </tr>
            <tr v-if="list.length === 0"><td colspan="10" class="pm-empty">暂无项目，点击右上角「新建立项申请」</td></tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:pageNum="pageNum" v-model:pageSize="pageSize" :total="total" />
    </div>

    <!-- 立项申请 -->
    <div v-if="initOpen" class="pm-drawer-mask" @click.self="initOpen = false">
      <div class="pm-card" style="width: 560px; margin: auto; max-height: 88vh; overflow: auto">
        <h3 style="margin: 0 0 12px; font-size: 15px">立项申请单</h3>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>服务产品</label>
          <select v-model="initForm.productId" class="pm-select" @change="onProductChange">
            <option :value="null">请选择</option>
            <option v-for="p in products" :key="p.id" :value="p.id">{{ p.productName }}</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>项目名称</label>
          <input v-model="initForm.projectName" class="pm-input" placeholder="默认取「客户名称+服务产品」" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>客户主体 ID</label>
          <input v-model.number="initForm.customerId" type="number" class="pm-input" placeholder="引 CRM 客户 ID" />
        </div>
        <div class="pm-form-item">
          <label>客户名称</label>
          <input v-model="initForm.customerName" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>关联合同号</label>
          <input v-model="initForm.contractNo" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>我方签约主体</label>
          <input v-model="initForm.signEntity" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>预估合同额（元）</label>
          <input v-model.number="initForm.estimatedAmount" type="number" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>年化案件量</label>
          <input v-model.number="initForm.annualCases" type="number" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>计价收费口径</label>
          <select v-model="initForm.pricingMode" class="pm-select">
            <option v-for="(label, key) in PM_PRICING" :key="key" :value="key">{{ label }}</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>项目评级</label>
          <select v-model="initForm.rating" class="pm-select">
            <option value="A">A</option><option value="B">B</option><option value="C">C</option><option value="D">D</option>
          </select>
        </div>
        <div class="pm-form-item is-full">
          <label>四维风险评估（1-5 分）</label>
          <div style="display: flex; gap: 12px">
            <div v-for="(value, key) in initForm.risks" :key="key" style="flex: 1">
              <div style="font-size: 12px; margin-bottom: 4px">{{ key }}</div>
              <input v-model.number="initForm.risks[key]" type="number" min="1" max="5" class="pm-input" />
            </div>
          </div>
        </div>
        <div style="display: flex; justify-content: flex-end; gap: 8px">
          <button class="pm-btn" @click="initOpen = false">取消</button>
          <button class="pm-btn is-primary" :disabled="saving" @click="submitInitiation">创建立项申请</button>
        </div>
      </div>
    </div>
  </div>
</template>
