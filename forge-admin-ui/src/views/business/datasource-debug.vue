<template>
  <div class="business-datasource-debug-page">
    <!-- 工具栏 -->
    <section class="debug-toolbar">
      <div class="debug-toolbar-title">
        <h2>业务数据源调试</h2>
        <span class="debug-toolbar-desc">验证租户默认业务库路由、写入与读取是否落到正确数据源</span>
      </div>
      <n-space>
        <n-button :loading="tenantLoading" @click="loadTenants">
          <template #icon>
            <i class="i-material-symbols:refresh" />
          </template>
          刷新租户
        </n-button>
        <n-button type="primary" :loading="currentLoading" @click="loadCurrentRoute">
          <template #icon>
            <i class="i-material-symbols:route" />
          </template>
          检测路由
        </n-button>
      </n-space>
    </section>

    <!-- 上下文 + 路由结果 -->
    <div class="debug-layout">
      <section class="debug-panel">
        <div class="panel-title-row">
          <h3>调试上下文</h3>
          <NTag :type="selectedTenant?.defaultBusinessDatasourceCode ? 'success' : 'default'" size="small">
            {{ displayDatasourceCode(selectedTenant?.defaultBusinessDatasourceCode) }}
          </NTag>
        </div>

        <n-form label-placement="top" :show-feedback="false" class="debug-form">
          <n-form-item label="租户">
            <n-select
              v-model:value="form.tenantId"
              filterable
              clearable
              :loading="tenantLoading"
              :options="tenantOptions"
              placeholder="选择租户"
              @update:value="handleTenantChange"
            />
          </n-form-item>

          <n-grid :cols="2" :x-gap="12" :y-gap="4">
            <n-form-item-gi label="写入标题">
              <n-input
                v-model:value="form.title"
                clearable
                maxlength="128"
                placeholder="测试记录标题"
              />
            </n-form-item-gi>
            <n-form-item-gi label="读取数量">
              <n-input-number
                v-model:value="form.limit"
                :min="1"
                :max="100"
                style="width: 100%"
              />
            </n-form-item-gi>
          </n-grid>
        </n-form>

        <div class="control-actions">
          <n-button type="primary" :loading="prepareLoading" @click="prepareRecord">
            <template #icon>
              <i class="i-material-symbols:add-circle-outline" />
            </template>
            写入测试记录
          </n-button>
          <n-button :loading="listLoading" @click="loadRecords">
            <template #icon>
              <i class="i-material-symbols:format-list-bulleted" />
            </template>
            读取记录
          </n-button>
        </div>

        <div class="tenant-config-box">
          <div class="config-row">
            <span>租户 ID</span>
            <strong>{{ selectedTenant?.id || form.tenantId || '-' }}</strong>
          </div>
          <div class="config-row">
            <span>租户名称</span>
            <strong>{{ selectedTenant?.tenantName || '-' }}</strong>
          </div>
          <div class="config-row">
            <span>默认业务数据源 ID</span>
            <strong>{{ selectedTenant?.defaultBusinessDatasourceId || '-' }}</strong>
          </div>
          <div class="config-row">
            <span>默认业务数据源编码</span>
            <strong>{{ displayDatasourceCode(selectedTenant?.defaultBusinessDatasourceCode) }}</strong>
          </div>
        </div>

        <n-alert v-if="errorMessage" type="error" :show-icon="false" class="debug-error">
          {{ errorMessage }}
        </n-alert>
      </section>

      <section class="debug-panel">
        <div class="panel-title-row">
          <h3>路由结果</h3>
          <NTag :type="datasourceTagType" size="small">
            {{ routeModeText }}
          </NTag>
        </div>

        <n-descriptions
          :column="2"
          size="small"
          label-placement="left"
          bordered
          class="route-descriptions"
        >
          <n-descriptions-item label="当前数据库">
            {{ routeResult?.databaseName || '-' }}
          </n-descriptions-item>
          <n-descriptions-item label="路由状态">
            {{ routeModeText }}
          </n-descriptions-item>
          <n-descriptions-item label="解析数据源键">
            {{ datasource.dsKey || '-' }}
          </n-descriptions-item>
          <n-descriptions-item label="线程数据源键">
            {{ currentDsKey }}
          </n-descriptions-item>
          <n-descriptions-item label="数据源编码">
            {{ displayDatasourceCode(datasource.datasourceCode) }}
          </n-descriptions-item>
          <n-descriptions-item label="数据源名称">
            {{ datasource.datasourceName || '-' }}
          </n-descriptions-item>
          <n-descriptions-item label="数据源 ID">
            {{ datasource.datasourceId || '-' }}
          </n-descriptions-item>
          <n-descriptions-item label="允许写入">
            <NTag :type="datasource.allowWrite ? 'success' : 'default'" size="small">
              {{ datasource.allowWrite ? '是' : '否' }}
            </NTag>
          </n-descriptions-item>
        </n-descriptions>

        <div v-if="latestRecord" class="latest-record">
          <div class="latest-title">
            最近写入
          </div>
          <div class="latest-content">
            <span>{{ latestRecord.title }}</span>
            <NTag size="small" type="info">
              {{ displayDatasourceCode(latestRecord.routeKey) }}
            </NTag>
          </div>
        </div>
      </section>
    </div>

    <!-- 演示表记录 -->
    <section class="debug-panel records-panel">
      <div class="panel-title-row">
        <h3>演示表记录</h3>
        <NTag size="small" type="info">
          {{ records.length }} 条
        </NTag>
      </div>

      <n-data-table
        :columns="recordColumns"
        :data="records"
        :loading="tableLoading"
        :row-key="row => row.id"
        size="small"
        striped
        :bordered="false"
        :scroll-x="980"
      />
    </section>
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import { request } from '@/utils'

defineOptions({ name: 'BusinessDatasourceDebug' })

const tenants = ref([])
const routeResult = ref(null)
const errorMessage = ref('')
const tenantLoading = ref(false)
const currentLoading = ref(false)
const prepareLoading = ref(false)
const listLoading = ref(false)

const form = reactive({
  tenantId: null,
  title: buildDefaultTitle(),
  limit: 10,
})

const tenantOptions = computed(() => tenants.value.map(item => ({
  label: `${item.tenantName || `租户 ${item.id}`}（${displayDatasourceCode(item.defaultBusinessDatasourceCode)}）`,
  value: item.id,
})))

const selectedTenant = computed(() => tenants.value.find(item => item.id === form.tenantId) || null)
const datasource = computed(() => routeResult.value?.datasource || {})
const records = computed(() => routeResult.value?.records || [])
const latestRecord = computed(() => routeResult.value?.record || records.value[0] || null)
const tableLoading = computed(() => currentLoading.value || prepareLoading.value || listLoading.value)
const datasourceTagType = computed(() => datasource.value.master ? 'default' : 'success')
const routeModeText = computed(() => datasource.value.master ? '主库' : '业务库')
const currentDsKey = computed(() => routeResult.value?.currentDsKey || datasource.value.dsKey || 'master')

const recordColumns = [
  {
    title: 'ID',
    key: 'id',
    width: 180,
    ellipsis: true,
  },
  {
    title: '租户 ID',
    key: 'tenantId',
    width: 100,
  },
  {
    title: '标题',
    key: 'title',
    minWidth: 220,
    ellipsis: true,
  },
  {
    title: '路由键',
    key: 'routeKey',
    width: 160,
    render: row => h(NTag, {
      size: 'small',
      type: row.routeKey ? 'success' : 'default',
    }, {
      default: () => displayDatasourceCode(row.routeKey),
    }),
  },
  {
    title: '创建人',
    key: 'createBy',
    width: 100,
    render: row => row.createBy || '-',
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 180,
    render: row => formatDateTime(row.createTime),
  },
]

onMounted(async () => {
  await loadTenants()
  await loadCurrentRoute()
})

function displayDatasourceCode(code) {
  if (!code || code === 'master')
    return '主库'
  return code
}

async function loadTenants() {
  tenantLoading.value = true
  try {
    const res = await request.get('/system/tenant/assignable/options')
    tenants.value = res.data || []
    if (!form.tenantId && tenants.value.length > 0) {
      form.tenantId = tenants.value[0].id
    }
  }
  catch (error) {
    handleError(error, '加载租户失败')
  }
  finally {
    tenantLoading.value = false
  }
}

async function handleTenantChange() {
  await loadCurrentRoute()
}

async function loadCurrentRoute() {
  await runRequest(currentLoading, async () => {
    const res = await request.get('/business/datasource-demo/current', { params: buildTenantParams() })
    routeResult.value = res.data
  }, '检测路由失败')
}

async function prepareRecord() {
  await runRequest(prepareLoading, async () => {
    const res = await request.post('/business/datasource-demo/prepare', null, {
      params: {
        ...buildTenantParams(),
        title: form.title || buildDefaultTitle(),
      },
    })
    routeResult.value = res.data
    form.title = buildDefaultTitle()
    window.$message?.success('写入成功')
  }, '写入测试记录失败')
}

async function loadRecords() {
  await runRequest(listLoading, async () => {
    const res = await request.get('/business/datasource-demo/list', {
      params: {
        ...buildTenantParams(),
        limit: form.limit || 10,
      },
    })
    routeResult.value = res.data
  }, '读取记录失败')
}

async function runRequest(loadingRef, action, fallbackMessage) {
  loadingRef.value = true
  errorMessage.value = ''
  try {
    await action()
  }
  catch (error) {
    handleError(error, fallbackMessage)
  }
  finally {
    loadingRef.value = false
  }
}

function buildTenantParams() {
  return form.tenantId ? { tenantId: form.tenantId } : {}
}

function handleError(error, fallbackMessage) {
  const message = error?.message || error?.detail?.message || fallbackMessage
  errorMessage.value = message
  window.$message?.error(message)
}

function buildDefaultTitle() {
  return `路由验证 ${formatDateTime(new Date())}`
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped>
.business-datasource-debug-page {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 100%;
  background: var(--bg-secondary);
}

.debug-toolbar,
.debug-panel {
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-primary);
}

.debug-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
}

.debug-toolbar-title {
  min-width: 0;
}

.debug-toolbar h2,
.panel-title-row h3 {
  margin: 0;
  color: var(--text-primary);
  font-weight: 600;
}

.debug-toolbar h2 {
  font-size: 16px;
  line-height: 1.4;
}

.debug-toolbar-desc {
  display: block;
  margin-top: 4px;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.4;
}

.debug-layout {
  display: grid;
  grid-template-columns: minmax(300px, 380px) minmax(0, 1fr);
  gap: 8px;
  min-height: 0;
}

.debug-panel {
  padding: 12px 14px;
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-title-row h3 {
  font-size: 14px;
}

.debug-form :deep(.n-form-item) {
  margin-bottom: 12px;
}

.control-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tenant-config-box {
  display: grid;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-secondary);
}

.config-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 13px;
}

.config-row strong {
  min-width: 0;
  color: var(--text-primary);
  font-weight: 600;
  text-align: right;
  word-break: break-all;
}

.debug-error {
  margin-top: 12px;
}

.route-descriptions {
  margin-top: 0;
}

.latest-record {
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-secondary);
}

.latest-title {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.latest-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

.records-panel {
  min-height: 280px;
}

@media (max-width: 1180px) {
  .debug-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .debug-toolbar,
  .panel-title-row,
  .latest-content {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
