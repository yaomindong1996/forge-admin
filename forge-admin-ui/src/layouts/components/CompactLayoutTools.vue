<template>
  <!-- 无顶栏布局收纳低频工具，仍复用真实身份和个人资料逻辑 -->
  <n-popover v-model:show="visible" trigger="click" :placement="placement" :show-arrow="false">
    <template #trigger>
      <button
        class="compact-tools-trigger chrome-icon-button" type="button"
        title="账户与工具" aria-label="账户与工具" :aria-expanded="visible" aria-haspopup="dialog"
      >
        <i class="i-lucide:circle-user-round" />
      </button>
    </template>
    <section class="compact-account-tools" aria-label="账户与工具">
      <UserAvatar />
      <div class="account-context">
        <TenantSwitcher />
        <OrgSwitcher />
      </div>
      <div class="account-actions">
        <ToggleTheme with-label />
        <Fullscreen with-label />
        <BeginnerGuide v-if="showGuide" with-label />
        <button type="button" @click="openAppearance">
          <i class="i-lucide:panels-top-left" /><span>布局与外观</span>
        </button>
      </div>
    </section>
  </n-popover>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ToggleTheme from '@/components/common/ToggleTheme.vue'
import { useAppStore } from '@/store'
import BeginnerGuide from './BeginnerGuide.vue'
import Fullscreen from './Fullscreen.vue'
import OrgSwitcher from './OrgSwitcher.vue'
import TenantSwitcher from './TenantSwitcher.vue'
import UserAvatar from './UserAvatar.vue'

defineProps({ placement: { type: String, default: 'right-end' }, showGuide: Boolean })
const route = useRoute()
const appStore = useAppStore()
const visible = ref(false)
function openAppearance() {
  visible.value = false
  appStore.appearanceOpen = true
}
watch(() => route.fullPath, () => {
  visible.value = false
})
</script>

<style scoped>
.compact-tools-trigger {
  flex-shrink: 0;
}
.compact-account-tools {
  --chrome-text: var(--text-primary);
  --chrome-control-hover-bg: var(--bg-secondary);
  --chrome-control-border: var(--border-light);
  display: grid;
  gap: 8px;
  width: min(224px, calc(100vw - 40px));
  max-height: calc(100dvh - 40px);
  overflow-y: auto;
  color: var(--text-primary);
}
.account-context,
.account-actions {
  display: grid;
  gap: 4px;
}
.account-context:empty {
  display: none;
}
.account-actions {
  padding-top: 8px;
  border-top: 1px solid var(--border-light);
}
.compact-account-tools :deep(button) {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  width: 100%;
  max-width: none;
  min-height: 36px;
  height: auto;
  margin: 0;
  padding: 4px 8px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}
.compact-account-tools :deep(button:hover) {
  background: var(--bg-secondary);
}
.compact-account-tools :deep(button:focus-visible) {
  outline: 2px solid var(--primary-color);
  outline-offset: -2px;
}
</style>
