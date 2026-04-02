<template>
  <div class="p-16">
    <div class="bg-white rounded p-16">
      <!-- 消息监控列表 -->
      <n-card title="消息监控">
        <!-- 搜索栏 -->
        <n-space class="mb-16" :vertical="false">
          <n-select
            v-model:value="queryParams.type"
            placeholder="消息类型"
            clearable
            style="width: 150px"
            :options="typeOptions"
          />
          <n-select
            v-model:value="queryParams.channel"
            placeholder="发送渠道"
            clearable
            style="width: 150px"
            :options="channelOptions"
          />
          <n-select
            v-model:value="queryParams.status"
            placeholder="发送状态"
            clearable
            style="width: 150px"
            :options="statusOptions"
          />
          <n-date-picker
            v-model:value="queryParams.timeRange"
            type="daterange"
            clearable
            placeholder="选择时间范围"
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
          <n-button type="primary" @click="showSendModal = true">
            <template #icon>
              <i class="i-material-symbols:send" />
            </template>
            发送测试消息
          </n-button>
        </n-space>

        <!-- 消息列表 -->
        <n-data-table
          :columns="columns"
          :data="dataSource"
          :loading="loading"
          :pagination="pagination"
          :row-key="row => row.id"
        />
      </n-card>
    </div>

    <!-- 消息发送测试弹窗 -->
    <n-modal v-model:show="showSendModal" preset="card" title="消息发送测试" style="width: 600px">
      <n-form ref="sendFormRef" :model="sendForm" label-width="100">
        <n-form-item label="发送渠道" path="channel">
          <n-select
            v-model:value="sendForm.channel"
            placeholder="选择发送渠道"
            :options="channelOptions"
          />
        </n-form-item>
        <n-form-item label="消息标题" path="title">
          <n-input
            v-model:value="sendForm.title"
            placeholder="请输入消息标题"
          />
        </n-form-item>
        <n-form-item label="消息内容" path="content">
          <n-input
            v-model:value="sendForm.content"
            type="textarea"
            placeholder="请输入消息内容"
            :rows="4"
          />
        </n-form-item>
        <n-form-item label="发送范围" path="sendScope">
          <n-select
            v-model:value="sendForm.sendScope"
            placeholder="选择发送范围"
            :options="scopeOptions"
          />
        </n-form-item>
        <n-form-item v-if="sendForm.sendScope === 'USERS'" label="指定用户" path="userIds">
          <n-select
            v-model:value="sendForm.userIds"
            multiple
            placeholder="搜索并选择接收用户"
            :options="userOptions"
            filterable
            clearable
            remote
            :loading="userLoading"
            @search="handleUserSearch"
          />
        </n-form-item>
        <n-form-item label="消息类型" path="type">
          <n-select
            v-model:value="sendForm.type"
            placeholder="选择消息类型"
            :options="typeOptions"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showSendModal = false">取消</n-button>
          <n-button type="primary" @click="handleSend" :loading="sending">
            发送
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 消息详情弹窗 -->
    <n-modal v-model:show="showDetail" preset="card" title="消息详情" style="width: 800px">
      <n-descriptions label-placement="left" :column="2" bordered>
        <n-descriptions-item label="消息标题">
          {{ currentDetail?.message?.title }}
        </n-descriptions-item>
        <n-descriptions-item label="消息类型">
          <n-tag :type="getTypeColor(currentDetail?.message?.type)" size="small">
            {{ getTypeText(currentDetail?.message?.type) }}
          </n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="发送渠道">
          <n-tag :type="getChannelColor(currentDetail?.message?.sendChannel)" size="small">
            {{ getChannelText(currentDetail?.message?.sendChannel) }}
          </n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="发送时间">
          {{ currentDetail?.message?.createTime }}
        </n-descriptions-item>
        <n-descriptions-item label="发送状态">
          <n-tag :type="getStatusColor(currentDetail?.sendRecord?.status)" size="small">
            {{ getStatusText(currentDetail?.sendRecord?.status) }}
          </n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="接收人数">
          {{ currentDetail?.sendRecord?.receiverCount }}
        </n-descriptions-item>
        <n-descriptions-item label="成功数">
          {{ currentDetail?.sendRecord?.successCount }}
        </n-descriptions-item>
        <n-descriptions-item label="失败数">
          {{ currentDetail?.sendRecord?.failCount }}
        </n-descriptions-item>
        <n-descriptions-item v-if="currentDetail?.sendRecord?.errorMsg" label="错误信息" :span="2">
          <n-text type="error">{{ currentDetail?.sendRecord?.errorMsg }}</n-text>
        </n-descriptions-item>
      </n-descriptions>

      <n-divider>消息内容</n-divider>
      <div class="message-content" v-html="currentDetail?.message?.content"></div>

      <n-divider>接收人列表</n-divider>
      <n-data-table
        :columns="receiverColumns"
        :data="currentDetail?.receivers || []"
        :max-height="300"
      />
    </n-modal>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import { NTag, NButton } from 'naive-ui'
import messageApi from '@/api/message'
import { request } from '@/utils'

defineOptions({ name: 'MessageManage' })

const loading = ref(false)
const sending = ref(false)
const userLoading = ref(false)
const dataSource = ref([])
const showDetail = ref(false)
const showSendModal = ref(false)
const currentDetail = ref(null)
const userOptions = ref([])

const sendForm = ref({
  channel: 'WEB',
  title: '',
  content: '',
  sendScope: 'USERS',
  userIds: [],
  type: 'SYSTEM'
})

const queryParams = ref({
  type: null,
  channel: null,
  status: null,
  timeRange: null,
  keyword: null
})

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

const channelOptions = [
  { label: '站内信', value: 'WEB' },
  { label: '短信', value: 'SMS' },
  { label: '邮件', value: 'EMAIL' }
]

const scopeOptions = [
  { label: '指定人员', value: 'USERS' },
  { label: '指定组织', value: 'ORG' },
  { label: '全员', value: 'ALL' }
]

const typeOptions = [
  { label: '系统消息', value: 'SYSTEM' },
  { label: '自定义', value: 'CUSTOM' }
]

const statusOptions = [
  { label: '草稿', value: 0 },
  { label: '已发送', value: 1 },
  { label: '发送失败', value: 2 }
]

const columns = [
  {
    title: '消息标题',
    key: 'title',
    ellipsis: { tooltip: true }
  },
  {
    title: '消息类型',
    key: 'type',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: getTypeColor(row.type),
        size: 'small'
      }, { default: () => getTypeText(row.type) })
    }
  },
  {
    title: '发送渠道',
    key: 'channel',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: getChannelColor(row.channel),
        size: 'small'
      }, { default: () => getChannelText(row.channel) })
    }
  },
  {
    title: '发送状态',
    key: 'status',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: getStatusColor(row.status),
        size: 'small'
      }, { default: () => getStatusText(row.status) })
    }
  },
  {
    title: '接收人数',
    key: 'receiverCount',
    width: 100
  },
  {
    title: '已读/未读',
    key: 'readStatus',
    width: 120,
    render: (row) => {
      return `${row.readCount}/${row.unreadCount}`
    }
  },
  {
    title: '发送时间',
    key: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
    render: (row) => {
      return h(NButton, {
        size: 'small',
        type: 'primary',
        text: true,
        onClick: () => handleViewDetail(row.id)
      }, { default: () => '查看详情' })
    }
  }
]

const receiverColumns = [
  {
    title: '用户名',
    key: 'userName'
  },
  {
    title: '组织',
    key: 'orgName'
  },
  {
    title: '阅读状态',
    key: 'readFlag',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: row.readFlag === 1 ? 'success' : 'error',
        size: 'small'
      }, { default: () => row.readFlag === 1 ? '已读' : '未读' })
    }
  },
  {
    title: '阅读时间',
    key: 'readTime',
    width: 180
  }
]

async function loadUsers(keyword = '') {
  try {
    userLoading.value = true
    const params = {
      pageNum: 1,
      pageSize: 50,
      realName: keyword || undefined
    }
    const res = await request.post('/api/system/user/page', params)
    if (res.code === 200 && res.data) {
      userOptions.value = (res.data.records || []).map(user => ({
        label: user.realName || user.userName,
        value: user.id
      }))
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
  } finally {
    userLoading.value = false
  }
}

function handleUserSearch(keyword) {
  loadUsers(keyword)
}

async function loadData() {
  try {
    loading.value = true
    const params = {
      type: queryParams.value.type,
      channel: queryParams.value.channel,
      status: queryParams.value.status,
      keyword: queryParams.value.keyword
    }

    if (queryParams.value.timeRange && queryParams.value.timeRange.length === 2) {
      const [start, end] = queryParams.value.timeRange
      params.startTime = new Date(start).toISOString().slice(0, 19).replace('T', ' ')
      params.endTime = new Date(end).toISOString().slice(0, 19).replace('T', ' ')
    }

    const res = await messageApi.getMessageManagePage(
      params,
      pagination.value.page,
      pagination.value.pageSize
    )

    if (res.code === 200 && res.data) {
      dataSource.value = res.data.records || []
      pagination.value.itemCount = res.data.total || 0
    }
  } catch (error) {
    console.error('加载消息列表失败:', error)
    window.$message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

async function handleSend() {
  if (!sendForm.value.title || !sendForm.value.content) {
    window.$message.warning('请填写消息标题和内容')
    return
  }

  if (sendForm.value.sendScope === 'USERS' && sendForm.value.userIds.length === 0) {
    window.$message.warning('请选择接收用户')
    return
  }

  try {
    sending.value = true
    const res = await messageApi.sendMessage(sendForm.value)
    if (res.code === 200) {
      window.$message.success('消息发送成功')
      showSendModal.value = false
      handleResetSendForm()
      loadData()
    } else {
      window.$message.error(res.msg || '发送失败')
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    window.$message.error('发送失败')
  } finally {
    sending.value = false
  }
}

function handleResetSendForm() {
  sendForm.value = {
    channel: 'WEB',
    title: '',
    content: '',
    sendScope: 'USERS',
    userIds: [],
    type: 'SYSTEM'
  }
}

function handleSearch() {
  pagination.value.page = 1
  loadData()
}

function handleReset() {
  queryParams.value = {
    type: null,
    channel: null,
    status: null,
    timeRange: null,
    keyword: null
  }
  pagination.value.page = 1
  loadData()
}

async function handleViewDetail(messageId) {
  try {
    const res = await messageApi.getMessageManageDetail(messageId)
    if (res.code === 200) {
      currentDetail.value = res.data
      showDetail.value = true
    }
  } catch (error) {
    console.error('获取消息详情失败:', error)
    window.$message.error('获取详情失败')
  }
}

function getTypeText(type) {
  const map = {
    'SYSTEM': '系统消息',
    'SMS': '短信',
    'EMAIL': '邮件',
    'CUSTOM': '自定义'
  }
  return map[type] || type
}

function getTypeColor(type) {
  const map = {
    'SYSTEM': 'info',
    'SMS': 'warning',
    'EMAIL': 'success',
    'CUSTOM': 'default'
  }
  return map[type] || 'default'
}

function getChannelText(channel) {
  const map = {
    'WEB': '站内信',
    'SMS': '短信',
    'EMAIL': '邮件',
    'PUSH': '推送'
  }
  return map[channel] || channel
}

function getChannelColor(channel) {
  const map = {
    'WEB': 'default',
    'SMS': 'warning',
    'EMAIL': 'success',
    'PUSH': 'info'
  }
  return map[channel] || 'default'
}

function getStatusText(status) {
  const map = {
    0: '草稿',
    1: '已发送',
    2: '发送失败'
  }
  return map[status] || '未发送'
}

function getStatusColor(status) {
  const map = {
    0: 'default',
    1: 'success',
    2: 'error'
  }
  return map[status] || 'default'
}

onMounted(() => {
  loadUsers()
  loadData()
})
</script>

<style scoped>
.message-content {
  line-height: 1.8;
  color: #333;
  padding: 16px;
}
</style>
