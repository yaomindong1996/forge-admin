<template>
  <div class="job-log-list">
    <!-- 搜索栏 -->
    <div class="search-bar mb-16">
      <n-space>
        <n-select
          v-model:value="searchStatus"
          style="width: 150px"
          placeholder="执行状态"
          clearable
          :options="statusOptions"
          @update:value="loadLogList"
        />
        <n-date-picker
          v-model:value="dateRange"
          type="datetimerange"
          clearable
          placeholder="选择时间范围"
          @update:value="loadLogList"
        />
        <n-button type="primary" @click="loadLogList">
          <template #icon>
            <i class="i-material-symbols:search" />
          </template>
          查询
        </n-button>
      </n-space>
    </div>

    <n-data-table
      :columns="columns"
      :data="logList"
      :loading="loading"
      :pagination="pagination"
      :row-key="row => row.id"
      size="small"
      :max-height="500"
      :scroll-x="1400"
      striped
    />
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import { NTag, NEllipsis, NButton } from 'naive-ui'
import { request } from '@/utils'

const props = defineProps({
  jobName: {
    type: String,
    default: null
  }
})

const loading = ref(false)
const logList = ref([])
const searchStatus = ref(null)
const dateRange = ref(null)

const statusOptions = [
  { label: '全部', value: null },
  { label: '执行成功', value: 1 },
  { label: '执行失败', value: 0 }
]

const pagination = ref({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100],
  prefix: (info) => `共 ${info.itemCount} 条`,
  onChange: (page) => {
    pagination.value.page = page
    loadLogList()
  },
  onUpdatePageSize: (pageSize) => {
    pagination.value.pageSize = pageSize
    pagination.value.page = 1
    loadLogList()
  }
})

// 表格列配置
const columns = [
  {
    title: '任务名称',
    key: 'jobName',
    width: 150,
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '任务分组',
    key: 'jobGroup',
    width: 120
  },
  {
    title: 'Handler',
    key: 'executorHandler',
    width: 150,
    ellipsis: {
      tooltip: true
    },
    render: (row) => row.executorHandler || '-'
  },
  {
    title: '执行状态',
    key: 'status',
    width: 100,
    render: (row) => {
      const statusMap = {
        0: { text: '执行失败', type: 'error' },
        1: { text: '执行成功', type: 'success' }
      }
      const config = statusMap[row.status] || { text: '未知', type: 'default' }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    }
  },
  {
    title: '执行时长',
    key: 'duration',
    width: 100,
    render: (row) => {
      return row.duration != null ? `${row.duration}ms` : '-'
    }
  },
  {
    title: '重试次数',
    key: 'retryCount',
    width: 90,
    render: (row) => {
      return row.retryCount || 0
    }
  },
  {
    title: '触发时间',
    key: 'triggerTime',
    width: 160,
    render: (row) => formatDateTime(row.triggerTime)
  },
  {
    title: '开始时间',
    key: 'startTime',
    width: 160,
    render: (row) => formatDateTime(row.startTime)
  },
  {
    title: '结束时间',
    key: 'endTime',
    width: 160,
    render: (row) => formatDateTime(row.endTime)
  },
  {
    title: '异常信息',
    key: 'exceptionMsg',
    width: 250,
    render: (row) => {
      if (!row.exceptionMsg) return '-'
      return h(NEllipsis, 
        { 
          lineClamp: 2,
          tooltip: {
            width: 600,
            style: { maxHeight: '400px', overflow: 'auto' }
          }
        },
        { default: () => row.exceptionMsg }
      )
    }
  },
  {
    title: '操作',
    key: 'action',
    width: 80,
    fixed: 'right',
    render: (row) => {
      return h(
        NButton,
        {
          text: true,
          type: 'primary',
          size: 'small',
          onClick: () => handleViewDetail(row)
        },
        { default: () => '详情' }
      )
    }
  }
]

// 格式化日期时间
function formatDateTime(dateTime) {
  if (!dateTime) return '-'
  try {
    // 如果是字符串，直接返回
    if (typeof dateTime === 'string') {
      return dateTime
    }
    // 如果是Date对象，格式化
    if (dateTime instanceof Date) {
      return dateTime.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    }
    return dateTime
  } catch {
    return dateTime
  }
}

// 加载日志列表
async function loadLogList() {
  try {
    loading.value = true
    const params = {
      pageNum: pagination.value.page,
      pageSize: pagination.value.pageSize
    }
    
    if (props.jobName) {
      params.jobName = props.jobName
    }
    
    if (searchStatus.value !== null && searchStatus.value !== undefined) {
      params.status = searchStatus.value
    }
    
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = new Date(dateRange.value[0]).toISOString()
      params.endTime = new Date(dateRange.value[1]).toISOString()
    }
    
    const res = await request.get('/job/log/page', { params })
    
    if (res.code === 200) {
      logList.value = res.data.records || res.data.list || []
      pagination.value.itemCount = res.data.total || 0
    }
  } catch (error) {
    console.error('加载日志失败:', error)
    window.$message.error('加载日志失败')
  } finally {
    loading.value = false
  }
}

// 查看详情
function handleViewDetail(row) {
  window.$dialog.info({
    title: '任务执行详情',
    content: () => {
      return h('div', { class: 'log-detail' }, [
        h('div', { class: 'detail-section' }, [
          h('h4', { class: 'section-title' }, '基本信息'),
          h('div', { class: 'detail-row' }, [
            h('span', { class: 'detail-label' }, '任务名称：'),
            h('span', { class: 'detail-value' }, row.jobName)
          ]),
          h('div', { class: 'detail-row' }, [
            h('span', { class: 'detail-label' }, '任务分组：'),
            h('span', { class: 'detail-value' }, row.jobGroup)
          ]),
          h('div', { class: 'detail-row' }, [
            h('span', { class: 'detail-label' }, 'Handler：'),
            h('span', { class: 'detail-value' }, row.executorHandler || '-')
          ]),
          h('div', { class: 'detail-row' }, [
            h('span', { class: 'detail-label' }, '任务参数：'),
            h('span', { class: 'detail-value' }, row.jobParam || '-')
          ])
        ]),
        h('div', { class: 'detail-section' }, [
          h('h4', { class: 'section-title' }, '执行信息'),
          h('div', { class: 'detail-row' }, [
            h('span', { class: 'detail-label' }, '执行状态：'),
            h('span', { 
              class: row.status === 1 ? 'text-success' : 'text-error',
              style: { fontWeight: 500 }
            }, row.status === 1 ? '✓ 执行成功' : '✗ 执行失败')
          ]),
          h('div', { class: 'detail-row' }, [
            h('span', { class: 'detail-label' }, '执行时长：'),
            h('span', { class: 'detail-value' }, `${row.duration || 0}ms`)
          ]),
          h('div', { class: 'detail-row' }, [
            h('span', { class: 'detail-label' }, '重试次数：'),
            h('span', { class: 'detail-value' }, `${row.retryCount || 0}次`)
          ]),
          h('div', { class: 'detail-row' }, [
            h('span', { class: 'detail-label' }, '触发时间：'),
            h('span', { class: 'detail-value' }, formatDateTime(row.triggerTime))
          ]),
          h('div', { class: 'detail-row' }, [
            h('span', { class: 'detail-label' }, '开始时间：'),
            h('span', { class: 'detail-value' }, formatDateTime(row.startTime))
          ]),
          h('div', { class: 'detail-row' }, [
            h('span', { class: 'detail-label' }, '结束时间：'),
            h('span', { class: 'detail-value' }, formatDateTime(row.endTime))
          ])
        ]),
        row.result ? h('div', { class: 'detail-section' }, [
          h('h4', { class: 'section-title' }, '执行结果'),
          h('pre', { 
            class: 'result-info',
            style: {
              background: '#f0f9ff',
              padding: '12px',
              borderRadius: '4px',
              fontSize: '12px',
              maxHeight: '200px',
              overflow: 'auto',
              border: '1px solid #d1e7fd'
            }
          }, row.result)
        ]) : null,
        row.exceptionMsg ? h('div', { class: 'detail-section' }, [
          h('h4', { class: 'section-title text-error' }, '异常信息'),
          h('pre', { 
            class: 'exception-info',
            style: {
              background: '#fff1f0',
              padding: '12px',
              borderRadius: '4px',
              fontSize: '12px',
              maxHeight: '300px',
              overflow: 'auto',
              border: '1px solid #ffccc7',
              color: '#cf1322'
            }
          }, row.exceptionMsg)
        ]) : null
      ])
    },
    positiveText: '关闭',
    style: {
      width: '800px'
    }
  })
}

onMounted(() => {
  loadLogList()
})

// 暴露刷新方法
defineExpose({
  refresh: loadLogList
})
</script>

<style scoped>
.job-log-list {
  width: 100%;
}

.search-bar {
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
}

:deep(.detail-section) {
  margin-bottom: 20px;
}

:deep(.detail-section:last-child) {
  margin-bottom: 0;
}

:deep(.section-title) {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 2px solid #f0f0f0;
}

:deep(.detail-row) {
  display: flex;
  align-items: flex-start;
  margin-bottom: 10px;
  line-height: 1.6;
}

:deep(.detail-label) {
  font-weight: 500;
  color: #666;
  min-width: 100px;
  flex-shrink: 0;
}

:deep(.detail-value) {
  color: #262626;
  word-break: break-all;
}

:deep(.text-success) {
  color: #18a058;
}

:deep(.text-error) {
  color: #d03050;
}

:deep(.result-info),
:deep(.exception-info) {
  font-family: 'Courier New', 'Consolas', monospace;
  white-space: pre-wrap;
  word-wrap: break-word;
  line-height: 1.5;
}
</style>
