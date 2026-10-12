<template>
  <div class="tool-call-card">
    <div class="tool-header">
      <NIcon size="14" class="tool-icon"><ConstructOutline /></NIcon>
      <NTag size="small" type="info" :bordered="false">
        {{ toolCall.name }}
      </NTag>
      <NTag
        v-if="toolCall.status"
        size="tiny"
        :bordered="false"
        :type="toolCall.status === 'error' ? 'error' : toolCall.status === 'done' ? 'success' : 'default'"
      >
        {{ toolStatusLabel(toolCall.status) }}
      </NTag>
    </div>
    <div v-if="hasVal(toolCall.args)" class="tool-args">
      <span class="tool-label">参数</span>
      <pre class="tool-code">{{ formatToolValue(toolCall.args) }}</pre>
    </div>
    <div v-if="hasVal(toolCall.result)" class="tool-result">
      <span class="tool-label">结果</span>
      <pre class="tool-code">{{ formatToolValue(toolCall.result) }}</pre>
    </div>
  </div>
</template>

<script setup>
import { ConstructOutline } from '@vicons/ionicons5'
import { NIcon, NTag } from 'naive-ui'
import { formatToolValue, hasVal, toolStatusLabel } from './chat-utils'

defineOptions({ name: 'ChatToolCard' })

defineProps({
  // 单次工具调用：{ name, args, result, status, error }
  toolCall: {
    type: Object,
    required: true,
  },
})
</script>

<style scoped>
.tool-call-card {
  padding: 10px 12px;
  margin-bottom: 8px;
  background: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 10px;
  font-size: 12px;
}

.tool-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.tool-icon {
  color: var(--text-muted);
}

.tool-args,
.tool-result {
  margin-top: 6px;
  color: var(--text-muted);
}

.tool-label {
  display: block;
  margin-bottom: 2px;
  font-weight: 500;
  color: var(--text-body);
}

.tool-code {
  margin: 0;
  max-height: 160px;
  padding: 8px 10px;
  overflow: auto;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 8px;
  font-size: 11px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

:global(.dark) .tool-code {
  background: color-mix(in srgb, var(--bg-primary) 5%, transparent);
}
</style>
