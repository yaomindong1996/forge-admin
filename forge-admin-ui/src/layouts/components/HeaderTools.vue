<template>
  <div class="header-tools" aria-label="系统工具">
    <!-- 常用动作保持直显；身份入口沿用现有接口与切换流程 -->
    <MenuSearch />
    <ToggleTheme v-if="!isCompact" />
    <MessageNotification />
    <!-- 极窄窗口只保留核心入口，账户工具不丢失、不重复挂载切换器 -->
    <CompactLayoutTools v-if="isCompact" placement="bottom-end" show-guide />
    <template v-else>
      <TenantSwitcher />
      <OrgSwitcher />
      <button
        v-if="showAppearance" id="layout-setting" class="chrome-icon-button" type="button"
        title="布局与外观" aria-label="布局与外观" @click="appStore.appearanceOpen = true"
      >
        <i class="i-lucide:panels-top-left" />
      </button>
      <!-- 低频工具集中收纳，弹层使用正文配色，不继承深色顶栏文字 -->
      <n-popover trigger="click" placement="bottom-end" :show-arrow="false">
        <template #trigger>
          <button class="chrome-icon-button" type="button" title="更多工具" aria-label="更多工具">
            <i class="i-lucide:ellipsis" />
          </button>
        </template>
        <div class="header-more-tools">
          <Fullscreen with-label />
          <BeginnerGuide with-label />
        </div>
      </n-popover>
      <UserAvatar />
    </template>
  </div>
</template>

<script setup>
import { useMediaQuery } from '@vueuse/core'
import ToggleTheme from '@/components/common/ToggleTheme.vue'
import { useAppStore } from '@/store'
import BeginnerGuide from './BeginnerGuide.vue'
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
.header-more-tools {
  --chrome-text: var(--text-primary);
  display: grid;
  gap: 4px;
  min-width: 144px;
}
.header-more-tools :deep(button) {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  width: 100%;
  padding: 0 10px;
  margin: 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}
.header-more-tools :deep(button:hover) {
  background: var(--bg-secondary);
}
.header-more-tools :deep(button:focus-visible) {
  outline: 2px solid var(--primary-color);
  outline-offset: -2px;
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
