<template>
  <div class="forge-field-shelf">
    <div class="shelf-head">
      <div>
        <h3>字段与布局</h3>
        <p>拖入画布或点击添加。</p>
      </div>
    </div>

    <n-input
      v-model:value="keyword"
      size="small"
      clearable
      :placeholder="activeShelfTab === 'components' ? '搜索组件' : '搜索字段'"
      class="shelf-search"
    />

    <div class="shelf-mode-tabs">
      <button type="button" :class="{ active: activeShelfTab === 'components' }" @click="activeShelfTab = 'components'">
        组件库 {{ componentTotal }}
      </button>
      <button type="button" :class="{ active: activeShelfTab === 'fields' }" @click="activeShelfTab = 'fields'">
        字段资产 {{ props.fields.length }}
      </button>
    </div>

    <div v-if="activeShelfTab === 'components'" class="component-palette">
      <div v-for="group in visibleTemplateGroups" :key="group.title" class="palette-section">
        <h4>
          <span>{{ group.title }}</span>
          <em>{{ group.items.length }}</em>
        </h4>
        <div class="palette-grid">
          <button
            v-for="item in group.items"
            :key="item.componentKey"
            type="button"
            class="palette-item"
            :class="{ dragging: draggingKey === `template:${item.componentKey}` }"
            draggable="true"
            @dragstart="handleTemplateDragStart($event, item)"
            @dragend="finishDragging"
          >
            <n-icon><component :is="item.icon" /></n-icon>
            <span>{{ item.label }}</span>
          </button>
        </div>
      </div>
      <div v-if="visibleLayoutItems.length" class="palette-section">
        <h4>
          <span>布局与业务区块</span>
          <em>{{ visibleLayoutItems.length }}</em>
        </h4>
        <div class="palette-grid">
          <button
            v-for="item in visibleLayoutItems"
            :key="item.componentKey"
            type="button"
            class="palette-item"
            :class="{ dragging: draggingKey === `layout:${item.componentKey}` }"
            draggable="true"
            @dragstart="handleLayoutDragStart($event, item)"
            @dragend="finishDragging"
          >
            <n-icon><component :is="item.icon" /></n-icon>
            <span>{{ item.label }}</span>
          </button>
        </div>
      </div>
      <n-empty v-if="!componentTotal" size="small" description="没有匹配组件" />
    </div>

    <div v-else class="field-asset-panel">
      <div class="field-tabs">
        <button type="button" :class="{ active: activeTab === 'unused' }" @click="activeTab = 'unused'">
          未使用 {{ unusedFields.length }}
        </button>
        <button type="button" :class="{ active: activeTab === 'used' }" @click="activeTab = 'used'">
          已使用 {{ usedFields.length }}
        </button>
        <button type="button" :class="{ active: activeTab === 'system' }" @click="activeTab = 'system'">
          系统 {{ systemFields.length }}
        </button>
      </div>

      <div class="field-list">
        <button
          v-for="field in visibleFields"
          :key="field.field || field.fieldCode"
          type="button"
          class="field-item"
          :class="{ used: isUsed(field), locked: isLocked(field), dragging: draggingKey === `field:${field.field || field.fieldCode}` }"
          :disabled="isUsed(field) || isLocked(field)"
          :draggable="!isUsed(field) && !isLocked(field)"
          @click="$emit('appendField', field)"
          @dragstart="handleFieldDragStart($event, field)"
          @dragend="finishDragging"
        >
          <span class="drag-handle" aria-hidden="true">
            <svg
              width="1em"
              height="1em"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              data-icon="DragOutlined"
            >
              <path
                d="M8.25 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm0 7.25a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm1.75 5.5a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0ZM14.753 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5ZM16.5 12a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0Zm-1.747 9a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Z"
                fill="currentColor"
              />
            </svg>
          </span>
          <span class="field-main">
            <strong>{{ field.label || field.fieldName || field.field }}</strong>
            <small>{{ field.field || field.fieldCode }} · {{ field.componentType || field.dataType || 'input' }}</small>
          </span>
          <em v-if="isUsed(field)">已用</em>
          <em v-else-if="isLocked(field)">系统</em>
        </button>
        <n-empty v-if="!visibleFields.length" size="small" description="没有匹配字段" />
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  AlbumsOutline,
  BrowsersOutline,
  CheckboxOutline,
  ChevronDownCircleOutline,
  CloudUploadOutline,
  GridOutline,
  ImageOutline,
  ListOutline,
  PersonOutline,
  ReorderThreeOutline,
  TextOutline,
  TimeOutline,
  ToggleOutline,
} from '@vicons/ionicons5'
import { computed, ref } from 'vue'
import { isReadonlySystemField } from '@/components/lowcode-builder/page/page-schema'
import { clearDesignerDragSource, clearDesignerDropKey } from './designerDragState'

const props = defineProps({
  fields: {
    type: Array,
    default: () => [],
  },
  usedFieldSet: {
    type: Object,
    default: () => new Set(),
  },
})

defineEmits(['appendField'])

const keyword = ref('')
const activeShelfTab = ref('components')
const activeTab = ref('unused')
const draggingKey = ref('')
const fieldTemplateGroups = [
  {
    title: '输入',
    items: [
      { componentKey: 'input', label: '输入框', icon: TextOutline },
      { componentKey: 'textarea', label: '多行文本', icon: TextOutline },
      { componentKey: 'number', label: '数字', icon: ReorderThreeOutline },
      { componentKey: 'money', label: '金额', icon: ReorderThreeOutline },
      { componentKey: 'slider', label: '滑块', icon: ReorderThreeOutline },
      { componentKey: 'rate', label: '评分', icon: ReorderThreeOutline },
      { componentKey: 'color', label: '颜色选择', icon: ToggleOutline },
    ],
  },
  {
    title: '选择',
    items: [
      { componentKey: 'select', label: '静态下拉', icon: ListOutline },
      { componentKey: 'dictSelect', label: '字典下拉', icon: ListOutline },
      { componentKey: 'radio', label: '单选', icon: CheckboxOutline },
      { componentKey: 'radioButton', label: '按钮单选', icon: CheckboxOutline },
      { componentKey: 'checkbox', label: '多选', icon: CheckboxOutline },
      { componentKey: 'cascader', label: '级联选择', icon: ChevronDownCircleOutline },
      { componentKey: 'treeSelect', label: '树形选择', icon: GridOutline },
      { componentKey: 'customSelect', label: '远程选择', icon: ListOutline },
      { componentKey: 'date', label: '日期', icon: TimeOutline },
      { componentKey: 'datetime', label: '日期时间', icon: TimeOutline },
      { componentKey: 'daterange', label: '日期范围', icon: TimeOutline },
      { componentKey: 'datetimerange', label: '日期时间范围', icon: TimeOutline },
      { componentKey: 'month', label: '月份', icon: TimeOutline },
      { componentKey: 'year', label: '年份', icon: TimeOutline },
      { componentKey: 'timerange', label: '时间范围', icon: TimeOutline },
      { componentKey: 'switch', label: '开关', icon: ToggleOutline },
    ],
  },
  {
    title: '业务',
    items: [
      { componentKey: 'userSelect', label: '人员选择', icon: PersonOutline },
      { componentKey: 'orgTreeSelect', label: '部门选择', icon: GridOutline },
      { componentKey: 'regionTreeSelect', label: '行政区划', icon: GridOutline },
      { componentKey: 'objectReference', label: '对象引用', icon: BrowsersOutline },
      { componentKey: 'fileUpload', label: '文件上传', icon: CloudUploadOutline },
      { componentKey: 'imageUpload', label: '图片上传', icon: ImageOutline },
      { componentKey: 'text', label: '文本展示', icon: TextOutline },
    ],
  },
]
const layoutItems = [
  { componentKey: 'row', label: '栅格布局', icon: GridOutline },
  { componentKey: 'table', label: '表格布局', icon: GridOutline },
  { componentKey: 'AiCrudPage', label: 'CRUD区块', icon: ListOutline },
  { componentKey: 'button', label: '按钮', icon: ToggleOutline },
  { componentKey: 'title', label: '分组标题', icon: ReorderThreeOutline },
  { componentKey: 'AiFormSectionTitle', label: '表单分隔线', icon: ReorderThreeOutline },
  { componentKey: 'card', label: '卡片分组', icon: AlbumsOutline },
  { componentKey: 'tabs', label: '标签页', icon: BrowsersOutline },
  { componentKey: 'collapse', label: '折叠面板', icon: ChevronDownCircleOutline },
]

const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase())
const visibleTemplateGroups = computed(() => {
  const text = normalizedKeyword.value
  if (!text)
    return fieldTemplateGroups
  return fieldTemplateGroups
    .map(group => ({
      ...group,
      items: group.items.filter(item => [item.label, item.componentKey].some(value => String(value || '').toLowerCase().includes(text))),
    }))
    .filter(group => group.items.length)
})
const visibleLayoutItems = computed(() => {
  const text = normalizedKeyword.value
  if (!text)
    return layoutItems
  return layoutItems.filter(item => [item.label, item.componentKey].some(value => String(value || '').toLowerCase().includes(text)))
})
const componentTotal = computed(() => {
  return visibleTemplateGroups.value.reduce((total, group) => total + group.items.length, 0) + visibleLayoutItems.value.length
})
const filteredFields = computed(() => {
  const text = normalizedKeyword.value
  if (!text)
    return props.fields
  return props.fields.filter((field) => {
    return [field.label, field.fieldName, field.field, field.fieldCode, field.componentType]
      .some(value => String(value || '').toLowerCase().includes(text))
  })
})
const businessFields = computed(() => filteredFields.value.filter(field => !isReadonlySystemField(field)))
const systemFields = computed(() => filteredFields.value.filter(field => isReadonlySystemField(field)))
const usedFields = computed(() => businessFields.value.filter(field => isUsed(field)))
const unusedFields = computed(() => businessFields.value.filter(field => !isUsed(field)))
const visibleFields = computed(() => {
  if (activeTab.value === 'used')
    return usedFields.value
  if (activeTab.value === 'system')
    return systemFields.value
  return unusedFields.value
})

function isUsed(field = {}) {
  return props.usedFieldSet.has(field.field || field.fieldCode)
}

function isLocked(field = {}) {
  return isReadonlySystemField(field)
}

function handleFieldDragStart(event, field) {
  if (isUsed(field) || isLocked(field)) {
    event.preventDefault()
    return
  }
  draggingKey.value = `field:${field.field || field.fieldCode}`
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('application/x-forge-form-field', JSON.stringify(field))
}

function handleLayoutDragStart(event, item) {
  draggingKey.value = `layout:${item.componentKey}`
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('application/x-forge-form-layout', JSON.stringify(item))
}

function handleTemplateDragStart(event, item) {
  draggingKey.value = `template:${item.componentKey}`
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('application/x-forge-form-template', JSON.stringify(item))
}

function finishDragging() {
  draggingKey.value = ''
  clearDesignerDragSource()
  clearDesignerDropKey()
}
</script>

<style scoped>
.forge-field-shelf {
  display: grid;
  grid-template-rows: auto auto auto minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  background: #f3f6fa;
}

.shelf-head {
  padding: 10px 12px 8px;
  border-bottom: 1px solid #dbe3ee;
  background: #fff;
}

.shelf-head h3,
.shelf-head p {
  margin: 0;
}

.shelf-head h3 {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.shelf-head p {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}

.shelf-search {
  margin: 8px 10px 6px;
  width: calc(100% - 20px);
}

.shelf-mode-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  padding: 0 10px 8px;
}

.shelf-mode-tabs button {
  cursor: pointer;
  height: 34px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  transition:
    border-color 180ms ease,
    background 180ms ease,
    color 180ms ease,
    box-shadow 180ms ease;
}

.shelf-mode-tabs button:hover {
  border-color: #93c5fd;
  color: #2563eb;
}

.shelf-mode-tabs button.active {
  border-color: #60a5fa;
  background: #eff6ff;
  color: #1d4ed8;
  box-shadow: inset 0 0 0 1px rgba(96, 165, 250, 0.18);
}

.component-palette {
  min-height: 0;
  overflow: auto;
  padding: 2px 10px 12px;
  background: linear-gradient(180deg, #f8fafc 0%, #f3f6fa 100%);
}

.palette-section + .palette-section {
  margin-top: 12px;
}

.palette-section h4 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin: 2px 0 8px;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.palette-section h4 em {
  border-radius: 999px;
  background: #dbe3ee;
  color: #64748b;
  font-size: 11px;
  font-style: normal;
  line-height: 18px;
  padding: 0 7px;
}

.palette-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.palette-item,
.field-item {
  cursor: grab;
  border: 1px solid #dbe3ee;
  background: #fff;
  transition:
    border-color 180ms ease,
    background 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.palette-item:hover,
.field-item:hover:not(:disabled) {
  border-color: #93c5fd;
  background: #f8fbff;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.08);
}

.palette-item.dragging,
.field-item.dragging {
  opacity: 0.48;
  border-color: #60a5fa;
  background: #dbeafe;
}

.palette-item:active,
.field-item:active {
  cursor: grabbing;
}

.palette-item {
  min-height: 42px;
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  border-radius: 8px;
  padding: 0 10px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  overflow: hidden;
}

.palette-item :deep(.n-icon) {
  color: #64748b;
  font-size: 18px;
  flex: 0 0 auto;
}

.palette-item:hover :deep(.n-icon) {
  color: #2563eb;
}

.field-asset-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  border-top: 1px solid #dbe3ee;
  background: #fff;
}

.field-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  padding: 8px 10px 7px;
  border-bottom: 1px solid #dbe3ee;
  background: #fff;
}

.field-tabs button {
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 6px;
  background: #dbe3ee;
  color: #64748b;
  font-size: 12px;
  line-height: 26px;
}

.field-tabs button.active {
  border-color: #94a3b8;
  background: #fff;
  color: #0f172a;
  font-weight: 600;
}

.field-list {
  display: grid;
  align-content: start;
  gap: 5px;
  min-height: 0;
  overflow: auto;
  background: #fff;
  padding: 8px 10px 12px;
}

.field-item {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  border-radius: 6px;
  padding: 5px 8px;
  text-align: left;
}

.field-item:disabled {
  cursor: not-allowed;
  background: #f8fafc;
  color: #94a3b8;
}

.drag-handle {
  display: grid;
  place-items: center;
  color: #94a3b8;
}

.field-item:hover:not(:disabled) .drag-handle {
  color: #2563eb;
}

.field-main {
  min-width: 0;
}

.field-main strong,
.field-main small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-main strong {
  color: #0f172a;
  font-size: 12px;
}

.field-main small {
  margin-top: 2px;
  color: #64748b;
  font-size: 11px;
}

.field-item em {
  border-radius: 999px;
  background: #e2e8f0;
  color: #64748b;
  font-size: 11px;
  font-style: normal;
  line-height: 20px;
  padding: 0 6px;
}

@media (prefers-reduced-motion: reduce) {
  .palette-item,
  .field-item {
    transition: none;
  }
}
</style>
