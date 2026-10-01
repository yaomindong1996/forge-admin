/** crud-generator.vue setup part 1. */
import { AddOutline, ArrowBackOutline, ChevronBackOutline, ChevronDownOutline, ChevronForwardOutline, ChevronUpOutline, CloseOutline, CopyOutline, DownloadOutline, PaperPlaneOutline, SaveOutline, ServerOutline, SparklesOutline } from '@vicons/ionicons5'
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCrudGenerator } from '@/composables/useCrudGenerator'
import { managedFetch } from '@/composables/useGlobalLoading'
import { request } from '@/utils'
import ApiConfigEditor from '../components/ApiConfigEditor.vue'
import DesensitizeConfigPanel from '../components/DesensitizeConfigPanel.vue'
import DictConfigPanel from '../components/DictConfigPanel.vue'
import EncryptConfigPanel from '../components/EncryptConfigPanel.vue'
import ImportDbTableModal from '../components/ImportDbTableModal.vue'
import MenuTreeSelectModal from '../components/MenuTreeSelectModal.vue'
import SchemaFieldEditor from '../components/SchemaFieldEditor.vue'
import TransConfigPanel from '../components/TransConfigPanel.vue'
export function applyCrudGeneratorPart1() {
  const __impl = {}
  const mut = {}

  const route = useRoute()
  const router = useRouter()

  const {
    sessionId,
    messages,
    configKey,
    tableName,
    generating,
    activeFile,
    inputText,
    sessionList,
    displayContent,

    layoutType,
    templateList,

    providerId,
    modelId,
    providerOptions,
    modelOptions,
    currentStage,

    configSaved,
    loadTemplateList,
    loadProviderOptions,
    loadModelOptions,
    loadSessionList,
    startNewSession,
    loadSession,
    deleteSession,
    sendMessage,
    abortGenerate,
    saveConfig,
    loadTableStructure,
    initWithConfigKey,
    previewCrudPage,
    copyCurrentFile,
    exportAllFiles,
  } = useCrudGenerator()

  const expandedReasonings = ref({})
  const messageListRef = ref(null)
  const sidebarCollapsed = ref(false)
  const previewCollapsed = ref(false)
  const activeTabGroup = ref('core')
  const showModelPanel = ref(false)

  const currentProviderLabel = computed(() => {
    const item = providerOptions.value.find(p => p.value === providerId.value)
    return item?.label || '未选择供应商'
  })

  const currentModelLabel = computed(() => {
    const item = modelOptions.value.find(m => m.value === modelId.value)
    if (!item)
      return '请选择模型'
    return item.modelCode || item.label
  })

  watch(providerId, async (val, old) => {
    if (val && val !== old)
      await loadModelOptions(val, false)
  })

  const generateStages = [
    { key: 'analyzing', label: '分析需求' },
    { key: 'generating-meta', label: '推断元数据' },
    { key: 'generating-search', label: '搜索配置' },
    { key: 'generating-columns', label: '表格列' },
    { key: 'generating-edit', label: '编辑表单' },
    { key: 'generating-api', label: '接口配置' },
    { key: 'generating-sql', label: '建表 SQL' },
  ]

  const currentStageIndex = computed(() => {
    return generateStages.findIndex(s => s.key === currentStage.value)
  })

  const moreActionOptions = computed(() => [
    { label: '预览页面', key: 'preview', disabled: !configSaved.value },
    { label: '下载代码', key: 'download', disabled: !configSaved.value },
  ])

  function handleMoreAction(key) {
    if (key === 'preview')
      previewCrudPage()
    if (key === 'download')
      handleDownloadCode()
  }

  function switchTabGroup(group) {
    activeTabGroup.value = group
    // 切换分组时自动选中该组第一个tab
    const defaultTabs = {
      core: 'searchSchema',
      advanced: 'dictConfig',
      sql: 'createTableSql',
    }
    if (defaultTabs[group]) {
      activeFile.value = defaultTabs[group]
    }
  }

  function scrollToBottom() {
    nextTick(() => {
      if (messageListRef.value) {
        messageListRef.value.scrollTop = messageListRef.value.scrollHeight
      }
    })
  }

  watch(() => messages.value.length, () => scrollToBottom())
  watch(generating, (val) => {
    if (val)
      scrollToBottom()
  })

  const examplePrompts = [
    { label: '员工管理', text: '员工管理，包含姓名、工号、部门、职位、入职日期、手机号、邮箱、状态', tableName: 'sys_employee' },
    { label: '产品目录', text: '产品管理，包含产品名称、分类、价格、库存、状态、创建时间', tableName: 'biz_product' },
    { label: '订单系统', text: '订单管理，包含订单号、客户名称、订单金额、支付状态、下单时间、备注', tableName: 'biz_order' },
  ]

  function fillExample(example) {
    inputText.value = example.text
    if (example.tableName) {
      tableName.value = example.tableName
    }
    autoGenerateConfigKey()
  }

  const configKeyError = ref('')
  const tableNameError = ref('')

  function validateConfigKey(val) {
    if (!val) {
      configKeyError.value = ''
      return
    }
    if (/[\u4E00-\u9FA5]/.test(val)) {
      configKeyError.value = '页面标识不能包含中文'
    }
    else if (!/^[a-z][a-z0-9_]{1,63}$/.test(val)) {
      configKeyError.value = '格式：小写字母开头，仅小写字母+数字+下划线，长度2-64位'
    }
    else {
      configKeyError.value = ''
    }
  }

  function validateTableName(val) {
    if (!val) {
      tableNameError.value = ''
      return
    }
    if (/[\u4E00-\u9FA5]/.test(val)) {
      tableNameError.value = '表名不能包含中文'
    }
    else if (!/^[a-z_]\w{0,127}$/i.test(val)) {
      tableNameError.value = '格式：字母/下划线开头，仅字母+数字+下划线'
    }
    else {
      tableNameError.value = ''
    }
  }

  watch(configKey, validateConfigKey)
  watch(tableName, validateTableName)

  const currentFileContent = computed({
    get: () => displayContent.value[activeFile.value] || '',
    set: (val) => { displayContent.value[activeFile.value] = val },
  })

  const tableOptions = ref([])
  const showImportModal = ref(false)
  const showMenuModal = ref(false)

  async function loadTableOptions() {
    try {
      const res = await request.get('/generator/list', { params: { pageNum: 1, pageSize: 100 } })
      if (res.code === 200) {
        const records = res.data?.records || []
        tableOptions.value = records.map(item => ({
          label: `${item.tableName} (${item.tableComment || ''})`,
          value: item.tableName,
        }))
      }
    }
    catch (e) {
      console.error('加载表列表失败:', e)
    }
  }

  function handleTableSelect(selectedTableName) {
    tableName.value = selectedTableName
    if (selectedTableName) {
      loadTableStructure(selectedTableName)
    }
  }

  function handleImportSuccess(importedTableName) {
    tableName.value = importedTableName
    loadTableOptions()
    loadTableStructure(importedTableName)
  }

  watch(tableName, (val) => {
    loadTableStructure(val)
  })

  function openSaveModal() {
    if (configSaved.value) {
      saveConfig({})
      return
    }
    showMenuModal.value = true
  }

  async function handleMenuConfirm({ menuParentId, menuName }) {
    showMenuModal.value = false
    await saveConfig({ menuParentId, menuName })
  }

  function formatTime(time) {
    if (!time)
      return ''
    const d = new Date(time)
    return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
  }

  function getStageLabel(stage) {
    const labels = {
      'analyzing': '分析阶段',
      'generating-meta': '推断元数据',
      'generating-search': '生成搜索配置',
      'generating-columns': '生成表格列',
      'generating-edit': '生成编辑表单',
      'generating-api': '生成API配置',
      'generating-sql': '生成建表SQL',
      'complete': '完成',
      'error': '错误',
      'retrying': '重试中',
    }
    return labels[stage] || stage
  }

  function toggleReasoning(idx) {
    expandedReasonings.value[idx] = !expandedReasonings.value[idx]
  }

  function autoGenerateConfigKey() {
    let key = ''

    if (tableName.value) {
      key = tableName.value.toLowerCase().replace(/^sys_/, '').replace(/[^a-z0-9]/g, '_')
    }
    else if (inputText.value && inputText.value.trim()) {
      const text = inputText.value.trim()
      const keywordMap = {
        管理: 'manage',
        系统: 'system',
        配置: 'config',
        设置: 'setting',
        列表: 'list',
        详情: 'detail',
        信息: 'info',
        记录: 'record',
        用户: 'user',
        角色: 'role',
        权限: 'permission',
        菜单: 'menu',
        部门: 'department',
        岗位: 'position',
        字典: 'dict',
        日志: 'log',
        订单: 'order',
        产品: 'product',
        商品: 'goods',
        客户: 'customer',
        员工: 'employee',
        考勤: 'attendance',
        薪资: 'salary',
        招聘: 'recruit',
      }
      const words = []
      for (const [cn, en] of Object.entries(keywordMap)) {
        if (text.includes(cn)) {
          words.push(en)
        }
      }
      if (words.length > 0) {
        key = words.slice(0, 3).join('_')
      }
    }

    if (!key) {
      const now = new Date()
      key = `page_${now.getMonth() + 1}${now.getDate()}_${Math.random().toString(36).substr(2, 4)}`
    }

    key = key.replace(/_+/g, '_').replace(/^_|_$/g, '')
    if (!key || key.length < 2) {
      key = `page_${Math.random().toString(36).substr(2, 4)}`
    }

    configKey.value = key
  }

  function goBack() {
    router.push('/ai/crud-config')
  }

  async function handleDownloadCode() {
    if (!configKey.value) {
      window.$message?.warning('请先输入 configKey')
      return
    }
    if (!configSaved.value) {
      window.$message?.warning('请先保存配置')
      return
    }
    const { useAuthStore } = await import('@/store')
    const authStore = useAuthStore()
    const BASE_URL = import.meta.env.VITE_REQUEST_PREFIX || ''
    const url = `${BASE_URL}/ai/crud-config/codegen/download/${configKey.value}`
    const uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = Math.random() * 16 | 0
      return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16)
    })
    try {
      const resp = await managedFetch(url, {
        method: 'GET',
        headers: {
          'Authorization': authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
          'X-Timestamp': Date.now().toString(),
          'X-Nonce': uuid,
        },
      }, {
        globalLoadingType: 'download',
        globalLoadingText: '文件下载处理中，请稍候...',
      })
      if (!resp.ok) {
        const text = await resp.text()
        window.$message?.error(`下载失败: ${text || resp.statusText}`)
        return
      }
      const blob = await resp.blob()
      const blobUrl = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = blobUrl
      a.download = `${configKey.value}-code.zip`
      a.click()
      URL.revokeObjectURL(blobUrl)
      window.$message?.success('代码包下载成功')
    }
    catch (e) {
      window.$message?.error(`下载失败: ${e.message}`)
    }
  }

  function handleNewSession() {
    startNewSession()
  }

  const executingSql = ref(false)
  async function executeCreateTableSql() {
    const sql = displayContent.value.createTableSql
    if (!sql || !sql.trim()) {
      window.$message.warning('请先生成建表SQL')
      return
    }
    window.$dialog.warning({
      title: '执行建表',
      content: '确认要执行该 CREATE TABLE 语句吗？若表已存在将会报错。',
      positiveText: '执行',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          executingSql.value = true
          await request.post('/generator/executeSql', { sql })
          window.$message.success('建表成功！可将该表导入并使用。')
          loadTableOptions()
        }
        catch (e) {
          const msg = e?.response?.data?.msg || e?.message || '执行失败'
          window.$message.error(msg)
        }
        finally {
          executingSql.value = false
        }
      },
    })
  }

  onMounted(async () => {
    loadTableOptions()
    loadTemplateList()
    loadProviderOptions()
    const ck = route.query.configKey
    if (ck) {
      await initWithConfigKey(ck)
    }
    else {
      await loadSessionList()
    }
  })
  __impl.switchTabGroup = switchTabGroup
  __impl.scrollToBottom = scrollToBottom
  __impl.fillExample = fillExample
  __impl.validateConfigKey = validateConfigKey
  __impl.validateTableName = validateTableName
  __impl.loadTableOptions = loadTableOptions
  __impl.handleTableSelect = handleTableSelect
  __impl.handleImportSuccess = handleImportSuccess
  __impl.openSaveModal = openSaveModal
  __impl.handleMenuConfirm = handleMenuConfirm
  __impl.formatTime = formatTime
  __impl.getStageLabel = getStageLabel
  __impl.toggleReasoning = toggleReasoning
  __impl.autoGenerateConfigKey = autoGenerateConfigKey
  __impl.goBack = goBack
  __impl.handleDownloadCode = handleDownloadCode
  __impl.handleNewSession = handleNewSession
  __impl.executeCreateTableSql = executeCreateTableSql

  return {
    __impl, mut, autoGenerateConfigKey, executeCreateTableSql, fillExample, formatTime, getStageLabel, goBack,
    handleDownloadCode, handleImportSuccess, handleMenuConfirm, handleMoreAction, handleNewSession, handleTableSelect, loadTableOptions, openSaveModal,
    scrollToBottom, switchTabGroup, toggleReasoning, validateConfigKey, validateTableName, route, router, expandedReasonings,
    messageListRef, sidebarCollapsed, previewCollapsed, activeTabGroup, showModelPanel, currentProviderLabel, currentModelLabel, generateStages,
    currentStageIndex, moreActionOptions, examplePrompts, configKeyError, tableNameError, currentFileContent, tableOptions, showImportModal,
    showMenuModal, executingSql,
    sessionId, messages, configKey, tableName, generating, activeFile, inputText, sessionList, displayContent,
    layoutType, templateList, providerId, modelId, providerOptions, modelOptions, currentStage, configSaved,
    loadTemplateList, loadProviderOptions, loadModelOptions, loadSessionList, startNewSession, loadSession, deleteSession,
    sendMessage, abortGenerate, saveConfig, loadTableStructure, initWithConfigKey, previewCrudPage, copyCurrentFile, exportAllFiles,
  }
}
