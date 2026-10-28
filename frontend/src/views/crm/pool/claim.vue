<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import Pagination from '@/components/Pagination.vue'
import { claimApi } from '@/api/crm'
import type { CrmClaimVO } from '@/api/crm'
import { dateTimeText, useCrmDict } from '../shared/useCrmDict'
import { useCrmList } from '../shared/useCrmList'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const router = useRouter()
const dict = useCrmDict(['crm_claim_status'])

const { list, total, loading, loaded, pageNum, pageSize, query, load, reset } = useCrmList<CrmClaimVO>(
  claimApi.page,
  { keyword: '', status: 'APPROVING' }
)

const pendingCount = ref(0)
const canApprove = computed(() => userStore.hasPerm('crm:account:claim'))

function statusTag(status?: string) {
  if (status === 'APPROVED') return 'is-success'
  if (status === 'APPROVING') return 'is-warning'
  if (status === 'REJECTED') return 'is-danger'
  return 'is-muted'
}

function appealLabel(status?: string) {
  return { NONE: '无申诉', APPEALED: '已申诉', EXPIRED: '申诉期已过' }[status ?? ''] ?? '—'
}

onMounted(async () => {
  await dict.loadDicts()
  await load()
  try {
    pendingCount.value = (await claimApi.pendingCount()).count
  } catch {
    pendingCount.value = 0
  }
})
</script>

<template>
  <div>
    <PageHeader title="客户认领审批" :desc="`待审批 ${pendingCount} 条 · 审批动作在审批中心处理`">
      <template #actions>
        <button class="pm-btn is-primary" @click="router.push('/flow/center')">
          <i class="ti ti-checks" /> 去审批中心
        </button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="query.keyword" class="pm-input" placeholder="客户名称 / 申请人" @keyup.enter="load" />
        </div>
        <div class="pm-field">
          <label>审批状态</label>
          <select v-model="query.status" class="pm-select">
            <option value="">全部</option>
            <option v-for="item in dict.options('crm_claim_status')" :key="item.dictValue" :value="item.dictValue">
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
      <div v-if="!canApprove" class="pm-empty">你没有认领审批权限（crm:account:claim）</div>
      <div v-else class="pm-table-wrap">
        <table class="pm-table">
          <thead>
            <tr>
              <th>客户</th>
              <th>申请人</th>
              <th>原负责人</th>
              <th>申请理由</th>
              <th>状态</th>
              <th>申诉</th>
              <th>申诉截止</th>
              <th>申请时间</th>
              <th style="width: 120px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td>{{ row.accountName || `#${row.accountId}` }}</td>
              <td>{{ row.applicantName || '—' }}</td>
              <td>{{ row.oldOwnerName || '—' }}</td>
              <td style="max-width: 220px">{{ row.applyReason || '—' }}</td>
              <td>
                <span class="pm-tag" :class="statusTag(row.status)">
                  {{ dict.label('crm_claim_status', row.status) }}
                </span>
              </td>
              <td>{{ appealLabel(row.appealStatus) }}</td>
              <td>{{ dateTimeText(row.appealDeadline) }}</td>
              <td>{{ dateTimeText(row.createdTime) }}</td>
              <td>
                <button class="pm-btn is-ghost is-sm" @click="router.push('/flow/center')">审批</button>
              </td>
            </tr>
            <tr v-if="loaded && list.length === 0">
              <td colspan="9" class="pm-empty">没有认领申请</td>
            </tr>
            <tr v-else-if="loading">
              <td colspan="9" class="pm-empty">加载中…</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>
  </div>
</template>
