<template>
  <div
    class="forge-form-designer"
    :class="{ 'left-collapsed': leftCollapsed, 'right-open': rightOpen, 'right-collapsed': !rightOpen }"
  >
    <aside class="designer-left">
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
      <div class="designer-form-tabs-bar" @contextmenu.prevent="openFormTabsMenu">
        <div class="designer-form-tabs" aria-label="表单切换">
          <button type="button" class="designer-form-tab active" @click="selectedId = ''">
            <em>1</em>
            <span>{{ normalizedSchema.formName || '主表单' }}</span>
            <strong>当前</strong>
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

      <ForgeFormCanvas
        :schema="normalizedSchema"
        :fields="fields"
        :selected-id="selectedId"
        @update:schema="updateSchema"
        @update:selected-id="handleCanvasSelectedIdChange"
        @configure="openPropertyPanel"
      />
    </main>

    <aside class="designer-right">
      <ForgePropertyPanel
        v-if="rightOpen"
        :schema="normalizedSchema"
        :selected-id="selectedId"
        @update:schema="updateSchema"
        @update:selected-id="selectedId = $event"
        @close="rightOpen = false"
      />
    </aside>
  </div>
  <n-modal
    v-model:show="previewDialogVisible"
    preset="card"
    title="预览当前表单"
    class="designer-preview-modal"
    :bordered="false"
    :style="{ width: '70%' }"
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
      <template v-for="section in previewSections" :key="section.id">
        <AiForm
          v-if="section.kind === 'form'"
          class="designer-preview-runtime-form"
          :schema="section.schema"
          :value="section.value"
          :label-placement="previewLayout.labelPlacement || 'left'"
          :label-width="previewLayout.labelWidth ?? 'auto'"
          :label-align="previewLayout.labelAlign || 'right'"
          :size="previewLayout.size || 'medium'"
          :grid-cols="previewLayout.gridCols || previewLayout.gridColumns || 1"
          :x-gap="previewLayout.xGap || previewLayout.columnGap || 12"
          :y-gap="previewLayout.yGap || previewLayout.rowGap || 0"
          :show-actions="false"
          :show-feedback="previewLayout.showFeedback !== false"
          :form-assets="formAssets"
        />
        <n-card
          v-else-if="section.componentKey === 'card'"
          class="designer-preview-runtime-card"
          :title="section.component.label || section.component.props?.title"
          size="small"
          embedded
        >
          <AiForm
            v-if="section.childSchema.length"
            :schema="section.childSchema"
            :value="section.childValue"
            :label-placement="previewLayout.labelPlacement || 'left'"
            :label-width="previewLayout.labelWidth ?? 'auto'"
            :label-align="previewLayout.labelAlign || 'right'"
            :size="previewLayout.size || 'medium'"
            :grid-cols="previewLayout.gridCols || previewLayout.gridColumns || 1"
            :show-actions="false"
            :form-assets="formAssets"
          />
        </n-card>
        <n-tabs
          v-else-if="section.componentKey === 'tabs'"
          class="designer-preview-runtime-tabs"
          type="line"
          animated
        >
          <n-tab-pane
            v-for="tab in getRuntimeTabs(section.component)"
            :key="tab.key"
            :name="tab.key"
            :tab="tab.label"
          >
            <AiForm
              v-if="tab.schema.length"
              :schema="tab.schema"
              :value="tab.value"
              :label-placement="previewLayout.labelPlacement || 'left'"
              :label-width="previewLayout.labelWidth ?? 'auto'"
              :label-align="previewLayout.labelAlign || 'right'"
              :size="previewLayout.size || 'medium'"
              :grid-cols="previewLayout.gridCols || previewLayout.gridColumns || 1"
              :show-actions="false"
              :form-assets="formAssets"
            />
          </n-tab-pane>
        </n-tabs>
        <n-collapse
          v-else-if="section.componentKey === 'collapse'"
          class="designer-preview-runtime-collapse"
        >
          <n-collapse-item
            v-for="panel in getRuntimeCollapsePanels(section.component)"
            :key="panel.key"
            :name="panel.key"
            :title="panel.label"
          >
            <AiForm
              v-if="panel.schema.length"
              :schema="panel.schema"
              :value="panel.value"
              :label-placement="previewLayout.labelPlacement || 'left'"
              :label-width="previewLayout.labelWidth ?? 'auto'"
              :label-align="previewLayout.labelAlign || 'right'"
              :size="previewLayout.size || 'medium'"
              :grid-cols="previewLayout.gridCols || previewLayout.gridColumns || 1"
              :show-actions="false"
              :form-assets="formAssets"
            />
          </n-collapse-item>
        </n-collapse>
        <component
          :is="RuntimeAiFormGroupTitle"
          v-else-if="isGroupTitleRuntimeComponent(section.componentKey)"
          class="designer-preview-runtime-section-title"
          v-bind="buildGroupTitleRuntimeProps(section.component)"
        />
        <component
          :is="RuntimeAiFormSectionTitle"
          v-else-if="isFormDividerRuntimeComponent(section.componentKey)"
          v-bind="buildSectionTitleRuntimeProps(section.component)"
        />
        <n-divider v-else-if="section.componentKey === 'divider'">
          {{ section.component.label || section.component.props?.title }}
        </n-divider>
        <component
          :is="RuntimeAiCrudPage"
          v-else-if="isCrudRuntimeComponent(section.componentKey)"
          class="designer-preview-runtime-crud"
          v-bind="buildCrudRuntimeProps(section.component)"
        />
        <div v-else class="designer-preview-runtime-placeholder">
          <span>{{ section.component.label || section.componentKey }}</span>
          <small>该区块暂未接入运行态组件</small>
        </div>
      </template>
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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import AiForm from '@/components/ai-form/AiForm.vue'
import AiFormGroupTitle from '@/components/ai-form/AiFormGroupTitle.vue'
import AiFormSectionTitle from '@/components/ai-form/AiFormSectionTitle.vue'
import { repairFormDesignerFieldRefs } from '../form-first/fieldReferenceUtils'
import { extractForgeSchemaFieldRefs } from '../form-first/forgeToFormCreate'
import {
  applyGridColumnsToFormDesignerSchema,
  createComponentFromField,
  createDefaultFormDesignerSchema,
  normalizeFormDesignerSchema,
} from '../form-first/formDesignerSchema'
import ForgeFieldShelf from './ForgeFieldShelf.vue'
import ForgeFormCanvas from './ForgeFormCanvas.vue'
import ForgePropertyPanel from './ForgePropertyPanel.vue'

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
  extraMoreOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'dirtyChange', 'moreSelect'])

const selectedId = ref('')
const leftCollapsed = ref(false)
const rightOpen = ref(false)
const formTabsMenuVisible = ref(false)
const formTabsMenuX = ref(0)
const formTabsMenuY = ref(0)
const undoStack = ref([])
const redoStack = ref([])
const HISTORY_LIMIT = 50
const previewMode = ref('create')
const clearDialogVisible = ref(false)
const clearScope = ref('current')
const previewModeOptions = [
  { label: '新增', value: 'create' },
  { label: '编辑', value: 'edit' },
  { label: '详情', value: 'detail' },
]
const baseDesignerMoreOptions = [
  { label: '按字段生成', key: 'resetFromFields' },
  { label: '清理失效字段', key: 'repairRefs' },
]

const normalizedSchema = computed(() => normalizeFormDesignerSchema(props.modelValue || createDefaultFormDesignerSchema({
  objectCode: props.objectCode,
  objectName: props.objectName,
  fields: props.fields,
})))
const previewLayout = computed(() => normalizedSchema.value.layout || {})
const previewSections = computed(() => buildRuntimePreviewSections(normalizedSchema.value, previewMode.value))
const usedFieldSet = computed(() => new Set(extractForgeSchemaFieldRefs(normalizedSchema.value || {})))
const formAssets = computed(() => Array.isArray(normalizedSchema.value.settings?.formAssets) ? normalizedSchema.value.settings.formAssets : [])
const designerMoreOptions = computed(() => [
  ...baseDesignerMoreOptions,
  ...(props.extraMoreOptions || []),
])
const canUndo = computed(() => undoStack.value.length > 0)
const canRedo = computed(() => redoStack.value.length > 0)
const canClearCanvas = computed(() => {
  if (normalizedSchema.value.components?.length)
    return true
  return formAssets.value.some(asset => asset?.schema?.components?.length)
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
  const componentCount = countComponents(normalizedSchema.value.components)
  return `${fieldCount} 个业务字段 · ${componentCount} 个画布节点 · ${normalizedSchema.value.layout?.gridColumns || 2} 列`
})

function updateSchema(schema, options = {}) {
  const nextSchema = normalizeFormDesignerSchema(schema || {})
  const currentSchema = normalizeFormDesignerSchema(normalizedSchema.value || {})
  if (isSameDesignerSchema(nextSchema, currentSchema))
    return
  if (options.recordHistory !== false) {
    pushHistorySnapshot(currentSchema)
    redoStack.value = []
  }
  emit('update:modelValue', nextSchema)
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

function handleDesignerMoreSelect(key = '') {
  if (key === 'resetFromFields') {
    resetFromFields()
    return
  }
  if (key === 'repairRefs')
    repairRefs()
  else
    emit('moreSelect', key)
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

function handleCanvasSelectedIdChange(componentId = '') {
  selectedId.value = componentId
  if (componentId)
    rightOpen.value = true
}

function flushDesigner() {
  return normalizeFormDesignerSchema(normalizedSchema.value)
}

function countComponents(components = []) {
  return (Array.isArray(components) ? components : []).reduce((total, component) => {
    return total + 1 + countComponents(component.children || [])
  }, 0)
}

defineExpose({
  flushDesigner,
  resetFromFields,
  repairRefs,
  appendField,
})
const renameDialogVisible = ref(false)
const renameFormName = ref('')
const previewDialogVisible = ref(false)

const RuntimeAiCrudPage = AiCrudPage
const RuntimeAiFormGroupTitle = AiFormGroupTitle
const RuntimeAiFormSectionTitle = AiFormSectionTitle

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
  const runtimeProps = resolveRuntimeProps(component.props || {}, componentKey)
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
  return components.map(component => normalizeRuntimeComponent(component, mode)).filter(Boolean)
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
  if (componentKey === 'time')
    return '09:30:00'
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

const runtimeBlockComponentKeys = new Set([
  'crud',
  'crudBlock',
  'AiCrudPage',
  'aiCrudPage',
  'card',
  'tabs',
  'collapse',
  'divider',
  'elDivider',
  'sectionTitle',
  'title',
  'fcTitle',
  'groupTitle',
  'formSectionTitle',
  'FormSectionTitle',
  'groupHeader',
  'GroupHeader',
  'titleBlock',
  'section',
  'AiFormSectionTitle',
  'aiFormSectionTitle',
])

function isRuntimeBlockComponent(component) {
  return runtimeBlockComponentKeys.has(component?.componentKey || component?.type)
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

function buildGroupTitleRuntimeProps(component) {
  return {
    ...(component?.props || {}),
    title: component?.props?.title || component?.label || '分组标题',
    label: component?.label || component?.props?.title || '分组标题',
  }
}

function buildSectionTitleRuntimeProps(component) {
  return {
    ...(component?.props || {}),
    title: component?.props?.title || component?.label || '表单分隔线',
    label: component?.label || component?.props?.title || '表单分隔线',
  }
}

function createRuntimeFormSection(components, index, mode = 'create') {
  const schema = buildRuntimeFormSchema(components, mode)
  return {
    id: `form-${index}`,
    kind: 'form',
    schema,
    value: buildRuntimeFormValue(components, mode),
  }
}

function createRuntimeBlockSection(component, index, mode = 'create') {
  const componentKey = component?.componentKey || component?.type
  const childSchema = buildRuntimeFormSchema(component?.children || [], mode)
  return {
    id: component?.id || `${componentKey}-${index}`,
    kind: 'block',
    componentKey,
    component,
    childSchema,
    childValue: buildRuntimeFormValue(component?.children || [], mode),
  }
}

function buildRuntimePreviewSections(schema, mode = 'create') {
  const normalized = normalizeFormDesignerSchema(schema || {})
  const sections = []
  let formBuffer = []

  ;(normalized.components || []).forEach((component, index) => {
    if (isRuntimeBlockComponent(component)) {
      if (formBuffer.length) {
        sections.push(createRuntimeFormSection(formBuffer, sections.length, mode))
        formBuffer = []
      }
      sections.push(createRuntimeBlockSection(component, index, mode))
      return
    }
    formBuffer.push(component)
  })

  if (formBuffer.length) {
    sections.push(createRuntimeFormSection(formBuffer, sections.length, mode))
  }

  return sections
}

function normalizeRuntimeContainerItems(component, fallbackLabel) {
  const items = component?.props?.items || component?.props?.tabs || component?.props?.panels || component?.children || []
  if (!Array.isArray(items) || !items.length) {
    return [{
      key: `${component?.id || fallbackLabel}-default`,
      label: component?.label || component?.props?.title || fallbackLabel,
      children: component?.children || [],
    }]
  }

  return items.map((item, index) => ({
    key: item.key || item.name || item.value || `${component?.id || fallbackLabel}-${index}`,
    label: item.label || item.title || item.name || `${fallbackLabel}${index + 1}`,
    children: item.children || item.components || [],
  }))
}

function getRuntimeTabs(component) {
  return normalizeRuntimeContainerItems(component, '标签').map(tab => ({
    ...tab,
    schema: buildRuntimeFormSchema(tab.children, previewMode.value),
    value: buildRuntimeFormValue(tab.children, previewMode.value),
  }))
}

function getRuntimeCollapsePanels(component) {
  return normalizeRuntimeContainerItems(component, '面板').map(panel => ({
    ...panel,
    schema: buildRuntimeFormSchema(panel.children, previewMode.value),
    value: buildRuntimeFormValue(panel.children, previewMode.value),
  }))
}

function getCrudSearchComponents(component) {
  return (component?.children || []).filter((child) => {
    const key = child?.componentKey || child?.type
    return key !== 'table' && key !== 'tableGrid'
  })
}

function getCrudTableComponent(component) {
  return (component?.children || []).find((child) => {
    const key = child?.componentKey || child?.type
    return key === 'table'
  })
}

function getCrudColumns(component) {
  const propsColumns = component?.props?.columns || component?.props?.schema
  if (Array.isArray(propsColumns) && propsColumns.length)
    return propsColumns

  const table = getCrudTableComponent(component)
  const tableColumns = table?.props?.columns
  if (Array.isArray(tableColumns) && tableColumns.length)
    return tableColumns

  const tableChildren = table?.children || []
  return tableChildren
    .flatMap(cell => cell?.children || [])
    .map(child => normalizeRuntimeComponent(child))
    .filter(Boolean)
}

function buildCrudRuntimeProps(component) {
  const searchComponents = getCrudSearchComponents(component)
  const columns = getCrudColumns(component)
  const runtimeColumns = columns.length
    ? columns
    : [{
        title: '暂无列配置',
        key: '__empty',
        field: '__empty',
        prop: '__empty',
        component: 'input',
        type: 'input',
      }]
  const crudOptions = component?.props?.crudOptions || {}

  return {
    ...crudOptions,
    ...(component?.props || {}),
    title: component?.props?.title || component?.label,
    apiConfig: component?.props?.apiConfig || component?.props?.api || {},
    rowKey: component?.props?.rowKey || 'id',
    schema: runtimeColumns,
    columns: runtimeColumns,
    formAssets: formAssets.value,
    searchSchema: component?.props?.searchSchema || component?.props?.querySchema || buildRuntimeFormSchema(searchComponents, previewMode.value),
  }
}

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
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleDesignerShortcut)
})
</script>

<style scoped>
.forge-form-designer {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr) 0;
  height: clamp(680px, calc(100vh - 190px), 860px);
  min-height: 680px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #eef3f8;
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
  border: 1px solid #dbe3ee;
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
  border-color: #93c5fd;
  background: #eff6ff;
  color: #2563eb;
}

.designer-right {
  position: relative;
  border-left: 1px solid #dbe3ee;
  overflow: hidden;
}

.forge-form-designer.right-collapsed .designer-right {
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
    border-left: 1px solid #dbe3ee;
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
  --n-color: #eef6ff !important;
  --n-color-hover: #dbeafe !important;
  --n-color-pressed: #bfdbfe !important;
  --n-color-focus: #eef6ff !important;
  --n-border: 1px solid #bfdbfe !important;
  --n-border-hover: 1px solid #93c5fd !important;
  --n-border-pressed: 1px solid #60a5fa !important;
  --n-border-focus: 1px solid #93c5fd !important;
  --n-text-color: #1d4ed8 !important;
  --n-text-color-hover: #1e40af !important;
  --n-text-color-pressed: #1e3a8a !important;
  --n-text-color-focus: #1d4ed8 !important;
  font-weight: 600;
}

.designer-toolbar-more-button,
.designer-toolbar-icon-button {
  --n-color: #2563eb !important;
  --n-color-hover: #1d4ed8 !important;
  --n-color-pressed: #1e40af !important;
  --n-color-focus: #2563eb !important;
  --n-border: 1px solid #2563eb !important;
  --n-border-hover: 1px solid #1d4ed8 !important;
  --n-border-pressed: 1px solid #1e40af !important;
  --n-border-focus: 1px solid #2563eb !important;
  --n-text-color: #fff !important;
  --n-text-color-hover: #fff !important;
  --n-text-color-pressed: #fff !important;
  --n-text-color-focus: #fff !important;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.18);
}

.designer-toolbar-more-button :deep(.n-button__icon),
.designer-toolbar-more-button :deep(.n-icon),
.designer-toolbar-icon-button :deep(.n-button__icon),
.designer-toolbar-icon-button :deep(.n-icon) {
  color: #fff !important;
}

.field-shelf-collapse-button {
  flex: 0 0 auto;
  --n-color: #f8fafc !important;
  --n-color-hover: #eff6ff !important;
  --n-color-pressed: #dbeafe !important;
  --n-color-focus: #f8fafc !important;
  --n-border: 1px solid #cbd5e1 !important;
  --n-border-hover: 1px solid #93c5fd !important;
  --n-border-pressed: 1px solid #60a5fa !important;
  --n-border-focus: 1px solid #93c5fd !important;
  --n-text-color: #475569 !important;
  --n-text-color-hover: #2563eb !important;
  --n-text-color-pressed: #1d4ed8 !important;
  --n-text-color-focus: #2563eb !important;
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
  border-color: #bfdbfe;
  background: #f8fafc;
}

.designer-form-tab.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.12);
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
  background: #2563eb;
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

.designer-form-tab strong {
  flex: 0 0 auto;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 10px;
  line-height: 16px;
  padding: 0 5px;
}
.forge-form-designer {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 350px;
  min-height: 0;
  overflow: hidden;
}

.forge-form-designer.left-collapsed {
  grid-template-columns: 36px minmax(0, 1fr) 350px;
}

.forge-form-designer.right-collapsed {
  grid-template-columns: 260px minmax(0, 1fr) 36px;
}

.forge-form-designer.left-collapsed.right-collapsed {
  grid-template-columns: 36px minmax(0, 1fr) 36px;
}

.designer-left,
.designer-center,
.designer-right {
  min-width: 0;
  min-height: 0;
}

.designer-center {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  overflow: hidden;
}

.designer-toolbar {
  min-height: 42px;
  padding: 6px 10px;
}

.designer-toolbar h3 {
  margin: 0;
  font-size: 14px;
  line-height: 20px;
}

.designer-toolbar p {
  margin: 1px 0 0;
  font-size: 11px;
  line-height: 16px;
}

.designer-form-tabs-bar {
  padding: 5px 10px 4px;
}

.designer-form-tab {
  height: 28px;
}

.designer-form-tab em {
  width: 16px;
  height: 16px;
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

.designer-preview-modal {
  width: min(760px, calc(100vw - 48px));
}

.designer-preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin: -4px 0 14px;
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
  padding: 2px 0 0;
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
