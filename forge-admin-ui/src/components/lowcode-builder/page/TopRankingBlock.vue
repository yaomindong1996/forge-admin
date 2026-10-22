<template>
  <div
    class="top-ranking"
    :class="{ 'is-compact': compact }"
    :style="rootStyle"
  >
    <header v-if="showHeader" class="tr-head">
      <div class="tr-head-copy">
        <h3 class="tr-title">
          {{ title || '排行榜' }}
        </h3>
        <p v-if="subtitle" class="tr-subtitle">
          {{ subtitle }}
        </p>
      </div>
      <span v-if="showTopHint" class="tr-hint">TOP {{ displayLimit }}</span>
    </header>

    <ol v-if="rankedItems.length" class="tr-list">
      <li
        v-for="item in rankedItems"
        :key="item.key"
        class="tr-row"
        :class="{
          'is-top': item.rank <= 3,
          [`rank-${item.rank}`]: item.rank <= 3,
          'is-interactive': interactive && item.clickable,
        }"
        @click="onSelect(item)"
      >
        <span class="tr-rank" aria-hidden="true">{{ item.rank }}</span>
        <div class="tr-main">
          <div class="tr-main-top">
            <strong class="tr-name">{{ item.name }}</strong>
            <span class="tr-value">
              {{ item.displayValue }}<small v-if="item.unit">{{ item.unit }}</small>
            </span>
          </div>
          <div v-if="showBar" class="tr-bar" aria-hidden="true">
            <i :style="{ width: `${item.percent}%` }" />
          </div>
        </div>
      </li>
    </ol>
    <div v-else class="tr-empty">
      {{ emptyText || '暂无排行数据' }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '排行榜' },
  subtitle: { type: String, default: '' },
  items: { type: Array, default: () => [] },
  /** 展示前 N 名 */
  limit: { type: [Number, String], default: 10 },
  compact: { type: Boolean, default: false },
  showHeader: { type: Boolean, default: true },
  showBar: { type: Boolean, default: true },
  showTopHint: { type: Boolean, default: true },
  accentColor: { type: String, default: '#2563eb' },
  emptyText: { type: String, default: '' },
  valueUnit: { type: String, default: '' },
  interactive: { type: Boolean, default: false },
})

const emit = defineEmits(['select'])

const rootStyle = computed(() => ({
  '--tr-accent': props.accentColor || 'var(--primary-color, #2563eb)',
}))

const displayLimit = computed(() => Math.max(1, Math.min(50, Number(props.limit) || 10)))

const rankedItems = computed(() => {
  const source = Array.isArray(props.items) ? props.items : []
  const normalized = source.map((raw, index) => normalizeItem(raw, index))
    .filter(item => item.name)
    .sort((a, b) => b.numericValue - a.numericValue)
    .slice(0, displayLimit.value)

  const max = Math.max(...normalized.map(item => item.numericValue), 0)
  return normalized.map((item, index) => ({
    ...item,
    rank: index + 1,
    percent: max > 0 ? Math.max(6, Math.round((item.numericValue / max) * 100)) : 0,
  }))
})

function normalizeItem(raw = {}, index = 0) {
  const name = String(raw.name ?? raw.label ?? raw.title ?? '').trim()
  const unit = String(raw.unit ?? props.valueUnit ?? '').trim()
  const numericValue = resolveNumeric(raw.value ?? raw.count ?? raw.score ?? raw.amount)
  const displayValue = raw.displayValue != null && String(raw.displayValue).trim()
    ? String(raw.displayValue).trim()
    : formatValue(numericValue)
  const clickable = Boolean(raw.href || raw.targetPageId || raw.path || raw.link)
  return {
    key: String(raw.key || raw.id || `rank_${index + 1}`),
    name: name || `项目 ${index + 1}`,
    unit,
    numericValue,
    displayValue,
    clickable,
    raw,
  }
}

function resolveNumeric(value) {
  if (typeof value === 'number' && Number.isFinite(value))
    return value
  const text = String(value ?? '').replace(/,/g, '').trim()
  const num = Number(text)
  return Number.isFinite(num) ? num : 0
}

function formatValue(value) {
  if (!Number.isFinite(value))
    return '0'
  if (Math.abs(value) >= 10000)
    return `${(value / 10000).toFixed(value % 10000 === 0 ? 0 : 1)}万`
  if (Number.isInteger(value))
    return String(value)
  return value.toFixed(1)
}

function onSelect(item) {
  if (!props.interactive || !item.clickable)
    return
  emit('select', item.raw)
}
</script>

<style scoped>
.top-ranking {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  min-height: 0;
  height: 100%;
}

.tr-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  flex-shrink: 0;
}

.tr-head-copy {
  min-width: 0;
}

.tr-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
  color: #0f172a;
}

.tr-subtitle {
  margin: 3px 0 0;
  font-size: 12px;
  line-height: 1.4;
  color: #64748b;
}

.tr-hint {
  flex-shrink: 0;
  margin-top: 2px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--tr-accent, #2563eb);
  background: color-mix(in srgb, var(--tr-accent, #2563eb) 12%, #fff);
}

.tr-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 0;
  overflow: auto;
  flex: 1;
}

.tr-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: #f8fafc;
}

.tr-row.is-interactive {
  cursor: pointer;
}

.tr-row.is-interactive:hover {
  border-color: color-mix(in srgb, var(--tr-accent, #2563eb) 28%, #e2e8f0);
  background: color-mix(in srgb, var(--tr-accent, #2563eb) 6%, #fff);
}

.tr-rank {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: #64748b;
  background: #e2e8f0;
}

.tr-row.rank-1 .tr-rank {
  color: #fff;
  background: linear-gradient(160deg, #f59e0b, #d97706);
}

.tr-row.rank-2 .tr-rank {
  color: #fff;
  background: linear-gradient(160deg, #94a3b8, #64748b);
}

.tr-row.rank-3 .tr-rank {
  color: #fff;
  background: linear-gradient(160deg, #d97706, #b45309);
}

.tr-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.tr-main-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.tr-name {
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tr-value {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--tr-accent, #2563eb);
  line-height: 1.35;
}

.tr-value small {
  margin-left: 2px;
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
}

.tr-bar {
  height: 4px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.tr-bar > i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--tr-accent, #2563eb);
}

.tr-row.rank-1 .tr-bar > i {
  background: #d97706;
}

.tr-row.rank-2 .tr-bar > i {
  background: #64748b;
}

.tr-row.rank-3 .tr-bar > i {
  background: #b45309;
}

.tr-empty {
  padding: 28px 12px;
  text-align: center;
  font-size: 12px;
  color: #64748b;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
}

.is-compact .tr-row {
  padding: 6px 8px;
}

.is-compact .tr-bar {
  display: none;
}
</style>
