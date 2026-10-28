<script setup lang="ts">
import { RouterView } from 'vue-router'
import Sidebar from './Sidebar.vue'
import Topbar from './Topbar.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<template>
  <div class="pm-layout">
    <Sidebar />
    <div class="pm-main">
      <div v-if="userStore.impersonate" style="height: 40px" />
      <Topbar />
      <main class="pm-content">
        <RouterView v-slot="{ Component }">
          <component :is="Component" :key="$route.fullPath" />
        </RouterView>
      </main>
    </div>
  </div>
</template>
