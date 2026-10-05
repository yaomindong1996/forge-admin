<template>
  <!-- 桌面顶栏保留个人下拉，复用统一身份展示与账户动作 -->
  <n-dropdown trigger="click" :options="dropdownOptions" @select="handleAccountAction">
    <button id="user-dropdown" class="user-trigger" type="button" aria-label="个人中心">
      <AccountIdentity />
    </button>
  </n-dropdown>
</template>

<script setup>
import { h } from 'vue'
import AccountIdentity from './AccountIdentity.vue'
import { useAccountActions } from './composables/useAccountActions'

const props = defineProps({
  /**
   * 应用门户提供应用内个人资料入口，避免把用户带回系统布局。
   * 未传入时保持系统布局原有的 /profile 路由。
   */
  profileRoute: {
    type: [String, Object, Function],
    default: null,
  },
})

const { handleAccountAction } = useAccountActions(() => props.profileRoute)
const dropdownOptions = [
  { label: '个人资料', key: 'profile', icon: () => h('i', { class: 'i-lucide:user-round text-14' }) },
  { label: '退出登录', key: 'logout', icon: () => h('i', { class: 'i-lucide:log-out text-14' }) },
]
</script>

<style scoped>
.user-trigger {
  border: 0;
  padding: 0 4px;
  min-height: 32px;
  max-width: 180px;
  background: transparent;
  color: var(--chrome-text, var(--text-primary));
  font: inherit;
  cursor: pointer;
}
@media (max-width: 900px) {
  .user-trigger :deep(.account-name) {
    display: none;
  }
}
</style>
