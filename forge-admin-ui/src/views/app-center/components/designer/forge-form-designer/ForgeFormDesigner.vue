<template>
  <div
    class="forge-form-designer"
    :class="{ 'left-collapsed': leftCollapsed, 'right-open': rightOpen, 'right-collapsed': !rightOpen }"
  >
    <aside class="designer-left">
      <button
        v-if="leftCollapsed"
        type="button"
        class="collapsed-rail-button"
        title="展开组件库"
        @click="leftCollapsed = false"
      >
        <n-icon><ChevronForwardOutline /></n-icon>
      </button>
      <ForgeFieldShelf
        v-else
        :fields="fields"
        :used-field-set="usedFieldSet"
        @append-field="appendField"
      />
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
          <n-button class="designer-toolbar-button" circle size="small" secondary :title="leftCollapsed ? '展开组件库' : '收起组件库'" @click="leftCollapsed = !leftCollapsed">
            <template #icon>
              <n-icon>
                <ChevronForwardOutline v-if="leftCollapsed" />
                <ChevronBackOutline v-else />
              </n-icon>
            </template>
          </n-button>
          <n-button class="designer-toolbar-button" circle size="small" secondary :title="rightOpen ? '收起属性栏' : '打开属性栏'" @click="rightOpen = !rightOpen">
            <template #icon>
              <n-icon>
                <ChevronForwardOutline v-if="rightOpen" />
                <ChevronBackOutline v-else />
              </n-icon>
            </template>
          </n-button>
          <n-button class="designer-toolbar-text-button" size="small" secondary @click="resetFromFields">
            按字段生成
          </n-button>
          <n-button class="designer-toolbar-text-button" size="small" secondary @click="repairRefs">
            清理失效字段
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
        @update:selected-id="selectedId = $event"
        @configure="openPropertyPanel"
      />
    </main>

    <aside class="designer-right">
      <button
        v-if="!rightOpen"
        type="button"
        class="right-rail-button"
        title="打开属性栏"
        @click="openPropertyPanel(selectedId)"
      >
        <n-icon><ChevronBackOutline /></n-icon>
        <span>属性</span>
      </button>
      <ForgePropertyPanel
        v-else
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
import { ChevronBackOutline, ChevronForwardOutline } from '@vicons/ionicons5'
import { NSpace } from 'naive-ui'
import { computed, ref } from 'vue'
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
})

const emit = defineEmits(['update:modelValue', 'dirtyChange'])

const selectedId = ref('')
const leftCollapsed = ref(false)
const rightOpen = ref(false)
const formTabsMenuVisible = ref(false)
const formTabsMenuX = ref(0)
const formTabsMenuY = ref(0)

const normalizedSchema = computed(() => normalizeFormDesignerSchema(props.modelValue || createDefaultFormDesignerSchema({
  objectCode: props.objectCode,
  objectName: props.objectName,
  fields: props.fields,
})))
const previewLayout = computed(() => normalizedSchema.value.layout || {})
const previewSections = computed(() => buildRuntimePreviewSections(normalizedSchema.value))
const usedFieldSet = computed(() => new Set(extractForgeSchemaFieldRefs(normalizedSchema.value || {})))
const formAssets = computed(() => Array.isArray(normalizedSchema.value.settings?.formAssets) ? normalizedSchema.value.settings.formAssets : [])
const formTabsMenuOptions = [
  { label: '＋ 新建空白表单', key: 'create' },
  { label: '⧉ 复制当前表单', key: 'duplicate' },
]
const canvasMetaText = computed(() => {
  const fieldCount = usedFieldSet.value.size
  const componentCount = countComponents(normalizedSchema.value.components)
  return `${fieldCount} 个业务字段 · ${componentCount} 个画布节点 · ${normalizedSchema.value.layout?.gridColumns || 2} 列`
})

function updateSchema(schema) {
  emit('update:modelValue', normalizeFormDesignerSchema(schema || {}))
  emit('dirtyChange', true)
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

function normalizeRuntimeComponent(component) {
  if (!component)
    return component

  const componentKey = component.componentKey || component.type || component.component || 'input'
  const runtimeType = componentTypeAlias[componentKey] || componentKey
  const nodeType = resolveRuntimeNodeType(componentKey)
  const fieldCode = getRuntimeFieldCode(component)
  const rules = [...(component.validation?.rules || [])]

  if (component.validation?.required && !rules.some(rule => rule?.required)) {
    rules.unshift({
      required: true,
      message: component.validation.requiredMessage || `请输入${component.label || ''}`,
      trigger: ['blur', 'change'],
    })
  }

  const normalized = {
    ...component,
    ...component.props,
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
    readonly: Boolean(component.visibility?.readonly),
    disabled: Boolean(component.visibility?.readonly || component.props?.disabled),
    span: component.layout?.span ?? component.span,
    children: Array.isArray(component.children)
      ? component.children.map(child => normalizeRuntimeComponent(child)).filter(Boolean)
      : component.children,
    props: {
      ...component.props,
      disabled: Boolean(component.visibility?.readonly || component.props?.disabled),
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

function buildRuntimeFormSchema(schema) {
  const components = Array.isArray(schema) ? schema : schema?.components || []
  return components.map(component => normalizeRuntimeComponent(component)).filter(Boolean)
}

function getDefaultRuntimeValue(component) {
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

function buildRuntimeFormValue(schema) {
  const components = Array.isArray(schema) ? schema : schema?.components || []
  return components.reduce((model, component) => {
    const fieldCode = getRuntimeFieldCode(component)
    if (fieldCode)
      model[fieldCode] = getDefaultRuntimeValue(component)
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

function createRuntimeFormSection(components, index) {
  const schema = buildRuntimeFormSchema(components)
  return {
    id: `form-${index}`,
    kind: 'form',
    schema,
    value: buildRuntimeFormValue(components),
  }
}

function createRuntimeBlockSection(component, index) {
  const componentKey = component?.componentKey || component?.type
  const childSchema = buildRuntimeFormSchema(component?.children || [])
  return {
    id: component?.id || `${componentKey}-${index}`,
    kind: 'block',
    componentKey,
    component,
    childSchema,
    childValue: buildRuntimeFormValue(component?.children || []),
  }
}

function buildRuntimePreviewSections(schema) {
  const normalized = normalizeFormDesignerSchema(schema || {})
  const sections = []
  let formBuffer = []

  ;(normalized.components || []).forEach((component, index) => {
    if (isRuntimeBlockComponent(component)) {
      if (formBuffer.length) {
        sections.push(createRuntimeFormSection(formBuffer, sections.length))
        formBuffer = []
      }
      sections.push(createRuntimeBlockSection(component, index))
      return
    }
    formBuffer.push(component)
  })

  if (formBuffer.length) {
    sections.push(createRuntimeFormSection(formBuffer, sections.length))
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
    schema: buildRuntimeFormSchema(tab.children),
    value: buildRuntimeFormValue(tab.children),
  }))
}

function getRuntimeCollapsePanels(component) {
  return normalizeRuntimeContainerItems(component, '面板').map(panel => ({
    ...panel,
    schema: buildRuntimeFormSchema(panel.children),
    value: buildRuntimeFormValue(panel.children),
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
    searchSchema: component?.props?.searchSchema || component?.props?.querySchema || buildRuntimeFormSchema(searchComponents),
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
  const activeSchema = props.modelValue
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
  emit('update:modelValue', { ...activeSchema })
}
</script>

<style scoped>
.forge-form-designer {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr) 46px;
  height: clamp(680px, calc(100vh - 190px), 860px);
  min-height: 680px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #eef3f8;
  overflow: hidden;
}

.forge-form-designer.left-collapsed {
  grid-template-columns: 42px minmax(0, 1fr) 46px;
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

.collapsed-rail-button {
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
}

.collapsed-rail-button:hover {
  border-color: #93c5fd;
  background: #eff6ff;
  color: #2563eb;
}

.designer-right {
  position: relative;
  border-left: 1px solid #dbe3ee;
  overflow: hidden;
}

.right-rail-button {
  display: grid;
  grid-template-rows: 24px auto;
  place-items: center;
  gap: 8px;
  width: 100%;
  height: 100%;
  min-height: 0;
  cursor: pointer;
  border: 0;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0;
}

.right-rail-button span {
  writing-mode: vertical-rl;
}

.right-rail-button:hover {
  background: #eff6ff;
  color: #2563eb;
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
    grid-template-columns: 220px minmax(0, 1fr) 46px;
  }

  .forge-form-designer.left-collapsed {
    grid-template-columns: 42px minmax(0, 1fr) 46px;
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

.designer-toolbar-button,
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

.designer-preview-modal {
  width: min(760px, calc(100vw - 48px));
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
