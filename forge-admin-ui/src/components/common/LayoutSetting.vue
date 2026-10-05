<template>
  <div>
    <!-- 空白布局也保留恢复入口；工作台在顶栏调用同一个 Pinia 状态 -->
    <n-tooltip v-if="showTrigger" trigger="hover" placement="left">
      <template #trigger>
        <button
          id="layout-setting" class="layout-setting-entry" type="button" aria-label="布局与外观"
          @click="appStore.appearanceOpen = true"
        >
          <i class="i-lucide:panels-top-left" />
        </button>
      </template>
      布局与外观
    </n-tooltip>
    <n-drawer
      v-model:show="appStore.appearanceOpen" placement="right"
      width="min(760px, calc(100vw - 24px))" class="appearance-drawer"
    >
      <n-drawer-content title="布局与外观" closable>
        <!-- 统一设置，无论切到哪种布局面板都不会销毁 -->
        <n-tabs type="line">
          <n-tab-pane name="layout" tab="布局">
            <LayoutPicker v-model="layout" />
            <p class="setting-note">
              切换即时生效，只影响当前会话；空白布局保留右侧恢复入口。
            </p>
          </n-tab-pane>
          <n-tab-pane name="theme" tab="配色">
            <AppearanceThemeEditor v-model="theme" :layout="layout" />
          </n-tab-pane>
        </n-tabs>
        <p class="setting-note">
          租户默认布局和品牌配色请在租户管理中保存。
        </p>
        <template #footer>
          <AppearanceRestoreActions />
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAppStore } from '@/store'
import AppearanceRestoreActions from './appearance/AppearanceRestoreActions.vue'
import AppearanceThemeEditor from './appearance/AppearanceThemeEditor.vue'
import LayoutPicker from './appearance/LayoutPicker.vue'

defineProps({ showTrigger: { type: Boolean, default: true } })
const appStore = useAppStore()
const layout = computed({ get: () => appStore.layout, set: value => appStore.setLayout(value) })
const theme = computed({ get: () => appStore.themeConfig, set: value => appStore.setThemeConfig(value) })
</script>

<style scoped>
.layout-setting-entry {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  color: var(--text-secondary);
  background: var(--bg-primary);
  cursor: pointer;
  box-shadow: 0 2px 8px rgb(0 0 0 / 6%);
}
.layout-setting-entry:hover {
  color: var(--primary-color);
  border-color: var(--primary-color);
}
.layout-setting-entry i {
  width: 18px;
  height: 18px;
}
.setting-note {
  font-size: 12px;
  color: var(--text-tertiary);
  line-height: 1.6;
  margin: 12px 0 0;
}
</style>
