<template>
  <div class="crud-generator-page">
    <div :class="['generator-sidebar', { collapsed: sidebarCollapsed }]">
      <div class="sidebar-header">
        <template v-if="!sidebarCollapsed">
          <n-button size="small" text @click="goBack">
            <template #icon><n-icon><ArrowBackOutline /></n-icon></template>
            返回
          </n-button>
          <n-button size="small" type="primary" @click="handleNewSession">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            新对话
          </n-button>
        </template>
        <template v-else>
          <n-button size="small" quaternary circle @click="sidebarCollapsed = false">
            <template #icon><n-icon><ChevronForwardOutline /></n-icon></template>
          </n-button>
        </template>
      </div>
      <template v-if="!sidebarCollapsed">
        <div class="sidebar-section-title">
          <span>历史对话</span>
          <n-button size="tiny" quaternary circle @click="sidebarCollapsed = true">
            <template #icon><n-icon size="14"><ChevronBackOutline /></n-icon></template>
          </n-button>
        </div>
        <div class="session-list">
          <div
            v-for="session in sessionList"
            :key="session.id"
            :class="['session-item', { active: sessionId === session.id }]"
            @click="loadSession(session.id)"
          >
            <div class="session-title">
              <span v-if="session.metadata && session.metadata.configKey" class="session-config-key">{{ session.metadata.configKey }}</span>
              <span v-else>{{ session.sessionName || '未命名会话' }}</span>
            </div>
            <div class="session-meta">
              <span v-if="session.metadata && session.metadata.tableName" class="session-table">{{ session.metadata.tableName }}</span>
              <span class="session-time">{{ formatTime(session.createTime) }}</span>
            </div>
            <n-button class="delete-btn" size="tiny" text type="error" @click.stop="deleteSession(session.id)">
              <n-icon><CloseOutline /></n-icon>
            </n-button>
          </div>
          <div v-if="sessionList.length === 0" class="empty-tip">暂无历史对话</div>
        </div>
      </template>
    </div>

    <div class="generator-main">
      <div class="chat-area">
        <div class="message-list" ref="messageListRef">
          <div v-for="(msg, idx) in messages" :key="idx" :class="['message', msg.role]">
            <div class="message-avatar">
              <div v-if="msg.role === 'user'" class="avatar user-avatar">我</div>
              <div v-else class="avatar ai-avatar">AI</div>
            </div>
            <div class="message-body">
              <div v-if="msg.stage" class="stage-indicator">{{ getStageLabel(msg.stage) }}</div>
              <div class="message-bubble">
                <div class="message-content">{{ msg.content }}</div>
              </div>
            </div>
          </div>
          <div v-if="messages.length === 0" class="empty-chat">
            <div class="empty-chat-icon">💬</div>
            <div class="empty-chat-tip">输入你的需求，AI 将自动生成 CRUD 配置</div>
            <div class="example-prompts">
              <div class="example-title">试试这些示例：</div>
              <div class="example-list">
                <div v-for="example in examplePrompts" :key="example.label" class="example-item" @click="fillExample(example)">
                  <span class="example-label">{{ example.label }}</span>
                  <span class="example-desc">{{ example.text }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="input-section">
          <!-- 配置行 -->
          <div class="config-bar">
            <!-- 页面标识 -->
            <div :class="['config-field', { 'has-error': configKeyError }]">
              <span class="field-tag">标识</span>
              <n-input
                v-model:value="configKey"
                placeholder="如 employee_manage"
                size="small"
                :status="configKeyError ? 'error' : undefined"
                class="field-input"
              >
                <template #suffix>
                  <span class="auto-btn" @click="autoGenerateConfigKey">自动</span>
                </template>
              </n-input>
              <span v-if="configKeyError" class="field-error-tip">{{ configKeyError }}</span>
            </div>

            <div class="config-divider" />

            <!-- 关联表 -->
            <div class="config-field">
              <span class="field-tag">关联表</span>
              <n-select
                v-model:value="tableName"
                :options="tableOptions"
                placeholder="选择或搜索"
                clearable
                filterable
                size="small"
                class="field-input"
                @update:value="handleTableSelect"
              />
            </div>

            <div v-if="templateList.length > 0" class="config-divider" />

            <!-- 页面模板 -->
            <div v-if="templateList.length > 0" class="config-field config-field--shrink">
              <span class="field-tag">模板</span>
              <n-select
                v-model:value="layoutType"
                :options="templateList.map(t => ({ label: t.templateName, value: t.templateKey }))"
                size="small"
                class="field-input"
                :consistent-menu-width="false"
              />
            </div>

            <div class="config-divider" />

            <!-- 操作按钮 -->
            <n-button size="small" text type="primary" @click="showImportModal = true" class="bar-action-btn">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              导入表
            </n-button>
          </div>

          <!-- 输入框 -->
          <div class="input-area">
            <n-input
              v-model:value="inputText"
              type="textarea"
              :rows="3"
              placeholder="详细描述你需要的 CRUD 页面功能，如：员工管理系统，包含姓名、工号、部门、职位、入职日期、手机号、邮箱、状态等字段"
              style="flex: 1"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <div class="input-buttons">
              <n-button v-if="generating" type="error" @click="abortGenerate">
                <template #icon><n-icon><CloseOutline /></n-icon></template>
                停止
              </n-button>
              <n-button v-else type="primary" :disabled="!configKey || !inputText.trim()" @click="sendMessage">
                发送
              </n-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div :class="['generator-preview', { 'preview-collapsed': previewCollapsed }]">
      <!-- 折叠状态下的竖向标签栏 -->
      <div v-if="previewCollapsed" class="preview-collapsed-bar" @click="previewCollapsed = false">
        <n-icon size="16"><ChevronBackOutline /></n-icon>
        <span class="preview-collapsed-text">配置面板</span>
      </div>

      <template v-if="!previewCollapsed">
        <div class="preview-header">
          <div class="preview-header-left">
            <n-button size="small" quaternary circle @click="previewCollapsed = true" title="折叠配置面板">
              <template #icon><n-icon><ChevronForwardOutline /></n-icon></template>
            </n-button>
            <span class="preview-title">配置面板</span>
          </div>
          <div class="preview-actions">
            <n-button-group size="small">
              <n-button quaternary @click="copyCurrentFile" title="复制当前配置">
                <template #icon><n-icon><CopyOutline /></n-icon></template>
              </n-button>
              <n-button quaternary @click="exportAllFiles" title="导出全部配置">
                <template #icon><n-icon><DownloadOutline /></n-icon></template>
              </n-button>
            </n-button-group>
            <n-button type="primary" size="small" @click="openSaveModal">
              <template #icon><n-icon><SaveOutline /></n-icon></template>
              保存
            </n-button>
            <n-dropdown
              :options="moreActionOptions"
              @select="handleMoreAction"
              trigger="click"
            >
              <n-button size="small" :disabled="!configSaved">
                更多 ▾
              </n-button>
            </n-dropdown>
          </div>
        </div>

        <!-- Tab 分组：核心配置 / 高级配置 -->
        <div class="preview-tab-group">
          <div class="tab-group-switcher">
            <span
              :class="['group-tab', { active: activeTabGroup === 'core' }]"
              @click="switchTabGroup('core')"
            >核心配置</span>
            <span
              :class="['group-tab', { active: activeTabGroup === 'advanced' }]"
              @click="switchTabGroup('advanced')"
            >高级配置</span>
            <span
              :class="['group-tab', { active: activeTabGroup === 'sql' }]"
              @click="switchTabGroup('sql')"
            >SQL / 表结构</span>
          </div>
        </div>

        <div class="preview-tabs">
          <!-- 核心配置 -->
          <template v-if="activeTabGroup === 'core'">
            <n-tabs v-model:value="activeFile" type="line" animated :tabs-padding="12">
              <n-tab-pane name="searchSchema" tab="搜索配置">
                <div class="editor-area">
                  <SchemaFieldEditor
                    mode="search"
                    :value="displayContent.searchSchema"
                    :table-structure="displayContent.tableStructure"
                    @update:value="displayContent.searchSchema = $event"
                  />
                </div>
              </n-tab-pane>
              <n-tab-pane name="columnsSchema" tab="表格列">
                <div class="editor-area">
                  <SchemaFieldEditor
                    mode="columns"
                    :value="displayContent.columnsSchema"
                    :table-structure="displayContent.tableStructure"
                    @update:value="displayContent.columnsSchema = $event"
                  />
                </div>
              </n-tab-pane>
              <n-tab-pane name="editSchema" tab="编辑表单">
                <div class="editor-area">
                  <SchemaFieldEditor
                    mode="edit"
                    :value="displayContent.editSchema"
                    :table-structure="displayContent.tableStructure"
                    @update:value="displayContent.editSchema = $event"
                  />
                </div>
              </n-tab-pane>
              <n-tab-pane name="apiConfig" tab="接口配置">
                <div class="editor-area">
                  <ApiConfigEditor
                    :value="displayContent.apiConfig"
                    @update:value="displayContent.apiConfig = $event"
                  />
                </div>
              </n-tab-pane>
            </n-tabs>
          </template>

          <!-- 高级配置 -->
          <template v-if="activeTabGroup === 'advanced'">
            <n-tabs v-model:value="activeFile" type="line" animated :tabs-padding="12">
              <n-tab-pane name="dictConfig" tab="字典">
                <div class="editor-area">
                  <DictConfigPanel
                    :search-schema="displayContent.searchSchema"
                    :columns-schema="displayContent.columnsSchema"
                    :edit-schema="displayContent.editSchema"
                    :dict-config="displayContent.dictConfig"
                    @update:value="displayContent.dictConfig = $event"
                  />
                </div>
              </n-tab-pane>
              <n-tab-pane name="desensitizeConfig" tab="脱敏">
                <div class="editor-area">
                  <DesensitizeConfigPanel
                    :value="displayContent.desensitizeConfig"
                    :columns-schema="displayContent.columnsSchema"
                    @update:value="displayContent.desensitizeConfig = $event"
                  />
                </div>
              </n-tab-pane>
              <n-tab-pane name="encryptConfig" tab="加解密">
                <div class="editor-area">
                  <EncryptConfigPanel
                    :value="displayContent.encryptConfig"
                    @update:value="displayContent.encryptConfig = $event"
                  />
                </div>
              </n-tab-pane>
              <n-tab-pane name="transConfig" tab="翻译">
                <div class="editor-area">
                  <TransConfigPanel
                    :value="displayContent.transConfig"
                    :columns-schema="displayContent.columnsSchema"
                    :edit-schema="displayContent.editSchema"
                    @update:value="displayContent.transConfig = $event"
                  />
                </div>
              </n-tab-pane>
            </n-tabs>
          </template>

          <!-- SQL / 表结构 -->
          <template v-if="activeTabGroup === 'sql'">
            <n-tabs v-model:value="activeFile" type="line" animated :tabs-padding="12">
              <n-tab-pane name="createTableSql" tab="建表 SQL">
                <div class="editor-area sql-editor-wrap">
                  <textarea
                    v-model="currentFileContent"
                    class="json-editor"
                    :placeholder="generating ? '正在生成...' : '等待生成...'"
                    spellcheck="false"
                  />
                  <div class="sql-actions">
                    <n-button
                      type="warning"
                      size="small"
                      :loading="executingSql"
                      :disabled="!currentFileContent"
                      @click="executeCreateTableSql"
                    >
                      执行建表
                    </n-button>
                  </div>
                </div>
              </n-tab-pane>
              <n-tab-pane name="tableStructure" tab="表结构">
                <div class="editor-area">
                  <textarea
                    v-model="currentFileContent"
                    class="json-editor"
                    :placeholder="generating ? '正在生成...' : '等待生成...'"
                    spellcheck="false"
                  />
                </div>
              </n-tab-pane>
            </n-tabs>
          </template>

          <div v-if="generating && currentFileContent" class="typing-indicator">正在写入...</div>
        </div>
      </template>
    </div>
  </div>
  <ImportDbTableModal v-model:show="showImportModal" @success="handleImportSuccess" />
  <MenuTreeSelectModal
    v-model:show="showMenuModal"
    @confirm="handleMenuConfirm"
  />
</template>

<script setup>
import { onMounted, computed, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AddOutline, CloseOutline, ArrowBackOutline, CopyOutline, DownloadOutline, SaveOutline, EyeOutline, ChevronBackOutline, ChevronForwardOutline } from '@vicons/ionicons5'
import { useCrudGenerator } from '@/composables/useCrudGenerator'
import { request } from '@/utils'
import ImportDbTableModal from './components/ImportDbTableModal.vue'
import MenuTreeSelectModal from './components/MenuTreeSelectModal.vue'
import SchemaFieldEditor from './components/SchemaFieldEditor.vue'
import ApiConfigEditor from './components/ApiConfigEditor.vue'
import DictConfigPanel from './components/DictConfigPanel.vue'
import DesensitizeConfigPanel from './components/DesensitizeConfigPanel.vue'
import EncryptConfigPanel from './components/EncryptConfigPanel.vue'
import TransConfigPanel from './components/TransConfigPanel.vue'

defineOptions({ name: 'AiCrudGenerator' })

const route = useRoute()
const router = useRouter()

const {
  sessionId,
  messages,
  configKey,
  tableName,
  generating,
  generatedFiles,
  activeFile,
  inputText,
  sessionList,
  displayContent,

  layoutType,
  templateList,

  configSaved,
  loadTemplateList,
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

const messageListRef = ref(null)
const sidebarCollapsed = ref(false)
const previewCollapsed = ref(false)
const activeTabGroup = ref('core')
const showAdvancedInput = ref(false)

// 更多操作下拉选项
const moreActionOptions = computed(() => [
  { label: '预览页面', key: 'preview', disabled: !configSaved.value },
  { label: '下载代码', key: 'download', disabled: !configSaved.value },
])

function handleMoreAction(key) {
  if (key === 'preview') previewCrudPage()
  if (key === 'download') handleDownloadCode()
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
watch(generating, (val) => { if (val) scrollToBottom() })

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
  if (!val) { configKeyError.value = ''; return }
  if (/[\u4e00-\u9fa5]/.test(val)) {
    configKeyError.value = '页面标识不能包含中文'
  } else if (!/^[a-z][a-z0-9_]{1,63}$/.test(val)) {
    configKeyError.value = '格式：小写字母开头，仅小写字母+数字+下划线，长度2-64位'
  } else {
    configKeyError.value = ''
  }
}

function validateTableName(val) {
  if (!val) { tableNameError.value = ''; return }
  if (/[\u4e00-\u9fa5]/.test(val)) {
    tableNameError.value = '表名不能包含中文'
  } else if (!/^[a-zA-Z_][a-zA-Z0-9_]{0,127}$/.test(val)) {
    tableNameError.value = '格式：字母/下划线开头，仅字母+数字+下划线'
  } else {
    tableNameError.value = ''
  }
}

watch(configKey, validateConfigKey)
watch(tableName, validateTableName)

const currentFileContent = computed({
  get: () => displayContent.value[activeFile.value] || '',
  set: (val) => { displayContent.value[activeFile.value] = val }
})

const tableOptions = ref([])
const showImportModal = ref(false)
const showMenuModal = ref(false)

function getSchemaLabel(key) {
  const labelMap = {
    searchSchema: '搜索配置', columnsSchema: '表格列配置', editSchema: '编辑表单配置',
    apiConfig: '接口配置', dictConfig: '字典配置', desensitizeConfig: '脱敏配置',
    encryptConfig: '加解密配置', transConfig: '翻译配置', createTableSql: '建表SQL', tableStructure: '表结构',
  }
  return labelMap[key] || key
}

async function regenerateSingleSchema(schemaKey) {
  if (generating.value) {
    warning('正在生成中，请等待完成')
    return
  }
  if (!configKey.value) {
    warning('请先输入 configKey')
    return
  }
  if (!inputText.value.trim() && !description.value) {
    warning('请先输入需求描述')
    return
  }

  const schemaLabel = getSchemaLabel(schemaKey)
  const prompt = `请只生成"${schemaLabel}"部分的配置，其他部分不需要生成。${inputText.value || description.value}`

  generating.value = true
  activeFile.value = schemaKey
  currentReceivingFile.value = schemaKey
  
  displayContent.value[schemaKey] = ''
  generatedFiles.value[schemaKey] = ''

  const request = {
    sessionId: sessionId.value || generateSessionId(),
    configKey: configKey.value,
    tableName: tableName.value || undefined,
    description: prompt,
    singleSchema: schemaKey,
  }

  messages.value.push({
    role: 'user',
    content: `[重新生成${schemaLabel}] ${inputText.value || description.value}`,
    createTime: new Date().toISOString(),
  })

  messages.value.push({
    role: 'assistant',
    content: `正在重新生成${schemaLabel}...`,
    streaming: true,
    createTime: new Date().toISOString(),
  })

  abortController.value = streamGenerate(
    request,
    handleSSEChunk,
    handleSSEComplete,
    handleSSEError
  )
}

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
  } catch (e) {
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
  if (!time) return ''
  const d = new Date(time)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}

function getStageLabel(stage) {
  const labels = {
    analyzing: '分析阶段',
    'generating-meta': '推断元数据',
    'generating-search': '生成搜索配置',
    'generating-columns': '生成表格列',
    'generating-edit': '生成编辑表单',
    'generating-api': '生成API配置',
    'generating-sql': '生成建表SQL',
    complete: '完成',
    error: '错误',
    retrying: '重试中',
  }
  return labels[stage] || stage
}

function autoGenerateConfigKey() {
  let key = ''
  
  if (tableName.value) {
    key = tableName.value.toLowerCase().replace(/^sys_/, '').replace(/[^a-z0-9]/g, '_')
  }
  else if (inputText.value && inputText.value.trim()) {
    const text = inputText.value.trim()
    const keywordMap = {
      '管理': 'manage', '系统': 'system', '配置': 'config', '设置': 'setting',
      '列表': 'list', '详情': 'detail', '信息': 'info', '记录': 'record',
      '用户': 'user', '角色': 'role', '权限': 'permission', '菜单': 'menu',
      '部门': 'department', '岗位': 'position', '字典': 'dict', '日志': 'log',
      '订单': 'order', '产品': 'product', '商品': 'goods', '客户': 'customer',
      '员工': 'employee', '考勤': 'attendance', '薪资': 'salary', '招聘': 'recruit',
    }
    let words = []
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
    key = 'page_' + Math.random().toString(36).substr(2, 4)
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
    const resp = await fetch(url, {
      method: 'GET',
      headers: {
        Authorization: authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
        'X-Timestamp': Date.now().toString(),
        'X-Nonce': uuid,
      },
    })
    if (!resp.ok) {
      const text = await resp.text()
      window.$message?.error('下载失败: ' + (text || resp.statusText))
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
  } catch (e) {
    window.$message?.error('下载失败: ' + e.message)
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
      } catch (e) {
        const msg = e?.response?.data?.msg || e?.message || '执行失败'
        window.$message.error(msg)
      } finally {
        executingSql.value = false
      }
    },
  })
}

onMounted(async () => {
  loadTableOptions()
  loadTemplateList()
  const ck = route.query.configKey
  if (ck) {
    await initWithConfigKey(ck)
  } else {
    await loadSessionList()
  }
})
</script>

<style scoped>
.crud-generator-page {
  display: flex;
  height: calc(100vh - 100px);
  background: #F8FAFC;
  overflow: hidden;
}

/* ============ 左侧历史导航 ============ */
.generator-sidebar {
  width: 240px;
  background: #FFFFFF;
  border-right: 1px solid #E2E8F0;
  display: flex;
  flex-direction: column;
  transition: width 0.25s ease;
  flex-shrink: 0;
}

.generator-sidebar.collapsed {
  width: 48px;
  overflow: hidden;
}

.sidebar-header {
  padding: 12px 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #F1F5F9;
  min-height: 52px;
}

.sidebar-section-title {
  padding: 8px 14px;
  font-size: 11px;
  color: #94A3B8;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  border-bottom: 1px solid #F1F5F9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  padding: 10px 14px;
  border-bottom: 1px solid #F8FAFC;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  position: relative;
  transition: all 0.15s ease;
}

.session-item:hover { background: #F8FAFC; }
.session-item:hover .delete-btn { opacity: 1; }

.session-item.active {
  background: #EEF2FF;
  border-left: 3px solid #6366F1;
}

.session-title {
  font-size: 13px;
  color: #1E293B;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-config-key {
  font-weight: 600;
  color: #6366F1;
  font-family: 'Fira Code', 'Monaco', monospace;
  font-size: 12px;
}

.session-table {
  font-size: 11px;
  color: #10B981;
  margin-right: 4px;
}

.session-meta {
  display: flex;
  gap: 8px;
  margin-top: 3px;
}

.session-time {
  font-size: 11px;
  color: #94A3B8;
}

.delete-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: opacity 0.2s;
}

.empty-tip {
  padding: 40px 20px;
  text-align: center;
  color: #CBD5E1;
  font-size: 13px;
}

/* ============ 中间对话区 ============ */
.generator-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #FFFFFF;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message.user { flex-direction: row-reverse; }

.message-avatar { flex-shrink: 0; }

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  color: #FFFFFF;
}

.user-avatar { background: #6366F1; }
.ai-avatar { background: linear-gradient(135deg, #10B981, #6366F1); font-size: 10px; }

.message-body {
  display: flex;
  flex-direction: column;
  max-width: 70%;
  gap: 4px;
}

.message.user .message-body { align-items: flex-end; }

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  word-break: break-word;
  white-space: pre-wrap;
  line-height: 1.6;
  font-size: 14px;
}

.message.user .message-bubble {
  background: #6366F1;
  color: #FFFFFF;
  border-radius: 12px 4px 12px 12px;
}

.message.assistant .message-bubble {
  background: #F8FAFC;
  color: #1E293B;
  border-radius: 4px 12px 12px 12px;
}

.stage-indicator {
  font-size: 11px;
  color: #6366F1;
  font-weight: 500;
}

/* 空状态 */
.empty-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: #94A3B8;
  padding: 24px;
}

.empty-chat-icon { font-size: 48px; opacity: 0.5; }
.empty-chat-tip { font-size: 16px; font-weight: 500; color: #64748B; }

.example-prompts { margin-top: 16px; text-align: center; width: 100%; }
.example-title { font-size: 13px; color: #64748B; margin-bottom: 10px; font-weight: 500; }

.example-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 420px;
  margin: 0 auto;
}

.example-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.example-item:hover {
  border-color: #6366F1;
  background: #FAFAFE;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.08);
}

.example-label { font-size: 13px; font-weight: 600; color: #1E293B; white-space: nowrap; flex-shrink: 0; }
.example-desc { font-size: 12px; color: #64748B; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }

/* 输入区 */
.input-section {
  padding: 12px 16px;
  border-top: 1px solid #F1F5F9;
  background: #FFFFFF;
  display: flex;
  flex-direction: column;
  gap: 10px;
  box-shadow: 0 -2px 8px rgba(0,0,0,0.04);
}

/* 配置条：一体化棓形输入条 */
.config-bar {
  display: flex;
  align-items: center;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 0 4px;
  height: 38px;
  gap: 0;
  transition: border-color 0.15s;
}

.config-bar:focus-within {
  border-color: #818CF8;
  box-shadow: 0 0 0 3px rgba(99,102,241,0.06);
}

.config-field {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  padding: 0 6px;
  gap: 6px;
  position: relative;
}

.config-field--shrink {
  flex: 0 0 auto;
  width: 140px;
}

.config-field.has-error .field-tag {
  color: #EF4444;
}

.field-tag {
  font-size: 11px;
  font-weight: 600;
  color: #94A3B8;
  white-space: nowrap;
  flex-shrink: 0;
  letter-spacing: 0.3px;
}

.field-input {
  flex: 1;
  min-width: 0;
}

/* 去掉 n-input 和 n-select 内部的边框和背景，融入整体栏 */
.config-field :deep(.n-input) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}
.config-field :deep(.n-input .n-input__border),
.config-field :deep(.n-input .n-input__state-border) {
  display: none !important;
}
.config-field :deep(.n-input .n-input-wrapper) {
  padding-left: 0;
  padding-right: 0;
}
.config-field :deep(.n-base-selection) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}
.config-field :deep(.n-base-selection .n-base-selection__border),
.config-field :deep(.n-base-selection .n-base-selection__state-border) {
  display: none !important;
}

.auto-btn {
  font-size: 11px;
  color: #6366F1;
  cursor: pointer;
  white-space: nowrap;
  font-weight: 500;
  padding: 2px 4px;
  border-radius: 4px;
  transition: background 0.15s;
}
.auto-btn:hover {
  background: rgba(99,102,241,0.08);
}

.field-error-tip {
  position: absolute;
  bottom: -18px;
  left: 0;
  font-size: 11px;
  color: #EF4444;
  white-space: nowrap;
}

.config-divider {
  width: 1px;
  height: 18px;
  background: #E2E8F0;
  flex-shrink: 0;
  margin: 0 2px;
}

.bar-action-btn {
  white-space: nowrap;
  flex-shrink: 0;
  padding: 0 8px !important;
  font-size: 12px !important;
}

.input-area {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.input-buttons {
  display: flex;
  align-items: flex-end;
  flex-shrink: 0;
}

/* ============ 右侧配置面板 ============ */
.generator-preview {
  width: 580px;
  background: #FFFFFF;
  border-left: 1px solid #E2E8F0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.25s ease;
}

.generator-preview.preview-collapsed {
  width: 40px;
  overflow: hidden;
}

/* 折叠状态竖向栏 */
.preview-collapsed-bar {
  width: 40px;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  color: #94A3B8;
  transition: color 0.15s;
  padding: 12px 0;
}

.preview-collapsed-bar:hover { color: #6366F1; background: #F8FAFC; }

.preview-collapsed-text {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 2px;
  color: inherit;
  white-space: nowrap;
}

/* 标题栏 */
.preview-header {
  padding: 10px 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #F1F5F9;
  background: #FAFAFE;
  min-height: 52px;
}

.preview-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #1E293B;
}

.preview-actions {
  display: flex;
  gap: 6px;
  align-items: center;
}

/* Tab 分组切换器 */
.preview-tab-group {
  padding: 8px 14px 0;
  border-bottom: 1px solid #F1F5F9;
  background: #FAFAFE;
}

.tab-group-switcher {
  display: flex;
  gap: 0;
  background: #F1F5F9;
  border-radius: 8px;
  padding: 3px;
  width: fit-content;
}

.group-tab {
  padding: 5px 14px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #64748B;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.group-tab:hover { color: #1E293B; background: rgba(255,255,255,0.6); }

.group-tab.active {
  background: #FFFFFF;
  color: #6366F1;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

/* 内容区 */
.preview-tabs {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
}

.preview-tabs :deep(.n-tabs) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.preview-tabs :deep(.n-tabs .n-tabs-pane-wrapper) {
  flex: 1;
  overflow: hidden;
}

.preview-tabs :deep(.n-tabs .n-tab-pane) { height: 100%; }

.preview-tabs :deep(.n-tabs .n-tabs-bar) { background-color: #6366F1; }

.preview-tabs :deep(.n-tabs .n-tab--active .n-tabs-tab__label) { color: #6366F1; }

.editor-area {
  padding: 12px;
  height: 100%;
  overflow: auto;
}

.json-editor {
  width: 100%;
  height: 100%;
  border: 1px solid #E2E8F0;
  resize: none;
  font-family: 'Fira Code', 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  line-height: 1.6;
  background: #F8FAFC;
  padding: 12px;
  box-sizing: border-box;
  border-radius: 8px;
  transition: all 0.15s ease;
}

.json-editor:focus {
  outline: none;
  background: #FFFFFF;
  border-color: #818CF8;
  box-shadow: 0 0 0 3px rgba(99,102,241,0.08);
}

.sql-editor-wrap {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sql-editor-wrap .json-editor { flex: 1; height: 0; }

.sql-actions {
  padding: 10px 0 0;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.typing-indicator {
  position: absolute;
  bottom: 16px;
  right: 16px;
  background: #6366F1;
  color: #FFFFFF;
  padding: 5px 10px;
  border-radius: 6px;
  font-size: 11px;
  animation: pulse 1.5s infinite;
  z-index: 10;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}



.generator-sidebar.collapsed {
  width: 48px;
  overflow: hidden;
}

.sidebar-header {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #F1F5F9;
}

.sidebar-section-title {
  padding: 10px 16px;
  font-size: 11px;
  color: #94A3B8;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  border-bottom: 1px solid #F1F5F9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  padding: 12px 16px;
  border-bottom: 1px solid #F8FAFC;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  position: relative;
  transition: all 0.15s ease;
}

.session-item:hover {
  background: #F8FAFC;
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.session-item.active {
  background: #EEF2FF;
  border-left: 3px solid #6366F1;
}

.session-title {
  font-size: 13px;
  color: #1E293B;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-config-key {
  font-weight: 600;
  color: #6366F1;
  font-family: 'Fira Code', 'Monaco', monospace;
  font-size: 12px;
}

.session-table {
  font-size: 11px;
  color: #10B981;
  margin-right: 4px;
}

.session-meta {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.session-time {
  font-size: 11px;
  color: #94A3B8;
}

.delete-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: opacity 0.2s;
}

.empty-tip {
  padding: 40px 20px;
  text-align: center;
  color: #CBD5E1;
  font-size: 13px;
}

.generator-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #FFFFFF;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  color: #FFFFFF;
}

.user-avatar {
  background: #6366F1;
}

.ai-avatar {
  background: linear-gradient(135deg, #10B981, #6366F1);
  font-size: 10px;
}

.message-body {
  display: flex;
  flex-direction: column;
  max-width: 70%;
  gap: 4px;
}

.message.user .message-body {
  align-items: flex-end;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  background: #F8FAFC;
  word-break: break-word;
  white-space: pre-wrap;
  line-height: 1.6;
}

.message.user .message-bubble {
  background: #6366F1;
  color: #FFFFFF;
  border-radius: 12px 4px 12px 12px;
}

.message.assistant .message-bubble {
  background: #F8FAFC;
  color: #1E293B;
  border-radius: 4px 12px 12px 12px;
}

.stage-indicator {
  font-size: 11px;
  color: #6366F1;
  font-weight: 500;
}

.empty-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: #94A3B8;
}

.empty-chat-icon {
  font-size: 48px;
  opacity: 0.5;
}

.empty-chat-tip {
  font-size: 16px;
  font-weight: 500;
}

.example-prompts {
  margin-top: 24px;
  text-align: center;
}

.example-title {
  font-size: 13px;
  color: #64748B;
  margin-bottom: 12px;
  font-weight: 500;
}

.example-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 420px;
  margin: 0 auto;
}

.example-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.example-item:hover {
  border-color: #6366F1;
  background: #FAFAFE;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.08);
}

.example-label {
  font-size: 13px;
  font-weight: 600;
  color: #1E293B;
  white-space: nowrap;
  flex-shrink: 0;
}

.example-desc {
  font-size: 12px;
  color: #64748B;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.input-section {
  padding: 16px 24px;
  border-top: 1px solid #F1F5F9;
  background: #FFFFFF;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.config-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.input-area {
  display: flex;
  gap: 12px;
}

.input-buttons {
  display: flex;
  align-items: flex-end;
}

.generator-preview {
  width: 520px;
  background: #FFFFFF;
  border-left: 1px solid #E2E8F0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.preview-header {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #F1F5F9;
  background: #FAFAFE;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #1E293B;
}

.preview-actions {
  display: flex;
  gap: 6px;
}

.preview-tabs {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
}

.preview-tabs :deep(.n-tabs) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.preview-tabs :deep(.n-tabs .n-tabs-pane-wrapper) {
  flex: 1;
  overflow: hidden;
}

.preview-tabs :deep(.n-tabs .n-tab-pane) {
  height: 100%;
}

.editor-area {
  padding: 16px;
  height: 100%;
  overflow: auto;
}

.json-editor {
  width: 100%;
  height: 100%;
  border: 1px solid #E2E8F0;
  resize: none;
  font-family: 'Fira Code', 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  line-height: 1.6;
  background: #F8FAFC;
  padding: 12px;
  box-sizing: border-box;
  border-radius: 8px;
  transition: all 0.15s ease;
}

.json-editor:focus {
  outline: none;
  background: #FFFFFF;
  border-color: #818CF8;
}

.sql-editor-wrap {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sql-editor-wrap .json-editor {
  flex: 1;
  height: 0;
}

.sql-actions {
  padding: 12px 0;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.typing-indicator {
  position: absolute;
  bottom: 20px;
  right: 20px;
  background: #6366F1;
  color: #FFFFFF;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  animation: pulse 1.5s infinite;
  z-index: 10;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.template-section {
  width: 100%;
  margin: 16px 0 8px;
  text-align: left;
}

.template-title {
  font-size: 12px;
  color: #64748B;
  margin-bottom: 10px;
  font-weight: 500;
}

.template-cards {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.template-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border: 1.5px solid #E2E8F0;
  border-radius: 10px;
  cursor: pointer;
  background: #FFFFFF;
  transition: all 0.15s;
  position: relative;
  min-width: 180px;
  flex: 1;
}

.template-card:hover {
  border-color: #818CF8;
  background: #FAFAFE;
}

.template-card.active {
  border-color: #6366F1;
  background: #EEF2FF;
}

.template-card-icon {
  color: #6366F1;
  flex-shrink: 0;
  margin-top: 2px;
}

.template-card-body {
  flex: 1;
}

.template-card-name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.template-card-desc {
  font-size: 11px;
  color: #94A3B8;
  margin-top: 2px;
  line-height: 1.4;
}

.template-card-check {
  position: absolute;
  top: 6px;
  right: 8px;
  color: #6366F1;
  font-size: 13px;
  font-weight: 700;
}
</style>
