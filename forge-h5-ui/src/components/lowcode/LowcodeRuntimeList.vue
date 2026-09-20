<template>
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
  <AiEmpty v-else title="暂无记录" description="点击右上角新增一条记录" />
  <view v-if="total > pageSize" class="runtime-pagination">
    <AiButton size="sm" variant="secondary" :disabled="page <= 1" @click="$emit('change-page', -1)">上一页</AiButton>
    <text>第 {{ page }} / {{ pageCount }} 页</text>
    <AiButton size="sm" variant="secondary" :disabled="page >= pageCount" @click="$emit('change-page', 1)">下一页</AiButton>
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
.runtime-list-head { display: flex; align-items: center; justify-content: space-between; gap: 18rpx; margin-bottom: 18rpx; padding: 22rpx 24rpx; border: 1rpx solid #e7edf5; border-radius: 18rpx; background: #fff; box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, .04); }
.runtime-list-head__actions, .runtime-search-actions, .runtime-actions { display: flex; align-items: center; justify-content: flex-end; gap: 12rpx; }
.runtime-list-head__actions { flex: 0 0 auto; gap: 10rpx; }
.runtime-toolbar__copy { display: flex; flex-direction: column; gap: 6rpx; }
.runtime-toolbar__title { color: var(--text-strong); font-size: 30rpx; font-weight: 850; }
.runtime-toolbar__desc { color: #94a3b8; font-size: 22rpx; }
.runtime-filter-shell { margin-bottom: 18rpx; }
.runtime-filter-summary { display: flex; align-items: center; justify-content: space-between; gap: 18rpx; padding: 18rpx 22rpx; border: 1rpx solid #e7edf5; border-radius: 16rpx; color: #334155; background: #fff; }
.runtime-filter-summary view { display: flex; min-width: 0; flex-direction: column; gap: 5rpx; }
.runtime-filter-summary__title { color: #475569; font-size: 24rpx; font-weight: 750; }
.runtime-filter-summary__desc { overflow: hidden; color: #94a3b8; font-size: 22rpx; text-overflow: ellipsis; white-space: nowrap; }
.runtime-filter-summary__arrow { flex: 0 0 auto; color: #2563eb; font-size: 22rpx; font-weight: 700; }
.runtime-search-card { margin-bottom: 24rpx; padding: 26rpx; border: 1rpx solid #e7edf5; border-radius: 18rpx; background: #fff; box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, .04); }
.runtime-search-actions { padding-top: 6rpx; }
.runtime-record-list { display: flex; flex-direction: column; gap: 18rpx; }
.runtime-record-card { padding: 24rpx; border: 1rpx solid #e7edf5; border-radius: 18rpx; background: #fff; box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, .04); }
.runtime-record-card__head { display: flex; align-items: flex-start; justify-content: space-between; gap: 18rpx; margin-bottom: 18rpx; }
.runtime-record-card__status { padding: 5rpx 12rpx; border-radius: 999rpx; color: #2563eb; font-size: 21rpx; background: #eff6ff; }
.runtime-record-card__grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16rpx 20rpx; }
.runtime-record-card__item { min-width: 0; display: flex; flex-direction: column; gap: 5rpx; }
.runtime-record-card__label { color: #94a3b8; font-size: 21rpx; }
.runtime-record-card__value { overflow: hidden; color: #334155; font-size: 24rpx; text-overflow: ellipsis; white-space: nowrap; }
.runtime-actions { margin-top: 20rpx; }
.runtime-pagination { display: flex; align-items: center; justify-content: center; gap: 18rpx; padding: 28rpx 0; color: #64748b; font-size: 23rpx; }
</style>
