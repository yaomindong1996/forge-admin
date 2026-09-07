<template>
  <NConfigProvider :theme="appStore.isDark ? darkTheme : null" :theme-overrides="appStore.naiveThemeOverrides" :locale="zhCN" :date-locale="dateZhCN">
    <NMessageProvider><NDialogProvider>
      <PreviewContent />
    </NDialogProvider></NMessageProvider>
  </NConfigProvider>
</template>

<script setup>
import { defineComponent, h } from 'vue'
import { NButton, NConfigProvider, NDialogProvider, NMessageProvider, darkTheme, dateZhCN, useDialog, useMessage, zhCN } from 'naive-ui'
import { useAppStore } from '@/store'
import { applyThemeConfig } from '@/config/theme.config'
import SystemPageLayout from '@/components/common/SystemPageLayout.vue'
import DataScopeConfig from '@/views/system/dataScopeConfig.vue'

const appStore = useAppStore()
const PreviewContent = defineComponent({
  setup() {
    window.$dialog = useDialog()
    window.$message = useMessage()
    return () => h('div', { class: 'preview-host' }, [
      h('header', { class: 'preview-header' }, [
        h('span', '系统管理 / 数据权限'),
        h(NButton, { size: 'small', onClick: () => {
          appStore.isDark = !appStore.isDark
          applyThemeConfig(appStore.themeConfig, appStore.isDark)
          document.documentElement.classList.toggle('dark', appStore.isDark)
        } }, () => '切换主题'),
      ]),
      h('main', { class: 'preview-main' }, [h(SystemPageLayout, null, () => h(DataScopeConfig))]),
    ])
  },
})
</script>

<style>
html, body, #app, .n-config-provider { height: 100%; margin: 0; }
.preview-host { height: 100%; display: flex; flex-direction: column; background: var(--bg-secondary); color: var(--text-primary); }
.preview-header { display: flex; align-items: center; justify-content: space-between; height: 48px; flex-shrink: 0; padding: 0 16px; border-bottom: 1px solid var(--border-light); background: var(--bg-primary); font-size: 13px; }
.preview-main { flex: 1; min-height: 0; padding: 8px; }
</style>
