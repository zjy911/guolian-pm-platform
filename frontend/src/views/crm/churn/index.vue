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
const dict = useCrmDict(['crm_industry', 'crm_account_type', 'crm_pool_status'])

const { list, total, loading, loaded, pageNum, pageSize, query, load, reset } = useCrmList<CrmAccount>(
  accountApi.page,
  { keyword: '', poolStatus: 'CHURNED', industry: '' }
)

const canRevive = computed(() => userStore.hasPerm('crm:account:edit'))
const readOnly = computed(() => userStore.profile?.readOnly ?? false)

async function revive(row: CrmAccount) {
  const reason = window.prompt('恢复原因（选填）') ?? undefined
  try {
    await accountApi.revive(row.id, reason)
    appStore.toast('已恢复为私有客户')
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
    <PageHeader title="流失客户" desc="流失原因留痕，可恢复为私有客户" />

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="query.keyword" class="pm-input" placeholder="客户全称" @keyup.enter="load" />
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
              <th>行业</th>
              <th>分类</th>
              <th>流失原因</th>
              <th>原负责人</th>
              <th>年营收</th>
              <th>最近跟进</th>
              <th style="width: 130px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td style="font-weight: 600">{{ row.companyName }}</td>
              <td>{{ dict.label('crm_industry', row.industry) }}</td>
              <td>{{ dict.label('crm_account_type', row.accountType) }}</td>
              <td style="max-width: 240px">{{ row.churnReason || '—' }}</td>
              <td>{{ row.ownerUserName || '—' }}</td>
              <td>{{ money(row.annualRevenue) }}</td>
              <td>{{ dateTimeText(row.lastActivityTime) }}</td>
              <td>
                <button v-if="canRevive && !readOnly" class="pm-btn is-primary is-sm" @click="revive(row)">恢复</button>
                <span v-else class="pm-tag is-muted">只读</span>
              </td>
            </tr>
            <tr v-if="loaded && list.length === 0">
              <td colspan="8" class="pm-empty">暂无流失客户</td>
            </tr>
            <tr v-else-if="loading">
              <td colspan="8" class="pm-empty">加载中…</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>
  </div>
</template>
