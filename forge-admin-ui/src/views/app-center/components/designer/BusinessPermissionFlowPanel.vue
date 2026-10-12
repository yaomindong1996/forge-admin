<template>
  <div class="business-permission-flow-panel">
    <div class="permission-flow-head">
      <div class="permission-flow-head-main">
        <span class="permission-flow-badge" aria-hidden="true">
          <i class="i-lucide:git-fork" />
        </span>
        <div>
          <h3>树形模型</h3>
          <p>配置对象的父子层级、显示字段和加载方式。</p>
        </div>
      </div>
      <n-tag size="medium" :type="treeConfig.enabled ? 'success' : 'default'" :bordered="false">
        {{ treeConfig.enabled ? '已启用' : '未启用' }}
      </n-tag>
    </div>

    <div class="permission-flow-body">
      <main class="permission-summary-pane">
        <section class="summary-card">
          <div class="summary-card-head">
            <div>
              <h4>层级配置</h4>
              <p>树形对象发布后使用父子字段构建层级列表。</p>
            </div>
            <n-switch :value="treeConfig.enabled === true" @update:value="updateTreeEnabled" />
          </div>
          <n-form label-placement="top" size="small" :show-feedback="false">
            <n-grid :cols="2" :x-gap="14" :y-gap="4" responsive="screen">
              <n-form-item-gi label="树标题">
                <n-input
                  :value="treeConfig.treeTitle"
                  placeholder="例如：客户分类树"
                  :disabled="!treeConfig.enabled"
                  @update:value="value => updateTreeConfig({ treeTitle: value })"
                />
              </n-form-item-gi>
              <n-form-item-gi label="加载方式">
                <n-select
                  :value="treeConfig.loadMode || 'full'"
                  :options="treeLoadModeOptions"
                  :disabled="!treeConfig.enabled"
                  @update:value="value => updateTreeConfig({ loadMode: value })"
                />
              </n-form-item-gi>
              <n-form-item-gi label="主键字段">
                <n-select
                  :value="treeConfig.keyField || 'id'"
                  :options="fieldOptions"
                  filterable
                  :disabled="!treeConfig.enabled"
                  @update:value="value => updateTreeConfig({ keyField: value })"
                />
              </n-form-item-gi>
              <n-form-item-gi label="父级字段">
                <n-select
                  :value="treeConfig.parentField || 'parentId'"
                  :options="fieldOptions"
                  filterable
                  :disabled="!treeConfig.enabled"
                  @update:value="value => updateTreeConfig({ parentField: value, filterField: value })"
                />
              </n-form-item-gi>
              <n-form-item-gi label="显示字段">
                <n-select
                  :value="treeConfig.labelField"
                  :options="fieldOptions"
                  filterable
                  :disabled="!treeConfig.enabled"
                  @update:value="value => updateTreeConfig({ labelField: value })"
                />
              </n-form-item-gi>
              <n-form-item-gi label="子级字段名">
                <n-input
                  :value="treeConfig.childrenField || 'children'"
                  :disabled="!treeConfig.enabled"
                  @update:value="value => updateTreeConfig({ childrenField: value })"
                />
              </n-form-item-gi>
            </n-grid>
          </n-form>
        </section>
      </main>

      <aside class="permission-flow-tips">
        <section>
          <h4>当前配置</h4>
          <div class="permission-facts">
            <div>
              <span>树形模型</span>
              <strong>{{ treeConfig.enabled ? '已启用' : '未启用' }}</strong>
            </div>
            <div>
              <span>主键字段</span>
              <strong>{{ resolveFieldLabel(treeConfig.keyField || 'id') }}</strong>
            </div>
            <div>
              <span>父级字段</span>
              <strong>{{ resolveFieldLabel(treeConfig.parentField || 'parentId') }}</strong>
            </div>
            <div>
              <span>显示字段</span>
              <strong>{{ resolveFieldLabel(treeConfig.labelField) || '-' }}</strong>
            </div>
            <div>
              <span>加载方式</span>
              <strong>{{ treeConfig.loadMode === 'lazy' ? '懒加载' : '一次性加载' }}</strong>
            </div>
            <div>
              <span>子级字段名</span>
              <strong>{{ treeConfig.childrenField || 'children' }}</strong>
            </div>
          </div>
        </section>
        <section>
          <h4>配置说明</h4>
          <p>启用树形模型会把对象列表切换为树形运行态；关闭时会移除列表中的树形运行配置。</p>
          <p>父级字段用于「添加下级」和层级拼树；未在表单画布放置时，运行态仍会写入该字段。</p>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import {
  cloneSchema,
  createDefaultField,
  isSameSchema,
} from '@/components/lowcode-builder/model/model-schema'
import { syncPageSchemaWithModel } from '@/components/lowcode-builder/page/page-schema'

const props = defineProps({
  modelSchema: {
    type: Object,
    default: () => ({}),
  },
  pageSchema: {
    type: Object,
    default: null,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  objectName: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelSchema', 'update:pageSchema', 'dirtyChange'])

const treeLoadModeOptions = [
  { label: '一次性加载', value: 'full' },
  { label: '懒加载', value: 'lazy' },
]

const localModel = ref(normalizeModel(props.modelSchema))

const treeConfig = computed(() => localModel.value.treeConfig || {})
const fieldOptions = computed(() => {
  const fields = localModel.value.fields?.length ? localModel.value.fields : props.fields.map(toPageField)
  return fields
    .filter(field => field?.field)
    .map(field => ({
      label: `${field.label || field.fieldName || field.field}（${field.field}）`,
      value: field.field,
    }))
})

function resolveFieldLabel(fieldCode) {
  const code = String(fieldCode || '').trim()
  if (!code)
    return ''
  return fieldOptions.value.find(item => item.value === code)?.label || code
}

watch(
  () => props.modelSchema,
  (value) => {
    const next = normalizeModel(value)
    if (!isSameSchema(next, localModel.value))
      localModel.value = next
  },
  { deep: true },
)

function updateTreeEnabled(value) {
  const next = value ? ensureTreeModel(localModel.value) : disableTreeModel(localModel.value)
  commitModel(next)
  commitPageSchema(next)
}

function updateTreeConfig(patch) {
  if (!treeConfig.value.enabled)
    return
  const next = {
    ...localModel.value,
    treeConfig: {
      ...treeConfig.value,
      ...patch,
      enabled: true,
    },
  }
  if (!next.treeConfig.filterField)
    next.treeConfig.filterField = next.treeConfig.parentField || 'parentId'
  if (!next.treeConfig.targetField)
    next.treeConfig.targetField = next.treeConfig.keyField || 'id'
  commitModel(next)
  commitPageSchema(next)
}

function commitModel(model) {
  localModel.value = normalizeModel(model)
  emit('update:modelSchema', cloneSchema(localModel.value))
  emit('dirtyChange', true)
}

function commitPageSchema(model) {
  const enabled = model.treeConfig?.enabled === true
  const pageSchema = cloneSchema(props.pageSchema || {})
  pageSchema.layoutType = enabled
    ? 'tree-crud'
    : pageSchema.layoutType === 'tree-crud' ? 'simple-crud' : pageSchema.layoutType || 'simple-crud'
  const synced = syncPageSchemaWithModel(pageSchema, model)
  synced.zones = (synced.zones || []).map((zone) => {
    if (zone.zoneKey !== 'table')
      return zone
    const props = { ...(zone.props || {}) }
    if (enabled) {
      props.treeConfig = cloneSchema(model.treeConfig || {})
    }
    else {
      delete props.treeConfig
    }
    return {
      ...zone,
      props,
    }
  })
  emit('update:pageSchema', synced)
}

function normalizeModel(value) {
  const source = cloneSchema(value || {})
  source.fields = source.fields?.length ? source.fields : props.fields.map(toPageField)
  source.treeConfig = normalizeTreeConfig(source)
  source.appType = source.treeConfig.enabled ? 'TREE' : (source.appType || 'SINGLE')
  return source
}

function normalizeTreeConfig(model) {
  const source = model.treeConfig || {}
  const fields = model.fields || []
  const parentField = source.parentField || 'parentId'
  const keyField = source.keyField || 'id'
  const labelField = source.labelField
    || fields.find(field => field.field === 'name')?.field
    || fields.find(field => field.field !== parentField && field.field !== keyField && !field.systemField)?.field
    || fields.find(field => field.field && field.field !== parentField)?.field
    || 'name'
  return {
    enabled: readBoolean(source.enabled, model.appType === 'TREE'),
    keyField,
    parentField,
    labelField,
    filterField: source.filterField || parentField,
    targetField: source.targetField || keyField,
    childrenField: source.childrenField || 'children',
    treeTitle: source.treeTitle || `${model.businessName || props.objectName || '业务'}树`,
    loadMode: source.loadMode || 'full',
  }
}

function ensureTreeModel(model) {
  const next = cloneSchema(model || {})
  next.fields = next.fields || []
  const parentField = next.treeConfig?.parentField || 'parentId'
  if (!next.fields.some(field => field.field === parentField)) {
    next.fields.push({
      ...createDefaultField(parentField, '上级节点'),
      dataType: 'bigint',
      componentType: 'treeSelect',
      queryType: 'eq',
      searchable: false,
      listVisible: false,
      formVisible: true,
      width: 120,
    })
  }
  next.appType = 'TREE'
  next.treeConfig = {
    ...normalizeTreeConfig(next),
    ...(next.treeConfig || {}),
    enabled: true,
    parentField,
    filterField: next.treeConfig?.filterField || parentField,
    targetField: next.treeConfig?.targetField || next.treeConfig?.keyField || 'id',
  }
  return next
}

function disableTreeModel(model) {
  const next = cloneSchema(model || {})
  next.appType = next.appType === 'TREE' ? 'SINGLE' : next.appType || 'SINGLE'
  next.treeConfig = {
    ...normalizeTreeConfig(next),
    enabled: false,
  }
  return next
}

function toPageField(field = {}) {
  return {
    ...field,
    field: field.field || field.fieldCode,
    label: field.label || field.fieldName || field.fieldCode,
    columnName: field.columnName,
    dataType: field.dataType,
    componentType: field.componentType,
    dictType: field.dictType,
    systemField: field.systemField,
    fieldStatus: field.fieldStatus,
    basicProps: { ...(field.basicProps || {}) },
    advancedProps: { ...(field.advancedProps || {}) },
  }
}

function readBoolean(value, defaultValue = false) {
  if (value === null || value === undefined)
    return defaultValue
  if (typeof value === 'boolean')
    return value
  if (typeof value === 'number')
    return value !== 0
  const text = String(value).trim().toLowerCase()
  if (!text)
    return defaultValue
  return ['true', '1', 'yes'].includes(text)
}

defineExpose({
  saveConfig: () => {
    emit('update:modelSchema', cloneSchema(localModel.value))
    emit('dirtyChange', true)
  },
  setTreeEnabled: updateTreeEnabled,
})
</script>

<style scoped>
.business-permission-flow-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 100%;
}

.permission-flow-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--border-light);
  background: linear-gradient(180deg, #f8fbff 0%, var(--bg-primary) 100%);
  padding: 16px 18px;
}

.permission-flow-head-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
}

.permission-flow-badge {
  display: grid;
  place-items: center;
  flex: none;
  width: 36px;
  height: 36px;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #dbeafe;
  border-radius: 4px;
  font-size: 18px;
}

.permission-flow-head h3,
.summary-card h4,
.permission-flow-tips h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
}

.permission-flow-head p,
.summary-card p,
.permission-flow-tips p {
  margin: 4px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.permission-flow-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  min-height: 0;
}

.permission-summary-pane {
  display: grid;
  align-content: start;
  gap: 12px;
  min-width: 0;
  overflow: visible;
  background: var(--bg-secondary);
  padding: 14px;
}

.summary-card,
.permission-flow-tips section {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 14px;
}

.summary-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.permission-flow-tips {
  display: grid;
  align-content: start;
  gap: 12px;
  border-left: 1px solid var(--border-light);
  background: var(--bg-secondary);
  padding: 12px;
}

.permission-facts {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 10px;
}

.permission-facts div {
  min-width: 0;
  border-radius: 6px;
  background: var(--bg-tertiary);
  padding: 10px;
}

.permission-facts span {
  display: block;
  color: var(--text-secondary);
  font-size: 12px;
}

.permission-facts strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .permission-flow-body {
    grid-template-columns: 1fr;
  }

  .permission-flow-tips {
    border-left: 0;
    border-top: 1px solid var(--border-light);
  }
}
</style>
