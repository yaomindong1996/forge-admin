<template>
  <div class="p-16">
    <!-- 顶部操作栏 -->
    <div class="mb-16 flex items-center justify-between">
      <div class="flex items-center gap-12">
        <n-input
          v-model:value="queryParams.modelName"
          placeholder="搜索模型名称或 Key"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <i class="i-material-symbols:search text-gray-400" />
          </template>
        </n-input>
        <n-select
          v-model:value="queryParams.category"
          placeholder="流程分类"
          clearable
          style="width: 150px"
          :options="categoryOptions"
        />
        <n-select
          v-model:value="queryParams.status"
          placeholder="全部状态"
          clearable
          style="width: 130px"
          :options="statusOptions"
        />
        <n-button type="primary" ghost @click="handleSearch">
          搜索
        </n-button>
        <n-button @click="handleReset">
          重置
        </n-button>
      </div>
      <n-button type="primary" @click="handleAdd">
        <template #icon>
          <i class="i-material-symbols:add" />
        </template>
        新增模型
      </n-button>
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
          <!-- 卡片头：图标 + 状态 -->
          <div class="card-header">
            <div class="card-icon" :class="iconClass(item)">
              <i :class="iconName(item)" class="text-24" />
            </div>
            <n-tag :type="statusTagType[item.status] ?? 'default'" size="small" round>
              {{ statusText[item.status] ?? '未知' }}
            </n-tag>
          </div>

          <!-- 卡片体：名称 + key + 描述 -->
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

          <!-- 卡片脚：元信息 + 操作 -->
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
              <n-button size="small" type="primary" @click.stop="handleDesign(item)">
                设计
              </n-button>
              <n-button
                v-if="item.status === 0"
                size="small"
                type="success"
                @click.stop="handleDeploy(item)"
              >
                部署
              </n-button>
              <n-button
                v-if="item.status === 1"
                size="small"
                @click.stop="handleViewInstances(item)"
              >
                实例
              </n-button>
              <n-dropdown
                trigger="click"
                :options="getActionOptions(item)"
                @select="(key) => handleActionSelect(key, item)"
              >
                <n-button size="small" quaternary circle @click.stop>
                  <template #icon>
                    <i class="i-material-symbols:more-horiz" />
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
        class="py-64"
      >
        <template #extra>
          <n-button type="primary" @click="handleAdd">
            新增模型
          </n-button>
        </template>
      </n-empty>
    </n-spin>

    <!-- 分页 -->
    <div v-if="pagination.itemCount > 0" class="mt-16 flex justify-end">
      <n-pagination
        v-model:page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :item-count="pagination.itemCount"
        :page-sizes="[12, 24, 48]"
        show-size-picker
        @update:page="fetchData"
        @update:page-size="(v) => { pagination.pageSize = v; pagination.page = 1; fetchData() }"
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
              <div class="w-full">
                <n-radio-group v-model:value="formData.notifyType" class="mb-8">
                  <n-space>
                    <n-radio value="none">
                      不通知
                    </n-radio>
                    <n-radio value="redis">
                      Redis Pub/Sub
                      <n-tooltip trigger="hover" placement="top">
                        <template #trigger>
                          <i class="i-material-symbols:info-outline ml-2 cursor-help text-gray-400" />
                        </template>
                        流程完成/驳回/取消时，发布消息到 Redis 频道<br>
                        频道名：flow:event:{modelKey} 及 flow:event:all<br>
                        业务侧通过 SUBSCRIBE/PSUBSCRIBE 消费
                      </n-tooltip>
                    </n-radio>
                    <n-radio value="webhook">
                      HTTP Webhook
                      <n-tooltip trigger="hover" placement="top">
                        <template #trigger>
                          <i class="i-material-symbols:info-outline ml-2 cursor-help text-gray-400" />
                        </template>
                        流程完成/驳回/取消时，POST 请求回调 Webhook URL<br>
                        携带请求头：X-Flow-Event-Type / X-Flow-Process-Key / X-Flow-Business-Key
                      </n-tooltip>
                    </n-radio>
                  </n-space>
                </n-radio-group>
                <!-- webhook URL 仅在选择 webhook 时显示 -->
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
import { onMounted, reactive, ref } from 'vue'
// h 函数（给 dropdown icon 用）
import { h } from 'vue'
import { useRouter } from 'vue-router'

import flowApi from '@/api/flow'

const router = useRouter()

// ==================== 选项 ====================
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

const statusTagType = { 0: 'warning', 1: 'success', 2: 'default', 3: 'error' }
const statusText = { 0: '设计中', 1: '已部署', 2: '已挂起', 3: '已禁用' }

// 卡片图标颜色映射（按分类/类型给予不同视觉风格）
const iconColorMap = ['#4f46e5', '#0891b2', '#059669', '#d97706', '#dc2626', '#7c3aed']
function iconClass(item) {
  const idx = Math.abs(hashStr(item.modelKey || '')) % iconColorMap.length
  return `icon-bg-${idx}`
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
  for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0
  return h
}
function formatDate(d) {
  if (!d)
    return ''
  return d.slice(0, 10)
}

// ==================== 操作下拉 ====================
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

// ==================== 查询/数据 ====================
const queryParams = reactive({ modelName: '', category: null, status: null })
const dataSource = ref([])
const loading = ref(false)
const pagination = reactive({ page: 1, pageSize: 12, itemCount: 0 })

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
  catch (e) { console.error(e) }
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
    }
  }
  catch (e) { console.error(e) }
  finally { loading.value = false }
}
function handleSearch() { pagination.page = 1; fetchData() }
function handleReset() {
  Object.assign(queryParams, { modelName: '', category: null, status: null })
  pagination.page = 1
  fetchData()
}

// ==================== 表单弹窗 ====================
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
  catch (e) { console.error(e) }
  finally { submitLoading.value = false }
}

// ==================== 业务操作 ====================
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
      if (res.code === 200) { window.$message?.success('部署成功'); fetchData() }
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
      if (res.code === 200) { window.$message?.success('复制成功'); fetchData() }
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
      if (res.code === 200) { window.$message?.success('已挂起'); fetchData() }
      else {
        window.$message?.error(res.message || '挂起失败')
      }
    },
  })
}
async function handleActivate(row) {
  const res = await flowApi.activateModel(row.id)
  if (res.code === 200) { window.$message?.success('已激活'); fetchData() }
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
      if (res.code === 200) { window.$message?.success('删除成功'); fetchData() }
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
/* 卡片网格：固定列宽，多列排列 */
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

/* 单张卡片：竖向堆叠 */
.model-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition:
    box-shadow 0.2s,
    border-color 0.2s,
    transform 0.15s;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.model-card:hover {
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.1);
  border-color: #d0d0d0;
  transform: translateY(-2px);
}

/* 头部：图标左 + 状态右 */
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
  color: #fff;
  flex-shrink: 0;
}
.icon-bg-0 {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
}
.icon-bg-1 {
  background: linear-gradient(135deg, #22d3ee, #0891b2);
}
.icon-bg-2 {
  background: linear-gradient(135deg, #34d399, #059669);
}
.icon-bg-3 {
  background: linear-gradient(135deg, #fbbf24, #d97706);
}
.icon-bg-4 {
  background: linear-gradient(135deg, #f87171, #dc2626);
}
.icon-bg-5 {
  background: linear-gradient(135deg, #a78bfa, #7c3aed);
}

/* 主体：名称 + key + 描述 */
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
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-key {
  font-size: 12px;
  color: #9ca3af;
  font-family: monospace;
  letter-spacing: 0.3px;
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
  line-height: 1.5;
  margin-top: 2px;
}

/* 底部：元信息 + 操作按钮 */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid #f3f4f6;
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
  color: #9ca3af;
  white-space: nowrap;
}
.card-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
</style>
