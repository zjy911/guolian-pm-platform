import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { tokenStore } from '@/api/request'
import { useUserStore } from '@/stores/user'

const Layout = () => import('@/layout/index.vue')

/** 业务模块占位页：由独立方案建设，底座只预留菜单、路由与权限位 */
const placeholderRoutes: RouteRecordRaw[] = [
  { path: '/crm/payment', title: '回款管理', perm: 'crm:payment:list' },
  { path: '/project/product', title: '产品管理', perm: 'project:product:list' },
  { path: '/project/list', title: '项目列表', perm: 'project:project:list' },
  { path: '/project/board', title: '任务看板', perm: 'project:task:list' },
  { path: '/project/initiation', title: '项目立项', perm: 'project:initiation:list' },
  { path: '/project/review', title: '项目评审', perm: 'project:review:list' },
  { path: '/project/acceptance', title: '项目验收', perm: 'project:acceptance:list' },
  { path: '/project/retro', title: '项目复盘', perm: 'project:retro:list' },
  { path: '/cockpit/overview', title: '经营总览', perm: 'cockpit:overview:view' },
  { path: '/cockpit/sales', title: '销售看板', perm: 'cockpit:sales:view' },
  { path: '/cockpit/region', title: '地区部经营看板', perm: 'cockpit:region:view' },
  { path: '/cockpit/business', title: '商务部看板', perm: 'cockpit:business:view' },
  { path: '/cockpit/hr', title: '人事看板', perm: 'cockpit:hr:view' }
].map((item) => ({
  path: item.path,
  name: `placeholder-${item.path.replace(/\//g, '-')}`,
  component: () => import('@/views/placeholder/index.vue'),
  meta: { title: item.title, perm: item.perm, placeholder: true }
}))

/** CRM 模块：路径与 sys_menu.path 严格一致，否则菜单点击会落到 403 */
const crmRoutes: RouteRecordRaw[] = [
  {
    path: '/crm/customer',
    name: 'crm-account',
    component: () => import('@/views/crm/account/index.vue'),
    meta: { title: '客户管理', perm: 'crm:account:list' }
  },
  {
    path: '/crm/pool',
    name: 'crm-pool',
    component: () => import('@/views/crm/pool/index.vue'),
    meta: { title: '公共客户池', perm: 'crm:account:pool' }
  },
  {
    path: '/crm/claim',
    name: 'crm-claim',
    component: () => import('@/views/crm/pool/claim.vue'),
    meta: { title: '客户认领审批', perm: 'crm:account:claim' }
  },
  {
    path: '/crm/churn',
    name: 'crm-churn',
    component: () => import('@/views/crm/churn/index.vue'),
    meta: { title: '流失客户', perm: 'crm:account:list' }
  },
  {
    path: '/crm/contact',
    name: 'crm-contact',
    component: () => import('@/views/crm/contact/index.vue'),
    meta: { title: '联系人管理', perm: 'crm:contact:list' }
  },
  {
    path: '/crm/lead',
    name: 'crm-lead',
    component: () => import('@/views/crm/lead/index.vue'),
    meta: { title: '线索管理', perm: 'crm:lead:list' }
  },
  {
    path: '/crm/lead-convert',
    name: 'crm-lead-convert',
    component: () => import('@/views/crm/lead/convert.vue'),
    meta: { title: '线索转化工作台', perm: 'crm:lead:convert' }
  },
  {
    path: '/crm/opportunity',
    name: 'crm-opportunity',
    component: () => import('@/views/crm/opportunity/index.vue'),
    meta: { title: '商机管理', perm: 'crm:opportunity:list' }
  },
  {
    path: '/crm/opportunity-funnel',
    name: 'crm-opportunity-funnel',
    component: () => import('@/views/crm/opportunity/funnel.vue'),
    meta: { title: '商机漏斗', perm: 'crm:opportunity:list' }
  },
  {
    path: '/crm/follow',
    name: 'crm-activity',
    component: () => import('@/views/crm/activity/index.vue'),
    meta: { title: '销售活动', perm: 'crm:activity:list' }
  },
  {
    path: '/crm/activity-timeline',
    name: 'crm-activity-timeline',
    component: () => import('@/views/crm/activity/timeline.vue'),
    meta: { title: '活动时间线', perm: 'crm:activity:list' }
  },
  {
    path: '/crm/contract',
    name: 'crm-contract',
    component: () => import('@/views/crm/contract/index.vue'),
    meta: { title: '法务合同', perm: 'crm:contract:list' }
  },
  {
    path: '/crm/contract-expire',
    name: 'crm-contract-expire',
    component: () => import('@/views/crm/contract/expire.vue'),
    meta: { title: '合同到期预警', perm: 'crm:contract:list' }
  },
  {
    path: '/crm/agreement',
    name: 'crm-agreement',
    component: () => import('@/views/crm/agreement/index.vue'),
    meta: { title: '协议管理', perm: 'crm:agreement:list' }
  },
  {
    path: '/crm/bid',
    name: 'crm-bid',
    component: () => import('@/views/crm/bid/index.vue'),
    meta: { title: '投标管理', perm: 'crm:bid:list' }
  },
  {
    path: '/crm/bid-calendar',
    name: 'crm-bid-calendar',
    component: () => import('@/views/crm/bid/calendar.vue'),
    meta: { title: '投标日历', perm: 'crm:bid:list' }
  }
]

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', name: 'home', redirect: '/system/user' },
      ...crmRoutes,
      ...placeholderRoutes,
      {
        path: '/system/user',
        name: 'system-user',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', perm: 'system:user:list' }
      },
      {
        path: '/system/transfer',
        name: 'system-transfer',
        component: () => import('@/views/system/transfer/index.vue'),
        meta: { title: '账号交接', perm: 'system:transfer:list' }
      },
      {
        path: '/system/org',
        name: 'system-org',
        component: () => import('@/views/system/org/index.vue'),
        meta: { title: '组织架构', perm: 'system:org:list' }
      },
      {
        path: '/system/company',
        name: 'system-company',
        component: () => import('@/views/system/company/index.vue'),
        meta: { title: '公司主体', perm: 'system:company:list' }
      },
      {
        path: '/system/role',
        name: 'system-role',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', perm: 'system:role:list' }
      },
      {
        path: '/system/menu',
        name: 'system-menu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', perm: 'system:menu:list' }
      },
      {
        path: '/system/flow',
        name: 'system-flow',
        component: () => import('@/views/system/flow/index.vue'),
        meta: { title: '审批流程', perm: 'system:flow:list' }
      },
      {
        path: '/system/data-scope',
        name: 'system-data-scope',
        component: () => import('@/views/system/data-scope/index.vue'),
        meta: { title: '数据权限', perm: 'system:role:data-scope' }
      },
      {
        path: '/system/sensitive',
        name: 'system-sensitive',
        component: () => import('@/views/system/sensitive/index.vue'),
        meta: { title: '字段脱敏', perm: 'system:sensitive:list' }
      },
      {
        path: '/system/dict',
        name: 'system-dict',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '字典管理', perm: 'system:dict:list' }
      },
      {
        path: '/system/config',
        name: 'system-config',
        component: () => import('@/views/system/config/index.vue'),
        meta: { title: '参数配置', perm: 'system:config:list' }
      },
      {
        path: '/system/notice',
        name: 'system-notice',
        component: () => import('@/views/system/notice/index.vue'),
        meta: { title: '通知公告', perm: 'system:notice:list' }
      },
      {
        path: '/system/oper-log',
        name: 'system-oper-log',
        component: () => import('@/views/system/log/oper/index.vue'),
        meta: { title: '操作日志', perm: 'system:log:oper' }
      },
      {
        path: '/system/login-log',
        name: 'system-login-log',
        component: () => import('@/views/system/log/login/index.vue'),
        meta: { title: '登录日志', perm: 'system:log:login' }
      },
      {
        path: '/system/online',
        name: 'system-online',
        component: () => import('@/views/system/online/index.vue'),
        meta: { title: '在线用户', perm: 'system:online:list' }
      },
      {
        path: '/system/file',
        name: 'system-file',
        component: () => import('@/views/system/file/index.vue'),
        meta: { title: '附件管理', perm: 'system:file:list' }
      },
      {
        path: '/flow/center',
        name: 'flow-center',
        component: () => import('@/views/flow/center/index.vue'),
        meta: { title: '审批中心' }
      },
      {
        path: '/profile',
        name: 'profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: '/403',
        name: 'forbidden',
        component: () => import('@/views/error/403.vue'),
        meta: { title: '无权限' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/403' }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} · 国联政信项目管理平台` : '国联政信项目管理平台'
  if (to.meta.public) {
    return true
  }
  if (!tokenStore.get()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  const userStore = useUserStore()
  if (!userStore.profile) {
    try {
      await userStore.loadUserInfo()
    } catch {
      return { path: '/login' }
    }
  }
  const perm = to.meta.perm as string | undefined
  if (perm && !userStore.hasPerm(perm)) {
    return { path: '/403' }
  }
  return true
})

export default router
