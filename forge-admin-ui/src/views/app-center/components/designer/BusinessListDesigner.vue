<template>
  <div class="business-list-designer">
    <div class="list-designer-head">
      <div>
        <h3>列表设计</h3>
        <p>维护查询条件、表格列、工具栏按钮和行操作。</p>
      </div>
      <n-space size="small" align="center">
        <n-switch
          :value="treeLayoutEnabled"
          size="small"
          @update:value="updateTreeLayoutEnabled"
        >
          <template #checked>
            左侧导航
          </template>
          <template #unchecked>
            标准列表
          </template>
        </n-switch>
        <n-radio-group :value="listLayoutMode" size="small" @update:value="updateListLayoutMode">
          <n-radio-button value="structured">
            结构化配置
          </n-radio-button>
          <n-radio-button value="grid">
            自由布局
          </n-radio-button>
        </n-radio-group>
        <n-tag
          size="small"
          :type="layoutModeTagType"
          :bordered="false"
        >
          {{ layoutModeLabel }}
        </n-tag>
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
    </div>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { saveBusinessObjectDesigner, saveBusinessObjectListLayout } from '@/api/business-app'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import ListPageGridDesigner from '@/components/lowcode-builder/page/ListPageGridDesigner.vue'
import {
  applyGridLayoutToZones,
  buildPageDesignModelSchema,
  createDefaultListGridLayout,
  createDefaultPageSchema,
  createPageModelRef,
  resolveDefaultTreeConfig,
  syncGridLayoutWithModel,
  syncPageSchemaWithModel,
} from '@/components/lowcode-builder/page/page-schema'
import StructuredListPageDesigner from '@/components/lowcode-builder/page/StructuredListPageDesigner.vue'
import { createViewSchemaFromPageSchema } from './form-first/viewSchema'

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
  viewSchema: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update:modelValue', 'update:viewSchema', 'saved', 'dirtyChange'])

const message = useMessage()
const saving = ref(false)
let applyingExternalSchema = false

const baseModelSchema = computed(() => {
  const modelFields = props.modelSchema?.fields || []
  return {
    ...(props.modelSchema || {}),
    fields: modelFields.length ? modelFields : props.fields.map(toPageField),
  }
})

const localSchema = ref(resolveSchema(props.modelValue, resolveDesignModelSchema(props.modelValue, baseModelSchema.value)))
const effectiveModelSchema = computed(() => resolveDesignModelSchema(localSchema.value, baseModelSchema.value))
const designFields = computed(() => effectiveModelSchema.value.fields || [])
const listLayoutMode = computed(() => localSchema.value.listLayoutMode || 'structured')
const treeLayoutEnabled = computed(() => localSchema.value.layoutType === 'tree-crud')
const layoutModeLabel = computed(() => resolveLayoutModeLabel(localSchema.value.layoutType))
const layoutModeTagType = computed(() => localSchema.value.layoutType === 'simple-crud' ? 'default' : 'info')

watch(
  () => props.modelValue,
  (value) => {
    const next = resolveSchema(value, resolveDesignModelSchema(value, baseModelSchema.value))
    setLocalSchema(next, { external: true })
  },
  { deep: true },
)

watch(
  baseModelSchema,
  (value) => {
    const next = resolveSchema(localSchema.value, resolveDesignModelSchema(localSchema.value, value))
    setLocalSchema(next)
  },
  { deep: true },
)

watch(
  localSchema,
  (value) => {
    if (applyingExternalSchema) {
      applyingExternalSchema = false
      return
    }
    if (!isSameSchema(value, props.modelValue)) {
      emit('update:modelValue', cloneSchema(value))
      emit('update:viewSchema', cloneSchema(buildCurrentViewSchema(value)))
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
  setLocalSchema(resolveSchema(next, effectiveModelSchema.value))
}

function updateTreeLayoutEnabled(enabled) {
  const nextLayoutType = enabled
    ? 'tree-crud'
    : isRelationLayout(localSchema.value, effectiveModelSchema.value) ? 'master-detail-crud' : 'simple-crud'
  const next = {
    ...localSchema.value,
    layoutType: nextLayoutType,
    zones: updateTreeZone(localSchema.value.zones || [], enabled),
  }

  if (next.listLayoutMode === 'grid' || next.listGridLayout?.items?.length) {
    const sourceGrid = next.listGridLayout?.items?.length
      ? next.listGridLayout
      : createDefaultListGridLayout(effectiveModelSchema.value, { layoutType: nextLayoutType })
    next.listGridLayout = syncGridLayoutWithModel(sourceGrid, effectiveModelSchema.value, { layoutType: nextLayoutType })
    next.zones = applyGridLayoutToZones(next.zones || [], next.listGridLayout, effectiveModelSchema.value)
  }

  setLocalSchema(resolveSchema(next, effectiveModelSchema.value))
}

function handleGridLayoutUpdate(layout) {
  const synced = syncGridLayoutWithModel(layout, effectiveModelSchema.value, { layoutType: localSchema.value.layoutType })
  setLocalSchema({
    ...localSchema.value,
    listGridLayout: synced,
    zones: applyGridLayoutToZones(localSchema.value.zones || [], synced, effectiveModelSchema.value),
  })
}

async function saveLayout() {
  if (!props.objectId)
    return
  const schema = resolveSchema(localSchema.value, effectiveModelSchema.value)
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
    const viewSchema = buildCurrentViewSchema(schema)
    await saveBusinessObjectDesigner(props.objectId, {
      viewSchema: cloneSchema(viewSchema),
    })
    setLocalSchema(schema, { external: true })
    emit('update:viewSchema', cloneSchema(viewSchema))
    emit('saved', cloneSchema(schema))
    emit('dirtyChange', false)
    message.success('列表布局已保存')
  }
  catch (error) {
    message.error(error?.message || '列表布局保存失败')
    throw error
  }
  finally {
    saving.value = false
  }
}

function buildCurrentViewSchema(schema = localSchema.value) {
  return createViewSchemaFromPageSchema(schema, designFields.value, props.viewSchema || {})
}

function resolveSchema(pageSchema, modelSchema) {
  const source = cloneSchema(pageSchema || createDefaultPageSchema(modelSchema))
  const layoutType = inferLayoutType(source, modelSchema)
  const schema = syncPageSchemaWithModel(
    {
      ...source,
      layoutType,
    },
    modelSchema,
  )
  return {
    ...schema,
    layoutType,
    listLayoutMode: schema.listLayoutMode || 'structured',
  }
}

function setLocalSchema(schema, options = {}) {
  if (isSameSchema(schema, localSchema.value))
    return
  applyingExternalSchema = !!options.external
  localSchema.value = schema
}

function inferLayoutType(pageSchema, modelSchema) {
  if (isTreeLayout(pageSchema, modelSchema))
    return 'tree-crud'
  if (isRelationLayout(pageSchema, modelSchema))
    return 'master-detail-crud'
  return 'simple-crud'
}

function isTreeLayout(pageSchema = {}, modelSchema = {}) {
  const hasPageTreeConfig = Boolean(pageSchema.zones?.find(zone => zone.zoneKey === 'table')?.props?.treeConfig?.enabled)
  const hasTreeGridBlock = Boolean(pageSchema.listGridLayout?.items?.some(item => item.blockType === 'tree-panel'))
  if (pageSchema.layoutType === 'simple-crud' && !hasPageTreeConfig && !hasTreeGridBlock)
    return false
  return modelSchema?.appType === 'TREE'
    || modelSchema?.treeConfig?.enabled === true
    || pageSchema.layoutType === 'tree-crud'
    || hasPageTreeConfig
    || hasTreeGridBlock
}

function isRelationLayout(pageSchema = {}, modelSchema = {}) {
  return modelSchema?.appType === 'MASTER_DETAIL'
    || (pageSchema.modelRefs || []).some(ref => ref && !ref.primary)
    || (modelSchema.pageModelRefs || []).some(ref => ref && !ref.primary)
    || (modelSchema.fields || []).some(field => field?.modelCode && field.field !== (field.sourceField || field.field))
}

function resolveLayoutModeLabel(layoutType) {
  if (layoutType === 'tree-crud')
    return '已启用左侧导航'
  if (layoutType === 'master-detail-crud')
    return '已启用关联数据'
  return '标准列表'
}

function updateTreeZone(zones = [], enabled) {
  const defaultTreeConfig = resolveDefaultTreeConfig(effectiveModelSchema.value, effectiveModelSchema.value?.treeConfig || {})
  return zones.map((zone) => {
    if (zone.zoneKey !== 'table')
      return zone
    const props = { ...(zone.props || {}) }
    if (enabled) {
      props.treeConfig = {
        ...defaultTreeConfig,
        ...(props.treeConfig || {}),
        enabled: true,
      }
    }
    else {
      delete props.treeConfig
    }
    return {
      ...zone,
      props,
    }
  })
}

function resolveDesignModelSchema(pageSchema, modelSchema) {
  const refs = mergePrimaryModelRef(pageSchema?.modelRefs || [], modelSchema || {})
  return buildPageDesignModelSchema(modelSchema || {}, refs)
}

function mergePrimaryModelRef(modelRefs, modelSchema) {
  if (!Array.isArray(modelRefs) || !modelRefs.length)
    return []
  const primaryRef = createPageModelRef({ modelSchema }, { primary: true })
  const refs = modelRefs.map(ref => ref?.primary
    ? {
        ...ref,
        modelCode: primaryRef.modelCode || ref.modelCode,
        modelName: primaryRef.modelName || ref.modelName,
        tableName: primaryRef.tableName || ref.tableName,
        relations: primaryRef.relations?.length ? primaryRef.relations : ref.relations,
        fields: primaryRef.fields,
      }
    : ref)
  if (!refs.some(ref => ref?.primary))
    refs.unshift(primaryRef)
  return refs
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
    fieldStatus: field.fieldStatus,
    basicProps: { ...(field.basicProps || {}) },
    advancedProps: { ...(field.advancedProps || {}) },
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
  height: calc(100vh - 106px);
  min-height: 680px;
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
  grid-template-columns: minmax(0, 1fr);
  min-height: 0;
}

.list-workspace {
  min-width: 0;
  min-height: 0;
  overflow: auto;
  background: #f8fafc;
  padding: 14px;
}

@container (max-width: 1180px) {
  .list-designer-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
