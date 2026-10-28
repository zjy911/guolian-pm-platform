<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { onlineApi } from '@/api'
import type { OnlineSession } from '@/api/types'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const rows = ref<OnlineSession[]>([])
const canKick = userStore.hasPerm('system:online:list')

async function load() {
  try {
    rows.value = await onlineApi.list()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function kick(row: OnlineSession) {
  if (!window.confirm(`确认将 ${row.realName} 强制下线？`)) {
    return
  }
  try {
    await onlineApi.forceLogout(row.sessionId)
    appStore.toast('已强制下线')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="在线用户" desc="支持强制下线">
      <template #actions>
        <button class="pm-btn" @click="load"><i class="ti ti-refresh" /> 刷新</button>
      </template>
    </PageHeader>
    <div class="pm-card">
      <table class="pm-table">
        <thead><tr><th>账号</th><th>姓名</th><th>当前角色</th><th>组织</th><th>IP</th><th>登录时间</th><th>最后活跃</th><th>模拟</th><th style="width: 120px">操作</th></tr></thead>
        <tbody>
          <tr v-for="row in rows" :key="row.sessionId">
            <td>{{ row.username }}</td>
            <td>{{ row.realName }}</td>
            <td>{{ row.roleName }}</td>
            <td>{{ row.orgName }}</td>
            <td>{{ row.ip }}</td>
            <td>{{ row.loginTime }}</td>
            <td>{{ row.lastActiveTime }}</td>
            <td>{{ row.impersonate ? '是' : '否' }}</td>
            <td><button v-if="canKick" class="pm-btn is-ghost is-sm is-danger" @click="kick(row)">强制下线</button></td>
          </tr>
          <tr v-if="rows.length === 0"><td colspan="9" class="pm-empty">暂无在线会话</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
