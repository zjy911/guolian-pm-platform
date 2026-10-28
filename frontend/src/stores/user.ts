import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { tokenStore } from '@/api/request'
import type { AuthResult, MenuNode, Profile, RoleOption } from '@/api/types'

interface UserState {
  profile: Profile | null
  menus: MenuNode[]
  roles: RoleOption[]
  todoCount: number
  unreadCount: number
  impersonate: boolean
  impersonateName: string
  /** 模拟前的原始令牌，退出模拟时恢复 */
  originalToken: string | null
  originalProfile: Profile | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    profile: null,
    menus: [],
    roles: [],
    todoCount: 0,
    unreadCount: 0,
    impersonate: false,
    impersonateName: '',
    originalToken: null,
    originalProfile: null
  }),
  getters: {
    isLogin: (state) => Boolean(tokenStore.get() && state.profile),
    perms: (state) => state.profile?.perms ?? [],
    orgRoleLabel: (state) =>
      state.profile ? `${state.profile.orgName} · ${state.profile.roleName}` : ''
  },
  actions: {
    applyResult(result: AuthResult) {
      tokenStore.set(result.token)
      this.profile = result.profile
      this.menus = result.menus
      this.roles = result.roles
      this.todoCount = result.todoCount
      this.unreadCount = result.unreadCount
      this.impersonate = result.profile.impersonate
      this.impersonateName = result.profile.impersonateBy ?? ''
    },
    async login(username: string, password: string) {
      const result = await authApi.login(username, password)
      this.applyResult(result)
      return result
    },
    async loadUserInfo() {
      const result = await authApi.userInfo()
      this.applyResult(result)
      return result
    },
    async switchRole(roleId: number) {
      const result = await authApi.switchRole(roleId)
      this.applyResult(result)
      return result
    },
    async impersonateUser(userId: number) {
      const currentToken = tokenStore.get()
      const result = await authApi.impersonate(userId)
      this.originalToken = currentToken
      this.originalProfile = this.profile
      this.applyResult(result)
      return result
    },
    async exitImpersonate() {
      if (this.originalToken) {
        tokenStore.set(this.originalToken)
      }
      this.originalToken = null
      this.originalProfile = null
      await this.loadUserInfo()
    },
    async refreshBadge() {
      // 角标由 user-info 返回，切换角色后已同步；此处保留扩展点
    },
    hasPerm(perm: string) {
      return this.perms.includes(perm) || this.perms.includes('*:*:*')
    },
    async logout() {
      try {
        await authApi.logout()
      } catch {
        // 登出失败不阻断前端清理
      }
      tokenStore.clear()
      this.$reset()
    }
  }
})
