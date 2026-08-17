<template>
  <div class="managed-cache-panel">
    <div class="managed-toolbar">
      <div class="filter-group">
        <NInput
          v-model:value="filters.applicationCode"
          clearable
          placeholder="应用编码"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <i class="i-mdi:application-brackets-outline" />
          </template>
        </NInput>
        <NInput
          v-model:value="filters.cacheName"
          clearable
          placeholder="缓存名称"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <i class="i-mdi:magnify" />
          </template>
        </NInput>
      </div>
      <div class="toolbar-actions">
        <NButton type="primary" @click="handleSearch">
          <template #icon>
            <i class="i-mdi:magnify" />
          </template>
          查询
        </NButton>
        <NButton @click="handleResetFilters">
          <template #icon>
            <i class="i-mdi:refresh" />
          </template>
          重置
        </NButton>
      </div>
    </div>

    <NDataTable
      :columns="columns"
      :data="records"
      :loading="loading"
      :pagination="false"
      :row-key="row => `${row.applicationCode}::${row.cacheName}`"
      :scroll-x="1280"
      size="small"
      striped
    />

    <div class="pagination-bar">
      <NPagination
        v-model:page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :item-count="pagination.total"
        :page-sizes="[10, 20, 50]"
        show-size-picker
        @update:page="loadPolicies"
        @update:page-size="handlePageSizeChange"
      />
    </div>

    <NModal
      v-model:show="editVisible"
      preset="dialog"
      title="编辑缓存策略"
      positive-text="保存"
      negative-text="取消"
      :loading="saving"
      :style="{ width: '680px', maxWidth: 'calc(100vw - 32px)' }"
      @positive-click="handleSave"
    >
      <NForm
        :model="editForm"
        label-placement="left"
        label-width="110"
        class="policy-form"
      >
        <NFormItem label="缓存">
          <div class="readonly-identity">
            <strong>{{ editForm.cacheName }}</strong>
            <span>{{ editForm.applicationCode }}</span>
          </div>
        </NFormItem>
        <NFormItem label="启用">
          <NSwitch v-model:value="editForm.enabled" />
        </NFormItem>
        <NFormItem label="缓存模式">
          <NRadioGroup v-model:value="editForm.cacheMode" size="small">
            <NRadioButton
              v-for="mode in editingAllowedModes"
              :key="mode"
              :value="mode"
            >
              {{ modeLabels[mode] || mode }}
            </NRadioButton>
          </NRadioGroup>
        </NFormItem>
        <div class="form-grid">
          <NFormItem label="本地 TTL">
            <NInputNumber
              v-model:value="editForm.localTtlSeconds"
              :min="1"
              :precision="0"
              class="number-input"
            >
              <template #suffix>
                秒
              </template>
            </NInputNumber>
          </NFormItem>
          <NFormItem label="Redis TTL">
            <NInputNumber
              v-model:value="editForm.redisTtlSeconds"
              :min="1"
              :precision="0"
              class="number-input"
            >
              <template #suffix>
                秒
              </template>
            </NInputNumber>
          </NFormItem>
          <NFormItem label="本地容量">
            <NInputNumber
              v-model:value="editForm.localMaxSize"
              :min="1"
              :precision="0"
              class="number-input"
            />
          </NFormItem>
          <NFormItem label="空值 TTL">
            <NInputNumber
              v-model:value="editForm.nullTtlSeconds"
              :min="1"
              :precision="0"
              class="number-input"
            >
              <template #suffix>
                秒
              </template>
            </NInputNumber>
          </NFormItem>
        </div>
        <NFormItem label="缓存空值">
          <NSwitch v-model:value="editForm.cacheNull" />
        </NFormItem>
      </NForm>
    </NModal>
  </div>
</template>

<script setup>
import { NButton, NSpace, NTag } from 'naive-ui'
import { h, onMounted, reactive, ref } from 'vue'
import { request } from '@/utils'
import { normalizeManagedCachePolicy, validateManagedCachePolicy } from './managed-cache-policy'

const modeLabels = {
  LOCAL: '本地',
  REDIS: 'Redis',
  MULTI: '多级',
}

const scopeLabels = {
  GLOBAL: '全局',
  TENANT: '租户',
  TENANT_USER: '租户 + 用户',
  TENANT_USER_ORG: '租户 + 用户 + 组织',
}

const loading = ref(false)
const saving = ref(false)
const records = ref([])
const editVisible = ref(false)
const editingAllowedModes = ref([])
const filters = reactive({ applicationCode: '', cacheName: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const editForm = reactive(normalizeManagedCachePolicy())

const columns = [
  {
    title: '缓存',
    key: 'cacheName',
    width: 270,
    fixed: 'left',
    render: row => h('div', { class: 'cache-identity' }, [
      h('div', { class: 'cache-name' }, row.cacheName),
      h('div', { class: 'cache-description' }, row.description || row.source || row.applicationCode),
      h('div', { class: 'cache-source' }, `${row.applicationCode} · ${row.source || '-'}`),
    ]),
  },
  {
    title: '作用域',
    key: 'scope',
    width: 150,
    render: row => h(NTag, { size: 'small', bordered: false }, {
      default: () => scopeLabels[row.scope] || row.scope,
    }),
  },
  {
    title: '有效模式',
    key: 'cacheMode',
    width: 110,
    render: row => h(NTag, { size: 'small', type: modeTagType(row.cacheMode) }, {
      default: () => modeLabels[row.cacheMode] || row.cacheMode,
    }),
  },
  {
    title: '允许模式',
    key: 'allowedModes',
    width: 210,
    render: row => h(NSpace, { size: 4, wrap: true }, {
      default: () => (row.allowedModes || []).map(mode => h(NTag, {
        key: mode,
        size: 'tiny',
        bordered: false,
      }, { default: () => modeLabels[mode] || mode })),
    }),
  },
  {
    title: 'TTL',
    key: 'ttl',
    width: 190,
    render: row => h('div', { class: 'ttl-cell' }, [
      h('span', `L1 ${formatDuration(row.localTtlSeconds)}`),
      h('span', `L2 ${formatDuration(row.redisTtlSeconds)}`),
    ]),
  },
  {
    title: '本地容量',
    key: 'localMaxSize',
    width: 110,
    align: 'right',
    render: row => Number(row.localMaxSize || 0).toLocaleString(),
  },
  {
    title: '状态',
    key: 'enabled',
    width: 130,
    render: row => h(NSpace, { size: 4 }, {
      default: () => [
        h(NTag, { size: 'small', type: row.enabled ? 'success' : 'default' }, {
          default: () => row.enabled ? '启用' : '停用',
        }),
        row.overridden
          ? h(NTag, { size: 'small', type: 'warning', bordered: false }, { default: () => '已覆盖' })
          : null,
      ],
    }),
  },
  {
    title: '命中 / 未命中',
    key: 'stats',
    width: 135,
    align: 'right',
    render: row => `${row.hitCount || 0} / ${row.missCount || 0}`,
  },
  {
    title: '操作',
    key: 'actions',
    width: 190,
    fixed: 'right',
    render: row => h(NSpace, { size: 12 }, {
      default: () => [
        actionButton('编辑', 'primary', () => openEdit(row)),
        actionButton('清空', 'warning', () => confirmClear(row)),
        actionButton('恢复默认', 'default', () => confirmReset(row), !row.overridden),
      ],
    }),
  },
]

onMounted(loadPolicies)

async function loadPolicies() {
  loading.value = true
  try {
    const response = await request.get('/system/cache/policy/page', {
      params: {
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize,
        applicationCode: filters.applicationCode || undefined,
        cacheName: filters.cacheName || undefined,
      },
    })
    if (response.code === 200) {
      records.value = response.data?.records || []
      pagination.total = Number(response.data?.total || 0)
    }
  }
  catch {
    window.$message.error('加载受管缓存策略失败')
  }
  finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadPolicies()
}

function handleResetFilters() {
  filters.applicationCode = ''
  filters.cacheName = ''
  pagination.pageNum = 1
  loadPolicies()
}

function handlePageSizeChange(pageSize) {
  pagination.pageSize = pageSize
  pagination.pageNum = 1
  loadPolicies()
}

function openEdit(row) {
  Object.assign(editForm, normalizeManagedCachePolicy(row))
  editingAllowedModes.value = [...(row.allowedModes || [])]
  editVisible.value = true
}

async function handleSave() {
  const error = validateManagedCachePolicy(editForm, editingAllowedModes.value)
  if (error) {
    window.$message.warning(error)
    return false
  }
  saving.value = true
  try {
    const response = await request.post('/system/cache/policy/edit', { ...editForm })
    if (response.code === 200) {
      window.$message.success('缓存策略已更新')
      editVisible.value = false
      await loadPolicies()
      return true
    }
    return false
  }
  catch {
    return false
  }
  finally {
    saving.value = false
  }
}

function confirmClear(row) {
  window.$dialog.warning({
    title: '清空受管缓存',
    content: `确定清空 ${row.cacheName} 的全部缓存条目吗？`,
    positiveText: '清空',
    negativeText: '取消',
    onPositiveClick: async () => {
      const response = await request.post('/system/cache/policy/clear', null, {
        params: identityParams(row),
      })
      if (response.code === 200) {
        window.$message.success('缓存已清空')
        await loadPolicies()
      }
    },
  })
}

function confirmReset(row) {
  if (!row.overridden)
    return
  window.$dialog.warning({
    title: '恢复默认策略',
    content: `确定让 ${row.cacheName} 恢复代码声明的默认策略吗？`,
    positiveText: '恢复默认',
    negativeText: '取消',
    onPositiveClick: async () => {
      const response = await request.post('/system/cache/policy/reset', null, {
        params: identityParams(row),
      })
      if (response.code === 200) {
        window.$message.success('已恢复默认策略')
        await loadPolicies()
      }
    },
  })
}

function identityParams(row) {
  return {
    applicationCode: row.applicationCode,
    cacheName: row.cacheName,
  }
}

function actionButton(label, type, onClick, disabled = false) {
  return h(NButton, { text: true, size: 'small', type, disabled, onClick }, { default: () => label })
}

function modeTagType(mode) {
  if (mode === 'MULTI')
    return 'success'
  if (mode === 'REDIS')
    return 'info'
  return 'warning'
}

function formatDuration(seconds) {
  const value = Number(seconds || 0)
  if (value > 0 && value % 3600 === 0)
    return `${value / 3600}h`
  if (value > 0 && value % 60 === 0)
    return `${value / 60}m`
  return `${value}s`
}
</script>

<style scoped>
.managed-cache-panel {
  min-height: 520px;
  padding: 4px 0 12px;
}

.managed-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0 16px;
}

.filter-group,
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-group {
  width: min(560px, 100%);
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.readonly-identity {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.readonly-identity span {
  color: #6b7280;
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.number-input {
  width: 100%;
}

:deep(.cache-identity) {
  line-height: 1.45;
}

:deep(.cache-name) {
  color: #111827;
  font-weight: 600;
}

:deep(.cache-description) {
  overflow: hidden;
  color: #4b5563;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.cache-source) {
  overflow: hidden;
  color: #9ca3af;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.ttl-cell) {
  display: flex;
  gap: 10px;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 720px) {
  .managed-toolbar,
  .filter-group {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions > * {
    flex: 1;
  }

  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
