<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  healthTag, PM_PHASES, PM_PROJECT_ROLE, PM_STATUS, PM_TASK_STATUS,
  projectApi, taskStatusTag, type PmFormInstance, type PmProject, type PmProjectTask
} from '@/api/pm'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const route = useRoute()
const router = useRouter()

const projectId = Number(route.params.id)
const project = ref<PmProject | null>(null)
const tasks = ref<PmProjectTask[]>([])
const logs = ref<{ action: string; detail: string; operatorName: string; createdTime: string }[]>([])
const members = ref<{ id: number; userName: string; projectRole: string; inputRatio: number; status: string }[]>([])
const loading = ref(true)

const activePhase = ref(1)
const currentTask = ref<PmProjectTask | null>(null)
const form = ref<PmFormInstance | null>(null)
const formDataText = ref('{}')
const formSaving = ref(false)
const onlyOpen = ref(false)

const phaseSections = computed(() => {
  const result: Record<number, { category: string; tasks: PmProjectTask[] }[]> = {}
  for (const task of tasks.value) {
    if (onlyOpen.value && task.status === 'DONE') continue
    const sections = (result[task.phase] ??= [])
    let section = sections.find((s) => s.category === task.category)
    if (!section) {
      section = { category: task.category, tasks: [] }
      sections.push(section)
    }
    section.tasks.push(task)
  }
  return result
})

function phaseProgress(phase: number) {
  const phaseTasks = tasks.value.filter((t) => t.phase === phase)
  const done = phaseTasks.filter((t) => t.status === 'DONE').length
  return { done, total: phaseTasks.length }
}

function phaseLocked(phase: number) {
  // 当前环节之后且前置未完成则置灰锁定（PRD 5.3-1）
  return phase > (project.value?.phase ?? 1)
}

async function load() {
  loading.value = true
  try {
    const [ws, memberList] = await Promise.all([
      projectApi.workspace(projectId),
      projectApi.members(projectId)
    ])
    project.value = ws.project
    tasks.value = ws.tasks
    logs.value = ws.logs
    members.value = memberList
    activePhase.value = project.value?.phase ?? 1
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

async function openTask(task: PmProjectTask) {
  if (phaseLocked(task.phase)) {
    appStore.toast('前置环节未完成，该任务暂不可操作', 'error')
    return
  }
  if (task.taskType === 'DATA') {
    currentTask.value = task
    form.value = null
    formDataText.value = '{}'
    return
  }
  try {
    const instance = await projectApi.taskForm(task.id)
    currentTask.value = task
    form.value = instance
    formDataText.value = JSON.stringify(JSON.parse(instance.formData || '{}'), null, 2)
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function saveForm() {
  if (!form.value) return
  try {
    JSON.parse(formDataText.value)
  } catch {
    appStore.toast('表单数据 JSON 格式不正确', 'error')
    return
  }
  formSaving.value = true
  try {
    await projectApi.saveForm(form.value.id, formDataText.value)
    appStore.toast('已暂存')
    form.value = { ...form.value, formData: formDataText.value, status: 'DRAFT' }
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    formSaving.value = false
  }
}

async function submitForm() {
  if (!form.value) return
  try {
    JSON.parse(formDataText.value)
  } catch {
    appStore.toast('表单数据 JSON 格式不正确', 'error')
    return
  }
  if (!confirm('提交后进入审批流，确认提交？')) return
  formSaving.value = true
  try {
    await projectApi.saveForm(form.value.id, formDataText.value)
    await projectApi.submitForm(form.value.id)
    appStore.toast('已提交审批')
    if (currentTask.value) {
      currentTask.value = { ...currentTask.value, status: 'APPROVING' }
    }
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    formSaving.value = false
  }
}

async function markDataDone() {
  if (!currentTask.value) return
  try {
    JSON.parse(formDataText.value)
  } catch {
    appStore.toast('数据 JSON 格式不正确', 'error')
    return
  }
  try {
    const instance = form.value
    if (instance) {
      await projectApi.saveForm(instance.id, formDataText.value)
    }
    appStore.toast('数据任务已记录')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function advancePhase() {
  if (!confirm('确认推进到下一环节？阻塞任务未完成将被阻断。')) return
  try {
    await projectApi.advance(projectId)
    appStore.toast('环节已推进')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function amountText(value?: number | null) {
  return value == null ? '—' : `${(value / 10000).toLocaleString()} 万`
}

function taskIcon(type?: string) {
  return type === 'DATA' ? 'ti-chart-line' : type === 'COLLAB' ? 'ti-paperclip' : 'ti-forms'
}

onMounted(load)
</script>

<template>
  <div>
    <!-- 顶部看板条（固定不随滚动消失） -->
    <div v-if="project" class="pm-card" style="position: sticky; top: 0; z-index: 5">
      <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap">
        <button class="pm-btn is-sm" @click="router.push('/project/list')"><i class="ti ti-arrow-left" /></button>
        <div>
          <div style="font-size: 16px; font-weight: 700">{{ project.projectName }}</div>
          <div style="font-size: 12px; color: var(--pm-neutral-500)">
            {{ project.projectCode }} · {{ project.productName }} · {{ project.customerName }} ·
            模板 {{ project.templateVersion }}
          </div>
        </div>
        <span class="pm-tag is-solid">评级 {{ project.rating ?? '—' }}</span>
        <span class="pm-tag" :class="healthTag(project.healthLight)">健康度 {{ project.healthScore ?? '—' }}</span>
        <span class="pm-tag is-muted">毛利率 {{ project.grossMargin != null ? (project.grossMargin * 100).toFixed(1) + '%' : '—' }}</span>
        <span style="flex: 1" />
        <button v-if="(project.phase ?? 1) < 6" class="pm-btn is-primary" @click="advancePhase">
          <i class="ti ti-player-play" /> 推进环节
        </button>
      </div>
      <div style="display: flex; gap: 6px; margin-top: 10px; flex-wrap: wrap">
        <div
          v-for="(name, index) in PM_PHASES"
          :key="index"
          class="pm-tag"
          :class="{
            'is-success': index + 1 < project.phase,
            'is-solid': index + 1 === project.phase,
            'is-muted': index + 1 > project.phase
          }"
          style="min-width: 96px; text-align: center"
        >
          {{ index + 1 < project.phase ? '✓ ' : '' }}{{ index + 1 }}. {{ name }}
        </div>
      </div>
    </div>

    <div v-if="loading" class="pm-card pm-empty">加载中…</div>

    <div v-else-if="project" style="display: flex; gap: 12px; align-items: flex-start">
      <!-- 左侧主工作区 -->
      <div class="pm-card" style="flex: 1; min-height: 420px">
        <div v-if="!currentTask" class="pm-empty">
          从右侧手风琴选择任务，此处将加载对应的表单 / 数据 / 协同界面
        </div>
        <div v-else>
          <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px">
            <i :class="`ti ${taskIcon(currentTask.taskType)}`" />
            <span style="font-size: 15px; font-weight: 700">{{ currentTask.taskName }}</span>
            <span class="pm-tag" :class="taskStatusTag(currentTask.status)">{{ PM_TASK_STATUS[currentTask.status] }}</span>
            <span v-if="currentTask.blocking" class="pm-tag is-danger">阻塞项</span>
            <span class="pm-tag is-muted">{{ currentTask.assigneeName ?? '未指派' }}</span>
            <span style="flex: 1" />
            <span style="font-size: 12px; color: var(--pm-neutral-500)">截止：{{ currentTask.dueDate ?? '—' }}</span>
          </div>

          <!-- 表单任务容器 -->
          <template v-if="currentTask.taskType === 'FORM'">
            <div v-if="form" class="pm-tag is-muted" style="margin-bottom: 8px">
              表单实例 #{{ form.id }} · 状态：{{ { DRAFT: '草稿', APPROVING: '审批中', EFFECTIVE: '已生效', REJECTED: '已驳回' }[form.status] ?? form.status }}
              <template v-if="form.approveComment"> · 审批意见：{{ form.approveComment }}</template>
            </div>
            <textarea v-model="formDataText" class="pm-input" rows="14" style="font-family: monospace; font-size: 12.5px"
              :disabled="form?.status === 'APPROVING' || form?.status === 'EFFECTIVE'" />
            <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 10px"
              v-if="form && form.status !== 'APPROVING' && form.status !== 'EFFECTIVE'">
              <button class="pm-btn" :disabled="formSaving" @click="saveForm">暂存</button>
              <button class="pm-btn is-primary" :disabled="formSaving" @click="submitForm">提交审批</button>
            </div>
          </template>

          <!-- 数据任务容器 -->
          <template v-else-if="currentTask.taskType === 'DATA'">
            <p style="font-size: 12.5px; color: var(--pm-neutral-500)">
              按期录入量化指标（JSON）；健康度评分卡提交后自动按五维加权回写项目灯态。
            </p>
            <textarea v-model="formDataText" class="pm-input" rows="12" style="font-family: monospace; font-size: 12.5px" />
            <div style="display: flex; justify-content: flex-end; margin-top: 10px">
              <button class="pm-btn is-primary" :disabled="formSaving" @click="markDataDone">保存记录</button>
            </div>
          </template>

          <!-- 协同任务容器 -->
          <template v-else>
            <p style="font-size: 12.5px; color: var(--pm-neutral-500)">
              上传交付物与多方签认（附件存储对接预留）；未签认不允许置为完成（PRD 5.4）。
            </p>
            <textarea v-model="formDataText" class="pm-input" rows="10" style="font-family: monospace; font-size: 12.5px"
              placeholder="记录移交清单 / 签认情况…" />
            <div style="display: flex; justify-content: flex-end; margin-top: 10px">
              <button class="pm-btn is-primary" :disabled="formSaving" @click="markDataDone">保存记录</button>
            </div>
          </template>
        </div>
      </div>

      <!-- 右侧手风琴 -->
      <div class="pm-card" style="width: 320px; flex-shrink: 0; max-height: 78vh; overflow: auto">
        <div style="display: flex; align-items: center; margin-bottom: 8px">
          <strong style="font-size: 13.5px">项目环节与任务</strong>
          <span style="flex: 1" />
          <label style="font-size: 12px; display: flex; gap: 4px; align-items: center">
            <input v-model="onlyOpen" type="checkbox" /> 只看未完成
          </label>
        </div>
        <div v-for="(name, index) in PM_PHASES" :key="index" style="margin-bottom: 6px">
          <div
            class="pm-tag"
            :class="{
              'is-success': index + 1 < project.phase,
              'is-solid': index + 1 === activePhase,
              'is-muted': index + 1 > project.phase
            }"
            style="width: 100%; cursor: pointer; height: auto; padding: 8px 10px"
            @click="activePhase = index + 1"
          >
            <div style="display: flex; justify-content: space-between; width: 100%">
              <span>{{ index + 1 < project.phase ? '✓ ' : phaseLocked(index + 1) ? '🔒 ' : '' }}{{ index + 1 }}. {{ name }}</span>
              <span style="font-size: 11px">{{ phaseProgress(index + 1).done }}/{{ phaseProgress(index + 1).total }}</span>
            </div>
          </div>
          <div v-if="activePhase === index + 1" style="padding: 6px 0 2px 10px">
            <div v-for="section in phaseSections[index + 1] ?? []" :key="section.category" style="margin-bottom: 8px">
              <div style="font-size: 11.5px; color: var(--pm-neutral-500); margin: 4px 0">{{ section.category }}</div>
              <div
                v-for="task in section.tasks"
                :key="task.id"
                style="display: flex; align-items: center; gap: 6px; padding: 4px 6px; border-radius: 6px; cursor: pointer; font-size: 12.5px"
                :style="currentTask?.id === task.id ? 'background: var(--pm-primary-50)' : ''"
                @click="openTask(task)"
              >
                <i :class="`ti ${taskIcon(task.taskType)}`" style="font-size: 13px" />
                <span style="flex: 1">{{ task.taskName }}</span>
                <i v-if="task.blocking && task.status !== 'DONE'" class="ti ti-lock" style="font-size: 11px; color: var(--pm-warning-500)" />
                <span class="pm-tag" :class="taskStatusTag(task.status)" style="font-size: 10.5px">
                  {{ PM_TASK_STATUS[task.status] }}
                </span>
              </div>
            </div>
            <div v-if="(phaseSections[index + 1] ?? []).length === 0" class="pm-empty" style="font-size: 12px">该环节暂无任务</div>
          </div>
        </div>

        <hr style="border: none; border-top: 1px solid var(--pm-neutral-200); margin: 10px 0" />
        <div style="font-size: 12px; color: var(--pm-neutral-500); margin-bottom: 6px">项目成员</div>
        <div v-for="member in members" :key="member.id" style="display: flex; justify-content: space-between; font-size: 12.5px; padding: 2px 0">
          <span>{{ member.userName }} · {{ PM_PROJECT_ROLE[member.projectRole] ?? member.projectRole }}</span>
          <span class="pm-tag is-muted">{{ member.inputRatio }}%</span>
        </div>
      </div>
    </div>

    <!-- 项目履历 -->
    <div v-if="project" class="pm-card" style="margin-top: 12px">
      <strong style="font-size: 13.5px">项目履历</strong>
      <table class="pm-table" style="margin-top: 8px">
        <thead><tr><th>时间</th><th>动作</th><th>内容</th><th>操作人</th></tr></thead>
        <tbody>
          <tr v-for="(entry, index) in logs" :key="index">
            <td>{{ entry.createdTime }}</td>
            <td><span class="pm-tag is-muted">{{ entry.action }}</span></td>
            <td>{{ entry.detail }}</td>
            <td>{{ entry.operatorName }}</td>
          </tr>
          <tr v-if="logs.length === 0"><td colspan="4" class="pm-empty">暂无履历</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
