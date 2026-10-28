<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { accountApi, activityApi, contactApi, leadApi, opportunityApi } from '@/api/crm'
import type { CrmActivity } from '@/api/crm'
import { dateTimeText, useCrmDict } from '../shared/useCrmDict'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const dict = useCrmDict(['crm_activity_type', 'crm_activity_level', 'crm_target_type'])

interface TargetOption {
  id: number
  label: string
}

const targetType = ref('ACCOUNT')
const targetId = ref<number | null>(null)
const targetOptions = ref<TargetOption[]>([])
const timeline = ref<CrmActivity[]>([])
const loading = ref(false)

const mine = ref<CrmActivity[]>([])
const range = ref({ startDate: '', endDate: '' })

async function loadTargets() {
  targetOptions.value = []
  targetId.value = null
  try {
    if (targetType.value === 'ACCOUNT') {
      targetOptions.value = (await accountApi.page({ pageSize: 50 })).list.map((item) => ({
        id: item.id,
        label: item.companyName
      }))
    } else if (targetType.value === 'LEAD') {
      targetOptions.value = (await leadApi.page({ pageSize: 50 })).list.map((item) => ({
        id: item.id,
        label: `${item.name} · ${item.companyName ?? ''}`
      }))
    } else if (targetType.value === 'CONTACT') {
      targetOptions.value = (await contactApi.page({ pageSize: 50 })).list.map((item) => ({
        id: item.id,
        label: item.name
      }))
    } else {
      targetOptions.value = (await opportunityApi.page({ pageSize: 50 })).list.map((item) => ({
        id: item.id,
        label: item.name
      }))
    }
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function loadTimeline() {
  if (!targetId.value) {
    appStore.toast('请选择要查看的对象', 'error')
    return
  }
  loading.value = true
  try {
    timeline.value = await activityApi.timeline(targetType.value, targetId.value)
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

async function loadMine() {
  try {
    const params: { startDate?: string; endDate?: string } = {}
    if (range.value.startDate) params.startDate = range.value.startDate
    if (range.value.endDate) params.endDate = range.value.endDate
    mine.value = await activityApi.mine(params)
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

onMounted(async () => {
  await dict.loadDicts()
  await loadTargets()
  await loadMine()
})
</script>

<template>
  <div>
    <PageHeader title="活动时间线" desc="按对象倒序展示跟进记录，作废记录保留痕迹" />

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>对象类型</label>
          <select v-model="targetType" class="pm-select" @change="loadTargets">
            <option v-for="item in dict.options('crm_target_type')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-field">
          <label>对象</label>
          <select v-model="targetId" class="pm-select">
            <option :value="null">请选择</option>
            <option v-for="item in targetOptions" :key="item.id" :value="item.id">{{ item.label }}</option>
          </select>
        </div>
        <div class="pm-actions">
          <button class="pm-btn is-primary" @click="loadTimeline"><i class="ti ti-search" /> 查看时间线</button>
        </div>
      </div>
    </div>

    <div class="pm-card">
      <h3 style="font-size: 14px; margin: 0 0 12px">对象时间线</h3>
      <div v-if="loading" class="pm-empty">加载中…</div>
      <div v-else-if="timeline.length === 0" class="pm-empty">选择对象后查看该对象的全部跟进记录</div>
      <div v-else class="pm-timeline">
        <div v-for="item in timeline" :key="item.id" class="pm-timeline-item">
          <div class="pm-timeline-dot" :class="{ 'is-done': item.status !== 'VOID' }" />
          <div>
            <div style="font-weight: 600">
              {{ item.subject }}
              <span v-if="item.status === 'VOID'" class="pm-tag is-danger" style="margin-left: 6px">已作废</span>
            </div>
            <div style="font-size: 12px; color: var(--pm-neutral-500)">
              {{ dict.label('crm_activity_type', item.type) }} ·
              {{ dict.label('crm_activity_level', item.level) }} · {{ dateTimeText(item.startTime) }} ·
              {{ item.ownerUserName || '—' }}
            </div>
            <div v-if="item.description" style="font-size: 13px; margin-top: 4px">{{ item.description }}</div>
            <div v-if="item.voidReason" style="font-size: 12px; color: var(--pm-danger); margin-top: 2px">
              作废原因：{{ item.voidReason }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="pm-card">
      <h3 style="font-size: 14px; margin: 0 0 12px">我的活动</h3>
      <div class="pm-filter">
        <div class="pm-field">
          <label>开始日期</label>
          <input v-model="range.startDate" type="date" class="pm-input" />
        </div>
        <div class="pm-field">
          <label>结束日期</label>
          <input v-model="range.endDate" type="date" class="pm-input" />
        </div>
        <div class="pm-actions">
          <button class="pm-btn is-primary" @click="loadMine"><i class="ti ti-search" /> 查询</button>
        </div>
      </div>
      <div class="pm-table-wrap" style="margin-top: 12px">
        <table class="pm-table">
          <thead>
            <tr><th>主题</th><th>类型</th><th>挂接对象</th><th>开始时间</th><th>结束时间</th><th>状态</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in mine" :key="`m-${row.id}`">
              <td>{{ row.subject }}</td>
              <td>{{ dict.label('crm_activity_type', row.type) }}</td>
              <td>{{ dict.label('crm_target_type', row.targetType) }} #{{ row.targetId }}</td>
              <td>{{ dateTimeText(row.startTime) }}</td>
              <td>{{ dateTimeText(row.endTime) }}</td>
              <td>
                <span class="pm-tag" :class="row.status === 'VOID' ? 'is-danger' : 'is-success'">
                  {{ row.status === 'VOID' ? '已作废' : '有效' }}
                </span>
              </td>
            </tr>
            <tr v-if="mine.length === 0">
              <td colspan="6" class="pm-empty">所选区间内没有我的活动</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
