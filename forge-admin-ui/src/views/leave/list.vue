<template>
  <div class="leave-list p-4">
    <n-card title="我的请假">
      <!-- 搜索栏 -->
      <NSpace class="mb-4">
        <n-select
          v-model:value="searchStatus"
          :options="statusOptions"
          placeholder="选择状态"
          clearable
          style="width: 150px"
          @update:value="handleSearch"
        />
        <NButton type="primary" @click="router.push('/leave/apply')">
          <template #icon>
            <n-icon><i-mdi-plus /></n-icon>
          </template>
          新建申请
        </NButton>
      </NSpace>

      <!-- 数据表格 -->
      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="row => row.id"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </n-card>

    <!-- 详情弹窗 -->
    <n-modal v-model:show="showDetailModal" preset="card" title="请假详情" style="width: 600px">
      <n-descriptions :column="1" label-placement="left" bordered>
        <n-descriptions-item label="请假类型">
          {{ getLeaveTypeName(detailData?.leaveType) }}
        </n-descriptions-item>
        <n-descriptions-item label="开始时间">
          {{ formatDateTime(detailData?.startTime) }}
        </n-descriptions-item>
        <n-descriptions-item label="结束时间">
          {{ formatDateTime(detailData?.endTime) }}
        </n-descriptions-item>
        <n-descriptions-item label="请假天数">
          {{ detailData?.duration }} 天
        </n-descriptions-item>
        <n-descriptions-item label="请假原因">
          {{ detailData?.reason }}
        </n-descriptions-item>
        <n-descriptions-item label="状态">
          <NTag :type="getStatusType(detailData?.status)">
            {{ getStatusName(detailData?.status) }}
          </NTag>
        </n-descriptions-item>
        <n-descriptions-item label="申请时间">
          {{ formatDateTime(detailData?.createTime) }}
        </n-descriptions-item>
        <n-descriptions-item v-if="detailData?.approveUserName" label="审批人">
          {{ detailData?.approveUserName }}
        </n-descriptions-item>
        <n-descriptions-item v-if="detailData?.approveComment" label="审批意见">
          {{ detailData?.approveComment }}
        </n-descriptions-item>
        <n-descriptions-item v-if="detailData?.approveTime" label="审批时间">
          {{ formatDateTime(detailData?.approveTime) }}
        </n-descriptions-item>
      </n-descriptions>

      <template #footer>
        <NSpace justify="end">
          <NButton
            v-if="detailData?.status === 'pending'"
            type="warning"
            @click="handleCancel"
          >
            撤销申请
          </NButton>
          <NButton @click="showDetailModal = false">
            关闭
          </NButton>
        </NSpace>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NButton, NSpace, NTag } from 'naive-ui'
import { h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import leaveApi from '@/api/leave'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const searchStatus = ref(null)
const showDetailModal = ref(false)
const detailData = ref(null)

// 分页配置
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
})

// 状态选项
const statusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '审批中', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
  { label: '已取消', value: 'canceled' },
]

// 表格列定义
const columns = [
  {
    title: '请假类型',
    key: 'leaveType',
    render: row => getLeaveTypeName(row.leaveType),
  },
  {
    title: '开始时间',
    key: 'startTime',
    render: row => formatDateTime(row.startTime),
  },
  {
    title: '结束时间',
    key: 'endTime',
    render: row => formatDateTime(row.endTime),
  },
  {
    title: '请假天数',
    key: 'duration',
    render: row => `${row.duration} 天`,
  },
  {
    title: '状态',
    key: 'status',
    render: row => h(NTag, { type: getStatusType(row.status) }, () => getStatusName(row.status)),
  },
  {
    title: '申请时间',
    key: 'createTime',
    render: row => formatDateTime(row.createTime),
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    render: (row) => {
      const buttons = []

      // 查看详情
      buttons.push(
        h(NButton, {
          size: 'small',
          onClick: () => handleDetail(row),
        }, () => '详情'),
      )

      // 草稿状态可以编辑和删除
      if (row.status === 'draft') {
        buttons.push(
          h(NButton, {
            size: 'small',
            type: 'primary',
            onClick: () => handleEdit(row),
          }, () => '编辑'),
        )
        buttons.push(
          h(NButton, {
            size: 'small',
            type: 'error',
            onClick: () => handleDelete(row),
          }, () => '删除'),
        )
      }

      // 审批中可以撤销
      if (row.status === 'pending') {
        buttons.push(
          h(NButton, {
            size: 'small',
            type: 'warning',
            onClick: () => handleCancel(row),
          }, () => '撤销'),
        )
      }

      return h(NSpace, null, () => buttons)
    },
  },
]

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await leaveApi.getPage({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      status: searchStatus.value,
    })

    tableData.value = res.data?.records || []
    pagination.itemCount = res.data?.total || 0
  }
  catch (error) {
    console.error('加载数据失败:', error)
  }
  finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  loadData()
}

// 分页变化
function handlePageChange(page) {
  pagination.page = page
  loadData()
}

function handlePageSizeChange(pageSize) {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadData()
}

// 查看详情
async function handleDetail(row) {
  try {
    const res = await leaveApi.getDetail(row.businessKey)
    detailData.value = res.data
    showDetailModal.value = true
  }
  catch (error) {
    console.error('获取详情失败:', error)
  }
}

// 编辑草稿
function handleEdit(row) {
  router.push({
    path: '/leave/apply',
    query: { businessKey: row.businessKey },
  })
}

// 删除草稿
async function handleDelete(row) {
  try {
    await window.$dialog?.warning({
      title: '确认删除',
      content: '确定要删除这条草稿吗？',
      positiveText: '确定',
      negativeText: '取消',
    })

    await leaveApi.delete(row.businessKey)
    window.$message?.success('删除成功')
    loadData()
  }
  catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 撤销申请
async function handleCancel(row) {
  try {
    await window.$dialog?.warning({
      title: '确认撤销',
      content: '确定要撤销这条申请吗？',
      positiveText: '确定',
      negativeText: '取消',
    })

    const businessKey = row?.businessKey || detailData.value?.businessKey
    await leaveApi.cancel(businessKey)
    window.$message?.success('撤销成功')
    showDetailModal.value = false
    loadData()
  }
  catch (error) {
    if (error !== 'cancel') {
      console.error('撤销失败:', error)
    }
  }
}

// 获取请假类型名称
function getLeaveTypeName(type) {
  const map = {
    annual: '年假',
    sick: '病假',
    personal: '事假',
    marriage: '婚假',
    maternity: '产假',
  }
  return map[type] || type
}

// 获取状态名称
function getStatusName(status) {
  const map = {
    draft: '草稿',
    pending: '审批中',
    approved: '已通过',
    rejected: '已驳回',
    canceled: '已取消',
  }
  return map[status] || status
}

// 获取状态标签类型
function getStatusType(status) {
  const map = {
    draft: 'default',
    pending: 'warning',
    approved: 'success',
    rejected: 'error',
    canceled: 'info',
  }
  return map[status] || 'default'
}

// 格式化日期时间
function formatDateTime(dateStr) {
  if (!dateStr)
    return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 初始化
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.leave-list {
  /* 样式 */
}
</style>
