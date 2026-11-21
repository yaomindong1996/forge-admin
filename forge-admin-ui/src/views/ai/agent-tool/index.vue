<script setup>
import { computed, ref } from 'vue'
import McpServiceDirectoryPanel from '../components/resource-center/McpServiceDirectoryPanel.vue'
import ResourceToolCatalogPanel from '../components/resource-center/ResourceToolCatalogPanel.vue'
import SkillPackageDirectoryPanel from '../components/resource-center/SkillPackageDirectoryPanel.vue'

defineOptions({ name: 'AiAgentTool' })

const activeTab = ref('tools')

const tabs = [
  { id: 'tools', label: '工具目录', desc: '查看平台内置工具、MCP 远程工具与能力平台目录，在智能体配置中按需装配。' },
  { id: 'mcp', label: 'MCP 服务', desc: '接入 Streamable HTTP 远程 MCP 工具服务，测试并同步工具目录。' },
  { id: 'skills', label: '技能资料', desc: '导入不可变只读 ZIP 技能资料包，启用后固定版本绑定至智能体。' },
]

const currentTab = computed(() => tabs.find(item => item.id === activeTab.value) || tabs[0])
</script>

<template>
  <section class="resource-center-page" aria-label="AI 资源中心">
    <header class="resource-center-header">
      <div>
        <h1>资源中心</h1>
        <p>{{ currentTab.desc }}</p>
      </div>
      <nav class="resource-center-tabs" aria-label="资源分类">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          class="resource-center-tab"
          :class="{ 'is-active': activeTab === tab.id }"
          :aria-current="activeTab === tab.id ? 'page' : undefined"
          @click="activeTab = tab.id"
        >
          {{ tab.label }}
        </button>
      </nav>
    </header>

    <div class="resource-center-body">
      <ResourceToolCatalogPanel v-if="activeTab === 'tools'" />
      <McpServiceDirectoryPanel v-else-if="activeTab === 'mcp'" />
      <SkillPackageDirectoryPanel v-else />
    </div>
  </section>
</template>

<style scoped>
.resource-center-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.resource-center-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 16px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}

.resource-center-header h1 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.resource-center-header p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-tertiary, #6b7280);
}

.resource-center-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
}

.resource-center-tab {
  padding: 6px 14px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  color: var(--text-secondary, #4b5563);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.resource-center-tab.is-active {
  color: var(--primary-color, #2563eb);
  border-color: var(--primary-color, #2563eb);
  font-weight: 600;
}

.resource-center-body {
  padding: 14px 16px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}
</style>
