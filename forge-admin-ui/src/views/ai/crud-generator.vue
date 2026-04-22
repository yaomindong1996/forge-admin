<template>
  <div class="crud-generator-page">
    <div class="generator-sidebar">
      <div class="sidebar-header">
        <n-button size="tiny" text @click="goBack">
          <template #icon><n-icon><ArrowBackOutline /></n-icon></template>
          返回列表
        </n-button>
        <n-button size="tiny" type="primary" @click="handleNewSession">
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
          </div>
        </div>
        <div class="input-section">
          <div class="config-row">
            <n-form-item :feedback="configKeyError" :validation-status="configKeyError ? 'error' : undefined" style="flex:1;margin-bottom:0">
              <n-input v-model:value="configKey" placeholder="页面标识（英文，如 employee_manage）" />
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
            <n-button size="small" @click="showImportModal = true">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              导入表
            </n-button>
          </div>
          <div class="input-area">
            <n-input
              v-model:value="inputText"
              type="textarea"
              :rows="2"
              placeholder="描述你需要的 CRUD 页面功能，如：员工管理，包含姓名、部门、职位..."
              @keydown.enter.exact.prevent="sendMessage"
            />
            <div class="input-buttons">
              <n-button v-if="generating" type="error" size="small" @click="abortGenerate">停止</n-button>
              <n-button v-else type="primary" :disabled="!configKey" @click="sendMessage">发送</n-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="generator-preview">
      <div class="preview-header">
        <span>配置预览</span>
        <div class="preview-actions">
          <n-button size="small" @click="copyCurrentFile">复制</n-button>
          <n-button size="small" @click="exportAllFiles">导出</n-button>
          <n-button type="primary" size="small" @click="openSaveModal">保存配置</n-button>
          <n-button type="info" size="small" :disabled="!configSaved" @click="previewCrudPage">预览效果</n-button>
        </div>
      </div>
      <div class="preview-body">
        <div class="file-tree">
          <n-tree
            :data="fileTreeData"
            :selected-keys="[activeFile]"
            block-line
            :selectable="isSelectable"
            @update:selected-keys="handleFileSelect"
          />
        </div>
        <div class="editor-area">
          <!-- 搜索配置：可视化行编辑器 -->
          <template v-if="activeFile === 'searchSchema'">
            <SchemaFieldEditor
              mode="search"
              :value="displayContent.searchSchema"
              @update:value="displayContent.searchSchema = $event"
            />
          </template>
          <!-- 表格列配置：可视化行编辑器 -->
          <template v-else-if="activeFile === 'columnsSchema'">
            <SchemaFieldEditor
              mode="columns"
              :value="displayContent.columnsSchema"
              @update:value="displayContent.columnsSchema = $event"
            />
          </template>
          <!-- 编辑表单配置：可视化行编辑器 -->
          <template v-else-if="activeFile === 'editSchema'">
            <SchemaFieldEditor
              mode="edit"
              :value="displayContent.editSchema"
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
import { AddOutline, CloseOutline, ArrowBackOutline } from '@vicons/ionicons5'
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

  configSaved,
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

// ===== 中文校验 =====
const configKeyError = ref('')
const tableNameError = ref('')

function validateConfigKey(val) {
  if (!val) { configKeyError.value = ''; return }
  if (/[\u4e00-\u9fa5]/.test(val)) {
    configKeyError.value = '页面标识不能包含中文'
  } else if (!/^[a-z][a-z0-9_]{0,63}$/.test(val)) {
    configKeyError.value = '格式：小写字母开头，仅小写字母+数字+下划线'
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
const fileTreeData = computed(() => [
  {
    label: '配置',
    key: 'config-group',
    children: [
      { label: '搜索配置', key: 'searchSchema' },
      { label: '表格列配置', key: 'columnsSchema' },
      { label: '编辑表单配置', key: 'editSchema' },
      { label: '接口配置', key: 'apiConfig' },
      { label: '字典配置', key: 'dictConfig' },
      { label: '脱敏配置', key: 'desensitizeConfig' },
      { label: '加解密配置', key: 'encryptConfig' },
      { label: '翻译配置', key: 'transConfig' },
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
  }
  return labels[stage] || stage
}

function goBack() {
  router.push('/ai/crud-config')
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
.crud-generator-page {
  display: flex;
  height: calc(100vh - 100px);
  background: #f5f5f5;
}

.generator-sidebar {
  width: 200px;
  background: #fff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e0e0e0;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  padding: 10px 12px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  position: relative;
}

.session-item:hover {
  background: #f0f0f0;
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.session-item.active {
  background: #e6f7ff;
  border-left: 3px solid #1890ff;
}

.session-title {
  font-size: 13px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-config-key {
  font-weight: 600;
  color: #1890ff;
}

.session-table {
  font-size: 11px;
  color: #52c41a;
  margin-right: 4px;
}

.session-meta {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.session-time {
  font-size: 11px;
  color: #999;
}

.session-count {
  font-size: 11px;
  color: #999;
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
  padding: 20px;
  text-align: center;
  color: #999;
  font-size: 13px;
}

.sidebar-section-title {
  padding: 6px 12px;
  font-size: 11px;
  color: #aaa;
  font-weight: 500;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  background: #fafafa;
  border-bottom: 1px solid #eee;
}

.generator-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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
  padding: 16px 12px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  color: #fff;
}

.user-avatar {
  background: #1890ff;
}

.ai-avatar {
  background: linear-gradient(135deg, #52c41a, #1890ff);
  font-size: 10px;
}

.message-body {
  display: flex;
  flex-direction: column;
  max-width: 75%;
  gap: 4px;
}

.message.user .message-body {
  align-items: flex-end;
}

.message-bubble {
  padding: 8px 12px;
  border-radius: 8px;
  background: #f0f0f0;
  word-break: break-word;
  white-space: pre-wrap;
}

.message.user .message-bubble {
  background: #1890ff;
  color: #fff;
  border-radius: 8px 2px 8px 8px;
}

.message.assistant .message-bubble {
  background: #f0f0f0;
  color: #333;
  border-radius: 2px 8px 8px 8px;
}

.stage-indicator {
  font-size: 11px;
  color: #1890ff;
}

.empty-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #aaa;
}

.empty-chat-icon {
  font-size: 36px;
}

.empty-chat-tip {
  font-size: 14px;
}

.input-section {
  padding: 12px;
  border-top: 1px solid #e0e0e0;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.config-row {
  display: flex;
  gap: 8px;
  align-items: center;
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
  width: 600px;
  background: #fff;
  border-left: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.preview-header {
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e0e0e0;
}

.preview-actions {
  display: flex;
  gap: 6px;
}

.preview-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.file-tree {
  width: 160px;
  border-right: 1px solid #e0e0e0;
  overflow-y: auto;
  padding: 8px 0;
  background: #fafafa;
}

.editor-area {
  flex: 1;
  padding: 12px;
  overflow: hidden;
  position: relative;
}

.json-editor {
  width: 100%;
  height: 100%;
  border: none;
  resize: none;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  line-height: 1.5;
  background: #fafafa;
  padding: 8px;
  box-sizing: border-box;
}

.json-editor:focus {
  outline: none;
  background: #fff;
}

.typing-indicator {
  position: absolute;
  bottom: 20px;
  right: 20px;
  background: #1890ff;
  color: #fff;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
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
  padding: 8px;
  border-top: 1px solid #eee;
  background: #fafafa;
  display: flex;
  justify-content: flex-end;
}
</style>