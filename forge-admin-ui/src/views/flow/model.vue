<template>
  <div class="flow-page">
    <!-- 统计卡片 -->
    <FlowModelStats
      :total-count="totalCount"
      :designing-count="designingCount"
      :deployed-count="deployedCount"
      :suspended-count="suspendedCount"
      :disabled-count="disabledCount"
      @filter="handleFilter"
    />

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="title-row">
          <div class="title-icon">
            <i class="i-material-symbols:device-hub" />
          </div>
          <h2 class="page-title">
            流程模型
          </h2>
        </div>
      </div>
      <div class="header-right">
        <n-button type="primary" @click="handleAdd">
          <i class="i-material-symbols:add mr-2" />
          新增模型
        </n-button>
        <n-input
          v-model:value="queryParams.modelName"
          placeholder="搜索模型名称或 Key"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
        <NTreeSelect
          v-model:value="queryParams.category"
          placeholder="流程分类"
          clearable
          class="category-select"
          :options="categoryTreeOptions"
          :default-expand-all="true"
        />
        <n-select
          v-model:value="queryParams.status"
          placeholder="状态"
          clearable
          class="status-select"
          :options="statusOptions"
        />
        <n-button @click="handleSearch">
          <i class="i-material-symbols:search mr-2" />
          搜索
        </n-button>
        <n-button @click="handleReset">
          <i class="i-material-symbols:refresh mr-2" />
          重置
        </n-button>
      </div>
    </div>

    <!-- 模型列表 -->
    <n-spin :show="loading" class="model-list-spin">
      <div v-if="dataSource.length > 0" class="model-grid">
        <div
          v-for="item in dataSource"
          :key="item.id"
          class="model-card"
          @click="handleDesign(item)"
        >
          <div class="card-header">
            <div class="card-icon" :class="iconClass(item)">
              <i :class="iconName(item)" />
            </div>
            <span class="status-tag" :class="statusClass(item.status)">
              {{ statusText[item.status] }}
            </span>
          </div>
          <div class="card-body">
            <div class="card-title">
              {{ item.modelName }}
            </div>
            <div class="card-key">
              {{ item.modelKey }}
            </div>
            <div class="card-desc">
              {{ item.description || '暂无描述' }}
            </div>
          </div>
          <div class="card-footer">
            <div class="card-meta">
              <div class="meta-item">
                <i class="i-material-symbols:history-outline" />
                {{ formatDate(item.updateTime) }}
              </div>
              <div class="meta-item">
                <i class="i-material-symbols:deployed-code-outline" />
                v{{ item.version || 1 }}
              </div>
            </div>
            <div class="card-actions">
              <n-button
                size="small"
                type="primary"
                secondary
                @click.stop="handleDesign(item)"
              >
                <i class="i-material-symbols:edit-outline mr-1" />
                设计
              </n-button>
              <n-button
                v-if="item.status === 0"
                size="small"
                type="primary"
                @click.stop="handleDeploy(item)"
              >
                部署
              </n-button>
              <n-button
                v-else-if="item.status === 1"
                size="small"
                @click.stop="handleViewInstances(item)"
              >
                查看实例
              </n-button>
              <n-dropdown
                trigger="click"
                :options="getActionOptions(item)"
                @select="key => handleActionSelect(key, item)"
              >
                <n-button size="small" @click.stop>
                  <template #icon>
                    <NIcon><EllipsisVertical /></NIcon>
                  </template>
                </n-button>
              </n-dropdown>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <n-empty
        v-else-if="!loading"
        description="暂无流程模型，点击「新增模型」开始设计"
        class="empty-state"
      >
        <template #extra>
          <n-button type="primary" @click="handleAdd">
            <i class="i-material-symbols:add mr-2" />
            新增模型
          </n-button>
        </template>
      </n-empty>
    </n-spin>

    <!-- 分页 -->
    <div v-if="pagination.itemCount > 0" class="pagination-wrapper">
      <n-pagination
        v-model:page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :item-count="pagination.itemCount"
        :page-sizes="[12, 24, 48]"
        show-size-picker
        @update:page="fetchData"
        @update:page-size="handlePageSizeChange"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
      <n-modal
        v-model:show="showModal"
        preset="card"
        :title="modalTitle"
        style="width: 620px"
        :mask-closable="false"
      >
        <n-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-placement="left"
          label-width="100"
        >
          <n-grid :cols="2" :x-gap="16">
            <n-form-item-gi label="模型名称" path="modelName" :span="2">
              <n-input v-model:value="formData.modelName" placeholder="请输入模型名称" />
            </n-form-item-gi>
            <n-form-item-gi label="模型Key" path="modelKey">
              <n-input
                v-model:value="formData.modelKey"
                placeholder="唯一标识，如 leave-apply"
                :disabled="isEdit"
              />
            </n-form-item-gi>
            <n-form-item-gi label="流程分类" path="category">
              <NTreeSelect
                v-model:value="formData.category"
                placeholder="请选择分类"
                :options="categoryTreeOptions"
                :default-expand-all="true"
              />
            </n-form-item-gi>
            <n-form-item-gi label="表单类型" path="formType">
              <n-select
                v-model:value="formData.formType"
                placeholder="请选择表单类型"
                :options="formTypeOptions"
              />
            </n-form-item-gi>
            <n-form-item-gi label="流程类型" path="flowType">
              <n-input v-model:value="formData.flowType" placeholder="如 approval" />
            </n-form-item-gi>
            <n-form-item-gi label="事件通知" path="notifyType" :span="2">
              <div class="notify-section">
                <n-radio-group v-model:value="formData.notifyType" class="radio-group">
                  <n-space>
                    <n-radio value="none">
                      不通知
                    </n-radio>
                    <n-radio value="redis">
                      Redis Pub/Sub
                      <n-tooltip trigger="hover" placement="top">
                        <template #trigger>
                          <i class="i-material-symbols:info-outline ml-2 cursor-help" />
                        </template>
                        流程完成/驳回/取消时，发布消息到 Redis 频道
                      </n-tooltip>
                    </n-radio>
                    <n-radio value="webhook">
                      HTTP Webhook
                      <n-tooltip trigger="hover" placement="top">
                        <template #trigger>
                          <i class="i-material-symbols:info-outline ml-2 cursor-help" />
                        </template>
                        流程完成/驳回/取消时，POST 请求回调 Webhook URL
                      </n-tooltip>
                    </n-radio>
                  </n-space>
                </n-radio-group>
                <n-input
                  v-if="formData.notifyType === 'webhook'"
                  v-model:value="formData.webhookUrl"
                  placeholder="回调地址，如 http://your-service/api/flow/callback"
                  clearable
                />
              </div>
            </n-form-item-gi>
            <n-form-item-gi label="描述" path="description" :span="2">
              <n-input
                v-model:value="formData.description"
                type="textarea"
                placeholder="请输入描述"
                :rows="3"
              />
            </n-form-item-gi>
          </n-grid>
        </n-form>
        <template #footer>
          <n-space justify="end">
            <n-button @click="showModal = false">
              取消
            </n-button>
            <n-button type="primary" :loading="submitLoading" @click="handleSubmit">
              确定
            </n-button>
          </n-space>
        </template>
      </n-modal>
    </Teleport>

    <VersionHistory
      v-if="showVersionHistory"
      :model-id="currentModelId"
      :current-version="currentModelVersion"
      @close="showVersionHistory = false"
      @refresh="fetchData"
    />
  </div>
</template>

<script setup>
import { CopyOutline, CreateOutline, EllipsisVertical, PauseCircleOutline, PlayCircleOutline, TimeOutline, TrashOutline } from '@vicons/ionicons5'
import { NIcon, NTreeSelect } from 'naive-ui'
import { h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import flowApi from '@/api/flow'
import FlowModelStats from '@/components/flow/FlowModelStats.vue'
import VersionHistory from './version.vue'

const router = useRouter()

const statusOptions = [
  { label: '设计中', value: 0 },
  { label: '已部署', value: 1 },
  { label: '已挂起', value: 2 },
  { label: '已禁用', value: 3 },
]
const formTypeOptions = [
  { label: '动态表单', value: 'dynamic' },
  { label: '外置表单', value: 'external' },
  { label: '无表单', value: 'none' },
]
const categoryOptions = ref([])
const categoryTreeOptions = ref([])

function buildTreeSelectOptions(treeData) {
  return treeData.map(item => ({
    label: item.categoryName,
    value: item.id,
    key: item.id,
    children: item.children && item.children.length > 0 ? buildTreeSelectOptions(item.children) : undefined,
  }))
}

const statusText = { 0: '设计中', 1: '已部署', 2: '已挂起', 3: '已禁用' }

function statusClass(status) {
  const cls = { 0: 'designing', 1: 'deployed', 2: 'suspended', 3: 'disabled' }
  return cls[status] || 'default'
}

const iconColorClasses = ['icon-bg-0', 'icon-bg-1', 'icon-bg-2', 'icon-bg-3', 'icon-bg-4', 'icon-bg-5']

function iconClass(item) {
  const idx = Math.abs(hashStr(item.modelKey || '')) % iconColorClasses.length
  return iconColorClasses[idx]
}

function iconName(item) {
  const typeMap = {
    leave: 'i-material-symbols:free-cancellation-outline',
    expense: 'i-material-symbols:receipt-long-outline',
    approval: 'i-material-symbols:approval-delegation-outline',
    purchase: 'i-material-symbols:shopping-cart-outline',
  }
  const ft = item.flowType || item.businessType || ''
  for (const [k, v] of Object.entries(typeMap)) {
    if (ft.toLowerCase().includes(k))
      return v
  }
  return 'i-material-symbols:device-hub'
}

function hashStr(s) {
  let h = 0
  for (let i = 0; i < s.length; i++)
    h = (Math.imul(31, h) + s.charCodeAt(i)) | 0
  return h
}

function formatDate(d) {
  if (!d)
    return ''
  return d.slice(0, 10)
}

function getActionOptions(row) {
  const renderIcon = (icon) => {
    return () => h(NIcon, null, { default: () => h(icon) })
  }
  const opts = [
    { label: '编辑信息', key: 'edit', icon: renderIcon(CreateOutline) },
    { label: '版本历史', key: 'versionHistory', icon: renderIcon(TimeOutline) },
    { label: '复制模型', key: 'copy', icon: renderIcon(CopyOutline) },
  ]
  if (row.status === 1) {
    opts.push({ label: '挂起', key: 'suspend', icon: renderIcon(PauseCircleOutline) })
  }
  if (row.status === 2) {
    opts.push({ label: '激活', key: 'activate', icon: renderIcon(PlayCircleOutline) })
  }
  opts.push({ type: 'divider', key: 'd1' })
  opts.push({ label: '删除', key: 'delete', icon: renderIcon(TrashOutline), props: { style: 'color: #d03050' } })
  return opts
}

function handleActionSelect(key, row) {
  const map = { edit: handleEdit, copy: handleCopy, versionHistory: handleVersionHistory, suspend: handleSuspend, activate: handleActivate, delete: handleDelete }
  map[key]?.(row)
}

const queryParams = reactive({ modelName: '', category: null, status: null })
const dataSource = ref([])
const loading = ref(false)
const pagination = reactive({ page: 1, pageSize: 20, itemCount: 0 })
const showVersionHistory = ref(false)
const currentModelId = ref('')
const currentModelVersion = ref(null)

const totalCount = ref(0)
const designingCount = ref(0)
const deployedCount = ref(0)
const suspendedCount = ref(0)
const disabledCount = ref(0)

async function fetchCategories() {
  try {
    const res = await flowApi.getCategoryTreeSelect(false)
    if (res.code === 200) {
      categoryTreeOptions.value = buildTreeSelectOptions(res.data || [])
      categoryOptions.value = (res.data || []).map(item => ({
        label: item.categoryName,
        value: item.categoryCode,
      }))
    }
  }
  catch {
    console.error('加载分类失败')
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await flowApi.getModelPage({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      ...queryParams,
    })
    if (res.code === 200) {
      dataSource.value = res.data?.records || []
      pagination.itemCount = res.data?.total || 0
      totalCount.value = res.data?.total || 0
      designingCount.value = dataSource.value.filter(r => r.status === 0).length
      deployedCount.value = dataSource.value.filter(r => r.status === 1).length
      suspendedCount.value = dataSource.value.filter(r => r.status === 2).length
      disabledCount.value = dataSource.value.filter(r => r.status === 3).length
    }
  }
  catch {
    console.error('加载模型列表失败')
  }
  finally {
    loading.value = false
  }
}

function handlePageSizeChange(v) {
  pagination.pageSize = v
  pagination.page = 1
  fetchData()
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  Object.assign(queryParams, { modelName: '', category: null, status: null })
  pagination.page = 1
  fetchData()
}

function handleFilter(status) {
  if (status === 'all') {
    queryParams.status = null
  }
  else {
    queryParams.status = status
  }
  pagination.page = 1
  fetchData()
}

const showModal = ref(false)
const modalTitle = ref('新增模型')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const formData = reactive({
  id: '',
  modelName: '',
  modelKey: '',
  category: '',
  flowType: '',
  formType: 'dynamic',
  description: '',
  notifyType: 'none',
  webhookUrl: '',
})
const rules = {
  modelName: { required: true, message: '请输入模型名称', trigger: 'blur' },
  modelKey: { required: true, message: '请输入模型Key', trigger: 'blur' },
  category: { required: true, message: '请选择分类', trigger: 'change' },
}

function handleAdd() {
  isEdit.value = false
  modalTitle.value = '新增模型'
  Object.assign(formData, { id: '', modelName: '', modelKey: '', category: '', flowType: '', formType: 'dynamic', description: '', notifyType: 'none', webhookUrl: '' })
  showModal.value = true
}

function handleEdit(row) {
  isEdit.value = true
  modalTitle.value = '编辑模型'
  Object.assign(formData, row)
  showModal.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    const api = isEdit.value ? flowApi.updateModel : flowApi.createModel
    const res = await api(formData)
    if (res.code === 200) {
      window.$message?.success(isEdit.value ? '编辑成功' : '新增成功')
      showModal.value = false
      fetchData()
    }
    else {
      window.$message?.error(res.message || '操作失败')
    }
  }
  catch {
    console.error('提交失败')
  }
  finally {
    submitLoading.value = false
  }
}

function handleDesign(row) {
  router.push({ path: '/flow/design', query: { id: row.id } })
}

function handleViewInstances(row) {
  router.push({ path: '/flow/monitor', query: { modelKey: row.modelKey } })
}

async function handleDeploy(row) {
  window.$dialog?.info({
    title: '确认部署',
    content: `确定要部署「${row.modelName}」吗？部署后流程将可以发起。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      const res = await flowApi.deployModel(row.id)
      if (res.code === 200) {
        window.$message?.success('部署成功')
        fetchData()
      }
      else {
        window.$message?.error(res.message || '部署失败')
      }
    },
  })
}

async function handleCopy(row) {
  window.$dialog?.info({
    title: '复制模型',
    content: `确定要复制「${row.modelName}」吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      const res = await flowApi.copyModel(row.id, `${row.modelName} - 副本`)
      if (res.code === 200) {
        window.$message?.success('复制成功')
        fetchData()
      }
      else {
        window.$message?.error(res.message || '复制失败')
      }
    },
  })
}

async function handleSuspend(row) {
  window.$dialog?.warning({
    title: '确认挂起',
    content: `挂起后，「${row.modelName}」相关的进行中流程实例将暂停，确定继续？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      const res = await flowApi.suspendModel(row.id)
      if (res.code === 200) {
        window.$message?.success('已挂起')
        fetchData()
      }
      else {
        window.$message?.error(res.message || '挂起失败')
      }
    },
  })
}

async function handleActivate(row) {
  const res = await flowApi.activateModel(row.id)
  if (res.code === 200) {
    window.$message?.success('已激活')
    fetchData()
  }
  else {
    window.$message?.error(res.message || '激活失败')
  }
}

async function handleDelete(row) {
  window.$dialog?.error({
    title: '确认删除',
    content: `删除后不可恢复，确定要删除「${row.modelName}」吗？`,
    positiveText: '确定删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      const res = await flowApi.deleteModel(row.id)
      if (res.code === 200) {
        window.$message?.success('删除成功')
        fetchData()
      }
      else {
        window.$message?.error(res.message || '删除失败')
      }
    },
  })
}

function handleVersionHistory(row) {
  currentModelId.value = row.id
  currentModelVersion.value = row.version
  showVersionHistory.value = true
}

onMounted(() => {
  fetchCategories()
  fetchData()
})
</script>

<style scoped>
.flow-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
  background: #f5f7fb;
}

.page-header {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 120px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #475569;
  font-size: 17px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0;
  letter-spacing: 0;
}

.header-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex: 1;
  min-width: 0;
  flex-wrap: nowrap;
}

.search-input {
  width: 240px;
}

.category-select {
  width: 148px;
}

.status-select {
  width: 124px;
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  align-content: start;
  gap: 12px;
  padding: 4px;
}

.model-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px;
  cursor: pointer;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    background 160ms ease;
  display: flex;
  flex-direction: column;
  min-height: 180px;
  width: 100%;
  box-sizing: border-box;
}

.model-card:hover {
  border-color: #b8c2cc;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.06);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-icon {
  width: 34px;
  height: 34px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  border: 1px solid transparent;
}

.icon-bg-0 {
  color: #2563eb;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.icon-bg-1 {
  color: #0891b2;
  background: #ecfeff;
  border-color: #a5f3fc;
}

.icon-bg-2 {
  color: #15803d;
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.icon-bg-3 {
  color: #b45309;
  background: #fffbeb;
  border-color: #fde68a;
}

.icon-bg-4 {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fecaca;
}

.icon-bg-5 {
  color: #7c3aed;
  background: #f5f3ff;
  border-color: #ddd6fe;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  border: 1px solid transparent;
}

.status-tag.designing {
  background: #fffbeb;
  color: #92400e;
  border-color: #fde68a;
}

.status-tag.deployed {
  background: #ecfdf5;
  color: #047857;
  border-color: #bbf7d0;
}

.status-tag.suspended {
  background: #f8fafc;
  color: #475569;
  border-color: #e2e8f0;
}

.status-tag.disabled {
  background: #fef2f2;
  color: #b91c1c;
  border-color: #fecaca;
}

.card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 22px;
}

.card-key {
  font-size: 12px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  letter-spacing: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-desc {
  font-size: 12px;
  color: #6b7280;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 18px;
  min-height: 36px;
  margin-top: 4px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid #eef2f7;
  gap: 8px;
  min-height: 32px;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: #64748b;
  white-space: nowrap;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-actions .n-button {
  font-size: 12px;
  padding: 0 10px;
  height: 28px;
}

.empty-state {
  padding: 48px 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.pagination-wrapper {
  display: flex;
  justify-content: end;
  flex-shrink: 0;
  padding-top: 2px;
}

.notify-section {
  width: 100%;
}

.radio-group {
  margin-bottom: 8px;
}

.cursor-help {
  cursor: help;
  color: #64748b;
}

:deep(.n-spin-container),
:deep(.n-spin-content) {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.model-list-spin {
  display: flex;
}

.model-list-spin :deep(.n-spin-container) {
  flex: 1;
}

@media (max-width: 1500px) {
  .model-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1180px) {
  .model-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .header-right {
    overflow-x: auto;
    padding-bottom: 2px;
  }
}

@media (max-width: 760px) {
  .flow-page {
    padding: 12px;
  }

  .model-grid {
    grid-template-columns: 1fr;
  }

  .search-input,
  .category-select,
  .status-select {
    width: 160px;
  }

  .card-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .card-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
