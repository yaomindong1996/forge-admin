<script setup>
import {
  NButton,
  NDataTable,
  NDrawer,
  NDrawerContent,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NPopconfirm,
  NSpace,
  NSwitch,
  NTag,
} from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import {
  mcpConnectionCreate,
  mcpConnectionDelete,
  mcpConnectionList,
  mcpConnectionSync,
  mcpConnectionUpdate,
} from '@/api/ai'
import './resource-center.css'

const loading = ref(false)
const saving = ref(false)
const busyId = ref(null)
const keyword = ref('')
const rows = ref([])
const drawerOpen = ref(false)
const toolsDrawer = reactive({ open: false, connectionName: '', tools: [] })
const formRef = ref(null)

const form = reactive({
  id: null,
  expectedRowVersion: null,
  connectionCode: '',
  connectionName: '',
  endpoint: '',
  token: '',
  clearToken: false,
  timeoutSeconds: 15,
  enabled: 1,
})

const rules = {
  connectionCode: { required: true, message: '请输入服务编码（字母/数字/连字符）', trigger: 'blur' },
  connectionName: { required: true, message: '请输入服务名称', trigger: 'blur' },
  endpoint: { required: true, message: '请输入 Streamable HTTP 地址', trigger: 'blur' },
}

const filteredRows = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q)
    return rows.value
  return rows.value.filter(row =>
    `${row.connectionName || ''} ${row.connectionCode || ''} ${row.endpoint || ''}`.toLowerCase().includes(q),
  )
})

function syncTagType(state) {
  if (state === 'SYNCED')
    return 'success'
  if (state === 'FAILED')
    return 'error'
  return 'default'
}

function syncLabel(state) {
  if (state === 'SYNCED')
    return '已同步'
  if (state === 'FAILED')
    return '同步失败'
  return '未同步'
}

function openCreate() {
  Object.assign(form, {
    id: null,
    expectedRowVersion: null,
    connectionCode: '',
    connectionName: '',
    endpoint: '',
    token: '',
    clearToken: false,
    timeoutSeconds: 15,
    enabled: 1,
  })
  drawerOpen.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    expectedRowVersion: Number(row.rowVersion || 1),
    connectionCode: row.connectionCode,
    connectionName: row.connectionName,
    endpoint: row.endpoint,
    token: '',
    clearToken: false,
    timeoutSeconds: Number(row.timeoutSeconds || 15),
    enabled: Number(row.enabled ?? 1),
  })
  drawerOpen.value = true
}

function openTools(row) {
  toolsDrawer.connectionName = row.connectionName
  toolsDrawer.tools = row.tools || []
  toolsDrawer.open = true
}

async function loadList() {
  loading.value = true
  try {
    const res = await mcpConnectionList()
    rows.value = res.code === 200 && Array.isArray(res.data) ? res.data : []
  }
  catch (e) {
    window.$message.error(e.message || '加载 MCP 服务列表失败')
  }
  finally {
    loading.value = false
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  }
  catch {
    return
  }
  saving.value = true
  try {
    const payload = { ...form }
    const res = form.id
      ? await mcpConnectionUpdate(payload)
      : await mcpConnectionCreate(payload)
    if (res.code === 200) {
      window.$message.success(form.id ? 'MCP 服务已更新' : 'MCP 服务已新增')
      drawerOpen.value = false
      await loadList()
    }
    else {
      window.$message.error(res.msg || '保存失败')
    }
  }
  catch (e) {
    window.$message.error(e.message || '保存失败')
  }
  finally {
    saving.value = false
  }
}

async function handleSync(row) {
  busyId.value = row.id
  try {
    const res = await mcpConnectionSync(row.id, Number(row.rowVersion || 1))
    if (res.code === 200) {
      window.$message.success('连接测试通过，已同步远程工具目录')
      await loadList()
    }
    else {
      window.$message.error(res.msg || '同步失败')
    }
  }
  catch (e) {
    window.$message.error(e.message || '同步失败')
  }
  finally {
    busyId.value = null
  }
}

async function handleDelete(row) {
  busyId.value = row.id
  try {
    const res = await mcpConnectionDelete(row.id, Number(row.rowVersion || 1))
    if (res.code === 200) {
      window.$message.success('已删除 MCP 服务')
      await loadList()
    }
    else {
      window.$message.error(res.msg || '删除失败')
    }
  }
  catch (e) {
    window.$message.error(e.message || '删除失败')
  }
  finally {
    busyId.value = null
  }
}

const columns = [
  {
    title: '服务名称',
    key: 'connectionName',
    minWidth: 180,
    render(row) {
      return h('div', [
        h('strong', { style: 'font-size: 13px;' }, row.connectionName),
        h('div', { style: 'font-size: 11px; color: var(--text-tertiary); font-family: monospace;' }, row.connectionCode),
      ])
    },
  },
  {
    title: 'Endpoint（Streamable HTTP）',
    key: 'endpoint',
    minWidth: 240,
    ellipsis: { tooltip: true },
  },
  {
    title: '同步状态',
    key: 'syncState',
    width: 110,
    render: row => h(NTag, { size: 'small', type: syncTagType(row.syncState), bordered: false }, () => syncLabel(row.syncState)),
  },
  {
    title: '工具数',
    key: 'toolCount',
    width: 90,
    render: row => h(NButton, { text: true, type: 'primary', size: 'small', onClick: () => openTools(row) }, () => `${row.toolCount || 0} 个`),
  },
  {
    title: '状态',
    key: 'enabled',
    width: 80,
    render: row => h(NTag, { size: 'small', type: Number(row.enabled) === 1 ? 'success' : 'default', bordered: false }, () => (Number(row.enabled) === 1 ? '启用' : '停用')),
  },
  {
    title: '操作',
    key: 'actions',
    width: 240,
    render(row) {
      const loadingRow = busyId.value === row.id
      return h(NSpace, { size: 6 }, () => [
        h(NButton, { size: 'tiny', type: 'primary', secondary: true, loading: loadingRow, onClick: () => handleSync(row) }, () => '测试并同步'),
        h(NButton, { size: 'tiny', quaternary: true, onClick: () => openEdit(row) }, () => '编辑'),
        h(
          NPopconfirm,
          { onPositiveClick: () => handleDelete(row) },
          {
            trigger: () => h(NButton, { size: 'tiny', quaternary: true, type: 'error' }, () => '删除'),
            default: () => `确认删除 MCP 服务「${row.connectionName}」？`,
          },
        ),
      ])
    },
  },
]

const toolColumns = [
  { title: '工具标识', key: 'toolKey', minWidth: 160 },
  { title: '标题', key: 'title', minWidth: 140 },
  { title: '说明', key: 'description', minWidth: 200, ellipsis: { tooltip: true } },
]

onMounted(() => {
  void loadList()
})
</script>

<template>
  <div class="rc-panel">
    <div class="rc-toolbar">
      <NInput v-model:value="keyword" clearable placeholder="搜索服务名称、编码或地址" style="width: 260px" />
      <NSpace>
        <NButton secondary :loading="loading" @click="loadList">
          刷新
        </NButton>
        <NButton type="primary" @click="openCreate">
          接入 MCP 服务
        </NButton>
      </NSpace>
    </div>

    <NDataTable
      :columns="columns"
      :data="filteredRows"
      :loading="loading"
      :row-key="row => row.id"
      :bordered="false"
      size="medium"
    />

    <NDrawer v-model:show="drawerOpen" :width="460">
      <NDrawerContent :title="form.id ? '编辑 MCP 服务' : '接入 MCP 服务'" closable>
        <NForm ref="formRef" :model="form" :rules="rules" label-placement="top">
          <NFormItem label="服务编码" path="connectionCode">
            <NInput v-model:value="form.connectionCode" :disabled="!!form.id" placeholder="如 crm-tools" />
          </NFormItem>
          <NFormItem label="服务名称" path="connectionName">
            <NInput v-model:value="form.connectionName" placeholder="如 CRM 客户工具服务" />
          </NFormItem>
          <NFormItem label="Streamable HTTP 地址" path="endpoint">
            <NInput v-model:value="form.endpoint" placeholder="https://example.com/mcp" />
          </NFormItem>
          <NFormItem label="Bearer 令牌（可选）" path="token">
            <NInput
              v-model:value="form.token"
              type="password"
              show-password-on="click"
              :placeholder="form.id ? '留空表示保持原有令牌不变' : '如服务端需要认证请填写'"
            />
          </NFormItem>
          <NFormItem label="超时时间（秒）" path="timeoutSeconds">
            <NInputNumber v-model:value="form.timeoutSeconds" :min="3" :max="60" style="width: 100%" />
          </NFormItem>
          <NFormItem label="启用状态" path="enabled">
            <NSwitch v-model:value="form.enabled" :checked-value="1" :unchecked-value="0" />
          </NFormItem>
        </NForm>
        <template #footer>
          <NSpace justify="end">
            <NButton @click="drawerOpen = false">
              取消
            </NButton>
            <NButton type="primary" :loading="saving" @click="handleSave">
              保存
            </NButton>
          </NSpace>
        </template>
      </NDrawerContent>
    </NDrawer>

    <NDrawer v-model:show="toolsDrawer.open" :width="600">
      <NDrawerContent :title="`${toolsDrawer.connectionName} — 已同步工具`" closable>
        <NDataTable :columns="toolColumns" :data="toolsDrawer.tools" :row-key="r => r.toolKey" size="small" />
      </NDrawerContent>
    </NDrawer>
  </div>
</template>
