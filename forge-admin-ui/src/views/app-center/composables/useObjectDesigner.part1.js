/** object-designer.[objectCode].vue setup part 1. */
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
export function applyObjectDesignerPart1(props, emit) {
  const __impl = {}
  const mut = {}

  const route = useRoute()
  const router = useRouter()
  const message = useMessage()
  const tabStore = useTabStore()
  const userStore = useUserStore()

  const loading = ref(false)
  const saving = ref(false)
  const publishing = ref(false)
  const dirty = ref(false)
  const designerDraftDirty = ref(false)
  const fieldDraftDirty = ref(false)
  const ready = ref(false)
  const activePanel = ref(resolveInitialPanel())
  const initialFormCanvasView = resolveInitialFormCanvasView()
  const developerMode = ref(false)
  const designer = ref(null)
  const runtimeInfo = ref(null)
  const publishCheckState = ref(null)
  const functionMarketVisible = ref(false)
  const fieldManagerRef = ref(null)
  const tableMappingSummaryRef = ref(null)
  const tableMapping = ref(null)
  const formDesignerRef = ref(null)
  const listDesignerRef = ref(null)
  const relationDesignerRef = ref(null)
  const flowAppConfigRef = ref(null)
  const treeModelRef = ref(null)
  const publishChecklistRef = ref(null)
  const draft = reactive(createEmptyDraft())

  const objectCode = computed(() => props.embedded ? props.embeddedObjectCode : route.params.objectCode)
  const suiteCode = computed(() => (props.embedded ? props.embeddedSuiteCode : route.query.suiteCode) || draft.suiteCode)
  const objectId = computed(() => designer.value?.objectId || draft.objectId)
  const applicationCode = computed(() => resolveApplicationCode())
  const pageTitle = computed(() => `${draft.objectName || objectCode.value || '业务单元'}设计`)
  const canAdvanced = computed(() => {
    return userStore.isAdmin
      || hasPermission(userStore.permissions, 'ai:businessObject:advanced')
      || hasPermission(userStore.apiPermissions, 'ai:businessObject:advanced')
      || hasPermission(userStore.getDataPermission, 'ai:businessObject:advanced')
  })
  const isCodeAppDesigner = computed(() => {
    return draft.designerOptions?.codeApp === true
      || designer.value?.options?.codeApp === true
      || (!objectId.value && (route.query.codeApp === '1' || route.query.appType === 'code'))
  })
  const usesLegacyObjectPanels = computed(() => props.embedded || isCodeAppDesigner.value)
  const effectiveDesignerPanel = computed(() => {
    // 数据关系分区只承载对象关系；树形模型已是一级入口
    if (activePanel.value === 'data-model')
      return 'relations'
    return activePanel.value
  })
  const publishDisabled = computed(() => {
    if (isCodeAppDesigner.value)
      return true
    if (!objectId.value)
      return true
    return publishCheckState.value?.publishable === false
  })
  // ObjectActionEditor 跨对象字段通过 businessObjectList 兜底加载，无需本地维护对象列表
  const workspaceObjects = []
  const designerNavPanels = computed(() => {
    if (props.embedded && props.embeddedNavPanels.length)
      return props.embeddedNavPanels
    return isCodeAppDesigner.value ? ['form', 'list', 'flow-app'] : []
  })
  const fieldOptions = computed(() => {
    const modelFields = draft.modelSchema?.fields || []
    const source = modelFields.length ? modelFields : draft.fields.map(toPageField)
    return source
      .filter(field => field.field && !isInactiveField(field))
      .map(field => ({
        label: `${field.label || field.field}（${field.field}）`,
        value: field.field,
      }))
  })
  const runtimeFormOptions = computed(() => {
    const schema = normalizeMultiFormDesignerSchema(draft.formDesignerSchema || {})
    return (schema.forms || [])
      .filter(form => form?.formKey)
      .map(form => ({
        label: `${form.formName || form.formKey}${form.formKey === schema.defaultFormKey ? '（默认）' : ''}`,
        value: form.formKey,
      }))
  })

  function resolveInitialPanel() {
    const panel = props.embedded ? props.initialPanel : route.query.panel
    const modelTab = props.embedded ? '' : route.query.modelTab
    // 兼容旧深链：数据关系 > 树形模型 子 Tab → 一级「树形模型」
    if (!props.embedded
      && String(panel || '') === 'data-model'
      && resolveDataModelTab(modelTab) === 'tree-model') {
      return 'tree-model'
    }
    const normalized = normalizePanel(panel) || 'fields'
    const codeAppRoute = route.query.codeApp === '1' || route.query.appType === 'code'
    return props.embedded || codeAppRoute ? normalized : resolveStandaloneObjectDesignerSection(normalized)
  }

  function resolveApplicationCode() {
    const directCode = route.query.applicationCode
      || route.query.appCode
      || designer.value?.applicationCode
      || draft.designerOptions?.applicationCode
    if (directCode)
      return String(directCode)
    const returnTo = String(route.query.returnTo || '')
    return decodeURIComponent(returnTo.match(/\/app-center\/application\/([^/?]+)/)?.[1] || '')
  }

  function openProcessWorkspace() {
    if (!applicationCode.value) {
      router.push('/app-center')
      return
    }
    router.push({
      name: 'BusinessApplicationWorkspace',
      params: { applicationCode: applicationCode.value },
      query: { section: 'automation' },
    })
  }

  function normalizePanel(panel) {
    if (panel === 'permission')
      return 'tree-model'
    if (['trigger', 'automation-trigger', 'triggers'].includes(panel))
      return 'fields'
    if (['flow', 'document', 'automation', 'flow-app'].includes(panel))
      return 'flow-app'
    return panel === 'detail' ? 'form' : panel
  }

  function resolveInitialFormCanvasView() {
    const legacyView = props.embedded
      ? props.initialDetailTab
      : route.query.panel === 'detail' ? 'detail' : route.query.detailTab
    if (legacyView === 'detail')
      return 'detail'
    if (legacyView === 'sections')
      return 'sections'
    return 'layout'
  }

  onMounted(() => {
    loadDesigner()
    window.addEventListener('beforeunload', handleBeforeUnload)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('beforeunload', handleBeforeUnload)
    if (!props.embedded)
      tabStore.setTabDirty(route.fullPath, false)
  })

  onBeforeRouteLeave(() => {
    if (tabStore.consumeDirtyNavigation(route.fullPath))
      return true
    if (!dirty.value)
      return true
    return confirmLeave()
  })

  watch(dirty, (value) => {
    if (props.embedded)
      emit('dirtyChange', value)
    else
      tabStore.setTabDirty(route.fullPath, value, '当前业务对象设计存在未保存的更改')
  }, { immediate: true })

  watch(pageTitle, (title) => {
    if (props.embedded)
      return
    if (!title)
      return
    route.meta.title = title
    document.title = `${title} | ${getDefaultPageTitle()}`
    tabStore.updateTabTitle(route.fullPath, title)
  }, { immediate: true })

  watch(activePanel, (panel) => {
    if (props.embedded)
      return
    router.replace({
      path: route.path,
      query: {
        ...route.query,
        panel,
      },
    })
  })

  watch(canAdvanced, (value) => {
    if (!value && activePanel.value === 'advanced')
      activePanel.value = 'form'
  }, { immediate: true })

  async function loadDesigner() {
    loading.value = true
    ready.value = false
    try {
      if (isCodeAppRoute()) {
        await applyCodeAppDesignerDraft()
        await nextTick()
        dirty.value = false
        designerDraftDirty.value = false
        ready.value = true
        return
      }
      const object = await resolveBusinessObject()
      if (isCodeAppBusinessObject(object)) {
        await applyCodeAppDesignerDraft(object)
        await nextTick()
        dirty.value = false
        designerDraftDirty.value = false
        ready.value = true
        return
      }
      if (!object?.id) {
        if (shouldOpenCodeAppDesigner()) {
          await applyCodeAppDesignerDraft()
          await nextTick()
          dirty.value = false
          designerDraftDirty.value = false
          ready.value = true
          return
        }
        message.warning('未找到业务单元')
        return
      }
      const res = await businessObjectDesigner(object.id)
      designer.value = res.data || null
      Object.assign(draft, createDraftFromDesigner(designer.value))
      if (object.id)
        await loadRuntimeInfo(object.id)
      await nextTick()
      dirty.value = false
      designerDraftDirty.value = false
      ready.value = true
    }
    finally {
      loading.value = false
    }
  }

  async function resolveBusinessObject() {
    const queryObjectId = props.embedded ? props.embeddedObjectId : route.query.objectId
    const identity = pickBusinessObjectIdentity({ queryObjectId })
    if (identity?.id)
      return identity
    return findBusinessObjectByCode()
  }

  async function findBusinessObjectByCode() {
    if (!objectCode.value)
      return null
    const res = await businessObjectList({
      suiteCode: suiteCode.value,
      objectCode: objectCode.value,
    })
    return (res.data || [])[0] || null
  }

  function isCodeAppRoute() {
    if (props.embedded)
      return false
    return route.query.codeApp === '1' || route.query.appType === 'code'
  }

  function shouldOpenCodeAppDesigner() {
    return ['form', 'list', 'flow-app'].includes(activePanel.value)
      && !props.embedded
      && !!objectCode.value
      && (isCodeAppRoute() || ['form', 'list', 'detail', 'actions', 'flow-app'].includes(String(route.query.panel || '')))
  }

  function isCodeAppBusinessObject(object = {}) {
    return hasCodeAppFlag(object?.options) || hasCodeAppFlag(object?.designerOptions)
  }

  function hasCodeAppFlag(value) {
    if (!value)
      return false
    if (typeof value === 'object')
      return value.codeApp === true
    if (typeof value !== 'string')
      return false
    const compactValue = value.replace(/\s/g, '')
    if (compactValue.includes('"codeApp":true'))
      return true
    try {
      return JSON.parse(value)?.codeApp === true
    }
    catch {
      return false
    }
  }

  async function applyCodeAppDesignerDraft(sourceObject = null) {
    const code = String(objectCode.value || '').trim()
    const res = await businessFlowAppConfig(code)
    const config = res.data || {}
    const name = String(sourceObject?.objectName || config.objectName || route.query.name || route.query.objectName || code || '代码应用')
    const metadata = resolveCodeAppMetadata(config)
    const providerCatalogAssets = collectCodeAppProviderCatalogAssets(config.formAssets?.providerCatalog || [])
    const providerAssets = hydrateCodeAppProviderAssets(
      normalizeCodeAppAssets(config.formAssets?.formAssets || []),
      providerCatalogAssets,
    )
    const metadataAssets = normalizeCodeAppAssets(metadata.formAssets || [])
    const baseProviderAssets = providerAssets.length ? providerAssets : providerCatalogAssets
    const formAssets = mergeCodeAppAssets(baseProviderAssets, metadataAssets, code, name)
    const providerFields = collectCodeAppAssetFields(baseProviderAssets.length ? baseProviderAssets : formAssets)
    const configuredFields = metadata.fields?.length ? metadata.fields : collectCodeAppAssetFields(metadataAssets)
    const fields = mergeCodeAppFields(providerFields, configuredFields)
    const modelSchema = createCodeAppModelSchema(code, name, fields)
    const pageSchema = createCodeAppPageSchema(metadata.pageSchema, modelSchema)
    const viewSchema = createCodeAppViewSchema(metadata.viewSchema, fields)
    const formDesignerSchema = createCodeAppFormDesignerSchema(metadata, formAssets, fields, code, name)
    const virtualDesigner = {
      objectId: null,
      objectCode: code,
      objectName: sourceObject?.objectName || config.objectName || name,
      suiteCode: sourceObject?.suiteCode || config.suiteCode || 'CODE_APP',
      suiteName: sourceObject?.suiteName || config.suiteName || '代码应用',
      designStatus: 'PUBLISHED',
      publishStatus: 'PUBLISHED',
      options: { codeApp: true },
      designerOptions: {
        codeApp: true,
        codeAppAssets: formAssets,
      },
      fields,
      modelSchema,
      pageSchema,
      formDesignerSchema,
      viewSchema,
      documentConfig: config.documentConfig || null,
    }
    designer.value = virtualDesigner
    runtimeInfo.value = null
    publishCheckState.value = null
    if (!['form', 'list', 'flow-app'].includes(activePanel.value))
      activePanel.value = 'form'
    Object.assign(draft, createDraftFromDesigner(virtualDesigner))
  }

  async function loadRuntimeInfo(id = objectId.value) {
    if (!id)
      return
    const res = await businessObjectRuntimeInfo(id)
    runtimeInfo.value = res.data || null
  }

  async function handleSave() {
    if (saving.value)
      return false
    saving.value = true
    await waitForSaveLoadingPaint()
    try {
      const currentPanel = effectiveDesignerPanel.value
      if (!await confirmSeedConfigurationTakeover())
        return false
      if (currentPanel === 'fields') {
        const saved = await fieldManagerRef.value?.saveSelectedField?.()
        if (saved === true)
          fieldDraftDirty.value = false
        return saved === true
      }
      if (isCodeAppDesigner.value && currentPanel === 'form') {
        await syncActiveFormDraft()
        await saveCodeAppDesignerDraft(true)
        return true
      }
      if (isCodeAppDesigner.value && currentPanel === 'list') {
        await syncActiveListDraft()
        await saveCodeAppDesignerDraft(true)
        return true
      }
      if (currentPanel === 'form') {
        await formDesignerRef.value?.saveLayout?.()
        await loadDesigner()
        return true
      }
      if (currentPanel === 'list') {
        await listDesignerRef.value?.saveLayout?.()
        await loadDesigner()
        return true
      }
      if (currentPanel === 'relations') {
        await relationDesignerRef.value?.saveRelations?.()
        await loadDesigner()
        return true
      }
      if (currentPanel === 'flow-app') {
        const codeAppMetadata = isCodeAppDesigner.value && designerDraftDirty.value
          ? buildCodeAppMetadataPayload()
          : null
        if (!isCodeAppDesigner.value)
          await persistPendingDesignerDraft()
        await flowAppConfigRef.value?.saveConfig?.({ codeAppMetadata })
        await loadDesigner()
        return true
      }
      if (currentPanel === 'tree-model') {
        treeModelRef.value?.saveConfig?.()
      }
      await saveDesignerDraft(true, { manageSaving: false })
      return true
    }
    finally {
      saving.value = false
    }
  }

  async function confirmSeedConfigurationTakeover() {
    if (!requiresSeedTakeoverConfirmation(designer.value || {}))
      return true
    const summary = buildSeedTakeoverSummary(designer.value || {}, draft)
    const confirmed = await requestSeedTakeoverConfirmation(summary)
    if (!confirmed)
      return false

    draft.designerOptions = markSeedTakeoverAccepted(draft.designerOptions || {})
    if (effectiveDesignerPanel.value === 'form')
      await syncActiveFormDraft()
    if (effectiveDesignerPanel.value === 'list')
      await syncActiveListDraft()
    await saveBusinessObjectDesigner(objectId.value, buildDesignerPayload())
    designer.value = {
      ...(designer.value || {}),
      designerOptions: cloneSchema(draft.designerOptions),
    }
    return true
  }

  function requestSeedTakeoverConfirmation(summary) {
    const content = `此配置由系统种子初始化。确认后，后续修改由设计器草稿接管；已发布版本保持不变，只有正式发布才会生成新快照。\n本次变化：${summary}`
    if (!window.$dialog)
      return Promise.resolve(false)
    return new Promise((resolve) => {
      window.$dialog.warning({
        title: '接管内置配置',
        content,
        positiveText: '确认接管并保存',
        negativeText: '取消',
        onPositiveClick: () => resolve(true),
        onNegativeClick: () => resolve(false),
        onClose: () => resolve(false),
      })
    })
  }

  async function saveDesignerDraft(showMessage = true, options = {}) {
    if (!objectId.value)
      return
    const reload = options.reload !== false
    const manageSaving = options.manageSaving !== false
    if (manageSaving)
      saving.value = true
    try {
      await saveBusinessObjectDesigner(objectId.value, buildDesignerPayload())
      dirty.value = false
      designerDraftDirty.value = false
      if (showMessage)
        message.success('设计器已保存')
      if (reload)
        await loadDesigner()
      emit('saved')
    }
    finally {
      if (manageSaving)
        saving.value = false
    }
  }

  async function waitForSaveLoadingPaint() {
    await nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))
  }

  async function handlePanelSwitch(panel) {
    const rawPanel = String(panel || '').trim()
    const normalizedPanel = !usesLegacyObjectPanels.value && rawPanel === 'detail'
      ? 'detail'
      : normalizePanel(panel)
    const compatibilityPanel = ['publish', 'advanced'].includes(normalizedPanel)
    const nextPanel = usesLegacyObjectPanels.value || compatibilityPanel
      ? normalizedPanel
      : resolveStandaloneObjectDesignerSection(normalizedPanel)
    if (!nextPanel || nextPanel === activePanel.value)
      return
    if (!await saveFieldDraftBeforePanelSwitch())
      return
    await syncActiveFormDraft()
    await syncActiveListDraft()
    activePanel.value = nextPanel
  }

  async function saveFieldDraftBeforePanelSwitch() {
    if (activePanel.value !== 'fields' || !fieldDraftDirty.value)
      return true
    if (saving.value)
      return false
    saving.value = true
    await waitForSaveLoadingPaint()
    try {
      const saved = await fieldManagerRef.value?.saveSelectedField?.()
      if (saved !== true)
        return false
      fieldDraftDirty.value = false
      return true
    }
    finally {
      saving.value = false
    }
  }

  async function syncActiveFormDraft() {
    const panel = effectiveDesignerPanel.value
    if (!['form', 'detail'].includes(panel))
      return
    await nextTick()
    const result = formDesignerRef.value?.syncDesignerDraft?.()
    const changed = applyDesignerDraftSyncResult(result)
    if (result?.dirty || changed) {
      dirty.value = true
      designerDraftDirty.value = true
    }
    await nextTick()
  }

  async function syncActiveListDraft() {
    if (effectiveDesignerPanel.value !== 'list')
      return
    await nextTick()
    const result = listDesignerRef.value?.syncDesignerDraft?.()
    const changed = applyDesignerDraftSyncResult(result)
    if (result?.dirty || changed) {
      dirty.value = true
      designerDraftDirty.value = true
    }
    await nextTick()
  }

  function applyDesignerDraftSyncResult(result = {}) {
    if (!result || typeof result !== 'object')
      return false
    let changed = false
    let fieldsChanged = false

    if (hasOwn(result, 'fields') && Array.isArray(result.fields)) {
      const nextFields = cloneSchema(result.fields)
      fieldsChanged = !isSameSchema(nextFields, draft.fields)
      if (fieldsChanged) {
        draft.fields = nextFields
        changed = true
      }
    }

    if (hasOwn(result, 'modelSchema') && result.modelSchema) {
      const nextModelSchema = cloneSchema(result.modelSchema)
      if (!isSameSchema(nextModelSchema, draft.modelSchema)) {
        draft.modelSchema = nextModelSchema
        changed = true
      }
    }
    else if (fieldsChanged) {
      syncDraftModelFields(draft.fields)
    }

    if (hasOwn(result, 'pageSchema')) {
      const nextPageSchema = cloneSchema(result.pageSchema || null)
      if (!isSameSchema(nextPageSchema, draft.pageSchema)) {
        draft.pageSchema = nextPageSchema
        changed = true
      }
    }

    if (hasOwn(result, 'formDesignerSchema')) {
      const nextFormDesignerSchema = cloneSchema(result.formDesignerSchema || null)
      if (!isSameSchema(nextFormDesignerSchema, draft.formDesignerSchema)) {
        draft.formDesignerSchema = nextFormDesignerSchema
        changed = true
      }
    }

    if (hasOwn(result, 'viewSchema')) {
      const nextViewSchema = sanitizeViewSchemaFieldRefs(result.viewSchema || {}, draft.fields)
      if (!isSameSchema(nextViewSchema, draft.viewSchema)) {
        draft.viewSchema = cloneSchema(nextViewSchema)
        changed = true
      }
    }
    else if (fieldsChanged && draft.viewSchema) {
      const nextViewSchema = sanitizeViewSchemaFieldRefs(draft.viewSchema || {}, draft.fields)
      if (!isSameSchema(nextViewSchema, draft.viewSchema)) {
        draft.viewSchema = cloneSchema(nextViewSchema)
        changed = true
      }
    }

    return changed
  }

  function hasOwn(value, key) {
    return Object.prototype.hasOwnProperty.call(value, key)
  }

  async function persistPendingDesignerDraft() {
    if (!designerDraftDirty.value)
      return
    if (isCodeAppDesigner.value) {
      await saveCodeAppDesignerDraft(false, { reload: false })
      return
    }
    await saveDesignerDraft(false, { reload: false })
  }

  async function handlePreview() {
    if (effectiveDesignerPanel.value === 'form') {
      try {
        await syncActiveFormDraft()
        await formDesignerRef.value?.saveLayout?.()
      }
      catch {
        return
      }
      window.dispatchEvent(new CustomEvent('forge-form-designer:preview-current-form'))
      return
    }
    if (effectiveDesignerPanel.value === 'list') {
      // 列表预览由 BusinessListDesigner.openLocalPreview 统一「先保存再打开」
      window.dispatchEvent(new CustomEvent('forge-list-designer:preview-current-list'))
      return
    }
    message.info('当前面板暂未接入本地预览')
  }

  async function handlePublish(options = {}) {
    const explicitOptions = options && typeof options === 'object' && !('target' in options) ? options : {}
    const checklistOptions = activePanel.value === 'publish'
      ? publishChecklistRef.value?.getPublishOptions?.() || {}
      : {}
    const publishOptions = { ...checklistOptions, ...explicitOptions }
    if (!objectId.value || publishing.value)
      return
    publishing.value = true
    try {
      window.$loading?.show?.('正在发布业务单元，请稍候...')
      await waitForSaveLoadingPaint()
      await syncActiveFormDraft()
      await syncActiveListDraft()
      await persistPendingDesignerDraft()
      if (dirty.value) {
        await handleSave()
      }
      const latestCheck = await refreshPublishCheckState()
      if (latestCheck?.publishable === false) {
        message.warning(formatPublishBlockMessage(latestCheck))
        activePanel.value = 'publish'
        await nextTick()
        await publishChecklistRef.value?.refresh?.()
        return
      }
      if (hasTableSyncIssue(latestCheck) && !publishOptions.syncTable) {
        message.warning('发布前请在发布检查中确认同步数据表结构')
        activePanel.value = 'publish'
        await nextTick()
        await publishChecklistRef.value?.refresh?.()
        return
      }
      const res = await publishBusinessObject(objectId.value, {
        publishMode: 'PUBLISH',
        syncTable: !!publishOptions.syncTable,
        force: false,
        modelSchema: cloneSchema(draft.modelSchema || {}),
        pageSchema: cloneSchema(draft.pageSchema || {}),
        publishOptions: {},
      })
      message.success(res.data ? `业务单元已发布，版本 ${res.data}` : '业务单元已发布')
      await loadRuntimeInfo()
      await loadDesigner()
      emit('saved')
      await nextTick()
      if (activePanel.value === 'publish')
        await publishChecklistRef.value?.refresh?.()
    }
    finally {
      window.$loading?.close?.()
      publishing.value = false
    }
  }

  async function refreshPublishCheckState() {
    if (!objectId.value)
      return null
    const res = await businessObjectPublishCheck(objectId.value)
    publishCheckState.value = res.data || null
    return publishCheckState.value
  }

  function formatPublishBlockMessage(check = {}) {
    const blocks = Array.isArray(check.blockItems) ? check.blockItems : []
    if (!blocks.length)
      return '发布检查存在阻断项，请先修复'
    const summary = blocks
      .slice(0, 5)
      .map((item) => {
        const title = item?.title || item?.itemCode || ''
        const detail = item?.message || ''
        return detail ? `${title}：${detail}` : title
      })
      .filter(Boolean)
      .join('；')
    const remain = blocks.length > 5 ? `；另有 ${blocks.length - 5} 项` : ''
    return `发布检查存在 ${blocks.length} 个阻断项：${summary}${remain}`
  }

  async function handleBack() {
    if (props.embedded) {
      if (dirty.value) {
        const confirmed = await confirmLeave()
        if (!confirmed)
          return
      }
      emit('close')
      return
    }
    if (route.query.returnTo) {
      router.push(String(route.query.returnTo))
      return
    }
    if (suiteCode.value)
      router.push(`/app-center/suite/${suiteCode.value}`)
    else
      router.push(`/app-center/object/${objectCode.value}`)
  }

  function openRuntime() {
    if (!runtimeInfo.value?.canOpen) {
      message.warning(runtimeInfo.value?.message || '应用暂不可打开')
      return
    }
    router.push(runtimeInfo.value.routePath)
  }

  async function handleFieldsUpdated(fields, options = {}) {
    draft.fields = cloneSchema(fields || [])
    syncDraftModelFields(draft.fields)
    if (options?.fieldRename)
      applyFieldRename(options.fieldRename)
    draft.viewSchema = sanitizeViewSchemaFieldRefs(draft.viewSchema || {}, draft.fields)
    if (options?.reloadDesigner) {
      await loadDesigner()
      await tableMappingSummaryRef.value?.refresh?.()
      return
    }
    if (options?.persisted === false) {
      dirty.value = true
      designerDraftDirty.value = true
    }
    else {
      dirty.value = false
      designerDraftDirty.value = false
      await tableMappingSummaryRef.value?.refresh?.()
    }
  }

  async function handleAddFieldToForm(field) {
    if (!field)
      return
    activePanel.value = 'form'
    await nextTick()
    formDesignerRef.value?.appendFieldToForm?.(toPageField(field))
  }

  function hasTableSyncIssue(result) {
    const items = Array.isArray(result?.items) ? result.items : []
    return items.some(item =>
      item?.fixAction === 'SYNC_TABLE' || item?.itemCode === 'TABLE_MISSING' || item?.itemCode === 'TABLE_COLUMN_MISSING')
  }

  function handleLayoutSaved(pageSchema) {
    draft.pageSchema = cloneSchema(pageSchema || draft.pageSchema)
    dirty.value = false
    designerDraftDirty.value = false
    // 透传保存后的pageSchema，供宿主页同步页面区块的查询字段等快照。
    emit('saved', pageSchema ? cloneSchema(pageSchema) : undefined)
  }

  function applyFieldRename(rename = {}) {
    const oldFieldCode = String(rename.oldFieldCode || '').trim()
    const newFieldCode = String(rename.newFieldCode || '').trim()
    if (!oldFieldCode || !newFieldCode || oldFieldCode === newFieldCode)
      return
    const normalizedRename = {
      ...rename,
      oldFieldCode,
      newFieldCode,
    }
    draft.formDesignerSchema = renameFormDesignerFieldRefs(draft.formDesignerSchema || {}, normalizedRename)
    draft.viewSchema = renameViewSchemaFieldRefs(draft.viewSchema || {}, normalizedRename)
    draft.pageSchema = renameFieldRefsInValue(draft.pageSchema || {}, normalizedRename)
    if (draft.displayField === oldFieldCode)
      draft.displayField = newFieldCode
  }

  function handleRelationsUpdated(relations) {
    draft.relations = cloneSchema(relations || [])
  }

  async function handleFlowAppSaved() {
    await loadDesigner()
    if (isCodeAppDesigner.value) {
      if (!['form', 'list', 'flow-app'].includes(activePanel.value))
        activePanel.value = 'flow-app'
      return
    }
    const mainFlow = draft.documentConfig?.mainFlowSummary || {}
    if (draft.documentConfig?.documentEnabled && !mainFlow.configured) {
      activePanel.value = usesLegacyObjectPanels.value ? 'flow-app' : 'data-model'
    }
    const startMode = String(mainFlow.startMode || '').toUpperCase()
    if (startMode === 'TRIGGER' || startMode === 'BOTH')
      message.info('自动化触发配置请前往应用工作台的业务流程画布维护')
    activePanel.value = 'publish'
  }

  function handleFieldGenerationUpdate(payload = {}) {
    const fieldCode = String(payload.fieldCode || '').trim()
    if (!fieldCode)
      return
    const generation = cloneSchema(payload.generation || { enabled: false })
    const hideOnForm = payload.hideOnForm === true
    draft.fields = (draft.fields || []).map((field) => {
      const code = field.fieldCode || field.field
      if (code !== fieldCode)
        return field
      const enabled = generation.enabled === true
      return {
        ...field,
        // 自动编号默认只读可见；仅显式要求隐藏时才从填写表单拿掉
        readonly: enabled ? true : field.readonly,
        required: enabled ? false : field.required,
        formVisible: enabled ? !hideOnForm : field.formVisible,
        basicProps: {
          ...(field.basicProps || {}),
          generation,
        },
        advancedProps: {
          ...(field.advancedProps || {}),
          generation,
        },
      }
    })
    syncDraftModelFields(draft.fields)
    draft.formDesignerSchema = updateFormDesignerFieldGeneration(
      draft.formDesignerSchema || {},
      fieldCode,
      generation,
      { hideOnForm },
    )
    dirty.value = true
    designerDraftDirty.value = true
  }

  function handleActionsUpdated(actions) {
    draft.designerOptions = {
      ...(draft.designerOptions || {}),
      actions: cloneSchema(actions || []),
    }
    if (ready.value && ['list', 'relations', 'actions'].includes(effectiveDesignerPanel.value)) {
      dirty.value = true
      designerDraftDirty.value = true
    }
  }

  function handlePublishCheckUpdated(check) {
    publishCheckState.value = check || null
  }

  async function handleFixTarget(panel) {
    const targetPanel = normalizePanel(panel)
    if (panel === 'advanced' && !canAdvanced.value) {
      message.warning('该修复入口需要高级配置权限')
      return
    }
    if (panel === 'detail') {
      await handlePanelSwitch('form')
      return
    }
    await handlePanelSwitch(targetPanel || 'form')
  }

  function openDeveloperPath(path) {
    if (!path)
      return
    router.push(path)
  }

  function handleDirtyChange(value) {
    if (!ready.value)
      return
    dirty.value = !!value
    const panel = effectiveDesignerPanel.value
    if (value && ['form', 'detail'].includes(panel))
      designerDraftDirty.value = true
    if (value && ['list', 'relations', 'actions'].includes(panel))
      designerDraftDirty.value = true
  }

  function handleFieldDirtyChange(value) {
    if (!ready.value)
      return
    fieldDraftDirty.value = !!value
    if (value) {
      dirty.value = true
      return
    }
    if (!designerDraftDirty.value)
      dirty.value = false
  }

  function handleModelSchemaUpdated(modelSchema) {
    draft.modelSchema = cloneSchema(modelSchema || {})
    dirty.value = true
    designerDraftDirty.value = true
  }

  function handleListModelSchemaUpdate(modelSchema) {
    const next = cloneSchema(modelSchema || {})
    draft.modelSchema = next
    const modelFields = Array.isArray(next.fields) ? next.fields : []
    if (modelFields.length) {
      const existing = new Map((draft.fields || []).map(field => [field.fieldCode || field.field, field]))
      modelFields.forEach((field) => {
        const code = field.field || field.fieldCode
        if (!code || existing.has(code))
          return
        existing.set(code, {
          fieldCode: code,
          field: code,
          fieldName: field.label || field.fieldName || code,
          label: field.label || field.fieldName || code,
          columnName: field.columnName || code,
          dataType: field.dataType || 'varchar',
          componentType: field.componentType || 'input',
          queryType: field.queryType || 'eq',
          required: field.required === true,
          searchable: field.searchable === true,
          listVisible: field.listVisible !== false,
          formVisible: field.formVisible !== false,
          systemField: field.systemField === true,
          width: field.width || 120,
        })
      })
      draft.fields = [...existing.values()]
    }
    dirty.value = true
    designerDraftDirty.value = true
  }

  async function handleModelSchemaApplied(modelSchema) {
    handleModelSchemaUpdated(modelSchema)
    await saveDesignerDraft(true)
    await tableMappingSummaryRef.value?.refresh?.()
  }

  function markDirty() {
    if (ready.value)
      dirty.value = true
  }

  function updateStatus(value) {
    draft.status = value ? 1 : 0
    markDirty()
  }

  function buildDesignerPayload() {
    const viewSchema = sanitizeViewSchemaFieldRefs(draft.viewSchema || {}, draft.fields)
    return {
      objectId: objectId.value,
      objectName: draft.objectName,
      description: draft.description,
      icon: draft.icon,
      displayField: draft.displayField,
      status: draft.status,
      designStatus: draft.designStatus,
      modelSchema: cloneSchema(draft.modelSchema || {}),
      pageSchema: cloneSchema(draft.pageSchema || {}),
      fields: draft.fields.map(toFieldPayload),
      formDesignerSchema: cloneSchema(draft.formDesignerSchema || {}),
      viewSchema: cloneSchema(viewSchema),
      linkageSchema: cloneSchema(draft.linkageSchema || {}),
      designerOptions: cloneSchema(draft.designerOptions || {}),
    }
  }

  async function saveCodeAppDesignerDraft(showMessage = true, options = {}) {
    const code = draft.objectCode || objectCode.value
    if (!code)
      return
    const reload = options.reload !== false
    const metadata = buildCodeAppMetadataPayload()
    await saveBusinessFlowAppConfig(code, {
      documentConfig: null,
      flowBinding: null,
      options: {
        codeAppMetadata: metadata,
      },
    })
    dirty.value = false
    designerDraftDirty.value = false
    if (showMessage)
      message.success('代码应用设计已保存')
    emit('saved')
    if (reload)
      await loadDesigner()
  }

  function buildCodeAppMetadataPayload() {
    const baseFields = normalizeCodeAppFields(draft.fields || [])
    const assets = normalizeCodeAppAssets(draft.designerOptions?.codeAppAssets || [])
    const fallbackAsset = {
      formKey: `${draft.objectCode || objectCode.value || 'code_app'}_form`,
      formName: `${draft.objectName || '代码业务'}表单`,
      formMode: 'BUSINESS_CODE_FORM',
      type: 'BUSINESS_CODE_FORM',
      providerKey: '',
      formUrl: '',
    }
    const sourceAssets = assets.length ? assets : [fallbackAsset]
    const formDesignerSchema = cloneSchema(
      draft.formDesignerSchema || createCodeAppFormDesignerSchema({}, sourceAssets, baseFields, draft.objectCode, draft.objectName),
    )
    const fields = applyCodeAppFormVisibility(baseFields, formDesignerSchema)
    const formAssets = (assets.length ? assets : [fallbackAsset]).map(asset => ({
      ...asset,
      objectCode: draft.objectCode || objectCode.value,
      objectName: draft.objectName,
      fields,
      fieldCatalog: fields,
      fieldCount: fields.length,
    }))
    return {
      objectCode: draft.objectCode || objectCode.value,
      objectName: draft.objectName,
      formAssets,
      fields,
      formDesignerSchema,
      pageSchema: cloneSchema(draft.pageSchema || {}),
      viewSchema: cloneSchema(createCodeAppViewSchema(draft.viewSchema, fields)),
    }
  }

  function createEmptyDraft() {
    return {
      objectId: null,
      appId: null,
      suiteCode: '',
      suiteName: '',
      objectCode: '',
      objectName: '',
      configKey: '',
      displayField: '',
      icon: '',
      description: '',
      status: 1,
      designStatus: 'DRAFT',
      publishStatus: 'UNPUBLISHED',
      lastPublishVersion: null,
      modelSchema: null,
      pageSchema: null,
      formDesignerSchema: null,
      viewSchema: null,
      linkageSchema: null,
      fields: [],
      relations: [],
      documentConfig: null,
      designerOptions: {},
    }
  }

  function appendCodeAppAsset(...args) { return __impl.appendCodeAppAsset(...args) }
  function appendCodeAppDesignField(...args) { return __impl.appendCodeAppDesignField(...args) }
  function fieldCode(...args) { return __impl.fieldCode(...args) }
  function applyCodeAppFormVisibility(...args) { return __impl.applyCodeAppFormVisibility(...args) }
  function assetKeys(...args) { return __impl.assetKeys(...args) }
  function buildMergedCodeAppAsset(...args) { return __impl.buildMergedCodeAppAsset(...args) }
  function collectCodeAppAssetFields(...args) { return __impl.collectCodeAppAssetFields(...args) }
  function collectCodeAppFormFieldRefs(...args) { return __impl.collectCodeAppFormFieldRefs(...args) }
  function collectCodeAppFormFieldVisibility(...args) { return __impl.collectCodeAppFormFieldVisibility(...args) }
  function collectCodeAppPageFieldRefs(...args) { return __impl.collectCodeAppPageFieldRefs(...args) }
  function collectCodeAppProviderCatalogAssets(...args) { return __impl.collectCodeAppProviderCatalogAssets(...args) }
  function compareCodeAppFieldOrder(...args) { return __impl.compareCodeAppFieldOrder(...args) }
  function confirmLeave(...args) { return __impl.confirmLeave(...args) }
  function createCodeAppFormDesignerSchema(...args) { return __impl.createCodeAppFormDesignerSchema(...args) }
  function createCodeAppModelSchema(...args) { return __impl.createCodeAppModelSchema(...args) }
  function createCodeAppPageSchema(...args) { return __impl.createCodeAppPageSchema(...args) }
  function createCodeAppViewSchema(...args) { return __impl.createCodeAppViewSchema(...args) }
  function createDraftFromDesigner(...args) { return __impl.createDraftFromDesigner(...args) }
  function ensureCodeAppFormSchemaFields(...args) { return __impl.ensureCodeAppFormSchemaFields(...args) }
  function ensureCodeAppPrimaryFormSchema(...args) { return __impl.ensureCodeAppPrimaryFormSchema(...args) }
  function findCodeAppAsset(...args) { return __impl.findCodeAppAsset(...args) }
  function handleBeforeUnload(...args) { return __impl.handleBeforeUnload(...args) }
  function hasCodeAppFieldComponents(...args) { return __impl.hasCodeAppFieldComponents(...args) }
  function hasDetailViewFields(...args) { return __impl.hasDetailViewFields(...args) }
  function hasPermission(...args) { return __impl.hasPermission(...args) }
  function hasUsableCodeAppFormSchema(...args) { return __impl.hasUsableCodeAppFormSchema(...args) }
  function hasUsableCodeAppPageSchema(...args) { return __impl.hasUsableCodeAppPageSchema(...args) }
  function hydrateCodeAppProviderAssets(...args) { return __impl.hydrateCodeAppProviderAssets(...args) }
  function inferCodeAppDataType(...args) { return __impl.inferCodeAppDataType(...args) }
  function isInactiveField(...args) { return __impl.isInactiveField(...args) }
  function mergeCodeAppAssets(...args) { return __impl.mergeCodeAppAssets(...args) }
  function mergeCodeAppDetailSections(...args) { return __impl.mergeCodeAppDetailSections(...args) }
  function mergeCodeAppFields(...args) { return __impl.mergeCodeAppFields(...args) }
  function mergeCodeAppPageZones(...args) { return __impl.mergeCodeAppPageZones(...args) }
  function mergeCodeAppViewItems(...args) { return __impl.mergeCodeAppViewItems(...args) }
  function normalizeCodeAppAssets(...args) { return __impl.normalizeCodeAppAssets(...args) }
  function normalizeCodeAppComponentType(...args) { return __impl.normalizeCodeAppComponentType(...args) }
  function normalizeCodeAppFields(...args) { return __impl.normalizeCodeAppFields(...args) }
  function pickCodeAppAssetConfig(...args) { return __impl.pickCodeAppAssetConfig(...args) }
  function renameFieldRefsInClonedValue(...args) { return __impl.renameFieldRefsInClonedValue(...args) }
  function renameFieldRefsInValue(...args) { return __impl.renameFieldRefsInValue(...args) }
  function resolveCodeAppMetadata(...args) { return __impl.resolveCodeAppMetadata(...args) }
  function syncDraftModelFields(...args) { return __impl.syncDraftModelFields(...args) }
  function toCodeAppModelField(...args) { return __impl.toCodeAppModelField(...args) }
  function toFieldPayload(...args) { return __impl.toFieldPayload(...args) }
  function toPageField(...args) { return __impl.toPageField(...args) }
  function updateFormDesignerFieldGeneration(...args) { return __impl.updateFormDesignerFieldGeneration(...args) }

  __impl.resolveInitialPanel = resolveInitialPanel
  __impl.resolveApplicationCode = resolveApplicationCode
  __impl.openProcessWorkspace = openProcessWorkspace
  __impl.normalizePanel = normalizePanel
  __impl.resolveInitialFormCanvasView = resolveInitialFormCanvasView
  __impl.loadDesigner = loadDesigner
  __impl.resolveBusinessObject = resolveBusinessObject
  __impl.findBusinessObjectByCode = findBusinessObjectByCode
  __impl.isCodeAppRoute = isCodeAppRoute
  __impl.shouldOpenCodeAppDesigner = shouldOpenCodeAppDesigner
  __impl.isCodeAppBusinessObject = isCodeAppBusinessObject
  __impl.hasCodeAppFlag = hasCodeAppFlag
  __impl.applyCodeAppDesignerDraft = applyCodeAppDesignerDraft
  __impl.loadRuntimeInfo = loadRuntimeInfo
  __impl.handleSave = handleSave
  __impl.confirmSeedConfigurationTakeover = confirmSeedConfigurationTakeover
  __impl.requestSeedTakeoverConfirmation = requestSeedTakeoverConfirmation
  __impl.saveDesignerDraft = saveDesignerDraft
  __impl.waitForSaveLoadingPaint = waitForSaveLoadingPaint
  __impl.handlePanelSwitch = handlePanelSwitch
  __impl.saveFieldDraftBeforePanelSwitch = saveFieldDraftBeforePanelSwitch
  __impl.syncActiveFormDraft = syncActiveFormDraft
  __impl.syncActiveListDraft = syncActiveListDraft
  __impl.applyDesignerDraftSyncResult = applyDesignerDraftSyncResult
  __impl.hasOwn = hasOwn
  __impl.persistPendingDesignerDraft = persistPendingDesignerDraft
  __impl.handlePreview = handlePreview
  __impl.handlePublish = handlePublish
  __impl.refreshPublishCheckState = refreshPublishCheckState
  __impl.formatPublishBlockMessage = formatPublishBlockMessage
  __impl.handleBack = handleBack
  __impl.openRuntime = openRuntime
  __impl.handleFieldsUpdated = handleFieldsUpdated
  __impl.handleAddFieldToForm = handleAddFieldToForm
  __impl.hasTableSyncIssue = hasTableSyncIssue
  __impl.handleLayoutSaved = handleLayoutSaved
  __impl.applyFieldRename = applyFieldRename
  __impl.handleRelationsUpdated = handleRelationsUpdated
  __impl.handleFlowAppSaved = handleFlowAppSaved
  __impl.handleFieldGenerationUpdate = handleFieldGenerationUpdate
  __impl.handleActionsUpdated = handleActionsUpdated
  __impl.handlePublishCheckUpdated = handlePublishCheckUpdated
  __impl.handleFixTarget = handleFixTarget
  __impl.openDeveloperPath = openDeveloperPath
  __impl.handleDirtyChange = handleDirtyChange
  __impl.handleFieldDirtyChange = handleFieldDirtyChange
  __impl.handleModelSchemaUpdated = handleModelSchemaUpdated
  __impl.handleListModelSchemaUpdate = handleListModelSchemaUpdate
  __impl.handleModelSchemaApplied = handleModelSchemaApplied
  __impl.markDirty = markDirty
  __impl.updateStatus = updateStatus
  __impl.buildDesignerPayload = buildDesignerPayload
  __impl.saveCodeAppDesignerDraft = saveCodeAppDesignerDraft
  __impl.buildCodeAppMetadataPayload = buildCodeAppMetadataPayload
  __impl.createEmptyDraft = createEmptyDraft

  return {
    props, emit, __impl, mut, appendCodeAppAsset, appendCodeAppDesignField, applyCodeAppDesignerDraft, applyCodeAppFormVisibility,
    applyDesignerDraftSyncResult, applyFieldRename, assetKeys, buildCodeAppMetadataPayload, buildDesignerPayload, buildMergedCodeAppAsset, collectCodeAppAssetFields, collectCodeAppFormFieldRefs,
    collectCodeAppFormFieldVisibility, collectCodeAppPageFieldRefs, collectCodeAppProviderCatalogAssets, compareCodeAppFieldOrder, confirmLeave, confirmSeedConfigurationTakeover, createCodeAppFormDesignerSchema, createCodeAppModelSchema,
    createCodeAppPageSchema, createCodeAppViewSchema, createDraftFromDesigner, createEmptyDraft, ensureCodeAppFormSchemaFields, ensureCodeAppPrimaryFormSchema, fieldCode,
    findBusinessObjectByCode, findCodeAppAsset, formatPublishBlockMessage, handleActionsUpdated, handleAddFieldToForm, handleBack, handleBeforeUnload, handleDirtyChange,
    handleFieldDirtyChange, handleFieldGenerationUpdate, handleFieldsUpdated, handleFixTarget, handleFlowAppSaved, handleLayoutSaved, handleListModelSchemaUpdate, handleModelSchemaApplied,
    handleModelSchemaUpdated, handlePanelSwitch, handlePreview, handlePublish, handlePublishCheckUpdated, handleRelationsUpdated, handleSave, hasCodeAppFieldComponents,
    hasCodeAppFlag, hasDetailViewFields, hasOwn, hasPermission, hasTableSyncIssue, hasUsableCodeAppFormSchema, hasUsableCodeAppPageSchema, hydrateCodeAppProviderAssets,
    inferCodeAppDataType, isCodeAppBusinessObject, isCodeAppRoute, isInactiveField, loadDesigner, loadRuntimeInfo, markDirty, mergeCodeAppAssets,
    mergeCodeAppDetailSections, mergeCodeAppFields, mergeCodeAppPageZones, mergeCodeAppViewItems, normalizeCodeAppAssets, normalizeCodeAppComponentType, normalizeCodeAppFields, normalizePanel,
    openDeveloperPath, openProcessWorkspace, openRuntime, persistPendingDesignerDraft, pickCodeAppAssetConfig, refreshPublishCheckState, renameFieldRefsInClonedValue, renameFieldRefsInValue,
    requestSeedTakeoverConfirmation, resolveApplicationCode, resolveBusinessObject, resolveCodeAppMetadata, resolveInitialFormCanvasView, resolveInitialPanel, saveCodeAppDesignerDraft, saveDesignerDraft,
    saveFieldDraftBeforePanelSwitch, shouldOpenCodeAppDesigner, syncActiveFormDraft, syncActiveListDraft, syncDraftModelFields, toCodeAppModelField, toFieldPayload, toPageField,
    updateFormDesignerFieldGeneration, updateStatus, waitForSaveLoadingPaint, route,
    router, message, tabStore, userStore, loading, saving, publishing, dirty,
    designerDraftDirty, fieldDraftDirty, ready, activePanel, initialFormCanvasView, developerMode, designer, runtimeInfo,
    publishCheckState, functionMarketVisible, fieldManagerRef, tableMappingSummaryRef, tableMapping, formDesignerRef, listDesignerRef, relationDesignerRef,
    flowAppConfigRef, treeModelRef, publishChecklistRef, draft, objectCode, suiteCode, objectId, applicationCode,
    pageTitle, canAdvanced, isCodeAppDesigner, usesLegacyObjectPanels, effectiveDesignerPanel, publishDisabled, workspaceObjects, designerNavPanels,
    fieldOptions, runtimeFormOptions,
  }
}
