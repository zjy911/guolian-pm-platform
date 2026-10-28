<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { sensitiveApi } from '@/api'
import type { SensitiveField, SensitiveGroup } from '@/api/types'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const groups = ref<SensitiveGroup[]>([])
const fields = ref<SensitiveField[]>([])
const whitelist = ref<Record<string, { fieldName: string; fieldLabel: string; groupCode: string; maskRule: string }[]>>({})
const activeGroup = ref('')
const groupOpen = ref(false)
const fieldOpen = ref(false)
const saving = ref(false)
const groupForm = reactive<Record<string, unknown>>({})
const fieldForm = reactive<Record<string, unknown>>({})

const canAdd = userStore.hasPerm('system:sensitive:add')

async function load() {
  try {
    groups.value = await sensitiveApi.groups()
    fields.value = await sensitiveApi.fields()
    whitelist.value = await sensitiveApi.whitelist()
    if (!activeGroup.value && groups.value.length) {
      activeGroup.value = groups.value[0].groupCode
    }
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function openGroup(row?: SensitiveGroup) {
  Object.keys(groupForm).forEach((key) => delete groupForm[key])
  Object.assign(groupForm, row ?? { id: null, groupCode: '', groupName: '', defaultLevel: 'MASKED', riskLevel: 'MEDIUM', sortNo: 0, status: 1 })
  groupOpen.value = true
}

function openField(bizObject: string) {
  Object.keys(fieldForm).forEach((key) => delete fieldForm[key])
  const candidates = whitelist.value[bizObject] || []
  Object.assign(fieldForm, {
    id: null,
    groupCode: activeGroup.value,
    bizObject,
    fieldName: candidates[0]?.fieldName ?? '',
    fieldLabel: candidates[0]?.fieldLabel ?? '',
    maskRule: candidates[0]?.maskRule ?? 'FULL_MASK',
    maskParam: '',
    allowPlain: 0,
    status: 1
  })
  fieldOpen.value = true
}

function onFieldChange() {
  const candidates = whitelist.value[String(fieldForm.bizObject)] || []
  const found = candidates.find((item) => item.fieldName === fieldForm.fieldName)
  if (found) {
    fieldForm.fieldLabel = found.fieldLabel
    fieldForm.maskRule = found.maskRule
  }
}

async function saveGroup() {
  saving.value = true
  try {
    if (groupForm.id) {
      await sensitiveApi.updateGroup(groupForm.id as number, { ...groupForm })
    } else {
      await sensitiveApi.createGroup({ ...groupForm })
    }
    appStore.toast('字段组已保存，策略缓存将在 1 分钟内生效')
    groupOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

async function saveField() {
  saving.value = true
  try {
    if (fieldForm.id) {
      await sensitiveApi.updateField(fieldForm.id as number, { ...fieldForm })
    } else {
      await sensitiveApi.createField({ ...fieldForm })
    }
    appStore.toast('字段已保存')
    fieldOpen.value = false
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
    <PageHeader title="字段脱敏" desc="字段组 → 字段注册 → 角色档位">
      <template #actions>
        <button v-if="canAdd" class="pm-btn is-primary" @click="openGroup()"><i class="ti ti-plus" /> 新增字段组</button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 14px">
        <div
          v-for="group in groups"
          :key="group.groupCode"
          class="pm-tag"
          :class="activeGroup === group.groupCode ? 'is-solid' : ''"
          style="height: 30px; cursor: pointer; align-items: center"
          @click="activeGroup = group.groupCode"
        >
          {{ group.groupName }}（{{ group.defaultLevel }}）
        </div>
      </div>

      <div style="display: flex; gap: 10px; margin-bottom: 10px">
        <button v-if="canAdd" class="pm-btn is-sm" @click="openField('user')">在 user 下注册字段</button>
        <button v-if="canAdd" class="pm-btn is-sm" @click="openField('company')">在 company 下注册字段</button>
        <button class="pm-btn is-sm" @click="openGroup(groups.find((g) => g.groupCode === activeGroup))">编辑当前字段组</button>
      </div>

      <table class="pm-table">
        <thead><tr><th>业务对象</th><th>字段</th><th>算法</th><th>参数</th><th>允许明文</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="field in fields.filter((item) => item.groupCode === activeGroup)" :key="field.id">
            <td>{{ field.bizObject }}</td>
            <td>{{ field.fieldLabel }}<span class="pm-tag is-muted" style="margin-left: 6px">{{ field.fieldName }}</span></td>
            <td>{{ field.maskRule }}</td>
            <td>{{ field.maskParam || '—' }}</td>
            <td>{{ field.allowPlain === 1 ? '是' : '否' }}</td>
            <td><span class="pm-tag" :class="field.status === 1 ? 'is-success' : 'is-muted'">{{ field.status === 1 ? '启用' : '停用' }}</span></td>
          </tr>
          <tr v-if="fields.filter((item) => item.groupCode === activeGroup).length === 0">
            <td colspan="6" class="pm-empty">该字段组下暂无字段</td>
          </tr>
        </tbody>
      </table>
      <p style="font-size: 12px; color: var(--pm-neutral-500); margin-top: 10px">
        字段名只能从研发维护的白名单中选择，避免配置错字段名导致脱敏静默失效。高风险组 + 不允许明文时，任何角色都不能配为「明文可见」。
      </p>
    </div>

    <FormDrawer :open="groupOpen" :title="groupForm.id ? '编辑字段组' : '新增字段组'" :loading="saving" @close="groupOpen = false" @submit="saveGroup">
      <div class="pm-form-grid">
        <div class="pm-form-item"><label><span class="pm-required">*</span>组编码</label><input v-model="groupForm.groupCode" class="pm-input" /></div>
        <div class="pm-form-item"><label><span class="pm-required">*</span>组名称</label><input v-model="groupForm.groupName" class="pm-input" /></div>
        <div class="pm-form-item">
          <label>默认档位</label>
          <select v-model="groupForm.defaultLevel" class="pm-select"><option value="PLAIN">明文</option><option value="MASKED">脱敏</option><option value="HIDDEN">隐藏</option></select>
        </div>
        <div class="pm-form-item">
          <label>风险等级</label>
          <select v-model="groupForm.riskLevel" class="pm-select"><option value="LOW">低</option><option value="MEDIUM">中</option><option value="HIGH">高</option></select>
        </div>
      </div>
    </FormDrawer>

    <FormDrawer :open="fieldOpen" :title="'注册脱敏字段'" :loading="saving" @close="fieldOpen = false" @submit="saveField">
      <div class="pm-form-grid">
        <div class="pm-form-item">
          <label>业务对象</label>
          <select v-model="fieldForm.bizObject" class="pm-select" @change="onFieldChange">
            <option value="user">user</option>
            <option value="company">company</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>字段（白名单）</label>
          <select v-model="fieldForm.fieldName" class="pm-select" @change="onFieldChange">
            <option v-for="item in whitelist[String(fieldForm.bizObject)] || []" :key="item.fieldName" :value="item.fieldName">
              {{ item.fieldLabel }}（{{ item.fieldName }}）
            </option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>算法</label>
          <select v-model="fieldForm.maskRule" class="pm-select">
            <option value="KEEP_HEAD_TAIL">保留首尾</option>
            <option value="KEEP_TAIL">仅保留末位</option>
            <option value="EMAIL_MASK">邮箱脱敏</option>
            <option value="ADDRESS_MASK">保留省市</option>
            <option value="FULL_MASK">全掩码</option>
            <option value="AMOUNT_RANGE">区间显示</option>
          </select>
        </div>
        <div class="pm-form-item"><label>算法参数</label><input v-model="fieldForm.maskParam" class="pm-input" placeholder="如 3,4" /></div>
        <div class="pm-form-item">
          <label>允许明文</label>
          <select v-model="fieldForm.allowPlain" class="pm-select"><option :value="0">否</option><option :value="1">是</option></select>
        </div>
      </div>
    </FormDrawer>
  </div>
</template>
