<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { menuApi } from '@/api'
import type { MenuNode } from '@/api/types'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const tree = ref<MenuNode[]>([])
const formOpen = ref(false)
const saving = ref(false)
const form = reactive<Record<string, any>>({})
const canEdit = userStore.hasPerm('system:menu:edit')

async function load() {
  try {
    tree.value = await menuApi.tree()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

function open(node?: MenuNode, parent?: MenuNode) {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(
    form,
    node
      ? { ...node }
      : { id: null, parentId: parent?.id ?? 0, menuName: '', menuType: 2, path: '', component: '', perms: '', icon: '', sortNo: 0, visible: 1, status: 1 }
  )
  formOpen.value = true
}

async function submit() {
  saving.value = true
  try {
    const payload = { ...form }
    delete payload.children
    if (form.id) {
      await menuApi.update(form.id as number, payload)
    } else {
      await menuApi.create(payload)
    }
    appStore.toast('菜单已保存')
    formOpen.value = false
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
    <PageHeader title="菜单管理" desc="目录 → 菜单 → 按钮，权限标识 module:resource:action">
      <template #actions>
        <button v-if="canEdit" class="pm-btn is-primary" @click="open()"><i class="ti ti-plus" /> 新增一级目录</button>
      </template>
    </PageHeader>

    <div class="pm-card">
      <table class="pm-table">
        <thead><tr><th>名称</th><th>类型</th><th>路由</th><th>组件</th><th>权限标识</th><th>排序</th><th style="width: 180px">操作</th></tr></thead>
        <tbody>
          <template v-for="group in tree" :key="group.id">
            <tr>
              <td style="font-weight: 600"><i :class="`ti ti-${group.icon || 'folder'}`" /> {{ group.name }}</td>
              <td><span class="pm-tag">目录</span></td>
              <td>{{ group.path }}</td>
              <td>{{ group.component }}</td>
              <td>—</td>
              <td>{{ group.id }}</td>
              <td>
                <div class="pm-actions">
                  <button v-if="canEdit" class="pm-btn is-ghost is-sm" @click="open(undefined, group)">新增子菜单</button>
                  <button v-if="canEdit" class="pm-btn is-ghost is-sm" @click="open(group)">编辑</button>
                </div>
              </td>
            </tr>
            <tr v-for="child in group.children" :key="child.id">
              <td style="padding-left: 32px">{{ child.name }}</td>
              <td><span class="pm-tag is-muted">菜单</span></td>
              <td>{{ child.path }}</td>
              <td>{{ child.component }}</td>
              <td>{{ child.perms || '—' }}</td>
              <td>{{ child.id }}</td>
              <td>
                <div class="pm-actions">
                  <button v-if="canEdit" class="pm-btn is-ghost is-sm" @click="open(child)">编辑</button>
                  <button v-if="canEdit" class="pm-btn is-ghost is-sm is-danger" @click="menuApi.remove(child.id).then(load)">删除</button>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <FormDrawer :open="formOpen" :title="form.id ? '编辑菜单' : '新增菜单'" :loading="saving" @close="formOpen = false" @submit="submit">
      <div class="pm-form-grid">
        <div class="pm-form-item"><label><span class="pm-required">*</span>名称</label><input v-model="form.menuName" class="pm-input" /></div>
        <div class="pm-form-item">
          <label>类型</label>
          <select v-model="form.menuType" class="pm-select"><option :value="1">目录</option><option :value="2">菜单</option><option :value="3">按钮</option></select>
        </div>
        <div class="pm-form-item"><label>路由路径</label><input v-model="form.path" class="pm-input" /></div>
        <div class="pm-form-item"><label>组件路径</label><input v-model="form.component" class="pm-input" /></div>
        <div class="pm-form-item"><label>权限标识</label><input v-model="form.perms" class="pm-input" placeholder="system:user:add" /></div>
        <div class="pm-form-item"><label>图标</label><input v-model="form.icon" class="pm-input" placeholder="tabler 图标名" /></div>
        <div class="pm-form-item"><label>排序</label><input v-model.number="form.sortNo" type="number" class="pm-input" /></div>
      </div>
    </FormDrawer>
  </div>
</template>
