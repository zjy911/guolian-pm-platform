<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { flowApi } from '@/api'
import type { FlowHistoryVO, FlowInstanceVO, FlowTaskVO } from '@/api/types'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()

const tab = ref<'todo' | 'done' | 'mine' | 'cc'>('todo')
const todo = ref<FlowTaskVO[]>([])
const done = ref<FlowTaskVO[]>([])
const mine = ref<FlowInstanceVO[]>([])
const cc = ref<FlowInstanceVO[]>([])

const actionOpen = ref(false)
const activeTask = ref<FlowTaskVO | null>(null)
const actionType = ref<'approve' | 'reject' | 'transfer'>('approve')
const comment = ref('')
const targetUserId = ref<number | null>(null)

const historyOpen = ref(false)
const history = ref<FlowHistoryVO[]>([])
const historyTitle = ref('')
const readOnly = userStore.profile?.readOnly ?? false

async function load() {
  try {
    if (tab.value === 'todo') {
      todo.value = await flowApi.todo()
    } else if (tab.value === 'done') {
      done.value = await flowApi.done()
    } else if (tab.value === 'mine') {
      mine.value = await flowApi.mine()
    } else {
      cc.value = await flowApi.cc()
    }
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function switchTab(value: typeof tab.value) {
  tab.value = value
  load()
}

function openAction(task: FlowTaskVO, type: 'approve' | 'reject' | 'transfer') {
  activeTask.value = task
  actionType.value = type
  comment.value = type === 'approve' ? '同意' : ''
  targetUserId.value = null
  actionOpen.value = true
}

async function submitAction() {
  if (!activeTask.value) {
    return
  }
  try {
    if (actionType.value === 'approve') {
      await flowApi.approve(activeTask.value.taskId, comment.value)
    } else if (actionType.value === 'reject') {
      await flowApi.reject(activeTask.value.taskId, comment.value)
    } else {
      if (!targetUserId.value) {
        appStore.toast('请输入转办目标用户 ID', 'error')
        return
      }
      await flowApi.transfer(activeTask.value.taskId, targetUserId.value, comment.value)
    }
    appStore.toast('操作成功')
    actionOpen.value = false
    await userStore.loadUserInfo()
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function showHistory(instanceId: string, title: string) {
  try {
    history.value = await flowApi.history(instanceId)
    historyTitle.value = title
    historyOpen.value = true
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function withdraw(row: FlowInstanceVO) {
  const reason = window.prompt('请输入撤回原因')
  if (reason === null) {
    return
  }
  try {
    await flowApi.withdraw(row.processInstanceId, reason)
    appStore.toast('已撤回')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="审批中心" desc="待办 / 我发起的 / 我已办 / 抄送我的" />

    <div class="pm-card">
      <div class="pm-tabs">
        <div class="pm-tab" :class="{ 'is-active': tab === 'todo' }" @click="switchTab('todo')">我的待办（{{ todo.length }}）</div>
        <div class="pm-tab" :class="{ 'is-active': tab === 'done' }" @click="switchTab('done')">我已办</div>
        <div class="pm-tab" :class="{ 'is-active': tab === 'mine' }" @click="switchTab('mine')">我发起的</div>
        <div class="pm-tab" :class="{ 'is-active': tab === 'cc' }" @click="switchTab('cc')">抄送我的</div>
      </div>

      <div v-if="tab === 'todo' || tab === 'done'" class="pm-table-wrap">
        <table class="pm-table">
          <thead><tr><th>节点</th><th>单据</th><th>发起人</th><th>时间</th><th style="width: 260px">操作</th></tr></thead>
          <tbody>
            <tr v-for="row in tab === 'todo' ? todo : done" :key="row.taskId">
              <td><span class="pm-tag">{{ row.taskName }}</span></td>
              <td>
                <div style="font-weight: 600">{{ row.bizTitle || row.bizNo || '—' }}</div>
                <div style="font-size: 12px; color: var(--pm-neutral-500)">{{ row.bizType }} · {{ row.bizNo }}</div>
              </td>
              <td>{{ row.initiatorName || '—' }}</td>
              <td>{{ row.startTime }}</td>
              <td>
                <div class="pm-actions">
                  <template v-if="tab === 'todo' && !readOnly">
                    <button class="pm-btn is-ghost is-sm" @click="openAction(row, 'approve')">同意</button>
                    <button class="pm-btn is-ghost is-sm is-danger" @click="openAction(row, 'reject')">驳回</button>
                    <button class="pm-btn is-ghost is-sm" @click="openAction(row, 'transfer')">转办</button>
                  </template>
                  <button class="pm-btn is-ghost is-sm" @click="showHistory(row.processInstanceId, row.bizTitle || row.bizNo)">
                    流水
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="(tab === 'todo' ? todo : done).length === 0"><td colspan="5" class="pm-empty">暂无数据</td></tr>
          </tbody>
        </table>
      </div>

      <div v-else class="pm-table-wrap">
        <table class="pm-table">
          <thead><tr><th>单据</th><th>状态</th><th>当前节点</th><th>发起时间</th><th style="width: 200px">操作</th></tr></thead>
          <tbody>
            <tr v-for="row in tab === 'mine' ? mine : cc" :key="row.processInstanceId">
              <td>
                <div style="font-weight: 600">{{ row.bizTitle }}</div>
                <div style="font-size: 12px; color: var(--pm-neutral-500)">{{ row.bizNo }}</div>
              </td>
              <td><span class="pm-tag" :class="row.status === 'APPROVED' ? 'is-success' : row.status === 'RUNNING' ? 'is-warning' : 'is-muted'">{{ row.status }}</span></td>
              <td>{{ row.currentNodeName }}</td>
              <td>{{ row.startTime }}</td>
              <td>
                <div class="pm-actions">
                  <button class="pm-btn is-ghost is-sm" @click="showHistory(row.processInstanceId, row.bizTitle)">流水</button>
                  <button v-if="tab === 'mine' && row.status === 'RUNNING' && !readOnly" class="pm-btn is-ghost is-sm is-danger" @click="withdraw(row)">撤回</button>
                </div>
              </td>
            </tr>
            <tr v-if="(tab === 'mine' ? mine : cc).length === 0"><td colspan="5" class="pm-empty">暂无数据</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <FormDrawer :open="actionOpen" :title="actionType === 'approve' ? '审批同意' : actionType === 'reject' ? '审批驳回' : '转办'" @close="actionOpen = false" @submit="submitAction">
      <div class="pm-form-item is-full" style="grid-template-columns: 110px 1fr">
        <label>{{ actionType === 'reject' ? '驳回意见' : '审批意见' }}</label>
        <textarea v-model="comment" class="pm-input" />
      </div>
      <div v-if="actionType === 'transfer'" class="pm-form-item is-full" style="grid-template-columns: 110px 1fr; margin-top: 10px">
        <label>转办给（用户 ID）</label>
        <input v-model.number="targetUserId" type="number" class="pm-input" />
      </div>
      <p style="font-size: 12px; color: var(--pm-neutral-500); margin-top: 10px">
        驳回未指定节点时流程终止并标记为已驳回；指定节点时可退回该节点继续处理。
      </p>
    </FormDrawer>

    <FormDrawer :open="historyOpen" :title="`审批流水 · ${historyTitle}`" @close="historyOpen = false" @submit="historyOpen = false">
      <div class="pm-timeline">
        <div v-for="item in history" :key="item.taskId" class="pm-timeline-item">
          <div class="pm-timeline-dot" :class="{ 'is-done': item.finished }" />
          <div>
            <div style="font-weight: 600; font-size: 13px">{{ item.taskName }} · {{ item.assigneeName || '未指派' }}</div>
            <div style="font-size: 12px; color: var(--pm-neutral-500)">
              {{ item.startTime }} → {{ item.endTime || '处理中' }} <span v-if="item.duration">（{{ item.duration }}）</span>
            </div>
            <div v-if="item.comment" style="font-size: 13px; margin-top: 4px">{{ item.comment }}</div>
          </div>
        </div>
        <div v-if="history.length === 0" class="pm-empty">暂无流水</div>
      </div>
    </FormDrawer>
  </div>
</template>
