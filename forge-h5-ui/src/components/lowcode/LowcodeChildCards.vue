<template>
  <view v-for="child in children" :key="child.key" class="runtime-child-card">
    <view class="runtime-child-card__head">
      <view>
        <text class="runtime-child-card__title">{{ resolveChildTitle(child) }}</text>
        <text class="runtime-child-card__count">{{ resolveChildSubtitle(child) || `${visibleRows(child).length} 条` }}</text>
      </view>
      <view class="runtime-child-card__tools">
        <AiButton v-for="action in toolbarActions(child)" :key="action.actionCode || action.key" size="sm" variant="secondary" :disabled="action.disabled === true" @click="$emit('toolbar-action', { action, child })">
          {{ action.label || action.actionName || action.actionCode }}
        </AiButton>
        <AiButton v-if="canAdd(child)" size="sm" variant="secondary" @click="$emit('add-row', child)">添加</AiButton>
      </view>
    </view>
    <view v-if="visibleRows(child).length" class="runtime-child-list">
      <view v-for="(row, rowIndex) in visibleRows(child)" :key="String(row.id || rowIndex)" class="runtime-child-row">
        <view class="runtime-child-row__head">
          <text class="runtime-child-row__title">第 {{ rowIndex + 1 }} 条</text>
          <AiButton v-if="canRemove(child, row)" size="sm" variant="danger" @click="$emit('remove-row', { child, index: rows(child).indexOf(row) })">删除</AiButton>
        </view>
        <view class="runtime-child-row__body">
          <LowcodeForm
            :ref="instance => $emit('set-form-ref', { child, row, rowIndex, instance })"
            :fields="child.fields"
            :data="row"
            :dict-options="dictOptions"
            :current-children="childData"
            :readonly="isRowReadonly(child, row)"
            :context="context"
            :field-linkages="fieldLinkages"
            @field-event="payload => $emit('field-event', { child, row, payload })"
          />
        </view>
        <view v-if="rowActions(child, row).length" class="runtime-actions runtime-actions--child">
          <AiButton v-for="action in rowActions(child, row)" :key="action.actionCode || action.key" size="sm" variant="secondary" :disabled="action.disabled === true" @click="$emit('row-action', { action, row, child })">
            {{ action.label || action.actionName || action.actionCode }}
          </AiButton>
        </view>
      </view>
    </view>
    <view v-else class="runtime-child-empty">暂无明细</view>
  </view>
</template>

<script setup>
import AiButton from '@/components/AiButton.vue'
import LowcodeForm from '@/components/lowcode/LowcodeForm.vue'
import {
  actionVisible,
  resolveActionDefinition,
  resolveActionPermission,
  resolveChildRows,
  resolveChildSubtitle,
  resolveChildTitle,
} from '@/utils/lowcode-runtime'

const props = defineProps({
  children: { type: Array, default: () => [] },
  mode: { type: String, default: 'detail' },
  childData: { type: Object, default: () => ({}) },
  dictOptions: { type: Object, default: () => ({}) },
  context: { type: Object, default: () => ({}) },
  fieldLinkages: { type: Array, default: () => [] },
  config: { type: Object, default: () => ({}) },
  permissions: { type: Array, default: () => [] },
})

defineEmits(['toolbar-action', 'add-row', 'remove-row', 'set-form-ref', 'field-event', 'row-action'])

function rows(child) { return resolveChildRows(child, props.childData) }
function visibleRows(child) { return rows(child).filter(row => !isDeletedRow(row)) }
function canAdd(child) {
  if (props.mode === 'detail' || child?.inlineCreateEnabled === false || child?.readonly === true) return false
  return child?.approvalPermissionControlled === true ? child?.allowCreate === true : true
}
function canRemove(child, row) {
  if (props.mode === 'detail' || child?.readonly === true) return false
  if (child?.approvalPermissionControlled !== true) return true
  return hasPersistedRowId(row) ? child?.allowDelete === true : child?.allowCreate === true
}
function isRowReadonly(child, row) {
  if (props.mode === 'detail' || child?.readonly === true) return true
  if (child?.approvalPermissionControlled !== true) return false
  return hasPersistedRowId(row) ? child?.allowUpdate !== true : child?.allowCreate !== true
}
function resolveActions(actions = []) {
  return actions
    .map(action => resolveActionDefinition(props.config, action))
    .map(action => resolveActionPermission(action, props.permissions))
    .filter(Boolean)
}
function toolbarActions(child) { return resolveActions(child.toolbarActions).filter(action => action.actionCode || action.key) }
function rowActions(child, row) { return resolveActions(child.rowActions).filter(action => actionVisible(action, row)) }
function hasPersistedRowId(row = {}) {
  const id = row.id ?? row.ID
  return id !== undefined && id !== null && String(id).trim() !== ''
}
function isDeletedRow(row = {}) {
  const value = row._deleted ?? row.__deleted
  if (typeof value === 'boolean') return value
  return ['true', '1', 'yes', 'y'].includes(String(value || '').trim().toLowerCase())
}
</script>

<style lang="scss" scoped>
.runtime-child-card { margin-bottom: 32rpx; padding: 32rpx; border: 1rpx solid var(--border-color); border-radius: var(--radius-card); background: #fff; }
.runtime-child-card__head { display: flex; align-items: center; justify-content: space-between; gap: 18rpx; margin-bottom: 18rpx; }
.runtime-child-card__head > view { display: flex; align-items: baseline; gap: 12rpx; }
.runtime-child-card__title { color: var(--text-strong); font-size: 32rpx; font-weight: 500; }
.runtime-child-card__count { color: var(--text-muted); font-size: 20rpx; }
.runtime-child-card__tools, .runtime-actions { display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 12rpx; }
.runtime-child-list { display: flex; flex-direction: column; gap: 10rpx; }
.runtime-child-row { padding: 18rpx; border: 1rpx solid var(--border-light); border-radius: var(--radius-control); background: var(--surface-subtle); }
.runtime-child-row__head { display: flex; align-items: center; justify-content: space-between; gap: 12rpx; margin-bottom: 14rpx; }
.runtime-child-row__title { color: var(--text-strong); font-size: 22rpx; font-weight: 500; }
.runtime-child-row__body { padding: 4rpx 0 2rpx; }
.runtime-actions--child { justify-content: flex-start; margin-top: 16rpx; }
.runtime-child-empty { padding: 26rpx 0; color: var(--text-muted); font-size: 22rpx; text-align: center; }

@media (min-width: 1024px) {
  .runtime-child-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
}
</style>
