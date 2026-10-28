<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { authApi, roleApi } from '@/api'
import type { RoleVO } from '@/api/types'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const roles = ref<RoleVO[]>([])
const scope = ref<Record<string, unknown> | null>(null)

const currentRole = computed(() => roles.value.find((role) => role.id === userStore.profile?.roleId))

async function load() {
  try {
    roles.value = await roleApi.options()
    scope.value = await authApi.dataScopeSql()
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="数据权限" desc="行级范围只按当前角色计算，不叠加" />

    <div class="pm-card">
      <h3 style="font-size: 15px; margin: 0 0 10px">当前角色范围诊断</h3>
      <div style="display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px">
        <div class="pm-card" style="margin: 0; box-shadow: none; border: 1px solid var(--pm-neutral-100)">
          <div style="font-size: 12px; color: var(--pm-neutral-500)">当前角色</div>
          <div style="font-weight: 600; margin-top: 4px">{{ userStore.profile?.roleName }}</div>
          <div style="font-size: 12px; color: var(--pm-neutral-500)">{{ userStore.profile?.orgName }}</div>
        </div>
        <div class="pm-card" style="margin: 0; box-shadow: none; border: 1px solid var(--pm-neutral-100)">
          <div style="font-size: 12px; color: var(--pm-neutral-500)">数据范围</div>
          <div style="font-weight: 600; margin-top: 4px">{{ userStore.profile?.dataScopeLabel }}</div>
          <div style="font-size: 12px; color: var(--pm-neutral-500)">{{ userStore.profile?.dataScope }}</div>
        </div>
        <div class="pm-card" style="margin: 0; box-shadow: none; border: 1px solid var(--pm-neutral-100)">
          <div style="font-size: 12px; color: var(--pm-neutral-500)">可见用户数</div>
          <div style="font-weight: 600; margin-top: 4px">{{ scope?.visibleUserCount ?? '—' }}</div>
          <div style="font-size: 12px; color: var(--pm-neutral-500)">受当前角色范围约束</div>
        </div>
      </div>

      <h3 style="font-size: 14px; margin: 18px 0 8px">最终 SQL 条件</h3>
      <pre
        style="
          background: var(--pm-neutral-50);
          padding: 12px;
          border-radius: 8px;
          font-size: 12px;
          overflow: auto;
          border: 1px solid var(--pm-neutral-100);
        "
        >{{ scope?.condition }}</pre
      >
      <p style="font-size: 12px; color: var(--pm-neutral-500)">
        组织基准直接取 sys_role.org_id；超管（ALL）绕过但日志标记 dataScope=bypass；列表、详情、导出、统计总数共用同一套逻辑。
      </p>
    </div>

    <div class="pm-card">
      <h3 style="font-size: 15px; margin: 0 0 10px">各角色数据范围</h3>
      <table class="pm-table">
        <thead><tr><th>角色</th><th>所属组织</th><th>数据范围</th><th>人数</th></tr></thead>
        <tbody>
          <tr v-for="role in roles" :key="role.id">
            <td>{{ role.roleName }}<span v-if="role.id === currentRole?.id" class="pm-tag is-solid" style="margin-left: 6px">当前</span></td>
            <td>{{ role.orgName }}</td>
            <td>{{ role.dataScopeLabel }}</td>
            <td>{{ role.userCount }}</td>
          </tr>
        </tbody>
      </table>
      <p style="font-size: 12px; color: var(--pm-neutral-500); margin-top: 10px">
        修改角色数据范围请前往「角色管理 → 授权 → 数据权限」，保存后立即刷新权限缓存。
      </p>
    </div>
  </div>
</template>
