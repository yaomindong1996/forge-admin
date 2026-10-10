<template>
  <section class="plugin-gallery" :aria-busy="loading" aria-label="我的插件">
    <!-- 首次加载保持卡片结构，刷新则保留原清单 -->
    <div v-if="loading && !records.length" class="plugin-gallery__grid">
      <div v-for="index in 6" :key="index" class="plugin-card plugin-card--skeleton">
        <div class="plugin-card__body">
          <NSkeleton width="64px" height="64px" />
          <div class="plugin-card__content">
            <NSkeleton text width="65%" />
            <NSkeleton text />
          </div>
        </div>
        <div class="plugin-card__footer">
          <NSkeleton text width="36px" />
          <NSkeleton text width="112px" />
        </div>
      </div>
    </div>
    <IllustratedEmpty
      v-else-if="!records.length" artwork="application"
      :description="filtered ? '没有找到匹配的插件，试试其他关键词' : '当前服务暂无已加载插件'"
    />
    <!-- 插画仅识别能力，安装记录和版本始终来自真实接口 -->
    <div v-else class="plugin-gallery__grid">
      <article v-for="row in records" :key="row.id" class="plugin-card">
        <div class="plugin-card__body">
          <PluginIllustration :plugin="row" :size="64" />
          <div class="plugin-card__content">
            <div class="plugin-card__identity">
              <h3 :title="row.name || row.id">
                {{ row.name || row.id }}
              </h3>
              <DictTag
                :options="dict.sys_plugin_origin" :value="row.origin"
                :bordered="false" type="default" force-tag
              />
            </div>
            <p :title="pluginPresentation(row).summary">
              {{ pluginPresentation(row).summary }}
            </p>
          </div>
        </div>
        <footer class="plugin-card__footer">
          <span class="plugin-card__version" :title="row.version">
            {{ row.version ? `v${row.version}` : '版本未提供' }}
          </span>
          <div class="plugin-card__actions">
            <NButton text type="primary" :disabled="!canDetail" @click="emit('open', row.id)">
              查看详情
            </NButton>
            <span class="plugin-card__separator" aria-hidden="true" />
            <NButton text :disabled="!canDetail" @click="emit('open', row.id, 'installation')">
              安装说明
            </NButton>
          </div>
        </footer>
      </article>
    </div>
  </section>
</template>

<script setup>
import { NButton, NSkeleton } from 'naive-ui'
import IllustratedEmpty from '@/components/common/IllustratedEmpty.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { pluginPresentation } from '../pluginPresentation'
import PluginIllustration from './PluginIllustration.vue'

defineProps({
  records: { type: Array, default: () => [] },
  loading: Boolean,
  canDetail: Boolean,
  filtered: Boolean,
})
const emit = defineEmits(['open'])
const { dict } = useDict('sys_plugin_origin')
</script>

<style scoped>
.plugin-gallery__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 300px), 1fr));
  gap: 12px;
}
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
.plugin-card:hover {
  border-color: var(--primary-color);
}
.plugin-card__body {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  padding: 16px;
}
.plugin-card__content {
  min-width: 0;
}
.plugin-card__identity {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.plugin-card__identity :deep(.n-tag) {
  flex: none;
}
.plugin-card h3 {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}
.plugin-card p {
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-tertiary);
  font-size: 13px;
  line-height: 1.6;
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
  min-width: 0;
  color: var(--text-tertiary);
  font-size: 11px;
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
</style>
