import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { tokenStore } from '@/api/request'
import { useUserStore } from '@/stores/user'

const Layout = () => import('@/layout/index.vue')

/** 业务模块占位页：由独立方案建设，底座只预留菜单、路由与权限位 */
const placeholderRoutes: RouteRecordRaw[] = [
  { path: '/crm/customer', title: '客户管理', perm: 'crm:customer:list' },
  { path: '/crm/pool', title: '公共客户池', perm: 'crm:pool:list' },
  { path: '/crm/lead', title: '线索与商机', perm: 'crm:lead:list' },
  { path: '/crm/follow', title: '销售跟进', perm: 'crm:follow:list' },
  { path: '/crm/contract', title: '法务合同', perm: 'crm:contract:list' },
  { path: '/crm/agreement', title: '协议管理', perm: 'crm:agreement:list' },
  { path: '/crm/bid', title: '投标管理', perm: 'crm:bid:list' },
  { path: '/crm/payment', title: '回款管理', perm: 'crm:payment:list' },
  { path: '/crm/churn', title: '流失客户', perm: 'crm:churn:list' },
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
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} · 国联征信项目管理平台` : '国联征信项目管理平台'
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
