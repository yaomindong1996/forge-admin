<template>
  <div class="crud-generator-page">
    <div class="generator-sidebar">
      <div class="sidebar-header">
        <n-button size="small" text @click="goBack">
          <template #icon><n-icon><ArrowBackOutline /></n-icon></template>
          返回列表
        </n-button>
        <n-button size="small" type="primary" @click="handleNewSession">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新对话
        </n-button>
      </div>
      <div class="sidebar-section-title">历史对话记录</div>
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

            <!-- 模板选择卡片 -->
            <div v-if="templateList.length > 0" class="template-section">
              <div class="template-title">选择页面模板（当前：{{ templateList.find(t => t.templateKey === layoutType)?.templateName || layoutType }}）</div>
              <div class="template-cards">
                <div
                  v-for="tpl in templateList"
                  :key="tpl.templateKey"
                  :class="['template-card', { active: layoutType === tpl.templateKey }]"
                  @click="layoutType = tpl.templateKey"
                >
                  <div class="template-card-icon">
                    <n-icon size="24"><component :is="tpl.icon || 'mdi:table'" /></n-icon>
                  </div>
                  <div class="template-card-body">
                    <div class="template-card-name">{{ tpl.templateName }}</div>
                    <div class="template-card-desc">{{ tpl.description }}</div>
                  </div>
                  <div v-if="layoutType === tpl.templateKey" class="template-card-check">✔</div>
                </div>
              </div>
            </div>

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
          <div class="config-row">
            <n-form-item :feedback="configKeyError" :validation-status="configKeyError ? 'error' : undefined" style="flex:1;margin-bottom:0">
              <n-input v-model:value="configKey" placeholder="页面标识（英文，如 employee_manage）">
                <template #suffix>
                  <n-button text size="small" type="primary" @click="autoGenerateConfigKey" :disabled="generating">
                    自动生成
                  </n-button>
                </template>
              </n-input>
            </n-form-item>
            <n-form-item :feedback="tableNameError" :validation-status="tableNameError ? 'error' : undefined" style="width:160px;margin-bottom:0">
              <n-input v-model:value="tableName" placeholder="关联表名（可选）" />
            </n-form-item>
            <n-select
              v-model:value="tableName"
              :options="tableOptions"
              placeholder="选择已有表"
              clearable
              style="width: 180px"
              @update:value="handleTableSelect"
            />
            <n-button size="small" type="success" @click="showImportModal = true">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              导入表
            </n-button>
          </div>
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
              <n-button v-if="generating" type="error" size="small" @click="abortGenerate">
                <template #icon><n-icon><CloseOutline /></n-icon></template>
                停止
              </n-button>
              <n-button v-else type="primary" :disabled="!configKey" @click="sendMessage">
                发送
              </n-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="generator-preview">
      <div class="preview-header">
        <span>配置预览</span>
        <div class="preview-actions">
          <n-button size="small" @click="copyCurrentFile">
            <template #icon><n-icon><CopyOutline /></n-icon></template>
            复制
          </n-button>
          <n-button size="small" @click="exportAllFiles">
            <template #icon><n-icon><DownloadOutline /></n-icon></template>
            导出
          </n-button>
          <n-button type="primary" size="small" @click="openSaveModal">
            <template #icon><n-icon><SaveOutline /></n-icon></template>
            保存配置
          </n-button>
          <n-button type="info" size="small" :disabled="!configSaved" @click="previewCrudPage">
            <template #icon><n-icon><EyeOutline /></n-icon></template>
            预览效果
          </n-button>
          <n-button type="warning" size="small" :disabled="!configSaved" @click="handleDownloadCode">
            <template #icon><n-icon><DownloadOutline /></n-icon></template>
            下载代码
          </n-button>
        </div>
      </div>
      <div class="preview-body">
        <div class="file-tree">
          <n-dropdown
            :show="showContextMenu"
            :options="contextMenuOptions"
            :x="contextMenuX"
            :y="contextMenuY"
            trigger="manual"
            @clickoutside="showContextMenu = false"
            @select="handleContextMenuSelect"
          />
          <n-tree
            :data="fileTreeData"
            :selected-keys="[activeFile]"
            block-line
            :selectable="isSelectable"
            @update:selected-keys="handleFileSelect"
            @contextmenu="handleTreeContextMenu"
          />
        </div>
        <div class="editor-area">
          <!-- 搜索配置：可视化行编辑器 -->
          <template v-if="activeFile === 'searchSchema'">
            <SchemaFieldEditor
              mode="search"
              :value="displayContent.searchSchema"
              :table-structure="displayContent.tableStructure"
              @update:value="displayContent.searchSchema = $event"
            />
          </template>
          <!-- 表格列配置：可视化行编辑器 -->
          <template v-else-if="activeFile === 'columnsSchema'">
            <SchemaFieldEditor
              mode="columns"
              :value="displayContent.columnsSchema"
              :table-structure="displayContent.tableStructure"
              @update:value="displayContent.columnsSchema = $event"
            />
          </template>
          <!-- 编辑表单配置：可视化行编辑器 -->
          <template v-else-if="activeFile === 'editSchema'">
            <SchemaFieldEditor
              mode="edit"
              :value="displayContent.editSchema"
              :table-structure="displayContent.tableStructure"
              @update:value="displayContent.editSchema = $event"
            />
          </template>
          <!-- API配置：表单式编辑器 -->
          <template v-else-if="activeFile === 'apiConfig'">
            <ApiConfigEditor
              :value="displayContent.apiConfig"
              @update:value="displayContent.apiConfig = $event"
            />
          </template>
          <!-- 字典配置 -->
          <template v-else-if="activeFile === 'dictConfig'">
            <DictConfigPanel
              :search-schema="displayContent.searchSchema"
              :columns-schema="displayContent.columnsSchema"
              :edit-schema="displayContent.editSchema"
              :dict-config="displayContent.dictConfig"
              @update:value="displayContent.dictConfig = $event"
            />
          </template>
          <!-- 脱敏配置 -->
          <template v-else-if="activeFile === 'desensitizeConfig'">
            <DesensitizeConfigPanel
              :value="displayContent.desensitizeConfig"
              :columns-schema="displayContent.columnsSchema"
              @update:value="displayContent.desensitizeConfig = $event"
            />
          </template>
          <!-- 加解密配置 -->
          <template v-else-if="activeFile === 'encryptConfig'">
            <EncryptConfigPanel
              :value="displayContent.encryptConfig"
              @update:value="displayContent.encryptConfig = $event"
            />
          </template>
          <!-- 翻译配置 -->
          <template v-else-if="activeFile === 'transConfig'">
            <TransConfigPanel
              :value="displayContent.transConfig"
              :columns-schema="displayContent.columnsSchema"
              :edit-schema="displayContent.editSchema"
              @update:value="displayContent.transConfig = $event"
            />
          </template>
          <!-- 建表SQL / 表结构：原始文本编辑 -->
          <template v-else>
            <div v-if="activeFile === 'createTableSql'" class="sql-editor-wrap">
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
                >执行建表</n-button>
              </div>
            </div>
            <textarea
              v-else
              v-model="currentFileContent"
              class="json-editor"
              :placeholder="generating ? '正在生成...' : '等待生成...'"
              spellcheck="false"
            />
          </template>
          <div v-if="generating && currentFileContent" class="typing-indicator">正在写入...</div>
        </div>
      </div>
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
import { AddOutline, CloseOutline, ArrowBackOutline, CopyOutline, DownloadOutline, SaveOutline, EyeOutline } from '@vicons/ionicons5'
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
  currentStageMessage,
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

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

watch(() => messages.value.length, () => scrollToBottom())
watch(generating, (val) => { if (val) scrollToBottom() })

// ===== 示例引导 =====
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
  // 自动生成configKey
  autoGenerateConfigKey()
}

// ===== 中文校验 =====
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

// ===== 文件树 =====
// 辅助函数：计算JSON数组中的字段数量
function getFieldCount(content) {
  if (!content || !content.trim()) return 0
  try {
    const parsed = JSON.parse(content)
    if (Array.isArray(parsed)) return parsed.length
    if (typeof parsed === 'object' && parsed !== null) return Object.keys(parsed).length
  } catch {
    // JSON解析失败，尝试按行统计（非JSON格式）
    return content.split('\n').filter(line => line.trim() && !line.trim().startsWith('//')).length
  }
  return 0
}

// 辅助函数：生成带摘要的标签
function getLabelWithCount(label, content) {
  const count = getFieldCount(content)
  if (count > 0) {
    return `${label} (${count})`
  }
  return label
}

const fileTreeData = computed(() => [
  {
    label: '配置',
    key: 'config-group',
    children: [
      { label: getLabelWithCount('搜索配置', displayContent.value.searchSchema), key: 'searchSchema' },
      { label: getLabelWithCount('表格列配置', displayContent.value.columnsSchema), key: 'columnsSchema' },
      { label: getLabelWithCount('编辑表单配置', displayContent.value.editSchema), key: 'editSchema' },
      { label: getLabelWithCount('接口配置', displayContent.value.apiConfig), key: 'apiConfig' },
      { label: getLabelWithCount('字典配置', displayContent.value.dictConfig), key: 'dictConfig' },
      { label: getLabelWithCount('脱敏配置', displayContent.value.desensitizeConfig), key: 'desensitizeConfig' },
      { label: getLabelWithCount('加解密配置', displayContent.value.encryptConfig), key: 'encryptConfig' },
      { label: getLabelWithCount('翻译配置', displayContent.value.transConfig), key: 'transConfig' },
      { label: '建表SQL', key: 'createTableSql' },
    ],
  },
  {
    label: '表模型',
    key: 'table-group',
    children: [
      { label: '表结构', key: 'tableStructure' },
    ],
  },
])

const currentFileContent = computed({
  get: () => displayContent.value[activeFile.value] || '',
  set: (val) => { displayContent.value[activeFile.value] = val }
})

const tableOptions = ref([])
const showImportModal = ref(false)
const showMenuModal = ref(false)

// ===== 右键菜单（增量生成） =====
const showContextMenu = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuTarget = ref('')

const contextMenuOptions = computed(() => [
  { label: '重新生成此项', key: 'regenerate', icon: () => '🔄' },
  { label: '清空此项内容', key: 'clear', icon: () => '🗑️' },
  { type: 'divider', key: 'd1' },
  { label: '复制此项内容', key: 'copy', icon: () => '📋' },
])

function handleTreeContextMenu(e) {
  // 查找最近的树节点key
  const target = e.target
  const nodeEl = target.closest('.n-tree-node')
  if (nodeEl) {
    // 从节点文本推断key
    const nodeText = nodeEl.textContent?.trim()
    const nodeMap = {
      '搜索配置': 'searchSchema', '表格列配置': 'columnsSchema', '编辑表单配置': 'editSchema',
      '接口配置': 'apiConfig', '字典配置': 'dictConfig', '脱敏配置': 'desensitizeConfig',
      '加解密配置': 'encryptConfig', '翻译配置': 'transConfig', '建表SQL': 'createTableSql', '表结构': 'tableStructure',
    }
    for (const [label, key] of Object.entries(nodeMap)) {
      if (nodeText && nodeText.startsWith(label)) {
        contextMenuTarget.value = key
        break
      }
    }
  }
  
  if (contextMenuTarget.value) {
    e.preventDefault()
    showContextMenu.value = true
    contextMenuX.value = e.clientX
    contextMenuY.value = e.clientY
  }
}

function handleContextMenuSelect(key) {
  showContextMenu.value = false
  const targetKey = contextMenuTarget.value
  if (!targetKey) return

  switch (key) {
    case 'regenerate':
      regenerateSingleSchema(targetKey)
      break
    case 'clear':
      displayContent.value[targetKey] = ''
      generatedFiles.value[targetKey] = ''
      success(`已清空 ${getSchemaLabel(targetKey)}`)
      break
    case 'copy':
      const content = displayContent.value[targetKey]
      if (content) {
        navigator.clipboard.writeText(content)
        success(`已复制 ${getSchemaLabel(targetKey)}`)
      } else {
        warning(`${getSchemaLabel(targetKey)} 无内容`)
      }
      break
  }
}

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

  // 构建单schema生成请求
  const schemaLabel = getSchemaLabel(schemaKey)
  const prompt = `请只生成"${schemaLabel}"部分的配置，其他部分不需要生成。${inputText.value || description.value}`

  generating.value = true
  activeFile.value = schemaKey
  currentReceivingFile.value = schemaKey
  
  // 清空目标schema
  displayContent.value[schemaKey] = ''
  generatedFiles.value[schemaKey] = ''

  const request = {
    sessionId: sessionId.value || generateSessionId(),
    configKey: configKey.value,
    tableName: tableName.value || undefined,
    description: prompt,
    singleSchema: schemaKey, // 告诉后端只生成单个schema
  }

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: `[重新生成${schemaLabel}] ${inputText.value || description.value}`,
    createTime: new Date().toISOString(),
  })

  // 添加AI消息
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

// 加载已导入的表列表
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

function isSelectable(node) {
  return !node.children || node.children.length === 0
}

function handleFileSelect(keys) {
  if (keys && keys.length > 0) {
    activeFile.value = keys[0]
  }
}

watch(tableName, (val) => {
  loadTableStructure(val)
})

function openSaveModal() {
  // 已有配置（更新场景）：直接保存，无需再次选择菜单归属
  if (configSaved.value) {
    saveConfig({})
    return
  }
  // 首次保存：弹出菜单选择弹窗
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

/**
 * 自动生成 configKey
 * 优先级：tableName > 描述文本 > 随机生成
 */
function autoGenerateConfigKey() {
  let key = ''
  
  // 1. 从 tableName 生成
  if (tableName.value) {
    key = tableName.value.toLowerCase().replace(/^sys_/, '').replace(/[^a-z0-9]/g, '_')
  }
  // 2. 从描述文本生成
  else if (inputText.value && inputText.value.trim()) {
    // 提取中文关键词并转换
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
  
  // 3. 如果还是空，生成一个基于时间的key
  if (!key) {
    const now = new Date()
    key = `page_${now.getMonth() + 1}${now.getDate()}_${Math.random().toString(36).substr(2, 4)}`
  }
  
  // 确保符合格式要求
  key = key.replace(/_+/g, '_').replace(/^_|_$/g, '')
  if (!key || key.length < 2) {
    key = 'page_' + Math.random().toString(36).substr(2, 4)
  }
  
  configKey.value = key
}

function goBack() {
  router.push('/ai/crud-config')
}

/**
 * 下载 CODEGEN 代码包
 */
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
  // 生成防重放参数（模拟 axios 拦截器行为）
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

// 新对话：清空当前对话区，保留 configKey 和已加载的配置
function handleNewSession() {
  startNewSession()
  // startNewSession 会清空 configKey/tableName，用户点新对话是要全新开始
}

// ===== 建表 SQL 执行 =====
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
        // 刷新表列表
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
    // 从 crud-config 列表页跳转进来，根据 configKey 加载历史会话或新建
    await initWithConfigKey(ck)
  } else {
    // 直接访问，加载会话列表
    await loadSessionList()
  }
})
</script>

<style scoped>
@import './crud-generator.css';
</style>