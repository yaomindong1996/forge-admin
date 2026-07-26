<template>
  <n-drawer :show="show" width="min(920px, 96vw)" @update:show="value => emit('update:show', value)">
    <n-drawer-content :title="isEditing ? '编辑应用' : '新建应用'" closable>
      <div v-if="!isEditing && !isDirectPageStart" class="application-steps">
        <n-steps :current="currentStep" size="small">
          <n-step title="基本信息" description="定义稳定应用身份" />
          <n-step title="模板与起点" description="直接生成常用页面结构" />
        </n-steps>
      </div>

      <n-form
        v-show="isEditing || currentStep === 1"
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="top"
      >
        <n-grid :cols="2" :x-gap="14">
          <n-form-item-gi label="应用名称" path="applicationName">
            <n-input v-model:value="form.applicationName" placeholder="例如：客户经营" />
          </n-form-item-gi>
          <n-form-item-gi label="应用编码" path="applicationCode">
            <n-input
              v-model:value="form.applicationCode"
              :disabled="isEditing"
              placeholder="例如：crm_center"
              @blur="form.applicationCode = normalizeCode(form.applicationCode)"
            />
          </n-form-item-gi>
        </n-grid>

        <n-form-item label="所属业务域" path="suiteCode">
          <n-select
            v-model:value="form.suiteCode"
            filterable
            :disabled="isEditing && hasManagedAssets"
            :options="suiteOptions"
            placeholder="选择应用所属的业务域"
          />
          <template v-if="isEditing && hasManagedAssets" #feedback>
            已关联对象或入口的应用不能直接移动业务域。
          </template>
        </n-form-item>

        <n-form-item label="应用图标">
          <IconSelector v-model="form.icon" />
        </n-form-item>

        <n-grid :cols="2" :x-gap="14">
          <n-form-item-gi label="启用状态">
            <n-switch v-model:value="form.status" :checked-value="1" :unchecked-value="0" />
          </n-form-item-gi>
          <n-form-item-gi v-if="isEditing" label="当前设计状态">
            <DictTag
              dict-type="ai_business_application_design_status"
              :value="form.designStatus"
              :bordered="false"
            />
          </n-form-item-gi>
        </n-grid>

        <n-form-item label="应用说明">
          <n-input
            v-model:value="form.description"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="说明应用解决什么问题、由谁使用"
          />
        </n-form-item>
      </n-form>

      <div v-if="!isEditing && currentStep === 2" class="initialize-panel">
        <n-alert type="info" :bordered="false">
          应用会先保存为草稿。后续初始化失败时，草稿仍会保留并可重新配置。
        </n-alert>
        <n-alert v-if="initializationWarning" type="warning" :bordered="false">
          {{ initializationWarning }}。应用草稿已保留，{{ initializationRecoveryHint }}
        </n-alert>
        <ApplicationInitializeStep
          v-model="initializeMode"
          v-model:object-id="primaryObjectId"
          v-model:datasource-id="runtimeDatasourceId"
          v-model:table-name="importTableName"
          :object-options="objectOptions"
          :loading-objects="loadingObjects"
          :datasource-options="datasourceOptions"
          :table-options="tableOptions"
          :selected-table="selectedTableInfo"
          :loading-datasources="datasourceLoading"
          :loading-tables="tableLoading"
          :template-config="templateConfig"
          @update:template-config="updateTemplateConfig"
        />
      </div>

      <template #footer>
        <div class="drawer-footer">
          <n-button
            v-if="!isEditing && currentStep === 2"
            :disabled="Boolean(createdApplicationId)"
            @click="currentStep = 1"
          >
            上一步
          </n-button>
          <span v-else />
          <n-space>
            <n-button @click="emit('update:show', false)">
              取消
            </n-button>
            <n-button v-if="!isEditing && currentStep === 1" type="primary" :loading="saving" @click="isDirectPageStart ? save() : goNext()">
              {{ isDirectPageStart ? '创建并开始设计' : '下一步' }}
            </n-button>
            <n-button v-else type="primary" :loading="saving" @click="save">
              {{ primaryActionText }}
            </n-button>
          </n-space>
        </div>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import {
  businessObjectList,
  createBusinessObject,
  genDatasourceEnabled,
  genDatasourceTables,
} from '@/api/business-app'
import {
  createBusinessApplication,
  initializeBusinessApplicationTemplate,
  saveBusinessApplicationObjects,
  updateBusinessApplication,
} from '@/api/business-application'
import DictTag from '@/components/DictTag.vue'
import IconSelector from '@/components/IconSelector.vue'
import ApplicationInitializeStep from './ApplicationInitializeStep.vue'
import { buildModelCode, normalizeObjectCode } from './designer/form-first/namingUtils'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  application: {
    type: Object,
    default: null,
  },
  suites: {
    type: Array,
    default: () => [],
  },
  defaultSuiteCode: {
    type: String,
    default: null,
  },
  defaultInitializeMode: {
    type: String,
    default: 'TEMPLATE_SINGLE_CRUD',
  },
})

const emit = defineEmits(['update:show', 'saved'])
const message = useMessage()
const formRef = ref(null)
const currentStep = ref(1)
const saving = ref(false)
const loadingObjects = ref(false)
const datasourceLoading = ref(false)
const tableLoading = ref(false)
const initializeMode = ref('TEMPLATE_SINGLE_CRUD')
const primaryObjectId = ref(null)
const objectOptions = ref([])
const datasourceList = ref([])
const tableList = ref([])
const runtimeDatasourceId = ref(null)
const importTableName = ref(null)
const createdApplicationId = ref(null)
const createdPrimaryObjectId = ref(null)
const createdPrimaryObjectCode = ref('')
const initializationWarning = ref('')
const form = reactive(defaultForm())
const templateConfig = reactive(defaultTemplateConfig())

const isEditing = computed(() => Boolean(form.id))
const isDirectPageStart = computed(() => !isEditing.value && initializeMode.value === 'BLANK')
const isTemplateMode = computed(() => initializeMode.value.startsWith('TEMPLATE_'))
const hasManagedAssets = computed(() => Number(form.objectCount || 0) > 0 || Number(form.entryCount || 0) > 0)
const primaryActionText = computed(() => {
  if (isEditing.value)
    return '保存修改'
  if (createdApplicationId.value && initializeMode.value === 'EXISTING_OBJECT')
    return '重试绑定并进入应用'
  if (createdApplicationId.value && initializeMode.value === 'DATABASE_TABLE')
    return createdPrimaryObjectId.value ? '重试绑定并进入应用' : '重试导入并进入应用'
  if (createdApplicationId.value && isTemplateMode.value)
    return '重试生成并进入应用'
  if (createdApplicationId.value)
    return '进入空白应用'
  if (isDirectPageStart.value)
    return '创建并开始设计'
  return '创建并进入应用'
})
const initializationRecoveryHint = computed(() => {
  if (isTemplateMode.value)
    return '可调整模板配置后重试；模板资产会整体生成，不会保留半套对象。也可改为空白应用继续进入。'
  if (initializeMode.value === 'DATABASE_TABLE') {
    return createdPrimaryObjectId.value
      ? '数据表对象已创建，可直接重试加入应用，或改为空白应用继续进入。'
      : '可调整数据源或数据表后重试，或改为空白应用继续进入。'
  }
  return '可更换主对象后重试，或改为空白应用继续进入。'
})
const selectedTemplateCode = computed(() => ({
  TEMPLATE_SINGLE_CRUD: 'SINGLE_CRUD',
  TEMPLATE_TREE_TABLE: 'TREE_TABLE',
  TEMPLATE_MASTER_DETAIL: 'MASTER_DETAIL',
})[initializeMode.value] || '')
const suiteOptions = computed(() => props.suites.map(suite => ({
  label: suite.suiteName || suite.suiteCode,
  value: suite.suiteCode,
  disabled: Number(suite.status) !== 1,
})))
const datasourceOptions = computed(() => datasourceList.value.map(item => ({
  label: `${item.datasourceName || item.datasourceCode}${item.isDefault === 1 ? '（默认）' : ''} / ${item.dbType || '-'}`,
  value: item.datasourceId,
})))
const tableOptions = computed(() => tableList.value.map(item => ({
  label: item.tableComment ? `${item.tableName}（${item.tableComment}）` : item.tableName,
  value: item.tableName,
})))
const selectedTableInfo = computed(() => tableList.value.find(item => item.tableName === importTableName.value) || null)

const rules = {
  applicationName: {
    required: true,
    message: '请输入应用名称',
    trigger: ['blur', 'input'],
  },
  applicationCode: {
    required: true,
    validator: (_, value) => /^[a-z]\w{1,63}$/i.test(normalizeCode(value)),
    message: '应用编码需以字母开头，仅包含字母、数字和下划线（2-64字符）',
    trigger: ['blur', 'input'],
  },
  suiteCode: {
    required: true,
    message: '请选择所属业务域',
    trigger: ['blur', 'change'],
  },
}

watch(() => props.show, async (visible) => {
  if (!visible)
    return
  Object.assign(form, defaultForm(), props.application || {})
  if (!form.suiteCode)
    form.suiteCode = props.defaultSuiteCode || null
  currentStep.value = 1
  initializeMode.value = props.defaultInitializeMode || 'TEMPLATE_SINGLE_CRUD'
  primaryObjectId.value = null
  objectOptions.value = []
  datasourceList.value = []
  tableList.value = []
  runtimeDatasourceId.value = null
  importTableName.value = null
  createdApplicationId.value = null
  createdPrimaryObjectId.value = null
  createdPrimaryObjectCode.value = ''
  initializationWarning.value = ''
  Object.assign(templateConfig, defaultTemplateConfig())
})

watch(currentStep, async (step) => {
  if (step !== 2)
    return
  if (isTemplateMode.value)
    await Promise.all([loadDatasources(), loadObjectOptions()])
  else if (initializeMode.value === 'DATABASE_TABLE')
    await loadDatasources()
  else if (initializeMode.value === 'EXISTING_OBJECT')
    await loadObjectOptions()
})

watch(initializeMode, async (mode) => {
  initializationWarning.value = ''
  if (currentStep.value !== 2)
    return
  if (mode.startsWith('TEMPLATE_'))
    await Promise.all([loadDatasources(), loadObjectOptions()])
  else if (mode === 'DATABASE_TABLE')
    await loadDatasources()
  else if (mode === 'EXISTING_OBJECT')
    await loadObjectOptions()
})

watch(runtimeDatasourceId, async (value, previous) => {
  if (value === previous || initializeMode.value !== 'DATABASE_TABLE')
    return
  await loadTables(value)
})

watch(() => form.suiteCode, () => {
  if (createdApplicationId.value)
    return
  primaryObjectId.value = null
  objectOptions.value = []
  if (currentStep.value !== 2)
    return
  if (isTemplateMode.value) {
    loadDatasources()
    loadObjectOptions()
  }
  else if (initializeMode.value === 'DATABASE_TABLE') {
    loadDatasources()
  }
  else if (initializeMode.value === 'EXISTING_OBJECT') {
    loadObjectOptions()
  }
})

async function goNext() {
  await formRef.value?.validate()
  ensureTemplateDefaults()
  currentStep.value = 2
}

async function loadObjectOptions() {
  if (!form.suiteCode)
    return
  loadingObjects.value = true
  try {
    const response = await businessObjectList({ suiteCode: form.suiteCode, status: 1 })
    objectOptions.value = (response.data || []).map(object => ({
      label: `${object.objectName || object.objectCode} · ${object.objectCode}`,
      value: object.id,
    }))
  }
  finally {
    loadingObjects.value = false
  }
}

async function save() {
  await formRef.value?.validate()
  if (!isEditing.value && initializeMode.value === 'EXISTING_OBJECT' && !primaryObjectId.value) {
    message.warning('请选择要绑定的主业务对象')
    return
  }
  if (!isEditing.value && initializeMode.value === 'DATABASE_TABLE') {
    if (!runtimeDatasourceId.value) {
      message.warning('请选择运行数据源')
      return
    }
    if (!importTableName.value) {
      message.warning('请选择要导入的数据表')
      return
    }
  }
  if (!isEditing.value && isTemplateMode.value && !validateTemplateConfig())
    return

  saving.value = true
  try {
    const payload = buildPayload()
    if (isEditing.value) {
      await updateBusinessApplication(payload)
      message.success('应用信息已更新')
      emit('saved', { application: payload, created: false })
      emit('update:show', false)
      return
    }

    const retryingInitialization = Boolean(createdApplicationId.value)
    const applicationId = createdApplicationId.value || await createApplicationDraft(payload)
    if (initializeMode.value === 'EXISTING_OBJECT') {
      try {
        await saveBusinessApplicationObjects(applicationId, [{
          objectId: primaryObjectId.value,
          objectRole: 'PRIMARY',
          sortOrder: 0,
        }])
      }
      catch (error) {
        createdApplicationId.value = applicationId
        initializationWarning.value = error?.message || '主对象绑定失败'
        message.warning('应用草稿已创建，但主对象绑定失败，请修正后重试')
        emit('saved', {
          application: { ...payload, id: applicationId },
          created: true,
          initializeMode: initializeMode.value,
          initializationWarning: initializationWarning.value,
        })
        return
      }
    }
    else if (initializeMode.value === 'DATABASE_TABLE') {
      try {
        await initializeApplicationFromTable(applicationId)
      }
      catch (error) {
        createdApplicationId.value = applicationId
        initializationWarning.value = error?.message || '数据库表导入失败'
        message.warning(createdPrimaryObjectId.value
          ? '数据表对象已创建，但加入应用失败，请重试'
          : '应用草稿已创建，但数据表导入失败，请修正后重试')
        emit('saved', {
          application: { ...payload, id: applicationId },
          created: true,
          initializeMode: initializeMode.value,
          initializationWarning: initializationWarning.value,
        })
        return
      }
    }
    else if (isTemplateMode.value) {
      try {
        await initializeApplicationFromTemplate(applicationId)
      }
      catch (error) {
        createdApplicationId.value = applicationId
        initializationWarning.value = error?.message || '应用模板初始化失败'
        message.warning('应用草稿已创建，但模板生成失败，请修正后重试')
        emit('saved', {
          application: { ...payload, id: applicationId },
          created: true,
          initializeMode: initializeMode.value,
          initializationWarning: initializationWarning.value,
        })
        return
      }
    }
    initializationWarning.value = ''
    message.success(isTemplateMode.value
      ? (retryingInitialization ? '应用模板已重新生成' : '应用模板已生成')
      : (retryingInitialization ? '应用初始化已完成' : '应用草稿已创建'))
    emit('saved', {
      application: { ...payload, id: applicationId },
      created: true,
      initializeMode: initializeMode.value,
      initializationCompleted: initializeMode.value === 'DATABASE_TABLE' || isTemplateMode.value,
      templateCode: selectedTemplateCode.value || null,
      initializedObjectId: createdPrimaryObjectId.value,
      initializedObjectCode: createdPrimaryObjectCode.value || null,
      initializationWarning: null,
    })
    emit('update:show', false)
  }
  finally {
    saving.value = false
  }
}

async function initializeApplicationFromTemplate(applicationId) {
  const response = await initializeBusinessApplicationTemplate(applicationId, {
    templateCode: selectedTemplateCode.value,
    primarySource: normalizeTemplateSource(templateConfig.primarySource),
    treeSource: initializeMode.value === 'TEMPLATE_TREE_TABLE'
      ? normalizeTemplateSource(templateConfig.treeSource)
      : null,
    treeKeyField: trimToNull(templateConfig.treeKeyField),
    treeLabelField: trimToNull(templateConfig.treeLabelField),
    treeParentField: trimToNull(templateConfig.treeParentField),
    primaryTreeField: trimToNull(templateConfig.primaryTreeField),
    primaryKeyField: trimToNull(templateConfig.primaryKeyField),
    details: (templateConfig.details || []).map(detail => ({
      source: normalizeTemplateSource(detail.source),
      foreignKeyField: trimToNull(detail.foreignKeyField),
      relationName: String(detail.relationName || '').trim(),
    })),
  })
  createdPrimaryObjectId.value = response.data?.primaryObjectId || null
  createdPrimaryObjectCode.value = response.data?.primaryObjectCode || ''
}

async function createApplicationDraft(payload) {
  const response = await createBusinessApplication(payload)
  createdApplicationId.value = response.data
  return response.data
}

async function initializeApplicationFromTable(applicationId) {
  if (!createdPrimaryObjectId.value) {
    const table = selectedTableInfo.value
    if (!table)
      throw new Error('请选择要导入的数据表')
    const objectName = String(table.tableComment || table.tableName || '').trim()
    const objectCode = normalizeObjectCode(table.tableName, objectName)
    const response = await createBusinessObject({
      suiteCode: form.suiteCode,
      objectName,
      objectCode,
      modelCode: buildModelCode(form.suiteCode, objectCode),
      objectType: 'MASTER',
      icon: trimToNull(form.icon),
      description: `从数据表 ${table.tableName} 导入`,
      status: 1,
      createMode: 'DB_IMPORT',
      runtimeDatasourceId: Number(runtimeDatasourceId.value),
      importDatasourceId: Number(runtimeDatasourceId.value),
      importTableName: table.tableName,
      options: JSON.stringify(buildDatabaseObjectOptions(table)),
    })
    createdPrimaryObjectId.value = response.data
    createdPrimaryObjectCode.value = objectCode
  }
  await saveBusinessApplicationObjects(applicationId, [{
    objectId: createdPrimaryObjectId.value,
    objectRole: 'PRIMARY',
    sortOrder: 0,
  }])
}

function buildDatabaseObjectOptions(table) {
  const datasourceId = Number(runtimeDatasourceId.value)
  const datasource = datasourceList.value.find(item => Number(item.datasourceId) === datasourceId) || {}
  return {
    createMode: 'DB_IMPORT',
    runtimeDatasourceId: datasourceId,
    runtimeDatasource: {
      datasourceId,
      datasourceCode: datasource.datasourceCode || '',
      datasourceName: datasource.datasourceName || '',
      dbType: datasource.dbType || '',
      usageScope: datasource.usageScope || '',
      allowWrite: datasource.allowRuntimeWrite === 1,
      allowDdl: datasource.allowRuntimeDdl === 1,
      readonly: datasource.readonly === 1,
      riskLevel: datasource.riskLevel || '',
      tableMode: 'EXISTING',
    },
    sourceTable: {
      datasourceId,
      tableName: table.tableName,
      tableComment: table.tableComment || null,
    },
  }
}

async function loadDatasources() {
  if (datasourceLoading.value)
    return
  if (datasourceList.value.length) {
    selectDefaultDatasource()
    return
  }
  datasourceLoading.value = true
  try {
    const response = await genDatasourceEnabled('LOWCODE_RUNTIME')
    datasourceList.value = response.data || []
    selectDefaultDatasource()
  }
  finally {
    datasourceLoading.value = false
  }
}

function selectDefaultDatasource() {
  if (runtimeDatasourceId.value && datasourceList.value.some(
    item => Number(item.datasourceId) === Number(runtimeDatasourceId.value),
  )) {
    return
  }
  const datasource = datasourceList.value.find(item => item.isDefault === 1) || datasourceList.value[0]
  runtimeDatasourceId.value = datasource?.datasourceId || null
}

async function loadTables(datasourceId) {
  importTableName.value = null
  tableList.value = []
  if (!datasourceId)
    return
  tableLoading.value = true
  try {
    const response = await genDatasourceTables(datasourceId)
    tableList.value = response.data || []
  }
  finally {
    tableLoading.value = false
  }
}

function buildPayload() {
  return {
    id: form.id || undefined,
    applicationCode: normalizeCode(form.applicationCode),
    applicationName: String(form.applicationName || '').trim(),
    suiteCode: form.suiteCode,
    icon: trimToNull(form.icon),
    description: trimToNull(form.description),
    status: Number(form.status) === 0 ? 0 : 1,
  }
}

function normalizeCode(value) {
  return String(value || '')
    .trim()
    .replace(/[\s-]+/g, '_')
    .replace(/\W/g, '')
    .toLowerCase()
}

function updateTemplateConfig(value) {
  Object.assign(templateConfig, value || {})
}

function ensureTemplateDefaults() {
  const applicationName = String(form.applicationName || '').trim() || '业务数据'
  if (!templateConfig.primarySource)
    templateConfig.primarySource = defaultTemplateSource()
  if (!templateConfig.treeSource)
    templateConfig.treeSource = defaultTemplateSource()
  if (!templateConfig.details?.length) {
    templateConfig.details = [{
      clientKey: `detail_${Date.now()}`,
      source: defaultTemplateSource(),
      foreignKeyField: null,
      relationName: `${applicationName}明细`,
    }]
  }
}

function validateTemplateConfig() {
  ensureTemplateDefaults()
  if (!validateTemplateSource(templateConfig.primarySource, '主对象'))
    return false
  if (initializeMode.value === 'TEMPLATE_TREE_TABLE') {
    if (!validateTemplateSource(templateConfig.treeSource, '左侧树对象'))
      return false
    if (![templateConfig.treeKeyField, templateConfig.treeLabelField, templateConfig.treeParentField, templateConfig.primaryTreeField]
      .every(isValidFieldCode)) {
      message.warning('请从真实字段中补全树主键、显示字段、父级字段和主表筛选字段')
      return false
    }
  }
  if (initializeMode.value === 'TEMPLATE_MASTER_DETAIL') {
    if (!templateConfig.details?.length) {
      message.warning('主子表模板至少需要一个子表')
      return false
    }
    if (!isValidFieldCode(templateConfig.primaryKeyField)) {
      message.warning('请选择主对象主键')
      return false
    }
    for (let index = 0; index < templateConfig.details.length; index++) {
      const detail = templateConfig.details[index]
      if (!validateTemplateSource(detail.source, `子表 ${index + 1}`))
        return false
      if (!isValidFieldCode(detail.foreignKeyField)) {
        message.warning(`请选择子表 ${index + 1} 中关联主对象的外键字段`)
        return false
      }
    }
  }
  return true
}

function validateTemplateSource(source, label) {
  if (source?.sourceType === 'EXISTING_OBJECT' && source.objectId)
    return true
  if (source?.sourceType === 'DATABASE_TABLE' && source.datasourceId && source.tableName)
    return true
  message.warning(`${label}请选择数据库表或已有业务对象`)
  return false
}

function isValidFieldCode(value) {
  return /^[a-z][a-zA-Z0-9]{0,63}$/.test(String(value || '').trim())
}

function normalizeTemplateSource(source = {}) {
  return {
    sourceType: source.sourceType,
    objectId: source.sourceType === 'EXISTING_OBJECT' ? source.objectId : null,
    datasourceId: source.sourceType === 'DATABASE_TABLE' ? Number(source.datasourceId) : null,
    tableName: source.sourceType === 'DATABASE_TABLE' ? trimToNull(source.tableName) : null,
  }
}

function trimToNull(value) {
  const text = String(value || '').trim()
  return text || null
}

function defaultForm() {
  return {
    id: null,
    applicationCode: '',
    applicationName: '',
    suiteCode: null,
    icon: '',
    description: '',
    status: 1,
    designStatus: 'DRAFT',
    objectCount: 0,
    entryCount: 0,
  }
}

function defaultTemplateConfig() {
  return {
    primarySource: defaultTemplateSource(),
    treeSource: defaultTemplateSource(),
    treeKeyField: null,
    treeLabelField: null,
    treeParentField: null,
    primaryTreeField: null,
    primaryKeyField: null,
    details: [{
      clientKey: 'detail_default',
      source: defaultTemplateSource(),
      foreignKeyField: null,
      relationName: '明细',
    }],
  }
}

function defaultTemplateSource() {
  return {
    sourceType: 'DATABASE_TABLE',
    objectId: null,
    datasourceId: null,
    tableName: null,
  }
}
</script>

<style scoped>
.application-steps {
  margin-bottom: 24px;
  padding: 14px 18px;
  border: 1px solid var(--n-border-color, #e5e7eb);
  border-radius: 10px;
  background: var(--n-color-embedded, #f7f8fa);
}

.initialize-panel {
  display: grid;
  gap: 18px;
}

.drawer-footer {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
}
</style>
