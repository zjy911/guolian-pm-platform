<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import Pagination from '@/components/Pagination.vue'
import { productApi, PM_PRICING, type PmProduct } from '@/api/pm'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const list = ref<PmProduct[]>([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const query = reactive({ keyword: '', status: '' })

const formOpen = ref(false)
const saving = ref(false)
const form = reactive<Record<string, any>>({
  id: null as number | null,
  productCode: '',
  productName: '',
  bgOrgId: null as number | null,
  bgOrgName: '',
  pricingMode: 'UNIT_PRICE',
  remark: '',
  templateJson: ''
})
const templateOpen = ref(false)
const templateProduct = ref<PmProduct | null>(null)
const templateJson = ref('')

async function load() {
  loading.value = true
  try {
    const data = await productApi.page({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: query.keyword || undefined,
      status: query.status || undefined
    })
    list.value = data.list
    total.value = data.total
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: null,
    productCode: '',
    productName: '',
    bgOrgId: null,
    bgOrgName: '',
    pricingMode: 'UNIT_PRICE',
    remark: '',
    templateJson: ''
  })
}

function openCreate() {
  resetForm()
  formOpen.value = true
}

function openEdit(row: PmProduct) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    productCode: row.productCode,
    productName: row.productName,
    bgOrgId: row.bgOrgId,
    bgOrgName: row.bgOrgName ?? '',
    pricingMode: row.pricingMode,
    remark: row.remark ?? ''
  })
  formOpen.value = true
}

async function submit() {
  saving.value = true
  try {
    if (form.id) {
      await productApi.update(form.id, { ...form })
      appStore.toast('产品已更新')
    } else {
      await productApi.create({ ...form })
      appStore.toast('产品已创建，请继续配置并发布任务模板')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

async function openTemplate(row: PmProduct) {
  templateProduct.value = row
  try {
    const detail = await productApi.detail(row.id)
    templateJson.value = detail.template ? JSON.stringify(JSON.parse(detail.template.templateJson), null, 2) : ''
  } catch {
    templateJson.value = ''
  }
  templateOpen.value = true
}

async function publish() {
  if (!templateProduct.value) return
  try {
    JSON.parse(templateJson.value)
  } catch {
    appStore.toast('模板 JSON 格式不正确', 'error')
    return
  }
  try {
    const result = await productApi.publishTemplate(templateProduct.value.id, templateJson.value)
    appStore.toast(`模板已发布，版本 v${result.version}.0（已立项项目不受影响）`)
    templateOpen.value = false
    await load()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function pricingLabel(mode?: string) {
  return PM_PRICING[mode ?? ''] ?? mode ?? '—'
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="产品管理" desc="设计态：服务产品与任务模板，模板发布后项目实例化取快照">
      <template #actions>
        <button class="pm-btn is-primary" @click="openCreate"><i class="ti ti-plus" /> 新增产品</button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <div class="pm-filter">
        <div class="pm-field">
          <label>关键字</label>
          <input v-model="query.keyword" class="pm-input" placeholder="产品编码 / 名称" @keyup.enter="load" />
        </div>
        <div class="pm-field">
          <label>状态</label>
          <select v-model="query.status" class="pm-select">
            <option value="">全部</option>
            <option value="ON_SALE">在售</option>
            <option value="DISABLED">停用</option>
          </select>
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
            <tr><th>产品编码</th><th>产品名称</th><th>归属 BG</th><th>默认计价</th><th>模板版本</th><th>状态</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td>{{ row.productCode }}</td>
              <td>{{ row.productName }}</td>
              <td>{{ row.bgOrgName ?? '—' }}</td>
              <td>{{ pricingLabel(row.pricingMode) }}</td>
              <td><span class="pm-tag is-muted">{{ row.templateVersion }}</span></td>
              <td><span class="pm-tag" :class="row.status === 'ON_SALE' ? 'is-success' : 'is-muted'">{{ row.status === 'ON_SALE' ? '在售' : '停用' }}</span></td>
              <td style="white-space: nowrap">
                <button class="pm-btn is-sm" @click="openTemplate(row)">任务模板</button>
                <button class="pm-btn is-sm" style="margin-left: 6px" @click="openEdit(row)">编辑</button>
              </td>
            </tr>
            <tr v-if="list.length === 0"><td colspan="7" class="pm-empty">暂无产品</td></tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:pageNum="pageNum" v-model:pageSize="pageSize" :total="total" />
    </div>

    <!-- 产品表单 -->
    <div v-if="formOpen" class="pm-drawer-mask" @click.self="formOpen = false">
      <div class="pm-card" style="width: 520px; margin: auto; max-height: 86vh; overflow: auto">
        <h3 style="margin: 0 0 12px; font-size: 15px">{{ form.id ? '编辑产品' : '新增产品' }}</h3>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>产品编码</label>
          <input v-model="form.productCode" class="pm-input" placeholder="如 PM-PROD-004" />
        </div>
        <div class="pm-form-item">
          <label><span class="pm-required">*</span>产品名称</label>
          <input v-model="form.productName" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>归属 BG 组织 ID</label>
          <input v-model.number="form.bgOrgId" type="number" class="pm-input" placeholder="组织树 BG 节点 ID" />
        </div>
        <div class="pm-form-item">
          <label>归属 BG 名称</label>
          <input v-model="form.bgOrgName" class="pm-input" />
        </div>
        <div class="pm-form-item">
          <label>默认计价口径</label>
          <select v-model="form.pricingMode" class="pm-select">
            <option v-for="(label, key) in PM_PRICING" :key="key" :value="key">{{ label }}</option>
          </select>
        </div>
        <div class="pm-form-item">
          <label>备注</label>
          <textarea v-model="form.remark" class="pm-input" />
        </div>
        <div style="display: flex; justify-content: flex-end; gap: 8px">
          <button class="pm-btn" @click="formOpen = false">取消</button>
          <button class="pm-btn is-primary" :disabled="saving" @click="submit">保存</button>
        </div>
      </div>
    </div>

    <!-- 任务模板 -->
    <div v-if="templateOpen" class="pm-drawer-mask" @click.self="templateOpen = false">
      <div class="pm-card" style="width: 720px; margin: auto; max-height: 86vh; overflow: auto">
        <h3 style="margin: 0 0 4px; font-size: 15px">任务模板 · {{ templateProduct?.productName }}</h3>
        <p style="font-size: 12px; color: var(--pm-neutral-500); margin: 0 0 10px">
          6 环节固定（立项/筹备/实施/交接/运营/撤场）；任务以 JSON 配置，发布生成新版本快照，不影响已立项项目。
        </p>
        <textarea v-model="templateJson" class="pm-input" rows="18" style="font-family: monospace; font-size: 12px" />
        <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 10px">
          <button class="pm-btn" @click="templateOpen = false">取消</button>
          <button class="pm-btn is-primary" @click="publish">发布模板</button>
        </div>
      </div>
    </div>
  </div>
</template>
