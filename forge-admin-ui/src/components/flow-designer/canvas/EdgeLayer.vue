<script setup>
/**
 * EdgeLayer — SVG 连线层
 *
 * Props:
 *   - edges        flowJson.edges 数组
 *   - paths        Map<edgeId, {points, type}>（layoutFlow 输出）
 *   - canvasBounds layoutFlow 的 canvasBounds，用于计算 SVG 尺寸
 *   - nodeStatuses { [bpmnElementId]: { status } } 可选，查看器场景使用
 *   - pathType     全局 path 类型覆盖（rounded / orthogonal / bezier / straight）
 *
 * 使用 SVG <defs> 注册 5 种箭头 marker。
 */
import { computed } from 'vue'
import { EDGE_COLORS } from '../utils/path-builder.js'
import EdgePath from './EdgePath.vue'

const props = defineProps({
  edges: { type: Array, default: () => [] },
  paths: { type: Map, default: () => new Map() },
  canvasBounds: { type: Object, default: null },
  nodeStatuses: { type: Object, default: () => ({}) },
  pathType: { type: String, default: 'rounded' },
})

const svgSize = computed(() => {
  const b = props.canvasBounds
  const w = Math.max((b?.maxX || 1000) + 80, 1000)
  const h = Math.max((b?.maxY || 800) + 80, 800)
  return { width: w, height: h }
})

function statusOf(edge) {
  // 边的状态约定：取 source 节点状态（spec 10.8）
  return props.nodeStatuses?.[edge.source]?.status || null
}
</script>

<template>
  <svg
    class="edge-layer pointer-events-none absolute left-0 top-0"
    :width="svgSize.width"
    :height="svgSize.height"
    :viewBox="`0 0 ${svgSize.width} ${svgSize.height}`"
  >
    <defs>
      <marker id="arrow-default" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="8" markerHeight="8" orient="auto">
        <path d="M 0 0 L 10 5 L 0 10 z" :fill="EDGE_COLORS.default" />
      </marker>
      <marker id="arrow-default-branch" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="8" markerHeight="8" orient="auto">
        <path d="M 0 0 L 10 5 L 0 10 z" :fill="EDGE_COLORS.defaultBranch" />
      </marker>
      <marker id="arrow-completed" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="8" markerHeight="8" orient="auto">
        <path d="M 0 0 L 10 5 L 0 10 z" :fill="EDGE_COLORS.completed" />
      </marker>
      <marker id="arrow-running" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="8" markerHeight="8" orient="auto">
        <path d="M 0 0 L 10 5 L 0 10 z" :fill="EDGE_COLORS.running" />
      </marker>
      <marker id="arrow-rejected" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="8" markerHeight="8" orient="auto">
        <path d="M 0 0 L 10 5 L 0 10 z" :fill="EDGE_COLORS.rejected" />
      </marker>
    </defs>
    <EdgePath
      v-for="edge in edges"
      :key="edge.id"
      :edge="edge"
      :path="paths.get(edge.id) || { points: [], type: pathType }"
      :path-type="pathType"
      :status="statusOf(edge)"
    />
  </svg>
</template>
