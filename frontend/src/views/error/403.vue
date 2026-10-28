<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="pm-placeholder">
    <i class="ti ti-lock-off pm-placeholder-icon" />
    <h2>没有权限访问该页面</h2>
    <p>当前角色未被授予该页面的功能权限。请切换角色，或联系管理员在「角色管理 → 角色授权」中勾选对应菜单。</p>
    <button class="pm-btn is-primary" @click="router.push('/')">返回首页</button>
  </div>
</template>
