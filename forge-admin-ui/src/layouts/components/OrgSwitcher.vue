<template>
  <!-- 紧凑账户面板等低频入口仍可内嵌；桌面顶栏改走头像菜单二级切换 -->
  <n-dropdown
    v-if="showOrgSwitcher"
    trigger="click"
    :options="dropdownOptions"
    :disabled="switchingOrg"
    @select="handleSelect"
  >
    <button
      type="button"
      class="org-switcher"
      :class="{ 'is-quiet': quiet }"
      :disabled="switchingOrg"
      :title="`当前组织：${currentOrgName}`"
      :aria-label="`切换组织，当前为${currentOrgName}`"
    >
      <i class="org-icon i-material-symbols:account-tree-rounded" />
      <span class="org-name">{{ currentOrgName }}</span>
      <i class="org-arrow i-material-symbols:expand-more-rounded" />
    </button>
  </n-dropdown>
</template>

<script setup>
import { useOrgSwitch } from './composables/useOrgSwitch'

defineProps({
  /** 面板内使用时弱化边框，避免抢视觉 */
  quiet: {
    type: Boolean,
    default: false,
  },
})

const {
  switchingOrg,
  canSwitchOrg,
  currentOrgName,
  orgMenuChildren,
  handleOrgMenuSelect,
} = useOrgSwitch()

const showOrgSwitcher = canSwitchOrg
const dropdownOptions = orgMenuChildren

function handleSelect(key) {
  handleOrgMenuSelect(key)
}
</script>

<style scoped>
.org-switcher {
  height: 32px;
  max-width: 260px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  border: 1px solid var(--chrome-control-border, var(--border-light));
  border-radius: 6px;
  background: var(--chrome-control-bg, var(--bg-secondary));
  color: var(--chrome-text, var(--text-secondary));
  cursor: pointer;
  transition: all var(--transition-base);
  margin-right: 0;
}

.org-switcher:hover {
  border-color: var(--chrome-control-border, var(--border-default));
  background: var(--chrome-control-hover-bg, var(--bg-secondary));
  color: var(--chrome-text, var(--text-primary));
}

.org-switcher.is-quiet {
  width: 100%;
  max-width: none;
  justify-content: flex-start;
  border-color: transparent;
  background: transparent;
}

.org-switcher.is-quiet:hover {
  background: var(--chrome-control-hover-bg, var(--bg-secondary));
}

.org-icon,
.org-arrow {
  flex-shrink: 0;
  font-size: 16px;
}

.org-name {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  text-align: left;
}
</style>
