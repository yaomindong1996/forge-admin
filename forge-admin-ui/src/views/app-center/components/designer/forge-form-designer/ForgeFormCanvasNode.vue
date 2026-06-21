<template>
  <div
    class="node-wrap"
    :class="{ selected: isSelected }"
    :style="nodeWrapStyle"
    :data-forge-node-id="component.id"
    :data-forge-parent-id="parentId"
    :data-forge-index="index"
    @dragover.prevent.stop="handleNodeDragOver"
    @dragleave="handleNodeDragLeave"
    @drop.stop="handleNodeDrop"
  >
    <div
      class="drop-line before"
      :class="{ active: beforeActive }"
      :style="dropIndicatorStyle"
    />
    <article
      ref="nodeRef"
      class="canvas-node"
      :class="[`node-${component.componentKey}`, { 'selected': isSelected, 'layout': isLayout, 'dragging': dragging, 'resizing': resizing, 'structural-slot': isStructuralSlot, 'border-hidden': designerBorderHidden }]"
      :style="nodeCustomStyle"
      draggable="false"
      tabindex="0"
      @click.stop="$emit('select', component.id)"
      @focus="$emit('select', component.id)"
    >
      <div v-if="!isStructuralSlot" class="node-overlay">
        <div class="quick-actions">
          <button
            v-if="isField"
            type="button"
            class="quick-switch"
            :class="{ active: component.validation?.required }"
            role="switch"
            :aria-checked="Boolean(component.validation?.required)"
            title="必填"
            @click.stop="toggleRequired"
            @pointerdown.stop
          >
            <span>必填</span>
            <span class="switch-track">
              <span class="switch-thumb" />
            </span>
          </button>
          <button
            type="button"
            class="icon-action"
            title="复制"
            @click.stop="duplicateNode"
            @pointerdown.stop
          >
            <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" data-icon="CopyOutlined">
              <path d="M9 3a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v12a1 1 0 1 1-2 0V4h-9a1 1 0 0 1-1-1Z" fill="currentColor" />
              <path d="M5 6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2H5Zm0 2h10v12H5V8Z" fill="currentColor" />
            </svg>
          </button>
          <button
            type="button"
            class="icon-action danger"
            title="删除"
            @click.stop="removeNode"
            @pointerdown.stop
          >
            <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" data-icon="DeleteTrashOutlined">
              <path d="M8 4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2h5a1 1 0 1 1 0 2h-1v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6H3a1 1 0 0 1 0-2h5ZM6 6v14h12V6H6Zm4 3a1 1 0 0 1 1 1v6a1 1 0 1 1-2 0v-6a1 1 0 0 1 1-1Zm4 0a1 1 0 0 1 1 1v6a1 1 0 1 1-2 0v-6a1 1 0 0 1 1-1Z" fill="currentColor" />
            </svg>
          </button>
        </div>
        <n-dropdown
          trigger="click"
          placement="bottom"
          :options="nodeMenuOptions"
          @select="handleNodeMenuSelect"
        >
          <button
            type="button"
            class="menu-trigger"
            title="更多操作"
            @click.stop
            @pointerdown.stop
          >
            <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" data-icon="MoreVerticalOutlined">
              <path d="M12 5.5A1.75 1.75 0 1 1 12 2a1.75 1.75 0 0 1 0 3.5Zm0 8.225a1.75 1.75 0 1 1 0-3.5 1.75 1.75 0 0 1 0 3.5ZM12 22a1.75 1.75 0 1 1 0-3.5 1.75 1.75 0 0 1 0 3.5Z" fill="currentColor" />
            </svg>
          </button>
        </n-dropdown>
        <span
          class="drag-handle"
          title="拖动排序"
          @pointerdown.stop="startPointerDrag"
          @click.stop
        >
          <svg
            width="1em"
            height="1em"
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            data-icon="DragOutlined"
            class="drag-handle-svg"
          >
            <path
              d="M8.25 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm0 7.25a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm1.75 5.5a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0ZM14.753 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5ZM16.5 12a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0Zm-1.747 9a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Z"
              fill="currentColor"
            />
          </svg>
        </span>
      </div>
      <div v-if="isSelected" class="node-resize-handles" @click.stop>
        <span
          v-for="direction in resizeDirections"
          :key="direction"
          class="resize-anchor"
          :class="`anchor-${direction}`"
          :title="resizeAnchorTitle(direction)"
          @pointerdown.stop.prevent="startNodeResize(direction, $event)"
        />
      </div>

      <AiFormItem
        v-if="isField"
        :field="previewField"
        :value="previewValue"
        :form-data="previewFormData"
        :context="previewContext"
        @update:value="previewValue = $event"
      />
      <AiFormSectionTitle
        v-else-if="isFormDividerComponent(component)"
        class="form-divider-preview"
        v-bind="buildFormDividerProps(component)"
      />
      <AiFormGroupTitle
        v-else-if="isTitle"
        v-bind="buildGroupTitleProps(component)"
      />
      <div v-else-if="isButton" class="button-preview">
        <n-button
          :type="component.props?.type || 'primary'"
          :size="component.props?.size || 'medium'"
          :secondary="!!component.props?.secondary"
          :tertiary="!!component.props?.tertiary"
          :quaternary="!!component.props?.quaternary"
          :dashed="!!component.props?.dashed"
          :round="!!component.props?.round"
          :block="!!component.props?.block"
          :loading="!!component.props?.loading"
          :disabled="!!component.props?.disabled"
        >
          {{ component.props?.text || component.label || '按钮' }}
        </n-button>
      </div>
      <div
        v-else-if="isCrudBlock"
        class="crud-preview"
        @dragenter.prevent.stop="handleInsideDragOver"
        @dragover.prevent.stop="handleInsideDragOver"
        @drop.stop="handleInsideDrop"
      >
        <div class="crud-live-preview">
          <AiCrudPage
            v-bind="crudPreviewOptions"
            :lazy="true"
            :api-config="crudApiConfig"
            :row-key="component.props?.rowKey || 'id'"
            :columns="crudPreviewColumns"
            :search-schema="crudSearchSchema"
            :edit-schema="crudEditSchema"
          />
        </div>
        <div v-if="!component.children?.length" class="crud-empty-drop" :class="{ active: activeInside }">
          拖入字段生成查询、表格列和编辑表单
        </div>
      </div>
      <n-card
        v-else-if="isCardLayout"
        class="real-layout real-layout-card"
        :size="component.props?.size || 'small'"
        :bordered="component.props?.bordered !== false"
        :embedded="!!component.props?.embedded"
        :segmented="component.props?.segmented || false"
        :hoverable="!!component.props?.hoverable"
      >
        <template #header>
          {{ component.props?.header || component.label || '卡片分组' }}
        </template>
        <div
          class="layout-children real-layout-children"
          :class="{ active: activeInside }"
          @dragenter.prevent.stop="handleInsideDragOver"
          @dragover.prevent.stop="handleInsideDragOver"
          @drop.stop="handleInsideDrop"
        >
          <div v-if="!component.children?.length" class="empty-child-zone" :class="{ active: activeInside }">
            拖入字段或布局
          </div>
          <ForgeFormCanvasNode
            v-for="(child, childIndex) in component.children"
            :key="child.id"
            :component="child"
            :fields="fields"
            :schema="schema"
            :selected-id="selectedId"
            :depth="depth + 1"
            :parent-id="component.id"
            :index="childIndex"
            @select="$emit('select', $event)"
            @update:schema="$emit('update:schema', $event)"
            @configure="$emit('configure', $event)"
            @drop-before="event => handleChildDrop(childIndex, event)"
            @drop-after="event => handleChildDrop(childIndex + 1, event)"
          />
        </div>
      </n-card>
      <n-tabs
        v-else-if="isTabsLayout"
        class="real-layout real-layout-tabs"
        :type="component.props?.type || 'line'"
        :size="component.props?.size || 'medium'"
        :placement="component.props?.placement || 'top'"
        :trigger="component.props?.trigger || 'click'"
        :animated="component.props?.animated !== false"
        :closable="!!component.props?.closable"
        :addable="!!component.props?.addable"
        :justify-content="component.props?.justifyContent"
        :tabs-padding="component.props?.tabsPadding"
        :pane-style="component.props?.paneStyle"
        :tab-style="component.props?.tabStyle"
        :default-value="component.children?.[0]?.props?.name || component.children?.[0]?.id"
      >
        <n-tab-pane
          v-for="(child, childIndex) in component.children"
          :key="child.id"
          :name="child.props?.name || child.id"
          :tab="child.props?.label || child.label || `标签 ${childIndex + 1}`"
        >
          <ForgeFormCanvasNode
            :component="child"
            :fields="fields"
            :schema="schema"
            :selected-id="selectedId"
            :depth="depth + 1"
            :parent-id="component.id"
            :index="childIndex"
            @select="$emit('select', $event)"
            @update:schema="$emit('update:schema', $event)"
            @configure="$emit('configure', $event)"
            @drop-before="event => handleChildDrop(childIndex, event)"
            @drop-after="event => handleChildDrop(childIndex + 1, event)"
          />
        </n-tab-pane>
        <n-tab-pane v-if="!component.children?.length" name="empty" tab="标签一">
          <div
            class="layout-children real-layout-children"
            :class="{ active: activeInside }"
            @dragenter.prevent.stop="handleInsideDragOver"
            @dragover.prevent.stop="handleInsideDragOver"
            @drop.stop="handleInsideDrop"
          >
            <div class="empty-child-zone" :class="{ active: activeInside }">
              拖入标签页
            </div>
          </div>
        </n-tab-pane>
      </n-tabs>
      <n-collapse
        v-else-if="isCollapseLayout"
        class="real-layout real-layout-collapse"
        :accordion="!!component.props?.accordion"
        :arrow-placement="component.props?.arrowPlacement || 'left'"
        :display-directive="component.props?.displayDirective || 'if'"
        :trigger-areas="component.props?.triggerAreas || ['main', 'arrow']"
        :default-expanded-names="component.props?.defaultExpandedNames || component.children?.map(child => child.props?.name || child.id)"
        :expanded-names="component.props?.expandedNames"
      >
        <n-collapse-item
          v-for="(child, childIndex) in component.children"
          :key="child.id"
          :name="child.props?.name || child.id"
          :title="child.props?.title || child.label || `分组 ${childIndex + 1}`"
        >
          <ForgeFormCanvasNode
            :component="child"
            :fields="fields"
            :schema="schema"
            :selected-id="selectedId"
            :depth="depth + 1"
            :parent-id="component.id"
            :index="childIndex"
            @select="$emit('select', $event)"
            @update:schema="$emit('update:schema', $event)"
            @configure="$emit('configure', $event)"
            @drop-before="event => handleChildDrop(childIndex, event)"
            @drop-after="event => handleChildDrop(childIndex + 1, event)"
          />
        </n-collapse-item>
        <n-collapse-item v-if="!component.children?.length" name="empty" title="分组一">
          <div
            class="layout-children real-layout-children"
            :class="{ active: activeInside }"
            @dragenter.prevent.stop="handleInsideDragOver"
            @dragover.prevent.stop="handleInsideDragOver"
            @drop.stop="handleInsideDrop"
          >
            <div class="empty-child-zone" :class="{ active: activeInside }">
              拖入折叠项
            </div>
          </div>
        </n-collapse-item>
      </n-collapse>
      <div
        v-else
        class="layout-children"
        :class="{ 'grid-layout-children': isGridRow, 'table-layout-children': isTableLayout, 'active': activeInside }"
        :style="childrenGridStyle"
        @dragenter.prevent.stop="handleInsideDragOver"
        @dragover.prevent.stop="handleInsideDragOver"
        @drop.stop="handleInsideDrop"
      >
        <div v-if="!component.children?.length" class="empty-child-zone" :class="{ active: activeInside }">
          拖入字段或布局
        </div>
        <ForgeFormCanvasNode
          v-for="(child, childIndex) in component.children"
          :key="child.id"
          :component="child"
          :fields="fields"
          :schema="schema"
          :selected-id="selectedId"
          :depth="depth + 1"
          :parent-id="component.id"
          :index="childIndex"
          @select="$emit('select', $event)"
          @update:schema="$emit('update:schema', $event)"
          @configure="$emit('configure', $event)"
          @drop-before="event => handleChildDrop(childIndex, event)"
          @drop-after="event => handleChildDrop(childIndex + 1, event)"
        />
      </div>
    </article>
    <div
      class="drop-line after"
      :class="{ active: afterActive }"
      :style="dropIndicatorStyle"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import AiFormGroupTitle from '@/components/ai-form/AiFormGroupTitle.vue'
import AiFormItem from '@/components/ai-form/AiFormItem.vue'
import AiFormSectionTitle from '@/components/ai-form/AiFormSectionTitle.vue'
import {
  canAcceptDesignerChild,
  createComponentFromField,
  duplicateDesignerComponent,
  getDesignerComponent,
  insertDesignerComponent,
  isFieldComponent,
  isLayoutComponent,
  moveDesignerComponent,
  removeDesignerComponent,
  updateDesignerComponent,
} from '../form-first/formDesignerSchema'
import {
  clearDesignerDragSource,
  clearDesignerDropError,
  clearDesignerDropKey,
  designerDragSourceId,
  designerDropKey,
  setDesignerDragSource,
  setDesignerDropError,
  setDesignerDropKey,
} from './designerDragState'
import { createForgeFieldTemplateComponent, createForgeLayoutComponent } from './designerLayoutFactory'

defineOptions({
  name: 'ForgeFormCanvasNode',
})

const props = defineProps({
  component: {
    type: Object,
    required: true,
  },
  schema: {
    type: Object,
    required: true,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  selectedId: {
    type: String,
    default: '',
  },
  depth: {
    type: Number,
    default: 0,
  },
  parentId: {
    type: String,
    default: '',
  },
  index: {
    type: Number,
    default: 0,
  },
})

const emit = defineEmits(['select', 'update:schema', 'dropBefore', 'dropAfter', 'configure'])

const DRAG_COMPONENT_MIME = 'application/x-forge-form-component'
const DRAG_FIELD_MIME = 'application/x-forge-form-field'
const DRAG_LAYOUT_MIME = 'application/x-forge-form-layout'
const DRAG_TEMPLATE_MIME = 'application/x-forge-form-template'
const FORGE_DRAG_TYPES = [DRAG_COMPONENT_MIME, DRAG_FIELD_MIME, DRAG_LAYOUT_MIME, DRAG_TEMPLATE_MIME]
const MAX_FORM_GRID_COLUMNS = 24
const resizeDirections = ['n', 'e', 's', 'w', 'ne', 'nw', 'se', 'sw']
let activeDragImage = null
let activeDragImageOffset = { x: 0, y: 0 }
let activePointerDropTarget = null
let activePointerHandle = null
let activePointerId = null
let pointerDragStarted = false
let activeResizeHandle = null
let activeResizePointerId = null
let activeResizeState = null

const activeDropPosition = ref('')
const dragging = ref(false)
const resizing = ref(false)
const nodeRef = ref(null)
const nodeHeight = ref(72)
const previewValue = ref(null)
let resizeObserver = null

const isSelected = computed(() => props.selectedId === props.component.id)
const isField = computed(() => isFieldComponent(props.component))
const isLayout = computed(() => isLayoutComponent(props.component))
const isTitle = computed(() => ['title', 'fcTitle'].includes(props.component.componentKey))
const isButton = computed(() => ['button', 'elButton'].includes(props.component.componentKey))
const isCrudBlock = computed(() => ['AiCrudPage', 'crudBlock'].includes(props.component.componentKey))
const isGridRow = computed(() => ['row', 'fcRow'].includes(props.component.componentKey))
const isTableLayout = computed(() => ['table', 'fcTable'].includes(props.component.componentKey))
const isCardLayout = computed(() => ['card', 'elCard'].includes(props.component.componentKey))
const isTabsLayout = computed(() => ['tabs', 'elTabs'].includes(props.component.componentKey))
const isCollapseLayout = computed(() => ['collapse', 'elCollapse'].includes(props.component.componentKey))
const isStructuralSlot = computed(() => ['col', 'tableGrid', 'fcTableGrid', 'tabPane', 'elTabPane', 'collapseItem', 'elCollapseItem'].includes(props.component.componentKey))
const selectedDesignerStyle = computed(() => props.component.props?.__designerStyle || {})
const designerBorderHidden = computed(() => selectedDesignerStyle.value.hideInnerBorder || selectedDesignerStyle.value.borderStyle === 'none')
const beforeDropKey = computed(() => `${props.component.id}:before`)
const afterDropKey = computed(() => `${props.component.id}:after`)
const insideDropKey = computed(() => `${props.component.id}:inside`)
const beforeActive = computed(() => designerDropKey.value === beforeDropKey.value)
const afterActive = computed(() => designerDropKey.value === afterDropKey.value)
const activeInside = computed(() => designerDropKey.value === insideDropKey.value)
const displayLabel = computed(() => props.component.label || props.component.props?.header || props.component.props?.title || props.component.componentKey)
const previewContext = computed(() => ({ mode: 'designer-preview' }))
const previewFormData = computed(() => ({
  [props.component.fieldBinding?.fieldCode || props.component.id]: previewValue.value,
}))
const rootColumns = computed(() => Math.max(1, Math.min(MAX_FORM_GRID_COLUMNS, Number(props.schema.layout?.gridColumns || 2))))
const parentGridColumns = computed(() => {
  if (props.component.componentKey !== 'col')
    return rootColumns.value
  const parent = props.parentId ? getDesignerComponent(props.schema, props.parentId) : null
  if (!['row', 'fcRow'].includes(parent?.componentKey))
    return rootColumns.value
  const columns = Number(parent.props?.columns || MAX_FORM_GRID_COLUMNS)
  return Math.max(1, Math.min(MAX_FORM_GRID_COLUMNS, Number.isFinite(columns) ? columns : MAX_FORM_GRID_COLUMNS))
})
const nodeSpan = computed(() => Math.max(1, Math.min(parentGridColumns.value, Number(props.component.layout?.span || props.component.props?.span || 1))))
const nodeWrapStyle = computed(() => ({
  gridColumn: props.depth === 0 || props.component.componentKey === 'col' ? `span ${nodeSpan.value}` : undefined,
}))
const dropIndicatorStyle = computed(() => ({
  '--drop-placeholder-height': `${Math.max(nodeHeight.value, 44)}px`,
}))
const nodeCustomStyle = computed(() => {
  const designerStyle = props.component.props?.__designerStyle || {}
  const minHeight = designerStyle.minHeight || undefined
  const style = {
    ...(designerStyle.customStyle || {}),
    width: designerStyle.width || undefined,
    height: designerStyle.height || undefined,
    minHeight: isStructuralSlot.value ? undefined : minHeight,
    backgroundColor: designerStyle.backgroundColor || undefined,
    borderColor: designerStyle.borderColor || undefined,
    borderStyle: designerStyle.borderStyle === 'none' ? undefined : designerStyle.borderStyle || undefined,
    borderRadius: designerStyle.borderRadius || undefined,
    boxShadow: designerStyle.boxShadow || undefined,
    opacity: designerStyle.opacity || undefined,
  }
  if (minHeight)
    style['--forge-slot-min-height'] = minHeight
  return style
})
const nodeMenuOptions = computed(() => [
  { label: '配置', key: 'config' },
  { label: props.component.validation?.required ? '取消必填' : '设为必填', key: 'required', disabled: !isField.value },
  { label: '复制', key: 'copy' },
  {
    label: '更换背景色',
    key: 'background',
    children: [
      { label: '恢复默认', key: 'bg-default' },
      { label: '浅灰', key: 'bg-gray' },
      { label: '浅蓝', key: 'bg-blue' },
      { label: '浅绿', key: 'bg-green' },
      { label: '浅黄', key: 'bg-yellow' },
    ],
  },
  {
    label: '背景描边',
    key: 'border',
    children: [
      { label: '恢复默认', key: 'border-default' },
      { label: '灰色虚线', key: 'border-dashed' },
      { label: '蓝色实线', key: 'border-blue' },
      { label: '隐藏边框', key: 'border-none' },
    ],
  },
  { label: '移入', key: 'move-into', disabled: true },
  { type: 'divider', key: 'divider' },
  { label: '删除', key: 'delete' },
])
const childrenGridStyle = computed(() => {
  if (!isGridRow.value && !isTableLayout.value)
    return {}
  const count = isTableLayout.value ? resolveTableColumns(props.component) : resolveRowColumns(props.component)
  return {
    gridTemplateColumns: `repeat(${count}, minmax(0, 1fr))`,
    columnGap: `${resolveGap(props.component.props?.gutter, 12)}px`,
    rowGap: `${resolveGap(props.component.props?.rowGap, 8)}px`,
  }
})
const previewField = computed(() => {
  const componentKey = props.component.componentKey || 'input'
  const fieldCode = props.component.fieldBinding?.fieldCode || props.component.id || componentKey
  const rawProps = resolveRuntimeOptionProps({ ...(props.component.props || {}) }, componentKey)
  delete rawProps.disabled
  delete rawProps.readonly
  return {
    field: fieldCode,
    label: displayLabel.value,
    type: normalizePreviewFieldType(componentKey),
    span: nodeSpan.value,
    labelWidth: props.component.layout?.labelWidth || props.schema.layout?.labelWidth || 100,
    placeholder: rawProps.placeholder || buildPreviewPlaceholder(componentKey, displayLabel.value),
    required: Boolean(props.component.validation?.required),
    clearable: true,
    disabled: false,
    readonly: false,
    dictType: rawProps.dictType,
    options: resolvePreviewOptions(rawProps, componentKey),
    props: {
      ...rawProps,
      disabled: false,
      readonly: false,
    },
    showFeedback: false,
  }
})
const crudDesignerFields = computed(() => collectDesignerFields(props.component.children || []))
const crudSearchFields = computed(() => {
  const maxVisible = Math.max(1, Math.min(6, Number(props.component.props?.crudOptions?.searchMaxVisibleFields || 3)))
  return crudDesignerFields.value.filter((field, index) => isCrudRoleEnabled(field, 'search', index, maxVisible))
})
const crudTableFields = computed(() => crudDesignerFields.value.filter((field, index) => isCrudRoleEnabled(field, 'table', index)))
const crudEditFields = computed(() => crudDesignerFields.value.filter((field, index) => isCrudRoleEnabled(field, 'edit', index)))
const crudSearchSchema = computed(() => {
  const fields = crudSearchFields.value
  return fields.length ? fields.map(field => toCrudFormField(field, 'search')) : [buildFallbackCrudField('keyword', '关键词', 'input')]
})
const crudEditSchema = computed(() => {
  const fields = crudEditFields.value.length
    ? crudEditFields.value
    : [
        buildFallbackCrudField('name', '名称', 'input'),
        buildFallbackCrudField('status', '状态', 'select'),
      ]
  return fields.map(field => toCrudFormField(field, 'edit'))
})
const crudPreviewColumns = computed(() => {
  const fields = crudTableFields.value.length
    ? crudTableFields.value
    : [
        buildFallbackCrudField('name', '名称', 'input'),
        buildFallbackCrudField('status', '状态', 'select'),
        buildFallbackCrudField('createTime', '创建时间', 'datetime'),
      ]
  return [
    ...fields.slice(0, 6).map(field => ({
      title: field.props?.__crudConfig?.table?.title || field.label || field.fieldBinding?.fieldCode || field.componentKey,
      key: field.fieldBinding?.fieldCode || field.id,
      width: field.props?.__crudConfig?.table?.width || undefined,
      minWidth: field.props?.__crudConfig?.table?.minWidth || (field.componentKey === 'textarea' ? 180 : 120),
      align: field.props?.__crudConfig?.table?.align || undefined,
      fixed: field.props?.__crudConfig?.table?.fixed || undefined,
      sorter: field.props?.__crudConfig?.table?.sorter || undefined,
      ellipsis: field.props?.__crudConfig?.table?.ellipsis === false ? false : { tooltip: true },
    })),
    {
      title: '操作',
      key: 'actions',
      width: 140,
      fixed: 'right',
      actions: [
        { label: '编辑', key: 'edit', type: 'primary' },
        { label: '删除', key: 'delete', type: 'error' },
      ],
    },
  ]
})
const crudApiConfig = computed(() => props.component.props?.apiConfig || buildCrudApiConfig(props.component.props?.apiBase))
const crudPreviewOptions = computed(() => ({
  showSearch: true,
  showPagination: true,
  searchGridCols: 3,
  searchLabelWidth: 'auto',
  searchEnableCollapse: false,
  searchMaxVisibleFields: 3,
  searchYGap: 10,
  editGridCols: 2,
  editLabelWidth: 'auto',
  editLabelPlacement: 'left',
  editLabelAlign: 'right',
  editSize: 'small',
  editShowFeedback: false,
  editXGap: 12,
  editYGap: 8,
  tableSize: 'small',
  renderMode: 'table',
  showRenderModeSwitch: false,
  hideToolbar: false,
  hideAdd: false,
  hideBatchDelete: false,
  hideSelection: false,
  striped: false,
  bordered: false,
  showImport: false,
  showExport: false,
  showExportTasks: false,
  loadDetailOnEdit: false,
  pageSize: 5,
  ...(props.component.props?.crudOptions || {}),
}))

onMounted(() => {
  syncNodeHeight()
  if (typeof ResizeObserver === 'undefined' || !nodeRef.value)
    return
  resizeObserver = new ResizeObserver(syncNodeHeight)
  resizeObserver.observe(nodeRef.value)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  cancelPointerDrag()
  cancelNodeResize()
  removeDragImage()
})

function syncNodeHeight() {
  if (!nodeRef.value)
    return
  nodeHeight.value = Math.max(44, Math.round(nodeRef.value.offsetHeight || 72))
}

function startPointerDrag(event) {
  if (event.button !== 0)
    return
  event.preventDefault()
  activePointerHandle = event.currentTarget
  activePointerId = event.pointerId
  activePointerHandle?.setPointerCapture?.(event.pointerId)
  emit('select', props.component.id)
  dragging.value = true
  pointerDragStarted = true
  activePointerDropTarget = null
  document.documentElement.style.setProperty('--forge-designer-drag-height', `${Math.max(nodeHeight.value, 44)}px`)
  document.documentElement.dataset.forgeDesignerDraggingId = props.component.id
  document.body.classList.add('forge-pointer-dragging')
  setDesignerDragSource(props.component.id)
  clearDesignerDropKey()
  mountPointerDragImage(event)
  window.addEventListener('pointermove', handlePointerMove, { capture: true, passive: false })
  window.addEventListener('pointerup', finishPointerDrag, true)
  window.addEventListener('pointercancel', cancelPointerDrag, true)
}

function cleanupPointerDrag() {
  dragging.value = false
  pointerDragStarted = false
  activePointerDropTarget = null
  activeDropPosition.value = ''
  document.documentElement.style.removeProperty('--forge-designer-drag-height')
  delete document.documentElement.dataset.forgeDesignerDraggingId
  document.body.classList.remove('forge-pointer-dragging')
  try {
    activePointerHandle?.releasePointerCapture?.(activePointerId)
  }
  catch {
    // Pointer capture can already be released by the browser after cancel/up.
  }
  activePointerHandle = null
  activePointerId = null
  clearDesignerDragSource()
  clearDesignerDropKey()
  removeDragImage()
  window.removeEventListener('pointermove', handlePointerMove, true)
  window.removeEventListener('pointerup', finishPointerDrag, true)
  window.removeEventListener('pointercancel', cancelPointerDrag, true)
}

function handlePointerMove(event) {
  if (!pointerDragStarted)
    return
  event.preventDefault()
  updateDragImagePosition(event)
  updatePointerDropPreview(event)
}

function finishPointerDrag(event) {
  if (!pointerDragStarted)
    return
  event.preventDefault()
  const target = activePointerDropTarget
  cleanupPointerDrag()
  if (!target)
    return
  emit('select', props.component.id)
  emit('update:schema', moveDesignerComponent(props.schema, props.component.id, target))
}

function cancelPointerDrag() {
  cleanupPointerDrag()
}

function startNodeResize(direction, event) {
  if (event.button !== 0 || !nodeRef.value)
    return
  event.preventDefault()
  emit('select', props.component.id)
  const rect = nodeRef.value.getBoundingClientRect()
  activeResizeHandle = event.currentTarget
  activeResizePointerId = event.pointerId
  activeResizeHandle?.setPointerCapture?.(event.pointerId)
  activeResizeState = {
    direction,
    startX: event.clientX,
    startY: event.clientY,
    startWidth: rect.width,
    startHeight: rect.height,
    startSpan: nodeSpan.value,
    columnWidth: Math.max(24, rect.width / Math.max(1, nodeSpan.value)),
    designerStyle: { ...(props.component.props?.__designerStyle || {}) },
  }
  resizing.value = true
  window.addEventListener('pointermove', handleNodeResizeMove, { capture: true, passive: false })
  window.addEventListener('pointerup', finishNodeResize, true)
  window.addEventListener('pointercancel', cancelNodeResize, true)
}

function handleNodeResizeMove(event) {
  if (!activeResizeState)
    return
  event.preventDefault()
  const deltaX = event.clientX - activeResizeState.startX
  const deltaY = event.clientY - activeResizeState.startY
  const direction = activeResizeState.direction || ''
  const patch = {}
  const nextDesignerStyle = { ...activeResizeState.designerStyle }

  if (direction.includes('e') || direction.includes('w')) {
    const horizontalDelta = deltaX * (direction.includes('w') ? -1 : 1)
    if (isSpanResizable()) {
      const nextSpan = clamp(
        activeResizeState.startSpan + Math.round(horizontalDelta / activeResizeState.columnWidth),
        1,
        parentGridColumns.value,
      )
      patch.layout = { span: nextSpan }
      if (props.component.componentKey === 'col')
        patch.props = { span: nextSpan }
    }
    else {
      nextDesignerStyle.width = `${Math.round(Math.max(96, activeResizeState.startWidth + horizontalDelta))}px`
      nextDesignerStyle.widthMode = 'fixed'
    }
  }

  if (direction.includes('s') || direction.includes('n')) {
    const verticalDelta = deltaY * (direction.includes('n') ? -1 : 1)
    nextDesignerStyle.minHeight = `${Math.round(Math.max(44, activeResizeState.startHeight + verticalDelta))}px`
    nextDesignerStyle.heightMode = 'custom'
  }

  patch.props = {
    ...(patch.props || {}),
    __designerStyle: nextDesignerStyle,
  }
  emit('update:schema', updateDesignerComponent(props.schema, props.component.id, patch))
}

function finishNodeResize() {
  cleanupNodeResize()
}

function cancelNodeResize() {
  cleanupNodeResize()
}

function cleanupNodeResize() {
  resizing.value = false
  activeResizeState = null
  try {
    activeResizeHandle?.releasePointerCapture?.(activeResizePointerId)
  }
  catch {
    // Pointer capture can already be released by the browser after cancel/up.
  }
  activeResizeHandle = null
  activeResizePointerId = null
  window.removeEventListener('pointermove', handleNodeResizeMove, true)
  window.removeEventListener('pointerup', finishNodeResize, true)
  window.removeEventListener('pointercancel', cancelNodeResize, true)
}

function isSpanResizable() {
  return props.depth === 0 || props.component.componentKey === 'col'
}

function resizeAnchorTitle(direction) {
  if (direction === 'n' || direction === 's')
    return '调整高度'
  if (direction === 'e' || direction === 'w')
    return '调整宽度'
  return '调整宽高'
}

function handleBeforeDrop(event) {
  clearDropPreview()
  emit('dropBefore', event)
}

function handleAfterDrop(event) {
  clearDropPreview()
  emit('dropAfter', event)
}

function handleNodeDragOver(event) {
  if (!hasForgeDragType(event))
    return
  const target = event.target
  const insideDropArea = target?.closest?.('.layout-children, .crud-empty-drop')
  if (insideDropArea && nodeRef.value?.contains?.(insideDropArea)) {
    handleInsideDragOver(event)
    return
  }
  if (designerDragSourceId.value === props.component.id || document.documentElement.dataset.forgeDesignerDraggingId === props.component.id) {
    clearDropPreview()
    showInvalidDrop('不能拖入组件自身')
    return
  }
  const rect = nodeRef.value?.getBoundingClientRect()
  if (!rect)
    return
  const ratio = rect.height > 0 ? (event.clientY - rect.top) / rect.height : 0.5
  const canDropInside = canPreviewDropIntoCurrentNode(event)

  if (canDropInside && ratio > 0.28 && ratio < 0.72) {
    clearDesignerDropError()
    activeDropPosition.value = 'inside'
    setDesignerDropKey(insideDropKey.value)
    return
  }

  clearDesignerDropError()
  const nextPosition = resolveDropPosition(ratio)
  activeDropPosition.value = nextPosition
  setDesignerDropKey(nextPosition === 'before' ? beforeDropKey.value : afterDropKey.value)
}

function handleInsideDragOver(event) {
  if (!hasForgeDragType(event))
    return
  if (designerDragSourceId.value === props.component.id || document.documentElement.dataset.forgeDesignerDraggingId === props.component.id) {
    clearDropPreview()
    showInvalidDrop('不能拖入组件自身')
    return
  }
  if (!canDropIntoCurrentNode(event)) {
    showInvalidDrop(resolveInvalidDropMessage(event))
    return
  }
  const slotTarget = resolveContainerSlotTarget(event)
  clearDesignerDropError()
  activeDropPosition.value = 'inside'
  event.dataTransfer.dropEffect = event.dataTransfer.effectAllowed === 'copy' ? 'copy' : 'move'
  setDesignerDropKey(slotTarget?.dropKey || insideDropKey.value)
}

function handleNodeDragLeave() {
  // Drag events fire leave/enter repeatedly for form controls inside the node.
  // Keep the latest preview until the next dragover/drop/dragend to avoid flicker.
}

function handleNodeDrop(event) {
  if (beforeActive.value) {
    handleBeforeDrop(event)
    return
  }
  if (afterActive.value) {
    handleAfterDrop(event)
    return
  }
  handleInsideDrop(event)
}

function handleInsideDrop(event) {
  const slotTarget = resolveContainerSlotTarget(event)
  if (slotTarget) {
    handleDropToTarget(slotTarget, event)
    return
  }
  if (isGridRow.value) {
    handleRowDropToColumn(0, event)
    return
  }
  if (isTableLayout.value) {
    handleTableDropToCell(0, event)
    return
  }
  if (!canDropIntoCurrentNode(event)) {
    showInvalidDrop(resolveInvalidDropMessage(event))
    return
  }
  handleDropToChildren((props.component.children || []).length, event)
}

function handleChildDrop(index, event) {
  if (isGridRow.value) {
    handleRowDropToColumn(index, event)
    return
  }
  if (isTableLayout.value) {
    const child = resolveDraggedComponent(event)
    if (!['tableGrid', 'fcTableGrid'].includes(child?.componentKey)) {
      handleTableDropToCell(index, event)
      return
    }
  }
  handleDropToChildren(index, event)
}

function handleDropToChildren(index, event) {
  handleDropToTarget({ parentId: props.component.id, index }, event)
}

function handleDropToTarget(target, event) {
  clearDropPreview()
  clearDesignerDropError()
  const sourceId = event.dataTransfer.getData(DRAG_COMPONENT_MIME)
  if (sourceId) {
    emit('select', sourceId)
    emit('update:schema', moveDesignerComponent(props.schema, sourceId, target))
    return
  }

  const fieldText = event.dataTransfer.getData(DRAG_FIELD_MIME)
  if (fieldText) {
    const field = parsePayload(fieldText)
    const component = createComponentFromField(field, target.index)
    emit('select', component.id)
    emit('update:schema', insertDesignerComponent(props.schema, target, component))
    return
  }

  const layoutText = event.dataTransfer.getData(DRAG_LAYOUT_MIME)
  if (layoutText) {
    const layout = parsePayload(layoutText)
    const component = createForgeLayoutComponent(layout.componentKey || 'card', props.schema)
    emit('select', component.id)
    emit('update:schema', insertDesignerComponent(props.schema, target, component))
    return
  }

  const templateText = event.dataTransfer.getData(DRAG_TEMPLATE_MIME)
  if (templateText) {
    const template = parsePayload(templateText)
    const component = createForgeFieldTemplateComponent(template, props.schema)
    emit('select', component.id)
    emit('update:schema', insertDesignerComponent(props.schema, target, component))
  }
}

function handleRowDropToColumn(index, event) {
  const child = resolveDraggedComponent(event)
  if (child?.componentKey === 'col') {
    handleDropToChildren(index, event)
    return
  }
  const columns = (props.component.children || []).filter(item => item?.componentKey === 'col')
  const targetColumn = columns[Math.max(0, Math.min(columns.length - 1, Number(index) || 0))]
  if (!targetColumn) {
    showInvalidDrop('请先添加栅格列')
    return
  }
  if (!canAcceptDesignerChild(targetColumn, child)) {
    showInvalidDrop('该格子不支持放入这个组件')
    return
  }
  handleDropToTarget({
    parentId: targetColumn.id,
    index: targetColumn.children?.length || 0,
  }, event)
}

function handleTableDropToCell(index, event) {
  const child = resolveDraggedComponent(event)
  if (['tableGrid', 'fcTableGrid'].includes(child?.componentKey)) {
    handleDropToChildren(index, event)
    return
  }
  const cells = (props.component.children || []).filter(item => ['tableGrid', 'fcTableGrid'].includes(item?.componentKey))
  const targetCell = cells[Math.max(0, Math.min(cells.length - 1, Number(index) || 0))]
  if (!targetCell) {
    showInvalidDrop('请先添加表格单元格')
    return
  }
  if (!canAcceptDesignerChild(targetCell, child)) {
    showInvalidDrop('该单元格不支持放入这个组件')
    return
  }
  handleDropToTarget({
    parentId: targetCell.id,
    index: targetCell.children?.length || 0,
  }, event)
}

function canDropIntoCurrentNode(event) {
  if (isField.value || isTitle.value)
    return false
  const child = resolveDraggedComponent(event)
  if (isGridRow.value)
    return child?.componentKey === 'col' || resolveContainerSlotTarget(event, child)?.parentId
  if (isTableLayout.value)
    return ['tableGrid', 'fcTableGrid'].includes(child?.componentKey) || Boolean(resolveContainerSlotTarget(event, child)?.parentId)
  return canAcceptDesignerChild(props.component, child)
}

function canPreviewDropIntoCurrentNode(event) {
  if (!hasForgeDragType(event) || isField.value || isTitle.value || isTableLayout.value)
    return false
  if (isGridRow.value) {
    const child = resolveDraggedComponent(event)
    return child?.componentKey !== 'col' && (props.component.children || []).some(item => item?.componentKey === 'col')
  }
  const parentKey = props.component.componentKey || ''
  if (['tabs', 'elTabs', 'collapse', 'elCollapse'].includes(parentKey))
    return false
  return true
}

function resolveDraggedComponent(event) {
  const sourceId = event.dataTransfer.getData(DRAG_COMPONENT_MIME)
  if (sourceId)
    return getDesignerComponent(props.schema, sourceId)

  const fieldText = event.dataTransfer.getData(DRAG_FIELD_MIME)
  if (fieldText)
    return createComponentFromField(parsePayload(fieldText), 0)

  const layoutText = event.dataTransfer.getData(DRAG_LAYOUT_MIME)
  if (layoutText) {
    const layout = parsePayload(layoutText)
    return createForgeLayoutComponent(layout.componentKey || 'card', props.schema)
  }

  const templateText = event.dataTransfer.getData(DRAG_TEMPLATE_MIME)
  if (templateText)
    return createForgeFieldTemplateComponent(parsePayload(templateText), props.schema)
  return null
}

function resolveContainerSlotTarget(event, draggedComponent = resolveDraggedComponent(event), containerComponent = props.component) {
  if (!draggedComponent)
    return null
  const containerKey = containerComponent?.componentKey || ''
  const childKey = draggedComponent.componentKey || ''
  if (['row', 'fcRow'].includes(containerKey) && childKey === 'col') {
    const index = resolveLayoutSlotIndex(event, containerComponent.children || [])
    return {
      parentId: containerComponent.id,
      index,
      dropKey: `${containerComponent.id}:inside`,
    }
  }
  if (['table', 'fcTable'].includes(containerKey) && ['tableGrid', 'fcTableGrid'].includes(childKey)) {
    const index = resolveLayoutSlotIndex(event, containerComponent.children || [])
    return {
      parentId: containerComponent.id,
      index,
      dropKey: `${containerComponent.id}:inside`,
    }
  }
  let slotKeys = []
  if (['row', 'fcRow'].includes(containerKey))
    slotKeys = ['col']
  else if (['table', 'fcTable'].includes(containerKey))
    slotKeys = ['tableGrid', 'fcTableGrid']
  if (!slotKeys.length)
    return null
  const slots = (containerComponent.children || []).filter(item => slotKeys.includes(item?.componentKey))
  const targetSlot = resolveLayoutSlotAtPoint(event, slots)
  if (!targetSlot || !canAcceptDesignerChild(targetSlot, draggedComponent))
    return null
  return {
    parentId: targetSlot.id,
    index: targetSlot.children?.length || 0,
    dropKey: `${targetSlot.id}:inside`,
  }
}

function resolveLayoutSlotAtPoint(event, slots = []) {
  if (!slots.length)
    return null
  const rects = slots
    .map((slot, index) => {
      const rect = findNodeWrapRect(slot.id)
      return rect ? { slot, index, rect } : null
    })
    .filter(Boolean)
  if (!rects.length)
    return slots[0]
  const matched = rects.find(({ rect }) =>
    event.clientX >= rect.left
    && event.clientX <= rect.right
    && event.clientY >= rect.top
    && event.clientY <= rect.bottom)
  if (matched)
    return matched.slot
  const sortedRects = rects
    .map((entry) => {
      const centerX = entry.rect.left + entry.rect.width / 2
      const centerY = entry.rect.top + entry.rect.height / 2
      return {
        ...entry,
        distance: Math.abs(event.clientX - centerX) + Math.abs(event.clientY - centerY),
      }
    })
    .sort((a, b) => a.distance - b.distance)
  const nearest = sortedRects[0]
  return nearest?.slot || slots[0]
}

function resolveLayoutSlotIndex(event, slots = []) {
  const targetSlot = resolveLayoutSlotAtPoint(event, slots)
  const index = slots.findIndex(item => item?.id === targetSlot?.id)
  return index === -1 ? slots.length : index + 1
}

function findNodeWrapRect(componentId = '') {
  if (!componentId)
    return null
  const wraps = Array.from(document.querySelectorAll('[data-forge-node-id]'))
  const wrap = wraps.find(item => item?.dataset?.forgeNodeId === componentId)
  return wrap?.getBoundingClientRect?.() || null
}

function hasForgeDragType(event) {
  const types = Array.from(event.dataTransfer?.types || [])
  return FORGE_DRAG_TYPES.some(type => types.includes(type))
}

function clearDropPreview() {
  clearDesignerDropKey()
  activeDropPosition.value = ''
}

function resolveDropPosition(ratio) {
  return ratio <= 0.5 ? 'before' : 'after'
}

function updatePointerDropPreview(event) {
  const hoveredElement = document.elementFromPoint(event.clientX, event.clientY)
  if (!hoveredElement) {
    clearPointerDropPreview()
    return
  }

  if (hoveredElement.closest?.('.root-drop-zone')) {
    activeDropPosition.value = 'before'
    activePointerDropTarget = { parentId: '', index: 0 }
    setDesignerDropKey('root:before')
    return
  }

  const targetWrap = hoveredElement.closest?.('[data-forge-node-id]')
  if (!targetWrap) {
    clearPointerDropPreview()
    return
  }

  const targetId = targetWrap.dataset.forgeNodeId || ''
  if (!targetId || targetId === props.component.id) {
    showInvalidDrop('不能拖入组件自身')
    clearPointerDropPreview()
    return
  }

  if (isDescendantNodeWrap(targetWrap)) {
    showInvalidDrop('不能拖入自己的子组件')
    clearPointerDropPreview()
    return
  }

  const targetNode = targetWrap.querySelector?.('.canvas-node')
  const rect = targetNode?.getBoundingClientRect?.()
  if (!rect) {
    clearPointerDropPreview()
    return
  }

  const targetComponent = getDesignerComponent(props.schema, targetId)
  const ratio = rect.height > 0 ? (event.clientY - rect.top) / rect.height : 0.5
  const slotTarget = resolveContainerSlotTarget(event, props.component, targetComponent)
  if (slotTarget) {
    clearDesignerDropError()
    activeDropPosition.value = 'inside'
    activePointerDropTarget = {
      parentId: slotTarget.parentId,
      index: slotTarget.index,
    }
    setDesignerDropKey(slotTarget.dropKey)
    return
  }
  if (canPointerDropInside(targetComponent, ratio)) {
    clearDesignerDropError()
    activeDropPosition.value = 'inside'
    activePointerDropTarget = { parentId: targetId, index: targetComponent?.children?.length || 0 }
    setDesignerDropKey(`${targetId}:inside`)
    return
  }

  const targetParentId = targetWrap.dataset.forgeParentId || ''
  const parentComponent = targetParentId ? getDesignerComponent(props.schema, targetParentId) : null
  const targetIndex = Number(targetWrap.dataset.forgeIndex || 0)
  const rowColumnTarget = resolvePointerRowColumnTarget(parentComponent, targetIndex)
  if (rowColumnTarget) {
    clearDesignerDropError()
    activeDropPosition.value = 'inside'
    activePointerDropTarget = rowColumnTarget
    setDesignerDropKey(`${rowColumnTarget.parentId}:inside`)
    return
  }
  if (!canAcceptDesignerChild(parentComponent, props.component)) {
    showInvalidDrop(resolveInvalidPointerDropMessage(parentComponent))
    clearPointerDropPreview()
    return
  }

  const position = resolveDropPosition(ratio)
  activeDropPosition.value = position
  activePointerDropTarget = {
    parentId: targetParentId,
    index: targetIndex + (position === 'after' ? 1 : 0),
  }
  clearDesignerDropError()
  setDesignerDropKey(`${targetId}:${position}`)
}

function clearPointerDropPreview() {
  activePointerDropTarget = null
  clearDropPreview()
}

function isDescendantNodeWrap(targetWrap) {
  let current = targetWrap.parentElement
  while (current) {
    if (current.dataset?.forgeNodeId === props.component.id)
      return true
    current = current.parentElement
  }
  return false
}

function canPointerDropInside(targetComponent, ratio) {
  if (!targetComponent || ratio <= 0.28 || ratio >= 0.72)
    return false
  const targetKey = targetComponent.componentKey || ''
  if (isFieldComponent(targetComponent) || ['title', 'fcTitle', 'divider', 'elDivider'].includes(targetKey))
    return false
  if (['row', 'fcRow', 'table', 'fcTable', 'tabs', 'elTabs', 'collapse', 'elCollapse'].includes(targetKey))
    return false
  return canAcceptDesignerChild(targetComponent, props.component)
}

function resolvePointerRowColumnTarget(parentComponent, targetIndex = 0) {
  if (!['row', 'fcRow'].includes(parentComponent?.componentKey))
    return null
  if (props.component.componentKey === 'col')
    return null
  const columns = (parentComponent.children || []).filter(item => item?.componentKey === 'col')
  const targetColumn = columns[Math.max(0, Math.min(columns.length - 1, Number(targetIndex) || 0))]
  if (!targetColumn || !canAcceptDesignerChild(targetColumn, props.component))
    return null
  return {
    parentId: targetColumn.id,
    index: targetColumn.children?.length || 0,
  }
}

function showInvalidDrop(message = '当前位置不能放置该组件') {
  setDesignerDropError(message)
}

function resolveInvalidDropMessage(event) {
  if (isField.value)
    return '字段组件不能作为容器'
  if (isTitle.value)
    return '标题组件不能作为容器'
  if (isGridRow.value)
    return '请拖到具体格子里'
  const child = resolveDraggedComponent(event)
  if (!canAcceptDesignerChild(props.component, child))
    return '该容器不支持放入这个组件'
  return '当前位置不能放置该组件'
}

function resolveInvalidPointerDropMessage(parentComponent) {
  if (!parentComponent)
    return '当前位置不能放置该组件'
  const key = parentComponent.componentKey || ''
  if (['row', 'fcRow'].includes(key))
    return '请拖到具体格子里'
  if (['table', 'fcTable'].includes(key))
    return '表格布局只能拖入表格单元格'
  return '该容器不支持放入这个组件'
}

function mountPointerDragImage(event) {
  removeDragImage()
  if (!nodeRef.value)
    return

  const rect = nodeRef.value.getBoundingClientRect()
  const clone = nodeRef.value.cloneNode(true)
  clone.classList.add('drag-follow-clone')
  Object.assign(clone.style, {
    position: 'fixed',
    top: '0',
    left: '0',
    zIndex: '2147483647',
    width: `${rect.width}px`,
    height: `${rect.height}px`,
    margin: '0',
    background: props.component.props?.__designerStyle?.backgroundColor || '#fff',
    pointerEvents: 'none',
    transition: 'none',
    willChange: 'transform',
  })
  document.body.appendChild(clone)
  activeDragImage = clone
  activeDragImageOffset = {
    x: clampDragOffset(event.clientX - rect.left, rect.width, 20),
    y: clampDragOffset(event.clientY - rect.top, rect.height, 18),
  }
  updateDragImagePosition(event)
}

function removeDragImage() {
  activeDragImage?.remove?.()
  activeDragImage = null
  activeDragImageOffset = { x: 0, y: 0 }
}

function updateDragImagePosition(event) {
  if (!activeDragImage)
    return
  if (!event.clientX && !event.clientY)
    return
  const x = Math.round(event.clientX - activeDragImageOffset.x)
  const y = Math.round(event.clientY - activeDragImageOffset.y)
  activeDragImage.style.transform = `translate3d(${x}px, ${y}px, 0)`
}

function clampDragOffset(value, size, minOffset) {
  if (!Number.isFinite(value))
    return minOffset
  if (size <= minOffset * 2)
    return Math.max(0, size / 2)
  return Math.min(Math.max(value, minOffset), size - minOffset)
}

function resolveRowColumns(component = {}) {
  const columns = Number(component.props?.columns || component.children?.length || rootColumns.value)
  return Math.max(1, Math.min(MAX_FORM_GRID_COLUMNS, Number.isFinite(columns) ? columns : rootColumns.value))
}

function resolveTableColumns(component = {}) {
  const columns = Number(component.props?.columns || component.children?.length || rootColumns.value)
  return Math.max(1, Math.min(MAX_FORM_GRID_COLUMNS, Number.isFinite(columns) ? columns : rootColumns.value))
}

function resolveGap(value, fallback) {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

function collectDesignerFields(components = []) {
  return (Array.isArray(components) ? components : []).flatMap((item) => {
    if (!item)
      return []
    if (isFieldComponent(item))
      return [item]
    return collectDesignerFields(item.children || [])
  })
}

function isCrudRoleEnabled(field = {}, role = 'table', index = 0, maxSearchFields = 3) {
  const roles = field.props?.__crudRoles || {}
  if (Object.prototype.hasOwnProperty.call(roles, role))
    return roles[role] !== false
  if (role === 'search')
    return index < maxSearchFields
  return true
}

function toCrudFormField(component = {}, area = 'edit') {
  const componentKey = component.componentKey || 'input'
  const field = component.fieldBinding?.fieldCode || component.id || componentKey
  const rawProps = resolveRuntimeOptionProps({ ...(component.props || {}) }, componentKey)
  const crudConfig = rawProps.__crudConfig || {}
  delete rawProps.__designerStyle
  delete rawProps.customStyleText
  delete rawProps.__crudRoles
  delete rawProps.__crudConfig
  const areaConfig = crudConfig[area] || {}
  return {
    field,
    label: areaConfig.label || component.label || field,
    type: normalizePreviewFieldType(componentKey),
    placeholder: areaConfig.placeholder || rawProps.placeholder || buildPreviewPlaceholder(componentKey, component.label || field),
    required: Boolean(component.validation?.required),
    clearable: rawProps.clearable !== false,
    disabled: false,
    readonly: area === 'edit' ? Boolean(crudConfig.edit?.readonly) : false,
    span: Math.max(1, Math.min(6, Number(areaConfig.span || component.layout?.span || 1))),
    dictType: rawProps.dictType,
    options: resolvePreviewOptions(rawProps, componentKey),
    showFeedback: false,
    props: {
      ...rawProps,
      disabled: false,
      readonly: false,
    },
  }
}

function buildFallbackCrudField(field, label, type) {
  return {
    id: `crud_${field}`,
    componentKey: type,
    label,
    fieldBinding: { fieldCode: field },
    props: {},
    layout: { span: 1 },
    validation: { required: false },
  }
}

function buildCrudApiConfig(apiBase = '/business/object') {
  return {
    list: `get@${apiBase}/page`,
    detail: `post@${apiBase}/getById`,
    add: `post@${apiBase}/add`,
    update: `post@${apiBase}/edit`,
    delete: `post@${apiBase}/remove/:id`,
  }
}

function duplicateNode() {
  emit('update:schema', duplicateDesignerComponent(props.schema, props.component.id))
}

function removeNode() {
  emit('update:schema', removeDesignerComponent(props.schema, props.component.id))
}

function toggleRequired() {
  if (!isField.value)
    return
  const required = !props.component.validation?.required
  emit('update:schema', updateDesignerComponent(props.schema, props.component.id, {
    validation: {
      required,
      requiredMessage: required ? props.component.validation?.requiredMessage || `${displayLabel.value}不能为空` : '',
    },
  }))
}

function handleNodeMenuSelect(key) {
  if (key === 'config') {
    emit('select', props.component.id)
    emit('configure', props.component.id)
    return
  }
  if (key === 'copy') {
    duplicateNode()
    return
  }
  if (key === 'required') {
    toggleRequired()
    return
  }
  if (key === 'delete') {
    removeNode()
    return
  }
  if (key.startsWith('bg-')) {
    updateNodeDesignerStyle(resolveBackgroundStyle(key))
    return
  }
  if (key.startsWith('border-'))
    updateNodeDesignerStyle(resolveBorderStyle(key))
}

function updateNodeDesignerStyle(stylePatch = {}) {
  const current = props.component.props?.__designerStyle || {}
  emit('update:schema', updateDesignerComponent(props.schema, props.component.id, {
    props: {
      __designerStyle: {
        ...current,
        ...stylePatch,
      },
    },
  }))
}

function resolveBackgroundStyle(key) {
  const map = {
    'bg-default': { backgroundColor: undefined },
    'bg-gray': { backgroundColor: '#f3f4f6' },
    'bg-blue': { backgroundColor: '#eff6ff' },
    'bg-green': { backgroundColor: '#ecfdf5' },
    'bg-yellow': { backgroundColor: '#fffbeb' },
  }
  return map[key] || {}
}

function resolveBorderStyle(key) {
  const map = {
    'border-default': { borderColor: undefined, borderStyle: undefined },
    'border-dashed': { borderColor: '#9ca3af', borderStyle: 'dashed' },
    'border-blue': { borderColor: '#2563eb', borderStyle: 'solid' },
    'border-none': { borderColor: 'transparent', borderStyle: 'none', hideInnerBorder: true },
  }
  return map[key] || {}
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value))
}

function parsePayload(value) {
  try {
    return JSON.parse(value || '{}')
  }
  catch {
    return {}
  }
}

function normalizePreviewFieldType(componentKey = '') {
  const typeMap = {
    integer: 'number',
    money: 'number',
    orgSelect: 'orgTreeSelect',
    departmentSelect: 'orgTreeSelect',
    departmentTreeSelect: 'orgTreeSelect',
    deptSelect: 'orgTreeSelect',
    deptTreeSelect: 'orgTreeSelect',
    userPicker: 'userSelect',
    upload: 'fileUpload',
  }
  return typeMap[componentKey] || componentKey || 'input'
}

function buildPreviewPlaceholder(componentKey = '', label = '') {
  if (['select', 'dictSelect', 'radio', 'radioButton', 'checkbox', 'date', 'datetime', 'daterange', 'datetimerange', 'month', 'year', 'time', 'timerange', 'userSelect', 'orgTreeSelect', 'regionTreeSelect', 'treeSelect', 'customSelect', 'color'].includes(componentKey))
    return `请选择${label}`
  return `请输入${label}`
}

function buildPreviewOptions(componentKey = '') {
  if (!['select', 'radio', 'radioButton', 'checkbox'].includes(componentKey))
    return undefined
  return [
    { label: '选项一', value: '1' },
    { label: '选项二', value: '2' },
  ]
}

function resolveRuntimeOptionProps(rawProps = {}, componentKey = '') {
  const nextProps = { ...(rawProps || {}) }
  if (shouldUseDictOptions(nextProps, componentKey))
    delete nextProps.options
  return nextProps
}

function resolvePreviewOptions(rawProps = {}, componentKey = '') {
  if (shouldUseDictOptions(rawProps, componentKey))
    return undefined
  return rawProps.options || buildPreviewOptions(componentKey)
}

function shouldUseDictOptions(rawProps = {}, componentKey = '') {
  return Boolean(rawProps.dictType) && ['select', 'dictSelect', 'radio', 'radioButton', 'checkbox', 'cascader'].includes(componentKey)
}
function buildGroupTitleProps(component) {
  return {
    ...(component?.props || {}),
    title: component?.props?.title || component?.label || '分组标题',
    label: component?.label || component?.props?.title || '分组标题',
  }
}

function isFormDividerComponent(component) {
  return ['AiFormSectionTitle', 'aiFormSectionTitle', 'formDivider', 'formSectionTitle'].includes(component?.componentKey)
}

function buildFormDividerProps(component) {
  return {
    ...(component?.props || {}),
    title: component?.props?.title || component?.label || '表单分隔线',
    label: component?.label || component?.props?.title || '表单分隔线',
  }
}
</script>

<style scoped>
.node-wrap {
  min-width: 0;
}

.drop-line {
  position: relative;
  height: 8px;
  grid-column: 1 / -1;
  margin: -4px 0;
  overflow: visible;
  border-radius: 6px;
  pointer-events: none;
  transition:
    background 160ms ease,
    border-color 160ms ease;
}

.drop-line.active {
  height: var(--forge-designer-drag-height, var(--drop-placeholder-height, 72px));
  margin: 4px 0;
  border: 1px dashed #9ca3af;
  background: #f3f4f6;
  border-radius: 6px;
  box-shadow: none;
}

.drop-line::before {
  display: none;
}

.drop-line.active::before {
  display: none;
}

.canvas-node {
  container-type: inline-size;
  position: relative;
  min-width: 0;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  padding: 32px 8px 8px;
  transition:
    border-color 180ms ease,
    background 180ms ease,
    box-shadow 180ms ease;
}

.canvas-node:hover,
.canvas-node:focus {
  border-color: #93c5fd;
  background: #f8fbff;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.08);
  outline: none;
}

.canvas-node.selected {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.14);
}

.canvas-node.dragging {
  opacity: 0.72;
  border-color: #93c5fd;
  background: #f8fbff;
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.08);
}

:global(.drag-follow-clone) {
  box-sizing: border-box;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 2147483647;
  pointer-events: none;
  border: 1px solid #2563eb;
  border-radius: 6px;
  background: #fff;
  opacity: 0.96;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.18);
  transform-origin: 0 0;
  will-change: transform;
  transition: none !important;
}

:global(.drag-follow-clone .node-overlay) {
  display: none;
}

:global(.forge-pointer-dragging),
:global(.forge-pointer-dragging *) {
  cursor: grabbing !important;
  user-select: none !important;
}

.canvas-node.node-row,
.canvas-node.node-fcRow {
  border-color: transparent;
  background: #fbfdff;
  padding: 24px 6px 6px;
}

.canvas-node.structural-slot {
  border-color: transparent;
  background: transparent;
  padding: 0;
  box-shadow: none;
}

.canvas-node.node-row > .layout-children,
.canvas-node.node-fcRow > .layout-children {
  gap: inherit;
  border: 0;
  background: transparent;
  padding: 0;
}

.canvas-node.node-table,
.canvas-node.node-fcTable {
  border-color: transparent;
  background: #fbfdff;
}

.canvas-node.node-tableGrid,
.canvas-node.node-fcTableGrid {
  min-height: 82px;
  border-color: transparent;
  background: transparent;
}

.canvas-node.node-crudBlock,
.canvas-node.node-AiCrudPage {
  border-color: #cbd5e1;
  background: #fff;
}

.canvas-node.structural-slot:hover,
.canvas-node.structural-slot:focus {
  border-color: transparent;
  background: transparent;
  box-shadow: none;
}

.canvas-node.structural-slot .layout-children {
  min-height: var(--forge-slot-min-height, 96px);
  align-content: start;
  border-color: transparent;
  background: #f8fafc;
  box-shadow: none;
}

.canvas-node.node-row > .layout-children,
.canvas-node.node-fcRow > .layout-children,
.canvas-node.node-table > .layout-children,
.canvas-node.node-fcTable > .layout-children {
  min-height: var(--forge-slot-min-height, auto);
}

.canvas-node.node-row:hover,
.canvas-node.node-fcRow:hover,
.canvas-node.node-table:hover,
.canvas-node.node-fcTable:hover,
.canvas-node.structural-slot:hover .layout-children,
.canvas-node.structural-slot:focus .layout-children {
  border-color: #dbe3ee;
}

.canvas-node.structural-slot .layout-children.active {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow:
    inset 0 0 0 1px rgba(37, 99, 235, 0.2),
    0 6px 16px rgba(37, 99, 235, 0.12);
}

.canvas-node.structural-slot.selected .layout-children {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow:
    inset 0 0 0 1px rgba(37, 99, 235, 0.24),
    0 10px 22px rgba(37, 99, 235, 0.1);
}

.canvas-node.layout.selected,
.canvas-node.node-row.selected,
.canvas-node.node-fcRow.selected,
.canvas-node.node-table.selected,
.canvas-node.node-fcTable.selected {
  border-color: #2563eb !important;
  background: #eff6ff;
  box-shadow:
    0 0 0 2px rgba(37, 99, 235, 0.18),
    0 10px 22px rgba(37, 99, 235, 0.1);
}

.canvas-node.structural-slot.selected {
  border-color: transparent !important;
  background: transparent;
  box-shadow: none;
}

.canvas-node.border-hidden:not(.selected) {
  border-color: transparent !important;
  box-shadow: none;
}

.canvas-node.border-hidden:not(.selected) :deep(.n-card) {
  border-color: transparent !important;
}

.node-resize-handles {
  position: absolute;
  inset: 0;
  z-index: 7;
  pointer-events: none;
}

.resize-anchor {
  position: absolute;
  display: block;
  width: 10px;
  height: 10px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #2563eb;
  box-shadow: 0 2px 7px rgba(37, 99, 235, 0.32);
  pointer-events: auto;
}

.anchor-n {
  top: -6px;
  left: 50%;
  cursor: ns-resize;
  transform: translateX(-50%);
}

.anchor-e {
  top: 50%;
  right: -6px;
  cursor: ew-resize;
  transform: translateY(-50%);
}

.anchor-s {
  bottom: -6px;
  left: 50%;
  cursor: ns-resize;
  transform: translateX(-50%);
}

.anchor-w {
  top: 50%;
  left: -6px;
  cursor: ew-resize;
  transform: translateY(-50%);
}

.anchor-ne {
  top: -6px;
  right: -6px;
  cursor: nesw-resize;
}

.anchor-nw {
  top: -6px;
  left: -6px;
  cursor: nwse-resize;
}

.anchor-se {
  right: -6px;
  bottom: -6px;
  cursor: nwse-resize;
}

.anchor-sw {
  bottom: -6px;
  left: -6px;
  cursor: nesw-resize;
}

.node-overlay {
  position: absolute;
  top: 6px;
  right: 6px;
  left: 6px;
  z-index: 5;
  height: 24px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 160ms ease;
}

.canvas-node:hover .node-overlay,
.canvas-node.selected .node-overlay {
  opacity: 1;
  pointer-events: auto;
}

.drag-handle,
.menu-trigger,
.icon-action,
.quick-switch {
  display: grid;
  place-items: center;
  height: 24px;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #64748b;
  font-size: 15px;
}

.quick-actions {
  position: absolute;
  top: 0;
  right: 30px;
  display: flex;
  gap: 2px;
}

.icon-action {
  width: 26px;
  padding: 0;
  border: 1px solid transparent;
  color: #475569;
  cursor: pointer;
  font-size: 14px;
}

.quick-switch {
  display: inline-flex;
  width: auto;
  gap: 5px;
  padding: 0 6px;
  border: 1px solid transparent;
  color: #475569;
  cursor: pointer;
  font-size: 12px;
  line-height: 22px;
}

.quick-switch .switch-track {
  position: relative;
  width: 22px;
  height: 12px;
  border-radius: 999px;
  background: #cbd5e1;
  transition: background 160ms ease;
}

.quick-switch .switch-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.2);
  transition: transform 160ms ease;
}

.quick-switch.active {
  color: #2563eb;
  font-weight: 600;
}

.quick-switch.active .switch-track {
  background: #2563eb;
}

.quick-switch.active .switch-thumb {
  transform: translateX(10px);
}

.icon-action.danger {
  color: #dc2626;
}

.quick-switch:hover,
.icon-action:hover {
  border-color: #dbeafe;
  background: #eff6ff;
  color: #2563eb;
}

.drag-handle,
.menu-trigger {
  width: 26px;
}

.menu-trigger {
  position: absolute;
  top: 0;
  right: 0;
  padding: 0;
  cursor: pointer;
}

.drag-handle {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  cursor: grab;
}

.drag-handle-svg {
  transform: rotate(90deg);
}

@container (max-width: 360px) {
  .drag-handle {
    left: 0;
    transform: none;
  }

  .quick-actions {
    right: 30px;
  }
}

@container (max-width: 280px) {
  .quick-switch > span:first-child {
    display: none;
  }

  .quick-switch {
    padding: 0 5px;
  }
}

.canvas-node:hover .drag-handle,
.canvas-node:hover .menu-trigger,
.canvas-node:hover .quick-switch,
.canvas-node:hover .icon-action,
.menu-trigger:hover,
.drag-handle:hover {
  background: #eff6ff;
  color: #2563eb;
}

.icon-action.danger:hover {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.canvas-node:active .drag-handle {
  cursor: grabbing;
}

.crud-preview {
  display: grid;
  gap: 10px;
}

.crud-live-preview {
  overflow: hidden;
  border: 1px solid #dbe3ee;
  border-radius: 6px;
  background: #fff;
  padding: 8px;
  pointer-events: none;
}

.crud-empty-drop {
  border: 1px dashed #bfdbfe;
  border-radius: 6px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  line-height: 18px;
  padding: 12px;
  text-align: center;
}

.crud-empty-drop.active {
  border-color: #2563eb;
  background: #dbeafe;
}

.crud-live-preview :deep(.ai-crud-page) {
  min-height: 0;
}

.crud-live-preview :deep(.ai-crud-search) {
  margin-bottom: 8px;
}

.crud-live-preview :deep(.ai-crud-table) {
  min-height: 0;
}

.crud-live-preview :deep(.n-data-table-empty) {
  min-height: 56px;
}

.crud-layout-children {
  background: #fbfdff;
}

.real-layout {
  overflow: hidden;
  background: #fff;
}

.real-layout-card :deep(.n-card-header) {
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 600;
}

.real-layout-card :deep(.n-card__content) {
  padding: 10px;
}

.real-layout-tabs :deep(.n-tabs-nav) {
  padding: 0 4px;
}

.real-layout-tabs :deep(.n-tab-pane) {
  padding: 8px 0 0;
}

.real-layout-collapse :deep(.n-collapse-item__header) {
  padding: 8px 0;
  font-size: 13px;
  font-weight: 600;
}

.real-layout-collapse :deep(.n-collapse-item__content-inner) {
  padding-top: 6px;
}

.real-layout-children {
  background: #fff;
}

.layout-children {
  display: grid;
  gap: 8px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: #f8fafc;
  padding: 10px;
  transition:
    border-color 180ms ease,
    background 180ms ease,
    box-shadow 180ms ease;
}

.layout-children.active {
  border-color: #60a5fa;
  background: #eff6ff;
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.12);
}

.grid-layout-children {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.table-layout-children {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  padding: 6px;
  background: #f1f5f9;
}

.table-layout-children > :deep(.node-wrap) {
  min-height: 78px;
}

.empty-child-zone {
  display: grid;
  place-items: center;
  min-height: 64px;
  border: 0;
  border-radius: 7px;
  background: #fff;
  color: #2563eb;
  font-size: 12px;
  pointer-events: none;
  box-shadow: inset 0 0 0 1px rgba(191, 219, 254, 0.42);
  transition:
    background 180ms ease,
    box-shadow 180ms ease;
}

.canvas-node :deep(.n-form-item) {
  margin-bottom: 0;
}

.canvas-node :deep(.n-form-item-label) {
  font-size: 13px;
}

.empty-child-zone.active {
  background: #dbeafe;
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.28);
}

@media (prefers-reduced-motion: reduce) {
  .drop-line,
  .canvas-node {
    transition: none;
  }
}
</style>
