<template>
  <div class="operation-log-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/system/operationLog/page',
        detail: 'get@/system/operationLog/{id}'
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      row-key="id"
      :hide-add="true"
      :hide-selection="true"
      :hide-batch-delete="true"
      :before-search="handleBeforeSearch"
    >
    </AiCrudPage>

    <!-- 详情弹窗 -->
    <n-modal
      v-model:show="detailVisible"
      title="操作日志详情"
      preset="card"
      style="width: 900px"
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
          <h4 class="section-title">操作信息</h4>
          <div class="detail-row">
            <span class="label">操作模块：</span>
            <span class="value">{{ currentLog.operationModule || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">操作类型：</span>
            <n-tag :type="getOperationTypeTag(currentLog.operationType).type" size="small">
              {{ getOperationTypeTag(currentLog.operationType).text }}
            </n-tag>
          </div>
          <div class="detail-row">
            <span class="label">操作描述：</span>
            <span class="value">{{ currentLog.operationDesc || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">操作状态：</span>
            <n-tag :type="currentLog.operationStatus === 1 ? 'success' : 'error'" size="small">
              {{ currentLog.operationStatus === 1 ? '成功' : '失败' }}
            </n-tag>
          </div>
          <div class="detail-row">
            <span class="label">操作时间：</span>
            <span class="value">{{ currentLog.operationTime }}</span>
          </div>
          <div class="detail-row">
            <span class="label">执行耗时：</span>
            <span class="value">{{ currentLog.executeTime || 0 }}ms</span>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">请求信息</h4>
          <div class="detail-row">
            <span class="label">请求方法：</span>
            <n-tag size="small">{{ currentLog.requestMethod || '-' }}</n-tag>
          </div>
          <div class="detail-row">
            <span class="label">请求URL：</span>
            <span class="value">{{ currentLog.requestUrl || '-' }}</span>
          </div>
          <div v-if="currentLog.requestParams" class="detail-row full">
            <span class="label">请求参数：</span>
            <pre class="code-block">{{ formatJson(currentLog.requestParams) }}</pre>
          </div>
        </div>

        <div v-if="currentLog.responseResult" class="detail-section">
          <h4 class="section-title">响应结果</h4>
          <pre class="code-block">{{ formatJson(currentLog.responseResult) }}</pre>
        </div>

        <div v-if="currentLog.errorMsg" class="detail-section">
          <h4 class="section-title text-error">错误信息</h4>
          <pre class="error-block">{{ currentLog.errorMsg }}</pre>
        </div>

        <div class="detail-section">
          <h4 class="section-title">环境信息</h4>
          <div class="detail-row">
            <span class="label">操作IP：</span>
            <span class="value">{{ currentLog.operationIp || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">操作地点：</span>
            <span class="value">{{ currentLog.operationLocation || '-' }}</span>
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
import { ref, h, computed } from 'vue'
import { NTag } from 'naive-ui'
import { AiCrudPage } from '@/components/ai-form'
import {formatDateTime, request} from '@/utils'

defineOptions({ name: 'OperationLog' })

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
    field: 'operationModule',
    label: '操作模块',
    type: 'input',
    props: {
      placeholder: '请输入操作模块'
    }
  },
  {
    field: 'operationType',
    label: '操作类型',
    type: 'select',
    props: {
      placeholder: '请选择操作类型',
      clearable: true,
      options: [
        { label: '查询', value: 'QUERY' },
        { label: '新增', value: 'INSERT' },
        { label: '更新', value: 'UPDATE' },
        { label: '删除', value: 'DELETE' },
        { label: '导入', value: 'IMPORT' },
        { label: '导出', value: 'EXPORT' },
        { label: '其他', value: 'OTHER' }
      ]
    }
  },
  {
    field: 'operationStatus',
    label: '操作状态',
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
    field: 'operationIp',
    label: '操作IP',
    type: 'input',
    props: {
      placeholder: '请输入IP地址'
    }
  },
  {
    field: 'requestParams',
    label: '请求参数',
    type: 'input',
    props: {
      placeholder: '请输入请求参数关键字'
    }
  },
  {
    field: 'timeRange',
    label: '操作时间',
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
const tableColumns = computed(() => [
  {
    prop: 'username',
    label: '用户名',
    width: 100
  },
  {
    prop: 'operationModule',
    label: '操作模块',
    width: 120,
    ellipsis: { tooltip: true }
  },
  {
    prop: 'operationType',
    label: '操作类型',
    width: 90,
    render: (row) => {
      const config = getOperationTypeTag(row.operationType)
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    }
  },
  {
    prop: 'operationDesc',
    label: '操作描述',
    width: 150,
    ellipsis: { tooltip: true },
    render: (row) => row.operationDesc || '-'
  },
  {
    prop: 'requestMethod',
    label: '请求方法',
    width: 90,
    render: (row) => {
      return h(NTag, { size: 'small' }, { default: () => row.requestMethod || '-' })
    }
  },
  {
    prop: 'requestUrl',
    label: '请求URL',
    width: 200,
    ellipsis: { tooltip: true }
  },
  {
    prop: 'operationStatus',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(NTag,
        { type: row.operationStatus === 1 ? 'success' : 'error', size: 'small' },
        { default: () => row.operationStatus === 1 ? '成功' : '失败' }
      )
    }
  },
  {
    prop: 'operationIp',
    label: '操作IP',
    width: 130
  },
  {
    prop: 'executeTime',
    label: '耗时(ms)',
    width: 90,
    render: (row) => row.executeTime || 0
  },
  {
    prop: 'operationTime',
    label: '操作时间',
    width: 160
  },
  {
    prop: 'action',
    label: '操作',
    width: 80,
    fixed: 'right',
    actions: [
      { label: '详情', key: 'detail', onClick: handleViewDetail }
    ]
  }
])

// 获取操作类型标签配置
function getOperationTypeTag(operationType) {
  const typeMap = {
    'QUERY': { text: '查询', type: 'info' },
    'INSERT': { text: '新增', type: 'success' },
    'UPDATE': { text: '更新', type: 'warning' },
    'DELETE': { text: '删除', type: 'error' },
    'IMPORT': { text: '导入', type: 'primary' },
    'EXPORT': { text: '导出', type: 'primary' },
    'OTHER': { text: '其他', type: 'default' }
  }
  return typeMap[operationType] || { text: operationType, type: 'default' }
}

// 格式化JSON
function formatJson(jsonStr) {
  if (!jsonStr) return '-'
  try {
    const obj = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr
    return JSON.stringify(obj, null, 2)
  } catch {
    return jsonStr
  }
}

// 查看详情
async function handleViewDetail(row) {
  try {
    const res = await request.get(`/system/operationLog/${row.id}`)
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
.operation-log-page {
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

.section-title.text-error {
  color: #d03050;
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

.code-block {
  font-family: 'Courier New', 'Consolas', monospace;
  font-size: 12px;
  background: #f0f9ff;
  padding: 12px;
  border-radius: 4px;
  margin: 8px 0 0 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  line-height: 1.5;
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #d1e7fd;
}

.error-block {
  font-family: 'Courier New', 'Consolas', monospace;
  font-size: 12px;
  background: #fff1f0;
  padding: 12px;
  border-radius: 4px;
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  line-height: 1.5;
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #ffccc7;
  color: #cf1322;
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
