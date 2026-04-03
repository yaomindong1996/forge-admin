<template>
  <div class="message-list-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="apiConfig"
      :search-schema="searchSchema"
      :columns="tableColumns"
      row-key="id"
      :hide-add="true"
      :hide-batch-delete="true"
      @selection-change="handleSelectionChange"
    >
      <!-- 自定义操作列 -->
      <template #table-action="{ row }">
        <div class="flex gap-8">
          <a class="text-primary cursor-pointer" @click="handleViewDetail(row)">
            查看
          </a>
          <a v-if="row.bizType && row.bizKey" class="text-primary cursor-pointer" @click="handleJumpToBiz(row)">
            查看详情
          </a>
          <a v-if="row.readFlag === 0" class="text-primary cursor-pointer" @click="handleMarkRead(row.id)">
            标记已读
          </a>
        </div>
      </template>

      <!-- 自定义顶部工具栏 -->
      <template #toolbar-end>
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
      </template>
    </AiCrudPage>

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
            <n-divider v-if="currentMessage?.bizType && currentMessage?.bizKey" />
            <div v-if="currentMessage?.bizType && currentMessage?.bizKey" class="biz-info">
              <n-space align="center" justify="space-between">
                <div>
                  <span class="text-gray-500">关联业务：</span>
                  <span>{{ getBizTypeName(currentMessage?.bizType) }} ({{ currentMessage?.bizKey }})</span>
                </div>
                <n-button type="primary" size="small" @click="handleJumpToBiz(currentMessage)">
                  查看详情
                </n-button>
              </n-space>
            </div>
          </div>
        </n-drawer-content>
      </n-drawer>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, h } from 'vue'
import { NTag } from 'naive-ui'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'
import { useRouter } from 'vue-router'
import messageApi from '@/api/message'

defineOptions({ name: 'MessageList' })

const router = useRouter()
const crudRef = ref(null)
const showDetail = ref(false)
const currentMessage = ref(null)
const selectedRowKeys = ref([])
const bizTypeOptions = ref([])

const apiConfig = {
  list: 'post@/api/message/page'
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
        { label: '短信', value: 'SMS' },
        { label: '邮件', value: 'EMAIL' },
        { label: '自定义', value: 'CUSTOM' }
      ]
    }
  },
  {
    field: 'readFlag',
    label: '阅读状态',
    type: 'select',
    props: {
      placeholder: '请选择阅读状态',
      clearable: true,
      options: [
        { label: '未读', value: 0 },
        { label: '已读', value: 1 }
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
  }
]

const tableColumns = [
  {
    prop: 'title',
    label: '消息标题',
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
    prop: 'type',
    label: '消息类型',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: getMessageTypeColor(row.type),
        size: 'small'
      }, { default: () => getMessageTypeText(row.type) })
    }
  },
  {
    prop: 'readFlag',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(NTag, {
        type: row.readFlag === 0 ? 'error' : 'success',
        size: 'small'
      }, { default: () => row.readFlag === 0 ? '未读' : '已读' })
    }
  },
  {
    prop: 'createTime',
    label: '接收时间',
    width: 180
  },
  {
    prop: 'action',
    label: '操作',
    width: 120,
    fixed: 'right',
    _slot: 'action'
  }
]

function handleSelectionChange(keys) {
  selectedRowKeys.value = keys
}

async function handleViewDetail(row) {
  try {
    const res = await request.get(`/api/message/${row.id}`)
    if (res.code === 200) {
      currentMessage.value = res.data
      showDetail.value = true
      if (row.readFlag === 0) {
        await request.post(`/api/message/${row.id}/read`)
        crudRef.value?.refresh()
      }
    }
  } catch (error) {
    console.error('获取消息详情失败:', error)
    window.$message.error('获取详情失败')
  }
}

async function handleMarkRead(id) {
  try {
    await request.post(`/api/message/${id}/read`)
    window.$message.success('已标记为已读')
    crudRef.value?.refresh()
  } catch (error) {
    window.$message.error('操作失败')
  }
}

async function handleBatchMarkRead() {
  if (selectedRowKeys.value.length === 0) {
    window.$message.warning('请选择要操作的消息')
    return
  }
  try {
    await request.post('/api/message/read/batch', selectedRowKeys.value)
    window.$message.success('已批量标记为已读')
    selectedRowKeys.value = []
    crudRef.value?.refresh()
  } catch (error) {
    window.$message.error('操作失败')
  }
}

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
        crudRef.value?.refresh()
      } catch (error) {
        window.$message.error('操作失败')
      }
    }
  })
}

function getMessageTypeText(type) {
  const map = {
    'SYSTEM': '系统消息',
    'SMS': '短信',
    'EMAIL': '邮件',
    'CUSTOM': '自定义'
  }
  return map[type] || type
}

function getMessageTypeColor(type) {
  const map = {
    'SYSTEM': 'info',
    'SMS': 'warning',
    'EMAIL': 'success',
    'CUSTOM': 'default'
  }
  return map[type] || 'default'
}

async function loadBizTypes() {
  try {
    const res = await messageApi.getBizTypeListEnabled()
    if (res.code === 200 && res.data) {
      bizTypeOptions.value = res.data
    }
  } catch (error) {
    console.error('加载业务类型失败:', error)
  }
}

function getBizTypeName(bizType) {
  const item = bizTypeOptions.value.find(opt => opt.bizType === bizType)
  return item ? item.bizName : bizType
}

function handleJumpToBiz(message) {
  const bizConfig = bizTypeOptions.value.find(opt => opt.bizType === message.bizType)
  if (bizConfig && bizConfig.jumpUrl) {
    let jumpUrl = bizConfig.jumpUrl
    jumpUrl = jumpUrl.replace('${bizKey}', message.bizKey)
    jumpUrl = jumpUrl.replace('${messageId}', message.id)
    
    if (bizConfig.jumpTarget === '_blank') {
      window.open(jumpUrl, '_blank')
    } else {
      router.push(jumpUrl)
    }
  }
}

loadBizTypes()
</script>

<style scoped>
.message-list-page {
  height: 100%;
}

.message-detail {
  padding: 16px 0;
}

.biz-info {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
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
