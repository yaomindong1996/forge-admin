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
 * 删除网关节点：递归删除所有分支链。
 *
 * 算法：
 * 1. 网关 outgoing.target 是每条分支的入口
 * 2. 沿单出边链向下追，把节点纳入删除集合，遇汇合点（入度 ≥ 2 的节点，且不在已删集合）停止
 * 3. 删除网关本身后，把它的入边接到第一个汇合点（如果存在）；否则把入边目标置空（孤立）
 */
export function deleteGateway(json, gatewayId, idGen) {
  const inDegreeOf = id => json.edges.filter(e => e.target === id).length

  const toDelete = new Set([gatewayId])
  const branchHeads = json.edges.filter(e => e.source === gatewayId).map(e => e.target)
  const visitedConvergence = new Set()

  for (const head of branchHeads) {
    let cur = head
    while (cur && !toDelete.has(cur)) {
      // 入度 ≥ 2 视为汇合点（其他分支也共享）→ 停止下钻，记录
      if (inDegreeOf(cur) >= 2 && cur !== head) {
        visitedConvergence.add(cur)
        break
      }
      toDelete.add(cur)
      const out = json.edges.filter(e => e.source === cur)
      if (out.length !== 1)
        break
      cur = out[0].target
    }
  }

  // 入网关的边
  const incoming = json.edges.filter(e => e.target === gatewayId && !toDelete.has(e.source))
  // 网关删除后的合理后继：第一个汇合点
  const successor = visitedConvergence.values().next().value || null

  // 移除节点 + 与待删集相关的所有边
  json.nodes = json.nodes.filter(n => !toDelete.has(n.id))
  json.edges = json.edges.filter(e => !toDelete.has(e.source) && !toDelete.has(e.target))

  // 把入边接到 successor
  if (successor) {
    for (const inE of incoming) {
      json.edges.push(makeEdge(idGen.nextEdgeId(), inE.source, successor, {
        condition: inE.condition,
        conditionType: inE.conditionType,
        isDefault: inE.isDefault,
        branchId: inE.branchId,
      }))
    }
  }
}

export { GATEWAY_TYPES }
