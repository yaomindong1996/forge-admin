/** flow/design setup part 1. */
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
export function applyFlowDesignPart1(props, emit) {
  const __impl = {}
  const mut = {
    persistedBusinessFormRef: undefined,
    persistedBusinessObjectCode: undefined,
    businessSelectionClearTimer: null,
  }
  const applyProcessConfigFromXml = (...args) => __impl.applyProcessConfigFromXml(...args)
  const applyProcessConfigToXml = (...args) => __impl.applyProcessConfigToXml(...args)
  const buildBusinessGlobalFormJson = (...args) => __impl.buildBusinessGlobalFormJson(...args)
  const buildDiagramXml = (...args) => __impl.buildDiagramXml(...args)
  const buildWaypoints = (...args) => __impl.buildWaypoints(...args)
  const collectBusinessAssetFields = (...args) => __impl.collectBusinessAssetFields(...args)
  const decodeHtmlEntities = (...args) => __impl.decodeHtmlEntities(...args)
  const ensureBusinessGlobalFormSelection = (...args) => __impl.ensureBusinessGlobalFormSelection(...args)
  const ensureDiagramNamespaces = (...args) => __impl.ensureDiagramNamespaces(...args)
  const ensureProcessId = (...args) => __impl.ensureProcessId(...args)
  const escapeXmlAttr = (...args) => __impl.escapeXmlAttr(...args)
  const extractBpmnXml = (...args) => __impl.extractBpmnXml(...args)
  const extractJsonObject = (...args) => __impl.extractJsonObject(...args)
  const extractProcessConfigFromXml = (...args) => __impl.extractProcessConfigFromXml(...args)
  const findBusinessFormAsset = (...args) => __impl.findBusinessFormAsset(...args)
  const findBusinessFormAssetInList = (...args) => __impl.findBusinessFormAssetInList(...args)
  const findElementByLocalName = (...args) => __impl.findElementByLocalName(...args)
  const findElementsByLocalName = (...args) => __impl.findElementsByLocalName(...args)
  const getBpmnDisplayIssue = (...args) => __impl.getBpmnDisplayIssue(...args)
  const getBpmnNodeSize = (...args) => __impl.getBpmnNodeSize(...args)
  const getXmlForSave = (...args) => __impl.getXmlForSave(...args)
  const getXmlParseError = (...args) => __impl.getXmlParseError(...args)
  const goBackToBusinessApp = (...args) => __impl.goBackToBusinessApp(...args)
  const handleAbortAi = (...args) => __impl.handleAbortAi(...args)
  const handleAiSend = (...args) => __impl.handleAiSend(...args)
  const handleApplyAiDraft = (...args) => __impl.handleApplyAiDraft(...args)
  const handleBack = (...args) => __impl.handleBack(...args)
  const handleBpmnChange = (...args) => __impl.handleBpmnChange(...args)
  const handleBusinessElementSelect = (...args) => __impl.handleBusinessElementSelect(...args)
  const handleBusinessGlobalFormUpdate = (...args) => __impl.handleBusinessGlobalFormUpdate(...args)
  const handleBusinessManagedFormTypeChange = (...args) => __impl.handleBusinessManagedFormTypeChange(...args)
  const handleBusinessObjectChange = (...args) => __impl.handleBusinessObjectChange(...args)
  const handleBusinessPanelClose = (...args) => __impl.handleBusinessPanelClose(...args)
  const handleDeploy = (...args) => __impl.handleDeploy(...args)
  const handleDiagramImportEnd = (...args) => __impl.handleDiagramImportEnd(...args)
  const handleDiagramImportStart = (...args) => __impl.handleDiagramImportStart(...args)
  const handleFlowSSEChunk = (...args) => __impl.handleFlowSSEChunk(...args)
  const handleFlowSSEComplete = (...args) => __impl.handleFlowSSEComplete(...args)
  const handleFlowSSEError = (...args) => __impl.handleFlowSSEError(...args)
  const handleFormSelect = (...args) => __impl.handleFormSelect(...args)
  const handleFormTypeChange = (...args) => __impl.handleFormTypeChange(...args)
  const handleModelerReady = (...args) => __impl.handleModelerReady(...args)
  const handleNewAiSession = (...args) => __impl.handleNewAiSession(...args)
  const handleOpenVersionHistory = (...args) => __impl.handleOpenVersionHistory(...args)
  const handlePreviewAiXml = (...args) => __impl.handlePreviewAiXml(...args)
  const handleSaveDraft = (...args) => __impl.handleSaveDraft(...args)
  const handleSaveFormSchema = (...args) => __impl.handleSaveFormSchema(...args)
  const handleVersionHistoryRefresh = (...args) => __impl.handleVersionHistoryRefresh(...args)
  const hasBpmnDiagram = (...args) => __impl.hasBpmnDiagram(...args)
  const isBpmnFlowNode = (...args) => __impl.isBpmnFlowNode(...args)
  const layoutBpmnNodes = (...args) => __impl.layoutBpmnNodes(...args)
  const loadModel = (...args) => __impl.loadModel(...args)
  const normalizeAiBpmnXml = (...args) => __impl.normalizeAiBpmnXml(...args)
  const normalizeAiFlowDraft = (...args) => __impl.normalizeAiFlowDraft(...args)
  const normalizeBpmnId = (...args) => __impl.normalizeBpmnId(...args)
  const normalizeBusinessEntryRoute = (...args) => __impl.normalizeBusinessEntryRoute(...args)
  const normalizeBusinessFieldCatalog = (...args) => __impl.normalizeBusinessFieldCatalog(...args)
  const normalizeBusinessFormAssets = (...args) => __impl.normalizeBusinessFormAssets(...args)
  const normalizeBusinessFormMode = (...args) => __impl.normalizeBusinessFormMode(...args)
  const normalizeBusinessGlobalFormBeforeSave = (...args) => __impl.normalizeBusinessGlobalFormBeforeSave(...args)
  const parseAiFlowResponse = (...args) => __impl.parseAiFlowResponse(...args)
  const parseXmlDocument = (...args) => __impl.parseXmlDocument(...args)
  const readFlowableAttr = (...args) => __impl.readFlowableAttr(...args)
  const rebuildDiagramInfo = (...args) => __impl.rebuildDiagramInfo(...args)
  const refreshBusinessFormFieldCatalog = (...args) => __impl.refreshBusinessFormFieldCatalog(...args)
  const repairBpmnXml = (...args) => __impl.repairBpmnXml(...args)
  const resolveBusinessBindingForModel = (...args) => __impl.resolveBusinessBindingForModel(...args)
  const resolveLocalFormFieldCatalog = (...args) => __impl.resolveLocalFormFieldCatalog(...args)
  // —— 纯工具：part1 的 computed 会在 part2/part3 挂 __impl 前求值，必须本地实现 ——
  function routeQueryText(value) {
    return String(Array.isArray(value) ? value[0] || '' : value || '').trim()
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
  function isAppManagedFormType(value) {
    return value === 'business'
  }
  function normalizeAppManagedFormType(_value) {
    return 'business'
  }
  function getElementTitle(el) {
    if (!el)
      return '属性设置'
    const typeNames = {
      'bpmn:StartEvent': '开始节点',
      'bpmn:EndEvent': '结束节点',
      'bpmn:UserTask': '用户任务',
      'bpmn:ServiceTask': '服务任务',
      'bpmn:ScriptTask': '脚本任务',
      'bpmn:BusinessRuleTask': '业务规则任务',
      'bpmn:ManualTask': '手工任务',
      'bpmn:ExclusiveGateway': '排他网关',
      'bpmn:ParallelGateway': '并行网关',
      'bpmn:InclusiveGateway': '包容网关',
      'bpmn:SequenceFlow': '序列流',
      'bpmn:SubProcess': '子流程',
      'bpmn:CallActivity': '调用活动',
    }
    return el.businessObject?.name || typeNames[el.type] || '属性设置'
  }
  function getElementIcon(el) {
    const iconMap = {
      'bpmn:StartEvent': 'i-material-symbols:play-circle-outline',
      'bpmn:EndEvent': 'i-material-symbols:stop-circle-outline',
      'bpmn:UserTask': 'i-material-symbols:person-check-outline',
      'bpmn:ServiceTask': 'i-material-symbols:settings-outline',
      'bpmn:ScriptTask': 'i-material-symbols:code-blocks-outline',
      'bpmn:BusinessRuleTask': 'i-material-symbols:rule-settings-outline',
      'bpmn:ManualTask': 'i-material-symbols:pan-tool-outline',
      'bpmn:ExclusiveGateway': 'i-material-symbols:conversion-path-outline',
      'bpmn:ParallelGateway': 'i-material-symbols:call-split-outline',
      'bpmn:InclusiveGateway': 'i-material-symbols:merge-type-outline',
      'bpmn:SequenceFlow': 'i-material-symbols:arrow-right-alt',
      'bpmn:SubProcess': 'i-material-symbols:account-tree-outline',
      'bpmn:CallActivity': 'i-material-symbols:call-made',
    }
    return iconMap[el?.type] || 'i-material-symbols:tune'
  }
  Object.assign(__impl, {
    routeQueryText,
    parseBooleanWithDefault,
    normalizeDesignerType,
    normalizeAutoApprovalMode,
    normalizeRejectStrategy,
    isAppManagedFormType,
    normalizeAppManagedFormType,
    getElementTitle,
    getElementIcon,
    // part2 挂载前的安全桩；part2 会覆盖为真实实现
    findBusinessFormAsset: () => null,
    findBusinessFormAssetInList: () => null,
  })
  const scrollActiveReasoningToBottom = (...args) => __impl.scrollActiveReasoningToBottom(...args)
  const scrollAiToBottom = (...args) => __impl.scrollAiToBottom(...args)
  const setReasoningContentRef = (...args) => __impl.setReasoningContentRef(...args)
  const stripCodeFence = (...args) => __impl.stripCodeFence(...args)
  const stripDiagramInfo = (...args) => __impl.stripDiagramInfo(...args)
  const syncSafeModelKey = (...args) => __impl.syncSafeModelKey(...args)
  const toggleAiPanel = (...args) => __impl.toggleAiPanel(...args)
  const toggleReasoning = (...args) => __impl.toggleReasoning(...args)
  const updateLastAiAssistantMessage = (...args) => __impl.updateLastAiAssistantMessage(...args)
  const validateBusinessGlobalFormBeforeSave = (...args) => __impl.validateBusinessGlobalFormBeforeSave(...args)
  const route = useRoute()
  const router = useRouter()
  const tabStore = useTabStore()
  const message = window.$message
  const { dict } = useDict('flow_designer_type', 'flow_process_form_type', 'flow_auto_approval_mode', 'flow_model_status')

  const FlowModeler = defineAsyncComponent(() => import('@/components/bpmn/FlowModeler.vue'))

  // 与流程通知运行时保持一致；新建模型及历史空值模型都会展示该默认地址，清空后仍可回退后端默认值。
  const DEFAULT_TODO_DETAIL_URL_TEMPLATE = '/#/pages/todo-detail?taskId={taskId}'

  const embedded = computed(() => props.embedded)
  const explicitBusinessObjectCode = computed(() => routeQueryText(props.businessObjectCode || route.query.businessObjectCode || route.query.objectCode))
  const explicitBusinessObjectName = computed(() => routeQueryText(props.businessObjectName || route.query.businessObjectName || route.query.objectName))
  const explicitBusinessApplicationId = computed(() => routeQueryText(props.applicationId || route.query.applicationId || route.query.appId))
  const explicitBusinessEntryRoute = computed(() => routeQueryText(props.businessEntryRoute || route.query.businessEntryRoute))
  const resolvedBusinessBinding = ref(null)
  const manualBusinessApplicationId = ref('')
  const manualBusinessApplicationName = ref('')
  const manualBusinessObjectCode = ref('')
  const manualBusinessObjectName = ref('')
  const businessApplicationOptions = ref([])
  const businessApplicationLoading = ref(false)
  const businessObjectOptions = ref([])
  const businessObjectLoading = ref(false)
  const businessObjectOptionsApplicationId = ref('')

  const businessObjectCode = computed(() => explicitBusinessObjectCode.value
    || routeQueryText(resolvedBusinessBinding.value?.objectCode)
    || manualBusinessObjectCode.value
    || routeQueryText(mut.persistedBusinessObjectCode?.value))
  const businessContextActive = computed(() => !!businessObjectCode.value)
  const businessContextName = computed(() => explicitBusinessObjectName.value
    || routeQueryText(resolvedBusinessBinding.value?.objectName)
    || manualBusinessObjectName.value
    || routeQueryText(mut.persistedBusinessFormRef?.value?.objectName)
    || businessObjectCode.value)
  const businessEntryRoute = computed(() => explicitBusinessEntryRoute.value || routeQueryText(resolvedBusinessBinding.value?.entryRoute))
  const effectiveApplicationId = computed(() => explicitBusinessApplicationId.value
    || manualBusinessApplicationId.value
    || routeQueryText(mut.persistedBusinessFormRef?.value?.applicationId)
    || routeQueryText(resolvedBusinessBinding.value?.applicationId))
  const businessApplicationName = computed(() => manualBusinessApplicationName.value
    || routeQueryText(mut.persistedBusinessFormRef?.value?.applicationName)
    || routeQueryText(resolvedBusinessBinding.value?.applicationName)
    || businessApplicationOptions.value.find(item => item.value === effectiveApplicationId.value)?.application?.applicationName
    || '')
  const effectiveCodeApp = computed(() => {
    return props.codeApp
      || parseBooleanWithDefault(routeQueryText(route.query.codeApp), false)
      || (!explicitBusinessObjectCode.value && parseBooleanWithDefault(resolvedBusinessBinding.value?.codeApp, false))
  })

  const saving = ref(false)
  const deploying = ref(false)
  const pageLoading = ref(true)
  const diagramLoadingCount = ref(0)
  const modelerRef = ref(null)
  const bpmnXml = ref('')
  const hasChanges = ref(false)
  watch(hasChanges, (value) => {
    if (!props.embedded)
      tabStore.setTabDirty(route.fullPath, value, '当前流程设计存在未保存的更改')
  }, { immediate: true })
  const aiSending = ref(false)
  const aiPrompt = ref('')
  const aiSessionId = ref('')
  const aiMessages = ref([])
  const aiDraft = ref(null)
  const showAiXmlPreview = ref(false)
  const showVersionHistory = ref(false)
  const aiAbortController = ref(null)
  const aiRawContent = ref('')
  const aiReasoningContent = ref('')
  const aiIsReasoningPhase = ref(false)
  const aiReasoningStartTime = ref(null)
  const aiReasoningEndTime = ref(null)
  const aiCurrentStage = ref('')
  const expandedReasonings = ref({})
  const aiMessageListRef = ref(null)
  const aiMessageEndRef = ref(null)
  const reasoningContentRefs = ref([])
  const showModelPanel = ref(false)
  const syncingModel = ref(false)

  const aiProviderId = ref(null)
  const aiModelId = ref(null)
  const providerOptions = ref([])
  const modelOptions = ref([])

  const workspaceMode = ref('design')
  const rightActiveTab = ref('flow')

  const modelInfo = reactive({
    id: '',
    modelName: '',
    modelKey: '',
    category: '',
    flowType: '',
    designerType: 'approval',
    allowSubmitterWithdraw: true,
    allowMultiReturn: false,
    rejectStrategy: 'TO_INITIATOR_MODIFY',
    autoApprovalMode: 'none',
    formType: 'dynamic',
    formId: null,
    formUrl: '',
    todoDetailUrlTemplate: DEFAULT_TODO_DETAIL_URL_TEMPLATE,
    formJson: '',
    description: '',
    status: 0,
    version: 1,
    startListener: '',
    endListener: '',
    notifyType: 'redis',
    notifyConfig: null,
  })

  // 必须在 persistedBusinessFormRef 之前定义：该 computed 会在 part2 挂 __impl 前被求值。
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
  __impl.parseBusinessGlobalFormRef = parseBusinessGlobalFormRef

  // 业务表单引用会把应用和对象上下文一起持久化，独立打开流程模型时可据此恢复级联选择。
  mut.persistedBusinessFormRef = computed(() => parseBusinessGlobalFormRef(modelInfo.formJson, ''))
  mut.persistedBusinessObjectCode = computed(() => routeQueryText(mut.persistedBusinessFormRef.value.objectCode))

  const showFormDesigner = ref(false)
  const formDesignerRef = ref(null)
  const formSchema = ref([])
  const formOptions = ref([])
  const formFieldCatalog = ref([])
  const formFieldCatalogError = ref('')
  const showFormPreview = ref(false)
  const businessFormAssets = ref([])

  const categoryTreeOptions = ref([])
  const modelerInstance = ref(null)
  const dockedElement = ref(null)

  const notifyEventDefinitions = [
    {
      key: 'todo',
      label: '新待办',
      description: '任务创建或转办后通知当前处理人。',
    },
    {
      key: 'result',
      label: '审批结果',
      description: '流程通过或驳回后通知发起人。',
    },
    {
      key: 'cc',
      label: '流程通过抄送',
      description: '流程通过后通知抄送接收人，短信不适用于抄送。',
    },
  ]
  const notifyChannelOrder = ['WEB', 'EMAIL', 'SMS', 'COLLABORATION']
  const defaultNotificationTemplateCodes = { todo: 'FLOW_TODO_CARD', result: 'FLOW_RESULT_CARD', cc: 'FLOW_CC_CARD' }
  const notifyChannels = ref([])
  const notifyChannelsLoading = ref(false)
  const notifyChannelsLoaded = ref(false)
  const notificationTemplates = ref([])
  const notificationTemplatesLoading = ref(false)
  const notificationTemplatesLoaded = ref(false)
  const notifyMatrixDirty = ref(false)
  const notifyConfigInitialPresent = ref(false)
  const notifyMatrix = reactive({ todo: [], result: [], cc: [] })
  const notifyTemplateCodes = reactive({ todo: null, result: null, cc: null })
  const notifyEventConfigured = reactive({ todo: false, result: false, cc: false })
  const showNotificationTemplatePreview = ref(false)
  const notificationPreview = ref(null)

  const designerTypeOptions = computed(() => dict.value.flow_designer_type || [])
  const formTypeOptions = computed(() => dict.value.flow_process_form_type || [])
  const businessManagedFormTypeOptions = computed(() => [...formTypeOptions.value]
    .filter(item => item.value !== 'dynamic')
    .sort((a, b) => Number(b.value === 'business') - Number(a.value === 'business')))
  const autoApprovalModeOptions = computed(() => (dict.value.flow_auto_approval_mode || []).map(item => ({
    ...item,
    desc: item.remark,
  })))
  const modelStatusOptions = computed(() => toNumberDictOptions(dict.value.flow_model_status))

  const aiExamples = [
    { label: '请假审批', text: '生成一个请假审批流程：3天以内直属上级审批，超过3天先直属上级再HR审批。' },
    { label: '报销审批', text: '把当前流程改成报销审批：金额超过5000增加财务经理审批，否则财务专员审批。' },
    { label: '驳回路径', text: '给当前流程增加驳回路径，审批不通过时回到发起人修改。' },
  ]

  const aiStages = [
    { key: 'analyzing', label: '分析需求' },
    { key: 'generating', label: '生成流程' },
    { key: 'reasoning', label: '推理结构' },
    { key: 'complete', label: '完成' },
  ]

  const currentAiStageIndex = computed(() => {
    return aiStages.findIndex(s => s.key === aiCurrentStage.value)
  })

  const statusTag = computed(() => {
    const option = modelStatusOptions.value.find(item => item.value === Number(modelInfo.status))
    return option
      ? { type: option.listClass || 'default', label: option.label }
      : { type: 'default', label: modelInfo.status ?? '未知' }
  })

  const isReadonly = computed(() => modelInfo.status === 1)

  const designerLoading = computed(() => pageLoading.value || diagramLoadingCount.value > 0)

  const aiGeneratingCanvasHintVisible = computed(() => aiSending.value && !designerLoading.value)

  const aiCanvasHintText = computed(() => {
    const stageText = aiStages.find(s => s.key === aiCurrentStage.value)?.label || '生成流程'
    return `${stageText}中，请耐心等待，完成后可一键加载到画布`
  })

  const currentProviderLabel = computed(() => {
    const item = providerOptions.value.find(p => p.value === aiProviderId.value)
    return item?.label || '未选择供应商'
  })

  const currentModelLabel = computed(() => {
    const item = modelOptions.value.find(m => m.value === aiModelId.value)
    if (!item)
      return '请选择模型'
    return item.modelCode || item.label
  })

  const designerType = computed(() => normalizeDesignerType(modelInfo.designerType))
  const isApprovalDesigner = computed(() => designerType.value === 'approval')
  const isBusinessDesigner = computed(() => designerType.value === 'business')
  const designerRenderKey = computed(() => `${modelInfo.id || 'new'}:${designerType.value}`)
  const designerTypeLabel = computed(() => {
    return designerTypeOptions.value.find(item => item.value === designerType.value)?.label || designerType.value
  })
  const businessPanelTitle = computed(() => getElementTitle(dockedElement.value))
  const businessPanelIcon = computed(() => getElementIcon(dockedElement.value))

  const processConfig = computed(() => ({
    allowSubmitterWithdraw: modelInfo.allowSubmitterWithdraw !== false,
    autoApprovalMode: normalizeAutoApprovalMode(modelInfo.autoApprovalMode),
    rejectStrategy: normalizeRejectStrategy(modelInfo.rejectStrategy),
  }))

  const appManagedFormTypeActive = computed(() => isAppManagedFormType(modelInfo.formType))
  const businessFormConfigActive = computed(() => businessContextActive.value || appManagedFormTypeActive.value)
  const businessApplicationPickerVisible = computed(() => {
    return appManagedFormTypeActive.value && !explicitBusinessApplicationId.value
  })
  const businessObjectPickerVisible = computed(() => {
    return appManagedFormTypeActive.value
      && !explicitBusinessObjectCode.value
      && !routeQueryText(resolvedBusinessBinding.value?.objectCode)
  })

  const nodeFormAssetOptions = computed(() => {
    if (businessContextActive.value) {
      return businessFormAssets.value.map(asset => ({
        ...asset,
        label: `${asset.formName || asset.formKey}（${asset.formKey}）`,
        value: asset.formKey,
        formKey: asset.formKey,
        formName: asset.formName || asset.formKey,
        fieldCatalog: asset.fieldCatalog || [],
        formMode: asset.formMode || asset.type || 'BUSINESS_OBJECT_FORM',
        sourceType: asset.sourceType || asset.source || '',
        fieldCount: asset.fieldCount,
        fieldPreview: asset.fieldPreview || [],
        providerKey: asset.providerKey || '',
        providerName: asset.providerName || '',
        objectName: asset.objectName || '',
        source: asset.source || 'business',
      })).filter(item => item.value)
    }
    return formOptions.value.map(item => ({
      label: item.label || item.formKey || item.value,
      value: item.formKey || item.value,
      formKey: item.formKey || item.value,
      formName: item.label || item.formKey || item.value,
      currentVersionId: item.currentVersionId,
      fieldCatalog: [],
      source: 'flowForm',
    })).filter(item => item.value)
  })

  const businessGlobalFormRef = computed(() => {
    if (!businessFormConfigActive.value || !appManagedFormTypeActive.value)
      return {}
    return parseBusinessGlobalFormRef(modelInfo.formJson)
  })

  const businessGlobalFormNode = computed(() => {
    const ref = businessGlobalFormRef.value
    const asset = findBusinessFormAsset(
      ref.formKey,
      ref.providerKey,
      ref.formMode || ref.type || ref.formRef?.formMode || ref.formRef?.type,
    )
    return {
      formMode: ref.formMode || ref.type || asset?.formMode || asset?.type || 'BUSINESS_OBJECT_FORM',
      formKey: ref.formKey || '',
      formName: ref.formName || asset?.formName || '',
      providerKey: ref.providerKey || asset?.providerKey || '',
      formUrl: ref.formUrl || asset?.formUrl || '',
      viewKey: ref.viewKey || asset?.viewKey || 'default',
      formRef: ref.formRef || ref,
    }
  })

  const selectedBusinessGlobalFormAsset = computed(() => {
    if (!businessContextActive.value)
      return null
    const current = businessGlobalFormNode.value
    return findBusinessFormAsset(current.formKey, current.providerKey, current.formMode)
  })

  const formConfigStatus = computed(() => {
    if (businessFormConfigActive.value) {
      if (modelInfo.formType === 'none')
        return { label: '无表单', type: 'default' }
      if (modelInfo.formType === 'external') {
        return modelInfo.formUrl
          ? { label: '已配置', type: 'success' }
          : { label: '未配置', type: 'warning' }
      }
      if (!businessContextActive.value) {
        return effectiveApplicationId.value
          ? { label: '未选对象', type: 'warning' }
          : { label: '未选应用', type: 'warning' }
      }
      if (businessGlobalFormNode.value.formKey)
        return { label: '已配置', type: 'success' }
      return { label: '未配置', type: 'warning' }
    }
    if (modelInfo.formType === 'none')
      return { label: '无表单', type: 'default' }
    if (modelInfo.formType === 'external') {
      return modelInfo.formUrl
        ? { label: '已配置', type: 'success' }
        : { label: '未配置', type: 'warning' }
    }
    if (modelInfo.formId || modelInfo.formJson || formSchema.value.length)
      return { label: '已配置', type: 'success' }
    return { label: '未配置', type: 'warning' }
  })

  const notifySmsSelected = computed(() => Object.values(notifyMatrix)
    .some(channels => channels.includes('SMS')))

  const notificationConfigStatus = computed(() => {
    if (notifyMatrixDirty.value || notifyConfigInitialPresent.value)
      return { label: '已配置', type: 'success' }
    return { label: '默认', type: 'default' }
  })

  function normalizeNotifyChannels(channels = []) {
    const allowed = new Set(notifyChannelOrder)
    return [...new Set((Array.isArray(channels) ? channels : [])
      .map(channel => String(channel || '').trim().toUpperCase())
      .filter(channel => allowed.has(channel)))]
      .sort((a, b) => notifyChannelOrder.indexOf(a) - notifyChannelOrder.indexOf(b))
  }

  function parseNotifyConfig(value) {
    if (!value)
      return null
    if (typeof value === 'object')
      return value
    try {
      const parsed = JSON.parse(value)
      return parsed && typeof parsed === 'object' ? parsed : null
    }
    catch {
      return null
    }
  }

  function defaultNotifyChannels(eventKey) {
    if (eventKey === 'todo' && notifyChannels.value.some(channel => channel.channel === 'COLLABORATION'))
      return ['WEB', 'COLLABORATION']
    return eventKey === 'todo' ? ['WEB'] : []
  }

  function resetNotifyMatrix(rawConfig = null) {
    const parsed = parseNotifyConfig(rawConfig)
    notifyMatrixDirty.value = false
    notifyConfigInitialPresent.value = Boolean(parsed)
    for (const event of notifyEventDefinitions) {
      const hasConfig = Boolean(parsed && Object.prototype.hasOwnProperty.call(parsed, event.key))
      const config = hasConfig ? parsed[event.key] : null
      const channels = normalizeNotifyChannels(config?.channels)
      if (event.key === 'todo' && hasConfig && !channels.includes('WEB'))
        channels.unshift('WEB')
      notifyEventConfigured[event.key] = hasConfig && Array.isArray(config?.channels)
      notifyMatrix[event.key] = hasConfig ? channels : defaultNotifyChannels(event.key)
      notifyTemplateCodes[event.key] = typeof config?.templateCode === 'string' && config.templateCode.trim()
        ? config.templateCode.trim()
        : null
    }
  }

  function updateNotifyChannels(eventKey, channels) {
    const normalized = normalizeNotifyChannels(channels)
    if (eventKey === 'todo' && !normalized.includes('WEB'))
      normalized.unshift('WEB')
    notifyMatrix[eventKey] = normalized
    notifyEventConfigured[eventKey] = true
    notifyMatrixDirty.value = true
    hasChanges.value = true
  }

  function channelsForEvent(eventKey) {
    return notifyChannels.value.filter(channel => eventKey !== 'cc' || channel.channel !== 'SMS')
  }

  function notificationTemplatePrefix(eventKey) {
    return eventKey === 'todo' ? 'FLOW_TODO_CARD' : eventKey === 'result' ? 'FLOW_RESULT_CARD' : 'FLOW_CC_CARD'
  }

  function notificationTemplateOptions(eventKey) {
    const prefix = notificationTemplatePrefix(eventKey)
    const templates = notificationTemplates.value
      .filter(item => String(item.templateCode || '').startsWith(prefix))
      .map(item => ({
        label: `${item.templateName || item.templateCode}（${item.templateCode}）`,
        value: item.templateCode,
        template: item,
      }))
    const defaultCode = defaultNotificationTemplateCodes[eventKey]
    if (!templates.some(item => item.value === defaultCode)) {
      templates.unshift({
        label: `系统默认模板（${defaultCode}）`,
        value: defaultCode,
        template: { templateCode: defaultCode, templateName: '系统默认模板' },
      })
    }
    return templates
  }

  function selectedNotificationTemplateValue(eventKey) {
    return notifyTemplateCodes[eventKey] || defaultNotificationTemplateCodes[eventKey]
  }

  function notificationTemplateSummary(eventKey) {
    const code = selectedNotificationTemplateValue(eventKey)
    const option = notificationTemplateOptions(eventKey).find(item => item.value === code)
    return option?.label || code || '系统默认模板'
  }

  function updateNotificationTemplate(eventKey, value) {
    const defaultCode = defaultNotificationTemplateCodes[eventKey]
    notifyTemplateCodes[eventKey] = value && value !== defaultCode ? value : null
    notifyEventConfigured[eventKey] = true
    notifyMatrixDirty.value = true
    hasChanges.value = true
  }

  async function loadNotificationMetadata(force = false) {
    if ((notifyChannelsLoading.value || notificationTemplatesLoading.value)
      || (!force && notifyChannelsLoaded.value && notificationTemplatesLoaded.value)) {
      return
    }
    notifyChannelsLoading.value = true
    notificationTemplatesLoading.value = true
    try {
      const [channelResult, templateResult] = await Promise.allSettled([
        flowApi.getFlowNotifyChannels(),
        messageApi.getTemplatePage({ type: 'SYSTEM', keyword: 'FLOW_', pageNum: 1, pageSize: 100 }),
      ])
      const channelRes = channelResult.status === 'fulfilled' ? channelResult.value : null
      const templateRes = templateResult.status === 'fulfilled' ? templateResult.value : null
      if (channelRes?.code === 200 && Array.isArray(channelRes.data)) {
        notifyChannels.value = channelRes.data.filter(item => item?.channel && item?.name)
        notifyChannelsLoaded.value = true
        if (!notifyMatrixDirty.value && !modelInfo.notifyConfig)
          resetNotifyMatrix(null)
      }
      if (templateRes?.code === 200) {
        notificationTemplates.value = templateRes.data?.records || []
        notificationTemplatesLoaded.value = true
      }
      if (channelResult.status === 'rejected')
        console.warn('[FlowDesign] 加载通知渠道失败:', channelResult.reason?.message || channelResult.reason)
      if (templateResult.status === 'rejected')
        console.warn('[FlowDesign] 加载通知模板失败:', templateResult.reason?.message || templateResult.reason)
    }
    catch (error) {
      console.warn('[FlowDesign] 加载通知渠道或模板失败:', error?.message || error)
    }
    finally {
      notifyChannelsLoading.value = false
      notificationTemplatesLoading.value = false
    }
  }

  function buildNotifyConfig() {
    if (!notifyMatrixDirty.value && !modelInfo.notifyConfig)
      return null
    if (!notifyMatrixDirty.value && modelInfo.notifyConfig)
      return typeof modelInfo.notifyConfig === 'string' ? modelInfo.notifyConfig : JSON.stringify(modelInfo.notifyConfig)

    const config = {}
    for (const event of notifyEventDefinitions) {
      if (!notifyEventConfigured[event.key])
        continue
      const channels = normalizeNotifyChannels(notifyMatrix[event.key])
      if (!channels.length)
        continue
      config[event.key] = { channels }
      if (notifyTemplateCodes[event.key])
        config[event.key].templateCode = notifyTemplateCodes[event.key]
    }
    if (!Object.keys(config).length)
      return null

    const todoChannels = normalizeNotifyChannels(config.todo?.channels)
    const collaborationAvailable = notifyChannels.value.some(channel => channel.channel === 'COLLABORATION')
    const isLegacyDefault = !notifyConfigInitialPresent.value
      && Object.keys(config).length === 1
      && !notifyTemplateCodes.todo
      && !notifyTemplateCodes.result
      && !notifyTemplateCodes.cc
      && todoChannels.join(',') === (collaborationAvailable ? 'WEB,COLLABORATION' : 'WEB')
    return isLegacyDefault ? null : JSON.stringify(config)
  }

  async function previewNotificationTemplate(eventKey) {
    const code = selectedNotificationTemplateValue(eventKey)
    if (!code)
      return
    let template = notificationTemplates.value.find(item => item.templateCode === code)
    if (!template || !template.contentTemplate) {
      try {
        const res = await messageApi.getTemplateByCode(code)
        if (res.code === 200 && res.data) {
          template = res.data
          notificationTemplates.value = [
            ...notificationTemplates.value.filter(item => item.templateCode !== code),
            template,
          ]
        }
      }
      catch (error) {
        console.warn('[FlowDesign] 加载通知模板详情失败:', code, error?.message || error)
      }
    }
    notificationPreview.value = template || {
      templateCode: code,
      templateName: '系统默认模板',
      titleTemplate: eventKey === 'result' ? '流程审批结果通知' : eventKey === 'cc' ? '流程抄送通知' : '您有新的流程待办',
      contentTemplate: eventKey === 'result'
        ? '<div class="gray">流程审批结果通知</div><div class="normal">流程：$' + '{processName}</div><div class="normal">结果：$' + '{result}</div>'
        : eventKey === 'cc'
          ? '<div class="gray">流程抄送通知</div><div class="normal">流程：$' + '{processName}</div>'
          : '<div class="gray">流程待办提醒</div><div class="normal">任务：$' + '{taskTitle}</div><div class="normal">流程：$' + '{processName}</div><div class="normal">发起人：$' + '{startUserName}</div>',
    }
    showNotificationTemplatePreview.value = true
  }

  function renderNotificationTemplate(value) {
    const sample = {
      taskTitle: '采购单审批',
      processName: modelInfo.modelName || '示例流程',
      startUserName: '张三',
      applyUserName: '张三',
      result: '已通过',
      businessKey: 'demo:1001',
      processInstanceId: 'demo-process-001',
      url: '#',
    }
    return String(value || '').replace(/\$?\{([^}]+)\}/g, (_, key) => sample[key.trim()] ?? `{{${key.trim()}}}`)
  }

  const autoApprovalModeLabel = computed(() => {
    return autoApprovalModeOptions.value.find(item => item.value === processConfig.value.autoApprovalMode)?.label || processConfig.value.autoApprovalMode
  })

  const isAiPanelActive = computed(() => workspaceMode.value === 'settings' && rightActiveTab.value === 'ai')

  const settingsTreeGroups = computed(() => [
    {
      label: '基础设置',
      children: [
        {
          key: 'flow',
          label: '流程属性',
          desc: '分类 / 说明',
          icon: 'i-material-symbols:tune',
        },
        {
          key: 'form',
          label: '表单配置',
          desc: '发起表单',
          icon: 'i-material-symbols:dynamic-form',
        },
        ...(isApprovalDesigner.value
          ? [{
              key: 'approval',
              label: '审批与待办',
              desc: '退回 / 撤回 / 待办跳转',
              icon: 'i-material-symbols:approval-delegation-outline',
            }]
          : []),
        {
          key: 'notification',
          label: '通知与推送',
          desc: '渠道 / 模板 / 预览',
          icon: 'i-material-symbols:notifications-outline',
        },
        {
          key: 'description',
          label: '说明',
          desc: '业务备注',
          icon: 'i-material-symbols:notes',
        },
      ],
    },
    {
      label: '增强能力',
      children: [
        {
          key: 'ai',
          label: 'AI助手',
          desc: '生成 / 修改流程',
          icon: 'i-material-symbols:auto-awesome',
        },
      ],
    },
  ])

  function setWorkspaceMode(mode) {
    workspaceMode.value = mode
  }

  function openSettingsPanel(key) {
    workspaceMode.value = 'settings'
    rightActiveTab.value = key
  }

  function getSettingsBadge(key) {
    if (key === 'form')
      return formConfigStatus.value.label
    if (key === 'notification')
      return notificationConfigStatus.value.label
    if (key === 'approval')
      return processConfig.value.autoApprovalMode === 'none' ? '人工审批' : '自动'
    if (key === 'ai' && aiSending.value)
      return '生成中'
    return ''
  }

  function getSettingsBadgeClass(key) {
    if (key === 'form')
      return `is-${formConfigStatus.value.type}`
    if (key === 'notification')
      return `is-${notificationConfigStatus.value.type}`
    if (key === 'approval')
      return processConfig.value.autoApprovalMode === 'none' ? 'is-default' : 'is-info'
    if (key === 'ai')
      return 'is-info'
    return ''
  }

  watch(aiProviderId, async (val, old) => {
    if (val && val !== old) {
      await loadModelOptions(val, false)
    }
  })

  watch(
    () => [modelInfo.allowSubmitterWithdraw, modelInfo.allowMultiReturn, modelInfo.autoApprovalMode, modelInfo.todoDetailUrlTemplate],
    () => {
      if (!syncingModel.value && !pageLoading.value)
        hasChanges.value = true
    },
  )

  watch(
    () => modelInfo.designerType,
    (value) => {
      const normalized = normalizeDesignerType(value)
      if (value !== normalized) {
        modelInfo.designerType = normalized
        return
      }
      dockedElement.value = null
      modelerInstance.value = null
      if (normalized === 'business' && rightActiveTab.value === 'approval')
        rightActiveTab.value = 'flow'
    },
  )

  function getRouteModelId() {
    const id = route.query.id
    return Array.isArray(id) ? id[0] : id
  }

  function resetNewModelState() {
    Object.assign(modelInfo, {
      id: '',
      modelName: '新流程',
      modelKey: `process_${Date.now()}`,
      category: '',
      flowType: '',
      designerType: 'approval',
      allowSubmitterWithdraw: true,
      allowMultiReturn: false,
      autoApprovalMode: 'none',
      formType: 'dynamic',
      formId: null,
      formUrl: '',
      todoDetailUrlTemplate: DEFAULT_TODO_DETAIL_URL_TEMPLATE,
      formJson: '',
      description: '',
      status: 0,
      version: 1,
      startListener: '',
      endListener: '',
      notifyType: 'redis',
      notifyConfig: null,
    })
    resetNotifyMatrix(null)
    bpmnXml.value = ''
    formSchema.value = []
    formFieldCatalog.value = []
    businessFormAssets.value = []
    resolvedBusinessBinding.value = null
    manualBusinessApplicationId.value = ''
    manualBusinessApplicationName.value = ''
    manualBusinessObjectCode.value = ''
    manualBusinessObjectName.value = ''
    businessApplicationOptions.value = []
    businessObjectOptions.value = []
    businessObjectOptionsApplicationId.value = ''
    dockedElement.value = null
    modelerInstance.value = null
    workspaceMode.value = 'design'
    rightActiveTab.value = 'flow'
  }

  onMounted(async () => {
    try {
      await loadCategories()
      await loadForms()
      await loadProviderOptions()

      const modelId = props.modelId || getRouteModelId()
      if (modelId) {
        await loadModel(modelId)
      }
      else {
        resetNewModelState()
      }
      await loadNotificationMetadata()
    }
    finally {
      pageLoading.value = false
    }
  })

  watch(() => route.query.id, async (value, oldValue) => {
    if (props.embedded || value === oldValue)
      return

    pageLoading.value = true
    try {
      const modelId = getRouteModelId()
      if (modelId) {
        await loadModel(modelId)
      }
      else {
        resetNewModelState()
      }
      await nextTick()
      await modelerRef.value?.setXML?.(bpmnXml.value || '')
      hasChanges.value = false
    }
    finally {
      pageLoading.value = false
    }
  })

  watch(() => props.modelId, async (value, oldValue) => {
    if (!props.embedded || !value || value === oldValue)
      return
    pageLoading.value = true
    try {
      await loadModel(value)
      await nextTick()
      await modelerRef.value?.setXML?.(bpmnXml.value || '')
      hasChanges.value = false
    }
    finally {
      pageLoading.value = false
    }
  })

  watch(businessObjectCode, async (value, oldValue) => {
    if (value === oldValue || !modelInfo.id || syncingModel.value)
      return
    await refreshFormFieldCatalog()
  })

  watch(effectiveApplicationId, async (value, oldValue) => {
    if (value === oldValue || !modelInfo.id || syncingModel.value)
      return
    await refreshBusinessFormFieldCatalog()
  })

  onUnmounted(() => {
    if (!props.embedded)
      tabStore.setTabDirty(route.fullPath, false)
    if (mut.businessSelectionClearTimer) {
      clearTimeout(mut.businessSelectionClearTimer)
      mut.businessSelectionClearTimer = null
    }
    if (aiAbortController.value) {
      aiAbortController.value.abort()
      aiAbortController.value = null
    }
  })

  async function loadProviderOptions(preserveSelection = false) {
    try {
      const res = await providerPage({ pageNum: 1, pageSize: 100 })
      const records = (res.data?.records || []).filter(item => item.status === undefined || item.status === '0')
      providerOptions.value = records.map(item => ({
        label: item.providerName,
        value: item.id,
      }))

      const hasSelectedProvider = providerOptions.value.some(item => item.value === aiProviderId.value)
      if (!hasSelectedProvider) {
        aiProviderId.value = null
      }

      if (!aiProviderId.value && records.length > 0) {
        const defaultProvider = records.find(item => item.isDefault === '1') || records[0]
        aiProviderId.value = defaultProvider?.id || null
      }

      await loadModelOptions(aiProviderId.value, preserveSelection)
    }
    catch (e) {
      console.warn('[FlowDesign] 加载供应商列表失败:', e.message)
      providerOptions.value = []
    }
  }

  async function loadModelOptions(selectedProviderId, preserveSelection = false) {
    if (!selectedProviderId) {
      modelOptions.value = []
      aiModelId.value = null
      return
    }
    try {
      const res = await modelListByProvider(selectedProviderId)
      const models = (res.data || []).filter(item => item.status === undefined || item.status === '0')
      modelOptions.value = models.map(item => ({
        label: item.modelName ? `${item.modelName} (${item.modelId})` : item.modelId,
        value: item.modelId,
        modelCode: item.modelId,
        maxTokens: item.maxTokens,
        isDefault: item.isDefault,
      }))

      const hasSelectedModel = modelOptions.value.some(item => item.value === aiModelId.value)
      if (!preserveSelection || !hasSelectedModel) {
        const defaultModel = modelOptions.value.find(item => item.isDefault === '1') || modelOptions.value[0]
        aiModelId.value = defaultModel?.value || null
      }
    }
    catch (e) {
      console.warn('[FlowDesign] 加载模型列表失败:', e.message)
      modelOptions.value = []
      aiModelId.value = null
    }
  }

  async function loadCategories() {
    try {
      const res = await flowApi.getCategoryTreeSelect(false)
      if (res.code === 200) {
        categoryTreeOptions.value = buildFlowCategoryTreeOptions(res.data || [])
      }
    }
    catch (error) {
      console.error('加载分类失败:', error)
    }
  }

  async function loadForms() {
    try {
      const res = await flowApi.getEnabledForms()
      if (res.code === 200) {
        formOptions.value = (res.data || []).map(item => ({
          label: item.formName,
          value: item.id,
          formKey: item.formKey,
          currentVersionId: item.currentVersionId,
        }))
      }
    }
    catch (error) {
      console.error('加载表单列表失败:', error)
    }
  }

  async function refreshFormFieldCatalog(formDetail = null) {
    formFieldCatalogError.value = ''
    if (modelInfo.formType === 'external') {
      await refreshExternalFormFieldCatalog()
      return
    }
    if (businessContextActive.value) {
      await refreshBusinessFormFieldCatalog()
      return
    }
    businessFormAssets.value = []
    if (appManagedFormTypeActive.value) {
      formFieldCatalog.value = []
      return
    }
    if (modelInfo.formType !== 'dynamic') {
      formFieldCatalog.value = []
      return
    }

    let detail = formDetail
    if (!detail && modelInfo.formId) {
      const option = formOptions.value.find(item => item.value === modelInfo.formId)
      detail = option ? { formKey: option.formKey, currentVersionId: option.currentVersionId } : null
    }

    const formKey = detail?.formKey
    const versionId = detail?.currentVersionId
    if (formKey || versionId) {
      try {
        const res = await flowApi.getFormFieldCatalog({
          formKey,
          versionId,
          modelKey: modelInfo.modelKey,
        })
        if (res.code === 200) {
          const remoteCatalog = res.data || []
          formFieldCatalog.value = remoteCatalog.length ? remoteCatalog : resolveLocalFormFieldCatalog()
          if (remoteCatalog.length)
            return
        }
      }
      catch (error) {
        console.warn('[FlowDesign] 加载表单字段目录失败:', error?.message || error)
      }
    }

    formFieldCatalog.value = resolveLocalFormFieldCatalog()
  }

  async function refreshExternalFormFieldCatalog() {
    const formUrl = String(modelInfo.formUrl || '').trim()
    if (!formUrl) {
      formFieldCatalog.value = []
      return
    }
    try {
      const catalog = await loadFlowBusinessFormFieldCatalog(formUrl)
      formFieldCatalog.value = catalog
      if (!catalog.length)
        formFieldCatalogError.value = '该外置表单尚未登记字段目录，请在表单组件中配置 flowFieldCatalog'
    }
    catch (error) {
      formFieldCatalog.value = []
      formFieldCatalogError.value = error?.message || '外置表单字段目录加载失败'
    }
  }

  async function loadBusinessApplicationOptions() {
    if (businessApplicationOptions.value.length || businessApplicationLoading.value)
      return
    businessApplicationLoading.value = true
    try {
      const res = await businessApplicationList({})
      const list = Array.isArray(res.data) ? res.data : []
      const seen = new Set()
      businessApplicationOptions.value = list
        .filter((item) => {
          const applicationId = routeQueryText(item?.id)
          if (!applicationId || seen.has(applicationId))
            return false
          seen.add(applicationId)
          return true
        })
        .map(item => ({
          label: `${item.applicationName || item.applicationCode || item.id}${item.applicationCode ? `（${item.applicationCode}）` : ''}`,
          value: routeQueryText(item.id),
          application: item,
        }))

      // 已保存的应用可能已停用，仍保留当前值以便用户明确替换它。
      const persistedApplicationId = effectiveApplicationId.value
      if (persistedApplicationId && !seen.has(persistedApplicationId)) {
        const persistedRef = mut.persistedBusinessFormRef.value
        const persistedApplicationName = routeQueryText(persistedRef.applicationName)
          || routeQueryText(resolvedBusinessBinding.value?.applicationName)
          || persistedApplicationId
        businessApplicationOptions.value.unshift({
          label: `${persistedApplicationName}（当前已保存）`,
          value: persistedApplicationId,
          application: {
            id: persistedApplicationId,
            applicationName: persistedApplicationName,
          },
        })
      }
    }
    catch (error) {
      console.warn('[FlowDesign] 加载业务应用列表失败:', error?.message || error)
      businessApplicationOptions.value = []
    }
    finally {
      businessApplicationLoading.value = false
    }
  }

  async function loadBusinessObjectOptions() {
    const applicationId = effectiveApplicationId.value
    if (!applicationId || businessObjectLoading.value)
      return
    if (businessObjectOptionsApplicationId.value === applicationId)
      return

    businessObjectLoading.value = true
    try {
      const res = await businessApplicationObjects(applicationId)
      const list = Array.isArray(res.data) ? res.data : []
      const seen = new Set()
      businessObjectOptions.value = list
        .filter((item) => {
          const objectCode = routeQueryText(item?.objectCode)
          if (!objectCode || seen.has(objectCode))
            return false
          seen.add(objectCode)
          return true
        })
        .map(item => ({
          label: `${item.objectName || item.objectCode}（${item.objectCode}）`,
          value: item.objectCode,
          object: item,
        }))
      businessObjectOptionsApplicationId.value = applicationId
    }
    catch (error) {
      console.warn('[FlowDesign] 加载应用业务对象列表失败:', error?.message || error)
      businessObjectOptions.value = []
    }
    finally {
      businessObjectLoading.value = false
    }
  }

  async function handleBusinessApplicationChange(value) {
    const applicationId = routeQueryText(value)
    const selected = businessApplicationOptions.value.find(item => item.value === applicationId)?.application
    const canChangeObject = businessObjectPickerVisible.value

    manualBusinessApplicationId.value = applicationId
    manualBusinessApplicationName.value = routeQueryText(selected?.applicationName) || applicationId
    businessObjectOptions.value = []
    businessObjectOptionsApplicationId.value = ''
    if (canChangeObject) {
      manualBusinessObjectCode.value = ''
      manualBusinessObjectName.value = ''
    }
    modelInfo.formType = 'business'
    modelInfo.formId = null
    modelInfo.formUrl = ''
    modelInfo.formJson = ''
    formSchema.value = []
    businessFormAssets.value = []
    formFieldCatalog.value = []

    if (applicationId) {
      await loadBusinessObjectOptions()
      if (!canChangeObject && businessObjectCode.value && !modelInfo.id)
        await refreshBusinessFormFieldCatalog()
    }
    hasChanges.value = true
  }

  __impl.normalizeNotifyChannels = normalizeNotifyChannels
  __impl.parseNotifyConfig = parseNotifyConfig
  __impl.defaultNotifyChannels = defaultNotifyChannels
  __impl.resetNotifyMatrix = resetNotifyMatrix
  __impl.updateNotifyChannels = updateNotifyChannels
  __impl.channelsForEvent = channelsForEvent
  __impl.notificationTemplatePrefix = notificationTemplatePrefix
  __impl.notificationTemplateOptions = notificationTemplateOptions
  __impl.selectedNotificationTemplateValue = selectedNotificationTemplateValue
  __impl.notificationTemplateSummary = notificationTemplateSummary
  __impl.updateNotificationTemplate = updateNotificationTemplate
  __impl.loadNotificationMetadata = loadNotificationMetadata
  __impl.buildNotifyConfig = buildNotifyConfig
  __impl.previewNotificationTemplate = previewNotificationTemplate
  __impl.renderNotificationTemplate = renderNotificationTemplate
  __impl.setWorkspaceMode = setWorkspaceMode
  __impl.openSettingsPanel = openSettingsPanel
  __impl.getSettingsBadge = getSettingsBadge
  __impl.getSettingsBadgeClass = getSettingsBadgeClass
  __impl.getRouteModelId = getRouteModelId
  __impl.resetNewModelState = resetNewModelState
  __impl.loadProviderOptions = loadProviderOptions
  __impl.loadModelOptions = loadModelOptions
  __impl.loadCategories = loadCategories
  __impl.loadForms = loadForms
  __impl.refreshFormFieldCatalog = refreshFormFieldCatalog
  __impl.refreshExternalFormFieldCatalog = refreshExternalFormFieldCatalog
  __impl.loadBusinessApplicationOptions = loadBusinessApplicationOptions
  __impl.loadBusinessObjectOptions = loadBusinessObjectOptions
  __impl.handleBusinessApplicationChange = handleBusinessApplicationChange

  return {
    props, emit, __impl, mut, applyProcessConfigFromXml, applyProcessConfigToXml, buildBusinessGlobalFormJson, buildDiagramXml,
    buildNotifyConfig, buildWaypoints, channelsForEvent, collectBusinessAssetFields, decodeHtmlEntities, defaultNotifyChannels, ensureBusinessGlobalFormSelection, ensureDiagramNamespaces,
    ensureProcessId, escapeXmlAttr, extractBpmnXml, extractJsonObject, extractProcessConfigFromXml, findBusinessFormAsset, findBusinessFormAssetInList, findElementByLocalName,
    findElementsByLocalName, getBpmnDisplayIssue, getBpmnNodeSize, getElementIcon, getElementTitle, getRouteModelId, getSettingsBadge, getSettingsBadgeClass,
    getXmlForSave, getXmlParseError, goBackToBusinessApp, handleAbortAi, handleAiSend, handleApplyAiDraft, handleBack, handleBpmnChange,
    handleBusinessApplicationChange, handleBusinessElementSelect, handleBusinessGlobalFormUpdate, handleBusinessManagedFormTypeChange, handleBusinessObjectChange, handleBusinessPanelClose, handleDeploy, handleDiagramImportEnd,
    handleDiagramImportStart, handleFlowSSEChunk, handleFlowSSEComplete, handleFlowSSEError, handleFormSelect, handleFormTypeChange, handleModelerReady, handleNewAiSession,
    handleOpenVersionHistory, handlePreviewAiXml, handleSaveDraft, handleSaveFormSchema, handleVersionHistoryRefresh, hasBpmnDiagram, isAppManagedFormType, isBpmnFlowNode,
    layoutBpmnNodes, loadBusinessApplicationOptions, loadBusinessObjectOptions, loadCategories, loadForms, loadModel, loadModelOptions, loadNotificationMetadata,
    loadProviderOptions, normalizeAiBpmnXml, normalizeAiFlowDraft, normalizeAppManagedFormType, normalizeAutoApprovalMode, normalizeBpmnId, normalizeBusinessEntryRoute, normalizeBusinessFieldCatalog,
    normalizeBusinessFormAssets, normalizeBusinessFormMode, normalizeBusinessGlobalFormBeforeSave, normalizeDesignerType, normalizeNotifyChannels, normalizeRejectStrategy, notificationTemplateOptions, notificationTemplatePrefix,
    notificationTemplateSummary, openSettingsPanel, parseAiFlowResponse, parseBooleanWithDefault, parseBusinessGlobalFormRef, parseNotifyConfig, parseXmlDocument, previewNotificationTemplate,
    readFlowableAttr, rebuildDiagramInfo, refreshBusinessFormFieldCatalog, refreshExternalFormFieldCatalog, refreshFormFieldCatalog, renderNotificationTemplate, repairBpmnXml, resetNewModelState,
    resetNotifyMatrix, resolveBusinessBindingForModel, resolveLocalFormFieldCatalog, routeQueryText, scrollActiveReasoningToBottom, scrollAiToBottom, selectedNotificationTemplateValue, setReasoningContentRef,
    setWorkspaceMode, stripCodeFence, stripDiagramInfo, syncSafeModelKey, toggleAiPanel, toggleReasoning, updateLastAiAssistantMessage, updateNotificationTemplate,
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
    sanitizeHtml,
  }
}
