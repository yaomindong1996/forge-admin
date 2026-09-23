<template>
  <view class="section-runtime" :class="{ 'section-runtime--with-bottom-bar': visibleBottomActions.length }">
    <template v-for="section in visibleSections" :key="section.sectionId">
      <CardSection
        v-if="section.sectionType === 'card'"
        :title="section.title"
        :collapsible="section.collapsible === true"
        :collapsed-by-default="section.collapsedByDefault === true"
      >
        <LowcodeForm
          :ref="instance => setMainFormRef(section, instance)"
          :fields="resolveFields(section)"
          :data="mainData"
          :dict-options="dictOptions"
          :readonly="mode === 'detail' || sectionReadonly(section)"
          :context="runtimeContext"
          :field-linkages="fieldLinkages"
          @field-event="payload => emit('main-field-event', payload)"
        />
      </CardSection>

      <CardSection
        v-else-if="isChildSection(section, 'inline_grid')"
        :title="section.title"
        :collapsible="section.collapsible === true"
        :collapsed-by-default="section.collapsedByDefault === true"
      >
        <view class="section-child-head">
          <text class="section-child-count">{{ visibleChildRows(section).length }} 条</text>
          <view class="section-child-head__tools">
            <AiButton
              v-for="action in visibleChildToolbarActions(section)"
              :key="'toolbar-' + (action.actionCode || action.key)"
              size="sm"
              variant="secondary"
              :disabled="action.disabled === true"
              @click="emit('child-toolbar-action', { action, child: childConfig(section) })"
            >
              {{ action.label || action.actionName || action.actionCode || action.key }}
            </AiButton>
            <AiButton
              v-if="canAddChild(section)"
              size="sm"
              variant="secondary"
              @click="emit('add-child-row', childConfig(section))"
            >
              添加
            </AiButton>
          </view>
        </view>
        <view v-if="visibleChildRows(section).length" class="section-child-list">
          <view
            v-for="(row, rowIndex) in visibleChildRows(section)"
            :key="childRowKey(section, row, rowIndex)"
            class="section-child-row"
          >
            <view class="section-child-row__head">
              <text class="section-child-row__title">第 {{ rowIndex + 1 }} 条</text>
              <AiButton
                v-if="canRemoveChild(section, row)"
                size="sm"
                variant="danger"
                @click="removeChild(section, row)"
              >
                删除
              </AiButton>
            </view>
            <LowcodeForm
              :ref="instance => setChildFormRef(section, row, rowIndex, instance)"
              :fields="childConfig(section).fields"
              :data="row"
              :dict-options="dictOptions"
              :current-children="childData"
              :readonly="isChildReadonly(section, row)"
              :context="runtimeContext"
              :field-linkages="fieldLinkages"
              layout="inline_grid"
              @field-event="payload => childFieldEvent(section, row, payload)"
            />
            <view v-if="visibleChildActions(section, row).length" class="section-row-actions">
              <AiButton
                v-for="action in visibleChildActions(section, row)"
                :key="action.actionCode || action.key"
                size="sm"
                variant="secondary"
                :disabled="action.disabled === true"
                @click="emit('child-action', { action, row, child: childConfig(section) })"
              >
                {{ action.label || action.actionName || action.actionCode || action.key }}
              </AiButton>
            </view>
          </view>
        </view>
        <view v-else class="section-empty">暂无明细</view>
      </CardSection>

      <CardSection
        v-else-if="isChildSection(section, 'card_list')"
        :title="section.title"
        :collapsible="section.collapsible === true"
        :collapsed-by-default="section.collapsedByDefault === true"
      >
        <view class="section-child-head">
          <text class="section-child-count">{{ visibleChildRows(section).length }} 条</text>
          <view class="section-child-head__tools">
            <AiButton
              v-for="action in visibleChildToolbarActions(section)"
              :key="'toolbar-' + (action.actionCode || action.key)"
              size="sm"
              variant="secondary"
              :disabled="action.disabled === true"
              @click="emit('child-toolbar-action', { action, child: childConfig(section) })"
            >
              {{ action.label || action.actionName || action.actionCode || action.key }}
            </AiButton>
            <AiButton
              v-if="canAddChild(section)"
              size="sm"
              variant="secondary"
              @click="emit('add-child-row', childConfig(section))"
            >
              添加
            </AiButton>
          </view>
        </view>
        <view v-if="visibleChildRows(section).length" class="section-card-list">
          <view
            v-for="(row, rowIndex) in visibleChildRows(section)"
            :key="childRowKey(section, row, rowIndex)"
            class="section-card-row"
          >
            <view class="section-child-row__head">
              <text class="section-child-row__title">第 {{ rowIndex + 1 }} 条</text>
              <AiButton
                v-if="canRemoveChild(section, row)"
                size="sm"
                variant="danger"
                @click="removeChild(section, row)"
              >
                删除
              </AiButton>
            </view>
            <LowcodeForm
              :ref="instance => setChildFormRef(section, row, rowIndex, instance)"
              :fields="childConfig(section).fields"
              :data="row"
              :dict-options="dictOptions"
              :current-children="childData"
              :readonly="isChildReadonly(section, row)"
              :context="runtimeContext"
              :field-linkages="fieldLinkages"
              @field-event="payload => childFieldEvent(section, row, payload)"
            />
            <view v-if="visibleChildActions(section, row).length" class="section-row-actions">
              <AiButton
                v-for="action in visibleChildActions(section, row)"
                :key="action.actionCode || action.key"
                size="sm"
                variant="secondary"
                :disabled="action.disabled === true"
                @click="emit('child-action', { action, row, child: childConfig(section) })"
              >
                {{ action.label || action.actionName || action.actionCode || action.key }}
              </AiButton>
            </view>
          </view>
        </view>
        <view v-else class="section-empty">暂无明细</view>
      </CardSection>

      <CardSection
        v-else-if="isChildSection(section, 'bottom_sheet')"
        :title="section.title"
        :collapsible="section.collapsible === true"
        :collapsed-by-default="section.collapsedByDefault === true"
      >
        <view v-if="visibleChildToolbarActions(section).length" class="section-child-head">
          <text class="section-child-count">{{ visibleChildRows(section).length }} 条</text>
          <view class="section-child-head__tools">
            <AiButton
              v-for="action in visibleChildToolbarActions(section)"
              :key="'toolbar-' + (action.actionCode || action.key)"
              size="sm"
              variant="secondary"
              :disabled="action.disabled === true"
              @click="emit('child-toolbar-action', { action, child: childConfig(section) })"
            >
              {{ action.label || action.actionName || action.actionCode || action.key }}
            </AiButton>
          </view>
        </view>
        <button class="section-sheet-trigger" hover-class="section-sheet-trigger--pressed" @click="openSheet(section)">
          <view class="section-sheet-trigger__copy">
            <text class="section-sheet-trigger__label">查看{{ section.title || '明细' }}</text>
            <text class="section-sheet-trigger__count">{{ visibleChildRows(section).length }} 条记录</text>
          </view>
          <AiIcon name="chevron-right" color="#4266f7" size="sm" />
        </button>
      </CardSection>
    </template>

    <BottomSheet v-model:visible="sheetVisible" :title="activeSheetSection?.title || '明细'">
      <view v-if="activeSheetRows.length" class="section-sheet-list">
        <view v-for="(row, rowIndex) in activeSheetRows" :key="String(row.id || rowIndex)" class="section-sheet-row">
          <text class="section-sheet-row__title">第 {{ rowIndex + 1 }} 条</text>
          <view
            v-for="field in activeSheetChild?.fields || []"
            :key="field.field"
            class="section-readonly-field"
          >
            <text class="section-readonly-field__label">{{ field.label }}</text>
            <text class="section-readonly-field__value">{{ formatFieldValue(row[field.field], field) }}</text>
          </view>
        </view>
      </view>
      <view v-else class="section-empty section-empty--sheet">暂无记录</view>
    </BottomSheet>

    <view v-if="visibleBottomActions.length && !sheetVisible" class="section-bottom-bar">
      <AiButton
        v-for="(action, index) in visibleBottomActions"
        :key="actionKey(action, index)"
        :variant="buttonVariant(action.variant)"
        :loading="bottomActionLoading === actionKey(action, index)"
        :disabled="action.disabled === true || (Boolean(bottomActionLoading) && bottomActionLoading !== actionKey(action, index))"
        @click="emit('bottom-action', action)"
      >
        {{ action.label }}
      </AiButton>
    </view>
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import AiButton from '@/components/AiButton.vue'
import AiIcon from '@/components/AiIcon.vue'
import BottomSheet from './BottomSheet.vue'
import CardSection from './CardSection.vue'
import LowcodeForm from './LowcodeForm.vue'
import {
  actionVisible,
  resolveActionPermission,
  resolveBottomBarActions,
  resolveChildRows,
  resolvePageSectionChild,
  resolvePageSectionFields,
  resolveVisiblePageSections,
  isPageSectionReadonly,
} from '@/utils/lowcode-runtime'

const props = defineProps({
  sections: { type: Array, default: () => [] },
  mainFields: { type: Array, default: () => [] },
  mainData: { type: Object, default: () => ({}) },
  children: { type: Array, default: () => [] },
  childData: { type: Object, default: () => ({}) },
  mode: { type: String, default: 'create' },
  dictOptions: { type: Object, default: () => ({}) },
  runtimeContext: { type: Object, default: () => ({}) },
  bottomBar: { type: Object, default: null },
  bottomActionLoading: { type: String, default: '' },
  permissions: { type: Array, default: () => [] },
  fieldLinkages: { type: Array, default: () => [] },
  flowInteraction: { type: Object, default: () => ({}) },
  currentFlowNodeKey: { type: String, default: '' },
})

const emit = defineEmits([
  'main-field-event',
  'child-field-event',
  'add-child-row',
  'remove-child-row',
  'set-child-form-ref',
  'set-main-form-ref',
  'bottom-action',
  'child-action',
  'child-toolbar-action',
])

const sheetVisible = ref(false)
const activeSheetSectionId = ref('')
const visibleSections = computed(() => resolveVisiblePageSections(
  props.sections,
  props.mode,
  props.flowInteraction,
  props.currentFlowNodeKey,
))
const visibleBottomActions = computed(() => resolveBottomBarActions(props.bottomBar, props.mainData, props.mode, props.permissions))
const activeSheetSection = computed(() => visibleSections.value
  .find(section => String(section.sectionId) === activeSheetSectionId.value) || null)
const activeSheetChild = computed(() => childConfig(activeSheetSection.value))
const activeSheetRows = computed(() => activeSheetSection.value ? visibleChildRows(activeSheetSection.value) : [])

watch(visibleSections, () => {
  if (activeSheetSectionId.value && !activeSheetSection.value)
    sheetVisible.value = false
})

function resolveFields(section) {
  return resolvePageSectionFields(section, props.mainFields)
}

function childConfig(section) {
  return section ? resolvePageSectionChild(section, props.children) : null
}

function childRows(section) {
  const child = childConfig(section)
  return child ? resolveChildRows(child, props.childData) : []
}

function visibleChildRows(section) {
  return childRows(section).filter(row => !isDeletedRow(row))
}

function isChildSection(section, displayMode) {
  return section.sectionType === 'child_table'
    && String(section.displayMode || 'card_list') === displayMode
    && Boolean(childConfig(section))
}

function canAddChild(section) {
  const child = childConfig(section)
  if (props.mode === 'detail' || child?.readonly === true || child?.inlineCreateEnabled === false) return false
  return child?.approvalPermissionControlled === true ? child?.allowCreate === true : true
}

function canRemoveChild(section, row) {
  const child = childConfig(section)
  if (props.mode === 'detail' || child?.readonly === true || child?.inlineEditEnabled === false) return false
  if (child?.approvalPermissionControlled !== true) return true
  return hasPersistedRowId(row) ? child?.allowDelete === true : child?.allowCreate === true
}

function isChildReadonly(section, row) {
  const child = childConfig(section)
  if (props.mode === 'detail' || child?.readonly === true || sectionReadonly(section)) return true
  if (child?.approvalPermissionControlled !== true) return false
  return hasPersistedRowId(row) ? child?.allowUpdate !== true : child?.allowCreate !== true
}

function sectionReadonly(section) {
  return isPageSectionReadonly(section, props.flowInteraction, props.currentFlowNodeKey)
}

function visibleChildActions(section, row) {
  return (childConfig(section)?.rowActions || [])
    .filter(action => actionVisible(action, row))
    .map(action => resolveActionPermission(action, props.permissions))
    .filter(Boolean)
}

function visibleChildToolbarActions(section) {
  return (childConfig(section)?.toolbarActions || [])
    .filter(action => action?.visible !== false)
    .map(action => resolveActionPermission(action, props.permissions))
    .filter(Boolean)
}

function setMainFormRef(section, instance) {
  emit('set-main-form-ref', { sectionId: String(section.sectionId || ''), instance })
}

function setChildFormRef(section, row, rowIndex, instance) {
  emit('set-child-form-ref', { child: childConfig(section), row, rowIndex, instance })
}

function childFieldEvent(section, row, payload) {
  emit('child-field-event', { child: childConfig(section), row, payload })
}

function removeChild(section, row) {
  const index = childRows(section).indexOf(row)
  if (index >= 0) emit('remove-child-row', { child: childConfig(section), index })
}

function openSheet(section) {
  activeSheetSectionId.value = String(section.sectionId || '')
  sheetVisible.value = true
}

function childRowKey(section, row, index) {
  return `${section.sectionId}:${row?.id || index}`
}

function formatFieldValue(value, field) {
  if (value === undefined || value === null || value === '')
    return '-'
  const dictType = field.dictType || field.props?.dictType
  if (dictType) {
    const option = props.dictOptions[dictType]?.find(item => String(item.value) === String(value))
    return option?.label || String(value)
  }
  return String(value)
}

function buttonVariant(variant) {
  return ['primary', 'secondary', 'danger'].includes(variant) ? variant : 'secondary'
}

function actionKey(action, index) {
  return `${action.type}:${action.actionCode || action.label || index}`
}

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
.section-runtime {
  min-width: 0;
}

.section-runtime--with-bottom-bar {
  padding-bottom: 220rpx;
}

.section-child-head,
.section-child-row__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.section-child-head {
  margin-bottom: 16rpx;
}

.section-child-head__tools {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
}

.section-child-count {
  color: #4e5969;
  font-size: 23rpx;
  font-weight: 500;
}

.section-child-list,
.section-card-list,
.section-sheet-list {
  display: flex;
  flex-direction: column;
}

.section-child-row,
.section-card-row,
.section-sheet-row {
  min-width: 0;
  padding: 22rpx 0;
  border-top: 1rpx solid #e5e6eb;
}

.section-child-row:first-child,
.section-card-row:first-child,
.section-sheet-row:first-child {
  padding-top: 4rpx;
  border-top: 0;
}

.section-child-row:last-child,
.section-card-row:last-child,
.section-sheet-row:last-child {
  padding-bottom: 0;
}

.section-child-row__head {
  margin-bottom: 16rpx;
}

.section-child-row__title,
.section-sheet-row__title {
  display: block;
  color: #1d2129;
  font-size: 24rpx;
  font-weight: 500;
}

.section-row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 16rpx;
}

.section-empty {
  padding: 30rpx 0 12rpx;
  color: #86909c;
  font-size: 24rpx;
  text-align: center;
}

.section-empty--sheet {
  padding: 56rpx 0;
}

.section-sheet-trigger {
  display: flex;
  width: 100%;
  min-height: 88rpx;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin: 0;
  padding: 12rpx 0;
  border: 0;
  color: inherit;
  text-align: left;
  background: transparent;
  box-sizing: border-box;
}

.section-sheet-trigger::after {
  border: 0;
}

.section-sheet-trigger--pressed {
  opacity: 0.72;
}

.section-sheet-trigger__copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 6rpx;
}

.section-sheet-trigger__label {
  color: #1d2129;
  font-size: 26rpx;
  font-weight: 500;
}

.section-sheet-trigger__count {
  color: #86909c;
  font-size: 22rpx;
}

.section-readonly-field {
  display: grid;
  grid-template-columns: minmax(140rpx, 0.75fr) minmax(0, 1.25fr);
  gap: 18rpx;
  margin-top: 14rpx;
}

.section-readonly-field__label {
  color: #86909c;
  font-size: 22rpx;
}

.section-readonly-field__value {
  min-width: 0;
  color: #1d2129;
  font-size: 24rpx;
  line-height: 1.45;
  overflow-wrap: anywhere;
  text-align: right;
}

.section-bottom-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 100;
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  padding: 16rpx 32rpx calc(16rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #e5e6eb;
  background: #fff;
}

.section-bottom-bar > * {
  flex: 1;
}

.section-bottom-bar :deep(.ai-button--primary) {
  order: -1;
  flex: 1 0 100%;
}

@media (min-width: 1024px) {
  .section-runtime--with-bottom-bar { padding-bottom: 112rpx; }
  .section-bottom-bar {
    justify-content: flex-end;
    padding-right: max(24px, calc((100vw - 1280px) / 2 + 24px));
    padding-left: max(24px, calc((100vw - 1280px) / 2 + 24px));
  }
  .section-bottom-bar > *,
  .section-bottom-bar :deep(.ai-button--primary) { flex: 0 0 auto; order: initial; min-width: 128px; }
}
</style>
