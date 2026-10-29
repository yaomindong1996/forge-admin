<script setup>
/**
 * EdgeLayer — SVG 连线层
 *
 * - 所有描边在节点下，线不盖卡片
 * - 选中边的端点/拐点/蓝块抬到节点上，贴齐四边可拖
 */
import { computed, useId } from 'vue'
import { EDGE_COLORS } from '../utils/path-builder.js'
import EdgePath from './EdgePath.vue'

const props = defineProps({
  edges: { type: Array, default: () => [] },
  paths: { type: [Map, Object], default: () => ({}) },
  canvasBounds: { type: Object, default: null },
  nodeStatuses: { type: Object, default: () => ({}) },
  pathType: { type: String, default: 'orthogonal' },
  showLabels: { type: Boolean, default: true },
  neutral: Boolean,
  interactive: Boolean,
  selectedEdgeId: { type: String, default: null },
  reconnecting: { type: Object, default: null },
  selectedWaypointIndex: { type: Number, default: null },
})

const emit = defineEmits(['edgeClick', 'hitPointerDown', 'endpointDown', 'bendDown', 'segmentDown'])

const instanceId = String(useId() || 'edge').replace(/[^a-zA-Z0-9_-]/g, '') || 'edge'
const markerPrefix = computed(() => `e${instanceId}-`)
const colors = computed(() => (props.neutral
  ? { ...EDGE_COLORS, default: '#5b6f86', defaultBranch: '#5b6f86' }
  : EDGE_COLORS))

const svgSize = computed(() => {
  const b = props.canvasBounds || { minX: 0, minY: 0, maxX: 1000, maxY: 800 }
  const pad = 240
  const rawX = Math.max(finiteOr(b.maxX, 0), finiteOr(b.minX, 0), 800) + pad
  const rawY = Math.max(finiteOr(b.maxY, 0), finiteOr(b.minY, 0), 600) + pad
  return {
    width: clampSize(rawX),
    height: clampSize(rawY),
  }
})

const selectedEdge = computed(() => props.edges.find(edge => edge.id === props.selectedEdgeId) || null)

function finiteOr(v, fallback) {
  const n = Number(v)
  return Number.isFinite(n) ? n : fallback
}

function clampSize(v) {
  const n = Math.ceil(Number(v) || 0)
  if (!Number.isFinite(n) || n < 100)
    return 1200
  return Math.min(n, 20000)
}

function statusOf(edge) {
  return props.nodeStatuses?.[edge.source]?.status || null
}

function pathOf(edge) {
  const bag = props.paths
  if (!bag)
    return { points: [], type: props.pathType || 'orthogonal' }
  let found
  if (typeof bag.get === 'function')
    found = bag.get(edge.id)
  else
    found = bag[edge.id]
  if (found?.points?.length >= 2)
    return found
  return { points: [], type: props.pathType || 'orthogonal' }
}

function onEdgeClick(edge) {
  emit('edgeClick', edge)
}
function onHit(edge, event) {
  emit('hitPointerDown', edge, event)
}
function onEndpoint(edge, which, event) {
  emit('endpointDown', edge, which, event)
}
function onBend(edge, index, event) {
  emit('bendDown', edge, index, event)
}
function onSegment(edge, index, event) {
  emit('segmentDown', edge, index, event)
}
</script>

<template>
  <!-- 全部描边在节点下 -->
  <svg
    class="edge-layer"
    :width="svgSize.width"
    :height="svgSize.height"
    :viewBox="`0 0 ${svgSize.width} ${svgSize.height}`"
  >
    <defs>
      <marker
        :id="`${markerPrefix}arrow-default`" viewBox="0 0 10 10" refX="10" refY="5"
        :markerWidth="neutral ? 6 : 8" :markerHeight="neutral ? 6 : 8" orient="auto"
      >
        <path d="M 0 0 L 10 5 L 0 10 z" :fill="colors.default" />
      </marker>
      <marker
        :id="`${markerPrefix}arrow-default-branch`" viewBox="0 0 10 10" refX="10" refY="5"
        :markerWidth="neutral ? 6 : 8" :markerHeight="neutral ? 6 : 8" orient="auto"
      >
        <path d="M 0 0 L 10 5 L 0 10 z" :fill="colors.defaultBranch" />
      </marker>
      <marker
        :id="`${markerPrefix}arrow-completed`" viewBox="0 0 10 10" refX="10" refY="5"
        markerWidth="8" markerHeight="8" orient="auto"
      >
        <path d="M 0 0 L 10 5 L 0 10 z" :fill="EDGE_COLORS.completed" />
      </marker>
      <marker
        :id="`${markerPrefix}arrow-running`" viewBox="0 0 10 10" refX="10" refY="5"
        markerWidth="8" markerHeight="8" orient="auto"
      >
        <path d="M 0 0 L 10 5 L 0 10 z" :fill="EDGE_COLORS.running" />
      </marker>
      <marker
        :id="`${markerPrefix}arrow-rejected`" viewBox="0 0 10 10" refX="10" refY="5"
        markerWidth="8" markerHeight="8" orient="auto"
      >
        <path d="M 0 0 L 10 5 L 0 10 z" :fill="EDGE_COLORS.rejected" />
      </marker>
      <marker
        :id="`${markerPrefix}arrow-selected`" viewBox="0 0 10 10" refX="10" refY="5"
        markerWidth="7" markerHeight="7" orient="auto"
      >
        <path d="M 0 0 L 10 5 L 0 10 z" fill="#356de0" />
      </marker>
    </defs>
    <EdgePath
      v-for="edge in edges"
      :key="`stroke-${edge.id}`"
      :edge="edge"
      :path="pathOf(edge)"
      path-type="orthogonal"
      :status="statusOf(edge)"
      :neutral="neutral"
      :marker-prefix="markerPrefix"
      :show-label="showLabels"
      :interactive="interactive"
      :geometric-arrow="Boolean(interactive && neutral)"
      :selected="selectedEdgeId === edge.id"
      :render-mode="selectedEdgeId === edge.id ? 'stroke' : 'full'"
      :hide-endpoint="reconnecting?.edgeId === edge.id ? reconnecting.which : null"
      :active-endpoint="reconnecting?.edgeId === edge.id ? reconnecting.which : null"
      @click="onEdgeClick(edge)"
      @hit-pointer-down="(event) => onHit(edge, event)"
      @endpoint-down="(which, event) => onEndpoint(edge, which, event)"
      @bend-down="(index, event) => onBend(edge, index, event)"
      @segment-down="(index, event) => onSegment(edge, index, event)"
    />
  </svg>

  <!-- 仅端点/拐点抬到节点上；描边仍在下层，避免线穿字 -->
  <svg
    v-if="selectedEdge"
    class="edge-layer edge-layer--chrome"
    :width="svgSize.width"
    :height="svgSize.height"
    :viewBox="`0 0 ${svgSize.width} ${svgSize.height}`"
  >
    <EdgePath
      :edge="selectedEdge"
      :path="pathOf(selectedEdge)"
      path-type="orthogonal"
      :status="statusOf(selectedEdge)"
      :neutral="neutral"
      :marker-prefix="markerPrefix"
      :show-label="false"
      :interactive="interactive"
      :geometric-arrow="false"
      selected
      render-mode="chrome"
      :hide-endpoint="reconnecting?.edgeId === selectedEdge.id ? reconnecting.which : null"
      :active-endpoint="reconnecting?.edgeId === selectedEdge.id ? reconnecting.which : null"
      :selected-waypoint-index="selectedWaypointIndex"
      @endpoint-down="(which, event) => onEndpoint(selectedEdge, which, event)"
      @bend-down="(index, event) => onBend(selectedEdge, index, event)"
    />
  </svg>
</template>

<style scoped>
.edge-layer {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
  overflow: visible;
  pointer-events: none;
}
.edge-layer--chrome {
  z-index: 6;
}
</style>
