<template>
  <div class="business-list-designer">
    <div class="list-designer-head">
      <div>
        <h3>列表设计</h3>
        <p>维护查询条件、表格列、工具栏按钮和行操作。</p>
      </div>
      <n-space size="small" align="center">
        <n-radio-group :value="listLayoutMode" size="small" @update:value="updateListLayoutMode">
          <n-radio-button value="structured">
            结构化配置
          </n-radio-button>
          <n-radio-button value="grid">
            自由布局
          </n-radio-button>
        </n-radio-group>
        <n-select
          :value="localSchema.layoutType"
          :options="layoutOptions"
          size="small"
          style="width: 132px"
          @update:value="updateLayoutType"
        />
        <n-button size="small" type="primary" :loading="saving" @click="saveLayout">
          保存列表
        </n-button>
      </n-space>
    </div>

    <div class="list-designer-body">
      <main class="list-workspace">
        <StructuredListPageDesigner
          v-if="listLayoutMode === 'structured'"
          v-model="localSchema"
          :fields="designFields"
          :layout-type="localSchema.layoutType"
        />
        <ListPageGridDesigner
          v-else
          :model-value="localSchema.listGridLayout || {}"
          :fields="designFields"
          :model-schema="effectiveModelSchema"
          :layout-type="localSchema.layoutType"
          @update:model-value="handleGridLayoutUpdate"
        />
      </main>

      <aside class="list-summary">
        <section>
          <h4>查询条件</h4>
          <p>{{ searchFields.length }} 个字段</p>
          <div class="summary-tags">
            <n-tag v-for="field in searchFields" :key="field.field" size="small" :bordered="false">
              {{ field.label || field.field }}
            </n-tag>
          </div>
        </section>
        <section>
          <h4>表格列</h4>
          <p>{{ tableFields.length }} 个字段</p>
          <div class="summary-tags">
            <n-tag v-for="field in tableFields" :key="field.field" size="small" :bordered="false" type="info">
              {{ field.label || field.field }}
            </n-tag>
          </div>
        </section>
        <section>
          <h4>工具栏</h4>
          <div class="action-list">
            <span v-if="tableZone?.props?.showImport">导入</span>
            <span v-if="tableZone?.props?.showExport">导出</span>
            <span v-if="tableZone?.props?.enableCustomQuery !== false">自定义查询</span>
            <span v-if="tableZone?.props?.hideBatchDelete !== true">批量删除</span>
          </div>
        </section>
        <section>
          <h4>行操作</h4>
          <div class="action-list">
            <span>查看</span>
            <span>编辑</span>
            <span>删除</span>
            <span v-for="action in customActions" :key="action.actionCode || action.label">
              {{ action.label || action.actionName || action.actionCode }}
            </span>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { saveBusinessObjectListLayout } from '@/api/business-app'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import ListPageGridDesigner from '@/components/lowcode-builder/page/ListPageGridDesigner.vue'
import {
  applyGridLayoutToZones,
  createDefaultListGridLayout,
  createDefaultPageSchema,
  syncGridLayoutWithModel,
  syncPageSchemaWithModel,
} from '@/components/lowcode-builder/page/page-schema'
import StructuredListPageDesigner from '@/components/lowcode-builder/page/StructuredListPageDesigner.vue'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  modelValue: {
    type: Object,
    default: null,
  },
  modelSchema: {
    type: Object,
    default: () => ({}),
  },
  fields: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'saved', 'dirtyChange'])

const message = useMessage()
const saving = ref(false)
const layoutOptions = [
  { label: '标准单表', value: 'simple-crud' },
  { label: '左树右表', value: 'tree-crud' },
  { label: '主子表', value: 'master-detail-crud' },
]

const designFields = computed(() => {
  const modelFields = props.modelSchema?.fields || []
  if (modelFields.length)
    return modelFields
  return props.fields.map(toPageField)
})
const effectiveModelSchema = computed(() => ({
  ...(props.modelSchema || {}),
  fields: designFields.value,
}))

const localSchema = ref(resolveSchema(props.modelValue, effectiveModelSchema.value))
const listLayoutMode = computed(() => localSchema.value.listLayoutMode || 'structured')
const searchZone = computed(() => localSchema.value.zones?.find(zone => zone.zoneKey === 'search') || null)
const tableZone = computed(() => localSchema.value.zones?.find(zone => zone.zoneKey === 'table') || null)
const fieldMap = computed(() => new Map(designFields.value.map(field => [field.field, field])))
const searchFields = computed(() => (searchZone.value?.fieldRefs || []).map(ref => fieldMap.value.get(ref)).filter(Boolean))
const tableFields = computed(() => (tableZone.value?.fieldRefs || []).map(ref => fieldMap.value.get(ref)).filter(Boolean))
const customActions = computed(() => tableZone.value?.props?.customActions || [])

watch(
  () => props.modelValue,
  (value) => {
    const next = resolveSchema(value, effectiveModelSchema.value)
    if (!isSameSchema(next, localSchema.value))
      localSchema.value = next
  },
  { deep: true },
)

watch(
  effectiveModelSchema,
  (value) => {
    const next = syncPageSchemaWithModel(localSchema.value, value)
    if (!isSameSchema(next, localSchema.value))
      localSchema.value = next
  },
  { deep: true },
)

watch(
  localSchema,
  (value) => {
    if (!isSameSchema(value, props.modelValue)) {
      emit('update:modelValue', cloneSchema(value))
      emit('dirtyChange', true)
    }
  },
  { deep: true },
)

function updateListLayoutMode(value) {
  if (value === listLayoutMode.value)
    return
  const next = {
    ...localSchema.value,
    listLayoutMode: value,
  }
  if (value === 'grid') {
    const grid = next.listGridLayout?.items?.length
      ? next.listGridLayout
      : createDefaultListGridLayout(effectiveModelSchema.value, { layoutType: next.layoutType })
    next.listGridLayout = syncGridLayoutWithModel(grid, effectiveModelSchema.value, { layoutType: next.layoutType })
    next.zones = applyGridLayoutToZones(next.zones || [], next.listGridLayout, effectiveModelSchema.value)
  }
  localSchema.value = next
}

function updateLayoutType(value) {
  localSchema.value = syncPageSchemaWithModel({
    ...localSchema.value,
    layoutType: value,
  }, effectiveModelSchema.value)
}

function handleGridLayoutUpdate(layout) {
  const synced = syncGridLayoutWithModel(layout, effectiveModelSchema.value, { layoutType: localSchema.value.layoutType })
  localSchema.value = {
    ...localSchema.value,
    listGridLayout: synced,
    zones: applyGridLayoutToZones(localSchema.value.zones || [], synced, effectiveModelSchema.value),
  }
}

async function saveLayout() {
  if (!props.objectId)
    return
  const schema = syncPageSchemaWithModel(localSchema.value, effectiveModelSchema.value)
  saving.value = true
  try {
    await saveBusinessObjectListLayout(props.objectId, {
      layoutKey: 'list',
      layoutName: '列表布局',
      layoutType: schema.layoutType,
      pageSchema: cloneSchema(schema),
      zones: schema.zones?.filter(zone => ['search', 'table'].includes(zone.zoneKey)) || [],
      settings: {
        listLayoutMode: schema.listLayoutMode,
      },
    })
    localSchema.value = schema
    emit('saved', cloneSchema(schema))
    emit('dirtyChange', false)
    message.success('列表布局已保存')
  }
  finally {
    saving.value = false
  }
}

function resolveSchema(pageSchema, modelSchema) {
  const schema = syncPageSchemaWithModel(
    cloneSchema(pageSchema || createDefaultPageSchema(modelSchema)),
    modelSchema,
  )
  return {
    ...schema,
    listLayoutMode: schema.listLayoutMode || 'structured',
  }
}

function toPageField(field) {
  return {
    ...field,
    field: field.field || field.fieldCode,
    label: field.label || field.fieldName || field.fieldCode,
    comment: field.remark || field.fieldName,
    columnName: field.columnName,
    dataType: field.dataType,
    componentType: field.componentType,
    dictType: field.dictType,
    required: field.required,
    systemField: field.systemField,
    readonly: field.readonly,
    searchable: field.searchable,
    listVisible: field.listVisible,
    formVisible: field.formVisible,
  }
}

defineExpose({
  saveLayout,
})
</script>

<style scoped>
.business-list-designer {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: calc(100vh - 106px);
  container-type: inline-size;
}

.list-designer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 16px;
}

.list-designer-head h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  letter-spacing: 0;
}

.list-designer-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.list-designer-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  min-height: 0;
}

.list-workspace {
  min-width: 0;
  overflow: auto;
  background: #f8fafc;
  padding: 14px;
}

.list-summary {
  display: grid;
  align-content: start;
  gap: 12px;
  border-left: 1px solid #e5e7eb;
  background: #fbfcfe;
  overflow: auto;
  padding: 12px;
}

.list-summary section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.list-summary h4 {
  margin: 0;
  color: #111827;
  font-size: 13px;
  letter-spacing: 0;
}

.list-summary p {
  margin: 4px 0 10px;
  color: #64748b;
  font-size: 12px;
}

.summary-tags,
.action-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.action-list span {
  border-radius: 4px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  line-height: 22px;
  padding: 0 8px;
}

@container (max-width: 1180px) {
  .list-designer-head,
  .list-designer-body {
    grid-template-columns: 1fr;
  }

  .list-designer-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .list-summary {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    border-left: 0;
    border-top: 1px solid #e5e7eb;
  }
}
</style>
