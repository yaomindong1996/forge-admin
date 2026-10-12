<template>
  <div class="flow-readonly-form-panel">
    <div v-if="formInfoLoading" class="form-loading">
      <n-spin size="small" />
      <span>加载表单内容中...</span>
    </div>

    <template v-else>
      <FlowApprovalChecklist
        :responsibility-description="taskFormInfo?.responsibilityDescription || ''"
        :approval-points="taskFormInfo?.approvalPoints || []"
        :legacy-approval-point="taskFormInfo?.approvalPoint || ''"
        readonly
      />

      <FlowBusinessForm
        v-if="useExternalForm"
        :form-url="taskFormInfo.formUrl"
        :task-id="taskFormInfo.taskId"
        :business-key="taskFormInfo.businessKey"
        :process-instance-id="taskFormInfo.processInstanceId"
        :task-def-key="taskFormInfo.taskDefKey"
        :process-def-key="taskFormInfo.processDefKey"
        :variables="taskFormInfo.variables || {}"
        :approval-policy="readonlyApprovalPolicy"
        read-only
        @submit="noop"
      />

      <div v-else-if="businessFormLoading" class="form-loading">
        <n-spin size="small" />
        <span>加载业务表单中...</span>
      </div>

      <div v-else-if="useBusinessManagedForm" class="business-task-form-section readonly">
        <div class="approval-form-title">
          <span>{{ businessFormTitle }}</span>
        </div>
        <AiForm
          v-model:value="businessFormData"
          :schema="readonlyBusinessFormFields"
          :field-permissions="readonlyBusinessFormFieldPermissions"
          :show-actions="false"
          :show-feedback="false"
          :grid-cols="businessFormGridCols"
          :label-placement="businessFormLabelPlacement"
          :label-width="businessFormLabelWidth"
          :size="businessFormSize"
          :context="businessFormRenderContext"
          :form-assets="businessFormContext.formAssets || []"
        />
        <ChildTableEditor
          v-if="businessFormChildrenConfig.length"
          v-model:value="businessChildFormData"
          :children-config="businessFormChildrenConfig"
          readonly
          :parent-form-data="businessFormData"
          :context="businessFormRenderContext"
        />
        <div v-if="businessFormWarnings.length" class="business-form-warnings">
          <n-alert v-for="warning in businessFormWarnings" :key="warning" type="warning" :show-icon="false">
            {{ warning }}
          </n-alert>
        </div>
        <div v-if="businessCodeFormUrl" class="business-form-actions">
          <NButton type="primary" secondary @click="openBusinessCodeForm">
            打开完整业务页
          </NButton>
        </div>
      </div>

      <div v-if="useDynamicForm" class="dynamic-form-section readonly">
        <div class="approval-form-title">
          节点动态表单
        </div>
        <AiForm
          v-model:value="dynamicFormData"
          :schema="readonlyDynamicFormSchema"
          :field-permissions="readonlyDynamicFormFieldPermissions"
          :show-actions="false"
          :show-feedback="false"
          :grid-cols="2"
          label-placement="top"
        />
      </div>

      <n-empty v-if="showNoFormContent" :description="emptyText" size="small" />
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { businessTaskFormReadonlyContext } from '@/api/business-app'
import flowApi from '@/api/flow'
import { AiForm } from '@/components/ai-form'
import { formCreateToAiSchema } from '@/components/ai-form/adapters/formCreate'
import FlowBusinessForm from '@/components/common/FlowBusinessForm.vue'
import FlowApprovalChecklist from '@/components/flow/FlowApprovalChecklist.vue'
import ChildTableEditor from '@/components/page-templates/ChildTableEditor.vue'
import { pickFirstNonEmptyFieldPermissions, pickFirstNonEmptyPermissionSource } from '@/utils/field-permissions'
import { childTableKeysAlias } from '@/utils/flow-field-permissions'
import { compactParams } from '@/views/flow/utils/monitorAdmin'
import { getBusinessFormDisplayTitle } from '@/views/flow/utils/processDisplay'
import { resolveBusinessTaskAiFormSchema } from '@/views/flow/utils/resolveBusinessTaskAiFormSchema'

const props = defineProps({
  row: { type: Object, default: () => ({}) },
  source: { type: String, default: 'flowDone' },
  emptyText: { type: String, default: '暂无可展示的表单内容' },
})

const router = useRouter()
const taskFormInfo = ref(null)
const formInfoLoading = ref(false)
const dynamicFormData = ref({})
const businessFormContext = ref(null)
const businessFormData = ref({})
const businessChildFormData = ref({})
const businessFormLoading = ref(false)

const readonlyApprovalPolicy = {
  allowApprove: false,
  allowReject: false,
  allowDelegate: false,
  allowReturn: false,
  allowTerminate: false,
  requireComment: false,
  requireSignature: false,
}

const dynamicFormSchema = computed(() => formCreateToAiSchema(taskFormInfo.value?.formJson || []))
const useBusinessObjectForm = computed(() => businessFormContext.value?.configured === true && businessFormContext.value?.formType === 'business-object')
const useBusinessCodeForm = computed(() => businessFormContext.value?.configured === true && businessFormContext.value?.formType === 'business-code')
const useBusinessManagedForm = computed(() => useBusinessObjectForm.value || useBusinessCodeForm.value)
const useDynamicForm = computed(() => {
  if (useBusinessManagedForm.value)
    return false
  return dynamicFormSchema.value.length > 0
})
const useExternalForm = computed(() => !useBusinessManagedForm.value && taskFormInfo.value?.formType === 'external' && taskFormInfo.value?.formUrl)
const businessFormTitle = computed(() => getBusinessFormDisplayTitle(businessFormContext.value, '业务表单'))
const businessFormWarnings = computed(() => Array.isArray(businessFormContext.value?.warnings) ? businessFormContext.value.warnings : [])
const businessFormChildrenConfig = computed(() => {
  const children = Array.isArray(businessFormContext.value?.childrenConfig) ? businessFormContext.value.childrenConfig : []
  return children.filter(child => child?.showInDetail !== false && Array.isArray(child.fields) && child.fields.length)
})
const businessCodeFormUrl = computed(() => businessFormContext.value?.formUrl || businessFormContext.value?.formRef?.formUrl || '')
const businessFormGridCols = computed(() => Math.max(1, Number(businessFormContext.value?.gridCols || 1)))
const businessFormLabelPlacement = computed(() => ['left', 'top'].includes(businessFormContext.value?.labelPlacement)
  ? businessFormContext.value.labelPlacement
  : 'left')
const businessFormLabelWidth = computed(() => businessFormContext.value?.labelWidth || '100')
const businessFormSize = computed(() => ['small', 'medium', 'large'].includes(businessFormContext.value?.size)
  ? businessFormContext.value.size
  : 'medium')
const businessFormRenderContext = computed(() => ({
  task: props.row,
  taskFormInfo: taskFormInfo.value,
  businessFormContext: businessFormContext.value,
  formAssets: businessFormContext.value?.formAssets || [],
  protocolVersion: businessFormContext.value?.protocolVersion || null,
  uiDocument: businessFormContext.value?.uiDocument || null,
}))
const readonlyBusinessFormFieldPermissions = computed(() => {
  return pickFirstNonEmptyFieldPermissions([
    businessFormContext.value?.fieldPermissions,
    taskFormInfo.value?.fieldPermissions,
    taskFormInfo.value?.formFieldPermissions,
  ], { readOnly: true })
})
const readonlyDynamicFormFieldPermissions = computed(() => {
  return pickFirstNonEmptyPermissionSource([
    taskFormInfo.value?.fieldPermissions,
    taskFormInfo.value?.formFieldPermissions,
  ])
})
const readonlyDynamicFormSchema = computed(() => dynamicFormSchema.value.map(toReadonlyField))
const readonlyBusinessFormFields = computed(() => {
  return resolveBusinessTaskAiFormSchema(businessFormContext.value).map(toReadonlyField)
})
const showNoFormContent = computed(() => {
  if (formInfoLoading.value || businessFormLoading.value)
    return false
  const hasDuty = Boolean(taskFormInfo.value?.responsibilityDescription)
    || Boolean(taskFormInfo.value?.approvalPoint)
    || (Array.isArray(taskFormInfo.value?.approvalPoints) && taskFormInfo.value.approvalPoints.length > 0)
  return !useExternalForm.value && !useDynamicForm.value && !useBusinessManagedForm.value && !hasDuty
})

function toReadonlyField(field = {}) {
  return {
    ...field,
    writable: false,
    readonly: true,
    disabled: true,
    props: {
      ...(field.props || {}),
      disabled: true,
      readonly: true,
    },
  }
}

function parseJsonObject(value) {
  if (!value)
    return {}
  if (typeof value === 'object')
    return { ...value }
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch {
    return {}
  }
}

function resetReadonlyForm() {
  taskFormInfo.value = null
  dynamicFormData.value = {}
  businessFormContext.value = null
  businessFormData.value = {}
  businessChildFormData.value = {}
  formInfoLoading.value = false
  businessFormLoading.value = false
}

function buildProcessFormInfoQuery(row = {}) {
  return compactParams({
    taskId: row.taskId || row.id,
    businessKey: row.businessKey,
    processInstanceId: row.processInstanceId,
    processDefKey: row.processDefKey || row.processDefinitionKey,
    taskDefKey: row.taskDefKey || row.taskDefinitionKey,
  })
}

function buildBusinessReadonlyQuery(row = {}, formInfo = {}) {
  return compactParams({
    taskId: formInfo.taskId || row.taskId || row.id,
    businessKey: formInfo.businessKey || row.businessKey,
    processInstanceId: formInfo.processInstanceId || row.processInstanceId || row.id,
    processDefKey: formInfo.processDefKey || row.processDefKey || row.processDefinitionKey,
    taskDefKey: formInfo.taskDefKey || row.taskDefKey || row.taskDefinitionKey,
    objectCode: formInfo.objectCode || row.objectCode,
    recordId: formInfo.recordId || row.recordId,
    formKey: formInfo.formKey,
  })
}

function hasBusinessReadonlyQuery(query = {}) {
  return Boolean(query.processInstanceId || query.businessKey || (query.objectCode && query.recordId))
}

function normalizeBusinessRecordData(recordData) {
  if (recordData && typeof recordData === 'object' && !Array.isArray(recordData)) {
    const main = recordData.main
    if (main && typeof main === 'object' && !Array.isArray(main))
      return { ...main }
    const { children, ...mainRecord } = recordData
    return { ...mainRecord }
  }
  return {}
}

function normalizeBusinessChildrenData(recordData) {
  const source = recordData?.children && typeof recordData.children === 'object' && !Array.isArray(recordData.children)
    ? recordData.children
    : {}
  const result = {}
  const usedSourceKeys = new Set()
  businessFormChildrenConfig.value.forEach((child) => {
    const key = resolveBusinessChildKey(child)
    const matched = findBusinessChildRows(source, child, usedSourceKeys)
    result[key] = matched.rows
    if (matched.sourceKey)
      usedSourceKeys.add(matched.sourceKey)
  })
  return result
}

function findBusinessChildRows(source = {}, child = {}, usedSourceKeys = new Set()) {
  const candidates = [
    child.modelCode,
    child.relationKey,
    child.key,
    child.tableName,
  ].map(value => String(value || '').trim()).filter(Boolean)

  for (const candidate of candidates) {
    if (usedSourceKeys.has(candidate))
      continue
    if (Array.isArray(source[candidate]))
      return { rows: source[candidate], sourceKey: candidate }
  }

  let bestKey = ''
  let bestDelta = Number.POSITIVE_INFINITY
  Object.keys(source || {}).forEach((dataKey) => {
    if (usedSourceKeys.has(dataKey) || !Array.isArray(source[dataKey]))
      return
    const matched = candidates.some(candidate => childTableKeysAlias(candidate, dataKey))
    if (!matched)
      return
    const delta = Math.min(...candidates.map(candidate => Math.abs(candidate.length - dataKey.length)))
    if (delta < bestDelta) {
      bestDelta = delta
      bestKey = dataKey
    }
  })
  if (bestKey)
    return { rows: source[bestKey], sourceKey: bestKey }
  return { rows: [], sourceKey: '' }
}

function resolveBusinessChildKey(child = {}) {
  return child.modelCode || child.relationKey || child.key || child.tableName || 'children'
}

async function loadReadonlyBusinessTaskFormContext(row, formInfo) {
  businessFormContext.value = null
  businessFormData.value = {}
  businessChildFormData.value = {}
  const query = buildBusinessReadonlyQuery(row, formInfo)
  if (!hasBusinessReadonlyQuery(query))
    return null

  businessFormLoading.value = true
  try {
    const res = await businessTaskFormReadonlyContext(query)
    if (res.code !== 200)
      return null
    businessFormContext.value = res.data || null
    businessFormData.value = normalizeBusinessRecordData(res.data?.recordData)
    businessChildFormData.value = normalizeBusinessChildrenData(res.data?.recordData)
    return businessFormContext.value
  }
  catch (error) {
    console.error('加载业务表单只读上下文失败', error)
    return null
  }
  finally {
    businessFormLoading.value = false
  }
}

async function loadReadonlyFormInfo(row = {}) {
  const query = buildProcessFormInfoQuery(row)
  if (!query.processInstanceId && !query.businessKey && !query.taskId) {
    resetReadonlyForm()
    return
  }

  formInfoLoading.value = true
  try {
    const res = await flowApi.getProcessFormInfo(query)
    if (res.code !== 200) {
      resetReadonlyForm()
      return
    }
    const formInfo = res.data || {}
    taskFormInfo.value = formInfo
    dynamicFormData.value = {
      ...(formInfo.variables || {}),
      ...parseJsonObject(formInfo.formData),
    }
    await loadReadonlyBusinessTaskFormContext(row, formInfo)
  }
  catch (error) {
    console.error('加载流程表单只读信息失败', error)
    resetReadonlyForm()
  }
  finally {
    formInfoLoading.value = false
  }
}

function openBusinessCodeForm() {
  const url = businessCodeFormUrl.value
  if (!url)
    return
  if (/^https?:\/\//i.test(url)) {
    window.open(url, '_blank', 'noopener,noreferrer')
    return
  }
  router.push({
    path: url,
    query: compactParams({
      taskId: businessFormContext.value?.taskId || taskFormInfo.value?.taskId || props.row?.taskId,
      businessKey: businessFormContext.value?.businessKey,
      processInstanceId: businessFormContext.value?.processInstanceId,
      taskDefKey: businessFormContext.value?.taskDefKey,
      processDefKey: businessFormContext.value?.processDefKey,
      objectCode: businessFormContext.value?.objectCode,
      recordId: businessFormContext.value?.recordId,
      source: props.source,
      readOnly: 'true',
    }),
  })
}

function noop() {}

watch(
  () => [
    props.row?.id,
    props.row?.processInstanceId,
    props.row?.businessKey,
    props.row?.taskId,
    props.row?.taskDefKey,
    props.row?.processDefKey,
  ],
  () => {
    loadReadonlyFormInfo(props.row)
  },
  { immediate: true },
)
</script>

<style scoped>
.form-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 88px;
  color: var(--text-secondary);
}

.dynamic-form-section,
.business-task-form-section {
  margin-bottom: 16px;
  padding: 14px;
  border: 1px solid #d7dde7;
  border-radius: 8px;
  background: var(--bg-secondary);
}

.dynamic-form-section.readonly,
.business-task-form-section.readonly {
  background: var(--bg-secondary);
}

.approval-form-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.business-form-warnings {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.business-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
</style>
