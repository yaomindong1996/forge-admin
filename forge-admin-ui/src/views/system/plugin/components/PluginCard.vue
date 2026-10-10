<template>
  <article class="plugin-card">
    <!-- 模块外观统一，符号区分能力；版本和来源来自接口。 -->
    <div class="plugin-card__body">
      <header class="plugin-card__identity">
        <PluginIllustration :plugin="plugin" :size="36" />
        <div class="plugin-card__name">
          <h3 :title="plugin.name || plugin.id">
            {{ plugin.name || plugin.id }}
          </h3>
          <code :title="plugin.id">{{ plugin.id }}</code>
        </div>
        <DictTag :options="origins" :value="plugin.origin" :bordered="false" type="default" force-tag />
      </header>
      <p class="plugin-card__summary" :title="presentation.summary">
        {{ presentation.summary }}
      </p>
      <div class="plugin-card__capabilities">
        <span v-for="item in presentation.highlights.slice(0, 2)" :key="item">{{ item }}</span>
        <span v-if="!presentation.highlights.length">功能介绍以发布方说明为准</span>
      </div>
    </div>
    <!-- 次要信息与操作分区，整卡不嵌套链接或按钮。 -->
    <footer class="plugin-card__footer">
      <span class="plugin-card__version" :title="plugin.version || '版本未提供'">
        <i class="i-lucide:package" aria-hidden="true" />
        <span>{{ plugin.version ? `v${plugin.version}` : '版本未提供' }}</span>
      </span>
      <div class="plugin-card__actions">
        <NButton text type="primary" :disabled="!canDetail" @click="emit('open', plugin.id)">
          查看详情
        </NButton>
        <span class="plugin-card__separator" aria-hidden="true" />
        <NButton text :disabled="!canDetail" @click="emit('open', plugin.id, 'installation')">
          安装说明
        </NButton>
      </div>
    </footer>
  </article>
</template>

<script setup>
import { NButton } from 'naive-ui'
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
  border: 1px solid var(--border-light);
  border-radius: 4px;
  background: var(--bg-primary);
  transition: border-color 150ms;
}
.plugin-card:hover,
.plugin-card:focus-within {
  border-color: var(--primary-color);
}
.plugin-card__body {
  flex: 1;
  padding: 16px;
}
.plugin-card__identity {
  display: flex;
  align-items: center;
  gap: 10px;
}
.plugin-card__identity :deep(.n-tag) {
  flex: none;
  font-size: 11px;
}
.plugin-card__name {
  flex: 1;
  min-width: 0;
}
.plugin-card h3 {
  margin: 0 0 3px;
  font-size: 14px;
  font-weight: 500;
}
.plugin-card h3,
.plugin-card code {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.plugin-card code {
  display: block;
  color: var(--text-tertiary);
  font-size: 11px;
}
.plugin-card__summary {
  margin: 14px 0 10px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
  min-height: 21px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: anywhere;
}
.plugin-card__capabilities {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  font-size: 11px;
}
.plugin-card__capabilities span {
  color: var(--text-tertiary);
}
.plugin-card__capabilities span + span {
  border-left: 1px solid var(--border-light);
  padding-left: 12px;
}
.plugin-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 16px;
  border-top: 1px solid var(--border-light);
  background: var(--bg-secondary);
}
.plugin-card__version {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 5px;
  color: var(--text-tertiary);
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
  gap: 8px;
}
.plugin-card__actions :deep(.n-button) {
  font-size: 12px;
}
.plugin-card__separator {
  height: 12px;
  width: 1px;
  background: var(--border-light);
}
@media (prefers-reduced-motion: reduce) {
  .plugin-card {
    transition: none;
  }
}
</style>
