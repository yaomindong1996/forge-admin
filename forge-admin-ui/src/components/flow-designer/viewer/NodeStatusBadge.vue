<script setup>
/**
 * NodeStatusBadge — 节点状态徽章（独立组件）
 *
 * 用法：在节点旁边或节点列表中显示节点运行状态。
 * NodeCard 内部已经有 inline 徽章，这是给查看器汇总场景用的独立标签。
 *
 * Props:
 *   - status: 'completed' | 'running' | 'pending' | 'rejected' | 'skipped' | null
 *   - size: 'tiny' | 'small' | 'medium'
 */
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, default: null },
  size: { type: String, default: 'small' },
})

const STATUS_META = {
  completed: { label: '已完成', class: 'bg-success/10 text-success border-success/30' },
  running: { label: '审批中', class: 'bg-info/10 text-info border-info/30' },
  pending: { label: '待办', class: 'bg-gray-100 text-gray-500 border-gray-300' },
  rejected: { label: '已驳回', class: 'bg-error/10 text-error border-error/30' },
  skipped: { label: '已跳过', class: 'bg-gray-100 text-gray-400 border-gray-200' },
}

const meta = computed(() => STATUS_META[props.status] || null)

const sizeClass = computed(() => {
  switch (props.size) {
    case 'tiny': return 'text-[10px] px-1 py-0'
    case 'medium': return 'text-sm px-2 py-1'
    default: return 'text-xs px-1.5 py-0.5'
  }
})
</script>

<template>
  <span
    v-if="meta"
    class="node-status-badge inline-flex items-center border rounded"
    :class="[meta.class, sizeClass]"
  >
    {{ meta.label }}
  </span>
</template>
