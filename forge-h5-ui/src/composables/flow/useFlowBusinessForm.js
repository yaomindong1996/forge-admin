import { computed, reactive } from 'vue'
import api from '@/api'
import { useAuthStore } from '@/store'
import { useBusinessTaskFormState } from '@/composables/lowcode/useBusinessTaskFormState'
import { hasDeclaredFormCreateRules } from '@/utils/form-create-mobile'
import { resolveTaskUiDocument } from '@/utils/task-ui-document'
import { normalizeDictOptions } from '@/utils/lowcode-runtime'
import { toast } from '@/utils/notify'
import {
  adaptBusinessTaskFields,
  adaptChildrenConfig,
  buildBusinessTaskFormData,
  buildDefaultPageSections,
  buildFlowInteraction,
  collectDictTypes,
  extractMainData,
  extractPageSections,
  hasWritableBusinessTaskForm,
} from '@/utils/business-task-form-adapter'

export function isConfiguredBusinessTaskForm(context) {
  return context?.configured === true && ['business-object', 'business-code'].includes(context?.formType)
}

/**
 * 审批类页面共用的业务表单状态，返回值用 reactive() 包裹后传给 FlowBusinessFormPanel。
 * @param {object} options
 * @param {import('vue').Ref} options.businessContext task-form-context 接口返回
 * @param {import('vue').Ref} options.formInfo Flow 表单快照
 * @param {import('vue').Ref} [options.businessContextError]
 * @param {() => boolean} options.isReadonly
 * @param {() => object} [options.getRouteQuery]
 * @param {(info: object) => void} [options.seedApprovalPointChecks]
 */
export function useFlowBusinessForm(options) {
  const { businessContext, formInfo, businessContextError, seedApprovalPointChecks } = options
  const mainData = reactive({})
  const childData = reactive({})
  const dictOptions = reactive({})
  const schema = useBusinessFormSchema(options)
  const blockedReason = computed(() => resolveBlockedReason({
    contextError: businessContextError?.value || '',
    formInfo: formInfo.value,
    context: businessContext.value,
    hasLowcodeForm: schema.hasLowcodeForm.value,
  }))
  const state = useBusinessTaskFormState({
    mainData,
    childData,
    formInfo,
    seedApprovalPointChecks: seedApprovalPointChecks || (() => {}),
    getMode: () => schema.formMode.value,
  })
  const formRefs = useBusinessFormRefs()

  async function loadDictOptions() {
    const types = collectDictTypes(schema.mainFields.value, schema.allChildren.value)
    await Promise.all([...types].map(async type => {
      try { dictOptions[type] = normalizeDictOptions((await api.getDictOptions(type))?.data) }
      catch { dictOptions[type] = [] }
    }))
  }

  function buildCurrentBusinessFormData() {
    return buildBusinessTaskFormData({
      formType: businessContext.value?.formType,
      fields: schema.mainFields.value,
      children: schema.allChildren.value,
      mainData,
      childData,
    })
  }

  return {
    mainData,
    childData,
    dictOptions,
    ...schema,
    blockedReason,
    applyBusinessContext: state.applyBusinessContext,
    resetBusinessData: state.resetBusinessData,
    addBusinessChildRow: state.addBusinessChildRow,
    removeBusinessChildRow: state.removeBusinessChildRow,
    ...formRefs,
    loadDictOptions,
    buildCurrentBusinessFormData,
  }
}

function useBusinessFormSchema({ businessContext, isReadonly, getRouteQuery }) {
  const authStore = useAuthStore()
  const businessSchemaFallback = computed(() => {
    const context = businessContext.value || {}
    if (Array.isArray(context.fields) && context.fields.length) return []
    return context.formRef?.fields || context.formRef?.fieldCatalog || []
  })
  const businessProviderUnavailable = computed(() => {
    const warnings = Array.isArray(businessContext.value?.warnings) ? businessContext.value.warnings : []
    return warnings.some(item => String(item).includes('Provider未注册'))
  })
  const documentForm = computed(() => resolveTaskUiDocument(businessContext.value, businessSchemaFallback.value))
  const mainNodes = computed(() => documentForm.value?.nodes || [])
  const mainFields = computed(() => {
    if (documentForm.value) return documentForm.value.fields
    const context = businessContext.value
    if (Array.isArray(context?.fields) && context.fields.length)
      return adaptBusinessTaskFields(context.fields, context.fieldPermissions)
    if (businessSchemaFallback.value.length)
      return adaptBusinessTaskFields(businessSchemaFallback.value, context?.fieldPermissions)
    return []
  })
  const allChildren = computed(() => adaptChildrenConfig(
    businessContext.value?.childrenConfig || [],
    businessContext.value?.fieldPermissions || [],
  ))
  const hasLowcodeForm = computed(() => mainFields.value.length > 0 || allChildren.value.length > 0)
  const formMode = computed(() => {
    const fallbackOnly = businessSchemaFallback.value.length > 0 && !documentForm.value?.hasComponentTree
    if (isReadonly() || businessProviderUnavailable.value || fallbackOnly) return 'detail'
    return hasWritableBusinessTaskForm(mainFields.value, allChildren.value) ? 'edit' : 'detail'
  })
  return {
    mainNodes,
    mainFields,
    allChildren,
    pageSections: computed(() => buildDefaultPageSections(
      mainFields.value,
      allChildren.value,
      documentForm.value?.sections || extractPageSections(businessContext.value || {}),
    )),
    flowInteraction: computed(() => buildFlowInteraction(businessContext.value || {})),
    currentFlowNodeKey: computed(() => String(businessContext.value?.taskDefKey || '')),
    runtimeContext: computed(() => ({
      routeQuery: getRouteQuery?.() || {},
      user: authStore.userInfo || {},
      currentUser: authStore.userInfo || {},
    })),
    hasLowcodeForm,
    showBusinessFormPanel: computed(() => Boolean(
      businessContext.value && (isConfiguredBusinessTaskForm(businessContext.value) || hasLowcodeForm.value),
    )),
    formMode,
    businessFormHasWritableFields: computed(() =>
      hasWritableBusinessTaskForm(mainFields.value, allChildren.value) && formMode.value === 'edit',
    ),
    businessProviderUnavailable,
    formSchemaUnavailable: computed(() =>
      !hasLowcodeForm.value && Object.keys(extractMainData(businessContext.value?.recordData) || {}).length > 0,
    ),
  }
}

function useBusinessFormRefs() {
  const mainSectionFormRefs = new Map()
  const childFormRefs = new Map()

  function setMainFormRef({ sectionId, instance }) {
    const key = String(sectionId || 'main')
    if (instance) mainSectionFormRefs.set(key, instance)
    else mainSectionFormRefs.delete(key)
  }

  function setChildFormRef({ child, row, rowIndex, instance }) {
    if (!child) return
    const key = `${child.modelCode}:${row?.id || rowIndex || 0}`
    if (instance) childFormRefs.set(key, instance)
    else childFormRefs.delete(key)
  }

  function validateRequiredFields() {
    const forms = [...mainSectionFormRefs.values(), ...childFormRefs.values()]
    const invalid = forms.find(form => form?.validate?.() === false)
    if (invalid) {
      toast('请完善必填字段', { type: 'warning' })
      return false
    }
    return true
  }

  return { setMainFormRef, setChildFormRef, validateRequiredFields }
}

function resolveBlockedReason({ contextError, formInfo, context, hasLowcodeForm }) {
  if (contextError)
    return `业务表单加载失败：${contextError}`
  if (!formInfo && !isConfiguredBusinessTaskForm(context))
    return '未取得审批节点的表单和权限配置，请刷新后重试。'
  if (isConfiguredBusinessTaskForm(context) && !hasLowcodeForm)
    return '当前业务表单没有可在移动端渲染的字段配置，请检查流程节点表单资产。'
  if (formInfo?.formType === 'external' && formInfo?.formUrl && !hasLowcodeForm)
    return '此节点未提供可移动端渲染的字段描述，不能跳过 PC 专属表单直接审批。'
  if (hasDeclaredFormCreateRules(formInfo?.formJson) && !hasLowcodeForm)
    return '此动态表单没有可识别的字段描述，不能跳过填写直接审批。'
  return ''
}
