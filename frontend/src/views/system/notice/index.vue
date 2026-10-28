<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import Pagination from '@/components/Pagination.vue'
import { noticeApi } from '@/api'
import type { NoticeVO } from '@/api/types'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const list = ref<NoticeVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const query = reactive({ keyword: '', noticeType: null as number | null })
const formOpen = ref(false)
const saving = ref(false)
const form = reactive<Record<string, any>>({})

const canAdd = userStore.hasPerm('system:notice:add')
const canEdit = userStore.hasPerm('system:notice:edit')

async function load() {
  try {
    const data = await noticeApi.page({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: query.keyword || undefined,
      noticeType: query.noticeType ?? undefined
    })
    list.value = data.list
    total.value = data.total
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function open(row?: NoticeVO) {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, row ?? { id: null, title: '', noticeType: 2, content: '', pinned: 0, status: 1, receiverScope: 'ALL' })
  formOpen.value = true
}

async function submit() {
  saving.value = true
  try {
    if (form.id) {
      await noticeApi.update(form.id as number, { ...form })
    } else {
      await noticeApi.create({ ...form })
    }
    appStore.toast('已保存')
    formOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

async function remove(row: NoticeVO) {
  if (!window.confirm(`确认删除「${row.title}」？`)) {
    return
  }
  try {
    await noticeApi.remove(row.id)
    appStore.toast('已删除')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="通知公告" desc="与 Topbar 铃铛联动">
      <template #actions>
        <button v-if="canAdd" class="pm-btn is-primary" @click="open()"><i class="ti ti-plus" /> 新增公告</button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>标题</label>
          <input v-model="query.keyword" class="pm-input" @keyup.enter="load" />
        </div>
        <div class="pm-field">
          <label>类型</label>
          <select v-model="query.noticeType" class="pm-select">
            <option :value="null">全部</option>
            <option :value="1">通知</option>
            <option :value="2">公告</option>
            <option :value="3">待办</option>
          </select>
        </div>
        <div class="pm-actions"><button class="pm-btn is-primary" @click="load"><i class="ti ti-search" /> 查询</button></div>
      </div>
    </div>

    <div class="pm-card">
      <table class="pm-table">
        <thead><tr><th>标题</th><th>类型</th><th>置顶</th><th>状态</th><th>发布时间</th><th style="width: 140px">操作</th></tr></thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td style="font-weight: 600">{{ row.title }}</td>
            <td>{{ { 1: '通知', 2: '公告', 3: '待办' }[row.noticeType] }}</td>
            <td>{{ row.pinned === 1 ? '是' : '否' }}</td>
            <td><span class="pm-tag" :class="row.status === 1 ? 'is-success' : 'is-muted'">{{ row.status === 1 ? '已发布' : '草稿' }}</span></td>
            <td>{{ row.publishTime }}</td>
            <td>
              <div class="pm-actions">
                <button v-if="canEdit" class="pm-btn is-ghost is-sm" @click="open(row)">编辑</button>
                <button v-if="canEdit" class="pm-btn is-ghost is-sm is-danger" @click="remove(row)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>

    <FormDrawer :open="formOpen" :title="form.id ? '编辑公告' : '新增公告'" :wide="true" :loading="saving" @close="formOpen = false" @submit="submit">
      <div class="pm-form-grid">
        <div class="pm-form-item is-full"><label><span class="pm-required">*</span>标题</label><input v-model="form.title" class="pm-input" /></div>
        <div class="pm-form-item">
          <label>类型</label>
          <select v-model="form.noticeType" class="pm-select"><option :value="1">通知</option><option :value="2">公告</option><option :value="3">待办</option></select>
        </div>
        <div class="pm-form-item">
          <label>置顶</label>
          <select v-model="form.pinned" class="pm-select"><option :value="1">置顶</option><option :value="0">普通</option></select>
        </div>
        <div class="pm-form-item is-full"><label>内容</label><textarea v-model="form.content" class="pm-input" style="min-height: 160px" /></div>
      </div>
    </FormDrawer>
  </div>
</template>
