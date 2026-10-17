<template>
  <NConfigProvider :theme="dark ? darkTheme : undefined" :locale="zhCN"
    :theme-overrides="{ common: { primaryColor: defaultThemeConfig.primaryColor } }">
    <header class="verification-toolbar">
      <strong>最近下载 · 正式组件验证</strong><span>仅合成测试数据，不连接真实后端</span>
      <button @click="toggleDark">{{ dark ? '浅色模式' : '深色模式' }}</button>
      <button @click="collapsed = !collapsed">{{ collapsed ? '展开侧栏' : '收起侧栏' }}</button>
      <button @click="setMode('empty')">空状态</button><button @click="setMode('error')">错误状态</button>
      <button @click="setMode('normal')">恢复列表</button><button @click="setMode('failed')">下载失败</button>
      <button @click="setFixtureMode('normal')">网络恢复</button>
    </header>
    <div class="verification-shell">
      <aside v-if="!narrow" class="verification-sidebar" :class="{ collapsed }">
        <div class="verification-logo">F<span v-if="!collapsed">Forge Admin</span></div>
        <nav><div v-for="(name, index) in menuNames" :key="name" :class="{ selected: index === 3 }">
          <i :class="icons[index]" /><span v-if="!collapsed">{{ name }}</span>
        </div></nav>
        <RecentDownloads docked :compact="collapsed" />
        <div class="verification-account"><i class="i-lucide:circle-user-round" />
          <span v-if="!collapsed">测试账号<span>隔离验证环境</span></span></div>
      </aside>
      <main>
        <header>企业协同 <span>›</span> 采购订单</header>
        <section class="verification-table">
          <h1>采购订单</h1><p>此区域仅作为布局参照，下载面板、状态和请求使用正式代码。</p>
          <div class="verification-filters">订单名称 <input placeholder="请输入订单名称"><button>查询</button></div>
          <table><thead><tr><th>订单名称</th><th>审批状态</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody><tr v-for="index in 8" :key="index"><td>测试采购记录 {{ index }}</td>
              <td>已通过</td><td>2026-10-17 09:30</td><td>查看</td></tr></tbody></table>
        </section>
      </main>
    </div>
    <RecentDownloads v-if="narrow" compact />
  </NConfigProvider>
</template>

<script setup>
import { NConfigProvider, darkTheme, zhCN } from 'naive-ui'
import { onBeforeUnmount, ref } from 'vue'
import RecentDownloads from '@/layouts/components/RecentDownloads.vue'
import { applyThemeConfig, defaultThemeConfig } from '@/config/theme.config'
import { useRecentDownloadsStore } from '@/stores/system/recentDownloadsStore'
import { setFixtureMode } from './runtime'

const dark = ref(false)
const collapsed = ref(false)
const narrow = ref(window.innerWidth <= 768)
const resize = () => { narrow.value = window.innerWidth <= 768 }
window.addEventListener('resize', resize)
onBeforeUnmount(() => window.removeEventListener('resize', resize))
const store = useRecentDownloadsStore()
const menuNames = ['工作台', '应用中心', '流程中心', '企业协同', '数据与报表', '插件中心']
const icons = ['i-lucide:house', 'i-lucide:layout-grid', 'i-lucide:workflow',
  'i-lucide:users-round', 'i-lucide:chart-no-axes-combined', 'i-lucide:puzzle']
applyThemeConfig(defaultThemeConfig)

function toggleDark() {
  dark.value = !dark.value
  applyThemeConfig(defaultThemeConfig, dark.value)
  document.documentElement.classList.toggle('dark', dark.value)
}
async function setMode(mode) {
  await setFixtureMode(mode)
  store.clear()
  // 恢复测试模式需要恢复本浏览器隐藏 ID，仍不接触真实业务数据。
  localStorage.clear()
  store.$dispose()
  location.reload()
}
</script>

<style>
body { margin: 0; font: 13px/1.5 -apple-system, BlinkMacSystemFont, "PingFang SC", sans-serif;
  background: var(--bg-secondary); color: var(--text-primary); }
button, input { font: inherit; }
button { cursor: pointer; }
.verification-toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; min-height: 56px;
  padding: 8px 16px; border-bottom: 1px solid var(--border-light); background: var(--bg-primary); }
.verification-toolbar strong { font-weight: 500; }
.verification-toolbar span { color: var(--text-tertiary); font-size: 11px; margin-right: auto; }
.verification-toolbar button { padding: 4px 8px; border: 1px solid var(--border-light);
  border-radius: 4px; background: var(--bg-primary); color: var(--text-secondary); }
.verification-shell { display: flex; height: calc(100dvh - 86px); min-height: 650px; }
.verification-sidebar { display: flex; width: 218px; flex-shrink: 0; flex-direction: column;
  border-right: 1px solid var(--border-light); background: var(--side-menu-bg-color); }
.verification-sidebar.collapsed { width: 64px; }
.verification-logo { display: flex; align-items: center; gap: 12px; padding: 20px;
  color: var(--primary-color); font-size: 19px; font-weight: 600; }
.verification-logo span { color: var(--text-primary); font-size: 15px; }
.verification-sidebar nav { flex: 1; padding: 12px; }
.verification-sidebar nav > div { display: flex; align-items: center; gap: 12px; padding: 12px 6px;
  color: var(--side-menu-text-color); border-radius: 4px; }
.verification-sidebar nav i { font-size: 20px; color: var(--primary-color); }
.verification-sidebar nav .selected { background: var(--bg-secondary); }
.verification-account { display: flex; align-items: center; gap: 10px; padding: 16px;
  border-top: 1px solid var(--border-light); color: var(--text-secondary); }
.verification-account i { font-size: 24px; }
.verification-account > span > span { display: block; color: var(--text-tertiary); font-size: 10px; }
.verification-shell > main { min-width: 0; flex: 1; }
.verification-shell > main > header { padding: 17px 24px; background: var(--bg-primary);
  border-bottom: 1px solid var(--border-light); color: var(--text-secondary); }
.verification-shell > main > header span { margin: 0 12px; color: var(--text-tertiary); }
.verification-table { margin: 12px; padding: 20px; background: var(--bg-primary);
  border: 1px solid var(--border-light); border-radius: 6px; overflow: auto; }
.verification-table h1 { margin: 0; font-size: 16px; font-weight: 500; }
.verification-table p { color: var(--text-tertiary); font-size: 11px; }
.verification-filters { display: flex; align-items: center; gap: 12px; margin: 24px 0; }
.verification-filters input { width: 160px; padding: 6px 10px; border: 1px solid var(--border-light);
  border-radius: 4px; background: var(--bg-primary); color: var(--text-primary); }
.verification-filters button { padding: 6px 14px; border: 0; border-radius: 4px;
  background: var(--primary-color); color: white; }
table { width: 100%; border-spacing: 0; white-space: nowrap; text-align: left; }
th, td { padding: 14px 12px; border-bottom: 1px solid var(--border-light); font-weight: 400; font-size: 12px; }
th { background: var(--bg-secondary); color: var(--text-tertiary); }
</style>
