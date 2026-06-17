<script setup>
/**
 * NodeDetailPopover — 节点详情气泡
 *
 * 在查看器中点击节点时显示。展示该节点的运行实例信息：
 *   - 审批人
 *   - 开始/完成时间
 *   - 审批结果（同意 / 驳回）
 *   - 审批意见
 *
 * Props:
 *   - visible
 *   - position: { x, y } 屏幕坐标
 *   - node: flowJson 节点
 *   - taskInfo: { assigneeName, startTime, endTime, result, comment, status } 任务实例信息
 *
 * Events:
 *   - close
 */
import { onMounted, onUnmounted } from 'vue'
import NodeStatusBadge from './NodeStatusBadge.vue'

const props = defineProps({
  visible: Boolean,
  position: { type: Object, default: () => ({ x: 0, y: 0 }) },
  node: { type: Object, default: null },
  taskInfo: { type: Object, default: null },
})

const emit = defineEmits(['close'])

function fmtDate(v) {
  if (!v)
    return '—'
  try {
    const d = typeof v === 'string' ? new Date(v) : v
    if (Number.isNaN(d.getTime()))
      return String(v)
    const Y = d.getFullYear()
    const M = String(d.getMonth() + 1).padStart(2, '0')
    const D = String(d.getDate()).padStart(2, '0')
    const h = String(d.getHours()).padStart(2, '0')
    const m = String(d.getMinutes()).padStart(2, '0')
    return `${Y}-${M}-${D} ${h}:${m}`
  }
  catch {
    return String(v)
  }
}

function handleClickOutside(event) {
  if (!event.target.closest('.node-detail-popover'))
    emit('close')
}

onMounted(() => {
  if (typeof window !== 'undefined')
    window.addEventListener('mousedown', handleClickOutside, true)
})
onUnmounted(() => {
  if (typeof window !== 'undefined')
    window.removeEventListener('mousedown', handleClickOutside, true)
})
</script>

<template>
  <div
    v-if="visible && node"
    class="node-detail-popover text-sm fixed z-50 w-80 border border-gray-200 rounded-md bg-white p-3 shadow-lg"
    :style="{ left: `${position.x}px`, top: `${position.y}px` }"
    @click.stop
  >
    <div class="mb-2 flex items-center justify-between">
      <span class="text-gray-700 font-medium">{{ node.name || node.id }}</span>
      <NodeStatusBadge :status="taskInfo?.status" />
    </div>

    <div class="text-xs text-gray-600 space-y-1">
      <div v-if="taskInfo?.assigneeName">
        <span class="text-gray-400">审批人：</span>
        <span>{{ taskInfo.assigneeName }}</span>
      </div>
      <div v-if="taskInfo?.startTime">
        <span class="text-gray-400">开始时间：</span>
        <span>{{ fmtDate(taskInfo.startTime) }}</span>
      </div>
      <div v-if="taskInfo?.endTime">
        <span class="text-gray-400">完成时间：</span>
        <span>{{ fmtDate(taskInfo.endTime) }}</span>
      </div>
      <div v-if="taskInfo?.result">
        <span class="text-gray-400">审批结果：</span>
        <span :class="taskInfo.result === 'rejected' ? 'text-error' : 'text-success'">
          {{ taskInfo.result === 'rejected' ? '驳回' : '通过' }}
        </span>
      </div>
      <div v-if="taskInfo?.comment" class="rounded bg-gray-50 p-2 text-gray-600">
        <div class="mb-1 text-[11px] text-gray-400">
          审批意见：
        </div>
        <div>{{ taskInfo.comment }}</div>
      </div>
      <div v-if="!taskInfo" class="text-gray-400">
        暂无运行数据
      </div>
    </div>
  </div>
</template>
