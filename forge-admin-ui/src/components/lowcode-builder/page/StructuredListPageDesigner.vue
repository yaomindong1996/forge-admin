<template>
  <div class="structured-designer">
    <section class="surface-section">
      <div class="section-head">
        <div>
          <div class="section-title">
            查询集
          </div>
          <div class="section-desc">
            查询、重置、展开收起固定在查询集内；这里只调整字段和顺序。
          </div>
        </div>
        <n-space size="small" align="center">
          <NSwitch
            :value="searchZone?.enabled !== false"
            size="small"
            @update:value="patchZone('search', { enabled: $event })"
          />
          <NButton size="small" @click="resetSearchFields">
            恢复默认
          </NButton>
        </n-space>
      </div>

      <div class="query-surface">
        <div class="query-grid">
          <n-form-item
            v-for="queryField in searchFields"
            :key="queryField.field"
            :label="queryField.label || queryField.field"
            :show-feedback="false"
          >
            <ComponentPreviewControl
              :field="queryField"
              :query-type="resolveSearchQueryType(queryField)"
              :options="resolveOptions(queryField)"
              disabled
            />
          </n-form-item>
        </div>
        <div class="query-actions">
          <NButton type="primary" size="small" disabled>
            查询
          </NButton>
          <NButton size="small" disabled>
            重置
          </NButton>
          <NButton text type="primary" size="small" disabled>
            条件收起
          </NButton>
          <NButton v-if="tableZone?.props?.enableCustomQuery" text size="small" disabled>
            自定义查询
          </NButton>
        </div>
      </div>

      <FieldConfigSummary
        title="查询字段"
        :fields="searchFields"
        :total="fields.length"
        empty-text="当前没有查询字段"
        @configure="openFieldModal('search')"
      />
    </section>

    <section class="surface-section">
      <div class="section-head">
        <div>
          <div class="section-title">
            列表页
          </div>
          <div class="section-desc">
            新增、导出是独立工具按钮；表格列只配置字段顺序和显示范围。
          </div>
        </div>
        <n-space size="small" align="center">
          <NSwitch
            :value="tableZone?.enabled !== false"
            size="small"
            @update:value="patchZone('table', { enabled: $event })"
          />
          <NButton size="small" @click="resetTableFields">
            恢复默认
          </NButton>
        </n-space>
      </div>

      <div class="table-toolbar">
        <n-space size="small">
          <NButton type="primary" size="small" disabled>
            新增
          </NButton>
          <NButton v-if="tableZone?.props?.showExport" size="small" disabled>
            导出
          </NButton>
          <NButton v-if="tableZone?.props?.showImport" size="small" disabled>
            导入
          </NButton>
        </n-space>
        <n-space size="small" align="center">
          <span class="toggle-label">导入</span>
          <NSwitch
            :value="!!tableZone?.props?.showImport"
            size="small"
            @update:value="updateTableProp('showImport', $event)"
          />
          <span class="toggle-label">导出</span>
          <NSwitch
            :value="!!tableZone?.props?.showExport"
            size="small"
            @update:value="updateTableProp('showExport', $event)"
          />
          <span class="toggle-label">自定义查询</span>
          <NSwitch
            :value="tableZone?.props?.enableCustomQuery !== false"
            size="small"
            @update:value="updateTableProp('enableCustomQuery', $event)"
          />
        </n-space>
      </div>

      <div class="table-toolbar sort-setting-bar">
        <n-space size="small" align="center">
          <span class="toggle-label">默认排序</span>
          <NSelect
            :value="tableZone?.props?.defaultSortField || 'id'"
            :options="sortFieldOptions"
            size="small"
            filterable
            style="width: 180px"
            @update:value="updateTableProp('defaultSortField', $event)"
          />
          <NSelect
            :value="tableZone?.props?.defaultSortOrder || 'desc'"
            :options="sortOrderOptions"
            size="small"
            style="width: 120px"
            @update:value="updateTableProp('defaultSortOrder', $event)"
          />
        </n-space>
      </div>

      <n-data-table
        :columns="tableColumns"
        :data="sampleRows"
        :bordered="false"
        size="small"
        class="preview-table"
      />

      <FieldConfigSummary
        title="列表字段"
        :fields="tableFields"
        :total="fields.length"
        empty-text="当前没有列表字段"
        @configure="openFieldModal('table')"
      />

      <template v-if="layoutType === 'tree-crud'">
        <n-divider>树形导航</n-divider>
        <div class="tree-grid">
          <n-form-item label="树标题">
            <NInput
              :value="treeConfig.treeTitle"
              placeholder="例如：组织架构"
              @update:value="updateTreeConfig('treeTitle', $event)"
            />
          </n-form-item>
          <n-form-item label="父级字段">
            <NSelect
              :value="treeConfig.parentField"
              :options="fieldOptions"
              placeholder="请选择父级字段"
              @update:value="updateTreeConfig('parentField', $event)"
            />
          </n-form-item>
          <n-form-item label="显示字段">
            <NSelect
              :value="treeConfig.labelField"
              :options="fieldOptions"
              placeholder="请选择树节点显示字段"
              @update:value="updateTreeConfig('labelField', $event)"
            />
          </n-form-item>
          <n-form-item label="加载方式">
            <NSelect
              :value="treeConfig.loadMode || 'full'"
              :options="treeLoadModeOptions"
              @update:value="updateTreeConfig('loadMode', $event)"
            />
          </n-form-item>
        </div>
      </template>
    </section>

    <n-modal v-model:show="fieldModalVisible" :mask-closable="false">
      <n-card
        class="field-config-modal"
        :title="activeFieldEditor?.title"
        :bordered="false"
        role="dialog"
        aria-modal="true"
      >
        <FieldOrderEditor
          v-if="activeFieldEditor"
          :title="activeFieldEditor.title"
          :empty-text="activeFieldEditor.emptyText"
          :fields="fields"
          :selected-refs="activeFieldEditor.selectedRefs"
          :filter="activeFieldEditor.filter"
          :mode="activeFieldEditor.mode"
          :settings="activeFieldEditor.settings"
          @update="updateZoneRefs(activeFieldEditor.zoneKey, $event)"
          @update-settings="updateZoneFieldSetting(activeFieldEditor.zoneKey, $event)"
        />
        <template #footer>
          <n-space justify="end">
            <NButton @click="fieldModalVisible = false">
              完成
            </NButton>
          </n-space>
        </template>
      </n-card>
    </n-modal>
  </div>
</template>

<script setup>
import { NButton, NDatePicker, NInput, NInputNumber, NSelect, NSwitch, NTimePicker, NTreeSelect, NUpload } from 'naive-ui'
import { computed, defineComponent, h, ref } from 'vue'
import draggable from 'vuedraggable'
import { isPageFieldVisible } from './page-schema'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  layoutType: {
    type: String,
    default: 'simple-crud',
  },
})

const emit = defineEmits(['update:modelValue'])
const queryTypeOptions = [
  { label: '等于', value: 'eq' },
  { label: '包含', value: 'like' },
  { label: '大于等于', value: 'ge' },
  { label: '小于等于', value: 'le' },
  { label: '区间', value: 'between' },
  { label: '多值', value: 'in' },
]
const searchComponentOptions = [
  { label: '自动', value: '' },
  { label: '输入框', value: 'input' },
  { label: '数字输入', value: 'number' },
  { label: '下拉选择', value: 'select' },
  { label: '字典选择', value: 'dictSelect' },
  { label: '组织树', value: 'orgTreeSelect' },
  { label: '用户选择', value: 'userSelect' },
  { label: '区划树', value: 'regionTreeSelect' },
  { label: '树形选择', value: 'treeSelect' },
  { label: '日期', value: 'date' },
  { label: '日期时间', value: 'datetime' },
  { label: '时间', value: 'time' },
]
const tableRenderOptions = [
  { label: '默认', value: '' },
  { label: '字典标签', value: 'dictTag' },
  { label: '组织名称', value: 'orgName' },
  { label: '用户名称', value: 'userName' },
  { label: '区划名称', value: 'regionName' },
  { label: '文件名称', value: 'fileUpload' },
  { label: '图片预览', value: 'imageUpload' },
]
const treeLoadModeOptions = [
  { label: '全量加载', value: 'full' },
  { label: '懒加载', value: 'lazy' },
]
const sortOrderOptions = [
  { label: '降序', value: 'desc' },
  { label: '升序', value: 'asc' },
]

const ComponentPreviewControl = defineComponent({
  name: 'ComponentPreviewControl',
  props: {
    field: {
      type: Object,
      required: true,
    },
    options: {
      type: Array,
      default: () => [],
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    queryType: {
      type: String,
      default: '',
    },
  },
  setup(controlProps) {
    return () => {
      const field = controlProps.field
      const label = field.label || field.field
      const componentType = field.componentType
      const previewType = resolveSearchControlType(field, controlProps.queryType)
      if (componentType === 'number' || ['int', 'bigint', 'decimal'].includes(field.dataType)) {
        return h(NInputNumber, {
          disabled: controlProps.disabled,
          placeholder: `请输入${label}`,
          showButton: false,
          style: 'width: 100%',
        })
      }
      if (['daterange', 'datetimerange'].includes(previewType)) {
        return h(NDatePicker, {
          disabled: controlProps.disabled,
          type: previewType,
          startPlaceholder: `开始${label}`,
          endPlaceholder: `结束${label}`,
          style: 'width: 100%',
        })
      }
      if (previewType === 'timerange') {
        return h('div', { class: 'preview-time-range' }, [
          h(NTimePicker, {
            disabled: controlProps.disabled,
            placeholder: `开始${label}`,
            style: 'width: 100%',
          }),
          h('span', null, '至'),
          h(NTimePicker, {
            disabled: controlProps.disabled,
            placeholder: `结束${label}`,
            style: 'width: 100%',
          }),
        ])
      }
      if (['date', 'datetime'].includes(previewType)) {
        return h(NDatePicker, {
          disabled: controlProps.disabled,
          type: previewType,
          placeholder: `请选择${label}`,
          style: 'width: 100%',
        })
      }
      if (previewType === 'time') {
        return h(NTimePicker, {
          disabled: controlProps.disabled,
          placeholder: `请选择${label}`,
          style: 'width: 100%',
        })
      }
      if (['treeSelect', 'orgTreeSelect', 'regionTreeSelect'].includes(componentType)) {
        return h(NTreeSelect, {
          disabled: controlProps.disabled,
          options: controlProps.options,
          placeholder: `请选择${label}`,
          clearable: true,
          filterable: true,
        })
      }
      if (field.dictType || ['select', 'radio', 'checkbox', 'dictSelect', 'userSelect'].includes(componentType)) {
        return h(NSelect, {
          disabled: controlProps.disabled,
          options: controlProps.options,
          placeholder: `请选择${label}`,
          clearable: true,
        })
      }
      if (['fileUpload', 'imageUpload', 'upload'].includes(componentType)) {
        return h(NUpload, {
          disabled: controlProps.disabled,
          defaultUpload: false,
          showFileList: false,
          accept: componentType === 'imageUpload' ? 'image/*' : undefined,
        }, {
          default: () => h(NButton, { size: 'small', disabled: controlProps.disabled }, { default: () => componentType === 'imageUpload' ? '选择图片' : '选择文件' }),
        })
      }
      return h(NInput, {
        disabled: controlProps.disabled,
        type: componentType === 'textarea' ? 'textarea' : 'text',
        rows: componentType === 'textarea' ? 2 : undefined,
        placeholder: `请输入${label}`,
      })
    }
  },
})

const FieldConfigSummary = defineComponent({
  name: 'FieldConfigSummary',
  props: {
    title: {
      type: String,
      required: true,
    },
    fields: {
      type: Array,
      default: () => [],
    },
    total: {
      type: Number,
      default: 0,
    },
    emptyText: {
      type: String,
      required: true,
    },
  },
  emits: ['configure'],
  setup(summaryProps, { emit }) {
    return () => h('div', { class: 'field-config-summary' }, [
      h('div', { class: 'summary-main' }, [
        h('div', { class: 'summary-title' }, [
          h('strong', null, summaryProps.title),
          h('span', null, `${summaryProps.fields.length}/${summaryProps.total} 个字段`),
        ]),
        summaryProps.fields.length
          ? h('div', { class: 'summary-chip-list' }, [
              ...summaryProps.fields.slice(0, 10).map(field => h('span', {
                key: field.field,
                class: 'summary-chip',
              }, field.label || field.field)),
              summaryProps.fields.length > 10
                ? h('span', { class: 'summary-chip muted' }, `+${summaryProps.fields.length - 10}`)
                : null,
            ])
          : h('div', { class: 'summary-empty' }, summaryProps.emptyText),
      ]),
      h(NButton, {
        size: 'small',
        type: 'primary',
        secondary: true,
        onClick: () => emit('configure'),
      }, { default: () => '配置字段' }),
    ])
  },
})

const FieldOrderEditor = defineComponent({
  name: 'FieldOrderEditor',
  props: {
    title: {
      type: String,
      required: true,
    },
    emptyText: {
      type: String,
      required: true,
    },
    fields: {
      type: Array,
      default: () => [],
    },
    selectedRefs: {
      type: Array,
      default: () => [],
    },
    filter: {
      type: Function,
      required: true,
    },
    mode: {
      type: String,
      default: '',
    },
    settings: {
      type: Object,
      default: () => ({}),
    },
  },
  emits: ['update', 'updateSettings'],
  setup(editorProps, { emit }) {
    const fieldMap = computed(() => new Map(editorProps.fields.map(field => [field.field, field])))
    const selectedRows = computed(() => editorProps.selectedRefs.map(ref => fieldMap.value.get(ref)).filter(Boolean))
    const availableRows = computed(() => editorProps.fields.filter((field) => {
      return editorProps.filter(field) && !editorProps.selectedRefs.includes(field.field)
    }))
    const queryFieldOptions = computed(() => editorProps.fields
      .filter(field => !field.systemField)
      .map(field => ({
        label: field.label ? `${field.label}（${field.sourceField || field.field}）` : (field.sourceField || field.field),
        value: field.field,
      })))
    const renderTargetFieldOptions = (field = {}) => {
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
    const updateRows = rows => emit('update', rows.map(row => row.field))
    const remove = field => emit('update', editorProps.selectedRefs.filter(ref => ref !== field))
    const add = field => emit('update', [...editorProps.selectedRefs, field])
    const updateSetting = (field, patch) => {
      emit('updateSettings', {
        field,
        settings: {
          ...(editorProps.settings?.[field] || {}),
          ...patch,
        },
      })
    }
    return () => h('div', { class: 'field-editor' }, [
      h('div', { class: 'field-editor-title' }, editorProps.title),
      selectedRows.value.length
        ? h(draggable, {
            'modelValue': selectedRows.value,
            'itemKey': 'field',
            'handle': '.field-handle',
            'animation': 160,
            'class': 'selected-field-list',
            'onUpdate:modelValue': updateRows,
          }, {
            item: ({ element }) => {
              const setting = editorProps.settings?.[element.field] || {}
              const tableRenderType = setting.renderType || resolveDefaultTableRenderType(element)
              return h('div', { class: ['selected-field-row', editorProps.mode ? `mode-${editorProps.mode}` : ''] }, [
                h('span', { class: 'field-handle' }, '☰'),
                h('span', { class: 'field-name' }, [
                  h('span', null, element.label || element.field),
                  element.sourceLabel || element.modelName
                    ? h('small', null, element.sourceLabel || element.modelName)
                    : null,
                ]),
                h('span', { class: 'field-code' }, element.field),
                h('button', { type: 'button', onClick: () => remove(element.field) }, '移除'),
                editorProps.mode === 'search'
                  ? h('div', { class: 'field-setting-row search-setting-row' }, [
                      h(NSelect, {
                        value: setting.queryType || element.queryType || 'like',
                        options: queryTypeOptions,
                        size: 'tiny',
                        placeholder: '查询方式',
                        onUpdateValue: value => updateSetting(element.field, { queryType: value }),
                      }),
                      h(NSelect, {
                        value: setting.componentType || resolveDefaultSearchComponentType(element),
                        options: searchComponentOptions,
                        size: 'tiny',
                        placeholder: '查询组件',
                        onUpdateValue: value => updateSetting(element.field, { componentType: value }),
                      }),
                      h(NSelect, {
                        value: setting.queryField || element.field,
                        options: queryFieldOptions.value,
                        size: 'tiny',
                        filterable: true,
                        placeholder: '映射字段',
                        onUpdateValue: value => updateSetting(element.field, { queryField: value }),
                      }),
                    ])
                  : null,
                editorProps.mode === 'table'
                  ? h('div', { class: 'field-setting-row table-setting-row' }, [
                      h('span', { class: 'field-inline-switch' }, [
                        h('span', null, '排序'),
                        h(NSwitch, {
                          value: Boolean(setting.sortable ?? element.sortable),
                          size: 'small',
                          onUpdateValue: value => updateSetting(element.field, { sortable: value }),
                        }),
                      ]),
                      h(NSelect, {
                        value: tableRenderType,
                        options: tableRenderOptions,
                        size: 'tiny',
                        placeholder: '渲染方式',
                        onUpdateValue: value => updateSetting(element.field, { renderType: value }),
                      }),
                      isNameRenderType(tableRenderType)
                        ? h(NSelect, {
                            value: setting.targetField || `${element.field}Name`,
                            options: renderTargetFieldOptions(element),
                            size: 'tiny',
                            filterable: true,
                            tag: true,
                            placeholder: '名称字段',
                            onUpdateValue: value => updateSetting(element.field, { targetField: value }),
                          })
                        : null,
                    ])
                  : null,
              ])
            },
          })
        : h('div', { class: 'field-empty' }, editorProps.emptyText),
      availableRows.value.length
        ? h('div', { class: 'available-field-list' }, availableRows.value.map(field => h('button', {
            key: field.field,
            type: 'button',
            onClick: () => add(field.field),
          }, [
            h('span', null, field.label || field.field),
            field.sourceLabel || field.modelName ? h('small', null, field.sourceLabel || field.modelName) : null,
          ])))
        : null,
    ])
  },
})

const fieldMap = computed(() => new Map(props.fields.map(field => [field.field, field])))
const searchZone = computed(() => findZone('search'))
const tableZone = computed(() => findZone('table'))
const fieldModalVisible = ref(false)
const activeFieldZone = ref('search')
const fieldOptions = computed(() => props.fields.map(field => ({
  label: field.label ? `${field.label}（${field.field}）` : field.field,
  value: field.field,
})))
const sortFieldOptions = computed(() => {
  const options = fieldOptions.value.map(item => ({ ...item }))
  if (!options.some(item => item.value === 'id')) {
    options.unshift({ label: 'ID（id）', value: 'id' })
  }
  return options
})
const searchFields = computed(() => resolveFields(searchZone.value, field => field.searchable))
const tableFields = computed(() => resolveFields(tableZone.value, field => field.listVisible !== false))
const treeConfig = computed(() => tableZone.value?.props?.treeConfig || {})
const activeFieldEditor = computed(() => {
  if (activeFieldZone.value === 'table') {
    return {
      zoneKey: 'table',
      title: '列表字段',
      emptyText: '当前没有列表字段',
      mode: 'table',
      selectedRefs: tableZone.value?.fieldRefs || [],
      settings: tableZone.value?.props?.fieldSettings || {},
      filter: field => isPageFieldVisible(field, 'table'),
    }
  }
  return {
    zoneKey: 'search',
    title: '查询字段',
    emptyText: '当前没有查询字段',
    mode: 'search',
    selectedRefs: searchZone.value?.fieldRefs || [],
    settings: searchZone.value?.props?.fieldSettings || {},
    filter: field => isPageFieldVisible(field, 'search'),
  }
})
const tableColumns = computed(() => [
  ...tableFields.value.map(field => ({
    key: field.field,
    title: field.label || field.field,
    minWidth: field.width || 140,
    ellipsis: { tooltip: true },
  })),
  { key: 'actions', title: '操作', width: 140, fixed: 'right' },
])
const sampleRows = computed(() => {
  return Array.from({ length: 3 }).map((_, index) => {
    const row = { id: index + 1, actions: '编辑 / 删除' }
    tableFields.value.forEach((field) => {
      row[field.field] = resolveSampleValue(field, index)
    })
    return row
  })
})

function findZone(zoneKey) {
  return props.modelValue.zones?.find(zone => zone.zoneKey === zoneKey) || null
}

function resolveFields(zone, fallback) {
  if (!zone || zone.enabled === false)
    return []
  const refs = zone.fieldRefs?.length
    ? zone.fieldRefs
    : props.fields.filter(fallback).map(field => field.field)
  return refs.map(ref => fieldMap.value.get(ref)).filter(Boolean)
}

function resolveSearchQueryType(field) {
  return searchZone.value?.props?.fieldSettings?.[field.field]?.queryType || field.queryType || 'like'
}

function resolveSearchControlType(field, queryType = '') {
  const componentType = field.componentType || field.dataType
  if (queryType === 'between') {
    if (componentType === 'datetime')
      return 'datetimerange'
    if (componentType === 'date')
      return 'daterange'
    if (componentType === 'time')
      return 'timerange'
  }
  if (componentType === 'datetime')
    return 'datetime'
  if (componentType === 'date')
    return 'date'
  if (componentType === 'time')
    return 'time'
  return componentType
}

function resolveDefaultSearchComponentType(field = {}) {
  const componentType = field.componentType || field.dataType || 'input'
  if (field.dictType)
    return 'dictSelect'
  if (['int', 'bigint', 'decimal', 'double', 'float'].includes(field.dataType))
    return 'number'
  return componentType === 'inputNumber' ? 'number' : componentType
}

function resolveDefaultTableRenderType(field = {}) {
  const componentType = field.componentType || ''
  if (field.dictType)
    return 'dictTag'
  if (componentType === 'orgTreeSelect')
    return 'orgName'
  if (componentType === 'userSelect')
    return 'userName'
  if (componentType === 'regionTreeSelect')
    return 'regionName'
  if (componentType === 'fileUpload' || componentType === 'imageUpload')
    return componentType
  return ''
}

function isNameRenderType(renderType) {
  return ['orgName', 'userName', 'regionName', 'fileUpload', 'imageUpload'].includes(renderType)
}

function updateZoneRefs(zoneKey, refs) {
  patchZone(zoneKey, { fieldRefs: refs })
}

function updateTableProp(key, value) {
  patchZone('table', {
    props: {
      ...(tableZone.value?.props || {}),
      [key]: value,
    },
  })
}

function updateTreeConfig(key, value) {
  patchZone('table', {
    props: {
      ...(tableZone.value?.props || {}),
      treeConfig: {
        ...(treeConfig.value || {}),
        enabled: true,
        [key]: value,
      },
    },
  })
}

function updateZoneFieldSetting(zoneKey, payload) {
  const zone = findZone(zoneKey)
  if (!zone || !payload?.field)
    return
  patchZone(zoneKey, {
    props: {
      ...(zone.props || {}),
      fieldSettings: {
        ...(zone.props?.fieldSettings || {}),
        [payload.field]: payload.settings,
      },
    },
  })
}

function openFieldModal(zoneKey) {
  activeFieldZone.value = zoneKey
  fieldModalVisible.value = true
}

function patchZone(zoneKey, patch) {
  const zones = (props.modelValue.zones || []).map((zone) => {
    if (zone.zoneKey !== zoneKey)
      return zone
    return {
      ...zone,
      ...patch,
      props: patch.props ? { ...(zone.props || {}), ...patch.props } : zone.props || {},
    }
  })
  emit('update:modelValue', { ...props.modelValue, zones })
}

function resetSearchFields() {
  updateZoneRefs('search', props.fields.filter(field => isPageFieldVisible(field, 'search')).map(field => field.field))
}

function resetTableFields() {
  updateZoneRefs('table', props.fields.filter(field => isPageFieldVisible(field, 'table')).map(field => field.field))
}

function resolveOptions(field) {
  if (field?.dictType)
    return [{ label: `${field.dictType}字典项`, value: '' }]
  return [
    { label: '选项一', value: '1' },
    { label: '选项二', value: '2' },
  ]
}

function resolveSampleValue(field, index = 0) {
  if (field.dictType)
    return '字典值'
  if (field.componentType === 'switch' || field.dataType === 'tinyint')
    return index % 2 === 0 ? '是' : '否'
  if (field.componentType === 'number' || ['int', 'bigint', 'decimal'].includes(field.dataType))
    return field.dataType === 'decimal' ? `${128 + index}.00` : String(128 + index)
  if (field.componentType === 'date' || field.dataType === 'date')
    return '2026-05-20'
  if (field.componentType === 'datetime' || field.dataType === 'datetime')
    return '2026-05-20 09:30:00'
  return field.label ? `${field.label}示例${index + 1}` : `示例${index + 1}`
}
</script>

<style scoped>
.structured-designer {
  display: grid;
  gap: 14px;
  container-type: inline-size;
}

.surface-section {
  padding: 16px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
}

.section-head,
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.section-head {
  margin-bottom: 14px;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.section-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.query-surface {
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.query-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.query-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 4px;
}

:deep(.preview-time-range) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

:deep(.preview-time-range span) {
  color: #64748b;
  font-size: 12px;
}

.table-toolbar {
  margin-bottom: 10px;
}

.toggle-label {
  font-size: 12px;
  color: #475569;
}

.preview-table {
  margin-bottom: 14px;
}

.tree-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

:deep(.field-editor) {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #eef2f7;
}

.field-config-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 14px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
}

.summary-main {
  display: grid;
  min-width: 0;
  gap: 8px;
}

.summary-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-title strong {
  color: #0f172a;
  font-size: 13px;
}

.summary-title span,
.summary-empty {
  color: #64748b;
  font-size: 12px;
}

.summary-chip-list {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 6px;
}

.summary-chip {
  max-width: 128px;
  min-height: 24px;
  overflow: hidden;
  border: 1px solid #dbe3ee;
  border-radius: 6px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  line-height: 22px;
  padding: 0 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-chip.muted {
  color: #64748b;
}

.field-config-modal {
  width: min(960px, calc(100vw - 48px));
}

:deep(.field-editor-title) {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

:deep(.selected-field-list),
:deep(.available-field-list) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.selected-field-row) {
  display: grid;
  grid-template-columns: 16px minmax(80px, auto) minmax(90px, auto) auto;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 8px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  font-size: 12px;
}

:deep(.selected-field-row.mode-search) {
  grid-template-columns: 16px minmax(80px, auto) minmax(90px, auto) auto;
}

:deep(.selected-field-row.mode-table) {
  grid-template-columns: 16px minmax(80px, auto) minmax(90px, auto) auto;
}

:deep(.field-setting-row) {
  display: grid;
  grid-column: 2 / -1;
  gap: 6px;
}

:deep(.search-setting-row) {
  grid-template-columns: 96px minmax(110px, 1fr) minmax(120px, 1fr);
}

:deep(.table-setting-row) {
  grid-template-columns: auto minmax(110px, 0.8fr) minmax(140px, 1.2fr);
}

:deep(.field-inline-switch) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
}

:deep(.field-handle) {
  color: #94a3b8;
  cursor: grab;
}

:deep(.field-name) {
  display: grid;
  gap: 2px;
  color: #0f172a;
  font-weight: 600;
}

:deep(.field-name small),
:deep(.available-field-list small) {
  color: #64748b;
  font-size: 11px;
  font-weight: 400;
}

:deep(.field-code) {
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

:deep(.selected-field-row button),
:deep(.available-field-list button) {
  border: 0;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font-size: 12px;
}

:deep(.field-empty) {
  color: #94a3b8;
  font-size: 12px;
}

:deep(.available-field-list) {
  margin-top: 10px;
}

:deep(.available-field-list button) {
  display: inline-grid;
  gap: 2px;
  min-height: 30px;
  padding: 0 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
}

.field-config-modal :deep(.field-editor) {
  margin-top: 0;
  padding-top: 0;
  border-top: 0;
}

.field-config-modal :deep(.selected-field-list) {
  display: grid;
  max-height: 360px;
  overflow: auto;
  padding-right: 4px;
}

.field-config-modal :deep(.available-field-list) {
  max-height: 180px;
  overflow: auto;
  padding-right: 4px;
}

.field-config-modal :deep(.selected-field-row) {
  grid-template-columns: 20px minmax(120px, 1fr) minmax(140px, 1fr) auto;
  min-height: 36px;
}

.field-config-modal :deep(.selected-field-row.mode-search) {
  grid-template-columns: 20px minmax(120px, 1fr) minmax(140px, 1fr) auto;
}

.field-config-modal :deep(.selected-field-row.mode-table) {
  grid-template-columns: 20px minmax(120px, 1fr) minmax(140px, 1fr) auto;
}

@container (max-width: 980px) {
  .query-grid,
  .tree-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .section-head,
  .table-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}

@container (max-width: 620px) {
  .query-grid,
  .tree-grid {
    grid-template-columns: 1fr;
  }

  .field-config-summary {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
