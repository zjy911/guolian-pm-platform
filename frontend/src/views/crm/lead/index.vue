<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import Pagination from '@/components/Pagination.vue'
import { crmCommonApi, leadApi } from '@/api/crm'
import type { CrmLead, DupHit } from '@/api/crm'
import { dateTimeText, useCrmDict } from '../shared/useCrmDict'
import { useCrmList } from '../shared/useCrmList'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const router = useRouter()
const dict = useCrmDict([
  'crm_lead_status',
  'crm_lead_source',
  'crm_lead_grade',
  'crm_account_nature',
  'crm_industry'
])

const { list, total, loading, loaded, pageNum, pageSize, query, load, reset } = useCrmList<CrmLead>(
  leadApi.page,
  { keyword: '', status: '', grade: '', channel: '', accountNature: '' }
)

const canAdd = computed(() => userStore.hasPerm('crm:lead:add'))
const canEdit = computed(() => userStore.hasPerm('crm:lead:edit'))
const canRemove = computed(() => userStore.hasPerm('crm:lead:remove'))
const canConvert = computed(() => userStore.hasPerm('crm:lead:convert'))
const canImport = computed(() => userStore.hasPerm('crm:lead:import'))
const readOnly = computed(() => userStore.profile?.readOnly ?? false)

// ---------------- 新增 / 编辑 ----------------
const formOpen = ref(false)
const saving = ref(false)
const form = reactive<Record<string, any>>({})

function resetForm() {
  Object.assign(form, {
    id: null,
    name: '',
    companyName: '',
    mobile: '',
    email: '',
    positionName: '',
    industry: '',
    sourceChannel: 'WEB',
    grade: 'C',
    accountNature: 'TO_B',
    dupReason: '',
    remark: ''
  })
}

function openCreate() {
  resetForm()
  formOpen.value = true
}

function openEdit(row: CrmLead) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    name: row.name,
    companyName: row.companyName ?? '',
    mobile: row.mobile ?? '',
    email: row.email ?? '',
    positionName: row.positionName ?? '',
    industry: row.industry ?? '',
    sourceChannel: row.sourceChannel ?? 'WEB',
    grade: row.grade ?? 'C',
    accountNature: row.accountNature ?? 'TO_B',
    dupReason: row.dupReason ?? '',
    remark: row.remark ?? ''
  })
  formOpen.value = true
}

async function submit() {
  if (!form.name || !form.mobile) {
    appStore.toast('姓名与手机号为必填项', 'error')
    return
  }
  saving.value = true
  try {
    const payload: Record<string, unknown> = {
      name: form.name,
      companyName: form.companyName || undefined,
      mobile: form.mobile,
      email: form.email || undefined,
      positionName: form.positionName || undefined,
      industry: form.industry || undefined,
      sourceChannel: form.sourceChannel,
      grade: form.grade,
      accountNature: form.accountNature,
      dupReason: form.dupReason || undefined,
      remark: form.remark || undefined
    }
    if (form.id) {
      await leadApi.update(form.id as number, payload)
      appStore.toast('线索已更新')
    } else {
      await leadApi.create(payload)
      appStore.toast('线索已创建')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

// ---------------- 查重 ----------------
const dupHits = ref<DupHit[]>([])
const checking = ref(false)

async function checkDup() {
  if (!form.mobile && !form.email) return
  checking.value = true
  try {
    const result = await crmCommonApi.checkDuplicate({
      type: 'LEAD',
      mobile: form.mobile || undefined,
      email: form.email || undefined
    })
    dupHits.value = result.hits ?? []
  } catch {
    dupHits.value = []
  } finally {
    checking.value = false
  }
}

// ---------------- 作废 ----------------
const invalidDialog = ref<{ open: boolean; row: CrmLead | null; reason: string }>({
  open: false,
  row: null,
  reason: ''
})

function openInvalid(row: CrmLead) {
  invalidDialog.value = { open: true, row, reason: '' }
}

async function submitInvalid() {
  const row = invalidDialog.value.row
  if (!row) return
  if (!invalidDialog.value.reason.trim()) {
    appStore.toast('请填写作废原因', 'error')
    return
  }
  try {
    await leadApi.invalid(row.id, invalidDialog.value.reason.trim())
    appStore.toast('线索已作废')
    invalidDialog.value.open = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function remove(row: CrmLead) {
  if (!window.confirm(`确认删除线索「${row.name}」？`)) return
  try {
    await leadApi.remove(row.id)
    appStore.toast('线索已删除')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function downloadTemplate() {
  try {
    await crmCommonApi.importTemplate('lead')
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function statusTag(status?: string) {
  if (status === 'NEW') return 'is-solid'
  if (status === 'FOLLOWING') return 'is-warning'
  if (status === 'CONVERTED') return 'is-success'
  if (status === 'INVALID') return 'is-danger'
  return 'is-muted'
}

onMounted(async () => {
  await dict.loadDicts()
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="线索管理" desc="手机号 / 邮箱加密落库，查重走哈希匹配">
      <template #actions>
        <button v-if="canImport" class="pm-btn" @click="downloadTemplate"><i class="ti ti-download" /> 导入模板</button>
        <button v-if="canConvert" class="pm-btn" @click="router.push('/crm/lead-convert')">
          <i class="ti ti-transform" /> 转化工作台
        </button>
        <button v-if="canAdd && !readOnly" class="pm-btn is-primary" @click="openCreate">
          <i class="ti ti-plus" /> 新增线索
        </button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="query.keyword" class="pm-input" placeholder="姓名 / 公司" @keyup.enter="load" />
        </div>
        <div class="pm-field">
          <label>状态</label>
          <select v-model="query.status" class="pm-select">
            <option value="">全部</option>
            <option v-for="item in dict.options('crm_lead_status')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-field">
          <label>等级</label>
          <select v-model="query.grade" class="pm-select">
            <option value="">全部</option>
            <option v-for="item in dict.options('crm_lead_grade')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-field">
          <label>来源</label>
          <select v-model="query.channel" class="pm-select">
            <option value="">全部</option>
            <option v-for="item in dict.options('crm_lead_source')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-actions">
          <button class="pm-btn is-primary" @click="load"><i class="ti ti-search" /> 查询</button>
          <button class="pm-btn" @click="reset">重置</button>
        </div>
      </div>
    </div>

    <div class="pm-card">
      <div class="pm-table-wrap">
        <table class="pm-table">
          <thead>
            <tr>
              <th>姓名 / 公司</th>
              <th>联系方式</th>
              <th>来源 / 等级</th>
              <th>性质</th>
              <th>状态</th>
              <th>负责人</th>
              <th>最近跟进</th>
              <th style="width: 220px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td>
                <div style="font-weight: 600">{{ row.name }}</div>
                <div style="font-size: 12px; color: var(--pm-neutral-500)">{{ row.companyName || '—' }}</div>
              </td>
              <td>
                <div>{{ row.mobile || '—' }}</div>
                <div style="font-size: 12px; color: var(--pm-neutral-500)">{{ row.email || '—' }}</div>
              </td>
              <td>
                {{ dict.label('crm_lead_source', row.sourceChannel) }}
                <div><span class="pm-tag is-muted">{{ dict.label('crm_lead_grade', row.grade) }}</span></div>
              </td>
              <td>{{ dict.label('crm_account_nature', row.accountNature) }}</td>
              <td>
                <span class="pm-tag" :class="statusTag(row.status)">{{ dict.label('crm_lead_status', row.status) }}</span>
                <div v-if="row.invalidReason" style="font-size: 12px; color: var(--pm-danger)">
                  {{ row.invalidReason }}
                </div>
              </td>
              <td>{{ row.ownerUserName || '—' }}</td>
              <td>{{ dateTimeText(row.lastFollowTime) }}</td>
              <td>
                <div class="pm-actions">
                  <button v-if="canEdit && !readOnly" class="pm-btn is-ghost is-sm" @click="openEdit(row)">编辑</button>
                  <button
                    v-if="canConvert && !readOnly && row.status !== 'CONVERTED' && row.status !== 'INVALID'"
                    class="pm-btn is-ghost is-sm"
                    @click="router.push({ path: '/crm/lead-convert', query: { leadId: row.id } })"
                  >
                    转化
                  </button>
                  <button
                    v-if="canEdit && !readOnly && row.status !== 'CONVERTED' && row.status !== 'INVALID'"
                    class="pm-btn is-ghost is-sm"
                    @click="openInvalid(row)"
                  >
                    作废
                  </button>
                  <button v-if="canRemove && !readOnly" class="pm-btn is-ghost is-sm" @click="remove(row)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="loaded && list.length === 0">
              <td colspan="8" class="pm-empty">没有符合条件的线索</td>
            </tr>
            <tr v-else-if="loading">
              <td colspan="8" class="pm-empty">加载中…</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>

    <!-- 新增 / 编辑 -->
    <FormDrawer
      :open="formOpen"
      :title="form.id ? '编辑线索' : '新增线索'"
      :wide="true"
      :loading="saving"
      @close="formOpen = false"
      @submit="submit"
    >
      <div class="pm-form-grid">
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>姓名</label>
          <input v-model="form.name" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>手机号</label>
          <input v-model="form.mobile" class="pm-input" placeholder="加密存储，用于查重" @blur="checkDup" />
        </div>
        <div class="pm-form-item">
          <label>邮箱</label>
          <input v-model="form.email" class="pm-input" @blur="checkDup" />
        </div>
        <div class="pm-form-item">
          <label>公司名称</label>
          <input v-model="form.companyName" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>职位</label>
          <input v-model="form.positionName" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>所属行业</label>
          <select v-model="form.industry" class="pm-select">
            <option value="">请选择</option>
            <option v-for="item in dict.options('crm_industry')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>来源渠道</label>
          <select v-model="form.sourceChannel" class="pm-select">
            <option v-for="item in dict.options('crm_lead_source')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>线索等级</label>
          <select v-model="form.grade" class="pm-select">
            <option v-for="item in dict.options('crm_lead_grade')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>客户性质</label>
          <select v-model="form.accountNature" class="pm-select">
            <option v-for="item in dict.options('crm_account_nature')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-form-item is-full">
          <label>备注</label>
          <textarea v-model="form.remark" class="pm-input" />
        </div>
      </div>

      <div v-if="checking" class="pm-tag is-muted" style="margin-top: 8px">正在查重…</div>
      <div v-else-if="dupHits.length > 0" class="pm-card" style="background: var(--pm-warning-bg); margin-top: 12px">
        <div style="font-weight: 600; font-size: 13px; margin-bottom: 6px">
          <i class="ti ti-alert-triangle" /> 查到 {{ dupHits.length }} 条疑似重复
        </div>
        <div v-for="hit in dupHits" :key="`${hit.type}-${hit.id}`" style="font-size: 13px; margin-bottom: 4px">
          <span class="pm-tag is-muted">{{ hit.type }}</span>
          {{ hit.name || hit.companyName }} · 负责人 {{ hit.ownerUserName || '—' }} ·
          {{ hit.mobileMasked || '—' }}
        </div>
        <div class="pm-form-item" style="margin-top: 8px">
          <label><span class="pm-required">*</span>强制建线原因</label>
          <input v-model="form.dupReason" class="pm-input" placeholder="确认不是同一人时填写，例如「同名不同客户」" />
        </div>
      </div>
    </FormDrawer>

    <!-- 作废 -->
    <div v-if="invalidDialog.open" class="pm-drawer-mask" @click.self="invalidDialog.open = false">
      <div class="pm-card" style="width: 420px; margin: auto">
        <h3 style="margin: 0 0 12px; font-size: 15px">作废线索 · {{ invalidDialog.row?.name }}</h3>
        <textarea v-model="invalidDialog.reason" class="pm-input" rows="3" placeholder="作废原因（必填）" />
        <div class="pm-actions" style="justify-content: flex-end; margin-top: 12px">
          <button class="pm-btn" @click="invalidDialog.open = false">取消</button>
          <button class="pm-btn is-danger" @click="submitInvalid">确认作废</button>
        </div>
      </div>
    </div>
  </div>
</template>
