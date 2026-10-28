import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { onUnauthorized } from './api/request'
import { useUserStore } from './stores/user'
import '@tabler/icons-webfont/dist/tabler-icons.min.css'
import './styles/theme-navy.scss'
import './styles/index.scss'

const app = createApp(App)
app.use(createPinia())
app.use(router)

// 401 统一跳登录，避免每个页面重复处理
onUnauthorized(() => {
  const userStore = useUserStore()
  userStore.$reset()
  router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
})

/** 按钮级权限指令：v-perm="['system:user:add']" */
app.directive('perm', {
  mounted(el: HTMLElement, binding) {
    const userStore = useUserStore()
    const required = Array.isArray(binding.value) ? binding.value : [binding.value]
    const allowed = required.some((perm: string) => userStore.hasPerm(perm))
    if (!allowed) {
      el.parentNode?.removeChild(el)
    }
  }
})

app.mount('#app')
