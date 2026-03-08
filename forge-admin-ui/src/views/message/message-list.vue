<template>
  <div class="p-16">
    <div class="bg-white rounded p-16">
      <h2 class="text-18 font-bold mb-16">我的消息</h2>
      <!-- 搜索栏 -->
      <n-space class="mb-16" :vertical="false">
        <n-select
            v-model:value="queryParams.type"
            placeholder="消息类型"
            clearable
            style="width: 150px"
            :options="messageTypeOptions"
        />
        <n-select
            v-model:value="queryParams.readFlag"
            placeholder="阅读状态"
            clearable
            style="width: 150px"
            :options="readFlagOptions"
        />
        <n-input
            v-model:value="queryParams.keyword"
            placeholder="搜索标题或内容"
            clearable
            style="width: 250px"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
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
        <n-button
            v-if="selectedRowKeys.length > 0"
            @click="handleBatchMarkRead"
        >
          <template #icon>
            <i class="i-material-symbols:mark-email-read-outline" />
          </template>
          批量标记已读
        </n-button>
        <n-button @click="handleMarkAllRead">
          <template #icon>
            <i class="i-material-symbols:done-all" />
          </template>
          全部已读
        </n-button>
      </n-space>

      <!-- 消息列表 -->
      <n-data-table
          :columns="columns"
          :data="dataSource"
          :loading="loading"
          :pagination="pagination"
          :row-key="row => row.id"
          @update:checked-row-keys="handleCheck"
      />
    </div>

    <!-- 消息详情抽屉 -->
    <Teleport to="body">
      <n-drawer
        v-model:show="showDetail"
        :width="600"
        placement="right"
    >
      <n-drawer-content :title="currentMessage?.title" closable>
        <div class="message-detail">
          <div class="detail-meta">
            <n-space>
              <n-tag :type="getMessageTypeColor(currentMessage?.type)" size="small">
                {{ getMessageTypeText(currentMessage?.type) }}
              </n-tag>
              <n-tag v-if="currentMessage?.readFlag === 0" type="error" size="small">
                未读
              </n-tag>
              <n-tag v-else type="success" size="small">
                已读
              </n-tag>
            </n-space>
            <div class="text-gray-500 text-12 mt-8">
              {{ currentMessage?.createTime }}
            </div>
          </div>
          <n-divider />
          <div class="message-content" v-html="currentMessage?.content"></div>
        </div>
      </n-drawer-content>
    </n-drawer>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import { NTag, NButton } from 'naive-ui'
import { request } from '@/utils'

defineOptions({ name: 'MessageList' })

const loading = ref(false)
const dataSource = ref([])
const selectedRowKeys = ref([])
const showDetail = ref(false)
const currentMessage = ref(null)

// 查询参数
const queryParams = ref({
  type: null,
  readFlag: null,
  keyword: null
})

// 分页配置
const pagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100],
  onChange: (page) => {
    pagination.value.page = page
    loadData()
  },
  onUpdatePageSize: (pageSize) => {
    pagination.value.pageSize = pageSize
    pagination.value.page = 1
    loadData()
  }
})

// 消息类型选项
const messageTypeOptions = [
  { label: '系统消息', value: 'SYSTEM' },
  { label: '短信', value: 'SMS' },
  { label: '邮件', value: 'EMAIL' },
  { label: '自定义', value: 'CUSTOM' }
]

// 阅读状态选项
const readFlagOptions = [
  { label: '未读', value: 0 },
  { label: '已读', value: 1 }
]

// 表格列配置
const columns = [
  {
    type: 'selection'
  },
  {
    title: '消息标题',
    key: 'title',
    ellipsis: { tooltip: true },
    render: (row) => {
      return h('div', { class: 'flex items-center' }, [
        row.readFlag === 0 ? h('span', {
          class: 'w-6 h-6 bg-red-500 rounded-full mr-8'
        }) : null,
        h('span', {
          class: row.readFlag === 0 ? 'font-bold' : '',
          style: { cursor: 'pointer', color: '#18a058' },
          onClick: () => handleViewDetail(row)
        }, row.title)
      ])
    }
  },
  {
    title: '消息类型',
    key: 'type',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: getMessageTypeColor(row.type),
        size: 'small'
      }, { default: () => getMessageTypeText(row.type) })
    }
  },
  {
    title: '状态',
    key: 'readFlag',
    width: 80,
    render: (row) => {
      return h(NTag, {
        type: row.readFlag === 0 ? 'error' : 'success',
        size: 'small'
      }, { default: () => row.readFlag === 0 ? '未读' : '已读' })
    }
  },
  {
    title: '接收时间',
    key: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
    render: (row) => {
      return h('div', { class: 'flex gap-8' }, [
        h(NButton, {
          size: 'small',
          type: 'primary',
          text: true,
          onClick: () => handleViewDetail(row)
        }, { default: () => '查看' }),
        row.readFlag === 0 ? h(NButton, {
          size: 'small',
          type: 'info',
          text: true,
          onClick: () => handleMarkRead(row.id)
        }, { default: () => '标记已读' }) : null
      ])
    }
  }
]

// 加载数据
async function loadData() {
  try {
    loading.value = true
    const res = await request.post(
      `/api/message/page?pageNum=${pagination.value.page}&pageSize=${pagination.value.pageSize}`,
      queryParams.value
    )
    if (res.code === 200 && res.data) {
      dataSource.value = res.data.records || res.data.list || []
      pagination.value.itemCount = res.data.total || 0
    }
  } catch (error) {
    console.error('加载消息列表失败:', error)
    window.$message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.value.page = 1
  loadData()
}

// 重置
function handleReset() {
  queryParams.value = {
    type: null,
    readFlag: null,
    keyword: null
  }
  pagination.value.page = 1
  loadData()
}

// 选中行
function handleCheck(keys) {
  selectedRowKeys.value = keys
}

// 查看详情
async function handleViewDetail(row) {
  try {
    const res = await request.get(`/api/message/${row.id}`)
    if (res.code === 200) {
      currentMessage.value = res.data
      showDetail.value = true
      // 如果未读,标记为已读
      if (row.readFlag === 0) {
        await request.post(`/api/message/${row.id}/read`)
        loadData()
      }
    }
  } catch (error) {
    console.error('获取消息详情失败:', error)
    window.$message.error('获取详情失败')
  }
}

// 标记已读
async function handleMarkRead(id) {
  try {
    await request.post(`/api/message/${id}/read`)
    window.$message.success('已标记为已读')
    loadData()
  } catch (error) {
    window.$message.error('操作失败')
  }
}

// 批量标记已读
async function handleBatchMarkRead() {
  if (selectedRowKeys.value.length === 0) {
    window.$message.warning('请选择要操作的消息')
    return
  }
  try {
    await request.post('/api/message/read/batch', selectedRowKeys.value)
    window.$message.success('已批量标记为已读')
    selectedRowKeys.value = []
    loadData()
  } catch (error) {
    window.$message.error('操作失败')
  }
}

// 全部标记已读
async function handleMarkAllRead() {
  window.$dialog.warning({
    title: '确认',
    content: '确定要将所有未读消息标记为已读吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.post('/api/message/read/all')
        window.$message.success('已全部标记为已读')
        loadData()
      } catch (error) {
        window.$message.error('操作失败')
      }
    }
  })
}

// 获取消息类型文本
function getMessageTypeText(type) {
  const map = {
    'SYSTEM': '系统消息',
    'SMS': '短信',
    'EMAIL': '邮件',
    'CUSTOM': '自定义'
  }
  return map[type] || type
}

// 获取消息类型颜色
function getMessageTypeColor(type) {
  const map = {
    'SYSTEM': 'info',
    'SMS': 'warning',
    'EMAIL': 'success',
    'CUSTOM': 'default'
  }
  return map[type] || 'default'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.message-detail {
  padding: 16px 0;
}

.detail-meta {
  margin-bottom: 16px;
}

.message-content {
  line-height: 1.8;
  color: #333;
}

.message-content :deep(p) {
  margin: 8px 0;
}
</style>
