<template>
  <div class="business-datasource-debug-page">
    <section class="debug-header">
      <div>
        <p class="debug-kicker">
          Tenant Business Datasource
        </p>
        <h2>业务数据源调试</h2>
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

    <div class="debug-layout">
      <section class="debug-panel control-panel">
        <div class="panel-title-row">
          <div>
            <p class="panel-kicker">
              Context
            </p>
            <h3>调试上下文</h3>
          </div>
          <NTag :type="selectedTenant?.defaultBusinessDatasourceCode ? 'success' : 'default'" size="small">
            {{ selectedTenant?.defaultBusinessDatasourceCode || 'master' }}
          </NTag>
        </div>

        <n-form label-placement="top" :show-feedback="false">
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

          <n-grid :cols="2" :x-gap="12">
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
            <span>租户ID</span>
            <strong>{{ selectedTenant?.id || form.tenantId || '-' }}</strong>
          </div>
          <div class="config-row">
            <span>租户名称</span>
            <strong>{{ selectedTenant?.tenantName || '-' }}</strong>
          </div>
          <div class="config-row">
            <span>默认业务数据源ID</span>
            <strong>{{ selectedTenant?.defaultBusinessDatasourceId || '-' }}</strong>
          </div>
          <div class="config-row">
            <span>默认业务数据源编码</span>
            <strong>{{ selectedTenant?.defaultBusinessDatasourceCode || 'master' }}</strong>
          </div>
        </div>

        <n-alert v-if="errorMessage" type="error" :show-icon="false" class="debug-error">
          {{ errorMessage }}
        </n-alert>
      </section>

      <section class="debug-panel result-panel">
        <div class="panel-title-row">
          <div>
            <p class="panel-kicker">
              Runtime
            </p>
            <h3>路由结果</h3>
          </div>
          <NTag :type="datasourceTagType" size="small">
            {{ routeModeText }}
          </NTag>
        </div>

        <div class="metric-grid">
          <div v-for="item in routeMetrics" :key="item.key" class="metric-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>

        <n-descriptions
          :column="2"
          size="small"
          label-placement="left"
          bordered
          class="route-descriptions"
        >
          <n-descriptions-item label="解析 dsKey">
            {{ datasource.dsKey || '-' }}
          </n-descriptions-item>
          <n-descriptions-item label="线程 dsKey">
            {{ currentDsKey }}
          </n-descriptions-item>
          <n-descriptions-item label="数据源编码">
            {{ datasource.datasourceCode || '-' }}
          </n-descriptions-item>
          <n-descriptions-item label="数据源名称">
            {{ datasource.datasourceName || '-' }}
          </n-descriptions-item>
          <n-descriptions-item label="数据源ID">
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
              {{ latestRecord.routeKey || 'master' }}
            </NTag>
          </div>
        </div>
      </section>
    </div>

    <section class="debug-panel records-panel">
      <div class="panel-title-row">
        <div>
          <p class="panel-kicker">
            Records
          </p>
          <h3>演示表记录</h3>
        </div>
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
  label: `${item.tenantName || `租户 ${item.id}`} (${item.defaultBusinessDatasourceCode || 'master'})`,
  value: item.id,
})))

const selectedTenant = computed(() => tenants.value.find(item => item.id === form.tenantId) || null)
const datasource = computed(() => routeResult.value?.datasource || {})
const records = computed(() => routeResult.value?.records || [])
const latestRecord = computed(() => routeResult.value?.record || records.value[0] || null)
const tableLoading = computed(() => currentLoading.value || prepareLoading.value || listLoading.value)
const datasourceTagType = computed(() => datasource.value.master ? 'default' : 'success')
const routeModeText = computed(() => datasource.value.master ? 'master' : 'tenant ds')
const currentDsKey = computed(() => routeResult.value?.currentDsKey || datasource.value.dsKey || 'master')

const routeMetrics = computed(() => [
  {
    key: 'database',
    label: '当前数据库',
    value: routeResult.value?.databaseName || '-',
  },
  {
    key: 'threadDsKey',
    label: '线程 dsKey',
    value: currentDsKey.value,
  },
  {
    key: 'tenantDsKey',
    label: '租户配置',
    value: selectedTenant.value?.defaultBusinessDatasourceCode || 'master',
  },
  {
    key: 'route',
    label: '路由状态',
    value: datasource.value.master ? '主库' : '业务库',
  },
])

const recordColumns = [
  {
    title: 'ID',
    key: 'id',
    width: 180,
    ellipsis: true,
  },
  {
    title: '租户ID',
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
    title: 'routeKey',
    key: 'routeKey',
    width: 160,
    render: row => h(NTag, {
      size: 'small',
      type: row.routeKey ? 'success' : 'default',
    }, {
      default: () => row.routeKey || 'master',
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
  min-height: 100%;
  padding: 16px;
  background: #f5f7fb;
}

.debug-header,
.debug-panel {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
}

.debug-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 18px 20px;
}

.debug-header h2,
.panel-title-row h3 {
  margin: 0;
  color: var(--text-primary);
  font-weight: 650;
  letter-spacing: 0;
}

.debug-header h2 {
  font-size: 22px;
}

.panel-title-row h3 {
  font-size: 16px;
}

.debug-kicker,
.panel-kicker {
  margin: 0 0 4px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0;
  text-transform: uppercase;
}

.debug-layout {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.debug-panel {
  padding: 16px;
}

.panel-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.control-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 4px;
}

.tenant-config-box {
  display: grid;
  gap: 10px;
  margin-top: 16px;
  padding: 12px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
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
  margin-top: 16px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.metric-card {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary);
}

.metric-card span {
  display: block;
  color: var(--text-secondary);
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-descriptions {
  margin-top: 4px;
}

.latest-record {
  margin-top: 16px;
  padding: 12px;
  border-left: 3px solid #10b981;
  border-radius: 6px;
  background: #ecfdf5;
}

.latest-title {
  color: #047857;
  font-size: 12px;
  font-weight: 600;
}

.latest-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  color: #064e3b;
  font-weight: 600;
}

.records-panel {
  min-height: 280px;
}

@media (max-width: 1180px) {
  .debug-layout,
  .metric-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .business-datasource-debug-page {
    padding: 12px;
  }

  .debug-header,
  .panel-title-row,
  .latest-content {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
