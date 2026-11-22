<template>
  <details
    v-if="hasContent"
    class="chat-run-trace"
    :open="forceOpen || undefined"
  >
    <summary class="chat-run-trace__summary">
      <span class="chat-run-trace__title">{{ summaryText }}</span>
      <span class="chat-run-trace__hint">点击展开详情</span>
    </summary>

    <div v-if="resourceChips.length" class="chat-run-trace__resources">
      <span
        v-for="chip in resourceChips"
        :key="chip.key"
        class="chat-run-trace__chip"
        :data-kind="chip.kind"
      >
        {{ chip.label }}
      </span>
    </div>

    <ul v-if="tools.length" class="chat-run-trace__tools">
      <li v-for="(tool, idx) in tools" :key="tool.callId || `${tool.name}-${idx}`">
        <button
          type="button"
          class="chat-run-trace__tool-row"
          @click="toggleDetail(idx)"
        >
          <span class="chat-run-trace__tool-main">
            <em v-if="tool.source === 'mcp'">MCP</em>
            <strong>{{ tool.label || tool.name || '工具' }}</strong>
          </span>
          <small :class="`is-${tool.status || 'running'}`">{{ toolStatusLabel(tool.status) }}</small>
        </button>
        <div v-if="expandedIdx === idx" class="chat-run-trace__detail">
          <ToolCard :tool-call="tool" />
        </div>
      </li>
    </ul>
  </details>
</template>

<script setup>
import { computed, ref } from 'vue'
import { toolStatusLabel } from './chat-utils'
import ToolCard from './ToolCard.vue'

defineOptions({ name: 'ChatRunTrace' })

const props = defineProps({
  /** 工具调用列表：{ name, label?, source?, status, args?, result?, error?, callId? } */
  tools: { type: Array, default: () => [] },
  /** 本轮可用资源：{ skills?: string[], knowledge?: string[] } */
  resources: { type: Object, default: () => ({}) },
  /** 流式中可强制展开（一般仍保持收起） */
  forceOpen: { type: Boolean, default: false },
})

const expandedIdx = ref(-1)

const resourceChips = computed(() => {
  const chips = []
  for (const name of props.resources?.skills || []) {
    if (name)
      chips.push({ key: `skill:${name}`, kind: 'skill', label: `技能资料 · ${name}` })
  }
  for (const name of props.resources?.knowledge || []) {
    if (name)
      chips.push({ key: `kb:${name}`, kind: 'knowledge', label: `知识库 · ${name}` })
  }
  return chips
})

const hasContent = computed(() => props.tools.length > 0 || resourceChips.value.length > 0)

const summaryText = computed(() => {
  const parts = []
  if (props.tools.length) {
    const names = new Set(props.tools.map(t => t.name).filter(Boolean))
    parts.push(names.size && names.size < props.tools.length
      ? `工具 ${names.size} 种 / ${props.tools.length} 次`
      : `工具 ${props.tools.length} 次`)
  }
  const skillCount = (props.resources?.skills || []).filter(Boolean).length
  const kbCount = (props.resources?.knowledge || []).filter(Boolean).length
  if (skillCount)
    parts.push(`技能资料 ${skillCount}`)
  if (kbCount)
    parts.push(`知识库 ${kbCount}`)
  return parts.length ? parts.join(' · ') : '运行详情'
})

function toggleDetail(idx) {
  expandedIdx.value = expandedIdx.value === idx ? -1 : idx
}
</script>

<style scoped>
.chat-run-trace {
  margin: 6px 0 10px;
  padding: 0;
  border: 1px solid var(--border-light, var(--border));
  border-radius: 10px;
  background: var(--bg-soft, var(--bg-secondary));
  font-size: 12px;
  color: var(--text-secondary);
}

.chat-run-trace__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  list-style: none;
  user-select: none;
}

.chat-run-trace__summary::-webkit-details-marker {
  display: none;
}

.chat-run-trace__title {
  font-weight: 600;
  color: var(--text-primary);
}

.chat-run-trace__hint {
  flex: none;
  color: var(--text-tertiary, var(--text-muted));
  font-size: 11px;
}

.chat-run-trace[open] .chat-run-trace__hint {
  opacity: 0.55;
}

.chat-run-trace__resources {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 12px 8px;
}

.chat-run-trace__chip {
  padding: 2px 8px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--primary-color, #3b82f6) 10%, transparent);
  color: var(--text-secondary);
  font-size: 11px;
}

.chat-run-trace__chip[data-kind='knowledge'] {
  background: color-mix(in srgb, #16a34a 12%, transparent);
}

.chat-run-trace__tools {
  display: grid;
  gap: 4px;
  margin: 0;
  padding: 0 8px 8px;
  list-style: none;
}

.chat-run-trace__tool-row {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 8px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.chat-run-trace__tool-row:hover {
  background: color-mix(in srgb, var(--bg-primary, #fff) 6%, transparent);
}

.chat-run-trace__tool-main {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.chat-run-trace__tool-main em {
  flex: none;
  padding: 0 5px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--primary-color, #3b82f6) 14%, transparent);
  color: var(--primary-color, #2563eb);
  font-style: normal;
  font-size: 10px;
}

.chat-run-trace__tool-main strong {
  overflow: hidden;
  font-weight: 600;
  color: var(--text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-run-trace__tools small.is-done,
.chat-run-trace__tools small.is-success {
  color: #16a34a;
}

.chat-run-trace__tools small.is-error,
.chat-run-trace__tools small.is-aborted {
  color: var(--error-color, #dc2626);
}

.chat-run-trace__detail {
  padding: 0 4px 4px;
}
</style>
