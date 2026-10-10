<template>
  <view class="runtime-list-workspace" :class="{ 'runtime-list-workspace--without-filter': searchFields.length !== 1 }">
  <view class="runtime-list-head">
    <view class="runtime-toolbar__copy">
      <text class="runtime-toolbar__title">{{ config.tableComment || config.objectName || title }}</text>
      <text class="runtime-toolbar__desc">共 {{ total }} 条记录，按卡片浏览和办理</text>
    </view>
    <view class="runtime-list-head__actions">
      <AiButton v-if="searchFields.length" size="sm" variant="secondary" @click="searchFields.length > 1 ? openFilter() : $emit('toggle-search')">
        {{ searchFields.length > 1 ? `筛选${activeSearchCount ? `(${activeSearchCount})` : ''}` : searchExpanded ? '收起筛选' : '筛选' }}
      </AiButton>
      <AiButton size="sm" @click="$emit('create')">新增</AiButton>
    </view>
  </view>

  <view v-if="searchFields.length === 1" class="runtime-filter-shell">
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
  <!-- 多条件查询只在弹层中编辑草稿，确认时才同步到运行时查询状态。 -->
  <AiFilterSheet v-if="searchFields.length > 1" v-model="filterVisible" title="筛选记录" @reset="resetFilterDraft" @apply="applyFilterDraft">
    <LowcodeForm :fields="searchFields" :data="draftSearchData" :dict-options="dictOptions" :context="context" />
  </AiFilterSheet>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import AiButton from '@/components/AiButton.vue'
import AiEmpty from '@/components/AiEmpty.vue'
import AiFilterSheet from '@/components/AiFilterSheet.vue'
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

const emit = defineEmits(['toggle-search', 'create', 'search-change', 'reset-search', 'search', 'open-row', 'action', 'change-page'])

const filterVisible = ref(false)
const draftSearchData = reactive({})
function openFilter() {
  Object.keys(draftSearchData).forEach(key => delete draftSearchData[key])
  Object.assign(draftSearchData, props.searchData)
  filterVisible.value = true
}
function resetFilterDraft() {
  Object.keys(draftSearchData).forEach(key => delete draftSearchData[key])
}
function applyFilterDraft() {
  Object.keys(props.searchData).forEach(key => delete props.searchData[key])
  Object.assign(props.searchData, draftSearchData)
  filterVisible.value = false
  emit('search')
}

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
.runtime-list-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; padding: 14px 16px; border: 0; border-radius: var(--forge-radius-card); background: var(--forge-surface); }
.runtime-list-head__actions, .runtime-search-actions, .runtime-actions { display: flex; align-items: center; justify-content: flex-end; gap: 12rpx; }
.runtime-list-head__actions { flex: 0 0 auto; gap: 10rpx; }
.runtime-toolbar__copy { display: flex; flex-direction: column; gap: 4rpx; }
.runtime-toolbar__title { color: var(--forge-text-primary); font-size: 17px; font-weight: 600; }
.runtime-toolbar__desc { color: var(--forge-text-tertiary); font-size: 12px; }
.runtime-filter-shell { margin-bottom: 12px; }
.runtime-filter-summary { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 16px; border: 0; border-radius: var(--forge-radius-card); color: var(--forge-text-secondary); background: var(--forge-surface); }
.runtime-filter-summary view { display: flex; min-width: 0; flex-direction: column; gap: 2px; }
.runtime-filter-summary__title { color: var(--forge-text-primary); font-size: 15px; font-weight: 400; }
.runtime-filter-summary__desc { overflow: hidden; color: var(--forge-text-tertiary); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.runtime-filter-summary__arrow { flex: 0 0 auto; color: var(--forge-color-primary); font-size: 14px; font-weight: 400; }
.runtime-search-card { margin-bottom: 12px; padding: 16px; border: 0; border-radius: var(--forge-radius-card); background: var(--forge-surface); }
.runtime-search-actions { padding-top: 4px; }
.runtime-record-list { display: flex; flex-direction: column; gap: 8px; }
.runtime-record-card { padding: 14px 16px; border: 0; border-radius: var(--forge-radius-card); background: var(--forge-surface); }
.runtime-record-card__head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.runtime-record-card__status { padding: 2px 6px; border: 0; border-radius: 4px; color: var(--forge-color-primary); font-size: 12px; background: var(--forge-color-primary-soft); }
.runtime-record-card__grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px 16px; }
.runtime-record-card__item { min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.runtime-record-card__label { color: var(--forge-text-tertiary); font-size: 12px; }
.runtime-record-card__value { overflow: hidden; color: var(--forge-text-primary); font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.runtime-actions { margin-top: 12px; }
.runtime-pagination { display: flex; align-items: center; justify-content: center; gap: 12px; padding: 16px 0; color: var(--forge-text-secondary); font-size: 13px; }

@media (hover: hover) {
  .runtime-record-card:hover,
  .runtime-filter-summary:hover { background: var(--forge-surface-subtle); }
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
