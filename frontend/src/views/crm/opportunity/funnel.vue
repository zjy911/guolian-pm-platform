<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { crmCommonApi, opportunityApi } from '@/api/crm'
import type { FunnelStage } from '@/api/crm'
import { wan } from '../shared/useCrmDict'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

const stages = ref<FunnelStage[]>([])
const leadStats = ref<{ newLeads: number; convertedLeads: number; leadConvertRate: number; stages: FunnelStage[] } | null>(
  null
)
const loading = ref(false)

const maxAmount = computed(() => Math.max(1, ...stages.value.map((item) => Number(item.amount) || 0)))

function barWidth(amount: number) {
  return `${Math.max(2, ((Number(amount) || 0) / maxAmount.value) * 100)}%`
}

async function load() {
  loading.value = true
  try {
    const [stageList, stats] = await Promise.all([opportunityApi.funnel(), crmCommonApi.funnel()])
    stages.value = stageList
    leadStats.value = stats
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="商机漏斗" desc="按阶段统计金额与转化率">
      <template #actions>
        <button class="pm-btn" @click="load"><i class="ti ti-refresh" /> 刷新</button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <h3 style="font-size: 14px; margin: 0 0 4px">线索转化</h3>
      <div v-if="!leadStats" class="pm-empty">{{ loading ? '加载中…' : '暂无数据' }}</div>
      <div v-else style="display: flex; gap: 32px; flex-wrap: wrap; padding: 8px 0">
        <div>
          <div style="font-size: 12px; color: var(--pm-neutral-500)">新增线索</div>
          <div style="font-size: 22px; font-weight: 600">{{ leadStats.newLeads }}</div>
        </div>
        <div>
          <div style="font-size: 12px; color: var(--pm-neutral-500)">已转化</div>
          <div style="font-size: 22px; font-weight: 600">{{ leadStats.convertedLeads }}</div>
        </div>
        <div>
          <div style="font-size: 12px; color: var(--pm-neutral-500)">线索转化率</div>
          <div style="font-size: 22px; font-weight: 600; color: var(--pm-primary-700)">
            {{ leadStats.leadConvertRate }}%
          </div>
        </div>
      </div>
    </div>

    <div class="pm-card">
      <h3 style="font-size: 14px; margin: 0 0 12px">商机阶段漏斗</h3>
      <div v-if="stages.length === 0" class="pm-empty">{{ loading ? '加载中…' : '暂无商机数据' }}</div>
      <div v-else style="display: flex; flex-direction: column; gap: 14px">
        <div v-for="(item, index) in stages" :key="item.stage">
          <div style="display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 4px">
            <span>
              <span class="pm-tag is-muted">{{ index + 1 }}</span>
              {{ item.stageLabel || item.stage }}
              <span style="color: var(--pm-neutral-500)">（{{ item.count }} 个）</span>
            </span>
            <span>
              {{ wan(item.amount) }}
              <span v-if="index > 0" style="color: var(--pm-neutral-500); margin-left: 8px">
                阶段转化 {{ item.convertRate }}%
              </span>
            </span>
          </div>
          <div style="height: 18px; background: var(--pm-neutral-100); border-radius: 4px; overflow: hidden">
            <div
              :style="{
                width: barWidth(item.amount),
                height: '100%',
                background: 'var(--pm-primary-600)',
                opacity: String(1 - index * 0.15)
              }"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="pm-card">
      <div class="pm-table-wrap">
        <table class="pm-table">
          <thead>
            <tr><th>阶段</th><th>数量</th><th>金额</th><th>相对上一阶段转化率</th></tr>
          </thead>
          <tbody>
            <tr v-for="item in stages" :key="`row-${item.stage}`">
              <td>{{ item.stageLabel || item.stage }}</td>
              <td>{{ item.count }}</td>
              <td>{{ wan(item.amount) }}</td>
              <td>{{ item.convertRate }}%</td>
            </tr>
            <tr v-if="stages.length === 0">
              <td colspan="4" class="pm-empty">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
