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
        <n-select
          v-model:value="queryParams.category"
          placeholder="流程分类"
          clearable
          class="category-select"
          :options="categoryOptions"
        />
        <n-select
          v-model:value="queryParams.status"
          placeholder="全部状态"
          clearable
          class="status-select"
          :options="statusOptions"
        />
        <NButton type="primary" class="search-btn" @click="handleSearch">
          <i class="i-material-symbols:search mr-2" />
          搜索
        </NButton>
        <NButton class="reset-btn" @click="handleReset">
          <i class="i-material-symbols:refresh mr-2" />
          重置
        </NButton>
        <NButton type="primary" class="add-btn" @click="handleAdd">
          <i class="i-material-symbols:add mr-2" />
          新增模型
        </NButton>
      </div>
    </div>

    <!-- 卡片网格 -->
    <n-spin :show="loading">
      <div v-if="dataSource.length > 0" class="model-grid">
        <div
          v-for="item in dataSource"
          :key="item.id"
          class="model-card"
          @click="handleDesign(item)"
        >
          <!-- 卡片头 -->
          <div class="card-header">
            <div class="card-icon" :class="iconClass(item)">
              <i :class="iconName(item)" />
            </div>
            <span class="status-tag" :class="statusClass(item.status)">
              {{ statusText[item.status] ?? '未知' }}
            </span>
          </div>

          <!-- 卡片体 -->
          <div class="card-body">
            <div class="card-title" :title="item.modelName">
              {{ item.modelName }}
            </div>
            <div class="card-key">
              {{ item.modelKey }}
            </div>
            <div class="card-desc" :title="item.description">
              {{ item.description || '暂无描述' }}
            </div>
          </div>

          <!-- 卡片脚 -->
          <div class="card-footer">
            <div class="card-meta">
              <span class="meta-item">
                <i class="i-material-symbols:category-outline" />
                {{ item.category || '未分类' }}
              </span>
              <span class="meta-item">
                <i class="i-material-symbols:layers-outline" />
                v{{ item.version || 1 }}
              </span>
              <span v-if="item.deployTime" class="meta-item">
                <i class="i-material-symbols:schedule-outline" />
                {{ formatDate(item.deployTime) }}
              </span>
            </div>
            <div class="card-actions" @click.stop>
              <NButton size="small" type="primary" @click.stop="handleDesign(item)">
                <i class="i-material-symbols:edit-outline mr-1" />
                设计
              </NButton>
              <NButton
                v-if="item.status === 0"
                size="small"
                type="success"
                @click.stop="handleDeploy(item)"
              >
                <i class="i-material-symbols:rocket-launch mr-1" />
                部署
              </NButton>
              <NButton
                v-if="item.status === 1"
                size="small"
                @click.stop="handleViewInstances(item)"
              >
                <i class="i-material-symbols:visibility-outline mr-1" />
                实例
              </NButton>
              <n-dropdown
                trigger="click"
                :options="getActionOptions(item)"
                @select="(key) => handleActionSelect(key, item)"
              >
                <NButton size="small" quaternary circle @click.stop>
                  <template #icon>
                    <i class="i-material-symbols:more-horiz" />
                  </template>
                </NButton>
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
          <NButton type="primary" @click="handleAdd">
            <i class="i-material-symbols:add mr-2" />
            新增模型
          </NButton>
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
              <n-select
                v-model:value="formData.category"
                placeholder="请选择分类"
                :options="categoryOptions"
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
  </div>
</template>

<script setup>
import { h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import flowApi from '@/api/flow'
import FlowModelStats from '@/components/flow/FlowModelStats.vue'

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
  const opts = [
    { label: '编辑信息', key: 'edit', icon: () => h('i', { class: 'i-material-symbols:edit-outline' }) },
    { label: '复制模型', key: 'copy', icon: () => h('i', { class: 'i-material-symbols:content-copy-outline' }) },
  ]
  if (row.status === 1) {
    opts.push({ label: '挂起', key: 'suspend', icon: () => h('i', { class: 'i-material-symbols:pause-circle-outline' }) })
  }
  if (row.status === 2) {
    opts.push({ label: '激活', key: 'activate', icon: () => h('i', { class: 'i-material-symbols:play-circle-outline' }) })
  }
  opts.push({ type: 'divider', key: 'd1' })
  opts.push({ label: '删除', key: 'delete', props: { style: 'color: #d03050' } })
  return opts
}

function handleActionSelect(key, row) {
  const map = { edit: handleEdit, copy: handleCopy, suspend: handleSuspend, activate: handleActivate, delete: handleDelete }
  map[key]?.(row)
}

const queryParams = reactive({ modelName: '', category: null, status: null })
const dataSource = ref([])
const loading = ref(false)
const pagination = reactive({ page: 1, pageSize: 12, itemCount: 0 })

const totalCount = ref(0)
const designingCount = ref(0)
const deployedCount = ref(0)
const suspendedCount = ref(0)
const disabledCount = ref(0)

async function fetchCategories() {
  try {
    const res = await flowApi.getEnabledCategories()
    if (res.code === 200) {
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

onMounted(() => {
  fetchCategories()
  fetchData()
})
</script>

<style scoped>
.flow-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.page-header {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 240px;
}

.category-select {
  width: 150px;
}

.status-select {
  width: 130px;
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

@media (max-width: 1400px) {
  .model-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1000px) {
  .model-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.model-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.model-card:hover {
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.1);
  border-color: #cbd5e1;
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.card-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
  flex-shrink: 0;
}

.icon-bg-0 {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
}

.icon-bg-1 {
  background: linear-gradient(135deg, #22d3ee 0%, #0891b2 100%);
}

.icon-bg-2 {
  background: linear-gradient(135deg, #34d399 0%, #059669 100%);
}

.icon-bg-3 {
  background: linear-gradient(135deg, #fbbf24 0%, #d97706 100%);
}

.icon-bg-4 {
  background: linear-gradient(135deg, #f87171 0%, #dc2626 100%);
}

.icon-bg-5 {
  background: linear-gradient(135deg, #a78bfa 0%, #7c3aed 100%);
}

.status-tag {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}

.status-tag.designing {
  background: #fef3c7;
  color: #b45309;
}

.status-tag.deployed {
  background: #dcfce7;
  color: #15803d;
}

.status-tag.suspended {
  background: #f1f5f9;
  color: #64748b;
}

.status-tag.disabled {
  background: #fee2e2;
  color: #b91c1c;
}

.card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-key {
  font-size: 12px;
  color: #64748b;
  font-family: monospace;
  letter-spacing: 0.3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-desc {
  font-size: 12px;
  color: #94a3b8;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.5;
  margin-top: 2px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
  gap: 8px;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1;
  min-width: 0;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.empty-state {
  padding: 64px 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: end;
  margin-top: 16px;
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
</style>
