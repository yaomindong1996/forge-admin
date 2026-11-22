<script setup>
import { NButton, NDataTable, NInput, NSelect, NTag } from 'naive-ui'
import { computed, h, onMounted, ref } from 'vue'
import { resourceToolsCatalog } from '@/api/ai'
import './resource-center.css'

const loading = ref(false)
const keyword = ref('')
const source = ref(null)
const items = ref([])

const sourceOptions = [
  { label: '内置工具', value: 'builtin' },
  { label: 'MCP 远程工具', value: 'mcp' },
  { label: '能力平台', value: 'capability' },
]

const filteredItems = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return items.value.filter((row) => {
    if (source.value && row.source !== source.value)
      return false
    if (!q)
      return true
    return `${row.toolKey || ''} ${row.title || ''} ${row.description || ''}`.toLowerCase().includes(q)
  })
})

const columns = [
  {
    title: '工具名称',
    key: 'title',
    minWidth: 180,
    render(row) {
      return h('div', [
        h('strong', { style: 'font-size: 13px;' }, row.title || row.toolKey),
        h('div', { style: 'font-size: 11px; color: var(--text-tertiary); font-family: monospace;' }, row.toolKey),
      ])
    },
  },
  {
    title: '来源',
    key: 'sourceLabel',
    width: 130,
    render(row) {
      const type = row.source === 'mcp' ? 'primary' : row.source === 'builtin' ? 'success' : 'info'
      return h(NTag, { size: 'small', type, bordered: false }, () => row.sourceLabel || row.source)
    },
  },
  {
    title: '说明',
    key: 'description',
    minWidth: 260,
    ellipsis: { tooltip: true },
    render: row => row.description || '-',
  },
  {
    title: '状态',
    key: 'available',
    width: 100,
    render(row) {
      return h(
        NTag,
        { size: 'small', type: row.available ? 'success' : 'warning', bordered: false },
        () => (row.available ? '可用' : '停用'),
      )
    },
  },
]

function unwrapCatalogTools(payload) {
  if (Array.isArray(payload))
    return payload
  if (Array.isArray(payload?.tools))
    return payload.tools
  return []
}

async function loadCatalog() {
  loading.value = true
  try {
    const res = await resourceToolsCatalog({})
    items.value = res.code === 200 ? unwrapCatalogTools(res.data) : []
  }
  catch (e) {
    window.$message.error(e.message || '加载工具目录失败')
  }
  finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadCatalog()
})
</script>

<template>
  <div class="rc-panel">
    <div class="rc-toolbar">
      <div class="rc-toolbar__filters">
        <NInput v-model:value="keyword" clearable placeholder="搜索工具名称、标识或说明" style="width: 260px" />
        <NSelect v-model:value="source" clearable placeholder="全部来源" :options="sourceOptions" style="width: 160px" />
      </div>
      <NButton secondary :loading="loading" @click="loadCatalog">
        刷新目录
      </NButton>
    </div>
    <NDataTable
      :columns="columns"
      :data="filteredItems"
      :loading="loading"
      :row-key="row => row.toolKey"
      :bordered="false"
      size="medium"
    />
  </div>
</template>
