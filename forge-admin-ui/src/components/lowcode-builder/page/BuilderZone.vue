<template>
  <div class="builder-zone" :class="{ active, disabled: zone.enabled === false }" @click="$emit('select')">
    <div class="zone-head">
      <div>
        <div class="zone-title">
          {{ resolveZoneTitle(zone.zoneKey) }}
        </div>
        <div class="zone-meta">
          {{ zone.componentKey }}
        </div>
      </div>
      <NSwitch
        :value="zone.enabled !== false"
        size="small"
        @click.stop
        @update:value="handleEnabledChange"
      />
    </div>

    <div v-if="selectedFields.length === 0" class="empty-state">
      从右侧参数选择字段，或把左侧业务组件拖到这里启用
    </div>

    <template v-else-if="zone.zoneKey === 'search'">
      <n-form size="small" label-placement="top" :show-feedback="false">
        <div class="control-grid">
          <n-form-item
            v-for="canvasField in selectedFields"
            :key="canvasField.field"
            :label="canvasField.label || canvasField.field"
          >
            <ComponentPreviewControl
              v-model:value="demoModel[canvasField.field]"
              :field="canvasField"
              :options="resolveOptions(canvasField)"
            />
          </n-form-item>
        </div>
      </n-form>
      <div class="preview-actions">
        <NButton size="tiny" type="primary" @click.stop="showPreviewMessage('查询')">
          查询
        </NButton>
        <NButton size="tiny" @click.stop="resetDemoModel">
          重置
        </NButton>
      </div>
    </template>

    <template v-else-if="zone.zoneKey === 'table'">
      <div class="table-preview-actions">
        <NSpace size="small">
          <NButton size="tiny" type="primary" @click.stop="showPreviewMessage('新增')">
            新增
          </NButton>
          <NButton
            v-if="zone.props?.showImport"
            size="tiny"
            @click.stop="showPreviewMessage('批量导入')"
          >
            批量导入
          </NButton>
          <NButton
            v-if="zone.props?.showExport"
            size="tiny"
            @click.stop="showPreviewMessage('数据导出')"
          >
            数据导出
          </NButton>
        </NSpace>
      </div>
      <n-data-table
        :columns="tableColumns"
        :data="sampleRows"
        :bordered="false"
        size="small"
        class="canvas-table"
      />
    </template>

    <template v-else-if="zone.zoneKey === 'edit'">
      <n-form size="small" label-placement="top" :show-feedback="false">
        <div class="control-grid">
          <n-form-item
            v-for="canvasField in selectedFields"
            :key="canvasField.field"
            :label="canvasField.label || canvasField.field"
            :required="canvasField.required"
          >
            <ComponentPreviewControl
              v-model:value="demoModel[canvasField.field]"
              :field="canvasField"
              :options="resolveOptions(canvasField)"
            />
          </n-form-item>
        </div>
      </n-form>
      <div class="preview-actions">
        <NButton size="tiny" type="primary" @click.stop="showPreviewMessage('保存')">
          保存
        </NButton>
        <NButton size="tiny" @click.stop="resetDemoModel">
          清空
        </NButton>
      </div>
    </template>

    <template v-else>
      <n-descriptions size="small" :column="2" bordered>
        <n-descriptions-item
          v-for="detailField in selectedFields"
          :key="detailField.field"
          :label="detailField.label || detailField.field"
        >
          {{ resolveSampleValue(detailField) }}
        </n-descriptions-item>
      </n-descriptions>
    </template>

    <div class="zone-fields">
      <span class="field-count">{{ selectedFields.length }} 个字段</span>
      <n-tag
        v-for="tagField in selectedFields.slice(0, 6)"
        :key="tagField.field"
        size="small"
        :bordered="false"
      >
        {{ tagField.label || tagField.field }}
      </n-tag>
      <span v-if="selectedFields.length > 6" class="more-text">+{{ selectedFields.length - 6 }}</span>
    </div>
  </div>
</template>

<script setup>
import { NButton, NCascader, NCheckboxGroup, NDatePicker, NInput, NInputNumber, NRadio, NRadioGroup, NSelect, NSpace, NSwitch, NTreeSelect, NUpload } from 'naive-ui'
import { computed, defineComponent, h, reactive, ref, watch } from 'vue'
import { getDictData } from '@/composables/useDict'
import { resolveZoneTitle } from './page-schema'

const props = defineProps({
  zone: {
    type: Object,
    required: true,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  active: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['select', 'update:zone'])

const ComponentPreviewControl = defineComponent({
  name: 'ComponentPreviewControl',
  props: {
    value: {
      type: [String, Number, Boolean, Array, Object],
      default: null,
    },
    field: {
      type: Object,
      required: true,
    },
    options: {
      type: Array,
      default: () => [],
    },
  },
  emits: ['update:value'],
  setup(controlProps, { emit }) {
    const updateValue = value => emit('update:value', value)
    return () => {
      const field = controlProps.field
      const placeholder = field.label ? `请输入${field.label}` : '请输入'
      const selectPlaceholder = field.label ? `请选择${field.label}` : '请选择'
      const componentType = field.componentType
      const hasOptions = field.dictType || ['select', 'radio', 'checkbox', 'dictSelect', 'userSelect'].includes(componentType)

      if (componentType === 'textarea') {
        return h(NInput, {
          value: controlProps.value,
          type: 'textarea',
          rows: 2,
          placeholder,
          onUpdateValue: updateValue,
        })
      }
      if (componentType === 'number' || ['int', 'bigint', 'decimal'].includes(field.dataType)) {
        return h(NInputNumber, {
          value: controlProps.value,
          placeholder,
          showButton: false,
          style: 'width: 100%',
          onUpdateValue: updateValue,
        })
      }
      if (componentType === 'date' || field.dataType === 'date') {
        return h(NDatePicker, {
          value: controlProps.value,
          type: 'date',
          placeholder: selectPlaceholder,
          style: 'width: 100%',
          onUpdateValue: updateValue,
        })
      }
      if (componentType === 'datetime' || field.dataType === 'datetime') {
        return h(NDatePicker, {
          value: controlProps.value,
          type: 'datetime',
          placeholder: selectPlaceholder,
          style: 'width: 100%',
          onUpdateValue: updateValue,
        })
      }
      if (componentType === 'switch') {
        return h(NSwitch, {
          value: controlProps.value,
          checkedValue: 1,
          uncheckedValue: 0,
          onUpdateValue: updateValue,
        })
      }
      if (componentType === 'radio') {
        return h(NRadioGroup, {
          value: controlProps.value,
          onUpdateValue: updateValue,
        }, {
          default: () => h(NSpace, { size: 'small' }, {
            default: () => controlProps.options.map(option => h(NRadio, {
              key: option.value,
              value: option.value,
            }, { default: () => option.label })),
          }),
        })
      }
      if (componentType === 'checkbox') {
        return h(NCheckboxGroup, {
          value: Array.isArray(controlProps.value) ? controlProps.value : [],
          options: controlProps.options,
          onUpdateValue: updateValue,
        })
      }
      if (componentType === 'fileUpload' || componentType === 'imageUpload') {
        return h(NUpload, {
          defaultUpload: false,
          showFileList: false,
          accept: componentType === 'imageUpload' ? 'image/*' : undefined,
        }, {
          default: () => h(NButton, { size: 'small' }, { default: () => componentType === 'imageUpload' ? '选择图片' : '选择文件' }),
        })
      }
      if (['treeSelect', 'orgTreeSelect', 'regionTreeSelect'].includes(componentType)) {
        return h(NTreeSelect, {
          value: controlProps.value,
          options: controlProps.options,
          clearable: true,
          filterable: true,
          placeholder: selectPlaceholder,
          onUpdateValue: updateValue,
        })
      }
      if (componentType === 'cascader') {
        return h(NCascader, {
          value: controlProps.value,
          options: controlProps.options,
          clearable: true,
          filterable: true,
          placeholder: selectPlaceholder,
          onUpdateValue: updateValue,
        })
      }
      if (hasOptions) {
        return h(NSelect, {
          value: controlProps.value,
          options: controlProps.options,
          clearable: true,
          filterable: true,
          placeholder: selectPlaceholder,
          onUpdateValue: updateValue,
        })
      }
      return h(NInput, {
        value: controlProps.value,
        placeholder,
        onUpdateValue: updateValue,
      })
    }
  },
})

const demoModel = reactive({})
const dictOptionsMap = ref({})

const selectedFields = computed(() => {
  const fieldMap = new Map(props.fields.map(field => [field.field, field]))
  return (props.zone.fieldRefs || []).map(ref => fieldMap.get(ref)).filter(Boolean)
})

const tableColumns = computed(() => [
  ...selectedFields.value.map(field => ({
    key: field.field,
    title: field.label || field.field,
    minWidth: field.width || 140,
    ellipsis: { tooltip: true },
  })),
  {
    key: 'actions',
    title: '操作',
    width: 120,
    fixed: 'right',
  },
])

const sampleRows = computed(() => {
  const row = { id: 1, actions: '编辑 / 删除' }
  selectedFields.value.forEach((field) => {
    row[field.field] = resolveSampleValue(field)
  })
  return [row]
})

const dictTypes = computed(() => {
  return Array.from(new Set(selectedFields.value.map(field => field.dictType).filter(Boolean)))
})

watch(selectedFields, initDemoModel, { immediate: true })
watch(dictTypes, loadDictOptions, { immediate: true })

function handleEnabledChange(value) {
  patchZone({ enabled: value })
}

function patchZone(patch) {
  emit('update:zone', {
    ...props.zone,
    ...patch,
    props: patch.props ? { ...(props.zone.props || {}), ...patch.props } : props.zone.props || {},
  })
}

function initDemoModel() {
  selectedFields.value.forEach((field) => {
    if (demoModel[field.field] !== undefined)
      return
    if (field.componentType === 'checkbox') {
      demoModel[field.field] = []
    }
    else if (field.componentType === 'switch') {
      demoModel[field.field] = 1
    }
    else {
      demoModel[field.field] = null
    }
  })
}

async function loadDictOptions(types) {
  const nextMap = { ...dictOptionsMap.value }
  for (const type of types || []) {
    if (!nextMap[type]) {
      nextMap[type] = await getDictData(type)
    }
  }
  dictOptionsMap.value = nextMap
}

function resolveOptions(field) {
  if (field.dictType && dictOptionsMap.value[field.dictType]?.length) {
    return dictOptionsMap.value[field.dictType]
  }
  return [
    { label: '选项一', value: '1' },
    { label: '选项二', value: '2' },
  ]
}

function resolveSampleValue(field) {
  if (field.dictType) {
    return resolveOptions(field)[0]?.label || '字典值'
  }
  if (field.componentType === 'switch' || field.dataType === 'tinyint')
    return '是'
  if (field.componentType === 'number' || ['int', 'bigint', 'decimal'].includes(field.dataType))
    return field.dataType === 'decimal' ? '128.00' : '128'
  if (field.componentType === 'date' || field.dataType === 'date')
    return '2026-05-20'
  if (field.componentType === 'datetime' || field.dataType === 'datetime')
    return '2026-05-20 09:30:00'
  if (field.componentType === 'fileUpload')
    return '合同附件.pdf'
  if (field.componentType === 'imageUpload')
    return '图片'
  return field.label ? `${field.label}示例` : '示例数据'
}

function resetDemoModel() {
  selectedFields.value.forEach((field) => {
    demoModel[field.field] = field.componentType === 'checkbox' ? [] : null
  })
}

function showPreviewMessage(action) {
  window.$message?.info(`${action}为画布交互预览，发布后接入运行时接口`)
}
</script>

<style scoped>
.builder-zone {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 14px;
  cursor: pointer;
  min-width: 0;
}

.builder-zone:hover,
.builder-zone.active {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.08);
}

.builder-zone.disabled {
  opacity: 0.58;
}

.zone-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.zone-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.zone-meta {
  margin-top: 2px;
  font-size: 11px;
  color: var(--text-tertiary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.zone-fields {
  min-height: 28px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px dashed var(--border-light);
}

.empty-state {
  min-height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary);
  color: var(--text-tertiary);
  font-size: 12px;
}

.control-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px 12px;
}

.preview-actions,
.table-preview-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.table-preview-actions {
  justify-content: space-between;
  margin-bottom: 10px;
}

.canvas-table {
  --n-td-padding: 8px;
}

.field-count,
.more-text {
  color: var(--text-secondary);
  font-size: 12px;
}

@media (max-width: 1280px) {
  .control-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
