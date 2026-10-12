<template>
  <div class="agent-tool-page">
    <!-- 顶部工具条 -->
    <div class="tool-bar">
      <div>
        <div class="page-title">
          工具管理
        </div>
        <div class="page-subtitle">
          管理 Agent 可使用的工具绑定（MCP / 内置 / 能力平台）
        </div>
      </div>
      <NButton type="primary" @click="handleAdd">
        <template #icon>
          <i class="ai-icon:plus" />
        </template>
        新增工具绑定
      </NButton>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <NSelect
        v-model:value="search.agentId"
        placeholder="全部 Agent"
        clearable
        filterable
        :options="agentOptions"
        class="filter-agent"
      />
      <NSelect
        v-model:value="search.toolSource"
        placeholder="全部来源"
        clearable
        :options="toolSourceOptions"
        class="filter-source"
      />
      <n-input
        v-model:value="search.keyword"
        placeholder="搜索工具标识"
        clearable
        class="filter-keyword"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <i class="ai-icon:search" />
        </template>
      </n-input>
      <NButton secondary @click="handleSearch">
        查询
      </NButton>
      <NButton quaternary @click="handleReset">
        重置
      </NButton>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <n-data-table
        :columns="columns"
        :data="list"
        :loading="loading"
        :scroll-x="960"
        :row-key="row => row.id"
        size="small"
        :pagination="pagination"
        :bordered="false"
      />
    </div>

    <!-- 编辑抽屉 -->
    <n-drawer v-model:show="drawerVisible" :width="420">
      <n-drawer-content :title="editingId ? '编辑工具绑定' : '新增工具绑定'" closable>
        <n-form label-placement="left" label-width="90">
          <n-form-item label="Agent">
            <NSelect
              v-model:value="form.agentId"
              :options="agentOptions"
              placeholder="请选择 Agent"
              filterable
              clearable
            />
          </n-form-item>
          <n-form-item label="工具来源">
            <NSelect
              v-model:value="form.toolSource"
              :options="toolSourceOptions"
              placeholder="请选择工具来源"
            />
          </n-form-item>
          <n-form-item label="工具标识">
            <n-input v-model:value="form.toolKey" placeholder="如 mcp.server.tool" />
          </n-form-item>
          <n-form-item label="工具组">
            <n-input v-model:value="form.toolGroup" placeholder="默认 default" />
          </n-form-item>
          <n-form-item label="启用">
            <NSwitch v-model:value="form.enabled" checked-value="1" unchecked-value="0" />
          </n-form-item>
        </n-form>
        <template #footer>
          <div class="drawer-footer">
            <NButton @click="drawerVisible = false">
              取消
            </NButton>
            <NButton type="primary" :loading="saving" @click="handleSave">
              确定
            </NButton>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { NButton, NPopconfirm, NSelect, NSwitch, NTag, useMessage } from 'naive-ui'
import { h, onMounted, reactive, ref } from 'vue'
import {
  agentToolAdd,
  agentToolDelete,
  agentToolGetById,
  agentToolPage,
  agentToolUpdate,
  agentList as fetchAgentList,
} from '@/api/ai'

defineOptions({ name: 'AiAgentTool' })

const message = useMessage()

const list = ref([])
const loading = ref(false)
const agents = ref([])
const agentOptions = ref([])
const saving = ref(false)
const drawerVisible = ref(false)
const editingId = ref(null)
const rowLoading = reactive({})
const switchUpdating = reactive({})

// 工具来源固定为字典 ai_tool_source 的三类
const toolSourceOptions = [
  { label: '内置', value: 'builtin' },
  { label: 'MCP', value: 'mcp' },
  { label: '能力平台', value: 'capability' },
]

const search = reactive({ agentId: null, toolSource: null, keyword: '' })

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  prefix: ({ itemCount }) => `共 ${itemCount} 条`,
})

const form = reactive({
  agentId: null,
  toolSource: null,
  toolKey: '',
  toolGroup: '',
  enabled: '1',
})

function isRowLoading(id, type) {
  return rowLoading[id] === type
}

async function loadAgents() {
  try {
    const res = await fetchAgentList()
    const data = res.data || []
    agents.value = data
    agentOptions.value = data.map(a => ({
      label: a.agentName ? `${a.agentName} (${a.agentCode})` : a.agentCode,
      value: a.id,
    }))
  }
  catch {
    // 加载失败时下拉为空，不阻塞页面
  }
}

function getAgentLabel(id) {
  const agent = agents.value.find(a => a.id === id)
  return agent ? agent.agentName || agent.agentCode : `#${id}`
}

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: pagination.page, pageSize: pagination.pageSize }
    if (search.agentId)
      params.agentId = search.agentId
    if (search.toolSource)
      params.toolSource = search.toolSource
    if (search.keyword)
      params.keyword = search.keyword
    const res = await agentToolPage(params)
    if (res.code === 200) {
      list.value = res.data?.records || []
      pagination.itemCount = Number(res.data?.total || 0)
    }
  }
  catch (e) {
    message.error(e.message || '加载失败')
  }
  finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadList()
}

function handleReset() {
  search.agentId = null
  search.toolSource = null
  search.keyword = ''
  pagination.page = 1
  loadList()
}

function handleAdd() {
  editingId.value = null
  Object.assign(form, { agentId: null, toolSource: null, toolKey: '', toolGroup: '', enabled: '1' })
  drawerVisible.value = true
}

async function handleEdit(row) {
  if (isRowLoading(row.id, 'edit'))
    return
  rowLoading[row.id] = 'edit'
  try {
    const res = await agentToolGetById(row.id)
    if (res.code === 200 && res.data) {
      editingId.value = row.id
      Object.assign(form, {
        agentId: res.data.agentId,
        toolSource: res.data.toolSource,
        toolKey: res.data.toolKey || '',
        toolGroup: res.data.toolGroup || '',
        enabled: res.data.enabled || '1',
      })
      drawerVisible.value = true
    }
  }
  catch (e) {
    message.error(e.message || '读取失败')
  }
  finally {
    delete rowLoading[row.id]
  }
}

async function handleSave() {
  if (!form.agentId) {
    message.warning('请选择 Agent')
    return
  }
  if (!form.toolSource) {
    message.warning('请选择工具来源')
    return
  }
  if (!form.toolKey?.trim()) {
    message.warning('请输入工具标识')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, toolKey: form.toolKey.trim(), toolGroup: form.toolGroup?.trim() || 'default' }
    const res = editingId.value
      ? await agentToolUpdate({ ...payload, id: editingId.value })
      : await agentToolAdd(payload)
    if (res.code === 200) {
      message.success(editingId.value ? '已保存' : '已创建')
      drawerVisible.value = false
      loadList()
    }
    else {
      message.error(res.msg || '操作失败')
    }
  }
  catch (e) {
    message.error(e.message || '操作失败')
  }
  finally {
    saving.value = false
  }
}

async function handleToggleEnabled(row, value) {
  switchUpdating[row.id] = true
  try {
    const res = await agentToolUpdate({ ...row, enabled: value ? '1' : '0' })
    if (res.code === 200) {
      row.enabled = value ? '1' : '0'
      message.success(value ? '已启用' : '已停用')
    }
    else {
      message.error(res.msg || '操作失败')
    }
  }
  catch (e) {
    message.error(e.message || '操作失败')
  }
  finally {
    delete switchUpdating[row.id]
  }
}

async function handleDelete(row) {
  if (isRowLoading(row.id, 'delete'))
    return
  rowLoading[row.id] = 'delete'
  try {
    const res = await agentToolDelete(row.id)
    if (res.code === 200) {
      message.success('已删除')
      if (list.value.length === 1 && pagination.page > 1)
        pagination.page -= 1
      loadList()
    }
    else {
      message.error(res.msg || '删除失败')
    }
  }
  catch (e) {
    message.error(e.message || '删除失败')
  }
  finally {
    delete rowLoading[row.id]
  }
}

function renderSource(row) {
  const map = { builtin: { label: '内置', type: 'success' }, mcp: { label: 'MCP', type: 'info' }, capability: { label: '能力平台', type: 'warning' } }
  const item = map[row.toolSource] || { label: row.toolSource || '-', type: 'default' }
  return h(NTag, { size: 'small', type: item.type, bordered: false }, { default: () => item.label })
}

const columns = [
  { title: 'Agent', key: 'agentId', width: 180, render: row => getAgentLabel(row.agentId) },
  { title: '来源', key: 'toolSource', width: 100, render: renderSource },
  { title: '工具标识', key: 'toolKey', minWidth: 180, ellipsis: { tooltip: true } },
  { title: '工具组', key: 'toolGroup', width: 100, render: row => row.toolGroup || '-' },
  {
    title: '启用',
    key: 'enabled',
    width: 80,
    align: 'center',
    render: row => h(NSwitch, {
      value: row.enabled === '1',
      size: 'small',
      loading: switchUpdating[row.id],
      onUpdateValue: value => handleToggleEnabled(row, value),
    }),
  },
  {
    title: '操作',
    key: 'actions',
    width: 130,
    fixed: 'right',
    render: (row) => {
      const edit = h(NButton, {
        text: true,
        size: 'small',
        type: 'primary',
        loading: isRowLoading(row.id, 'edit'),
        disabled: !!rowLoading[row.id] && rowLoading[row.id] !== 'edit',
        onClick: () => handleEdit(row),
      }, { default: () => '编辑' })
      const del = h(NPopconfirm, {
        onPositiveClick: () => handleDelete(row),
      }, {
        trigger: () => h(NButton, {
          text: true,
          size: 'small',
          type: 'error',
          loading: isRowLoading(row.id, 'delete'),
          disabled: !!rowLoading[row.id] && rowLoading[row.id] !== 'delete',
        }, { default: () => '删除' }),
        default: () => '确定删除该工具绑定吗？',
      })
      return h('span', { class: 'row-actions' }, [edit, del])
    },
  },
]

onMounted(() => {
  loadAgents()
  loadList()
})
</script>

<style scoped>
.agent-tool-page {
  --page-bg: #f3f6fa;
  --panel-bg: var(--bg-primary);
  --panel-subtle: #f8fafc;
  --panel-border: #dfe6ee;
  --text-strong: #111827;
  --text-body: #475569;
  --text-muted: #64748b;
  --accent: #0369a1;
  --accent-soft: #eaf4fb;
  min-height: 100%;
  padding: 20px;
  color: var(--text-body);
  background: radial-gradient(circle at 100% 0, rgba(14, 116, 144, 0.07), transparent 340px), var(--page-bg);
}

:global(.dark) .agent-tool-page {
  --page-bg: #0d1420;
  --panel-bg: #151f2d;
  --panel-subtle: #111a27;
  --panel-border: #2c3a4d;
  --text-strong: #f1f5f9;
  --text-body: #cbd5e1;
  --text-muted: #94a3b8;
  --accent: #38bdf8;
  --accent-soft: rgba(14, 165, 233, 0.12);
  background: radial-gradient(circle at 100% 0, rgba(14, 165, 233, 0.08), transparent 360px), var(--page-bg);
}

.tool-bar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-title {
  color: var(--text-strong);
  font-size: 20px;
  font-weight: 600;
}

.page-subtitle {
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 12px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  margin-bottom: 14px;
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  border-radius: 9px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.filter-agent {
  width: 220px;
}
.filter-source {
  width: 140px;
}
.filter-keyword {
  width: 220px;
}

.table-card {
  padding: 4px;
  overflow: hidden;
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  border-radius: 9px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 720px) {
  .filter-bar {
    align-items: stretch;
    flex-direction: column;
  }
  .filter-agent,
  .filter-source,
  .filter-keyword {
    width: 100%;
  }
}
</style>
