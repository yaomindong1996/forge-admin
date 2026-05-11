<template>
  <div class="external-manage-page">
    <n-card class="panel-card" :bordered="false">
      <template #header>
        <div class="panel-header">
          <div>
            <span>{{ isApiStandalone ? '外部接口' : '外部系统' }}</span>
            <div v-if="isApiStandalone" class="panel-subtitle">
              维护所有外部接口配置
            </div>
          </div>
          <NSpace v-if="!isApiStandalone" align="center">
            <NTag size="small" type="info">
              {{ systemCountText }}
            </NTag>
            <NButton size="small" secondary @click="showAllApis">
              查看全部接口
            </NButton>
          </NSpace>
        </div>
      </template>

      <div v-if="!isApiStandalone" class="overview-strip">
        <div class="overview-card">
          <span class="overview-label">系统总数</span>
          <strong>{{ systemStats.total }}</strong>
        </div>
        <div class="overview-card">
          <span class="overview-label">当前页启用</span>
          <strong>{{ systemStats.enabled }}</strong>
        </div>
        <div class="overview-card">
          <span class="overview-label">当前页接口</span>
          <strong>{{ systemStats.apiTotal }}</strong>
        </div>
        <div class="overview-card">
          <span class="overview-label">当前页需认证</span>
          <strong>{{ systemStats.authed }}</strong>
        </div>
      </div>

      <AiCrudPage
        v-if="!isApiStandalone"
        ref="systemCrudRef"
        api="/external/system"
        :api-config="systemApiConfig"
        :load-detail-on-edit="true"
        :search-schema="systemSearchSchema"
        :columns="systemTableColumns"
        :edit-schema="systemEditSchema"
        :before-render-list="beforeRenderSystemList"
        :before-render-detail="beforeRenderSystemDetail"
        :before-render-form="beforeRenderSystemForm"
        :before-submit="beforeSubmitSystem"
        row-key="id"
        add-button-text="新增外部系统"
        :edit-grid-cols="2"
        modal-width="960px"
        :hide-selection="true"
        :scroll-x="1180"
        @load-list-success="handleSystemListLoaded"
        @submit-success="handleSystemSaved"
        @delete="handleSystemDeleted"
      >
        <template #form-customAuthConfig="{ value, updateValue }">
          <div class="kv-config-editor">
            <div
              v-for="(row, index) in normalizeConfigRows(value)"
              :key="index"
              class="kv-config-row"
            >
              <n-input
                class="kv-key"
                :value="row.key"
                placeholder="KEY，如 header"
                @update:value="updateConfigRow(value, updateValue, index, 'key', $event)"
              />
              <n-input
                class="kv-value"
                :value="row.value"
                placeholder="VALUE，如 X-Sign"
                @update:value="updateConfigRow(value, updateValue, index, 'value', $event)"
              />
              <NButton
                quaternary
                type="error"
                :disabled="normalizeConfigRows(value).length === 1"
                @click="removeConfigRow(value, updateValue, index)"
              >
                删除
              </NButton>
            </div>
            <NButton size="small" secondary type="primary" @click="addConfigRow(value, updateValue)">
              添加配置项
            </NButton>
          </div>
        </template>
      </AiCrudPage>

      <AiCrudPage
        v-else
        ref="apiCrudRef"
        api="/external/api"
        :api-config="apiApiConfig"
        :load-detail-on-edit="true"
        :search-schema="apiSearchSchema"
        :columns="apiTableColumns"
        :edit-schema="apiEditSchema"
        :before-render-form="beforeRenderApiForm"
        :before-render-detail="beforeRenderApiDetail"
        :before-submit="beforeSubmitApi"
        row-key="id"
        add-button-text="新增外部接口"
        :edit-grid-cols="2"
        modal-width="1040px"
        :hide-selection="true"
        :scroll-x="1780"
        @submit-success="handleApiSaved"
        @delete="handleApiDeleted"
      />
    </n-card>

    <n-modal v-model:show="debugModalVisible" title="接口调试" preset="card" style="width: min(920px, 92vw)" :mask-closable="false">
      <div class="debug-panel">
        <div class="debug-title">
          {{ debugApi?.apiName || '-' }}
          <NTag size="small" :type="debugApi?.apiMethod === 'GET' ? 'success' : 'info'">
            {{ debugApi?.apiMethod || '-' }}
          </NTag>
        </div>
        <n-input v-model:value="debugParamsText" type="textarea" :rows="8" placeholder="请输入 JSON 调试参数，例如：{&quot;userId&quot;:&quot;1001&quot;}" />
        <div v-if="debugResult" class="debug-result">
          <NSpace align="center">
            <NTag :type="debugResult.success ? 'success' : 'error'">
              {{ debugResult.success ? '成功' : '失败' }}
            </NTag>
            <span>HTTP: {{ debugResult.httpStatusCode ?? '-' }}</span>
            <span>耗时: {{ debugResult.durationMs ?? '-' }}ms</span>
          </NSpace>
          <pre>{{ formatJson(debugResult.responseData || debugResult.responseBody || debugResult.errorMessage) }}</pre>
        </div>
      </div>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="debugModalVisible = false">
            关闭
          </NButton>
          <NButton type="primary" :loading="debugLoading" @click="handleRunDebug">
            发送调试
          </NButton>
        </NSpace>
      </template>
    </n-modal>

    <n-modal v-model:show="docModalVisible" title="接口文档" preset="card" style="width: min(760px, 92vw)" :mask-closable="false">
      <div class="doc-panel">
        <div class="doc-current">
          <span>当前文档：</span>
          <a v-if="docApi?.docFileId" class="table-link" @click="downloadDoc">
            {{ docApi.docFileName || docApi.docFileId }}
          </a>
          <span v-else class="secondary-text">暂无文档</span>
        </div>
        <FileUpload
          v-model="docFiles"
          business-type="external_api_doc"
          :business-id="docApi?.id ? String(docApi.id) : ''"
          :limit="1"
          :multiple="false"
          :file-size="20"
          :file-type="['doc', 'docx', 'md', 'txt']"
          value-type="object"
          upload-button-text="上传接口文档"
          @remove="handleDocRemove"
          @success="handleDocUploadSuccess"
        />
      </div>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="docModalVisible = false">
            关闭
          </NButton>
          <NButton v-if="docApi?.docFileId" secondary @click="downloadDoc">
            下载文档
          </NButton>
          <NButton type="primary" :loading="docSaving" @click="handleSaveDoc">
            保存
          </NButton>
        </NSpace>
      </template>
    </n-modal>

    <n-drawer v-model:show="logDrawerVisible" :width="apiDrawerWidth" placement="right">
      <n-drawer-content :title="logDrawerTitle" closable>
        <div class="log-summary-strip">
          <div class="log-summary-card">
            <span>调用总数</span>
            <strong>{{ logSummary.totalCount || 0 }}</strong>
          </div>
          <div class="log-summary-card success">
            <span>成功率</span>
            <strong>{{ formatPercent(logSummary.successRate) }}</strong>
          </div>
          <div class="log-summary-card danger">
            <span>失败数</span>
            <strong>{{ logSummary.failureCount || 0 }}</strong>
          </div>
          <div class="log-summary-card">
            <span>平均耗时</span>
            <strong>{{ formatDuration(logSummary.avgDurationMs) }}</strong>
          </div>
          <div class="log-summary-card">
            <span>最大耗时</span>
            <strong>{{ formatDuration(logSummary.maxDurationMs) }}</strong>
          </div>
        </div>
        <AiCrudPage
          v-if="logDrawerVisible"
          ref="logCrudRef"
          api="/external/api/log"
          :api-config="logApiConfig"
          :load-detail-on-edit="true"
          :search-schema="logSearchSchema"
          :columns="logTableColumns"
          :edit-schema="logDetailSchema"
          :before-load-list="beforeLoadLogList"
          row-key="id"
          :hide-add="true"
          :hide-selection="true"
          :hide-modal-footer="true"
          :scroll-x="1500"
          modal-width="920px"
        >
          <template #toolbar-start>
            <NButton size="small" type="error" secondary @click="handleClearLogs">
              清空日志
            </NButton>
            <NButton size="small" secondary @click="handleRefreshLogs">
              刷新统计
            </NButton>
          </template>
        </AiCrudPage>
      </n-drawer-content>
    </n-drawer>

    <n-drawer v-model:show="apiDrawerVisible" :width="apiDrawerWidth" placement="right" :mask-closable="false">
      <n-drawer-content :title="apiDrawerTitle" closable>
        <AiCrudPage
          v-if="apiDrawerVisible && !isApiStandalone"
          ref="apiCrudRef"
          api="/external/api"
          :api-config="apiApiConfig"
          :load-detail-on-edit="true"
          :search-schema="apiSearchSchema"
          :columns="apiTableColumns"
          :edit-schema="apiEditSchema"
          :before-load-list="beforeLoadApiList"
          :before-render-form="beforeRenderApiForm"
          :before-render-detail="beforeRenderApiDetail"
          :before-submit="beforeSubmitApi"
          row-key="id"
          add-button-text="新增外部接口"
          :edit-grid-cols="2"
          modal-width="1040px"
          :hide-selection="true"
          :scroll-x="1780"
          @submit-success="handleApiSaved"
          @delete="handleApiDeleted"
        />
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { NButton, NSpace, NTag, NTooltip } from 'naive-ui'
import { computed, h, nextTick, onMounted, ref } from 'vue'
import { clearExternalApiLogs, debugExternalApi, getExternalApiById, getExternalApiLogSummary, updateExternalApiDocument } from '@/api/external/api'
import { getExternalSystemList } from '@/api/external/system'
import { AiCrudPage } from '@/components/ai-form'
import FileUpload from '@/components/file-upload/index.vue'
import { useDict } from '@/composables/useDict'
import { useAuthStore } from '@/store'
import { generateUUID, getFileDownloadUrl } from '@/utils'

defineOptions({ name: 'ExternalManage' })

const props = defineProps({
  initialView: {
    type: String,
    default: 'system',
  },
})

const message = window.$message
const authStore = useAuthStore()
const systemCrudRef = ref()
const apiCrudRef = ref()
const logCrudRef = ref()
const selectedSystem = ref(null)
const systemOptions = ref([])
const systemRows = ref([])
const systemCount = ref(0)
const apiDrawerVisible = ref(false)
const logDrawerVisible = ref(false)
const logApi = ref(null)
const logSummary = ref({})
const debugModalVisible = ref(false)
const debugApi = ref(null)
const debugParamsText = ref('{}')
const debugResult = ref(null)
const debugLoading = ref(false)
const docModalVisible = ref(false)
const docApi = ref(null)
const docFiles = ref([])
const docSaving = ref(false)
const EXTERNAL_AUTH_ADAPTER_DICT = 'external_auth_adapter'
const { dict } = useDict(EXTERNAL_AUTH_ADAPTER_DICT)

const isApiStandalone = computed(() => props.initialView === 'api')
const apiDrawerWidth = computed(() => window.innerWidth < 768 ? '100vw' : 'min(1120px, 92vw)')
const apiDrawerTitle = computed(() => {
  if (!selectedSystem.value) {
    return '全部外部接口'
  }
  return `外部接口 - ${selectedSystem.value.systemName || selectedSystem.value.systemCode || ''}`
})
const logDrawerTitle = computed(() => logApi.value ? `调用日志 - ${logApi.value.apiName || logApi.value.apiCode}` : '外部接口调用日志')

const systemApiConfig = {
  list: 'get@/external/system/page',
  detail: 'get@/external/system/:id',
  add: 'post@/external/system',
  update: 'put@/external/system',
  delete: 'delete@/external/system/:id',
}

const apiApiConfig = {
  list: 'get@/external/api/page',
  detail: 'get@/external/api/:id',
  add: 'post@/external/api',
  update: 'put@/external/api',
  delete: 'delete@/external/api/:id',
}

const logApiConfig = {
  list: 'get@/external/api/log/page',
  detail: 'get@/external/api/log/:id',
  delete: 'delete@/external/api/log/:id',
}

const authTypeOptions = [
  { label: '无认证', value: 'none' },
  { label: 'Basic', value: 'basic' },
  { label: 'Token', value: 'token' },
  { label: '当前用户Token透传', value: 'current_token' },
  { label: 'OAuth2', value: 'oauth2' },
  { label: 'API Key', value: 'api_key' },
  { label: '自定义认证', value: 'custom' },
]

const apiKeyPositionOptions = [
  { label: 'Header', value: 'header' },
  { label: 'Query', value: 'query' },
  { label: 'Body', value: 'body' },
]

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
]

const methodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' },
  { label: 'PATCH', value: 'PATCH' },
]

const contentTypeOptions = [
  { label: 'application/json', value: 'application/json' },
  { label: 'application/x-www-form-urlencoded', value: 'application/x-www-form-urlencoded' },
  { label: 'text/plain', value: 'text/plain' },
]

const booleanOptions = [
  { label: '是', value: true },
  { label: '否', value: false },
]

const systemCountText = computed(() => systemCount.value ? `${systemCount.value} 个系统` : '暂无系统')
const systemStats = computed(() => {
  const rows = systemRows.value || []
  return {
    total: rows.length,
    enabled: rows.filter(row => row.systemStatus === 1).length,
    apiTotal: rows.reduce((total, row) => total + (Number(row.apiCount) || 0), 0),
    authed: rows.filter(row => row.authType && row.authType !== 'none').length,
  }
})

const systemSearchSchema = [
  { field: 'systemName', label: '系统名称', type: 'input', props: { placeholder: '搜索系统名称' } },
  { field: 'systemCode', label: '系统编码', type: 'input', props: { placeholder: '搜索系统编码' } },
  { field: 'systemStatus', label: '状态', type: 'select', props: { placeholder: '全部状态', options: [{ label: '全部', value: null }, ...statusOptions] } },
]

const apiSearchSchema = [
  { field: 'apiName', label: '接口名称', type: 'input', props: { placeholder: '搜索接口名称' } },
  { field: 'apiCode', label: '接口编码', type: 'input', props: { placeholder: '搜索接口编码' } },
  { field: 'apiMethod', label: '请求方法', type: 'select', props: { placeholder: '全部方法', options: [{ label: '全部', value: null }, ...methodOptions] } },
  { field: 'apiStatus', label: '状态', type: 'select', props: { placeholder: '全部状态', options: [{ label: '全部', value: null }, ...statusOptions] } },
]

const systemTableColumns = computed(() => [
  {
    prop: 'systemName',
    label: '系统名称',
    width: 220,
    render: row => h('div', { class: 'name-cell' }, [
      h('span', { class: 'primary-text' }, row.systemName || '-'),
      h('span', { class: 'secondary-text' }, row.systemCode || '未设置编码'),
    ]),
  },
  {
    prop: 'baseUrl',
    label: '基础URL',
    width: 300,
    render: row => renderTextWithTooltip(row.baseUrl, 'url-cell'),
  },
  {
    prop: 'authType',
    label: '认证',
    width: 120,
    render: (row) => {
      const option = authTypeOptions.find(item => item.value === row.authType)
      return h(NTag, { size: 'small', type: row.authType === 'none' ? 'default' : 'info' }, { default: () => option?.label || row.authType || '-' })
    },
  },
  {
    prop: 'apiCount',
    label: '接口数',
    width: 90,
    align: 'right',
    render: row => h('a', {
      class: 'table-link',
      onClick: (event) => {
        event.stopPropagation()
        handleOpenApiDrawer(row)
      },
    }, row.apiCount ?? 0),
  },
  {
    prop: 'requestLoggingEnabled',
    label: '请求日志',
    width: 90,
    render: row => renderBooleanTag(row.requestLoggingEnabled),
  },
  {
    prop: 'trustedInternal',
    label: '内部系统',
    width: 90,
    render: row => renderBooleanTag(row.trustedInternal),
  },
  {
    prop: 'systemStatus',
    label: '状态',
    width: 80,
    render: row => renderStatusTag(row.systemStatus),
  },
  {
    prop: 'action',
    label: '操作',
    width: 230,
    fixed: 'right',
    maxActionButtons: 4,
    actions: [
      { label: '接口', key: 'apis', type: 'primary', onClick: handleOpenApiDrawer },
      { label: '日志', key: 'logs', type: 'info', onClick: handleOpenSystemLogs },
      { label: '编辑', key: 'edit', type: 'primary', onClick: row => systemCrudRef.value?.showEdit(row) },
      { label: '删除', key: 'delete', type: 'error', onClick: row => systemCrudRef.value?.handleDelete(row) },
    ],
  },
])

const apiTableColumns = computed(() => [
  {
    prop: 'apiName',
    label: '接口名称',
    width: 220,
    render: row => h('div', { class: 'name-cell' }, [
      h('span', { class: 'primary-text' }, row.apiName || '-'),
      h('span', { class: 'secondary-text' }, row.apiCode || '未设置编码'),
    ]),
  },
  { prop: 'systemName', label: '所属系统', width: 160 },
  {
    prop: 'apiMethod',
    label: '方法',
    width: 90,
    render: row => renderMethodTag(row.apiMethod),
  },
  { prop: 'requestContentType', label: '请求配置', width: 180, render: row => renderApiRequestConfig(row) },
  { prop: 'apiPath', label: '接口路径', width: 280, render: row => renderTextWithTooltip(row.apiPath, 'url-cell') },
  { prop: 'responseDataPath', label: '数据路径', width: 130, render: row => renderTextWithTooltip(row.responseDataPath || '-', 'mono-cell') },
  { prop: 'successCodes', label: '成功规则', width: 160, render: row => renderApiResponseRule(row) },
  {
    prop: 'docFileId',
    label: '文档',
    width: 80,
    render: row => renderDocTag(row),
  },
  {
    prop: 'apiStatus',
    label: '状态',
    width: 80,
    render: row => renderStatusTag(row.apiStatus),
  },
  { prop: 'sortOrder', label: '排序', width: 80, align: 'right' },
  { prop: 'createTime', label: '创建时间', width: 170 },
  {
    prop: 'action',
    label: '操作',
    width: 320,
    fixed: 'right',
    maxActionButtons: 5,
    actions: [
      { label: '调试', key: 'debug', type: 'success', onClick: handleOpenDebug },
      { label: '日志', key: 'logs', type: 'info', onClick: handleOpenLogs },
      { label: '文档', key: 'doc', type: 'primary', onClick: handleOpenDoc },
      { label: '编辑', key: 'edit', type: 'primary', onClick: row => apiCrudRef.value?.showEdit(row) },
      { label: '删除', key: 'delete', type: 'error', onClick: row => apiCrudRef.value?.handleDelete(row) },
    ],
  },
])

const logSearchSchema = [
  { field: 'callStatus', label: '调用状态', type: 'select', props: { placeholder: '全部状态', options: [{ label: '全部', value: null }, { label: '成功', value: 1 }, { label: '失败', value: 0 }] } },
  { field: 'debugFlag', label: '调用类型', type: 'select', props: { placeholder: '全部类型', options: [{ label: '全部', value: null }, { label: '调试', value: true }, { label: '正式调用', value: false }] } },
]

const logTableColumns = computed(() => [
  { prop: 'apiName', label: '接口名称', width: 180 },
  { prop: 'requestMethod', label: '方法', width: 80, render: row => renderMethodTag(row.requestMethod) },
  { prop: 'requestUrl', label: '请求地址', width: 340, render: row => renderTextWithTooltip(row.requestUrl, 'url-cell') },
  {
    prop: 'callStatus',
    label: '状态',
    width: 80,
    render: row => h(NTag, { size: 'small', type: row.callStatus === 1 ? 'success' : 'error' }, { default: () => row.callStatus === 1 ? '成功' : '失败' }),
  },
  { prop: 'httpStatusCode', label: 'HTTP', width: 80, align: 'right' },
  { prop: 'durationMs', label: '耗时', width: 100, align: 'right', render: row => renderDurationTag(row.durationMs) },
  {
    prop: 'debugFlag',
    label: '类型',
    width: 90,
    render: row => h(NTag, { size: 'small', type: row.debugFlag ? 'warning' : 'default' }, { default: () => row.debugFlag ? '调试' : '调用' }),
  },
  { prop: 'createTime', label: '调用时间', width: 170 },
  { prop: 'errorMessage', label: '错误信息', width: 220, render: row => renderTextWithTooltip(row.errorMessage || '-', 'error-cell') },
  {
    prop: 'action',
    label: '操作',
    width: 120,
    fixed: 'right',
    maxActionButtons: 2,
    actions: [
      { label: '查看', key: 'edit', type: 'primary', onClick: row => logCrudRef.value?.showEdit(row) },
      { label: '删除', key: 'delete', type: 'error', onClick: row => logCrudRef.value?.handleDelete(row) },
    ],
  },
])

const logDetailSchema = [
  { field: 'apiName', label: '接口名称', type: 'text' },
  { field: 'callStatus', label: '调用状态', type: 'text', formatter: value => value === 1 ? '成功' : '失败' },
  { field: 'requestUrl', label: '请求地址', type: 'text', span: 2 },
  { field: 'requestBody', label: '请求参数', type: 'textarea', span: 2, props: { rows: 5, readonly: true } },
  { field: 'responseBody', label: '响应内容', type: 'textarea', span: 2, props: { rows: 8, readonly: true } },
  { field: 'errorMessage', label: '错误信息', type: 'textarea', span: 2, props: { rows: 3, readonly: true } },
]

const systemEditSchema = computed(() => [
  { field: '__system_base', type: 'divider', label: '基础信息', span: 2, props: { titlePlacement: 'left' } },
  { field: 'systemName', label: '系统名称', type: 'input', required: true, props: { placeholder: '如：统一用户中心' } },
  { field: 'systemCode', label: '系统编码', type: 'input', required: true, props: { placeholder: '如：user_center' } },
  { field: 'baseUrl', label: '基础URL', type: 'input', required: true, span: 2, props: { placeholder: '如：https://api.example.com，接口路径会基于该地址拼接' } },
  { field: 'trustedInternal', label: '可信内部系统', type: 'radio', defaultValue: false, props: { options: booleanOptions } },
  { field: 'systemDesc', label: '系统描述', type: 'textarea', span: 2, props: { placeholder: '说明系统用途、负责人或调用范围', rows: 2 } },

  { field: '__system_auth', type: 'divider', label: '认证配置', span: 2, props: { titlePlacement: 'left' } },
  { field: 'authType', label: '认证类型', type: 'select', required: true, props: { options: authTypeOptions } },
  { field: 'systemStatus', label: '状态', type: 'radio', defaultValue: 1, props: { options: statusOptions } },
  { field: 'basicUsername', label: 'Basic用户名', type: 'input', vIf: form => form.authType === 'basic', props: { placeholder: 'Basic Auth username' } },
  { field: 'basicPassword', label: 'Basic密码', type: 'input', vIf: form => form.authType === 'basic', props: { type: 'password', showPasswordOn: 'click', placeholder: 'Basic Auth password' } },
  { field: 'tokenHeaderName', label: 'Token Header', type: 'input', vIf: form => form.authType === 'token', props: { placeholder: '默认 Authorization' } },
  { field: 'tokenPrefix', label: 'Token前缀', type: 'input', vIf: form => form.authType === 'token', props: { placeholder: '默认 Bearer，可为空' } },
  { field: 'tokenValue', label: 'Token值', type: 'textarea', span: 2, vIf: form => form.authType === 'token', props: { placeholder: '请输入 Token，不会在列表中展示', rows: 3 } },
  { field: 'tokenHeaderName', label: '透传Header', type: 'input', vIf: form => form.authType === 'current_token', props: { placeholder: '默认 Authorization' } },
  { field: 'tokenPrefix', label: '透传前缀', type: 'input', vIf: form => form.authType === 'current_token', props: { placeholder: '默认 Bearer，可为空' } },
  { field: 'oauth2TokenUrl', label: 'Token URL', type: 'input', span: 2, vIf: form => form.authType === 'oauth2', props: { placeholder: 'OAuth2 获取 Token 的地址' } },
  { field: 'oauth2ClientId', label: 'Client ID', type: 'input', vIf: form => form.authType === 'oauth2' },
  { field: 'oauth2ClientSecret', label: 'Client Secret', type: 'input', vIf: form => form.authType === 'oauth2', props: { type: 'password', showPasswordOn: 'click' } },
  { field: 'oauth2GrantType', label: '授权类型', type: 'input', vIf: form => form.authType === 'oauth2', props: { placeholder: '默认 client_credentials' } },
  { field: 'oauth2Scope', label: 'Scope', type: 'input', vIf: form => form.authType === 'oauth2', props: { placeholder: '可选' } },
  { field: 'apiKeyName', label: 'API Key名称', type: 'input', vIf: form => form.authType === 'api_key', props: { placeholder: '如：X-API-Key' } },
  { field: 'apiKeyPosition', label: 'API Key位置', type: 'select', vIf: form => form.authType === 'api_key', props: { options: apiKeyPositionOptions } },
  { field: 'apiKeyValue', label: 'API Key值', type: 'textarea', span: 2, vIf: form => form.authType === 'api_key', props: { rows: 3, placeholder: '请输入 API Key' } },
  {
    field: 'customAuthAdapter',
    label: '认证适配器',
    type: 'select',
    required: true,
    vIf: form => form.authType === 'custom',
    props: {
      options: dict.value[EXTERNAL_AUTH_ADAPTER_DICT] || [],
      placeholder: '请选择认证适配器（字典 external_auth_adapter）',
      filterable: true,
    },
  },
  {
    field: 'customAuthConfig',
    label: '自定义认证配置',
    type: 'slot',
    slotName: 'customAuthConfig',
    span: 2,
    vIf: form => form.authType === 'custom',
  },

  { field: '__system_network', type: 'divider', label: '网络与稳定性', span: 2, props: { titlePlacement: 'left' } },
  { field: 'connectTimeout', label: '连接超时(ms)', type: 'inputNumber', defaultValue: 5000, props: { min: 0, step: 1000 } },
  { field: 'readTimeout', label: '读取超时(ms)', type: 'inputNumber', defaultValue: 10000, props: { min: 0, step: 1000 } },
  { field: 'writeTimeout', label: '写入超时(ms)', type: 'inputNumber', defaultValue: 10000, props: { min: 0, step: 1000 } },
  { field: 'sslVerifyEnabled', label: '验证SSL证书', type: 'radio', defaultValue: true, props: { options: booleanOptions } },
  { field: 'retryEnabled', label: '启用重试', type: 'radio', defaultValue: true, props: { options: booleanOptions } },
  { field: 'requestLoggingEnabled', label: '记录请求日志', type: 'radio', defaultValue: true, props: { options: booleanOptions } },
  { field: 'retryMaxAttempts', label: '最大重试次数', type: 'inputNumber', defaultValue: 3, vIf: form => form.retryEnabled === true, props: { min: 1, step: 1 } },
  { field: 'retryBackoffInterval', label: '重试间隔(ms)', type: 'inputNumber', defaultValue: 1000, vIf: form => form.retryEnabled === true, props: { min: 0, step: 500 } },
  { field: 'proxyEnabled', label: '启用代理', type: 'radio', defaultValue: false, props: { options: booleanOptions } },
  { field: 'proxyHost', label: '代理主机', type: 'input', vIf: form => form.proxyEnabled === true, props: { placeholder: '代理服务器地址' } },
  { field: 'proxyPort', label: '代理端口', type: 'inputNumber', vIf: form => form.proxyEnabled === true, props: { min: 1, max: 65535 } },
  { field: 'proxyUsername', label: '代理用户名', type: 'input', vIf: form => form.proxyEnabled === true },
  { field: 'proxyPassword', label: '代理密码', type: 'input', vIf: form => form.proxyEnabled === true, props: { type: 'password', showPasswordOn: 'click' } },
  { field: 'remark', label: '备注', type: 'textarea', span: 2, props: { rows: 2, placeholder: '补充说明' } },
])

const apiEditSchema = computed(() => [
  { field: '__api_base', type: 'divider', label: '基础信息', span: 2, props: { titlePlacement: 'left' } },
  {
    field: 'systemNameDisplay',
    label: '所属系统',
    type: 'text',
    vIf: () => !!selectedSystem.value && !isApiStandalone.value,
    formatter: () => selectedSystem.value?.systemName || selectedSystem.value?.systemCode || '-',
  },
  {
    field: 'systemId',
    label: '所属系统',
    type: 'select',
    required: true,
    vIf: () => !selectedSystem.value || isApiStandalone.value,
    props: { options: systemOptions.value, placeholder: '请选择外部系统' },
  },
  { field: 'apiStatus', label: '状态', type: 'radio', defaultValue: 1, props: { options: statusOptions } },
  { field: 'apiName', label: '接口名称', type: 'input', required: true, props: { placeholder: '如：查询用户列表' } },
  { field: 'apiCode', label: '接口编码', type: 'input', required: true, props: { placeholder: '如：query_users' } },
  { field: 'apiMethod', label: '请求方法', type: 'select', required: true, props: { options: methodOptions } },
  { field: 'apiPath', label: '接口路径', type: 'input', required: true, props: { placeholder: '如：/api/v1/users' } },
  { field: 'apiDesc', label: '接口描述', type: 'textarea', span: 2, props: { placeholder: '说明接口用途和调用场景', rows: 2 } },

  { field: '__api_request', type: 'divider', label: '请求配置', span: 2, props: { titlePlacement: 'left' } },
  { field: 'requestContentType', label: '请求Content-Type', type: 'select', defaultValue: 'application/json', props: { options: contentTypeOptions } },
  { field: 'responseContentType', label: '响应Content-Type', type: 'select', defaultValue: 'application/json', props: { options: contentTypeOptions } },
  { field: 'requestHeaders', label: '额外请求头', type: 'textarea', span: 2, props: { rows: 3, placeholder: 'JSON 格式，如：{\"X-App\":\"forge\"}' } },
  { field: 'requestParams', label: '固定请求参数', type: 'textarea', span: 2, props: { rows: 3, placeholder: 'JSON 格式，如：{\"pageSize\":20}' } },
  { field: 'requestBodyTemplate', label: '请求体模板', type: 'textarea', span: 2, vIf: form => ['POST', 'PUT', 'PATCH'].includes(form.apiMethod), props: { rows: 4, placeholder: 'JSON 请求体模板，可使用变量占位' } },

  { field: '__api_response', type: 'divider', label: '响应提取与转换', span: 2, props: { titlePlacement: 'left' } },
  { field: 'responseDataPath', label: '数据路径', type: 'input', props: { placeholder: '如：data.records 或 $.data.records' } },
  { field: 'responseTotalPath', label: '总数路径', type: 'input', props: { placeholder: '如：data.total 或 $.data.total，用于分页接口' } },
  { field: 'paramMappingEnabled', label: '启用参数映射', type: 'radio', defaultValue: false, props: { options: booleanOptions } },
  { field: 'responseTransformEnabled', label: '启用响应转换', type: 'radio', defaultValue: false, props: { options: booleanOptions } },
  { field: 'paramMappings', label: '参数映射规则', type: 'textarea', span: 2, vIf: form => form.paramMappingEnabled === true, props: { rows: 4, placeholder: 'JSON 格式，如：{\"pageNum\":\"page\",\"keyword\":{\"target\":\"q\",\"defaultValue\":\"\"}}' } },
  { field: 'responseTransformScript', label: '响应转换脚本', type: 'textarea', span: 2, vIf: form => form.responseTransformEnabled === true, props: { rows: 5, placeholder: 'JavaScript 转换脚本' } },
  { field: 'errorCodePath', label: '错误码路径', type: 'input', props: { placeholder: '如：code 或 $.code' } },
  { field: 'errorMsgPath', label: '错误消息路径', type: 'input', props: { placeholder: '如：message 或 $.message' } },
  { field: 'successCodes', label: '成功码列表', type: 'input', defaultValue: '0,200', props: { placeholder: '如：0,200' } },
  { field: 'sortOrder', label: '排序', type: 'inputNumber', defaultValue: 0, props: { min: 0, step: 1 } },

  { field: '__api_guard', type: 'divider', label: '限流、缓存与权限', span: 2, props: { titlePlacement: 'left' } },
  { field: 'rateLimitEnabled', label: '启用限流', type: 'radio', defaultValue: false, props: { options: booleanOptions } },
  { field: 'rateLimitQps', label: '限流QPS', type: 'inputNumber', defaultValue: 10, vIf: form => form.rateLimitEnabled === true, props: { min: 1, step: 1 } },
  { field: 'cacheEnabled', label: '启用缓存', type: 'radio', defaultValue: false, props: { options: booleanOptions } },
  { field: 'cacheTtl', label: '缓存时长(秒)', type: 'inputNumber', defaultValue: 300, vIf: form => form.cacheEnabled === true, props: { min: 1, step: 60 } },
  { field: 'cacheKeyTemplate', label: '缓存Key模板', type: 'input', span: 2, vIf: form => form.cacheEnabled === true, props: { placeholder: '如：external:user:{userId}' } },
  { field: 'permissionCheckEnabled', label: '启用权限校验', type: 'radio', defaultValue: true, props: { options: booleanOptions } },
  { field: 'requiredPermission', label: '所需权限标识', type: 'input', vIf: form => form.permissionCheckEnabled === true, props: { placeholder: '如：external:user:list' } },
  { field: 'remark', label: '备注', type: 'textarea', span: 2, props: { rows: 2, placeholder: '补充说明' } },
])

function renderStatusTag(status) {
  const enabled = status === 1
  return h(NTag, { type: enabled ? 'success' : 'error', size: 'small' }, { default: () => enabled ? '启用' : '停用' })
}

function renderMethodTag(method) {
  const typeMap = {
    GET: 'success',
    POST: 'info',
    PUT: 'warning',
    DELETE: 'error',
    PATCH: 'default',
  }
  return h(NTag, { type: typeMap[method || ''] || 'default', size: 'small' }, { default: () => method || '-' })
}

function renderBooleanTag(value) {
  return h(NTag, { size: 'small', type: value === false ? 'default' : 'success' }, { default: () => value === false ? '关闭' : '开启' })
}

function renderDocTag(row) {
  return h(NTag, { size: 'small', type: row.docFileId ? 'success' : 'default' }, { default: () => row.docFileId ? '已维护' : '未维护' })
}

function renderApiRequestConfig(row) {
  const hasExtraConfig = row.requestHeaders || row.requestParams || row.requestBodyTemplate || row.paramMappingEnabled
  return h('div', { class: 'name-cell' }, [
    h('span', { class: 'primary-text' }, row.requestContentType || '-'),
    h('span', { class: 'secondary-text' }, hasExtraConfig ? '已配置请求规则' : '默认透传参数'),
  ])
}

function renderApiResponseRule(row) {
  const ruleText = row.errorCodePath ? `错误码: ${row.errorCodePath}` : `HTTP ${row.successCodes || '2xx'}`
  return h('div', { class: 'name-cell' }, [
    h('span', { class: 'primary-text' }, ruleText),
    h('span', { class: 'secondary-text' }, row.responseTransformEnabled ? '已启用响应转换' : '未转换'),
  ])
}

function renderDurationTag(duration) {
  const value = Number(duration) || 0
  const type = value > 3000 ? 'error' : value > 1000 ? 'warning' : 'success'
  return h(NTag, { size: 'small', type }, { default: () => formatDuration(value) })
}

function renderTextWithTooltip(value, className = 'truncate-cell') {
  const text = value || '-'
  return h(NTooltip, { trigger: 'hover' }, {
    trigger: () => h('span', { class: className }, text),
    default: () => text,
  })
}

function beforeRenderSystemList(list) {
  systemRows.value = list
  mergeSystemOptions(list)
  return list
}

function handleSystemListLoaded({ list, total }) {
  systemRows.value = list || []
  systemCount.value = Number(total || 0)
}

function getDefaultSystemForm() {
  return {
    authType: 'none',
    systemStatus: 1,
    proxyEnabled: false,
    trustedInternal: false,
    retryEnabled: true,
    retryMaxAttempts: 3,
    retryBackoffInterval: 1000,
    connectTimeout: 5000,
    readTimeout: 10000,
    writeTimeout: 10000,
    sslVerifyEnabled: true,
    requestLoggingEnabled: true,
  }
}

function beforeRenderSystemForm(row) {
  return formatSystemForm(row || getDefaultSystemForm())
}

function beforeRenderSystemDetail(data) {
  return formatSystemForm({
    ...getDefaultSystemForm(),
    ...data,
  })
}

function beforeSubmitSystem(formData) {
  const payload = cleanSystemAuthFields(formData)
  if (payload.authType === 'custom') {
    if (!payload.customAuthAdapter) {
      message.error('请选择认证适配器')
      return false
    }
    const customAuthConfig = configRowsToJson(payload.customAuthConfig, '自定义认证配置')
    if (customAuthConfig === false) {
      return false
    }
    payload.customAuthConfig = customAuthConfig
  }
  return trimPayload(payload)
}

function beforeLoadApiList(params) {
  if (selectedSystem.value?.id) {
    return { ...params, systemId: selectedSystem.value.id }
  }
  return params
}

function beforeLoadLogList(params) {
  const nextParams = buildLogParams(params)
  loadLogSummary(nextParams)
  return nextParams
}

function buildLogParams(params = {}) {
  const nextParams = { ...params }
  if (logApi.value?.scopeType === 'system') {
    nextParams.systemId = logApi.value.systemId
    return nextParams
  }
  if (logApi.value?.id) {
    nextParams.apiId = logApi.value.id
  }
  if (logApi.value?.systemId) {
    nextParams.systemId = logApi.value.systemId
  }
  return nextParams
}

function getDefaultApiForm() {
  const systemId = getSelectedSystemId()
  return {
    systemId,
    systemNameDisplay: selectedSystem.value?.systemName || selectedSystem.value?.systemCode || '',
    apiMethod: 'GET',
    requestContentType: 'application/json',
    responseContentType: 'application/json',
    paramMappingEnabled: false,
    responseTransformEnabled: false,
    successCodes: '0,200',
    rateLimitEnabled: false,
    rateLimitQps: 10,
    cacheEnabled: false,
    cacheTtl: 300,
    permissionCheckEnabled: true,
    apiStatus: 1,
    sortOrder: 0,
  }
}

function beforeRenderApiForm(row) {
  if (!row) {
    return getDefaultApiForm()
  }
  const systemId = getSelectedSystemId()
  return {
    ...row,
    systemId: systemId || row.systemId,
    systemNameDisplay: selectedSystem.value?.systemName || row.systemName || '',
  }
}

function beforeRenderApiDetail(data) {
  const systemId = getSelectedSystemId()
  return {
    ...getDefaultApiForm(),
    ...data,
    systemId: systemId || data?.systemId,
    systemNameDisplay: selectedSystem.value?.systemName || data?.systemName || '',
  }
}

function beforeSubmitApi(formData) {
  const payload = { ...formData }
  const systemId = getSelectedSystemId()
  if (systemId && !isApiStandalone.value) {
    payload.systemId = systemId
  }
  if (!payload.systemId) {
    message.error('请选择所属系统')
    return false
  }
  const jsonFields = [
    ['requestHeaders', '额外请求头'],
    ['requestParams', '固定请求参数'],
    ['paramMappings', '参数映射规则'],
  ]
  for (const [field, label] of jsonFields) {
    if (!validateJson(payload[field], label)) {
      return false
    }
  }
  if (payload.requestContentType === 'application/json' && !validateJson(payload.requestBodyTemplate, '请求体模板')) {
    return false
  }
  return trimPayload(payload)
}

function getSelectedSystemId() {
  return selectedSystem.value?.id || selectedSystem.value?.systemId || null
}

function handleOpenDebug(row) {
  debugApi.value = row
  debugParamsText.value = formatDebugParams(row.requestParams)
  debugResult.value = null
  debugModalVisible.value = true
}

function formatDebugParams(value) {
  if (!value || typeof value !== 'string') {
    return '{}'
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  }
  catch {
    return '{}'
  }
}

async function handleRunDebug() {
  let params = {}
  try {
    params = debugParamsText.value?.trim() ? JSON.parse(debugParamsText.value) : {}
    if (!params || Array.isArray(params) || typeof params !== 'object') {
      message.error('调试参数必须是 JSON 对象')
      return
    }
  }
  catch {
    message.error('调试参数必须是合法 JSON')
    return
  }
  debugLoading.value = true
  try {
    const res = await debugExternalApi(debugApi.value.id, params)
    debugResult.value = res.data
    logCrudRef.value?.refresh()
  }
  finally {
    debugLoading.value = false
  }
}

function handleOpenLogs(row) {
  logApi.value = row
  logDrawerVisible.value = true
  const params = buildLogParams()
  loadLogSummary(params)
  nextTick(() => logCrudRef.value?.refresh())
}

function handleOpenSystemLogs(row) {
  logApi.value = {
    scopeType: 'system',
    systemId: row.id,
    apiName: row.systemName || row.systemCode,
  }
  logDrawerVisible.value = true
  const params = buildLogParams()
  loadLogSummary(params)
  nextTick(() => logCrudRef.value?.refresh())
}

async function loadLogSummary(params = {}) {
  try {
    const res = await getExternalApiLogSummary(params)
    logSummary.value = res.data || {}
  }
  catch (error) {
    console.error('加载外部接口日志汇总失败', error)
  }
}

function handleClearLogs() {
  const targetName = logApi.value?.apiName || logApi.value?.apiCode
  const targetLabel = logApi.value?.scopeType === 'system' ? '系统' : '接口'
  window.$dialog.warning({
    title: '确认清空',
    content: logApi.value ? `确定清空${targetLabel}「${targetName}」的调用日志吗？` : '确定清空调用日志吗？',
    positiveText: '清空',
    negativeText: '取消',
    onPositiveClick: async () => {
      await clearExternalApiLogs({
        apiId: logApi.value?.id,
        systemId: logApi.value?.systemId,
      })
      message.success('清空成功')
      loadLogSummary(buildLogParams())
      logCrudRef.value?.refresh()
    },
  })
}

function handleRefreshLogs() {
  loadLogSummary(buildLogParams())
  logCrudRef.value?.refresh()
}

async function handleOpenDoc(row) {
  const res = await getExternalApiById(row.id)
  docApi.value = res.data || row
  docFiles.value = docApi.value?.docFileId
    ? [{
        id: docApi.value.docFileId,
        fileId: docApi.value.docFileId,
        originalName: docApi.value.docFileName || docApi.value.docFileId,
        name: docApi.value.docFileName || docApi.value.docFileId,
      }]
    : []
  docModalVisible.value = true
}

function handleDocUploadSuccess(fileData) {
  if (!docApi.value) {
    return
  }
  docApi.value.docFileId = fileData.fileId
  docApi.value.docFileName = fileData.originalName || fileData.fileName || fileData.fileId
}

function handleDocRemove() {
  if (!docApi.value) {
    return
  }
  docApi.value.docFileId = ''
  docApi.value.docFileName = ''
}

async function handleSaveDoc() {
  if (!docApi.value) {
    return
  }
  const currentFile = Array.isArray(docFiles.value) ? docFiles.value[0] : null
  docApi.value.docFileId = currentFile?.fileId || currentFile?.url || ''
  docApi.value.docFileName = currentFile?.originalName || currentFile?.fileName || currentFile?.name || ''
  docSaving.value = true
  try {
    await updateExternalApiDocument(docApi.value)
    message.success('接口文档保存成功')
    docModalVisible.value = false
    apiCrudRef.value?.refresh()
  }
  finally {
    docSaving.value = false
  }
}

async function downloadDoc() {
  if (!docApi.value?.docFileId) {
    message.warning('暂无可下载文档')
    return
  }
  try {
    const response = await fetch(getFileDownloadUrl(docApi.value.docFileId), {
      method: 'GET',
      headers: {
        'Authorization': authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
        'X-Timestamp': Date.now().toString(),
        'X-Nonce': generateUUID(),
      },
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(errorText || '下载失败')
    }

    const blob = await response.blob()
    const blobUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = getDownloadFileName(response, docApi.value.docFileName || docApi.value.docFileId)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    setTimeout(() => URL.revokeObjectURL(blobUrl), 100)
  }
  catch (error) {
    message.error(`下载失败：${error.message || '未知错误'}`)
  }
}

function getDownloadFileName(response, fallbackName) {
  const contentDisposition = response.headers.get('Content-Disposition')
  if (!contentDisposition) {
    return fallbackName
  }
  const utf8Match = contentDisposition.match(/filename\*=utf-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }
  const normalMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  if (normalMatch?.[1]) {
    return decodeURIComponent(normalMatch[1])
  }
  return fallbackName
}

function formatPercent(value) {
  return `${Number(value || 0).toFixed(2)}%`
}

function formatDuration(value) {
  const duration = Number(value || 0)
  if (duration >= 1000) {
    return `${(duration / 1000).toFixed(2)}s`
  }
  return `${Math.round(duration)}ms`
}

function formatJson(value) {
  if (value === null || value === undefined) {
    return ''
  }
  if (typeof value === 'string') {
    try {
      return JSON.stringify(JSON.parse(value), null, 2)
    }
    catch {
      return value
    }
  }
  return JSON.stringify(value, null, 2)
}

function handleOpenApiDrawer(row) {
  selectedSystem.value = row
  apiDrawerVisible.value = true
  nextTick(() => {
    apiCrudRef.value?.setSearchParams({})
    apiCrudRef.value?.refresh()
  })
}

function showAllApis() {
  selectedSystem.value = null
  apiDrawerVisible.value = true
  nextTick(() => apiCrudRef.value?.refresh())
}

async function handleSystemSaved() {
  await loadSystemOptions()
  systemCrudRef.value?.refresh()
  apiCrudRef.value?.refresh()
}

async function handleSystemDeleted() {
  selectedSystem.value = null
  await loadSystemOptions()
  apiCrudRef.value?.refresh()
}

function handleApiSaved() {
  systemCrudRef.value?.refresh()
}

function handleApiDeleted() {
  systemCrudRef.value?.refresh()
}

async function loadSystemOptions() {
  try {
    const res = await getExternalSystemList()
    mergeSystemOptions(res.data || [])
  }
  catch (error) {
    console.error('加载外部系统列表失败', error)
  }
}

function mergeSystemOptions(list) {
  const optionMap = new Map()
  systemOptions.value.forEach(item => optionMap.set(item.value, item))
  list.forEach((item) => {
    if (item.id) {
      optionMap.set(item.id, {
        label: item.systemCode ? `${item.systemName}（${item.systemCode}）` : item.systemName,
        value: item.id,
      })
    }
  })
  systemOptions.value = Array.from(optionMap.values())
}

function validateJson(value, label) {
  if (typeof value !== 'string' || value.trim() === '') {
    return true
  }
  try {
    JSON.parse(value)
    return true
  }
  catch {
    message.error(`${label} 必须是合法 JSON`)
    return false
  }
}

function formatSystemForm(data) {
  return {
    ...data,
    customAuthConfig: jsonToConfigRows(data.customAuthConfig),
  }
}

function configRowsToJson(value, label) {
  const rows = normalizeConfigRows(value)
  const filledRows = rows.filter(row => row.key || row.value)
  if (filledRows.length === 0) {
    return '{}'
  }
  const result = {}
  for (const row of filledRows) {
    const key = row.key?.trim()
    if (!key) {
      message.error(`${label} 存在未填写 KEY 的配置项`)
      return false
    }
    result[key] = row.value ?? ''
  }
  return JSON.stringify(result)
}

function jsonToConfigRows(value) {
  if (typeof value !== 'string' || value.trim() === '') {
    return [createConfigRow()]
  }
  try {
    const parsed = JSON.parse(value)
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      return [createConfigRow()]
    }
    const rows = Object.entries(parsed).map(([key, itemValue]) => ({
      key,
      value: formatKeyValueItem(itemValue),
    }))
    return rows.length ? rows : [createConfigRow()]
  }
  catch {
    return [createConfigRow()]
  }
}

function normalizeConfigRows(value) {
  if (!Array.isArray(value) || value.length === 0) {
    return [createConfigRow()]
  }
  return value.map(row => ({
    key: row?.key ?? '',
    value: row?.value ?? '',
  }))
}

function createConfigRow() {
  return { key: '', value: '' }
}

function updateConfigRow(value, updateValue, index, field, fieldValue) {
  const rows = normalizeConfigRows(value)
  rows[index] = {
    ...rows[index],
    [field]: fieldValue,
  }
  updateValue(rows)
}

function addConfigRow(value, updateValue) {
  updateValue([...normalizeConfigRows(value), createConfigRow()])
}

function removeConfigRow(value, updateValue, index) {
  const rows = normalizeConfigRows(value).filter((_, rowIndex) => rowIndex !== index)
  updateValue(rows.length ? rows : [createConfigRow()])
}

function formatKeyValueItem(value) {
  if (value === null || value === undefined) {
    return ''
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

function trimPayload(data) {
  const payload = { ...data }
  Object.keys(payload).forEach((key) => {
    if (typeof payload[key] === 'string') {
      payload[key] = payload[key].trim()
    }
  })
  return payload
}

function cleanSystemAuthFields(data) {
  const payload = { ...data }
  if (payload.authType !== 'basic') {
    payload.basicUsername = undefined
    payload.basicPassword = undefined
  }
  if (payload.authType !== 'token' && payload.authType !== 'current_token') {
    payload.tokenValue = undefined
    payload.tokenHeaderName = undefined
    payload.tokenPrefix = undefined
  }
  if (payload.authType === 'current_token') {
    payload.tokenValue = undefined
  }
  if (payload.authType !== 'oauth2') {
    payload.oauth2TokenUrl = undefined
    payload.oauth2ClientId = undefined
    payload.oauth2ClientSecret = undefined
    payload.oauth2GrantType = undefined
    payload.oauth2Scope = undefined
  }
  if (payload.authType !== 'api_key') {
    payload.apiKeyName = undefined
    payload.apiKeyValue = undefined
    payload.apiKeyPosition = undefined
  }
  if (payload.authType !== 'custom') {
    payload.customAuthAdapter = undefined
    payload.customAuthConfig = undefined
  }
  if (!payload.proxyEnabled) {
    payload.proxyHost = undefined
    payload.proxyPort = undefined
    payload.proxyUsername = undefined
    payload.proxyPassword = undefined
  }
  return payload
}

onMounted(() => {
  loadSystemOptions()
})
</script>

<style lang="scss" scoped>
.external-manage-page {
  padding: 16px;
}

.panel-card {
  min-height: calc(100vh - 120px);
  border-radius: 16px;
  box-shadow: 0 14px 40px rgba(31, 41, 55, 0.08);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.overview-strip,
.log-summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.log-summary-strip {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.overview-card,
.log-summary-card {
  position: relative;
  overflow: hidden;
  padding: 14px 16px;
  border: 1px solid rgba(42, 87, 154, 0.08);
  border-radius: 14px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(244, 248, 255, 0.78)),
    radial-gradient(circle at 100% 0, rgba(24, 144, 255, 0.14), transparent 34%);
}

.overview-card strong,
.log-summary-card strong {
  display: block;
  margin-top: 6px;
  color: var(--text-primary);
  font-size: 22px;
  line-height: 1;
}

.overview-label,
.log-summary-card span {
  color: var(--text-tertiary);
  font-size: 12px;
}

.log-summary-card.success {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(239, 253, 246, 0.84)),
    radial-gradient(circle at 100% 0, rgba(24, 160, 88, 0.16), transparent 36%);
}

.log-summary-card.danger {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(255, 246, 246, 0.86)),
    radial-gradient(circle at 100% 0, rgba(208, 48, 80, 0.14), transparent 36%);
}

.panel-subtitle {
  margin-top: 4px;
  color: var(--text-tertiary);
  font-size: 12px;
  font-weight: 400;
}

:deep(.name-cell) {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

:deep(.primary-text) {
  color: var(--text-primary);
  font-weight: 600;
}

:deep(.secondary-text) {
  color: var(--text-tertiary);
  font-size: 12px;
}

:deep(.table-link) {
  color: var(--primary-color);
  cursor: pointer;
  font-weight: 600;
}

:deep(.url-cell),
:deep(.mono-cell),
:deep(.error-cell) {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}

:deep(.url-cell) {
  color: #2f5f9f;
}

:deep(.error-cell) {
  color: var(--error-color);
}

.kv-config-editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.kv-config-row {
  display: grid;
  grid-template-columns: minmax(160px, 0.8fr) minmax(220px, 1.2fr) 64px;
  gap: 8px;
  align-items: center;
}

.debug-panel,
.doc-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.debug-title,
.doc-current {
  display: flex;
  align-items: center;
  gap: 8px;
}

.debug-result {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.debug-result pre {
  max-height: 360px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border-radius: 10px;
  background: #0f172a;
  color: #d1e7ff;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .overview-strip,
  .log-summary-strip {
    grid-template-columns: 1fr 1fr;
  }

  .kv-config-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .panel-card {
    min-height: auto;
  }
}
</style>
