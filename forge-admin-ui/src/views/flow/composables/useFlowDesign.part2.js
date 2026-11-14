/** flow/design setup part 2. */
import { NTag, NTreeSelect } from 'naive-ui'
import { computed, defineAsyncComponent, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { modelListByProvider, providerPage } from '@/api/ai'
import { businessFlowFormAssets, businessFlowModelBindings } from '@/api/business-app'
import { businessApplicationList, businessApplicationObjects } from '@/api/business-application'
import flowApi from '@/api/flow'
import { streamFlowGenerate } from '@/api/flow-generator'
import messageApi from '@/api/message'
import NodePropertiesPanel from '@/components/bpmn/NodePropertiesPanel.vue'
import { DingFlowDesigner } from '@/components/flow-designer'
import FlowPropertyPanelShell from '@/components/flow/FlowPropertyPanelShell.vue'
import FlowFormCreateDesigner from '@/components/form-create/FlowFormCreateDesigner.vue'
import FlowFormCreateRenderer from '@/components/form-create/FlowFormCreateRenderer.vue'
import { useDict } from '@/composables/useDict'
import { useTabStore } from '@/store'
import { toNumberDictOptions } from '@/utils/dict-options'
import { appendChildTableCatalogFields } from '@/utils/flow-field-permissions'
import { loadFlowBusinessFormFieldCatalog } from '@/utils/flow-form-loader'
import { sanitizeHtml } from '@/utils/sanitize-html'
import BusinessFlowFormAssetSelect from '@/views/app-center/components/designer/BusinessFlowFormAssetSelect.vue'
import { buildFlowCategoryTreeOptions, resolveFlowCategoryValue } from '../utils/categoryOptions'
import { buildLocalFormFieldCatalog } from '../utils/form-field-catalog'
import VersionHistory from '../version.vue'
export function applyFlowDesignPart2(props, emit, deps = {}) {
  const {
    __impl, mut, buildDiagramXml, buildNotifyConfig, buildWaypoints, channelsForEvent, defaultNotifyChannels, ensureDiagramNamespaces,
    ensureProcessId, escapeXmlAttr, extractBpmnXml, extractJsonObject, findElementByLocalName, findElementsByLocalName, getBpmnDisplayIssue, getBpmnNodeSize,
    getElementIcon, getElementTitle, getRouteModelId, getSettingsBadge, getSettingsBadgeClass, getXmlParseError, handleBack, handleBpmnChange,
    handleBusinessApplicationChange, handleBusinessElementSelect, handleBusinessPanelClose, handleDeploy, handleDiagramImportEnd, handleDiagramImportStart, handleModelerReady, handleSaveDraft,
    hasBpmnDiagram, isBpmnFlowNode, layoutBpmnNodes, loadBusinessApplicationOptions, loadBusinessObjectOptions, loadCategories, loadForms, loadModelOptions,
    loadNotificationMetadata, loadProviderOptions, normalizeBpmnId, normalizeBusinessGlobalFormBeforeSave, normalizeNotifyChannels, notificationTemplateOptions, notificationTemplatePrefix, notificationTemplateSummary,
    openSettingsPanel, parseNotifyConfig, parseXmlDocument, previewNotificationTemplate, rebuildDiagramInfo, refreshExternalFormFieldCatalog, refreshFormFieldCatalog, renderNotificationTemplate,
    repairBpmnXml, resetNewModelState, resetNotifyMatrix, selectedNotificationTemplateValue, setWorkspaceMode, stripDiagramInfo, syncSafeModelKey, updateNotificationTemplate,
    updateNotifyChannels, validateBusinessGlobalFormBeforeSave, route, router, tabStore, message, FlowModeler, DEFAULT_TODO_DETAIL_URL_TEMPLATE,
    embedded, explicitBusinessObjectCode, explicitBusinessObjectName, explicitBusinessApplicationId, explicitBusinessEntryRoute, resolvedBusinessBinding, manualBusinessApplicationId, manualBusinessApplicationName,
    manualBusinessObjectCode, manualBusinessObjectName, businessApplicationOptions, businessApplicationLoading, businessObjectOptions, businessObjectLoading, businessObjectOptionsApplicationId, businessObjectCode,
    businessContextActive, businessContextName, businessEntryRoute, effectiveApplicationId, businessApplicationName, effectiveCodeApp, saving, deploying,
    pageLoading, diagramLoadingCount, modelerRef, bpmnXml, hasChanges, aiSending, aiPrompt, aiSessionId,
    aiMessages, aiDraft, showAiXmlPreview, showVersionHistory, aiAbortController, aiRawContent, aiReasoningContent, aiIsReasoningPhase,
    aiReasoningStartTime, aiReasoningEndTime, aiCurrentStage, expandedReasonings, aiMessageListRef, aiMessageEndRef, reasoningContentRefs, showModelPanel,
    syncingModel, aiProviderId, aiModelId, providerOptions, modelOptions, workspaceMode, rightActiveTab, modelInfo,
    showFormDesigner, formDesignerRef, formSchema, formOptions, formFieldCatalog, formFieldCatalogError, showFormPreview, businessFormAssets,
    categoryTreeOptions, modelerInstance, dockedElement, notifyEventDefinitions, notifyChannelOrder, defaultNotificationTemplateCodes, notifyChannels, notifyChannelsLoading,
    notifyChannelsLoaded, notificationTemplates, notificationTemplatesLoading, notificationTemplatesLoaded, notifyMatrixDirty, notifyConfigInitialPresent, notifyMatrix, notifyTemplateCodes,
    notifyEventConfigured, showNotificationTemplatePreview, notificationPreview, designerTypeOptions, formTypeOptions, businessManagedFormTypeOptions, autoApprovalModeOptions, modelStatusOptions,
    aiExamples, aiStages, currentAiStageIndex, statusTag, isReadonly, designerLoading, aiGeneratingCanvasHintVisible, aiCanvasHintText,
    currentProviderLabel, currentModelLabel, designerType, isApprovalDesigner, isBusinessDesigner, designerRenderKey, designerTypeLabel, businessPanelTitle,
    businessPanelIcon, processConfig, appManagedFormTypeActive, businessFormConfigActive, businessApplicationPickerVisible, businessObjectPickerVisible, nodeFormAssetOptions, businessGlobalFormRef,
    businessGlobalFormNode, selectedBusinessGlobalFormAsset, formConfigStatus, notifySmsSelected, notificationConfigStatus, autoApprovalModeLabel, isAiPanelActive, settingsTreeGroups,
  } = deps
  async function handleBusinessObjectChange(value) {
    manualBusinessObjectCode.value = routeQueryText(value)
    const selected = businessObjectOptions.value.find(item => item.value === manualBusinessObjectCode.value)?.object
    manualBusinessObjectName.value = routeQueryText(selected?.objectName) || manualBusinessObjectCode.value
    modelInfo.formType = 'business'
    modelInfo.formId = null
    modelInfo.formUrl = ''
    modelInfo.formJson = ''
    formSchema.value = []
    businessFormAssets.value = []
    formFieldCatalog.value = []
    if (manualBusinessObjectCode.value)
      await refreshBusinessFormFieldCatalog()
    hasChanges.value = true
  }

  async function handleBusinessManagedFormTypeChange(value) {
    if (value === 'none') {
      modelInfo.formType = 'none'
      modelInfo.formId = null
      modelInfo.formUrl = ''
      modelInfo.formJson = ''
      formSchema.value = []
      formFieldCatalog.value = []
      hasChanges.value = true
      return
    }
    if (value === 'external') {
      modelInfo.formType = 'external'
      modelInfo.formId = null
      modelInfo.formJson = ''
      formSchema.value = []
      formFieldCatalog.value = []
      hasChanges.value = true
      refreshFormFieldCatalog()
      return
    }
    // 业务应用表单统一使用 business；dynamic 是历史动态表单值，不能再写回业务模型。
    modelInfo.formType = 'business'
    modelInfo.formId = null
    modelInfo.formUrl = ''
    formSchema.value = []
    if (!businessContextActive.value) {
      await loadBusinessApplicationOptions()
      modelInfo.formJson = ''
      formFieldCatalog.value = []
      hasChanges.value = true
      return
    }
    await refreshBusinessFormFieldCatalog()
    hasChanges.value = true
  }

  async function resolveBusinessBindingForModel() {
    if (explicitBusinessObjectCode.value) {
      resolvedBusinessBinding.value = null
      return
    }

    const modelKey = routeQueryText(modelInfo.modelKey)
    if (!modelKey) {
      resolvedBusinessBinding.value = null
      return
    }

    try {
      const res = await businessFlowModelBindings(modelKey)
      const bindings = res.code === 200 && Array.isArray(res.data)
        ? res.data.filter(item => routeQueryText(item?.objectCode))
        : []
      resolvedBusinessBinding.value = bindings[0] || null
    }
    catch (error) {
      console.warn('[FlowDesign] 反查业务对象绑定失败:', modelKey, error?.message || error)
      resolvedBusinessBinding.value = null
    }
  }

  async function refreshBusinessFormFieldCatalog() {
    const applicationId = effectiveApplicationId.value
    if (!businessObjectCode.value || !applicationId) {
      businessFormAssets.value = []
      formFieldCatalog.value = []
      return
    }
    try {
      const res = await businessFlowFormAssets(businessObjectCode.value, {
        includeInternal: true,
        applicationId,
      })
      const assets = normalizeBusinessFormAssets(res.data?.formAssets || [])
      businessFormAssets.value = assets
      ensureBusinessGlobalFormSelection(assets)
      formFieldCatalog.value = collectBusinessAssetFields(selectedBusinessGlobalFormAsset.value
        ? [selectedBusinessGlobalFormAsset.value]
        : assets)
    }
    catch (error) {
      console.warn('[FlowDesign] 加载业务表单字段目录失败:', error?.message || error)
      businessFormAssets.value = []
      formFieldCatalog.value = []
    }
  }

  function normalizeBusinessFormAssets(assets = []) {
    return (Array.isArray(assets) ? assets : [])
      .map((asset) => {
        const formKey = routeQueryText(asset?.formKey || asset?.key || asset?.id)
        const fieldCatalog = Array.isArray(asset?.fieldCatalog) ? asset.fieldCatalog : []
        const fields = fieldCatalog.length
          ? fieldCatalog
          : Array.isArray(asset?.fields) ? asset.fields : []
        return {
          ...asset,
          formKey,
          formName: routeQueryText(asset?.formName || asset?.name || asset?.label) || formKey,
          fieldCatalog: appendChildTableCatalogFields(normalizeBusinessFieldCatalog(fields), asset),
        }
      })
      .filter(asset => asset.formKey)
  }

  function handleBusinessGlobalFormUpdate(payload = {}) {
    const formKey = routeQueryText(payload.formKey)
    modelInfo.formType = normalizeAppManagedFormType(modelInfo.formType)
    modelInfo.formId = null
    modelInfo.formUrl = ''
    formSchema.value = []

    if (!formKey) {
      modelInfo.formJson = ''
      formFieldCatalog.value = collectBusinessAssetFields(businessFormAssets.value)
      hasChanges.value = true
      return
    }

    const asset = findBusinessFormAsset(
      formKey,
      payload.providerKey,
      payload.formMode || payload.formRef?.formMode || payload.formRef?.type,
    ) || {}
    modelInfo.formJson = buildBusinessGlobalFormJson({
      ...asset,
      ...payload,
      formKey,
    })
    formFieldCatalog.value = collectBusinessAssetFields(asset.formKey ? [asset] : businessFormAssets.value)
    hasChanges.value = true
  }

  function ensureBusinessGlobalFormSelection(assets = businessFormAssets.value) {
    if (!businessContextActive.value)
      return
    if (!isAppManagedFormType(modelInfo.formType))
      return
    const availableAssets = Array.isArray(assets) ? assets.filter(asset => routeQueryText(asset?.formKey)) : []
    if (!availableAssets.length)
      return

    const currentRef = parseBusinessGlobalFormRef(modelInfo.formJson)
    const currentFormKey = routeQueryText(currentRef.formKey)
    const currentProviderKey = routeQueryText(currentRef.providerKey)
    const currentFormMode = normalizeBusinessFormMode(
      currentRef.formMode || currentRef.type || currentRef.formRef?.formMode || currentRef.formRef?.type,
    )
    const currentAsset = currentFormKey
      ? findBusinessFormAssetInList(availableAssets, currentFormKey, currentProviderKey, currentFormMode)
      : null

    if (isAppManagedFormType(modelInfo.formType) && currentAsset) {
      const normalizedFormJson = buildBusinessGlobalFormJson({
        ...currentAsset,
        ...(currentRef.formRef || {}),
        ...currentRef,
        formKey: currentFormKey,
      })
      if (normalizedFormJson)
        modelInfo.formJson = normalizedFormJson
      return
    }

    const preferredFormKey = routeQueryText(props.businessFormKey)
    const preferredAsset = preferredFormKey
      ? findBusinessFormAssetInList(availableAssets, preferredFormKey)
      : null
    const nextAsset = currentAsset || preferredAsset || availableAssets[0]
    modelInfo.formType = normalizeAppManagedFormType(modelInfo.formType)
    modelInfo.formId = null
    modelInfo.formUrl = ''
    formSchema.value = []
    modelInfo.formJson = buildBusinessGlobalFormJson(nextAsset)
  }

  function buildBusinessGlobalFormJson(source = {}) {
    const formKey = routeQueryText(source.formKey || source.key || source.value)
    if (!formKey)
      return ''
    const formMode = normalizeBusinessFormMode(source.formMode || source.type, 'BUSINESS_OBJECT_FORM')
    const providerKey = routeQueryText(source.providerKey)
    const formRef = {
      ...(source.formRef || {}),
      objectCode: source.objectCode || businessObjectCode.value,
      objectName: source.objectName || businessContextName.value,
      type: formMode,
      formMode,
      formKey,
      formName: source.formName || source.name || source.label || formKey,
      providerKey,
      formUrl: source.formUrl || '',
      viewKey: source.viewKey || 'default',
      applicationId: source.applicationId || effectiveApplicationId.value || '',
      applicationName: source.applicationName || businessApplicationName.value || '',
      pageId: source.pageId || '',
      pageCode: source.pageCode || '',
      pageName: source.pageName || '',
      pageType: source.pageType || '',
      sourceFormKey: source.sourceFormKey || '',
    }
    return JSON.stringify({
      type: formMode,
      formMode,
      objectCode: formRef.objectCode,
      objectName: formRef.objectName,
      formKey,
      formName: formRef.formName,
      providerKey,
      formUrl: formRef.formUrl,
      viewKey: formRef.viewKey,
      applicationId: formRef.applicationId,
      applicationName: formRef.applicationName,
      pageId: formRef.pageId,
      pageCode: formRef.pageCode,
      pageName: formRef.pageName,
      pageType: formRef.pageType,
      sourceFormKey: formRef.sourceFormKey,
      formRef,
    })
  }

  function parseBusinessGlobalFormRef(value, fallbackObjectCode) {
    const text = routeQueryText(value)
    if (!text)
      return {}
    try {
      const parsed = JSON.parse(text)
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
        return {}
      const formRef = parsed.formRef && typeof parsed.formRef === 'object' && !Array.isArray(parsed.formRef)
        ? parsed.formRef
        : {}
      const resolvedObjectCode = formRef.objectCode || parsed.objectCode
        || (fallbackObjectCode !== undefined ? fallbackObjectCode : businessObjectCode.value)
      const normalizedFormRef = {
        ...formRef,
        objectCode: resolvedObjectCode,
        objectName: formRef.objectName || parsed.objectName,
        formMode: formRef.formMode || formRef.type || parsed.formMode || parsed.type,
        type: formRef.type || formRef.formMode || parsed.type || parsed.formMode,
        formKey: formRef.formKey || parsed.formKey,
        formName: formRef.formName || parsed.formName,
        providerKey: formRef.providerKey || parsed.providerKey,
        formUrl: formRef.formUrl || parsed.formUrl,
        viewKey: formRef.viewKey || parsed.viewKey,
        applicationId: formRef.applicationId || parsed.applicationId,
        applicationName: formRef.applicationName || parsed.applicationName,
        pageId: formRef.pageId || parsed.pageId,
        pageCode: formRef.pageCode || parsed.pageCode,
        pageName: formRef.pageName || parsed.pageName,
        pageType: formRef.pageType || parsed.pageType,
        sourceFormKey: formRef.sourceFormKey || parsed.sourceFormKey,
      }
      return {
        ...formRef,
        ...parsed,
        ...normalizedFormRef,
        formRef: normalizedFormRef,
      }
    }
    catch (error) {
      console.warn('[FlowDesign] 解析业务全局表单引用失败:', error?.message || error)
      return {}
    }
  }

  function findBusinessFormAsset(formKey, providerKey = '', formMode = '') {
    return findBusinessFormAssetInList(businessFormAssets.value, formKey, providerKey, formMode)
  }

  function findBusinessFormAssetInList(assets = [], formKey, providerKey = '', formMode = '') {
    const key = routeQueryText(formKey)
    const provider = routeQueryText(providerKey)
    const mode = normalizeBusinessFormMode(formMode)
    if (!key)
      return null
    const availableAssets = Array.isArray(assets) ? assets : []
    const sameFormKey = availableAssets.filter(asset => routeQueryText(asset?.formKey) === key)
    if (!sameFormKey.length)
      return null
    const exactAsset = sameFormKey.find(asset =>
      (!mode || normalizeBusinessFormMode(asset.formMode || asset.type, 'BUSINESS_OBJECT_FORM') === mode)
      && (!provider || routeQueryText(asset.providerKey) === provider),
    )
    if (exactAsset)
      return exactAsset
    if (provider) {
      const providerAsset = sameFormKey.find(asset => routeQueryText(asset.providerKey) === provider)
      if (providerAsset)
        return providerAsset
    }
    if (mode) {
      const modeAsset = sameFormKey.find(asset =>
        normalizeBusinessFormMode(asset.formMode || asset.type, 'BUSINESS_OBJECT_FORM') === mode,
      )
      if (modeAsset)
        return modeAsset
    }
    return sameFormKey[0] || null
  }

  function normalizeBusinessFormMode(value, fallback = '') {
    const normalized = routeQueryText(value).toUpperCase()
    if (normalized === 'BUSINESS_CODE_FORM' || normalized === 'BUSINESS_OBJECT_FORM' || normalized === 'EXTERNAL')
      return normalized
    return fallback
  }

  function isAppManagedFormType(value) {
    return value === 'business'
  }

  function normalizeAppManagedFormType(_value) {
    return 'business'
  }

  function collectBusinessAssetFields(assets = []) {
    const result = []
    const used = new Set()
    ;(Array.isArray(assets) ? assets : []).forEach((asset) => {
      const fields = Array.isArray(asset?.fieldCatalog)
        ? asset.fieldCatalog
        : normalizeBusinessFieldCatalog(asset?.fields || [])
      fields.forEach((field) => {
        const code = routeQueryText(field?.field || field?.fieldCode || field?.code || field?.name)
        const scope = routeQueryText(field?.scope || (field?.childKey ? 'child' : 'main')).toLowerCase()
        const childKey = routeQueryText(field?.childKey || field?.relationKey)
        const childField = routeQueryText(field?.childField || (scope === 'child' ? code : ''))
        const permissionKey = scope === 'child'
          ? (childKey && childField ? `child:${childKey}:${childField}` : '')
          : (code ? `main:${code}` : '')
        if (!permissionKey || used.has(permissionKey))
          return
        used.add(permissionKey)
        result.push({
          field: scope === 'child' ? childField : code,
          fieldCode: scope === 'child' ? childField : code,
          label: field?.label || field?.fieldLabel || field?.fieldName || field?.title || code,
          componentType: field?.componentType || field?.type || field?.fieldType || '',
          dataType: field?.dataType || '',
          required: field?.required === true,
          readonly: field?.readonly === true || field?.systemField === true || field?.writable === false,
          scope: scope === 'child' ? 'child' : 'main',
          ...(scope === 'child'
            ? {
                childKey,
                childField,
                childLabel: field?.childLabel || field?.relationName || childKey,
                relationName: field?.relationName || field?.childLabel || '',
              }
            : {}),
        })
      })
    })
    return result
  }

  function normalizeBusinessFieldCatalog(fields = []) {
    return (Array.isArray(fields) ? fields : [])
      .map((field) => {
        const code = routeQueryText(field?.field || field?.fieldCode || field?.code || field?.name)
        if (!code)
          return null
        return {
          ...field,
          field: code,
          label: field?.label || field?.fieldLabel || field?.fieldName || field?.title || code,
          componentType: field?.componentType || field?.type || field?.fieldType || '',
          dataType: field?.dataType || '',
          required: field?.required === true,
          readonly: field?.readonly === true || field?.systemField === true || field?.writable === false,
        }
      })
      .filter(Boolean)
  }

  function resolveLocalFormFieldCatalog() {
    return buildLocalFormFieldCatalog(formSchema.value)
  }

  function routeQueryText(value) {
    return String(Array.isArray(value) ? value[0] || '' : value || '').trim()
  }

  function goBackToBusinessApp() {
    if (props.embedded) {
      emit('close')
      return
    }
    if (businessEntryRoute.value) {
      router.push(normalizeBusinessEntryRoute(businessEntryRoute.value))
      return
    }
    router.push({
      path: `/app-center/object/${businessObjectCode.value}/designer`,
      query: {
        panel: 'flow-app',
        codeApp: effectiveCodeApp.value ? '1' : undefined,
      },
    })
  }

  function normalizeBusinessEntryRoute(value) {
    const route = decodeHtmlEntities(value)
    if (!route)
      return route
    const [path, query = ''] = route.split('?')
    const params = new URLSearchParams(query)
    if (!params.get('panel'))
      params.set('panel', 'flow-app')
    if (effectiveCodeApp.value && !params.get('codeApp'))
      params.set('codeApp', '1')
    return `${path}?${params.toString()}`
  }

  function decodeHtmlEntities(value) {
    return routeQueryText(value)
      .replace(/&amp;/g, '&')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .replace(/&quot;/g, '"')
      .replace(/&#39;/g, '\'')
  }

  function normalizeDesignerType(value) {
    return value === 'business' ? 'business' : 'approval'
  }

  function normalizeAutoApprovalMode(value) {
    return ['firstOnly', 'consecutive', 'none'].includes(value) ? value : 'none'
  }

  function normalizeRejectStrategy(value) {
    const text = String(value || '').trim().toUpperCase()
    if (!text)
      return 'MANUAL'
    if (text === 'TO_END' || text === 'END' || text === 'TERMINATE')
      return 'TO_END'
    if (text === 'MANUAL' || text === 'NONE' || text === 'OFF')
      return 'MANUAL'
    if (text === 'TO_INITIATOR_MODIFY' || text === 'TO_START' || text === 'MODIFY')
      return 'TO_INITIATOR_MODIFY'
    return 'MANUAL'
  }

  function parseBooleanWithDefault(value, fallback) {
    if (value == null || value === '')
      return fallback
    const normalized = String(value).trim().toLowerCase()
    if (['true', '1', 'y', 'yes'].includes(normalized))
      return true
    if (['false', '0', 'n', 'no'].includes(normalized))
      return false
    return fallback
  }

  function readFlowableAttr(el, name) {
    if (!el)
      return null
    const nsValue = el.getAttributeNS?.('http://flowable.org/bpmn', name)
    if (nsValue != null && nsValue !== '')
      return nsValue
    return el.getAttribute?.(`flowable:${name}`) || el.getAttribute?.(name) || null
  }

  function extractProcessConfigFromXml(xml) {
    if (!xml)
      return { allowSubmitterWithdraw: true, autoApprovalMode: 'none', rejectStrategy: 'MANUAL' }
    try {
      const doc = new DOMParser().parseFromString(xml, 'application/xml')
      if (doc.querySelector('parsererror'))
        return { allowSubmitterWithdraw: true, autoApprovalMode: 'none', rejectStrategy: 'MANUAL' }
      const processEl = findElementByLocalName(doc, 'process')
      return {
        allowSubmitterWithdraw: parseBooleanWithDefault(
          readFlowableAttr(processEl, 'allowSubmitterWithdraw'),
          true,
        ),
        autoApprovalMode: normalizeAutoApprovalMode(readFlowableAttr(processEl, 'autoApprovalMode')),
        rejectStrategy: normalizeRejectStrategy(readFlowableAttr(processEl, 'rejectStrategy')),
      }
    }
    catch {
      return { allowSubmitterWithdraw: true, autoApprovalMode: 'none', rejectStrategy: 'MANUAL' }
    }
  }

  function applyProcessConfigFromXml(xml) {
    const config = extractProcessConfigFromXml(xml)
    modelInfo.allowSubmitterWithdraw = config.allowSubmitterWithdraw
    modelInfo.autoApprovalMode = config.autoApprovalMode
    modelInfo.rejectStrategy = config.rejectStrategy
  }

  function applyProcessConfigToXml(xml) {
    if (!xml)
      return ''
    try {
      const doc = new DOMParser().parseFromString(xml, 'application/xml')
      if (doc.querySelector('parsererror'))
        return xml

      const processEl = findElementByLocalName(doc, 'process')
      if (!processEl)
        return xml

      const definitionsEl = doc.documentElement
      if (definitionsEl && !definitionsEl.getAttribute('xmlns:flowable'))
        definitionsEl.setAttribute('xmlns:flowable', 'http://flowable.org/bpmn')
      // 流程定义名随模型名称保存；新模型模板里的「新流程」不能部署成 Flowable 定义名
      if (String(modelInfo.modelName || '').trim())
        processEl.setAttribute('name', String(modelInfo.modelName).trim())

      processEl.setAttributeNS(
        'http://flowable.org/bpmn',
        'flowable:allowSubmitterWithdraw',
        String(processConfig.value.allowSubmitterWithdraw !== false),
      )
      processEl.setAttributeNS(
        'http://flowable.org/bpmn',
        'flowable:autoApprovalMode',
        normalizeAutoApprovalMode(processConfig.value.autoApprovalMode),
      )
      processEl.setAttributeNS(
        'http://flowable.org/bpmn',
        'flowable:rejectStrategy',
        normalizeRejectStrategy(processConfig.value.rejectStrategy),
      )
      return new XMLSerializer().serializeToString(doc)
    }
    catch {
      return xml
    }
  }

  async function getXmlForSave() {
    const currentXml = modelerRef.value?.getXML
      ? await modelerRef.value.getXML()
      : bpmnXml.value
    const xml = applyProcessConfigToXml(currentXml)
    if (xml)
      bpmnXml.value = xml
    return xml
  }

  async function loadModel(id) {
    syncingModel.value = true
    try {
      const res = await flowApi.getModelDetail(id)
      if (res.code === 200 && res.data) {
        bpmnXml.value = ''
        formSchema.value = []
        formFieldCatalog.value = []
        dockedElement.value = null
        resolvedBusinessBinding.value = null
        manualBusinessApplicationId.value = ''
        manualBusinessApplicationName.value = ''
        manualBusinessObjectCode.value = ''
        manualBusinessObjectName.value = ''
        businessApplicationOptions.value = []
        businessObjectOptions.value = []
        businessObjectOptionsApplicationId.value = ''
        Object.assign(modelInfo, res.data)
        modelInfo.allowSubmitterWithdraw = parseBooleanWithDefault(res.data.allowSubmitterWithdraw, true)
        modelInfo.allowMultiReturn = parseBooleanWithDefault(res.data.allowMultiReturn, false)
        modelInfo.todoDetailUrlTemplate = routeQueryText(res.data.todoDetailUrlTemplate) || DEFAULT_TODO_DETAIL_URL_TEMPLATE
        resetNotifyMatrix(res.data.notifyConfig || null)
        modelInfo.designerType = normalizeDesignerType(res.data.designerType)
        modelInfo.category = resolveFlowCategoryValue(res.data.category, categoryTreeOptions.value)
        bpmnXml.value = res.data.bpmnXml || ''
        applyProcessConfigFromXml(bpmnXml.value)
        await resolveBusinessBindingForModel()
        if (
          businessContextActive.value
          && modelInfo.formType !== 'none'
          && modelInfo.formType !== 'external'
          && !isAppManagedFormType(modelInfo.formType)
        ) {
          modelInfo.formType = 'business'
        }

        if (res.data.formType === 'business' || modelInfo.formType === 'business') {
          formSchema.value = []
        }
        else if (res.data.formJson) {
          try {
            const parsedFormSchema = JSON.parse(res.data.formJson)
            formSchema.value = Array.isArray(parsedFormSchema) ? parsedFormSchema : []
          }
          catch (e) {
            console.error('解析表单配置失败:', e)
            formSchema.value = []
          }
        }
        else {
          formSchema.value = []
        }
        if (appManagedFormTypeActive.value && !explicitBusinessApplicationId.value)
          await loadBusinessApplicationOptions()
        if (businessObjectPickerVisible.value && effectiveApplicationId.value)
          await loadBusinessObjectOptions()
        await refreshFormFieldCatalog()
      }
    }
    catch (error) {
      console.error('加载模型失败:', error)
    }
    finally {
      syncingModel.value = false
    }
  }

  function handleOpenVersionHistory() {
    if (!modelInfo.id) {
      window.$message?.warning('请先保存模型后查看更改记录')
      return
    }
    showVersionHistory.value = true
  }

  async function handleVersionHistoryRefresh() {
    if (!modelInfo.id)
      return

    await loadModel(modelInfo.id)
    await nextTick()
    await modelerRef.value?.setXML?.(bpmnXml.value || '')
    hasChanges.value = false
  }

  function handleFormTypeChange(value) {
    if (value !== 'dynamic') {
      formSchema.value = []
      modelInfo.formId = null
      modelInfo.formJson = ''
      formFieldCatalog.value = []
    }
    if (value !== 'external') {
      modelInfo.formUrl = ''
    }
    if (value === 'dynamic' || value === 'external')
      refreshFormFieldCatalog()
  }

  async function handleFormSelect(formId) {
    if (!formId) {
      formSchema.value = []
      modelInfo.formJson = ''
      formFieldCatalog.value = resolveLocalFormFieldCatalog()
      return
    }

    try {
      const res = await flowApi.getFormById(formId)
      if (res.code === 200 && res.data) {
        if (res.data.formSchema) {
          formSchema.value = JSON.parse(res.data.formSchema)
          modelInfo.formJson = res.data.formSchema
        }
        await refreshFormFieldCatalog(res.data)
      }
    }
    catch (error) {
      console.error('加载表单失败:', error)
      message.error('加载表单失败')
    }
  }

  function handleSaveFormSchema(schema) {
    formSchema.value = schema
    modelInfo.formJson = JSON.stringify(schema)
    showFormDesigner.value = false
    hasChanges.value = true
    formFieldCatalog.value = resolveLocalFormFieldCatalog()
    message.success('表单设计已保存')
  }

  function toggleAiPanel() {
    if (isAiPanelActive.value) {
      workspaceMode.value = 'design'
      return
    }
    openSettingsPanel('ai')
  }

  async function handleAiSend() {
    const prompt = aiPrompt.value.trim()
    if (!prompt) {
      message.warning('请输入流程需求')
      return
    }
    if (aiSending.value) {
      return
    }
    if (!aiProviderId.value) {
      message.warning('请选择AI供应商')
      return
    }
    if (!aiModelId.value) {
      message.warning('请选择对话模型')
      return
    }

    try {
      aiSending.value = true
      aiDraft.value = null
      aiRawContent.value = ''
      aiReasoningContent.value = ''
      aiIsReasoningPhase.value = false
      aiReasoningStartTime.value = null
      aiReasoningEndTime.value = null
      aiCurrentStage.value = 'analyzing'

      if (!aiSessionId.value) {
        aiSessionId.value = `flow_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
      }
      aiMessages.value.push({ role: 'user', content: prompt })
      aiMessages.value.push({
        role: 'assistant',
        content: '正在分析流程需求...',
        streaming: true,
        isReasoning: false,
        reasoning: '',
        reasoningTime: null,
      })
      aiPrompt.value = ''
      scrollAiToBottom()

      const currentXml = await modelerRef.value?.getXML(true)

      aiAbortController.value = streamFlowGenerate(
        {
          sessionId: aiSessionId.value,
          description: prompt,
          providerId: aiProviderId.value,
          modelId: aiModelId.value,
          flowModelId: modelInfo.id || modelInfo.modelKey || '',
          aiModelName: currentModelLabel.value,
          modelKey: modelInfo.modelKey || `process_${Date.now()}`,
          modelName: modelInfo.modelName || '新流程',
          category: modelInfo.category || '',
          flowType: modelInfo.flowType || '',
          formType: modelInfo.formType || 'dynamic',
          currentBpmnXml: currentXml || bpmnXml.value || '',
          currentFormJson: modelInfo.formJson || '',
          temperature: 0.2,
          maxTokens: 12000,
        },
        handleFlowSSEChunk,
        handleFlowSSEComplete,
        handleFlowSSEError,
      )
    }
    catch (error) {
      console.error('AI生成流程失败:', error)
      message.error(error.message || 'AI生成流程失败')
      aiSending.value = false
      aiCurrentStage.value = ''
      updateLastAiAssistantMessage('AI生成流程失败', { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime: null })
    }
  }

  function handleAbortAi() {
    if (aiAbortController.value) {
      aiAbortController.value.abort()
      aiAbortController.value = null
    }
    aiSending.value = false
    aiCurrentStage.value = ''
    updateLastAiAssistantMessage('已停止生成', { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime: null })
  }

  function handleFlowSSEChunk({ event, data }) {
    if (event === 'progress') {
      aiCurrentStage.value = 'generating'
      updateLastAiAssistantMessage(data.message || '正在生成流程配置...', { reasoning: aiReasoningContent.value, isReasoning: aiIsReasoningPhase.value, reasoningTime: null })
      scrollAiToBottom()
      return
    }

    if (event !== 'chunk') {
      return
    }

    const chunkContent = data.content || ''
    if (!chunkContent) {
      return
    }

    if (chunkContent.includes('==================== 思考过程 ====================')) {
      aiIsReasoningPhase.value = true
      aiCurrentStage.value = 'reasoning'
      aiReasoningStartTime.value = Date.now()
      aiReasoningEndTime.value = null
      aiReasoningContent.value = ''
      const afterDelimiter = chunkContent.split('==================== 思考过程 ====================')[1] || ''
      aiReasoningContent.value += afterDelimiter.replace('\n', '')
      updateLastAiAssistantMessage('', { reasoning: aiReasoningContent.value, isReasoning: true, reasoningTime: null })
    }
    else if (chunkContent.includes('==================== 完整回复 ====================')) {
      aiIsReasoningPhase.value = false
      aiCurrentStage.value = 'generating'
      aiReasoningEndTime.value = Date.now()
      const reasoningTime = aiReasoningStartTime.value ? Math.round((aiReasoningEndTime.value - aiReasoningStartTime.value) / 1000) : null
      const afterDelimiter = chunkContent.split('==================== 完整回复 ====================')[1] || ''
      aiRawContent.value += afterDelimiter.replace('\n', '')
      updateLastAiAssistantMessage(aiRawContent.value || '正在生成 BPMN XML...', { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime })
    }
    else if (aiIsReasoningPhase.value) {
      aiReasoningContent.value += chunkContent
      updateLastAiAssistantMessage('', { reasoning: aiReasoningContent.value, isReasoning: true, reasoningTime: null })
    }
    else {
      aiRawContent.value += chunkContent
      updateLastAiAssistantMessage(aiRawContent.value || '正在生成 BPMN XML...', { reasoning: aiReasoningContent.value, isReasoning: false })
    }
    scrollAiToBottom()
  }

  function handleFlowSSEComplete(data) {
    aiSending.value = false
    aiAbortController.value = null
    aiCurrentStage.value = 'complete'

    if (data?.sessionId) {
      aiSessionId.value = data.sessionId
    }

    const reasoningTime = aiReasoningStartTime.value ? Math.round((Date.now() - aiReasoningStartTime.value) / 1000) : null

    const parsed = parseAiFlowResponse(aiRawContent.value)
    if (parsed?.bpmnXml) {
      aiDraft.value = normalizeAiFlowDraft(parsed)
      updateLastAiAssistantMessage(aiDraft.value.summary || aiDraft.value.description || '已生成流程配置，可一键加载到画布。', { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime })
    }
    else {
      updateLastAiAssistantMessage(aiRawContent.value || 'AI未返回可解析的流程配置', { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime })
      message.warning('AI响应未解析到 BPMN XML，请继续追问或调整需求')
    }
    scrollAiToBottom()
  }

  function handleFlowSSEError(errorMessage) {
    aiSending.value = false
    aiAbortController.value = null
    aiCurrentStage.value = ''
    const reasoningTime = aiReasoningStartTime.value ? Math.round((Date.now() - aiReasoningStartTime.value) / 1000) : null
    updateLastAiAssistantMessage(`生成失败: ${errorMessage}`, { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime })
    message.error(errorMessage || 'AI生成流程失败')
  }

  function updateLastAiAssistantMessage(content, reasoningData = null) {
    const last = aiMessages.value[aiMessages.value.length - 1]
    if (last && last.role === 'assistant') {
      last.content = content
      last.streaming = aiSending.value
      if (reasoningData) {
        last.reasoning = reasoningData.reasoning
        last.isReasoning = reasoningData.isReasoning
        if (reasoningData.reasoningTime !== undefined && reasoningData.reasoningTime !== null) {
          last.reasoningTime = reasoningData.reasoningTime
        }
        if (reasoningData.isReasoning) {
          expandedReasonings.value[aiMessages.value.length - 1] = true
        }
      }
    }
    else {
      aiMessages.value.push({
        role: 'assistant',
        content,
        streaming: aiSending.value,
        isReasoning: reasoningData?.isReasoning || false,
        reasoning: reasoningData?.reasoning || '',
        reasoningTime: reasoningData?.reasoningTime || null,
      })
      if (reasoningData?.isReasoning) {
        expandedReasonings.value[aiMessages.value.length - 1] = true
      }
    }
  }

  function toggleReasoning(idx) {
    expandedReasonings.value[idx] = !expandedReasonings.value[idx]
    scrollAiToBottom()
  }

  function scrollAiToBottom() {
    nextTick(() => {
      requestAnimationFrame(() => {
        scrollActiveReasoningToBottom()
        if (aiMessageEndRef.value) {
          aiMessageEndRef.value.scrollIntoView({ block: 'end' })
        }
        if (aiMessageListRef.value) {
          aiMessageListRef.value.scrollTop = aiMessageListRef.value.scrollHeight
        }
        requestAnimationFrame(() => {
          scrollActiveReasoningToBottom()
          if (aiMessageListRef.value) {
            aiMessageListRef.value.scrollTop = aiMessageListRef.value.scrollHeight
          }
        })
      })
    })
  }

  function setReasoningContentRef(el, index) {
    if (el) {
      reasoningContentRefs.value[index] = el
    }
    else {
      delete reasoningContentRefs.value[index]
    }
  }

  function scrollActiveReasoningToBottom() {
    const activeIndex = aiMessages.value.findLastIndex(msg => msg.role === 'assistant' && msg.isReasoning)
    const reasoningEl = reasoningContentRefs.value[activeIndex]
    if (reasoningEl) {
      reasoningEl.scrollTop = reasoningEl.scrollHeight
    }
  }

  watch(() => aiMessages.value.length, () => scrollAiToBottom())
  watch(() => [aiRawContent.value, aiReasoningContent.value, aiDraft.value], () => scrollAiToBottom(), { flush: 'post' })
  watch(aiSending, (val) => {
    if (val)
      scrollAiToBottom()
  })

  async function handleApplyAiDraft() {
    if (!aiDraft.value?.bpmnXml) {
      message.warning('暂无可加载的流程配置')
      return
    }

    try {
      if (aiDraft.value.modelName) {
        modelInfo.modelName = aiDraft.value.modelName
      }
      if (!modelInfo.id && aiDraft.value.modelKey) {
        modelInfo.modelKey = aiDraft.value.modelKey
      }
      if (aiDraft.value.category) {
        modelInfo.category = resolveFlowCategoryValue(aiDraft.value.category, categoryTreeOptions.value)
      }
      if (aiDraft.value.flowType) {
        modelInfo.flowType = aiDraft.value.flowType
      }
      if (aiDraft.value.description) {
        modelInfo.description = aiDraft.value.description
      }
      if (aiDraft.value.formType) {
        modelInfo.formType = aiDraft.value.formType
      }
      if (aiDraft.value.formJson) {
        modelInfo.formJson = typeof aiDraft.value.formJson === 'string'
          ? aiDraft.value.formJson
          : JSON.stringify(aiDraft.value.formJson)
        try {
          formSchema.value = JSON.parse(modelInfo.formJson)
        }
        catch {
          formSchema.value = []
        }
      }

      const safeModelKey = syncSafeModelKey(modelInfo.modelKey || aiDraft.value.modelKey)
      const xmlToImport = normalizeAiBpmnXml(aiDraft.value.bpmnXml, safeModelKey)
      const displayIssue = getBpmnDisplayIssue(xmlToImport)
      if (displayIssue) {
        throw new Error(displayIssue)
      }

      aiDraft.value = {
        ...aiDraft.value,
        bpmnXml: xmlToImport,
      }
      applyProcessConfigFromXml(xmlToImport)
      bpmnXml.value = xmlToImport
      await modelerRef.value?.setXML(xmlToImport)
      hasChanges.value = true
      message.success('AI流程配置已加载到画布')
    }
    catch (error) {
      console.error('加载AI流程配置失败:', error)
      message.error(`加载失败: ${error.message}`)
    }
  }

  function handleNewAiSession() {
    if (aiAbortController.value) {
      aiAbortController.value.abort()
      aiAbortController.value = null
    }
    aiSending.value = false
    aiCurrentStage.value = ''
    aiSessionId.value = ''
    aiPrompt.value = ''
    aiMessages.value = []
    aiDraft.value = null
    aiRawContent.value = ''
    aiReasoningContent.value = ''
    aiIsReasoningPhase.value = false
    aiReasoningStartTime.value = null
    aiReasoningEndTime.value = null
    expandedReasonings.value = {}
  }

  function handlePreviewAiXml() {
    showAiXmlPreview.value = true
  }

  function normalizeAiFlowDraft(draft) {
    const modelKey = normalizeBpmnId(draft.modelKey || modelInfo.modelKey || `process_${Date.now()}`)
    return {
      ...draft,
      modelKey,
      modelName: draft.modelName || modelInfo.modelName || 'AI生成流程',
      bpmnXml: normalizeAiBpmnXml(draft.bpmnXml, modelKey),
    }
  }

  function normalizeAiBpmnXml(xml, modelKey) {
    const safeModelKey = normalizeBpmnId(modelKey)
    const extractedXml = extractBpmnXml(xml) || (xml || '').trim()
    const bpmnXml = ensureProcessId(extractedXml, safeModelKey)
    return repairBpmnXml(bpmnXml, safeModelKey)
  }

  function parseAiFlowResponse(content) {
    if (!content)
      return null

    const cleaned = stripCodeFence(content)
    const jsonCandidates = [
      cleaned,
      extractJsonObject(cleaned),
    ].filter(Boolean)

    for (const candidate of jsonCandidates) {
      try {
        return JSON.parse(candidate)
      }
      catch {
        // try next candidate
      }
    }

    const xml = extractBpmnXml(cleaned)
    if (xml) {
      return {
        bpmnXml: xml,
        summary: '已从AI响应中提取 BPMN XML',
      }
    }
    return null
  }

  function stripCodeFence(content) {
    let text = content.trim()
    if (text.startsWith('```json')) {
      text = text.slice(7)
    }
    else if (text.startsWith('```xml')) {
      text = text.slice(6)
    }
    else if (text.startsWith('```')) {
      text = text.slice(3)
    }
    if (text.endsWith('```')) {
      text = text.slice(0, -3)
    }
    return text.trim()
  }

  __impl.handleBusinessObjectChange = handleBusinessObjectChange
  __impl.handleBusinessManagedFormTypeChange = handleBusinessManagedFormTypeChange
  __impl.resolveBusinessBindingForModel = resolveBusinessBindingForModel
  __impl.refreshBusinessFormFieldCatalog = refreshBusinessFormFieldCatalog
  __impl.normalizeBusinessFormAssets = normalizeBusinessFormAssets
  __impl.handleBusinessGlobalFormUpdate = handleBusinessGlobalFormUpdate
  __impl.ensureBusinessGlobalFormSelection = ensureBusinessGlobalFormSelection
  __impl.buildBusinessGlobalFormJson = buildBusinessGlobalFormJson
  __impl.parseBusinessGlobalFormRef = parseBusinessGlobalFormRef
  __impl.findBusinessFormAsset = findBusinessFormAsset
  __impl.findBusinessFormAssetInList = findBusinessFormAssetInList
  __impl.normalizeBusinessFormMode = normalizeBusinessFormMode
  __impl.isAppManagedFormType = isAppManagedFormType
  __impl.normalizeAppManagedFormType = normalizeAppManagedFormType
  __impl.collectBusinessAssetFields = collectBusinessAssetFields
  __impl.normalizeBusinessFieldCatalog = normalizeBusinessFieldCatalog
  __impl.resolveLocalFormFieldCatalog = resolveLocalFormFieldCatalog
  __impl.routeQueryText = routeQueryText
  __impl.goBackToBusinessApp = goBackToBusinessApp
  __impl.normalizeBusinessEntryRoute = normalizeBusinessEntryRoute
  __impl.decodeHtmlEntities = decodeHtmlEntities
  __impl.normalizeDesignerType = normalizeDesignerType
  __impl.normalizeAutoApprovalMode = normalizeAutoApprovalMode
  __impl.normalizeRejectStrategy = normalizeRejectStrategy
  __impl.parseBooleanWithDefault = parseBooleanWithDefault
  __impl.readFlowableAttr = readFlowableAttr
  __impl.extractProcessConfigFromXml = extractProcessConfigFromXml
  __impl.applyProcessConfigFromXml = applyProcessConfigFromXml
  __impl.applyProcessConfigToXml = applyProcessConfigToXml
  __impl.getXmlForSave = getXmlForSave
  __impl.loadModel = loadModel
  __impl.handleOpenVersionHistory = handleOpenVersionHistory
  __impl.handleVersionHistoryRefresh = handleVersionHistoryRefresh
  __impl.handleFormTypeChange = handleFormTypeChange
  __impl.handleFormSelect = handleFormSelect
  __impl.handleSaveFormSchema = handleSaveFormSchema
  __impl.toggleAiPanel = toggleAiPanel
  __impl.handleAiSend = handleAiSend
  __impl.handleAbortAi = handleAbortAi
  __impl.handleFlowSSEChunk = handleFlowSSEChunk
  __impl.handleFlowSSEComplete = handleFlowSSEComplete
  __impl.handleFlowSSEError = handleFlowSSEError
  __impl.updateLastAiAssistantMessage = updateLastAiAssistantMessage
  __impl.toggleReasoning = toggleReasoning
  __impl.scrollAiToBottom = scrollAiToBottom
  __impl.setReasoningContentRef = setReasoningContentRef
  __impl.scrollActiveReasoningToBottom = scrollActiveReasoningToBottom
  __impl.handleApplyAiDraft = handleApplyAiDraft
  __impl.handleNewAiSession = handleNewAiSession
  __impl.handlePreviewAiXml = handlePreviewAiXml
  __impl.normalizeAiFlowDraft = normalizeAiFlowDraft
  __impl.normalizeAiBpmnXml = normalizeAiBpmnXml
  __impl.parseAiFlowResponse = parseAiFlowResponse
  __impl.stripCodeFence = stripCodeFence

  return {
    ...deps,
  }
}
