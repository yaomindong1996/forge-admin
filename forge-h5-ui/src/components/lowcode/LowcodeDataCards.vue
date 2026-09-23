<template>
  <view class="mobile-data-cards">
    <view v-if="title" class="mobile-data-cards__title">{{ title }}</view>
    <view v-if="rows.length" class="mobile-data-cards__list">
      <view v-for="(entry, index) in rows" :key="rowKey(entry.row, index)" class="mobile-data-card">
        <view class="mobile-data-card__head" :style="treeIndent(entry.depth)">
          <text v-if="tree" class="mobile-data-card__branch">{{ entry.depth ? '└' : '●' }}</text>
          <text>{{ rowTitle(entry.row, index) }}</text>
        </view>
        <view class="mobile-data-card__fields">
          <view v-for="column in visibleColumns" :key="columnKey(column)" class="mobile-data-card__field">
            <text>{{ column.label || column.title || columnKey(column) }}</text>
            <text>{{ formatValue(entry.row[columnKey(column)]) }}</text>
          </view>
        </view>
      </view>
    </view>
    <view v-else class="mobile-data-cards__empty">已适配为移动端卡片，暂无运行数据</view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  title: { type: String, default: '' },
  items: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  tree: { type: Boolean, default: false },
  rowKeyField: { type: String, default: 'id' },
  titleField: { type: String, default: '' },
})
const rows = computed(() => props.tree ? flattenTree(props.items) : props.items.map(row => ({ row, depth: 0 })))
const visibleColumns = computed(() => {
  if (props.columns.length) return props.columns.filter(column => column?.visible !== false).slice(0, 6)
  const sample = rows.value[0]?.row
  return sample && typeof sample === 'object'
    ? Object.keys(sample).filter(key => !['children', props.rowKeyField].includes(key)).slice(0, 6).map(key => ({ field: key, label: key }))
    : []
})
function flattenTree(source, depth = 0) {
  return source.flatMap((row) => [{ row, depth }, ...flattenTree(Array.isArray(row?.children) ? row.children : [], depth + 1)])
}
function columnKey(column) { return column.prop || column.field || column.key || '' }
function rowKey(row, index) { return String(row?.[props.rowKeyField] || row?.key || index) }
function rowTitle(row, index) {
  if (!row || typeof row !== 'object') return String(row ?? `第 ${index + 1} 条`)
  const firstField = props.titleField || columnKey(visibleColumns.value[0])
  return String(row[firstField] ?? row.name ?? row.title ?? `第 ${index + 1} 条`)
}
function formatValue(value) {
  if (value === undefined || value === null || value === '') return '-'
  if (Array.isArray(value)) return value.join('、')
  if (typeof value === 'object') return value.label || value.name || JSON.stringify(value)
  return String(value)
}
function treeIndent(depth) { return props.tree ? { paddingLeft: `${Math.min(5, depth) * 28}rpx` } : {} }
</script>

<style lang="scss" scoped>
.mobile-data-cards__title { margin-bottom: 16rpx; color: #1d2129; font-size: 32rpx; font-weight: 500; }
.mobile-data-cards__list { display: flex; flex-direction: column; gap: 14rpx; }
.mobile-data-card { padding: 32rpx; border: 1rpx solid var(--border-color); border-radius: var(--radius-card); background: #fff; }
.mobile-data-card__head { display: flex; align-items: center; gap: 10rpx; color: #1d2129; font-size: 28rpx; font-weight: 500; }
.mobile-data-card__branch { color: #86909c; }
.mobile-data-card__fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14rpx 18rpx; margin-top: 16rpx; }
.mobile-data-card__field { display: flex; min-width: 0; flex-direction: column; gap: 4rpx; }
.mobile-data-card__field text:first-child { color: #86909c; font-size: 20rpx; }
.mobile-data-card__field text:last-child { overflow: hidden; color: #4e5969; font-size: 23rpx; text-overflow: ellipsis; white-space: nowrap; }
.mobile-data-cards__empty { padding: 32rpx; border: 1rpx dashed var(--border-color); border-radius: var(--radius-card); color: #4e5969; font-size: 24rpx; text-align: center; background: #f7f8fa; }

@media (min-width: 1024px) {
  .mobile-data-cards__list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
}
</style>
