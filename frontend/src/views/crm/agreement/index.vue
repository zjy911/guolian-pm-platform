<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import Pagination from '@/components/Pagination.vue'
import { accountApi, agreementApi } from '@/api/crm'
import type { CrmAccount, CrmAgreement } from '@/api/crm'
import { companyApi, orgApi } from '@/api'
import type { CompanyVO, OrgVO } from '@/api/types'
import { dateText, money, useCrmDict } from '../shared/useCrmDict'
import { useCrmList } from '../shared/useCrmList'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const dict = useCrmDict(['crm_agreement_status', 'crm_agreement_type'])

const { list, total, loading, loaded, pageNum, pageSize, query, load, reset } = useCrmList<CrmAgreement>(
  agreementApi.page,
  { keyword: '', status: '', accountId: null }
)

const canAdd = computed(() => userStore.hasPerm('crm:agreement:add'))
const canEdit = computed(() => userStore.hasPerm('crm:agreement:edit'))
const canSubmit = computed(() => userStore.hasPerm('crm:agreement:submit'))
const canArchive = computed(() => userStore.hasPerm('crm:agreement:archive'))
const canRenew = computed(() => userStore.hasPerm('crm:agreement:renew'))
const canRemove = computed(() => userStore.hasPerm('crm:agreement:remove'))
const readOnly = computed(() => userStore.profile?.readOnly ?? false)

const accountOptions = ref<CrmAccount[]>([])
const companies = ref<CompanyVO[]>([])
const orgs = ref<{ id: number; name: string }[]>([])

function flatten(nodes: OrgVO[], prefix = ''): { id: number; name: string }[] {
  const result: { id: number; name: string }[] = []
  nodes.forEach((node) => {
    result.push({ id: node.id, name: `${prefix}${node.orgName}` })
    result.push(...flatten(node.children || [], `${prefix}　`))
  })
  return result
}

async function loadOptions() {
  try {
    const [accountData, companyList, orgTree] = await Promise.all([
      accountApi.page({ pageSize: 50 }),
      companyApi.options(),
      orgApi.tree(true)
    ])
    accountOptions.value = accountData.list
    companies.value = companyList
    orgs.value = flatten(orgTree)
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function accountName(id?: number) {
  if (!id) return '—'
  return accountOptions.value.find((item) => item.id === id)?.companyName ?? `#${id}`
}

const formOpen = ref(false)
const saving = ref(false)
const form = reactive<Record<string, any>>({})

function resetForm() {
  Object.assign(form, {
    id: null,
    name: '',
    agreementType: 'FRAMEWORK',
    accountId: null,
    companyId: null,
    businessGroupId: null,
    signType: 'NEW',
    firstAgreementId: null,
    signDate: '',
    startDate: '',
    endDate: '',
    winAmount: null,
    remark: ''
  })
}

function openCreate() {
  resetForm()
  formOpen.value = true
}

function openEdit(row: CrmAgreement) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    name: row.name,
    agreementType: row.agreementType ?? 'FRAMEWORK',
    accountId: row.accountId ?? null,
    companyId: row.companyId ?? null,
    businessGroupId: row.businessGroupId ?? null,
    signType: row.signType ?? 'NEW',
    firstAgreementId: row.firstAgreementId ?? null,
    signDate: row.signDate ?? '',
    startDate: row.startDate ?? '',
    endDate: row.endDate ?? '',
    winAmount: row.winAmount ?? null,
    remark: row.remark ?? ''
  })
  formOpen.value = true
}

async function submit() {
  if (!form.name || !form.accountId || !form.companyId || !form.businessGroupId || !form.agreementType || !form.startDate) {
    appStore.toast('名称、客户、签约主体、业务线、协议类型、生效日期为必填项', 'error')
    return
  }
  saving.value = true
  try {
    const payload: Record<string, unknown> = {
      name: form.name,
      agreementType: form.agreementType,
      accountId: form.accountId,
      companyId: form.companyId,
      businessGroupId: form.businessGroupId,
      signType: form.signType,
      firstAgreementId: form.signType === 'RENEW' ? form.firstAgreementId : undefined,
      signDate: form.signDate || undefined,
      startDate: form.startDate,
      endDate: form.endDate || undefined,
      winAmount: form.winAmount ?? undefined,
      remark: form.remark || undefined
    }
    if (form.id) {
      await agreementApi.update(form.id as number, payload)
      appStore.toast('协议已更新')
    } else {
      await agreementApi.create(payload)
      appStore.toast('协议已创建，状态为草稿')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

async function submitFlow(row: CrmAgreement) {
  try {
    await agreementApi.submit(row.id)
    appStore.toast('已提交审批')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function withdraw(row: CrmAgreement) {
  const reason = window.prompt('撤回原因（选填）') ?? undefined
  try {
    await agreementApi.withdraw(row.id, reason)
    appStore.toast('已撤回')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

const archiveDialog = ref<{ open: boolean; row: CrmAgreement | null; paperBoxNo: string; paperCopies: number | null }>({
  open: false,
  row: null,
  paperBoxNo: '',
  paperCopies: null
})

function openArchive(row: CrmAgreement) {
  archiveDialog.value = { open: true, row, paperBoxNo: '', paperCopies: null }
}

async function submitArchive() {
  const row = archiveDialog.value.row
  if (!row) return
  try {
    await agreementApi.archive(row.id, {
      paperBoxNo: archiveDialog.value.paperBoxNo || undefined,
      paperCopies: archiveDialog.value.paperCopies ?? undefined
    })
    appStore.toast('已归档')
    archiveDialog.value.open = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function renew(row: CrmAgreement) {
  const endDate = window.prompt('续签协议到期日（yyyy-MM-dd）')
  if (!endDate) return
  try {
    await agreementApi.renew(row.id, {
      name: `${row.name}（续签）`,
      agreementType: row.agreementType,
      accountId: row.accountId,
      companyId: row.companyId,
      businessGroupId: row.businessGroupId,
      startDate: row.endDate ?? row.startDate,
      endDate,
      winAmount: row.winAmount
    })
    appStore.toast('续签协议已创建')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function remove(row: CrmAgreement) {
  if (!window.confirm(`确认删除协议「${row.name}」？`)) return
  try {
    await agreementApi.remove(row.id)
    appStore.toast('协议已删除')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function statusTag(status?: string) {
  if (status === 'ACTIVE') return 'is-success'
  if (status === 'APPROVING') return 'is-warning'
  if (status === 'EXPIRED') return 'is-muted'
  if (status === 'TERMINATED') return 'is-danger'
  return 'is-muted'
}

onMounted(async () => {
  await dict.loadDicts()
  await loadOptions()
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="协议管理" desc="框架协议可不填到期日（长期有效）">
      <template #actions>
        <button v-if="canAdd && !readOnly" class="pm-btn is-primary" @click="openCreate">
          <i class="ti ti-plus" /> 新增协议
        </button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="query.keyword" class="pm-input" placeholder="协议编号 / 名称" @keyup.enter="load" />
        </div>
        <div class="pm-field">
          <label>状态</label>
          <select v-model="query.status" class="pm-select">
            <option value="">全部</option>
            <option v-for="item in dict.options('crm_agreement_status')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-field">
          <label>客户</label>
          <select v-model="query.accountId" class="pm-select">
            <option :value="null">全部</option>
            <option v-for="item in accountOptions" :key="item.id" :value="item.id">{{ item.companyName }}</option>
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
              <th>协议编号</th>
              <th>名称</th>
              <th>客户</th>
              <th>类型</th>
              <th>状态</th>
              <th>生效 / 到期</th>
              <th>金额</th>
              <th>归档</th>
              <th style="width: 260px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td>{{ row.agreementNo || '—' }}</td>
              <td style="font-weight: 600">{{ row.name }}</td>
              <td>{{ accountName(row.accountId) }}</td>
              <td>{{ dict.label('crm_agreement_type', row.agreementType) }}</td>
              <td>
                <span class="pm-tag" :class="statusTag(row.status)">
                  {{ dict.label('crm_agreement_status', row.status) }}
                </span>
              </td>
              <td>
                {{ dateText(row.startDate) }}
                <div style="font-size: 12px; color: var(--pm-neutral-500)">至 {{ dateText(row.endDate) }}</div>
              </td>
              <td>{{ money(row.winAmount) }}</td>
              <td>{{ row.isArchived === 1 ? '已归档' : '未归档' }}</td>
              <td>
                <div class="pm-actions">
                  <button v-if="canEdit && !readOnly && row.status === 'DRAFT'" class="pm-btn is-ghost is-sm" @click="openEdit(row)">
                    编辑
                  </button>
                  <button
                    v-if="canSubmit && !readOnly && row.status === 'DRAFT'"
                    class="pm-btn is-ghost is-sm"
                    @click="submitFlow(row)"
                  >
                    提交审批
                  </button>
                  <button
                    v-if="canSubmit && !readOnly && row.status === 'APPROVING'"
                    class="pm-btn is-ghost is-sm"
                    @click="withdraw(row)"
                  >
                    撤回
                  </button>
                  <button
                    v-if="canArchive && !readOnly && row.status === 'ACTIVE' && row.isArchived !== 1"
                    class="pm-btn is-ghost is-sm"
                    @click="openArchive(row)"
                  >
                    归档
                  </button>
                  <button
                    v-if="canRenew && !readOnly && (row.status === 'ACTIVE' || row.status === 'EXPIRED')"
                    class="pm-btn is-ghost is-sm"
                    @click="renew(row)"
                  >
                    续签
                  </button>
                  <button
                    v-if="canRemove && !readOnly && row.status === 'DRAFT'"
                    class="pm-btn is-ghost is-sm"
                    @click="remove(row)"
                  >
                    删除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="loaded && list.length === 0">
              <td colspan="9" class="pm-empty">没有符合条件的协议</td>
            </tr>
            <tr v-else-if="loading">
              <td colspan="9" class="pm-empty">加载中…</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>

    <FormDrawer
      :open="formOpen"
      :title="form.id ? '编辑协议' : '新增协议'"
      :wide="true"
      :loading="saving"
      @close="formOpen = false"
      @submit="submit"
    >
      <div class="pm-form-grid">
        <div class="pm-form-item is-full">
          <label><span class="pm-required">*</span>协议名称</label>
          <input v-model="form.name" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>协议类型</label>
          <select v-model="form.agreementType" class="pm-select">
            <option v-for="item in dict.options('crm_agreement_type')" :key="item.dictValue" :value="item.dictValue">
              {{ item.dictLabel }}
            </option>
          </select>
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>客户</label>
          <select v-model="form.accountId" class="pm-select">
            <option :value="null">请选择</option>
            <option v-for="item in accountOptions" :key="item.id" :value="item.id">{{ item.companyName }}</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>我方签约主体</label>
          <select v-model="form.companyId" class="pm-select">
            <option :value="null">请选择</option>
            <option v-for="item in companies" :key="item.id" :value="item.id">
              {{ item.shortName || item.companyName }}
            </option>
          </select>
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>业务线（BG）</label>
          <select v-model="form.businessGroupId" class="pm-select">
            <option :value="null">请选择</option>
            <option v-for="item in orgs" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>签订类型</label>
          <select v-model="form.signType" class="pm-select">
            <option value="NEW">新签</option>
            <option value="RENEW">续签</option>
          </select>
        </div>
        <div v-if="form.signType === 'RENEW'" class="pm-form-item">
          <label><span class="pm-required">*</span>首次协议 ID</label>
          <input v-model.number="form.firstAgreementId" type="number" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>签订日期</label>
          <input v-model="form.signDate" type="date" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>生效日期</label>
          <input v-model="form.startDate" type="date" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>到期日期</label>
          <input v-model="form.endDate" type="date" class="pm-input" placeholder="框架协议可留空" />
        </div>
        <div class="pm-form-item">
          <label>协议金额（元）</label>
          <input v-model.number="form.winAmount" type="number" class="pm-input" />
        </div>
        <div class="pm-form-item is-full">
          <label>备注</label>
          <textarea v-model="form.remark" class="pm-input" />
        </div>
      </div>
    </FormDrawer>

    <div v-if="archiveDialog.open" class="pm-drawer-mask" @click.self="archiveDialog.open = false">
      <div class="pm-card" style="width: 420px; margin: auto">
        <h3 style="margin: 0 0 12px; font-size: 15px">纸质归档 · {{ archiveDialog.row?.name }}</h3>
        <div class="pm-form-item">
          <label>档案盒号</label>
          <input v-model="archiveDialog.paperBoxNo" class="pm-input" />
        </div>
        <div class="pm-form-item" style="margin-top: 10px">
          <label>纸质份数</label>
          <input v-model.number="archiveDialog.paperCopies" type="number" class="pm-input" />
        </div>
        <div class="pm-actions" style="justify-content: flex-end; margin-top: 12px">
          <button class="pm-btn" @click="archiveDialog.open = false">取消</button>
          <button class="pm-btn is-primary" @click="submitArchive">确认归档</button>
        </div>
      </div>
    </div>
  </div>
</template>
