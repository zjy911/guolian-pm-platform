import { defineStore } from 'pinia'

export interface ToastItem {
  id: number
  message: string
  type: 'info' | 'error'
}

export const useAppStore = defineStore('app', {
  state: () => ({
    collapsed: false,
    toasts: [] as ToastItem[],
    noticeOpen: false
  }),
  actions: {
    toggleSidebar() {
      this.collapsed = !this.collapsed
    },
    toast(message: string, type: 'info' | 'error' = 'info') {
      const id = Date.now() + Math.random()
      this.toasts.push({ id, message, type })
      setTimeout(() => {
        this.toasts = this.toasts.filter((item) => item.id !== id)
      }, 2600)
    },
    errorMessage(error: unknown) {
      if (error instanceof Error) {
        return error.message
      }
      return '操作失败，请稍后重试'
    }
  }
})
