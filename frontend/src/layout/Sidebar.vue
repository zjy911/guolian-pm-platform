<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import type { MenuNode } from '@/api/types'

const userStore = useUserStore()
const appStore = useAppStore()
const route = useRoute()
const router = useRouter()

const expanded = ref<Record<number, boolean>>({})

const menus = computed<MenuNode[]>(() => userStore.menus)

function isGroupActive(menu: MenuNode) {
  return menu.children?.some((child) => child.path === route.path)
}

function toggleGroup(menu: MenuNode) {
  if (appStore.collapsed) {
    appStore.collapsed = false
  }
  expanded.value[menu.id] = !expanded.value[menu.id]
}

function isOpen(menu: MenuNode) {
  return expanded.value[menu.id] ?? isGroupActive(menu)
}

function go(path?: string) {
  if (path) {
    router.push(path)
  }
}
</script>

<template>
  <aside class="pm-sidebar" :class="{ 'is-collapsed': appStore.collapsed }">
    <div
      style="
        height: 56px;
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 0 16px;
        border-bottom: 1px solid rgba(255, 255, 255, 0.12);
      "
    >
      <div class="pm-logo">联</div>
      <span v-if="!appStore.collapsed" style="font-size: 13px; font-weight: 600; color: #fff">项目管理平台</span>
    </div>

    <nav class="pm-nav">
      <div v-for="menu in menus" :key="menu.id">
        <div
          class="pm-nav-group-title"
          :class="{ 'is-active': isGroupActive(menu) }"
          :title="menu.name"
          @click="toggleGroup(menu)"
        >
          <i :class="`ti ti-${menu.icon || 'circle'}`" style="font-size: 18px" />
          <span v-if="!appStore.collapsed" style="flex: 1">{{ menu.name }}</span>
          <i
            v-if="!appStore.collapsed"
            class="ti"
            :class="isOpen(menu) ? 'ti-chevron-up' : 'ti-chevron-down'"
            style="font-size: 13px; opacity: 0.6"
          />
        </div>
        <template v-if="!appStore.collapsed && isOpen(menu)">
          <div
            v-for="child in menu.children"
            :key="child.id"
            class="pm-nav-item"
            :class="{ 'is-active': route.path === child.path }"
            @click="go(child.path)"
          >
            {{ child.name }}
          </div>
        </template>
      </div>
    </nav>

    <div class="pm-sidebar-footer">
      <span v-if="!appStore.collapsed">v1.0.0 · {{ userStore.profile?.orgName }}</span>
      <span v-else>v1.0</span>
    </div>
  </aside>
</template>
