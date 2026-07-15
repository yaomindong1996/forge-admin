<template>
  <div class="model-designer" :class="{ 'fields-active': activeTab === 'fields' }">
    <div class="model-main">
      <div class="model-summary">
        <div>
          <div class="summary-kicker">
            {{ localModel.domain?.name || domain?.domainName || '未选择业务领域' }} / 数据模型
          </div>
          <h2>{{ localModel.object?.name || localModel.businessName || '未命名模型' }}</h2>
          <p>{{ localModel.object?.description || '维护模型基础信息、字段、关系和规则。' }}</p>
        </div>
        <div class="summary-metrics">
          <div>
            <strong>{{ businessFieldCount }}</strong>
            <span>业务字段</span>
          </div>
          <div>
            <strong>{{ systemFieldCount }}</strong>
            <span>系统字段</span>
          </div>
          <div>
            <strong>{{ requiredCount }}</strong>
            <span>必填</span>
          </div>
        </div>
      </div>

      <n-tabs v-model:value="activeTab" type="line" animated class="model-tabs">
        <n-tab-pane v-if="showBasicTab" name="basic" tab="模型基础信息">
          <section class="designer-section">
            <div class="section-head">
              <div>
                <strong>基础信息</strong>
                <span>定义业务对象、表名、模型类型和描述。</span>
              </div>
            </div>
            <n-form label-placement="top" size="small" class="basic-form" :show-feedback="false">
              <n-form-item label="模型名称">
                <n-input v-model:value="localModel.object.name" placeholder="例如：客户档案" />
              </n-form-item>
              <n-form-item label="模型编码 / 数据表名">
                <n-input
                  :value="localModel.object.code"
                  placeholder="tf_f_order"
                  @update:value="updateModelCode"
                />
              </n-form-item>
              <n-form-item label="业务名称">
                <n-input v-model:value="localModel.businessName" placeholder="例如：客户档案" />
              </n-form-item>
              <n-form-item label="应用类型">
                <n-select
                  v-model:value="localModel.appType"
                  :options="appTypeOptions"
                  @update:value="handleAppTypeChange"
                />
              </n-form-item>
              <n-form-item class="span-2" label="模型描述">
                <n-input
                  v-model:value="localModel.object.description"
                  type="textarea"
                  :autosize="{ minRows: 3, maxRows: 5 }"
                  placeholder="描述该模型承载的业务数据、状态流转和关键约束"
                />
              </n-form-item>
            </n-form>
          </section>
        </n-tab-pane>

        <n-tab-pane name="fields" tab="字段设计">
          <section class="designer-section">
            <div class="section-toolbar">
              <n-space>
                <n-button type="primary" @click="addField">
                  <template #icon>
                    <n-icon><AddOutline /></n-icon>
                  </template>
                  添加字段
                </n-button>
                <n-button :disabled="!fieldTemplateCount" @click="addDomainFieldTemplates">
                  引入领域字段模板
                </n-button>
              </n-space>
              <n-button :loading="validating" @click="validateModel">
                校验模型
              </n-button>
            </div>
            <div v-if="fieldTemplateCount" class="domain-hint">
              当前领域可引入 {{ fieldTemplateCount }} 个字段模板，ID、租户和审计字段由系统固定维护。
            </div>
            <ModelFieldTable
              :fields="localModel.fields"
              :selected-index="selectedIndex"
              @update:fields="handleFieldsUpdate"
              @select="selectedIndex = $event"
              @copy="copyField"
              @remove="removeField"
            />
            <ModelFieldPropertyPanel
              :field="currentField"
              :fields="localModel.fields"
              :domain="domain"
              class="field-property-panel"
              @update:field="handleFieldUpdate"
            />
          </section>
        </n-tab-pane>

        <n-tab-pane name="relations" tab="关联配置">
          <section class="designer-section">
            <div class="section-toolbar">
              <div>
                <div class="section-title">
                  业务对象关系
                </div>
                <p class="section-desc">
                  用于说明当前模型如何关联其它模型，常见场景是订单关联客户、明细关联主表。
                </p>
              </div>
              <n-button type="primary" @click="addRelation">
                添加关系
              </n-button>
            </div>
            <div class="relation-guide">
              <strong>配置示例：</strong>
              当前模型“订单”里有字段 customerId，目标模型选择“客户”，本模型字段选 customerId，关联对象字段选 id，回显字段选客户名称。
            </div>
            <div v-if="localModel.relations?.length" class="relation-list">
              <div v-for="(relation, index) in localModel.relations" :key="index" class="relation-row">
                <div class="relation-field">
                  <span>关系类型</span>
                  <n-select
                    :value="relation.relationType"
                    :options="relationTypeOptions"
                    size="small"
                    @update:value="updateRelation(index, { relationType: $event })"
                  />
                  <small>{{ relationTypeHint(relation.relationType) }}</small>
                </div>
                <div class="relation-field">
                  <span>关联对象</span>
                  <n-select
                    :value="relation.targetObjectCode"
                    size="small"
                    filterable
                    :options="targetModelOptions"
                    placeholder="选择要关联的模型"
                    @update:value="handleTargetModelChange(index, $event)"
                  />
                  <small>被当前模型引用，或承载子记录的模型。</small>
                </div>
                <div class="relation-field">
                  <span>本模型字段</span>
                  <n-select
                    :value="relation.sourceField"
                    :options="businessFieldOptions"
                    size="small"
                    placeholder="选择本模型里的关联字段"
                    @update:value="updateRelation(index, { sourceField: $event })"
                  />
                  <small>通常是 customerId、parentId、orderId 这类外键字段。</small>
                </div>
                <div class="relation-field">
                  <span>关联对象字段</span>
                  <n-select
                    :value="relation.targetField"
                    size="small"
                    filterable
                    :options="targetFieldOptions(relation)"
                    placeholder="通常选择 id"
                    @update:value="updateRelation(index, { targetField: $event })"
                  />
                  <small>本模型字段会与这个字段匹配，通常是关联对象的 id。</small>
                </div>
                <div class="relation-field">
                  <span>回显字段</span>
                  <n-select
                    :value="relation.displayField"
                    size="small"
                    filterable
                    clearable
                    :options="targetFieldOptions(relation)"
                    placeholder="选择列表/表单展示字段"
                    @update:value="updateRelation(index, { displayField: $event || '' })"
                  />
                  <small>页面上用于显示名称，如客户名称、部门名称，可不填。</small>
                </div>
                <div class="relation-action">
                  <n-button text size="small" class="text-error" @click="removeRelation(index)">
                    删除
                  </n-button>
                </div>
              </div>
            </div>
            <n-empty v-else description="暂无关联关系" />
          </section>
        </n-tab-pane>

        <n-tab-pane name="rules" tab="校验规则">
          <section class="designer-section rules-grid">
            <div class="rule-card">
              <div class="rule-card-title">
                数据策略
              </div>
              <n-form label-placement="top" size="small" :show-feedback="false">
                <n-form-item label="数据范围">
                  <n-select
                    v-model:value="localModel.policies.dataScope"
                    :options="dataScopeOptions"
                    @update:value="handleDataScopeChange"
                  />
                </n-form-item>
                <template v-if="isFollowSystemDataScope">
                  <n-form-item label="本人字段">
                    <n-select v-model:value="localModel.policies.userField" clearable :options="fieldOptions" @update:value="handlePolicyFieldChange('user', $event)" />
                  </n-form-item>
                  <n-form-item label="组织字段">
                    <n-select v-model:value="localModel.policies.orgField" clearable :options="fieldOptions" @update:value="handlePolicyFieldChange('org', $event)" />
                  </n-form-item>
                  <n-form-item label="区划字段">
                    <n-select v-model:value="localModel.policies.regionField" clearable :options="fieldOptions" @update:value="handlePolicyFieldChange('region', $event)" />
                  </n-form-item>
                </template>
                <n-form-item label="主键策略">
                  <n-input :value="`${localModel.policies.primaryKeyField || 'id'} / ${localModel.policies.primaryKeyStrategy || 'AUTO_INCREMENT'}`" disabled />
                </n-form-item>
                <n-form-item label="租户字段">
                  <n-input :value="`${localModel.policies.tenantField || 'tenantId'} / ${localModel.policies.tenantColumn || 'tenant_id'}`" disabled />
                </n-form-item>
                <n-form-item label="逻辑删除字段">
                  <n-input :value="`${localModel.policies.logicDeleteField || 'delFlag'} / ${localModel.policies.logicDeleteColumn || 'del_flag'}`" disabled />
                </n-form-item>
                <n-form-item label="审计字段">
                  <n-switch :value="true" disabled />
                </n-form-item>
              </n-form>
            </div>
            <div class="rule-card">
              <div class="rule-card-title">
                树形模型
              </div>
              <n-form label-placement="top" size="small" :show-feedback="false">
                <n-form-item label="启用树形数据">
                  <n-switch v-model:value="localModel.treeConfig.enabled" @update:value="handleTreeEnabledChange" />
                </n-form-item>
                <n-form-item label="主键字段">
                  <n-select v-model:value="localModel.treeConfig.keyField" :options="fieldOptions" />
                </n-form-item>
                <n-form-item label="父级字段">
                  <n-select v-model:value="localModel.treeConfig.parentField" :options="fieldOptions" />
                </n-form-item>
                <n-form-item label="显示字段">
                  <n-select v-model:value="localModel.treeConfig.labelField" :options="fieldOptions" />
                </n-form-item>
                <n-form-item label="加载方式">
                  <n-select v-model:value="localModel.treeConfig.loadMode" :options="treeLoadModeOptions" />
                </n-form-item>
              </n-form>
            </div>
          </section>
        </n-tab-pane>

        <n-tab-pane name="extensions" tab="扩展配置">
          <section class="designer-section">
            <div class="section-toolbar">
              <div>
                <div class="section-title">
                  索引配置
                </div>
                <p class="section-desc">
                  只会创建这里明确保存的普通索引或唯一索引，查询字段和关联字段不会自动建索引。
                </p>
              </div>
              <n-button type="primary" @click="addIndex">
                添加索引
              </n-button>
            </div>
            <div v-if="localModel.indexes?.length" class="index-list">
              <div v-for="(index, idx) in localModel.indexes" :key="idx" class="index-row">
                <n-input
                  :value="index.indexName"
                  size="small"
                  placeholder="索引名称，例如 idx_order_no"
                  @update:value="updateIndex(idx, { indexName: normalizeIndexName($event) })"
                />
                <n-select
                  :value="index.indexType || (index.unique ? 'UNIQUE' : 'NORMAL')"
                  size="small"
                  :options="indexTypeOptions"
                  @update:value="updateIndex(idx, { indexType: $event, unique: $event === 'UNIQUE' })"
                />
                <n-select
                  :value="index.fields || []"
                  multiple
                  filterable
                  size="small"
                  placeholder="选择索引字段，可多选"
                  :options="businessFieldOptions"
                  @update:value="updateIndex(idx, { fields: $event })"
                />
                <n-input
                  :value="index.remark"
                  size="small"
                  placeholder="说明"
                  @update:value="updateIndex(idx, { remark: $event })"
                />
                <n-button text size="small" class="text-error" @click="removeIndex(idx)">
                  删除
                </n-button>
              </div>
            </div>
            <n-empty v-else description="暂无自定义索引" />
          </section>
        </n-tab-pane>
      </n-tabs>
    </div>
    <ModelFieldPropertyPanel
      v-if="activeTab !== 'fields'"
      :field="currentField"
      :fields="localModel.fields"
      :domain="domain"
      class="model-side"
      @update:field="handleFieldUpdate"
    />
  </div>
</template>

<script setup>
import { AddOutline } from '@vicons/ionicons5'
import { computed, ref, watch } from 'vue'
import { lowcodeValidateModel } from '@/api/lowcode-crud'
import {
  appTypeOptions,
  cloneSchema,
  createDefaultField,
  createDefaultIndex,
  createFieldFromIndex,
  createFieldFromTemplate,
  ensureSystemFields,
  indexTypeOptions,
  isAuditField,
  isLockedSystemField,
  isSameSchema,
  isSystemField,
  normalizeLowcodePolicies,
  normalizeObjectCode,
  normalizeTableName,
} from './model-schema'
import ModelFieldPropertyPanel from './ModelFieldPropertyPanel.vue'
import ModelFieldTable from './ModelFieldTable.vue'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
  domain: {
    type: Object,
    default: null,
  },
  dataModels: {
    type: Array,
    default: () => [],
  },
  showBasicTab: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:modelValue', 'validated'])

const localModel = ref(cloneSchema(props.modelValue))
const selectedIndex = ref(0)
const validating = ref(false)
const activeTab = ref(props.showBasicTab ? 'basic' : 'fields')

const domainSchema = computed(() => props.domain?.domainSchema || {})
const currentField = computed(() => localModel.value.fields?.[selectedIndex.value] || null)
const businessFieldCount = computed(() => (localModel.value.fields || []).filter(field => !isSystemField(field)).length)
const systemFieldCount = computed(() => (localModel.value.fields || []).filter(field => isSystemField(field)).length)
const requiredCount = computed(() => (localModel.value.fields || []).filter(field => field.required && !isSystemField(field)).length)
const fieldTemplateCount = computed(() => (domainSchema.value.fieldTemplates || []).length)
const fieldOptions = computed(() => (localModel.value.fields || []).map(field => ({
  label: `${field.label || field.field} (${field.field})`,
  value: field.field,
})))
const treeLoadModeOptions = [
  { label: '全量加载', value: 'full' },
  { label: '懒加载', value: 'lazy' },
]
const businessFieldOptions = computed(() => (localModel.value.fields || [])
  .filter(field => !isSystemField(field))
  .map(field => ({
    label: `${field.label || field.field} (${field.field})`,
    value: field.field,
  })))
const targetModelOptions = computed(() => props.dataModels.map(model => ({
  label: `${model.modelName || model.modelCode} (${model.modelCode})`,
  value: model.modelCode,
})))
const relationTypeOptions = [
  { label: '引用', value: 'REFERENCE' },
  { label: '一对多', value: 'ONE_TO_MANY' },
  { label: '一对一', value: 'ONE_TO_ONE' },
]
const relationTypeHints = {
  REFERENCE: '当前模型保存对方 ID，如订单保存客户 ID。',
  ONE_TO_MANY: '当前模型是主表，对方模型是多条子记录。',
  ONE_TO_ONE: '当前模型与对方模型一条对一条扩展。',
}
const dataScopeOptions = [
  { label: '租户隔离', value: 'TENANT' },
  { label: '跟随系统角色', value: 'FOLLOW_SYSTEM' },
]
const isFollowSystemDataScope = computed(() => localModel.value.policies?.dataScope === 'FOLLOW_SYSTEM')

watch(
  () => props.modelValue,
  (value) => {
    if (isSameSchema(value, localModel.value))
      return
    localModel.value = cloneSchema(value)
    ensureModelCollections()
    if (selectedIndex.value >= localModel.value.fields.length)
      selectedIndex.value = Math.max(localModel.value.fields.length - 1, 0)
  },
  { deep: true },
)

watch(
  () => props.domain,
  (domain) => {
    if (!domain)
      return
    localModel.value.domain = {
      id: domain.id,
      code: domain.domainCode,
      name: domain.domainName,
    }
  },
  { deep: true },
)

watch(
  () => props.showBasicTab,
  (value) => {
    if (!value && activeTab.value === 'basic')
      activeTab.value = 'fields'
  },
)

watch(
  localModel,
  (value) => {
    if (!isSameSchema(value, props.modelValue))
      emit('update:modelValue', cloneSchema(value))
  },
  { deep: true },
)

ensureModelCollections()

function handleFieldsUpdate(fields) {
  const normalized = fields.map((field, index) => {
    const oldField = localModel.value.fields?.[index]
    if (oldField && oldField.field === field.field && oldField.label === field.label && oldField.columnName === field.columnName)
      return field
    return withDomainRecommendations(field)
  })
  localModel.value.fields = ensureSystemFields(normalized, true)
}

function handleFieldUpdate(field) {
  if (selectedIndex.value < 0)
    return
  const oldField = localModel.value.fields[selectedIndex.value]
  if (isLockedSystemField(oldField))
    return
  const changedName = oldField?.field !== field.field || oldField?.label !== field.label || oldField?.columnName !== field.columnName
  localModel.value.fields.splice(selectedIndex.value, 1, changedName ? withDomainRecommendations(field) : field)
  localModel.value.fields = ensureSystemFields(localModel.value.fields, true)
}

function addField() {
  const next = withDomainRecommendations(createFieldFromIndex((localModel.value.fields?.length || 0) + 1))
  const insertIndex = businessInsertIndex()
  localModel.value.fields.splice(insertIndex, 0, next)
  localModel.value.fields = ensureSystemFields(localModel.value.fields, true)
  selectedIndex.value = localModel.value.fields.findIndex(field => field.field === next.field)
  activeTab.value = 'fields'
}

function addDomainFieldTemplates() {
  const templates = domainSchema.value.fieldTemplates || []
  if (!templates.length) {
    window.$message?.warning('当前领域没有字段模板')
    return
  }
  const fields = localModel.value.fields || []
  let added = 0
  for (const template of templates) {
    const field = withDomainRecommendations(createFieldFromTemplate(template))
    if (isAuditField(field))
      continue
    if (fields.some(item => item.field === field.field || item.columnName === field.columnName))
      continue
    fields.push(field)
    added += 1
  }
  localModel.value.fields = fields
  if (added > 0) {
    localModel.value.fields = ensureSystemFields(fields, true)
    selectedIndex.value = Math.max(localModel.value.fields.findIndex(field => field.field === fields[fields.length - 1]?.field), 0)
    window.$message?.success(`已引入 ${added} 个领域字段`)
  }
  else {
    window.$message?.info('没有可新增的领域字段')
  }
}

function withDomainRecommendations(field) {
  const next = { ...field }
  const dictRecommendation = findRecommendation(domainSchema.value.dictRecommendations || [], next)
  if (dictRecommendation && !next.dictType)
    next.dictType = dictRecommendation.dictType

  const securityPolicy = findRecommendation(domainSchema.value.securityPolicies || [], next)
  if (securityPolicy) {
    if (!next.sensitiveType || next.sensitiveType === 'NONE')
      next.sensitiveType = securityPolicy.sensitiveType || 'NONE'
    if (!next.encryptAlgorithm)
      next.encryptAlgorithm = securityPolicy.encryptAlgorithm || ''
  }
  return next
}

function findRecommendation(items, field) {
  const text = `${field.field || ''} ${field.columnName || ''} ${field.label || ''}`.toLowerCase()
  return items.find((item) => {
    const pattern = String(item.fieldPattern || '').toLowerCase()
    return pattern && text.includes(pattern)
  })
}

function handleAppTypeChange(value) {
  if (value !== 'TREE')
    return
  ensureTreeModel()
}

function handleTreeEnabledChange(value) {
  if (value)
    ensureTreeModel()
}

function handleDataScopeChange() {
  normalizeLowcodePolicies(localModel.value)
}

function handlePolicyFieldChange(kind, fieldName) {
  const field = (localModel.value.fields || []).find(item => item.field === fieldName || item.columnName === fieldName)
  const columnName = field?.columnName || ''
  if (kind === 'user')
    localModel.value.policies.userColumn = columnName
  if (kind === 'org')
    localModel.value.policies.orgColumn = columnName
  if (kind === 'region')
    localModel.value.policies.regionColumn = columnName
}

function ensureTreeModel() {
  const fields = localModel.value.fields || []
  if (!localModel.value.treeConfig)
    localModel.value.treeConfig = {}

  const parentField = localModel.value.treeConfig.parentField || 'parentId'
  if (!fields.some(field => field.field === parentField)) {
    fields.push({
      ...createDefaultField(parentField, '上级节点'),
      dataType: 'bigint',
      componentType: 'treeSelect',
      queryType: 'eq',
      searchable: false,
      listVisible: false,
      formVisible: true,
      width: 120,
    })
    localModel.value.fields = fields
  }

  const labelField = fields.find(field => field.field === 'name')?.field
    || fields.find(field => field.field !== parentField)?.field
    || 'name'
  localModel.value.treeConfig = {
    enabled: true,
    keyField: localModel.value.treeConfig.keyField || 'id',
    parentField,
    labelField: localModel.value.treeConfig.labelField || labelField,
    childrenField: localModel.value.treeConfig.childrenField || 'children',
    treeTitle: localModel.value.treeConfig.treeTitle || `${localModel.value.businessName || '业务'}树`,
    loadMode: localModel.value.treeConfig.loadMode || 'full',
  }
}

function copyField(index) {
  const source = localModel.value.fields[index]
  if (isLockedSystemField(source)) {
    window.$message?.warning('系统字段不能复制')
    return
  }
  const copy = cloneSchema(source)
  copy.field = `${source.field}Copy`
  copy.columnName = `${source.columnName}_copy`
  copy.label = `${source.label}副本`
  localModel.value.fields.splice(Math.min(index + 1, businessInsertIndex()), 0, copy)
  localModel.value.fields = ensureSystemFields(localModel.value.fields, true)
  selectedIndex.value = Math.max(localModel.value.fields.findIndex(field => field.field === copy.field), 0)
}

function removeField(index) {
  if (isLockedSystemField(localModel.value.fields[index])) {
    window.$message?.warning('系统字段不能删除')
    return
  }
  localModel.value.fields.splice(index, 1)
  localModel.value.fields = ensureSystemFields(localModel.value.fields, true)
  selectedIndex.value = Math.max(Math.min(selectedIndex.value, localModel.value.fields.length - 1), 0)
}

function addRelation() {
  localModel.value.relations.push({
    relationType: 'REFERENCE',
    targetObjectCode: '',
    sourceField: currentField.value?.field || '',
    targetField: 'id',
    displayField: '',
  })
  activeTab.value = 'relations'
}

function handleTargetModelChange(index, modelCode) {
  const targetModel = props.dataModels.find(model => model.modelCode === modelCode)
  const targetFields = targetModel?.modelSchema?.fields || []
  const idField = targetFields.find(field => field.field === 'id')?.field
  const firstField = targetFields[0]?.field || ''
  const nameField = targetFields.find(field => ['name', 'title', 'label'].includes(field.field))?.field
    || targetFields.find(field => field.field !== idField)?.field
    || ''
  updateRelation(index, {
    targetObjectCode: modelCode || '',
    targetField: idField || firstField,
    displayField: nameField,
  })
}

function updateRelation(index, patch) {
  localModel.value.relations.splice(index, 1, {
    ...localModel.value.relations[index],
    ...patch,
  })
}

function removeRelation(index) {
  localModel.value.relations.splice(index, 1)
}

function addIndex() {
  localModel.value.indexes.push(createDefaultIndex((localModel.value.indexes || []).length + 1))
}

function updateIndex(index, patch) {
  localModel.value.indexes.splice(index, 1, {
    ...localModel.value.indexes[index],
    ...patch,
  })
}

function removeIndex(index) {
  localModel.value.indexes.splice(index, 1)
}

function updateModelCode(value) {
  const code = normalizeObjectCode(value)
  localModel.value.object.code = code
  localModel.value.tableName = normalizeTableName(code)
}

function targetFieldOptions(relation) {
  const targetModel = props.dataModels.find(model => model.modelCode === relation?.targetObjectCode)
  return (targetModel?.modelSchema?.fields || []).map(field => ({
    label: `${field.label || field.field} (${field.field})`,
    value: field.field,
  }))
}

function relationTypeHint(type) {
  return relationTypeHints[type] || '选择当前模型与关联对象的业务关系。'
}

function businessInsertIndex() {
  const fields = localModel.value.fields || []
  const index = fields.findIndex((field, fieldIndex) => fieldIndex > 0 && isSystemField(field))
  return index >= 0 ? index : fields.length
}

function normalizeIndexName(value) {
  return String(value || '')
    .trim()
    .replace(/\W/g, '_')
    .replace(/_+/g, '_')
    .toLowerCase()
    .replace(/^[^a-z]+/, '')
    .slice(0, 64)
}

function ensureModelCollections() {
  if (!localModel.value.schemaVersion)
    localModel.value.schemaVersion = 2
  if (!localModel.value.domain)
    localModel.value.domain = { id: null, code: '', name: '' }
  if (!localModel.value.object)
    localModel.value.object = { code: '', name: localModel.value.businessName || '', description: '' }
  if (!localModel.value.fields)
    localModel.value.fields = []
  localModel.value.fields = ensureSystemFields(localModel.value.fields, true)
  if (!localModel.value.relations)
    localModel.value.relations = []
  if (!localModel.value.indexes)
    localModel.value.indexes = []
  else
    localModel.value.indexes = localModel.value.indexes.filter(index => index?.auto !== true)
  normalizeLowcodePolicies(localModel.value)
  if (!localModel.value.tableName && localModel.value.object?.code)
    localModel.value.tableName = normalizeTableName(localModel.value.object.code)
  if (!localModel.value.treeConfig) {
    localModel.value.treeConfig = {
      keyField: 'id',
      parentField: 'parentId',
      labelField: 'name',
      childrenField: 'children',
      treeTitle: '树形导航',
      loadMode: 'full',
      enabled: false,
    }
  }
  if (localModel.value.treeConfig.enabled === undefined)
    localModel.value.treeConfig.enabled = localModel.value.appType === 'TREE'
  if (!localModel.value.treeConfig.loadMode)
    localModel.value.treeConfig.loadMode = 'full'
}

async function validateModel() {
  validating.value = true
  try {
    await lowcodeValidateModel(localModel.value)
    window.$message?.success('模型校验通过')
    emit('validated')
  }
  catch (e) {
    window.$message?.error(e?.message || '模型校验失败')
  }
  finally {
    validating.value = false
  }
}
</script>

<style scoped>
.model-designer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 12px;
  min-height: calc(100vh - 226px);
}

.model-designer.fields-active {
  grid-template-columns: minmax(0, 1fr);
}

.model-main {
  min-width: 0;
}

.field-property-panel {
  height: auto;
  margin-top: 12px;
}

.field-property-panel :deep(.field-form) {
  max-height: none;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
}

.field-property-panel :deep(.form-section) {
  border-bottom: 0;
  padding-bottom: 0;
}

.model-summary,
.designer-section {
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
}

.model-summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
  padding: 14px 16px;
}

.summary-kicker {
  color: #2563eb;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 700;
}

.model-summary h2 {
  margin: 4px 0 0;
  color: #0f172a;
  font-size: 18px;
}

.model-summary p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
}

.summary-metrics {
  display: grid;
  grid-template-columns: repeat(3, 72px);
  gap: 8px;
}

.summary-metrics div {
  display: grid;
  gap: 2px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  padding: 8px;
  text-align: center;
}

.summary-metrics strong {
  color: #1d4ed8;
  font-size: 18px;
  line-height: 1;
}

.summary-metrics span {
  color: #64748b;
  font-size: 11px;
}

.model-tabs {
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  padding: 0 12px 12px;
}

.designer-section {
  padding: 12px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-head div {
  display: grid;
  gap: 3px;
}

.section-head strong,
.section-title,
.rule-card-title,
.placeholder-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.section-head span {
  color: #64748b;
  font-size: 12px;
}

.section-desc {
  margin: 3px 0 0;
  color: #64748b;
  font-size: 12px;
}

.basic-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.span-2 {
  grid-column: 1 / -1;
}

.section-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.domain-hint {
  margin-bottom: 10px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  color: #1e40af;
  font-size: 12px;
  padding: 8px 10px;
}

.relation-guide {
  margin-bottom: 10px;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  background: #f0fdf4;
  color: #166534;
  font-size: 12px;
  line-height: 1.6;
  padding: 8px 10px;
}

.relation-list {
  display: grid;
  max-height: 460px;
  gap: 8px;
  overflow-y: auto;
  padding-right: 4px;
}

.relation-row {
  display: grid;
  grid-template-columns: 132px minmax(160px, 1.1fr) minmax(160px, 1.1fr) minmax(160px, 1.1fr) minmax(160px, 1.1fr) 52px;
  gap: 10px;
  align-items: start;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  padding: 8px;
}

.relation-field {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.relation-field > span {
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}

.relation-field > small {
  min-height: 34px;
  color: #64748b;
  font-size: 11px;
  line-height: 1.45;
}

.relation-action {
  display: flex;
  align-items: center;
  min-height: 58px;
}

.er-section {
  overflow: hidden;
}

.er-board {
  display: grid;
  grid-template-columns: minmax(280px, 0.92fr) 180px minmax(300px, 1.08fr);
  gap: 14px;
  min-height: 420px;
  overflow: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: linear-gradient(#f8fafc 31px, transparent 32px), linear-gradient(90deg, #f8fafc 31px, transparent 32px);
  background-color: #ffffff;
  background-size: 32px 32px;
  padding: 18px;
}

.er-column,
.er-related-list {
  display: grid;
  align-content: start;
  gap: 12px;
  min-width: 0;
}

.primary-column {
  align-content: center;
}

.er-table-card {
  overflow: hidden;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.08);
}

.er-table-card.primary {
  border-color: #2563eb;
}

.er-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 10px 12px;
}

.er-card-head div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.er-card-head strong,
.er-card-head span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.er-card-head strong {
  color: #0f172a;
  font-size: 13px;
}

.er-card-head span {
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.er-field-list {
  display: grid;
}

.er-field-row {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 76px;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  border-bottom: 1px solid #eef2f7;
  padding: 0 10px;
}

.er-field-row:last-child {
  border-bottom: 0;
}

.er-field-row.muted {
  background: #fbfdff;
  color: #94a3b8;
}

.er-badge {
  display: inline-flex;
  height: 18px;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  background: #e2e8f0;
  color: #475569;
  font-size: 10px;
  font-weight: 800;
}

.er-badge.pk {
  background: #dbeafe;
  color: #1d4ed8;
}

.er-badge.fk,
.er-badge.ref {
  background: #dcfce7;
  color: #15803d;
}

.er-badge.sys {
  background: #f1f5f9;
  color: #64748b;
}

.er-field-name,
.er-field-type {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.er-field-name {
  color: #0f172a;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.er-field-type {
  justify-self: end;
  color: #64748b;
  font-size: 11px;
}

.er-link-column {
  display: grid;
  align-items: center;
  min-width: 0;
}

.er-link-stack {
  display: grid;
  gap: 14px;
}

.er-link {
  display: grid;
  grid-template-columns: minmax(18px, 1fr) auto;
  gap: 5px;
  align-items: center;
  color: #1d4ed8;
  font-size: 11px;
  text-align: center;
}

.er-link strong,
.er-link small {
  grid-column: 1 / -1;
}

.er-line {
  display: block;
  grid-column: 1 / -1;
  height: 2px;
  border-radius: 999px;
  background: #60a5fa;
}

.er-no-link {
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  color: #64748b;
  font-size: 12px;
  padding: 14px;
  text-align: center;
}

.index-list {
  display: grid;
  gap: 8px;
}

.index-row {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) 112px minmax(220px, 1.6fr) minmax(120px, 1fr) 52px;
  gap: 8px;
  align-items: center;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  padding: 8px;
}

.rules-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.rule-card {
  border: 1px solid #eef2f7;
  border-radius: 8px;
  padding: 12px;
}

.rule-card-title {
  margin-bottom: 10px;
}

.placeholder-board {
  display: grid;
  gap: 12px;
}

.placeholder-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.placeholder-grid div {
  min-height: 72px;
  border: 1px dashed #bfdbfe;
  border-radius: 8px;
  background: #f8fbff;
  color: #1e40af;
  font-size: 13px;
  font-weight: 700;
  padding: 12px;
}

.model-side {
  min-height: calc(100vh - 226px);
}

@media (max-width: 1260px) {
  .model-designer {
    grid-template-columns: 1fr;
  }

  .relation-row {
    grid-template-columns: 1fr;
  }

  .index-row {
    grid-template-columns: 1fr;
  }

  .er-board {
    grid-template-columns: 1fr;
  }

  .rules-grid,
  .placeholder-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .model-summary {
    flex-direction: column;
  }

  .summary-metrics,
  .basic-form {
    width: 100%;
    grid-template-columns: 1fr;
  }

  .section-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
