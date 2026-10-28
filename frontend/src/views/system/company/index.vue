<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import Pagination from '@/components/Pagination.vue'
import { companyApi, sensitiveApi } from '@/api'
import type { CompanyVO } from '@/api/types'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const list = ref<CompanyVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const formOpen = ref(false)
const saving = ref(false)
const form = reactive<Record<string, unknown>>({})

const canEdit = userStore.hasPerm('system:company:edit')

async function load() {
  try {
    const data = await companyApi.page({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value || undefined })
    list.value = data.list
    total.value = data.total
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function openCreate() {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, {
    id: null,
    companyCode: '',
    companyName: '',
    shortName: '',
    companyType: 'HEAD',
    legalPerson: '',
    creditCode: '',
    regAddress: '',
    officeAddress: '',
    taxNo: '',
    bankName: '',
    bankAccount: '',
    status: 1
  })
  formOpen.value = true
}

function openEdit(row: CompanyVO) {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, row)
  formOpen.value = true
}

async function submit() {
  saving.value = true
  try {
    if (form.id) {
      await companyApi.update(form.id as number, { ...form })
      appStore.toast('主体已更新')
    } else {
      await companyApi.create({ ...form })
      appStore.toast('主体已创建')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

async function remove(row: CompanyVO) {
  if (!window.confirm(`确认删除主体「${row.companyName}」？`)) {
    return
  }
  try {
    await companyApi.remove(row.id)
    appStore.toast('主体已删除')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function viewPlain(row: CompanyVO, group: string) {
  try {
    const result = await sensitiveApi.plain('company', row.id, group)
    appStore.toast(`明文：${result.value}（本次查看已写入审计日志）`)
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="公司主体" desc="与组织无直接关系，只与用户关联">
      <template #actions>
        <button v-if="canEdit" class="pm-btn is-primary" @click="openCreate"><i class="ti ti-plus" /> 新增主体</button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="keyword" class="pm-input" placeholder="名称 / 编码 / 简称" @keyup.enter="load" />
        </div>
        <div class="pm-actions">
          <button class="pm-btn is-primary" @click="load"><i class="ti ti-search" /> 查询</button>
        </div>
      </div>
    </div>

    <div class="pm-card">
      <div class="pm-table-wrap">
        <table class="pm-table">
          <thead>
            <tr><th>主体名称</th><th>编码</th><th>统一社会信用代码</th><th>法人</th><th>银行账号</th><th>在岗人数</th><th>状态</th><th style="width: 200px">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td style="font-weight: 600">{{ row.companyName }}</td>
              <td>{{ row.companyCode }}</td>
              <td>{{ row.creditCode }}</td>
              <td>{{ row.legalPerson }}</td>
              <td>
                <span>{{ row.bankAccount || '（已隐藏）' }}</span>
                <button class="pm-btn is-ghost is-sm" @click="viewPlain(row, 'BANK_ACCOUNT')">查看明文</button>
              </td>
              <td>{{ row.userCount }}</td>
              <td><span class="pm-tag" :class="row.status === 1 ? 'is-success' : 'is-muted'">{{ row.status === 1 ? '启用' : '停用' }}</span></td>
              <td>
                <div class="pm-actions">
                  <button v-if="canEdit" class="pm-btn is-ghost is-sm" @click="openEdit(row)">编辑</button>
                  <button v-if="canEdit" class="pm-btn is-ghost is-sm is-danger" @click="remove(row)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>

    <FormDrawer :open="formOpen" :title="form.id ? '编辑公司主体' : '新增公司主体'" :loading="saving" @close="formOpen = false" @submit="submit">
      <div class="pm-form-grid">
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>主体编码</label>
          <input v-model="form.companyCode" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>主体全称</label>
          <input v-model="form.companyName" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>简称</label>
          <input v-model="form.shortName" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>主体类型</label>
          <select v-model="form.companyType" class="pm-select">
            <option value="HEAD">总公司</option>
            <option value="BRANCH">分公司</option>
            <option value="SUB">子公司</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>法人</label>
          <input v-model="form.legalPerson" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>信用代码</label>
          <input v-model="form.creditCode" class="pm-input" placeholder="18 位" />
        </div>
        <div class="pm-form-item is-full">
          <label>注册地址</label>
          <input v-model="form.regAddress" class="pm-input" />
        </div>
        <div class="pm-form-item is-full">
          <label>办公地址</label>
          <input v-model="form.officeAddress" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>开户行</label>
          <input v-model="form.bankName" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>银行账号</label>
          <input v-model="form.bankAccount" class="pm-input" />
        </div>
      </div>
    </FormDrawer>
  </div>
</template>
