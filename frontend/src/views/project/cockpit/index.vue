<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import { cockpitApi, healthTag, type CockpitSummary } from '@/api/pm'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const router = useRouter()
const summary = ref<CockpitSummary | null>(null)
const loading = ref(false)

const maxPhase = computed(() => Math.max(1, ...(summary.value?.byPhase ?? [1])))

function lightLabel(light: string) {
  return { GREEN: '绿灯（≥85）', YELLOW: '黄灯（70-84）', RED: '红灯（<70）' }[light] ?? light
}

function lightClass(light: string) {
  return { GREEN: 'is-success', YELLOW: 'is-warning', RED: 'is-danger' }[light] ?? 'is-muted'
}

function amountText(value?: number) {
  return value == null ? '—' : `${(value / 10000).toLocaleString()} 万`
}

onMounted(async () => {
  loading.value = true
  try {
    summary.value = await cockpitApi.summary()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <PageHeader title="项目驾驶舱" desc="分析态（只读）：环节分布、健康度与预警，指标口径引指标字典" />

    <div v-if="summary" style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 12px">
      <div class="pm-card" style="text-align: center">
        <div style="font-size: 26px; font-weight: 700">{{ summary.total }}</div>
        <div style="font-size: 12.5px; color: var(--pm-neutral-500)">项目总数</div>
      </div>
      <div class="pm-card" style="text-align: center">
        <div style="font-size: 26px; font-weight: 700">{{ amountText(summary.estimatedAmountTotal) }}</div>
        <div style="font-size: 12.5px; color: var(--pm-neutral-500)">预估合同额合计</div>
      </div>
      <div class="pm-card" style="text-align: center">
        <div style="font-size: 26px; font-weight: 700">{{ summary.avgHealth }}</div>
        <div style="font-size: 12.5px; color: var(--pm-neutral-500)">平均健康度</div>
      </div>
      <div class="pm-card" style="text-align: center">
        <div style="font-size: 26px; font-weight: 700">{{ summary.openTaskCount }}</div>
        <div style="font-size: 12.5px; color: var(--pm-neutral-500)">未闭环任务</div>
      </div>
    </div>

    <div v-if="summary" style="display: flex; gap: 12px; align-items: flex-start">
      <div class="pm-card" style="flex: 1">
        <strong style="font-size: 13.5px">项目环节分布（6 环节固定口径）</strong>
        <div v-for="(count, index) in summary.byPhase" :key="index" style="margin-top: 10px">
          <div style="display: flex; justify-content: space-between; font-size: 12.5px">
            <span>{{ index + 1 }}. {{ summary.phaseNames[index] }}</span>
            <span>{{ count }}</span>
          </div>
          <div style="height: 8px; background: var(--pm-neutral-100); border-radius: 4px; margin-top: 4px">
            <div
              style="height: 100%; border-radius: 4px; background: var(--pm-primary-500)"
              :style="{ width: `${(count / maxPhase) * 100}%` }"
            />
          </div>
        </div>
        <hr style="border: none; border-top: 1px solid var(--pm-neutral-200); margin: 14px 0" />
        <strong style="font-size: 13.5px">健康度灯分布</strong>
        <div style="display: flex; gap: 12px; margin-top: 8px">
          <span v-for="(count, light) in summary.byLight" :key="light" class="pm-tag" :class="lightClass(light)">
            {{ lightLabel(light) }}：{{ count }}
          </span>
        </div>
      </div>

      <div class="pm-card" style="flex: 1">
        <strong style="font-size: 13.5px">健康度预警清单（红/黄灯）</strong>
        <table class="pm-table" style="margin-top: 8px">
          <thead><tr><th>项目</th><th>环节</th><th>健康度</th><th>评级</th></tr></thead>
          <tbody>
            <tr v-for="alert in summary.alerts" :key="alert.id">
              <td>
                <a style="cursor: pointer; color: var(--pm-primary-600)" @click="router.push(`/project/workspace/${alert.id}`)">
                  {{ alert.projectName }}
                </a>
              </td>
              <td>{{ (alert.phase ?? 1) }}. {{ summary.phaseNames[(alert.phase ?? 1) - 1] }}</td>
              <td><span class="pm-tag" :class="healthTag(alert.healthLight)">{{ alert.healthScore ?? '—' }}</span></td>
              <td>{{ alert.rating }}</td>
            </tr>
            <tr v-if="summary.alerts.length === 0"><td colspan="4" class="pm-empty">当前无红黄灯预警</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="loading" class="pm-card pm-empty">加载中…</div>
  </div>
</template>
