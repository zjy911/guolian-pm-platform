import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import type { MenuNode } from '@/api/types'

/** 顶部模块切换项：CRM 与 项目管理 */
export type NavModuleKey = 'crm' | 'pm'

export interface NavModule {
  key: NavModuleKey
  /** 顶部切换器上显示的文字 */
  shortName: string
  /** 无障碍标签 / tooltip */
  name: string
  icon: string
  /** 该模块在左侧竖栏中展示的一级分组 */
  groups: MenuNode[]
}

/** 两个模块共用的分组：首页、个人工作台（审批相关） */
function isSharedGroup(menu: MenuNode): boolean {
  const path = menu.path ?? ''
  return path === '/home' || path.startsWith('/flow')
}

/** 分组归属 CRM：自身或其子级路径以 /crm 开头 */
function isCrmGroup(menu: MenuNode): boolean {
  const paths = [menu.path ?? '', ...(menu.children ?? []).map((child) => child.path ?? '')]
  return paths.some((path) => path.startsWith('/crm'))
}

/** 共用分组排在前，其余保持接口返回顺序 */
function orderGroups(list: MenuNode[]): MenuNode[] {
  return [...list.filter(isSharedGroup), ...list.filter((menu) => !isSharedGroup(menu))]
}

/** 取分组树里第一个可跳转的叶子路径（分组本身是叶子时用自身路径） */
function firstRouteOf(groups: MenuNode[]): string | undefined {
  for (const group of groups) {
    if (group.children?.length) {
      const leaf = group.children.find((item) => item.path)
      if (leaf?.path) return leaf.path
    } else if (group.path) {
      return group.path
    }
  }
  return undefined
}

/**
 * 一级模块导航：把后端返回的菜单树按「CRM / 项目管理」两个模块切分。
 * 当前模块由路由前缀推导（/crm/* 属于 CRM，其余属于项目管理），
 * 因此刷新页面、直接打开深链接都能落到正确的模块；
 * 首页与个人工作台属于公共分组，两个模块下都会出现。
 */
export function useNavModules() {
  const userStore = useUserStore()
  const route = useRoute()

  const modules = computed<NavModule[]>(() => {
    const all = userStore.menus ?? []
    const crmGroups = orderGroups(all.filter((menu) => isSharedGroup(menu) || isCrmGroup(menu)))
    const pmGroups = orderGroups(all.filter((menu) => isSharedGroup(menu) || !isCrmGroup(menu)))

    const list: NavModule[] = [
      { key: 'pm', shortName: '项目管理', name: '项目管理', icon: 'briefcase', groups: pmGroups }
    ]
    if (all.some((menu) => isCrmGroup(menu))) {
      list.unshift({ key: 'crm', shortName: 'CRM', name: 'CRM 客户关系管理', icon: 'users', groups: crmGroups })
    }
    return list
  })

  const activeKey = computed<NavModuleKey>(() => (route.path.startsWith('/crm') ? 'crm' : 'pm'))

  const activeModule = computed<NavModule | undefined>(
    () => modules.value.find((item) => item.key === activeKey.value) ?? modules.value[0]
  )

  /** 当前模块在竖栏中应展示的分组 */
  const groups = computed<MenuNode[]>(() => activeModule.value?.groups ?? [])

  /** 切换模块时的默认落地页 */
  function defaultPathOf(key: NavModuleKey): string | undefined {
    const target = modules.value.find((item) => item.key === key)
    return target ? firstRouteOf(target.groups) : undefined
  }

  return { modules, activeKey, activeModule, groups, defaultPathOf }
}
