<template>
  <div class="page-template-page">
    <!-- 顶部工具栏 -->
    <div class="page-header">
      <div class="header-left">
        <span class="header-title">页面模板</span>
        <span class="header-desc">管理 AI CRUD 生成器的页面模板，每个模板可配置专属 AI 提示词约束</span>
      </div>
      <n-button type="primary" size="small" @click="openCreateModal">
        <template #icon><n-icon><AddOutline /></n-icon></template>
        新增模板
      </n-button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-area">
      <n-spin size="large" description="加载中..." />
    </div>

    <!-- 模板卡片网格 -->
    <div v-else class="template-grid">
      <div
        v-for="tpl in templateList"
        :key="tpl.id"
        class="template-card"
        :class="{ disabled: tpl.enabled === 0 }"
      >
        <!-- 卡片头部 -->
        <div class="card-header">
          <div class="card-icon" :style="{ background: getIconBg(tpl.templateKey) }">
            <n-icon size="22" :color="getIconColor(tpl.templateKey)">
              <component :is="resolveIcon(tpl.icon)" />
            </n-icon>
          </div>
          <div class="card-meta">
            <div class="card-name">{{ tpl.templateName }}</div>
            <div class="card-key">{{ tpl.templateKey }}</div>
          </div>
          <div class="card-badges">
            <n-tag v-if="tpl.isBuiltin === 1" size="tiny" type="info">内置</n-tag>
            <n-tag v-if="tpl.enabled === 0" size="tiny" type="warning">停用</n-tag>
          </div>
        </div>

        <!-- 描述 -->
        <div class="card-desc">{{ tpl.description || '暂无描述' }}</div>

        <!-- 默认配置预览 -->
        <div v-if="tpl.defaultConfig" class="card-config">
          <div class="config-label">默认配置</div>
          <div class="config-chips">
            <span v-for="(val, key) in parseJson(tpl.defaultConfig)" :key="key" class="config-chip">
              {{ key }}: {{ typeof val === 'object' ? '...' : val }}
            </span>
          </div>
        </div>

        <!-- AI 提示词摘要 -->
        <div v-if="tpl.systemPrompt" class="card-prompt">
          <div class="prompt-label">
            <n-icon size="12"><SparklesOutline /></n-icon>
            AI 约束
          </div>
          <div class="prompt-preview">{{ truncate(tpl.systemPrompt, 80) }}</div>
        </div>

        <!-- 操作按钮 -->
        <div class="card-actions">
          <n-button size="tiny" @click="openEditModal(tpl)">
            <template #icon><n-icon><CreateOutline /></n-icon></template>
            编辑
          </n-button>
          <n-button
            size="tiny"
            :type="tpl.enabled === 1 ? 'warning' : 'success'"
            @click="toggleEnabled(tpl)"
          >
            {{ tpl.enabled === 1 ? '停用' : '启用' }}
          </n-button>
          <n-popconfirm
            v-if="tpl.isBuiltin !== 1"
            @positive-click="handleDelete(tpl.id)"
          >
            <template #trigger>
              <n-button size="tiny" type="error">
                <template #icon><n-icon><TrashOutline /></n-icon></template>
                删除
              </n-button>
            </template>
            确认删除该模板？
          </n-popconfirm>
          <n-tooltip v-else>
            <template #trigger>
              <n-button size="tiny" disabled>
                <template #icon><n-icon><TrashOutline /></n-icon></template>
                删除
              </n-button>
            </template>
            内置模板不可删除
          </n-tooltip>
        </div>
      </div>

      <!-- 新增占位卡 -->
      <div class="template-card add-card" @click="openCreateModal">
        <n-icon size="32" color="#9CA3AF"><AddCircleOutline /></n-icon>
        <span class="add-card-text">新增模板</span>
      </div>
    </div>

    <div v-if="total > 0" class="template-pagination">
      <n-pagination
        v-model:page="currentPage"
        v-model:page-size="pageSize"
        :item-count="total"
        :page-sizes="[12, 24, 48]"
        show-size-picker
        @update:page="loadList"
        @update:page-size="loadList"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <n-modal
      v-model:show="showModal"
      :title="isEdit ? '编辑模板' : '新增模板'"
      preset="card"
      style="width: 720px"
      :mask-closable="false"
    >
      <n-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-placement="left"
        label-width="100"
        require-mark-placement="right-hanging"
      >
        <n-grid :cols="2" :x-gap="16">
          <n-grid-item>
            <n-form-item label="模板标识" path="templateKey">
              <n-input
                v-model:value="formData.templateKey"
                :disabled="isEdit"
                placeholder="如 simple-crud（仅小写字母、数字、短横线）"
              />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="模板名称" path="templateName">
              <n-input v-model:value="formData.templateName" placeholder="如 标准 CRUD" />
            </n-form-item>
          </n-grid-item>
          <n-grid-item :span="2">
            <n-form-item label="模板描述">
              <n-input v-model:value="formData.description" type="textarea" :rows="2" placeholder="简短描述此模板的适用场景" />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="图标">
              <n-input v-model:value="formData.icon" placeholder="如 mdi:table" />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="排序">
              <n-input-number v-model:value="formData.sort" :min="0" style="width:100%" placeholder="数字越小越靠前" />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="状态">
              <n-switch v-model:value="formData.enabledBool" :round="false">
                <template #checked>启用</template>
                <template #unchecked>停用</template>
              </n-switch>
            </n-form-item>
          </n-grid-item>
          <n-grid-item :span="2">
            <n-form-item label="默认配置" class="json-form-item">
              <div class="json-editor-wrap">
                <n-input
                  v-model:value="formData.defaultConfig"
                  type="textarea"
                  :rows="4"
                  placeholder='{"modalType":"drawer","modalWidth":"800px","searchGridCols":4}'
                  class="mono-input"
                />
                <span class="json-hint">JSON 格式，将作为 crud-page 渲染时的默认参数</span>
              </div>
            </n-form-item>
          </n-grid-item>
          <n-grid-item :span="2">
            <n-form-item label="Schema 约束" class="json-form-item">
              <div class="json-editor-wrap">
                <n-input
                  v-model:value="formData.schemaHint"
                  type="textarea"
                  :rows="4"
                  placeholder='{"requiredFields":["parentId","sort"]}'
                  class="mono-input"
                />
                <span class="json-hint">JSON 格式，告诉 AI 该模板必须生成哪些字段</span>
              </div>
            </n-form-item>
          </n-grid-item>
          <n-grid-item :span="2">
            <n-form-item label="AI 提示词约束">
              <div class="prompt-editor-wrap">
                <div class="prompt-editor-header">
                  <span class="prompt-editor-tip">
                    <n-icon><SparklesOutline /></n-icon>
                    此内容会追加到 AI system prompt 末尾，约束 AI 按模板规范生成配置
                  </span>
                  <n-button size="tiny" text @click="insertPromptExample">插入示例</n-button>
                </div>
                <n-input
                  v-model:value="formData.systemPrompt"
                  type="textarea"
                  :rows="8"
                  placeholder="描述该模板的 AI 生成约束，如必须包含的字段、结构要求等..."
                  class="prompt-input"
                />
              </div>
            </n-form-item>
          </n-grid-item>
        </n-grid>
      </n-form>
      <template #footer>
        <div style="display:flex;justify-content:flex-end;gap:8px">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '创建模板' }}
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { AddOutline, AddCircleOutline, CreateOutline, TrashOutline, SparklesOutline } from '@vicons/ionicons5'
import { useDiscreteMessage } from '@/composables/useDiscreteMessage'
import {
  listEnabledTemplates,
  pageTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate,
} from '@/api/page-template'

defineOptions({ name: 'AiPageTemplate' })

const { success, error } = useDiscreteMessage()

const loading = ref(false)
const templateList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const showModal = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const defaultFormData = () => ({
  id: null,
  templateKey: '',
  templateName: '',
  description: '',
  icon: '',
  systemPrompt: '',
  schemaHint: '',
  defaultConfig: '',
  sort: 0,
  enabledBool: true,
})

const formData = ref(defaultFormData())

const formRules = {
  templateKey: [
    { required: true, message: '请输入模板标识', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9-]{1,63}$/, message: '仅小写字母、数字、短横线，以字母开头', trigger: 'blur' },
  ],
  templateName: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
  ],
}

async function loadList() {
  loading.value = true
  try {
    const res = await pageTemplates({ pageNum: currentPage.value, pageSize: pageSize.value })
    templateList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    error('加载模板列表失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

function openCreateModal() {
  isEdit.value = false
  formData.value = defaultFormData()
  showModal.value = true
}

function openEditModal(tpl) {
  isEdit.value = true
  formData.value = {
    id: tpl.id,
    templateKey: tpl.templateKey,
    templateName: tpl.templateName,
    description: tpl.description || '',
    icon: tpl.icon || '',
    systemPrompt: tpl.systemPrompt || '',
    schemaHint: tpl.schemaHint || '',
    defaultConfig: tpl.defaultConfig || '',
    sort: tpl.sort ?? 0,
    enabledBool: tpl.enabled === 1,
  }
  showModal.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  // 验证 JSON 字段
  if (formData.value.defaultConfig && !isValidJson(formData.value.defaultConfig)) {
    error('默认配置 JSON 格式不正确')
    return
  }
  if (formData.value.schemaHint && !isValidJson(formData.value.schemaHint)) {
    error('Schema 约束 JSON 格式不正确')
    return
  }

  submitting.value = true
  try {
    const payload = {
      ...formData.value,
      enabled: formData.value.enabledBool ? 1 : 0,
    }
    delete payload.enabledBool

    if (isEdit.value) {
      await updateTemplate(payload)
      success('模板已更新')
    } else {
      await createTemplate(payload)
      success('模板已创建')
    }
    showModal.value = false
    await loadList()
  } catch (e) {
    error((isEdit.value ? '更新' : '创建') + '失败: ' + e.message)
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteTemplate(id)
    success('模板已删除')
    await loadList()
  } catch (e) {
    error('删除失败: ' + e.message)
  }
}

async function toggleEnabled(tpl) {
  try {
    await updateTemplate({
      id: tpl.id,
      templateKey: tpl.templateKey,
      templateName: tpl.templateName,
      enabled: tpl.enabled === 1 ? 0 : 1,
    })
    success(tpl.enabled === 1 ? '已停用' : '已启用')
    await loadList()
  } catch (e) {
    error('操作失败: ' + e.message)
  }
}

// ===== 工具函数 =====
function parseJson(str) {
  if (!str) return {}
  try { return JSON.parse(str) } catch { return {} }
}

function isValidJson(str) {
  if (!str) return true
  try { JSON.parse(str); return true } catch { return false }
}

function truncate(str, len) {
  if (!str) return ''
  return str.length > len ? str.slice(0, len) + '...' : str
}

function resolveIcon(icon) {
  // 简单返回 icon 字符串，由 n-icon 结合 iconify 渲染
  // 若项目未引入 iconify，则回退到 SparklesOutline
  return SparklesOutline
}

const iconColors = ['#6366F1', '#10B981', '#F59E0B', '#EF4444', '#3B82F6', '#8B5CF6']
const iconBgs = ['#EEF2FF', '#D1FAE5', '#FEF3C7', '#FEE2E2', '#DBEAFE', '#EDE9FE']

function getIconColor(key) {
  const idx = Math.abs(hashStr(key)) % iconColors.length
  return iconColors[idx]
}

function getIconBg(key) {
  const idx = Math.abs(hashStr(key)) % iconBgs.length
  return iconBgs[idx]
}

function hashStr(str) {
  let h = 0
  for (let i = 0; i < str.length; i++) {
    h = (Math.imul(31, h) + str.charCodeAt(i)) | 0
  }
  return h
}

function insertPromptExample() {
  const example = `当前模板为「${formData.value.templateName || '自定义模板'}」，请确保：
1. columnsSchema 包含内容列 + 操作列(actions)
2. editSchema 简洁清晰，不要嵌套
3. modalType 默认为 drawer
4. 尽量精减搜索条件，保留最常用的 3-5 个`
  formData.value.systemPrompt = example
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.page-template-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  background: #fff;
  padding: 16px 20px;
  border-radius: 12px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.header-desc {
  font-size: 13px;
  color: #9CA3AF;
}

.loading-area {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

/* 卡片网格 */
.template-grid {
  flex: 1;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.template-card {
  background: #fff;
  border: 1.5px solid #E5E7EB;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: box-shadow 0.15s, border-color 0.15s;
}

.template-card:hover {
  border-color: #A5B4FC;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.08);
}

.template-card.disabled {
  opacity: 0.55;
}

/* 卡片头部 */
.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-meta {
  flex: 1;
  min-width: 0;
}

.card-name {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-key {
  font-size: 11px;
  color: #9CA3AF;
  font-family: monospace;
  margin-top: 1px;
}

.card-badges {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

/* 描述 */
.card-desc {
  font-size: 12px;
  color: #6B7280;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 默认配置 */
.card-config {
  background: #F9FAFB;
  border-radius: 6px;
  padding: 8px 10px;
}

.config-label {
  font-size: 10px;
  color: #9CA3AF;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 4px;
}

.config-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.config-chip {
  background: #E5E7EB;
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 11px;
  color: #374151;
  font-family: monospace;
}

/* AI 提示词摘要 */
.card-prompt {
  background: #F5F3FF;
  border-radius: 6px;
  padding: 8px 10px;
  border-left: 2px solid #A5B4FC;
}

.prompt-label {
  font-size: 10px;
  color: #7C3AED;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 3px;
  margin-bottom: 3px;
}

.prompt-preview {
  font-size: 11px;
  color: #6D28D9;
  line-height: 1.4;
}

/* 操作按钮 */
.card-actions {
  display: flex;
  gap: 6px;
  margin-top: auto;
  padding-top: 4px;
  border-top: 1px solid #F3F4F6;
}

/* 新增占位卡 */
.add-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  border-style: dashed;
  border-color: #D1D5DB;
  background: #FAFAFA;
  min-height: 160px;
  transition: all 0.15s;
}

.add-card:hover {
  border-color: #6366F1;
  background: #EEF2FF;
}

.add-card-text {
  font-size: 13px;
  color: #9CA3AF;
}

/* 弹窗内部样式 */
.json-editor-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mono-input {
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: 12px;
}

.json-hint {
  font-size: 11px;
  color: #9CA3AF;
}

.prompt-editor-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.prompt-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.prompt-editor-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #7C3AED;
}

.prompt-input {
  font-size: 13px;
  line-height: 1.6;
}

:deep(.n-input.mono-input .n-input__textarea-el) {
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: 12px;
}

.template-pagination {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 12px 24px 16px;
  background: #FFFFFF;
  border-top: 1px solid #F1F5F9;
}
</style>
