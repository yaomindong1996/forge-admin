/** BusinessFormDesigner.vue setup part 2. */
import { ChevronDownOutline, ChevronUpOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, nextTick, provide, ref, watch } from 'vue'
import { saveBusinessObjectDesigner, saveBusinessObjectFormLayout } from '@/api/business-app'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import {
  buildPageDesignModelSchema,
  createDefaultPageSchema,
  createPageModelRef,
  isReadonlySystemField,
  syncPageSchemaWithModel,
} from '@/components/lowcode-builder/page/page-schema'
import { isPageWidgetComponentKey } from '@/components/lowcode-builder/shared/page-widget-schema'
import { hasRuntimeVisibilityRules } from '@/components/lowcode-builder/shared/runtime-rules'
import BusinessDetailDesigner from '../BusinessDetailDesigner.vue'
import BusinessFormCreateDesigner from '../BusinessFormCreateDesigner.vue'
import ButtonActionConfig from '../ButtonActionConfig.vue'
import {
  removeChildTableSectionConfig,
  reorderChildTableSectionConfig,
  resolveChildTableSectionEditConfig,
  safeKey,
  upsertChildTableSectionConfig,
} from '../child-table-section-config'
import ChildTableSectionWizard from '../ChildTableSectionWizard.vue'
import ForgeFormDesigner from '../forge-form-designer/ForgeFormDesigner.vue'
import { alignFormDesignerBindingsToFields, buildAutoFieldAssets } from '../form-first/autoFieldRegistry'
import { FIELD_COMPONENT_DEFAULTS as COMPONENT_FIELD_DEFAULTS, FORM_FIELD_COMPONENT_KEYS } from '../form-first/fieldComponentCatalog'
import { extractForgeSchemaFieldRefs, forgeSchemaToFormCreate } from '../form-first/forgeToFormCreate'
import { applyGridColumnsToFormDesignerSchema, generateFieldCode, normalizeFormDesignerSchema, normalizeFormDesignerSchemaForSave } from '../form-first/formDesignerSchema'
import RelationOverviewModal from '../RelationOverviewModal.vue'
export function applyBusinessFormDesignerPart2(props, emit, expose, deps = {}) {
  const {
    __impl, mut, appendAllUnusedFields, appendField, applyRuntimeFieldMeta, assignLocalSchema, buildFormDesignerEditZone, buildFormRuntimeFieldSettings,
    buildPrimaryModelFieldSet, buildRelationAliases, buildRuntimeFormFieldSetting, buildRuntimeFormLayout, buildRuntimeFormLayoutNode, buildRuntimeFormLayoutNodes, buildRuntimeLayoutMeta, buildVisibleFormFieldSet,
    canMoveRelationField, clampNumber, clearRelationGroupFields, collectRuntimeFieldComponents, collectSubTableRelationKeys, copyDefined, copyPropsToRuntimeField, handleButtonActionConfirm,
    handleChildTableSectionConfirm, handleConfigureBottomAction, handleEditChildTableSection, handleEditSubTableContainer, handleFormDesignerMoreSelect, handleSubTableBridgeSync, isColumnLayoutComponent, isPlainObject,
    isRelationField, isRelationFieldAllowed, isRowLayoutComponent, locateSubTableOnCanvas, moveRelationField, normalizeAlign, normalizeEditZoneFieldRefs, normalizeFormOpenMode,
    normalizeFormOpenModePatch, normalizeRuntimeComponentType, normalizeRuntimeFormSize, openChildTableSectionWizard, orderRelationRefs, replaceModelFieldSettings, replaceZone, resolveNumber,
    resolvePrimaryFieldRefs, resolveRuntimeLayoutLabel, resolveSelectedRelationFieldRefs, sanitizeRuntimeFieldProps, sanitizeRuntimeLayoutProps, selectRelationGroupFields, selectedRelationRefsByModel, sortRelationGroupFields,
    syncFormDesignerSchemaToPageSchema, toggleRelationField, updateEditZoneProps, updateFormDesignerLayout, upsertSubTableContainer, DICT_FIELD_TYPES, DICT_COMPONENT_TYPES, message,
    saving, shelfTab, activeObjectKey, formCreateDesignerRef, forgeFormDesignerRef, childTableWizardVisible, childTableWizardValue, relationOverviewVisible,
    buttonActionConfigVisible, buttonActionIndex, buttonActionValue, useLegacyFormCreateDesigner, activeFormDesignerRef, baseModelSchema, localSchema, localFormDesignerSchema,
    localViewSchema, effectiveModelSchema, designFields, editZone, pageModelRefs, buttonTargetPages, primaryModelCode, primaryDesignFields,
    relationFields, primaryFieldSet, relationFieldSet, usedFieldSet, selectedRelationFieldRefs, selectedRelationFieldSet, businessFields, primaryBusinessFields,
    primaryBusinessFieldAssets, systemFields, usedFields, unusedFields, formOpenModeOptions, editFormOpenMode, formGridColumns, formDesignerMoreOptions,
    relationFieldGroups, relationFormRows, formObjectTabs, isPrimaryObjectActive, activeRelationRow, activeRelationGroup, activeRelationCanvasFields, activeRelationAvailableFields,
    activeObjectTitle, activeObjectDescription, visibleShelfFields, subTableSummaryRows, subTableContainerRelationKeys, subTableContainerOrder,
  } = deps
  function matchesSourceField(field = {}, sourceField) {
    if (!sourceField)
      return false
    const source = field.sourceField || field.field
    return source === sourceField
      || field.field === sourceField
      || field.columnName === sourceField
      || field.columnName === camelToSnake(sourceField)
  }

  function camelToSnake(value) {
    return String(value || '').replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase()
  }

  function booleanFlag(value) {
    return value === true || value === 'true'
  }

  function firstNonBlank(...values) {
    for (const value of values) {
      const text = String(value ?? '').trim()
      if (text)
        return text
    }
    return ''
  }

  function normalizeRelationFormRows(relations = [], groups = []) {
    return (relations || [])
      .map((relation, index) => {
        const config = parseRelationConfig(relation.relationConfig)
        const targetCode = relation.targetObjectCode || ''
        const group = findRelationGroup(targetCode, groups)
        const relationKey = normalizeRelationKey(targetCode) || 'relation'
        const key = `${relationKey}_${relation.id || relation.clientKey || index}`
        return {
          key,
          modelCode: targetCode,
          title: config.detailTabTitle || config.detailTab || relation.detailTabTitle || relation.relationName || relation.targetObjectName || targetCode || `关联表单${index + 1}`,
          fieldCount: group?.fields?.length || 0,
          selectedCount: group?.selectedCount || 0,
          group,
          showInDetail: config.showInDetail !== false && relation.showInDetail !== false,
          inlineCreateEnabled: resolveRelationToggle(relation, config, 'inlineCreateEnabled', true),
          inlineEditEnabled: resolveRelationToggle(relation, config, 'inlineEditEnabled', false),
          status: relation.status ?? 1,
        }
      })
      .filter(relation => relation.status !== 0 && (relation.showInDetail || relation.inlineCreateEnabled || relation.inlineEditEnabled))
  }

  function findRelationGroup(targetCode, groups = []) {
    const targetKey = normalizeRelationKey(targetCode)
    if (!targetKey)
      return null
    return groups.find((group) => {
      const aliases = Array.isArray(group.aliases) ? group.aliases : [normalizeRelationKey(group.key), normalizeRelationKey(group.modelCode)]
      return aliases.some(alias => relationKeyMatches(alias, targetKey))
    }) || null
  }

  function relationKeyMatches(left, right) {
    if (!left || !right)
      return false
    return left === right || left.endsWith(`_${right}`) || right.endsWith(`_${left}`)
  }

  function normalizeRelationKey(value) {
    return String(value || '')
      .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
      .replace(/\W+/g, '_')
      .replace(/_+/g, '_')
      .replace(/^_|_$/g, '')
      .toLowerCase()
  }

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

  function canInlineRelation(relation = {}) {
    return ['CHILD_LIST', 'DETAIL'].includes(String(relation.relationType || '').toUpperCase())
  }

  function resolveRelationToggle(relation = {}, config = {}, key, defaultValue) {
    if (!canInlineRelation(relation))
      return false
    if (config[key] === true || config[key] === 'true')
      return true
    if (config[key] === false || config[key] === 'false')
      return false
    if (relation[key] === true)
      return true
    if (relation[key] === false)
      return false
    return defaultValue
  }

  async function saveLayout() {
    if (!props.objectId)
      return
    const { formSchema, createdFields, normalizedFields, nextModelSchema, schema } = buildCurrentDesignerDraft()
    saving.value = true
    try {
      if (createdFields.length || formSchema) {
        await saveBusinessObjectDesigner(props.objectId, {
          fields: normalizedFields.map(toBusinessFieldPayload),
          modelSchema: cloneSchema(nextModelSchema || {}),
          pageSchema: cloneSchema(schema || {}),
          formDesignerSchema: cloneSchema(formSchema || localFormDesignerSchema.value || {}),
          viewSchema: cloneSchema(localViewSchema.value || {}),
        })
      }
      await saveBusinessObjectFormLayout(props.objectId, {
        layoutKey: 'form',
        layoutName: '表单布局',
        layoutType: schema.layoutType,
        pageSchema: cloneSchema(schema),
        zones: schema.zones?.filter(zone => zone.zoneKey === 'edit') || [],
        settings: {},
      })
      assignLocalSchema(schema, { markDirty: false })
      emit('saved', cloneSchema(schema))
      if (formSchema)
        emit('fieldsUpdated', normalizedFields, { persisted: true })
      emit('dirtyChange', false)
      message.success(createdFields.length ? `表单布局已保存，已自动创建 ${createdFields.length} 个字段` : '表单布局已保存')
    }
    finally {
      saving.value = false
    }
  }

  function syncDesignerDraft() {
    const { formSchema, createdFields, normalizedFields, nextModelSchema, schema } = buildCurrentDesignerDraft()
    const baseline = buildDesignerDraftFromFormSchema(localFormDesignerSchema.value)
    const formChanged = formSchema && !isSameSchema(formSchema, baseline.formSchema)
    const fieldsChanged = !isSameSchema(normalizedFields, baseline.normalizedFields)
    const pageChanged = !isSameSchema(schema, baseline.schema)
    const fieldsCreated = createdFields.length > 0
    if (formChanged)
      localFormDesignerSchema.value = cloneSchema(formSchema)
    if (pageChanged)
      assignLocalSchema(schema, { markDirty: false })
    if (fieldsChanged || fieldsCreated)
      emit('fieldsUpdated', normalizedFields, { persisted: false })
    if (formChanged || fieldsChanged || pageChanged || fieldsCreated)
      emit('dirtyChange', true)
    return {
      pageSchema: cloneSchema(schema),
      formDesignerSchema: cloneSchema(formSchema || localFormDesignerSchema.value || {}),
      viewSchema: cloneSchema(localViewSchema.value || {}),
      modelSchema: cloneSchema(nextModelSchema || {}),
      fields: cloneSchema(normalizedFields),
      createdFields: cloneSchema(createdFields),
      dirty: formChanged || fieldsChanged || pageChanged || fieldsCreated,
    }
  }

  function handleFieldAssetUpdated(payload = {}) {
    const fieldCode = payload.fieldCode || payload.field
    if (!fieldCode)
      return
    const normalizedPayload = normalizeBusinessFieldAsset(payload)
    let matched = false
    const nextFields = primaryBusinessFieldAssets.value.map((field) => {
      const code = field.fieldCode || field.field
      if (code !== fieldCode)
        return field
      matched = true
      return mergeBusinessFieldAsset(field, normalizedPayload)
    })
    if (!matched)
      nextFields.push(normalizedPayload)
    emit('fieldsUpdated', nextFields, { persisted: false })
    emit('dirtyChange', true)
  }

  function buildCurrentDesignerDraft() {
    const formSchema = activeFormDesignerRef.value?.flushDesigner?.() || localFormDesignerSchema.value
    return buildDesignerDraftFromFormSchema(formSchema)
  }

  function buildDesignerDraftFromFormSchema(formSchema) {
    const normalizedFormSchema = normalizeGeneratedTemplateFieldCodes(
      normalizeFormDesignerSchemaForSave(formSchema || {}),
      primaryBusinessFieldAssets.value,
    )
    const { fields: nextFields, createdFields } = buildAutoFieldAssets(normalizedFormSchema, primaryBusinessFieldAssets.value)
    // 就地回写外层 ForSave schema（含 forms[]），避免 snake 绑定与驼峰目录再次撞码
    alignFormDesignerBindingsToFields(normalizedFormSchema, nextFields)
    const formFieldComponents = buildFormFieldComponentMap(normalizedFormSchema)
    const createdFieldCodes = new Set(createdFields.map(field => field.fieldCode || field.field).filter(Boolean))
    const normalizedFields = nextFields.map(field => normalizeUnconfiguredDesignerField(
      field,
      formFieldComponents,
      createdFieldCodes.has(field.fieldCode || field.field),
    ))
    const primaryModelFields = [
      ...systemFields.value,
      ...normalizedFields.map(toPageField),
    ]
    const nextModelSchema = {
      ...baseModelSchema.value,
      fields: primaryModelFields,
    }
    const nextDesignModelSchema = {
      ...effectiveModelSchema.value,
      fields: [
        ...primaryModelFields,
        ...relationFields.value,
      ],
    }
    let sourceSchema = localSchema.value
    if (formSchema) {
      const sourceEditZone = sourceSchema?.zones?.find(zone => zone.zoneKey === 'edit') || null
      const editZone = buildFormDesignerEditZone(sourceEditZone, normalizedFormSchema, primaryModelFields)
      if (editZone) {
        sourceSchema = {
          ...sourceSchema,
          zones: (sourceSchema.zones || []).map(zone => zone.zoneKey === editZone.zoneKey ? editZone : zone),
        }
      }
    }
    const schema = syncPageSchemaWithModel(sourceSchema, nextDesignModelSchema)
    return {
      formSchema: normalizedFormSchema,
      createdFields,
      normalizedFields,
      nextModelSchema,
      schema,
    }
  }

  function normalizeGeneratedTemplateFieldCodes(schema = {}, existingFields = []) {
    const next = cloneSchema(schema || {})
    const existingCodes = new Set((existingFields || [])
      .map(field => field?.fieldCode || field?.field)
      .filter(Boolean))
    const reservedCodes = new Set(existingCodes)
    const componentGroups = collectFormDesignerComponentGroups(next)
    componentGroups.forEach((components) => {
      walkFormDesignerComponents(components, (component) => {
        const fieldCode = component?.fieldBinding?.fieldCode || ''
        if (!fieldCode)
          return
        if (!shouldRewriteGeneratedTemplateField(component, existingCodes))
          reservedCodes.add(fieldCode)
      })
    })
    componentGroups.forEach((components) => {
      walkFormDesignerComponents(components, (component) => {
        if (!shouldRewriteGeneratedTemplateField(component, existingCodes))
          return
        rewriteGeneratedTemplateField(component, reserveGeneratedTemplateFieldCode(component, reservedCodes))
      })
    })
    syncActiveFormSchemaEntry(next)
    return next
  }

  function collectFormDesignerComponentGroups(schema = {}) {
    const groups = [schema.components || []]
    const assets = Array.isArray(schema?.settings?.formAssets) ? schema.settings.formAssets : []
    assets.forEach((asset) => {
      const assetSchema = asset?.schema || asset
      groups.push(assetSchema?.components || [])
    })
    ;(Array.isArray(schema?.forms) ? schema.forms : []).forEach((form) => {
      if (form?.formKey && form.formKey === schema.formKey)
        return
      groups.push(form?.schema?.components || form?.components || [])
    })
    return groups
  }

  function syncActiveFormSchemaEntry(schema = {}) {
    if (!Array.isArray(schema.forms) || !schema.formKey)
      return
    const activeForm = schema.forms.find(form => form?.formKey === schema.formKey)
    if (!activeForm)
      return
    activeForm.formName = schema.formName || activeForm.formName
    activeForm.schema = {
      ...(activeForm.schema || {}),
      schemaVersion: schema.schemaVersion,
      formKey: schema.formKey,
      formName: schema.formName,
      layout: cloneSchema(schema.layout || {}),
      components: cloneSchema(schema.components || []),
      settings: {
        ...(activeForm.schema?.settings || {}),
        formAssets: [],
      },
    }
  }

  function shouldRewriteGeneratedTemplateField(component = {}, existingCodes = new Set()) {
    const binding = component?.fieldBinding || {}
    const fieldCode = binding.fieldCode || ''
    if (!fieldCode || existingCodes.has(fieldCode))
      return false
    if (binding.source && binding.source !== 'designer')
      return false
    if (binding.createIfMissing === false)
      return false
    return isGeneratedTemplateFieldCode(fieldCode)
  }

  function rewriteGeneratedTemplateField(component = {}, nextFieldCode = '') {
    const oldFieldCode = component?.fieldBinding?.fieldCode || ''
    if (!nextFieldCode || !oldFieldCode || oldFieldCode === nextFieldCode)
      return
    component.id = component.id === `cmp_${oldFieldCode}` ? `cmp_${nextFieldCode}` : component.id
    component.fieldBinding = {
      ...(component.fieldBinding || {}),
      fieldCode: nextFieldCode,
      columnName: camelToSnake(nextFieldCode),
    }
    if (component.props?.fieldCode === oldFieldCode)
      component.props.fieldCode = nextFieldCode
    if (component.props?.fieldBinding?.fieldCode === oldFieldCode) {
      component.props.fieldBinding = {
        ...(component.props.fieldBinding || {}),
        fieldCode: nextFieldCode,
        columnName: camelToSnake(nextFieldCode),
      }
    }
  }

  function reserveGeneratedTemplateFieldCode(component = {}, reservedCodes = new Set()) {
    const base = buildGeneratedTemplateFieldBase(component)
    if (!reservedCodes.has(base)) {
      reservedCodes.add(base)
      return base
    }
    for (let index = 2; index < 1000; index += 1) {
      const candidate = `${base}${index}`
      if (!reservedCodes.has(candidate)) {
        reservedCodes.add(candidate)
        return candidate
      }
    }
    const fallback = `${base}${Date.now().toString(36)}`
    reservedCodes.add(fallback)
    return fallback
  }

  function buildGeneratedTemplateFieldBase(component = {}) {
    const generated = generateFieldCode(component.label || component.props?.label || component.componentKey || '字段')
    if (generated && !isGenericGeneratedFieldCode(generated))
      return generated
    const key = String(component.componentKey || 'input')
      .replace(/[^a-z0-9]/gi, '')
      .replace(/^\d+/, '')
    const suffix = key ? `${key[0].toUpperCase()}${key.slice(1)}` : 'Input'
    return `field${suffix}`
  }

  function isGeneratedTemplateFieldCode(value = '') {
    return /^field_[a-z0-9]+(?:_[a-z0-9]+)?$/i.test(String(value || '').trim())
  }

  function isGenericGeneratedFieldCode(value = '') {
    const text = String(value || '').trim()
    const normalized = text.toLowerCase()
    if (!text)
      return true
    if (/^field[0-9a-z]{4,}$/i.test(text))
      return true
    return [
      'input',
      'textarea',
      'number',
      'integer',
      'money',
      'date',
      'datetime',
      'time',
      'switch',
      'select',
      'selector',
      'radio',
      'checkbox',
      'dictselect',
      'cascader',
      'field',
    ].includes(normalized)
  }

  function walkFormDesignerComponents(components = [], visitor) {
    ;(Array.isArray(components) ? components : []).forEach((component) => {
      if (!component || typeof component !== 'object')
        return
      visitor(component)
      if (Array.isArray(component.children))
        walkFormDesignerComponents(component.children, visitor)
    })
  }

  function normalizeBusinessFieldAsset(field = {}) {
    const fieldCode = field.fieldCode || field.field || ''
    const fieldName = field.fieldName || field.label || field.comment || fieldCode
    return {
      ...field,
      field: field.field || fieldCode,
      label: field.label || fieldName,
      fieldCode,
      fieldName,
    }
  }

  function mergeBusinessFieldAsset(field = {}, payload = {}) {
    return normalizeBusinessFieldAsset({
      ...field,
      ...payload,
      field: payload.field || payload.fieldCode || field.field || field.fieldCode,
      label: payload.fieldName || payload.label || field.label || field.fieldName,
      basicProps: {
        ...(field.basicProps || {}),
        ...(payload.basicProps || {}),
      },
      advancedProps: {
        ...(field.advancedProps || {}),
        ...(payload.advancedProps || {}),
      },
      formulaConfig: Object.prototype.hasOwnProperty.call(payload, 'formulaConfig')
        ? payload.formulaConfig
        : field.formulaConfig,
    })
  }

  function buildFormFieldComponentMap(schema = {}) {
    const map = new Map()
    collectFormFieldComponents(normalizeFormDesignerSchema(schema).components, map)
    return map
  }

  function collectFormFieldComponents(components = [], map = new Map()) {
    ;(Array.isArray(components) ? components : []).forEach((component) => {
      if (!component || typeof component !== 'object')
        return
      const fieldCode = component.fieldBinding?.mode === 'field' ? component.fieldBinding?.fieldCode : ''
      if (fieldCode)
        map.set(fieldCode, component)
      if (Array.isArray(component.children))
        collectFormFieldComponents(component.children, map)
    })
    return map
  }

  function normalizeUnconfiguredDesignerField(field = {}, formFieldComponents = new Map(), createdFromDesigner = false) {
    const fieldCode = field.fieldCode || field.field
    if (!fieldCode)
      return field

    if (!createdFromDesigner)
      return normalizeBusinessFieldAsset(field)

    const formComponent = formFieldComponents.get(fieldCode)
    const componentType = normalizeRuntimeComponentType(formComponent?.componentKey || field.componentType)
    const mergedField = mergeFieldWithFormComponent(field, formComponent, componentType)
    const syncedField = syncDesignerFieldWithFormComponent(mergedField, formComponent)
    if (isDictField(mergedField) && formComponent && !requiresDictConfig(formComponent))
      return downgradeDesignerFieldToText(syncedField)
    if (isReferenceField(mergedField) && formComponent && !requiresReferenceConfig(formComponent))
      return downgradeDesignerFieldToText(syncedField)
    if (!formComponent && (isUnconfiguredDictField(mergedField) || isUnconfiguredReferenceField(mergedField)))
      return downgradeDesignerFieldToText(syncedField)
    return syncedField
  }

  function mergeFieldWithFormComponent(field = {}, formComponent = null, componentType = '') {
    if (!formComponent)
      return field
    const props = formComponent.props || {}
    const defaults = COMPONENT_FIELD_DEFAULTS[componentType] || {}
    const mergedDictType = firstNonBlank(
      props.dictType,
      field.dictType,
      field.basicProps?.dictType,
      field.advancedProps?.dictType,
      formComponent.advancedProps?.dictType,
    )
    return {
      ...applyComponentFieldDefaults(field, defaults),
      componentType: componentType || field.componentType,
      dictType: mergedDictType || field.dictType,
      referenceObjectCode: props.referenceObjectCode ?? field.referenceObjectCode,
      referenceDisplayField: props.referenceDisplayField ?? field.referenceDisplayField,
      formulaConfig: props.formulaConfig ?? field.formulaConfig ?? formComponent.advancedProps?.formulaConfig ?? null,
      basicProps: {
        ...(field.basicProps || {}),
        ...(props || {}),
        ...(mergedDictType ? { dictType: mergedDictType } : {}),
      },
      advancedProps: {
        ...(field.advancedProps || {}),
        ...(formComponent.advancedProps || {}),
        ...(mergedDictType ? { dictType: mergedDictType } : {}),
      },
    }
  }

  function syncDesignerFieldWithFormComponent(field = {}, formComponent = null) {
    if (!formComponent) {
      return {
        ...field,
        formVisible: false,
      }
    }

    const props = formComponent.props || {}
    const label = formComponent.label || field.fieldName || field.label || field.fieldCode || field.field || '字段'
    const maxLength = resolveNumber(props.maxlength, null)
    return {
      ...field,
      fieldName: label,
      label,
      length: maxLength || field.length,
      required: Boolean(formComponent.validation?.required),
      validation: {
        ...(field.validation || {}),
        ...(formComponent.validation || {}),
      },
      readonly: Boolean(formComponent.visibility?.readonly),
      defaultValue: Object.prototype.hasOwnProperty.call(props, 'defaultValue') ? props.defaultValue : field.defaultValue,
      formVisible: formComponent.visibility?.hidden !== true,
      placeholder: props.placeholder ?? field.placeholder ?? field.basicProps?.placeholder ?? '',
      sortOrder: resolveDesignerFieldSortOrder(formComponent, field.sortOrder),
    }
  }

  function resolveDesignerFieldSortOrder(component = {}, fallback = 0) {
    const candidates = [
      component?.props?.sortOrder,
      component?.layout?.order,
      fallback,
    ]
    for (const candidate of candidates) {
      const value = Number(candidate)
      if (Number.isFinite(value) && value > 0)
        return value
    }
    return fallback
  }

  function applyComponentFieldDefaults(field = {}, defaults = {}) {
    if (!Object.keys(defaults).length)
      return { ...field }
    return {
      ...field,
      fieldType: field.fieldType || defaults.fieldType,
      businessFieldType: field.businessFieldType || field.fieldType || defaults.businessFieldType || defaults.fieldType,
      dataType: field.dataType || defaults.dataType,
      length: field.length ?? defaults.length,
      precision: field.precision ?? defaults.precision,
      queryType: field.queryType || defaults.queryType,
    }
  }

  function downgradeDesignerFieldToText(field = {}) {
    return {
      ...field,
      fieldType: 'TEXT',
      businessFieldType: 'TEXT',
      componentType: 'input',
      queryType: 'like',
      dictType: '',
      referenceObjectCode: '',
      referenceDisplayField: '',
      basicProps: {
        ...(field.basicProps || {}),
        dictType: '',
        referenceObjectCode: '',
        referenceDisplayField: '',
      },
      advancedProps: {
        ...(field.advancedProps || {}),
        dictType: '',
      },
    }
  }

  function requiresDictConfig(component = null) {
    if (!component)
      return false
    return DICT_COMPONENT_TYPES.has(normalizeRuntimeComponentType(component.componentKey))
  }

  function requiresReferenceConfig(component = null) {
    if (!component)
      return false
    return normalizeRuntimeComponentType(component.componentKey) === 'objectReference'
  }

  function isUnconfiguredDictField(field = {}) {
    const dictType = field.dictType || field.basicProps?.dictType || field.advancedProps?.dictType
    return isDictField(field) && !String(dictType || '').trim()
  }

  function isUnconfiguredReferenceField(field = {}) {
    const referenceObjectCode = field.referenceObjectCode || field.basicProps?.referenceObjectCode
    const referenceDisplayField = field.referenceDisplayField || field.basicProps?.referenceDisplayField
    return isReferenceField(field)
      && (!String(referenceObjectCode || '').trim() || !String(referenceDisplayField || '').trim())
  }

  function isDictField(field = {}) {
    const fieldType = String(field.fieldType || field.businessFieldType || '').toUpperCase()
    const componentType = normalizeRuntimeComponentType(field.componentType)
    return DICT_FIELD_TYPES.has(fieldType) || DICT_COMPONENT_TYPES.has(componentType)
  }

  function isReferenceField(field = {}) {
    const fieldType = String(field.fieldType || field.businessFieldType || '').toUpperCase()
    const componentType = normalizeRuntimeComponentType(field.componentType)
    return fieldType === 'REFERENCE' || componentType === 'objectReference'
  }

  function resolveSchema(pageSchema, modelSchema) {
    return syncPageSchemaWithModel(
      cloneSchema(pageSchema || createDefaultPageSchema(modelSchema)),
      modelSchema,
    )
  }

  function resolveDesignModelSchema(pageSchema, modelSchema) {
    const refs = mergePrimaryModelRef(pageSchema?.modelRefs || [], modelSchema || {})
    return buildPageDesignModelSchema(modelSchema || {}, refs)
  }

  function mergePrimaryModelRef(modelRefs, modelSchema) {
    if (!Array.isArray(modelRefs) || !modelRefs.length)
      return []
    const primaryRef = createPageModelRef({ modelSchema }, { primary: true })
    const refs = modelRefs.map(ref => ref?.primary
      ? {
          ...ref,
          modelCode: primaryRef.modelCode || ref.modelCode,
          modelName: primaryRef.modelName || ref.modelName,
          tableName: primaryRef.tableName || ref.tableName,
          relations: primaryRef.relations?.length ? primaryRef.relations : ref.relations,
          fields: primaryRef.fields,
        }
      : ref)
    if (!refs.some(ref => ref?.primary))
      refs.unshift(primaryRef)
    return refs
  }

  function resolveCanvasFieldRefs(items) {
    const refs = new Set()
    items.forEach((item) => {
      if (item.fieldRef)
        refs.add(item.fieldRef)
      ;(item.fieldRefs || item.props?.fieldRefs || []).forEach(ref => refs.add(ref))
    })
    return Array.from(refs)
  }

  function mergeUniqueRefs(...groups) {
    return Array.from(new Set(groups.flat().filter(Boolean)))
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
      fieldStatus: field.fieldStatus,
      formulaConfig: field.formulaConfig ?? null,
      basicProps: { ...(field.basicProps || {}) },
      advancedProps: { ...(field.advancedProps || {}) },
    }
  }

  function toBusinessFieldPayload(field = {}) {
    return {
      fieldName: field.fieldName || field.label || field.field,
      fieldCode: field.fieldCode || field.field,
      columnName: field.columnName,
      fieldType: field.fieldType || field.businessFieldType || 'TEXT',
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
      systemField: false,
      readonly: field.readonly,
      fieldStatus: field.fieldStatus || 'ENABLED',
      referenceObjectCode: field.referenceObjectCode,
      referenceDisplayField: field.referenceDisplayField,
      placeholder: field.placeholder || field.basicProps?.placeholder || '',
      remark: field.remark,
      sortOrder: field.sortOrder,
      formulaConfig: field.formulaConfig ?? null,
      fieldBinding: { ...(field.fieldBinding || field.basicProps?.fieldBinding || {}) },
      basicProps: { ...(field.basicProps || {}) },
      advancedProps: { ...(field.advancedProps || {}) },
    }
  }

  expose?.({
    saveLayout,
    syncDesignerDraft,
    appendFieldToForm: appendField,
  })
  __impl.matchesSourceField = matchesSourceField
  __impl.camelToSnake = camelToSnake
  __impl.booleanFlag = booleanFlag
  __impl.firstNonBlank = firstNonBlank
  __impl.normalizeRelationFormRows = normalizeRelationFormRows
  __impl.findRelationGroup = findRelationGroup
  __impl.relationKeyMatches = relationKeyMatches
  __impl.normalizeRelationKey = normalizeRelationKey
  __impl.parseRelationConfig = parseRelationConfig
  __impl.canInlineRelation = canInlineRelation
  __impl.resolveRelationToggle = resolveRelationToggle
  __impl.saveLayout = saveLayout
  __impl.syncDesignerDraft = syncDesignerDraft
  __impl.handleFieldAssetUpdated = handleFieldAssetUpdated
  __impl.buildCurrentDesignerDraft = buildCurrentDesignerDraft
  __impl.buildDesignerDraftFromFormSchema = buildDesignerDraftFromFormSchema
  __impl.normalizeGeneratedTemplateFieldCodes = normalizeGeneratedTemplateFieldCodes
  __impl.collectFormDesignerComponentGroups = collectFormDesignerComponentGroups
  __impl.syncActiveFormSchemaEntry = syncActiveFormSchemaEntry
  __impl.shouldRewriteGeneratedTemplateField = shouldRewriteGeneratedTemplateField
  __impl.rewriteGeneratedTemplateField = rewriteGeneratedTemplateField
  __impl.reserveGeneratedTemplateFieldCode = reserveGeneratedTemplateFieldCode
  __impl.buildGeneratedTemplateFieldBase = buildGeneratedTemplateFieldBase
  __impl.isGeneratedTemplateFieldCode = isGeneratedTemplateFieldCode
  __impl.isGenericGeneratedFieldCode = isGenericGeneratedFieldCode
  __impl.walkFormDesignerComponents = walkFormDesignerComponents
  __impl.normalizeBusinessFieldAsset = normalizeBusinessFieldAsset
  __impl.mergeBusinessFieldAsset = mergeBusinessFieldAsset
  __impl.buildFormFieldComponentMap = buildFormFieldComponentMap
  __impl.collectFormFieldComponents = collectFormFieldComponents
  __impl.normalizeUnconfiguredDesignerField = normalizeUnconfiguredDesignerField
  __impl.mergeFieldWithFormComponent = mergeFieldWithFormComponent
  __impl.syncDesignerFieldWithFormComponent = syncDesignerFieldWithFormComponent
  __impl.resolveDesignerFieldSortOrder = resolveDesignerFieldSortOrder
  __impl.applyComponentFieldDefaults = applyComponentFieldDefaults
  __impl.downgradeDesignerFieldToText = downgradeDesignerFieldToText
  __impl.requiresDictConfig = requiresDictConfig
  __impl.requiresReferenceConfig = requiresReferenceConfig
  __impl.isUnconfiguredDictField = isUnconfiguredDictField
  __impl.isUnconfiguredReferenceField = isUnconfiguredReferenceField
  __impl.isDictField = isDictField
  __impl.isReferenceField = isReferenceField
  __impl.resolveSchema = resolveSchema
  __impl.resolveDesignModelSchema = resolveDesignModelSchema
  __impl.mergePrimaryModelRef = mergePrimaryModelRef
  __impl.resolveCanvasFieldRefs = resolveCanvasFieldRefs
  __impl.mergeUniqueRefs = mergeUniqueRefs
  __impl.toPageField = toPageField
  __impl.toBusinessFieldPayload = toBusinessFieldPayload

  function hydrateInitialState() {
    const next = resolveSchema(props.modelValue, resolveDesignModelSchema(props.modelValue, baseModelSchema.value))
    assignLocalSchema(next, { markDirty: false })
  }
  __impl.hydrateInitialState = hydrateInitialState

  return {
    ...deps,
  }
}
