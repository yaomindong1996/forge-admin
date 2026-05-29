<template>
  <div class="business-detail-designer">
    <div class="detail-designer-head">
      <div>
        <h3>详情设计</h3>
        <p>维护详情分组、页签和关联列表入口。</p>
      </div>
      <n-space size="small">
        <n-button size="small" secondary @click="appendAllVisibleFields">
          自动补齐字段
        </n-button>
        <n-button size="small" type="primary" :loading="saving" @click="saveLayout">
          保存详情
        </n-button>
      </n-space>
    </div>

    <div class="detail-designer-body">
      <main class="detail-workspace">
        <n-tabs v-model:value="activeSubTab" type="line" animated>
          <n-tab-pane name="basic" tab="基本信息">
            <div class="detail-group-toolbar">
              <n-space size="small">
                <n-select
                  v-model:value="selectedGroupKey"
                  :options="groupOptions"
                  size="small"
                  style="width: 180px"
                />
                <n-button size="small" secondary @click="addGroup">
                  新建分组
                </n-button>
                <n-button size="small" secondary @click="applyDefaultLayout">
                  恢复默认
                </n-button>
              </n-space>
              <n-tag size="small" type="info" :bordered="false">
                {{ visibleFieldCount }} 个可见字段
              </n-tag>
            </div>

            <div class="detail-group-list">
              <section v-for="(group, index) in detailConfig.groups" :key="group.key" class="detail-group-card">
                <header class="detail-group-head">
                  <div class="group-head-main">
                    <n-input
                      v-model:value="group.title"
                      size="small"
                      placeholder="分组标题"
                      @update:value="markDirty"
                    />
                    <n-select
                      v-model:value="group.columns"
                      :options="columnOptions"
                      size="small"
                      style="width: 92px"
                      @update:value="markDirty"
                    />
                  </div>
                  <n-space size="small">
                    <n-button quaternary circle size="small" :disabled="index === 0" @click="moveGroup(index, -1)">
                      <template #icon>
                        <n-icon><ChevronUpOutline /></n-icon>
                      </template>
                    </n-button>
                    <n-button quaternary circle size="small" :disabled="index === detailConfig.groups.length - 1" @click="moveGroup(index, 1)">
                      <template #icon>
                        <n-icon><ChevronDownOutline /></n-icon>
                      </template>
                    </n-button>
                    <n-popconfirm @positive-click="removeGroup(index)">
                      <template #trigger>
                        <n-button quaternary circle size="small" :disabled="detailConfig.groups.length === 1">
                          <template #icon>
                            <n-icon><TrashOutline /></n-icon>
                          </template>
                        </n-button>
                      </template>
                      确认删除该分组？
                    </n-popconfirm>
                  </n-space>
                </header>

                <div class="detail-group-meta">
                  <span>已选字段 {{ group.items.length }}</span>
                  <span>可见字段 {{ group.items.filter(item => !item.hidden).length }}</span>
                  <span>只读字段 {{ group.items.filter(item => item.readonly).length }}</span>
                </div>

                <div class="detail-group-add">
                  <n-select
                    :value="null"
                    :options="availableFieldOptions(group.key)"
                    clearable
                    filterable
                    placeholder="选择字段后自动加入当前分组"
                    @update:value="value => addFieldToGroup(group.key, value)"
                  />
                </div>

                <div class="detail-field-list">
                  <article v-for="(item, fieldIndex) in group.items" :key="item.fieldRef" class="detail-field-item">
                    <div class="field-item-main">
                      <strong>{{ resolveFieldLabel(item.fieldRef) }}</strong>
                      <p>{{ resolveFieldMeta(item.fieldRef) }}</p>
                    </div>
                    <div class="field-item-switches">
                      <n-switch :value="!item.hidden" @update:value="value => updateFieldVisibility(group.key, fieldIndex, value)" />
                      <span>显示</span>
                      <n-switch :value="item.readonly" @update:value="value => updateFieldReadonly(group.key, fieldIndex, value)" />
                      <span>只读</span>
                    </div>
                    <n-space size="small" class="field-item-actions">
                      <n-button quaternary circle size="small" :disabled="fieldIndex === 0" @click="moveField(group.key, fieldIndex, -1)">
                        <template #icon>
                          <n-icon><ChevronUpOutline /></n-icon>
                        </template>
                      </n-button>
                      <n-button quaternary circle size="small" :disabled="fieldIndex === group.items.length - 1" @click="moveField(group.key, fieldIndex, 1)">
                        <template #icon>
                          <n-icon><ChevronDownOutline /></n-icon>
                        </template>
                      </n-button>
                      <n-popconfirm @positive-click="removeField(group.key, fieldIndex)">
                        <template #trigger>
                          <n-button quaternary circle size="small">
                            <template #icon>
                              <n-icon><CloseOutline /></n-icon>
                            </template>
                          </n-button>
                        </template>
                        确认移除该字段？
                      </n-popconfirm>
                    </n-space>
                  </article>
                  <n-empty v-if="!group.items.length" description="该分组暂无字段" />
                </div>
              </section>
            </div>
          </n-tab-pane>

          <n-tab-pane name="relations" tab="关联数据">
            <div class="detail-relations">
              <div class="detail-subhead">
                <div>
                  <h4>关联页签</h4>
                  <p>将业务关系挂到详情页签，发布检查会校验目标对象是否可打开。</p>
                </div>
                <n-switch v-model:value="detailConfig.showRelationTab" @update:value="markDirty">
                  <template #checked>
                    启用
                  </template>
                  <template #unchecked>
                    关闭
                  </template>
                </n-switch>
              </div>

              <div v-if="relationOptions.length" class="relation-link-list">
                <article v-for="relation in relationOptions" :key="relation.id" class="relation-link-card">
                  <div class="relation-link-head">
                    <div>
                      <strong>{{ relation.relationName }}</strong>
                      <p>{{ relation.sourceObjectName || relation.sourceObjectCode }} → {{ relation.targetObjectName || relation.targetObjectCode }}</p>
                    </div>
                    <n-switch :value="relation.enabled" @update:value="value => updateRelationEnabled(relation.id, value)" />
                  </div>
                  <div class="relation-link-form">
                    <n-input
                      v-model:value="relation.tabTitle"
                      size="small"
                      placeholder="页签名称"
                      @update:value="markDirty"
                    />
                    <n-input
                      v-model:value="relation.defaultFilter"
                      size="small"
                      placeholder="默认筛选条件"
                      @update:value="markDirty"
                    />
                  </div>
                </article>
              </div>
              <n-empty v-else description="暂无可挂载的关联配置" />
            </div>
          </n-tab-pane>

          <n-tab-pane name="logs" tab="操作日志">
            <n-alert type="info" :bordered="false">
              当前只保存详情页签开关和说明，不重写日志底层。
            </n-alert>
            <div class="detail-toggle-list">
              <label class="detail-toggle-row">
                <span>显示操作日志页签</span>
                <n-switch v-model:value="detailConfig.showOperationLog" @update:value="markDirty" />
              </label>
              <label class="detail-toggle-row">
                <span>显示审批记录页签</span>
                <n-switch v-model:value="detailConfig.showApprovalLog" @update:value="markDirty" />
              </label>
            </div>
          </n-tab-pane>

          <n-tab-pane name="approval" tab="审批记录">
            <n-alert type="info" :bordered="false">
              审批记录页签只负责展示开关和入口说明，流程绑定由权限流程面板维护。
            </n-alert>
          </n-tab-pane>
        </n-tabs>
      </main>

      <aside class="detail-shelf">
        <div class="detail-shelf-head">
          <div>
            <h4>字段库</h4>
            <p>点击字段即可加入当前分组，系统字段同样可显示为只读。</p>
          </div>
        </div>

        <div class="detail-shelf-tabs">
          <button type="button" :class="{ active: shelfTab === 'unused' }" @click="shelfTab = 'unused'">
            未使用 {{ unusedFields.length }}
          </button>
          <button type="button" :class="{ active: shelfTab === 'used' }" @click="shelfTab = 'used'">
            已使用 {{ usedFields.length }}
          </button>
          <button type="button" :class="{ active: shelfTab === 'system' }" @click="shelfTab = 'system'">
            系统 {{ systemFields.length }}
          </button>
        </div>

        <div class="detail-shelf-list">
          <button
            v-for="field in visibleShelfFields"
            :key="field.field"
            type="button"
            class="detail-shelf-item"
            :disabled="usedFieldSet.has(field.field)"
            @click="addFieldToGroup(selectedGroupKey, field.field)"
          >
            <strong>{{ field.label || field.field }}</strong>
            <span>{{ field.field }} · {{ field.componentType || field.dataType || 'input' }}</span>
            <em v-if="usedFieldSet.has(field.field)">已使用</em>
            <em v-else-if="field.systemField">系统字段</em>
          </button>
          <n-empty v-if="!visibleShelfFields.length" description="当前分组没有字段" />
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ChevronDownOutline, ChevronUpOutline, CloseOutline, TrashOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { saveBusinessObjectDetailLayout } from '@/api/business-app'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import { createDefaultPageSchema, isReadonlySystemField, syncPageSchemaWithModel } from '@/components/lowcode-builder/page/page-schema'

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
  relations: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'saved', 'dirtyChange'])

const message = useMessage()
const saving = ref(false)
const activeSubTab = ref('basic')
const shelfTab = ref('unused')
const selectedGroupKey = ref('')

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
const detailZone = computed(() => localSchema.value.zones?.find(zone => zone.zoneKey === 'detail') || null)
const detailConfig = ref(resolveDetailConfig(detailZone.value, designFields.value, props.relations))

const fieldMap = computed(() => new Map(designFields.value.map(field => [field.field, field])))
const usedFieldSet = computed(() => new Set(detailConfig.value.groups.flatMap(group => group.items.map(item => item.fieldRef))))
const visibleFieldCount = computed(() => detailConfig.value.groups.reduce((sum, group) => sum + group.items.filter(item => !item.hidden).length, 0))
const systemFields = computed(() => designFields.value.filter(field => isReadonlySystemField(field)))
const usedFields = computed(() => designFields.value.filter(field => usedFieldSet.value.has(field.field)))
const unusedFields = computed(() => designFields.value.filter(field => !usedFieldSet.value.has(field.field) && !isReadonlySystemField(field)))
const visibleShelfFields = computed(() => {
  if (shelfTab.value === 'used')
    return usedFields.value
  if (shelfTab.value === 'system')
    return systemFields.value
  return unusedFields.value
})
const relationOptions = computed(() => detailConfig.value.relationTabs)
const groupOptions = computed(() => detailConfig.value.groups.map(group => ({
  label: group.title || '未命名分组',
  value: group.key,
})))
const columnOptions = [
  { label: '1 列', value: 1 },
  { label: '2 列', value: 2 },
  { label: '3 列', value: 3 },
  { label: '4 列', value: 4 },
]

watch(
  () => props.modelValue,
  (value) => {
    const next = resolveSchema(value, effectiveModelSchema.value)
    if (!isSameSchema(next, localSchema.value))
      localSchema.value = next
    detailConfig.value = resolveDetailConfig(next.zones?.find(zone => zone.zoneKey === 'detail') || null, designFields.value, props.relations)
  },
  { deep: true, immediate: true },
)

watch(
  effectiveModelSchema,
  (value) => {
    const next = syncPageSchemaWithModel(localSchema.value, value)
    if (!isSameSchema(next, localSchema.value))
      localSchema.value = next
    detailConfig.value = resolveDetailConfig(next.zones?.find(zone => zone.zoneKey === 'detail') || null, designFields.value, props.relations)
  },
  { deep: true },
)

watch(
  () => props.relations,
  (value) => {
    detailConfig.value = resolveDetailConfig(detailZone.value, designFields.value, value || [])
    syncDetailSchema()
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

watch(
  detailConfig,
  () => {
    syncDetailSchema()
  },
  { deep: true },
)

function syncDetailSchema() {
  const zone = buildDetailZone(detailConfig.value, detailZone.value)
  let zones = localSchema.value.zones || []
  if (zones.some(item => item.zoneKey === 'detail'))
    zones = zones.map(item => item.zoneKey === 'detail' ? zone : item)
  else
    zones = [...zones, zone]
  localSchema.value = {
    ...localSchema.value,
    zones,
  }
}

function resolveSchema(pageSchema, modelSchema) {
  return syncPageSchemaWithModel(
    cloneSchema(pageSchema || createDefaultPageSchema(modelSchema)),
    modelSchema,
  )
}

function resolveDetailConfig(zone, fields, relations) {
  const props = zone?.props || {}
  const groups = normalizeGroups(props.detailGroups, zone?.fieldRefs || [], fields)
  const relationTabs = normalizeRelationTabs(props.relationTabs, relations)
  return {
    groups,
    relationTabs,
    showRelationTab: props.showRelationTab !== false,
    showOperationLog: props.showOperationLog !== false,
    showApprovalLog: props.showApprovalLog !== false,
  }
}

function normalizeGroups(groups, fieldRefs, fields) {
  const fieldMap = new Map((fields || []).map(field => [field.field, field]))
  const validRefs = new Set((fields || []).map(field => field.field).filter(Boolean))
  const fallbackRefs = (fieldRefs || []).length ? fieldRefs : (fields || []).map(field => field.field)
  const sourceGroups = Array.isArray(groups) && groups.length
    ? groups
    : [{
        key: 'basic',
        title: '基本信息',
        columns: 2,
        items: fallbackRefs.map(fieldRef => createGroupFieldItem(fieldRef, fieldMap.get(fieldRef))).filter(Boolean),
      }]
  const normalized = sourceGroups.map((group, index) => {
    const items = Array.isArray(group.items) && group.items.length
      ? group.items
      : (group.fieldRefs || []).map(fieldRef => createGroupFieldItem(fieldRef, fieldMap.get(fieldRef))).filter(Boolean)
    return {
      key: group.key || `group_${index + 1}`,
      title: group.title || `分组 ${index + 1}`,
      columns: normalizeColumns(group.columns),
      items: items
        .map(item => normalizeGroupFieldItem(item, fieldMap.get(item.fieldRef)))
        .filter(item => item && validRefs.has(item.fieldRef)),
    }
  })
  if (!normalized.length) {
    normalized.push({
      key: 'basic',
      title: '基本信息',
      columns: 2,
      items: [],
    })
  }
  return normalized
}

function normalizeRelationTabs(relationTabs, relations) {
  const relationMap = new Map((relations || []).map(item => [item.id, item]))
  const source = Array.isArray(relationTabs) && relationTabs.length
    ? relationTabs
    : (relations || []).map(relation => ({
        relationId: relation.id,
        relationName: relation.relationName,
        enabled: true,
        tabTitle: relation.relationName,
        defaultFilter: relation.relationConfig || '',
      }))
  return source.map((item, index) => {
    const relation = relationMap.get(item.relationId)
    return {
      relationId: item.relationId || relation?.id || null,
      relationName: item.relationName || relation?.relationName || `关联 ${index + 1}`,
      sourceObjectCode: relation?.sourceObjectCode || item.sourceObjectCode || '',
      sourceObjectName: relation?.sourceObjectName || item.sourceObjectName || '',
      targetObjectCode: relation?.targetObjectCode || item.targetObjectCode || '',
      targetObjectName: relation?.targetObjectName || item.targetObjectName || '',
      enabled: item.enabled !== false,
      tabTitle: item.tabTitle || relation?.relationName || `关联 ${index + 1}`,
      defaultFilter: item.defaultFilter || relation?.relationConfig || '',
    }
  })
}

function buildDetailZone(config, currentZone) {
  const fieldRefs = config.groups.flatMap(group => group.items.filter(item => !item.hidden).map(item => item.fieldRef)).filter(Boolean)
  return {
    ...(currentZone || {}),
    zoneKey: 'detail',
    componentKey: 'detail-panel',
    enabled: true,
    fieldRefs,
    props: {
      ...(currentZone?.props || {}),
      detailGroups: config.groups,
      relationTabs: config.relationTabs,
      showRelationTab: config.showRelationTab,
      showOperationLog: config.showOperationLog,
      showApprovalLog: config.showApprovalLog,
    },
  }
}

function createGroupFieldItem(fieldRef, field) {
  if (!fieldRef)
    return null
  return {
    fieldRef,
    hidden: false,
    readonly: isReadonlySystemField(field),
  }
}

function normalizeGroupFieldItem(item, field) {
  if (!item?.fieldRef)
    return null
  return {
    fieldRef: item.fieldRef,
    hidden: Boolean(item.hidden),
    readonly: item.readonly ?? isReadonlySystemField(field),
  }
}

function normalizeColumns(columns) {
  const value = Number(columns)
  if ([1, 2, 3, 4].includes(value))
    return value
  return 2
}

function updateDetailConfig(mutator) {
  const next = cloneSchema(detailConfig.value || {})
  mutator(next)
  next.groups = normalizeGroups(next.groups, designFields.value.map(field => field.field), designFields.value)
  detailConfig.value = next
  syncDetailSchema()
}

function addGroup() {
  updateDetailConfig((config) => {
    const index = config.groups.length + 1
    config.groups.push({
      key: `group_${index}`,
      title: `分组 ${index}`,
      columns: 2,
      items: [],
    })
    selectedGroupKey.value = config.groups[config.groups.length - 1].key
  })
}

function removeGroup(index) {
  updateDetailConfig((config) => {
    if (config.groups.length === 1)
      return
    config.groups.splice(index, 1)
    if (!config.groups.some(group => group.key === selectedGroupKey.value))
      selectedGroupKey.value = config.groups[0]?.key || ''
  })
}

function moveGroup(index, direction) {
  updateDetailConfig((config) => {
    const nextIndex = index + direction
    if (nextIndex < 0 || nextIndex >= config.groups.length)
      return
    const [item] = config.groups.splice(index, 1)
    config.groups.splice(nextIndex, 0, item)
  })
}

function availableFieldOptions(groupKey) {
  const currentGroup = detailConfig.value.groups.find(group => group.key === groupKey)
  const used = new Set(detailConfig.value.groups.flatMap(group => group.items.map(item => item.fieldRef)))
  return designFields.value
    .filter(field => !used.has(field.field) || currentGroup?.items.some(item => item.fieldRef === field.field))
    .map(field => ({
      label: `${field.label || field.field}（${field.field}）`,
      value: field.field,
    }))
}

function addFieldToGroup(groupKey, fieldRef) {
  if (!fieldRef)
    return
  updateDetailConfig((config) => {
    const targetGroup = config.groups.find(group => group.key === groupKey) || config.groups[0]
    if (!targetGroup)
      return
    if (targetGroup.items.some(item => item.fieldRef === fieldRef))
      return
    const field = fieldMap.value.get(fieldRef)
    targetGroup.items.push(createGroupFieldItem(fieldRef, field))
    selectedGroupKey.value = targetGroup.key
  })
}

function appendAllVisibleFields() {
  updateDetailConfig((config) => {
    const targetGroup = config.groups[0]
    if (!targetGroup)
      return
    designFields.value.forEach((field) => {
      if (usedFieldSet.value.has(field.field))
        return
      targetGroup.items.push(createGroupFieldItem(field.field, field))
    })
  })
}

function applyDefaultLayout() {
  detailConfig.value = resolveDetailConfig(detailZone.value, designFields.value, props.relations)
  syncDetailSchema()
}

function removeField(groupKey, fieldIndex) {
  updateDetailConfig((config) => {
    const targetGroup = config.groups.find(group => group.key === groupKey)
    if (!targetGroup)
      return
    targetGroup.items.splice(fieldIndex, 1)
  })
}

function moveField(groupKey, fieldIndex, direction) {
  updateDetailConfig((config) => {
    const targetGroup = config.groups.find(group => group.key === groupKey)
    if (!targetGroup)
      return
    const nextIndex = fieldIndex + direction
    if (nextIndex < 0 || nextIndex >= targetGroup.items.length)
      return
    const [item] = targetGroup.items.splice(fieldIndex, 1)
    targetGroup.items.splice(nextIndex, 0, item)
  })
}

function updateFieldVisibility(groupKey, fieldIndex, value) {
  updateDetailConfig((config) => {
    const targetGroup = config.groups.find(group => group.key === groupKey)
    if (!targetGroup)
      return
    targetGroup.items[fieldIndex].hidden = !value
  })
}

function updateFieldReadonly(groupKey, fieldIndex, value) {
  updateDetailConfig((config) => {
    const targetGroup = config.groups.find(group => group.key === groupKey)
    if (!targetGroup)
      return
    targetGroup.items[fieldIndex].readonly = !!value
  })
}

function updateRelationEnabled(relationId, value) {
  updateDetailConfig((config) => {
    const relation = config.relationTabs.find(item => item.relationId === relationId)
    if (!relation)
      return
    relation.enabled = !!value
  })
}

function resolveFieldLabel(fieldRef) {
  const field = fieldMap.value.get(fieldRef)
  return field?.label || fieldRef || '-'
}

function resolveFieldMeta(fieldRef) {
  const field = fieldMap.value.get(fieldRef)
  if (!field)
    return fieldRef || '-'
  return [field.field, field.componentType || field.dataType || 'input'].filter(Boolean).join(' · ')
}

async function saveLayout() {
  if (!props.objectId)
    return
  saving.value = true
  try {
    const schema = syncPageSchemaWithModel(localSchema.value, effectiveModelSchema.value)
    await saveBusinessObjectDetailLayout(props.objectId, {
      layoutKey: 'detail',
      layoutName: '详情布局',
      layoutType: schema.layoutType,
      pageSchema: cloneSchema(schema),
      zones: schema.zones?.filter(zone => zone.zoneKey === 'detail') || [],
      settings: {
        detailGroups: detailConfig.value.groups,
        relationTabs: detailConfig.value.relationTabs,
        showRelationTab: detailConfig.value.showRelationTab,
        showOperationLog: detailConfig.value.showOperationLog,
        showApprovalLog: detailConfig.value.showApprovalLog,
      },
    })
    emit('saved', cloneSchema(schema))
    emit('dirtyChange', false)
    message.success('详情布局已保存')
  }
  finally {
    saving.value = false
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

function markDirty() {
  emit('dirtyChange', true)
}

defineExpose({
  saveLayout,
})
</script>

<style scoped>
.business-detail-designer {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: calc(100vh - 106px);
}

.detail-designer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 16px;
}

.detail-designer-head h3,
.detail-subhead h4,
.detail-shelf-head h4 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.detail-designer-head p,
.detail-subhead p,
.detail-shelf-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.detail-designer-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  min-height: 0;
}

.detail-workspace {
  min-width: 0;
  overflow: auto;
  background: #f8fafc;
  padding: 14px;
}

.detail-group-toolbar,
.detail-subhead,
.detail-group-head,
.detail-group-meta,
.detail-field-item,
.relation-link-head,
.relation-link-form,
.detail-toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.detail-group-list {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}

.detail-group-card,
.relation-link-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.group-head-main {
  display: flex;
  gap: 8px;
  flex: 1;
}

.detail-group-meta {
  flex-wrap: wrap;
  justify-content: flex-start;
  margin-top: 10px;
  color: #64748b;
  font-size: 12px;
}

.detail-group-add {
  margin-top: 10px;
}

.detail-field-list {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.detail-field-item {
  align-items: center;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fdfefe;
  padding: 10px 12px;
}

.field-item-main {
  min-width: 0;
  flex: 1;
}

.field-item-main strong {
  display: block;
  overflow: hidden;
  color: #111827;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-item-main p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.field-item-switches {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #475569;
  font-size: 12px;
}

.detail-relations,
.detail-toggle-list {
  display: grid;
  gap: 12px;
}

.detail-toggle-row {
  justify-content: space-between;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px 14px;
}

.relation-link-card strong {
  color: #111827;
  font-size: 14px;
}

.relation-link-card p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.relation-link-form {
  margin-top: 10px;
}

.detail-shelf {
  display: grid;
  align-content: start;
  gap: 12px;
  border-left: 1px solid #e5e7eb;
  background: #fbfcfe;
  overflow: auto;
  padding: 12px;
}

.detail-shelf-tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.detail-shelf-tabs button {
  border: 0;
  border-radius: 4px;
  background: #f1f5f9;
  color: #475569;
  cursor: pointer;
  font-size: 12px;
  line-height: 28px;
  padding: 0 10px;
}

.detail-shelf-tabs button.active {
  background: #eaf2ff;
  color: #1d4ed8;
  font-weight: 700;
}

.detail-shelf-list {
  display: grid;
  gap: 8px;
}

.detail-shelf-item {
  display: grid;
  gap: 4px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  padding: 10px 12px;
  text-align: left;
}

.detail-shelf-item:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.detail-shelf-item strong {
  overflow: hidden;
  color: #111827;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-shelf-item span {
  color: #64748b;
  font-size: 12px;
}

.detail-shelf-item em {
  width: fit-content;
  border-radius: 4px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 11px;
  font-style: normal;
  line-height: 18px;
  padding: 0 6px;
}

@media (max-width: 1100px) {
  .detail-designer-body {
    grid-template-columns: 1fr;
  }

  .detail-shelf {
    border-left: 0;
    border-top: 1px solid #e5e7eb;
  }
}
</style>
