<template>
  <n-grid :cols="gridCols" :x-gap="xGap" :y-gap="yGap" class="af-layout-grid">
    <n-gi
      v-for="node in nodes"
      :key="resolveNodeKey(node)"
      :span="resolveNodeSpan(node)"
      :style="node.gridStyle"
      :class="node.gridClass"
    >
      <AiFormItem
        v-if="isFieldNode(node)"
        :field="{ ...node, showFeedback: node.showFeedback ?? showFeedback }"
        :value="formValue[node.field]"
        :form-data="formValue"
        :context="itemContext"
        @update:value="emit('fieldChange', node.field, $event)"
      >
        <template v-for="slotName in forwardedSlotNames" #[slotName]="slotProps">
          <slot :name="slotName" v-bind="slotProps" />
        </template>
      </AiFormItem>

      <AiFormLayoutNodes
        v-else-if="isRowNode(node)"
        :nodes="node.children || []"
        :form-value="formValue"
        :item-context="itemContext"
        :grid-cols="resolveRowColumns(node)"
        :x-gap="resolveGap(node.props?.gutter, xGap)"
        :y-gap="yGap"
        :show-feedback="showFeedback"
        @field-change="(...args) => emit('fieldChange', ...args)"
        @node-action="(...args) => emit('nodeAction', ...args)"
      >
        <template v-for="slotName in forwardedSlotNames" #[slotName]="slotProps">
          <slot :name="slotName" v-bind="slotProps" />
        </template>
      </AiFormLayoutNodes>

      <AiFormLayoutNodes
        v-else-if="isColumnNode(node)"
        :nodes="node.children || []"
        :form-value="formValue"
        :item-context="itemContext"
        :grid-cols="1"
        :x-gap="xGap"
        :y-gap="yGap"
        :show-feedback="showFeedback"
        @field-change="(...args) => emit('fieldChange', ...args)"
        @node-action="(...args) => emit('nodeAction', ...args)"
      >
        <template v-for="slotName in forwardedSlotNames" #[slotName]="slotProps">
          <slot :name="slotName" v-bind="slotProps" />
        </template>
      </AiFormLayoutNodes>

      <n-card
        v-else-if="isCardNode(node)"
        size="small"
        :title="node.label || node.props?.header || undefined"
        :bordered="true"
        :style="node.style"
        :class="node.className"
        class="af-layout-card"
      >
        <AiFormLayoutNodes
          :nodes="node.children || []"
          :form-value="formValue"
          :item-context="itemContext"
          :grid-cols="gridCols"
          :x-gap="xGap"
          :y-gap="yGap"
          :show-feedback="showFeedback"
          @field-change="(...args) => emit('fieldChange', ...args)"
          @node-action="(...args) => emit('nodeAction', ...args)"
        >
          <template v-for="slotName in forwardedSlotNames" #[slotName]="slotProps">
            <slot :name="slotName" v-bind="slotProps" />
          </template>
        </AiFormLayoutNodes>
      </n-card>

      <n-tabs
        v-else-if="isTabsNode(node)"
        :type="resolveTabsType(node.props?.type)"
        :placement="resolveTabsPlacement(node.props?.tabPosition)"
        :style="node.style"
        :class="node.className"
        class="af-layout-tabs"
      >
        <n-tab-pane
          v-for="pane in resolvePaneChildren(node, 'tabPane')"
          :key="resolveNodeKey(pane)"
          :name="resolveNodeKey(pane)"
          :tab="pane.label || pane.props?.label || '标签页'"
          :disabled="!!pane.props?.disabled"
        >
          <AiFormLayoutNodes
            :nodes="pane.children || []"
            :form-value="formValue"
            :item-context="itemContext"
            :grid-cols="gridCols"
            :x-gap="xGap"
            :y-gap="yGap"
            :show-feedback="showFeedback"
            @field-change="(...args) => emit('fieldChange', ...args)"
            @node-action="(...args) => emit('nodeAction', ...args)"
          >
            <template v-for="slotName in forwardedSlotNames" #[slotName]="slotProps">
              <slot :name="slotName" v-bind="slotProps" />
            </template>
          </AiFormLayoutNodes>
        </n-tab-pane>
      </n-tabs>

      <n-collapse
        v-else-if="isCollapseNode(node)"
        :accordion="!!node.props?.accordion"
        :style="node.style"
        :class="node.className"
        class="af-layout-collapse"
      >
        <n-collapse-item
          v-for="item in resolvePaneChildren(node, 'collapseItem')"
          :key="resolveNodeKey(item)"
          :name="resolveNodeKey(item)"
          :title="item.label || item.props?.title || '分组'"
          :disabled="!!item.props?.disabled"
        >
          <AiFormLayoutNodes
            :nodes="item.children || []"
            :form-value="formValue"
            :item-context="itemContext"
            :grid-cols="gridCols"
            :x-gap="xGap"
            :y-gap="yGap"
            :show-feedback="showFeedback"
            @field-change="(...args) => emit('fieldChange', ...args)"
            @node-action="(...args) => emit('nodeAction', ...args)"
          >
            <template v-for="slotName in forwardedSlotNames" #[slotName]="slotProps">
              <slot :name="slotName" v-bind="slotProps" />
            </template>
          </AiFormLayoutNodes>
        </n-collapse-item>
      </n-collapse>

      <AiFormSectionTitle
        v-else-if="isSectionTitleNode(node)"
        :title="node.label"
        :anchor-id="node.__sectionId"
        :description="node.props?.description || node.description"
        :badge="node.props?.badge || node.badge"
        :style="node.style"
        :class="node.className"
      />

      <AiFormGroupTitle
        v-else-if="isGroupTitleNode(node)"
        :label="node.label"
        :title="node.props?.title || node.title"
        :style="node.style"
        :class-name="node.className"
      />

      <div
        v-else-if="isButtonNode(node)"
        class="af-layout-button"
        :class="[`is-${resolveAlign(node)}`]"
        :style="node.style"
      >
        <n-button
          :type="resolveButtonType(node)"
          :size="resolveButtonSize(node)"
          :secondary="!!node.props?.secondary"
          :tertiary="!!node.props?.tertiary"
          :quaternary="!!node.props?.quaternary"
          :dashed="!!node.props?.dashed"
          :round="!!node.props?.round"
          :block="!!node.props?.block"
          :disabled="!!node.props?.disabled"
          @click="handleButtonClick(node)"
        >
          {{ node.props?.text || node.label || '按钮' }}
        </n-button>
      </div>

      <div
        v-else-if="isTableNode(node)"
        class="af-layout-table"
        :style="node.style"
        :class="node.className"
      >
        <AiFormLayoutNodes
          :nodes="resolveTableCells(node)"
          :form-value="formValue"
          :item-context="itemContext"
          :grid-cols="resolveTableColumns(node)"
          :x-gap="0"
          :y-gap="0"
          :show-feedback="showFeedback"
          @field-change="(...args) => emit('fieldChange', ...args)"
          @node-action="(...args) => emit('nodeAction', ...args)"
        >
          <template v-for="slotName in forwardedSlotNames" #[slotName]="slotProps">
            <slot :name="slotName" v-bind="slotProps" />
          </template>
        </AiFormLayoutNodes>
      </div>

      <div
        v-else-if="isTableGridNode(node)"
        class="af-layout-table-cell"
        :style="node.style"
        :class="node.className"
      >
        <AiFormLayoutNodes
          :nodes="node.children || []"
          :form-value="formValue"
          :item-context="itemContext"
          :grid-cols="1"
          :x-gap="xGap"
          :y-gap="yGap"
          :show-feedback="showFeedback"
          @field-change="(...args) => emit('fieldChange', ...args)"
          @node-action="(...args) => emit('nodeAction', ...args)"
        >
          <template v-for="slotName in forwardedSlotNames" #[slotName]="slotProps">
            <slot :name="slotName" v-bind="slotProps" />
          </template>
        </AiFormLayoutNodes>
      </div>

      <AiCrudPage
        v-else-if="isCrudNode(node)"
        class="af-layout-crud"
        v-bind="buildCrudProps(node)"
      />

      <AiFormLayoutNodes
        v-else
        :nodes="node.children || []"
        :form-value="formValue"
        :item-context="itemContext"
        :grid-cols="gridCols"
        :x-gap="xGap"
        :y-gap="yGap"
        :show-feedback="showFeedback"
        @field-change="(...args) => emit('fieldChange', ...args)"
        @node-action="(...args) => emit('nodeAction', ...args)"
      >
        <template v-for="slotName in forwardedSlotNames" #[slotName]="slotProps">
          <slot :name="slotName" v-bind="slotProps" />
        </template>
      </AiFormLayoutNodes>
    </n-gi>

    <slot name="gridAppend" />
  </n-grid>
</template>

<script setup>
import { computed, defineAsyncComponent, useSlots } from 'vue'
import AiFormGroupTitle from './AiFormGroupTitle.vue'
import AiFormItem from './AiFormItem.vue'
import AiFormSectionTitle from './AiFormSectionTitle.vue'

defineOptions({
  name: 'AiFormLayoutNodes',
})

const props = defineProps({
  nodes: {
    type: Array,
    default: () => [],
  },
  formValue: {
    type: Object,
    default: () => ({}),
  },
  itemContext: {
    type: Object,
    default: () => ({}),
  },
  gridCols: {
    type: Number,
    default: 1,
  },
  xGap: {
    type: Number,
    default: 12,
  },
  yGap: {
    type: Number,
    default: 0,
  },
  showFeedback: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['fieldChange', 'nodeAction'])
const slots = useSlots()
const AiCrudPage = defineAsyncComponent(() => import('./AiCrudPage.vue'))

const forwardedSlotNames = computed(() => Object.keys(slots).filter(name => name !== 'gridAppend'))

function isFieldNode(node = {}) {
  return (!node.nodeType && !isKnownLayoutNode(node)) || node.nodeType === 'field'
}

function resolveNodeSpan(node = {}) {
  if (isRowNode(node) || isCardNode(node) || isTabsNode(node) || isCollapseNode(node) || isCrudNode(node) || isSectionTitleNode(node) || isGroupTitleNode(node))
    return props.gridCols
  if (isTableNode(node))
    return Math.max(1, Math.min(props.gridCols, Number(node.span || props.gridCols)))
  return Math.max(1, Math.min(props.gridCols, Number(node.span || 1)))
}

function resolveNodeKey(node = {}) {
  return node.key || node.field || node.id || `${node.nodeType || node.type || 'node'}_${node.label || ''}`
}

function resolveGap(value, fallback) {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

function resolveRowColumns(node = {}) {
  const number = Number(node.props?.columns || props.gridCols)
  return Math.max(1, Math.min(4, Number.isFinite(number) ? number : props.gridCols))
}

function resolveTabsType(value) {
  return value === 'card' ? 'card' : 'line'
}

function resolveTabsPlacement(value) {
  return ['left', 'right', 'top', 'bottom'].includes(value) ? value : 'top'
}

function resolvePaneChildren(node = {}, paneType) {
  const children = Array.isArray(node.children) ? node.children : []
  const panes = children.filter(child => child?.nodeType === paneType)
  if (panes.length)
    return panes
  return [{
    key: `${resolveNodeKey(node)}_pane`,
    label: node.label,
    props: {},
    children,
  }]
}

function resolveNodeType(node = {}) {
  return node.nodeType || node.type || node.componentKey || ''
}

function isKnownLayoutNode(node = {}) {
  return isRowNode(node)
    || isColumnNode(node)
    || isCardNode(node)
    || isTabsNode(node)
    || isCollapseNode(node)
    || isButtonNode(node)
    || isTableNode(node)
    || isTableGridNode(node)
    || isCrudNode(node)
    || isSectionTitleNode(node)
    || isGroupTitleNode(node)
}

function isRowNode(node = {}) {
  return ['row', 'fcRow'].includes(resolveNodeType(node))
}

function isColumnNode(node = {}) {
  return resolveNodeType(node) === 'col'
}

function isCardNode(node = {}) {
  return ['card', 'elCard'].includes(resolveNodeType(node))
}

function isTabsNode(node = {}) {
  return ['tabs', 'elTabs'].includes(resolveNodeType(node))
}

function isCollapseNode(node = {}) {
  return ['collapse', 'elCollapse'].includes(resolveNodeType(node))
}

function isButtonNode(node = {}) {
  return resolveNodeType(node) === 'button'
}

function isTableNode(node = {}) {
  return resolveNodeType(node) === 'table'
}

function isTableGridNode(node = {}) {
  return resolveNodeType(node) === 'tableGrid'
}

function isCrudNode(node = {}) {
  return ['AiCrudPage', 'aiCrudPage', 'crud', 'crudBlock'].includes(resolveNodeType(node))
}

function resolveAlign(node = {}) {
  return ['left', 'center', 'right'].includes(node.layout?.align || node.align)
    ? node.layout?.align || node.align
    : 'left'
}

function resolveButtonType(node = {}) {
  return ['default', 'primary', 'info', 'success', 'warning', 'error'].includes(node.props?.type)
    ? node.props.type
    : 'primary'
}

function resolveButtonSize(node = {}) {
  return ['tiny', 'small', 'medium', 'large'].includes(node.props?.size)
    ? node.props.size
    : undefined
}

function handleButtonClick(node = {}) {
  const events = Array.isArray(node.props?.__events) ? node.props.__events : []
  const clickEvents = events.filter(event => !event?.trigger || event.trigger === 'click')
  if (!clickEvents.length) {
    emit('nodeAction', { node, event: null, action: 'click' })
    return
  }
  clickEvents.forEach(event => emit('nodeAction', { node, event, action: event.action || 'click' }))
}

function resolveTableColumns(node = {}) {
  const columns = Number(node.props?.columns || node.columns || (node.children || []).length || props.gridCols)
  return Math.max(1, Math.min(6, Number.isFinite(columns) ? columns : props.gridCols))
}

function resolveTableCells(node = {}) {
  const children = Array.isArray(node.children) ? node.children : []
  if (children.length)
    return children
  return Array.from({ length: resolveTableColumns(node) }).map((_, index) => ({
    nodeType: 'tableGrid',
    key: `${resolveNodeKey(node)}_cell_${index + 1}`,
    label: `单元格 ${index + 1}`,
    span: 1,
    children: [],
  }))
}

function buildCrudProps(node = {}) {
  const nodeProps = node.props || {}
  const crudOptions = nodeProps.crudOptions || {}
  const runtimeColumns = resolveCrudColumns(node)
  return {
    ...crudOptions,
    ...nodeProps,
    title: nodeProps.title || node.label || 'CRUD',
    apiConfig: nodeProps.apiConfig || nodeProps.api || {},
    rowKey: nodeProps.rowKey || 'id',
    columns: runtimeColumns,
    schema: runtimeColumns,
    searchSchema: nodeProps.searchSchema || nodeProps.querySchema || resolveCrudSearchSchema(node),
  }
}

function resolveCrudSearchSchema(node = {}) {
  return (node.children || [])
    .filter(child => !isTableNode(child) && !isTableGridNode(child))
    .flatMap(child => isFieldNode(child) ? [child] : child.children || [])
}

function resolveCrudColumns(node = {}) {
  const columns = node.props?.columns || node.props?.schema
  if (Array.isArray(columns) && columns.length)
    return columns
  const table = (node.children || []).find(child => isTableNode(child))
  const tableColumns = table?.props?.columns
  if (Array.isArray(tableColumns) && tableColumns.length)
    return tableColumns
  const fields = (table?.children || [])
    .flatMap(cell => cell?.children || [])
    .filter(child => isFieldNode(child))
  if (fields.length)
    return fields
  return [{
    title: '暂无列配置',
    key: '__empty',
    field: '__empty',
    prop: '__empty',
    component: 'input',
    type: 'input',
  }]
}

function isGroupTitleNode(node = {}) {
  return isLegacyGroupTitleNode(node) || ['title', 'fcTitle', 'sectionTitle', 'groupTitle', 'groupHeader', 'GroupHeader', 'titleBlock', 'section']
    .includes(resolveNodeType(node))
}

function isSectionTitleNode(node = {}) {
  return !isLegacyGroupTitleNode(node) && ['divider', 'elDivider', 'AiFormSectionTitle', 'aiFormSectionTitle', 'formSectionTitle', 'FormSectionTitle']
    .includes(resolveNodeType(node))
}

function isLegacyGroupTitleNode(node = {}) {
  const props = node.props || {}
  return node.nodeType === 'divider'
    && !node.componentKey
    && Object.prototype.hasOwnProperty.call(props, 'description')
    && !Object.prototype.hasOwnProperty.call(props, 'title')
}
</script>

<style scoped>
.af-layout-grid {
  min-width: 0;
}

.af-layout-card,
.af-layout-tabs,
.af-layout-collapse {
  width: 100%;
}

.af-layout-card :deep(.n-card__content) {
  padding-top: 12px;
}

.af-layout-button {
  display: flex;
  width: 100%;
}

.af-layout-button.is-left {
  justify-content: flex-start;
}

.af-layout-button.is-center {
  justify-content: center;
}

.af-layout-button.is-right {
  justify-content: flex-end;
}

.af-layout-table {
  width: 100%;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.af-layout-table :deep(.af-layout-grid) {
  gap: 0 !important;
}

.af-layout-table-cell {
  min-height: 56px;
  padding: 12px;
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
}

.af-layout-crud {
  width: 100%;
}
</style>
