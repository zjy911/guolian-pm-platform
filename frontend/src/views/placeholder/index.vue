<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const title = computed(() => (route.meta.title as string) || '模块')
const perms = computed(() => {
  const perm = route.meta.perm as string | undefined
  if (!perm) {
    return []
  }
  return userStore.perms.filter((item) => item.startsWith(perm.split(':').slice(0, 2).join(':')))
})
</script>

<template>
  <div class="pm-placeholder">
    <i class="ti ti-layout-dashboard pm-placeholder-icon" />
    <h2>模块待接入</h2>
    <p>该模块由独立方案建设，底座已预留菜单、路由与权限位。业务表单与接口不在底座交付范围内。</p>
    <div style="max-width: 640px; display: flex; flex-wrap: wrap; gap: 6px; justify-content: center">
      <span class="pm-tag">当前页面：{{ title }}</span>
      <span v-for="perm in perms" :key="perm" class="pm-tag is-muted">{{ perm }}</span>
    </div>
  </div>
</template>
