<template>
  <div class="login-log-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/system/loginLog/page',
        detail: 'get@/system/loginLog/{id}'
      }"
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
    </AiCrudPage>

    <!-- 详情弹窗 -->
    <n-modal
      v-model:show="detailVisible"
      title="登录日志详情"
      preset="card"
      style="width: 700px"
    >
      <div v-if="currentLog" class="log-detail">
        <div class="detail-section">
          <h4 class="section-title">用户信息</h4>
          <div class="detail-row">
            <span class="label">用户名：</span>
            <span class="value">{{ currentLog.username || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">用户ID：</span>
            <span class="value">{{ currentLog.userId || '-' }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">登录信息</h4>
          <div class="detail-row">
            <span class="label">登录类型：</span>
            <n-tag :type="getLoginTypeTag(currentLog.loginType).type" size="small">
              {{ getLoginTypeTag(currentLog.loginType).text }}
            </n-tag>
          </div>
          <div class="detail-row">
            <span class="label">登录状态：</span>
            <n-tag :type="currentLog.loginStatus === 1 ? 'success' : 'error'" size="small">
              {{ currentLog.loginStatus === 1 ? '成功' : '失败' }}
            </n-tag>
          </div>
          <div class="detail-row">
            <span class="label">登录时间：</span>
            <span class="value">{{ currentLog.loginTime }}</span>
          </div>
          <div class="detail-row">
            <span class="label">登录信息：</span>
            <span class="value">{{ currentLog.loginMessage || '-' }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">环境信息</h4>
          <div class="detail-row">
            <span class="label">登录IP：</span>
            <span class="value">{{ currentLog.loginIp || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">登录地点：</span>
            <span class="value">{{ currentLog.loginLocation || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">浏览器：</span>
            <span class="value">{{ currentLog.browser || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">操作系统：</span>
            <span class="value">{{ currentLog.os || '-' }}</span>
          </div>
          <div class="detail-row full">
            <span class="label">User-Agent：</span>
            <pre class="user-agent">{{ currentLog.userAgent || '-' }}</pre>
          </div>
        </div>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="detailVisible = false">关闭</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, h } from 'vue'
import { NTag } from 'naive-ui'
import { AiCrudPage } from '@/components/ai-form'
import {formatDateTime, request} from '@/utils'

defineOptions({ name: 'LoginLog' })

const crudRef = ref(null)
const detailVisible = ref(false)
const currentLog = ref(null)

// 搜索前处理 - 转换时间范围参数
function handleBeforeSearch(params) {
  const result = { ...params }

  // 处理时间范围转换
  if (params.timeRange && params.timeRange.length === 2) {
    result.startTime = formatDateTime(params.timeRange[0])
    result.endTime = formatDateTime(params.timeRange[1])
    console.log('Formatted Dates:', result.startTime, result.endTime)
    delete result.timeRange
  }

  return result
}

// 搜索表单配置
const searchSchema = [
  {
    field: 'username',
    label: '用户名',
    type: 'input',
    props: {
      placeholder: '请输入用户名'
    }
  },
  {
    field: 'loginType',
    label: '登录类型',
    type: 'select',
    props: {
      placeholder: '请选择登录类型',
      clearable: true,
      options: [
        { label: '登录', value: 'LOGIN' },
        { label: '登出', value: 'LOGOUT' },
        { label: '注册', value: 'REGISTER' },
        { label: '被踢下线', value: 'KICKOUT' },
        { label: '被顶下线', value: 'REPLACED' }
      ]
    }
  },
  {
    field: 'loginStatus',
    label: '登录状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      clearable: true,
      options: [
        { label: '成功', value: 1 },
        { label: '失败', value: 0 }
      ]
    }
  },
  {
    field: 'loginIp',
    label: '登录IP',
    type: 'input',
    props: {
      placeholder: '请输入IP地址'
    }
  },
  {
    field: 'timeRange',
    label: '登录时间',
    type: 'daterange',
    startPlaceholder: '开始时间',
    endPlaceholder: '结束时间',
    clearable: true,
    format: 'yyyy-MM-dd HH:mm:ss',
    valueFormat: 'yyyy-MM-dd HH:mm:ss',
    props: {
      type: 'datetimerange'
    },
    // 时间范围转换为 startTime 和 endTime
    transform: (value) => {
      if (value && value.length === 2) {
        return {
          startTime: value[0],
          endTime: value[1]
        }
      }
      return {}
    }
  }
]

// 表格列配置
const tableColumns = [
  {
    prop: 'username',
    label: '用户名',
    width: 120
  },
  {
    prop: 'loginType',
    label: '登录类型',
    width: 100,
    render: (row) => {
      const config = getLoginTypeTag(row.loginType)
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    }
  },
  {
    prop: 'loginStatus',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(NTag,
        { type: row.loginStatus === 1 ? 'success' : 'error', size: 'small' },
        { default: () => row.loginStatus === 1 ? '成功' : '失败' }
      )
    }
  },
  {
    prop: 'loginIp',
    label: '登录IP',
    width: 130
  },
  {
    prop: 'loginLocation',
    label: '登录地点',
    width: 150,
    render: (row) => row.loginLocation || '-'
  },
  {
    prop: 'browser',
    label: '浏览器',
    width: 120,
    ellipsis: { tooltip: true },
    render: (row) => row.browser || '-'
  },
  {
    prop: 'os',
    label: '操作系统',
    width: 120,
    ellipsis: { tooltip: true },
    render: (row) => row.os || '-'
  },
  {
    prop: 'loginTime',
    label: '登录时间',
    width: 160
  },
  {
    prop: 'loginMessage',
    label: '登录信息',
    minWidth: 150,
    ellipsis: { tooltip: true },
    render: (row) => row.loginMessage || '-'
  },
  {
    prop: 'action',
    label: '操作',
    width: 80,
    fixed: 'right',
    _slot: 'action'
  }
]

// 获取登录类型标签配置
function getLoginTypeTag(loginType) {
  const typeMap = {
    'LOGIN': { text: '登录', type: 'success' },
    'LOGOUT': { text: '登出', type: 'info' },
    'REGISTER': { text: '注册', type: 'warning' },
    'KICKOUT': { text: '踢下线', type: 'error' },
    'REPLACED': { text: '顶下线', type: 'error' }
  }
  return typeMap[loginType] || { text: loginType, type: 'default' }
}

// 查看详情
async function handleViewDetail(row) {
  try {
    const res = await request.get(`/system/loginLog/${row.id}`)
    if (res.code === 200) {
      currentLog.value = res.data
      detailVisible.value = true
    }
  } catch (error) {
    console.error('获取详情失败:', error)
  }
}
</script>

<style scoped>
.login-log-page {
  height: 100%;
}

.log-detail {
  padding: 8px 0;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 16px 0;
  padding-bottom: 8px;
  border-bottom: 2px solid #f0f0f0;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
  line-height: 1.6;
}

.detail-row.full {
  flex-direction: column;
}

.detail-row .label {
  font-weight: 500;
  color: #666;
  min-width: 100px;
  flex-shrink: 0;
}

.detail-row .value {
  color: #262626;
  word-break: break-all;
}

.user-agent {
  font-family: 'Courier New', 'Consolas', monospace;
  font-size: 12px;
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  margin: 8px 0 0 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  line-height: 1.5;
  max-height: 200px;
  overflow-y: auto;
}
</style>
