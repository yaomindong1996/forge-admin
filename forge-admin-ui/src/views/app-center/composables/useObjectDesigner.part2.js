/** object-designer.[objectCode].vue setup part 2. */
import { useMessage } from 'naive-ui'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import {
  businessFlowAppConfig,
  businessObjectDesigner,
  businessObjectList,
  businessObjectPublishCheck,
  businessObjectRuntimeInfo,
  publishBusinessObject,
  saveBusinessFlowAppConfig,
  saveBusinessObjectDesigner,
} from '@/api/business-app'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import { createDefaultPageSchema } from '@/components/lowcode-builder/page/page-schema'
import { useTabStore, useUserStore } from '@/store'
import { getDefaultPageTitle } from '@/utils/page-title'
import { renameFormDesignerFieldRefs } from '../components/designer/form-first/fieldReferenceUtils'
import { createDefaultFormDesignerSchema, normalizeMultiFormDesignerSchema } from '../components/designer/form-first/formDesignerSchema'
import { createDefaultViewSchema, renameViewSchemaFieldRefs, sanitizeViewSchemaFieldRefs } from '../components/designer/form-first/viewSchema'
import {
  pickBusinessObjectIdentity,
  resolveDataModelTab,
  resolveStandaloneObjectDesignerSection,
} from '../components/designer/object-designer-navigation'
import { buildSeedTakeoverSummary, markSeedTakeoverAccepted, requiresSeedTakeoverConfirmation } from '../components/designer/seed-takeover'
export function applyObjectDesignerPart2(props, emit, expose, deps = {}) {
  const {
    __impl, mut, applyCodeAppDesignerDraft, applyDesignerDraftSyncResult, applyFieldRename, buildCodeAppMetadataPayload, buildDesignerPayload, confirmSeedConfigurationTakeover,
    createEmptyDraft, findBusinessObjectByCode, formatPublishBlockMessage, handleActionsUpdated, handleAddFieldToForm, handleBack, handleDirtyChange,
    handleFieldDirtyChange, handleFieldGenerationUpdate, handleFieldsUpdated, handleFixTarget, handleFlowAppSaved, handleLayoutSaved, handleListModelSchemaUpdate, handleModelSchemaApplied,
    handleModelSchemaUpdated, handlePanelSwitch, handlePreview, handlePublish, handlePublishCheckUpdated, handleRelationsUpdated, handleSave, hasCodeAppFlag,
    hasOwn, hasTableSyncIssue, isCodeAppBusinessObject, isCodeAppRoute, loadDesigner, loadRuntimeInfo, markDirty, normalizePanel,
    openDeveloperPath, openProcessWorkspace, openRuntime, persistPendingDesignerDraft, refreshPublishCheckState, requestSeedTakeoverConfirmation, resolveApplicationCode, resolveBusinessObject,
    resolveInitialFormCanvasView, resolveInitialPanel, saveCodeAppDesignerDraft, saveDesignerDraft, saveFieldDraftBeforePanelSwitch, shouldOpenCodeAppDesigner, syncActiveFormDraft, syncActiveListDraft,
    updateStatus, waitForSaveLoadingPaint, route, router,
    message, tabStore, userStore, loading, saving, publishing, dirty, designerDraftDirty,
    fieldDraftDirty, ready, activePanel, initialFormCanvasView, developerMode, designer, runtimeInfo, publishCheckState,
    functionMarketVisible, fieldManagerRef, tableMappingSummaryRef, tableMapping, formDesignerRef, listDesignerRef, relationDesignerRef, flowAppConfigRef,
    treeModelRef, publishChecklistRef, draft, objectCode, suiteCode, objectId, applicationCode, pageTitle,
    canAdvanced, isCodeAppDesigner, usesLegacyObjectPanels, effectiveDesignerPanel, publishDisabled, workspaceObjects, designerNavPanels, fieldOptions,
    runtimeFormOptions,
  } = deps
  function createDraftFromDesigner(value = {}) {
    return {
      ...createEmptyDraft(),
      ...cloneSchema(value || {}),
      status: value?.status ?? 1,
      modelSchema: cloneSchema(value?.modelSchema || {}),
      pageSchema: cloneSchema(value?.pageSchema || null),
      formDesignerSchema: cloneSchema(value?.formDesignerSchema || null),
      viewSchema: cloneSchema(value?.viewSchema || null),
      linkageSchema: cloneSchema(value?.linkageSchema || null),
      fields: cloneSchema(value?.fields || []),
      relations: cloneSchema(value?.relations || []),
      documentConfig: cloneSchema(value?.documentConfig || null),
      designerOptions: cloneSchema(value?.designerOptions || {}),
    }
  }

  function resolveCodeAppMetadata(config = {}) {
    return cloneSchema(config.options?.codeAppMetadata || config.formAssets?.codeAppMetadata || {})
  }

  function createCodeAppModelSchema(code, name, fields = []) {
    return {
      schemaVersion: 2,
      object: {
        code,
        name,
        description: '',
      },
      appType: 'SINGLE',
      tableMode: 'CODE',
      tableName: code,
      businessName: name,
      fields: fields.map(toCodeAppModelField),
      relations: [],
      indexes: [],
      policies: {},
      children: [],
    }
  }

  function createCodeAppFormDesignerSchema(metadata = {}, assets = [], fields = [], code = '', name = '') {
    const defaultSchema = () => createDefaultFormDesignerSchema({
      objectCode: code,
      objectName: name,
      formKey: assets[0]?.formKey,
      formName: assets[0]?.formName || name,
      fields,
      includeReadonlyFields: true,
    })
    let source = hasUsableCodeAppFormSchema(metadata.formDesignerSchema)
      ? ensureCodeAppFormSchemaFields(cloneSchema(metadata.formDesignerSchema), fields, {
          objectCode: code,
          objectName: name,
          formKey: assets[0]?.formKey,
          formName: assets[0]?.formName || name,
        })
      : defaultSchema()
    source = ensureCodeAppPrimaryFormSchema(source, fields, {
      objectCode: code,
      objectName: name,
      formKey: assets[0]?.formKey,
      formName: assets[0]?.formName || name,
    })
    const formAssets = normalizeCodeAppAssets(assets).map(asset => ({
      ...asset,
      schema: hasUsableCodeAppFormSchema(asset.schema)
        ? ensureCodeAppFormSchemaFields(asset.schema, asset.fields?.length ? asset.fields : fields, {
            objectCode: code,
            objectName: name,
            formKey: asset.formKey,
            formName: asset.formName,
          })
        : createDefaultFormDesignerSchema({
            objectCode: code,
            objectName: name,
            formKey: asset.formKey,
            formName: asset.formName,
            fields: asset.fields?.length ? asset.fields : fields,
            includeReadonlyFields: true,
          }),
    }))
    return normalizeMultiFormDesignerSchema({
      ...source,
      objectCode: code,
      objectName: name,
      settings: {
        ...(source.settings || {}),
        formAssets,
      },
      forms: Array.isArray(source.forms) && source.forms.length
        ? source.forms
        : formAssets.map(asset => ({
            formKey: asset.formKey,
            formName: asset.formName,
            usage: ['create', 'edit', 'approve'],
            schema: asset.schema,
          })),
    })
  }

  function mergeCodeAppAssets(providerAssets = [], metadataAssets = [], code = '', name = '') {
    const metadataMap = new Map()
    metadataAssets.forEach((asset) => {
      assetKeys(asset).forEach(key => metadataMap.set(key, asset))
    })
    const result = []
    const seenKeys = new Set()
    const source = providerAssets.length ? providerAssets : metadataAssets
    source.forEach((asset) => {
      const configured = findCodeAppAsset(metadataMap, asset)
      appendCodeAppAsset(result, seenKeys, buildMergedCodeAppAsset(asset, configured, code, name), configured)
    })
    metadataAssets.forEach((asset) => {
      appendCodeAppAsset(result, seenKeys, buildMergedCodeAppAsset(asset, null, code, name), asset)
    })
    return result
  }

  function buildMergedCodeAppAsset(asset = {}, configured = null, code = '', name = '') {
    const configuredConfig = pickCodeAppAssetConfig(configured)
    const formMode = configuredConfig.formMode || configuredConfig.type || asset.formMode || asset.type || 'BUSINESS_CODE_FORM'
    const type = configuredConfig.type || configuredConfig.formMode || asset.type || asset.formMode || 'BUSINESS_CODE_FORM'
    const formKey = configuredConfig.formKey || asset.formKey || `${code || 'code_app'}_form`
    const objectName = configuredConfig.objectName || asset.objectName || name
    const businessName = configuredConfig.businessName || asset.businessName || objectName || name
    const appName = configuredConfig.appName || asset.appName || businessName || objectName || name
    const providerKey = configuredConfig.providerKey || asset.providerKey || ''
    const providerName = configuredConfig.providerName || asset.providerName || ''
    const formUrl = configuredConfig.formUrl || asset.formUrl || ''
    const formName = configuredConfig.formName || asset.formName || formKey || name
    const description = configuredConfig.description || asset.description || ''
    const supportsSave = configured && Object.prototype.hasOwnProperty.call(configured, 'supportsSave')
      ? configured.supportsSave !== false
      : asset.supportsSave !== false
    const fields = mergeCodeAppFields(
      asset.fields?.length ? asset.fields : asset.fieldCatalog || [],
      configured?.fields?.length ? configured.fields : configured?.fieldCatalog || [],
    )
    return {
      ...asset,
      ...configuredConfig,
      appName,
      objectCode: configuredConfig.objectCode || asset.objectCode || code,
      objectName,
      businessName,
      formKey,
      formName,
      formMode,
      type,
      providerKey,
      providerName,
      formUrl,
      description,
      supportsSave,
      fields,
      fieldCatalog: fields,
      fieldCount: fields.length,
    }
  }

  function appendCodeAppAsset(result, seenKeys, asset = {}, configured = null) {
    const keys = [...assetKeys(asset), ...assetKeys(configured)]
    if (keys.some(key => seenKeys.has(key)))
      return
    keys.forEach(key => seenKeys.add(key))
    result.push(asset)
  }

  function pickCodeAppAssetConfig(asset = {}) {
    if (!asset)
      return {}
    const result = {}
    ;['appName', 'objectCode', 'objectName', 'businessName', 'formKey', 'formName', 'formMode', 'type', 'providerKey', 'providerName', 'formUrl', 'description'].forEach((key) => {
      if (asset[key] !== undefined && asset[key] !== null && asset[key] !== '')
        result[key] = asset[key]
    })
    if (Object.prototype.hasOwnProperty.call(asset, 'supportsSave'))
      result.supportsSave = asset.supportsSave !== false
    return result
  }

  function createCodeAppPageSchema(source = {}, modelSchema = {}) {
    const base = hasUsableCodeAppPageSchema(source)
      ? cloneSchema(source)
      : createDefaultPageSchema(modelSchema)
    const fields = modelSchema?.fields || []
    if (!fields.length)
      return base
    const fieldCodes = new Set(fields.map(field => field?.field).filter(Boolean))
    const listRefs = collectCodeAppPageFieldRefs(base, ['table', 'list'])
    const editRefs = collectCodeAppPageFieldRefs(base, ['edit', 'detail', 'form'])
    const hasKnownListField = listRefs.some(ref => fieldCodes.has(ref))
    const hasKnownEditField = editRefs.some(ref => fieldCodes.has(ref))
    if (hasKnownListField && hasKnownEditField)
      return base
    const defaults = createDefaultPageSchema(modelSchema)
    return {
      ...base,
      listLayoutMode: base.listLayoutMode || defaults.listLayoutMode,
      listGridLayout: hasKnownListField ? base.listGridLayout : defaults.listGridLayout,
      zones: mergeCodeAppPageZones(base.zones, defaults.zones, { keepList: hasKnownListField, keepEdit: hasKnownEditField }),
      pages: hasKnownListField ? base.pages : defaults.pages,
    }
  }

  function collectCodeAppPageFieldRefs(schema = {}, zones = []) {
    const zoneSet = new Set(zones)
    const result = []
    function pushRefs(refs = []) {
      ;(Array.isArray(refs) ? refs : []).forEach((ref) => {
        if (ref && !result.includes(ref))
          result.push(ref)
      })
    }
    function visitGrid(grid = {}) {
      ;(Array.isArray(grid.items) ? grid.items : []).forEach((item) => {
        pushRefs(item.fieldRefs)
        pushRefs(item.props?.fieldRefs)
        if (item.fieldRef)
          pushRefs([item.fieldRef])
        visitGrid({ items: item.children || [] })
        ;(Array.isArray(item.props?.cells) ? item.props.cells : []).forEach(cell => visitGrid({ items: cell.children || [] }))
      })
    }
    ;(Array.isArray(schema.zones) ? schema.zones : [])
      .filter(zone => zoneSet.has(zone.zoneKey))
      .forEach((zone) => {
        pushRefs(zone.fieldRefs)
        pushRefs(zone.props?.fieldRefs)
        visitGrid(zone.props?.canvas || {})
      })
    if (zoneSet.has('table') || zoneSet.has('list')) {
      visitGrid(schema.listGridLayout || {})
      ;(Array.isArray(schema.pages) ? schema.pages : [])
        .filter(page => page.pageType === 'list' || page.pageKey === 'list')
        .forEach(page => visitGrid(page.gridLayout || {}))
    }
    if (zoneSet.has('detail')) {
      ;(Array.isArray(schema.pages) ? schema.pages : [])
        .filter(page => page.pageType === 'detail' || page.pageKey === 'detail')
        .forEach(page => visitGrid(page.gridLayout || {}))
    }
    return result
  }

  function mergeCodeAppPageZones(currentZones = [], defaultZones = [], options = {}) {
    const current = Array.isArray(currentZones) ? currentZones : []
    const defaults = Array.isArray(defaultZones) ? defaultZones : []
    const currentMap = new Map(current.map(zone => [zone.zoneKey, zone]))
    const result = defaults.map((zone) => {
      if (['table', 'search'].includes(zone.zoneKey) && !options.keepList)
        return zone
      if (['edit', 'detail'].includes(zone.zoneKey) && !options.keepEdit)
        return zone
      return currentMap.get(zone.zoneKey) || zone
    })
    current.forEach((zone) => {
      if (!result.some(item => item.zoneKey === zone.zoneKey))
        result.push(zone)
    })
    return result
  }

  function findCodeAppAsset(metadataMap, asset = {}) {
    for (const key of assetKeys(asset)) {
      if (metadataMap.has(key))
        return metadataMap.get(key)
    }
    return null
  }

  function assetKeys(asset = {}) {
    return [
      asset?.formKey ? `form:${asset.formKey}` : '',
      asset?.providerKey ? `provider:${asset.providerKey}` : '',
    ].filter(Boolean)
  }

  function mergeCodeAppFields(providerFields = [], configuredFields = []) {
    const configuredMap = new Map()
    normalizeCodeAppFields(configuredFields).forEach((field) => {
      configuredMap.set(field.field, field)
    })
    const result = []
    const seen = new Set()
    normalizeCodeAppFields(providerFields).forEach((field) => {
      const configured = configuredMap.get(field.field)
      appendCodeAppDesignField(result, seen, configured ? { ...field, ...configured } : field)
    })
    normalizeCodeAppFields(configuredFields).forEach((field) => {
      appendCodeAppDesignField(result, seen, field)
    })
    return result.sort(compareCodeAppFieldOrder)
  }

  function appendCodeAppDesignField(result, seen, field = {}) {
    const code = field.field || field.fieldCode
    if (!code || seen.has(code) || field.internal === true || field.systemField === true)
      return
    seen.add(code)
    result.push({
      ...field,
      field: code,
      fieldCode: code,
    })
  }

  function compareCodeAppFieldOrder(left = {}, right = {}) {
    const leftOrder = Number(left.sortOrder ?? left.order ?? Number.MAX_SAFE_INTEGER)
    const rightOrder = Number(right.sortOrder ?? right.order ?? Number.MAX_SAFE_INTEGER)
    if (leftOrder !== rightOrder)
      return leftOrder - rightOrder
    return 0
  }

  function createCodeAppViewSchema(source = {}, fields = []) {
    const defaults = createDefaultViewSchema({ fields })
    const current = sanitizeViewSchemaFieldRefs(source || {}, fields)
    return sanitizeViewSchemaFieldRefs({
      ...current,
      search: {
        ...current.search,
        fields: mergeCodeAppViewItems(defaults.search.fields, current.search?.fields || []),
      },
      list: {
        ...current.list,
        columns: mergeCodeAppViewItems(defaults.list.columns, current.list?.columns || []),
      },
      detail: {
        ...current.detail,
        sections: mergeCodeAppDetailSections(defaults.detail.sections, current.detail?.sections || []),
      },
    }, fields)
  }

  function mergeCodeAppViewItems(defaultItems = [], currentItems = []) {
    const result = Array.isArray(currentItems) ? [...currentItems] : []
    const seen = new Set(result.map(item => item?.fieldCode || item?.field).filter(Boolean))
    ;(Array.isArray(defaultItems) ? defaultItems : []).forEach((item) => {
      const field = item?.fieldCode || item?.field
      if (field && !seen.has(field)) {
        seen.add(field)
        result.push(item)
      }
    })
    return result
  }

  function mergeCodeAppDetailSections(defaultSections = [], currentSections = []) {
    if (!hasDetailViewFields(currentSections))
      return defaultSections
    const [defaultSection = { fields: [] }] = defaultSections
    return currentSections.map((section, index) => {
      if (index > 0)
        return section
      return {
        ...section,
        fields: mergeCodeAppViewItems(defaultSection.fields || [], section.fields || []),
      }
    })
  }

  function hasDetailViewFields(sections = []) {
    return Array.isArray(sections) && sections.some(section => Array.isArray(section?.fields) && section.fields.length)
  }

  function hasUsableCodeAppPageSchema(schema = {}) {
    if (!schema || typeof schema !== 'object')
      return false
    if (Array.isArray(schema.zones) && schema.zones.length)
      return true
    if (Array.isArray(schema.listGridLayout?.items) && schema.listGridLayout.items.length)
      return true
    return Array.isArray(schema.pages) && schema.pages.some(page => Array.isArray(page?.gridLayout?.items) && page.gridLayout.items.length)
  }

  function hasUsableCodeAppFormSchema(schema = {}) {
    if (!schema || typeof schema !== 'object')
      return false
    if (hasCodeAppFieldComponents(schema.components))
      return true
    if (Array.isArray(schema.forms) && schema.forms.some(form => hasUsableCodeAppFormSchema(form?.schema || form)))
      return true
    const assets = schema.settings?.formAssets
    return Array.isArray(assets) && assets.some(asset => hasUsableCodeAppFormSchema(asset?.schema || asset))
  }

  function hasCodeAppFieldComponents(components = []) {
    return (Array.isArray(components) ? components : []).some((component) => {
      if (!component || typeof component !== 'object')
        return false
      if (component.fieldBinding?.mode === 'field' && component.fieldBinding?.fieldCode)
        return true
      return hasCodeAppFieldComponents(component.children || [])
    })
  }

  function ensureCodeAppFormSchemaFields(schema = {}, fields = [], options = {}) {
    if (!hasUsableCodeAppFormSchema(schema))
      return schema
    const refs = collectCodeAppFormFieldRefs(schema)
    const missingFields = (fields || [])
      .filter(field => field && field.formVisible !== false && !field.internal && !field.systemField)
      .filter(field => !refs.has(field.field || field.fieldCode))
    if (!missingFields.length)
      return schema
    const appendSchema = createDefaultFormDesignerSchema({
      objectCode: options.objectCode,
      objectName: options.objectName,
      formKey: options.formKey || schema.formKey,
      formName: options.formName || schema.formName,
      fields: missingFields,
      includeReadonlyFields: true,
    })
    return {
      ...schema,
      components: [
        ...(Array.isArray(schema.components) ? schema.components : []),
        ...(appendSchema.components || []),
      ],
    }
  }

  function ensureCodeAppPrimaryFormSchema(schema = {}, fields = [], options = {}) {
    if (hasCodeAppFieldComponents(schema.components))
      return schema
    const fallback = createDefaultFormDesignerSchema({
      objectCode: options.objectCode,
      objectName: options.objectName,
      formKey: options.formKey || schema.formKey,
      formName: options.formName || schema.formName,
      fields,
      includeReadonlyFields: true,
    })
    return {
      ...schema,
      formKey: schema.formKey || fallback.formKey,
      formName: schema.formName || fallback.formName,
      layout: {
        ...(fallback.layout || {}),
        ...(schema.layout || {}),
      },
      components: fallback.components || [],
    }
  }

  function collectCodeAppFormFieldRefs(schema = {}) {
    const result = new Set()
    function visitSchema(value = {}) {
      if (!value || typeof value !== 'object')
        return
      visitComponents(value.components || [])
      ;(Array.isArray(value.forms) ? value.forms : []).forEach(form => visitSchema(form?.schema || form))
      ;(Array.isArray(value.settings?.formAssets) ? value.settings.formAssets : []).forEach(asset => visitSchema(asset?.schema || asset))
    }
    function visitComponents(components = []) {
      ;(Array.isArray(components) ? components : []).forEach((component) => {
        if (!component || typeof component !== 'object')
          return
        const fieldCode = String(component.fieldBinding?.fieldCode || '').trim()
        if (component.fieldBinding?.mode === 'field' && fieldCode)
          result.add(fieldCode)
        if (Array.isArray(component.children))
          visitComponents(component.children)
      })
    }
    visitSchema(schema)
    return result
  }

  function applyCodeAppFormVisibility(fields = [], formDesignerSchema = {}) {
    const visibility = collectCodeAppFormFieldVisibility(formDesignerSchema)
    if (!visibility.touched)
      return fields
    return fields.map((field) => {
      const code = field.field || field.fieldCode
      const state = visibility.map.get(code)
      return {
        ...field,
        formVisible: state ? state.visible !== false : false,
        readonly: state?.readonly === true || field.readonly === true,
        writable: state?.readonly === true ? false : field.writable,
        label: state?.label || field.label,
        fieldName: state?.label || field.fieldName,
      }
    })
  }

  function collectCodeAppFormFieldVisibility(schema = {}) {
    const result = new Map()
    function visitSchema(value = {}) {
      if (!value || typeof value !== 'object')
        return
      visitComponents(value.components || [])
      ;(Array.isArray(value.forms) ? value.forms : []).forEach(form => visitSchema(form?.schema || form))
      ;(Array.isArray(value.settings?.formAssets) ? value.settings.formAssets : []).forEach(asset => visitSchema(asset?.schema || asset))
    }
    function visitComponents(components = []) {
      ;(Array.isArray(components) ? components : []).forEach((component) => {
        if (!component || typeof component !== 'object')
          return
        const fieldCode = String(component.fieldBinding?.fieldCode || '').trim()
        if (component.fieldBinding?.mode === 'field' && fieldCode && !result.has(fieldCode)) {
          result.set(fieldCode, {
            visible: component.visibility?.hidden !== true,
            readonly: component.visibility?.readonly === true,
            label: component.label || '',
          })
        }
        if (Array.isArray(component.children))
          visitComponents(component.children)
      })
    }
    visitSchema(schema)
    return {
      touched: result.size > 0,
      map: result,
    }
  }

  function normalizeCodeAppAssets(source = []) {
    return (Array.isArray(source) ? source : [])
      .filter(Boolean)
      .map(asset => ({
        ...cloneSchema(asset),
        formMode: asset.formMode || asset.type || 'BUSINESS_CODE_FORM',
        type: asset.type || asset.formMode || 'BUSINESS_CODE_FORM',
        formKey: asset.formKey || 'default',
        formName: asset.formName || asset.objectName || '代码业务表单',
        providerKey: asset.providerKey || '',
        formUrl: asset.formUrl || '',
        fields: normalizeCodeAppFields(asset.fields?.length ? asset.fields : asset.fieldCatalog || []),
        fieldCatalog: normalizeCodeAppFields(asset.fieldCatalog?.length ? asset.fieldCatalog : asset.fields || []),
      }))
  }

  function collectCodeAppProviderCatalogAssets(source = []) {
    return (Array.isArray(source) ? source : [])
      .filter(Boolean)
      .flatMap((provider) => {
        const providerKey = String(provider?.providerKey || '').trim()
        const providerName = String(provider?.providerName || providerKey || '').trim()
        return normalizeCodeAppAssets(provider?.assets || []).map(asset => ({
          ...asset,
          providerKey: asset.providerKey || providerKey,
          providerName: asset.providerName || providerName,
        }))
      })
  }

  function hydrateCodeAppProviderAssets(assets = [], catalogAssets = []) {
    if (!assets.length)
      return catalogAssets
    const catalogMap = new Map(catalogAssets.flatMap(asset => assetKeys(asset).map(key => [key, asset])))
    const result = []
    const seen = new Set()
    assets.forEach((asset) => {
      const catalogAsset = findCodeAppAsset(catalogMap, asset)
      const fields = asset.fields?.length || asset.fieldCatalog?.length
        ? mergeCodeAppFields(asset.fields?.length ? asset.fields : asset.fieldCatalog, [])
        : mergeCodeAppFields(catalogAsset?.fields?.length ? catalogAsset.fields : catalogAsset?.fieldCatalog || [], [])
      appendCodeAppAsset(result, seen, {
        ...(catalogAsset || {}),
        ...asset,
        fields,
        fieldCatalog: fields,
        fieldCount: fields.length,
      }, catalogAsset)
    })
    catalogAssets.forEach(asset => appendCodeAppAsset(result, seen, asset))
    return result
  }

  function collectCodeAppAssetFields(assets = []) {
    const result = []
    const seen = new Set()
    assets.forEach((asset) => {
      const fields = asset.fields?.length ? asset.fields : asset.fieldCatalog || []
      fields.forEach((field) => {
        const code = fieldCode(field)
        if (!code || seen.has(code))
          return
        seen.add(code)
        result.push(field)
      })
    })
    return result
  }

  function normalizeCodeAppFields(source = []) {
    const result = []
    const seen = new Set()
    ;(Array.isArray(source) ? source : []).forEach((field) => {
      const code = fieldCode(field)
      if (!code || seen.has(code))
        return
      seen.add(code)
      const componentType = normalizeCodeAppComponentType(field.componentType || field.type || 'input')
      result.push({
        ...cloneSchema(field),
        field: code,
        fieldCode: code,
        fieldName: field.fieldName || field.label || field.fieldLabel || code,
        label: field.label || field.fieldLabel || field.fieldName || code,
        componentType,
        type: componentType,
        visible: field.visible !== false,
        writable: field.writable !== false && field.readonly !== true,
        readonly: field.readonly === true || field.writable === false,
        internal: field.internal === true,
        systemField: field.systemField === true,
        formVisible: field.formVisible !== false && field.visible !== false,
        listVisible: field.listVisible !== false && field.visible !== false,
        searchable: field.searchable === true,
      })
    })
    return result
  }

  function toCodeAppModelField(field = {}) {
    const code = field.field || field.fieldCode
    return {
      ...field,
      field: code,
      fieldCode: code,
      label: field.label || field.fieldName || code,
      columnName: field.columnName || code,
      dataType: field.dataType || inferCodeAppDataType(field.componentType || field.type),
      componentType: normalizeCodeAppComponentType(field.componentType || field.type),
      fieldStatus: field.fieldStatus || 'NORMAL',
      formVisible: field.formVisible !== false && field.visible !== false,
      listVisible: field.listVisible !== false && field.visible !== false,
      searchable: field.searchable === true,
      readonly: field.readonly === true,
      systemField: field.systemField === true,
      required: field.required === true,
    }
  }

  function fieldCode(field = {}) {
    return String(field.field || field.fieldCode || field.code || field.name || '').trim()
  }

  function normalizeCodeAppComponentType(type) {
    const value = String(type || 'input').trim()
    const aliases = {
      inputNumber: 'number',
      integer: 'number',
      file: 'fileUpload',
      image: 'imageUpload',
      upload: 'fileUpload',
    }
    return aliases[value] || value || 'input'
  }

  function inferCodeAppDataType(componentType) {
    const type = normalizeCodeAppComponentType(componentType)
    if (['number', 'money'].includes(type))
      return 'decimal'
    if (type === 'date')
      return 'date'
    if (type === 'datetime')
      return 'datetime'
    return 'varchar'
  }

  function toFieldPayload(field = {}) {
    return {
      fieldName: field.fieldName || field.label || field.fieldCode || field.field,
      fieldCode: field.fieldCode || field.field,
      columnName: field.columnName,
      fieldType: field.fieldType,
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
      placeholder: field.basicProps?.placeholder || '',
      remark: field.remark,
      sortOrder: field.sortOrder,
      fieldBinding: cloneSchema(field.fieldBinding || {}),
      basicProps: cloneSchema(field.basicProps || {}),
      advancedProps: cloneSchema(field.advancedProps || {}),
    }
  }

  function toPageField(field = {}) {
    return {
      ...field,
      field: field.field || field.fieldCode,
      label: field.label || field.fieldName || field.fieldCode,
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
      basicProps: cloneSchema(field.basicProps || {}),
      advancedProps: cloneSchema(field.advancedProps || {}),
    }
  }

  function isInactiveField(field = {}) {
    const status = String(field.fieldStatus || '').toUpperCase()
    return status === 'DISABLED' || status === 'HIDDEN'
  }

  function syncDraftModelFields(fields = []) {
    const modelFields = draft.modelSchema?.fields || []
    const existingMap = new Map(modelFields.map(field => [field.field, field]))
    draft.modelSchema = {
      ...(draft.modelSchema || {}),
      fields: fields.map((field) => {
        const fieldCode = field.fieldCode || field.field
        return {
          ...(existingMap.get(fieldCode) || {}),
          field: fieldCode,
          columnName: field.columnName,
          label: field.fieldName || field.label || fieldCode,
          dataType: field.dataType,
          length: field.length,
          precision: field.precision,
          required: field.required,
          defaultValue: field.defaultValue,
          searchable: field.searchable,
          listVisible: field.listVisible,
          formVisible: field.formVisible,
          componentType: field.componentType,
          queryType: field.queryType,
          dictType: field.dictType,
          sensitiveType: field.sensitiveType,
          encryptAlgorithm: field.encryptAlgorithm,
          sortable: field.sortable,
          systemField: field.systemField,
          readonly: field.readonly,
          width: field.width,
          remark: field.remark,
          businessFieldType: field.fieldType,
          fieldStatus: field.fieldStatus,
          importable: field.importable,
          exportable: field.exportable,
          referenceObjectCode: field.referenceObjectCode,
          referenceDisplayField: field.referenceDisplayField,
          sortOrder: field.sortOrder,
          formulaConfig: cloneSchema(field.formulaConfig || null),
          basicProps: cloneSchema(field.basicProps || {}),
          advancedProps: cloneSchema(field.advancedProps || {}),
        }
      }),
    }
  }

  function updateFormDesignerFieldGeneration(schema = {}, fieldCode = '', generation = {}, options = {}) {
    const next = cloneSchema(schema || {})
    const hideOnForm = options.hideOnForm === true
    visitFormDesignerSchema(next)
    return next

    function visitFormDesignerSchema(value = {}) {
      if (!value || typeof value !== 'object')
        return
      visitFormDesignerComponents(value.components || [])
      ;(Array.isArray(value.forms) ? value.forms : []).forEach(form => visitFormDesignerSchema(form?.schema || form))
      ;(Array.isArray(value.settings?.formAssets) ? value.settings.formAssets : []).forEach(asset => visitFormDesignerSchema(asset?.schema || asset))
    }

    function visitFormDesignerComponents(components = []) {
      ;(Array.isArray(components) ? components : []).forEach((component) => {
        if (!component || typeof component !== 'object')
          return
        if (component.fieldBinding?.mode === 'field' && component.fieldBinding?.fieldCode === fieldCode) {
          component.props = {
            ...(component.props || {}),
            generation,
          }
          if (generation.enabled === true) {
            component.validation = {
              ...(component.validation || {}),
              required: false,
              requiredMessage: '',
            }
            component.visibility = {
              ...(component.visibility || {}),
              readonly: true,
              // 与属性面板一致：默认只读可见，避免运行态整字段被投影滤掉
              hidden: hideOnForm,
            }
          }
        }
        if (Array.isArray(component.children))
          visitFormDesignerComponents(component.children)
      })
    }
  }

  const FIELD_REF_KEYS = new Set([
    'field',
    'fieldCode',
    'fieldRef',
    'queryField',
    'sourceField',
    'targetField',
    'displayField',
  ])

  function renameFieldRefsInValue(value, rename = {}) {
    return renameFieldRefsInClonedValue(cloneSchema(value || {}), rename)
  }

  function renameFieldRefsInClonedValue(value, rename = {}) {
    if (Array.isArray(value)) {
      return value.map((item) => {
        if (item === rename.oldFieldCode)
          return rename.newFieldCode
        if (rename.oldColumnName && item === rename.oldColumnName)
          return rename.newColumnName || item
        return renameFieldRefsInClonedValue(item, rename)
      })
    }
    if (!value || typeof value !== 'object')
      return value
    if (value.fieldSettings && typeof value.fieldSettings === 'object' && !Array.isArray(value.fieldSettings)) {
      if (Object.prototype.hasOwnProperty.call(value.fieldSettings, rename.oldFieldCode)) {
        const oldSetting = value.fieldSettings[rename.oldFieldCode]
        delete value.fieldSettings[rename.oldFieldCode]
        value.fieldSettings[rename.newFieldCode] = oldSetting
      }
    }
    Object.keys(value).forEach((key) => {
      const item = value[key]
      if (FIELD_REF_KEYS.has(key) && item === rename.oldFieldCode) {
        value[key] = rename.newFieldCode
        return
      }
      if (key === 'columnName' && rename.oldColumnName && item === rename.oldColumnName) {
        value[key] = rename.newColumnName || item
        return
      }
      value[key] = renameFieldRefsInClonedValue(item, rename)
    })
    return value
  }

  function handleBeforeUnload(event) {
    if (!dirty.value)
      return
    event.preventDefault()
    event.returnValue = ''
  }

  function confirmLeave() {
    return new Promise((resolve) => {
      if (!window.$dialog) {
        resolve(false)
        return
      }
      window.$dialog.warning({
        title: '未保存变更',
        content: '当前设计器有未保存变更，确认离开吗？',
        positiveText: '离开',
        negativeText: '取消',
        onPositiveClick: () => resolve(true),
        onNegativeClick: () => resolve(false),
        onClose: () => resolve(false),
      })
    })
  }

  function hasPermission(source, permission) {
    if (!Array.isArray(source))
      return false
    return source.includes(permission) || source.includes('**') || source.includes('*:*:*')
  }

  expose?.({
    save: handleSave,
  })
  __impl.createDraftFromDesigner = createDraftFromDesigner
  __impl.resolveCodeAppMetadata = resolveCodeAppMetadata
  __impl.createCodeAppModelSchema = createCodeAppModelSchema
  __impl.createCodeAppFormDesignerSchema = createCodeAppFormDesignerSchema
  __impl.mergeCodeAppAssets = mergeCodeAppAssets
  __impl.buildMergedCodeAppAsset = buildMergedCodeAppAsset
  __impl.appendCodeAppAsset = appendCodeAppAsset
  __impl.pickCodeAppAssetConfig = pickCodeAppAssetConfig
  __impl.createCodeAppPageSchema = createCodeAppPageSchema
  __impl.collectCodeAppPageFieldRefs = collectCodeAppPageFieldRefs
  __impl.mergeCodeAppPageZones = mergeCodeAppPageZones
  __impl.findCodeAppAsset = findCodeAppAsset
  __impl.assetKeys = assetKeys
  __impl.mergeCodeAppFields = mergeCodeAppFields
  __impl.appendCodeAppDesignField = appendCodeAppDesignField
  __impl.compareCodeAppFieldOrder = compareCodeAppFieldOrder
  __impl.createCodeAppViewSchema = createCodeAppViewSchema
  __impl.mergeCodeAppViewItems = mergeCodeAppViewItems
  __impl.mergeCodeAppDetailSections = mergeCodeAppDetailSections
  __impl.hasDetailViewFields = hasDetailViewFields
  __impl.hasUsableCodeAppPageSchema = hasUsableCodeAppPageSchema
  __impl.hasUsableCodeAppFormSchema = hasUsableCodeAppFormSchema
  __impl.hasCodeAppFieldComponents = hasCodeAppFieldComponents
  __impl.ensureCodeAppFormSchemaFields = ensureCodeAppFormSchemaFields
  __impl.ensureCodeAppPrimaryFormSchema = ensureCodeAppPrimaryFormSchema
  __impl.collectCodeAppFormFieldRefs = collectCodeAppFormFieldRefs
  __impl.applyCodeAppFormVisibility = applyCodeAppFormVisibility
  __impl.collectCodeAppFormFieldVisibility = collectCodeAppFormFieldVisibility
  __impl.normalizeCodeAppAssets = normalizeCodeAppAssets
  __impl.collectCodeAppProviderCatalogAssets = collectCodeAppProviderCatalogAssets
  __impl.hydrateCodeAppProviderAssets = hydrateCodeAppProviderAssets
  __impl.collectCodeAppAssetFields = collectCodeAppAssetFields
  __impl.normalizeCodeAppFields = normalizeCodeAppFields
  __impl.toCodeAppModelField = toCodeAppModelField
  __impl.fieldCode = fieldCode
  __impl.normalizeCodeAppComponentType = normalizeCodeAppComponentType
  __impl.inferCodeAppDataType = inferCodeAppDataType
  __impl.toFieldPayload = toFieldPayload
  __impl.toPageField = toPageField
  __impl.isInactiveField = isInactiveField
  __impl.syncDraftModelFields = syncDraftModelFields
  __impl.updateFormDesignerFieldGeneration = updateFormDesignerFieldGeneration
  __impl.renameFieldRefsInValue = renameFieldRefsInValue
  __impl.renameFieldRefsInClonedValue = renameFieldRefsInClonedValue
  __impl.handleBeforeUnload = handleBeforeUnload
  __impl.confirmLeave = confirmLeave
  __impl.hasPermission = hasPermission

  return {
    ...deps,
    FIELD_REF_KEYS,
  }
}
