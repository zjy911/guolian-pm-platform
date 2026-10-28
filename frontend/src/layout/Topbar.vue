<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'
import { DEMO as isDemo } from '@/api/demo'
import NoticeDrawer from './NoticeDrawer.vue'

const appStore = useAppStore()
const userStore = useUserStore()
const router = useRouter()
const panelOpen = ref(false)
const keyword = ref('')

const initial = computed(() => {
  const name = userStore.profile?.realName ?? ''
  return name ? name.slice(-1) : '联'
})

const todoCount = computed(() => userStore.todoCount)
const unreadCount = computed(() => userStore.unreadCount)

function toggle() {
  appStore.toggleSidebar()
}

async function switchRole(roleId: number) {
  if (roleId === userStore.profile?.roleId) {
    panelOpen.value = false
    return
  }
  try {
    await userStore.switchRole(roleId)
    panelOpen.value = false
    appStore.toast('角色已切换，组织与权限同步更新')
    await router.replace('/system/user')
    if (!userStore.hasPerm('system:user:list')) {
      const first = userStore.menus[0]?.children?.[0]
      if (first?.path) {
        await router.replace(first.path)
        appStore.toast('当前页面在新角色下无权限，已跳转到默认首页')
      }
    }
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

async function logout() {
  await userStore.logout()
  await router.push('/login')
}

async function exitImpersonate() {
  await userStore.exitImpersonate()
  appStore.toast('已退出模拟查看')
  await router.replace('/system/user')
}

async function globalSearch() {
  if (!keyword.value.trim()) {
    return
  }
  await router.push({ path: '/system/user', query: { keyword: keyword.value.trim() } })
  appStore.toast(`已按关键字「${keyword.value}」筛选用户`)
  panelOpen.value = false
}

function openTodo() {
  router.push('/flow/center')
}

function togglePanel() {
  panelOpen.value = !panelOpen.value
}

async function showScopeDebug() {
  try {
    const data = await authApi.dataScopeSql()
    appStore.toast(`当前数据范围：${String(data.condition)}`)
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}
</script>

<template>
  <header class="pm-topbar">
    <div style="display: flex; align-items: center; gap: 12px">
      <button class="pm-btn is-ghost is-sm" title="折叠/展开" @click="toggle">
        <i class="ti ti-layout-sidebar-left-expand" style="font-size: 18px" />
      </button>
      <div style="display: flex; align-items: center; gap: 8px">
        <div class="pm-logo">联</div>
        <span style="font-size: 13.5px; font-weight: 600">国联征信项目管理平台</span>
        <span
          v-if="isDemo"
          title="静态演示模式：数据为内置模拟数据，写操作不会持久化"
          style="font-size: 11px; padding: 2px 8px; border-radius: 999px; background: var(--pm-warning-bg, #fff7e6); color: #ad6800; border: 1px solid #ffd591; font-weight: 500"
        >演示模式</span>
      </div>
    </div>

    <div style="display: flex; align-items: center; gap: 14px">
      <div style="position: relative">
        <input
          v-model="keyword"
          class="pm-input"
          style="width: 220px; height: 32px; padding-left: 30px"
          placeholder="全局搜索 (Ctrl+K)"
          @keyup.enter="globalSearch"
        />
        <i
          class="ti ti-search"
          style="position: absolute; left: 10px; top: 8px; font-size: 15px; color: var(--pm-neutral-400)"
        />
      </div>

      <button class="pm-btn is-ghost is-sm" title="我的待办" @click="openTodo">
        <i class="ti ti-list-check" style="font-size: 18px" />
        <span v-if="todoCount > 0" class="pm-badge">{{ todoCount }}</span>
      </button>

      <button class="pm-btn is-ghost is-sm" title="通知公告" @click="appStore.noticeOpen = true">
        <i class="ti ti-bell" style="font-size: 18px" />
        <span v-if="unreadCount > 0" class="pm-badge">{{ unreadCount }}</span>
      </button>

      <button class="pm-btn is-ghost is-sm" title="帮助" @click="showScopeDebug">
        <i class="ti ti-help-circle" style="font-size: 18px" />
      </button>

      <div class="pm-account" @click="togglePanel">
        <div class="pm-avatar">{{ initial }}</div>
        <div class="pm-account-meta">
          <span class="pm-account-name">{{ userStore.profile?.realName }}</span>
          <span class="pm-account-sub" :title="userStore.orgRoleLabel">{{ userStore.orgRoleLabel }}</span>
        </div>
        <i class="ti ti-chevron-down" style="font-size: 14px; color: var(--pm-neutral-400)" />
      </div>
    </div>

    <div v-if="panelOpen" class="pm-dropdown">
      <div class="pm-dropdown-section">
        <div class="pm-dropdown-title">当前身份</div>
        <div style="display: flex; gap: 10px; align-items: center; margin-bottom: 8px">
          <div class="pm-avatar">{{ initial }}</div>
          <div style="line-height: 1.4">
            <div style="font-weight: 600">{{ userStore.profile?.realName }}</div>
            <div style="font-size: 12px; color: var(--pm-neutral-500)">
              {{ userStore.profile?.username }} · {{ userStore.profile?.companyNames || '未关联主体' }}
            </div>
          </div>
        </div>
        <div style="font-size: 12px; color: var(--pm-neutral-500)">
          组织：{{ userStore.profile?.orgName }}｜角色：{{ userStore.profile?.roleName }}
        </div>
        <div style="font-size: 12px; color: var(--pm-neutral-500); margin-top: 2px">
          数据范围：{{ userStore.profile?.dataScopeLabel }}
        </div>
      </div>

      <div class="pm-dropdown-section">
        <div class="pm-dropdown-title">切换角色（同时切换组织）</div>
        <div
          v-for="role in userStore.roles.slice(0, 5)"
          :key="role.roleId"
          class="pm-dropdown-item"
          :class="{ 'is-active': role.current }"
          @click="switchRole(role.roleId)"
        >
          <div style="display: flex; flex-direction: column">
            <span>{{ role.orgName }} / {{ role.roleName }}</span>
            <span style="font-size: 11px; color: var(--pm-neutral-500)">{{ role.dataScopeLabel }}</span>
          </div>
          <i v-if="role.current" class="ti ti-check" style="font-size: 15px" />
        </div>
      </div>

      <div class="pm-dropdown-section">
        <div class="pm-dropdown-title">常用操作</div>
        <div class="pm-dropdown-item" @click="router.push('/profile'); panelOpen = false">
          <span>个人中心</span><i class="ti ti-user" />
        </div>
        <div class="pm-dropdown-item" @click="router.push('/profile?tab=password'); panelOpen = false">
          <span>修改密码</span><i class="ti ti-lock" />
        </div>
        <div class="pm-dropdown-item" @click="router.push('/profile?tab=notify'); panelOpen = false">
          <span>通知设置</span><i class="ti ti-bell" />
        </div>
      </div>

      <div class="pm-dropdown-section">
        <div class="pm-dropdown-item" style="color: var(--pm-danger)" @click="logout">
          <span>退出登录</span><i class="ti ti-logout" />
        </div>
      </div>
    </div>

    <NoticeDrawer v-if="appStore.noticeOpen" @close="appStore.noticeOpen = false" />

    <div v-if="userStore.impersonate" class="pm-impersonate-bar" style="position: fixed; top: 0; left: 0; right: 0; z-index: 1100">
      正在以 {{ userStore.profile?.realName }} 的视角查看，此模式下不可执行写操作
      <button class="pm-btn is-sm" @click="exitImpersonate">退出模拟</button>
    </div>
  </header>
</template>
