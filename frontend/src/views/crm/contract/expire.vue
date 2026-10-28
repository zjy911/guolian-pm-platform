<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { contractApi } from '@/api/crm'
import type { CrmContract } from '@/api/crm'
import { dateText, dateTimeText, money, useCrmDict } from '../shared/useCrmDict'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const dict = useCrmDict(['crm_contract_status'])

const days = ref(30)
const rows = ref<CrmContract[]>([])
const loading = ref(false)
const canRenew = computed(() => userStore.hasPerm('crm:contract:renew'))
const readOnly = computed(() => userStore.profile?.readOnly ?? false)

async function load() {
  loading.value = true
  try {
    rows.value = await contractApi.expiring(days.value)
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

function daysLeft(endDate?: string) {
  if (!endDate) return '—'
  const end = new Date(`${endDate}T00:00:00`)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const diff = Math.round((end.getTime() - today.getTime()) / 86400000)
  if (diff < 0) return `已过期 ${Math.abs(diff)} 天`
  return `剩 ${diff} 天`
}

function urgencyClass(endDate?: string) {
  if (!endDate) return 'is-muted'
  const diff = Math.round((new Date(`${endDate}T00:00:00`).getTime() - new Date().setHours(0, 0, 0, 0)) / 86400000)
  if (diff < 0) return 'is-danger'
  if (diff <= 7) return 'is-danger'
  if (diff <= 30) return 'is-warning'
  return 'is-success'
}

async function renew(row: CrmContract) {
  const endDate = window.prompt('续签合同到期日（yyyy-MM-dd）')
  if (!endDate) return
  try {
    await contractApi.renew(row.id, {
      name: `${row.name}（续签）`,
      accountId: row.accountId,
      companyId: row.companyId,
      businessGroupId: row.businessGroupId,
      startDate: row.endDate,
      endDate,
      winAmount: row.winAmount
    })
    appStore.toast('续签合同已创建')
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
    <PageHeader title="合同到期预警" :desc="`未来 ${days} 天内到期的生效合同`">
      <template #actions>
        <button class="pm-btn" @click="$router.push('/crm/contract')">
          <i class="ti ti-list" /> 合同列表
        </button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>预警窗口</label>
          <select v-model.number="days" class="pm-select" @change="load">
            <option :value="7">7 天内</option>
            <option :value="15">15 天内</option>
            <option :value="30">30 天内</option>
            <option :value="60">60 天内</option>
            <option :value="90">90 天内</option>
          </select>
        </div>
        <div class="pm-actions">
          <button class="pm-btn is-primary" @click="load"><i class="ti ti-refresh" /> 刷新</button>
        </div>
      </div>
    </div>

    <div class="pm-card">
      <div v-if="loading" class="pm-empty">加载中…</div>
      <div v-else-if="rows.length === 0" class="pm-empty">该窗口内没有即将到期的合同</div>
      <div v-else class="pm-table-wrap">
        <table class="pm-table">
          <thead>
            <tr>
              <th>合同编号</th>
              <th>名称</th>
              <th>状态</th>
              <th>到期日</th>
              <th>剩余</th>
              <th>金额</th>
              <th>负责人</th>
              <th>归档</th>
              <th style="width: 130px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.contractNo || '—' }}</td>
              <td style="font-weight: 600">{{ row.name }}</td>
              <td>{{ dict.label('crm_contract_status', row.status) }}</td>
              <td>{{ dateText(row.endDate) }}</td>
              <td>
                <span class="pm-tag" :class="urgencyClass(row.endDate)">{{ daysLeft(row.endDate) }}</span>
              </td>
              <td>{{ money(row.winAmount) }}</td>
              <td>{{ row.ownerUserName || '—' }}</td>
              <td>{{ row.isArchived === 1 ? '已归档' : '未归档' }}</td>
              <td>
                <button v-if="canRenew && !readOnly" class="pm-btn is-primary is-sm" @click="renew(row)">续签</button>
                <span v-else class="pm-tag is-muted">只读</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div style="font-size: 12px; color: var(--pm-neutral-500); margin-top: 8px">
        最近刷新：{{ dateTimeText(new Date().toISOString()) }}
      </div>
    </div>
  </div>
</template>
