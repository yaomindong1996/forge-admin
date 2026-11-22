<script setup>
import { NButton, NEmpty, NSpin, NSwitch, NTag } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import {
  agentSkillPackages,
  agentToolAdd,
  agentToolDelete,
  agentToolPage,
  bindAgentSkillPackage,
  resourceToolsCatalog,
  skillPackagePage,
  unbindAgentSkillPackage,
} from '@/api/ai'

const props = defineProps({
  agentId: { type: [Number, String], default: null },
  mode: { type: String, required: true }, // 'mcp' | 'skill-package'
})

const emit = defineEmits(['changed'])

const loading = ref(false)
const busyKey = ref('')
const mcpCatalog = ref([])
const agentTools = ref([])
const readyPackages = ref([])
const boundPackages = ref([])

const boundMcpMap = computed(() => {
  const map = new Map()
  for (const row of agentTools.value) {
    if (row.toolSource === 'mcp')
      map.set(row.toolKey, row)
  }
  return map
})

const boundPkgMap = computed(() => {
  const map = new Map()
  for (const row of boundPackages.value)
    map.set(String(row.packageId), row)
  return map
})

function unwrapCatalogTools(payload) {
  if (Array.isArray(payload))
    return payload
  if (Array.isArray(payload?.tools))
    return payload.tools
  return []
}

async function loadData() {
  if (!props.agentId)
    return
  loading.value = true
  try {
    if (props.mode === 'mcp') {
      const [catalogRes, toolsRes] = await Promise.all([
        resourceToolsCatalog({}),
        agentToolPage({ agentId: props.agentId, pageNum: 1, pageSize: 200 }),
      ])
      const all = catalogRes.code === 200 ? unwrapCatalogTools(catalogRes.data) : []
      mcpCatalog.value = all.filter(tool => tool.source === 'mcp' && tool.available !== false)
      agentTools.value = toolsRes.data?.records || []
    }
    else {
      const [lookupRes, boundRes] = await Promise.all([
        skillPackagePage({ pageNum: 1, pageSize: 100, status: 'READY' }),
        agentSkillPackages(props.agentId),
      ])
      readyPackages.value = lookupRes.code === 200 && Array.isArray(lookupRes.data?.records)
        ? lookupRes.data.records
        : []
      boundPackages.value = boundRes.code === 200 && Array.isArray(boundRes.data) ? boundRes.data : []
    }
  }
  catch (e) {
    mcpCatalog.value = []
    readyPackages.value = []
    window.$message.error(e.message || '加载绑定列表失败')
  }
  finally {
    loading.value = false
  }
}

async function toggleMcpTool(tool, checked) {
  busyKey.value = tool.toolKey
  try {
    const existing = boundMcpMap.value.get(tool.toolKey)
    if (checked && !existing) {
      await agentToolAdd({
        agentId: props.agentId,
        toolSource: 'mcp',
        toolKey: tool.toolKey,
        toolGroup: 'mcp',
        enabled: '1',
      })
      window.$message.success(`已绑定 MCP 工具：${tool.title || tool.toolKey}`)
    }
    else if (!checked && existing) {
      await agentToolDelete(existing.id)
      window.$message.success(`已解除 MCP 工具：${tool.title || tool.toolKey}`)
    }
    await loadData()
    emit('changed')
  }
  catch (e) {
    window.$message.error(e.message || '操作失败')
  }
  finally {
    busyKey.value = ''
  }
}

async function toggleSkillPackage(pkg, checked) {
  busyKey.value = String(pkg.versionId)
  try {
    if (checked) {
      await bindAgentSkillPackage({ agentId: props.agentId, versionId: pkg.versionId })
      window.$message.success(`已绑定技能资料包：${pkg.packageName} v${pkg.versionName}`)
    }
    else {
      const bound = boundPkgMap.value.get(String(pkg.packageId))
      if (bound)
        await unbindAgentSkillPackage(props.agentId, bound.versionId)
      window.$message.success(`已解除技能资料包：${pkg.packageName}`)
    }
    await loadData()
    emit('changed')
  }
  catch (e) {
    window.$message.error(e.message || '操作失败')
  }
  finally {
    busyKey.value = ''
  }
}

watch(() => [props.agentId, props.mode], () => {
  void loadData()
}, { immediate: true })
</script>

<template>
  <NSpin :show="loading">
    <div v-if="mode === 'mcp'" class="binding-list">
      <div v-for="tool in mcpCatalog" :key="tool.toolKey" class="binding-row">
        <div class="binding-row__info">
          <div class="binding-row__title">
            <strong>{{ tool.title || tool.toolKey }}</strong>
            <NTag size="tiny" type="primary" :bordered="false">
              {{ tool.sourceLabel || 'MCP' }}
            </NTag>
          </div>
          <div class="binding-row__meta">
            {{ tool.toolKey }} · {{ tool.description || '无描述' }}
          </div>
        </div>
        <NSwitch
          size="small"
          :value="boundMcpMap.has(tool.toolKey)"
          :loading="busyKey === tool.toolKey"
          @update:value="val => toggleMcpTool(tool, val)"
        />
      </div>
      <NEmpty v-if="!loading && !mcpCatalog.length" description="请先在「资源中心 → MCP 服务」接入并「测试并同步」" size="small" />
    </div>

    <div v-else class="binding-list">
      <div v-for="pkg in readyPackages" :key="pkg.versionId" class="binding-row">
        <div class="binding-row__info">
          <div class="binding-row__title">
            <strong>{{ pkg.packageName }}</strong>
            <NTag size="tiny" type="success" :bordered="false">
              v{{ pkg.versionName }}
            </NTag>
          </div>
          <div class="binding-row__meta">
            {{ pkg.packageCode }} · {{ pkg.description || '只读文档资料，按需查阅' }}
          </div>
        </div>
        <NSwitch
          size="small"
          :value="boundPkgMap.has(String(pkg.packageId))"
          :loading="busyKey === String(pkg.versionId)"
          @update:value="val => toggleSkillPackage(pkg, val)"
        />
      </div>
      <NEmpty v-if="!loading && !readyPackages.length" description="请先在「资源中心 → 技能资料」导入并启用技能包" size="small" />
    </div>

    <div class="binding-refresh">
      <NButton text size="tiny" @click="loadData">
        刷新列表
      </NButton>
    </div>
  </NSpin>
</template>

<style scoped>
.binding-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 320px;
  overflow: auto;
}

.binding-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
}

.binding-row__info {
  min-width: 0;
  flex: 1;
}

.binding-row__title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.binding-row__meta {
  margin-top: 2px;
  font-size: 11px;
  color: var(--text-tertiary, #6b7280);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.binding-refresh {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}
</style>
