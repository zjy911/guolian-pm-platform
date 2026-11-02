<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import { flowApi } from '@/api'
import { projectApi } from '@/api/pm'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const router = useRouter()

const loading = ref(false)
const todoCount = ref(0)
const mineCount = ref(0)
const ccCount = ref(0)
const projectCount = ref(0)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const stats = computed(() => [
  { label: '待我处理', value: todoCount.value, icon: 'inbox', path: '/flow/mine' },
  { label: '我发起的', value: mineCount.value, icon: 'send', path: '/flow/mine' },
  { label: '抄送我的', value: ccCount.value, icon: 'mail-forward', path: '/flow/cc' },
  { label: '在办项目', value: projectCount.value, icon: 'briefcase', path: '/project/list' }
])

const shortcuts = computed(() =>
  [
    { label: '产品管理', desc: '服务产品与模板', path: '/project/product', perm: 'project:product:list', icon: 'box' },
    { label: '项目管理', desc: '六环节全流程', path: '/project/list', perm: 'project:project:list', icon: 'list-check' },
    { label: '客户管理', desc: '客户与联系人', path: '/crm/customer', perm: 'crm:account:list', icon: 'address-book' },
    { label: '用户管理', desc: '人员与角色', path: '/system/user', perm: 'system:user:list', icon: 'users' }
  ].filter((item) => userStore.hasPerm(item.perm))
)

async function load() {
  loading.value = true
  try {
    const [todo, mine, cc, projects] = await Promise.all([
      flowApi.todo().catch(() => []),
      flowApi.mine().catch(() => []),
      flowApi.cc().catch(() => []),
      projectApi.page({ pageNum: 1, pageSize: 1 }).catch(() => ({ total: 0 }))
    ])
    todoCount.value = todo.length
    mineCount.value = mine.length
    ccCount.value = cc.length
    projectCount.value = (projects as { total?: number }).total ?? 0
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="首页" desc="项目全生命周期的统一入口" />

    <div class="pm-card" style="display: flex; align-items: center; justify-content: space-between; gap: 16px">
      <div>
        <h2 style="margin: 0 0 4px; font-size: 18px">
          {{ greeting }}，{{ userStore.profile?.realName || '同事' }}
        </h2>
        <p style="margin: 0; font-size: 13px; color: var(--pm-neutral-500)">
          {{ userStore.orgRoleLabel }}｜今日待办 {{ todoCount }} 项，建议先处理审批与阻塞任务
        </p>
      </div>
      <button class="pm-btn is-primary" :disabled="loading" @click="load">
        <i class="ti ti-refresh" style="font-size: 16px" />{{ loading ? '刷新中…' : '刷新数据' }}
      </button>
    </div>

    <div class="pm-home-stats">
      <div v-for="item in stats" :key="item.label" class="pm-home-stat" @click="router.push(item.path)">
        <i :class="`ti ti-${item.icon}`" class="pm-home-stat-icon" />
        <div>
          <div class="pm-home-stat-value">{{ item.value }}</div>
          <div class="pm-home-stat-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <div class="pm-card">
      <h3 style="margin: 0 0 12px; font-size: 14px">快捷入口</h3>
      <div class="pm-home-shortcuts">
        <div v-for="item in shortcuts" :key="item.path" class="pm-home-shortcut" @click="router.push(item.path)">
          <i :class="`ti ti-${item.icon}`" style="font-size: 18px; color: var(--pm-primary-700)" />
          <div>
            <div style="font-size: 13px; font-weight: 600">{{ item.label }}</div>
            <div style="font-size: 12px; color: var(--pm-neutral-500)">{{ item.desc }}</div>
          </div>
        </div>
        <div v-if="!shortcuts.length" style="font-size: 13px; color: var(--pm-neutral-500)">
          当前角色暂无可用的快捷入口
        </div>
      </div>
    </div>

    <div class="pm-card">
      <h3 style="margin: 0 0 8px; font-size: 14px">使用说明</h3>
      <ul style="margin: 0; padding-left: 18px; font-size: 13px; line-height: 24px; color: var(--pm-neutral-700)">
        <li>顶部可在「CRM」与「项目管理」两个模块间切换，左侧菜单随模块变化</li>
        <li>项目管理按「立项 → 筹备 → 实施 → 交接 → 运营 → 撤场」六环节推进，阻塞任务未完成时不允许进入下一环节</li>
        <li>审批统一在「个人工作台」中处理，支持审批、驳回与转办</li>
      </ul>
    </div>
  </div>
</template>
