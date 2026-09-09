<template>
  <div class="forge-field-shelf">
    <div class="shelf-head">
      <div class="shelf-head-main">
        <h3>字段与布局</h3>
        <p>拖入画布或点击添加。</p>
        <div class="shelf-stats">
          <template v-if="activeShelfTab === 'components'">
            <span>共 {{ unifiedShelfTotal }} 个</span>
          </template>
          <template v-else>
            <span>共 {{ props.fields.length }} 个</span>
            <span v-if="keyword && visibleFields.length !== props.fields.length">匹配 {{ visibleFields.length }} 个</span>
            <span>未用 {{ unusedFields.length }}</span>
          </template>
        </div>
      </div>
      <slot name="actions" />
    </div>

    <n-input
      v-model:value="keyword"
      size="small"
      clearable
      :placeholder="activeShelfTab === 'components' ? '搜索组件' : '搜索字段'"
      class="shelf-search"
    >
      <template #prefix>
        <n-icon><SearchOutline /></n-icon>
      </template>
    </n-input>

    <div class="shelf-mode-tabs">
      <button type="button" :class="{ active: activeShelfTab === 'components' }" @click="activeShelfTab = 'components'">
        组件库 {{ unifiedShelfTotal }}
      </button>
      <button type="button" :class="{ active: activeShelfTab === 'fields' }" @click="activeShelfTab = 'fields'">
        字段资产 {{ props.fields.length }}
      </button>
    </div>

    <div v-if="activeShelfTab === 'components'" class="component-palette">
      <!-- 统一组件物料面板（designer-core）：与列表设计器同一注册表、同一分组、同一交互（空态由面板内部渲染） -->
      <!-- B2 修正：画布不支持的组件直接隐藏（用户验收反馈：禁用态同显太乱）；注册表仍是唯一事实源，两侧加/改组件只改 spec -->
      <UnifiedComponentPalette
        scope="F"
        :keyword="keyword"
        :item-filter="formShelfItemFilter"
        @item-drag-start="handleUnifiedShelfDragStart"
        @item-drag-end="finishDragging"
        @total-change="unifiedShelfTotal = $event"
      />
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
import { SearchOutline } from '@vicons/ionicons5'
import { computed, ref } from 'vue'
import { FORM_COMPONENT_KEY_OVERRIDES, toFieldPaletteGroups, toPageWidgetCatalog } from '@/components/lowcode-builder/designer-core'
import UnifiedComponentPalette from '@/components/lowcode-builder/designer-core/panel/UnifiedComponentPalette.vue'
import { isReadonlySystemField } from '@/components/lowcode-builder/page/page-schema'
import { clearDesignerDragPreview, clearDesignerDragSource, clearDesignerDropKey, setDesignerDragPreview } from './designerDragState'

const props = defineProps({
  fields: {
    type: Array,
    default: () => [],
  },
  usedFieldSet: {
    type: Object,
    default: () => new Set(),
  },
  relations: {
    type: Array,
    default: () => [],
  },
})

defineEmits(['appendField'])

const keyword = ref('')
const activeShelfTab = ref('components')
const activeTab = ref('unused')
const draggingKey = ref('')

// ─── 统一组件物料面板（designer-core，P2）──────────────
// 与列表设计器同一注册表、同一分组、同一交互；键名映射走 FORM_COMPONENT_KEY_OVERRIDES
const unifiedShelfTotal = ref(0)

/** 表单画布（createForgeLayoutComponent / isPageWidgetComponentKey）支持的布局与业务键名（映射后） */
const FORM_CANVAS_LAYOUT_KEYS = new Set([
  'row',
  'table',
  'card',
  'tabs',
  'collapse',
  'button',
  'title',
  'AiFormSectionTitle',
  'AiCrudPage',
  'subTable',
])
/** 字段模板类型（拖拽走 template 链路，生成字段绑定组件） */
const FORM_FIELD_TEMPLATE_TYPES = new Set(
  toFieldPaletteGroups().flatMap(group => group.items.map(item => item.componentKey)),
)
/** 画布挂件键名（isPageWidgetComponentKey 消费；transfer 以字段模板形态单独处理） */
const FORM_CANVAS_WIDGET_KEYS = new Set(
  toPageWidgetCatalog()
    .filter(item => item.componentKey !== 'transfer')
    .map(item => item.componentKey),
)

function resolveFormComponentKey(spec = {}) {
  return FORM_COMPONENT_KEY_OVERRIDES[spec.type] || spec.type
}

/**
 * 画布支持白名单过滤：不支持的组件直接隐藏（B2 修正，用户验收反馈禁用态同显太乱）。
 * 字段模板（field 类别）与 transfer 双重身份走 template 链路；其余按映射后的画布键名判定。
 */
function formShelfItemFilter(spec) {
  if (spec.group === '包装节点')
    return false
  // 导航组件（面包屑、菜单、分页）不适用于表单画布，仅在页面设计器中提供
  if (spec.group === '导航')
    return false
  if (spec.category === 'field' || spec.type === 'transfer')
    return true
  // 主子表不再依赖「数据模型-对象关系」前置配置，无关系时也可直接拖入/配置（产品决策：直接可添加）
  if (FORM_CANVAS_WIDGET_KEYS.has(spec.type))
    return true
  return FORM_CANVAS_LAYOUT_KEYS.has(resolveFormComponentKey(spec))
}

function handleUnifiedShelfDragStart({ spec, event }) {
  const componentKey = resolveFormComponentKey(spec)
  // 字段模板（含 transfer 双重身份）走 template 链路生成字段绑定组件；其余走 layout 链路
  const isFieldTemplate = FORM_FIELD_TEMPLATE_TYPES.has(componentKey) || spec.type === 'transfer'
  draggingKey.value = `${isFieldTemplate ? 'template' : 'layout'}:${componentKey}`
  event.dataTransfer.effectAllowed = 'copy'
  const payload = { componentKey, label: spec.label }
  event.dataTransfer.setData(
    isFieldTemplate ? 'application/x-forge-form-template' : 'application/x-forge-form-layout',
    JSON.stringify(payload),
  )
  setDesignerDragPreview({ componentKey, label: spec.label })
}

const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase())
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
  setDesignerDragPreview({
    componentKey: field.componentType || field.componentKey || field.type || 'input',
    label: field.label || field.fieldName || field.field || '字段',
  })
}

function finishDragging() {
  draggingKey.value = ''
  clearDesignerDragSource()
  clearDesignerDragPreview()
  clearDesignerDropKey()
}
</script>

<style scoped>
.forge-field-shelf {
  display: grid;
  grid-template-rows: auto auto auto minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  padding: 12px;
  background: #fcfcfc;
}

.shelf-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 0;
  border-bottom: 0;
  background: #fcfcfc;
}

.shelf-head-main {
  min-width: 0;
}

.shelf-head h3,
.shelf-head p {
  margin: 0;
}

.shelf-head h3 {
  color: #18181b;
  font-size: 13px;
  font-weight: 700;
  line-height: 18px;
}

.shelf-head p {
  margin-top: 2px;
  color: #71717a;
  font-size: 11px;
  line-height: 16px;
}

.shelf-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.shelf-stats span,
.palette-section h4 em {
  display: inline-flex;
  align-items: center;
  height: 18px;
  padding: 0 6px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
  line-height: 16px;
}

.shelf-search {
  margin-top: 12px;
  width: 100%;
}

.shelf-mode-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 3px;
  margin: 8px 0 10px;
  padding: 3px;
  border: 1px solid #e4e4e7;
  border-radius: 9px;
  background: #f4f4f5;
}

.shelf-mode-tabs button {
  cursor: pointer;
  height: 28px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #71717a;
  font-size: 12px;
  font-weight: 600;
  transition:
    border-color 180ms ease,
    background 180ms ease,
    color 180ms ease,
    box-shadow 180ms ease;
}

.shelf-mode-tabs button:hover {
  background: rgba(228, 228, 231, 0.72);
  color: #27272a;
}

.shelf-mode-tabs button.active {
  background: #fff;
  color: #27272a;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.component-palette {
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 2px 0 0;
  background: #fcfcfc;
}

.field-item {
  cursor: grab;
  border: 1px solid #e4e4e7;
  background: #fff;
  transition:
    border-color 180ms ease,
    background 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.field-item:hover:not(:disabled) {
  border-color: #c7d2fe;
  background: #f8faff;
  box-shadow: 0 8px 18px rgba(49, 83, 216, 0.08);
}

.field-item.dragging {
  opacity: 0.48;
  border-color: #60a5fa;
  background: #dbeafe;
}

.field-item:active {
  cursor: grabbing;
}

.field-asset-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  border-top: 0;
  background: transparent;
}

.field-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 3px;
  margin-bottom: 8px;
  padding: 3px;
  border: 1px solid #e4e4e7;
  border-radius: 9px;
  background: #f4f4f5;
}

.field-tabs button {
  cursor: pointer;
  height: 28px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #71717a;
  font-size: 12px;
  font-weight: 600;
  line-height: 28px;
}

.field-tabs button:hover {
  background: rgba(228, 228, 231, 0.72);
  color: #27272a;
}

.field-tabs button.active {
  background: #fff;
  color: #27272a;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.field-list {
  display: grid;
  align-content: start;
  gap: 5px;
  min-height: 0;
  overflow: auto;
  background: transparent;
  padding: 0;
}

.field-item {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  border-radius: 7px;
  padding: 6px 8px;
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
  .field-item {
    transition: none;
  }
}
</style>
