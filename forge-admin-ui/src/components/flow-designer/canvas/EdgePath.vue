<script setup>
/**
 * EdgePath — 单条 SVG 边
 *
 * Props:
 *   - edge       flowJson edge 对象（含 condition / isDefault）
 *   - path       { points: [{x,y}], type } — 来自 layoutFlow
 *   - status     节点/边状态：completed / running / pending / rejected / skipped
 *   - pathType   覆盖 path.type，可选 'rounded' / 'bezier' / 'orthogonal' / 'straight'
 *   - showLabel  是否显示条件文本（默认按 edge.condition 判断）
 *
 * 通过 marker-end 添加箭头；条件文本贴在路径中点。
 */
import { computed } from 'vue'
import {
  buildPathD,
  getEdgeColor,
  getEdgeDashArray,
  getEdgeMidpoint,
} from '../utils/path-builder.js'

const props = defineProps({
  edge: { type: Object, required: true },
  path: { type: Object, required: true },
  status: { type: String, default: null },
  pathType: { type: String, default: null }, // 覆盖 path.type
  showLabel: { type: Boolean, default: null },
})

const finalType = computed(() => props.pathType || props.path?.type || 'orthogonal')

const pathD = computed(() => buildPathD(props.path?.points || [], finalType.value))
const strokeColor = computed(() => getEdgeColor(props.edge, props.status))
const dashArray = computed(() => getEdgeDashArray(props.status))
const midpoint = computed(() => getEdgeMidpoint(props.path?.points || []))

const labelVisible = computed(() => {
  if (props.showLabel != null)
    return props.showLabel
  return Boolean(props.edge?.condition || props.edge?.isDefault)
})

const labelText = computed(() => {
  if (props.edge?.isDefault)
    return '默认'
  const text = String(props.edge?.condition || '')
  return text.length > 20 ? `${text.slice(0, 20)}…` : text
})

// 选取与状态一致的 marker
const markerId = computed(() => {
  switch (props.status) {
    case 'completed': return 'arrow-completed'
    case 'rejected': return 'arrow-rejected'
    case 'running': return 'arrow-running'
    default: return props.edge?.isDefault ? 'arrow-default-branch' : 'arrow-default'
  }
})
</script>

<template>
  <g class="edge-path">
    <path
      :d="pathD"
      :stroke="strokeColor"
      stroke-width="1.5"
      fill="none"
      :stroke-dasharray="dashArray || undefined"
      :marker-end="`url(#${markerId})`"
    />
    <g v-if="labelVisible && labelText" :transform="`translate(${midpoint.x}, ${midpoint.y})`">
      <rect
        x="-30" y="-9" width="60" height="18"
        rx="4" ry="4"
        fill="#fff"
        :stroke="strokeColor"
        stroke-width="1"
      />
      <text
        x="0" y="0"
        text-anchor="middle"
        dominant-baseline="middle"
        font-size="11"
        :fill="strokeColor"
      >
        {{ labelText }}
      </text>
    </g>
  </g>
</template>
