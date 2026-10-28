<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { bidApi } from '@/api/crm'
import type { CrmBid } from '@/api/crm'
import { money, useCrmDict } from '../shared/useCrmDict'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const dict = useCrmDict(['crm_bid_status', 'crm_tender_form'])

function currentMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

const month = ref(currentMonth())
const rows = ref<CrmBid[]>([])
const openingSoon = ref<CrmBid[]>([])
const loading = ref(false)

/** 把开标日聚合成 日 → 投标列表 */
const byDay = computed(() => {
  const map: Record<string, CrmBid[]> = {}
  rows.value.forEach((row) => {
    const key = row.openDate ?? ''
    if (!key) return
    if (!map[key]) map[key] = []
    map[key].push(row)
  })
  return map
})

/** 日历格子：补齐前后空白，周一为起始 */
const cells = computed(() => {
  const [yearText, monthText] = month.value.split('-')
  const year = Number(yearText)
  const monthIndex = Number(monthText) - 1
  const first = new Date(year, monthIndex, 1)
  const daysInMonth = new Date(year, monthIndex + 1, 0).getDate()
  const leading = (first.getDay() + 6) % 7
  const result: { day: number | null; key: string }[] = []
  for (let i = 0; i < leading; i += 1) {
    result.push({ day: null, key: `empty-${i}` })
  }
  for (let day = 1; day <= daysInMonth; day += 1) {
    result.push({ day, key: `${month.value}-${String(day).padStart(2, '0')}` })
  }
  return result
})

async function load() {
  loading.value = true
  try {
    const [calendarRows, soonRows] = await Promise.all([bidApi.calendar(month.value), bidApi.openingSoon(3)])
    rows.value = calendarRows
    openingSoon.value = soonRows
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

function shiftMonth(delta: number) {
  const [yearText, monthText] = month.value.split('-')
  const date = new Date(Number(yearText), Number(monthText) - 1 + delta, 1)
  month.value = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
  load()
}

function statusTag(status?: string) {
  if (status === 'WON') return 'is-success'
  if (status === 'LOST' || status === 'ABANDONED') return 'is-danger'
  if (status === 'OPENED' || status === 'SUBMITTED') return 'is-warning'
  return 'is-muted'
}

onMounted(async () => {
  await dict.loadDicts()
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="投标日历" desc="按开标日排布，3 日内开标高亮提醒">
      <template #actions>
        <button class="pm-btn" @click="$router.push('/crm/bid')"><i class="ti ti-list" /> 投标列表</button>
      </template>
    </PageHeader>

    <div v-if="openingSoon.length > 0" class="pm-card" style="background: var(--pm-warning-bg)">
      <div style="font-weight: 600; font-size: 13px; margin-bottom: 6px">
        <i class="ti ti-alert-triangle" /> 3 日内开标 {{ openingSoon.length }} 项
      </div>
      <div v-for="row in openingSoon" :key="`soon-${row.id}`" style="font-size: 13px">
        {{ row.openDate }} · {{ row.tenderName }}
        <span class="pm-tag is-muted" style="margin-left: 6px">
          {{ dict.label('crm_bid_status', row.bidStatus) }}
        </span>
      </div>
    </div>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>月份</label>
          <input v-model="month" type="month" class="pm-input" @change="load" />
        </div>
        <div class="pm-actions">
          <button class="pm-btn is-sm" @click="shiftMonth(-1)">上一月</button>
          <button class="pm-btn is-sm" @click="shiftMonth(1)">下一月</button>
          <button class="pm-btn is-primary" @click="load"><i class="ti ti-refresh" /> 刷新</button>
        </div>
      </div>
    </div>

    <div class="pm-card">
      <div v-if="loading" class="pm-empty">加载中…</div>
      <template v-else>
        <div style="display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; font-size: 12px; text-align: center">
          <div v-for="week in ['一', '二', '三', '四', '五', '六', '日']" :key="week" style="font-weight: 600; padding: 4px">
            周{{ week }}
          </div>
          <div
            v-for="cell in cells"
            :key="cell.key"
            :style="{
              minHeight: '86px',
              border: '1px solid var(--pm-neutral-200)',
              borderRadius: 'var(--pm-radius-control)',
              padding: '4px',
              textAlign: 'left',
              background: cell.day && byDay[cell.key] ? 'var(--pm-primary-50)' : 'transparent'
            }"
          >
            <div v-if="cell.day" style="font-weight: 600; margin-bottom: 2px">{{ cell.day }}</div>
            <div v-if="cell.day && byDay[cell.key]">
              <div
                v-for="bid in byDay[cell.key]"
                :key="bid.id"
                :title="`${bid.tenderName} · 限价 ${money(bid.priceCap)}`"
                style="font-size: 11px; line-height: 1.35; overflow: hidden; text-overflow: ellipsis; white-space: nowrap"
              >
                <span class="pm-tag" :class="statusTag(bid.bidStatus)" style="font-size: 10px; padding: 0 4px">
                  {{ dict.label('crm_bid_status', bid.bidStatus) }}
                </span>
                {{ bid.tenderName }}
              </div>
            </div>
          </div>
        </div>
        <div v-if="rows.length === 0" class="pm-empty" style="margin-top: 12px">本月没有开标安排</div>
      </template>
    </div>

    <div class="pm-card">
      <h3 style="font-size: 14px; margin: 0 0 12px">本月开标清单（{{ rows.length }}）</h3>
      <div class="pm-table-wrap">
        <table class="pm-table">
          <thead>
            <tr><th>开标日</th><th>项目名称</th><th>形式</th><th>状态</th><th>限价</th><th>负责人</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="`list-${row.id}`">
              <td>{{ row.openDate }}</td>
              <td>{{ row.tenderName }}</td>
              <td>{{ dict.label('crm_tender_form', row.tenderForm) }}</td>
              <td>
                <span class="pm-tag" :class="statusTag(row.bidStatus)">{{ dict.label('crm_bid_status', row.bidStatus) }}</span>
              </td>
              <td>{{ money(row.priceCap) }}</td>
              <td>{{ row.ownerUserName || '—' }}</td>
            </tr>
            <tr v-if="rows.length === 0"><td colspan="6" class="pm-empty">暂无数据</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
