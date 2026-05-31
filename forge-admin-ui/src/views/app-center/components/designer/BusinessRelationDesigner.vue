<template>
  <div class="business-relation-designer">
    <div class="relation-designer-head">
      <div>
        <h3>关系与级联</h3>
        <p>维护对象关系、字段联动和运行态过滤规则。</p>
      </div>
      <n-space size="small">
        <n-button size="small" secondary @click="loadRelations">
          刷新
        </n-button>
        <n-button v-if="activeTab === 'relations'" size="small" secondary @click="addRelation">
          新增关系
        </n-button>
        <n-button v-if="activeTab === 'linkage'" size="small" secondary @click="addLinkageRule">
          新增级联
        </n-button>
        <n-button size="small" type="primary" :loading="saving" @click="saveRelations">
          保存配置
        </n-button>
      </n-space>
    </div>

    <n-tabs v-model:value="activeTab" type="line" class="relation-tabs">
      <n-tab-pane name="relations" tab="对象关系">
        <div class="relation-designer-body">
          <main class="relation-list-pane">
            <n-spin :show="loading">
              <div v-if="localRelations.length" class="relation-card-list">
                <section v-for="(relation, index) in localRelations" :key="relation.clientKey" class="relation-card">
                  <header class="relation-card-head">
                    <div>
                      <strong>{{ relation.relationName || relationLabel(relation) }}</strong>
                      <p>{{ relationSentence(relation) }}</p>
                    </div>
                    <n-space size="small">
                      <n-tag size="small" :type="relation.status === 0 ? 'default' : 'success'" :bordered="false">
                        {{ relation.status === 0 ? '停用' : '启用' }}
                      </n-tag>
                      <n-tag v-if="relation.inlineEditEnabled" size="small" type="info" :bordered="false">
                        编辑内嵌
                      </n-tag>
                      <n-tag v-if="relation.inlineCreateEnabled" size="small" type="success" :bordered="false">
                        新增内嵌
                      </n-tag>
                      <n-popconfirm @positive-click="removeRelation(index)">
                        <template #trigger>
                          <n-button quaternary circle size="small">
                            <template #icon>
                              <n-icon><TrashOutline /></n-icon>
                            </template>
                          </n-button>
                        </template>
                        确认移除该关系？
                      </n-popconfirm>
                    </n-space>
                  </header>

                  <n-form label-placement="top" :show-feedback="false" size="small" class="relation-form">
                    <n-grid :cols="3" :x-gap="12" :y-gap="4" responsive="screen">
                      <n-form-item-gi label="业务关系">
                        <n-select
                          v-model:value="relation.relationType"
                          :options="relationTypeOptions"
                          @update:value="value => updateRelationType(relation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="目标对象">
                        <n-select
                          v-model:value="relation.targetObjectCode"
                          :options="targetObjectOptions"
                          filterable
                          placeholder="选择目标对象"
                          @update:value="value => updateTargetObject(relation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="页面显示名称">
                        <n-input v-model:value="relation.relationName" placeholder="例如：客户有多个联系人" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi :label="sourceFieldLabel(relation)">
                        <n-select
                          v-model:value="relation.sourceFieldCode"
                          :options="sourceFieldOptions"
                          clearable
                          filterable
                          :placeholder="sourceFieldPlaceholder(relation)"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi :label="targetFieldLabel(relation)">
                        <n-select
                          v-model:value="relation.targetFieldCode"
                          :options="targetFieldOptions(relation)"
                          :loading="targetFieldLoadingMap[relation.targetObjectCode]"
                          clearable
                          filterable
                          :placeholder="targetFieldPlaceholder(relation)"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi :label="displayFieldLabel(relation)">
                        <n-select
                          v-model:value="relation.displayField"
                          :options="targetDisplayFieldOptions(relation)"
                          :loading="targetFieldLoadingMap[relation.targetObjectCode]"
                          clearable
                          filterable
                          placeholder="选择列表中显示的名称字段"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="详情页签">
                        <n-input v-model:value="relation.detailTabTitle" placeholder="例如：联系人" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi label="详情展示">
                        <n-switch
                          :value="relation.showInDetail !== false"
                          @update:value="value => updateShowInDetail(relation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="新增表单维护">
                        <n-switch
                          :value="relation.inlineCreateEnabled === true"
                          :disabled="!canInlineEdit(relation)"
                          @update:value="value => updateInlineCreate(relation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="编辑表单维护">
                        <n-switch
                          :value="relation.inlineEditEnabled === true"
                          :disabled="!canInlineEdit(relation)"
                          @update:value="value => updateInlineEdit(relation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="启用状态">
                        <n-switch
                          :value="relation.status !== 0"
                          @update:value="value => updateStatus(relation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="排序">
                        <n-input-number v-model:value="relation.sortOrder" :min="0" style="width: 100%" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi label="默认筛选">
                        <n-input v-model:value="relation.defaultFilter" placeholder="可选，运行态自动带入" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi :span="3" label="说明">
                        <n-input
                          v-model:value="relation.description"
                          type="textarea"
                          :rows="3"
                          placeholder="描述这条业务关系的使用场景"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                    </n-grid>
                  </n-form>
                </section>
              </div>
              <n-empty v-else-if="!loading" description="暂无关系配置" />
            </n-spin>
          </main>

          <aside class="relation-summary-pane">
            <section>
              <h4>业务句子</h4>
              <p>关系会以业务语言进入详情页签和发布检查。</p>
              <div class="sentence-list">
                <span v-for="relation in localRelations" :key="relation.clientKey">
                  {{ relationSentence(relation) }}
                </span>
              </div>
            </section>
            <section>
              <h4>发布关注</h4>
              <ul>
                <li>目标对象必须存在。</li>
                <li>本对象匹配字段必须存在。</li>
                <li>目标对象所属字段必须从字段列表选择。</li>
                <li>目标对象未发布时会在发布检查中提示。</li>
                <li>需要在新增或编辑表单维护关联数据时，关系类型应使用“拥有多条”或“明细”。</li>
              </ul>
            </section>
          </aside>
        </div>
      </n-tab-pane>

      <n-tab-pane name="linkage" tab="级联规则">
        <div class="relation-designer-body">
          <main class="relation-list-pane">
            <div v-if="localLinkage.rules.length" class="relation-card-list">
              <section v-for="(rule, index) in localLinkage.rules" :key="rule.ruleId" class="relation-card linkage-card">
                <header class="relation-card-head">
                  <div>
                    <strong>{{ linkageRuleLabel(rule) }}</strong>
                    <p>{{ linkageRuleSentence(rule) }}</p>
                  </div>
                  <n-space size="small">
                    <n-tag size="small" :type="rule.enabled === false ? 'default' : 'success'" :bordered="false">
                      {{ rule.enabled === false ? '停用' : '启用' }}
                    </n-tag>
                    <n-popconfirm @positive-click="removeLinkageRule(index)">
                      <template #trigger>
                        <n-button quaternary circle size="small">
                          <template #icon>
                            <n-icon><TrashOutline /></n-icon>
                          </template>
                        </n-button>
                      </template>
                      确认移除该级联规则？
                    </n-popconfirm>
                  </n-space>
                </header>

                <n-form label-placement="top" :show-feedback="false" size="small" class="relation-form">
                  <n-grid :cols="3" :x-gap="12" :y-gap="4" responsive="screen">
                    <n-form-item-gi label="规则类型">
                      <n-select
                        v-model:value="rule.type"
                        :options="linkageTypeOptions"
                        @update:value="value => updateLinkageType(rule, value)"
                      />
                    </n-form-item-gi>
                    <n-form-item-gi label="上级字段">
                      <n-select
                        v-model:value="rule.sourceField"
                        :options="sourceFieldOptions"
                        clearable
                        filterable
                        placeholder="选择控制字段"
                        @update:value="value => updateLinkageSource(rule, value)"
                      />
                    </n-form-item-gi>
                    <n-form-item-gi label="目标字段">
                      <n-select
                        v-model:value="rule.targetField"
                        :options="linkageTargetFieldOptions(rule)"
                        clearable
                        filterable
                        placeholder="选择被过滤字段"
                        @update:value="value => updateLinkageTarget(rule, value)"
                      />
                    </n-form-item-gi>

                    <template v-if="rule.dataSourceType === 'dict'">
                      <n-form-item-gi label="上级字典类型">
                        <n-input v-model:value="rule.dictConfig.sourceDictType" placeholder="例如：sys_dict_type_a" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi label="目标字典类型">
                        <n-input v-model:value="rule.dictConfig.targetDictType" placeholder="例如：sys_dict_type_b" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi v-if="rule.type === 'linkedDict'" label="关联字典类型">
                        <n-input v-model:value="rule.dictConfig.linkedDictType" placeholder="匹配 linked_dict_type" @update:value="markDirty" />
                      </n-form-item-gi>
                    </template>

                    <template v-else>
                      <n-form-item-gi label="请求参数名">
                        <n-input v-model:value="rule.remoteConfig.paramName" placeholder="例如：orgId / parentId" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi label="远程接口">
                        <n-input v-model:value="rule.remoteConfig.url" placeholder="get@/api/xxx/options" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi label="请求方式">
                        <n-select v-model:value="rule.remoteConfig.method" :options="methodOptions" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi v-if="rule.type === 'objectReference'" label="目标对象">
                        <n-select
                          v-model:value="rule.objectConfig.targetObjectCode"
                          :options="targetObjectOptions"
                          clearable
                          filterable
                          placeholder="选择目标对象"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                    </template>

                    <n-form-item-gi label="上级为空">
                      <n-select
                        v-model:value="rule.emptyStrategy"
                        :options="emptyStrategyOptions"
                        @update:value="markDirty"
                      />
                    </n-form-item-gi>
                    <n-form-item-gi label="上级变化清空">
                      <n-switch
                        :value="rule.clearOnSourceChange !== false"
                        @update:value="value => updateRuleClearOnChange(rule, value)"
                      />
                    </n-form-item-gi>
                    <n-form-item-gi label="启用状态">
                      <n-switch
                        :value="rule.enabled !== false"
                        @update:value="value => updateRuleEnabled(rule, value)"
                      />
                    </n-form-item-gi>
                  </n-grid>
                </n-form>
              </section>
            </div>
            <n-empty v-else description="暂无级联规则" />
          </main>

          <aside class="relation-summary-pane">
            <section>
              <h4>规则摘要</h4>
              <div class="metric-list">
                <span>全部 {{ localLinkage.rules.length }}</span>
                <span>字典 {{ linkageStats.dict }}</span>
                <span>远程 {{ linkageStats.remote }}</span>
                <span>停用 {{ linkageStats.disabled }}</span>
              </div>
            </section>
            <section>
              <h4>发布关注</h4>
              <ul>
                <li>上级字段和目标字段必须存在。</li>
                <li>字典级联必须配置目标字典类型。</li>
                <li>远程级联必须配置请求参数名和接口。</li>
                <li>保存后规则会同步到目标字段运行属性。</li>
              </ul>
            </section>
          </aside>
        </div>
      </n-tab-pane>
    </n-tabs>
  </div>
</template>

<script setup>
import { TrashOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import {
  businessObjectDesigner,
  businessObjectList,
  businessObjectRelations,
  saveBusinessObjectDesigner,
} from '@/api/business-app'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import {
  applyLinkageSchemaToFields,
  createLinkageSchemaFromFields,
  normalizeLinkageSchema,
  validateLinkageSchema,
} from './form-first/linkageSchema'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  suiteCode: {
    type: String,
    default: '',
  },
  objectCode: {
    type: String,
    default: '',
  },
  objectName: {
    type: String,
    default: '',
  },
  fields: {
    type: Array,
    default: () => [],
  },
  linkageSchema: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['updated', 'dirtyChange', 'fieldsUpdated', 'update:linkageSchema'])

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const activeTab = ref('relations')
const businessObjects = ref([])
const localRelations = ref([])
const localLinkage = ref(normalizeLinkageSchema())
const targetFieldsMap = ref({})
const targetFieldLoadingMap = ref({})
const relationsLoaded = ref(false)
const relationDirty = ref(false)
let resettingLinkage = false

const relationTypeOptions = [
  { label: '属于目标对象', value: 'REFERENCE' },
  { label: '拥有多条目标记录', value: 'CHILD_LIST' },
  { label: '包含明细记录', value: 'DETAIL' },
  { label: '当前对象与目标对象多对多关联', value: 'MANY_TO_MANY' },
]

const linkageTypeOptions = [
  { label: '字典父子(parent_dict_code)', value: 'parentDictCode' },
  { label: '关联字典(linked_dict_type/value)', value: 'linkedDict' },
  { label: '远程参数过滤', value: 'remoteParam' },
  { label: '组织范围过滤', value: 'orgScope' },
  { label: '对象引用过滤', value: 'objectReference' },
]

const emptyStrategyOptions = [
  { label: '显示空选项', value: 'empty' },
  { label: '显示全部选项', value: 'all' },
  { label: '禁用目标字段', value: 'disabled' },
]

const methodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
]

const sourceFieldOptions = computed(() => {
  const fields = (props.fields || []).map(toPageField).filter(field => field.field && !isInactiveField(field))
  return fields.map(field => ({
    label: businessFieldLabel(field),
    value: field.field,
  }))
})

const targetObjectOptions = computed(() => {
  const objects = (businessObjects.value || []).filter(item => item.objectCode !== props.objectCode)
  return objects.map(item => ({
    label: `${item.objectName || item.objectCode}（${item.objectCode}）`,
    value: item.objectCode,
  }))
})

const objectNameMap = computed(() => new Map((businessObjects.value || []).map(item => [item.objectCode, item.objectName || item.objectCode])))
const fieldMap = computed(() => new Map((props.fields || []).map(field => [field.fieldCode || field.field, toPageField(field)]).filter(([fieldCode]) => fieldCode)))
const linkageStats = computed(() => {
  const rules = localLinkage.value?.rules || []
  return {
    dict: rules.filter(rule => rule.dataSourceType === 'dict').length,
    remote: rules.filter(rule => ['remote', 'org', 'object'].includes(rule.dataSourceType)).length,
    disabled: rules.filter(rule => rule.enabled === false).length,
  }
})

watch(() => props.objectId, () => {
  loadRelations()
}, { immediate: true })

watch(() => props.suiteCode, () => {
  loadBusinessObjects()
}, { immediate: true })

watch(
  () => [props.linkageSchema, props.fields],
  () => resetLinkageSchema(),
  { immediate: true, deep: true },
)

onMounted(() => {
  loadBusinessObjects()
})

async function loadRelations() {
  if (!props.objectId) {
    localRelations.value = []
    relationsLoaded.value = true
    relationDirty.value = false
    return
  }
  loading.value = true
  relationsLoaded.value = false
  relationDirty.value = false
  try {
    const res = await businessObjectRelations(props.objectId)
    localRelations.value = (res.data || [])
      .filter(relation => !props.objectCode || !relation.sourceObjectCode || relation.sourceObjectCode === props.objectCode)
      .map(normalizeRelation)
    relationsLoaded.value = true
    relationDirty.value = false
    emit('updated', localRelations.value)
    localRelations.value.forEach(relation => loadTargetFields(relation.targetObjectCode))
  }
  catch (error) {
    message.error(error?.message || '关系配置加载失败')
  }
  finally {
    loading.value = false
  }
}

async function loadBusinessObjects() {
  try {
    const res = await businessObjectList({
      suiteCode: props.suiteCode || undefined,
    })
    businessObjects.value = res.data || []
    localRelations.value.forEach(relation => loadTargetFields(relation.targetObjectCode))
  }
  catch {
    businessObjects.value = []
  }
}

function resetLinkageSchema() {
  resettingLinkage = true
  try {
    const next = createLinkageSchemaFromFields(props.fields || [], props.linkageSchema || {})
    if (!isSameSchema(next, localLinkage.value))
      localLinkage.value = cloneSchema(next)
  }
  finally {
    resettingLinkage = false
  }
}

async function addRelation() {
  const target = targetObjectOptions.value[0]
  if (!target) {
    message.warning('当前套件没有可关联的目标对象')
    return
  }
  const targetName = objectNameMap.value.get(target.value) || target.label
  await loadTargetFields(target.value)
  const relationType = inferDefaultRelationType(target.value)
  const sourceFieldCode = firstSourceField(relationType, target.value)
  const targetFieldCode = firstTargetField(target.value, relationType)
  const relation = {
    clientKey: createClientKey(),
    relationType,
    targetObjectCode: target.value,
    relationName: relationLabel({ relationType, targetObjectCode: target.value }),
    sourceFieldCode,
    targetFieldCode,
    displayField: firstDisplayField(target.value, targetFieldCode),
    detailTabTitle: targetName,
    showInDetail: true,
    inlineCreateEnabled: canInlineEdit({ relationType }),
    inlineEditEnabled: canInlineEdit({ relationType }),
    defaultFilter: '',
    description: '',
    status: 1,
    sortOrder: localRelations.value.length * 10 + 10,
  }
  localRelations.value.push(relation)
  markRelationDirty()
}

function addLinkageRule() {
  const source = sourceFieldOptions.value[0]
  const target = sourceFieldOptions.value.find(item => item.value !== source?.value) || sourceFieldOptions.value[1]
  if (!source || !target) {
    message.warning('至少需要两个可用字段才能配置级联')
    return
  }
  const rule = normalizeLinkageRuleDraft({
    ruleId: createLinkageRuleId(source.value, target.value),
    type: inferLinkageType(target.value),
    sourceField: source.value,
    targetField: target.value,
  })
  localLinkage.value = normalizeLinkageSchema({
    ...localLinkage.value,
    rules: [
      ...(localLinkage.value.rules || []),
      rule,
    ],
  })
  syncLinkageModel(true)
  activeTab.value = 'linkage'
}

function removeRelation(index) {
  localRelations.value.splice(index, 1)
  markRelationDirty()
}

function removeLinkageRule(index) {
  const rules = [...(localLinkage.value.rules || [])]
  rules.splice(index, 1)
  localLinkage.value = normalizeLinkageSchema({
    ...localLinkage.value,
    rules,
  })
  syncLinkageModel(true)
}

function updateRelationType(relation, value) {
  relation.relationType = value
  if (!canInlineEdit(relation)) {
    relation.inlineCreateEnabled = false
    relation.inlineEditEnabled = false
  }
  relation.sourceFieldCode = firstSourceField(value, relation.targetObjectCode, relation.sourceFieldCode)
  relation.targetFieldCode = firstTargetField(relation.targetObjectCode, value, relation.targetFieldCode)
  if (!relation.displayField)
    relation.displayField = firstDisplayField(relation.targetObjectCode, relation.targetFieldCode)
  relation.relationName = relationLabel(relation)
  markDirty()
}

async function updateTargetObject(relation, value) {
  relation.targetObjectCode = value
  await loadTargetFields(value)
  const targetName = objectNameMap.value.get(value) || value
  relation.detailTabTitle = relation.detailTabTitle || targetName
  relation.relationName = relationLabel(relation)
  relation.sourceFieldCode = firstSourceField(relation.relationType, value, relation.sourceFieldCode)
  relation.targetFieldCode = firstTargetField(value, relation.relationType)
  relation.displayField = firstDisplayField(value, relation.targetFieldCode)
  markDirty()
}

function updateStatus(relation, value) {
  relation.status = value ? 1 : 0
  markDirty()
}

function updateShowInDetail(relation, value) {
  relation.showInDetail = !!value
  markDirty()
}

function updateInlineEdit(relation, value) {
  relation.inlineEditEnabled = canInlineEdit(relation) && !!value
  markDirty()
}

function updateInlineCreate(relation, value) {
  relation.inlineCreateEnabled = canInlineEdit(relation) && !!value
  markDirty()
}

function updateLinkageType(rule, value) {
  const next = normalizeLinkageRuleDraft({
    ...rule,
    type: value,
    matchMode: value,
    dataSourceType: resolveLinkageDataSourceType(value),
  })
  Object.assign(rule, next)
  fillRuleFieldDefaults(rule)
  syncLinkageModel(true)
}

function updateLinkageSource(rule, value) {
  rule.sourceField = value || ''
  fillRuleFieldDefaults(rule)
  syncLinkageModel(true)
}

function updateLinkageTarget(rule, value) {
  rule.targetField = value || ''
  fillRuleFieldDefaults(rule)
  syncLinkageModel(true)
}

function updateRuleClearOnChange(rule, value) {
  rule.clearOnSourceChange = !!value
  syncLinkageModel(true)
}

function updateRuleEnabled(rule, value) {
  rule.enabled = !!value
  syncLinkageModel(true)
}

async function saveRelations() {
  if (!props.objectId)
    return
  const shouldSaveRelations = activeTab.value === 'relations' || relationDirty.value
  if (shouldSaveRelations) {
    if (!relationsLoaded.value) {
      message.warning('关系配置还没有加载完成，请刷新后再保存')
      return
    }
    const invalidRelation = localRelations.value.find(relation => !relation.targetObjectCode || !relation.sourceFieldCode || !relation.targetFieldCode)
    if (invalidRelation) {
      message.warning(`请先补全「${invalidRelation.relationName || relationLabel(invalidRelation)}」的关联对象和匹配字段`)
      return
    }
    const missingDisplayRelation = localRelations.value.find(relation => relation.relationType === 'REFERENCE' && !relation.displayField)
    if (missingDisplayRelation) {
      message.warning(`请先为「${missingDisplayRelation.relationName || relationLabel(missingDisplayRelation)}」选择目标对象回显字段`)
      return
    }
  }
  saving.value = true
  try {
    const linkageSchema = normalizeLinkageSchema(localLinkage.value)
    const validation = validateLinkageSchema(linkageSchema, sourceFieldOptions.value.map(item => item.value))
    if (!validation.valid) {
      activeTab.value = 'linkage'
      message.warning(validation.errors[0]?.message || '请先修复级联规则')
      return
    }
    const fields = applyLinkageSchemaToFields(props.fields || [], linkageSchema)
    const designerPayload = {
      fields: fields.map(toFieldPayload),
      linkageSchema,
    }
    if (shouldSaveRelations)
      designerPayload.relations = localRelations.value.map(toRelationPayload)
    await saveBusinessObjectDesigner(props.objectId, designerPayload)
    message.success('关系与级联配置已保存')
    relationDirty.value = false
    emit('fieldsUpdated', fields)
    emit('update:linkageSchema', linkageSchema)
    emit('dirtyChange', false)
    await loadRelations()
  }
  finally {
    saving.value = false
  }
}

function normalizeRelation(relation = {}) {
  const config = parseRelationConfig(relation.relationConfig)
  return {
    ...relation,
    clientKey: relation.id || createClientKey(),
    relationType: relation.relationType || 'REFERENCE',
    relationName: relation.relationName || relationLabel(relation),
    targetObjectCode: relation.targetObjectCode || '',
    sourceFieldCode: relation.sourceFieldCode || '',
    targetFieldCode: relation.targetFieldCode || '',
    detailTabTitle: config.detailTabTitle || relation.relationName || relation.targetObjectName || relation.targetObjectCode || '',
    showInDetail: config.showInDetail !== false,
    inlineCreateEnabled: canInlineEdit(relation) && config.inlineCreateEnabled !== false && config.inlineCreateEnabled !== 'false',
    inlineEditEnabled: canInlineEdit(relation) && config.inlineEditEnabled !== false && config.inlineEditEnabled !== 'false',
    defaultFilter: config.defaultFilter || '',
    displayField: config.displayField || '',
    status: relation.status ?? 1,
    sortOrder: relation.sortOrder ?? 0,
  }
}

function toRelationPayload(relation = {}) {
  return {
    id: relation.id,
    suiteCode: props.suiteCode,
    sourceObjectCode: props.objectCode,
    targetObjectCode: relation.targetObjectCode,
    relationType: relation.relationType,
    relationName: relation.relationName || relationLabel(relation),
    sourceFieldCode: relation.sourceFieldCode,
    targetFieldCode: relation.targetFieldCode,
    relationConfig: buildRelationConfig(relation),
    description: relation.description,
    status: relation.status ?? 1,
    sortOrder: relation.sortOrder ?? 0,
  }
}

function buildRelationConfig(relation) {
  const config = {}
  if (relation.detailTabTitle)
    config.detailTabTitle = relation.detailTabTitle
  config.showInDetail = relation.showInDetail !== false
  config.inlineCreateEnabled = canInlineEdit(relation) && relation.inlineCreateEnabled === true
  config.inlineEditEnabled = canInlineEdit(relation) && relation.inlineEditEnabled === true
  if (relation.defaultFilter)
    config.defaultFilter = relation.defaultFilter
  if (relation.displayField)
    config.displayField = relation.displayField
  return Object.keys(config).length ? JSON.stringify(config) : ''
}

function parseRelationConfig(value) {
  if (!value)
    return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' ? parsed : {}
  }
  catch {
    return {
      defaultFilter: value,
    }
  }
}

function normalizeLinkageRuleDraft(rule = {}) {
  const sourceField = rule.sourceField || ''
  const targetField = rule.targetField || ''
  const type = linkageTypeOptions.some(item => item.value === rule.type) ? rule.type : 'linkedDict'
  const sourceFieldConfig = fieldMap.value.get(sourceField) || {}
  const targetFieldConfig = fieldMap.value.get(targetField) || {}
  const dataSourceType = resolveLinkageDataSourceType(type)
  return {
    ruleId: rule.ruleId || createLinkageRuleId(sourceField, targetField),
    type,
    sourceField,
    targetField,
    dataSourceType,
    matchMode: type,
    dictConfig: {
      ...(rule.dictConfig || {}),
      sourceDictType: rule.dictConfig?.sourceDictType || sourceFieldConfig.dictType || '',
      targetDictType: rule.dictConfig?.targetDictType || targetFieldConfig.dictType || '',
      linkedDictType: rule.dictConfig?.linkedDictType || sourceFieldConfig.dictType || '',
      parentValueSource: rule.dictConfig?.parentValueSource || 'sourceValue',
    },
    remoteConfig: {
      ...(rule.remoteConfig || {}),
      url: rule.remoteConfig?.url || targetFieldConfig.basicProps?.optionSource?.api || '',
      method: rule.remoteConfig?.method || 'GET',
      paramName: rule.remoteConfig?.paramName || sourceField || '',
      valuePath: rule.remoteConfig?.valuePath || 'data',
      labelField: rule.remoteConfig?.labelField || 'label',
      valueField: rule.remoteConfig?.valueField || 'value',
    },
    objectConfig: {
      ...(rule.objectConfig || {}),
      targetObjectCode: rule.objectConfig?.targetObjectCode || targetFieldConfig.referenceObjectCode || targetFieldConfig.basicProps?.referenceObjectCode || '',
      displayField: rule.objectConfig?.displayField || targetFieldConfig.referenceDisplayField || targetFieldConfig.basicProps?.referenceDisplayField || '',
    },
    orgConfig: {
      ...(rule.orgConfig || {}),
      paramName: rule.orgConfig?.paramName || rule.remoteConfig?.paramName || sourceField || '',
    },
    condition: { ...(rule.condition || {}) },
    emptyStrategy: ['empty', 'all', 'disabled'].includes(rule.emptyStrategy) ? rule.emptyStrategy : 'empty',
    clearOnSourceChange: rule.clearOnSourceChange !== false,
    enabled: rule.enabled !== false,
  }
}

function fillRuleFieldDefaults(rule) {
  const normalized = normalizeLinkageRuleDraft(rule)
  Object.assign(rule, normalized)
}

function syncLinkageModel(dirty = false) {
  if (resettingLinkage)
    return
  const normalized = normalizeLinkageSchema(localLinkage.value)
  if (!isSameSchema(normalized, localLinkage.value))
    localLinkage.value = normalized
  if (!isSameSchema(normalized, props.linkageSchema))
    emit('update:linkageSchema', cloneSchema(normalized))
  if (dirty) {
    emit('dirtyChange', true)
  }
}

function resolveLinkageDataSourceType(type) {
  if (['parentDictCode', 'linkedDict'].includes(type))
    return 'dict'
  if (type === 'orgScope')
    return 'org'
  if (type === 'objectReference')
    return 'object'
  return 'remote'
}

function createLinkageRuleId(sourceField = '', targetField = '') {
  return `linkage_${sourceField || 'source'}_${targetField || Date.now()}`
    .replace(/\W+/g, '_')
    .replace(/_+/g, '_')
}

function inferLinkageType(targetField) {
  const target = fieldMap.value.get(targetField) || {}
  if (target.componentType === 'objectReference' || target.fieldType === 'REFERENCE')
    return 'objectReference'
  if (target.componentType === 'orgTreeSelect' || ['DEPT', 'ORG'].includes(target.fieldType))
    return 'orgScope'
  if (target.dictType || ['DICT', 'RADIO', 'CHECKBOX'].includes(target.fieldType))
    return 'linkedDict'
  return 'remoteParam'
}

function linkageTargetFieldOptions(rule = {}) {
  return sourceFieldOptions.value.filter(item => item.value !== rule.sourceField)
}

function linkageRuleLabel(rule = {}) {
  const target = fieldLabel(rule.targetField)
  const typeLabel = linkageTypeOptions.find(item => item.value === rule.type)?.label || rule.type
  return `${target || '目标字段'} · ${typeLabel}`
}

function linkageRuleSentence(rule = {}) {
  const source = fieldLabel(rule.sourceField) || '上级字段'
  const target = fieldLabel(rule.targetField) || '目标字段'
  const verbs = {
    parentDictCode: '按父级字典过滤',
    linkedDict: '按关联字典过滤',
    remoteParam: '作为远程参数过滤',
    orgScope: '作为组织范围过滤',
    objectReference: '过滤引用对象',
  }
  return `${source} → ${target}，${verbs[rule.type] || '联动'}`
}

function fieldLabel(fieldCode) {
  if (!fieldCode)
    return ''
  const field = fieldMap.value.get(fieldCode)
  return field ? `${field.label || field.fieldName || fieldCode}（${fieldCode}）` : fieldCode
}

function relationSentence(relation) {
  const source = props.objectName || props.objectCode || '当前对象'
  const target = objectNameMap.value.get(relation.targetObjectCode) || relation.targetObjectName || relation.targetObjectCode || '目标对象'
  const verbs = {
    REFERENCE: '属于',
    CHILD_LIST: '有多个',
    DETAIL: '包含明细',
    MANY_TO_MANY: '关联多个',
  }
  return `${source}${verbs[relation.relationType] || '关联'}${target}`
}

function sourceFieldLabel(relation) {
  return relation?.relationType === 'REFERENCE'
    ? '当前对象中的关联字段'
    : '当前对象匹配字段'
}

function sourceFieldPlaceholder(relation) {
  return relation?.relationType === 'REFERENCE'
    ? '例如：customerId'
    : '通常选择：记录ID'
}

function targetFieldLabel(relation) {
  return relation?.relationType === 'REFERENCE'
    ? '目标对象主键字段'
    : '目标对象里指向本对象的字段'
}

function targetFieldPlaceholder(relation) {
  return relation?.relationType === 'REFERENCE'
    ? '通常选择：记录ID'
    : '选择目标对象中的所属字段'
}

function displayFieldLabel(relation) {
  return relation?.relationType === 'REFERENCE'
    ? '运行态显示字段'
    : '目标对象回显字段'
}

function targetFieldOptions(relation) {
  const fields = targetFieldsMap.value[relation.targetObjectCode] || []
  const options = fields
    .filter(field => field.field && !isInactiveField(field))
    .map(field => ({
      label: businessFieldLabel(field),
      value: field.field,
    }))
  if (relation.targetFieldCode && !options.some(item => item.value === relation.targetFieldCode)) {
    options.unshift({
      label: `已配置字段：${relation.targetFieldCode}`,
      value: relation.targetFieldCode,
    })
  }
  return options
}

function targetDisplayFieldOptions(relation) {
  const fields = targetFieldsMap.value[relation.targetObjectCode] || []
  const options = fields
    .filter(field => field.field && !isInactiveField(field) && !field.systemField && field.field !== relation.targetFieldCode)
    .map(field => ({
      label: businessFieldLabel(field),
      value: field.field,
    }))
  if (relation.displayField && !options.some(item => item.value === relation.displayField)) {
    options.unshift({
      label: `已配置字段：${relation.displayField}`,
      value: relation.displayField,
    })
  }
  return options
}

async function loadTargetFields(objectCode) {
  if (!objectCode || targetFieldsMap.value[objectCode] || targetFieldLoadingMap.value[objectCode])
    return
  const targetObject = businessObjects.value.find(item => item.objectCode === objectCode)
  if (!targetObject?.id)
    return
  targetFieldLoadingMap.value = {
    ...targetFieldLoadingMap.value,
    [objectCode]: true,
  }
  try {
    const res = await businessObjectDesigner(targetObject.id)
    const fields = res.data?.fields || res.data?.modelSchema?.fields || []
    targetFieldsMap.value = {
      ...targetFieldsMap.value,
      [objectCode]: fields.map(toPageField),
    }
  }
  catch {
    targetFieldsMap.value = {
      ...targetFieldsMap.value,
      [objectCode]: [],
    }
  }
  finally {
    targetFieldLoadingMap.value = {
      ...targetFieldLoadingMap.value,
      [objectCode]: false,
    }
  }
}

function firstTargetField(objectCode, relationType = 'CHILD_LIST', currentValue = '') {
  const fields = targetFieldsMap.value[objectCode] || []
  if (relationType === 'REFERENCE') {
    if (currentValue && currentValue !== 'id' && fields.some(field => field.field === currentValue && !isInactiveField(field)))
      return currentValue
    return fields.find(field => field.field === 'id' && !isInactiveField(field))?.field
      || fields.find(field => field.primaryKey && !isInactiveField(field))?.field
      || fields.find(field => !isInactiveField(field))?.field
      || ''
  }
  const sourceObject = lowerFirst(props.objectCode || '')
  const candidates = [
    `${sourceObject}Id`,
    `${sourceObject}Code`,
    props.objectCode,
    'parentId',
  ].filter(Boolean)
  const activeFields = fields.filter(field => !isInactiveField(field))
  const matched = activeFields.find(field => candidates.includes(field.field))
    || activeFields.find((field) => {
      const label = field.label || ''
      const sourceName = props.objectName || props.objectCode || ''
      return sourceName && label.includes(sourceName)
    })
  if (matched)
    return matched.field
  if (currentValue && currentValue !== 'id' && fields.some(field => field.field === currentValue && !isInactiveField(field)))
    return currentValue
  const fallback = activeFields.find(field => field.field !== 'id' && !field.systemField)
  return fallback?.field || ''
}

function inferDefaultRelationType(targetObjectCode) {
  return findReferenceSourceField(targetObjectCode) ? 'REFERENCE' : 'CHILD_LIST'
}

function firstDisplayField(objectCode, relationField = '') {
  const targetObject = businessObjects.value.find(item => item.objectCode === objectCode)
  const fields = targetFieldsMap.value[objectCode] || []
  const activeFields = fields.filter(field => field.field && !isInactiveField(field) && !field.systemField && field.field !== relationField)
  const configured = targetObject?.displayField
  const matched = activeFields.find(field => configured && field.field === configured)
    || activeFields.find((field) => {
      const fieldName = String(field.field || '').toLowerCase()
      const label = String(field.label || field.fieldName || '')
      return fieldName.includes('name') || label.includes('名称') || label.includes('姓名')
    })
    || activeFields[0]
  return matched?.field || ''
}

function isInactiveField(field = {}) {
  const status = String(field.fieldStatus || '').toUpperCase()
  return status === 'DISABLED' || status === 'HIDDEN'
}

function businessFieldLabel(field = {}) {
  const fieldName = field.field || ''
  if (fieldName === 'id')
    return '记录ID'
  if (fieldName === 'createBy')
    return '创建人'
  if (fieldName === 'createTime')
    return '创建时间'
  if (fieldName === 'updateBy')
    return '修改人'
  if (fieldName === 'updateTime')
    return '修改时间'
  if (fieldName === 'createDept')
    return '创建部门'
  return field.label || field.fieldName || fieldName
}

function relationLabel(relation) {
  return relationSentence(relation)
}

function canInlineEdit(relation = {}) {
  return ['CHILD_LIST', 'DETAIL'].includes(relation.relationType)
}

function firstSourceField(relationType = 'CHILD_LIST', targetObjectCode = '', currentValue = '') {
  if (relationType === 'REFERENCE') {
    const matched = findReferenceSourceField(targetObjectCode)
    if (matched)
      return matched.value
    if (currentValue && sourceFieldOptions.value.some(item => item.value === currentValue))
      return currentValue
  }
  else {
    const idField = sourceFieldOptions.value.find(item => item.value === 'id')?.value
    if (idField)
      return idField
    if (currentValue && sourceFieldOptions.value.some(item => item.value === currentValue))
      return currentValue
  }
  return sourceFieldOptions.value.find(item => item.value === 'id')?.value || sourceFieldOptions.value[0]?.value || ''
}

function findReferenceSourceField(targetObjectCode = '') {
  const targetObject = businessObjects.value.find(item => item.objectCode === targetObjectCode)
  const targetCode = lowerFirst(targetObjectCode || '')
  const targetName = targetObject?.objectName || ''
  const candidates = [
    `${targetCode}Id`,
    `${targetCode}Code`,
    targetObjectCode,
  ].filter(Boolean)
  return sourceFieldOptions.value.find(item => candidates.includes(item.value))
    || sourceFieldOptions.value.find((item) => {
      const label = item.label || ''
      return targetName && label.includes(targetName)
    })
}

function createClientKey() {
  return `relation_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function lowerFirst(value) {
  if (!value)
    return ''
  const normalized = String(value)
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '')
    .toLowerCase()
  return normalized.replace(/_([a-z0-9])/g, (_, char) => char.toUpperCase())
}

function toPageField(field) {
  return {
    ...field,
    field: field.field || field.fieldCode,
    label: field.label || field.fieldName || field.fieldCode,
    fieldStatus: field.fieldStatus,
    basicProps: { ...(field.basicProps || {}) },
    advancedProps: { ...(field.advancedProps || {}) },
  }
}

function toFieldPayload(field = {}) {
  return {
    fieldName: field.fieldName || field.label,
    fieldCode: field.fieldCode || field.field,
    columnName: field.columnName,
    fieldType: field.fieldType || field.businessFieldType,
    dataType: field.dataType,
    length: field.length,
    precision: field.precision,
    required: field.required,
    defaultValue: field.defaultValue,
    searchable: field.searchable,
    listVisible: field.listVisible,
    formVisible: field.formVisible,
    importable: field.importable,
    exportable: field.exportable,
    componentType: field.componentType,
    queryType: field.queryType,
    dictType: field.dictType,
    sensitiveType: field.sensitiveType,
    encryptAlgorithm: field.encryptAlgorithm,
    sortable: field.sortable,
    systemField: field.systemField,
    readonly: field.readonly,
    fieldStatus: field.fieldStatus,
    referenceObjectCode: field.referenceObjectCode,
    referenceDisplayField: field.referenceDisplayField,
    placeholder: field.basicProps?.placeholder || field.placeholder || '',
    remark: field.remark,
    sortOrder: field.sortOrder,
    fieldBinding: { ...(field.fieldBinding || {}) },
    basicProps: { ...(field.basicProps || {}) },
    advancedProps: { ...(field.advancedProps || {}) },
  }
}

function markDirty() {
  if (activeTab.value === 'linkage') {
    syncLinkageModel(true)
    return
  }
  markRelationDirty()
}

function markRelationDirty() {
  relationDirty.value = true
  emit('dirtyChange', true)
}

defineExpose({
  saveRelations,
  loadRelations,
})
</script>

<style scoped>
.business-relation-designer {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: calc(100vh - 106px);
}

.relation-designer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 16px;
}

.relation-designer-head h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.relation-designer-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.relation-designer-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  min-height: 0;
}

.relation-tabs {
  min-height: 0;
}

.relation-tabs :deep(.n-tabs-nav) {
  padding: 0 16px;
}

.relation-tabs :deep(.n-tab-pane) {
  padding: 0;
}

.relation-list-pane {
  min-width: 0;
  overflow: auto;
  background: #f8fafc;
  padding: 14px;
}

.relation-card-list {
  display: grid;
  gap: 12px;
}

.relation-card,
.relation-summary-pane section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.relation-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.relation-card-head > div {
  min-width: 0;
}

.relation-card-head strong,
.relation-summary-pane h4 {
  margin: 0;
  color: #111827;
  font-size: 14px;
  overflow-wrap: anywhere;
}

.relation-card-head p,
.relation-summary-pane p,
.relation-summary-pane li {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.relation-summary-pane {
  display: grid;
  align-content: start;
  gap: 12px;
  border-left: 1px solid #e5e7eb;
  background: #fbfcfe;
  padding: 12px;
}

.sentence-list {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.sentence-list span {
  display: block;
  border-radius: 4px;
  background: #eef6ff;
  color: #1d4ed8;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
  padding: 6px 8px;
}

.relation-summary-pane ul {
  margin: 10px 0 0;
  padding-left: 18px;
}

.metric-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.metric-list span {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #334155;
  font-size: 12px;
  padding: 8px;
}

@media (max-width: 1100px) {
  .relation-designer-body {
    grid-template-columns: 1fr;
  }

  .relation-summary-pane {
    border-left: 0;
    border-top: 1px solid #e5e7eb;
  }
}
</style>
