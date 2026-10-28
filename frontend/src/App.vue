<script setup lang="ts">
import { RouterView } from 'vue-router'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<template>
  <RouterView />
  <div v-for="toast in appStore.toasts" :key="toast.id" class="pm-toast" :class="{ 'is-error': toast.type === 'error' }">
    {{ toast.message }}
  </div>
</template>
