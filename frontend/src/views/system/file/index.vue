<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import Pagination from '@/components/Pagination.vue'
import { fileApi } from '@/api'
import { useAppStore } from '@/stores/app'
import { tokenStore } from '@/api/request'

const appStore = useAppStore()
const rows = ref<Record<string, unknown>[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const uploading = ref(false)

async function load() {
  try {
    const data = await fileApi.page({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value || undefined })
    rows.value = data.list
    total.value = data.total
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function upload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }
  uploading.value = true
  try {
    const form = new FormData()
    form.append('file', file)
    const response = await fetch('/api/v1/system/files/upload', {
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenStore.get() ?? ''}` },
      body: form
    })
    const result = await response.json()
    if (result.code !== 0) {
      throw new Error(result.msg)
    }
    appStore.toast('上传成功')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function remove(row: Record<string, unknown>) {
  if (!window.confirm('确认删除该附件？')) {
    return
  }
  try {
    await fileApi.remove(Number(row.id))
    appStore.toast('已删除')
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function download(row: Record<string, unknown>) {
  window.open(fileApi.downloadUrl(Number(row.id)), '_blank')
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="附件管理" desc="统一附件表，业务模块通过 fileId 引用">
      <template #actions>
        <label class="pm-btn is-primary" style="cursor: pointer">
          {{ uploading ? '上传中…' : '上传附件' }}
          <input type="file" style="display: none" @change="upload" />
        </label>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>文件名</label>
          <input v-model="keyword" class="pm-input" @keyup.enter="load" />
        </div>
        <div class="pm-actions"><button class="pm-btn is-primary" @click="load"><i class="ti ti-search" /> 查询</button></div>
      </div>
    </div>

    <div class="pm-card">
      <table class="pm-table">
        <thead><tr><th>文件名</th><th>大小</th><th>类型</th><th>业务对象</th><th>上传时间</th><th style="width: 160px">操作</th></tr></thead>
        <tbody>
          <tr v-for="row in rows" :key="String(row.id)">
            <td>{{ row.originalName }}</td>
            <td>{{ Math.round(Number(row.fileSize) / 1024) }} KB</td>
            <td>{{ row.contentType }}</td>
            <td>{{ row.bizType || '—' }}</td>
            <td>{{ row.createdTime }}</td>
            <td>
              <div class="pm-actions">
                <button class="pm-btn is-ghost is-sm" @click="download(row)">下载</button>
                <button class="pm-btn is-ghost is-sm is-danger" @click="remove(row)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="rows.length === 0"><td colspan="6" class="pm-empty">暂无附件</td></tr>
        </tbody>
      </table>
      <Pagination v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </div>
  </div>
</template>
