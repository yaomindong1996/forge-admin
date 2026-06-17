/**
 * useFlowDesigner — 设计器编辑态核心 Composable
 *
 * 职责：
 * - 管理 flowJson reactive 状态
 * - 提供节点 / 边 CRUD（addNode / deleteNode / updateNode / moveNodeUp/Down / copyNode）
 * - 提供查询（getNode / getEdge / getOutgoingEdges / getIncomingEdges / find*Node）
 * - 提供整体加载 / 导出（loadJson / exportJson / reset）
 *
 * 约束：
 * - start / end 节点禁止删除
 * - addNode 在指定节点之后插入：保留原出边的 condition / branchId / isDefault
 * - deleteNode 普通节点：入边 × 出边 笛卡儿重连
 * - deleteNode 网关节点：递归删除分支链直到汇合点
 */

import { ref, shallowRef } from 'vue'
import { buildNode } from '../constants/default-configs.js'
import { NODE_TYPE } from '../constants/node-types.js'
import { collectExistingIds, createIdGenerator } from '../utils/id-generator.js'
import { cloneJson, deleteGateway, deletePlain, makeEdge } from './flow-designer-helpers.js'

const GATEWAY_TYPES = new Set([NODE_TYPE.CONDITION, NODE_TYPE.PARALLEL, NODE_TYPE.INCLUSIVE])

export function createEmptyFlow() {
  const start = buildNode(NODE_TYPE.START, { id: 'StartEvent_1', bpmnElementId: 'StartEvent_1', name: '发起人' })
  const end = buildNode(NODE_TYPE.END, { id: 'EndEvent_1', bpmnElementId: 'EndEvent_1', name: '结束' })
  return {
    processId: 'Process_1',
    processName: '新流程',
    nodes: [start, end],
    edges: [{
      id: 'Flow_1',
      source: 'StartEvent_1',
      target: 'EndEvent_1',
      bpmnElementId: 'Flow_1',
      conditionType: null,
      condition: '',
      isDefault: false,
      branchId: null,
    }],
  }
}

export function useFlowDesigner(initialJson) {
  const flowJson = shallowRef(initialJson ? cloneJson(initialJson) : createEmptyFlow())
  const selectedNodeId = ref(null)
  let idGen = createIdGenerator({ usedIds: collectExistingIds(flowJson.value) })

  const commit = next => (flowJson.value = next)

  const getNode = id => flowJson.value.nodes.find(n => n.id === id) || null
  const getEdge = id => flowJson.value.edges.find(e => e.id === id) || null
  const getOutgoingEdges = id => flowJson.value.edges.filter(e => e.source === id)
  const getIncomingEdges = id => flowJson.value.edges.filter(e => e.target === id)
  const findStartNode = () => flowJson.value.nodes.find(n => n.nodeType === NODE_TYPE.START) || null
  const findEndNode = () => flowJson.value.nodes.find(n => n.nodeType === NODE_TYPE.END) || null

  function addNode(afterId, nodeType, override = {}) {
    const next = cloneJson(flowJson.value)
    const after = next.nodes.find(n => n.id === afterId)
    if (!after)
      throw new Error(`addNode: afterId=${afterId} 不存在`)

    const newNode = buildNode(nodeType, override)
    newNode.id = idGen.nextNodeId()
    newNode.bpmnElementId = newNode.id
    next.nodes.push(newNode)

    const outgoing = next.edges.filter(e => e.source === afterId)
    if (outgoing.length === 0) {
      next.edges.push(makeEdge(idGen.nextEdgeId(), afterId, newNode.id))
    }
    else {
      const anchor = outgoing[0]
      const e1 = makeEdge(idGen.nextEdgeId(), afterId, newNode.id, {
        condition: anchor.condition,
        conditionType: anchor.conditionType,
        isDefault: anchor.isDefault,
        branchId: anchor.branchId,
      })
      const e2 = makeEdge(idGen.nextEdgeId(), newNode.id, anchor.target)
      next.edges = next.edges.filter(e => e.id !== anchor.id)
      next.edges.push(e1, e2)
    }

    commit(next)
    return newNode.id
  }

  function deleteNode(nodeId) {
    const node = getNode(nodeId)
    if (!node)
      throw new Error(`deleteNode: nodeId=${nodeId} 不存在`)
    if (node.nodeType === NODE_TYPE.START || node.nodeType === NODE_TYPE.END)
      throw new Error('deleteNode: start / end 节点不允许删除')

    const next = cloneJson(flowJson.value)
    if (GATEWAY_TYPES.has(node.nodeType))
      deleteGateway(next, nodeId, idGen)
    else
      deletePlain(next, nodeId, idGen)

    commit(next)
    if (selectedNodeId.value === nodeId)
      selectedNodeId.value = null
  }

  function updateNode(nodeId, patch) {
    if (!patch || typeof patch !== 'object')
      return
    const next = cloneJson(flowJson.value)
    const node = next.nodes.find(n => n.id === nodeId)
    if (!node)
      throw new Error(`updateNode: nodeId=${nodeId} 不存在`)
    for (const [k, v] of Object.entries(patch)) {
      if (k === 'config' && v && typeof v === 'object')
        node.config = { ...node.config, ...v }
      else
        node[k] = v
    }
    commit(next)
  }

  function moveNodeUp(id) { swapNeighbor(id, 'up') }
  function moveNodeDown(id) { swapNeighbor(id, 'down') }

  function swapNeighbor(nodeId, direction) {
    const next = cloneJson(flowJson.value)
    const incoming = next.edges.filter(e => e.target === nodeId)
    const outgoing = next.edges.filter(e => e.source === nodeId)
    if (incoming.length !== 1 || outgoing.length !== 1)
      throw new Error('swapNeighbor: 仅支持线性链中节点')

    const inEdge = incoming[0]
    const outEdge = outgoing[0]
    const neighborId = direction === 'up' ? inEdge.source : outEdge.target
    const neighbor = next.nodes.find(n => n.id === neighborId)
    if (!neighbor)
      return
    if (neighbor.nodeType === NODE_TYPE.START && direction === 'up')
      throw new Error('moveNodeUp: 已在最顶端')
    if (neighbor.nodeType === NODE_TYPE.END && direction === 'down')
      throw new Error('moveNodeDown: 已在最底端')

    const neighborIn = next.edges.filter(e => e.target === neighborId)
    const neighborOut = next.edges.filter(e => e.source === neighborId)
    if (neighborIn.length !== 1 || neighborOut.length !== 1)
      throw new Error('swapNeighbor: 邻居非线性，禁止移动')

    if (direction === 'up') {
      neighborIn[0].target = nodeId
      inEdge.source = nodeId
      inEdge.target = neighborId
      outEdge.source = neighborId
    }
    else {
      inEdge.target = neighborId
      outEdge.source = neighborId
      outEdge.target = nodeId
      neighborOut[0].source = nodeId
    }
    commit(next)
  }

  function copyNode(nodeId) {
    const node = getNode(nodeId)
    if (!node)
      throw new Error(`copyNode: nodeId=${nodeId} 不存在`)
    if (node.nodeType === NODE_TYPE.START || node.nodeType === NODE_TYPE.END)
      throw new Error('copyNode: start / end 节点不允许复制')
    const cloned = JSON.parse(JSON.stringify(node))
    delete cloned.id
    delete cloned.bpmnElementId
    cloned.name = `${cloned.name || ''} 副本`.trim()
    return addNode(nodeId, node.nodeType, cloned)
  }

  function updateEdge(edgeId, patch) {
    const next = cloneJson(flowJson.value)
    const edge = next.edges.find(e => e.id === edgeId)
    if (!edge)
      throw new Error(`updateEdge: edgeId=${edgeId} 不存在`)
    Object.assign(edge, patch)
    commit(next)
  }

  function reconnect(edgeId, newSource, newTarget) {
    updateEdge(edgeId, { source: newSource, target: newTarget })
  }

  function loadJson(json) {
    const cloned = json ? cloneJson(json) : createEmptyFlow()
    flowJson.value = cloned
    idGen = createIdGenerator({ usedIds: collectExistingIds(cloned) })
    selectedNodeId.value = null
  }

  function exportJson() {
    return cloneJson(flowJson.value)
  }

  function reset() {
    loadJson(createEmptyFlow())
  }

  return {
    flowJson,
    selectedNodeId,
    addNode,
    deleteNode,
    updateNode,
    moveNodeUp,
    moveNodeDown,
    copyNode,
    updateEdge,
    reconnect,
    getNode,
    getEdge,
    getOutgoingEdges,
    getIncomingEdges,
    findStartNode,
    findEndNode,
    loadJson,
    exportJson,
    reset,
  }
}
