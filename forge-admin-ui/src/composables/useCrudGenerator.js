import { ref } from 'vue'
import { streamGenerate, crudGeneratorSessionList, crudGeneratorSessionMessages, crudGeneratorSessionDelete } from '@/api/crud-generator'
import { crudConfigAdd, crudConfigUpdate, crudConfigGetByKey, updateSessionMetadata } from '@/api/ai'
import { request } from '@/utils'
import { useDiscreteMessage } from '@/composables/useDiscreteMessage'

const AGENT_CODE = 'crud_config_builder'

function generateSessionId() {
  return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

export function useCrudGenerator() {
  const { success, error, warning } = useDiscreteMessage()

  const sessionId = ref(null)
  const messages = ref([])
  const configKey = ref('')
  const tableName = ref('')
  const description = ref('')
  const generating = ref(false)
  const currentStage = ref('')
  const currentStageMessage = ref('')
  const generatedFiles = ref({
    searchSchema: '',
    columnsSchema: '',
    editSchema: '',
    apiConfig: '',
    createTableSql: '',
    tableStructure: '',
  })
  const activeFile = ref('searchSchema')
  const inputText = ref('')
  const sessionList = ref([])
  const abortController = ref(null)
  const rawContent = ref('')
  const typingQueue = ref([])
  const typingTimer = ref(null)
  const currentReceivingFile = ref(null)
  const lastLoadedTableName = ref('')
  const displayContent = ref({
    searchSchema: '',
    columnsSchema: '',
    editSchema: '',
    apiConfig: '',
    createTableSql: '',
    tableStructure: '',
    dictConfig: '',
    desensitizeConfig: '',
    encryptConfig: '',
    transConfig: '',
  })

  async function loadSessionList() {
    try {
      const res = await crudGeneratorSessionList({ pageNum: 1, pageSize: 20, agentCode: AGENT_CODE })
      sessionList.value = res.data?.records || []
    } catch (e) {
      error('加载会话列表失败: ' + e.message)
    }
  }

  function startNewSession() {
    sessionId.value = generateSessionId()
    messages.value = []
    rawContent.value = ''
    generatedFiles.value = { searchSchema: '', columnsSchema: '', editSchema: '', apiConfig: '', createTableSql: '', tableStructure: '', dictConfig: '', desensitizeConfig: '', encryptConfig: '', transConfig: '' }
    displayContent.value = { searchSchema: '', columnsSchema: '', editSchema: '', apiConfig: '', createTableSql: '', tableStructure: '', dictConfig: '', desensitizeConfig: '', encryptConfig: '', transConfig: '' }
    currentReceivingFile.value = null
    currentStage.value = ''
    currentStageMessage.value = ''
    generating.value = false
    configSaved.value = false
    stopTyping()
  }

  async function loadSession(id) {
    try {
      const res = await crudGeneratorSessionMessages(id)
      sessionId.value = id
      messages.value = res.data || []
      rawContent.value = ''
      generatedFiles.value = { searchSchema: '', columnsSchema: '', editSchema: '', apiConfig: '', createTableSql: '', tableStructure: '', dictConfig: '', desensitizeConfig: '', encryptConfig: '', transConfig: '' }
      displayContent.value = { searchSchema: '', columnsSchema: '', editSchema: '', apiConfig: '', createTableSql: '', tableStructure: '', dictConfig: '', desensitizeConfig: '', encryptConfig: '', transConfig: '' }
      configSaved.value = false

      // 从会话列表中查找该会话的 metadata
      const sessionItem = sessionList.value.find(s => s.id === id)
      if (sessionItem && sessionItem.metadata) {
        const meta = sessionItem.metadata
        if (meta.configKey) {
          configKey.value = meta.configKey
          tableName.value = meta.tableName || ''
          description.value = meta.description || ''
          // 加载已保存的配置
          try {
            const configRes = await crudConfigGetByKey(meta.configKey)
            if (configRes.data) {
              displayContent.value.searchSchema = configRes.data.searchSchema || ''
              displayContent.value.columnsSchema = configRes.data.columnsSchema || ''
              displayContent.value.editSchema = configRes.data.editSchema || ''
              displayContent.value.apiConfig = configRes.data.apiConfig || ''
              displayContent.value.dictConfig = configRes.data.dictConfig || ''
              displayContent.value.desensitizeConfig = configRes.data.desensitizeConfig || ''
              displayContent.value.encryptConfig = configRes.data.encryptConfig || ''
              displayContent.value.transConfig = configRes.data.transConfig || ''
              configSaved.value = true
              success('已恢复会话关联的配置')
            }
          } catch (e) {
            console.warn('[useCrudGenerator] 加载配置失败:', e.message)
          }
          // 加载表结构
          if (tableName.value) {
            await loadTableStructure(tableName.value)
          }
        }
      }
    } catch (e) {
      error('加载会话失败: ' + e.message)
    }
  }

  async function deleteSession(id) {
    try {
      await crudGeneratorSessionDelete(id)
      success('会话已删除')
      if (sessionId.value === id) {
        startNewSession()
      }
      await loadSessionList()
    } catch (e) {
      error('删除会话失败: ' + e.message)
    }
  }

  function sendMessage() {
    if (!configKey.value) {
      warning('请输入 configKey')
      return
    }
    if (/[\u4e00-\u9fa5]/.test(configKey.value)) {
      warning('页面标识不能包含中文')
      return
    }
    if (!configKey.value.match(/^[a-z][a-z0-9_]{1,63}$/)) {
      warning('configKey 格式不正确')
      return
    }
    if (tableName.value && /[\u4e00-\u9fa5]/.test(tableName.value)) {
      warning('关联表名不能包含中文')
      return
    }
    if (!inputText.value.trim()) {
      warning('请输入描述内容')
      return
    }

    if (!sessionId.value) {
      sessionId.value = generateSessionId()
    }

    generating.value = true
    rawContent.value = ''
    generatedFiles.value = { searchSchema: '', columnsSchema: '', editSchema: '', apiConfig: '', createTableSql: '', tableStructure: '' }
    displayContent.value = { searchSchema: '', columnsSchema: '', editSchema: '', apiConfig: '', createTableSql: '', tableStructure: '' }
    typingQueue.value = []
    currentReceivingFile.value = null
    currentStage.value = 'analyzing'
    currentStageMessage.value = '正在分析需求...'

    messages.value.push({
      role: 'user',
      content: inputText.value,
      createTime: new Date().toISOString(),
    })

    messages.value.push({
      role: 'assistant',
      content: currentStageMessage.value,
      stage: currentStage.value,
      streaming: true,
      createTime: new Date().toISOString(),
    })

    const request = {
      sessionId: sessionId.value,
      configKey: configKey.value,
      tableName: tableName.value || undefined,
      description: inputText.value,
    }

    // 如果有现有配置，作为迭代上下文传入
    const hasExisting = displayContent.value.searchSchema || displayContent.value.columnsSchema
      || displayContent.value.editSchema || displayContent.value.apiConfig
    if (hasExisting) {
      request.existingSearchSchema = displayContent.value.searchSchema || undefined
      request.existingColumnsSchema = displayContent.value.columnsSchema || undefined
      request.existingEditSchema = displayContent.value.editSchema || undefined
      request.existingApiConfig = displayContent.value.apiConfig || undefined
    }

    abortController.value = streamGenerate(
      request,
      handleSSEChunk,
      handleSSEComplete,
      handleSSEError
    )

    inputText.value = ''
  }

  function handleSSEChunk({ event, data }) {
    if (event === 'progress') {
      currentStage.value = data.stage
      currentStageMessage.value = data.message
      updateLastAssistantMessage(data.message)

      if (data.stage.startsWith('generating-')) {
        const stageToFile = {
          'generating-search': 'searchSchema',
          'generating-columns': 'columnsSchema',
          'generating-edit': 'editSchema',
          'generating-api': 'apiConfig',
          'generating-sql': 'createTableSql',
        }
        const fileKey = stageToFile[data.stage]
        if (fileKey) {
          activeFile.value = fileKey
          currentReceivingFile.value = fileKey
        }
      }
    } else if (event === 'meta') {
      // AI 推断的元数据
      if (data.configKey && !configKey.value) configKey.value = data.configKey
      if (data.tableName && !tableName.value) tableName.value = data.tableName
      if (data.tableComment && !description.value) description.value = data.tableComment
      updateLastAssistantMessage(`已推断: configKey=${data.configKey || '-'}, tableName=${data.tableName || '-'}`)
    } else if (event === 'chunk') {
      rawContent.value += data.content
      // 实时追加到当前阶段的 displayContent，使右侧预览区实时更新
      if (currentReceivingFile.value) {
        displayContent.value[currentReceivingFile.value] =
          (displayContent.value[currentReceivingFile.value] || '') + data.content
      } else {
        // 还没收到 progress 阶段事件时，先写入 searchSchema 作为默认预览
        displayContent.value.searchSchema =
          (displayContent.value.searchSchema || '') + data.content
      }
    }
  }

  function handleSSEComplete(data) {
    generating.value = false
    currentStage.value = 'complete'
    currentStageMessage.value = '生成完成！'

    // 先尝试对已有的分阶段内容做 JSON 格式化
    tryFormatDisplayContent()

    // 检测是否所有 schema 都成功解析出合法内容
    // 如果只有 1 个字段有内容（说明 AI 没按 [STAGE:xxx] 分段，所有内容都堆到了默认 tab），
    // 或者全部为空，则从 rawContent 整体解析
    const filledKeys = ['searchSchema', 'columnsSchema', 'editSchema', 'apiConfig', 'createTableSql', 'tableStructure', 'dictConfig', 'desensitizeConfig', 'encryptConfig', 'transConfig']
      .filter(k => displayContent.value[k] && displayContent.value[k].trim())
    if (filledKeys.length <= 1 && rawContent.value.trim()) {
      // 清空 displayContent 避免残留
      displayContent.value = { searchSchema: '', columnsSchema: '', editSchema: '', apiConfig: '', createTableSql: '', tableStructure: '', dictConfig: '', desensitizeConfig: '', encryptConfig: '', transConfig: '' }
      parseRawContentFallback(rawContent.value)
    }

    updateLastAssistantMessage('✅ 生成完成！右侧预览区可查看并编辑配置内容。')

    // 生成完成后，异步同步会话元数据（此时服务端会话已建立）
    if (sessionId.value && configKey.value) {
      updateSessionMetadata(sessionId.value, {
        configKey: configKey.value,
        tableName: tableName.value,
        description: description.value,
      }).catch(e => {
        console.warn('[useCrudGenerator] 生成完成后同步会话元数据失败:', e.message)
      })
    }
  }

  /**
   * 对每个 displayContent 的层展内容尝试做 JSON 格式化
   * 流式接收后的最终清理：如果内容是合法 JSON 则美化，否则保留原始内容
   */
  function tryFormatDisplayContent() {
    const keys = ['searchSchema', 'columnsSchema', 'editSchema', 'apiConfig', 'createTableSql', 'dictConfig', 'desensitizeConfig', 'encryptConfig', 'transConfig']
    for (const key of keys) {
      const raw = displayContent.value[key]
      if (!raw) continue
      try {
        const parsed = JSON.parse(raw)
        const formatted = JSON.stringify(parsed, null, 2)
        displayContent.value[key] = formatted
        generatedFiles.value[key] = formatted
      } catch (e) {
        // 保留原始内容，同步到 generatedFiles
        generatedFiles.value[key] = raw
      }
    }
  }

  /**
   * 兜底解析：当 AI 未按 [STAGE:xxx] 格式输出时，
   * 从 rawContent 整体解析一个完整的 JSON 对象并分发到各 schema 字段
   */
  function parseRawContentFallback(content) {
    let jsonStr = content.trim()

    // 去掉 markdown 代码块包裹
    if (jsonStr.includes('```json')) {
      jsonStr = jsonStr.substring(jsonStr.indexOf('```json') + 7)
      jsonStr = jsonStr.substring(0, jsonStr.indexOf('```'))
    } else if (jsonStr.includes('```')) {
      jsonStr = jsonStr.substring(jsonStr.indexOf('```') + 3)
      jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf('```'))
    }
    jsonStr = jsonStr.trim()

    // 尝试从 [STAGE:xxx] 标记分段解析
    const stageMap = parseStageMarkers(jsonStr)
    if (stageMap) {
      applyStageMap(stageMap)
      return
    }

    // 直接按完整 JSON 解析
    try {
      const parsed = JSON.parse(jsonStr)
      const keys = ['searchSchema', 'columnsSchema', 'editSchema', 'apiConfig', 'createTableSql', 'dictConfig', 'desensitizeConfig', 'encryptConfig', 'transConfig']
      for (const key of keys) {
        if (parsed[key] !== undefined && parsed[key] !== null) {
          const formatted = JSON.stringify(parsed[key], null, 2)
          displayContent.value[key] = formatted
          generatedFiles.value[key] = formatted
        }
      }
    } catch (e) {
      console.warn('[useCrudGenerator] 兜底 JSON 解析失败，保留原始内容:', e.message)
      // 最后兜底：将所有内容放入 searchSchema
      displayContent.value.searchSchema = jsonStr
      generatedFiles.value.searchSchema = jsonStr
    }
  }

  /**
   * 尝试按 [STAGE:xxx] 标记分段解析内容
   * @returns {Object|null} stageMap 或 null
   */
  function parseStageMarkers(content) {
    const stageRegex = /\[STAGE:(\w+)\]/g
    let match
    const positions = []
    while ((match = stageRegex.exec(content)) !== null) {
      positions.push({ stage: match[1], index: match.index, markerLen: match[0].length })
    }
    if (positions.length === 0) return null

    const stageMap = {}
    for (let i = 0; i < positions.length; i++) {
      const start = positions[i].index + positions[i].markerLen
      const end = i + 1 < positions.length ? positions[i + 1].index : content.length
      const jsonPart = content.slice(start, end).trim()
      stageMap[positions[i].stage] = jsonPart
    }
    return stageMap
  }

  /**
   * 将 stageMap 的内容写入 displayContent 和 generatedFiles
   */
  function applyStageMap(stageMap) {
    for (const [stage, jsonStr] of Object.entries(stageMap)) {
      if (!jsonStr) continue
      if (stage === 'meta') {
        try {
          const parsed = JSON.parse(jsonStr)
          if (parsed.configKey && !configKey.value) configKey.value = parsed.configKey
          if (parsed.tableName && !tableName.value) tableName.value = parsed.tableName
          if (parsed.tableComment && !description.value) description.value = parsed.tableComment
        } catch (e) {
          console.warn('[useCrudGenerator] meta 解析失败:', e.message)
        }
        continue
      }
      // 找到对应的 key（如 searchSchema）
      const targetKey = ['searchSchema', 'columnsSchema', 'editSchema', 'apiConfig', 'createTableSql', 'tableStructure', 'dictConfig', 'desensitizeConfig', 'encryptConfig', 'transConfig']
        .find(k => k === stage)
      if (!targetKey) continue
      try {
        const parsed = JSON.parse(jsonStr)
        // AI 可能输出 {"searchSchema": [...]} 或直接 [...]
        const value = parsed[targetKey] !== undefined ? parsed[targetKey] : parsed
        const formatted = JSON.stringify(value, null, 2)
        displayContent.value[targetKey] = formatted
        generatedFiles.value[targetKey] = formatted
      } catch (e) {
        displayContent.value[targetKey] = jsonStr
        generatedFiles.value[targetKey] = jsonStr
      }
    }
  }

  function stopTyping() {}

  function handleSSEError(msg) {
    generating.value = false
    currentStage.value = 'error'
    stopTyping()
    updateLastAssistantMessage('❌ 生成失败: ' + msg)
  }

  function updateLastAssistantMessage(content) {
    const lastAssistant = messages.value.filter(m => m.role === 'assistant').pop()
    if (lastAssistant) {
      lastAssistant.content = content
      lastAssistant.streaming = generating.value
    }
  }

  function abortGenerate() {
    if (abortController.value) {
      abortController.value.abort()
      abortController.value = null
      generating.value = false
      stopTyping()
      updateLastAssistantMessage('已停止生成')
    }
  }

  const configSaved = ref(false)

  async function saveConfig(menuOptions = {}) {
    if (!configKey.value) {
      warning('请输入 configKey')
      return
    }

    // 检查是否有内容可保存
    const hasContent = ['searchSchema', 'columnsSchema', 'editSchema', 'apiConfig', 'createTableSql', 'dictConfig', 'desensitizeConfig', 'encryptConfig', 'transConfig']
      .some(k => displayContent.value[k] && displayContent.value[k].trim())
    if (!hasContent) {
      warning('暂无可保存的配置内容')
      return
    }

    try {
      const existing = await crudConfigGetByKey(configKey.value)

      // 直接从 displayContent 读取（用户可能手动编辑过预览区内容）
      const payload = {
        configKey: configKey.value,
        tableName: tableName.value || configKey.value,
        tableComment: description.value || configKey.value,
        menuName: menuOptions.menuName || description.value || configKey.value,
        menuParentId: menuOptions.menuParentId || null,
        searchSchema: displayContent.value.searchSchema || '[]',
        columnsSchema: displayContent.value.columnsSchema || '[]',
        editSchema: displayContent.value.editSchema || '[]',
        apiConfig: displayContent.value.apiConfig || '{}',
        dictConfig: displayContent.value.dictConfig || '',
        desensitizeConfig: displayContent.value.desensitizeConfig || '',
        encryptConfig: displayContent.value.encryptConfig || '',
        transConfig: displayContent.value.transConfig || '',
        mode: 'CONFIG',
        status: '0',
      }

      if (existing.data) {
        payload.id = existing.data.id
        await crudConfigUpdate(payload)
        success('配置已更新')
      } else {
        await crudConfigAdd(payload)
        success('配置已保存')
      }
      configSaved.value = true
    } catch (e) {
      error('保存配置失败: ' + e.message)
    }
  }

  async function loadTableStructure(name) {
    if (!name) {
      displayContent.value.tableStructure = ''
      lastLoadedTableName.value = ''
      return
    }
    if (lastLoadedTableName.value === name) return
    try {
      const res = await request.get(`/generator/column/db/${name}`)
      if (res.code === 200 && res.data) {
        const columns = res.data
        if (columns.length > 0) {
          const lines = [
            `| 字段名 | 类型 | 注释 | 主键 | 自增 | 必填 |`,
            `|--------|------|------|------|------|------|`,
          ]
          for (const col of columns) {
            lines.push(
              `| ${col.columnName || ''} | ${col.columnType || ''} | ${col.columnComment || ''} | ${col.isPk ? '是' : ''} | ${col.isIncrement ? '是' : ''} | ${col.isRequired ? '是' : ''} |`
            )
          }
          displayContent.value.tableStructure = lines.join('\n')
          generatedFiles.value.tableStructure = displayContent.value.tableStructure
        }
      }
    } catch (e) {
      console.warn('[useCrudGenerator] 加载表结构失败:', e.message)
      displayContent.value.tableStructure = ''
    }
    lastLoadedTableName.value = name
  }

  async function loadExistingConfig() {
    if (!configKey.value) return null
    try {
      const res = await crudConfigGetByKey(configKey.value)
      return res.data
    } catch (e) {
      return null
    }
  }

  /**
   * 从 crud-config 列表页跳转进来时，根据 configKey 初始化：
   * 策略：始终新建干净会话，并从数据库加载该 configKey 的最新已保存配置填入预览区，
   * 历史对话记录仅保留在侧边栏供查阅，不自动还原到主对话区（避免混乱）
   */
  async function initWithConfigKey(ck) {
    if (!ck) return
    // 1. 新建干净会话（清空消息记录和预览区）
    startNewSession()
    configKey.value = ck
    // 2. 先加载会话列表（侧边栏查阅用）
    await loadSessionList()
    // 3. 从数据库加载该 configKey 的最新已保存配置
    try {
      const configRes = await crudConfigGetByKey(ck)
      if (configRes.data) {
        displayContent.value.searchSchema = configRes.data.searchSchema || ''
        displayContent.value.columnsSchema = configRes.data.columnsSchema || ''
        displayContent.value.editSchema = configRes.data.editSchema || ''
        displayContent.value.apiConfig = configRes.data.apiConfig || ''
        displayContent.value.dictConfig = configRes.data.dictConfig || ''
        displayContent.value.desensitizeConfig = configRes.data.desensitizeConfig || ''
        displayContent.value.encryptConfig = configRes.data.encryptConfig || ''
        displayContent.value.transConfig = configRes.data.transConfig || ''
        tableName.value = configRes.data.tableName || ''
        description.value = configRes.data.tableComment || ''
        configSaved.value = true
        // 在对话区显示一条系统提示
        messages.value.push({
          role: 'assistant',
          content: `已加载配置「${ck}」的最新版本，你可以继续对话来迭代优化配置。`,
          createTime: new Date().toISOString(),
        })
      }
    } catch (e) {
      console.warn('[useCrudGenerator] initWithConfigKey 加载配置失败:', e.message)
    }
    // 4. 加载表结构
    if (tableName.value) {
      await loadTableStructure(tableName.value)
    }
  }

  function previewCrudPage() {
    if (!configKey.value) {
      warning('请先输入 configKey')
      return
    }
    if (!configSaved.value) {
      warning('请先保存配置')
      return
    }
    window.open(`/#/ai/crud-page/${configKey.value}`, '_blank')
  }

  function copyCurrentFile() {
    const content = generatedFiles.value[activeFile.value]
    if (!content) {
      warning('当前文件无内容')
      return
    }
    navigator.clipboard.writeText(content)
    success('已复制到剪贴板')
  }

  function exportAllFiles() {
    const fullConfig = {
      configKey: configKey.value,
      tableName: tableName.value,
      searchSchema: JSON.parse(generatedFiles.value.searchSchema || '[]'),
      columnsSchema: JSON.parse(generatedFiles.value.columnsSchema || '[]'),
      editSchema: JSON.parse(generatedFiles.value.editSchema || '[]'),
      apiConfig: JSON.parse(generatedFiles.value.apiConfig || '{}'),
      createTableSql: generatedFiles.value.createTableSql || '',
    }
    const blob = new Blob([JSON.stringify(fullConfig, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${configKey.value}-config.json`
    a.click()
    URL.revokeObjectURL(url)
    success('已导出配置文件')
  }

  return {
    sessionId,
    messages,
    configKey,
    tableName,
    description,
    generating,
    currentStage,
    currentStageMessage,
    generatedFiles,
    activeFile,
    inputText,
    sessionList,
    rawContent,
    displayContent,

    loadSessionList,
    startNewSession,
    loadSession,
    deleteSession,
    sendMessage,
    abortGenerate,
    configSaved,
    saveConfig,
    loadExistingConfig,
    loadTableStructure,
    initWithConfigKey,
    previewCrudPage,
    copyCurrentFile,
    exportAllFiles,
  }
}