<script setup>
import { computed } from 'vue'
import IconRenderer from '@/components/IconRenderer.vue'
import {
  normalizeStatMetric,
  resolveToneCssVars,
  resolveWidgetLinkAction,
} from './free-layout-widget-kit.js'

const props = defineProps({
  metrics: { type: Array, default: () => [] },
  columns: { type: [Number, String], default: 4 },
  interactive: { type: Boolean, default: false },
  showTrend: { type: Boolean, default: true },
  showIcon: { type: Boolean, default: true },
  compact: { type: Boolean, default: false },
})

const emit = defineEmits(['select'])

const normalized = computed(() =>
  (Array.isArray(props.metrics) ? props.metrics : []).map((item, index) =>
    normalizeStatMetric(item, index),
  ),
)

const gridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${Math.max(1, Math.min(6, Number(props.columns) || 4))}, minmax(0, 1fr))`,
}))

function cardStyle(metric) {
  return resolveToneCssVars(metric.tone, metric.color)
}

function isClickable(metric) {
  if (!props.interactive)
    return false
  return resolveWidgetLinkAction(metric).type !== 'none'
}

function trendClass(trend) {
  const text = String(trend || '')
  if (/[+\u2191]|上升|增长/.test(text))
    return 'is-up'
  if (/[-−\u2193]|下降|回落/.test(text))
    return 'is-down'
  return ''
}

function onSelect(metric) {
  if (!isClickable(metric))
    return
  emit('select', metric)
}
</script>

<template>
  <div
    class="stats-strip"
    :class="{ 'is-compact': compact }"
    :style="gridStyle"
  >
    <button
      v-for="metric in normalized"
      :key="metric.key"
      type="button"
      class="stats-card"
      :class="{ 'is-interactive': isClickable(metric) }"
      :style="cardStyle(metric)"
      :disabled="interactive && !isClickable(metric)"
      @click="onSelect(metric)"
    >
      <div class="stats-card-top">
        <span class="stats-label">{{ metric.label }}</span>
        <span
          v-if="showIcon && metric.icon"
          class="stats-icon"
          aria-hidden="true"
        >
          <IconRenderer :icon="metric.icon" :size="16" />
        </span>
      </div>
      <div class="stats-value">{{ metric.value }}</div>
      <div
        v-if="showTrend && metric.trend"
        class="stats-trend"
        :class="trendClass(metric.trend)"
      >
        {{ metric.trend }}
      </div>
    </button>
    <div v-if="!normalized.length" class="block-empty">
      点击右侧添加指标项
    </div>
  </div>
</template>

<style scoped>
.stats-strip {
  display: grid;
  gap: 10px;
  width: 100%;
}

.stats-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 92px;
  padding: 14px 14px 12px;
  border: 1px solid color-mix(in srgb, var(--fl-tone-color) 16%, #e2e8f0);
  border-radius: 14px;
  background:
    linear-gradient(160deg, var(--fl-tone-soft), transparent 58%),
    #fff;
  text-align: left;
  cursor: default;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.stats-card.is-interactive {
  cursor: pointer;
}

.stats-card.is-interactive:hover {
  border-color: color-mix(in srgb, var(--fl-tone-color) 40%, #cbd5e1);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.stats-card:disabled {
  cursor: default;
}

.stats-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.stats-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 550;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stats-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 9px;
  color: var(--fl-tone-color);
  background: color-mix(in srgb, var(--fl-tone-color) 14%, #fff);
  flex-shrink: 0;
}

.stats-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
  line-height: 1.15;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stats-trend {
  font-size: 12px;
  color: #64748b;
}

.stats-trend.is-up {
  color: #16a34a;
}

.stats-trend.is-down {
  color: #dc2626;
}

.is-compact .stats-card {
  min-height: 72px;
  padding: 10px 12px;
  border-radius: 12px;
  gap: 4px;
}

.is-compact .stats-value {
  font-size: 20px;
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
