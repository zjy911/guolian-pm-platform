<script setup lang="ts">
const props = defineProps<{ total: number; pageNum: number; pageSize: number }>()
const emit = defineEmits<{ 'update:pageNum': [number]; 'update:pageSize': [number] }>()

function changeSize(event: Event) {
  emit('update:pageSize', Number((event.target as HTMLSelectElement).value))
  emit('update:pageNum', 1)
}

function prev() {
  if (props.pageNum > 1) {
    emit('update:pageNum', props.pageNum - 1)
  }
}

function next() {
  const max = Math.max(1, Math.ceil(props.total / props.pageSize))
  if (props.pageNum < max) {
    emit('update:pageNum', props.pageNum + 1)
  }
}
</script>

<template>
  <div class="pm-pagination">
    <span>共 {{ total }} 条</span>
    <select class="pm-select" style="width: 88px; height: 30px" :value="pageSize" @change="changeSize">
      <option :value="10">10 / 页</option>
      <option :value="20">20 / 页</option>
      <option :value="50">50 / 页</option>
      <option :value="100">100 / 页</option>
    </select>
    <button class="pm-btn is-sm" :disabled="pageNum <= 1" @click="prev">上一页</button>
    <span>{{ pageNum }} / {{ Math.max(1, Math.ceil(total / pageSize)) }}</span>
    <button class="pm-btn is-sm" :disabled="pageNum >= Math.ceil(total / pageSize)" @click="next">下一页</button>
  </div>
</template>
