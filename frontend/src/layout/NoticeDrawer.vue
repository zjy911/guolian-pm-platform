<script setup lang="ts">
import { ref, watch } from 'vue'
import { noticeApi } from '@/api'
import type { NoticeVO } from '@/api/types'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const emit = defineEmits<{ close: [] }>()
const appStore = useAppStore()
const userStore = useUserStore()

const tab = ref<'notice' | 'announcement' | 'todo'>('notice')
const notices = ref<NoticeVO[]>([])
const todos = ref<NoticeVO[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    if (tab.value === 'todo') {
      todos.value = await noticeApi.todos()
    } else {
      notices.value = await noticeApi.mine(tab.value === 'notice' ? 1 : 2)
    }
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

async function markAll() {
  try {
    await noticeApi.readAll()
    await userStore.loadUserInfo()
    await load()
    appStore.toast('已全部标记为已读')
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function readOne(item: NoticeVO) {
  try {
    await noticeApi.read(item.id)
    item.todoStatus = 1
    await userStore.loadUserInfo()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

watch(tab, load, { immediate: true })
</script>

<template>
  <div class="pm-drawer-mask" @click.self="emit('close')">
    <div class="pm-drawer" style="width: 380px">
      <div class="pm-drawer-header">
        <span>消息中心</span>
        <button class="pm-btn is-ghost is-sm" @click="emit('close')">
          <i class="ti ti-x" />
        </button>
      </div>
      <div class="pm-drawer-body" style="padding: 12px">
        <div class="pm-tabs">
          <div class="pm-tab" :class="{ 'is-active': tab === 'notice' }" @click="tab = 'notice'">通知</div>
          <div class="pm-tab" :class="{ 'is-active': tab === 'announcement' }" @click="tab = 'announcement'">公告</div>
          <div class="pm-tab" :class="{ 'is-active': tab === 'todo' }" @click="tab = 'todo'">待办</div>
          <div style="flex: 1" />
          <button class="pm-btn is-ghost is-sm" @click="markAll">全部已读</button>
        </div>

        <div v-if="loading" class="pm-empty">加载中…</div>
        <template v-else>
          <div v-for="item in tab === 'todo' ? todos : notices" :key="item.id" class="pm-card" style="padding: 12px; margin-bottom: 10px">
            <div style="display: flex; justify-content: space-between; gap: 8px">
              <strong style="font-size: 13px">{{ item.title }}</strong>
              <span v-if="item.pinned === 1" class="pm-tag is-warning">置顶</span>
            </div>
            <div style="font-size: 12px; color: var(--pm-neutral-500); margin-top: 6px">{{ item.publishTime }}</div>
            <div style="display: flex; gap: 8px; margin-top: 8px">
              <button v-if="item.todoStatus !== 1" class="pm-btn is-sm" @click="readOne(item)">标记已读</button>
              <button v-else class="pm-btn is-sm" disabled>已读</button>
            </div>
          </div>
          <div v-if="(tab === 'todo' ? todos : notices).length === 0" class="pm-empty">暂无消息</div>
        </template>
      </div>
    </div>
  </div>
</template>
