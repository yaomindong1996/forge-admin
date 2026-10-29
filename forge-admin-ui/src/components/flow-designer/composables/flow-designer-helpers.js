/**
 * useFlowDesigner 的私有工具函数
 *
 * 拆出来便于：
 * 1. useFlowDesigner.js 主体精简
 * 2. 单元测试直接 import 验证算法（deleteGateway / deletePlain）
 */

const GATEWAY_TYPES = new Set(['condition', 'parallel', 'inclusive'])

export function cloneJson(json) {
  return JSON.parse(JSON.stringify(json))
}

export function makeEdge(id, source, target, extras = {}) {
  return {
    id,
    source,
    target,
    bpmnElementId: id,
    conditionType: null,
    condition: '',
    isDefault: false,
    branchId: null,
    ...extras,
  }
}

/**
 * 删除普通节点：把入边 source 重连到出边 target（笛卡儿组合）。
 * 入边的 condition / branchId / isDefault 优先保留，避免分支链中插入的节点被删除时丢分支元数据。
 */
export function deletePlain(json, nodeId, idGen) {
  const incoming = json.edges.filter(e => e.target === nodeId)
  const outgoing = json.edges.filter(e => e.source === nodeId)

  json.nodes = json.nodes.filter(n => n.id !== nodeId)
  json.edges = json.edges.filter(e => e.source !== nodeId && e.target !== nodeId)

  for (const inE of incoming) {
    for (const outE of outgoing) {
      json.edges.push(makeEdge(idGen.nextEdgeId(), inE.source, outE.target, {
        condition: inE.condition,
        conditionType: inE.conditionType,
        isDefault: inE.isDefault,
        branchId: inE.branchId,
      }))
    }
  }
}

/**
 * 删除网关节点（软删）：只移除网关本身，保留各分支节点。
 *
 * 入边 × 出边重连；分支条件 / branchId / isDefault 取自原出边，
 * 避免删除条件网关时把分支上的审批节点一并清掉。
 */
export function deleteGateway(json, gatewayId, idGen) {
  const incoming = json.edges.filter(e => e.target === gatewayId)
  const outgoing = json.edges.filter(e => e.source === gatewayId)

  json.nodes = json.nodes.filter(n => n.id !== gatewayId)
  json.edges = json.edges.filter(e => e.source !== gatewayId && e.target !== gatewayId)

  for (const inE of incoming) {
    for (const outE of outgoing) {
      json.edges.push(makeEdge(idGen.nextEdgeId(), inE.source, outE.target, {
        condition: outE.condition,
        conditionType: outE.conditionType,
        isDefault: outE.isDefault,
        branchId: outE.branchId,
      }))
    }
  }
}

/** 流程是否处于自由布局（已有手工坐标或显式标记）。 */
export function isFreeLayout(json) {
  return Boolean(json?.config?.freeLayout)
    || (json?.nodes || []).some(n => Number.isFinite(n?.uiPosition?.x) && Number.isFinite(n?.uiPosition?.y))
}

export function markFreeLayout(json, enabled = true) {
  json.config = { ...(json.config || {}), freeLayout: Boolean(enabled) }
}

/**
 * 在自由布局下，把新节点放到锚点下方（或左右展开），避免无坐标节点飘到自动布局位置导致“分离”。
 */
export function placeNodeNearAnchor(json, nodeId, anchorId, offset = { x: 0, y: 120 }) {
  const node = json.nodes.find(n => n.id === nodeId)
  const anchor = json.nodes.find(n => n.id === anchorId)
  if (!node || !anchor?.uiPosition)
    return
  node.uiPosition = {
    x: anchor.uiPosition.x + (offset.x || 0),
    y: anchor.uiPosition.y + (offset.y || 120),
  }
}

export { GATEWAY_TYPES }
