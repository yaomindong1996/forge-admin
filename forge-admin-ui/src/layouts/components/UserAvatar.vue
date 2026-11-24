<template>
  <!-- 桌面顶栏：账户菜单承载低频「切换组织」二级菜单，避免顶栏常驻占位 -->
  <n-dropdown
    trigger="click"
    :options="dropdownOptions"
    @select="handleSelect"
  >
    <button id="user-dropdown" class="user-trigger" type="button" aria-label="个人中心">
      <AccountIdentity />
    </button>
  </n-dropdown>
</template>

<script setup>
import { computed, h } from 'vue'
import AccountIdentity from './AccountIdentity.vue'
import { useAccountActions } from './composables/useAccountActions'
import { useOrgSwitch } from './composables/useOrgSwitch'

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
const { orgSwitchMenuOption, handleOrgMenuSelect } = useOrgSwitch()

const dropdownOptions = computed(() => {
  const items = [
    { label: '个人资料', key: 'profile', icon: () => h('i', { class: 'i-lucide:user-round text-14' }) },
    { label: '关于系统', key: 'about', icon: () => h('i', { class: 'i-lucide:info text-14' }) },
  ]
  if (orgSwitchMenuOption.value) {
    items.push({ type: 'divider', key: 'd-org' })
    items.push(orgSwitchMenuOption.value)
  }
  items.push({ type: 'divider', key: 'd-logout' })
  items.push({ label: '退出登录', key: 'logout', icon: () => h('i', { class: 'i-lucide:log-out text-14' }) })
  return items
})

function handleSelect(key) {
  if (String(key || '').startsWith('org:')) {
    handleOrgMenuSelect(key)
    return
  }
  // 级联父项 key，忽略
  if (key === 'org-switch')
    return
  handleAccountAction(key)
}
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
