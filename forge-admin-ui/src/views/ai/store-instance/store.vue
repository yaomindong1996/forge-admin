<template>
  <div class="ai-store-page">
    <div class="store-page-head">
      <div class="store-page-head__main">
        <div class="store-page-head__icon">
          <i class="ai-icon:server" aria-hidden="true" />
        </div>
        <div>
          <h1>向量存储实例</h1>
          <p>管理 Milvus / PgVector / Elasticsearch 等向量存储与检索引擎实例，供知识库使用。</p>
        </div>
      </div>
      <div class="store-page-head__actions">
        <NButton type="primary" @click="handleAdd">
          <template #icon>
            <i class="ai-icon:plus" />
          </template>
          新增实例
        </NButton>
      </div>
    </div>

    <div class="store-filter-bar">
      <n-select
        v-model:value="search.category"
        placeholder="全部类别"
        clearable
        :options="categoryOptions"
        size="small"
        style="width: 150px"
        @update:value="handleSearch"
      />
      <n-select
        v-model:value="search.storeType"
        placeholder="全部类型"
        clearable
        :options="storeTypeOptions"
        size="small"
        style="width: 150px"
        @update:value="handleSearch"
      />
      <n-input
        v-model:value="search.instanceName"
        placeholder="搜索实例名称"
        clearable
        size="small"
        style="width: 220px"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <i class="ai-icon:search" />
        </template>
      </n-input>
      <NButton size="small" @click="handleReset">
        重置
      </NButton>
    </div>

    <div class="store-table-card">
      <n-data-table
        :columns="columns"
        :data="list"
        :loading="loading"
        :row-key="row => row.id"
        :scroll-x="1100"
        size="small"
      />
      <div class="store-pagination">
        <n-pagination
          :page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="pageSizes"
          show-size-picker
          show-quick-jumper
          size="small"
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </div>

    <!-- 新增/编辑 -->
    <n-modal
      v-model:show="modal.show"
      preset="card"
      :title="modal.isEdit ? '编辑实例' : '新增实例'"
      :style="{ maxWidth: '880px', width: 'calc(100vw - 32px)' }"
    >
      <n-form ref="formRef" :model="modal.form" :rules="rules" label-placement="top" size="medium">
        <!-- 双列卡片：基本信息 + 连接信息 -->
        <div class="vf-row">
          <div class="vf-col">
            <div class="vf-card">
              <div class="vf-card__head">
                <i class="ai-icon:settings" aria-hidden="true" />
                <span>基本信息</span>
              </div>
              <div class="vf-card__body">
                <n-form-item label="向量库类型" path="storeType" required>
                  <n-select v-model:value="modal.form.storeType" :options="storeTypeOptions" placeholder="请选择类型" />
                </n-form-item>
                <n-form-item label="类别" path="category" required>
                  <n-select v-model:value="modal.form.category" :options="categoryOptions" placeholder="请选择类别" />
                </n-form-item>
                <n-form-item label="名称" path="instanceName" required>
                  <n-input v-model:value="modal.form.instanceName" placeholder="例如：生产环境 Milvus" />
                </n-form-item>
                <n-form-item label="状态" path="status">
                  <n-radio-group v-model:value="modal.form.status">
                    <n-radio v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </n-radio>
                  </n-radio-group>
                </n-form-item>
              </div>
            </div>
          </div>
          <div class="vf-col">
            <div class="vf-card">
              <div class="vf-card__head">
                <i class="ai-icon:link" aria-hidden="true" />
                <span>连接信息</span>
              </div>
              <div class="vf-card__body">
                <n-form-item label="主机地址" path="configHost" required>
                  <n-input v-model:value="connConfig.host" placeholder="如 192.168.1.100" />
                </n-form-item>
                <n-form-item label="端口" path="configPort" required>
                  <n-input-number v-model:value="connConfig.port" :min="1" :max="65535" placeholder="如 19530" style="width: 100%" />
                </n-form-item>
                <n-form-item label="数据库名称" path="configDatabase" required>
                  <n-input v-model:value="connConfig.database" placeholder="向量库中实际的数据库或集合名称" />
                </n-form-item>
                <n-form-item label="用户名">
                  <n-input v-model:value="connConfig.user" placeholder="选填" />
                </n-form-item>
                <n-form-item label="密码">
                  <n-input v-model:value="connConfig.password" type="password" show-password-on="click" placeholder="选填" />
                </n-form-item>
              </div>
            </div>
          </div>
        </div>

        <!-- 配置信息：JSON 编辑器 -->
        <div class="vf-card vf-card--last">
          <div class="vf-card__head">
            <i class="ai-icon:code" aria-hidden="true" />
            <span>配置信息</span>
          </div>
          <div class="vf-card__body">
            <n-input
              v-model:value="configEditorText"
              type="textarea"
              :rows="4"
              placeholder="{&quot;token&quot;:&quot;&quot;,&quot;database&quot;:&quot;default&quot;}"
              @blur="syncConfigFromEditor"
            />
            <span class="config-editor__tip">主机地址 / 端口 / 数据库已在上方填写，此处可补充 token 等额外配置。</span>
          </div>
        </div>
      </n-form>
      <template #action>
        <div class="modal-footer-actions">
          <NButton @click="modal.show = false">
            取消
          </NButton>
          <NButton type="primary" :loading="modal.saving" @click="handleSave">
            确定
          </NButton>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NButton, NPopconfirm } from 'naive-ui'
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import {
  storeInstancePage as fetchStorePage,
  storeInstanceCreate,
  storeInstanceDelete,
  storeInstanceTest,
  storeInstanceUpdate,
} from '@/api/ai'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

defineOptions({ name: 'AiStoreInstance' })

const { dict } = useDict('ai_status', 'ai_store_instance_category', 'ai_vector_store_type')

const statusOptions = computed(() => dict.value.ai_status || [])
const categoryOptions = computed(() => dict.value.ai_store_instance_category || [])
const storeTypeOptions = computed(() => dict.value.ai_vector_store_type || [])

const pageSizes = [10, 20, 50]
const search = reactive({ category: null, storeType: null, instanceName: '' })
const list = ref([])
const loading = ref(false)
const pagination = reactive({ pageNum: 1, pageSize: 10, itemCount: 0 })

const formRef = ref(null)
const modal = reactive({ show: false, isEdit: false, saving: false, form: createForm() })
const configEditorText = ref('')
// 连接信息独立字段（组装为 configJson 存库）
const connConfig = reactive({
  host: '',
  port: null,
  user: '',
  password: '',
  database: '',
})

function createForm() {
  return {
    instanceName: '',
    category: 'vector_store',
    storeType: null,
    configJson: '',
    status: '0',
  }
}

const rules = {
  instanceName: [{ required: true, message: '请输入实例名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }],
  storeType: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

function normalizeConfigJson(str) {
  try {
    return JSON.stringify(JSON.parse(str || '{}'), null, 2)
  }
  catch {
    return (str || '').trim()
  }
}

/** 从 configJson 解析出 host/port 等独立字段 */
function configJsonToConnConfig(configJson) {
  try {
    const obj = JSON.parse(configJson || '{}')
    connConfig.host = obj.host ?? ''
    connConfig.port = obj.port ?? null
    connConfig.user = obj.user ?? ''
    connConfig.password = obj.password ?? ''
    connConfig.database = obj.database ?? ''
  }
  catch {
    connConfig.host = ''
    connConfig.port = null
    connConfig.user = ''
    connConfig.password = ''
    connConfig.database = ''
  }
}

/** 把独立字段 + JSON 编辑器内容组装成 configJson */
function syncConfigFromEditor() {
  const extra = {}
  try {
    Object.assign(extra, JSON.parse(configEditorText.value || '{}'))
  }
  catch { /* ignore invalid json */ }
  modal.form.configJson = JSON.stringify({
    ...extra,
    host: connConfig.host,
    port: connConfig.port,
    user: connConfig.user,
    password: connConfig.password,
    database: connConfig.database,
  }, null, 2)
}

/** 打开弹窗时回填独立字段 */
function resetConnConfig() {
  connConfig.host = ''
  connConfig.port = null
  connConfig.user = ''
  connConfig.password = ''
  connConfig.database = ''
}

watch(() => modal.show, (show) => {
  if (show) {
    configJsonToConnConfig(modal.form.configJson)
    configEditorText.value = normalizeConfigJson(modal.form.configJson)
  }
})

async function load() {
  loading.value = true
  try {
    const res = await fetchStorePage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...(search.category ? { category: search.category } : {}),
      ...(search.storeType ? { storeType: search.storeType } : {}),
      ...(search.instanceName ? { instanceName: search.instanceName } : {}),
    })
    if (res.code === 200 && res.data) {
      list.value = res.data.records || []
      pagination.itemCount = Number(res.data.total || 0)
    }
  }
  catch {}
  finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  load()
}

function handleReset() {
  search.category = null
  search.storeType = null
  search.instanceName = ''
  pagination.pageNum = 1
  load()
}

function handlePageChange(page) {
  pagination.pageNum = page
  load()
}

function handlePageSizeChange(pageSize) {
  pagination.pageSize = pageSize
  pagination.pageNum = 1
  load()
}

function handleAdd() {
  modal.isEdit = false
  modal.form = createForm()
  modal.show = true
}

async function handleEdit(row) {
  modal.isEdit = true
  modal.form = { ...createForm(), ...row }
  modal.show = true
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  }
  catch { return }
  syncConfigFromEditor()
  modal.saving = true
  try {
    const res = modal.isEdit ? await storeInstanceUpdate(modal.form) : await storeInstanceCreate(modal.form)
    if (res.code === 200) {
      window.$message.success(modal.isEdit ? '更新成功' : '新增成功')
      modal.show = false
      await load()
    }
    else {
      window.$message.error(res.msg || '操作失败')
    }
  }
  catch (e) {
    window.$message.error(e.message || '操作失败')
  }
  finally {
    modal.saving = false
  }
}

async function handleDelete(id) {
  try {
    const res = await storeInstanceDelete(id)
    if (res.code === 200) {
      window.$message.success('删除成功')
      await load()
    }
    else {
      window.$message.error(res.msg || '删除失败')
    }
  }
  catch (e) {
    window.$message.error(e.message || '删除失败')
  }
}

async function handleTest(row) {
  try {
    const res = await storeInstanceTest(row.id)
    if (res.code === 200)
      window.$message.success(res.data ? '连接成功' : '连接失败')
    else
      window.$message.error(res.msg || '连接测试失败')
  }
  catch (e) {
    window.$message.error(e.message || '连接测试失败')
  }
}

const columns = [
  { title: '实例名称', key: 'instanceName', width: 180, ellipsis: { tooltip: true } },
  {
    title: '类别',
    key: 'category',
    width: 110,
    render(row) { return h(DictTag, { dictType: 'ai_store_instance_category', value: row.category, size: 'small' }) },
  },
  {
    title: '类型',
    key: 'storeType',
    width: 130,
    render(row) { return h(DictTag, { dictType: 'ai_vector_store_type', value: row.storeType, size: 'small' }) },
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    align: 'center',
    render(row) { return h(DictTag, { dictType: 'ai_status', value: row.status, size: 'small' }) },
  },
  { title: '更新时间', key: 'updateTime', width: 160, render(row) { return row.updateTime ? String(row.updateTime).replace('T', ' ').slice(0, 16) : '—' } },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    fixed: 'right',
    render(row) {
      const actions = [
        h(NButton, { text: true, size: 'small', class: 'text-info', onClick: () => handleTest(row) }, { default: () => '测试连接' }),
        h(NButton, { text: true, size: 'small', class: 'text-primary', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
        h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
          trigger: () => h(NButton, { text: true, size: 'small', class: 'text-error' }, { default: () => '删除' }),
          default: () => '确定删除该实例吗？',
        }),
      ]
      return h('div', { class: 'store-actions' }, actions)
    },
  },
]

onMounted(() => {
  load()
})
</script>

<style scoped>
.ai-store-page {
  --page-bg: #f3f6fa;
  --panel-bg: var(--bg-primary);
  --panel-subtle: #f8fafc;
  --panel-border: #dfe6ee;
  --text-strong: #111827;
  --text-body: #475569;
  --text-muted: #64748b;
  --accent: #0369a1;
  --accent-soft: #eaf4fb;
  --accent-border: #b9d9ec;
  --shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
  min-height: 100%;
  padding: 20px;
  color: var(--text-body);
  background: var(--page-bg);
}

:global(.dark) .ai-store-page {
  --page-bg: #0d1420;
  --panel-bg: #151f2d;
  --panel-subtle: #111a27;
  --panel-border: #2c3a4d;
  --text-strong: #f1f5f9;
  --text-body: #cbd5e1;
  --text-muted: #94a3b8;
  --accent: #38bdf8;
  --accent-soft: rgba(14, 165, 233, 0.12);
  --accent-border: rgba(56, 189, 248, 0.3);
}

.store-page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 20px 24px;
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  border-radius: 12px;
  box-shadow: var(--shadow);
}

.store-page-head__main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 14px;
}

.store-page-head__icon {
  display: grid;
  flex: 0 0 48px;
  width: 48px;
  height: 48px;
  color: #fff;
  font-size: 22px;
  place-items: center;
  background: linear-gradient(145deg, #075985, #0e7490);
  border-radius: 12px;
}

.store-page-head h1 {
  margin: 0;
  color: var(--text-strong);
  font-size: 20px;
  font-weight: 600;
}

.store-page-head p {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 12px;
}

.store-page-head__actions {
  flex: 0 0 auto;
}

.store-filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 12px 16px;
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  border-radius: 10px;
}

.store-table-card {
  overflow: hidden;
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  border-radius: 10px;
}

.store-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px;
  background: var(--panel-bg);
  border-top: 1px solid var(--panel-border);
}

/* ── 双列卡片表单 ── */
.vf-row {
  display: flex;
  gap: 16px;
  margin-bottom: 14px;
}

.vf-col {
  flex: 1;
  min-width: 0;
}

.vf-card {
  overflow: hidden;
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  border-radius: 10px;
}

.vf-card__head {
  display: flex;
  padding: 10px 16px;
  align-items: center;
  gap: 6px;
  color: var(--text-strong);
  font-size: 13px;
  font-weight: 600;
  background: var(--panel-subtle);
  border-bottom: 1px solid var(--panel-border);
}

.vf-card__head i {
  color: var(--accent);
  font-size: 15px;
}

.vf-card__body {
  padding: 14px 16px 4px;
}

.vf-card--last {
  margin-bottom: 0;
}

.config-editor__tip {
  display: block;
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 11px;
}

.modal-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.store-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  white-space: nowrap;
}

:deep(.n-data-table .n-data-table-th),
:deep(.n-data-table .n-data-table-td) {
  padding: 10px 12px;
}

@media (max-width: 720px) {
  .store-filter-bar {
    flex-wrap: wrap;
  }

  .vf-row {
    flex-direction: column;
  }
}
</style>
