/** BusinessActionDesigner.vue setup part 2. */
import { NButton, NDropdown, NFormItemGi, NGrid, NInput, NInputNumber, NSelect, NTag } from 'naive-ui'
import { computed, defineComponent, h, ref, watch } from 'vue'
import { businessObjectDesigner, businessObjectList } from '@/api/business-app'
import {
  BUSINESS_ACTION_EXECUTION_MODE,
  canUseBusinessActionStep,
  createCallApiBusinessActionStep,
  createDefaultBusinessActionConfig,
  createLocalBusinessActionStep,
  LOCAL_TRANSACTION_STEP_TYPES,
  resolveBusinessActionExecutionMode,
} from '../business-action-designer-protocol'
import CallApiStepConfigPanel from '../CallApiStepConfigPanel.vue'
export function applyBusinessActionDesignerPart2(props, emit, deps = {}) {
  const {
    __impl, mut, actionSceneLabel, addAutomationAction, addCallApiStep, addDetailQuantityFlow, addExpectedFieldMapping, addFieldMapping,
    addInputSchemaField, addLocalStep, addNumberAdjustment, addQuantityStep, applyActionConfigText, buildActionRelations, buildCollectionPathOptions, buildStepVM,
    buildTargetConfigOptions, childBusinessSteps, collectCallbackActionMap, collectSchemaChildren, collectionKeyCandidates, collectionOptionsForStep, containsInternalStartFlow, createLocalStep,
    createQuantityStep, emitActions, flattenAllSteps, flattenRootSteps, hasConfiguredSteps, inputOptionsText, isAutomationAction, isDetailRelation,
    isInternalStepType, isQuantityStep, isSameRelation, loadBusinessObjects, loadTargetFields, lowerSnake, mappingSourcePlaceholder, mergeRelationFields,
    normalizeActionRelation, normalizeActionType, normalizeFields, patchActionConfig, patchAdjustment, patchExpectedFieldMapping, patchFieldMapping, patchInputSchemaField,
    patchSelectedAction, patchStep, patchStepConfig, preloadRelationFields, relationByCollectionPath, relationFields, removeAction, removeExpectedFieldMapping,
    removeFieldMapping, removeInputSchemaField, removeNumberAdjustment, removeStep, resolveActionScene, resolveCollectionPathLabel, resolveExecutionMode, stepAdjustments,
    stepExpectedFieldMappings, stepFieldMappings, stepTypeNeedsRecordId, targetFieldOptions, updateActionScene, updateCallApiStepConfig, updateChildActionRelation, updateExecutionMode,
    updateFallbackFields, updateInputOptions, updateLocalStepType, updateManualActionPosition, updateStepCollection, updateStepParam, INTERNAL_STEP, INTERNAL_ACTION,
    EXECUTION_MODE, LOCAL_STEP_TYPES, selectedActionIndex, actionConfigText, jsonError, businessObjects, targetFieldsMap, targetFieldLoadingMap,
    sceneOptions, manualActionPositionOptions, successBehaviorOptions, permissionStrategyOptions, executionModeOptions, inputTypeOptions, localStepTypeOptions, localStepMenuOptions,
    sourceTypeOptions, adjustmentOperatorOptions, quantityOperationOptions, paramLabels, actionList, approvalEntryActions, automationActions, pageInteractionActions,
    pageInteractionActionNames, selectedAction, rootSteps, resolvedExecutionMode, isLocalTransaction, localStepViews, nonLocalStepCount, inputSchemaRows,
    callApiRecordFieldOptions, callApiFormFieldOptions, targetConfigOptions, actionRelations, collectionPathOptions, childRelationOptions, selectedManualActionPosition, BusinessQuantityStepCard,
  } = deps
  function parseRelationConfig(value) {
    if (!value)
      return {}
    try {
      const parsed = JSON.parse(value)
      return parsed && typeof parsed === 'object' ? parsed : {}
    }
    catch {
      return {}
    }
  }

  function toPageField(field = {}) {
    return {
      ...field,
      field: field.field || field.fieldCode || field.sourceField,
      label: field.label || field.fieldName || field.name || field.fieldCode || field.sourceField,
      fieldStatus: field.fieldStatus,
      basicProps: { ...(field.basicProps || {}) },
      advancedProps: { ...(field.advancedProps || {}) },
    }
  }

  function isInactiveField(field = {}) {
    const status = String(field.fieldStatus || '').toUpperCase()
    return status === 'DISABLED' || status === 'HIDDEN'
  }

  function businessFieldLabel(field = {}) {
    const fieldName = field.field || field.fieldCode || field.sourceField || ''
    const systemLabels = {
      id: '记录ID',
      createBy: '创建人',
      createTime: '创建时间',
      updateBy: '修改人',
      updateTime: '修改时间',
      createDept: '创建部门',
      tenantId: '租户',
    }
    return systemLabels[fieldName] || field.label || field.fieldName || field.name || '未命名字段'
  }

  function fieldPathOptions(step = {}) {
    const options = []
    const seen = new Set()
    const add = (value, label) => {
      const text = String(value || '').trim()
      if (!text || seen.has(text))
        return
      seen.add(text)
      options.push({ label: label || '未命名字段', value: text })
    }
    collectMainFields(props.fields, props.modelSchema).forEach((sourceField) => {
      const field = toPageField(sourceField)
      const fieldCode = field.sourceField || field.field || field.fieldCode
      if (!fieldCode)
        return
      add(`record.main.${fieldCode}`, fieldDisplayLabel(field, '单据字段'))
    })
    const aliases = step.aliases?.length ? step.aliases : [{ alias: 'item', collectionPath: '' }]
    aliases.forEach((aliasInfo) => {
      const relation = relationByCollectionPath(aliasInfo.collectionPath) || actionRelations.value[0]
      const detailLabel = detailDisplayLabel(aliasInfo.collectionPath)
      const fields = relation?.fields || []
      fields.forEach((field) => {
        const fieldCode = field.sourceField || field.field || field.fieldCode
        if (!fieldCode)
          return
        add(`${aliasInfo.alias}.${fieldCode}`, fieldDisplayLabel(field, detailLabel))
      })
      add(`${aliasInfo.alias}.id`, `${detailLabel} · ID`)
    })
    return options
  }

  function fieldDisplayLabel(field = {}, scopeLabel = '') {
    const label = businessFieldLabel(field)
    return scopeLabel ? `${scopeLabel} · ${label}` : label
  }

  function collectMainFields(fields = [], modelSchema = {}) {
    if (Array.isArray(fields) && fields.length)
      return fields
    return Array.isArray(modelSchema.fields) ? modelSchema.fields : []
  }

  function detailDisplayLabel(collectionPath = '') {
    const relation = relationByCollectionPath(collectionPath) || actionRelations.value[0]
    return relation?.relationName || relation?.detailTabTitle || relation?.modelName || relation?.label || '明细字段'
  }

  function mergeSelectedFieldOptions(options = [], values = []) {
    const result = Array.isArray(options) ? [...options] : []
    const seen = new Set(result.map(item => item.value))
    normalizeStringList(values).forEach((value) => {
      if (seen.has(value))
        return
      result.push({
        label: resolvePathDisplayLabel(value, result),
        value,
      })
      seen.add(value)
    })
    return result
  }

  function resolvePathDisplayLabel(value, options = []) {
    const matched = options.find(item => item.value === value)
    if (matched?.label)
      return matched.label
    const fieldCode = String(value || '').split('.').pop()
    const field = findFieldByCode(fieldCode)
    if (field)
      return fieldDisplayLabel(field, String(value || '').startsWith('record.') ? '单据字段' : '明细字段')
    return '未识别字段（请在关系与级联中维护）'
  }

  function findFieldByCode(fieldCode) {
    if (!fieldCode)
      return null
    const allFields = [
      ...collectMainFields(props.fields, props.modelSchema).map(toPageField),
      ...actionRelations.value.flatMap(relation => relation.fields || []),
    ].map(toPageField)
    return allFields.find((field) => {
      const codes = [field.sourceField, field.field, field.fieldCode, field.columnName].filter(Boolean)
      return codes.some(code => String(code) === String(fieldCode))
    }) || null
  }

  function ensureActionConfig(action) {
    if (!action.actionConfig || typeof action.actionConfig !== 'object' || Array.isArray(action.actionConfig))
      action.actionConfig = {}
    return action.actionConfig
  }

  function ensureStepConfig(step) {
    if (!step.stepConfig || typeof step.stepConfig !== 'object' || Array.isArray(step.stepConfig))
      step.stepConfig = {}
    return step.stepConfig
  }

  function ensureParams(config) {
    if (!config.params || typeof config.params !== 'object' || Array.isArray(config.params))
      config.params = {}
    return config.params
  }

  function resolveStep(actions, step) {
    const action = actions[selectedActionIndex.value]
    if (!action?.actionConfig)
      return null
    return getPathValue(action.actionConfig, step.path)
  }

  function getPathValue(root, path = []) {
    let cursor = root
    for (const key of path) {
      if (cursor == null)
        return null
      cursor = cursor[key]
    }
    return cursor
  }

  function wrapExpression(path) {
    const text = String(path || '').trim()
    return text ? `\${${text}}` : ''
  }

  function unwrapExpression(value) {
    const text = String(value || '').trim()
    const match = text.match(/^\$\{([^}]+)\}$/)
    return match ? match[1] : text
  }

  function stringValue(value) {
    if (value == null)
      return ''
    if (typeof value === 'object')
      return JSON.stringify(value)
    return String(value)
  }

  function normalizeStringList(value) {
    const list = Array.isArray(value) ? value : value ? [value] : []
    return Array.from(new Set(list.map(item => String(item || '').trim()).filter(Boolean)))
  }

  function cloneValue(value) {
    return JSON.parse(JSON.stringify(value ?? []))
  }

  function stringifyJson(value) {
    try {
      return JSON.stringify(value || {}, null, 2)
    }
    catch {
      return '{}'
    }
  }
  __impl.parseRelationConfig = parseRelationConfig
  __impl.toPageField = toPageField
  __impl.isInactiveField = isInactiveField
  __impl.businessFieldLabel = businessFieldLabel
  __impl.fieldPathOptions = fieldPathOptions
  __impl.fieldDisplayLabel = fieldDisplayLabel
  __impl.collectMainFields = collectMainFields
  __impl.detailDisplayLabel = detailDisplayLabel
  __impl.mergeSelectedFieldOptions = mergeSelectedFieldOptions
  __impl.resolvePathDisplayLabel = resolvePathDisplayLabel
  __impl.findFieldByCode = findFieldByCode
  __impl.ensureActionConfig = ensureActionConfig
  __impl.ensureStepConfig = ensureStepConfig
  __impl.ensureParams = ensureParams
  __impl.resolveStep = resolveStep
  __impl.getPathValue = getPathValue
  __impl.wrapExpression = wrapExpression
  __impl.unwrapExpression = unwrapExpression
  __impl.stringValue = stringValue
  __impl.normalizeStringList = normalizeStringList
  __impl.cloneValue = cloneValue
  __impl.stringifyJson = stringifyJson

  return {
    ...deps, businessFieldLabel, cloneValue, collectMainFields, detailDisplayLabel, ensureActionConfig, ensureParams,
    ensureStepConfig, fieldDisplayLabel, fieldPathOptions, findFieldByCode, getPathValue, isInactiveField,
    mergeSelectedFieldOptions, normalizeStringList, parseRelationConfig, resolvePathDisplayLabel, resolveStep,
    stringValue, stringifyJson, toPageField, unwrapExpression, wrapExpression,
  }
}
