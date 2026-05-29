<template>
  <div class="business-relation-designer">
    <div class="relation-designer-head">
      <div>
        <h3>关系配置</h3>
        <p>用业务语言维护对象关系，同步到底层模型关系和应用中心入口。</p>
      </div>
      <n-space size="small">
        <n-button size="small" secondary @click="loadRelations">
          刷新
        </n-button>
        <n-button size="small" secondary @click="addRelation">
          新增关系
        </n-button>
        <n-button size="small" type="primary" :loading="saving" @click="saveRelations">
          保存关系
        </n-button>
      </n-space>
    </div>

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
                  <n-form-item-gi label="关系类型">
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
                  <n-form-item-gi label="关系名称">
                    <n-input v-model:value="relation.relationName" placeholder="例如：客户有多个联系人" @update:value="markDirty" />
                  </n-form-item-gi>
                  <n-form-item-gi label="当前对象字段">
                    <n-select
                      v-model:value="relation.sourceFieldCode"
                      :options="sourceFieldOptions"
                      clearable
                      filterable
                      placeholder="通常为 id"
                      @update:value="markDirty"
                    />
                  </n-form-item-gi>
                  <n-form-item-gi label="目标对象字段">
                    <n-input v-model:value="relation.targetFieldCode" placeholder="例如：customerId" @update:value="markDirty" />
                  </n-form-item-gi>
                  <n-form-item-gi label="详情页签">
                    <n-input v-model:value="relation.detailTabTitle" placeholder="例如：联系人" @update:value="markDirty" />
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
                    <n-input v-model:value="relation.description" type="textarea" :rows="2" placeholder="描述这条业务关系的使用场景" @update:value="markDirty" />
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
            <li>当前对象字段必须存在。</li>
            <li>目标对象未发布时会在发布检查中提示。</li>
          </ul>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { TrashOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import {
  businessObjectList,
  businessObjectRelations,
  saveBusinessObjectRelations,
} from '@/api/business-app'

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
})

const emit = defineEmits(['updated', 'dirtyChange'])

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const businessObjects = ref([])
const localRelations = ref([])

const relationTypeOptions = [
  { label: '属于 / 引用', value: 'REFERENCE' },
  { label: '拥有多个', value: 'CHILD_LIST' },
  { label: '明细', value: 'DETAIL' },
  { label: '多对多', value: 'MANY_TO_MANY' },
]

const sourceFieldOptions = computed(() => {
  const fields = (props.fields || []).map(toPageField).filter(field => field.field)
  return fields.map(field => ({
    label: `${field.label || field.field}（${field.field}）`,
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

watch(() => props.objectId, () => {
  loadRelations()
}, { immediate: true })

watch(() => props.suiteCode, () => {
  loadBusinessObjects()
}, { immediate: true })

onMounted(() => {
  loadBusinessObjects()
})

async function loadRelations() {
  if (!props.objectId) {
    localRelations.value = []
    return
  }
  loading.value = true
  try {
    const res = await businessObjectRelations(props.objectId)
    localRelations.value = (res.data || []).map(normalizeRelation)
    emit('updated', localRelations.value)
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
  }
  catch {
    businessObjects.value = []
  }
}

function addRelation() {
  const target = targetObjectOptions.value[0]
  if (!target) {
    message.warning('当前套件没有可关联的目标对象')
    return
  }
  const targetName = objectNameMap.value.get(target.value) || target.label
  const relation = {
    clientKey: createClientKey(),
    relationType: 'CHILD_LIST',
    targetObjectCode: target.value,
    relationName: `${props.objectName || props.objectCode}有多个${targetName}`,
    sourceFieldCode: firstSourceField(),
    targetFieldCode: `${lowerFirst(props.objectCode || 'object')}Id`,
    detailTabTitle: targetName,
    defaultFilter: '',
    description: '',
    status: 1,
    sortOrder: localRelations.value.length * 10 + 10,
  }
  localRelations.value.push(relation)
  emit('dirtyChange', true)
}

function removeRelation(index) {
  localRelations.value.splice(index, 1)
  emit('dirtyChange', true)
}

function updateRelationType(relation, value) {
  relation.relationType = value
  relation.relationName = relation.relationName || relationLabel(relation)
  markDirty()
}

function updateTargetObject(relation, value) {
  relation.targetObjectCode = value
  const targetName = objectNameMap.value.get(value) || value
  relation.detailTabTitle = relation.detailTabTitle || targetName
  relation.relationName = relationLabel(relation)
  markDirty()
}

function updateStatus(relation, value) {
  relation.status = value ? 1 : 0
  markDirty()
}

async function saveRelations() {
  if (!props.objectId)
    return
  saving.value = true
  try {
    const payload = localRelations.value.map(toRelationPayload)
    await saveBusinessObjectRelations(props.objectId, payload)
    message.success('关系配置已保存')
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
    defaultFilter: config.defaultFilter || '',
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
  if (relation.defaultFilter)
    config.defaultFilter = relation.defaultFilter
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

function relationSentence(relation) {
  const source = props.objectName || props.objectCode || '当前对象'
  const target = objectNameMap.value.get(relation.targetObjectCode) || relation.targetObjectName || relation.targetObjectCode || '目标对象'
  const verbs = {
    REFERENCE: '属于 / 引用',
    CHILD_LIST: '有多个',
    DETAIL: '包含明细',
    MANY_TO_MANY: '关联多个',
  }
  return `${source}${verbs[relation.relationType] || '关联'}${target}`
}

function relationLabel(relation) {
  return relationSentence(relation)
}

function firstSourceField() {
  return sourceFieldOptions.value.find(item => item.value === 'id')?.value || sourceFieldOptions.value[0]?.value || ''
}

function createClientKey() {
  return `relation_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function lowerFirst(value) {
  if (!value)
    return ''
  const normalized = String(value).replace(/\W+/g, '_')
  return normalized.charAt(0).toLowerCase() + normalized.slice(1)
}

function toPageField(field) {
  return {
    ...field,
    field: field.field || field.fieldCode,
    label: field.label || field.fieldName || field.fieldCode,
  }
}

function markDirty() {
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

.relation-card-head strong,
.relation-summary-pane h4 {
  margin: 0;
  color: #111827;
  font-size: 14px;
}

.relation-card-head p,
.relation-summary-pane p,
.relation-summary-pane li {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
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
  border-radius: 4px;
  background: #eef6ff;
  color: #1d4ed8;
  font-size: 12px;
  line-height: 24px;
  padding: 0 8px;
}

.relation-summary-pane ul {
  margin: 10px 0 0;
  padding-left: 18px;
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
