<template>
  <div class="ai-crud-config-page">
    <div class="config-page-header">
      <div class="header-left">
        <h2 class="page-title">CRUD 配置管理</h2>
        <span class="page-desc">通过 JSON 配置驱动页面自动渲染，实现零代码业务开发</span>
      </div>
      <div class="header-right">
        <n-input
          v-model:value="searchConfigKey"
          placeholder="搜索配置键 / 表名"
          clearable
          style="width: 240px"
          @update:value="handleSearch"
        >
          <template #prefix>
            <n-icon :size="16" color="#9CA3AF"><SearchOutline /></n-icon>
          </template>
        </n-input>
        <n-button type="primary" @click="goToGenerator()">
          <template #icon>
            <n-icon><SparklesOutline /></n-icon>
          </template>
          AI 生成配置
        </n-button>
      </div>
    </div>

    <div class="config-cards-grid">
      <n-spin :show="loading">
        <div v-if="configList.length > 0" class="cards-container">
          <div
            v-for="item in configList"
            :key="item.id"
            class="config-card"
            :class="{ disabled: item.status !== '0' }"
            @click="handleEdit(item)"
          >
            <div class="card-header">
              <div class="card-icon" :style="{ background: getIconBg(item.configKey) }">
                <n-icon size="22" :color="getIconColor(item.configKey)">
                  <ServerOutline />
                </n-icon>
              </div>
              <div class="card-meta">
                <div class="card-config-key">{{ item.configKey }}</div>
                <div class="card-table-name">{{ item.tableName || '-' }}</div>
              </div>
              <div class="card-badges">
                <n-tag :type="item.mode === 'CONFIG' ? 'success' : 'info'" size="tiny">
                  {{ item.mode === 'CONFIG' ? '配置驱动' : '代码生成' }}
                </n-tag>
                <n-tag v-if="item.status !== '0'" size="tiny" type="warning">停用</n-tag>
              </div>
            </div>
            <div class="card-desc">{{ item.tableComment || '暂无描述' }}</div>
            <div class="card-info">
              <div v-if="item.menuName" class="info-item">
                <n-icon :size="12"><MenuOutline /></n-icon>
                {{ item.menuName }}
              </div>
              <div class="info-item">
                <n-icon :size="12"><TimeOutline /></n-icon>
                {{ formatDate(item.createTime) }}
              </div>
            </div>
            <div class="card-actions" @click.stop>
              <n-button size="tiny" @click="goToGenerator(item.configKey)">
                <template #icon><n-icon><SparklesOutline /></n-icon></template>
                AI 生成
              </n-button>
              <n-button size="tiny" @click="handlePreview(item.configKey)">
                <template #icon><n-icon><EyeOutline /></n-icon></template>
                预览
              </n-button>
              <n-button size="tiny" @click="handleDownloadCode(item.configKey)">
                <template #icon><n-icon><CloudDownloadOutline /></n-icon></template>
                下载
              </n-button>
              <n-popconfirm @positive-click="handleDelete(item)">
                <template #trigger>
                  <n-button size="tiny" type="error">
                    <template #icon><n-icon><TrashOutline /></n-icon></template>
                    删除
                  </n-button>
                </template>
                确认删除该配置？
              </n-popconfirm>
            </div>
          </div>
        </div>
        <div v-else-if="!loading" class="empty-state">
          <n-icon :size="48" color="#D1D5DB"><FolderOpenOutline /></n-icon>
          <p>暂无 CRUD 配置</p>
          <n-button type="primary" @click="goToGenerator()">
            <template #icon><n-icon><SparklesOutline /></n-icon></template>
            创建第一个配置
          </n-button>
        </div>
      </n-spin>
    </div>

    <div v-if="total > 0" class="config-pagination">
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

    <n-drawer
      v-model:show="drawerVisible"
      :width="960"
      placement="right"
      :mask-closable="false"
      @after-leave="handleDrawerClose"
    >
      <n-drawer-content :title="drawerTitle" :closable="true">
        <n-tabs v-model:value="activeTab" type="line" animated>
          <n-tab-pane name="basic" tab="基础信息">
            <n-form
              ref="basicFormRef"
              :model="formData"
              :rules="basicRules"
              label-placement="left"
              label-width="100"
              style="max-width: 600px; padding: 16px 0;"
            >
              <n-form-item label="配置键" path="configKey">
                <n-input v-model:value="formData.configKey" placeholder="小写字母开头，仅含小写字母+数字+下划线" :disabled="isEdit" />
              </n-form-item>
              <n-form-item label="数据表" path="tableName">
                <n-input v-model:value="formData.tableName" placeholder="请输入表名" />
              </n-form-item>
              <n-form-item label="表描述" path="tableComment">
                <n-input v-model:value="formData.tableComment" placeholder="请输入表描述" />
              </n-form-item>
              <n-form-item label="模式" path="mode">
                <n-radio-group v-model:value="formData.mode">
                  <n-radio-button value="CONFIG">配置驱动</n-radio-button>
                  <n-radio-button value="CODEGEN">代码生成</n-radio-button>
                </n-radio-group>
              </n-form-item>
              <n-form-item label="状态" path="status">
                <n-switch
                  :value="formData.status === '0'"
                  @update:value="formData.status = $event ? '0' : '1'"
                >
                  <template #checked>启用</template>
                  <template #unchecked>停用</template>
                </n-switch>
              </n-form-item>
              <n-form-item label="菜单名称" path="menuName">
                <n-input v-model:value="formData.menuName" placeholder="请输入菜单名称" />
              </n-form-item>
              <n-form-item label="菜单排序" path="menuSort">
                <n-input-number v-model:value="formData.menuSort" placeholder="排序值" style="width: 100%" />
              </n-form-item>
            </n-form>
          </n-tab-pane>

          <n-tab-pane name="search" tab="搜索配置">
            <div class="tab-pane-content">
              <SchemaFieldEditor
                mode="search"
                :value="formData.searchSchema || ''"
                @update:value="v => formData.searchSchema = v"
              />
            </div>
          </n-tab-pane>

          <n-tab-pane name="columns" tab="表格列配置">
            <div class="tab-pane-content">
              <SchemaFieldEditor
                mode="columns"
                :value="formData.columnsSchema || ''"
                @update:value="v => formData.columnsSchema = v"
              />
            </div>
          </n-tab-pane>

          <n-tab-pane name="edit" tab="编辑表单配置">
            <div class="tab-pane-content">
              <SchemaFieldEditor
                mode="edit"
                :value="formData.editSchema || ''"
                @update:value="v => formData.editSchema = v"
              />
            </div>
          </n-tab-pane>

          <n-tab-pane name="api" tab="接口配置">
            <div class="tab-pane-content">
              <ApiConfigEditor
                :value="formData.apiConfig || ''"
                @update:value="v => formData.apiConfig = v"
              />
            </div>
          </n-tab-pane>

          <n-tab-pane name="dict" tab="字典配置">
            <div class="tab-pane-content">
              <DictConfigPanel
                :search-schema="formData.searchSchema || ''"
                :columns-schema="formData.columnsSchema || ''"
                :edit-schema="formData.editSchema || ''"
                :dict-config="formData.dictConfig || ''"
                @update:value="v => formData.dictConfig = v"
              />
            </div>
          </n-tab-pane>

          <n-tab-pane name="desensitize" tab="脱敏配置">
            <div class="tab-pane-content">
              <DesensitizeConfigPanel
                :value="formData.desensitizeConfig || ''"
                :columns-schema="formData.columnsSchema || ''"
                @update:value="v => formData.desensitizeConfig = v"
              />
            </div>
          </n-tab-pane>

          <n-tab-pane name="encrypt" tab="加解密配置">
            <div class="tab-pane-content">
              <EncryptConfigPanel
                :value="formData.encryptConfig || ''"
                @update:value="v => formData.encryptConfig = v"
              />
            </div>
          </n-tab-pane>

          <n-tab-pane name="trans" tab="翻译配置">
            <div class="tab-pane-content">
              <TransConfigPanel
                :value="formData.transConfig || ''"
                :columns-schema="formData.columnsSchema || ''"
                :edit-schema="formData.editSchema || ''"
                @update:value="v => formData.transConfig = v"
              />
            </div>
          </n-tab-pane>

          <n-tab-pane name="options" tab="扩展配置">
            <div class="tab-pane-content">
              <n-input
                v-model:value="formData.options"
                type="textarea"
                :rows="12"
                placeholder="请输入扩展配置 JSON"
                style="font-family: 'Fira Code', 'Monaco', monospace; font-size: 13px;"
              />
            </div>
          </n-tab-pane>
        </n-tabs>

        <template #footer>
          <n-space justify="end">
            <n-button @click="drawerVisible = false">取消</n-button>
            <n-button type="primary" :loading="submitLoading" @click="handleSubmit">
              保存
            </n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  SparklesOutline, EyeOutline, CloudDownloadOutline,
  SearchOutline, ServerOutline, MenuOutline, TimeOutline,
  TrashOutline, FolderOpenOutline,
} from '@vicons/ionicons5'
import SchemaFieldEditor from './components/SchemaFieldEditor.vue'
import ApiConfigEditor from './components/ApiConfigEditor.vue'
import DictConfigPanel from './components/DictConfigPanel.vue'
import DesensitizeConfigPanel from './components/DesensitizeConfigPanel.vue'
import EncryptConfigPanel from './components/EncryptConfigPanel.vue'
import TransConfigPanel from './components/TransConfigPanel.vue'
import { request } from '@/utils'

defineOptions({ name: 'AiCrudConfig' })

const router = useRouter()

const loading = ref(false)
const configList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const searchConfigKey = ref('')

const drawerVisible = ref(false)
const drawerTitle = ref('新增配置')
const isEdit = ref(false)
const submitLoading = ref(false)
const activeTab = ref('basic')
const basicFormRef = ref(null)

const formData = ref({
  id: null,
  configKey: '',
  tableName: '',
  tableComment: '',
  mode: 'CONFIG',
  status: '0',
  menuName: '',
  menuSort: 0,
  searchSchema: '',
  columnsSchema: '',
  editSchema: '',
  apiConfig: '',
  dictConfig: '',
  desensitizeConfig: '',
  encryptConfig: '',
  transConfig: '',
  options: '',
})

const basicRules = {
  configKey: [
    { required: true, message: '请输入配置键', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]{1,63}$/, message: '小写字母开头，仅含小写字母+数字+下划线，2-64字符', trigger: 'blur' },
  ],
  tableName: [{ required: true, message: '请输入表名', trigger: 'blur' }],
  mode: [{ required: true, message: '请选择模式', trigger: 'change' }],
}

function resetFormData() {
  formData.value = {
    id: null,
    configKey: '',
    tableName: '',
    tableComment: '',
    mode: 'CONFIG',
    status: '0',
    menuName: '',
    menuSort: 0,
    searchSchema: '',
    columnsSchema: '',
    editSchema: '',
    apiConfig: '',
    dictConfig: '',
    desensitizeConfig: '',
    encryptConfig: '',
    transConfig: '',
    options: '',
  }
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    }
    if (searchConfigKey.value) {
      params.configKey = searchConfigKey.value
      params.tableName = searchConfigKey.value
    }
    const res = await request.get('/ai/crud-config/page', { params })
    if (res.code === 200) {
      configList.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } catch (e) {
    console.error('加载配置列表失败:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadList()
}

function goToGenerator(configKey) {
  const query = configKey ? { configKey } : {}
  router.push({ path: '/ai/crud-generator', query })
}

function handlePreview(configKey) {
  const resolved = router.resolve({ path: `/ai/crud-page/${configKey}` })
  window.open(resolved.href, '_blank')
}

async function handleDownloadCode(configKey) {
  const { useAuthStore } = await import('@/store')
  const authStore = useAuthStore()
  const BASE_URL = import.meta.env.VITE_REQUEST_PREFIX || ''
  const url = `${BASE_URL}/ai/crud-config/codegen/download/${configKey}`
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
    a.download = `${configKey}-code.zip`
    a.click()
    URL.revokeObjectURL(blobUrl)
    window.$message?.success('代码包下载成功')
  } catch (e) {
    window.$message?.error('下载失败: ' + e.message)
  }
}

function handleEdit(item) {
  isEdit.value = true
  drawerTitle.value = '编辑配置'
  activeTab.value = 'basic'
  formData.value = {
    id: item.id,
    configKey: item.configKey || '',
    tableName: item.tableName || '',
    tableComment: item.tableComment || '',
    mode: item.mode || 'CONFIG',
    status: item.status || '0',
    menuName: item.menuName || '',
    menuSort: item.menuSort || 0,
    searchSchema: item.searchSchema || '',
    columnsSchema: item.columnsSchema || '',
    editSchema: item.editSchema || '',
    apiConfig: item.apiConfig || '',
    dictConfig: item.dictConfig || '',
    desensitizeConfig: item.desensitizeConfig || '',
    encryptConfig: item.encryptConfig || '',
    transConfig: item.transConfig || '',
    options: item.options || '',
  }
  drawerVisible.value = true
}

async function handleSubmit() {
  if (activeTab.value === 'basic' && basicFormRef.value) {
    try {
      await basicFormRef.value.validate()
    } catch {
      return
    }
  }
  submitLoading.value = true
  try {
    const payload = { ...formData.value }
    if (isEdit.value) {
      await request.put('/ai/crud-config', payload)
    } else {
      await request.post('/ai/crud-config', payload)
    }
    window.$message.success(isEdit.value ? '更新成功' : '创建成功')
    drawerVisible.value = false
    loadList()
  } catch (e) {
    const msg = e?.response?.data?.msg || e?.message || '操作失败'
    window.$message.error(msg)
  } finally {
    submitLoading.value = false
  }
}

function handleDrawerClose() {
  resetFormData()
  isEdit.value = false
}

async function handleDelete(item) {
  try {
    await request.delete(`/ai/crud-config/${item.id}`)
    window.$message.success('删除成功')
    loadList()
  } catch (e) {
    const msg = e?.response?.data?.msg || e?.message || '删除失败'
    window.$message.error(msg)
  }
}

function formatDate(time) {
  if (!time) return ''
  const d = new Date(time)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
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

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.ai-crud-config-page {
  width: 100%;
  height: 100%;
  background: #F8FAFC;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.config-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px 16px;
  background: #FFFFFF;
  border-bottom: 1px solid #F1F5F9;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #0F172A;
  margin: 0;
  letter-spacing: -0.02em;
}

.page-desc {
  font-size: 13px;
  color: #94A3B8;
}

.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.config-cards-grid {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

.config-cards-grid :deep(.n-spin-content) {
  display: block;
}

.cards-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.config-card {
  background: #fff;
  border: 1.5px solid #E5E7EB;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: box-shadow 0.15s, border-color 0.15s;
  cursor: pointer;
}

.config-card:hover {
  border-color: #A5B4FC;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.08);
}

.config-card.disabled {
  opacity: 0.55;
}

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

.card-config-key {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-table-name {
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

.card-desc {
  font-size: 12px;
  color: #6B7280;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item {
  font-size: 11px;
  color: #9CA3AF;
  display: flex;
  align-items: center;
  gap: 3px;
}

.card-actions {
  display: flex;
  gap: 6px;
  margin-top: auto;
  padding-top: 4px;
  border-top: 1px solid #F3F4F6;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
  color: #94A3B8;
}

.empty-state p {
  font-size: 15px;
  margin: 0;
}

.config-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 24px 16px;
  background: #FFFFFF;
  border-top: 1px solid #F1F5F9;
}

.tab-pane-content {
  padding: 16px 0;
}
</style>
