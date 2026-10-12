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
import { computed, onMounted, onUnmounted } from 'vue'
import NodeStatusBadge from './NodeStatusBadge.vue'

const props = defineProps({
  visible: Boolean,
  position: { type: Object, default: () => ({ x: 0, y: 0 }) },
  node: { type: Object, default: null },
  taskInfo: { type: Object, default: null },
})

const emit = defineEmits(['close'])

const PANEL_WIDTH = 360
const PANEL_MARGIN = 16

const popoverStyle = computed(() => {
  const viewportWidth = typeof window !== 'undefined' ? window.innerWidth : 1200
  const viewportHeight = typeof window !== 'undefined' ? window.innerHeight : 800
  const width = Math.min(PANEL_WIDTH, Math.max(300, viewportWidth - PANEL_MARGIN * 2))
  const maxHeight = Math.max(240, viewportHeight - PANEL_MARGIN * 2)
  const rawLeft = Number(props.position?.x) || PANEL_MARGIN
  const rawTop = Number(props.position?.y) || PANEL_MARGIN
  const left = clamp(rawLeft, PANEL_MARGIN, viewportWidth - width - PANEL_MARGIN)
  const top = clamp(rawTop, PANEL_MARGIN, viewportHeight - Math.min(maxHeight, 320) - PANEL_MARGIN)
  return {
    left: `${left}px`,
    top: `${top}px`,
    width: `${width}px`,
    maxHeight: `${maxHeight}px`,
  }
})

const assigneeName = computed(() => props.taskInfo?.assigneeName || props.taskInfo?.assignee || '未指派')

const resultMeta = computed(() => {
  const result = String(props.taskInfo?.result || '').toLowerCase()
  if (['rejected', 'reject', 'denied'].includes(result))
    return { label: '驳回', class: 'text-error' }
  if (['approved', 'approve', 'passed', 'pass', 'completed'].includes(result))
    return { label: '通过', class: 'text-success' }
  return { label: props.taskInfo?.result || '—', class: 'text-gray-700' }
})

const detailRows = computed(() => {
  if (!props.taskInfo)
    return []
  const rows = [
    { label: '处理人', value: assigneeName.value },
  ]
  if (props.taskInfo.startTime)
    rows.push({ label: '开始', value: fmtDate(props.taskInfo.startTime) })
  if (props.taskInfo.endTime)
    rows.push({ label: '完成', value: fmtDate(props.taskInfo.endTime) })
  if (props.taskInfo.result)
    rows.push({ label: '结果', value: resultMeta.value.label, class: resultMeta.value.class })
  return rows
})

function clamp(value, min, max) {
  if (max < min)
    return min
  return Math.min(Math.max(value, min), max)
}

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
    class="node-detail-popover fixed z-50 overflow-auto border border-gray-200 rounded-lg bg-white shadow-xl"
    :style="popoverStyle"
    @click.stop
  >
    <div class="detail-header">
      <div class="min-w-0">
        <div class="detail-title">
          {{ node.name || node.id }}
        </div>
      </div>
      <div class="flex shrink-0 items-center gap-2">
        <NodeStatusBadge :status="taskInfo?.status" size="small" />
        <button class="detail-close" aria-label="关闭详情" @click="emit('close')">
          <i class="i-mdi-close" />
        </button>
      </div>
    </div>

    <div v-if="taskInfo" class="detail-body">
      <div class="detail-list">
        <div v-for="row in detailRows" :key="row.label" class="detail-row">
          <span>{{ row.label }}</span>
          <strong :class="row.class">{{ row.value }}</strong>
        </div>
      </div>

      <div v-if="taskInfo.comment" class="detail-comment">
        <div class="detail-comment-label">
          审批意见
        </div>
        <div class="detail-comment-content">
          {{ taskInfo.comment }}
        </div>
      </div>
    </div>

    <div v-else class="detail-empty">
      暂无处理记录
    </div>
  </div>
</template>

<style scoped>
.node-detail-popover {
  color: var(--text-primary);
  border-radius: 8px;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #eef2f7;
}

.detail-title {
  min-width: 0;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-close {
  display: inline-flex;
  width: 26px;
  height: 26px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: var(--text-tertiary);
  transition:
    background-color 160ms ease,
    color 160ms ease;
}

.detail-close:hover {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.detail-body {
  display: grid;
  gap: 12px;
  padding: 12px 14px 14px;
}

.detail-list {
  display: grid;
  gap: 2px;
  padding: 2px 0;
}

.detail-row {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  align-items: start;
  gap: 10px;
  min-width: 0;
  padding: 6px 0;
}

.detail-row span,
.detail-comment-label {
  display: block;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.45;
}

.detail-row strong {
  display: block;
  min-width: 0;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.45;
  text-align: right;
  overflow-wrap: anywhere;
}

.detail-comment {
  padding: 10px 11px;
  border-left: 3px solid #d8e1ec;
  border-radius: 6px;
  background: var(--bg-secondary);
}

.detail-comment-label {
  margin-bottom: 4px;
}

.detail-comment-content {
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.55;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.detail-empty {
  padding: 22px 14px;
  color: var(--text-tertiary);
  font-size: 13px;
  text-align: center;
}

@media (max-width: 520px) {
  .detail-header {
    padding: 12px;
  }

  .detail-body {
    padding: 12px;
  }

  .detail-row {
    grid-template-columns: 52px minmax(0, 1fr);
  }
}
</style>
