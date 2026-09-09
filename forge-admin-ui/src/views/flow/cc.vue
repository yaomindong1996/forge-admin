<template>
  <div class="flow-page">
    <n-alert v-if="loadError" type="error" class="mb-3" :show-icon="true">
      抄送列表加载失败，请重试。
      <template #action>
        <NButton text type="primary" @click="loadData">
          重试
        </NButton>
      </template>
    </n-alert>
    <!-- 数据标签页 -->
    <div class="tabs-container">
      <n-tabs v-model:value="activeTab" type="line" @update:value="handleTabChange">
        <n-tab name="received">
          <div class="tab-content">
            <i class="i-material-symbols:inbox" />
            抄送给我的
            <span v-if="unreadCc > 0" class="tab-badge">{{ unreadCc > 99 ? '99+' : unreadCc }}未读</span>
          </div>
        </n-tab>
        <n-tab name="sent">
          <div class="tab-content">
            <i class="i-material-symbols:outbox" />
            我发送的
          </div>
        </n-tab>
      </n-tabs>
    </div>

    <!-- 抄送列表 -->
    <FlowTaskCardList
      v-model:selected-keys="selectedRowKeys"
      v-model:search-value="queryParams.title"
      :title="activeTab === 'received' ? '抄送给我的' : '我发送的'"
      :items="dataSource"
      :loading="loading"
      :pagination="pagination"
      :selectable="activeTab === 'received'"
      row-key="id"
      unread-key="isRead"
      search-placeholder="搜索流程名称或编号..."
      empty-text="暂无抄送记录"
      status-title="阅读状态"
      node-title="抄送内容"
      :user-title="activeTab === 'received' ? '发送人' : '抄送人'"
      @search="handleSearch"
      @refresh="loadData"
      @row-click="openCcDetail"
      @update:page="pagination.onChange"
      @update:page-size="pagination.onUpdatePageSize"
    >
      <template #filters>
        <n-select
          v-if="activeTab === 'received'"
          v-model:value="queryParams.isRead"
          placeholder="阅读状态"
          clearable
          class="read-select"
          :options="readOptions"
          @update:value="handleSearch"
        />
        <NButton secondary @click="handleReset">
          重置
        </NButton>
      </template>
      <template #batch-actions>
        <NButton v-if="activeTab === 'received' && selectedRowKeys.length > 0" size="small" secondary @click="handleBatchMarkRead">
          批量已读
        </NButton>
        <NButton v-if="activeTab === 'received'" size="small" secondary @click="handleMarkAllRead">
          全部已读
        </NButton>
      </template>
      <template #status="{ row }">
        <span class="task-status-pill" :class="row.isRead === 1 ? 'read' : 'unread'">
          {{ row.status === 1 ? getCcStatusLabel('flow_cc_status', row.status) : getLabel('flow_read_status', row.isRead) }}
        </span>
      </template>
      <template #title="{ row }">
        {{ getRowDisplayTitle(row) }}
      </template>
      <template #node="{ row }">
        {{ row.content || '暂无内容' }}
      </template>
      <template #user="{ row }">
        <span>{{ activeTab === 'received' ? (row.sendUserName || '-') : (row.ccUserName || '-') }}</span>
        <small>{{ row.ccTime || '-' }}</small>
      </template>
      <template #summary="{ row }">
        <FlowTaskBusinessSummary :row="row" />
      </template>
      <template #actions="{ row }">
        <button type="button" class="task-row-link-action" aria-label="查看抄送" @click="openCcDetail(row)">
          查看
        </button>
        <template v-if="activeTab === 'received' && row.isRead === 0">
          <span class="task-row-action-separator" />
          <button type="button" class="task-row-link-action" aria-label="标记已读" @click="handleMarkRead(row.id)">
            已读
          </button>
        </template>
        <template v-if="activeTab === 'sent' && row.status !== 1">
          <span class="task-row-action-separator" />
          <button type="button" class="task-row-link-action text-error" aria-label="撤回抄送" @click.stop="handleRevoke(row)">
            撤回
          </button>
        </template>
        <span class="task-row-action-separator" />
        <button type="button" class="task-row-link-action muted" aria-label="更多操作" @click="openCcDetail(row)">
          <i class="i-lucide:more-horizontal" />
        </button>
      </template>
    </FlowTaskCardList>

    <n-modal
      v-model:show="showDetailModal"
      preset="card"
      class="cc-detail-modal"
      closable
      :bordered="false"
      :segmented="{ content: true, footer: true }"
      content-style="padding: 0; overflow: hidden;"
    >
      <template #header>
        <div class="cc-detail-header">
          <div>
            <div class="cc-detail-title">
              {{ currentCc ? getRowDisplayTitle(currentCc) : '抄送详情' }}
            </div>
            <div class="cc-detail-meta">
              {{ activeTab === 'received' ? '抄送给我的' : '我发送的' }}
            </div>
          </div>
          <span class="read-tag" :class="currentCc?.isRead === 1 ? 'read' : 'unread'">
            {{ getLabel('flow_read_status', currentCc?.isRead) || '-' }}
          </span>
        </div>
      </template>

      <div v-if="currentCc" class="cc-detail-body">
        <div class="cc-info-grid">
          <div class="cc-info-card">
            <div class="cc-info-label">
              {{ activeTab === 'received' ? '发送人' : '抄送人' }}
            </div>
            <div class="cc-user-line">
              <UserAvatar :name="activeTab === 'received' ? (currentCc.sendUserName || '未知') : (currentCc.ccUserName || '未知')" :size="28" />
              <span>{{ activeTab === 'received' ? (currentCc.sendUserName || '-') : (currentCc.ccUserName || '-') }}</span>
            </div>
          </div>
          <div class="cc-info-card">
            <div class="cc-info-label">
              抄送时间
            </div>
            <div class="cc-info-value">
              {{ currentCc.ccTime || '-' }}
            </div>
          </div>
        </div>
        <div class="cc-content-panel">
          <div class="cc-info-label">
            内容
          </div>
          <div class="cc-content-text">
            {{ currentCc.content || '暂无内容' }}
          </div>
        </div>
        <div class="cc-business-form-panel">
          <div class="cc-business-form-head">
            <div>
              <div class="cc-business-form-title">
                业务表单
              </div>
              <div class="cc-business-form-subtitle">
                {{ ccFormInfo?.title || getRowDisplayTitle(currentCc) }}
              </div>
            </div>
          </div>

          <div v-if="ccFormLoading" class="cc-form-loading">
            <n-spin size="small" />
            <span>加载表单中...</span>
          </div>
          <FlowBusinessForm
            v-else-if="useCcExternalForm"
            :form-url="ccFormInfo.formUrl"
            :task-id="ccFormInfo.taskId"
            :business-key="ccFormInfo.businessKey"
            :process-instance-id="ccFormInfo.processInstanceId"
            :task-def-key="ccFormInfo.taskDefKey"
            :process-def-key="ccFormInfo.processDefKey"
            :variables="ccFormInfo.variables || {}"
            :read-only="true"
          />
          <n-empty v-else :description="ccFormError || '暂无业务表单'" size="small" />
        </div>
      </div>

      <template #footer>
        <n-space justify="end">
          <NButton @click="showDetailModal = false">
            关闭
          </NButton>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NButton } from 'naive-ui'
import { computed, onMounted, reactive, ref } from 'vue'
import flowApi from '@/api/flow'
import FlowBusinessForm from '@/components/common/FlowBusinessForm.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import FlowTaskBusinessSummary from '@/components/flow/FlowTaskBusinessSummary.vue'
import FlowTaskCardList from '@/components/flow/FlowTaskCardList.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { getRowDisplayTitle } from './utils/processDisplay'

const userStore = useUserStore()
const { dict, getLabel } = useDict('flow_read_status')
const { getLabel: getCcStatusLabel } = useDict('flow_cc_status')
const loading = ref(false)
const loadError = ref(false)
const dataSource = ref([])
const activeTab = ref('received')
const selectedRowKeys = ref([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  onChange: (page) => {
    pagination.page = page
    loadData()
  },
  onUpdatePageSize: (size) => {
    pagination.pageSize = size
    pagination.page = 1
    loadData()
  },
})

const queryParams = reactive({ title: '', isRead: null })

const readOptions = computed(() => toNumberOptions(dict.value.flow_read_status))

const unreadCc = ref(0)
const showDetailModal = ref(false)
const currentCc = ref(null)
const ccFormInfo = ref(null)
const ccFormLoading = ref(false)
const ccFormError = ref('')
const useCcExternalForm = computed(() => ccFormInfo.value?.formType === 'external' && ccFormInfo.value?.formUrl)

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

async function loadData() {
  loading.value = true
  loadError.value = false
  try {
    const params = { pageNum: pagination.page, pageSize: pagination.pageSize, userId: userStore.userId, title: queryParams.title || undefined }
    let res
    if (activeTab.value === 'received') {
      if (queryParams.isRead !== null)
        params.isRead = queryParams.isRead
      res = await flowApi.getMyCc(params)
    }
    else {
      res = await flowApi.getSentCc(params)
    }
    if (res.code === 200 && res.data) {
      dataSource.value = res.data.records || []
      pagination.itemCount = res.data.total || 0
      await refreshUnreadCount()
    }
    else {
      loadError.value = true
    }
  }
  catch (error) {
    console.error('加载抄送列表失败:', error)
    loadError.value = true
  }
  finally { loading.value = false }
}

async function refreshUnreadCount() {
  try {
    const res = await flowApi.getUnreadCcCount(userStore.userId)
    if (res.code === 200)
      unreadCc.value = Number(res.data?.count ?? res.data?.unreadCount ?? 0)
  }
  catch (error) {
    console.error('加载未读抄送数量失败:', error)
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  queryParams.title = ''
  queryParams.isRead = null
  pagination.page = 1
  loadData()
}

function handleTabChange() {
  pagination.page = 1
  selectedRowKeys.value = []
  loadData()
}

async function openCcDetail(row) {
  currentCc.value = row
  showDetailModal.value = true
  loadCcFormInfo(row)
  if (activeTab.value === 'received' && row.isRead === 0)
    await handleMarkRead(row.id, false)
}

async function loadCcFormInfo(row) {
  ccFormInfo.value = null
  ccFormError.value = ''
  if (!row?.id) {
    return
  }
  const ccId = row.id
  ccFormLoading.value = true
  try {
    const res = await flowApi.getCcFormInfo(ccId)
    if (currentCc.value?.id !== ccId) {
      return
    }
    if (res.code === 200) {
      ccFormInfo.value = res.data || null
    }
    else {
      ccFormError.value = res.message || '业务表单加载失败'
    }
  }
  catch (error) {
    if (currentCc.value?.id === ccId)
      ccFormError.value = error?.message || '业务表单加载失败'
  }
  finally {
    if (currentCc.value?.id === ccId)
      ccFormLoading.value = false
  }
}

async function handleMarkRead(id, showToast = true) {
  try {
    const res = await flowApi.markCcRead(id)
    if (res.code === 200) {
      if (showToast)
        window.$message.success('已标记已读')
      if (currentCc.value?.id === id)
        currentCc.value.isRead = 1
      loadData()
    }
  }
  catch {
    window.$message.error('操作失败')
  }
}

async function handleBatchMarkRead() {
  if (selectedRowKeys.value.length === 0)
    return
  try {
    const res = await flowApi.batchMarkCcRead(selectedRowKeys.value)
    if (res.code === 200) {
      window.$message.success('已批量标记已读')
      selectedRowKeys.value = []
      loadData()
    }
    else {
      window.$message.error(res.message || '操作失败')
    }
  }
  catch (error) {
    console.error('批量标记抄送已读失败:', error)
    window.$message.error('操作失败')
  }
}

function handleRevoke(row) {
  window.$dialog.warning({
    title: '撤回抄送',
    content: '撤回后接收人将无法继续查看该抄送，是否继续？',
    positiveText: '撤回',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await flowApi.revokeCc(row.id, '')
        if (res.code === 200) {
          window.$message.success('抄送已撤回')
          loadData()
        }
        else {
          window.$message.error(res.message || '撤回失败')
        }
      }
      catch (error) {
        console.error('撤回抄送失败:', error)
        window.$message.error('撤回失败')
      }
    },
  })
}

function handleMarkAllRead() {
  window.$dialog.warning({
    title: '确认',
    content: '确定将所有未读抄送标记为已读吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await flowApi.markAllCcRead()
        if (res.code === 200) {
          window.$message.success(`已全部标记已读${res.data ? `（${res.data}条）` : ''}`)
          await loadData()
        }
        else {
          window.$message.error(res.message || '操作失败')
        }
      }
      catch (error) {
        console.error('全部标记抄送已读失败:', error)
        window.$message.error('操作失败')
      }
    },
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.flow-page {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
  background: var(--bg-secondary);
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
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
}
.title-icon.cc {
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
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
.read-select {
  width: 116px;
}
.tabs-container {
  flex: 0 0 auto;
  background: var(--bg-primary);
  padding: 0 12px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
}
.tab-content {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-weight: 500;
}
.tab-badge {
  border: 1px solid #fecdd3;
  background: #fff1f2;
  color: #be123c;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 10px;
  font-weight: 600;
}
.table-container {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  flex: 1;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}
:deep(.cc-title) {
  font-weight: 500;
  color: #0f172a;
  cursor: pointer;
}
:deep(.cc-title:hover) {
  color: #0369a1;
}
:deep(.table-user) {
  display: flex;
  align-items: center;
  gap: 8px;
}
:deep(.user-name-text) {
  font-weight: 500;
  color: #0f172a;
}
:deep(.read-tag) {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}
:deep(.read-tag.read) {
  background: #dcfce7;
  color: #15803d;
}
:deep(.read-tag.unread) {
  background: #fee2e2;
  color: #b91c1c;
}

:deep(.cc-actions) {
  display: flex;
  gap: 6px;
  align-items: center;
}

.cc-detail-modal {
  width: min(820px, calc(100vw - 32px));
}

.cc-detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.cc-detail-title {
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.cc-detail-meta {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.cc-detail-body {
  display: grid;
  gap: 14px;
  max-height: calc(100vh - 178px);
  overflow-y: auto;
  padding: 18px 20px 20px;
}

.cc-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.cc-info-card,
.cc-content-panel {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  padding: 12px 14px;
}

.cc-info-label {
  margin-bottom: 8px;
  color: #64748b;
  font-size: 12px;
}

.cc-info-value,
.cc-user-line {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
}

.cc-user-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cc-content-text {
  min-height: 120px;
  color: #172033;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.cc-business-form-panel {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.cc-business-form-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #edf2f7;
}

.cc-business-form-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.cc-business-form-subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.cc-form-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 120px;
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 760px) {
  .cc-detail-modal {
    width: 100vw;
    height: 100vh;
    margin: 0;
  }

  .cc-detail-header,
  .header-right {
    align-items: stretch;
    flex-direction: column;
  }

  .cc-detail-body {
    max-height: calc(100vh - 126px);
    padding: 14px;
  }

  .cc-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
