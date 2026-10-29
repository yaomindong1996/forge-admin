<script setup>
/**
 * EdgePath — 单条连线
 * 选中后：仅纯横/竖段出蓝块并跟手平移；斜线段无蓝块，拖动才生成拐点（单击不加）
 */
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ARROW_SIZE, buildArrowPolygon, shortenPointsForArrow } from './edge-arrow.js'
import { pinHoverOnSegment, projectHoverOnOrthoSegment } from './edge-segment-edit.js'
import {
  buildPathD,
  getEdgeColor,
  getEdgeDashArray,
  getEdgeMidpoint,
} from '../utils/path-builder.js'

/** 设计器默认线色：比点阵底更深一档，保证灰线上也能看清 */
const DESIGNER_EDGE = '#5b6f86'

const props = defineProps({
  edge: { type: Object, required: true },
  path: { type: Object, required: true },
  status: { type: String, default: null },
  pathType: { type: String, default: null },
  showLabel: { type: Boolean, default: null },
  neutral: Boolean,
  markerPrefix: { type: String, default: '' },
  interactive: Boolean,
  selected: Boolean,
  hideEndpoint: { type: String, default: null },
  activeEndpoint: { type: String, default: null },
  selectedWaypointIndex: { type: Number, default: null },
  /** 设计器建议 true：不用 marker */
  geometricArrow: { type: Boolean, default: false },
  /** stroke=只画线；chrome=只画选中把手；full=全部 */
  renderMode: { type: String, default: 'full' },
})

const emit = defineEmits([
  'click',
  'hitPointerDown',
  'endpointDown',
  'bendDown',
  'segmentDown',
])

const points = computed(() => {
  const raw = props.path?.points
  if (!Array.isArray(raw))
    return []
  return raw.filter(p => Number.isFinite(p?.x) && Number.isFinite(p?.y))
})

const useGeometricArrow = computed(() => props.geometricArrow || (props.interactive && props.neutral))

/** 可见描边：几何箭头时缩短末段，避免线从三角穿出 */
const strokePoints = computed(() => {
  const pts = points.value
  if (pts.length < 2)
    return pts
  if (useGeometricArrow.value && props.hideEndpoint !== 'target' && !props.selected)
    return shortenPointsForArrow(pts, ARROW_SIZE)
  return pts
})

const pathD = computed(() => {
  const pts = strokePoints.value
  if (pts.length < 2)
    return ''
  // 设计器强制正交/直线，避开 rounded 短段问题
  const type = pts.length === 2 ? 'straight' : 'orthogonal'
  const d = buildPathD(pts, type)
  if (d && !d.includes('NaN') && !d.includes('undefined'))
    return d
  const a = pts[0]
  const b = pts[pts.length - 1]
  return `M ${a.x},${a.y} L ${b.x},${b.y}`
})

/** 命中热区仍用完整折点，保证端点附近可点 */
const hitPathD = computed(() => {
  const pts = points.value
  if (pts.length < 2)
    return ''
  const type = pts.length === 2 ? 'straight' : 'orthogonal'
  return buildPathD(pts, type) || ''
})

/** 端点手柄贴在真实折点上（不再外推），与对接点同心，避免双圈错位 */
const endpoints = computed(() => {
  const pts = points.value
  if (!pts.length)
    return { start: null, end: null, startHandle: null, endHandle: null }
  const start = pts[0]
  const end = pts[pts.length - 1]
  return {
    start,
    end,
    startHandle: start,
    endHandle: end,
  }
})

/** 已存在的中间拐点（拖动生成后才有） */
const bendHandles = computed(() => {
  if (!props.selected || !props.interactive)
    return []
  const pts = points.value
  if (pts.length < 3)
    return []
  return pts.slice(1, -1).map((p, i) => ({
    x: p.x,
    y: p.y,
    index: i,
  }))
})

/** 悬停跟手的蓝色线段块（只显示一个，跟着鼠标在横/竖段上滑） */
const hoverSeg = ref(null)
/** 拖段中：蓝块钉在线段上，随折点刷新，避免跟线分离 */
const segDrag = ref(null)
let segDragMove = null
let segDragUp = null
/** full=线+把手；stroke=线+热区+蓝块（压在节点下）；chrome=端点/拐点（抬到节点上） */
const showStroke = computed(() => props.renderMode !== 'chrome')
const showHit = computed(() => props.renderMode !== 'chrome' && Boolean(props.interactive))
const showSegHandle = computed(() => props.renderMode !== 'chrome' && props.selected && props.interactive)
const showEndpointChrome = computed(() => props.renderMode !== 'stroke' && props.selected && props.interactive)

watch(() => props.selected, (v) => {
  if (!v) {
    hoverSeg.value = null
    stopSegDragTrack()
  }
})

watch(points, () => {
  if (!segDrag.value || !props.selected)
    return
  hoverSeg.value = pinHoverOnSegment(
    points.value,
    segDrag.value.segIndex,
    segDrag.value.t,
    segDrag.value.horizontal,
  )
  if (hoverSeg.value)
    segDrag.value = { ...segDrag.value, segIndex: hoverSeg.value.segIndex }
})

onBeforeUnmount(() => stopSegDragTrack())

function clientToLocal(event, svgEl = null) {
  const svg = svgEl
    || event.currentTarget?.ownerSVGElement
    || event.target?.ownerSVGElement
  if (!svg?.createSVGPoint || !svg.getScreenCTM)
    return null
  const pt = svg.createSVGPoint()
  pt.x = event.clientX
  pt.y = event.clientY
  const ctm = svg.getScreenCTM()
  if (!ctm)
    return null
  const local = pt.matrixTransform(ctm.inverse())
  return { x: local.x, y: local.y }
}

function updateHoverSeg(event) {
  if (!props.selected || !props.interactive || segDrag.value)
    return
  const local = clientToLocal(event)
  if (!local) {
    hoverSeg.value = null
    return
  }
  hoverSeg.value = projectHoverOnOrthoSegment(points.value, local.x, local.y, 18)
}

/** 热区 ↔ 蓝块互切时 relatedTarget 仍在本边上，不要清掉，否则闪烁 */
function isStillOnEdgeUi(related) {
  if (!related || typeof related.closest !== 'function')
    return false
  return Boolean(
    related.closest('.edge-hit')
    || related.closest('.edge-seg-handle')
    || related.closest('.edge-bend')
    || related.closest('.edge-endpoint-group'),
  )
}

function onHitPointerLeave(event) {
  if (segDrag.value || isStillOnEdgeUi(event.relatedTarget))
    return
  hoverSeg.value = null
}

function stopSegDragTrack() {
  if (segDragMove)
    window.removeEventListener('pointermove', segDragMove, true)
  if (segDragUp)
    window.removeEventListener('pointerup', segDragUp, true)
  segDragMove = null
  segDragUp = null
  segDrag.value = null
}

function onSegHandlePointerDown(event) {
  if (!hoverSeg.value)
    return
  const svg = event.currentTarget?.ownerSVGElement || event.target?.ownerSVGElement
  segDrag.value = {
    segIndex: hoverSeg.value.segIndex,
    t: hoverSeg.value.t,
    horizontal: hoverSeg.value.horizontal,
  }
  emit('segmentDown', hoverSeg.value.segIndex, event)

  // 父级在 window 上拖线；这里同步钉住蓝块，防止线走块留
  segDragMove = (e) => {
    if (!segDrag.value)
      return
    const local = clientToLocal(e, svg)
    const pinned = pinHoverOnSegment(
      points.value,
      segDrag.value.segIndex,
      segDrag.value.t,
      segDrag.value.horizontal,
    )
    if (!pinned) {
      hoverSeg.value = null
      return
    }
    // 沿段方向跟手，法向钉在线上
    if (local) {
      const a = points.value[pinned.segIndex]
      const b = points.value[pinned.segIndex + 1]
      if (a && b) {
        const dx = b.x - a.x
        const dy = b.y - a.y
        const len2 = dx * dx + dy * dy
        let t = len2 < 1e-6 ? 0.5 : ((local.x - a.x) * dx + (local.y - a.y) * dy) / len2
        t = Math.max(0.08, Math.min(0.92, t))
        segDrag.value = { ...segDrag.value, segIndex: pinned.segIndex, t }
        hoverSeg.value = {
          segIndex: pinned.segIndex,
          x: a.x + dx * t,
          y: a.y + dy * t,
          horizontal: pinned.horizontal,
          t,
        }
        return
      }
    }
    hoverSeg.value = pinned
  }
  segDragUp = () => stopSegDragTrack()
  window.addEventListener('pointermove', segDragMove, true)
  window.addEventListener('pointerup', segDragUp, true)
}

const strokeColor = computed(() => {
  if (props.selected)
    return '#356de0'
  // 设计器中性线：主题灰蓝，比死黑柔和，比浅灰更清晰
  if (props.neutral && !props.status)
    return DESIGNER_EDGE
  return getEdgeColor(props.edge, props.status) || DESIGNER_EDGE
})

const dashArray = computed(() => {
  if (props.interactive && props.neutral)
    return null
  return getEdgeDashArray(props.status)
})

const midpoint = computed(() => getEdgeMidpoint(points.value))
const strokeWidth = computed(() => (props.selected ? 2.8 : (props.neutral ? 2.4 : 1.7)))

const labelVisible = computed(() => {
  if (props.showLabel != null)
    return props.showLabel
  if (props.edge?.branchId)
    return false
  return Boolean(props.edge?.condition || props.edge?.isDefault)
})

const labelText = computed(() => {
  if (props.edge?.isDefault)
    return '默认'
  if (!props.edge?.condition)
    return ''
  const ruleCount = Array.isArray(props.edge?.conditionRules) ? props.edge.conditionRules.length : 0
  return ruleCount > 1 ? `${ruleCount} 条条件` : '条件已设'
})

const labelWidth = computed(() => Math.min(Math.max(labelText.value.length * 8 + 22, 58), 176))

const markerId = computed(() => {
  if (props.selected)
    return 'arrow-selected'
  switch (props.status) {
    case 'completed': return 'arrow-completed'
    case 'rejected': return 'arrow-rejected'
    case 'running': return 'arrow-running'
    default: return props.edge?.isDefault ? 'arrow-default-branch' : 'arrow-default'
  }
})

const arrowPoints = computed(() => {
  // 选中态只显示端点圆，避免箭头与圆叠成「穿帮」
  if (!useGeometricArrow.value || props.hideEndpoint === 'target' || props.selected)
    return null
  return buildArrowPolygon(points.value, ARROW_SIZE)
})

const markerEnd = computed(() => {
  if (useGeometricArrow.value || props.hideEndpoint === 'target')
    return undefined
  return `url(#${props.markerPrefix}${markerId.value})`
})
</script>

<template>
  <g class="edge-path" :class="{ 'is-interactive': interactive, 'is-selected': selected }">
    <path
      v-if="showHit && hitPathD"
      :d="hitPathD"
      stroke="transparent"
      stroke-width="20"
      fill="none"
      class="edge-hit"
      @click.stop="emit('click', edge)"
      @pointerdown.stop="updateHoverSeg($event); emit('hitPointerDown', $event)"
      @pointermove="updateHoverSeg"
      @pointerleave="onHitPointerLeave"
    />
    <!-- 禁止写 SVG opacity="1"：UnoCSS [opacity~=\"1\"] 会当成 opacity-1=0.01，线全透明 -->
    <path
      v-if="showStroke && pathD"
      class="edge-stroke"
      :d="pathD"
      :stroke="strokeColor"
      :stroke-width="strokeWidth"
      fill="none"
      :stroke-linecap="useGeometricArrow && !selected ? 'butt' : 'round'"
      stroke-linejoin="round"
      :stroke-dasharray="dashArray || undefined"
      :marker-end="markerEnd"
      pointer-events="none"
      :style="hideEndpoint ? { opacity: 0.35 } : undefined"
    />
    <polygon
      v-if="showStroke && arrowPoints"
      class="edge-arrow"
      :points="arrowPoints"
      :fill="strokeColor"
      pointer-events="none"
      :style="hideEndpoint ? { opacity: 0.35 } : undefined"
    />
    <g v-if="showStroke && labelVisible && labelText" :transform="`translate(${midpoint.x}, ${midpoint.y})`" pointer-events="none">
      <rect
        :x="`${-labelWidth / 2}`" y="-10" :width="labelWidth" height="20"
        rx="6" ry="6"
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

    <!-- 蓝块跟线：画在描边层，拖段时钉住不分离 -->
    <rect
      v-if="showSegHandle && hoverSeg"
      class="edge-seg-handle"
      :class="hoverSeg.horizontal ? 'is-h' : 'is-v'"
      :x="hoverSeg.horizontal ? hoverSeg.x - 11 : hoverSeg.x - 4"
      :y="hoverSeg.horizontal ? hoverSeg.y - 4 : hoverSeg.y - 11"
      :width="hoverSeg.horizontal ? 22 : 8"
      :height="hoverSeg.horizontal ? 8 : 22"
      rx="4"
      ry="4"
      :title="hoverSeg.horizontal ? '拖动：上下调整' : '拖动：左右调整'"
      @pointerdown.stop.prevent="onSegHandlePointerDown"
      @pointermove="updateHoverSeg"
      @pointerleave="onHitPointerLeave"
    />

    <circle
      v-for="bend in bendHandles"
      v-show="showEndpointChrome"
      :key="`bend-${bend.index}`"
      class="edge-bend"
      :class="{ 'is-active': selectedWaypointIndex === bend.index }"
      :cx="bend.x"
      :cy="bend.y"
      r="4"
      title="拖动拐点"
      @pointerdown.stop.prevent="emit('bendDown', bend.index, $event)"
    />

    <!-- 端点抬到节点上，贴齐四边中心 -->
    <g
      v-if="showEndpointChrome && endpoints.startHandle && hideEndpoint !== 'source'"
      class="edge-endpoint-group"
      @pointerdown.stop.prevent="emit('endpointDown', 'source', $event)"
    >
      <circle
        class="edge-endpoint-hit"
        :cx="endpoints.startHandle.x"
        :cy="endpoints.startHandle.y"
        r="10"
      />
      <circle
        class="edge-endpoint"
        :class="{ 'is-active': activeEndpoint === 'source' }"
        :cx="endpoints.startHandle.x"
        :cy="endpoints.startHandle.y"
        r="4.5"
      >
        <title>拖动起点：可换到本节点其它边，或接到其它节点</title>
      </circle>
    </g>
    <g
      v-if="showEndpointChrome && endpoints.endHandle && hideEndpoint !== 'target'"
      class="edge-endpoint-group"
      @pointerdown.stop.prevent="emit('endpointDown', 'target', $event)"
    >
      <circle
        class="edge-endpoint-hit"
        :cx="endpoints.endHandle.x"
        :cy="endpoints.endHandle.y"
        r="10"
      />
      <circle
        class="edge-endpoint"
        :class="{ 'is-active': activeEndpoint === 'target' }"
        :cx="endpoints.endHandle.x"
        :cy="endpoints.endHandle.y"
        r="4.5"
      >
        <title>拖动终点：可换到本节点其它边，或接到其它节点</title>
      </circle>
    </g>
  </g>
</template>

<style scoped>
.edge-path.is-interactive .edge-hit {
  pointer-events: stroke;
  cursor: pointer;
}
.edge-path.is-selected .edge-hit {
  cursor: grab;
}
.edge-stroke {
  vector-effect: non-scaling-stroke;
}
.edge-endpoint-group {
  pointer-events: all;
  cursor: grab;
}
.edge-endpoint-hit {
  fill: transparent;
  stroke: none;
  pointer-events: all;
  cursor: grab;
}
.edge-endpoint,
.edge-bend {
  fill: #fff;
  stroke: #356de0;
  stroke-width: 2.5;
  pointer-events: all;
  cursor: grab;
}
.edge-seg-handle {
  fill: #356de0;
  stroke: #fff;
  stroke-width: 1.5;
  pointer-events: all;
}
.edge-seg-handle.is-h {
  cursor: ns-resize;
}
.edge-seg-handle.is-v {
  cursor: ew-resize;
}
.edge-seg-handle:hover {
  fill: #2554c7;
}
.edge-bend.is-active,
.edge-endpoint.is-active {
  fill: #356de0;
  stroke: #fff;
}
.edge-endpoint:active,
.edge-bend:active,
.edge-seg-handle:active,
.edge-path.is-selected .edge-hit:active {
  cursor: grabbing;
}
</style>
