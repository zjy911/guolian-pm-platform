<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import Pagination from '@/components/Pagination.vue'
import { accountApi } from '@/api/crm'
import type { CrmAccount } from '@/api/crm'
import { dateTimeText, money, useCrmDict } from '../shared/useCrmDict'
import { useCrmList } from '../shared/useCrmList'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const dict = useCrmDict(['crm_industry', 'crm_account_nature', 'crm_account_type', 'crm_pool_status'])

const { list, total, loading, loaded, pageNum, pageSize, query, load, reset } = useCrmList<CrmAccount>(
  accountApi.pool,
  { keyword: '' }
)

const canClaim = computed(() => userStore.hasPerm('crm:account:claim'))
const readOnly = computed(() => userStore.profile?.readOnly ?? false)

const claimDialog = ref<{ open: boolean; row: CrmAccount | null; reason: string }>({
  open: false,
  row: null,
  reason: ''
})

function openClaim(row: CrmAccount) {
  claimDialog.value = { open: true, row, reason: '' }
}

async function submitClaim() {
  const row = claimDialog.value.row
  if (!row) return
  try {
    await accountApi.claim(row.id, claimDialog.value.reason || undefined)
    appStore.toast('认领申请已提交，等待审批')
    claimDialog.value.open = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

onMounted(async () => {
  await dict.loadDicts()
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="公共客户池" desc="全员可见 · 只读 + 脱敏，认领需审批" />

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="query.keyword" class="pm-input" placeholder="客户全称" @keyup.enter="load" />
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
              <th>入池时间</th>
              <th>入池原因</th>
              <th>原负责人</th>
              <th>年营收</th>
              <th style="width: 140px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td>
                <div style="font-weight: 600">{{ row.companyName }}</div>
                <div style="font-size: 12px; color: var(--pm-neutral-500)">{{ row.province }}{{ row.city }}</div>
              </td>
              <td>
                {{ dict.label('crm_industry', row.industry) }}
                <div style="font-size: 12px; color: var(--pm-neutral-500)">
                  {{ dict.label('crm_account_nature', row.accountNature) }}
                </div>
              </td>
              <td>{{ dict.label('crm_account_type', row.accountType) }}</td>
              <td>{{ dateTimeText(row.poolInTime) }}</td>
              <td>{{ row.poolReason || '—' }}</td>
              <td>{{ row.ownerUserName || '—' }}</td>
              <td>{{ money(row.annualRevenue) }}</td>
              <td>
                <button v-if="canClaim && !readOnly" class="pm-btn is-primary is-sm" @click="openClaim(row)">
                  申请认领
                </button>
                <span v-else class="pm-tag is-muted">只读</span>
              </td>
            </tr>
            <tr v-if="loaded && list.length === 0">
              <td colspan="8" class="pm-empty">公共池暂无客户</td>
            </tr>
            <tr v-else-if="loading">
              <td colspan="8" class="pm-empty">加载中…</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>

    <div v-if="claimDialog.open" class="pm-drawer-mask" @click.self="claimDialog.open = false">
      <div class="pm-card" style="width: 460px; margin: auto">
        <h3 style="margin: 0 0 8px; font-size: 15px">申请认领 · {{ claimDialog.row?.companyName }}</h3>
        <p style="font-size: 13px; color: var(--pm-neutral-500); margin: 0 0 12px">
          提交后进入 CRM_CLAIM 审批流；审批通过后客户归你私有，原负责人可在申诉期内提出申诉。
        </p>
        <textarea v-model="claimDialog.reason" class="pm-input" rows="3" placeholder="认领理由（选填）" />
        <div class="pm-actions" style="justify-content: flex-end; margin-top: 12px">
          <button class="pm-btn" @click="claimDialog.open = false">取消</button>
          <button class="pm-btn is-primary" @click="submitClaim">提交申请</button>
        </div>
      </div>
    </div>
  </div>
</template>
