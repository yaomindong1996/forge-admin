<template>
  <div class="tool-call-card">
    <div class="tool-header">
      <NIcon size="14" class="tool-icon">
        <ConstructOutline />
      </NIcon>
      <NTag size="small" type="info" :bordered="false">
        {{ toolCall.name }}
      </NTag>
      <NTag
        v-if="toolCall.status"
        size="tiny"
        :bordered="false"
        :type="tagType"
      >
        {{ toolStatusLabel(toolCall.status) }}
      </NTag>
    </div>
    <div v-if="toolCall.error" class="tool-error" role="alert">
      {{ toolCall.error }}
    </div>
    <div v-if="hasVal(toolCall.args)" class="tool-args">
      <span class="tool-label">参数</span>
      <pre class="tool-code">{{ formatToolValue(toolCall.args) }}</pre>
    </div>
    <!-- 只识别已完成的内置图片任务，固定 ID 查询，不访问返回的任意 URL -->
    <ImageJobResult v-if="imageTask" :record-id="imageTask.recordId" class="tool-result" />
    <div v-else-if="hasVal(toolCall.result)" class="tool-result">
      <span class="tool-label">结果</span>
      <pre class="tool-code">{{ formatToolValue(toolCall.result) }}</pre>
    </div>
  </div>
</template>

<script setup>
import { ConstructOutline } from '@vicons/ionicons5'
import { NIcon, NTag } from 'naive-ui'
import { computed } from 'vue'
import { imageToolTask } from '@/stores/ai/image-job-utils'
import ImageJobResult from '../image/ImageJobResult.vue'
import { formatToolValue, hasVal, toolStatusLabel } from './chat-utils'

defineOptions({ name: 'ChatToolCard' })

const props = defineProps({
  // 单次工具调用：{ name, args, result, status, error }
  toolCall: {
    type: Object,
    required: true,
  },
})

const imageTask = computed(() => imageToolTask(props.toolCall))
const tagType = computed(() => {
  if (props.toolCall.status === 'error') {
    return 'error'
  }
  return ['done', 'success'].includes(props.toolCall.status) ? 'success' : 'default'
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

.tool-error {
  margin-top: 6px;
  color: var(--error-color);
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
