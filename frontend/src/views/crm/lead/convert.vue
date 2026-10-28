<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import Pagination from '@/components/Pagination.vue'
import { accountApi, crmCommonApi, leadApi } from '@/api/crm'
import type { CrmAccount, CrmLead } from '@/api/crm'
import { dateTimeText, useCrmDict } from '../shared/useCrmDict'
import { useCrmList } from '../shared/useCrmList'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const dict = useCrmDict(['crm_lead_status', 'crm_lead_grade', 'crm_lead_source'])

const { list, total, loading, loaded, pageNum, pageSize, query, load, reset } = useCrmList<CrmLead>(
  leadApi.page,
  { keyword: '', status: 'NEW', grade: '' }
)

const canConvert = computed(() => userStore.hasPerm('crm:lead:convert'))
const readOnly = computed(() => userStore.profile?.readOnly ?? false)

// ---------------- 转化表单 ----------------
const convertOpen = ref(false)
const converting = ref(false)
const current = ref<CrmLead | null>(null)
const form = reactive<Record<string, any>>({
  convertMode: 'NEW_ACCOUNT',
  accountId: null,
  productId: null,
  opportunityName: '',
  expectedAmount: null
})

const accountKeyword = ref('')
const accountOptions = ref<CrmAccount[]>([])
const products = ref<{ id: number; name: string }[]>([])
const productDegraded = ref(false)
const productHint = ref('')

async function searchAccounts() {
  try {
    const data = await accountApi.page({ keyword: accountKeyword.value || undefined, pageSize: 20 })
    accountOptions.value = data.list
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function openConvert(row: CrmLead) {
  current.value = row
  Object.assign(form, {
    convertMode: 'NEW_ACCOUNT',
    accountId: null,
    productId: null,
    opportunityName: row.companyName ? `${row.companyName}-数据服务` : '',
    expectedAmount: null
  })
  convertOpen.value = true
  if (accountOptions.value.length === 0) {
    searchAccounts()
  }
}

async function submitConvert() {
  if (!current.value) return
  if (form.convertMode === 'EXIST_ACCOUNT' && !form.accountId) {
    appStore.toast('请选择要关联的客户', 'error')
    return
  }
  converting.value = true
  try {
    const result = await leadApi.convert(current.value.id, {
      convertMode: form.convertMode,
      accountId: form.convertMode === 'EXIST_ACCOUNT' ? form.accountId : undefined,
      productId: form.productId ?? undefined,
      opportunityName: form.opportunityName || undefined,
      expectedAmount: form.expectedAmount ?? undefined
    })
    if (result.idempotent) {
      appStore.toast('该线索此前已转化过，已返回既有结果（未重复创建）')
    } else {
      appStore.toast('转化成功：已生成客户 / 联系人 / 商机')
    }
    convertOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    converting.value = false
  }
}

async function loadProducts() {
  try {
    const data = await crmCommonApi.products()
    productDegraded.value = data.degraded
    productHint.value = data.message ?? ''
    products.value = (data.list as { id: number; name: string }[]) ?? []
  } catch {
    productDegraded.value = true
    productHint.value = '服务产品加载失败，可先不选'
  }
}

onMounted(async () => {
  await dict.loadDicts()
  await loadProducts()
  if (route.query.leadId) {
    const leadId = Number(route.query.leadId)
    const target = list.value.find((item) => item.id === leadId)
    if (target) {
      openConvert(target)
    } else {
      try {
        const lead = await leadApi.detail(leadId)
        openConvert(lead)
      } catch {
        // 线索不可见时静默
      }
    }
  }
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="线索转化工作台" desc="转化幂等：重复提交不会重复建客户" />

    <div class="pm-card" style="background: var(--pm-primary-50)">
      <div style="font-size: 13px; line-height: 1.7">
        转化会一次性生成 <b>客户 → 联系人 → 商机</b> 三段数据，并回写线索的转化标记。
        重复点击或并发提交由「转化标记短路 + 行锁 + 唯一索引」三重保障，不会产生重复客户。
      </div>
    </div>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="query.keyword" class="pm-input" placeholder="姓名 / 公司" @keyup.enter="load" />
        </div>
        <div class="pm-field">
          <label>状态</label>
          <select v-model="query.status" class="pm-select">
            <option value="">全部</option>
            <option v-for="item in dict.options('crm_lead_status')" :key="item.dictValue" :value="item.dictValue">
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
              <th>姓名 / 公司</th>
              <th>等级</th>
              <th>来源</th>
              <th>状态</th>
              <th>负责人</th>
              <th>最近跟进</th>
              <th>转化结果</th>
              <th style="width: 130px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td>
                <div style="font-weight: 600">{{ row.name }}</div>
                <div style="font-size: 12px; color: var(--pm-neutral-500)">{{ row.companyName || '—' }}</div>
              </td>
              <td>{{ dict.label('crm_lead_grade', row.grade) }}</td>
              <td>{{ dict.label('crm_lead_source', row.sourceChannel) }}</td>
              <td>{{ dict.label('crm_lead_status', row.status) }}</td>
              <td>{{ row.ownerUserName || '—' }}</td>
              <td>{{ dateTimeText(row.lastFollowTime) }}</td>
              <td>
                <span v-if="row.convertedAccountId" class="pm-tag is-success">客户 #{{ row.convertedAccountId }}</span>
                <span v-else class="pm-tag is-muted">未转化</span>
              </td>
              <td>
                <button
                  v-if="canConvert && !readOnly && row.status !== 'CONVERTED' && row.status !== 'INVALID'"
                  class="pm-btn is-primary is-sm"
                  @click="openConvert(row)"
                >
                  转化
                </button>
                <span v-else class="pm-tag is-muted">不可转化</span>
              </td>
            </tr>
            <tr v-if="loaded && list.length === 0">
              <td colspan="8" class="pm-empty">没有待转化的线索</td>
            </tr>
            <tr v-else-if="loading">
              <td colspan="8" class="pm-empty">加载中…</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>

    <div v-if="convertOpen" class="pm-drawer-mask" @click.self="convertOpen = false">
      <div class="pm-card" style="width: 560px; margin: auto">
        <h3 style="margin: 0 0 4px; font-size: 15px">转化线索 · {{ current?.name }}</h3>
        <p style="font-size: 13px; color: var(--pm-neutral-500); margin: 0 0 12px">
          {{ current?.companyName || '未填公司名称' }}
        </p>

        <div class="pm-form-grid">
          <div class="pm-form-item is-full">
            <label><span class="pm-required">*</span>客户处理方式</label>
            <select v-model="form.convertMode" class="pm-select">
              <option value="NEW_ACCOUNT">新建客户</option>
              <option value="EXIST_ACCOUNT">关联已有客户</option>
            </select>
          </div>

          <template v-if="form.convertMode === 'EXIST_ACCOUNT'">
            <div class="pm-form-item is-full">
              <label>搜索已有客户</label>
              <div style="display: flex; gap: 8px">
                <input v-model="accountKeyword" class="pm-input" placeholder="客户全称" @keyup.enter="searchAccounts" />
                <button class="pm-btn" @click="searchAccounts">搜索</button>
              </div>
            </div>
            <div class="pm-form-item is-full">
              <label><span class="pm-required">*</span>选择客户</label>
              <select v-model="form.accountId" class="pm-select">
                <option :value="null">请选择</option>
                <option v-for="item in accountOptions" :key="item.id" :value="item.id">
                  {{ item.companyName }}
                </option>
              </select>
            </div>
          </template>

          <div class="pm-form-item is-full">
            <label>服务产品</label>
            <select v-model="form.productId" class="pm-select" :disabled="productDegraded">
              <option :value="null">{{ productDegraded ? '（项目模块未接入，暂不可选）' : '请选择' }}</option>
              <option v-for="item in products" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
            <div v-if="productDegraded" style="font-size: 12px; color: var(--pm-neutral-500); margin-top: 4px">
              {{ productHint }}
            </div>
          </div>

          <div class="pm-form-item is-full">
            <label>商机名称</label>
            <input v-model="form.opportunityName" class="pm-input" placeholder="默认沿用公司名称" />
          </div>
          <div class="pm-form-item is-full">
            <label>预计金额（元）</label>
            <input v-model.number="form.expectedAmount" type="number" class="pm-input" />
          </div>
        </div>

        <div class="pm-actions" style="justify-content: flex-end; margin-top: 16px">
          <button class="pm-btn" @click="convertOpen = false">取消</button>
          <button class="pm-btn is-primary" :disabled="converting" @click="submitConvert">
            {{ converting ? '转化中…' : '确认转化' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
