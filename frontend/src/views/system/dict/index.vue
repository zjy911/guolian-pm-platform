<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { dictApi } from '@/api'
import type { DictItem, DictType } from '@/api/types'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const types = ref<DictType[]>([])
const activeType = ref('')
const items = ref<DictItem[]>([])
const typeOpen = ref(false)
const itemOpen = ref(false)
const saving = ref(false)
const typeForm = reactive<Record<string, unknown>>({})
const itemForm = reactive<Record<string, unknown>>({})

const canAdd = userStore.hasPerm('system:dict:add')
const canEdit = userStore.hasPerm('system:dict:edit')

async function load() {
  try {
    const data = await dictApi.types({ pageNum: 1, pageSize: 100 })
    types.value = data.list
    if (!activeType.value && types.value.length) {
      activeType.value = types.value[0].dictType
    }
    if (activeType.value) {
      items.value = await dictApi.items(activeType.value)
    }
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function selectType(dictType: string) {
  activeType.value = dictType
  items.value = await dictApi.items(dictType)
}

function openType(row?: DictType) {
  Object.keys(typeForm).forEach((key) => delete typeForm[key])
  Object.assign(typeForm, row ?? { id: null, dictType: '', dictName: '', status: 1, remark: '' })
  typeOpen.value = true
}

function openItem(row?: DictItem) {
  Object.keys(itemForm).forEach((key) => delete itemForm[key])
  Object.assign(
    itemForm,
    row ?? { id: null, dictType: activeType.value, dictLabel: '', dictValue: '', sortNo: 0, status: 1 }
  )
  itemOpen.value = true
}

async function saveType() {
  saving.value = true
  try {
    if (typeForm.id) {
      await dictApi.updateType(typeForm.id as number, { ...typeForm })
    } else {
      await dictApi.createType({ ...typeForm })
    }
    appStore.toast('字典类型已保存')
    typeOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

async function saveItem() {
  saving.value = true
  try {
    if (itemForm.id) {
      await dictApi.updateItem(itemForm.id as number, { ...itemForm })
    } else {
      await dictApi.createItem({ ...itemForm })
    }
    appStore.toast('字典项已保存')
    itemOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="字典管理" desc="所有下拉统一取字典">
      <template #actions>
        <button v-if="canAdd" class="pm-btn is-primary" @click="openType()"><i class="ti ti-plus" /> 新增字典类型</button>
      </template>
    </PageHeader>

    <div style="display: grid; grid-template-columns: 300px 1fr; gap: 16px">
      <div class="pm-card" style="margin: 0">
        <div
          v-for="type in types"
          :key="type.id"
          class="pm-dropdown-item"
          :class="{ 'is-active': activeType === type.dictType }"
          @click="selectType(type.dictType)"
        >
          <div>
            <div style="font-weight: 600">{{ type.dictName }}</div>
            <div style="font-size: 11px; color: var(--pm-neutral-500)">{{ type.dictType }}</div>
          </div>
          <button v-if="canEdit" class="pm-btn is-ghost is-sm" @click.stop="openType(type)">编辑</button>
        </div>
      </div>

      <div class="pm-card" style="margin: 0">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px">
          <h3 style="margin: 0; font-size: 15px">字典项 · {{ activeType }}</h3>
          <button v-if="canAdd" class="pm-btn is-sm" @click="openItem()">新增字典项</button>
        </div>
        <table class="pm-table">
          <thead><tr><th>标签</th><th>值</th><th>排序</th><th>状态</th><th style="width: 120px">操作</th></tr></thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ item.dictLabel }}</td>
              <td>{{ item.dictValue }}</td>
              <td>{{ item.sortNo }}</td>
              <td><span class="pm-tag" :class="item.status === 1 ? 'is-success' : 'is-muted'">{{ item.status === 1 ? '启用' : '停用' }}</span></td>
              <td><button v-if="canEdit" class="pm-btn is-ghost is-sm" @click="openItem(item)">编辑</button></td>
            </tr>
            <tr v-if="items.length === 0"><td colspan="5" class="pm-empty">该字典暂无字典项</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <FormDrawer :open="typeOpen" :title="typeForm.id ? '编辑字典类型' : '新增字典类型'" :loading="saving" @close="typeOpen = false" @submit="saveType">
      <div class="pm-form-grid">
        <div class="pm-form-item"><label><span class="pm-required">*</span>类型编码</label><input v-model="typeForm.dictType" class="pm-input" /></div>
        <div class="pm-form-item"><label><span class="pm-required">*</span>类型名称</label><input v-model="typeForm.dictName" class="pm-input" /></div>
        <div class="pm-form-item is-full"><label>备注</label><input v-model="typeForm.remark" class="pm-input" /></div>
      </div>
    </FormDrawer>

    <FormDrawer :open="itemOpen" :title="itemForm.id ? '编辑字典项' : '新增字典项'" :loading="saving" @close="itemOpen = false" @submit="saveItem">
      <div class="pm-form-grid">
        <div class="pm-form-item"><label>标签</label><input v-model="itemForm.dictLabel" class="pm-input" /></div>
        <div class="pm-form-item"><label>值</label><input v-model="itemForm.dictValue" class="pm-input" /></div>
        <div class="pm-form-item"><label>排序</label><input v-model.number="itemForm.sortNo" type="number" class="pm-input" /></div>
        <div class="pm-form-item">
          <label>状态</label>
          <select v-model="itemForm.status" class="pm-select"><option :value="1">启用</option><option :value="0">停用</option></select>
        </div>
      </div>
    </FormDrawer>
  </div>
</template>
