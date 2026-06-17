/**
 * 网关分支识别（Task 4）
 *
 * 在 convertBpmnToJson 完成基础节点 + edges 解析后调用，对网关出边做：
 *   1. 按出边顺序分配 branchId（'b1' / 'b2' / ...）
 *   2. 标识默认分支：网关的 default 属性引用的 sequenceFlow → edge.isDefault=true
 *   3. 已在 parseEdge 阶段提取 condition 文本与 conditionType；本步骤补充 default
 *   4. 标识 mergeNodes：node 的入度 >= 2 标记为 mergeNode
 *
 * 不修改 nodes 列表，但会给 edges 字段补 branchId / isDefault，并返回额外的诊断信息。
 *
 * @param {object} flowJson 已经过 convertBpmnToJson 的对象（mutate）
 * @returns {{ inDegree: Map<string, number>, outDegree: Map<string, number>, mergeNodeIds: string[] }}
 */
import { NODE_TYPE } from '../constants/node-types.js'

const GATEWAY_TYPES = new Set([NODE_TYPE.CONDITION, NODE_TYPE.PARALLEL, NODE_TYPE.INCLUSIVE])

let branchSeq = 0

function nextBranchId() {
  branchSeq += 1
  return `b${branchSeq}`
}

export function markBranches(flowJson) {
  if (!flowJson || !Array.isArray(flowJson.nodes) || !Array.isArray(flowJson.edges))
    return { inDegree: new Map(), outDegree: new Map(), mergeNodeIds: [] }

  branchSeq = 0

  const nodesById = new Map(flowJson.nodes.map(n => [n.id, n]))
  const edgesBySource = new Map()
  for (const edge of flowJson.edges) {
    if (!edgesBySource.has(edge.source))
      edgesBySource.set(edge.source, [])
    edgesBySource.get(edge.source).push(edge)
  }

  // 1) 网关出边分配 branchId + 标识 default
  for (const node of flowJson.nodes) {
    if (!GATEWAY_TYPES.has(node.nodeType))
      continue
    const outgoing = edgesBySource.get(node.id) || []
    if (outgoing.length === 0)
      continue

    const defaultFlowId = node.config?.defaultFlowId || ''
    for (const edge of outgoing) {
      edge.branchId = nextBranchId()
      if (defaultFlowId && edge.id === defaultFlowId) {
        edge.isDefault = true
        // 默认分支不应有 condition，按 BPMN 规范清空
        edge.condition = ''
        edge.conditionType = null
      }
    }
  }

  // 2) 计算每节点入度 / 出度，找出汇合节点
  const inDegree = getNodeInDegree(flowJson.nodes, flowJson.edges)
  const outDegree = getNodeOutDegree(flowJson.nodes, flowJson.edges)
  const mergeNodeIds = []
  for (const [nodeId, deg] of inDegree.entries()) {
    if (deg >= 2) {
      mergeNodeIds.push(nodeId)
      // 给节点上额外标记，便于布局算法判断
      const node = nodesById.get(nodeId)
      if (node) {
        node.config = node.config || {}
        node.config.mergeNode = true
      }
    }
  }

  return { inDegree, outDegree, mergeNodeIds }
}

export function getNodeInDegree(nodes, edges) {
  const m = new Map()
  for (const n of nodes)
    m.set(n.id, 0)
  for (const e of edges) {
    if (m.has(e.target))
      m.set(e.target, m.get(e.target) + 1)
    else
      m.set(e.target, 1)
  }
  return m
}

export function getNodeOutDegree(nodes, edges) {
  const m = new Map()
  for (const n of nodes)
    m.set(n.id, 0)
  for (const e of edges) {
    if (m.has(e.source))
      m.set(e.source, m.get(e.source) + 1)
    else
      m.set(e.source, 1)
  }
  return m
}
