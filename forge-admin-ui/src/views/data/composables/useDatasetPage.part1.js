/** dataset.vue setup part 1. */
import { NInput, NSelect, NTag } from 'naive-ui'
import { computed, h, nextTick, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDataConnectionFields, getDataConnectionList, getDataConnectionTables } from '@/api/data/connection'
import {
  deleteDataDataset,
  getDashboardDatasetImpact,
  getDataDatasetById,
  getDataDatasetCategoryTree,
  offlineDataDataset,
  publishDataDataset,
  saveDataDatasetFields,
  syncDataDatasetFields,
} from '@/api/data/dataset'
import { getDataDimensionList } from '@/api/data/dimension'
import { AiCrudPage } from '@/components/ai-form'
import DatasetParamSchemaEditor from '@/components/data/DatasetParamSchemaEditor.vue'
import DictTag from '@/components/DictTag.vue'
import SqlEditor from '@/components/SqlEditor.vue'
import { getDictData, useDict } from '@/composables/useDict'
import { request } from '@/utils'
import { normalizeDictOptionValue, toNumberDictOptions } from '@/utils/dict-options'

export function applyDatasetPagePart1() {
  const __impl = {}
  const mut = {
    permissionOptionsRequest: null,
  }
  const addAclItem = (...args) => __impl.addAclItem(...args)
  const addRowScopeRule = (...args) => __impl.addRowScopeRule(...args)
  const appendMissingFlatOption = (...args) => __impl.appendMissingFlatOption(...args)
  const appendMissingTreeOption = (...args) => __impl.appendMissingTreeOption(...args)
  const beforeSubmit = (...args) => __impl.beforeSubmit(...args)
  const buildRowScopeRuleItems = (...args) => __impl.buildRowScopeRuleItems(...args)
  const canGoToNextStep = (...args) => __impl.canGoToNextStep(...args)
  const clearRowScopeColumns = (...args) => __impl.clearRowScopeColumns(...args)
  const confirmSyncDatasetFields = (...args) => __impl.confirmSyncDatasetFields(...args)
  const containsTreeValue = (...args) => __impl.containsTreeValue(...args)
  const createDefaultAclItem = (...args) => __impl.createDefaultAclItem(...args)
  const createDefaultRowScope = (...args) => __impl.createDefaultRowScope(...args)
  const createRowScopeRule = (...args) => __impl.createRowScopeRule(...args)
  const ensureRowScope = (...args) => __impl.ensureRowScope(...args)
  const findTreeOption = (...args) => __impl.findTreeOption(...args)
  const getAclCount = (...args) => __impl.getAclCount(...args)
  const getAclItemLabel = (...args) => __impl.getAclItemLabel(...args)
  const getAclOrgOptions = (...args) => __impl.getAclOrgOptions(...args)
  const getAclSubjectFallbackLabel = (...args) => __impl.getAclSubjectFallbackLabel(...args)
  const getAclSubjectOptions = (...args) => __impl.getAclSubjectOptions(...args)
  const getRowScopeAttributeOptions = (...args) => __impl.getRowScopeAttributeOptions(...args)
  const getRowScopeConditionPreview = (...args) => __impl.getRowScopeConditionPreview(...args)
  const getRowScopeConfiguredCount = (...args) => __impl.getRowScopeConfiguredCount(...args)
  const getRowScopeFieldOptions = (...args) => __impl.getRowScopeFieldOptions(...args)
  const getRowScopeFieldSourceLabel = (...args) => __impl.getRowScopeFieldSourceLabel(...args)
  const getRowScopeRemark = (...args) => __impl.getRowScopeRemark(...args)
  const getRowScopeRuleLabel = (...args) => __impl.getRowScopeRuleLabel(...args)
  const getRowScopeRules = (...args) => __impl.getRowScopeRules(...args)
  const goToCategoryManage = (...args) => __impl.goToCategoryManage(...args)
  const goToNextStep = (...args) => __impl.goToNextStep(...args)
  const goToPrevStep = (...args) => __impl.goToPrevStep(...args)
  const handleAccessModeChange = (...args) => __impl.handleAccessModeChange(...args)
  const handleAclAccessLevelChange = (...args) => __impl.handleAclAccessLevelChange(...args)
  const handleAclSubjectIdChange = (...args) => __impl.handleAclSubjectIdChange(...args)
  const handleAclSubjectTypeChange = (...args) => __impl.handleAclSubjectTypeChange(...args)
  const handleCacheStrategyChange = (...args) => __impl.handleCacheStrategyChange(...args)
  const handleDelete = (...args) => __impl.handleDelete(...args)
  const handleOfflineDataset = (...args) => __impl.handleOfflineDataset(...args)
  const handlePreviewSql = (...args) => __impl.handlePreviewSql(...args)
  const handlePublishDataset = (...args) => __impl.handlePublishDataset(...args)
  const handleRowScopeEnabledChange = (...args) => __impl.handleRowScopeEnabledChange(...args)
  const handleRowScopeRemarkChange = (...args) => __impl.handleRowScopeRemarkChange(...args)
  const handleRowScopeRuleAttributeChange = (...args) => __impl.handleRowScopeRuleAttributeChange(...args)
  const handleRowScopeRuleFieldChange = (...args) => __impl.handleRowScopeRuleFieldChange(...args)
  const handleRowScopeRuleLogicChange = (...args) => __impl.handleRowScopeRuleLogicChange(...args)
  const handleSaveFieldConfig = (...args) => __impl.handleSaveFieldConfig(...args)
  const handleStepReset = (...args) => __impl.handleStepReset(...args)
  const handleSyncCurrentFields = (...args) => __impl.handleSyncCurrentFields(...args)
  const handleSyncFields = (...args) => __impl.handleSyncFields(...args)
  const handleViewFields = (...args) => __impl.handleViewFields(...args)
  const isRowScopeEnabled = (...args) => __impl.isRowScopeEnabled(...args)
  const loadDatasetImpact = (...args) => __impl.loadDatasetImpact(...args)
  const loadOrgOptions = (...args) => __impl.loadOrgOptions(...args)
  const loadPermissionOptions = (...args) => __impl.loadPermissionOptions(...args)
  const loadRoleOptions = (...args) => __impl.loadRoleOptions(...args)
  const loadRowScopeTableFields = (...args) => __impl.loadRowScopeTableFields(...args)
  const loadTableOptions = (...args) => __impl.loadTableOptions(...args)
  const loadUserOptions = (...args) => __impl.loadUserOptions(...args)
  const normalizeAccessLevel = (...args) => __impl.normalizeAccessLevel(...args)
  const normalizeAclItems = (...args) => __impl.normalizeAclItems(...args)
  const normalizeAclSubjectType = (...args) => __impl.normalizeAclSubjectType(...args)
  const normalizeFieldRows = (...args) => __impl.normalizeFieldRows(...args)
  const normalizeParamSchema = (...args) => __impl.normalizeParamSchema(...args)
  const normalizeRowScope = (...args) => __impl.normalizeRowScope(...args)
  const normalizeRowScopeLogic = (...args) => __impl.normalizeRowScopeLogic(...args)
  const normalizeRowScopeRule = (...args) => __impl.normalizeRowScopeRule(...args)
  const normalizeSortInput = (...args) => __impl.normalizeSortInput(...args)
  const normalizeSubmitAclItems = (...args) => __impl.normalizeSubmitAclItems(...args)
  const normalizeSubmitRowScope = (...args) => __impl.normalizeSubmitRowScope(...args)
  const parseParamSchemaFormValue = (...args) => __impl.parseParamSchemaFormValue(...args)
  const removeAclItem = (...args) => __impl.removeAclItem(...args)
  const removeRowScopeRule = (...args) => __impl.removeRowScopeRule(...args)
  const renderDatasetImpactContent = (...args) => __impl.renderDatasetImpactContent(...args)
  const renderFieldInput = (...args) => __impl.renderFieldInput(...args)
  const renderFieldSelect = (...args) => __impl.renderFieldSelect(...args)
  const renderMaskRuleSelect = (...args) => __impl.renderMaskRuleSelect(...args)
  const syncDatasetFields = (...args) => __impl.syncDatasetFields(...args)
  const syncRowScopeColumnsFromRules = (...args) => __impl.syncRowScopeColumnsFromRules(...args)
  const syncSlotForm = (...args) => __impl.syncSlotForm(...args)
  const transformOrgTreeOptions = (...args) => __impl.transformOrgTreeOptions(...args)
  const trimToNull = (...args) => __impl.trimToNull(...args)
  const updateDatasetFormField = (...args) => __impl.updateDatasetFormField(...args)
  const validateFieldRows = (...args) => __impl.validateFieldRows(...args)
  
  const { dict } = useDict(
    'data_dataset_type',
    'sys_enable_disable',
    'data_dataset_publish_status',
    'data_dataset_access_mode',
    'data_acl_subject_type',
    'data_acl_access_level',
    'data_result_encoding',
    'data_row_scope_attribute',
    'data_row_scope_logic',
    'data_field_data_type',
    'data_mask_rule',
    'data_date_format',
    'data_unit',
    'data_field_role',
    'data_field_sensitive_level',
  )

  const router = useRouter()
  const crudRef = ref(null)
  const connectionOptions = ref([])
  const categoryTree = ref([])
  const categoryKeyword = ref('')
  const tableOptions = ref([])
  const dimensionOptions = ref([])
  const tableLoading = ref(false)
  const rowScopeTableFieldLoading = ref(false)
  const loadedTableConnectionId = ref(null)
  const loadingTableConnectionId = ref(null)
  const rowScopeTableFieldKey = ref('')
  const rowScopeTableFieldOptions = ref([])
  const fieldModalVisible = ref(false)
  const fieldLoading = ref(false)
  const fieldSaving = ref(false)
  const fieldModalTitle = ref('字段列表')
  const fieldRows = ref([])
  const currentFieldDataset = ref(null)
  const sqlPreviewVisible = ref(false)
  const sqlPreviewLoading = ref(false)
  const sqlPreviewColumns = ref([])
  const sqlPreviewRows = ref([])
  const sqlPreviewScrollX = ref(0)
  const roleOptions = ref([])
  const userOptions = ref([])
  const orgTreeOptions = ref([])
  const permissionOptionsLoaded = ref(false)
  const permissionOptionsLoading = ref(false)
  const activeCategoryScope = ref('all')
  const selectedCategoryId = ref(null)
  const currentFormMode = ref('edit')
  const currentEditingDataset = ref(null)
  const currentStep = ref(1)
  const stepDefinitions = [
    {
      label: '基础信息',
      caption: '定义数据集基本信息与SQL',
      title: '定义数据集基本信息与SQL',
      description: '配置数据集名称、编码、所属目录、数据源和 SQL 或数据表来源。',
    },
    {
      label: '查询条件',
      caption: '配置参数化查询条件',
      title: '配置参数化查询条件',
      description: '维护报表侧可绑定的查询条件，保证参数名、字段映射和默认值可预期。',
    },
    {
      label: '执行设置',
      caption: '设置执行限制与调度策略',
      title: '设置执行限制与调度策略',
      description: '控制返回行数、超时时间和缓存策略，让数据集在大屏运行时保持稳定。',
    },
    {
      label: '权限控制',
      caption: '配置数据访问权限策略',
      title: '配置数据访问权限策略',
      description: '配置公开或私有访问范围，并按用户属性映射行级数据权限。',
    },
  ]
  const totalSteps = stepDefinitions.length

  const queryForm = reactive({
    datasetName: '',
    connectionId: null,
    datasetType: null,
    publishStatus: null,
  })

  const datasetTypeOptions = computed(() => dict.value.data_dataset_type || [])

  const statusOptions = computed(() => toNumberDictOptions(dict.value.sys_enable_disable))

  const resultEncodingOptions = computed(() => dict.value.data_result_encoding || [])

  const publishStatusOptions = computed(() => toNumberDictOptions(dict.value.data_dataset_publish_status))

  const datasetImpactLimit = 10
  const datasetImpactVisibleLimit = 6

  const accessModeOptions = computed(() => dict.value.data_dataset_access_mode || [])

  const aclSubjectTypeOptions = computed(() => dict.value.data_acl_subject_type || [])

  const accessLevelOptions = computed(() => dict.value.data_acl_access_level || [])

  const rowScopeAttributeOptions = computed(() => (dict.value.data_row_scope_attribute || []).map(item => ({
    ...item,
    caption: item.remark,
  })))

  const rowScopeLogicOptions = computed(() => dict.value.data_row_scope_logic || [])

  const dataTypeOptions = computed(() => dict.value.data_field_data_type || [])

  const fieldRoleOptions = computed(() => dict.value.data_field_role || [])

  const sensitiveLevelOptions = computed(() => dict.value.data_field_sensitive_level || [])

  const maskRuleOptions = computed(() => dict.value.data_mask_rule || [])

  const dateFormatOptions = computed(() => dict.value.data_date_format || [])

  const dataUnitOptions = computed(() => dict.value.data_unit || [])

  const supportedParamOperators = ['=', '!=', '>', '>=', '<', '<=', 'LIKE']

  const isFormReadOnly = computed(() => currentFormMode.value === 'view' || currentEditingDataset.value?.publishStatus === 1)
  const fieldConfigReadonly = computed(() => currentFieldDataset.value?.publishStatus === 1)
  const selectedCategoryNode = computed(() => findCategoryById(categoryTree.value, selectedCategoryId.value))
  const selectedTreeKeys = computed(() => activeCategoryScope.value === 'category' && selectedCategoryId.value ? [selectedCategoryId.value] : [])
  const currentStepMeta = computed(() => stepDefinitions[currentStep.value - 1] || stepDefinitions[0])
  const formModeLabel = computed(() => {
    if (currentEditingDataset.value?.publishStatus === 1) {
      return '已发布 · 只读浏览'
    }
    if (currentFormMode.value === 'add') {
      return '新增草稿'
    }
    if (currentFormMode.value === 'view') {
      return '只读查看'
    }
    return '编辑草稿'
  })
  const stepNavigationNote = computed(() => {
    if (isFormReadOnly.value) {
      return '当前为只读模式，可继续浏览各步骤内容'
    }
    return currentStepMeta.value.caption
  })
  const stepProgressPercent = computed(() => {
    if (totalSteps <= 1) {
      return 0
    }
    return ((currentStep.value - 1) / (totalSteps - 1)) * 100
  })
  const stepShellStyle = {
    width: '100%',
    boxSizing: 'border-box',
  }
  const stepProgressWrapStyle = computed(() => ({
    position: 'relative',
    display: 'grid',
    gridTemplateColumns: `repeat(${totalSteps}, minmax(0, 1fr))`,
    gap: '0',
    width: '100%',
    maxWidth: 'none',
    boxSizing: 'border-box',
    paddingTop: '4px',
  }))
  const stepProgressBaseLineStyle = {
    position: 'absolute',
    top: '26px',
    left: '22px',
    right: '22px',
    height: '2px',
    background: '#dbe3ef',
  }
  const stepProgressActiveLineStyle = computed(() => ({
    position: 'absolute',
    top: '26px',
    left: '22px',
    width: `calc((100% - 44px) * ${stepProgressPercent.value / 100})`,
    height: '2px',
    background: 'linear-gradient(90deg, #0f172a 0%, #1d4ed8 100%)',
    transition: 'width 0.24s ease',
  }))
  const stepNavigationWrapperStyle = {
    display: 'flex',
    justifyContent: 'flex-end',
    alignItems: 'center',
    gap: '16px',
    width: '100%',
    boxSizing: 'border-box',
  }
  const stepNavigationActionsStyle = {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'flex-end',
    flexWrap: 'wrap',
    gap: '12px',
    width: '100%',
  }
  const stepNavigationMetaStyle = {
    display: 'flex',
    flexDirection: 'column',
    gap: '4px',
    minWidth: '0',
    textAlign: 'right',
  }

  const activeCategoryScopeLabel = computed(() => {
    if (activeCategoryScope.value === 'uncategorized') {
      return '当前范围：未分类数据集'
    }
    if (activeCategoryScope.value === 'category' && selectedCategoryNode.value) {
      return `当前范围：${selectedCategoryNode.value.categoryName}`
    }
    return '当前范围：全部数据集'
  })

  const categoryTreeNodes = computed(() => buildCategoryTreeNodes(filterCategoryTree(categoryTree.value, categoryKeyword.value)))
  const categoryTreeSelectOptions = computed(() => buildCategorySelectOptions(categoryTree.value))
  const fieldConfigStats = computed(() => {
    const rows = fieldRows.value || []
    return [
      { label: '字段总数', value: rows.length },
      { label: '维度字段', value: rows.filter(field => field.fieldRole === 'DIMENSION').length },
      { label: '已绑定维度', value: rows.filter(field => field.dimensionId).length },
      { label: '脱敏字段', value: rows.filter(field => field.sensitiveLevel === 'MASK' || field.sensitiveLevel === 'HIDDEN').length },
    ]
  })

  const tableColumns = computed(() => [
    {
      prop: 'datasetName',
      label: '数据集资产',
      width: 250,
      render: row => h('div', { class: 'asset-name-card' }, [
        h('div', { class: 'asset-name-row' }, [
          h('div', { class: 'asset-name', title: row.description || row.datasetName }, row.datasetName),
          h(DictTag, {
            options: datasetTypeOptions.value,
            value: row.datasetType,
            size: 'small',
            bordered: false,
            forceTag: true,
          }),
        ]),
        h('div', { class: 'asset-code' }, row.datasetCode),
      ]),
    },
    {
      prop: 'categoryName',
      label: '业务分类',
      width: 130,
      render: row => h('div', { class: 'asset-category' }, [
        h('div', { class: 'asset-category-name' }, row.categoryName || '未分类'),
        h('div', { class: 'asset-category-code' }, row.categoryCode || '暂未归档'),
      ]),
    },
    {
      prop: 'connectionId',
      label: '数据来源',
      width: 190,
      render: row => h('div', { class: 'asset-source', title: `最大返回 ${row.maxRows ?? '-'} 行` }, [
        h('div', { class: 'asset-source-name' }, row.connectionName || getConnectionName(row.connectionId)),
        h('div', { class: 'asset-source-detail' }, row.datasetType === 'TABLE'
          ? `表：${row.tableName || '-'}`
          : 'SQL 查询模式'),
      ]),
    },
    {
      prop: 'publishStatus',
      label: '状态',
      width: 100,
      render: row => h('div', { class: 'asset-status' }, [
        h(DictTag, { dictType: 'data_dataset_publish_status', value: String(row.publishStatus), size: 'small' }),
        h(DictTag, { dictType: 'sys_enable_disable', value: String(row.status), size: 'small' }),
      ]),
    },
    {
      prop: 'accessMode',
      label: '访问权限',
      width: 90,
      render: row => h(DictTag, {
        dictType: 'data_dataset_access_mode',
        value: row.accessMode,
        size: 'small',
      }),
    },
    { prop: 'updateTime', label: '更新时间', width: 150, render: row => formatDatasetDate(row.updateTime) },
    {
      prop: 'action',
      label: '操作',
      width: 160,
      fixed: 'right',
      maxActionButtons: 2,
      actions: [
        { label: '编辑', key: 'edit', type: 'primary', visible: row => row.publishStatus !== 1, onClick: handleEdit },
        { label: '查看', key: 'view', type: 'primary', visible: row => row.publishStatus === 1, onClick: handleViewDataset },
        { label: '发布', key: 'publish', type: 'success', visible: row => row.publishStatus !== 1, onClick: handlePublishDataset },
        { label: '下架', key: 'offline', type: 'warning', visible: row => row.publishStatus === 1, onClick: handleOfflineDataset },
        { label: '字段配置', key: 'fields', type: 'info', onClick: handleViewFields },
        { label: '同步字段', key: 'sync', type: 'info', visible: row => row.publishStatus !== 1, onClick: handleSyncFields },
        { label: '删除', key: 'delete', type: 'error', visible: row => row.publishStatus !== 1, onClick: handleDelete },
      ],
    },
  ])

  const fieldColumns = computed(() => [
    {
      title: '字段名',
      key: 'fieldName',
      width: 180,
      render: row => h('div', { class: 'field-name-cell' }, row.fieldName),
    },
    {
      title: '显示名称',
      key: 'fieldLabel',
      width: 190,
      render: row => renderFieldInput(row, 'fieldLabel', '请输入显示名称'),
    },
    {
      title: '字段说明',
      key: 'description',
      width: 250,
      render: row => renderFieldInput(row, 'description', '字段口径或配置说明'),
    },
    {
      title: '标准类型',
      key: 'dataType',
      width: 160,
      render: row => renderFieldSelect(row, 'dataType', dataTypeOptions.value, { placeholder: '标准类型' }),
    },
    {
      title: '字段角色',
      key: 'fieldRole',
      width: 140,
      render: row => renderFieldSelect(row, 'fieldRole', fieldRoleOptions.value, {
        placeholder: '字段角色',
        onChange: (value) => {
          if (value !== 'DIMENSION') {
            row.dimensionId = null
          }
        },
      }),
    },
    {
      title: '绑定维度',
      key: 'dimensionId',
      width: 220,
      render: (row) => {
        if (row.fieldRole !== 'DIMENSION') {
          return h('span', { class: 'field-muted-text' }, '指标字段无需绑定')
        }
        return renderFieldSelect(row, 'dimensionId', dimensionOptions.value, {
          placeholder: '选择维度翻译',
          clearable: true,
          filterable: true,
        })
      },
    },
    {
      title: '日期格式',
      key: 'dateFormat',
      width: 190,
      render: (row) => {
        if (!['DATE', 'DATETIME'].includes(row.dataType)) {
          return h('span', { class: 'field-muted-text' }, '非日期字段')
        }
        return renderFieldSelect(row, 'dateFormat', dateFormatOptions.value, {
          placeholder: '选择或输入格式',
          clearable: true,
          filterable: true,
          tag: true,
        })
      },
    },
    {
      title: '计量单位',
      key: 'dataUnit',
      width: 150,
      render: row => renderFieldSelect(row, 'dataUnit', dataUnitOptions.value, {
        placeholder: '单位',
        clearable: true,
        filterable: true,
        tag: true,
      }),
    },
    {
      title: '脱敏策略',
      key: 'sensitiveLevel',
      width: 150,
      render: row => renderFieldSelect(row, 'sensitiveLevel', sensitiveLevelOptions.value, { placeholder: '脱敏策略' }),
    },
    {
      title: '脱敏规则',
      key: 'maskRule',
      width: 230,
      render: (row) => {
        if (row.sensitiveLevel !== 'MASK') {
          return h('span', { class: 'field-muted-text' }, row.sensitiveLevel === 'HIDDEN' ? '字段隐藏' : '不脱敏')
        }
        return renderMaskRuleSelect(row)
      },
    },
    {
      title: '排序',
      key: 'sort',
      width: 170,
      render: row => fieldConfigReadonly.value
        ? h('span', { class: 'field-sort-value' }, row.sort ?? 0)
        : h('input', {
            class: 'field-sort-native-input',
            type: 'number',
            min: 0,
            value: row.sort ?? 0,
            onInput: event => row.sort = normalizeSortInput(event.target.value),
          }),
    },
    {
      title: '来源类型',
      key: 'dbType',
      width: 130,
      render: row => h(NTag, { size: 'small', bordered: false }, { default: () => row.dbType || '-' }),
    },
  ])

  const fieldTableScrollX = computed(() => fieldColumns.value.reduce((total, column) => total + (Number(column.width) || 140), 0))

  const editSchema = computed(() => [
    {
      field: 'datasetEditor',
      label: '',
      type: 'slot',
      slotName: 'datasetEditor',
      span: 12,
      showFeedback: false,
    },
  ])

  loadConnectionOptions()
  loadCategoryTree()
  loadDimensionOptions()

  async function loadConnectionOptions() {
    try {
      const res = await getDataConnectionList()
      if (res.code === 200 && Array.isArray(res.data)) {
        connectionOptions.value = res.data.map(item => ({
          label: item.connectionName,
          value: toIdString(item.id),
        }))
      }
    }
    catch (error) {
      console.error('Failed to load connections', error)
    }
  }

  async function loadDimensionOptions() {
    try {
      const res = await getDataDimensionList()
      if (res.code === 200 && Array.isArray(res.data)) {
        dimensionOptions.value = res.data.map(item => ({
          label: `${item.dimensionName}（${item.dimensionCode}）`,
          value: item.id,
        }))
      }
    }
    catch (error) {
      console.error('Failed to load dimensions', error)
    }
  }

  async function loadCategoryTree(options = {}) {
    const { silent = false } = options
    try {
      const res = await getDataDatasetCategoryTree()
      if (res.code === 200 && Array.isArray(res.data)) {
        categoryTree.value = res.data
        if (activeCategoryScope.value === 'category' && selectedCategoryId.value && !findCategoryById(categoryTree.value, selectedCategoryId.value)) {
          activeCategoryScope.value = 'all'
          selectedCategoryId.value = null
        }
        return true
      }
      if (!silent) {
        window.$message?.error(res.msg || '加载数据集分类失败')
      }
      return false
    }
    catch (error) {
      console.error('Failed to load dataset categories', error)
      if (!silent) {
        window.$message?.error('加载数据集分类失败')
      }
      return false
    }
  }

  function getConnectionName(connectionId) {
    const connection = connectionOptions.value.find(item => item.value === connectionId)
    return connection?.label || connectionId || '-'
  }

  function getCategoryName(categoryId) {
    return findCategoryById(categoryTree.value, categoryId)?.categoryName || '未分类'
  }

  function formatDatasetDate(value) {
    if (!value) {
      return '-'
    }
    return String(value).replace('T', ' ').slice(0, 19)
  }

  function setEditorStep(step, shouldScroll = false) {
    currentStep.value = step
    if (shouldScroll) {
      scrollToStepSection(step)
    }
  }

  async function scrollToStepSection(step) {
    await nextTick()
    const section = document.querySelector(`.data-dataset-edit-form [data-step-section="${step}"]`)
    section?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
      inline: 'nearest',
    })
  }

  function getPublishStatusLabel(status) {
    const item = dict.value.data_dataset_publish_status?.find(d => d.value === String(status))
    return item?.label || String(status ?? '-')
  }

  function getAccessModeLabel(accessMode) {
    const item = dict.value.data_dataset_access_mode?.find(d => d.value === String(accessMode))
    return item?.label || String(accessMode ?? '-')
  }

  function getDatasetCreatorLabel(formData) {
    return formData?.createByName || formData?.creatorName || formData?.createBy || '-'
  }

  function getDatasetUpdaterLabel(formData) {
    return formData?.updateByName || formData?.updaterName || formData?.updateBy || '-'
  }

  function getDatasetVersionLabel(formData) {
    if (formData?.versionNo) {
      return `v${formData.versionNo}`
    }
    return formData?.id ? 'v1' : '保存后生成'
  }

  function getDatasetTagLabels(formData) {
    return [
      getDatasetTypeLabel(formData?.datasetType),
      getAccessModeLabel(formData?.accessMode),
      getCategoryName(formData?.categoryId),
    ].filter(Boolean)
  }

  function getDatasetSourceGuide(formData) {
    if (formData?.datasetType === 'SQL') {
      return 'SQL 数据集适合多表关联、预聚合和复杂过滤，保存前建议先执行 SQL 预览。'
    }
    return '单表数据集适合标准明细表和维表建模，字段同步会按所选数据表结构生成。'
  }

  function getDatasetTypeLabel(datasetType) {
    const item = dict.value.data_dataset_type?.find(d => d.value === String(datasetType))
    return item?.label || String(datasetType ?? '-')
  }

  function getEnableStatusLabel(status) {
    const item = dict.value.sys_enable_disable?.find(d => d.value === String(status))
    return item?.label || String(status ?? '-')
  }

  function getDatasetSourceSubject(formData) {
    if (formData?.datasetType === 'SQL') {
      return 'SQL 语句'
    }
    return formData?.tableName || '待选择数据表'
  }

  function getDatasetParamGuide(formData) {
    if (formData?.datasetType === 'SQL') {
      const paramCount = getSqlParamCount(formData?.sqlText)
      return paramCount > 0
        ? `当前 SQL 已识别 ${paramCount} 个命名参数，条件参数名需要与 SQL 中的 :param 完全一致。`
        : '先在 SQL 中写入 :paramName，再回到这里定义参数类型、默认值和是否必填。'
    }
    return '单表模式下每个查询条件都要映射到具体数据表字段，便于运行时安全拼装过滤条件。'
  }

  function getDatasetParamReadiness(formData) {
    if (formData?.datasetType === 'SQL') {
      return `${getSqlParamCount(formData?.sqlText)} 个命名参数`
    }
    return formData?.tableName ? '已绑定数据表' : '待选择数据表'
  }

  function getDatasetParamConstraint(formData) {
    if (formData?.datasetType === 'SQL') {
      return '参数名需与 SQL 保持一致'
    }
    return '每项都需要映射字段'
  }

  function getParamPreviewRows(formData) {
    const value = formData?.paramSchemaJson
    let rows = []
    if (Array.isArray(value)) {
      rows = value
    }
    else if (typeof value === 'string' && value) {
      try {
        const parsed = JSON.parse(value)
        rows = Array.isArray(parsed) ? parsed : []
      }
      catch {
        rows = []
      }
    }

    return rows.filter(row => row?.paramName || row?.label || row?.fieldName)
  }

  function getParamPreviewDescription(param, datasetType) {
    const parts = [
      param.label || null,
      param.dataType || 'STRING',
      param.required ? '必填' : '可选',
    ]
    if (datasetType === 'TABLE' && param.fieldName) {
      parts.push(`${param.operator || '='} ${param.fieldName}`)
    }
    return parts.filter(Boolean).join(' / ')
  }

  function getStepNodeInlineStyle(index) {
    if (index > 0 && index < totalSteps - 1) {
      return {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '12px',
        minWidth: '0',
        position: 'relative',
        zIndex: 1,
        textAlign: 'center',
      }
    }

    if (index === totalSteps - 1) {
      return {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'flex-end',
        gap: '12px',
        minWidth: '0',
        position: 'relative',
        zIndex: 1,
        textAlign: 'right',
      }
    }

    return {
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'flex-start',
      gap: '12px',
      minWidth: '0',
      position: 'relative',
      zIndex: 1,
      textAlign: 'left',
    }
  }

  function getSqlParamCount(sqlText) {
    return extractSqlParamNames(sqlText).length
  }

  function extractSqlParamNames(sqlText) {
    if (!sqlText) {
      return []
    }

    const matches = sqlText.matchAll(/:([a-z_]\w*)/gi)
    return [...new Set(Array.from(matches, match => match[1]))]
  }

  function buildCategoryTreeNodes(tree) {
    return tree.map(item => ({
      key: item.id,
      label: item.status === 1 ? item.categoryName : `${item.categoryName} · ${getEnableStatusLabel(item.status)}`,
      children: item.children?.length ? buildCategoryTreeNodes(item.children) : undefined,
    }))
  }

  function buildCategorySelectOptions(tree) {
    return tree.map(item => ({
      label: item.status === 1 ? item.categoryName : `${item.categoryName}（${getEnableStatusLabel(item.status)}）`,
      value: item.id,
      key: item.id,
      children: item.children?.length ? buildCategorySelectOptions(item.children) : undefined,
    }))
  }

  function filterCategoryTree(tree, keyword) {
    const normalizedKeyword = keyword?.trim().toLowerCase()
    if (!normalizedKeyword) {
      return tree
    }

    return tree
      .map((item) => {
        const children = filterCategoryTree(item.children || [], keyword)
        const matched = item.categoryName?.toLowerCase().includes(normalizedKeyword)
          || item.categoryCode?.toLowerCase().includes(normalizedKeyword)
        if (!matched && children.length === 0) {
          return null
        }
        return {
          ...item,
          children,
        }
      })
      .filter(Boolean)
  }

  function findCategoryById(tree, id) {
    if (!id) {
      return null
    }
    for (const item of tree || []) {
      if (item.id === id) {
        return item
      }
      const child = findCategoryById(item.children, id)
      if (child) {
        return child
      }
    }
    return null
  }

  function handleCategoryTreeSelect(keys) {
    const nextId = Array.isArray(keys) && keys.length > 0 ? keys[0] : null
    if (!nextId) {
      return
    }
    selectedCategoryId.value = nextId
    activeCategoryScope.value = 'category'
    applySearch()
  }

  function selectAllCategories() {
    activeCategoryScope.value = 'all'
    selectedCategoryId.value = null
    applySearch()
  }

  function selectUncategorized() {
    activeCategoryScope.value = 'uncategorized'
    selectedCategoryId.value = null
    applySearch()
  }

  function buildSearchParams() {
    return {
      datasetName: queryForm.datasetName?.trim() || undefined,
      connectionId: queryForm.connectionId || undefined,
      datasetType: queryForm.datasetType || undefined,
      publishStatus: queryForm.publishStatus ?? undefined,
      categoryId: activeCategoryScope.value === 'category' ? selectedCategoryId.value : undefined,
      uncategorized: activeCategoryScope.value === 'uncategorized' ? true : undefined,
    }
  }

  function applySearch() {
    crudRef.value?.search(buildSearchParams())
  }

  function handleResetFilters() {
    queryForm.datasetName = ''
    queryForm.connectionId = null
    queryForm.datasetType = null
    queryForm.publishStatus = null
    activeCategoryScope.value = 'all'
    selectedCategoryId.value = null
    crudRef.value?.search({})
  }

  function handleAddDataset() {
    handleStepReset()
    currentFormMode.value = 'add'
    currentEditingDataset.value = null
    crudRef.value?.showAdd()
  }

  function handleEdit(row) {
    handleStepReset()
    currentFormMode.value = 'edit'
    currentEditingDataset.value = row
    crudRef.value?.showEdit({ ...row, __modalTitle: '编辑数据集' })
  }

  function handleViewDataset(row) {
    handleStepReset()
    currentFormMode.value = 'view'
    currentEditingDataset.value = row
    crudRef.value?.showEdit({ ...row, __modalTitle: '查看数据集' })
  }

  function handleDatasetModalClose() {
    currentFormMode.value = 'edit'
    currentEditingDataset.value = null
    handleStepReset()
  }

  function prepareDatasetFormData(sourceData = {}, options = {}) {
    const { applyScopeDefault = false } = options
    const nextFormData = {
      datasetType: 'TABLE',
      status: 1,
      maxRows: 1000,
      timeoutSeconds: 15,
      cacheEnabled: 0,
      cacheTtlSeconds: null,
      accessMode: 'PUBLIC',
      aclItems: [],
      rowScope: createDefaultRowScope(),
      ...sourceData,
    }

    if (applyScopeDefault && !nextFormData.categoryId && activeCategoryScope.value === 'category' && selectedCategoryId.value) {
      nextFormData.categoryId = selectedCategoryId.value
    }

    nextFormData.paramSchemaJson = parseParamSchemaFormValue(nextFormData.paramSchemaJson)
    nextFormData.connectionId = toIdString(nextFormData.connectionId)
    nextFormData.accessMode = nextFormData.accessMode === 'PRIVATE' ? 'PRIVATE' : 'PUBLIC'
    nextFormData.aclItems = normalizeAclItems(nextFormData.aclItems)
    nextFormData.rowScope = normalizeRowScope(nextFormData.rowScope)
    return nextFormData
  }

  async function beforeRenderForm(formData) {
    await ensureRowScopeDictOptions()
    const nextFormData = prepareDatasetFormData(formData || {}, {
      applyScopeDefault: !formData,
    })
    const connectionId = nextFormData.connectionId
    const datasetType = nextFormData.datasetType || 'TABLE'
    if (connectionId && datasetType === 'TABLE') {
      await loadTableOptions(connectionId)
      await loadRowScopeTableFields(nextFormData)
    }
    else {
      resetTableOptions()
      resetRowScopeTableFields()
    }
    if (nextFormData.accessMode === 'PRIVATE') {
      await loadPermissionOptions()
    }
    return nextFormData
  }

  async function ensureRowScopeDictOptions() {
    if (rowScopeAttributeOptions.value.length > 0 && rowScopeLogicOptions.value.length > 0)
      return

    const [attributeOptions, logicOptions] = await Promise.all([
      getDictData('data_row_scope_attribute'),
      getDictData('data_row_scope_logic'),
    ])
    dict.value.data_row_scope_attribute = attributeOptions
    dict.value.data_row_scope_logic = logicOptions
  }

  async function beforeRenderDetail(detailData) {
    await ensureRowScopeDictOptions()
    const nextFormData = prepareDatasetFormData(detailData || {})
    currentEditingDataset.value = nextFormData
    const connectionId = nextFormData.connectionId
    const datasetType = nextFormData.datasetType || 'TABLE'
    if (connectionId && datasetType === 'TABLE') {
      await loadTableOptions(connectionId)
      await loadRowScopeTableFields(nextFormData)
    }
    else {
      resetTableOptions()
      resetRowScopeTableFields()
    }
    if (nextFormData.accessMode === 'PRIVATE') {
      await loadPermissionOptions()
    }
    return nextFormData
  }

  async function handleConnectionChange(connectionId, formData, updateValue) {
    formData.connectionId = toIdString(connectionId)
    formData.tableName = null
    clearRowScopeColumns(formData)
    resetRowScopeTableFields()
    syncSlotForm(updateValue)
    if (formData.datasetType === 'TABLE') {
      await loadTableOptions(formData.connectionId)
    }
  }

  async function handleDatasetTypeChange(datasetType, formData, updateValue) {
    if (isFormReadOnly.value || formData.datasetType === datasetType) {
      return
    }
    formData.datasetType = datasetType
    clearRowScopeColumns(formData)
    resetRowScopeTableFields()
    if (datasetType === 'TABLE') {
      formData.sqlText = null
      syncSlotForm(updateValue)
      await loadTableOptions(formData.connectionId)
      return
    }

    formData.tableName = null
    if (formData.sqlText === null || formData.sqlText === undefined) {
      formData.sqlText = ''
    }
    syncSlotForm(updateValue)
  }

  async function handleTableNameChange(tableName, formData, updateValue) {
    formData.tableName = tableName
    clearRowScopeColumns(formData)
    syncSlotForm(updateValue)
    if (!tableName) {
      resetRowScopeTableFields()
      return
    }
    await loadRowScopeTableFields(formData, { force: true })
  }

  function resetTableOptions() {
    tableOptions.value = []
    loadedTableConnectionId.value = null
    loadingTableConnectionId.value = null
  }

  function resetRowScopeTableFields() {
    rowScopeTableFieldKey.value = ''
    rowScopeTableFieldOptions.value = []
  }

  function toIdString(value) {
    if (value === null || value === undefined || value === '') {
      return null
    }
    return String(value)
  }

  __impl.loadConnectionOptions = loadConnectionOptions
  __impl.loadDimensionOptions = loadDimensionOptions
  __impl.loadCategoryTree = loadCategoryTree
  __impl.getConnectionName = getConnectionName
  __impl.getCategoryName = getCategoryName
  __impl.formatDatasetDate = formatDatasetDate
  __impl.setEditorStep = setEditorStep
  __impl.scrollToStepSection = scrollToStepSection
  __impl.getPublishStatusLabel = getPublishStatusLabel
  __impl.getAccessModeLabel = getAccessModeLabel
  __impl.getDatasetCreatorLabel = getDatasetCreatorLabel
  __impl.getDatasetUpdaterLabel = getDatasetUpdaterLabel
  __impl.getDatasetVersionLabel = getDatasetVersionLabel
  __impl.getDatasetTagLabels = getDatasetTagLabels
  __impl.getDatasetSourceGuide = getDatasetSourceGuide
  __impl.getDatasetTypeLabel = getDatasetTypeLabel
  __impl.getEnableStatusLabel = getEnableStatusLabel
  __impl.getDatasetSourceSubject = getDatasetSourceSubject
  __impl.getDatasetParamGuide = getDatasetParamGuide
  __impl.getDatasetParamReadiness = getDatasetParamReadiness
  __impl.getDatasetParamConstraint = getDatasetParamConstraint
  __impl.getParamPreviewRows = getParamPreviewRows
  __impl.getParamPreviewDescription = getParamPreviewDescription
  __impl.getStepNodeInlineStyle = getStepNodeInlineStyle
  __impl.getSqlParamCount = getSqlParamCount
  __impl.extractSqlParamNames = extractSqlParamNames
  __impl.buildCategoryTreeNodes = buildCategoryTreeNodes
  __impl.buildCategorySelectOptions = buildCategorySelectOptions
  __impl.filterCategoryTree = filterCategoryTree
  __impl.findCategoryById = findCategoryById
  __impl.handleCategoryTreeSelect = handleCategoryTreeSelect
  __impl.selectAllCategories = selectAllCategories
  __impl.selectUncategorized = selectUncategorized
  __impl.buildSearchParams = buildSearchParams
  __impl.applySearch = applySearch
  __impl.handleResetFilters = handleResetFilters
  __impl.handleAddDataset = handleAddDataset
  __impl.handleEdit = handleEdit
  __impl.handleViewDataset = handleViewDataset
  __impl.handleDatasetModalClose = handleDatasetModalClose
  __impl.prepareDatasetFormData = prepareDatasetFormData
  __impl.beforeRenderForm = beforeRenderForm
  __impl.ensureRowScopeDictOptions = ensureRowScopeDictOptions
  __impl.beforeRenderDetail = beforeRenderDetail
  __impl.handleConnectionChange = handleConnectionChange
  __impl.handleDatasetTypeChange = handleDatasetTypeChange
  __impl.handleTableNameChange = handleTableNameChange
  __impl.resetTableOptions = resetTableOptions
  __impl.resetRowScopeTableFields = resetRowScopeTableFields
  __impl.toIdString = toIdString

  return {
    __impl, mut, addAclItem, addRowScopeRule, appendMissingFlatOption, appendMissingTreeOption, applySearch, beforeRenderDetail,
    beforeRenderForm, beforeSubmit, buildCategorySelectOptions, buildCategoryTreeNodes, buildRowScopeRuleItems, buildSearchParams, canGoToNextStep, clearRowScopeColumns,
    confirmSyncDatasetFields, containsTreeValue, createDefaultAclItem, createDefaultRowScope, createRowScopeRule, ensureRowScope, ensureRowScopeDictOptions, extractSqlParamNames,
    filterCategoryTree, findCategoryById, findTreeOption, formatDatasetDate, getAccessModeLabel, getAclCount, getAclItemLabel, getAclOrgOptions,
    getAclSubjectFallbackLabel, getAclSubjectOptions, getCategoryName, getConnectionName, getDatasetCreatorLabel, getDatasetParamConstraint, getDatasetParamGuide, getDatasetParamReadiness,
    getDatasetSourceGuide, getDatasetSourceSubject, getDatasetTagLabels, getDatasetTypeLabel, getDatasetUpdaterLabel, getDatasetVersionLabel, getEnableStatusLabel, getParamPreviewDescription,
    getParamPreviewRows, getPublishStatusLabel, getRowScopeAttributeOptions, getRowScopeConditionPreview, getRowScopeConfiguredCount, getRowScopeFieldOptions, getRowScopeFieldSourceLabel, getRowScopeRemark,
    getRowScopeRuleLabel, getRowScopeRules, getSqlParamCount, getStepNodeInlineStyle, goToCategoryManage, goToNextStep, goToPrevStep, handleAccessModeChange,
    handleAclAccessLevelChange, handleAclSubjectIdChange, handleAclSubjectTypeChange, handleAddDataset, handleCacheStrategyChange, handleCategoryTreeSelect, handleConnectionChange, handleDatasetModalClose,
    handleDatasetTypeChange, handleDelete, handleEdit, handleOfflineDataset, handlePreviewSql, handlePublishDataset, handleResetFilters, handleRowScopeEnabledChange,
    handleRowScopeRemarkChange, handleRowScopeRuleAttributeChange, handleRowScopeRuleFieldChange, handleRowScopeRuleLogicChange, handleSaveFieldConfig, handleStepReset, handleSyncCurrentFields, handleSyncFields,
    handleTableNameChange, handleViewDataset, handleViewFields, isRowScopeEnabled, loadCategoryTree, loadConnectionOptions, loadDatasetImpact, loadDimensionOptions,
    loadOrgOptions, loadPermissionOptions, loadRoleOptions, loadRowScopeTableFields, loadTableOptions, loadUserOptions, normalizeAccessLevel, normalizeAclItems,
    normalizeAclSubjectType, normalizeFieldRows, normalizeParamSchema, normalizeRowScope, normalizeRowScopeLogic, normalizeRowScopeRule, normalizeSortInput, normalizeSubmitAclItems,
    normalizeSubmitRowScope, parseParamSchemaFormValue, prepareDatasetFormData, removeAclItem, removeRowScopeRule, renderDatasetImpactContent, renderFieldInput, renderFieldSelect,
    renderMaskRuleSelect, resetRowScopeTableFields, resetTableOptions, scrollToStepSection, selectAllCategories, selectUncategorized, setEditorStep, syncDatasetFields,
    syncRowScopeColumnsFromRules, syncSlotForm, toIdString, transformOrgTreeOptions, trimToNull, updateDatasetFormField, validateFieldRows, router,
    crudRef, connectionOptions, categoryTree, categoryKeyword, tableOptions, dimensionOptions, tableLoading, rowScopeTableFieldLoading,
    loadedTableConnectionId, loadingTableConnectionId, rowScopeTableFieldKey, rowScopeTableFieldOptions, fieldModalVisible, fieldLoading, fieldSaving, fieldModalTitle,
    fieldRows, currentFieldDataset, sqlPreviewVisible, sqlPreviewLoading, sqlPreviewColumns, sqlPreviewRows, sqlPreviewScrollX, roleOptions,
    userOptions, orgTreeOptions, permissionOptionsLoaded, permissionOptionsLoading, activeCategoryScope, selectedCategoryId, currentFormMode, currentEditingDataset,
    currentStep, stepDefinitions, totalSteps, queryForm, datasetTypeOptions, statusOptions, resultEncodingOptions, publishStatusOptions,
    datasetImpactLimit, datasetImpactVisibleLimit, accessModeOptions, aclSubjectTypeOptions, accessLevelOptions, rowScopeAttributeOptions, rowScopeLogicOptions, dataTypeOptions,
    fieldRoleOptions, sensitiveLevelOptions, maskRuleOptions, dateFormatOptions, dataUnitOptions, supportedParamOperators, isFormReadOnly, fieldConfigReadonly,
    selectedCategoryNode, selectedTreeKeys, currentStepMeta, formModeLabel, stepNavigationNote, stepProgressPercent, stepShellStyle, stepProgressWrapStyle,
    stepProgressBaseLineStyle, stepProgressActiveLineStyle, stepNavigationWrapperStyle, stepNavigationActionsStyle, stepNavigationMetaStyle, activeCategoryScopeLabel, categoryTreeNodes, categoryTreeSelectOptions,
    fieldConfigStats, tableColumns, fieldColumns, fieldTableScrollX, editSchema,
  }
}
