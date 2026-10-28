<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import Pagination from '@/components/Pagination.vue'
import { logApi } from '@/api'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const rows = ref<Record<string, unknown>[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const keyword = ref('')

async function load() {
  try {
    const data = await logApi.login({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value || undefined })
    rows.value = data.list
    total.value = data.total
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="登录日志" desc="登录 / 登出 / 切换角色 / 模拟登录" />
    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>账号</label>
          <input v-model="keyword" class="pm-input" @keyup.enter="load" />
        </div>
        <div class="pm-actions"><button class="pm-btn is-primary" @click="load"><i class="ti ti-search" /> 查询</button></div>
      </div>
    </div>
    <div class="pm-card">
      <table class="pm-table">
        <thead><tr><th>时间</th><th>账号</th><th>类型</th><th>IP</th><th>浏览器</th><th>结果</th><th>失败原因</th></tr></thead>
        <tbody>
          <tr v-for="row in rows" :key="String(row.id)">
            <td>{{ row.createdTime }}</td>
            <td>{{ row.username }}</td>
            <td>{{ row.loginType }}</td>
            <td>{{ row.ip }}</td>
            <td style="max-width: 240px; overflow: hidden; text-overflow: ellipsis">{{ row.browser }}</td>
            <td><span class="pm-tag" :class="Number(row.success) === 1 ? 'is-success' : 'is-danger'">{{ Number(row.success) === 1 ? '成功' : '失败' }}</span></td>
            <td>{{ row.failReason || '—' }}</td>
          </tr>
          <tr v-if="rows.length === 0"><td colspan="7" class="pm-empty">暂无日志</td></tr>
        </tbody>
      </table>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>
  </div>
</template>
