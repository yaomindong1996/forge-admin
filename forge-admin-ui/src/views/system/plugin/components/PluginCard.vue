<template>
  <article class="plugin-card" :style="toneStyle">
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
        <span class="plugin-card__badge">
          <DictTag :options="origins" :value="plugin.origin" :bordered="false" type="default" force-tag />
        </span>
      </header>
      <div class="plugin-card__capabilities">
        <span v-for="item in presentation.highlights.slice(0, 2)" :key="item">{{ item }}</span>
        <span v-if="!presentation.highlights.length">功能介绍以发布方说明为准</span>
      </div>
      <p class="plugin-card__summary" :title="presentation.summary">
        {{ presentation.summary }}
      </p>
    </div>
    <!-- 次要信息与操作分区，整卡不嵌套链接或按钮。 -->
    <footer class="plugin-card__footer">
      <span class="plugin-card__version" :title="plugin.version || '版本未提供'">
        <i class="i-lucide:package" aria-hidden="true" />
        <span>{{ plugin.version ? `v${plugin.version}` : '版本未提供' }}</span>
      </span>
      <div class="plugin-card__actions">
        <button
          type="button"
          class="plugin-card__link plugin-card__link--primary"
          :disabled="!canDetail"
          @click="emit('open', plugin.id)"
        >
          <i class="i-lucide:info" aria-hidden="true" />
          详情
        </button>
        <span class="plugin-card__separator" aria-hidden="true" />
        <button
          type="button"
          class="plugin-card__link"
          :disabled="!canDetail"
          @click="emit('open', plugin.id, 'installation')"
        >
          <i class="i-lucide:book-open" aria-hidden="true" />
          安装说明
        </button>
      </div>
    </footer>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { pluginPresentation, pluginTone } from '../pluginPresentation'
import PluginIllustration from './PluginIllustration.vue'

const props = defineProps({
  plugin: { type: Object, required: true },
  origins: { type: Array, default: () => [] },
  canDetail: Boolean,
})
const emit = defineEmits(['open'])
const presentation = computed(() => pluginPresentation(props.plugin))
const toneStyle = computed(() => {
  const tone = pluginTone(props.plugin)
  return { '--plugin-ink': tone.ink, '--plugin-wash': tone.wash }
})
</script>

<style scoped>
.plugin-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 4px 14px rgb(15 23 42 / 4%);
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease;
}

.plugin-card:hover,
.plugin-card:focus-within {
  box-shadow: 0 12px 28px rgb(15 23 42 / 10%);
  transform: translateY(-2px);
}

.plugin-card__body {
  display: grid;
  gap: 10px;
  flex: 1;
  padding: 16px 16px 12px;
}

.plugin-card__identity {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.plugin-card__badge {
  flex: none;
  margin-left: auto;
}

.plugin-card__badge :deep(.n-tag) {
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 600;
  line-height: 22px;
}

.plugin-card__name {
  flex: 1;
  min-width: 0;
}

.plugin-card h3 {
  margin: 0 0 2px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 650;
  line-height: 1.35;
}

.plugin-card h3,
.plugin-card code {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plugin-card code {
  display: block;
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  line-height: 1.3;
}

.plugin-card__summary {
  margin: 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.55;
  min-height: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: anywhere;
}

.plugin-card__capabilities {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.plugin-card__capabilities span {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: 4px;
  background: color-mix(in srgb, var(--plugin-wash) 70%, #fff);
  color: var(--plugin-ink);
  font-size: 11px;
  font-weight: 500;
}

.plugin-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: nowrap;
  margin-top: auto;
  min-height: 36px;
  padding: 8px 14px;
  border-top: 1px solid #f1f5f9;
  background: #f8fafc;
}

.plugin-card__version {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 5px;
  color: #94a3b8;
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
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 0;
  padding: 0 4px;
  border: 0;
  background: transparent;
  color: #2563eb;
  font: inherit;
  font-size: 12px;
  line-height: 1.2;
  cursor: pointer;
}

.plugin-card__link:hover:not(:disabled) {
  color: #1d4ed8;
}

.plugin-card__link--primary {
  font-weight: 650;
}

.plugin-card__link:disabled {
  color: #94a3b8;
  cursor: not-allowed;
}

.plugin-card__separator {
  height: 12px;
  width: 1px;
  background: #e2e8f0;
}

@media (prefers-reduced-motion: reduce) {
  .plugin-card {
    transition: none;
  }

  .plugin-card:hover,
  .plugin-card:focus-within {
    transform: none;
  }
}
</style>
