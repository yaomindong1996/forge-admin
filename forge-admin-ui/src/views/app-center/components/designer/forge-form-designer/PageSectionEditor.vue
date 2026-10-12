<template>
  <div class="page-section-editor">
    <div class="section-workbench">
      <aside class="section-catalog">
        <div class="workbench-heading">
          <div>
            <strong>页面分区</strong>
            <span>{{ pageSections.length }} 个</span>
          </div>
          <n-dropdown :options="sectionAddOptions" trigger="click" @select="addSection">
            <n-button size="small" type="primary" secondary>
              <template #icon>
                <n-icon><AddOutline /></n-icon>
              </template>
              新增
            </n-button>
          </n-dropdown>
        </div>

        <draggable
          :model-value="pageSections"
          item-key="sectionId"
          handle=".section-drag-handle"
          ghost-class="section-sort-ghost"
          chosen-class="section-sort-chosen"
          :animation="180"
          class="section-list"
          @update:model-value="replaceSections"
        >
          <template #item="{ element, index }">
            <button
              type="button"
              class="section-list-item"
              :class="{ active: element.sectionId === selectedSectionId }"
              @click="selectedSectionId = element.sectionId"
            >
              <span class="section-drag-handle" title="拖拽排序" @click.stop>
                <n-icon><ReorderFourOutline /></n-icon>
              </span>
              <span class="section-index">{{ index + 1 }}</span>
              <span class="section-list-copy">
                <strong>{{ element.title || `分区 ${index + 1}` }}</strong>
                <small>{{ sectionTypeLabel(element.sectionType) }}</small>
              </span>
              <n-tag v-if="sectionWarningCount(element)" size="tiny" type="warning" :bordered="false">
                {{ sectionWarningCount(element) }}
              </n-tag>
            </button>
          </template>
        </draggable>

        <n-empty v-if="!pageSections.length" size="small" description="尚未配置页面分区" class="section-empty" />
      </aside>

      <main class="section-settings">
        <template v-if="selectedSection">
          <div class="settings-heading">
            <div>
              <n-tag size="small" :bordered="false">
                {{ sectionTypeLabel(selectedSection.sectionType) }}
              </n-tag>
              <strong>{{ selectedSection.title }}</strong>
            </div>
            <n-space size="small">
              <n-button
                v-if="selectedSection.sectionType === 'child_table'"
                size="small"
                secondary
                type="primary"
                @click="emit('editChildTableSection', { section: { ...selectedSection } })"
              >
                编辑子表配置
              </n-button>
              <n-button size="small" quaternary type="error" @click="handleRemoveSelectedSection">
                <template #icon>
                  <n-icon><TrashOutline /></n-icon>
                </template>
                删除分区
              </n-button>
            </n-space>
          </div>

          <div class="settings-grid">
            <n-form-item label="分区标题">
              <n-input
                :value="selectedSection.title"
                placeholder="例如：收款信息"
                @update:value="patchSelectedSection({ title: $event })"
              />
            </n-form-item>
            <n-form-item label="页面模式">
              <n-checkbox-group
                :value="visibleModes(selectedSection)"
                @update:value="patchSelectedSection({ visibleInModes: updateVisibleModes(selectedSection, $event) })"
              >
                <n-space>
                  <n-checkbox
                    v-for="mode in modeOptions"
                    :key="mode.value"
                    :value="mode.value"
                    :disabled="isOnlyVisibleMode(selectedSection, mode.value)"
                  >
                    {{ mode.label }}
                  </n-checkbox>
                </n-space>
              </n-checkbox-group>
            </n-form-item>
          </div>

          <template v-if="selectedSection.sectionType === 'card'">
            <section class="settings-block">
              <div class="block-heading">
                <div>
                  <strong>分区字段</strong>
                  <span>{{ selectedSection.fields?.length || 0 }} 个</span>
                </div>
              </div>
              <n-select
                :value="selectedSection.fields || []"
                :options="fieldOptions"
                multiple
                filterable
                clearable
                placeholder="选择已定义字段"
                @update:value="updateSectionFields"
              />

              <draggable
                :model-value="selectedFieldRows"
                item-key="fieldCode"
                handle=".field-drag-handle"
                ghost-class="field-sort-ghost"
                :animation="160"
                class="selected-field-list"
                @update:model-value="reorderSectionFields"
              >
                <template #item="{ element }">
                  <div class="selected-field-row" :class="{ invalid: element.invalid }">
                    <span class="field-drag-handle" title="拖拽排序">
                      <n-icon><ReorderFourOutline /></n-icon>
                    </span>
                    <div class="field-copy">
                      <strong>{{ element.label }}</strong>
                      <small>{{ element.fieldCode }}</small>
                    </div>
                    <n-select
                      class="field-control-select"
                      size="small"
                      :value="fieldOverrideType(element.fieldCode)"
                      :options="fieldControlOptions(element.fieldCode)"
                      @update:value="updateFieldOverride(element.fieldCode, $event)"
                    />
                    <label v-if="fieldOverrideType(element.fieldCode) === 'pillSelect'" class="compact-switch">
                      <span>允许清空</span>
                      <n-switch
                        size="small"
                        :value="fieldOverrideClearable(element.fieldCode)"
                        @update:value="updateFieldOverrideClearable(element.fieldCode, $event)"
                      />
                    </label>
                    <n-tag v-if="element.invalid" type="warning" size="tiny" :bordered="false">
                      字段失效
                    </n-tag>
                  </div>
                </template>
              </draggable>
            </section>

            <section class="settings-block compact-block">
              <div class="toggle-row">
                <div>
                  <strong>允许折叠</strong>
                  <span>移动端可收起当前分区</span>
                </div>
                <n-switch
                  :value="selectedSection.collapsible === true"
                  @update:value="patchSelectedSection({ collapsible: $event, collapsedByDefault: $event ? selectedSection.collapsedByDefault === true : false })"
                />
              </div>
              <div class="toggle-row" :class="{ disabled: selectedSection.collapsible !== true }">
                <div>
                  <strong>默认收起</strong>
                  <span>首次进入页面时保持折叠</span>
                </div>
                <n-switch
                  :disabled="selectedSection.collapsible !== true"
                  :value="selectedSection.collapsedByDefault === true"
                  @update:value="patchSelectedSection({ collapsedByDefault: $event })"
                />
              </div>
            </section>
          </template>

          <section v-else class="settings-block child-table-settings">
            <div class="settings-grid">
              <n-form-item label="子表关系">
                <n-select
                  :value="selectedSection.relationKey || ''"
                  :options="relationOptions"
                  filterable
                  clearable
                  placeholder="选择关系与级联中的子表"
                  @update:value="patchSelectedSection({ relationKey: $event || '' })"
                />
              </n-form-item>
              <n-form-item label="展示方式">
                <n-select
                  :value="selectedSection.displayMode || 'inline_grid'"
                  :options="displayModeOptions"
                  @update:value="patchSelectedSection({ displayMode: $event })"
                />
              </n-form-item>
            </div>

            <div class="child-interaction-grid">
              <div class="toggle-row">
                <div>
                  <strong>内嵌新增</strong>
                  <span>允许在当前表单中直接添加子表行</span>
                </div>
                <n-switch
                  :value="selectedSection.inlineCreateEnabled !== false"
                  @update:value="patchSelectedSection({ inlineCreateEnabled: $event })"
                />
              </div>
              <div class="toggle-row">
                <div>
                  <strong>内嵌编辑</strong>
                  <span>允许直接修改已加载的子表行</span>
                </div>
                <n-switch
                  :value="selectedSection.inlineEditEnabled !== false"
                  @update:value="patchSelectedSection({ inlineEditEnabled: $event })"
                />
              </div>
            </div>

            <section class="selector-settings">
              <div class="selector-heading">
                <div>
                  <strong>从已有记录选择</strong>
                  <span>开启后可查询候选数据并批量写入当前子表</span>
                </div>
                <n-switch
                  :value="selectedSection.selectorEnabled === true"
                  @update:value="patchSelectedSection({ selectorEnabled: $event })"
                />
              </div>

              <template v-if="selectedSection.selectorEnabled === true">
                <div class="settings-grid selector-basic-grid">
                  <n-form-item label="候选数据">
                    <n-select
                      :value="selectedSection.selectorObjectCode || ''"
                      :options="selectorObjectOptions"
                      filterable
                      tag
                      clearable
                      placeholder="选择或输入候选对象编码"
                      @update:value="patchSelectedSection({ selectorObjectCode: $event || '' })"
                    />
                  </n-form-item>
                  <n-form-item label="按钮文案">
                    <n-input
                      :value="selectedSection.selectorButtonText || ''"
                      placeholder="选择记录"
                      @update:value="patchSelectedSection({ selectorButtonText: $event || '' })"
                    />
                  </n-form-item>
                  <n-form-item label="弹窗标题">
                    <n-input
                      :value="selectedSection.selectorTitle || ''"
                      placeholder="例如：选择商品"
                      @update:value="patchSelectedSection({ selectorTitle: $event || '' })"
                    />
                  </n-form-item>
                </div>

                <div class="selector-field-groups">
                  <n-form-item label="列表展示字段">
                    <n-dynamic-tags
                      :value="selectedSection.selectorDisplayFields || []"
                      @update:value="patchSelectedSection({ selectorDisplayFields: $event })"
                    />
                  </n-form-item>
                  <n-form-item label="关键词搜索字段">
                    <n-dynamic-tags
                      :value="selectedSection.selectorKeywordFields || []"
                      @update:value="patchSelectedSection({ selectorKeywordFields: $event })"
                    />
                  </n-form-item>
                </div>

                <div class="mapping-section">
                  <div class="block-heading">
                    <div>
                      <strong>选中后字段映射</strong>
                      <span>{{ selectedSelectorMappings.length }} 条</span>
                    </div>
                    <n-button size="tiny" secondary @click="addSelectorMapping">
                      <template #icon>
                        <n-icon><AddOutline /></n-icon>
                      </template>
                      添加映射
                    </n-button>
                  </div>
                  <div v-for="(mapping, index) in selectedSelectorMappings" :key="`mapping_${index}`" class="mapping-row">
                    <n-input
                      :value="mapping.sourceField || ''"
                      placeholder="候选字段"
                      @update:value="patchSelectorMapping(index, { sourceField: $event })"
                    />
                    <span class="mapping-arrow">写入</span>
                    <n-input
                      :value="mapping.targetField || ''"
                      placeholder="子表字段"
                      @update:value="patchSelectorMapping(index, { targetField: $event })"
                    />
                    <n-button circle quaternary type="error" title="删除映射" @click="removeSelectorMapping(index)">
                      <template #icon>
                        <n-icon><TrashOutline /></n-icon>
                      </template>
                    </n-button>
                  </div>
                  <n-empty v-if="!selectedSelectorMappings.length" size="small" description="尚未配置字段映射" />
                </div>

                <div class="mapping-section">
                  <div class="block-heading">
                    <div>
                      <strong>候选数据筛选</strong>
                      <span>{{ selectedSelectorFilters.length }} 条</span>
                    </div>
                    <n-button size="tiny" secondary @click="addSelectorFilter">
                      <template #icon>
                        <n-icon><AddOutline /></n-icon>
                      </template>
                      添加筛选
                    </n-button>
                  </div>
                  <div v-for="(filter, index) in selectedSelectorFilters" :key="`filter_${index}`" class="mapping-row">
                    <n-select
                      :value="filter.sourceField || ''"
                      :options="fieldOptions"
                      filterable
                      clearable
                      placeholder="当前表单字段"
                      @update:value="patchSelectorFilter(index, { sourceField: $event || '' })"
                    />
                    <span class="mapping-arrow">作为</span>
                    <n-input
                      :value="filter.targetParam || ''"
                      placeholder="查询参数名"
                      @update:value="patchSelectorFilter(index, { targetParam: $event })"
                    />
                    <n-button circle quaternary type="error" title="删除筛选" @click="removeSelectorFilter(index)">
                      <template #icon>
                        <n-icon><TrashOutline /></n-icon>
                      </template>
                    </n-button>
                  </div>
                  <n-empty v-if="!selectedSelectorFilters.length" size="small" description="不限制候选数据" />
                </div>
              </template>
            </section>
          </section>
        </template>
        <n-empty v-else description="从左侧选择分区，或新增一个分区" />
      </main>
    </div>

    <BottomBarEditor
      :model-value="draft.bottomBar"
      :fields="fields"
      @update:model-value="updateBottomBar"
      @configure-bottom-action="forwardConfigureBottomAction"
    />

    <n-alert v-if="warnings.length" type="warning" :bordered="false" class="protocol-warning">
      <div class="warning-title">
        有 {{ warnings.length }} 项引用需要处理
      </div>
      <ul>
        <li v-for="warning in warnings" :key="warning.key">
          {{ warning.message }}
        </li>
      </ul>
    </n-alert>
  </div>
</template>

<script setup>
import { AddOutline, ReorderFourOutline, TrashOutline } from '@vicons/ionicons5'
import { computed, ref, watch } from 'vue'
import draggable from 'vuedraggable'
import { normalizeBottomBar, normalizePageSections } from '../form-first/formDesignerSchema'
import BottomBarEditor from './BottomBarEditor.vue'
import {
  appendMissingOptions,
  collectPageSectionWarnings,
  createPageSection,
  normalizeRelationOption,
  resolveVisibleModes,
  updateVisibleModes,
} from './pageSectionEditorUtils'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({}),
  },
  fields: {
    type: Array,
    default: () => [],
  },
  relations: {
    type: Array,
    default: () => [],
  },
  actions: {
    type: Array,
    default: () => [],
  },
})
const emit = defineEmits([
  'update:modelValue',
  'dirtyChange',
  'configureBottomAction',
  'editChildTableSection',
  'removeChildTableSection',
])

const modeOptions = [
  { label: '新增', value: 'create' },
  { label: '编辑', value: 'edit' },
  { label: '详情', value: 'detail' },
]
const sectionAddOptions = [
  { label: '内容分区', key: 'card' },
  { label: '子表分区', key: 'child_table' },
]
const displayModeOptions = [
  { label: '行内表格', value: 'inline_grid' },
  { label: '卡片列表', value: 'card_list' },
]

const draft = ref(createEditorDraft(props.modelValue))
const selectedSectionId = ref(draft.value.pageSections[0]?.sectionId || '')
const pageSections = computed(() => draft.value.pageSections)
const selectedSection = computed(() => pageSections.value.find(section => section.sectionId === selectedSectionId.value) || null)
const selectedSelectorMappings = computed(() => Array.isArray(selectedSection.value?.selectorMappings)
  ? selectedSection.value.selectorMappings
  : [])
const selectedSelectorFilters = computed(() => Array.isArray(selectedSection.value?.selectorFilters)
  ? selectedSection.value.selectorFilters
  : [])
const baseFieldOptions = computed(() => props.fields
  .map((field) => {
    const value = String(field?.fieldCode || field?.field || field?.sourceField || '').trim()
    return value
      ? { label: `${field?.fieldName || field?.label || field?.comment || value}（${value}）`, value }
      : null
  })
  .filter(Boolean))
const knownFieldCodes = computed(() => new Set(baseFieldOptions.value.map(option => option.value)))
const fieldOptions = computed(() => appendMissingOptions(
  baseFieldOptions.value,
  pageSections.value.flatMap(section => Array.isArray(section.fields) ? section.fields : []),
  '字段已失效',
))
const selectedFieldRows = computed(() => (selectedSection.value?.fields || []).map((fieldCode) => {
  const option = fieldOptions.value.find(item => item.value === fieldCode)
  return {
    fieldCode,
    label: option?.label?.replace(/（[^（）]+）$/, '') || fieldCode,
    invalid: !knownFieldCodes.value.has(fieldCode),
  }
}))
const relationOptions = computed(() => appendMissingOptions(
  props.relations.map(normalizeRelationOption).filter(Boolean),
  pageSections.value.filter(section => section.sectionType === 'child_table').map(section => section.relationKey),
  '关系已失效',
))
const selectorObjectOptions = computed(() => appendMissingOptions(
  props.relations.map(normalizeSelectorObjectOption).filter(Boolean),
  [selectedSection.value?.selectorObjectCode],
  '已有配置',
))
const warnings = computed(() => collectPageSectionWarnings({
  pageSections: pageSections.value,
  bottomBar: draft.value.bottomBar,
  fields: props.fields,
  relations: props.relations,
  actions: props.actions,
}))
let syncingFromProps = false

watch(
  () => props.modelValue,
  (value) => {
    syncingFromProps = true
    draft.value = createEditorDraft(value)
    selectedSectionId.value = draft.value.pageSections[0]?.sectionId || ''
    Promise.resolve().then(() => {
      syncingFromProps = false
    })
  },
)
watch(
  draft,
  () => {
    if (syncingFromProps)
      return
    emit('update:modelValue', getValue())
    emit('dirtyChange', true)
  },
  { deep: true },
)

function createEditorDraft(value = {}) {
  const pageSections = normalizePageSections(value?.pageSections)
  const bottomBar = normalizeBottomBar(value?.bottomBar)
  return {
    pageSections,
    bottomBar: {
      ...bottomBar,
      actions: (bottomBar.actions || []).map((action, index) => ({
        ...action,
        __editorKey: action.actionId || `existing_action_${index}_${action.type || 'action'}`,
      })),
    },
  }
}

function getValue() {
  return {
    pageSections: normalizePageSections(draft.value.pageSections),
    bottomBar: normalizeBottomBar({
      ...draft.value.bottomBar,
      actions: draft.value.bottomBar.actions.map(({ __editorKey, ...action }) => action),
    }),
  }
}

function replaceSections(sections) {
  draft.value = { ...draft.value, pageSections: sections }
}

function addSection(type) {
  const section = createPageSection(type, pageSections.value)
  draft.value = { ...draft.value, pageSections: [...pageSections.value, section] }
  selectedSectionId.value = section.sectionId
}

function patchSelectedSection(patch = {}) {
  const sectionId = selectedSectionId.value
  draft.value = {
    ...draft.value,
    pageSections: pageSections.value.map(section => section.sectionId === sectionId ? { ...section, ...patch } : section),
  }
}

function removeSelectedSection() {
  const currentIndex = pageSections.value.findIndex(section => section.sectionId === selectedSectionId.value)
  const nextSections = pageSections.value.filter(section => section.sectionId !== selectedSectionId.value)
  draft.value = { ...draft.value, pageSections: nextSections }
  selectedSectionId.value = nextSections[Math.min(Math.max(currentIndex, 0), nextSections.length - 1)]?.sectionId || ''
}

function handleRemoveSelectedSection() {
  if (selectedSection.value?.sectionType === 'child_table') {
    emit('removeChildTableSection', { section: { ...selectedSection.value } })
    return
  }
  removeSelectedSection()
}

function updateSectionFields(fieldCodes = []) {
  const selected = new Set(fieldCodes)
  const overrides = Object.fromEntries(Object.entries(selectedSection.value?.fieldOverrides || {}).filter(([fieldCode]) => selected.has(fieldCode)))
  patchSelectedSection({ fields: fieldCodes, fieldOverrides: overrides })
}

function reorderSectionFields(rows = []) {
  patchSelectedSection({ fields: rows.map(row => row.fieldCode) })
}

function fieldOverrideType(fieldCode) {
  return selectedSection.value?.fieldOverrides?.[fieldCode]?.componentKey || ''
}

function fieldControlOptions(fieldCode) {
  const options = [
    { label: '跟随字段', value: '' },
    { label: '横向选项', value: 'pillSelect' },
  ]
  const current = fieldOverrideType(fieldCode)
  return current && !options.some(item => item.value === current)
    ? [...options, { label: `${current}（已有配置）`, value: current }]
    : options
}

function updateFieldOverride(fieldCode, componentKey) {
  const overrides = { ...(selectedSection.value?.fieldOverrides || {}) }
  if (!componentKey) {
    delete overrides[fieldCode]
  }
  else {
    overrides[fieldCode] = {
      ...(overrides[fieldCode] || {}),
      componentKey,
      ...(componentKey === 'pillSelect'
        ? { props: { ...(overrides[fieldCode]?.props || {}), clearable: overrides[fieldCode]?.props?.clearable === true } }
        : {}),
    }
  }
  patchSelectedSection({ fieldOverrides: overrides })
}

function fieldOverrideClearable(fieldCode) {
  return selectedSection.value?.fieldOverrides?.[fieldCode]?.props?.clearable === true
}

function updateFieldOverrideClearable(fieldCode, clearable) {
  const overrides = { ...(selectedSection.value?.fieldOverrides || {}) }
  overrides[fieldCode] = {
    ...(overrides[fieldCode] || {}),
    componentKey: 'pillSelect',
    props: { ...(overrides[fieldCode]?.props || {}), clearable },
  }
  patchSelectedSection({ fieldOverrides: overrides })
}

function addSelectorMapping() {
  patchSelectedSection({
    selectorMappings: [...selectedSelectorMappings.value, { sourceField: '', targetField: '' }],
  })
}

function patchSelectorMapping(index, patch = {}) {
  patchSelectedSection({
    selectorMappings: selectedSelectorMappings.value.map((mapping, mappingIndex) => mappingIndex === index
      ? { ...mapping, ...patch }
      : mapping),
  })
}

function removeSelectorMapping(index) {
  patchSelectedSection({
    selectorMappings: selectedSelectorMappings.value.filter((_, mappingIndex) => mappingIndex !== index),
  })
}

function addSelectorFilter() {
  patchSelectedSection({
    selectorFilters: [...selectedSelectorFilters.value, { sourceField: '', targetParam: '' }],
  })
}

function patchSelectorFilter(index, patch = {}) {
  patchSelectedSection({
    selectorFilters: selectedSelectorFilters.value.map((filter, filterIndex) => filterIndex === index
      ? { ...filter, ...patch }
      : filter),
  })
}

function removeSelectorFilter(index) {
  patchSelectedSection({
    selectorFilters: selectedSelectorFilters.value.filter((_, filterIndex) => filterIndex !== index),
  })
}

function updateBottomBar(bottomBar) {
  draft.value = { ...draft.value, bottomBar: normalizeBottomBar(bottomBar || {}) }
}

function forwardConfigureBottomAction(payload) {
  emit('configureBottomAction', payload)
}

function visibleModes(item = {}) {
  return resolveVisibleModes(item)
}

function isOnlyVisibleMode(item = {}, mode = '') {
  const modes = visibleModes(item)
  return modes.length === 1 && modes[0] === mode
}

function sectionTypeLabel(type) {
  return type === 'child_table' ? '子表分区' : '内容分区'
}

function sectionWarningCount(section) {
  const prefix = `section:${pageSections.value.indexOf(section)}:`
  return warnings.value.filter(warning => warning.key.startsWith(prefix)).length
}

function normalizeSelectorObjectOption(relation = {}) {
  const value = String(relation.targetObjectCode || relation.modelCode || '').trim()
  if (!value)
    return null
  const label = relation.targetObjectName || relation.modelName || relation.relationName || value
  return { label: `${label}（${value}）`, value }
}

defineExpose({ getValue })
</script>

<style scoped>
.page-section-editor {
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 18px;
  color: var(--text-primary);
}

.section-workbench {
  display: grid;
  min-height: 420px;
  grid-template-columns: minmax(250px, 300px) minmax(0, 1fr);
  border: 1px solid var(--border-light);
  border-radius: 6px;
  overflow: hidden;
}

.section-catalog {
  min-width: 0;
  border-right: 1px solid var(--border-light);
  background: var(--bg-secondary);
}

.workbench-heading,
.settings-heading,
.block-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.workbench-heading {
  min-height: 48px;
  padding: 0 14px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-primary);
}

.workbench-heading > div,
.block-heading > div {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.workbench-heading strong,
.settings-heading strong,
.block-heading strong {
  font-size: 14px;
  font-weight: 600;
}

.workbench-heading span,
.block-heading span {
  color: var(--text-tertiary);
  font-size: 12px;
}

.section-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px;
}

.section-list-item {
  display: grid;
  width: 100%;
  min-height: 54px;
  grid-template-columns: 22px 22px minmax(0, 1fr) auto;
  align-items: center;
  gap: 6px;
  padding: 7px 8px;
  border: 1px solid transparent;
  border-radius: 5px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.section-list-item:hover {
  background: var(--bg-primary);
}

.section-list-item.active {
  border-color: #8ab5ff;
  background: #eaf2ff;
}

.section-drag-handle,
.field-drag-handle {
  display: inline-grid;
  color: var(--text-tertiary);
  cursor: grab;
  place-items: center;
}

.section-index {
  display: grid;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  background: #e5e6eb;
  color: var(--text-secondary);
  font-size: 11px;
  place-items: center;
}

.section-list-item.active .section-index {
  background: #3370ff;
  color: #fff;
}

.section-list-copy,
.field-copy {
  min-width: 0;
}

.section-list-copy strong,
.section-list-copy small,
.field-copy strong,
.field-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-list-copy strong,
.field-copy strong {
  font-size: 13px;
  font-weight: 500;
}

.section-list-copy small,
.field-copy small {
  margin-top: 2px;
  color: var(--text-tertiary);
  font-size: 11px;
}

.section-empty {
  padding: 52px 12px;
}

.section-settings {
  min-width: 0;
  padding: 0 20px 18px;
  overflow: auto;
  background: var(--bg-primary);
}

.settings-heading {
  min-height: 48px;
  margin: 0 -20px;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-primary);
}

.section-settings > .n-empty {
  padding-top: 48px;
}

.settings-heading > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.settings-grid {
  padding-top: 18px;
}

.settings-block {
  margin-top: 4px;
  padding: 16px 0;
  border-top: 1px solid #f0f1f2;
}

.block-heading {
  margin-bottom: 12px;
}

.selected-field-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 12px;
}

.selected-field-row {
  display: grid;
  min-height: 52px;
  grid-template-columns: 24px minmax(130px, 1fr) minmax(130px, 180px) auto auto;
  align-items: center;
  gap: 10px;
  padding: 7px 10px;
  border: 1px solid var(--border-light);
  border-radius: 5px;
  background: var(--bg-primary);
}

.selected-field-row.invalid {
  border-color: #f0c97d;
  background: #fffaf0;
}

.compact-switch {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 12px;
}

.compact-block {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.toggle-row {
  display: flex;
  min-height: 58px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: 5px;
}

.toggle-row strong,
.toggle-row span {
  display: block;
}

.toggle-row strong {
  font-size: 13px;
}

.toggle-row span {
  margin-top: 2px;
  color: var(--text-tertiary);
  font-size: 11px;
}

.toggle-row.disabled {
  opacity: 0.55;
}

.child-table-settings {
  display: grid;
  gap: 14px;
}

.child-interaction-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.selector-settings {
  padding-top: 16px;
  border-top: 1px solid #f0f1f2;
}

.selector-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.selector-heading strong,
.selector-heading span {
  display: block;
}

.selector-heading strong {
  font-size: 13px;
}

.selector-heading span {
  margin-top: 3px;
  color: var(--text-tertiary);
  font-size: 11px;
}

.selector-basic-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  padding-top: 14px;
}

.selector-field-groups {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.mapping-section {
  padding: 14px 0 2px;
  border-top: 1px solid #f0f1f2;
}

.mapping-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) 44px minmax(120px, 1fr) 34px;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.mapping-arrow {
  color: var(--text-tertiary);
  font-size: 11px;
  text-align: center;
}

.protocol-warning {
  flex: 0 0 auto;
}

.warning-title {
  font-weight: 600;
}

.protocol-warning ul {
  margin: 6px 0 0;
  padding-left: 18px;
}

.section-sort-ghost,
.field-sort-ghost {
  border: 1px dashed #3370ff;
  background: #eaf2ff;
  opacity: 0.55;
}

.section-sort-chosen {
  box-shadow: 0 6px 16px rgba(31, 35, 41, 0.12);
}

:deep(.n-form-item) {
  min-width: 0;
}

@media (max-width: 900px) {
  .section-workbench {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .selected-field-row {
    grid-template-columns: 24px minmax(100px, 1fr) minmax(120px, 160px);
  }

  .compact-switch,
  .selected-field-row > .n-tag {
    grid-column: 2 / -1;
  }

  .child-interaction-grid,
  .selector-basic-grid,
  .selector-field-groups {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
