<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { flowApi } from '@/api'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const defs = ref<Record<string, unknown>[]>([])
const formOpen = ref(false)
const saving = ref(false)
const form = reactive<Record<string, any>>({})
const canEdit = userStore.hasPerm('system:flow:edit')
const canPublish = userStore.hasPerm('system:flow:publish')

const template = JSON.stringify(
  {
    nodes: [
      { key: 'start', name: '发起人', type: 'START' },
      { key: 'leader', name: '直属上级', type: 'SINGLE', approverType: 'LEADER' },
      { key: 'deptLeader', name: '部门负责人', type: 'SINGLE', approverType: 'ORG_LEADER' },
      { key: 'hr', name: '人力资源', type: 'CANDIDATE', approverType: 'ROLE', roleCode: 'ROLE_GROUP_ADMIN' },
      { key: 'end', name: '结束', type: 'END' }
    ]
  },
  null,
  2
)

async function load() {
  try {
    defs.value = await flowApi.defs()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function open(row?: Record<string, unknown>) {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(
    form,
    row ?? {
      id: null,
      flowCode: '',
      flowName: '',
      bizType: '',
      processDefinitionKey: '',
      status: 0,
      nodeConfig: template
    }
  )
  formOpen.value = true
}

async function submit() {
  saving.value = true
  try {
    try {
      JSON.parse(String(form.nodeConfig))
    } catch {
      throw new Error('节点配置不是合法 JSON')
    }
    if (form.id) {
      await flowApi.updateDef(form.id as number, { ...form })
    } else {
      await flowApi.createDef({ ...form })
    }
    appStore.toast('模板已保存，发布后生成 BPMN 并部署')
    formOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

async function publish(row: Record<string, unknown>) {
  try {
    await flowApi.publishDef(Number(row.id))
    appStore.toast('已发布并部署到 Flowable')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="审批流程" desc="表单化配置节点，保存时生成 BPMN 并部署">
      <template #actions>
        <button v-if="canEdit" class="pm-btn is-primary" @click="open()"><i class="ti ti-plus" /> 新增模板</button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <table class="pm-table">
        <thead><tr><th>流程名称</th><th>编码</th><th>业务类型</th><th>流程定义 Key</th><th>版本</th><th>状态</th><th style="width: 200px">操作</th></tr></thead>
        <tbody>
          <tr v-for="row in defs" :key="String(row.id)">
            <td style="font-weight: 600">{{ row.flowName }}</td>
            <td>{{ row.flowCode }}</td>
            <td>{{ row.bizType }}</td>
            <td>{{ row.processDefinitionKey || '—' }}</td>
            <td>{{ row.deployedVersion ?? '—' }}</td>
            <td>
              <span class="pm-tag" :class="Number(row.status) === 1 ? 'is-success' : 'is-muted'">
                {{ Number(row.status) === 1 ? '已发布' : '草稿' }}
              </span>
            </td>
            <td>
              <div class="pm-actions">
                <button v-if="canEdit" class="pm-btn is-ghost is-sm" @click="open(row)">编辑</button>
                <button v-if="canPublish" class="pm-btn is-ghost is-sm" @click="publish(row)">发布</button>
              </div>
            </td>
          </tr>
          <tr v-if="defs.length === 0"><td colspan="7" class="pm-empty">暂无审批模板</td></tr>
        </tbody>
      </table>
      <p style="font-size: 12px; color: var(--pm-neutral-500); margin-top: 10px">
        节点类型：SINGLE 单人 / CANDIDATE 候选角色 / ALL_SIGN 会签 / OR_SIGN 或签 / CONDITION 条件分支。审批人类型：LEADER 直属上级 / ORG_LEADER 组织负责人 / ROLE 指定角色 / USER 指定人。
      </p>
    </div>

    <FormDrawer :open="formOpen" :title="form.id ? '编辑审批模板' : '新增审批模板'" :wide="true" :loading="saving" @close="formOpen = false" @submit="submit">
      <div class="pm-form-grid">
        <div class="pm-form-item"><label><span class="pm-required">*</span>流程名称</label><input v-model="form.flowName" class="pm-input" /></div>
        <div class="pm-form-item"><label><span class="pm-required">*</span>流程编码</label><input v-model="form.flowCode" class="pm-input" /></div>
        <div class="pm-form-item"><label><span class="pm-required">*</span>业务类型</label><input v-model="form.bizType" class="pm-input" placeholder="ACCOUNT_TRANSFER" /></div>
        <div class="pm-form-item"><label>流程定义 Key</label><input v-model="form.processDefinitionKey" class="pm-input" placeholder="留空则用流程编码" /></div>
        <div class="pm-form-item is-full">
          <label>节点配置</label>
          <textarea v-model="form.nodeConfig" class="pm-input" style="min-height: 260px; font-family: monospace; font-size: 12px" />
        </div>
      </div>
    </FormDrawer>
  </div>
</template>
