<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { authApi } from '@/api'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const tab = ref<'info' | 'password' | 'roles' | 'notify'>('info')
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const saving = ref(false)

async function changePassword() {
  if (passwordForm.newPassword !== passwordForm.confirm) {
    appStore.toast('两次输入的新密码不一致', 'error')
    return
  }
  saving.value = true
  try {
    await authApi.changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    appStore.toast('密码已修改，请牢记新密码')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirm = ''
  } catch (error) {
    appStore.toast(appStore.errorMessage(error), 'error')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (!userStore.profile) {
    await userStore.loadUserInfo()
  }
})
</script>

<template>
  <div>
    <PageHeader title="个人中心" />
    <div class="pm-card">
      <div class="pm-tabs">
        <div class="pm-tab" :class="{ 'is-active': tab === 'info' }" @click="tab = 'info'">基本资料</div>
        <div class="pm-tab" :class="{ 'is-active': tab === 'password' }" @click="tab = 'password'">修改密码</div>
        <div class="pm-tab" :class="{ 'is-active': tab === 'roles' }" @click="tab = 'roles'">我的角色与组织</div>
        <div class="pm-tab" :class="{ 'is-active': tab === 'notify' }" @click="tab = 'notify'">通知设置</div>
      </div>

      <div v-if="tab === 'info'" style="display: flex; gap: 20px; align-items: flex-start">
        <div class="pm-avatar" style="width: 64px; height: 64px; font-size: 22px">
          {{ userStore.profile?.realName?.slice(-1) }}
        </div>
        <div style="display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px 32px; font-size: 13px">
          <div>姓名：{{ userStore.profile?.realName }}</div>
          <div>登录账号：{{ userStore.profile?.username }}</div>
          <div>当前组织：{{ userStore.profile?.orgName }}</div>
          <div>当前角色：{{ userStore.profile?.roleName }}</div>
          <div>所属组织路径：{{ userStore.profile?.orgPath }}</div>
          <div>公司主体：{{ userStore.profile?.companyNames || '—' }}</div>
          <div>数据范围：{{ userStore.profile?.dataScopeLabel }}</div>
          <div>账号状态：{{ userStore.profile?.readOnly ? '只读（离职）' : '正常' }}</div>
        </div>
      </div>

      <form v-else-if="tab === 'password'" style="max-width: 360px; display: flex; flex-direction: column; gap: 12px" @submit.prevent="changePassword">
        <div class="pm-field"><label>原密码</label><input v-model="passwordForm.oldPassword" type="password" class="pm-input" /></div>
        <div class="pm-field"><label>新密码（8~32 位）</label><input v-model="passwordForm.newPassword" type="password" class="pm-input" /></div>
        <div class="pm-field"><label>确认新密码</label><input v-model="passwordForm.confirm" type="password" class="pm-input" /></div>
        <button class="pm-btn is-primary" type="submit" :disabled="saving">{{ saving ? '提交中…' : '保存' }}</button>
      </form>

      <div v-else-if="tab === 'roles'">
        <table class="pm-table">
          <thead><tr><th>组织</th><th>角色</th><th>数据范围</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="role in userStore.roles" :key="role.roleId">
              <td>{{ role.orgName }}</td>
              <td>{{ role.roleName }}<span v-if="role.current" class="pm-tag is-solid" style="margin-left: 6px">当前</span></td>
              <td>{{ role.dataScopeLabel }}</td>
              <td>{{ role.current ? '生效中' : '可切换' }}</td>
            </tr>
          </tbody>
        </table>
        <p style="font-size: 12px; color: var(--pm-neutral-500); margin-top: 10px">
          切换角色请在右上角账号区操作，切换后组织与权限同步变化。
        </p>
      </div>

      <div v-else style="font-size: 13px; color: var(--pm-neutral-500)">
        通知设置：站内通知默认开启，审批待办与公告通过 Topbar 铃铛聚合展示。邮件与短信通道由后续版本接入统一消息中心。
      </div>
    </div>
  </div>
</template>
