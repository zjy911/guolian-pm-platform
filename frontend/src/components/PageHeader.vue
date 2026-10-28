<script setup lang="ts">
defineProps<{ title: string; desc?: string }>()
</script>

<template>
  <div class="pm-page-header">
    <div style="display: flex; align-items: center; gap: 10px">
      <h1>{{ title }}</h1>
      <span v-if="desc" class="pm-tag is-muted">{{ desc }}</span>
    </div>
    <div class="pm-actions">
      <slot name="actions" />
    </div>
  </div>
</template>
