<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { configApi } from '@/api'
import type { SysConfigItem } from '@/api/types'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const list = ref<SysConfigItem[]>([])
const keyword = ref('')
const editingId = ref<number | null>(null)
const editingValue = ref('')

const canEdit = userStore.hasPerm('system:config:edit')

async function load() {
  try {
    list.value = await configApi.list(keyword.value || undefined)
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function startEdit(row: SysConfigItem) {
  editingId.value = row.id
  editingValue.value = row.configValue
}

async function save(row: SysConfigItem) {
  try {
    await configApi.update(row.id, editingValue.value)
    appStore.toast('参数已更新')
    editingId.value = null
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="参数配置" desc="阈值全部走参数，不在代码里硬编码" />
    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="keyword" class="pm-input" placeholder="参数键 / 名称" @keyup.enter="load" />
        </div>
        <div class="pm-actions"><button class="pm-btn is-primary" @click="load"><i class="ti ti-search" /> 查询</button></div>
      </div>
    </div>
    <div class="pm-card">
      <table class="pm-table">
        <thead><tr><th>参数键</th><th>参数名称</th><th>参数值</th><th>类型</th><th>内置</th><th style="width: 160px">操作</th></tr></thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td style="font-family: monospace">{{ row.configKey }}</td>
            <td>{{ row.configName }}</td>
            <td>
              <input v-if="editingId === row.id" v-model="editingValue" class="pm-input" style="height: 30px" />
              <span v-else style="font-weight: 600">{{ row.configValue }}</span>
            </td>
            <td>{{ row.configType }}</td>
            <td>{{ row.builtin === 1 ? '是' : '否' }}</td>
            <td>
              <div class="pm-actions">
                <template v-if="canEdit">
                  <button v-if="editingId !== row.id" class="pm-btn is-ghost is-sm" @click="startEdit(row)">修改</button>
                  <template v-else>
                    <button class="pm-btn is-sm is-primary" @click="save(row)">保存</button>
                    <button class="pm-btn is-sm" @click="editingId = null">取消</button>
                  </template>
                </template>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
