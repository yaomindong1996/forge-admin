/**
 * Canvas 渲染层布局引擎（Task 10）
 *
 * 与 converter/layout-algorithm.js 的区别：
 * - converter/layout-algorithm 服务于 JSON→BPMN，输出 BPMNDiagram 坐标，dc:Bounds + di:waypoint
 * - canvas/layout-engine 服务于画布渲染，输出节点位置 + 边 polyline + 路径类型，便于 EdgeLayer 渲染
 *
 * 输出：
 *   {
 *     nodePositions: Map<nodeId, {x, y, width, height}>,
 *     edgePaths: Map<edgeId, {points: [{x,y}], type: 'straight' | 'orthogonal'}>,
 *     canvasBounds: { minX, minY, maxX, maxY }
 *   }
 *
 * 复用 converter 的 calculateLayout 计算节点位置 + 原始 waypoints；本层再做：
 * - 边类型识别（同 x → straight；不同 x → orthogonal）
 * - canvasBounds 整体范围（用于 fitToScreen）
 */

import { calculateLayout as baseLayout } from '../converter/layout-algorithm.js'

const DEFAULT_OPTS = {
  NODE_WIDTH: 220,
  NODE_HEIGHT: 80,
  V_GAP: 50,
  H_GAP: 40,
  MARGIN_TOP: 40,
  MARGIN_LEFT: 200,
}

export function layoutFlow(flowJson, opts = {}) {
  const cfg = { ...DEFAULT_OPTS, ...opts }
  const result = {
    nodePositions: new Map(),
    edgePaths: new Map(),
    canvasBounds: { minX: 0, minY: 0, maxX: 0, maxY: 0 },
  }
  if (!flowJson || !Array.isArray(flowJson.nodes) || flowJson.nodes.length === 0)
    return result

  // 复用 converter 的几何计算
  const base = baseLayout(flowJson, cfg)

  // 节点位置直接复用
  for (const [id, pos] of base.nodePositions.entries()) {
    result.nodePositions.set(id, {
      x: pos.x,
      y: pos.y,
      width: pos.width || cfg.NODE_WIDTH,
      height: pos.height || cfg.NODE_HEIGHT,
    })
  }

  // 边 path：基于原始 waypoints，标识类型
  for (const edge of flowJson.edges) {
    const wp = base.edgeWaypoints.get(edge.id)
    if (!wp || wp.length < 2)
      continue
    // 起止 x 一致 → straight；否则按 orthogonal（折线）
    const type = wp.length === 2 ? 'straight' : 'orthogonal'
    result.edgePaths.set(edge.id, { points: wp, type })
  }

  // canvasBounds：聚合所有节点 + waypoint
  let minX = Infinity
  let minY = Infinity
  let maxX = -Infinity
  let maxY = -Infinity
  for (const pos of result.nodePositions.values()) {
    if (pos.x < minX)
      minX = pos.x
    if (pos.y < minY)
      minY = pos.y
    if (pos.x + pos.width > maxX)
      maxX = pos.x + pos.width
    if (pos.y + pos.height > maxY)
      maxY = pos.y + pos.height
  }
  for (const path of result.edgePaths.values()) {
    for (const p of path.points) {
      if (p.x < minX)
        minX = p.x
      if (p.y < minY)
        minY = p.y
      if (p.x > maxX)
        maxX = p.x
      if (p.y > maxY)
        maxY = p.y
    }
  }

  if (Number.isFinite(minX))
    result.canvasBounds = { minX, minY, maxX, maxY }

  return result
}
