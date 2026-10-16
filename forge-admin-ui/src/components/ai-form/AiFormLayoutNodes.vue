<template>
  <n-grid :cols="gridCols" :x-gap="xGap" :y-gap="yGap" class="af-layout-grid">
    <n-gi
      v-for="node in visibleNodes"
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
        :y-gap="resolveGap(node.props?.rowGap, yGap)"
        :show-feedback="showFeedback"
        :keep-empty-layout-nodes="keepEmptyLayoutNodes"
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
        :keep-empty-layout-nodes="keepEmptyLayoutNodes"
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
        :bordered="node.props?.bordered !== false"
        :style="resolveLayoutStyle(node)"
        :class="resolveLayoutClass(node, 'af-layout-card')"
      >
        <div v-if="!hasVisibleChildren(node)" class="af-layout-empty-placeholder">
          暂无内容
        </div>
        <AiFormLayoutNodes
          v-else
          :nodes="node.children || []"
          :form-value="formValue"
          :item-context="itemContext"
          :grid-cols="gridCols"
          :x-gap="xGap"
          :y-gap="yGap"
          :show-feedback="showFeedback"
          :keep-empty-layout-nodes="keepEmptyLayoutNodes"
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
        :size="node.props?.size || 'medium'"
        :placement="resolveTabsPlacement(node.props?.placement || node.props?.tabPosition)"
        :trigger="node.props?.trigger || 'click'"
        :animated="node.props?.animated !== false"
        :closable="!!node.props?.closable"
        :addable="!!node.props?.addable"
        :justify-content="node.props?.justifyContent"
        :tabs-padding="node.props?.tabsPadding"
        :default-value="resolveFirstPaneKey(node, 'tabPane')"
        :style="resolveLayoutStyle(node)"
        :class="resolveLayoutClass(node, 'af-layout-tabs')"
      >
        <n-tab-pane
          v-for="pane in resolvePaneChildren(node, 'tabPane')"
          :key="resolveNodeKey(pane)"
          :name="resolveNodeKey(pane)"
          :tab="pane.label || pane.props?.label || '标签页'"
          :disabled="!!pane.props?.disabled"
        >
          <div v-if="!hasVisibleChildren(pane)" class="af-layout-empty-placeholder">
            暂无内容
          </div>
          <AiFormLayoutNodes
            v-else
            :nodes="pane.children || []"
            :form-value="formValue"
            :item-context="itemContext"
            :grid-cols="gridCols"
            :x-gap="xGap"
            :y-gap="yGap"
            :show-feedback="showFeedback"
            :keep-empty-layout-nodes="keepEmptyLayoutNodes"
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
        :default-expanded-names="resolveCollapseDefaultNames(node)"
        :style="resolveLayoutStyle(node)"
        :class="resolveLayoutClass(node, 'af-layout-collapse')"
      >
        <n-collapse-item
          v-for="item in resolvePaneChildren(node, 'collapseItem')"
          :key="resolveNodeKey(item)"
          :name="resolveNodeKey(item)"
          :title="item.label || item.props?.title || '分组'"
          :disabled="!!item.props?.disabled"
        >
          <div v-if="!hasVisibleChildren(item)" class="af-layout-empty-placeholder">
            暂无内容
          </div>
          <AiFormLayoutNodes
            v-else
            :nodes="item.children || []"
            :form-value="formValue"
            :item-context="itemContext"
            :grid-cols="gridCols"
            :x-gap="xGap"
            :y-gap="yGap"
            :show-feedback="showFeedback"
            :keep-empty-layout-nodes="keepEmptyLayoutNodes"
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
          :loading="!!node.props?.loading"
          :disabled="resolveNodeDisabled(node)"
          @click="handleButtonClick(node)"
        >
          <template v-if="node.props?.icon" #icon>
            <IconRenderer :icon="node.props.icon" :size="16" />
          </template>
          {{ node.props?.text || node.label || '按钮' }}
        </n-button>
      </div>

      <div
        v-else-if="isTableNode(node)"
        :class="resolveLayoutClass(node, 'af-layout-table')"
        :style="resolveLayoutStyle(node)"
      >
        <AiFormLayoutNodes
          :nodes="resolveTableCells(node)"
          :form-value="formValue"
          :item-context="itemContext"
          :grid-cols="resolveTableColumns(node)"
          :x-gap="0"
          :y-gap="0"
          :show-feedback="showFeedback"
          :keep-empty-layout-nodes="keepEmptyLayoutNodes"
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
        :class="resolveLayoutClass(node, 'af-layout-table-cell')"
        :style="resolveLayoutStyle(node)"
      >
        <AiFormLayoutNodes
          :nodes="node.children || []"
          :form-value="formValue"
          :item-context="itemContext"
          :grid-cols="1"
          :x-gap="xGap"
          :y-gap="yGap"
          :show-feedback="showFeedback"
          :keep-empty-layout-nodes="keepEmptyLayoutNodes"
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
        v-bind="resolveEmbeddedCrudBind(node)"
      />

      <!-- 关联子表：按设计器布局位置内联渲染，未配置列时保留占位 -->
      <div
        v-else-if="isSubTableNode(node)"
        class="af-layout-sub-table"
        :style="resolveLayoutStyle(node)"
        :class="resolveLayoutClass(node, 'af-layout-sub-table')"
        :data-e2e="node.props?.e2eMarker || node.label || undefined"
      >
        <div class="af-layout-sub-table__header">
          {{ resolveSubTableTitle(node) }}
        </div>
        <ChildTableEditor
          v-if="buildSubTableChildrenConfig(node).length"
          :value="resolveSubTableValue(node)"
          :children-config="buildSubTableChildrenConfig(node)"
          :parent-form-data="formValue"
          :readonly="resolveNodeDisabled(node)"
          @update:value="handleSubTableUpdate(node, $event)"
        />
        <div v-else class="af-layout-empty-placeholder">
          请配置关联子表目标对象和可见字段
        </div>
      </div>

      <div
        v-else-if="isWidgetNode(node)"
        class="af-layout-widget"
        :style="resolveLayoutStyle(node)"
      >
        <PageWidgetRenderer
          :component-key="resolveWidgetKey(node)"
          :props-data="node.props || {}"
          :data-context="formValue || {}"
          :readonly="formReadonly || resolveNodeDisabled(node)"
          @update:props-data="handleWidgetUpdate(node, $event)"
        />
      </div>

      <AiFormLayoutNodes
        v-else
        :nodes="node.children || []"
        :form-value="formValue"
        :item-context="itemContext"
        :grid-cols="gridCols"
        :x-gap="xGap"
        :y-gap="yGap"
        :show-feedback="showFeedback"
        :keep-empty-layout-nodes="keepEmptyLayoutNodes"
        @field-change="(...args) => emit('fieldChange', ...args)"
        @node-action="(...args) => emit('nodeAction', ...args)"
      >
        <template v-for="slotName in forwardedSlotNames" #[slotName]="slotProps">
          <slot :name="slotName" v-bind="slotProps" />
        </template>
      </AiFormLayoutNodes>
    </n-gi>

    <!-- 设计器预览：空容器（刚拖入的栅格列/卡片/标签页等）渲染可见占位，避免预览一片空白 -->
    <n-gi
      v-if="keepEmptyLayoutNodes && !visibleNodes.length"
      :span="gridCols"
      class="af-empty-layout-placeholder"
    >
      空容器
    </n-gi>

    <slot name="gridAppend" />
  </n-grid>
</template>

<script setup>
import { computed, defineAsyncComponent, provide, reactive, useSlots } from 'vue'
import { useRoute } from 'vue-router'
import { crudConfigRender } from '@/api/ai/lowcode'
import IconRenderer from '@/components/IconRenderer.vue'
import { isPageWidgetComponentKey } from '@/components/lowcode-builder/shared/page-widget-schema'
import PageWidgetRenderer from '@/components/lowcode-builder/shared/PageWidgetRenderer.vue'
import { buildRuntimeCrudProps } from '@/components/lowcode-builder/shared/runtime-crud-props'
import { applyRuntimeControl, resolveRuntimeControl } from '@/components/lowcode-builder/shared/runtime-rules'
import ChildTableEditor from '@/components/page-templates/ChildTableEditor.vue'
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
  // 设计器预览模式：空布局容器渲染可见占位（与 AiForm.keepEmptyLayoutNodes 配套）
  keepEmptyLayoutNodes: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['fieldChange', 'nodeAction'])
const slots = useSlots()
const route = useRoute()
const AiCrudPage = defineAsyncComponent(() => import('./AiCrudPage.vue'))
/** 表单内嵌 CRUD：按 configKey 懒加载动态 CRUD 配置（editSchema/columns） */
const embeddedCrudRuntimeByKey = reactive({})
const embeddedCrudLoadingKeys = new Set()

const forwardedSlotNames = computed(() => Object.keys(slots).filter(name => name !== 'gridAppend'))
/** 显式 computed + provide：n-grid 可能不重跑槽位，挂件通过 inject 仍能跟上只读态 */
const formReadonly = computed(() => {
  const ctx = props.itemContext || {}
  return ctx.formReadonly === true || ctx.isDetail === true || ctx.modalStatus === 'detail'
})
provide('aiFormReadonly', formReadonly)
const visibleNodes = computed(() => (Array.isArray(props.nodes) ? props.nodes : [])
  .map(node => applyRuntimeControl(node, buildNodeRuleContext()))
  .filter(Boolean))

function buildNodeRuleContext(extra = {}) {
  return {
    ...(props.itemContext || {}),
    record: props.formValue || {},
    row: props.itemContext?.currentRow || props.itemContext?.row || props.formValue || {},
    formData: props.formValue || {},
    data: props.formValue || {},
    route: {
      query: route.query || {},
      params: route.params || {},
      path: route.path,
      fullPath: route.fullPath,
      name: route.name,
    },
    ...extra,
  }
}

function isFieldNode(node = {}) {
  // widget 虚拟组件（nodeType: 'widget'）由 AiFormItem 内部的 PageWidgetRenderer 渲染
  if (node.nodeType === 'widget')
    return true
  return (!node.nodeType && !isKnownLayoutNode(node)) || node.nodeType === 'field'
}

function resolveNodeSpan(node = {}) {
  const cols = Math.max(1, Number(props.gridCols) || 1)
  if (isRowNode(node) || isCardNode(node) || isTabsNode(node) || isCollapseNode(node) || isCrudNode(node) || isSubTableNode(node) || isSectionTitleNode(node) || isGroupTitleNode(node))
    return cols
  const raw = Number(node.layout?.span || node.props?.span || node.span)
  if (isTableNode(node))
    return Math.max(1, Math.min(cols, Number.isFinite(raw) && raw > 0 ? raw : cols))
  if (!Number.isFinite(raw) || raw <= 0)
    return 1
  // span 语义 = 相对父容器列数（与设计器画布 ForgeFormCanvasNode 的 min(parentCols, span) 一致）：
  // n-grid(cols=N) + n-gi(span=S<=N) 与 CSS grid repeat(N,1fr) + grid-column:span S 完全同义。
  // 历史上这里曾有「span>=24 或 span>=cols 时强制归 1」的补丁，导致设计态整行(span=24)预览变 1/24、
  // 半行(span>=cols)预览变一格，设计/预览严重不一致，已按统一语义移除。
  return Math.max(1, Math.min(cols, raw))
}

function resolveNodeKey(node = {}) {
  // key 唯一性优先级：显式 key > 组件 id > field。
  // 设计器复制组件（duplicateDesignerComponent→rewriteComponentIds）只重写 id、保留
  // fieldBinding.fieldCode，运行态 node.field 与原件相同；若 field 优先会产生重复 key，
  // 导致预览 diff 时丢节点/崩溃（isSameVNodeType null、patchElement 设置 __vnode 于 null el）。
  // 设计器 schema 的组件 id 由 normalizeFormDesignerSchema 保证唯一；无 id 的手写 schema
  // 仍回退 field，与历史行为一致。
  return node.key || node.id || node.field || `${node.nodeType || node.type || 'node'}_${node.label || ''}`
}

function resolveGap(value, fallback) {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

function resolveRowColumns(node = {}) {
  const number = Number(node.props?.columns || props.gridCols)
  return Math.max(1, Math.min(24, Number.isFinite(number) ? number : props.gridCols))
}

function resolveTabsType(value) {
  return ['card', 'segment'].includes(value) ? value : 'line'
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

function resolveFirstPaneKey(node = {}, paneType) {
  const panes = resolvePaneChildren(node, paneType)
  return panes.length ? resolveNodeKey(panes[0]) : undefined
}

function resolveCollapseDefaultNames(node = {}) {
  const panes = resolvePaneChildren(node, 'collapseItem')
  return panes.map(pane => resolveNodeKey(pane))
}

function hasVisibleChildren(node = {}) {
  const children = Array.isArray(node.children) ? node.children : []
  return children.some(child => child && typeof child === 'object')
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
    || isSubTableNode(node)
    || isSectionTitleNode(node)
    || isGroupTitleNode(node)
    || isWidgetNode(node)
}

function resolveWidgetKey(node = {}) {
  return node.componentKey || node.type || node.nodeType || ''
}

function isWidgetNode(node = {}) {
  // nodeType=widget：虚拟挂件，仍走布局槽位里的 PageWidgetRenderer
  if (node.nodeType === 'widget')
    return true
  if (!isPageWidgetComponentKey(resolveWidgetKey(node)))
    return false
  // 已绑定业务字段的 transfer 等：必须走 AiFormItem（标签/校验/样式/字段事件），
  // 不能因 componentKey 命中挂件目录就被布局层直接吃掉。
  const mode = node.fieldBinding?.mode
  if (mode === 'field' || (node.field && mode !== 'virtual'))
    return false
  return true
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
  return ['table', 'fcTable'].includes(resolveNodeType(node))
}

function isTableGridNode(node = {}) {
  return ['tableGrid', 'fcTableGrid'].includes(resolveNodeType(node))
}

function isCrudNode(node = {}) {
  return ['AiCrudPage', 'aiCrudPage', 'crud', 'crudBlock'].includes(resolveNodeType(node))
}

function isSubTableNode(node = {}) {
  return ['subTable', 'forgeSubTable', 'childTable'].includes(resolveNodeType(node))
}

function resolveSubTableTitle(node = {}) {
  return String(node.props?.header || node.props?.title || node.label || '关联子表').trim() || '关联子表'
}

function resolveSubTableRelationKey(node = {}) {
  return String(node.props?.relationKey || node.props?.modelCode || node.key || node.id || 'subTable').trim() || 'subTable'
}

function buildSubTableChildrenConfig(node = {}) {
  const nodeProps = node.props || {}
  const relationKey = resolveSubTableRelationKey(node)
  const columns = Array.isArray(nodeProps.columns) ? nodeProps.columns : []
  const fields = columns
    .map((column) => {
      const field = String(column?.fieldCode || column?.field || column?.key || column?.value || '').trim()
      if (!field)
        return null
      return {
        field,
        fieldCode: field,
        label: column.label || column.title || field,
        type: column.type || column.componentKey || column.componentType || 'input',
        required: !!column.required,
        ...(column.props && typeof column.props === 'object' ? { props: column.props } : {}),
        ...(column.optionSource ? { optionSource: column.optionSource } : {}),
      }
    })
    .filter(Boolean)
  if (!fields.length)
    return []
  return [{
    key: relationKey,
    relationKey,
    modelCode: nodeProps.modelCode || relationKey,
    title: resolveSubTableTitle(node),
    tabTitle: resolveSubTableTitle(node),
    header: resolveSubTableTitle(node),
    allowCreate: nodeProps.allowCreate !== false,
    displayMode: nodeProps.displayMode || 'inline_grid',
    fields,
  }]
}

function resolveSubTableValue(node = {}) {
  const relationKey = resolveSubTableRelationKey(node)
  const raw = props.formValue?.[relationKey]
  if (raw && typeof raw === 'object' && !Array.isArray(raw))
    return raw
  if (Array.isArray(raw))
    return { [relationKey]: raw }
  return { [relationKey]: [] }
}

function handleSubTableUpdate(node, value) {
  const relationKey = resolveSubTableRelationKey(node)
  const next = value && typeof value === 'object' ? value : {}
  const rows = Array.isArray(next[relationKey])
    ? next[relationKey]
    : (Array.isArray(next[Object.keys(next)[0]]) ? next[Object.keys(next)[0]] : [])
  emit('fieldChange', relationKey, rows)
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
  if (resolveNodeDisabled(node))
    return
  const events = Array.isArray(node.props?.__events)
    ? node.props.__events
    : (node.props?.__events && typeof node.props.__events === 'object'
        ? Object.values(node.props.__events)
        : [])
  const clickEvents = events.filter(event => !event?.trigger || event.trigger === 'click')
  if (!clickEvents.length) {
    emit('nodeAction', { node, event: null, action: 'click' })
    return
  }
  clickEvents.forEach(event => emit('nodeAction', { node, event, action: event.action || 'click' }))
}

function handleWidgetUpdate(node, value) {
  emit('nodeAction', { node, event: null, action: 'update:props-data', value })
}

function resolveNodeDisabled(node = {}) {
  if (formReadonly.value)
    return true
  const control = node.__runtimeControl || resolveRuntimeControl(node, buildNodeRuleContext())
  return control.disabled === true || control.readonly === true || node.props?.disabled === true
}

function resolveTableColumns(node = {}) {
  const columns = Number(node.props?.columns || node.columns || (node.children || []).length || props.gridCols)
  return Math.max(1, Math.min(24, Number.isFinite(columns) ? columns : props.gridCols))
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

function extractEmbeddedCrudConfigKey(apiBase = '') {
  return String(apiBase || '')
    .trim()
    .replace(/^\/ai\/crud\//i, '')
    .split('?')[0]
    .replace(/\/$/, '')
    .trim()
}

function isDesignPreviewRequest() {
  const q = route.query || {}
  return q.draft === '1' || q.preview === '1' || q.designPreview === '1' || q.edit === '1'
}

function ensureEmbeddedCrudConfigLoaded(configKey = '') {
  const key = String(configKey || '').trim()
  if (!key || embeddedCrudRuntimeByKey[key] || embeddedCrudLoadingKeys.has(key))
    return
  embeddedCrudLoadingKeys.add(key)
  const designPreview = isDesignPreviewRequest()
  crudConfigRender(key, designPreview)
    .then((response) => {
      const config = response?.data && typeof response.data === 'object' ? response.data : null
      if (!config)
        return
      embeddedCrudRuntimeByKey[key] = buildRuntimeCrudProps(config, { designPreview })
    })
    .catch(() => {
      // 配置不存在时保留占位；勿打断主表单
    })
    .finally(() => {
      embeddedCrudLoadingKeys.delete(key)
    })
}

function resolveEmbeddedCrudBind(node = {}) {
  const base = buildCrudProps(node)
  const configKey = extractEmbeddedCrudConfigKey(base.apiBase || base.api)
  ensureEmbeddedCrudConfigLoaded(configKey)
  const loaded = configKey ? embeddedCrudRuntimeByKey[configKey] : null
  if (!loaded) {
    return {
      ...base,
      configKey,
    }
  }
  // 节点显式 apiBase/标题优先；schema/列/搜索来自动态 CRUD 配置
  return {
    ...loaded,
    ...base,
    configKey: loaded.configKey || configKey,
    api: base.api || loaded.api,
    apiBase: base.apiBase || loaded.api,
    apiConfig: Object.keys(base.apiConfig || {}).length ? base.apiConfig : loaded.apiConfig,
    columns: Array.isArray(loaded.columns) && loaded.columns.length ? loaded.columns : base.columns,
    schema: Array.isArray(loaded.columns) && loaded.columns.length ? loaded.columns : base.schema,
    editSchema: loaded.editSchema,
    searchSchema: Array.isArray(loaded.searchSchema) && loaded.searchSchema.length
      ? loaded.searchSchema
      : base.searchSchema,
    uiDocument: loaded.uiDocument,
    formAssets: loaded.formAssets,
    options: loaded.options,
  }
}

function buildCrudProps(node = {}) {
  const nodeProps = node.props || {}
  const crudOptions = nodeProps.crudOptions || {}
  const runtimeColumns = resolveCrudColumns(node)
  const apiBase = normalizeEmbeddedCrudApiBase(
    nodeProps.configKey || nodeProps.apiBase || crudOptions.apiBase,
    resolveEmbeddedCrudApiHints(),
  )
  const apiConfig = resolveEmbeddedCrudApiConfig(nodeProps.apiConfig || nodeProps.api, apiBase)
  return {
    ...crudOptions,
    ...nodeProps,
    title: nodeProps.title || node.label || 'CRUD',
    apiBase,
    api: apiBase,
    apiConfig,
    rowKey: nodeProps.rowKey || 'id',
    columns: runtimeColumns,
    schema: runtimeColumns,
    searchSchema: nodeProps.searchSchema || nodeProps.querySchema || resolveCrudSearchSchema(node),
  }
}

/** 从表单运行上下文推断领域 configKey 前缀（如 hr_） */
function resolveEmbeddedCrudApiHints() {
  const ctx = props.itemContext || {}
  const objectRef = ctx.objectRef || ctx.pageObjectRef || {}
  const configKey = String(
    ctx.configKey
    || objectRef.configKey
    || '',
  ).trim()
  const objectCode = String(
    ctx.objectCode
    || objectRef.objectCode
    || '',
  ).trim()
  let configKeyPrefix = String(ctx.configKeyPrefix || ctx.domainCode || '').trim()
  if (!configKeyPrefix && configKey && objectCode && configKey.endsWith(objectCode))
    configKeyPrefix = configKey.slice(0, -objectCode.length)
  return { configKey, objectCode, configKeyPrefix }
}

/**
 * 表单内嵌 CRUD：纠正历史 kebab / *_default_form apiBase。
 * 动态 CRUD 的真实 configKey 是领域前缀 + objectCode（如 hr_business_object_bkhu），
 * 不是 formKey（business_object_bkhu_default_form）。
 */
function normalizeEmbeddedCrudApiBase(apiBase = '', hints = {}) {
  const text = String(apiBase || '').trim().replace(/\/+/g, '/')
  if (!text)
    return ''
  const withSlash = text.startsWith('/') ? text : `/${text}`
  const rawKey = withSlash
    .replace(/^\/ai\/crud\//i, '')
    .replace(/^\//, '')
    .replace(/-/g, '_')
    .replace(/[^a-z0-9_]/gi, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '')
    // formKey → 对象 CRUD configKey
    .replace(/_default_form$/i, '')
  if (!rawKey)
    return ''

  const preferred = String(hints.configKey || '').trim()
    .replace(/-/g, '_')
    .replace(/[^a-z0-9_]/gi, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '')
  if (preferred && (preferred === rawKey || preferred.endsWith(`_${rawKey}`) || preferred.endsWith(rawKey)))
    return `/ai/crud/${preferred}`

  let key = rawKey
  const prefix = String(hints.configKeyPrefix || '').replace(/_$/g, '').trim()
  if (prefix && key && !key.startsWith(`${prefix}_`) && key !== prefix)
    key = `${prefix}_${key}`
  return `/ai/crud/${key}`
}

function resolveEmbeddedCrudApiConfig(apiConfig, apiBase = '') {
  const source = apiConfig && typeof apiConfig === 'object' && !Array.isArray(apiConfig)
    ? { ...apiConfig }
    : {}
  const list = String(source.list || '').trim()
  if (list)
    return rewriteLegacyEmbeddedCrudApiConfig(source)
  if (!apiBase)
    return source
  return {
    list: `get@${apiBase}/page`,
    detail: `get@${apiBase}/:id`,
    create: `post@${apiBase}`,
    update: `put@${apiBase}`,
    delete: `delete@${apiBase}/:id`,
    ...source,
  }
}

function rewriteLegacyEmbeddedCrudApiConfig(apiConfig = {}) {
  const next = { ...apiConfig }
  for (const [key, value] of Object.entries(next)) {
    const text = String(value || '').trim()
    if (!text || text.includes('/ai/crud/'))
      continue
    // get@/business-object-xxx-default-form/page → get@/ai/crud/business_object_xxx_default_form/page
    const match = text.match(/^([a-zA-Z]+)@(\/[^?\s]+)/)
    if (!match)
      continue
    const method = match[1]
    const path = match[2]
    if (path.startsWith('/ai/crud/'))
      continue
    const normalized = normalizeEmbeddedCrudApiBase(
      path.replace(/\/page$/i, '').replace(/\/:id$/i, ''),
      resolveEmbeddedCrudApiHints(),
    )
    if (!normalized)
      continue
    if (/\/page$/i.test(path))
      next[key] = `${method}@${normalized}/page`
    else if (/\/:id$/i.test(path))
      next[key] = `${method}@${normalized}/:id`
    else
      next[key] = `${method}@${normalized}`
  }
  return next
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

function resolveLayoutClass(node = {}, baseClass = '') {
  return [
    baseClass,
    node.className,
    node.class,
    {
      'is-borderless': isLayoutBorderHidden(node),
    },
  ]
}

function resolveLayoutStyle(node = {}) {
  const designerStyle = resolveDesignerStyle(node)
  const style = {
    ...(isPlainObject(designerStyle.customStyle) ? designerStyle.customStyle : {}),
  }
  ;[
    'width',
    'height',
    'minHeight',
    'backgroundColor',
    'borderColor',
    'borderStyle',
    'borderWidth',
    'borderRadius',
    'boxShadow',
    'opacity',
  ].forEach((key) => {
    if (designerStyle[key] !== undefined && designerStyle[key] !== '')
      style[key] = designerStyle[key]
  })
  if (isLayoutBorderHidden(node)) {
    style.borderColor = 'transparent'
    style.borderStyle = 'none'
  }
  if (Object.keys(style).length)
    return node.style ? [node.style, style] : style
  return node.style
}

function isLayoutBorderHidden(node = {}) {
  const designerStyle = resolveDesignerStyle(node)
  return node.props?.bordered === false
    || designerStyle.hideInnerBorder
    || designerStyle.borderStyle === 'none'
}

function resolveDesignerStyle(node = {}) {
  return isPlainObject(node.props?.__designerStyle) ? node.props.__designerStyle : {}
}

function isPlainObject(value) {
  return Object.prototype.toString.call(value) === '[object Object]'
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

.af-empty-layout-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  font-size: 12px;
  color: var(--text-tertiary);
  border: 1px dashed var(--border-light);
  border-radius: 3px;
}

.af-layout-card,
.af-layout-tabs,
.af-layout-collapse {
  width: 100%;
}

.af-layout-card :deep(.n-card__content) {
  padding-top: 12px;
}

.af-layout-empty-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  padding: 12px;
  color: var(--text-tertiary);
  font-size: 12px;
  border: 1px dashed #d4d4d8;
  border-radius: 4px;
  background: var(--bg-secondary);
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
  border: 1px solid var(--border-light);
  border-radius: 6px;
}

.af-layout-table.is-borderless {
  border-color: transparent !important;
  border-style: none !important;
}

.af-layout-table :deep(.af-layout-grid) {
  gap: 0 !important;
}

.af-layout-table.is-borderless :deep(.af-layout-table-cell) {
  border-right-color: transparent !important;
  border-bottom-color: transparent !important;
}

.af-layout-table-cell {
  min-height: 56px;
  padding: 12px;
  border-right: 1px solid var(--border-light);
  /* border-bottom: 1px solid var(--border-light); */
}

.af-layout-table-cell.is-borderless {
  border-right-color: transparent !important;
  border-bottom-color: transparent !important;
}

.af-layout-sub-table {
  width: 100%;
  min-width: 0;
  padding: 8px;
  box-sizing: border-box;
}

.af-layout-sub-table__header {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #18181b);
}

.af-layout-crud {
  width: 100%;
}
</style>
