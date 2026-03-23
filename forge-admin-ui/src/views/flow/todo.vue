<template>
  <div class="p-16">
    <div class="bg-white rounded p-16">
      <h2 class="text-18 font-bold mb-16">我的待办</h2>
      
      <!-- 搜索栏 -->
      <n-space class="mb-16" :vertical="false">
        <n-input
          v-model:value="queryParams.title"
          placeholder="搜索任务标题"
          clearable
          style="width: 200px"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
        <n-select
          v-model:value="queryParams.category"
          placeholder="流程分类"
          clearable
          style="width: 150px"
          :options="categoryOptions"
        />
        <n-button type="primary" @click="handleSearch">
          <template #icon>
            <i class="i-material-symbols:search" />
          </template>
          搜索
        </n-button>
        <n-button @click="handleReset">
          <template #icon>
            <i class="i-material-symbols:refresh" />
          </template>
          重置
        </n-button>
      </n-space>

      <!-- 任务列表 -->
      <n-data-table
        :columns="columns"
        :data="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="row => row.id"
      />
    </div>

    <!-- 审批弹窗 -->
    <Teleport to="body">
      <n-modal
        v-model:show="showApproveModal"
        preset="dialog"
        title="审批任务"
        positive-text="提交"
        negative-text="取消"
        :loading="approveLoading"
        @positive-click="handleApprove"
      >
        <n-form ref="formRef" :model="approveForm" label-placement="left" label-width="80">
          <n-form-item label="任务名称">
            <n-input :value="currentTask?.taskName" disabled />
          </n-form-item>
          <n-form-item label="审批结果">
            <n-radio-group v-model:value="approveForm.result">
              <n-space>
                <n-radio value="approve">通过</n-radio>
                <n-radio value="reject">驳回</n-radio>
              </n-space>
            </n-radio-group>
          </n-form-item>
          <n-form-item label="审批意见">
            <n-input
              v-model:value="approveForm.comment"
              type="textarea"
              placeholder="请输入审批意见"
              :rows="3"
            />
          </n-form-item>
        </n-form>
      </n-modal>
    </Teleport>

    <!-- 流程图弹窗 -->
    <Teleport to="body">
      <n-modal v-model:show="showDiagramModal" preset="card" title="流程图" style="width: 90%; max-width: 1200px;">
        <ProcessDiagramViewer
          v-if="showDiagramModal && currentDiagramInstanceId"
          :process-instance-id="currentDiagramInstanceId"
        />
      </n-modal>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, h, onMounted } from 'vue'
import { NTag, NButton, NSpace, NBadge } from 'naive-ui'
import flowApi from '@/api/flow'
import { useUserStore } from '@/store'
import ProcessDiagramViewer from '@/components/bpmn/ProcessDiagramViewer.vue'

const userStore = useUserStore()
const loading = ref(false)
const dataSource = ref([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  onChange: (page) => {
    pagination.page = page
    loadData()
  }
})

const queryParams = reactive({
  title: '',
  category: ''
})

const categoryOptions = ref([])

// 审批相关
const showApproveModal = ref(false)
const approveLoading = ref(false)
const currentTask = ref(null)
const approveForm = reactive({
  result: 'approve',
  comment: ''
})

// 流程图相关
const showDiagramModal = ref(false)
const currentDiagramInstanceId = ref('')

// 状态映射
const statusMap = {
  0: { text: '待办', type: 'warning' },
  1: { text: '已签收', type: 'info' }
}

// 表格列
const columns = [
  {
    title: '任务标题',
    key: 'title',
    ellipsis: { tooltip: true }
  },
  {
    title: '任务名称',
    key: 'taskName',
    width: 120
  },
  {
    title: '发起人',
    key: 'startUserName',
    width: 100
  },
  {
    title: '发起部门',
    key: 'startDeptName',
    width: 120
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) => {
      const status = statusMap[row.status] || { text: '未知', type: 'default' }
      return h(NTag, { type: status.type, size: 'small' }, () => status.text)
    }
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 160
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    render: (row) => {
      return h(NSpace, {}, () => [
        h(NButton, {
          size: 'small',
          type: 'primary',
          onClick: () => handleOpenApprove(row)
        }, () => '审批'),
        h(NButton, {
          size: 'small',
          onClick: () => handleViewDiagram(row)
        }, () => '流程图'),
        row.status === 0 && !row.assignee ? h(NButton, {
          size: 'small',
          type: 'info',
          onClick: () => handleClaim(row)
        }, () => '签收') : null
      ])
    }
  }
]

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await flowApi.getTodoTasks({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      userId: userStore.userId,
      title: queryParams.title || undefined,
      category: queryParams.category || undefined
    })
    if (res.code === 200 && res.data) {
      dataSource.value = res.data.records || []
      pagination.itemCount = res.data.total || 0
    }
  } catch (error) {
    console.error('加载待办任务失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载分类
async function loadCategories() {
  try {
    const res = await flowApi.getEnabledCategories()
    if (res.code === 200 && res.data) {
      categoryOptions.value = res.data.map(item => ({
        label: item.categoryName,
        value: item.categoryCode
      }))
    }
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  loadData()
}

// 重置
function handleReset() {
  queryParams.title = ''
  queryParams.category = ''
  pagination.page = 1
  loadData()
}

// 打开审批弹窗
function handleOpenApprove(row) {
  currentTask.value = row
  approveForm.result = 'approve'
  approveForm.comment = ''
  showApproveModal.value = true
}

// 提交审批
async function handleApprove() {
  if (!approveForm.comment) {
    window.$message.warning('请输入审批意见')
    return false
  }
  
  approveLoading.value = true
  try {
    const api = approveForm.result === 'approve' ? flowApi.approveTask : flowApi.rejectTask
    const res = await api({
      taskId: currentTask.value.taskId,
      userId: userStore.userId,
      comment: approveForm.comment
    })
    if (res.code === 200) {
      window.$message.success(approveForm.result === 'approve' ? '审批通过' : '已驳回')
      showApproveModal.value = false
      loadData()
    } else {
      window.$message.error(res.message || '操作失败')
    }
  } catch (error) {
    window.$message.error('操作失败')
  } finally {
    approveLoading.value = false
  }
  return true
}

// 查看流程图
function handleViewDiagram(row) {
  if (!row.processInstanceId) {
    window.$message.warning('无法获取流程实例')
    return
  }
  currentDiagramInstanceId.value = row.processInstanceId
  showDiagramModal.value = true
}

// 签收任务
async function handleClaim(row) {
  try {
    const res = await flowApi.claimTask(row.taskId, userStore.userId)
    if (res.code === 200) {
      window.$message.success('签收成功')
      loadData()
    } else {
      window.$message.error(res.message || '签收失败')
    }
  } catch (error) {
    window.$message.error('签收失败')
  }
}

onMounted(() => {
  loadCategories()
  loadData()
})
</script>