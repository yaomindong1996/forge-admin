<script setup>
import { NButton, NSkeleton, NTag } from 'naive-ui'
import { computed } from 'vue'
import IllustratedEmpty from '@/components/common/IllustratedEmpty.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

const props = defineProps({
  records: { type: Array, default: () => [] },
  loading: Boolean,
  canDetail: Boolean,
})
const emit = defineEmits(['open'])
const { dict } = useDict('sys_plugin_origin', 'sys_plugin_edition', 'sys_plugin_load_state')

const skeletons = computed(() => Array.from({ length: 6 }, (_, index) => index))

function featureCount(row) {
  return Array.isArray(row?.features) ? row.features.length : 0
}

function open(row) {
  if (!props.canDetail || !row?.id)
    return
  emit('open', row.id)
}
</script>

<template>
  <div class="plugin-gallery" :aria-busy="loading ? 'true' : 'false'">
    <div v-if="loading && !records.length" class="plugin-gallery__grid">
      <div v-for="item in skeletons" :key="item" class="plugin-card is-skeleton">
        <NSkeleton height="148px" :sharp="false" />
      </div>
    </div>

    <IllustratedEmpty
      v-else-if="!records.length"
      artwork="application"
      description="当前服务没有符合条件的已加载插件"
    />

    <div v-else class="plugin-gallery__grid">
      <button
        v-for="row in records"
        :key="row.id"
        type="button"
        class="plugin-card"
        :class="{ 'is-interactive': canDetail }"
        :disabled="!canDetail"
        :aria-label="canDetail ? `查看插件 ${row.name}` : row.name"
        @click="open(row)"
      >
        <div class="plugin-card__cover">
          <span class="plugin-card__glyph" aria-hidden="true">
            <i class="i-lucide:puzzle" />
          </span>
          <DictTag
            class="plugin-card__state"
            :options="dict.sys_plugin_load_state"
            :value="row.loadState"
            size="small"
          />
        </div>
        <div class="plugin-card__body">
          <div class="plugin-card__title-row">
            <h3>{{ row.name || row.id }}</h3>
            <NTag size="small" :bordered="false" type="info">
              v{{ row.version || '—' }}
            </NTag>
          </div>
          <code class="plugin-card__id">{{ row.id }}</code>
          <div class="plugin-card__meta">
            <DictTag :options="dict.sys_plugin_origin" :value="row.origin" size="small" />
            <DictTag :options="dict.sys_plugin_edition" :value="row.edition" size="small" />
          </div>
          <p class="plugin-card__summary">
            {{ row.requiresCore ? `兼容核心 ${row.requiresCore}` : '随核心发行' }}
            · 声明功能 {{ featureCount(row) }} 项
            <template v-if="row.serverModule"> · {{ row.serverModule }}</template>
          </p>
        </div>
        <footer class="plugin-card__foot">
          <span>{{ row.hasUi ? '含前端声明' : '仅后端模块' }}</span>
          <NButton v-if="canDetail" text type="primary" tabindex="-1">
            查看详情
            <template #icon>
              <i class="i-lucide:arrow-right" />
            </template>
          </NButton>
        </footer>
      </button>
    </div>
  </div>
</template>

<style scoped>
.plugin-gallery__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.plugin-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 10px;
  background: var(--card-color, #fff);
  color: inherit;
  text-align: left;
  cursor: default;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}
.plugin-card.is-interactive {
  cursor: pointer;
}
.plugin-card.is-interactive:hover {
  border-color: color-mix(in srgb, var(--primary-color, #0e42d2) 35%, #fff);
  box-shadow: 0 10px 24px rgb(15 23 42 / 6%);
  transform: translateY(-2px);
}
.plugin-card.is-interactive:focus-visible {
  outline: 2px solid var(--primary-color, #0e42d2);
  outline-offset: 2px;
}
.plugin-card.is-skeleton {
  min-height: 220px;
  padding: 0;
  border: 0;
  background: transparent;
}
.plugin-card__cover {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 96px;
  background:
    radial-gradient(circle at 18% 20%, color-mix(in srgb, var(--primary-color, #0e42d2) 18%, #fff), transparent 48%),
    linear-gradient(160deg, #f8fafc 0%, #eef2ff 100%);
}
.plugin-card__glyph {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: color-mix(in srgb, var(--primary-color, #0e42d2) 12%, #fff);
  color: var(--primary-color, #0e42d2);
  font-size: 22px;
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--primary-color, #0e42d2) 16%, transparent);
}
.plugin-card__state {
  position: absolute;
  top: 10px;
  right: 10px;
}
.plugin-card__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  padding: 14px 14px 10px;
}
.plugin-card__title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}
.plugin-card__title-row h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.plugin-card__id {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.plugin-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.plugin-card__summary {
  margin: 0;
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.plugin-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 14px 12px;
  border-top: 1px solid var(--border-color, #e5e7eb);
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}
@media (max-width: 1200px) {
  .plugin-gallery__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 720px) {
  .plugin-gallery__grid {
    grid-template-columns: 1fr;
  }
}
</style>
