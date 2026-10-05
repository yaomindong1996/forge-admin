<template>
  <div class="header-tools" aria-label="系统工具">
    <!-- 常用动作保持直显；身份入口沿用现有接口与切换流程 -->
    <MenuSearch />
    <ToggleTheme v-if="!isCompact" />
    <MessageNotification />
    <!-- 极窄窗口只保留核心入口，账户工具不丢失、不重复挂载切换器 -->
    <CompactLayoutTools v-if="isCompact" placement="bottom-end" />
    <template v-else>
      <TenantSwitcher />
      <OrgSwitcher />
      <button
        v-if="showAppearance" id="layout-setting" class="chrome-icon-button" type="button"
        title="布局与外观" aria-label="布局与外观" @click="appStore.appearanceOpen = true"
      >
        <i class="i-lucide:panels-top-left" />
      </button>
      <!-- 移除指引后仅剩全屏，不再用额外浮层承载单一操作。 -->
      <Fullscreen />
      <UserAvatar />
    </template>
  </div>
</template>

<script setup>
import { useMediaQuery } from '@vueuse/core'
import ToggleTheme from '@/components/common/ToggleTheme.vue'
import { useAppStore } from '@/store'
import CompactLayoutTools from './CompactLayoutTools.vue'
import Fullscreen from './Fullscreen.vue'
import MenuSearch from './MenuSearch.vue'
import MessageNotification from './MessageNotification.vue'
import OrgSwitcher from './OrgSwitcher.vue'
import TenantSwitcher from './TenantSwitcher.vue'
import UserAvatar from './UserAvatar.vue'

defineProps({ showAppearance: Boolean })
const appStore = useAppStore()
const isCompact = useMediaQuery('(max-width: 480px)')
</script>

<style scoped>
.header-tools {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  color: var(--chrome-text, var(--text-primary));
}
.header-tools :deep(.tenant-switcher),
.header-tools :deep(.org-switcher) {
  margin: 0;
  padding-inline: 6px;
  border-radius: 4px;
}
.header-tools :deep(.tenant-name),
.header-tools :deep(.org-name) {
  max-width: 88px;
}
@media (max-width: 900px) {
  .header-tools :deep(.tenant-name),
  .header-tools :deep(.org-name),
  .header-tools :deep(#user-dropdown .user-name) {
    display: none;
  }
}
</style>
