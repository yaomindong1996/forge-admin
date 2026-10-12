<template>
  <div class="skill-page">
    <!-- 顶部工具条 -->
    <div class="tool-bar">
      <div>
        <div class="page-title">
          技能管理
        </div>
        <div class="page-subtitle">
          管理可复用的技能包（SKILL.md），供 Agent 调用
        </div>
      </div>
      <div class="tool-bar-actions">
        <NButton secondary @click="showUploadModal = true">
          <template #icon>
            <i class="ai-icon:upload" />
          </template>
          上传ZIP
        </NButton>
        <NButton secondary @click="openGenerateModal">
          <template #icon>
            <i class="ai-icon:sparkles" />
          </template>
          AI生成
        </NButton>
        <NButton type="primary" @click="openCreateDrawer">
          <template #icon>
            <i class="ai-icon:plus" />
          </template>
          新增技能
        </NButton>
      </div>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <NInput
        v-model:value="search.keyword"
        placeholder="搜索名称、编码或描述"
        clearable
        class="filter-keyword"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <i class="ai-icon:search" />
        </template>
      </NInput>
      <n-select
        v-model:value="search.status"
        placeholder="全部状态"
        clearable
        :options="statusOptions"
        class="filter-status"
      />
      <NButton secondary @click="handleSearch">
        查询
      </NButton>
      <NButton quaternary @click="handleReset">
        重置
      </NButton>
    </div>

    <!-- 技能卡片列表 -->
    <NSpin :show="loading">
      <div v-if="list.length" class="skill-grid">
        <article
          v-for="skill in list"
          :key="skill.id"
          class="skill-card"
        >
          <div class="skill-card-header">
            <div class="skill-card-icon">
              <i class="ai-icon:sparkles" />
            </div>
            <div class="skill-card-title-wrap">
              <h3 class="skill-card-title" :title="skill.skillName">
                {{ skill.skillName }}
              </h3>
              <code class="skill-card-code">{{ skill.skillCode }}</code>
            </div>
            <DictTag dict-type="ai_status" :value="skill.status" size="small" />
          </div>

          <p class="skill-card-desc">
            {{ skill.description || '暂无描述' }}
          </p>

          <div class="skill-card-meta">
            <span v-if="skill.version">v{{ skill.version }}</span>
          </div>

          <div class="skill-card-footer">
            <NButton text size="small" type="primary" @click="openEditDrawer(skill)">
              编辑
            </NButton>
            <NButton text size="small" @click="viewFiles(skill.id)">
              文件
            </NButton>
            <NButton text size="small" type="warning" @click="openOptimize(skill)">
              AI优化
            </NButton>
            <NPopconfirm @positive-click="handleDelete(skill)">
              <template #trigger>
                <NButton text size="small" type="error">
                  删除
                </NButton>
              </template>
              确定删除技能"{{ skill.skillName }}"吗？
            </NPopconfirm>
          </div>
        </article>
      </div>

      <div v-else-if="!loading" class="empty-state">
        <i class="ai-icon:sparkles" />
        <p>暂无技能</p>
        <NButton size="small" type="primary" @click="openCreateDrawer">
          新增技能
        </NButton>
      </div>
    </NSpin>

    <!-- 分页 -->
    <div v-if="pagination.itemCount > 0" class="pagination-wrap">
      <span class="pagination-total">共 {{ pagination.itemCount }} 个技能</span>
      <n-pagination
        v-model:page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :item-count="pagination.itemCount"
        :page-sizes="[8, 16, 32, 64]"
        show-size-picker
        size="small"
        @update:page="loadList"
        @update:page-size="handlePageSizeChange"
      />
    </div>

    <!-- 编辑抽屉 -->
    <n-drawer v-model:show="drawerVisible" :width="520">
      <n-drawer-content :title="drawerMode === 'create' ? '新增技能' : '编辑技能'" closable>
        <NForm ref="formRef" :model="form" :rules="formRules" label-placement="top">
          <NFormItem label="技能名称" path="skillName">
            <NInput v-model:value="form.skillName" placeholder="请输入技能名称" />
          </NFormItem>
          <NFormItem label="技能编码" path="skillCode">
            <NInput
              v-model:value="form.skillCode"
              placeholder="如 code_reviewer"
              :disabled="drawerMode === 'edit'"
            />
          </NFormItem>
          <NFormItem label="描述" path="description">
            <NInput v-model:value="form.description" type="textarea" :rows="3" placeholder="技能用途说明" />
          </NFormItem>
          <NFormItem label="版本" path="version">
            <NInput v-model:value="form.version" placeholder="如 1.0.0" />
          </NFormItem>
          <NFormItem label="状态" path="status">
            <NRadioGroup v-model:value="form.status">
              <NRadio v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </NRadio>
            </NRadioGroup>
          </NFormItem>
        </NForm>
        <template #footer>
          <div class="drawer-footer">
            <NButton @click="drawerVisible = false">
              取消
            </NButton>
            <NButton type="primary" :loading="saving" @click="handleSave">
              确定
            </NButton>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- ZIP 上传弹窗 -->
    <NModal v-model:show="showUploadModal" preset="card" title="上传技能包" style="width: 500px">
      <NUpload
        :max="1"
        accept=".zip"
        :custom-request="handleUploadZip"
        :show-file-list="false"
      >
        <NUploadDragger>
          <div style="padding: 20px; text-align: center">
            <NIcon size="40" :depth="3">
              <CloudUploadOutline />
            </NIcon>
            <p>点击或拖拽 ZIP 文件到此处</p>
            <p style="font-size: 12px; color: var(--n-text-color-3)">
              ZIP 包需包含 SKILL.md 文件
            </p>
          </div>
        </NUploadDragger>
      </NUpload>
    </NModal>

    <!-- AI 生成弹窗 -->
    <NModal v-model:show="showGenerateModal" preset="card" title="AI 生成技能" style="width: 600px">
      <NForm label-placement="top">
        <NFormItem label="技能描述">
          <NInput
            v-model:value="generateDescription"
            type="textarea"
            :rows="4"
            placeholder="描述你需要的技能功能..."
          />
        </NFormItem>
      </NForm>
      <template #action>
        <NSpace>
          <NButton @click="showGenerateModal = false">
            取消
          </NButton>
          <NButton type="primary" :loading="generating" @click="handleAiGenerate">
            生成
          </NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- AI 优化弹窗 -->
    <NModal v-model:show="showOptimizeModal" preset="card" title="AI 优化技能" style="width: 600px">
      <NForm label-placement="top">
        <NFormItem label="优化指令">
          <NInput
            v-model:value="optimizeInstruction"
            type="textarea"
            :rows="3"
            placeholder="描述你希望如何优化..."
          />
        </NFormItem>
      </NForm>
      <template #action>
        <NSpace>
          <NButton @click="showOptimizeModal = false">
            取消
          </NButton>
          <NButton type="primary" :loading="optimizing" @click="handleAiOptimize">
            优化
          </NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- 技能文件查看弹窗 -->
    <NModal v-model:show="showFilesModal" preset="card" title="技能文件" style="width: 800px">
      <NSpin :show="loadingFiles">
        <div v-if="skillFiles.length">
          <NCollapse>
            <NCollapseItem
              v-for="file in skillFiles"
              :key="file.id"
              :title="file.filePath"
              :name="file.id"
            >
              <pre class="file-content">{{ file.fileContent }}</pre>
            </NCollapseItem>
          </NCollapse>
        </div>
        <NEmpty v-else description="暂无文件" />
      </NSpin>
    </NModal>
  </div>
</template>

<script setup>
import { CloudUploadOutline } from '@vicons/ionicons5'
import {
  NButton,
  NCollapse,
  NCollapseItem,
  NEmpty,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NModal,
  NRadio,
  NRadioGroup,
  NSpace,
  NSpin,
  NUpload,
  NUploadDragger,
  useMessage,
} from 'naive-ui'
import { computed, reactive, ref } from 'vue'
import {
  skillPage as fetchSkillPage,
  skillAdd,
  skillAiGenerate,
  skillAiOptimize,
  skillDelete,
  skillGetById,
  skillGetFiles,
  skillUpdate,
  skillUploadZip,
} from '@/api/ai'
import { DictTag } from '@/components'
import { useDict } from '@/composables/useDict'

defineOptions({ name: 'AiSkill' })

const message = useMessage()
const { dict } = useDict('ai_status')
const statusOptions = computed(() => dict.value.ai_status || [])

const list = ref([])
const loading = ref(false)
const saving = ref(false)
const generating = ref(false)
const optimizing = ref(false)
const loadingFiles = ref(false)
const skillFiles = ref([])
const drawerVisible = ref(false)
const drawerMode = ref('create')
const editingId = ref(null)
const formRef = ref(null)

const search = reactive({ keyword: '', status: null })
const pagination = reactive({ page: 1, pageSize: 8, itemCount: 0 })

const form = reactive({ skillName: '', skillCode: '', description: '', version: '1.0.0', status: '0' })
const formRules = {
  skillName: [{ required: true, message: '请输入技能名称', trigger: 'blur' }],
  skillCode: [{ required: true, message: '请输入技能编码', trigger: 'blur' }],
}

// 弹窗状态
const showUploadModal = ref(false)
const showGenerateModal = ref(false)
const showOptimizeModal = ref(false)
const showFilesModal = ref(false)
const generateDescription = ref('')
const optimizeInstruction = ref('')
const optimizeSkillId = ref(null)
const optimizeSkillName = ref('')

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: pagination.page, pageSize: pagination.pageSize }
    if (search.keyword)
      params.keyword = search.keyword
    if (search.status)
      params.status = search.status
    const res = await fetchSkillPage(params)
    if (res.code === 200) {
      list.value = res.data?.records || []
      pagination.itemCount = Number(res.data?.total || 0)
    }
  }
  catch (e) {
    message.error(e.message || '加载失败')
  }
  finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadList()
}

function handleReset() {
  search.keyword = ''
  search.status = null
  pagination.page = 1
  loadList()
}

function handlePageSizeChange(pageSize) {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadList()
}

function resetForm() {
  Object.assign(form, { skillName: '', skillCode: '', description: '', version: '1.0.0', status: '0' })
}

function openCreateDrawer() {
  drawerMode.value = 'create'
  editingId.value = null
  resetForm()
  drawerVisible.value = true
}

async function openEditDrawer(skill) {
  try {
    const res = await skillGetById(skill.id)
    if (res.code === 200 && res.data) {
      drawerMode.value = 'edit'
      editingId.value = skill.id
      Object.assign(form, {
        skillName: res.data.skillName || '',
        skillCode: res.data.skillCode || '',
        description: res.data.description || '',
        version: res.data.version || '1.0.0',
        status: res.data.status || '0',
      })
      drawerVisible.value = true
    }
  }
  catch (e) {
    message.error(e.message || '读取技能失败')
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  }
  catch { return }
  saving.value = true
  try {
    const payload = { ...form }
    const res = drawerMode.value === 'edit' && editingId.value
      ? await skillUpdate({ ...payload, id: editingId.value })
      : await skillAdd(payload)
    if (res.code === 200) {
      message.success(drawerMode.value === 'edit' ? '已保存' : '已创建')
      drawerVisible.value = false
      loadList()
    }
    else {
      message.error(res.msg || '操作失败')
    }
  }
  catch (e) {
    message.error(e.message || '操作失败')
  }
  finally {
    saving.value = false
  }
}

async function handleDelete(skill) {
  try {
    const res = await skillDelete(skill.id)
    if (res.code === 200) {
      message.success('已删除')
      if (list.value.length === 1 && pagination.page > 1)
        pagination.page -= 1
      loadList()
    }
    else {
      message.error(res.msg || '删除失败')
    }
  }
  catch (e) {
    message.error(e.message || '删除失败')
  }
}

async function viewFiles(skillId) {
  showFilesModal.value = true
  loadingFiles.value = true
  skillFiles.value = []
  try {
    const res = await skillGetFiles(skillId)
    skillFiles.value = res.data || []
  }
  catch (e) {
    message.error(e.message || '加载文件失败')
  }
  finally {
    loadingFiles.value = false
  }
}

async function handleUploadZip({ file }) {
  if (!file.file.name.toLowerCase().endsWith('.zip')) {
    message.error('仅支持 ZIP 文件')
    return
  }
  try {
    const formData = new FormData()
    formData.append('file', file.file)
    const res = await skillUploadZip(formData)
    if (res.code === 200) {
      message.success('技能包上传成功')
      showUploadModal.value = false
      loadList()
    }
    else {
      message.error(res.msg || '上传失败')
    }
  }
  catch (e) {
    message.error(`上传失败: ${e.message || e}`)
  }
}

function openGenerateModal() {
  generateDescription.value = ''
  showGenerateModal.value = true
}

async function handleAiGenerate() {
  if (!generateDescription.value.trim()) {
    message.warning('请输入技能描述')
    return
  }
  generating.value = true
  try {
    const res = await skillAiGenerate(generateDescription.value.trim())
    if (res.code === 200) {
      message.success('AI 生成成功，已填入表单')
      showGenerateModal.value = false
      // 生成结果落地：打开新增抽屉并预填生成内容
      openCreateDrawer()
      // AI 生成接口返回的是生成文本，作为描述预填
      if (res.data) {
        form.description = typeof res.data === 'string' ? res.data : ''
      }
    }
    else {
      message.error(res.msg || '生成失败')
    }
  }
  catch (e) {
    message.error(`生成失败: ${e.message || e}`)
  }
  finally {
    generating.value = false
  }
}

function openOptimize(skill) {
  optimizeSkillId.value = skill.id
  optimizeSkillName.value = skill.skillName || ''
  optimizeInstruction.value = ''
  showOptimizeModal.value = true
}

async function handleAiOptimize() {
  if (!optimizeInstruction.value.trim()) {
    message.warning('请输入优化指令')
    return
  }
  optimizing.value = true
  try {
    const res = await skillAiOptimize(optimizeSkillId.value, optimizeInstruction.value.trim())
    if (res.code === 200) {
      message.success('AI 优化完成')
      showOptimizeModal.value = false
    }
    else {
      message.error(res.msg || '优化失败')
    }
  }
  catch (e) {
    message.error(`优化失败: ${e.message || e}`)
  }
  finally {
    optimizing.value = false
  }
}

loadList()
</script>

<style scoped>
.skill-page {
  --page-bg: #f3f6fa;
  --panel-bg: var(--bg-primary);
  --panel-subtle: #f8fafc;
  --panel-border: #dfe6ee;
  --text-strong: #111827;
  --text-body: #475569;
  --text-muted: #64748b;
  --accent: #0369a1;
  --accent-soft: #eaf4fb;
  min-height: 100%;
  padding: 20px;
  color: var(--text-body);
  background: radial-gradient(circle at 100% 0, rgba(14, 116, 144, 0.07), transparent 340px), var(--page-bg);
}

:global(.dark) .skill-page {
  --page-bg: #0d1420;
  --panel-bg: #151f2d;
  --panel-subtle: #111a27;
  --panel-border: #2c3a4d;
  --text-strong: #f1f5f9;
  --text-body: #cbd5e1;
  --text-muted: #94a3b8;
  --accent: #38bdf8;
  --accent-soft: rgba(14, 165, 233, 0.12);
  background: radial-gradient(circle at 100% 0, rgba(14, 165, 233, 0.08), transparent 360px), var(--page-bg);
}

.tool-bar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.tool-bar-actions {
  display: flex;
  gap: 8px;
}

.page-title {
  color: var(--text-strong);
  font-size: 20px;
  font-weight: 600;
}

.page-subtitle {
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 12px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  margin-bottom: 14px;
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  border-radius: 9px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.filter-keyword {
  width: 260px;
}
.filter-status {
  width: 150px;
}

.skill-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.skill-card {
  display: flex;
  min-height: 180px;
  flex-direction: column;
  padding: 16px;
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.skill-card:hover {
  border-color: rgba(3, 105, 161, 0.3);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.skill-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.skill-card-icon {
  display: grid;
  flex: 0 0 auto;
  width: 40px;
  height: 40px;
  color: #f59e0b;
  font-size: 20px;
  place-items: center;
  background: rgba(245, 158, 11, 0.12);
  border-radius: 10px;
}

.skill-card-title-wrap {
  min-width: 0;
  flex: 1;
}

.skill-card-title {
  overflow: hidden;
  margin: 0;
  color: var(--text-strong);
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.skill-card-code {
  display: block;
  overflow: hidden;
  margin-top: 3px;
  color: var(--text-muted);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.skill-card-desc {
  display: -webkit-box;
  overflow: hidden;
  margin: 12px 0 0;
  color: var(--text-body);
  font-size: 12px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.skill-card-meta {
  margin-top: auto;
  padding-top: 12px;
  color: var(--text-muted);
  font-size: 11px;
}

.skill-card-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 10px;
  margin-top: 8px;
  border-top: 1px solid var(--panel-border);
}

.empty-state {
  display: flex;
  min-height: 260px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  color: var(--text-muted);
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  border-radius: 10px;
}

.empty-state > i {
  font-size: 36px;
}

.pagination-wrap {
  display: flex;
  padding: 16px 4px 0;
  align-items: center;
  justify-content: space-between;
  color: var(--text-muted);
  font-size: 12px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.file-content {
  max-height: 400px;
  padding: 12px;
  margin: 0;
  overflow-y: auto;
  background: var(--n-color-embedded);
  border-radius: 6px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

@media (max-width: 1400px) {
  .skill-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1024px) {
  .skill-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .filter-bar,
  .tool-bar {
    align-items: stretch;
    flex-direction: column;
  }
  .filter-keyword,
  .filter-status {
    width: 100%;
  }
  .skill-grid {
    grid-template-columns: 1fr;
  }
}
</style>
