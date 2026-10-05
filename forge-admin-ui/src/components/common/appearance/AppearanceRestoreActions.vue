<template>
  <!-- 恢复动作仅针对当前会话，系统初始配色与租户外观明确分离 -->
  <div class="appearance-restore-actions">
    <n-button @click="restoreTenant">
      恢复租户外观
    </n-button>
    <n-popconfirm @positive-click="restoreSystem">
      <template #trigger>
        <n-button text class="restore-system-trigger">
          恢复系统配色
        </n-button>
      </template>
      仅恢复当前会话的系统初始配色，保留当前布局和身份，不修改租户配置。确定恢复吗？
    </n-popconfirm>
  </div>
</template>

<script setup>
import { useAppStore, useTenantStore } from '@/store'

const appStore = useAppStore()
const tenantStore = useTenantStore()
function restoreTenant() {
  appStore.restoreTenantAppearance(tenantStore.config)
  window.$message?.success('已恢复租户外观，仅影响当前会话')
}
function restoreSystem() {
  appStore.restoreSystemTheme()
  window.$message?.success('已恢复系统配色，当前布局保持不变')
}
</script>

<style scoped>
.appearance-restore-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.restore-system-trigger {
  color: var(--text-tertiary);
  font-size: 12px;
}
</style>
