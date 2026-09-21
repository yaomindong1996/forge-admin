import { defineStore } from 'pinia'
import { businessTaskFormContext, businessTaskFormReadonlyContext } from '@/api/business-app'
import flowApi from '@/api/flow'

const FLOW_SCENES = new Set(['FLOW_TODO', 'FLOW_DONE', 'FLOW_STARTED'])

function text(value) {
  const result = String(value ?? '').trim()
  return result || ''
}

function object(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
}

function compact(source) {
  return Object.fromEntries(Object.entries(source).filter(([, value]) => value !== undefined && value !== null && value !== ''))
}

function parseFormRef(formInfo = {}) {
  if (formInfo.formRef && typeof formInfo.formRef === 'object' && !Array.isArray(formInfo.formRef))
    return formInfo.formRef
  if (typeof formInfo.formRef === 'string' && formInfo.formRef.trim()) {
    try {
      return object(JSON.parse(formInfo.formRef))
    }
    catch {
      return {}
    }
  }
  return {}
}

function first(...values) {
  return values.map(text).find(Boolean) || ''
}

function requireValue(value, message) {
  const result = text(value)
  if (!result)
    throw new Error(message)
  return result
}

function recordIdFromBusinessKey(value, objectCode) {
  const businessKey = text(value)
  const prefix = `${text(objectCode)}:`
  if (!businessKey || prefix === ':' || !businessKey.startsWith(prefix))
    return ''
  return businessKey.slice(prefix.length).replace(/:R\d+$/, '')
}

function resolveProcessRunId(row, formInfo, context) {
  const variables = object(formInfo.variables)
  const contextVariables = object(object(context.taskFormInfo).variables)
  return first(context.processRunId, variables.processRunId, contextVariables.processRunId, row.processRunId)
}

export function flowPrintIdentityKey(row = {}, scene = '') {
  return [scene, row.taskId || row.id, row.processInstanceId, row.businessKey, row.recordId].map(text).join('|')
}

export function buildFlowPrintFormQuery(row = {}, formInfo = {}) {
  const formRef = parseFormRef(formInfo)
  return compact({
    taskId: formInfo.taskId || row.taskId || row.id,
    businessKey: formInfo.businessKey || row.businessKey,
    processInstanceId: formInfo.processInstanceId || row.processInstanceId,
    processDefKey: formInfo.processDefKey || row.processDefKey || row.processDefinitionKey,
    taskDefKey: formInfo.taskDefKey || row.taskDefKey || row.taskDefinitionKey,
    objectCode: formInfo.objectCode || formRef.objectCode || row.objectCode,
    recordId: formInfo.recordId || formRef.recordId || row.recordId,
    formKey: formInfo.formKey || formRef.formKey,
  })
}

export function resolveFlowPrintRecord({ row = {}, scene, formInfo = {}, businessContext = {} }) {
  if (!FLOW_SCENES.has(scene))
    throw new Error('流程打印场景无效')

  const context = object(businessContext)
  const contextRef = object(context.formRef)
  const formRef = parseFormRef(formInfo)
  const formMode = first(context.formType, contextRef.formMode, contextRef.type, formInfo.formMode, formRef.formMode, formRef.type).toUpperCase()
  const sourceType = formMode === 'BUSINESS_CODE_FORM' || formMode === 'BUSINESS-CODE' || formMode === 'BUSINESS-CODE-FORM'
    ? 'CODE'
    : 'LOWCODE'
  const applicationId = requireValue(first(context.applicationId, contextRef.applicationId, formRef.applicationId, row.applicationId), '当前流程表单缺少应用身份，无法打印')
  const objectCode = requireValue(first(context.objectCode, contextRef.objectCode, formInfo.objectCode, formRef.objectCode, row.objectCode), '当前流程表单缺少业务对象，无法打印')
  const businessKey = first(context.businessKey, formInfo.businessKey, row.businessKey)
  const recordId = requireValue(first(
    context.recordId,
    formInfo.recordId,
    formRef.recordId,
    row.recordId,
    recordIdFromBusinessKey(businessKey, objectCode),
  ), '当前流程没有可打印的已保存记录')
  const processInstanceId = requireValue(first(context.processInstanceId, formInfo.processInstanceId, row.processInstanceId), '当前流程缺少实例身份，无法打印')
  const taskId = scene === 'FLOW_STARTED'
    ? null
    : requireValue(first(context.taskId, formInfo.taskId, row.taskId, row.id), '当前流程缺少任务身份，无法打印')
  const processRunId = resolveProcessRunId(row, formInfo, context)

  const source = {
    applicationId,
    sourceType,
    pageId: sourceType === 'LOWCODE'
      ? requireValue(first(context.pageId, contextRef.pageId, formRef.pageId, row.pageId), '当前低代码流程表单缺少发布页面身份')
      : null,
    formKey: sourceType === 'CODE'
      ? requireValue(first(context.formKey, formInfo.formKey, contextRef.formKey, formRef.formKey), '当前代码流程表单缺少 formKey')
      : null,
    objectCode,
  }

  if (sourceType === 'LOWCODE' && !processRunId)
    throw new Error('当前低代码流程缺少运行版本身份，无法打印')

  return compact({
    source,
    recordId,
    scene,
    taskId,
    processInstanceId,
    processRunId: processRunId || undefined,
  })
}

async function loadFormInfo(row, scene) {
  const query = buildFlowPrintFormQuery(row)
  const response = scene === 'FLOW_TODO' && query.taskId
    ? await flowApi.getTaskFormInfo(query.taskId)
    : await flowApi.getProcessFormInfo(query)
  if (response.code !== 200)
    throw new Error(response.message || '流程表单身份加载失败')
  return object(response.data)
}

async function loadBusinessContext(row, scene, formInfo) {
  const query = buildFlowPrintFormQuery(row, formInfo)
  const loader = scene === 'FLOW_TODO' ? businessTaskFormContext : businessTaskFormReadonlyContext
  const response = await loader(query)
  if (response.code !== 200)
    throw new Error(response.message || '业务表单身份加载失败')
  const context = object(response.data)
  if (context.configured !== true)
    throw new Error(context.warnings?.[0] || '当前流程没有可打印的业务表单')
  return context
}

export const useFlowPrintContextStore = defineStore('flowPrintContext', {
  state: () => ({
    record: null,
    identityKey: '',
    loading: false,
    error: '',
    generation: 0,
  }),
  actions: {
    reset() {
      this.generation += 1
      this.record = null
      this.identityKey = ''
      this.loading = false
      this.error = ''
    },
    async sync({ row = {}, scene, formInfo = null, businessContext = null }) {
      this.reset()
      const generation = this.generation
      const identityKey = flowPrintIdentityKey(row, scene)
      this.loading = true
      try {
        const resolvedFormInfo = formInfo && typeof formInfo === 'object'
          ? formInfo
          : await loadFormInfo(row, scene)
        const resolvedBusinessContext = businessContext?.configured === true
          ? businessContext
          : await loadBusinessContext(row, scene, resolvedFormInfo)
        const record = resolveFlowPrintRecord({
          row,
          scene,
          formInfo: resolvedFormInfo,
          businessContext: resolvedBusinessContext,
        })
        if (generation !== this.generation)
          return null
        this.record = record
        this.identityKey = identityKey
        return record
      }
      catch (error) {
        if (generation === this.generation)
          this.error = error?.message || '流程打印上下文加载失败'
        return null
      }
      finally {
        if (generation === this.generation)
          this.loading = false
      }
    },
  },
})
