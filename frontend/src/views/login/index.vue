<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const form = reactive({ username: 'admin', password: 'Admin@123' })
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (!form.username || !form.password) {
    error.value = '请输入账号与密码'
    return
  }
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    appStore.toast('登录成功')
    const redirect = (route.query.redirect as string) || '/system/user'
    const target = userStore.hasPerm('system:user:list')
      ? redirect
      : userStore.menus[0]?.children?.[0]?.path || '/403'
    await router.replace(target)
  } catch (err) {
    error.value = appStore.errorMessage(err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="pm-login-page">
    <section class="pm-login-brand">
      <div class="pm-logo" style="background: #fff">联</div>
      <h1>国联政信<br />项目管理平台</h1>
      <p>底座框架 + 系统管理 + 通用审批能力。CRM、项目管理、驾驶舱由独立方案接入，底座已预留菜单、路由与权限位。</p>
      <ul>
        <li>· 角色即身份：切角色同时切组织、菜单、数据范围与脱敏策略</li>
        <li>· 行级数据权限 + 列级字段脱敏双重屏障</li>
        <li>· Flowable 7.1.0 通用审批，账号交接为正式跑通场景</li>
      </ul>
    </section>

    <section class="pm-login-form">
      <form @submit.prevent="submit">
        <div>
          <h1 style="font-size: 24px; margin: 0 0 6px">登录</h1>
          <p style="font-size: 13px; color: var(--pm-neutral-500); margin: 0">请使用企业账号登录，连续失败 5 次将锁定 10 分钟</p>
        </div>

        <div class="pm-field">
          <label>登录账号</label>
          <input v-model="form.username" class="pm-input" autocomplete="username" placeholder="请输入登录账号" />
        </div>

        <div class="pm-field">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            class="pm-input"
            autocomplete="current-password"
            placeholder="请输入密码"
          />
        </div>

        <div v-if="error" style="color: var(--pm-danger); font-size: 12px">{{ error }}</div>

        <button class="pm-btn is-primary" type="submit" :disabled="loading" style="justify-content: center">
          {{ loading ? '登录中…' : '登录' }}
        </button>

        <div style="font-size: 12px; color: var(--pm-neutral-500); line-height: 20px">
          演示账号（口令均为 Admin@123）：<br />
          admin（超管）、zhangwei（5 个角色 / 跨 3 组织）、lina（项目经理）、wangqiang（离职只读）
        </div>
      </form>
    </section>
  </div>
</template>
