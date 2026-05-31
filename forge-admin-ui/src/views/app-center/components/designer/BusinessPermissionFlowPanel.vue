<template>
  <div class="business-permission-flow-panel">
    <div class="permission-flow-head">
      <div>
        <h3>数据权限</h3>
        <p>维护对象运行时的数据范围，并配置树形对象的层级字段。</p>
      </div>
      <n-tag :type="localModel.policies?.dataScope === 'FOLLOW_SYSTEM' ? 'success' : 'info'" :bordered="false">
        {{ dataScopeLabel }}
      </n-tag>
    </div>

    <div class="permission-flow-body">
      <main class="permission-summary-pane">
        <section class="summary-card">
          <div class="summary-card-head">
            <div>
              <h4>数据策略</h4>
              <p>控制当前对象列表、详情、编辑接口可访问的数据范围。</p>
            </div>
          </div>
          <n-form label-placement="top" size="small" :show-feedback="false">
            <n-grid :cols="2" :x-gap="14" :y-gap="4" responsive="screen">
              <n-form-item-gi label="数据范围">
                <n-select
                  :value="localModel.policies.dataScope"
                  :options="dataScopeOptions"
                  @update:value="updateDataScope"
                />
              </n-form-item-gi>
              <n-form-item-gi label="租户字段">
                <n-input :value="`${localModel.policies.tenantField || 'tenantId'} / ${localModel.policies.tenantColumn || 'tenant_id'}`" disabled />
              </n-form-item-gi>
              <template v-if="isFollowSystemDataScope">
                <n-form-item-gi label="本人字段">
                  <n-select
                    :value="localModel.policies.userField"
                    clearable
                    filterable
                    :options="fieldOptions"
                    @update:value="value => updatePolicyField('user', value)"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="组织字段">
                  <n-select
                    :value="localModel.policies.orgField"
                    clearable
                    filterable
                    :options="fieldOptions"
                    @update:value="value => updatePolicyField('org', value)"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="区划字段">
                  <n-select
                    :value="localModel.policies.regionField"
                    clearable
                    filterable
                    :options="fieldOptions"
                    @update:value="value => updatePolicyField('region', value)"
                  />
                </n-form-item-gi>
              </template>
              <n-form-item-gi label="主键策略">
                <n-input :value="`${localModel.policies.primaryKeyField || 'id'} / ${localModel.policies.primaryKeyStrategy || 'AUTO_INCREMENT'}`" disabled />
              </n-form-item-gi>
              <n-form-item-gi label="逻辑删除字段">
                <n-input :value="`${localModel.policies.logicDeleteField || 'delFlag'} / ${localModel.policies.logicDeleteColumn || 'del_flag'}`" disabled />
              </n-form-item-gi>
              <n-form-item-gi label="审计字段">
                <n-switch :value="true" disabled />
              </n-form-item-gi>
            </n-grid>
          </n-form>
        </section>

        <section class="summary-card">
          <div class="summary-card-head">
            <div>
              <h4>树形模型</h4>
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
          <h4>当前策略</h4>
          <div class="permission-facts">
            <div>
              <span>数据范围</span>
              <strong>{{ dataScopeLabel }}</strong>
            </div>
            <div>
              <span>树形模型</span>
              <strong>{{ treeConfig.enabled ? '已启用' : '未启用' }}</strong>
            </div>
            <div>
              <span>本人字段</span>
              <strong>{{ localModel.policies.userField || '-' }}</strong>
            </div>
            <div>
              <span>组织字段</span>
              <strong>{{ localModel.policies.orgField || '-' }}</strong>
            </div>
          </div>
        </section>
        <section>
          <h4>发布关注</h4>
          <p>启用树形模型会把对象列表切换为树形运行态；关闭时会移除列表中的树形运行配置。</p>
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
  normalizeLowcodePolicies,
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

const dataScopeOptions = [
  { label: '租户隔离', value: 'TENANT' },
  { label: '跟随系统数据权限', value: 'FOLLOW_SYSTEM' },
]
const treeLoadModeOptions = [
  { label: '一次性加载', value: 'full' },
  { label: '懒加载', value: 'lazy' },
]

const localModel = ref(normalizeModel(props.modelSchema))

const treeConfig = computed(() => localModel.value.treeConfig || {})
const isFollowSystemDataScope = computed(() => localModel.value.policies?.dataScope === 'FOLLOW_SYSTEM')
const dataScopeLabel = computed(() => {
  const item = dataScopeOptions.find(option => option.value === localModel.value.policies?.dataScope)
  return item?.label || '租户隔离'
})
const fieldOptions = computed(() => {
  const fields = localModel.value.fields?.length ? localModel.value.fields : props.fields.map(toPageField)
  return fields
    .filter(field => field?.field)
    .map(field => ({
      label: `${field.label || field.fieldName || field.field}（${field.field}）`,
      value: field.field,
    }))
})

watch(
  () => props.modelSchema,
  (value) => {
    const next = normalizeModel(value)
    if (!isSameSchema(next, localModel.value))
      localModel.value = next
  },
  { deep: true },
)

function updateDataScope(value) {
  const next = {
    ...localModel.value,
    policies: {
      ...(localModel.value.policies || {}),
      dataScope: value,
    },
  }
  normalizeLowcodePolicies(next)
  commitModel(next)
}

function updatePolicyField(kind, fieldName) {
  const field = (localModel.value.fields || []).find(item => item.field === fieldName || item.columnName === fieldName)
  const columnName = field?.columnName || ''
  const policies = {
    ...(localModel.value.policies || {}),
  }
  if (kind === 'user') {
    policies.userField = fieldName || ''
    policies.userColumn = columnName
  }
  if (kind === 'org') {
    policies.orgField = fieldName || ''
    policies.orgColumn = columnName
  }
  if (kind === 'region') {
    policies.regionField = fieldName || ''
    policies.regionColumn = columnName
  }
  commitModel({
    ...localModel.value,
    policies,
  })
}

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
  source.policies = source.policies || {}
  normalizeLowcodePolicies(source)
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
})
</script>

<style scoped>
.business-permission-flow-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: calc(100vh - 106px);
}

.permission-flow-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 16px;
}

.permission-flow-head h3,
.summary-card h4,
.permission-flow-tips h4 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.permission-flow-head p,
.summary-card p,
.permission-flow-tips p {
  margin: 4px 0 0;
  color: #64748b;
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
  overflow: auto;
  background: #f8fafc;
  padding: 14px;
}

.summary-card,
.permission-flow-tips section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
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
  border-left: 1px solid #e5e7eb;
  background: #fbfcfe;
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
  background: #f1f5f9;
  padding: 10px;
}

.permission-facts span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.permission-facts strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #111827;
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
    border-top: 1px solid #e5e7eb;
  }
}
</style>
