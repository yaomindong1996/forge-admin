<script setup>
import { NButton, NTag } from 'naive-ui'

defineProps({
  name: { type: String, default: '' },
  status: { type: Object, default: () => ({ label: '', type: 'default' }) },
  mode: { type: String, default: 'design' },
  embedded: Boolean,
  aiActive: Boolean,
  saving: Boolean,
  deploying: Boolean,
})

defineEmits(['back', 'mode', 'history', 'ai', 'save', 'deploy'])
</script>

<template>
  <header class="flow-design-toolbar">
    <!-- 流程身份：名称和状态来自已加载模型，不使用效果图中的示例文案 -->
    <div class="flow-design-identity">
      <NButton quaternary size="small" @click="$emit('back')">
        <template #icon>
          <i :class="embedded ? 'i-lucide:x' : 'i-lucide:arrow-left'" />
        </template>
        {{ embedded ? '关闭' : '返回' }}
      </NButton>
      <span class="flow-design-identity__divider" />
      <i class="flow-design-identity__icon i-lucide:workflow" aria-hidden="true" />
      <h1 :title="name || '新流程'">
        {{ name || '新流程' }}
      </h1>
      <NTag v-if="status.label" :type="status.type" size="small" :bordered="false">
        {{ status.label }}
      </NTag>
    </div>

    <!-- 工作区切换 -->
    <nav class="flow-design-tabs" aria-label="流程工作区">
      <button
        v-for="tab in [{ key: 'design', label: '流程设计' }, { key: 'settings', label: '更多设置' }]"
        :key="tab.key"
        type="button"
        :class="{ active: mode === tab.key }"
        :aria-current="mode === tab.key ? 'page' : undefined"
        @click="$emit('mode', tab.key)"
      >
        {{ tab.label }}
      </button>
    </nav>

    <!-- 全局操作沿用原有保存、发布、历史与 AI 行为 -->
    <div class="flow-design-actions">
      <NButton
        quaternary size="small" class="flow-design-secondary" aria-label="更改记录" title="更改记录"
        @click="$emit('history')"
      >
        <template #icon>
          <i class="i-lucide:history" />
        </template>
        <span>更改记录</span>
      </NButton>
      <NButton
        quaternary size="small" class="flow-design-secondary" :type="aiActive ? 'primary' : 'default'"
        aria-label="AI辅助" title="AI辅助" @click="$emit('ai')"
      >
        <template #icon>
          <i class="i-lucide:sparkles" />
        </template>
        <span>AI辅助</span>
      </NButton>
      <NButton size="small" :loading="saving" @click="$emit('save')">
        保存草稿
      </NButton>
      <NButton size="small" type="primary" :loading="deploying" @click="$emit('deploy')">
        发布流程
      </NButton>
    </div>
  </header>
</template>

<style scoped>
.flow-design-toolbar {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) auto minmax(320px, 1fr);
  align-items: center;
  flex: none;
  min-height: 60px;
  gap: 16px;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-light, #e5e7eb);
  background: var(--bg-primary, #fff);
  color: var(--text-primary, #1d2129);
}

.flow-design-identity,
.flow-design-actions {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.flow-design-actions {
  justify-content: flex-end;
}
.flow-design-identity__divider {
  height: 18px;
  border-left: 1px solid var(--border-light, #e5e7eb);
}
.flow-design-identity__icon {
  flex: none;
  color: var(--primary-color, #356de0);
  font-size: 24px;
}
.flow-design-identity h1 {
  overflow: hidden;
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.flow-design-identity :deep(.n-tag) {
  flex: none;
  font-size: 11px;
}

.flow-design-tabs {
  display: flex;
  align-self: stretch;
  gap: 24px;
}
.flow-design-tabs button {
  position: relative;
  border: none;
  padding: 0 6px;
  background: transparent;
  color: var(--text-secondary, #4e5969);
  font-size: 14px;
  cursor: pointer;
  transition: color 160ms;
}
.flow-design-tabs button::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 2px;
  background: var(--primary-color, #356de0);
  content: '';
  opacity: 0;
}
.flow-design-tabs button.active {
  color: var(--primary-color, #356de0);
  font-weight: 500;
}
.flow-design-tabs button.active::after {
  opacity: 1;
}
.flow-design-tabs button:hover {
  color: var(--primary-color, #356de0);
}
.flow-design-tabs button:focus-visible {
  outline: 2px solid var(--primary-color, #356de0);
  outline-offset: -4px;
}

@media (max-width: 1100px) {
  .flow-design-toolbar {
    grid-template-columns: minmax(150px, 1fr) auto auto;
    gap: 12px;
    padding: 0 12px;
  }
  .flow-design-actions {
    gap: 6px;
  }
  .flow-design-identity :deep(.n-tag) {
    display: none;
  }
  .flow-design-secondary span {
    display: none;
  }
  .flow-design-secondary :deep(.n-button__icon) {
    margin: 0;
  }
}
@media (max-width: 680px) {
  .flow-design-toolbar {
    grid-template-columns: minmax(0, 1fr);
    gap: 0;
    padding: 8px 12px 0;
  }
  .flow-design-identity {
    grid-column: 1;
    min-height: 34px;
  }
  .flow-design-actions {
    grid-column: 1;
    min-height: 40px;
    justify-content: flex-start;
    flex-wrap: wrap;
  }
  .flow-design-tabs {
    grid-column: 1;
    grid-row: 3;
    height: 42px;
    justify-content: center;
    gap: 40px;
  }
}
</style>
