<template>
  <div
    class="forge-form-designer"
    :class="{ 'left-collapsed': leftCollapsed, 'right-open': rightOpen, 'right-collapsed': !rightOpen, 'canvas-focused': canvasFocusMode, 'section-view': canvasView !== 'layout' }"
  >
    <aside v-if="canvasView === 'layout'" class="designer-left">
      <button
        v-if="leftCollapsed"
        type="button"
        class="side-rail-toggle-button"
        title="展开组件库"
        @click="leftCollapsed = false"
      >
        <n-icon><ChevronForwardOutline /></n-icon>
      </button>
      <ForgeFieldShelf
        v-if="!leftCollapsed"
        :fields="fields"
        :used-field-set="usedFieldSet"
        :relations="relations"
        @append-field="appendField"
      >
        <template #actions>
          <n-button
            class="field-shelf-collapse-button"
            circle
            size="small"
            secondary
            title="收起组件库"
            @click="leftCollapsed = true"
          >
            <template #icon>
              <n-icon><ChevronBackOutline /></n-icon>
            </template>
          </n-button>
        </template>
      </ForgeFieldShelf>
    </aside>

    <main class="designer-center">
      <div class="designer-toolbar">
        <div>
          <div class="flex items-center gap-4">
            <h3>{{ normalizedSchema.formName || objectName || '业务表单' }}</h3>
            <n-button
              quaternary
              circle
              size="tiny"
              class="designer-toolbar-rename-btn"
              title="编辑表单名称"
              @click="openRenameCurrentForm"
            >
              <template #icon>
                <span class="designer-toolbar-rename-icon" aria-hidden="true">✎</span>
              </template>
            </n-button>
          </div>
          <p>{{ canvasMetaText }}</p>
        </div>
        <NSpace size="small" align="center">
          <n-button class="designer-toolbar-text-button" size="small" secondary :disabled="!canUndo" @click="undoSchema">
            <template #icon>
              <n-icon><ArrowUndoOutline /></n-icon>
            </template>
            撤销
          </n-button>
          <n-button class="designer-toolbar-text-button" size="small" secondary :disabled="!canRedo" @click="redoSchema">
            <template #icon>
              <n-icon><ArrowRedoOutline /></n-icon>
            </template>
            重做
          </n-button>
          <n-button class="designer-toolbar-text-button designer-toolbar-danger-button" size="small" secondary :disabled="!canClearCanvas" @click="openClearCanvasDialog">
            <template #icon>
              <n-icon><TrashOutline /></n-icon>
            </template>
            清空
          </n-button>
          <n-dropdown trigger="click" :options="designerMoreOptions" @select="handleDesignerMoreSelect">
            <n-button class="designer-toolbar-more-button" circle size="small" type="primary" title="更多操作">
              <template #icon>
                <n-icon><EllipsisHorizontalOutline /></n-icon>
              </template>
            </n-button>
          </n-dropdown>
          <n-button
            class="designer-toolbar-icon-button"
            circle
            size="small"
            type="primary"
            :title="rightOpen ? '收起属性栏' : '打开属性栏'"
            @click="rightOpen ? (rightOpen = false) : openPropertyPanel(selectedId)"
          >
            <template #icon>
              <n-icon>
                <ChevronForwardOutline v-if="rightOpen" />
                <ChevronBackOutline v-else />
              </n-icon>
            </template>
          </n-button>
        </NSpace>
      </div>

      <n-dropdown
        trigger="manual"
        placement="bottom-start"
        :show="formTabsMenuVisible"
        :x="formTabsMenuX"
        :y="formTabsMenuY"
        :options="formTabsMenuOptions"
        @select="handleFormTabsMenuSelect"
        @clickoutside="formTabsMenuVisible = false"
      />
      <div class="designer-form-tabs-bar page-design-switcher" @contextmenu.prevent="openFormTabsMenu">
        <div class="page-switch-title">
          <span class="page-switch-icon">{{ canvasViewIcon }}</span>
          <div>
            <strong>{{ canvasViewTitle }}</strong>
            <small>{{ canvasViewDescription }}</small>
          </div>
        </div>
        <div class="page-view-controls">
          <n-radio-group v-model:value="canvasView" size="small" aria-label="表单页画布视图">
            <n-radio-button value="layout">
              表单布局
            </n-radio-button>
            <n-radio-button v-if="enableSectionsView" value="sections">
              页面分区
            </n-radio-button>
            <n-radio-button v-if="hasDetailSettings" value="detail">
              详情设置
            </n-radio-button>
          </n-radio-group>
          <div v-if="formAssets.length && canvasView === 'layout'" class="designer-form-tabs" aria-label="表单切换">
            <button type="button" class="designer-form-tab active" @click="selectedId = ''">
              <em>1</em>
              <span>{{ normalizedSchema.formName || '主表单' }}</span>
            </button>
            <button
              v-for="(asset, assetIndex) in formAssets"
              :key="asset.formKey"
              type="button"
              class="designer-form-tab"
              @click="switchFormAsset(asset.formKey)"
            >
              <em>{{ assetIndex + 2 }}</em>
              <span>{{ asset.formName || `表单 ${assetIndex + 2}` }}</span>
            </button>
          </div>
        </div>
        <div class="page-tools">
          <div v-if="canvasView === 'layout'" class="page-switch-actions">
            <n-button class="designer-toolbar-icon-button neutral" circle size="small" secondary :disabled="!canUndo" title="撤销" @click="undoSchema">
              <template #icon>
                <n-icon><ArrowUndoOutline /></n-icon>
              </template>
            </n-button>
            <n-button class="designer-toolbar-icon-button neutral" circle size="small" secondary :disabled="!canRedo" title="重做" @click="redoSchema">
              <template #icon>
                <n-icon><ArrowRedoOutline /></n-icon>
              </template>
            </n-button>
            <n-button class="designer-toolbar-icon-button neutral danger" circle size="small" secondary :disabled="!canClearCanvas" title="清空画布" @click="openClearCanvasDialog">
              <template #icon>
                <n-icon><TrashOutline /></n-icon>
              </template>
            </n-button>
            <n-dropdown trigger="click" placement="bottom-end" :options="formTabsMenuOptions" @select="handleFormTabsMenuSelect">
              <n-button class="designer-toolbar-icon-button neutral" circle size="small" secondary title="表单页面操作">
                <template #icon>
                  <n-icon><EllipsisHorizontalOutline /></n-icon>
                </template>
              </n-button>
            </n-dropdown>
            <n-button
              class="designer-toolbar-icon-button neutral"
              circle
              size="small"
              secondary
              :title="rightOpen ? '收起属性栏' : '打开属性栏'"
              @click="rightOpen ? (rightOpen = false) : openPropertyPanel(selectedId)"
            >
              <template #icon>
                <n-icon>
                  <ChevronForwardOutline v-if="rightOpen" />
                  <ChevronBackOutline v-else />
                </n-icon>
              </template>
            </n-button>
          </div>
        </div>
      </div>

      <ForgeFormCanvas
        v-if="canvasView === 'layout'"
        :schema="normalizedSchema"
        :fields="fields"
        :selected-id="selectedId"
        @update:schema="updateSchema"
        @update:selected-id="handleCanvasSelectedIdChange"
        @configure="openPropertyPanel"
        @configure-sub-table="relationKey => emit('editSubTableContainer', relationKey)"
        @open-source="openSourcePanel"
        @toggle-focus="toggleCanvasFocus"
      />
      <PageSectionEditor
        v-else-if="canvasView === 'sections'"
        class="inline-page-section-editor"
        :model-value="pageSectionProtocol"
        :fields="fields"
        :relations="relations"
        :actions="actions"
        @update:model-value="updatePageSectionProtocol"
        @configure-bottom-action="emit('configureBottomAction', $event)"
        @edit-child-table-section="emit('editChildTableSection', $event)"
        @remove-child-table-section="emit('removeChildTableSection', $event)"
      />
      <div v-else class="inline-detail-settings">
        <slot name="detail-settings" />
      </div>
    </main>

    <aside v-if="canvasView === 'layout'" class="designer-right">
      <ForgePropertyPanel
        v-if="rightOpen"
        :schema="normalizedSchema"
        :fields="fields"
        :relations="relations"
        :object-code="objectCode"
        :selected-id="selectedId"
        :initial-form-tab="initialPropertyTab"
        @update:schema="updateSchema"
        @update:selected-id="selectedId = $event"
        @field-asset-updated="emit('fieldAssetUpdated', $event)"
        @close="rightOpen = false"
      />
    </aside>
  </div>
  <n-modal
    v-model:show="bottomBarDialogVisible"
    preset="card"
    title="底部操作栏"
    :bordered="false"
    class="designer-bottom-bar-modal"
    :style="{ width: 'min(960px, calc(100vw - 40px))' }"
  >
    <BottomBarEditor
      :model-value="normalizedSchema.bottomBar || {}"
      :fields="fields"
      @update:model-value="updateBottomBarFromDialog"
      @configure-bottom-action="payload => emit('configureBottomAction', payload)"
    />
  </n-modal>
  <n-modal
    v-model:show="previewDialogVisible"
    preset="card"
    title="预览当前表单"
    class="designer-preview-modal"
    :bordered="false"
    :style="{
      width: 'min(1120px, calc(100vw - 40px))',
      maxWidth: 'calc(100vw - 40px)',
      height: 'min(860px, calc(100vh - 40px))',
    }"
  >
    <div class="designer-preview-toolbar">
      <div>
        <strong>{{ previewModeTitle }}</strong>
        <span>{{ previewModeDescription }}</span>
      </div>
      <n-radio-group
        v-model:value="previewMode"
        size="small"
      >
        <n-radio-button
          v-for="option in previewModeOptions"
          :key="option.value"
          :value="option.value"
        >
          {{ option.label }}
        </n-radio-button>
      </n-radio-group>
    </div>
    <div class="designer-preview-runtime">
      <AiForm
        class="designer-preview-runtime-form"
        :schema="previewSchema"
        :value="previewValue"
        :label-placement="previewLayout.labelPlacement || 'left'"
        :label-width="previewLayout.labelWidth ?? 'auto'"
        :label-align="previewLayout.labelAlign || 'right'"
        :size="previewLayout.size || 'medium'"
        :grid-cols="previewGridCols"
        :x-gap="previewLayout.xGap || previewLayout.columnGap || 12"
        :y-gap="previewLayout.yGap || previewLayout.rowGap || 0"
        :show-actions="false"
        :show-feedback="previewLayout.showFeedback !== false"
        :context="previewRuntimeContext"
        :form-assets="formAssets"
        :keep-empty-layout-nodes="true"
      />
    </div>
  </n-modal>

  <n-modal
    v-model:show="clearDialogVisible"
    preset="card"
    title="清空画布"
    class="designer-clear-modal"
    :bordered="false"
    :mask-closable="false"
    style="width: 420px"
  >
    <div class="designer-clear-content">
      <div class="designer-clear-warning">
        <n-icon><WarningOutline /></n-icon>
        <div>
          <strong>清空后会删除画布上的组件配置</strong>
          <span>字段资产不会删除，操作可通过撤销恢复。</span>
        </div>
      </div>
      <n-radio-group v-model:value="clearScope">
        <NSpace vertical size="small">
          <n-radio value="current">
            仅清空当前画布：{{ normalizedSchema.formName || '当前表单' }}
          </n-radio>
          <n-radio value="all">
            清空全部画布：主表单和 {{ formAssets.length }} 个子表单
          </n-radio>
        </NSpace>
      </n-radio-group>
    </div>
    <template #footer>
      <div class="designer-clear-footer">
        <n-button size="small" @click="clearDialogVisible = false">
          取消
        </n-button>
        <n-button size="small" type="error" @click="confirmClearCanvas">
          确认清空
        </n-button>
      </div>
    </template>
  </n-modal>

  <n-modal
    v-model:show="renameDialogVisible"
    preset="card"
    title="编辑表单名称"
    class="designer-rename-modal"
    :bordered="false"
    :mask-closable="false"
    style="width: 400px"
  >
    <n-input
      v-model:value="renameFormName"
      placeholder="请输入表单名称"
      clearable
      @keyup.enter="confirmRenameCurrentForm"
    />
    <template #footer>
      <div class="designer-rename-modal-footer">
        <n-button size="small" @click="renameDialogVisible = false">
          取消
        </n-button>
        <n-button size="small" type="primary" @click="confirmRenameCurrentForm">
          保存
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup>
import { ArrowRedoOutline, ArrowUndoOutline, ChevronBackOutline, ChevronForwardOutline, EllipsisHorizontalOutline, TrashOutline, WarningOutline } from '@vicons/ionicons5'
import { NSpace } from 'naive-ui'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useSlots } from 'vue'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import AiForm from '@/components/ai-form/AiForm.vue'
import AiFormGroupTitle from '@/components/ai-form/AiFormGroupTitle.vue'
import AiFormSectionTitle from '@/components/ai-form/AiFormSectionTitle.vue'
import { normalizeRecordSelectorConfig as normalizeRuntimeRecordSelectorConfig } from '@/components/ai-form/record-selector-utils'
import { isPageWidgetComponentKey } from '@/components/lowcode-builder/shared/page-widget-schema'
import { buildLegacyLinkageSchema } from '../form-first/field-linkage-config'
import { repairFormDesignerFieldRefs } from '../form-first/fieldReferenceUtils'
import { extractForgeSchemaFieldRefs } from '../form-first/forgeToFormCreate'
import {
  applyGridColumnsToFormDesignerSchema,
  createComponentFromField,
  createDefaultFormDesignerSchema,
  normalizeFormDesignerSchema,
  normalizeFormDesignerSchemaForSave,
} from '../form-first/formDesignerSchema'
import BottomBarEditor from './BottomBarEditor.vue'
import ForgeFieldShelf from './ForgeFieldShelf.vue'
import ForgeFormCanvas from './ForgeFormCanvas.vue'
import ForgePropertyPanel from './ForgePropertyPanel.vue'
import { derivePageSectionsFromLayout } from './pageSectionDerivation'
import PageSectionEditor from './PageSectionEditor.vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: null,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  objectCode: {
    type: String,
    default: '',
  },
  objectName: {
    type: String,
    default: '',
  },
  relations: {
    type: Array,
    default: () => [],
  },
  actions: {
    type: Array,
    default: () => [],
  },
  linkageSchema: {
    type: Object,
    default: null,
  },
  extraMoreOptions: {
    type: Array,
    default: () => [],
  },
  initialPropertyTab: {
    type: String,
    default: 'basic',
  },
  initialCanvasView: {
    type: String,
    default: 'layout',
    validator: value => ['layout', 'sections', 'detail'].includes(value),
  },
  // 是否提供独立「页面分区」视图：分区由布局容器承载时宿主应关闭，分区随画布派生维护。
  enableSectionsView: {
    type: Boolean,
    default: true,
  },
  // 开启后每次 schema 变更都会从布局组件树派生 pageSections 写回（card/collapse=内容分区、subTable=子表分区）。
  deriveSectionsFromLayout: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'update:modelValue',
  'update:linkageSchema',
  'dirtyChange',
  'moreSelect',
  'fieldAssetUpdated',
  'configureBottomAction',
  'editSubTableContainer',
  'editChildTableSection',
  'removeChildTableSection',
])
const slots = useSlots()

const selectedId = ref('')
const leftCollapsed = ref(false)
const rightOpen = ref(true)
const canvasFocusMode = ref(false)
const formTabsMenuVisible = ref(false)
const formTabsMenuX = ref(0)
const formTabsMenuY = ref(0)
const undoStack = ref([])
const redoStack = ref([])
const HISTORY_LIMIT = 50
const previewMode = ref('create')
const clearDialogVisible = ref(false)
const clearScope = ref('current')
const bottomBarDialogVisible = ref(false)
const canvasView = ref(!props.enableSectionsView && props.initialCanvasView === 'sections' ? 'layout' : props.initialCanvasView)
const previewModeOptions = [
  { label: '新增', value: 'create' },
  { label: '编辑', value: 'edit' },
  { label: '详情', value: 'detail' },
]
const previewRuntimeContext = {
  mode: 'designer-preview',
  designerPreview: true,
  source: 'form-designer',
}
const baseDesignerMoreOptions = [
  { label: '按字段生成', key: 'resetFromFields' },
  { label: '清理失效字段', key: 'repairRefs' },
  { label: '底部操作栏', key: 'configureBottomBar' },
  { label: '打印表单', key: 'printDesign' },
]

const normalizedSchema = computed(() => normalizeFormDesignerSchema(props.modelValue || createDefaultFormDesignerSchema({
  objectCode: props.objectCode,
  objectName: props.objectName,
  fields: props.fields,
})))
const previewLayout = computed(() => normalizedSchema.value.layout || {})
// 预览列数与设计器画布（ForgeFormCanvasNode.rootColumns）完全同源：默认 2 列、clamp 1-24，
// 避免「设计默认 2 列 / 预览兕底 1 列」导致设计/预览不一致
const previewGridCols = computed(() => {
  const raw = Number(previewLayout.value.gridCols || previewLayout.value.gridColumns) || 2
  return Math.max(1, Math.min(24, raw))
})
const previewSchema = computed(() => buildRuntimeFormSchema(normalizedSchema.value, previewMode.value))
const previewValue = computed(() => buildRuntimeFormValue(normalizedSchema.value, previewMode.value))
const usedFieldSet = computed(() => new Set(extractForgeSchemaFieldRefs(normalizedSchema.value || {})))
const formAssets = computed(() => Array.isArray(normalizedSchema.value.settings?.formAssets) ? normalizedSchema.value.settings.formAssets : [])
const designerMoreOptions = computed(() => [
  ...baseDesignerMoreOptions,
  ...(props.extraMoreOptions || []),
])
const canUndo = computed(() => undoStack.value.length > 0)
const canRedo = computed(() => redoStack.value.length > 0)
const canClearCanvas = computed(() => {
  if (containsLockedField(normalizedSchema.value.components)
    || formAssets.value.some(asset => containsLockedField(asset?.schema?.components))) {
    return false
  }
  if (normalizedSchema.value.components?.length)
    return true
  return formAssets.value.some(asset => asset?.schema?.components?.length)
})
const componentCount = computed(() => countComponents(normalizedSchema.value.components))
const hasDetailSettings = computed(() => Boolean(slots['detail-settings']))
const pageSectionProtocol = computed(() => ({
  pageSections: normalizedSchema.value.pageSections || [],
  bottomBar: normalizedSchema.value.bottomBar || {},
}))
const canvasViewTitle = computed(() => ({
  layout: '表单页设计',
  sections: '页面分区',
  detail: '详情设置',
}[canvasView.value] || '表单页设计'))
const canvasViewIcon = computed(() => ({ layout: 'F', sections: 'S', detail: 'D' }[canvasView.value] || 'F'))
const canvasViewDescription = computed(() => {
  if (canvasView.value === 'sections')
    return `${pageSectionProtocol.value.pageSections.length} 个分区 · 与表单草稿同步保存`
  if (canvasView.value === 'detail')
    return '关系页签、日志和数量区块'
  return `${normalizedSchema.value.formName || '当前表单'} · ${componentCount.value} 个组件`
})
const previewModeTitle = computed(() => previewModeOptions.find(item => item.value === previewMode.value)?.label || '新增')
const previewModeDescription = computed(() => {
  const modeMap = {
    create: '空表单状态，按默认值初始化。',
    edit: '模拟详情接口返回后进入编辑。',
    detail: '模拟详情接口返回，并按只读态展示。',
  }
  return modeMap[previewMode.value] || ''
})
const formTabsMenuOptions = [
  { label: '＋ 新建空白表单', key: 'create' },
  { label: '⧉ 复制当前表单', key: 'duplicate' },
]
const canvasMetaText = computed(() => {
  const fieldCount = usedFieldSet.value.size
  return `${fieldCount} 个业务字段 · ${componentCount.value} 个画布节点 · ${normalizedSchema.value.layout?.gridColumns || 2} 列`
})

function updateSchema(schema, options = {}) {
  let nextSchema = normalizeFormDesignerSchema(schema || {})
  // 分区由布局承载时，pageSections 不再独立编辑，随画布结构派生写回。
  if (props.deriveSectionsFromLayout)
    nextSchema = { ...nextSchema, pageSections: derivePageSectionsFromLayout(nextSchema.components, nextSchema.pageSections || []) }
  const currentSchema = normalizeFormDesignerSchema(normalizedSchema.value || {})
  if (isSameDesignerSchema(nextSchema, currentSchema))
    return
  if (options.recordHistory !== false) {
    pushHistorySnapshot(currentSchema)
    redoStack.value = []
  }
  emit('update:modelValue', nextSchema)
  if (Array.isArray(nextSchema.settings?.governance?.fieldLinkages)) {
    emit('update:linkageSchema', buildLegacyLinkageSchema(nextSchema, props.linkageSchema || {}))
  }
  emit('dirtyChange', true)
}

function pushHistorySnapshot(schema) {
  undoStack.value = [...undoStack.value, cloneDesignerSchema(schema)].slice(-HISTORY_LIMIT)
}

function undoSchema() {
  if (!canUndo.value)
    return
  const currentSchema = normalizeFormDesignerSchema(normalizedSchema.value || {})
  const previousSchema = undoStack.value[undoStack.value.length - 1]
  undoStack.value = undoStack.value.slice(0, -1)
  redoStack.value = [cloneDesignerSchema(currentSchema), ...redoStack.value].slice(0, HISTORY_LIMIT)
  selectedId.value = ''
  updateSchema(previousSchema, { recordHistory: false })
}

function redoSchema() {
  if (!canRedo.value)
    return
  const currentSchema = normalizeFormDesignerSchema(normalizedSchema.value || {})
  const nextSchema = redoStack.value[0]
  redoStack.value = redoStack.value.slice(1)
  pushHistorySnapshot(currentSchema)
  selectedId.value = ''
  updateSchema(nextSchema, { recordHistory: false })
}

function handleDesignerShortcut(event) {
  const isUndoKey = (event.metaKey || event.ctrlKey) && !event.shiftKey && event.key?.toLowerCase?.() === 'z'
  const isRedoKey = (event.metaKey || event.ctrlKey) && ((event.shiftKey && event.key?.toLowerCase?.() === 'z') || event.key?.toLowerCase?.() === 'y')
  if (!isUndoKey && !isRedoKey)
    return
  const target = event.target
  if (target?.closest?.('input, textarea, [contenteditable="true"]'))
    return
  event.preventDefault()
  if (isRedoKey)
    redoSchema()
  else
    undoSchema()
}

function cloneDesignerSchema(value) {
  return JSON.parse(JSON.stringify(value || {}))
}

function isSameDesignerSchema(left, right) {
  return JSON.stringify(left || {}) === JSON.stringify(right || {})
}

function appendField(field = {}) {
  const fieldCode = field.fieldCode || field.field
  if (!fieldCode || usedFieldSet.value.has(fieldCode))
    return
  const schema = normalizeFormDesignerSchema(normalizedSchema.value)
  const component = createComponentFromField(field, schema.components.length)
  schema.components.push(component)
  selectedId.value = component.id
  rightOpen.value = true
  updateSchema(applyGridColumnsToFormDesignerSchema(schema, schema.layout?.gridColumns || 2))
}

function resetFromFields() {
  const nextSchema = createDefaultFormDesignerSchema({
    objectCode: props.objectCode,
    objectName: props.objectName,
    fields: props.fields,
    gridColumns: normalizedSchema.value.layout?.gridColumns || 2,
  })
  selectedId.value = ''
  updateSchema({
    ...nextSchema,
    formKey: normalizedSchema.value.formKey,
    formName: normalizedSchema.value.formName,
    settings: normalizedSchema.value.settings,
    layout: {
      ...(nextSchema.layout || {}),
      ...(normalizedSchema.value.layout || {}),
    },
  })
}

function openClearCanvasDialog() {
  clearScope.value = 'current'
  clearDialogVisible.value = true
}

function updatePageSectionProtocol(protocol) {
  if (!protocol || typeof protocol !== 'object')
    return
  updateSchema({
    ...normalizedSchema.value,
    pageSections: protocol.pageSections,
    bottomBar: protocol.bottomBar,
  })
}

// 底部操作栏弹窗即时写回：与分区编辑一致走 updateSchema，保留撤销历史与脏标记。
function updateBottomBarFromDialog(bottomBar) {
  updateSchema({
    ...normalizedSchema.value,
    bottomBar,
  })
}

function handleDesignerMoreSelect(key = '') {
  if (key === 'configureBottomBar') {
    bottomBarDialogVisible.value = true
    return
  }
  if (key === 'resetFromFields') {
    resetFromFields()
    return
  }
  if (key === 'printDesign') {
    openPrintPreview()
    return
  }
  if (key === 'repairRefs')
    repairRefs()
  else
    emit('moreSelect', key)
}

/** 打开预览并触发浏览器打印，像 Word 一样输出当前表单设计稿 */
function openPrintPreview() {
  previewDialogVisible.value = true
  nextTick(() => {
    setTimeout(() => window.print(), 400)
  })
}

function confirmClearCanvas() {
  const schema = normalizeFormDesignerSchema(normalizedSchema.value)
  selectedId.value = ''
  clearDialogVisible.value = false

  if (clearScope.value === 'all') {
    updateSchema({
      ...schema,
      components: [],
      settings: {
        ...(schema.settings || {}),
        formAssets: formAssets.value.map(asset => ({
          ...asset,
          schema: asset.schema
            ? {
                ...asset.schema,
                components: [],
              }
            : asset.schema,
        })),
      },
    })
    return
  }

  updateSchema({
    ...schema,
    components: [],
  })
}

function repairRefs() {
  const schema = repairFormDesignerFieldRefs(normalizedSchema.value, props.fields, 'mark')
  updateSchema(schema)
}

function switchFormAsset(formKey = '') {
  const asset = formAssets.value.find(item => item.formKey === formKey)
  if (!asset?.schema)
    return
  const currentAsset = {
    formKey: normalizedSchema.value.formKey,
    formName: normalizedSchema.value.formName,
    schema: {
      ...normalizeFormDesignerSchema(normalizedSchema.value),
      settings: {
        ...(normalizedSchema.value.settings || {}),
        formAssets: [],
      },
    },
  }
  const nextAssets = formAssets.value
    .filter(item => item.formKey !== formKey && item.formKey !== currentAsset.formKey)
    .concat(currentAsset)
  selectedId.value = ''
  updateSchema(normalizeFormDesignerSchema({
    ...asset.schema,
    settings: {
      ...(asset.schema.settings || {}),
      formAssets: nextAssets,
    },
  }))
}

function openFormTabsMenu(event) {
  formTabsMenuX.value = event.clientX
  formTabsMenuY.value = event.clientY
  formTabsMenuVisible.value = true
}

function handleFormTabsMenuSelect(key) {
  formTabsMenuVisible.value = false
  if (key === 'create') {
    createBlankFormAsset()
    return
  }
  if (key === 'duplicate')
    duplicateCurrentFormAsset()
}

function createBlankFormAsset() {
  const nextAssetIndex = formAssets.value.length + 2
  const nextAssetKey = `${normalizedSchema.value.formKey || 'form'}_form_${Date.now()}`
  const assetSchema = normalizeFormDesignerSchema({
    ...normalizedSchema.value,
    formKey: nextAssetKey,
    formName: `表单 ${nextAssetIndex}`,
    components: [],
    settings: {
      ...(normalizedSchema.value.settings || {}),
      formAssets: [],
    },
  })
  updateSchema({
    ...normalizedSchema.value,
    settings: {
      ...(normalizedSchema.value.settings || {}),
      formAssets: [
        {
          formKey: nextAssetKey,
          formName: assetSchema.formName,
          schema: assetSchema,
        },
        ...formAssets.value,
      ],
    },
  })
}

function duplicateCurrentFormAsset() {
  const nextAssetIndex = formAssets.value.length + 2
  const nextAssetKey = `${normalizedSchema.value.formKey || 'form'}_copy_${Date.now()}`
  const assetSchema = normalizeFormDesignerSchema({
    ...normalizedSchema.value,
    formKey: nextAssetKey,
    formName: `${normalizedSchema.value.formName || '表单'} 副本 ${nextAssetIndex}`,
    settings: {
      ...(normalizedSchema.value.settings || {}),
      formAssets: [],
    },
  })
  updateSchema({
    ...normalizedSchema.value,
    settings: {
      ...(normalizedSchema.value.settings || {}),
      formAssets: [
        {
          formKey: nextAssetKey,
          formName: assetSchema.formName,
          schema: assetSchema,
        },
        ...formAssets.value,
      ],
    },
  })
}

function openPropertyPanel(componentId = '') {
  if (componentId)
    selectedId.value = componentId
  rightOpen.value = true
}

function openSourcePanel() {
  rightOpen.value = true
  nextTick(() => {
    window.dispatchEvent(new CustomEvent('forge-form-designer:open-source-panel'))
  })
}

function toggleCanvasFocus() {
  canvasFocusMode.value = !canvasFocusMode.value
  leftCollapsed.value = canvasFocusMode.value
  rightOpen.value = false
}

function handleCanvasSelectedIdChange(componentId = '') {
  selectedId.value = componentId
  if (componentId)
    rightOpen.value = true
}

function handleCanvasDragStart() {
  rightOpen.value = false
}

function flushDesigner() {
  const currentSchema = normalizeFormDesignerSchema(normalizedSchema.value)
  const sourceSchema = props.modelValue && typeof props.modelValue === 'object' ? props.modelValue : {}
  return normalizeFormDesignerSchemaForSave({
    ...sourceSchema,
    ...currentSchema,
    defaultFormKey: sourceSchema.defaultFormKey || sourceSchema.settings?.defaultFormKey || currentSchema.settings?.defaultFormKey || currentSchema.formKey,
    settings: {
      ...(sourceSchema.settings || {}),
      ...(currentSchema.settings || {}),
      formAssets: currentSchema.settings?.formAssets || sourceSchema.settings?.formAssets || [],
    },
  })
}

function openPageSections() {
  // 分区由布局承载的宿主没有 sections 视图，向导确认等入口改跳画布查看承载容器。
  canvasView.value = props.enableSectionsView ? 'sections' : 'layout'
}

function countComponents(components = []) {
  return (Array.isArray(components) ? components : []).reduce((total, component) => {
    return total + 1 + countComponents(component.children || [])
  }, 0)
}

function containsLockedField(value = []) {
  if (Array.isArray(value))
    return value.some(item => containsLockedField(item))
  if (!value || typeof value !== 'object')
    return false
  if (value.fieldBinding?.locked === true)
    return true
  return Object.values(value).some(item => containsLockedField(item))
}

defineExpose({
  flushDesigner,
  openPageSections,
  resetFromFields,
  repairRefs,
  appendField,
})
const renameDialogVisible = ref(false)
const renameFormName = ref('')
const previewDialogVisible = ref(false)

// Runtime component aliases removed — preview now delegates to AiForm + AiFormLayoutNodes

const componentTypeAlias = {
  text: 'input',
  input: 'input',
  textarea: 'textarea',
  number: 'number',
  inputNumber: 'number',
  select: 'select',
  radio: 'radio',
  radioGroup: 'radio',
  checkbox: 'checkbox',
  checkboxGroup: 'checkbox',
  switch: 'switch',
  rate: 'rate',
  slider: 'slider',
  date: 'date',
  datePicker: 'date',
  datetime: 'datetime',
  time: 'time',
  timePicker: 'time',
  upload: 'upload',
  cascader: 'cascader',
  treeSelect: 'treeSelect',
  colorPicker: 'colorPicker',
  button: 'button',
  row: 'row',
  fcRow: 'row',
  col: 'col',
  table: 'table',
  tableGrid: 'tableGrid',
  divider: 'AiFormSectionTitle',
  elDivider: 'AiFormSectionTitle',
  title: 'groupTitle',
  fcTitle: 'groupTitle',
  sectionTitle: 'groupTitle',
  groupTitle: 'groupTitle',
  formSectionTitle: 'AiFormSectionTitle',
  FormSectionTitle: 'AiFormSectionTitle',
  groupHeader: 'groupTitle',
  GroupHeader: 'groupTitle',
  titleBlock: 'groupTitle',
  section: 'groupTitle',
  AiFormSectionTitle: 'AiFormSectionTitle',
  aiFormSectionTitle: 'AiFormSectionTitle',
  card: 'card',
  tabs: 'tabs',
  collapse: 'collapse',
  crud: 'crud',
  crudBlock: 'crud',
  AiCrudPage: 'crud',
  aiCrudPage: 'crud',
}

function getRuntimeFieldCode(component) {
  return (
    component?.fieldBinding?.fieldCode
    || component?.fieldBinding?.columnName
    || component?.field
    || component?.prop
    || component?.key
    || component?.id
  )
}

function normalizeRuntimeComponent(component, mode = 'create') {
  if (!component)
    return component

  const componentKey = component.componentKey || component.type || component.component || 'input'
  const runtimeType = componentTypeAlias[componentKey] || componentKey
  const nodeType = resolveRuntimeNodeType(componentKey)
  const fieldCode = getRuntimeFieldCode(component)
  const rules = [...(component.validation?.rules || [])]
  const runtimeProps = resolveRuntimeFieldProps(component, componentKey, fieldCode)
  const readonly = mode === 'detail' || Boolean(component.visibility?.readonly)
  const disabled = readonly || Boolean(runtimeProps.disabled)

  if (component.validation?.required && !rules.some(rule => rule?.required)) {
    rules.unshift({
      required: true,
      message: component.validation.requiredMessage || `请输入${component.label || ''}`,
      trigger: ['blur', 'change'],
    })
  }

  const normalized = {
    ...component,
    ...runtimeProps,
    type: runtimeType,
    component: runtimeType,
    componentKey,
    field: fieldCode,
    prop: fieldCode,
    path: fieldCode,
    name: fieldCode,
    label: component.label,
    required: Boolean(component.validation?.required),
    rules,
    hidden: Boolean(component.visibility?.hidden),
    readonly,
    disabled,
    span: component.layout?.span ?? component.span,
    children: Array.isArray(component.children)
      ? component.children.map(child => normalizeRuntimeComponent(child, mode)).filter(Boolean)
      : component.children,
    props: {
      ...runtimeProps,
      disabled,
      readonly,
    },
  }

  if (nodeType) {
    normalized.nodeType = nodeType
    if (nodeType !== 'field') {
      delete normalized.field
      delete normalized.prop
      delete normalized.path
      delete normalized.name
    }
  }

  return normalized
}

function resolveRuntimeProps(props = {}, componentKey = '') {
  const nextProps = { ...(props || {}) }
  if (nextProps.dictType && ['select', 'dictSelect', 'radio', 'radioButton', 'checkbox', 'cascader'].includes(componentKey))
    delete nextProps.options
  return nextProps
}

function resolveRuntimeFieldProps(component = {}, componentKey = '', fieldCode = '') {
  const runtimeProps = resolveRuntimeProps(component.props || {}, componentKey)
  const fieldAsset = findFieldAsset(fieldCode)
  return mergeRelationRuntimeProps(runtimeProps, component, fieldAsset)
}

function findFieldAsset(fieldCode = '') {
  const code = String(fieldCode || '').trim()
  if (!code)
    return null
  return (Array.isArray(props.fields) ? props.fields : []).find((field) => {
    const candidates = [
      field?.fieldCode,
      field?.field,
      field?.columnName,
      field?.prop,
      field?.name,
    ].map(value => String(value ?? '').trim()).filter(Boolean)
    return candidates.includes(code)
  }) || null
}

function mergeRelationRuntimeProps(runtimeProps = {}, component = {}, fieldAsset = null) {
  const next = { ...(runtimeProps || {}) }
  const fieldProps = {
    ...(fieldAsset?.basicProps && typeof fieldAsset.basicProps === 'object' ? fieldAsset.basicProps : {}),
    ...(fieldAsset?.props && typeof fieldAsset.props === 'object' ? fieldAsset.props : {}),
  }
  const componentKey = component?.componentKey || component?.type || ''
  const fieldType = String(fieldAsset?.fieldType || fieldAsset?.businessFieldType || '').trim()

  const referenceObjectCode = firstText(
    next.referenceObjectCode,
    component.props?.referenceObjectCode,
    component.referenceObjectCode,
    fieldAsset?.props?.referenceObjectCode,
    fieldAsset?.referenceObjectCode,
    fieldProps.referenceObjectCode,
  )
  const referenceDisplayField = firstText(
    next.referenceDisplayField,
    next.displayField,
    next.labelField,
    component.props?.referenceDisplayField,
    component.props?.displayField,
    component.props?.labelField,
    component.referenceDisplayField,
    fieldAsset?.props?.referenceDisplayField,
    fieldAsset?.props?.displayField,
    fieldAsset?.props?.labelField,
    fieldAsset?.referenceDisplayField,
    fieldProps.referenceDisplayField,
  )
  const referenceValueField = firstText(
    next.referenceValueField,
    next.valueField,
    component.props?.referenceValueField,
    component.props?.valueField,
    component.referenceValueField,
    fieldAsset?.props?.referenceValueField,
    fieldAsset?.props?.valueField,
    fieldAsset?.referenceValueField,
    fieldProps.referenceValueField,
    'id',
  )

  if (componentKey === 'objectReference' || fieldType === 'REFERENCE') {
    if (referenceObjectCode)
      next.referenceObjectCode = referenceObjectCode
    if (referenceDisplayField)
      next.referenceDisplayField = referenceDisplayField
    if (referenceValueField)
      next.referenceValueField = referenceValueField
  }

  const recordSelector = normalizeRuntimeRecordSelectorConfig({
    ...(fieldAsset || {}),
    ...(component || {}),
    ...(next || {}),
    basicProps: {
      ...(fieldAsset?.basicProps || {}),
      ...(component?.basicProps || {}),
    },
    props: {
      ...(fieldAsset?.props || {}),
      ...(fieldAsset?.basicProps || {}),
      ...(component?.props || {}),
      ...(next || {}),
    },
    recordSelector: next.recordSelector
      || component.props?.recordSelector
      || component.recordSelector
      || fieldAsset?.props?.recordSelector
      || fieldAsset?.basicProps?.recordSelector
      || fieldAsset?.recordSelector,
  })
  if ((componentKey === 'recordSelector' || fieldType === 'RECORD_SELECTOR') && recordSelector.objectCode) {
    next.recordSelector = recordSelector
    next.objectCode = recordSelector.objectCode
    next.businessObjectCode = recordSelector.businessObjectCode || recordSelector.objectCode
    next.targetObjectCode = recordSelector.targetObjectCode || recordSelector.objectCode
  }
  return next
}

function firstText(...values) {
  return values.map(value => String(value ?? '').trim()).find(Boolean) || ''
}

function resolveRuntimeNodeType(componentKey = '') {
  if (['row', 'fcRow'].includes(componentKey))
    return 'row'
  if (componentKey === 'col')
    return 'col'
  if (['card', 'elCard'].includes(componentKey))
    return 'card'
  if (['tabs', 'elTabs'].includes(componentKey))
    return 'tabs'
  if (['tabPane', 'elTabPane'].includes(componentKey))
    return 'tabPane'
  if (['collapse', 'elCollapse'].includes(componentKey))
    return 'collapse'
  if (['collapseItem', 'elCollapseItem'].includes(componentKey))
    return 'collapseItem'
  if (isFormDividerRuntimeComponent(componentKey))
    return 'divider'
  if (isGroupTitleRuntimeComponent(componentKey))
    return 'groupTitle'
  if (['button', 'table', 'tableGrid'].includes(componentKey))
    return componentKey
  if (isCrudRuntimeComponent(componentKey))
    return 'AiCrudPage'
  return ''
}

function buildRuntimeFormSchema(schema, mode = 'create') {
  const components = Array.isArray(schema) ? schema : schema?.components || []
  return components.map(component => preparePreviewNode(component)).filter(Boolean)
}

/**
 * 预览专用：为 AiForm + AiFormLayoutNodes 补齐 nodeType，不做运行时属性注入。
 * 避免 normalizeRuntimeComponent 添加的 hidden/visibility/disabled/readonly/required/rules
 * 干扰 AiForm 内部的 filterVisibleNodes 和 applyRuntimeControl 逻辑。
 */
function preparePreviewNode(component) {
  if (!component || typeof component !== 'object')
    return null
  const componentKey = component.componentKey || component.type || 'input'
  const nodeType = resolveRuntimeNodeType(componentKey)
  const children = Array.isArray(component.children)
    ? component.children.map(child => preparePreviewNode(child)).filter(Boolean)
    : []
  const isWidget = isPageWidgetComponentKey(componentKey)
  const result = {
    ...component,
    componentKey,
    children,
    ...(isWidget ? { fieldBinding: { ...(component.fieldBinding || {}), mode: component.fieldBinding?.mode || 'virtual' } } : {}),
  }
  if (nodeType) {
    result.nodeType = nodeType
    if (nodeType !== 'field') {
      delete result.field
      delete result.prop
      delete result.path
      delete result.name
    }
  }
  return result
}

function getDefaultRuntimeValue(component, mode = 'create') {
  if (mode === 'edit' || mode === 'detail')
    return getMockRuntimeValue(component)
  if (component?.props && Object.prototype.hasOwnProperty.call(component.props, 'defaultValue')) {
    return component.props.defaultValue
  }
  if (Object.prototype.hasOwnProperty.call(component || {}, 'defaultValue')) {
    return component.defaultValue
  }
  const componentKey = component?.componentKey || component?.type
  if (['date', 'datetime', 'month', 'year', 'time', 'daterange', 'datetimerange', 'timerange'].includes(componentKey))
    return null
  if (componentKey === 'checkbox' || componentKey === 'checkboxGroup')
    return []
  if (componentKey === 'switch')
    return false
  if (componentKey === 'rate' || componentKey === 'slider' || componentKey === 'number' || componentKey === 'inputNumber')
    return null
  return ''
}

function getMockRuntimeValue(component) {
  const componentKey = component?.componentKey || component?.type
  const label = component?.label || component?.props?.placeholder || '字段'
  const options = Array.isArray(component?.props?.options) ? component.props.options : []
  const firstOption = options[0]?.value ?? options[0]?.label ?? '1'

  if (['number', 'inputNumber', 'rate', 'slider'].includes(componentKey))
    return 100
  if (['select', 'radio', 'radioGroup'].includes(componentKey))
    return firstOption
  if (['checkbox', 'checkboxGroup'].includes(componentKey))
    return [firstOption]
  if (componentKey === 'switch')
    return true
  if (componentKey === 'date')
    return '2026-06-17'
  if (componentKey === 'datetime')
    return '2026-06-17 09:30:00'
  if (componentKey === 'month')
    return '2026-06'
  if (componentKey === 'year')
    return '2026'
  if (componentKey === 'time')
    return '09:30:00'
  if (componentKey === 'daterange')
    return ['2026-06-17', '2026-06-18']
  if (componentKey === 'datetimerange')
    return ['2026-06-17 09:30:00', '2026-06-18 18:00:00']
  if (componentKey === 'timerange')
    return ['09:30:00', '18:00:00']
  if (componentKey === 'textarea')
    return `${label}的模拟详情内容`
  if (['upload', 'imageUpload'].includes(componentKey))
    return []
  return `${label}示例`
}

function buildRuntimeFormValue(schema, mode = 'create') {
  const components = Array.isArray(schema) ? schema : schema?.components || []
  return components.reduce((model, component) => {
    const fieldCode = getRuntimeFieldCode(component)
    if (fieldCode)
      model[fieldCode] = getDefaultRuntimeValue(component, mode)
    return model
  }, {})
}

function isCrudRuntimeComponent(componentKey) {
  return ['crud', 'crudBlock', 'AiCrudPage', 'aiCrudPage'].includes(componentKey)
}

function isGroupTitleRuntimeComponent(componentKey) {
  return [
    'title',
    'fcTitle',
    'sectionTitle',
    'groupTitle',
    'groupHeader',
    'GroupHeader',
    'titleBlock',
    'section',
  ].includes(componentKey)
}

function isFormDividerRuntimeComponent(componentKey) {
  return [
    'AiFormSectionTitle',
    'aiFormSectionTitle',
    'formSectionTitle',
    'FormSectionTitle',
    'divider',
    'elDivider',
  ].includes(componentKey)
}

// ─── Preview section-split helpers removed ───
// Preview now passes the full normalized schema to AiForm + AiFormLayoutNodes,
// which handles tabs / card / collapse / divider / crud natively.

if (typeof window !== 'undefined') {
  window.addEventListener('forge-form-designer:preview-current-form', () => {
    previewDialogVisible.value = true
  })
}

function openRenameCurrentForm() {
  renameFormName.value = props.modelValue?.formName || '未命名表单'
  renameDialogVisible.value = true
}

function confirmRenameCurrentForm() {
  const activeSchema = cloneDesignerSchema(normalizedSchema.value)
  const nextName = renameFormName.value.trim()
  if (!activeSchema || !nextName)
    return

  activeSchema.formName = nextName
  const currentKey = activeSchema.formKey
  const assets = activeSchema.settings?.formAssets || []
  const currentAsset = assets.find(asset => asset?.formKey === currentKey)
  if (currentAsset) {
    currentAsset.formName = nextName
    if (currentAsset.schema) {
      currentAsset.schema.formName = nextName
    }
  }

  renameDialogVisible.value = false
  updateSchema({ ...activeSchema })
}

onMounted(() => {
  window.addEventListener('keydown', handleDesignerShortcut)
  window.addEventListener('forge-form-designer:canvas-drag-start', handleCanvasDragStart)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleDesignerShortcut)
  window.removeEventListener('forge-form-designer:canvas-drag-start', handleCanvasDragStart)
})
</script>

<style scoped>
.forge-form-designer {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr) 0;
  height: 100%;
  min-height: 0;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #f7f8fa;
  overflow: hidden;
}

.forge-form-designer.left-collapsed {
  grid-template-columns: 42px minmax(0, 1fr) 0;
}

.forge-form-designer.right-open {
  grid-template-columns: 248px minmax(0, 1fr) 336px;
}

.forge-form-designer.left-collapsed.right-open {
  grid-template-columns: 42px minmax(0, 1fr) 336px;
}

.designer-left,
.designer-right {
  min-width: 0;
  min-height: 0;
  background: #fff;
}

.designer-left {
  position: relative;
  border-right: 1px solid #e5e7eb;
  overflow: hidden;
}

.side-rail-toggle-button {
  position: absolute;
  top: 12px;
  left: 7px;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  cursor: pointer;
  border: 1px solid #e5e6eb;
  border-radius: 7px;
  background: #fff;
  color: #475569;
  z-index: 5;
}

.side-rail-toggle-button {
  left: 50%;
  transform: translateX(-50%);
}

.side-rail-toggle-button:hover {
  border-color: #c9cdd4;
  background: #f2f3f5;
  color: #1f2329;
}

.designer-right {
  position: relative;
  border-left: 1px solid #e5e6eb;
  overflow: hidden;
}

.forge-form-designer.right-collapsed .designer-right {
  display: none;
  border-left: 0;
}

.designer-center {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.designer-toolbar {
  min-height: 62px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.designer-toolbar h3,
.designer-toolbar p {
  margin: 0;
}

.designer-toolbar h3 {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.designer-toolbar p {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

@media (max-width: 1360px) {
  .forge-form-designer {
    grid-template-columns: 220px minmax(0, 1fr) 0;
  }

  .forge-form-designer.left-collapsed {
    grid-template-columns: 42px minmax(0, 1fr) 0;
  }

  .forge-form-designer.right-open {
    grid-template-columns: 220px minmax(0, 1fr) minmax(300px, 32vw);
  }

  .forge-form-designer.left-collapsed.right-open {
    grid-template-columns: 42px minmax(0, 1fr) minmax(300px, 32vw);
  }

  .designer-right {
    grid-column: auto;
    border-top: 0;
    border-left: 1px solid #e5e6eb;
  }
}
.designer-form-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  max-width: 100%;
  overflow-x: auto;
  padding: 0 2px 3px;
}

.designer-form-tabs-bar {
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
  padding: 8px 12px 6px;
}

.designer-toolbar-text-button {
  --n-color: #f2f3f5 !important;
  --n-color-hover: #e5e6eb !important;
  --n-color-pressed: #c9cdd4 !important;
  --n-color-focus: #f2f3f5 !important;
  --n-border: 1px solid #e5e6eb !important;
  --n-border-hover: 1px solid #c9cdd4 !important;
  --n-border-pressed: 1px solid #86909c !important;
  --n-border-focus: 1px solid #c9cdd4 !important;
  --n-text-color: #1f2329 !important;
  --n-text-color-hover: #1f2329 !important;
  --n-text-color-pressed: #1f2329 !important;
  --n-text-color-focus: #1f2329 !important;
  font-weight: 600;
}

.designer-toolbar-more-button,
.designer-toolbar-icon-button {
  --n-color: #4e5969 !important;
  --n-color-hover: #1f2329 !important;
  --n-color-pressed: #1f2329 !important;
  --n-color-focus: #4e5969 !important;
  --n-border: 1px solid #4e5969 !important;
  --n-border-hover: 1px solid #1f2329 !important;
  --n-border-pressed: 1px solid #1f2329 !important;
  --n-border-focus: 1px solid #4e5969 !important;
  --n-text-color: #fff !important;
  --n-text-color-hover: #fff !important;
  --n-text-color-pressed: #fff !important;
  --n-text-color-focus: #fff !important;
  box-shadow: 0 6px 14px rgba(31, 35, 41, 0.12);
}

.designer-toolbar-more-button :deep(.n-button__icon),
.designer-toolbar-more-button :deep(.n-icon),
.designer-toolbar-icon-button :deep(.n-button__icon),
.designer-toolbar-icon-button :deep(.n-icon) {
  color: #fff !important;
}

.field-shelf-collapse-button {
  flex: 0 0 auto;
  --n-color: #f7f8fa !important;
  --n-color-hover: #f2f3f5 !important;
  --n-color-pressed: #e5e6eb !important;
  --n-color-focus: #f7f8fa !important;
  --n-border: 1px solid #e5e6eb !important;
  --n-border-hover: 1px solid #c9cdd4 !important;
  --n-border-pressed: 1px solid #86909c !important;
  --n-border-focus: 1px solid #c9cdd4 !important;
  --n-text-color: #475569 !important;
  --n-text-color-hover: #1f2329 !important;
  --n-text-color-pressed: #1f2329 !important;
  --n-text-color-focus: #1f2329 !important;
}

.designer-toolbar-danger-button {
  --n-color: #fff7f7 !important;
  --n-color-hover: #fee2e2 !important;
  --n-color-pressed: #fecaca !important;
  --n-color-focus: #fff7f7 !important;
  --n-border: 1px solid #fecaca !important;
  --n-border-hover: 1px solid #fca5a5 !important;
  --n-border-pressed: 1px solid #f87171 !important;
  --n-border-focus: 1px solid #fca5a5 !important;
  --n-text-color: #dc2626 !important;
  --n-text-color-hover: #b91c1c !important;
  --n-text-color-pressed: #991b1b !important;
  --n-text-color-focus: #dc2626 !important;
}

.designer-form-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
  max-width: 176px;
  height: 34px;
  cursor: pointer;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  background: #fff;
  color: #475569;
  padding: 0 8px;
}

.designer-form-tab:hover {
  border-color: #c9cdd4;
  background: #f7f8fa;
}

.designer-form-tab.active {
  border-color: transparent;
  border-bottom: 2px solid #1f2329;
  border-radius: 0 0 4px 4px;
  background: transparent;
  color: #1f2329;
  box-shadow: none;
}

.designer-form-tab em {
  display: inline-grid;
  place-items: center;
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #e2e8f0;
  color: #475569;
  font-style: normal;
  font-size: 11px;
  font-weight: 700;
}

.designer-form-tab.active em {
  background: #4e5969;
  color: #fff;
}

.designer-form-tab span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 600;
}

.forge-form-designer {
  display: grid;
  grid-template-columns: 256px minmax(0, 1fr) 320px;
  height: 100%;
  min-height: 0;
  border: 1px solid #e4e4e7;
  border-radius: 8px;
  background: #f8f9fa;
  overflow: hidden;
}

.forge-form-designer.left-collapsed {
  grid-template-columns: 36px minmax(0, 1fr) 320px;
}

.forge-form-designer.right-collapsed {
  grid-template-columns: 256px minmax(0, 1fr) 0;
}

.forge-form-designer.left-collapsed.right-collapsed {
  grid-template-columns: 36px minmax(0, 1fr) 0;
}

.forge-form-designer.canvas-focused,
.forge-form-designer.canvas-focused.left-collapsed,
.forge-form-designer.canvas-focused.right-collapsed,
.forge-form-designer.canvas-focused.left-collapsed.right-collapsed {
  grid-template-columns: 0 minmax(0, 1fr) 0;
}

.forge-form-designer.section-view,
.forge-form-designer.section-view.left-collapsed,
.forge-form-designer.section-view.right-open,
.forge-form-designer.section-view.right-collapsed,
.forge-form-designer.section-view.canvas-focused {
  grid-template-columns: minmax(0, 1fr);
}

.forge-form-designer.canvas-focused .designer-left,
.forge-form-designer.canvas-focused .designer-right {
  border: 0;
  overflow: hidden;
}

.designer-left,
.designer-center,
.designer-right {
  min-width: 0;
  min-height: 0;
}

.designer-left {
  border-right: 1px solid #e4e4e7;
  background: #fcfcfc;
}

.designer-right {
  border-left: 1px solid #e4e4e7;
  background: #fafafa;
}

.designer-center {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  background: #f8f9fa;
  overflow: hidden;
}

.designer-toolbar {
  display: none;
  min-height: 48px;
  padding: 7px 12px;
  border-bottom: 1px solid #e4e4e7;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
}

.designer-toolbar h3 {
  margin: 0;
  color: #18181b;
  font-size: 13px;
  line-height: 18px;
}

.designer-toolbar p {
  margin: 1px 0 0;
  color: #71717a;
  font-size: 11px;
  line-height: 15px;
}

.designer-form-tabs-bar {
  padding: 8px 12px;
  border-bottom: 1px solid #e4e4e7;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(10px);
}

.page-design-switcher {
  z-index: 5;
  display: grid;
  grid-template-columns: minmax(160px, 1fr) minmax(220px, 1.4fr) minmax(64px, 1fr);
  align-items: center;
  gap: 12px;
}

.page-switch-title {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.page-switch-icon {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 1px solid #dbeafe;
  border-radius: 7px;
  background: #eef2ff;
  color: #3153d8;
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}

.page-switch-title strong,
.page-switch-title small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.page-switch-title strong {
  color: #27272a;
  font-size: 13px;
  line-height: 16px;
}

.page-switch-title small {
  margin-top: 2px;
  color: #a1a1aa;
  font-size: 10px;
  line-height: 13px;
}

.page-tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  justify-self: end;
  min-width: 0;
  gap: 10px;
}

.page-view-controls {
  display: grid;
  min-width: 0;
  justify-items: center;
  gap: 6px;
}

.page-view-controls :deep(.n-radio-group) {
  display: inline-flex;
  max-width: 100%;
}

.inline-page-section-editor,
.inline-detail-settings {
  min-height: 0;
  overflow: auto;
  padding: 14px;
}

.inline-page-section-editor :deep(.section-workbench) {
  min-height: 520px;
}

.inline-detail-settings > :deep(*) {
  min-height: 100%;
}

.page-switch-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
  border-left: 1px solid #e4e4e7;
  padding-left: 10px;
}

.page-section-entry-button {
  --n-color: #eef4ff !important;
  --n-color-hover: #e0eaff !important;
  --n-border: 1px solid #c9d8ff !important;
  --n-border-hover: 1px solid #8eacff !important;
  --n-text-color: #3153d8 !important;
  --n-text-color-hover: #2445bd !important;
}

.designer-form-tabs {
  justify-content: center;
  justify-self: center;
  max-width: 100%;
  overflow-x: auto;
  scrollbar-width: none;
}

.designer-form-tabs::-webkit-scrollbar {
  display: none;
}

.designer-form-tab {
  height: 30px;
  min-width: 76px;
  max-width: 156px;
  border: 0;
  border-bottom: 2px solid transparent;
  border-radius: 0;
  background: transparent;
  padding: 0 9px;
  color: #71717a;
  box-shadow: none;
}

.designer-form-tab:hover {
  background: rgba(228, 228, 231, 0.6);
}

.designer-form-tab.active {
  border-bottom-color: #1f2329;
  background: transparent;
  color: #1f2329;
  box-shadow: none;
}

.designer-form-tab em {
  width: 16px;
  height: 16px;
  background: #e4e4e7;
  color: #71717a;
  font-size: 10px;
}

.designer-form-tab.active em {
  background: #4266f7;
  color: #fff;
}

.designer-form-tab span {
  font-size: 12px;
}

.designer-toolbar-text-button,
.designer-toolbar-icon-button.neutral,
.designer-toolbar-more-button {
  --n-color: #fff !important;
  --n-color-hover: #f4f6ff !important;
  --n-color-pressed: #e8edff !important;
  --n-color-focus: #fff !important;
  --n-border: 1px solid #e4e4e7 !important;
  --n-border-hover: 1px solid #c7d2fe !important;
  --n-border-pressed: 1px solid #a5b4fc !important;
  --n-border-focus: 1px solid #c7d2fe !important;
  --n-text-color: #52525b !important;
  --n-text-color-hover: #3153d8 !important;
  --n-text-color-pressed: #253fb2 !important;
  --n-text-color-focus: #3153d8 !important;
  box-shadow: none;
  font-weight: 600;
}

.designer-toolbar-icon-button.neutral.danger {
  --n-text-color-hover: #dc2626 !important;
  --n-border-hover: 1px solid #fecaca !important;
  --n-color-hover: #fff7f7 !important;
}

.designer-toolbar-more-button :deep(.n-button__icon),
.designer-toolbar-more-button :deep(.n-icon),
.designer-toolbar-icon-button :deep(.n-button__icon),
.designer-toolbar-icon-button :deep(.n-icon) {
  color: inherit !important;
}

.field-shelf-collapse-button {
  --n-color: #fff !important;
  --n-color-hover: #f4f6ff !important;
  --n-border: 1px solid #e4e4e7 !important;
  --n-border-hover: 1px solid #c7d2fe !important;
  --n-text-color: #71717a !important;
  --n-text-color-hover: #3153d8 !important;
}
.designer-toolbar {
  position: relative;
}

.designer-toolbar > h3 {
  order: 0;
}

.designer-toolbar-rename-btn {
  flex: 0 0 auto;
  margin-left: 6px;
  order: 1;
  color: #2f63f6;
  background: #eef4ff;
  border: 1px solid #d8e4ff;
}

.designer-toolbar-rename-btn:hover {
  color: #1f4fd8;
  background: #e2ecff;
}

.designer-toolbar-rename-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  font-size: 13px;
  line-height: 1;
}

.designer-toolbar-title-row {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.designer-toolbar-title-row h3 {
  margin: 0;
}

.designer-toolbar > :not(h3):not(.designer-toolbar-rename-btn) {
  order: 2;
}

.designer-rename-modal {
  width: 360px;
}

.designer-rename-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.designer-clear-content {
  display: grid;
  gap: 16px;
}

.designer-clear-warning {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 10px;
  padding: 12px;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  background: #fff7ed;
  color: #9a3412;
}

.designer-clear-warning :deep(.n-icon) {
  margin-top: 1px;
  font-size: 22px;
}

.designer-clear-warning strong,
.designer-clear-warning span {
  display: block;
}

.designer-clear-warning strong {
  color: #7c2d12;
  font-size: 14px;
  line-height: 20px;
}

.designer-clear-warning span {
  margin-top: 2px;
  color: #9a3412;
  font-size: 12px;
  line-height: 18px;
}

.designer-clear-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

:global(.page-section-modal.n-card) {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
}

:global(.page-section-modal .n-card-header),
:global(.page-section-modal .n-card__footer) {
  flex: 0 0 auto;
  padding: 14px 18px;
}

:global(.page-section-modal .n-card-header) {
  border-bottom: 1px solid #eef2f7;
}

:global(.page-section-modal .n-card__content),
:global(.page-section-modal .n-card-content) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  padding: 16px 18px;
  overscroll-behavior: contain;
}

:global(.page-section-modal .n-card__footer) {
  border-top: 1px solid #eef2f7;
}

:global(.designer-preview-modal.n-card) {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
}

:global(.designer-preview-modal .n-card-header) {
  flex: 0 0 auto;
  min-height: 56px;
  padding: 14px 18px;
  border-bottom: 1px solid #eef2f7;
}

:global(.designer-preview-modal .n-card__content),
:global(.designer-preview-modal .n-card-content) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  padding: 12px;
  background: #fff;
  overscroll-behavior: contain;
}

.designer-preview-toolbar {
  position: sticky;
  z-index: 5;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin: 0 0 14px;
  padding: 10px 12px;
  border: 1px solid #dbe6f5;
  border-radius: 8px;
  background: #f8fbff;
}

.designer-preview-toolbar strong,
.designer-preview-toolbar span {
  display: block;
}

.designer-preview-toolbar strong {
  color: #1f2937;
  font-size: 14px;
  font-weight: 650;
  line-height: 20px;
}

.designer-preview-toolbar span {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.designer-preview-runtime-form {
  padding: 4px 2px 0;
}

.designer-preview-runtime {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 720px;
  min-height: 100%;
  padding: 2px 0 0;
  background: #fff;
}

.designer-preview-runtime-card,
.designer-preview-runtime-tabs,
.designer-preview-runtime-collapse,
.designer-preview-runtime-crud {
  width: 100%;
}

.designer-preview-runtime-section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 32px;
  margin: 2px 0 0;
  color: #1f2937;
  font-size: 15px;
  font-weight: 600;
}

.designer-preview-runtime-section-title::before {
  width: 3px;
  height: 16px;
  border-radius: 2px;
  background: #4266f7;
  content: '';
}

.designer-preview-runtime-crud {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.designer-preview-runtime-crud-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.designer-preview-runtime-crud-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.designer-preview-runtime-crud-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
}

.designer-preview-runtime-crud-search {
  margin-bottom: 12px;
  padding: 10px;
  border-radius: 6px;
  background: #f8fafc;
}

.designer-preview-runtime-placeholder {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 42px;
  padding: 10px 12px;
  border: 1px dashed #d8e0f0;
  border-radius: 6px;
  background: #f8fafc;
  color: #334155;
}

.designer-preview-runtime-placeholder small {
  color: #64748b;
}

.designer-preview-runtime-fallback {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  color: #1f2937;
}

.designer-preview-runtime-fallback small {
  color: #64748b;
}
</style>

<!-- 全局打印样式：仅打印表单/列表设计预览内容 -->
<style>
@media print {
  /* 隐藏页面所有常规元素 */
  body > *:not(.n-modal-container),
  .n-modal-container .n-card-header,
  .n-modal-container .n-card__close,
  .n-modal-container .designer-preview-toolbar,
  .forge-form-designer > aside,
  .forge-form-designer > main,
  .business-list-designer > .list-designer-head,
  .business-list-designer > .list-page-switch,
  .business-list-designer > .list-designer-body > .list-designer-left,
  .business-list-designer > .list-designer-body > .list-designer-right,
  .n-modal-overlay,
  .n-modal-mask {
    display: none !important;
  }

  body,
  html {
    margin: 0 !important;
    padding: 0 !important;
    background: #fff !important;
    overflow: visible !important;
  }

  /* 让模态框铺满打印页 */
  .n-modal-container,
  .n-modal-body-wrapper,
  .n-modal-body {
    position: static !important;
    inset: auto !important;
    width: 100% !important;
    max-width: 100% !important;
    height: auto !important;
    max-height: none !important;
    margin: 0 !important;
    padding: 0 !important;
    box-shadow: none !important;
    border-radius: 0 !important;
    background: #fff !important;
    overflow: visible !important;
  }

  .n-card {
    box-shadow: none !important;
    border: none !important;
  }

  .n-card__content {
    padding: 0 !important;
  }

  /* 打印预览内容铺满 */
  .designer-preview-runtime,
  .list-preview-modal .n-card__content {
    padding: 12mm !important;
    overflow: visible !important;
    height: auto !important;
  }

  /* 避免组件在分页处断裂 */
  .ai-form-section,
  .n-form-item,
  .n-card,
  .n-data-table,
  .n-collapse-item {
    break-inside: avoid;
    page-break-inside: avoid;
  }
}
</style>
