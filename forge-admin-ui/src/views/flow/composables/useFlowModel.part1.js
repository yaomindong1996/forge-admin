/** model.vue setup part 1. */
import { CopyOutline, CreateOutline, PauseCircleOutline, PlayCircleOutline, TimeOutline, TrashOutline } from '@vicons/ionicons5'
import { NIcon, NModal, NTreeSelect } from 'naive-ui'
import { computed, defineAsyncComponent, h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { businessFlowFormAssets, businessFlowModelBindings } from '@/api/business-app'
import flowApi from '@/api/flow'
import AiForm from '@/components/ai-form/AiForm.vue'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import { useDict } from '@/composables/useDict'
import { collectInitiatorSelectSelections } from '@/utils/initiatorSelect'
import DesignerAsyncLoader from '@/views/app-center/components/designer/DesignerAsyncLoader.vue'
import { buildFlowCategoryTreeOptions, resolveFlowCategoryLabel, resolveFlowCategoryValue } from '../utils/categoryOptions'
export function applyFlowModelPart1() {
  const __impl = {}
  const mut = {}
  const handleActivate = (...args) => __impl.handleActivate(...args)
  const handleDelete = (...args) => __impl.handleDelete(...args)
  const handleSuspend = (...args) => __impl.handleSuspend(...args)
  const handleVersionHistory = (...args) => __impl.handleVersionHistory(...args)

  const router = useRouter()
  const DEFAULT_TODO_DETAIL_URL_TEMPLATE = '/#/pages/todo-detail?taskId={taskId}'
  const { dict, getLabel } = useDict('flow_model_status', 'flow_process_form_type', 'flow_designer_type')

  const FlowDesignAsyncLoader = {
    name: 'FlowDesignAsyncLoader',
    setup() {
      return () => h(DesignerAsyncLoader, {
        title: '正在打开流程设计器',
        description: '首次加载需要准备流程画布与属性面板资源',
        overlay: true,
      })
    },
  }

  const FlowFormRendererAsyncLoader = {
    name: 'FlowFormRendererAsyncLoader',
    setup() {
      return () => h(DesignerAsyncLoader, {
        title: '正在加载测试表单',
        description: '首次打开需要准备表单渲染资源',
        overlay: true,
      })
    },
  }

  const FlowModalAsyncLoader = {
    name: 'FlowModalAsyncLoader',
    setup() {
      return () => h(NModal, {
        show: true,
        preset: 'card',
        title: '正在加载',
        style: 'width: min(560px, calc(100vw - 32px))',
        maskClosable: false,
      }, {
        default: () => h(DesignerAsyncLoader, {
          title: '正在加载版本历史',
          description: '首次打开需要准备流程图查看资源',
          overlay: true,
        }),
      })
    },
  }

  const FlowDesignPage = defineAsyncComponent({
    loader: () => import('../design.vue'),
    loadingComponent: FlowDesignAsyncLoader,
    delay: 120,
    suspensible: false,
  })
  const FlowFormCreateRenderer = defineAsyncComponent({
    loader: () => import('@/components/form-create/FlowFormCreateRenderer.vue'),
    loadingComponent: FlowFormRendererAsyncLoader,
    delay: 120,
    suspensible: false,
  })
  const VersionHistory = defineAsyncComponent({
    loader: () => import('../version.vue'),
    loadingComponent: FlowModalAsyncLoader,
    delay: 120,
    suspensible: false,
  })

  const statusOptions = computed(() => toNumberOptions(dict.value.flow_model_status))
  const categoryTreeOptions = ref([])
  const designerTypePresentation = {
    approval: {
      icon: 'i-material-symbols:approval-delegation-outline',
      desc: '适合人员审批、条件分支、抄送和表单权限配置。',
    },
    business: {
      icon: 'i-material-symbols:account-tree-outline',
      desc: '适合完整 BPMN 工作流、服务任务、事件和复杂业务编排。',
    },
  }
  const designerTypeOptions = computed(() => (dict.value.flow_designer_type || []).map(item => ({
    ...item,
    ...designerTypePresentation[item.value],
  })))

  function statusClass(status) {
    const cls = { 0: 'designing', 1: 'deployed', 2: 'suspended', 3: 'disabled' }
    return cls[status] || 'default'
  }

  function toNumberOptions(options = []) {
    return options.map(item => ({
      ...item,
      value: Number(item.value),
    }))
  }

  function formatDate(d) {
    if (!d)
      return ''
    return d.slice(0, 10)
  }

  function normalizeDesignerType(value) {
    return value === 'business' ? 'business' : 'approval'
  }

  function designerTypeLabel(value) {
    const normalizedValue = normalizeDesignerType(value)
    return designerTypeOptions.value.find(item => item.value === normalizedValue)?.label || normalizedValue
  }

  function designerTypeClass(value) {
    return normalizeDesignerType(value) === 'business' ? 'business' : 'approval'
  }

  function getCategoryDisplayName(row) {
    return row?.categoryName || resolveFlowCategoryLabel(row?.category, categoryTreeOptions.value, '')
  }

  function isCodeAppBinding(binding) {
    const value = binding?.codeApp
    if (value === true || value === 1)
      return true
    return ['true', '1', 'yes', 'y'].includes(String(value || '').trim().toLowerCase())
  }

  const modelActionLocks = ref(new Set())

  function modelActionKey(row, action) {
    return `${String(row?.id || row?.modelKey || '')}:${action}`
  }

  function isModelActionLocked(row, action) {
    return modelActionLocks.value.has(modelActionKey(row, action))
  }

  function isModelActionBusy(row) {
    const modelId = String(row?.id || row?.modelKey || '')
    return [...modelActionLocks.value].some(key => key.startsWith(`${modelId}:`))
  }

  function lockModelAction(row, action) {
    const key = modelActionKey(row, action)
    if (modelActionLocks.value.has(key))
      return null
    modelActionLocks.value = new Set(modelActionLocks.value).add(key)
    return key
  }

  function unlockModelAction(key) {
    if (!key)
      return
    const next = new Set(modelActionLocks.value)
    next.delete(key)
    modelActionLocks.value = next
  }

  function getActionOptions(row) {
    const renderIcon = (icon) => {
      return () => h(NIcon, null, { default: () => h(icon) })
    }
    const opts = [
      { label: '编辑信息', key: 'edit', icon: renderIcon(CreateOutline), disabled: isModelActionBusy(row) },
      { label: '版本历史', key: 'versionHistory', icon: renderIcon(TimeOutline), disabled: isModelActionBusy(row) },
      { label: '复制模型', key: 'copy', icon: renderIcon(CopyOutline), disabled: isModelActionLocked(row, 'copy') },
    ]
    if (row.status === 1) {
      opts.splice(1, 0, { label: '发起测试', key: 'startTest', icon: renderIcon(PlayCircleOutline), disabled: isModelActionBusy(row) })
      opts.push({ label: '挂起', key: 'suspend', icon: renderIcon(PauseCircleOutline), disabled: isModelActionLocked(row, 'suspend') })
    }
    if (row.status === 2) {
      opts.push({ label: '激活', key: 'activate', icon: renderIcon(PlayCircleOutline), disabled: isModelActionLocked(row, 'activate') })
    }
    opts.push({ type: 'divider', key: 'd1' })
    opts.push({ label: '删除', key: 'delete', icon: renderIcon(TrashOutline), disabled: isModelActionLocked(row, 'delete'), props: { style: 'color: #d03050' } })
    return opts
  }

  function handleActionSelect(key, row) {
    const map = { edit: handleEdit, startTest: handleStartTest, copy: handleCopy, versionHistory: handleVersionHistory, suspend: handleSuspend, activate: handleActivate, delete: handleDelete }
    map[key]?.(row)
  }

  const queryParams = reactive({ modelName: '', category: null, status: null })
  const activeStatsStatus = computed(() => queryParams.status ?? 'all')
  const dataSource = ref([])
  const loading = ref(false)
  const sortMode = ref(false)
  const sortSaving = ref(false)
  const draggingModelId = ref('')
  const pagination = reactive({ page: 1, pageSize: 12, itemCount: 0 })
  const showVersionHistory = ref(false)
  const currentModelId = ref('')
  const currentModelVersion = ref(null)
  const showDesignModal = ref(false)
  const currentDesignModelId = ref('')
  const currentDesignBinding = ref(null)
  const showStartTestModal = ref(false)
  const startTestLoading = ref(false)
  const startTestFormRef = ref(null)
  const startTestFormData = ref({})
  const startTestFormSchema = ref([])
  const startTestBusinessFormActive = ref(false)
  const startTestBusinessFormLoading = ref(false)
  const startTestBusinessFormAssets = ref([])
  const startTestBusinessFormLayout = reactive({
    gridCols: 1,
    labelPlacement: 'left',
    labelWidth: '100',
  })
  const startTestApproverNodes = ref([])
  const startTestApproverSelections = ref({})
  const startTestApproverLabels = ref({})
  const startTestPreflightDiagnostics = ref([])
  const currentStartModel = ref(null)
  const startTestTitle = computed(() => `发起测试 - ${currentStartModel.value?.modelName || '流程模型'}`)
  const startTestAlert = computed(() => {
    if (!currentStartModel.value)
      return null
    if (currentStartModel.value.status !== 1) {
      return { type: 'warning', text: '当前模型尚未部署，部署后才能发起测试流程。' }
    }
    if (currentStartModel.value.formType === 'external') {
      return { type: 'warning', text: '当前模型使用外置表单，测试工具不会渲染外置页面，将以空变量发起。' }
    }
    if (startTestBusinessFormActive.value) {
      return { type: 'info', text: '测试发起会使用当前业务应用表单收集变量；测试流程不创建真实业务单据。' }
    }
    if (!startTestFormSchema.value.length) {
      return { type: 'info', text: '当前模型没有动态表单字段，发起后仅使用系统内置流程变量。' }
    }
    return { type: 'info', text: '这是流程模型测试入口，只用于验证流程流转；正式业务入口仍由低代码应用或业务页面承载。' }
  })

  const totalCount = ref(0)
  const designingCount = ref(0)
  const deployedCount = ref(0)
  const suspendedCount = ref(0)
  const disabledCount = ref(0)

  async function fetchCategories() {
    try {
      const res = await flowApi.getCategoryTreeSelect(false)
      if (res.code === 200) {
        categoryTreeOptions.value = buildFlowCategoryTreeOptions(res.data || [])
      }
    }
    catch {
      console.error('加载分类失败')
    }
  }

  async function fetchData() {
    if (sortMode.value)
      return
    loading.value = true
    try {
      await Promise.all([
        fetchModelPage(),
        fetchModelStatistics(),
      ])
    }
    catch {
      console.error('加载模型列表失败')
    }
    finally {
      loading.value = false
    }
  }

  async function fetchModelPage() {
    const res = await flowApi.getModelPage({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      ...queryParams,
    })
    if (res.code === 200) {
      const records = res.data?.records || []
      dataSource.value = await enrichModelBusinessBindings(records)
      pagination.itemCount = res.data?.total || 0
    }
  }

  async function fetchModelStatistics() {
    const res = await flowApi.getModelStatistics({
      modelName: queryParams.modelName,
      category: queryParams.category,
    })
    if (res.code === 200) {
      applyModelStatistics(res.data || {})
    }
  }

  function applyModelStatistics(data = {}) {
    totalCount.value = toCount(data.total)
    designingCount.value = toCount(data.designing)
    deployedCount.value = toCount(data.deployed)
    suspendedCount.value = toCount(data.suspended)
    disabledCount.value = toCount(data.disabled)
  }

  function toCount(value) {
    const count = Number(value)
    return Number.isFinite(count) ? count : 0
  }

  function handlePageSizeChange(v) {
    if (sortMode.value)
      return
    pagination.pageSize = v
    pagination.page = 1
    fetchData()
  }

  function handleSearch() {
    if (sortMode.value)
      return
    pagination.page = 1
    fetchData()
  }

  function handleStatusSelect() {
    if (sortMode.value)
      return
    pagination.page = 1
    fetchData()
  }

  function handleReset() {
    if (sortMode.value)
      return
    Object.assign(queryParams, { modelName: '', category: null, status: null })
    pagination.page = 1
    fetchData()
  }

  function handleDragStart(row) {
    if (!sortMode.value)
      return
    draggingModelId.value = String(row?.id || '')
  }

  function handleDrop(target) {
    if (!sortMode.value || !draggingModelId.value || String(target?.id || '') === draggingModelId.value)
      return
    const fromIndex = dataSource.value.findIndex(item => String(item.id) === draggingModelId.value)
    const toIndex = dataSource.value.findIndex(item => String(item.id) === String(target?.id || ''))
    if (fromIndex < 0 || toIndex < 0)
      return
    const next = [...dataSource.value]
    const [moved] = next.splice(fromIndex, 1)
    next.splice(toIndex, 0, moved)
    dataSource.value = next
    draggingModelId.value = ''
  }

  function cancelModelOrder() {
    sortMode.value = false
    draggingModelId.value = ''
    fetchModelPage()
  }

  async function saveModelOrder() {
    if (sortSaving.value || !dataSource.value.length)
      return
    sortSaving.value = true
    try {
      const baseOrder = (pagination.page - 1) * pagination.pageSize
      const res = await flowApi.sortModels({
        items: dataSource.value.map((item, index) => ({
          modelId: String(item.id),
          sortOrder: baseOrder + index,
        })),
      })
      if (res.code !== 200)
        throw new Error(res.message || '排序保存失败')
      window.$message?.success('模型排序已保存')
      sortMode.value = false
      draggingModelId.value = ''
      await fetchData()
    }
    catch (error) {
      window.$message?.error(error?.message || '排序保存失败，请刷新后重试')
    }
    finally {
      sortSaving.value = false
    }
  }

  function handleFilter(status) {
    if (status === 'all') {
      queryParams.status = null
    }
    else {
      queryParams.status = status
    }
    pagination.page = 1
    fetchData()
  }

  const showModal = ref(false)
  const modalTitle = ref('新增模型')
  const isEdit = ref(false)
  const submitLoading = ref(false)
  const formRef = ref(null)
  const formData = reactive({
    id: '',
    modelName: '',
    modelKey: '',
    category: '',
    flowType: '',
    designerType: 'approval',
    formType: 'dynamic',
    description: '',
    notifyType: 'redis',
    webhookUrl: '',
    todoDetailUrlTemplate: DEFAULT_TODO_DETAIL_URL_TEMPLATE,
    notifyConfig: null,
    allowMultiReturn: false,
  })
  const rules = {
    modelName: { required: true, message: '请输入模型名称', trigger: 'blur' },
    category: { required: true, message: '请选择分类', trigger: 'change' },
    designerType: { required: true, message: '请选择流程模式', trigger: 'change' },
  }

  function handleAdd() {
    isEdit.value = false
    modalTitle.value = '新增模型'
    Object.assign(formData, { id: '', modelName: '', modelKey: generateModelKey(), category: '', flowType: '', designerType: 'approval', formType: 'dynamic', description: '', notifyType: 'redis', webhookUrl: '', todoDetailUrlTemplate: DEFAULT_TODO_DETAIL_URL_TEMPLATE, notifyConfig: null, allowMultiReturn: false })
    showModal.value = true
  }

  function generateModelKey() {
    const suffix = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`
    return `process_${suffix}`
  }

  function handleEdit(row) {
    isEdit.value = true
    modalTitle.value = '编辑模型'
    Object.assign(formData, row, {
      designerType: normalizeDesignerType(row.designerType),
      category: resolveFlowCategoryValue(row.category, categoryTreeOptions.value),
      notifyConfig: row.notifyConfig || null,
      todoDetailUrlTemplate: textValue(row.todoDetailUrlTemplate) || DEFAULT_TODO_DETAIL_URL_TEMPLATE,
      allowMultiReturn: row.allowMultiReturn === true || row.allowMultiReturn === 1 || String(row.allowMultiReturn) === '1',
    })
    showModal.value = true
  }

  async function handleSubmit() {
    try {
      await formRef.value?.validate()
      submitLoading.value = true
      const api = isEdit.value ? flowApi.updateModel : flowApi.createModel
      const res = await api(formData)
      if (res.code === 200) {
        window.$message?.success(isEdit.value ? '编辑成功' : '新增成功')
        showModal.value = false
        fetchData()
      }
      else {
        window.$message?.error(res.message || '操作失败')
      }
    }
    catch {
      console.error('提交失败')
    }
    finally {
      submitLoading.value = false
    }
  }

  async function enrichModelBusinessBindings(records = []) {
    await Promise.all(records.map(async (row) => {
      if (!row?.modelKey) {
        row.businessBindings = []
        return
      }
      try {
        const res = await businessFlowModelBindings(row.modelKey)
        const bindings = extractBusinessBindingRows(res)
        row.businessBindings = mergeModelBusinessBindings(bindings, buildModelFormBinding(row))
      }
      catch (error) {
        console.warn('[FlowModel] 加载业务绑定失败:', row.modelKey, error?.message || error)
        row.businessBindings = buildModelFormBinding(row)
      }
    }))
    return records
  }

  function mergeModelBusinessBindings(bindings = [], formBindings = []) {
    const normalizedBindings = Array.isArray(bindings) ? bindings : []
    const configuredBinding = Array.isArray(formBindings) ? formBindings[0] : null
    if (!configuredBinding)
      return normalizedBindings

    // 表单引用保存了用户实际选择的应用。对象被多个应用复用时，接口反查的
    // “主应用”不一定就是当前流程配置中的应用，因此优先展示表单引用中的应用。
    const hasApplicationContext = textValue(configuredBinding.applicationId)
      || textValue(configuredBinding.applicationName)
    if (!hasApplicationContext)
      return normalizedBindings.length ? normalizedBindings : formBindings

    const configuredObjectCode = textValue(configuredBinding.objectCode)
    return [
      configuredBinding,
      ...normalizedBindings.filter(item => textValue(item?.objectCode) !== configuredObjectCode),
    ]
  }

  function extractBusinessBindingRows(res) {
    if (Array.isArray(res))
      return res.filter(item => item && typeof item === 'object')
    if (res?.code !== undefined && res.code !== 200)
      return []
    const data = res?.data
    if (Array.isArray(data))
      return data.filter(item => item && typeof item === 'object')
    if (Array.isArray(data?.records))
      return data.records.filter(item => item && typeof item === 'object')
    if (Array.isArray(data?.list))
      return data.list.filter(item => item && typeof item === 'object')
    return []
  }

  function buildModelFormBinding(row = {}) {
    const reference = parseBusinessFormReference(row.formJson)
    const objectCode = textValue(reference.objectCode)
    const applicationId = textValue(reference.applicationId)
    const applicationName = textValue(reference.applicationName || reference.applicationCode)
    if (!objectCode && !applicationId)
      return []
    return [{
      flowModelKey: row.modelKey,
      bindingName: applicationName || textValue(reference.formName) || row.modelName,
      applicationId,
      applicationName,
      objectCode,
      objectName: textValue(reference.objectName || objectCode),
      entryRoute: textValue(reference.entryRoute),
    }]
  }

  function formatBusinessBindings(row = {}) {
    const bindings = Array.isArray(row.businessBindings) ? row.businessBindings : []
    if (!bindings.length)
      return '未绑定业务应用'
    const names = bindings.map((item) => {
      const application = textValue(item.applicationName || item.applicationCode)
      const object = textValue(item.objectName || item.objectCode)
      return application && object && application !== object
        ? `${application} · ${object}`
        : application || object || textValue(item.suiteName) || textValue(item.bindingName)
    }).filter(Boolean)
    if (!names.length)
      return '未绑定业务应用'
    return names.length > 2 ? `${names.slice(0, 2).join('、')} 等 ${names.length} 个业务应用` : names.join('、')
  }

  async function handleDesign(row) {
    if (!Array.isArray(row.businessBindings)) {
      const enriched = await enrichModelBusinessBindings([row])
      row.businessBindings = enriched[0]?.businessBindings || []
    }
    currentDesignBinding.value = row.businessBindings?.[0] || null
    currentDesignModelId.value = row.id
    showDesignModal.value = true
  }

  function handleDesignModalClose() {
    showDesignModal.value = false
    currentDesignModelId.value = ''
    currentDesignBinding.value = null
    fetchData()
  }

  function handleViewInstances(row) {
    router.push({ path: '/flow/monitor', query: { modelKey: row.modelKey } })
  }

  async function handleStartTest(row) {
    if (row.status !== 1) {
      window.$message?.warning('请先部署流程模型后再发起测试')
      return
    }
    const lockKey = lockModelAction(row, 'startTest')
    if (!lockKey)
      return
    currentStartModel.value = row
    startTestFormData.value = {}
    startTestFormSchema.value = []
    startTestBusinessFormActive.value = false
    startTestBusinessFormLoading.value = false
    startTestBusinessFormAssets.value = []
    resetStartTestBusinessFormLayout()
    startTestApproverNodes.value = []
    startTestApproverSelections.value = {}
    startTestApproverLabels.value = {}
    startTestPreflightDiagnostics.value = []
    showStartTestModal.value = true
    try {
      const res = await flowApi.getModelDetail(row.id)
      if (res.code === 200 && res.data) {
        currentStartModel.value = { ...row, ...res.data }
        if (isBusinessStartTestForm(currentStartModel.value)) {
          await loadStartTestBusinessForm(currentStartModel.value)
        }
        else {
          startTestFormSchema.value = parseFormSchema(res.data.formJson)
        }
      }
      const startConfig = await flowApi.getModelStartConfig(row.modelKey)
      if (startConfig.code === 200) {
        startTestApproverNodes.value = Array.isArray(startConfig.data?.initiatorSelectNodes)
          ? startConfig.data.initiatorSelectNodes
          : []
        startTestPreflightDiagnostics.value = Array.isArray(startConfig.data?.diagnostics)
          ? startConfig.data.diagnostics
          : []
      }
    }
    catch (error) {
      console.error('加载流程模型表单失败:', error)
      window.$message?.warning('加载流程模型表单失败，将以空变量发起')
    }
    finally {
      unlockModelAction(lockKey)
    }
  }

  function isBusinessStartTestForm(model = {}) {
    const formType = String(model.formType || '').trim().toLowerCase()
    if (['business', 'business_object_form', 'business_code_form'].includes(formType))
      return true
    const formRef = parseBusinessFormReference(model.formJson)
    const mode = String(formRef.formMode || formRef.type || '').trim().toUpperCase()
    if (mode === 'BUSINESS_OBJECT_FORM' || mode === 'BUSINESS_CODE_FORM')
      return true
    // 兼容早期已保存的业务引用：历史数据可能把 formType 写成 dynamic，
    // 但 formJson 仍保留 objectCode/formKey 两个业务资产身份。
    return Boolean(textValue(formRef.objectCode) && textValue(formRef.formKey))
  }

  async function loadStartTestBusinessForm(model = {}) {
    startTestBusinessFormActive.value = true
    startTestBusinessFormLoading.value = true
    try {
      const formRef = parseBusinessFormReference(model.formJson)
      const bindings = Array.isArray(model.businessBindings) ? model.businessBindings : []
      // 历史流程模型的 formJson 可能还保存旧 objectCode。优先使用同一应用下
      // 由业务绑定接口返回的规范编码，避免按旧编码查询不到应用页面表单资产。
      const formApplicationId = textValue(formRef.applicationId)
      const configuredObjectCode = textValue(formRef.objectCode)
      const fallbackBinding = bindings.find(binding =>
        textValue(binding?.objectCode)
        && (!configuredObjectCode || textValue(binding.objectCode) !== configuredObjectCode)
        && (!formApplicationId || textValue(binding?.applicationId) === formApplicationId),
      ) || bindings.find(binding => textValue(binding?.objectCode)) || null
      const formKey = textValue(formRef.formKey)
      const objectCode = textValue(fallbackBinding?.objectCode || formRef.objectCode)
      const applicationId = textValue(
        formRef.applicationId
        || fallbackBinding?.applicationId
        || resolveApplicationIdFromFormKey(formKey),
      )
      if (!objectCode) {
        window.$message?.warning('业务表单缺少业务对象信息，将以空变量发起测试')
        return
      }

      const res = await businessFlowFormAssets(objectCode, {
        includeInternal: true,
        applicationId: applicationId || undefined,
      })
      if (res.code !== undefined && res.code !== 200)
        throw new Error(res.message || '业务表单资产查询失败')
      const assetData = Array.isArray(res.data) ? res.data : res.data?.formAssets
      const assets = normalizeStartTestBusinessAssets(assetData || [])
      startTestBusinessFormAssets.value = assets
      const selectedAsset = assets.find(asset => asset.formKey === formKey)
        || assets[0]
        || null
      startTestFormSchema.value = normalizeStartTestBusinessFields(resolveStartTestBusinessFields(selectedAsset))
      applyStartTestBusinessFormLayout(selectedAsset)
    }
    catch (error) {
      console.warn('[FlowModel] 加载测试业务表单失败:', error?.message || error)
      startTestBusinessFormAssets.value = []
      startTestFormSchema.value = []
      window.$message?.warning('加载业务应用表单失败，将以空变量发起测试')
    }
    finally {
      startTestBusinessFormLoading.value = false
    }
  }

  function parseBusinessFormReference(formJson) {
    if (!formJson)
      return {}
    if (typeof formJson === 'object' && !Array.isArray(formJson)) {
      const nested = formJson.formRef && typeof formJson.formRef === 'object' ? formJson.formRef : {}
      return normalizeBusinessFormReference(nested, formJson)
    }
    try {
      const parsed = JSON.parse(formJson)
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
        return {}
      const nested = parsed.formRef && typeof parsed.formRef === 'object' ? parsed.formRef : {}
      return normalizeBusinessFormReference(nested, parsed)
    }
    catch {
      return {}
    }
  }

  function normalizeBusinessFormReference(nested = {}, root = {}) {
    const firstText = (...values) => values.map(textValue).find(Boolean) || ''
    return {
      ...nested,
      ...root,
      objectCode: firstText(nested.objectCode, root.objectCode),
      objectName: firstText(nested.objectName, root.objectName),
      applicationId: firstText(nested.applicationId, root.applicationId),
      applicationName: firstText(nested.applicationName, root.applicationName),
      formKey: firstText(nested.formKey, root.formKey),
      formName: firstText(nested.formName, root.formName),
      providerKey: firstText(nested.providerKey, root.providerKey),
      formMode: firstText(nested.formMode, nested.type, root.formMode, root.type),
      type: firstText(nested.type, nested.formMode, root.type, root.formMode),
    }
  }

  function resolveApplicationIdFromFormKey(formKey) {
    const match = textValue(formKey).match(/^app_([^_]+)_page_/i)
    return match ? textValue(match[1]) : ''
  }

  function normalizeStartTestBusinessAssets(assets = []) {
    return (Array.isArray(assets) ? assets : [])
      .map((asset) => {
        const schema = normalizeStartTestAssetSchema(
          hasStartTestAssetSchema(asset?.schema) ? asset.schema : asset?.formDesignerSchema,
        )
        const normalized = {
          ...asset,
          schema,
          formKey: textValue(asset.formKey || asset.key || asset.id || schema.formKey),
          formName: textValue(asset.formName || asset.name || asset.label || asset.formKey || schema.formName),
          fieldCatalog: Array.isArray(asset.fieldCatalog)
            ? asset.fieldCatalog
            : Array.isArray(asset.fields) ? asset.fields : [],
        }
        if (!normalized.fieldCatalog.length)
          normalized.fieldCatalog = resolveStartTestBusinessFields(normalized)
        return normalized
      })
      .filter(asset => asset.formKey)
  }

  /**
   * 业务表单资产在不同来源下的字段位置不完全一致：
   * - 业务对象页面通常返回 fieldCatalog/fields；
   * - 旧版本或代码 Provider 可能只返回 schema.components；
   * - 部分资产会保留空的 fieldCatalog，同时把字段放在 fields 中。
   * 统一在发起测试入口展开，避免空数组优先级导致表单被误判为无字段。
   */
  function resolveStartTestBusinessFields(asset = null) {
    if (!asset || typeof asset !== 'object')
      return []
    const fieldCatalog = Array.isArray(asset.fieldCatalog) ? asset.fieldCatalog : []
    const fields = Array.isArray(asset.fields) ? asset.fields : []
    if (fieldCatalog.length)
      return fieldCatalog
    if (fields.length)
      return fields

    const schema = normalizeStartTestAssetSchema(
      hasStartTestAssetSchema(asset.schema) ? asset.schema : asset.formDesignerSchema,
    )
    const schemaFields = Array.isArray(schema.fieldCatalog) ? schema.fieldCatalog : []
    if (schemaFields.length)
      return schemaFields
    if (Array.isArray(schema.fields) && schema.fields.length)
      return schema.fields
    return flattenStartTestBusinessComponents(schema.components)
  }

  function normalizeStartTestAssetSchema(value) {
    if (Array.isArray(value))
      return { components: value }
    if (value && typeof value === 'object')
      return value
    if (typeof value === 'string') {
      try {
        const parsed = JSON.parse(value)
        return Array.isArray(parsed) ? { components: parsed } : (parsed && typeof parsed === 'object' ? parsed : {})
      }
      catch {
        return {}
      }
    }
    return {}
  }

  function hasStartTestAssetSchema(value) {
    if (Array.isArray(value))
      return value.length > 0
    if (value && typeof value === 'object')
      return Object.keys(value).length > 0
    return typeof value === 'string' && value.trim().length > 0
  }

  function flattenStartTestBusinessComponents(components, result = []) {
    if (!Array.isArray(components))
      return result
    components.forEach((component) => {
      if (!component || typeof component !== 'object')
        return
      const binding = component.fieldBinding && typeof component.fieldBinding === 'object'
        ? component.fieldBinding
        : {}
      const props = component.props && typeof component.props === 'object' ? component.props : {}
      const fieldCode = textValue(binding.fieldCode || component.field || props.field)
      // 布局节点没有字段编码，只递归其子节点。
      if (fieldCode) {
        result.push({
          ...component,
          ...props,
          field: fieldCode,
          fieldCode,
          componentType: component.componentType || component.componentKey || component.type,
          type: component.type || component.componentType || component.componentKey,
          label: component.label || props.label || props.title,
          required: component.required ?? component.validation?.required,
          validation: component.validation,
        })
      }
      flattenStartTestBusinessComponents(component.children, result)
    })
    return result
  }

  function normalizeStartTestBusinessFields(fields = []) {
    const seen = new Set()
    return (Array.isArray(fields) ? fields : [])
      .filter(field => field && field.visible !== false && field.formVisible !== false
        && field.internal !== true && field.systemField !== true)
      .map((field) => {
        const binding = field.fieldBinding && typeof field.fieldBinding === 'object' ? field.fieldBinding : {}
        const fieldProps = field.props && typeof field.props === 'object' ? field.props : {}
        const fieldCode = textValue(
          field.field || field.fieldCode || binding.fieldCode || fieldProps.field || field.code || field.name,
        )
        if (!fieldCode || seen.has(fieldCode))
          return null
        seen.add(fieldCode)
        const type = normalizeStartTestFieldType(
          field.type || field.componentType || field.componentKey || field.fieldType,
        )
        const props = { ...fieldProps }
        const options = Array.isArray(field.options)
          ? field.options
          : Array.isArray(props.options) ? props.options : undefined
        delete props.disabled
        delete props.readonly
        return {
          ...field,
          field: fieldCode,
          code: fieldCode,
          prop: fieldCode,
          label: textValue(field.label || field.fieldName || field.title || fieldProps.label || fieldProps.title || fieldCode),
          type,
          required: field.required === true || field.validation?.required === true,
          readonly: false,
          disabled: false,
          props,
          ...(options ? { options } : {}),
        }
      })
      .filter(Boolean)
  }

  function normalizeStartTestFieldType(value) {
    const raw = textValue(value)
    const normalized = raw.replace(/[-_\s]/g, '').toLowerCase()
    if (['inputnumber', 'integer', 'decimal', 'money', 'number'].includes(normalized))
      return 'number'
    if (['dict', 'dictselect', 'dictionary'].includes(normalized))
      return 'dictSelect'
    if (['textarea', 'textareafield'].includes(normalized))
      return 'textarea'
    if (['datepicker', 'date'].includes(normalized))
      return 'date'
    if (['datetimepicker', 'datetime'].includes(normalized))
      return 'datetime'
    if (['datetimerange'].includes(normalized))
      return 'datetimerange'
    if (['daterange'].includes(normalized))
      return 'daterange'
    if (['timepicker', 'time'].includes(normalized))
      return 'time'
    if (['select', 'radio', 'radiobutton', 'checkbox', 'switch', 'cascader', 'treeselect', 'orgtreeselect', 'orgselect', 'regiontreeselect', 'userselect', 'userpicker', 'recordselector', 'objectreference', 'slider', 'rate', 'color', 'colorpicker', 'upload', 'fileupload', 'imageupload', 'customselect', 'transfer', 'month', 'year', 'timerange', 'barcodescanner', 'text'].includes(normalized)) {
      const aliases = {
        radiobutton: 'radioButton',
        treeselect: 'treeSelect',
        orgtreeselect: 'orgTreeSelect',
        orgselect: 'orgTreeSelect',
        regiontreeselect: 'regionTreeSelect',
        userselect: 'userSelect',
        userpicker: 'userSelect',
        recordselector: 'recordSelector',
        objectreference: 'objectReference',
        fileupload: 'fileUpload',
        imageupload: 'imageUpload',
        colorpicker: 'color',
        barcodescanner: 'barcodeScanner',
        customselect: 'customSelect',
      }
      return aliases[normalized] || normalized
    }
    return 'input'
  }

  function applyStartTestBusinessFormLayout(asset) {
    const schema = asset?.schema && typeof asset.schema === 'object' ? asset.schema : {}
    const settings = schema.settings && typeof schema.settings === 'object' ? schema.settings : {}
    const layout = settings.layout && typeof settings.layout === 'object' ? settings.layout : {}
    const gridCols = Number(layout.gridCols || layout.gridColumns || settings.gridCols || settings.gridColumns)
    startTestBusinessFormLayout.gridCols = Number.isFinite(gridCols) && gridCols > 0 ? gridCols : 1
    startTestBusinessFormLayout.labelPlacement = ['left', 'top'].includes(layout.labelPlacement || settings.labelPlacement)
      ? (layout.labelPlacement || settings.labelPlacement)
      : 'left'
    startTestBusinessFormLayout.labelWidth = layout.labelWidth || settings.labelWidth || '100'
  }

  function resetStartTestBusinessFormLayout() {
    Object.assign(startTestBusinessFormLayout, { gridCols: 1, labelPlacement: 'left', labelWidth: '100' })
  }

  function textValue(value) {
    return String(Array.isArray(value) ? value[0] || '' : value || '').trim()
  }

  function parseFormSchema(formJson) {
    if (!formJson)
      return []
    if (Array.isArray(formJson))
      return formJson
    try {
      const parsed = JSON.parse(formJson)
      return Array.isArray(parsed) ? parsed : []
    }
    catch {
      return []
    }
  }

  async function handleSubmitStartTest() {
    if (!currentStartModel.value)
      return
    startTestLoading.value = true
    try {
      const variables = await collectStartTestFormData()
      if (startTestApproverNodes.value.length) {
        try {
          variables.PROCESS_START_USER = collectInitiatorSelectSelections(
            startTestApproverNodes.value,
            startTestApproverSelections.value,
          )
        }
        catch (error) {
          window.$message?.warning(error?.message || '请选择审批人')
          return
        }
      }
      const now = Date.now()
      const businessKey = `FLOW_TEST:${currentStartModel.value.modelKey}:${now}`
      const title = `${currentStartModel.value.modelName || currentStartModel.value.modelKey}-测试发起`
      const res = await flowApi.startProcess(currentStartModel.value.modelKey, {
        businessKey,
        businessType: 'FLOW_MODEL_TEST',
        title,
        variables: variables || {},
        testStart: true,
      })
      if (res.code === 200) {
        window.$message?.success('测试流程已发起')
        showStartTestModal.value = false
        router.push({ path: '/flow/started', query: { title } })
      }
      else {
        window.$message?.error(res.message || '发起测试失败')
      }
    }
    catch (error) {
      console.error('发起测试失败:', error)
      window.$message?.error(error?.message || '发起测试失败')
    }
    finally {
      startTestLoading.value = false
    }
  }

  async function collectStartTestFormData() {
    if (!startTestFormSchema.value.length)
      return {}
    if (startTestBusinessFormActive.value) {
      await startTestFormRef.value?.validate?.()
      return startTestFormRef.value?.getFormData?.() || { ...startTestFormData.value }
    }
    return (await startTestFormRef.value?.submit?.()) || {}
  }

  function handleViewStarted() {
    showStartTestModal.value = false
    router.push('/flow/started')
  }

  async function handleDeploy(row) {
    const lockKey = lockModelAction(row, 'deploy')
    if (!lockKey)
      return
    window.$dialog?.info({
      title: '确认部署',
      content: `确定要部署「${row.modelName}」吗？部署后流程将可以发起。`,
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          const res = await flowApi.deployModel(row.id)
          if (res.code === 200) {
            window.$message?.success('部署成功')
            await fetchData()
          }
          else {
            window.$message?.error(res.message || '部署失败')
          }
        }
        catch (error) {
          window.$message?.error(error?.message || error?.response?.data?.message || '部署失败')
        }
        finally {
          unlockModelAction(lockKey)
        }
      },
      onNegativeClick: () => unlockModelAction(lockKey),
      onClose: () => unlockModelAction(lockKey),
    })
  }

  async function handleCopy(row) {
    const lockKey = lockModelAction(row, 'copy')
    if (!lockKey)
      return
    window.$dialog?.info({
      title: '复制模型',
      content: `确定要复制「${row.modelName}」吗？`,
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          const res = await flowApi.copyModel(row.id, `${row.modelName} - 副本`)
          if (res.code === 200) {
            window.$message?.success('复制成功')
            await fetchData()
          }
          else {
            window.$message?.error(res.message || '复制失败')
          }
        }
        catch (error) {
          window.$message?.error(error?.message || error?.response?.data?.message || '复制失败')
        }
        finally {
          unlockModelAction(lockKey)
        }
      },
      onNegativeClick: () => unlockModelAction(lockKey),
      onClose: () => unlockModelAction(lockKey),
    })
  }

  __impl.statusClass = statusClass
  __impl.toNumberOptions = toNumberOptions
  __impl.formatDate = formatDate
  __impl.normalizeDesignerType = normalizeDesignerType
  __impl.designerTypeLabel = designerTypeLabel
  __impl.designerTypeClass = designerTypeClass
  __impl.getCategoryDisplayName = getCategoryDisplayName
  __impl.isCodeAppBinding = isCodeAppBinding
  __impl.modelActionKey = modelActionKey
  __impl.isModelActionLocked = isModelActionLocked
  __impl.isModelActionBusy = isModelActionBusy
  __impl.lockModelAction = lockModelAction
  __impl.unlockModelAction = unlockModelAction
  __impl.getActionOptions = getActionOptions
  __impl.handleActionSelect = handleActionSelect
  __impl.fetchCategories = fetchCategories
  __impl.fetchData = fetchData
  __impl.fetchModelPage = fetchModelPage
  __impl.fetchModelStatistics = fetchModelStatistics
  __impl.applyModelStatistics = applyModelStatistics
  __impl.toCount = toCount
  __impl.handlePageSizeChange = handlePageSizeChange
  __impl.handleSearch = handleSearch
  __impl.handleStatusSelect = handleStatusSelect
  __impl.handleReset = handleReset
  __impl.handleDragStart = handleDragStart
  __impl.handleDrop = handleDrop
  __impl.cancelModelOrder = cancelModelOrder
  __impl.saveModelOrder = saveModelOrder
  __impl.handleFilter = handleFilter
  __impl.handleAdd = handleAdd
  __impl.generateModelKey = generateModelKey
  __impl.handleEdit = handleEdit
  __impl.handleSubmit = handleSubmit
  __impl.enrichModelBusinessBindings = enrichModelBusinessBindings
  __impl.mergeModelBusinessBindings = mergeModelBusinessBindings
  __impl.extractBusinessBindingRows = extractBusinessBindingRows
  __impl.buildModelFormBinding = buildModelFormBinding
  __impl.formatBusinessBindings = formatBusinessBindings
  __impl.handleDesign = handleDesign
  __impl.handleDesignModalClose = handleDesignModalClose
  __impl.handleViewInstances = handleViewInstances
  __impl.handleStartTest = handleStartTest
  __impl.isBusinessStartTestForm = isBusinessStartTestForm
  __impl.loadStartTestBusinessForm = loadStartTestBusinessForm
  __impl.parseBusinessFormReference = parseBusinessFormReference
  __impl.normalizeBusinessFormReference = normalizeBusinessFormReference
  __impl.resolveApplicationIdFromFormKey = resolveApplicationIdFromFormKey
  __impl.normalizeStartTestBusinessAssets = normalizeStartTestBusinessAssets
  __impl.resolveStartTestBusinessFields = resolveStartTestBusinessFields
  __impl.normalizeStartTestAssetSchema = normalizeStartTestAssetSchema
  __impl.hasStartTestAssetSchema = hasStartTestAssetSchema
  __impl.flattenStartTestBusinessComponents = flattenStartTestBusinessComponents
  __impl.normalizeStartTestBusinessFields = normalizeStartTestBusinessFields
  __impl.normalizeStartTestFieldType = normalizeStartTestFieldType
  __impl.applyStartTestBusinessFormLayout = applyStartTestBusinessFormLayout
  __impl.resetStartTestBusinessFormLayout = resetStartTestBusinessFormLayout
  __impl.textValue = textValue
  __impl.parseFormSchema = parseFormSchema
  __impl.handleSubmitStartTest = handleSubmitStartTest
  __impl.collectStartTestFormData = collectStartTestFormData
  __impl.handleViewStarted = handleViewStarted
  __impl.handleDeploy = handleDeploy
  __impl.handleCopy = handleCopy

  return {
    __impl, mut, applyModelStatistics, applyStartTestBusinessFormLayout, buildModelFormBinding, cancelModelOrder, collectStartTestFormData, designerTypeClass,
    designerTypeLabel, enrichModelBusinessBindings, extractBusinessBindingRows, fetchCategories, fetchData, fetchModelPage, fetchModelStatistics, flattenStartTestBusinessComponents,
    formatBusinessBindings, formatDate, generateModelKey, getActionOptions, getCategoryDisplayName, handleActionSelect, handleActivate, handleAdd,
    handleCopy, handleDelete, handleDeploy, handleDesign, handleDesignModalClose, handleDragStart, handleDrop, handleEdit,
    handleFilter, handlePageSizeChange, handleReset, handleSearch, handleStartTest, handleStatusSelect, handleSubmit, handleSubmitStartTest,
    handleSuspend, handleVersionHistory, handleViewInstances, handleViewStarted, hasStartTestAssetSchema, isBusinessStartTestForm, isCodeAppBinding, isModelActionBusy,
    isModelActionLocked, loadStartTestBusinessForm, lockModelAction, mergeModelBusinessBindings, modelActionKey, normalizeBusinessFormReference, normalizeDesignerType, normalizeStartTestAssetSchema,
    normalizeStartTestBusinessAssets, normalizeStartTestBusinessFields, normalizeStartTestFieldType, parseBusinessFormReference, parseFormSchema, resetStartTestBusinessFormLayout, resolveApplicationIdFromFormKey, resolveStartTestBusinessFields,
    saveModelOrder, statusClass, textValue, toCount, toNumberOptions, unlockModelAction, router, DEFAULT_TODO_DETAIL_URL_TEMPLATE,
    FlowDesignAsyncLoader, FlowFormRendererAsyncLoader, FlowModalAsyncLoader, FlowDesignPage, FlowFormCreateRenderer, VersionHistory, statusOptions, categoryTreeOptions,
    designerTypePresentation, designerTypeOptions, modelActionLocks, queryParams, activeStatsStatus, dataSource, loading, sortMode,
    sortSaving, draggingModelId, pagination, showVersionHistory, currentModelId, currentModelVersion, showDesignModal, currentDesignModelId,
    currentDesignBinding, showStartTestModal, startTestLoading, startTestFormRef, startTestFormData, startTestFormSchema, startTestBusinessFormActive, startTestBusinessFormLoading,
    startTestBusinessFormAssets, startTestBusinessFormLayout, startTestApproverNodes, startTestApproverSelections, startTestApproverLabels, startTestPreflightDiagnostics, currentStartModel, startTestTitle,
    startTestAlert, totalCount, designingCount, deployedCount, suspendedCount, disabledCount, showModal, modalTitle,
    isEdit, submitLoading, formRef, formData, rules,
    dict, getLabel,
  }
}
