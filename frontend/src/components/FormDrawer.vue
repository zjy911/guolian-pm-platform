<script setup lang="ts">
defineProps<{ open: boolean; title: string; wide?: boolean; loading?: boolean }>()
const emit = defineEmits<{ close: []; submit: [] }>()
</script>

<template>
  <div v-if="open" class="pm-drawer-mask" @click.self="emit('close')">
    <div class="pm-drawer" :class="{ 'is-wide': wide }">
      <div class="pm-drawer-header">
        <span>{{ title }}</span>
        <button class="pm-btn is-ghost is-sm" @click="emit('close')"><i class="ti ti-x" /></button>
      </div>
      <div class="pm-drawer-body">
        <slot />
      </div>
      <div class="pm-drawer-footer">
        <button class="pm-btn" @click="emit('close')">取消</button>
        <button class="pm-btn is-primary" :disabled="loading" @click="emit('submit')">
          {{ loading ? '提交中…' : '确定' }}
        </button>
      </div>
    </div>
  </div>
</template>
