<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import Pagination from '@/components/Pagination.vue'
import { accountApi, contactApi } from '@/api/crm'
import type { CrmAccount, CrmContact } from '@/api/crm'
import { dateText, dateTimeText } from '../shared/useCrmDict'
import { useCrmList } from '../shared/useCrmList'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()

const { list, total, loading, loaded, pageNum, pageSize, query, load, reset } = useCrmList<CrmContact>(
  contactApi.page,
  { keyword: '', accountId: null }
)

const canAdd = computed(() => userStore.hasPerm('crm:contact:add'))
const canEdit = computed(() => userStore.hasPerm('crm:contact:edit'))
const canRemove = computed(() => userStore.hasPerm('crm:contact:remove'))
const readOnly = computed(() => userStore.profile?.readOnly ?? false)

const accountOptions = ref<CrmAccount[]>([])
const accountKeyword = ref('')

async function searchAccounts() {
  try {
    const data = await accountApi.page({ keyword: accountKeyword.value || undefined, pageSize: 20 })
    accountOptions.value = data.list
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function accountName(id?: number) {
  if (!id) return '—'
  return accountOptions.value.find((item) => item.id === id)?.companyName ?? `#${id}`
}

// ---------------- 新增 / 编辑 ----------------
const formOpen = ref(false)
const saving = ref(false)
const form = reactive<Record<string, any>>({})

function resetForm() {
  Object.assign(form, {
    id: null,
    name: '',
    accountId: null,
    mobile: '',
    email: '',
    positionName: '',
    department: '',
    birthday: '',
    address: '',
    remark: ''
  })
}

function openCreate() {
  resetForm()
  formOpen.value = true
  if (accountOptions.value.length === 0) searchAccounts()
}

function openEdit(row: CrmContact) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    name: row.name,
    accountId: row.accountId ?? null,
    mobile: row.mobile ?? '',
    email: row.email ?? '',
    positionName: row.positionName ?? '',
    department: row.department ?? '',
    birthday: row.birthday ?? '',
    address: row.address ?? '',
    remark: row.remark ?? ''
  })
  formOpen.value = true
}

async function submit() {
  if (!form.name || !form.accountId) {
    appStore.toast('姓名与所属客户为必填项', 'error')
    return
  }
  saving.value = true
  try {
    const payload: Record<string, unknown> = {
      name: form.name,
      accountId: form.accountId,
      mobile: form.mobile || undefined,
      email: form.email || undefined,
      positionName: form.positionName || undefined,
      department: form.department || undefined,
      birthday: form.birthday || undefined,
      address: form.address || undefined,
      remark: form.remark || undefined
    }
    if (form.id) {
      await contactApi.update(form.id as number, payload)
      appStore.toast('联系人已更新')
    } else {
      await contactApi.create(payload)
      appStore.toast('联系人已创建')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

async function setPrimary(row: CrmContact) {
  try {
    await contactApi.setPrimary(row.id)
    appStore.toast('已设为主联系人')
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function remove(row: CrmContact) {
  if (!window.confirm(`确认删除联系人「${row.name}」？`)) return
  try {
    await contactApi.remove(row.id)
    appStore.toast('联系人已删除')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

onMounted(async () => {
  await searchAccounts()
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="联系人管理" desc="手机号 / 邮箱加密存储，按角色脱敏">
      <template #actions>
        <button v-if="canAdd && !readOnly" class="pm-btn is-primary" @click="openCreate">
          <i class="ti ti-plus" /> 新增联系人
        </button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="query.keyword" class="pm-input" placeholder="姓名 / 手机 / 邮箱" @keyup.enter="load" />
        </div>
        <div class="pm-field">
          <label>所属客户</label>
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
              <th>姓名</th>
              <th>所属客户</th>
              <th>职位 / 部门</th>
              <th>手机</th>
              <th>邮箱</th>
              <th>生日</th>
              <th>最近跟进</th>
              <th style="width: 210px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td style="font-weight: 600">{{ row.name }}</td>
              <td>{{ accountName(row.accountId) }}</td>
              <td>
                {{ row.positionName || '—' }}
                <div style="font-size: 12px; color: var(--pm-neutral-500)">{{ row.department || '—' }}</div>
              </td>
              <td>{{ row.mobile || '—' }}</td>
              <td>{{ row.email || '—' }}</td>
              <td>{{ dateText(row.birthday) }}</td>
              <td>{{ dateTimeText(row.lastActivityTime) }}</td>
              <td>
                <div class="pm-actions">
                  <button v-if="canEdit && !readOnly" class="pm-btn is-ghost is-sm" @click="openEdit(row)">编辑</button>
                  <button v-if="canEdit && !readOnly" class="pm-btn is-ghost is-sm" @click="setPrimary(row)">
                    设主联系人
                  </button>
                  <button v-if="canRemove && !readOnly" class="pm-btn is-ghost is-sm" @click="remove(row)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="loaded && list.length === 0">
              <td colspan="8" class="pm-empty">没有符合条件的联系人</td>
            </tr>
            <tr v-else-if="loading">
              <td colspan="8" class="pm-empty">加载中…</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>

    <FormDrawer
      :open="formOpen"
      :title="form.id ? '编辑联系人' : '新增联系人'"
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
          <label><span class="pm-required">*</span>所属客户</label>
          <select v-model="form.accountId" class="pm-select">
            <option :value="null">请选择</option>
            <option v-for="item in accountOptions" :key="item.id" :value="item.id">{{ item.companyName }}</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>手机号</label>
          <input v-model="form.mobile" class="pm-input" placeholder="加密存储" />
        </div>
        <div class="pm-form-item">
          <label>邮箱</label>
          <input v-model="form.email" class="pm-input" placeholder="加密存储" />
        </div>
        <div class="pm-form-item">
          <label>职位</label>
          <input v-model="form.positionName" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>部门</label>
          <input v-model="form.department" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>生日</label>
          <input v-model="form.birthday" type="date" class="pm-input" />
        </div>
        <div class="pm-form-item is-full">
          <label>地址</label>
          <input v-model="form.address" class="pm-input" placeholder="按脱敏策略展示" />
        </div>
        <div class="pm-form-item is-full">
          <label>备注</label>
          <textarea v-model="form.remark" class="pm-input" />
        </div>
      </div>
    </FormDrawer>
  </div>
</template>
