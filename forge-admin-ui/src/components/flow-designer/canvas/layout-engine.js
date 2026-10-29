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
import { END_INCOMING_DOCK } from './edge-dock.js'
import { toStoredWaypoints } from './edge-segment-edit.js'
import { buildOrthogonalPath, buildPathWithWaypoints, recomputeBounds } from './orthogonal-path.js'

/** 收敛历史脏拐点（自由曲线点云 → 少量正交拐点） */
function sanitizeWaypoints(raw) {
  const list = (raw || []).filter(p => Number.isFinite(p?.x) && Number.isFinite(p?.y))
  if (!list.length)
    return []
  if (list.length <= 12)
    return list.map(p => ({ x: Number(p.x), y: Number(p.y) }))
  // 用假端点走 toStoredWaypoints 抽样+简化
  return toStoredWaypoints([
    { x: list[0].x, y: list[0].y },
    ...list,
    { x: list[list.length - 1].x, y: list[list.length - 1].y },
  ])
}

const DEFAULT_OPTS = {
  NODE_WIDTH: 200,
  NODE_HEIGHT: 56,
  GATEWAY_SIZE: 44,
  /** 结束节点命中盒 = 灰圆，文字溢出绘制，避免线接到空白处 */
  END_WIDTH: 20,
  END_HEIGHT: 20,
  V_GAP: 56,
  H_GAP: 72,
  MARGIN_TOP: 56,
  MARGIN_LEFT: 240,
}

const GATEWAY_TYPES = new Set(['condition', 'parallel', 'inclusive'])

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
  const hasManual = flowJson.nodes.some(node => hasUiPosition(node))

  // 节点位置直接复用；网关/结束节点用真实可视尺寸，避免连线接到空白盒子边缘。
  for (const [id, pos] of base.nodePositions.entries()) {
    const node = flowJson.nodes.find(item => item.id === id)
    if (GATEWAY_TYPES.has(node?.nodeType)) {
      const size = cfg.GATEWAY_SIZE
      const auto = {
        x: pos.x + (pos.width - size) / 2,
        y: pos.y + (pos.height - size) / 2,
        width: size,
        height: size,
      }
      result.nodePositions.set(id, applyUiPosition(node, auto))
      continue
    }
    if (node?.nodeType === 'end') {
      const auto = {
        x: pos.x + (pos.width - cfg.END_WIDTH) / 2,
        y: pos.y + (pos.height - cfg.END_HEIGHT) / 2,
        width: cfg.END_WIDTH,
        height: cfg.END_HEIGHT,
      }
      result.nodePositions.set(id, applyEndPosition(node, auto, cfg))
      continue
    }
    result.nodePositions.set(id, applyUiPosition(node, {
      x: pos.x,
      y: pos.y,
      width: pos.width || cfg.NODE_WIDTH,
      height: pos.height || cfg.NODE_HEIGHT,
    }))
  }

  // 自由布局下：无坐标的新节点贴到前驱实际位置下方，避免和冻结节点“分离”
  if (hasManual)
    snapUnplacedToPredecessors(flowJson, result.nodePositions, cfg)

  // 边 path：自由布局用四向端口正交线；树状布局沿用自动折点。
  // 任意边都必须有 path，避免“数据还在、线看不见”。
  const sourceLane = new Map()
  const sourceOutCount = new Map()
  for (const edge of flowJson.edges)
    sourceOutCount.set(edge.source, (sourceOutCount.get(edge.source) || 0) + 1)

  const nodesById = new Map(flowJson.nodes.map(n => [n.id, n]))

  for (const edge of flowJson.edges) {
    const sourcePos = result.nodePositions.get(edge.source)
    const targetPos = result.nodePositions.get(edge.target)
    const targetNode = nodesById.get(edge.target)
    const endTarget = targetNode?.nodeType === 'end'
    // 历史脏数据可能存了上百个自由曲线点：渲染前收敛，避免选中时手柄爆炸
    const manualBends = Array.isArray(edge.uiWaypoints)
      ? sanitizeWaypoints(edge.uiWaypoints)
      : []
    const dockOpts = {
      sourceDock: edge.uiSourceDock || null,
      // 结束节点无视历史 left/bottom dock，强制顶心（与可见锚点一致）
      targetDock: endTarget ? { ...END_INCOMING_DOCK } : (edge.uiTargetDock || null),
      endTarget,
    }
    if (manualBends.length && sourcePos && targetPos) {
      const path = buildPathWithWaypoints(sourcePos, targetPos, manualBends, dockOpts)
      if (path.points.length >= 2) {
        result.edgePaths.set(edge.id, path)
        continue
      }
    }
    if (hasManual || !base.edgeWaypoints.get(edge.id)) {
      const lane = sourceLane.get(edge.source) || 0
      sourceLane.set(edge.source, lane + 1)
      const path = buildOrthogonalPath(sourcePos, targetPos, {
        lane: lane - ((sourceOutCount.get(edge.source) || 1) - 1) / 2,
        ...dockOpts,
      })
      if (path.points.length >= 2) {
        result.edgePaths.set(edge.id, path)
        continue
      }
    }
    const wp = base.edgeWaypoints.get(edge.id)
    if (wp && wp.length >= 2) {
      // 树状 layout 的 waypoint 对结束节点也可能绕侧/底：统一改走上方正交
      if (endTarget && sourcePos && targetPos) {
        const path = buildOrthogonalPath(sourcePos, targetPos, dockOpts)
        if (path.points.length >= 2) {
          result.edgePaths.set(edge.id, path)
          continue
        }
      }
      const points = normalizeEdgePoints(edge, wp, result.nodePositions)
      result.edgePaths.set(edge.id, {
        points,
        type: points.length === 2 ? 'straight' : 'orthogonal',
      })
      continue
    }
    // 兜底：两点直线，保证不断线
    if (sourcePos && targetPos) {
      result.edgePaths.set(edge.id, {
        type: 'straight',
        points: [
          { x: sourcePos.x + sourcePos.width / 2, y: sourcePos.y + sourcePos.height },
          { x: targetPos.x + targetPos.width / 2, y: targetPos.y },
        ],
      })
    }
  }

  result.canvasBounds = recomputeBounds(result.nodePositions, result.edgePaths)
  return result
}

function hasUiPosition(node) {
  return Number.isFinite(node?.uiPosition?.x) && Number.isFinite(node?.uiPosition?.y)
}

function applyUiPosition(node, auto) {
  if (!hasUiPosition(node))
    return auto
  return {
    ...auto,
    x: node.uiPosition.x,
    y: node.uiPosition.y,
  }
}

/** 结束点：强制紧凑尺寸；旧数据按大卡左上冻结时，按中心对齐到圆点。 */
function applyEndPosition(node, auto, cfg) {
  if (!hasUiPosition(node))
    return auto
  const storedW = node.uiPosition.w
  const storedH = node.uiPosition.h
  if (Number.isFinite(storedW) && storedW <= cfg.END_WIDTH + 8) {
    return {
      x: node.uiPosition.x,
      y: node.uiPosition.y,
      width: cfg.END_WIDTH,
      height: cfg.END_HEIGHT,
    }
  }
  const legacyW = Number.isFinite(storedW) ? storedW : cfg.NODE_WIDTH
  const legacyH = Number.isFinite(storedH) ? storedH : cfg.NODE_HEIGHT
  return {
    x: node.uiPosition.x + (legacyW - cfg.END_WIDTH) / 2,
    y: node.uiPosition.y + (legacyH - cfg.END_HEIGHT) / 2,
    width: cfg.END_WIDTH,
    height: cfg.END_HEIGHT,
  }
}

function snapUnplacedToPredecessors(flowJson, positions, cfg) {
  const gap = cfg.V_GAP || 56
  for (const edge of flowJson.edges || []) {
    const targetNode = flowJson.nodes.find(n => n.id === edge.target)
    if (!targetNode || hasUiPosition(targetNode))
      continue
    const sourcePos = positions.get(edge.source)
    const targetPos = positions.get(edge.target)
    if (!sourcePos || !targetPos)
      continue
    const expectedY = sourcePos.y + sourcePos.height + gap
    if (Math.abs(targetPos.y - expectedY) <= 160 && Math.abs(targetPos.x - sourcePos.x) <= 320)
      continue
    positions.set(edge.target, {
      ...targetPos,
      x: sourcePos.x + (sourcePos.width - targetPos.width) / 2,
      y: expectedY,
    })
  }
}

function normalizeEdgePoints(edge, points, nodePositions) {
  const source = nodePositions.get(edge.source)
  const target = nodePositions.get(edge.target)
  if (!source || !target)
    return points

  const sourceCenterX = source.x + source.width / 2
  const targetCenterX = target.x + target.width / 2
  const sourceAboveTarget = source.y <= target.y
  const start = {
    x: sourceCenterX,
    y: sourceAboveTarget ? source.y + source.height : source.y,
  }
  const end = {
    x: targetCenterX,
    y: sourceAboveTarget ? target.y : target.y + target.height,
  }

  if (points.length === 2)
    return [start, end]

  const next = points.map(point => ({ ...point }))
  next[0] = start
  next[next.length - 1] = end

  if (next[1]) {
    next[1].x = start.x
    if (Math.abs(next[1].y - points[0].y) < 1)
      next[1].y = start.y
  }
  if (next[next.length - 2]) {
    next[next.length - 2].x = end.x
    if (Math.abs(next[next.length - 2].y - points[points.length - 1].y) < 1)
      next[next.length - 2].y = end.y
  }

  return next
}
