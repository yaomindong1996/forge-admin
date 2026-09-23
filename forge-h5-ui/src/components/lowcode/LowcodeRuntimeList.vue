<template>
  <view class="runtime-list-workspace" :class="{ 'runtime-list-workspace--without-filter': !searchFields.length }">
  <view class="runtime-list-head">
    <view class="runtime-toolbar__copy">
      <text class="runtime-toolbar__title">{{ config.tableComment || config.objectName || title }}</text>
      <text class="runtime-toolbar__desc">共 {{ total }} 条记录，按卡片浏览和办理</text>
    </view>
    <view class="runtime-list-head__actions">
      <AiButton v-if="searchFields.length" size="sm" variant="secondary" @click="$emit('toggle-search')">
        {{ searchExpanded ? '收起筛选' : `筛选${activeSearchCount ? `(${activeSearchCount})` : ''}` }}
      </AiButton>
      <AiButton size="sm" @click="$emit('create')">新增</AiButton>
    </view>
  </view>

  <view v-if="searchFields.length" class="runtime-filter-shell">
    <view v-if="!searchExpanded" class="runtime-filter-summary" @click="$emit('toggle-search')">
      <view><text class="runtime-filter-summary__title">筛选条件</text><text class="runtime-filter-summary__desc">{{ searchSummary }}</text></view>
      <text class="runtime-filter-summary__arrow">展开</text>
    </view>
    <view v-else class="runtime-search-card">
      <LowcodeForm :fields="searchFields" :data="searchData" :dict-options="dictOptions" :context="context" @update:data="$emit('search-change')" />
      <view class="runtime-search-actions">
        <AiButton size="sm" variant="secondary" @click="$emit('reset-search')">重置</AiButton>
        <AiButton size="sm" @click="$emit('search')">查询</AiButton>
      </view>
    </view>
  </view>

  <view v-if="records.length" class="runtime-record-list">
    <view v-for="row in records" :key="String(row[config.rowKey || 'id'])" class="runtime-record-card" @click="$emit('open-row', row)">
      <view class="runtime-record-card__head">
        <text class="runtime-record-card__title">{{ rowTitle(row) }}</text>
        <text v-if="displayStatus(row)" class="runtime-record-card__status">{{ displayStatus(row) }}</text>
      </view>
      <view class="runtime-record-card__grid">
        <view v-for="column in columns" :key="column.prop || column.field" class="runtime-record-card__item">
          <text class="runtime-record-card__label">{{ column.label || column.title || column.prop }}</text>
          <text class="runtime-record-card__value">{{ formatValue(row[column.prop || column.field], column) }}</text>
        </view>
      </view>
      <view v-if="actionsFor(row).length" class="runtime-actions" @click.stop>
        <AiButton v-for="action in actionsFor(row)" :key="action.actionCode || action.key" size="sm" variant="secondary" :disabled="action.disabled === true" @click="$emit('action', action, row)">
          {{ action.label || action.actionName || action.actionCode }}
        </AiButton>
      </view>
    </view>
  </view>
  <view v-else class="runtime-list-empty"><AiEmpty title="暂无记录" description="点击右上角新增一条记录" /></view>
  <view v-if="total > pageSize" class="runtime-pagination">
    <AiButton size="sm" variant="secondary" :disabled="page <= 1" @click="$emit('change-page', -1)">上一页</AiButton>
    <text>第 {{ page }} / {{ pageCount }} 页</text>
    <AiButton size="sm" variant="secondary" :disabled="page >= pageCount" @click="$emit('change-page', 1)">下一页</AiButton>
  </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import AiButton from '@/components/AiButton.vue'
import AiEmpty from '@/components/AiEmpty.vue'
import LowcodeForm from '@/components/lowcode/LowcodeForm.vue'
import { actionVisible, normalizeActions, resolveActionPermission } from '@/utils/lowcode-runtime'

const props = defineProps({
  config: { type: Object, default: () => ({}) },
  title: { type: String, default: '' },
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  searchExpanded: { type: Boolean, default: false },
  searchFields: { type: Array, default: () => [] },
  searchData: { type: Object, default: () => ({}) },
  records: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  dictOptions: { type: Object, default: () => ({}) },
  context: { type: Object, default: () => ({}) },
  permissions: { type: Array, default: () => [] },
})

defineEmits(['toggle-search', 'create', 'search-change', 'reset-search', 'search', 'open-row', 'action', 'change-page'])

const pageCount = computed(() => Math.max(1, Math.ceil(Number(props.total || 0) / props.pageSize)))
const activeSearchCount = computed(() => Object.values(props.searchData).filter(value => value !== undefined && value !== null && value !== '' && !(Array.isArray(value) && !value.length)).length)
const searchSummary = computed(() => activeSearchCount.value ? `已设置 ${activeSearchCount.value} 个条件` : '默认收起，点击后输入条件')

function actionsFor(row) {
  return normalizeActions(props.config)
    .filter(action => !action.relationKey && actionVisible(action, row))
    .map(action => resolveActionPermission(action, props.permissions))
    .filter(Boolean)
}
function rowTitle(row) {
  const titleField = props.config?.options?.titleField
  if (titleField && row[titleField]) return String(row[titleField])
  const firstCol = props.columns[0]
  const value = firstCol && row[firstCol.prop || firstCol.field]
  return value !== undefined && value !== null && value !== ''
    ? String(value)
    : row[props.config.rowKey || 'id'] || props.config.objectName || '记录'
}
function displayStatus(row) {
  const field = props.config?.options?.statusField
  const value = field ? row[field] : ''
  if (value === undefined || value === null || value === '') return ''
  const dictType = props.config?.options?.statusDictType
  return dictType ? props.dictOptions[dictType]?.find(item => String(item.value) === String(value))?.label || String(value) : String(value)
}
function formatValue(value, column = {}) {
  if (value === undefined || value === null || value === '') return '-'
  return column.dictType ? props.dictOptions[column.dictType]?.find(item => String(item.value) === String(value))?.label || value : String(value)
}
</script>

<style lang="scss" scoped>
.runtime-list-workspace { min-width: 0; }
.runtime-list-head { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; margin-bottom: 14rpx; padding: 18rpx 20rpx; border: 1rpx solid var(--border-color); border-radius: var(--radius-card); background: #fff; }
.runtime-list-head__actions, .runtime-search-actions, .runtime-actions { display: flex; align-items: center; justify-content: flex-end; gap: 12rpx; }
.runtime-list-head__actions { flex: 0 0 auto; gap: 10rpx; }
.runtime-toolbar__copy { display: flex; flex-direction: column; gap: 4rpx; }
.runtime-toolbar__title { color: var(--text-strong); font-size: 32rpx; font-weight: 500; }
.runtime-toolbar__desc { color: var(--text-muted); font-size: 20rpx; }
.runtime-filter-shell { margin-bottom: 14rpx; }
.runtime-filter-summary { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; padding: 16rpx 18rpx; border: 1rpx solid var(--border-color); border-radius: var(--radius-card); color: var(--text-secondary); background: #fff; }
.runtime-filter-summary view { display: flex; min-width: 0; flex-direction: column; gap: 5rpx; }
.runtime-filter-summary__title { color: var(--text-secondary); font-size: 26rpx; font-weight: 500; }
.runtime-filter-summary__desc { overflow: hidden; color: var(--text-muted); font-size: 20rpx; text-overflow: ellipsis; white-space: nowrap; }
.runtime-filter-summary__arrow { flex: 0 0 auto; color: var(--primary-color); font-size: 24rpx; font-weight: 500; }
.runtime-search-card { margin-bottom: 16rpx; padding: 22rpx; border: 1rpx solid var(--border-color); border-radius: var(--radius-card); background: #fff; }
.runtime-search-actions { padding-top: 6rpx; }
.runtime-record-list { display: flex; flex-direction: column; gap: 10rpx; }
.runtime-record-card { padding: 18rpx 20rpx; border: 1rpx solid var(--border-color); border-radius: var(--radius-card); background: #fff; }
.runtime-record-card__head { display: flex; align-items: flex-start; justify-content: space-between; gap: 18rpx; margin-bottom: 18rpx; }
.runtime-record-card__status { padding: 4rpx 8rpx; border: 1rpx solid #4266f7; border-radius: 4rpx; color: var(--primary-color); font-size: 19rpx; background: var(--primary-soft); }
.runtime-record-card__grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16rpx 20rpx; }
.runtime-record-card__item { min-width: 0; display: flex; flex-direction: column; gap: 5rpx; }
.runtime-record-card__label { color: var(--text-muted); font-size: 19rpx; }
.runtime-record-card__value { overflow: hidden; color: var(--text-secondary); font-size: 22rpx; text-overflow: ellipsis; white-space: nowrap; }
.runtime-actions { margin-top: 20rpx; }
.runtime-pagination { display: flex; align-items: center; justify-content: center; gap: 16rpx; padding: 24rpx 0; color: var(--text-secondary); font-size: 21rpx; }

@media (hover: hover) {
  .runtime-record-card:hover,
  .runtime-filter-summary:hover { background: var(--surface-muted); }
}

@media (min-width: 1024px) {
  .runtime-list-workspace {
    display: grid;
    grid-template-areas:
      "head head"
      "filters records"
      "filters pagination";
    grid-template-columns: var(--forge-aside-width) minmax(0, 1fr);
    grid-template-rows: auto minmax(0, 1fr) auto;
    gap: 24px;
  }

  .runtime-list-workspace--without-filter {
    grid-template-areas:
      "head"
      "records"
      "pagination";
    grid-template-columns: minmax(0, 1fr);
  }

  .runtime-list-head { grid-area: head; margin-bottom: 0; padding: 20px 24px; }
  .runtime-filter-shell { grid-area: filters; margin-bottom: 0; }
  .runtime-record-list,
  .runtime-list-empty { grid-area: records; }
  .runtime-pagination { grid-area: pagination; }
  .runtime-search-card { margin-bottom: 0; padding: 24px; }
  .runtime-record-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; align-content: start; }
  .runtime-record-card { padding: 20px 24px; }
}
</style>
