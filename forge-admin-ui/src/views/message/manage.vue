<template>
  <div class="message-manage-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="apiConfig"
      :search-schema="searchSchema"
      :columns="tableColumns"
      row-key="id"
      :hide-add="true"
      :hide-selection="true"
      :hide-batch-delete="true"
      :before-search="handleBeforeSearch"
    >
      <!-- 自定义操作列 -->
      <template #table-action="{ row }">
        <a class="text-primary cursor-pointer" @click="handleViewDetail(row)">
          详情
        </a>
      </template>

      <!-- 自定义顶部工具栏 -->
      <template #toolbar-end>
        <n-button type="primary" @click="showSendModal = true">
          <template #icon>
            <i class="i-material-symbols:send" />
          </template>
          发送测试消息
        </n-button>
      </template>
    </AiCrudPage>

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
          <n-input v-model:value="sendForm.title" placeholder="请输入消息标题" />
        </n-form-item>
        <n-form-item label="消息内容" path="content">
          <n-input
            v-model:value="sendForm.content"
            type="textarea"
            placeholder="请输入消息内容"
            :rows="4"
          />
        </n-form-item>
        <n-form-item label="模版编码" path="templateCode">
          <n-input v-model:value="sendForm.templateCode" placeholder="请输入模版编码" />
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
        <n-form-item label="业务类型" path="bizType">
          <n-select
            v-model:value="sendForm.bizType"
            placeholder="选择业务类型（可选）"
            :options="bizTypeOptions"
            clearable
          />
        </n-form-item>
        <n-form-item v-if="sendForm.bizType" label="业务主键" path="bizKey">
          <n-input
            v-model:value="sendForm.bizKey"
            placeholder="请输入业务主键（如：订单ID、流程实例ID）"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showSendModal = false">取消</n-button>
          <n-button type="primary" :loading="sending" @click="handleSend">
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
        <n-descriptions-item v-if="currentDetail?.message?.bizType" label="业务类型">
          {{ getBizTypeName(currentDetail?.message?.bizType) }}
        </n-descriptions-item>
        <n-descriptions-item v-if="currentDetail?.message?.bizKey" label="业务主键">
          {{ currentDetail?.message?.bizKey }}
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
import { ref, h } from 'vue'
import { NTag } from 'naive-ui'
import { AiCrudPage } from '@/components/ai-form'
import messageApi from '@/api/message'
import { request } from '@/utils'

defineOptions({ name: 'MessageManage' })

const crudRef = ref(null)
const showSendModal = ref(false)
const sending = ref(false)
const userLoading = ref(false)
const showDetail = ref(false)
const currentDetail = ref(null)
const userOptions = ref([])
const bizTypeOptions = ref([])

const sendForm = ref({
  channel: 'WEB',
  title: '',
  content: '',
  templateCode: '',
  sendScope: 'USERS',
  userIds: [],
  type: 'SYSTEM',
  bizType: null,
  bizKey: ''
})

const apiConfig = {
  list: 'post@/api/message/manage/page',
  detail: 'get@/api/message/manage/{id}/detail'
}

const searchSchema = [
  {
    field: 'type',
    label: '消息类型',
    type: 'select',
    props: {
      placeholder: '请选择消息类型',
      clearable: true,
      options: [
        { label: '系统消息', value: 'SYSTEM' },
        { label: '自定义', value: 'CUSTOM' }
      ]
    }
  },
  {
    field: 'channel',
    label: '发送渠道',
    type: 'select',
    props: {
      placeholder: '请选择发送渠道',
      clearable: true,
      options: [
        { label: '站内信', value: 'WEB' },
        { label: '短信', value: 'SMS' },
        { label: '邮件', value: 'EMAIL' }
      ]
    }
  },
  {
    field: 'status',
    label: '发送状态',
    type: 'select',
    props: {
      placeholder: '请选择发送状态',
      clearable: true,
      options: [
        { label: '草稿', value: 0 },
        { label: '已发送', value: 1 },
        { label: '发送失败', value: 2 }
      ]
    }
  },
  {
    field: 'keyword',
    label: '关键词',
    type: 'input',
    props: {
      placeholder: '搜索标题或内容'
    }
  },
  {
    field: 'timeRange',
    label: '发送时间',
    type: 'daterange',
    props: {
      type: 'datetimerange',
      clearable: true
    }
  }
]

const tableColumns = [
  {
    prop: 'title',
    label: '消息标题',
    ellipsis: { tooltip: true }
  },
  {
    prop: 'type',
    label: '消息类型',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: getTypeColor(row.type),
        size: 'small'
      }, { default: () => getTypeText(row.type) })
    }
  },
  {
    prop: 'channel',
    label: '发送渠道',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: getChannelColor(row.channel),
        size: 'small'
      }, { default: () => getChannelText(row.channel) })
    }
  },
  {
    prop: 'status',
    label: '发送状态',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: getStatusColor(row.status),
        size: 'small'
      }, { default: () => getStatusText(row.status) })
    }
  },
  {
    prop: 'receiverCount',
    label: '接收人数',
    width: 100
  },
  {
    prop: 'readStatus',
    label: '已读/未读',
    width: 120,
    render: (row) => `${row.readCount}/${row.unreadCount}`
  },
  {
    prop: 'createTime',
    label: '发送时间',
    width: 180
  },
  {
    prop: 'action',
    label: '操作',
    width: 80,
    fixed: 'right',
    _slot: 'action'
  }
]

const receiverColumns = [
  { title: '用户名', key: 'userName' },
  { title: '组织', key: 'orgName' },
  {
    title: '阅读状态',
    key: 'readFlag',
    width: 100,
    render: (row) => h(NTag, {
      type: row.readFlag === 1 ? 'success' : 'error',
      size: 'small'
    }, { default: () => row.readFlag === 1 ? '已读' : '未读' })
  },
  { title: '阅读时间', key: 'readTime', width: 180 }
]

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

function handleBeforeSearch(params) {
  const result = { ...params }

  if (params.timeRange && params.timeRange.length === 2) {
    const [start, end] = params.timeRange
    result.startTime = new Date(start).toISOString().slice(0, 19).replace('T', ' ')
    result.endTime = new Date(end).toISOString().slice(0, 19).replace('T', ' ')
    delete result.timeRange
  }

  return result
}

async function loadUsers(keyword = '') {
  try {
    userLoading.value = true
    const params = {
      pageNum: 1,
      pageSize: 50,
      realName: keyword || undefined
    }
    const res = await request.get('/system/user/page', params)
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
      crudRef.value?.refresh()
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
    templateCode: '',
    sendScope: 'USERS',
    userIds: [],
    type: 'SYSTEM',
    bizType: null,
    bizKey: ''
  }
}

async function loadBizTypes() {
  try {
    const res = await messageApi.getBizTypeListEnabled()
    if (res.code === 200 && res.data) {
      bizTypeOptions.value = res.data.map(item => ({
        label: item.bizName,
        value: item.bizType
      }))
    }
  } catch (error) {
    console.error('加载业务类型失败:', error)
  }
}

function getBizTypeName(bizType) {
  const item = bizTypeOptions.value.find(opt => opt.value === bizType)
  return item ? item.label : bizType
}

async function handleViewDetail(row) {
  try {
    const res = await messageApi.getMessageManageDetail(row.id)
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
  const map = { 'SYSTEM': '系统消息', 'SMS': '短信', 'EMAIL': '邮件', 'CUSTOM': '自定义' }
  return map[type] || type
}

function getTypeColor(type) {
  const map = { 'SYSTEM': 'info', 'SMS': 'warning', 'EMAIL': 'success', 'CUSTOM': 'default' }
  return map[type] || 'default'
}

function getChannelText(channel) {
  const map = { 'WEB': '站内信', 'SMS': '短信', 'EMAIL': '邮件', 'PUSH': '推送' }
  return map[channel] || channel
}

function getChannelColor(channel) {
  const map = { 'WEB': 'default', 'SMS': 'warning', 'EMAIL': 'success', 'PUSH': 'info' }
  return map[channel] || 'default'
}

function getStatusText(status) {
  const map = { 0: '草稿', 1: '已发送', 2: '发送失败' }
  return map[status] || '未知'
}

function getStatusColor(status) {
  const map = { 0: 'default', 1: 'success', 2: 'error' }
  return map[status] || 'default'
}

loadUsers()
loadBizTypes()
</script>

<style scoped>
.message-manage-page {
  height: 100%;
}

.message-content {
  line-height: 1.8;
  color: #333;
  padding: 16px;
}
</style>
