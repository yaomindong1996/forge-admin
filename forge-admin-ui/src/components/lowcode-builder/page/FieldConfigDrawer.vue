<script setup>
/**
 * 列表页设计器 · 字段配置抽屉
 *
 * 从 ListPageGridDesigner 拆出的表单级面板：负责「已选字段」排序/删除、
 * 逐字段设置（渲染方式/对齐/点击动作等）与「可选字段」添加。
 *
 * 通信约定：
 * - 选中区块经 listDesigner store（Pinia）读取；
 * - 布局写入不由本组件直接操作 store.layout，统一 emit patch-props /
 *   patch-block 回传宿主，保持单一数据写入点。
 */
import { computed, ref, watch } from 'vue'
import draggable from 'vuedraggable'
import { useListDesignerStore } from '@/store'
import {
  alignOptions,
  buildCrudFieldListPatch,
  columnClickActionOptions,
  fixedColumnOptions,
  isNameRenderType,
  normalizeParamName,
  queryTypeOptions,
  resolveCrudFieldQuickValue,
  resolveDefaultSearchComponentType,
  resolveDefaultTableRenderType,
  resolveSelectedFieldRefs,
  searchComponentOptions,
  tableRenderOptions,
} from './fieldDrawerConfig'
import { isPageFieldVisible } from './page-schema'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  /** 抽屉模式：table = 列表字段，search = 查询条件 */
  mode: {
    type: String,
    default: 'table',
  },
  fields: {
    type: Array,
    default: () => [],
  },
  /** 打开时定位到的字段（inline 入口传入） */
  initialField: {
    type: String,
    default: '',
  },
  /** 宿主解析的区块元信息标题，用于抽屉标题 */
  blockMetaTitle: {
    type: String,
    default: '',
  },
  pageTargetOptions: {
    type: Array,
    default: () => [],
  },
  formTargetOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:show', 'patchProps', 'patchBlock'])

const designerStore = useListDesignerStore()

const selectedBlock = computed(() => designerStore.selectedBlock)

const activeDrawerFieldName = ref('')
const fieldAdvancedOpen = ref(false)

const zoneKey = computed(() => {
  if (props.mode === 'search')
    return 'search'
  return selectedBlock.value?.blockType === 'search-form' ? 'search' : 'table'
})
const drawerTitle = computed(() => zoneKey.value === 'search' ? '查询条件' : '字段')
const fieldMap = computed(() => new Map(props.fields.map(f => [f.field, f])))
const queryFieldOptions = computed(() => {
  const options = props.fields
    .filter(field => !field.systemField || field.field === 'id')
    .map(field => ({
      label: field.label ? `${field.label}（${field.sourceField || field.field}）` : (field.sourceField || field.field),
      value: field.field,
    }))
  if (!options.some(item => item.value === 'id'))
    options.unshift({ label: 'ID（id）', value: 'id' })
  return options
})
const selectedFieldRefs = computed(() => resolveSelectedFieldRefs(selectedBlock.value, zoneKey.value, props.fields))
const selectedFieldsList = computed(() => selectedFieldRefs.value
  .map(ref => fieldMap.value.get(ref))
  .filter(Boolean))
const availableFields = computed(() => {
  const set = new Set(selectedFieldRefs.value)
  return props.fields.filter(f => isPageFieldVisible(f, zoneKey.value) && !set.has(f.field))
})
const activeDrawerField = computed(() => {
  if (!selectedFieldsList.value.length)
    return null
  return selectedFieldsList.value.find(field => field.field === activeDrawerFieldName.value) || selectedFieldsList.value[0]
})
const activeDrawerFieldSetting = computed(() => activeDrawerField.value ? resolveFieldSetting(activeDrawerField.value.field) : {})

watch(selectedFieldRefs, (refs) => {
  if (!refs.length) {
    activeDrawerFieldName.value = ''
    return
  }
  if (!refs.includes(activeDrawerFieldName.value))
    activeDrawerFieldName.value = refs[0]
})

watch(() => props.show, (show) => {
  if (!show)
    return
  const refs = resolveSelectedFieldRefs(selectedBlock.value, zoneKey.value, props.fields)
  activeDrawerFieldName.value = props.initialField || refs?.[0] || ''
  fieldAdvancedOpen.value = false
})

function selectDrawerField(fieldName = '') {
  activeDrawerFieldName.value = fieldName
  fieldAdvancedOpen.value = false
}

function updateSelectedFieldRefs(fieldRefs = []) {
  if (!selectedBlock.value)
    return
  if (zoneKey.value === 'search' && selectedBlock.value.blockType === 'AiCrudPage') {
    emit('patchProps', { blockId: selectedBlock.value.id, patch: { searchFieldRefs: fieldRefs } })
    return
  }
  emit('patchBlock', { blockId: selectedBlock.value.id, patch: { fieldRefs } })
}

function toggleField(fieldName, add) {
  if (!selectedBlock.value)
    return
  const current = selectedFieldRefs.value || []
  const next = add
    ? [...current, fieldName]
    : current.filter(f => f !== fieldName)
  updateSelectedFieldRefs(Array.from(new Set(next)))
}

function handleSelectedReorder(rows) {
  if (!selectedBlock.value)
    return
  updateSelectedFieldRefs(rows.map(r => r.field))
}

function resolveFieldSetting(fieldName) {
  if (zoneKey.value === 'search' && selectedBlock.value?.blockType === 'AiCrudPage')
    return selectedBlock.value?.props?.searchFieldSettings?.[fieldName] || {}
  return selectedBlock.value?.props?.fieldSettings?.[fieldName] || {}
}

function updateFieldSetting(fieldName, settingPatch) {
  if (!selectedBlock.value)
    return
  if (zoneKey.value === 'search' && selectedBlock.value.blockType === 'AiCrudPage') {
    emit('patchProps', {
      blockId: selectedBlock.value.id,
      patch: {
        searchFieldSettings: {
          ...(selectedBlock.value.props?.searchFieldSettings || {}),
          [fieldName]: {
            ...(selectedBlock.value.props?.searchFieldSettings?.[fieldName] || {}),
            ...settingPatch,
          },
        },
      },
    })
    return
  }
  emit('patchProps', {
    blockId: selectedBlock.value.id,
    patch: {
      fieldSettings: {
        ...(selectedBlock.value.props?.fieldSettings || {}),
        [fieldName]: {
          ...(selectedBlock.value.props?.fieldSettings?.[fieldName] || {}),
          ...settingPatch,
        },
      },
    },
  })
}

function resolveFieldRoleEnabled(fieldName, role) {
  if (!fieldName)
    return false
  if (role === 'search')
    return resolveSelectedFieldRefs(selectedBlock.value, 'search', props.fields).includes(fieldName)
  if (role === 'table')
    return resolveSelectedFieldRefs(selectedBlock.value, 'table', props.fields).includes(fieldName)
  if (role === 'import')
    return resolveCrudFieldQuickValue(selectedBlock.value, fieldName, 'importable', true)
  if (role === 'export')
    return resolveCrudFieldQuickValue(selectedBlock.value, fieldName, 'exportable', true)
  return resolveFieldSetting(fieldName).editVisible !== false
}

function updateFieldRole(fieldName, role, enabled) {
  if (!selectedBlock.value || !fieldName)
    return
  if (role === 'import' || role === 'export') {
    updateCrudFieldQuickSetting(fieldName, role === 'import' ? 'importable' : 'exportable', enabled)
    return
  }
  if (role === 'search' || role === 'table') {
    const current = resolveSelectedFieldRefs(selectedBlock.value, role, props.fields)
    const next = enabled
      ? Array.from(new Set([...current, fieldName]))
      : current.filter(ref => ref !== fieldName)
    if (role === 'search' && selectedBlock.value.blockType === 'AiCrudPage') {
      emit('patchProps', { blockId: selectedBlock.value.id, patch: { searchFieldRefs: next } })
      return
    }
    if (role === 'table')
      emit('patchBlock', { blockId: selectedBlock.value.id, patch: { fieldRefs: next } })
    return
  }
  updateFieldSetting(fieldName, { editVisible: enabled })
}

function updateCrudFieldQuickSetting(fieldKey = '', settingKey = '', value = false) {
  if (!selectedBlock.value?.id || !fieldKey || !settingKey)
    return
  const blockProps = selectedBlock.value.props || {}
  const fieldSettings = { ...(blockProps.fieldSettings || {}) }
  const previous = { ...(fieldSettings[fieldKey] || {}) }
  fieldSettings[fieldKey] = {
    ...previous,
    field: fieldKey,
    [settingKey]: value === true,
  }
  if (settingKey === 'searchable')
    fieldSettings[fieldKey].showInSearch = value === true

  emit('patchProps', {
    blockId: selectedBlock.value.id,
    patch: {
      fieldSettings,
      ...buildCrudFieldListPatch(blockProps, fieldKey, settingKey, value === true),
    },
  })
}

function renderTargetFieldOptions(field = {}) {
  const options = queryFieldOptions.value.map(item => ({ ...item }))
  const defaultTarget = `${field.field}Name`
  if (!options.some(item => item.value === defaultTarget)) {
    options.unshift({
      label: `${defaultTarget}（默认翻译字段）`,
      value: defaultTarget,
    })
  }
  return options
}
</script>

<template>
  <n-drawer :show="show" :width="680" placement="right" @update:show="emit('update:show', $event)">
    <n-drawer-content :title="`配置${drawerTitle} · ${blockMetaTitle || ''}`" closable>
      <div v-if="selectedBlock" class="field-config">
        <div class="field-config-section">
          <div class="section-title">
            已选{{ drawerTitle }} ({{ selectedFieldRefs.length || 0 }})
          </div>
          <draggable
            :model-value="selectedFieldsList"
            item-key="field"
            handle=".f-handle"
            :animation="160"
            class="selected-list"
            @update:model-value="handleSelectedReorder"
          >
            <template #item="{ element }">
              <div
                class="selected-row"
                :class="{
                  search: zoneKey === 'search',
                  table: zoneKey !== 'search',
                  active: activeDrawerField?.field === element.field,
                }"
                @click="selectDrawerField(element.field)"
              >
                <span class="f-handle">☰</span>
                <span class="f-name">
                  {{ element.label || element.field }}
                  <small v-if="element.sourceLabel || element.modelName">{{ element.sourceLabel || element.modelName }}</small>
                </span>
                <span class="f-code">{{ element.field }}</span>
                <button type="button" class="f-remove" title="移除字段" @click.stop="toggleField(element.field, false)">
                  ×
                </button>
                <div v-if="zoneKey === 'search'" class="field-setting-row search-setting-row">
                  <n-select
                    :value="resolveFieldSetting(element.field).queryType || element.queryType || 'like'"
                    size="tiny"
                    :options="queryTypeOptions"
                    placeholder="查询方式"
                    @update:value="updateFieldSetting(element.field, { queryType: $event })"
                  />
                  <n-select
                    :value="resolveFieldSetting(element.field).componentType || resolveDefaultSearchComponentType(element)"
                    size="tiny"
                    :options="searchComponentOptions"
                    placeholder="查询组件"
                    @update:value="updateFieldSetting(element.field, { componentType: $event })"
                  />
                  <n-select
                    :value="resolveFieldSetting(element.field).queryField || element.field"
                    size="tiny"
                    :options="queryFieldOptions"
                    filterable
                    placeholder="映射字段"
                    @update:value="updateFieldSetting(element.field, { queryField: $event })"
                  />
                  <n-select
                    :value="resolveFieldSetting(element.field).align || 'left'"
                    size="tiny"
                    :options="alignOptions"
                    placeholder="对齐"
                    @update:value="updateFieldSetting(element.field, { align: $event || 'left' })"
                  />
                </div>
                <div v-if="zoneKey !== 'search' && ['data-table', 'AiCrudPage', 'AiTable', 'AiForm', 'detail-info'].includes(selectedBlock.blockType)" class="field-setting-row table-setting-row">
                  <n-select
                    :value="resolveFieldSetting(element.field).renderType || resolveDefaultTableRenderType(element)"
                    size="tiny"
                    :options="tableRenderOptions"
                    placeholder="渲染方式"
                    @update:value="updateFieldSetting(element.field, { renderType: $event })"
                  />
                  <n-select
                    :value="resolveFieldSetting(element.field).align || 'left'"
                    size="tiny"
                    :options="alignOptions"
                    placeholder="对齐"
                    @update:value="updateFieldSetting(element.field, { align: $event || 'left' })"
                  />
                  <n-select
                    v-if="isNameRenderType(resolveFieldSetting(element.field).renderType || resolveDefaultTableRenderType(element))"
                    :value="resolveFieldSetting(element.field).targetField || `${element.field}Name`"
                    size="tiny"
                    :options="renderTargetFieldOptions(element)"
                    filterable
                    tag
                    placeholder="名称字段"
                    @update:value="updateFieldSetting(element.field, { targetField: $event })"
                  />
                </div>
                <div v-if="zoneKey !== 'search' && ['data-table', 'AiCrudPage', 'AiTable'].includes(selectedBlock.blockType)" class="field-setting-row column-link-row">
                  <label class="field-setting-control">
                    <span>文字颜色</span>
                    <n-color-picker
                      :value="resolveFieldSetting(element.field).textColor || ''"
                      size="small"
                      :show-alpha="true"
                      placeholder="文字颜色"
                      @update:value="updateFieldSetting(element.field, { textColor: $event || '' })"
                    />
                  </label>
                  <label class="field-setting-control">
                    <span>点击动作</span>
                    <n-select
                      :value="resolveFieldSetting(element.field).clickAction || 'none'"
                      size="tiny"
                      :options="columnClickActionOptions"
                      placeholder="点击动作"
                      @update:value="updateFieldSetting(element.field, { clickAction: $event || 'none' })"
                    />
                  </label>
                  <label v-if="resolveFieldSetting(element.field).clickAction === 'navigate'" class="field-setting-control">
                    <span>目标页面</span>
                    <n-select
                      :value="resolveFieldSetting(element.field).targetPageKey || ''"
                      size="tiny"
                      :options="pageTargetOptions"
                      filterable
                      placeholder="目标页面"
                      @update:value="updateFieldSetting(element.field, { targetPageKey: $event || '' })"
                    />
                  </label>
                  <label v-if="resolveFieldSetting(element.field).clickAction === 'navigate' && formTargetOptions.length" class="field-setting-control">
                    <span>目标表单</span>
                    <n-select
                      :value="resolveFieldSetting(element.field).targetFormKey || ''"
                      size="tiny"
                      :options="formTargetOptions"
                      clearable
                      filterable
                      placeholder="默认表单"
                      @update:value="updateFieldSetting(element.field, { targetFormKey: $event || '' })"
                    />
                  </label>
                  <label v-if="resolveFieldSetting(element.field).clickAction === 'navigate'" class="field-setting-control">
                    <span>参数名</span>
                    <n-input
                      :value="resolveFieldSetting(element.field).targetParamName || 'id'"
                      size="tiny"
                      placeholder="参数名"
                      @update:value="updateFieldSetting(element.field, { targetParamName: normalizeParamName($event) || 'id' })"
                    />
                  </label>
                  <label v-if="resolveFieldSetting(element.field).clickAction === 'navigate'" class="field-setting-control">
                    <span>取值字段</span>
                    <n-select
                      :value="resolveFieldSetting(element.field).targetParamField || 'id'"
                      size="tiny"
                      :options="queryFieldOptions"
                      filterable
                      placeholder="取值字段"
                      @update:value="updateFieldSetting(element.field, { targetParamField: $event || 'id' })"
                    />
                  </label>
                  <div v-if="resolveFieldSetting(element.field).clickAction === 'navigate'" class="field-help">
                    点击当前列后跳到目标页面，默认传参：id = 当前行 id。参数名是目标页面接收的名字，取值字段是从当前行取哪个字段。
                  </div>
                </div>
              </div>
            </template>
          </draggable>
          <div v-if="!selectedFieldRefs.length" class="empty">
            当前没有选择字段
          </div>
          <div v-if="activeDrawerField" class="field-detail-card">
            <div class="field-detail-head">
              <div class="field-detail-title">
                <strong>{{ activeDrawerField.label || activeDrawerField.field }}</strong>
                <span>{{ activeDrawerField.sourceField || activeDrawerField.field }}</span>
              </div>
              <div class="field-role-switches">
                <label>
                  <span>查询</span>
                  <n-switch
                    size="small"
                    :value="resolveFieldRoleEnabled(activeDrawerField.field, 'search')"
                    :disabled="selectedBlock.blockType !== 'AiCrudPage'"
                    @update:value="updateFieldRole(activeDrawerField.field, 'search', $event)"
                  />
                </label>
                <label>
                  <span>表格列</span>
                  <n-switch
                    size="small"
                    :value="resolveFieldRoleEnabled(activeDrawerField.field, 'table')"
                    @update:value="updateFieldRole(activeDrawerField.field, 'table', $event)"
                  />
                </label>
                <label>
                  <span>编辑</span>
                  <n-switch
                    size="small"
                    :value="resolveFieldRoleEnabled(activeDrawerField.field, 'edit')"
                    @update:value="updateFieldRole(activeDrawerField.field, 'edit', $event)"
                  />
                </label>
                <label v-if="selectedBlock.blockType === 'AiCrudPage'">
                  <span>导入</span>
                  <n-switch
                    size="small"
                    :value="resolveFieldRoleEnabled(activeDrawerField.field, 'import')"
                    @update:value="updateFieldRole(activeDrawerField.field, 'import', $event)"
                  />
                </label>
                <label v-if="selectedBlock.blockType === 'AiCrudPage'">
                  <span>导出</span>
                  <n-switch
                    size="small"
                    :value="resolveFieldRoleEnabled(activeDrawerField.field, 'export')"
                    @update:value="updateFieldRole(activeDrawerField.field, 'export', $event)"
                  />
                </label>
              </div>
            </div>
            <div class="field-detail-grid">
              <label class="field-detail-control">
                <span>列标题</span>
                <n-input
                  :value="activeDrawerFieldSetting.title || activeDrawerField.label || activeDrawerField.field"
                  size="small"
                  @update:value="updateFieldSetting(activeDrawerField.field, { title: $event || '' })"
                />
              </label>
              <label class="field-detail-control">
                <span>列宽</span>
                <n-input
                  :value="activeDrawerFieldSetting.width || ''"
                  size="small"
                  placeholder="auto / px"
                  @update:value="updateFieldSetting(activeDrawerField.field, { width: $event || '' })"
                />
              </label>
              <label class="field-detail-control">
                <span>对齐</span>
                <n-select
                  :value="activeDrawerFieldSetting.align || 'left'"
                  size="small"
                  :options="alignOptions"
                  @update:value="updateFieldSetting(activeDrawerField.field, { align: $event || 'left' })"
                />
              </label>
              <label class="field-detail-control">
                <span>固定</span>
                <n-select
                  :value="activeDrawerFieldSetting.fixed || ''"
                  size="small"
                  :options="fixedColumnOptions"
                  @update:value="updateFieldSetting(activeDrawerField.field, { fixed: $event || '' })"
                />
              </label>
            </div>
            <div class="field-detail-footer">
              <div class="field-detail-toggles">
                <label>
                  <span>省略</span>
                  <n-switch
                    size="small"
                    :value="activeDrawerFieldSetting.ellipsis !== false"
                    @update:value="updateFieldSetting(activeDrawerField.field, { ellipsis: $event })"
                  />
                </label>
                <label>
                  <span>排序</span>
                  <n-switch
                    size="small"
                    :value="!!activeDrawerFieldSetting.sortable"
                    @update:value="updateFieldSetting(activeDrawerField.field, { sortable: $event })"
                  />
                </label>
              </div>
              <n-button size="tiny" secondary @click="fieldAdvancedOpen = !fieldAdvancedOpen">
                {{ fieldAdvancedOpen ? '收起配置' : '更多字段配置' }}
              </n-button>
            </div>
            <div v-if="fieldAdvancedOpen" class="field-advanced-panel">
              <template v-if="zoneKey === 'search'">
                <label class="field-detail-control">
                  <span>查询方式</span>
                  <n-select
                    :value="activeDrawerFieldSetting.queryType || activeDrawerField.queryType || 'like'"
                    size="small"
                    :options="queryTypeOptions"
                    @update:value="updateFieldSetting(activeDrawerField.field, { queryType: $event })"
                  />
                </label>
                <label class="field-detail-control">
                  <span>查询组件</span>
                  <n-select
                    :value="activeDrawerFieldSetting.componentType || resolveDefaultSearchComponentType(activeDrawerField)"
                    size="small"
                    :options="searchComponentOptions"
                    @update:value="updateFieldSetting(activeDrawerField.field, { componentType: $event })"
                  />
                </label>
                <label class="field-detail-control">
                  <span>映射字段</span>
                  <n-select
                    :value="activeDrawerFieldSetting.queryField || activeDrawerField.field"
                    size="small"
                    :options="queryFieldOptions"
                    filterable
                    @update:value="updateFieldSetting(activeDrawerField.field, { queryField: $event })"
                  />
                </label>
              </template>
              <template v-else>
                <label class="field-detail-control">
                  <span>渲染方式</span>
                  <n-select
                    :value="activeDrawerFieldSetting.renderType || resolveDefaultTableRenderType(activeDrawerField)"
                    size="small"
                    :options="tableRenderOptions"
                    @update:value="updateFieldSetting(activeDrawerField.field, { renderType: $event })"
                  />
                </label>
                <label v-if="isNameRenderType(activeDrawerFieldSetting.renderType || resolveDefaultTableRenderType(activeDrawerField))" class="field-detail-control">
                  <span>名称字段</span>
                  <n-select
                    :value="activeDrawerFieldSetting.targetField || `${activeDrawerField.field}Name`"
                    size="small"
                    :options="renderTargetFieldOptions(activeDrawerField)"
                    filterable
                    tag
                    @update:value="updateFieldSetting(activeDrawerField.field, { targetField: $event })"
                  />
                </label>
                <label class="field-detail-control">
                  <span>点击动作</span>
                  <n-select
                    :value="activeDrawerFieldSetting.clickAction || 'none'"
                    size="small"
                    :options="columnClickActionOptions"
                    @update:value="updateFieldSetting(activeDrawerField.field, { clickAction: $event || 'none' })"
                  />
                </label>
                <label class="field-detail-control">
                  <span>文字颜色</span>
                  <n-color-picker
                    :value="activeDrawerFieldSetting.textColor || ''"
                    size="small"
                    :show-alpha="true"
                    @update:value="updateFieldSetting(activeDrawerField.field, { textColor: $event || '' })"
                  />
                </label>
              </template>
            </div>
          </div>
        </div>
        <div class="field-config-section">
          <div class="section-title">
            可选字段
          </div>
          <div class="available-list">
            <button
              v-for="field in availableFields"
              :key="field.field"
              type="button"
              class="available-item"
              @click="toggleField(field.field, true)"
            >
              <span>{{ field.label || field.field }}</span>
              <small v-if="field.sourceLabel || field.modelName">{{ field.sourceLabel || field.modelName }}</small>
            </button>
            <span v-if="!availableFields.length" class="empty">所有字段已选择</span>
          </div>
        </div>
      </div>
    </n-drawer-content>
  </n-drawer>
</template>

<style scoped>
.field-config {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.field-config-section .section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 8px;
}

.selected-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 146px;
  overflow: auto;
  padding-right: 4px;
}

.selected-row {
  display: inline-grid;
  grid-template-columns: 12px minmax(0, auto) auto;
  align-items: center;
  gap: 5px;
  min-width: 0;
  max-width: 210px;
  padding: 5px 5px 5px 8px;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fff;
  color: #3f3f46;
  font-size: 11px;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    background-color 160ms ease;
}

.selected-row.search,
.selected-row.table {
  grid-template-columns: 12px minmax(0, auto) auto;
}

.selected-row:hover {
  border-color: #a5b4fc;
}

.selected-row.active {
  border-color: #6366f1;
  background: #eef2ff;
  color: #4338ca;
  box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.18);
}

.field-setting-row {
  display: none;
  grid-column: 1 / -1;
  gap: 8px;
  min-width: 0;
  margin-left: 26px;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
}

.search-setting-row {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.table-setting-row {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.column-link-row {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-detail-card {
  position: relative;
  display: grid;
  gap: 12px;
  margin-top: 12px;
  padding: 12px;
  border: 1px solid rgba(228, 228, 231, 0.8);
  border-radius: 8px;
  background: rgba(250, 250, 250, 0.82);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
}

.field-detail-card::before {
  content: '';
  position: absolute;
  top: -6px;
  left: 18px;
  width: 10px;
  height: 10px;
  transform: rotate(45deg);
  border-top: 1px solid rgba(228, 228, 231, 0.8);
  border-left: 1px solid rgba(228, 228, 231, 0.8);
  background: rgba(250, 250, 250, 0.82);
}

.field-detail-head {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(228, 228, 231, 0.72);
}

.field-detail-title {
  display: flex;
  align-items: baseline;
  gap: 7px;
  min-width: 0;
}

.field-detail-title strong {
  min-width: 0;
  overflow: hidden;
  color: #18181b;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-detail-title span {
  min-width: 0;
  overflow: hidden;
  color: #a1a1aa;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-role-switches,
.field-detail-toggles {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.field-role-switches label,
.field-detail-toggles label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #52525b;
  font-size: 11px;
  cursor: pointer;
}

.field-detail-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
}

.field-detail-control {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.field-detail-control > span {
  color: #52525b;
  font-size: 10px;
  font-weight: 600;
}

.field-detail-footer {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(228, 228, 231, 0.72);
}

.field-advanced-panel {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
  padding-top: 10px;
  border-top: 1px dashed rgba(212, 212, 216, 0.9);
}

.field-help {
  grid-column: 1 / -1;
  padding: 7px 9px;
  border: 1px dashed #d4d4d8;
  border-radius: 6px;
  background: #fff;
  color: #71717a;
  font-size: 11px;
  line-height: 1.55;
}

.field-setting-control {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.field-setting-control > span {
  color: #475569;
  font-size: 11px;
  font-weight: 700;
  line-height: 16px;
}

.field-setting-control :deep(.n-select),
.field-setting-control :deep(.n-input),
.field-setting-control :deep(.n-color-picker) {
  width: 100%;
  min-width: 0;
}

.field-setting-row :deep(.n-select),
.field-setting-row :deep(.n-input),
.field-setting-row :deep(.n-input-number),
.field-setting-row :deep(.n-color-picker) {
  width: 100%;
  min-width: 0;
}

.f-handle {
  color: #d4d4d8;
  font-size: 11px;
  line-height: 1;
  cursor: grab;
}

.f-name {
  display: block;
  min-width: 0;
  overflow: hidden;
  color: inherit;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.f-name small {
  display: none;
}

.available-item small {
  color: #64748b;
  font-size: 11px;
  font-weight: 400;
}

.f-code {
  display: none;
}

.f-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #d4d4d8;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  transition:
    opacity 160ms ease,
    color 160ms ease,
    background-color 160ms ease;
}

.selected-row:hover .f-remove {
  opacity: 1;
}

.f-remove:hover {
  background: #fef2f2;
  color: #ef4444;
}

.available-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 240px;
  overflow: auto;
  padding-right: 4px;
}

.available-item {
  display: inline-grid;
  gap: 2px;
  padding: 4px 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
  color: #2563eb;
  font-size: 12px;
  cursor: pointer;
}

.available-item:hover {
  border-color: #2563eb;
  background: #eff6ff;
}

.empty {
  font-size: 12px;
  color: #94a3b8;
}
</style>
