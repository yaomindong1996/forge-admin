<script setup>
import {
  NAlert,
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
  mcpAllowOrigin,
  mcpConnectionCreate,
  mcpConnectionDelete,
  mcpConnectionList,
  mcpConnectionSync,
  mcpConnectionUpdate,
} from '@/api/ai'
import { mcpOriginOf, parseMcpServersConfig } from './mcpConfigImport'
import './resource-center.css'

const loading = ref(false)
const saving = ref(false)
const busyId = ref(null)
const allowBusy = ref(null)
const keyword = ref('')
const rows = ref([])
const drawerOpen = ref(false)
const importOpen = ref(false)
const importText = ref('')
const importBusy = ref(false)
const toolsDrawer = reactive({ open: false, connectionName: '', tools: [] })
const formRef = ref(null)

const blockedOrigins = computed(() => {
  const seen = new Set()
  const list = []
  for (const row of rows.value) {
    if (row.destinationAllowed)
      continue
    const origin = mcpOriginOf(row.endpoint)
    if (!origin || seen.has(origin))
      continue
    seen.add(origin)
    list.push(origin)
  }
  return list
})

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

function openCreate(preset = null) {
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
    ...(preset || {}),
  })
  drawerOpen.value = true
}

function openImport() {
  importText.value = ''
  importOpen.value = true
}

function applyImport() {
  try {
    const entries = parseMcpServersConfig(importText.value)
    importOpen.value = false
    if (entries.length === 1) {
      openCreate(entries[0])
      window.$message.success('已填入 MCP 配置，请确认后保存')
      return
    }
    void importAll(entries)
  }
  catch (e) {
    window.$message.error(e.message || '导入失败')
  }
}

async function importAll(entries) {
  importBusy.value = true
  let ok = 0
  try {
    for (const item of entries) {
      const res = await mcpConnectionCreate(toSavePayload({
        ...item,
        id: null,
        expectedRowVersion: 0,
        clearToken: false,
      }))
      if (res.code === 200)
        ok += 1
      else
        window.$message.warning(`${item.connectionCode}：${res.msg || '保存失败'}`)
    }
    window.$message.success(`已导入 ${ok}/${entries.length} 个 MCP 服务`)
    await loadList()
  }
  catch (e) {
    window.$message.error(e.message || '批量导入失败')
  }
  finally {
    importBusy.value = false
  }
}

async function copyOrigin(origin) {
  try {
    await navigator.clipboard.writeText(origin)
    window.$message.success('已复制精确源地址')
  }
  catch {
    window.$message.warning('复制失败，请手动选中地址')
  }
}

async function allowOrigin(origin) {
  allowBusy.value = origin
  try {
    const res = await mcpAllowOrigin(origin)
    if (res.code === 200) {
      window.$message.success(`已放行 ${res.data?.origin || origin}，可重新「测试并同步」`)
      await loadList()
    }
    else {
      window.$message.error(res.msg || '放行失败')
    }
  }
  catch (e) {
    window.$message.error(e.message || '放行失败（需超级管理员）')
  }
  finally {
    allowBusy.value = null
  }
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

function toSavePayload(source = form) {
  return {
    id: source.id || null,
    connectionCode: source.connectionCode,
    connectionName: source.connectionName,
    endpoint: source.endpoint,
    bearerToken: source.token || null,
    clearSecret: !!source.clearToken,
    timeoutSeconds: Number(source.timeoutSeconds || 15),
    enabled: Number(source.enabled ?? 1),
    expectedRowVersion: source.id ? Number(source.expectedRowVersion || 1) : 0,
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
    const payload = toSavePayload()
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
    title: '出站',
    key: 'destinationAllowed',
    width: 96,
    render: row => h(
      NTag,
      { size: 'small', type: row.destinationAllowed ? 'success' : 'warning', bordered: false },
      () => (row.destinationAllowed ? '已放行' : '未加白'),
    ),
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
        <NButton secondary :loading="importBusy" @click="openImport">
          从 MCP 配置导入
        </NButton>
        <NButton type="primary" @click="openCreate()">
          接入 MCP 服务
        </NButton>
      </NSpace>
    </div>

    <NAlert
      v-if="blockedOrigins.length"
      type="warning"
      :bordered="false"
      style="margin-bottom: 12px"
      title="以下地址当前不能直接同步（多半是 HTTP / 私网）"
    >
      <div v-for="origin in blockedOrigins" :key="origin" style="display: flex; gap: 8px; align-items: center; margin-top: 6px; flex-wrap: wrap">
        <code>{{ origin }}</code>
        <NButton
          size="tiny"
          type="primary"
          secondary
          :loading="allowBusy === origin"
          @click="allowOrigin(origin)"
        >
          一键放行
        </NButton>
        <NButton size="tiny" quaternary type="primary" @click="copyOrigin(origin)">
          复制
        </NButton>
      </div>
      <p style="margin: 8px 0 0; font-size: 12px; color: var(--text-secondary)">
        公网 HTTPS 一般不用加白。HTTP 地址可由<strong>超级管理员</strong>在本页一键写入平台出站白名单（立即生效，无需重启）；本机与未授权私网仍禁止。
      </p>
    </NAlert>

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

    <NDrawer v-model:show="importOpen" :width="520">
      <NDrawerContent title="从 MCP 配置导入" closable>
        <p style="margin: 0 0 10px; font-size: 13px; color: var(--text-secondary)">
          粘贴 Cursor / Claude Desktop 的 <code>mcpServers</code> JSON，将自动识别 HTTP 地址与 Bearer 令牌。
        </p>
        <NInput
          v-model:value="importText"
          type="textarea"
          :rows="16"
          placeholder='{"mcpServers":{"forge-docs":{"type":"http","url":"http://host:port/mcp","headers":{"Authorization":"Bearer …"}}}}'
        />
        <template #footer>
          <NSpace justify="end">
            <NButton @click="importOpen = false">
              取消
            </NButton>
            <NButton type="primary" :loading="importBusy" @click="applyImport">
              解析并导入
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
