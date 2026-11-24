<template>
  <div class="header-tools" aria-label="系统工具">
    <!-- 按设计图分组：图标簇 | 租户/组织 | 布局工具 | 账户 -->
    <div class="tools-cluster tools-cluster--icons">
      <MenuSearch />
      <ToggleTheme v-if="!isCompact" />
      <MessageNotification />
    </div>
    <!-- 极窄窗口只保留核心入口，账户工具不丢失、不重复挂载切换器 -->
    <CompactLayoutTools v-if="isCompact" placement="bottom-end" />
    <template v-else>
      <span class="tools-divider" aria-hidden="true" />
      <div class="tools-cluster tools-cluster--context">
        <TenantSwitcher />
      </div>
      <div class="tools-cluster tools-cluster--icons">
        <button
          id="layout-setting"
          class="chrome-icon-button"
          type="button"
          title="布局与外观"
          aria-label="布局与外观"
          @click="appStore.appearanceOpen = true"
        >
          <i class="i-lucide:panels-top-left" />
        </button>
        <!-- 移除指引后仅剩全屏，不再用额外浮层承载单一操作。 -->
        <Fullscreen />
      </div>
      <span class="tools-divider" aria-hidden="true" />
      <!-- 组织切换改到头像下拉二级菜单，顶栏不再常驻占位 -->
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
import TenantSwitcher from './TenantSwitcher.vue'
import UserAvatar from './UserAvatar.vue'

// showAppearance 保留兼容旧调用；布局入口统一在顶栏，默认始终展示。
defineProps({ showAppearance: { type: Boolean, default: true } })
const appStore = useAppStore()
const isCompact = useMediaQuery('(max-width: 480px)')
</script>

<style scoped>
.header-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  color: var(--chrome-text, var(--text-primary));
}

.tools-cluster {
  display: inline-flex;
  align-items: center;
  min-width: 0;
}

.tools-cluster--icons {
  gap: 2px;
}

.tools-cluster--context {
  gap: 6px;
}

.tools-divider {
  width: 1px;
  height: 16px;
  flex: 0 0 auto;
  background: color-mix(in srgb, var(--chrome-text, var(--text-secondary)) 22%, transparent);
}

.header-tools :deep(.tenant-switcher) {
  margin: 0;
  padding-inline: 10px;
  border: 0;
  border-radius: 8px;
  background: color-mix(in srgb, var(--primary-color) 12%, var(--layout-header-bg-color, var(--bg-primary, #fff)));
  color: var(--chrome-text, var(--text-primary));
}

.header-tools :deep(.tenant-switcher:hover) {
  background: color-mix(in srgb, var(--primary-color) 18%, var(--layout-header-bg-color, var(--bg-primary, #fff)));
}

.header-tools :deep(.tenant-name) {
  max-width: 96px;
}

.header-tools :deep(#user-dropdown.user-trigger) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding-inline: 4px 2px;
  border-radius: 8px;
}

.header-tools :deep(#user-dropdown.user-trigger::after) {
  content: '';
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
  background: currentColor;
  opacity: 0.7;
  /* expand-more chevron via mask so不依赖额外图标组件 */
  -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='black' d='M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6z'/%3E%3C/svg%3E") center / contain no-repeat;
  mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='black' d='M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6z'/%3E%3C/svg%3E") center / contain no-repeat;
}

@media (max-width: 900px) {
  .header-tools :deep(.tenant-name),
  .header-tools :deep(#user-dropdown .user-name),
  .header-tools :deep(#user-dropdown .account-name) {
    display: none;
  }

  .header-tools {
    gap: 6px;
  }
}
</style>
