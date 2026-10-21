<script setup>
import { computed } from 'vue'
import IconRenderer from '@/components/IconRenderer.vue'
import {
  normalizeQuickEntryItem,
  resolveToneCssVars,
  resolveWidgetLinkAction,
} from './free-layout-widget-kit.js'

const props = defineProps({
  items: { type: Array, default: () => [] },
  columns: { type: [Number, String], default: 3 },
  interactive: { type: Boolean, default: false },
  /** card | tile | compact */
  variant: { type: String, default: 'card' },
  showDescription: { type: Boolean, default: true },
})

const emit = defineEmits(['select'])

const normalizedItems = computed(() =>
  (Array.isArray(props.items) ? props.items : []).map((item, index) =>
    normalizeQuickEntryItem(item, index),
  ),
)

const gridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${Math.max(1, Math.min(6, Number(props.columns) || 3))}, minmax(0, 1fr))`,
}))

function itemStyle(item) {
  return resolveToneCssVars(item.tone, item.color)
}

function isClickable(item) {
  if (!props.interactive)
    return false
  return resolveWidgetLinkAction(item).type !== 'none'
}

function onSelect(item) {
  if (!isClickable(item))
    return
  emit('select', item)
}
</script>

<template>
  <div
    class="quick-entry-grid"
    :class="`variant-${variant || 'card'}`"
    :style="gridStyle"
  >
    <button
      v-for="item in normalizedItems"
      :key="item.key"
      type="button"
      class="quick-entry-card"
      :class="{
        'is-interactive': isClickable(item),
        'is-muted': !isClickable(item) && interactive,
      }"
      :style="itemStyle(item)"
      :disabled="interactive && !isClickable(item)"
      @click="onSelect(item)"
    >
      <span class="quick-entry-icon-wrap" aria-hidden="true">
        <IconRenderer :icon="item.icon || 'GridOutline'" :size="20" />
      </span>
      <span class="quick-entry-copy">
        <span class="quick-entry-title">{{ item.title }}</span>
        <span
          v-if="showDescription && item.description"
          class="quick-entry-desc"
        >{{ item.description }}</span>
      </span>
      <span
        v-if="isClickable(item)"
        class="quick-entry-arrow"
        aria-hidden="true"
      >›</span>
    </button>
    <div v-if="!normalizedItems.length" class="block-empty">
      点击右侧添加快捷入口
    </div>
  </div>
</template>

<style scoped>
.quick-entry-grid {
  display: grid;
  gap: 10px;
  width: 100%;
}

.quick-entry-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 64px;
  padding: 12px 14px;
  border: 1px solid color-mix(in srgb, var(--fl-tone-color) 18%, #e2e8f0);
  border-radius: 14px;
  background:
    linear-gradient(135deg, var(--fl-tone-soft), transparent 62%),
    #fff;
  text-align: left;
  cursor: default;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.quick-entry-card.is-interactive {
  cursor: pointer;
}

.quick-entry-card.is-interactive:hover {
  border-color: color-mix(in srgb, var(--fl-tone-color) 42%, #cbd5e1);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.quick-entry-card.is-muted {
  opacity: 0.72;
}

.quick-entry-card:disabled {
  cursor: default;
}

.quick-entry-icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  flex-shrink: 0;
  color: var(--fl-tone-color);
  background: color-mix(in srgb, var(--fl-tone-color) 14%, #fff);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--fl-tone-color) 16%, transparent);
}

.quick-entry-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.quick-entry-title {
  font-size: 14px;
  font-weight: 650;
  color: #0f172a;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-entry-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-entry-arrow {
  flex-shrink: 0;
  font-size: 18px;
  color: color-mix(in srgb, var(--fl-tone-color) 70%, #94a3b8);
  line-height: 1;
}

.variant-tile .quick-entry-card {
  flex-direction: column;
  align-items: flex-start;
  min-height: 108px;
  padding: 14px;
}

.variant-tile .quick-entry-arrow {
  display: none;
}

.variant-compact .quick-entry-card {
  min-height: 48px;
  padding: 8px 12px;
  border-radius: 10px;
  gap: 10px;
}

.variant-compact .quick-entry-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: 9px;
}

.variant-compact .quick-entry-title {
  font-size: 13px;
}

.block-empty {
  grid-column: 1 / -1;
  padding: 20px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
  border: 1px dashed #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
}
</style>
