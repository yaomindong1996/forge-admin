<template>
  <article class="plugin-card">
    <!-- Header：图标 + 名称/编码，右侧来源状态 -->
    <header class="plugin-card__header">
      <div class="plugin-card__title-row">
        <PluginIllustration :plugin="plugin" :size="32" />
        <div class="plugin-card__name">
          <h3 :title="plugin.name || plugin.id">
            {{ plugin.name || plugin.id }}
          </h3>
          <code :title="plugin.id">{{ plugin.id }}</code>
        </div>
      </div>
      <span class="plugin-card__badge">
        <DictTag
          :options="origins"
          :value="plugin.origin"
          :bordered="false"
          size="small"
          type="default"
          force-tag
        />
      </span>
    </header>

    <!-- Tags + 简述 -->
    <div class="plugin-card__body">
      <div class="plugin-card__capabilities">
        <span v-for="item in presentation.highlights.slice(0, 2)" :key="item">{{ item }}</span>
        <span v-if="!presentation.highlights.length" class="is-muted">功能介绍以发布方说明为准</span>
      </div>
      <p class="plugin-card__summary" :title="presentation.summary">
        {{ presentation.summary || '暂无描述' }}
      </p>
    </div>

    <!-- Footer：版本元数据 + 文字操作 -->
    <footer class="plugin-card__footer">
      <span class="plugin-card__version" :title="plugin.version || '版本未提供'">
        <i class="i-lucide:package" aria-hidden="true" />
        <span>{{ plugin.version ? `v${plugin.version}` : '版本未提供' }}</span>
      </span>
      <div class="plugin-card__actions">
        <button
          type="button"
          class="plugin-card__link"
          :disabled="!canDetail"
          @click="emit('open', plugin.id)"
        >
          详情
        </button>
        <span class="plugin-card__separator" aria-hidden="true" />
        <button
          type="button"
          class="plugin-card__link"
          :disabled="!canDetail"
          @click="emit('open', plugin.id, 'installation')"
        >
          安装说明
        </button>
      </div>
    </footer>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { pluginPresentation } from '../pluginPresentation'
import PluginIllustration from './PluginIllustration.vue'

const props = defineProps({
  plugin: { type: Object, required: true },
  origins: { type: Array, default: () => [] },
  canDetail: Boolean,
})
const emit = defineEmits(['open'])
const presentation = computed(() => pluginPresentation(props.plugin))
</script>

<style scoped>
.plugin-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 4px;
  background: var(--bg-primary, #fff);
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.plugin-card:hover,
.plugin-card:focus-within {
  border-color: var(--primary-color, #356de0);
  box-shadow: 0 2px 8px rgb(0 0 0 / 4%);
}

.plugin-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 16px 16px 10px;
}

.plugin-card__title-row {
  display: flex;
  min-width: 0;
  flex: 1;
  gap: 10px;
}

.plugin-card__name {
  min-width: 0;
  flex: 1;
}

.plugin-card h3 {
  margin: 0;
  overflow: hidden;
  color: var(--text-primary, #0f172a);
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plugin-card code {
  display: block;
  margin-top: 3px;
  overflow: hidden;
  color: var(--text-tertiary, #94a3b8);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plugin-card__badge {
  flex: none;
  max-width: 40%;
}

.plugin-card__badge :deep(.n-tag) {
  max-width: 100%;
  height: 20px;
  padding: 0 6px;
  overflow: hidden;
  border-radius: 3px;
  background: var(--bg-secondary, #f6f8fb);
  color: var(--text-secondary, #4e5969);
  font-size: 11px;
  font-weight: 500;
  line-height: 20px;
}

.plugin-card__badge :deep(.n-tag__content) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plugin-card__body {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  padding: 0 16px 12px;
}

.plugin-card__capabilities {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.plugin-card__capabilities span {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  overflow: hidden;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 2px;
  background: var(--bg-secondary, #f6f8fb);
  color: var(--text-secondary, #4e5969);
  font-size: 11px;
  font-weight: 500;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plugin-card__capabilities span.is-muted {
  border-color: transparent;
  background: transparent;
  color: var(--text-tertiary, #94a3b8);
  padding: 0;
}

.plugin-card__summary {
  margin: 0;
  overflow: hidden;
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plugin-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 36px;
  margin-top: auto;
  padding: 8px 16px;
  border-top: 1px solid var(--border-light, #f1f5f9);
  background: var(--bg-secondary, #f8fafc);
}

.plugin-card__version {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 5px;
  color: var(--text-tertiary, #94a3b8);
  font-size: 11px;
}

.plugin-card__version i {
  flex: none;
}

.plugin-card__version span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plugin-card__actions {
  display: flex;
  flex: none;
  align-items: center;
  gap: 6px;
}

.plugin-card__link {
  margin: 0;
  padding: 0 2px;
  border: 0;
  background: transparent;
  color: var(--primary-color, #2563eb);
  font: inherit;
  font-size: 12px;
  line-height: 1.2;
  cursor: pointer;
}

.plugin-card__link:hover:not(:disabled) {
  color: var(--primary-hover, #1d4ed8);
}

.plugin-card__link:disabled {
  color: var(--text-tertiary, #94a3b8);
  cursor: not-allowed;
}

.plugin-card__separator {
  width: 1px;
  height: 12px;
  background: var(--border-light, #e2e8f0);
}

@media (prefers-reduced-motion: reduce) {
  .plugin-card {
    transition: none;
  }
}
</style>
