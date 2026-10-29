/**
 * useFlowDesigner — 设计器编辑态核心 Composable
 *
 * 职责：
 * - 管理 flowJson reactive 状态
 * - 提供节点 / 边 CRUD（addNode / addBranch / deleteNode / updateNode / moveNodeUp/Down / copyNode）
 * - 自由画布：placeNode / connectNodes / disconnectEdge / moveNodePosition / freeze|clear uiPosition
 * - 提供查询（getNode / getEdge / getOutgoingEdges / getIncomingEdges / find*Node）
 * - 提供整体加载 / 导出（loadJson / exportJson / reset）
 *
 * 约束：
 * - start / end 节点禁止删除
 * - addNode 在指定节点之后插入：保留原出边的 condition / branchId / isDefault
 * - deleteNode 普通节点：入边 × 出边 笛卡儿重连
 * - deleteNode 网关节点：软删，只移除网关并重连，保留分支节点
 */

import { ref, shallowRef } from 'vue'
import { END_INCOMING_DOCK } from '../canvas/edge-dock.js'
import { buildNode } from '../constants/default-configs.js'
import { NODE_TYPE } from '../constants/node-types.js'
import { mergeApprovalNodes as buildApprovalMerge } from '../utils/approval-node-merge.js'
import { collectExistingIds, createIdGenerator } from '../utils/id-generator.js'
import {
  cloneJson,
  deleteGateway,
  deletePlain,
  isFreeLayout,
  makeEdge,
  markFreeLayout,
  placeNodeNearAnchor,
} from './flow-designer-helpers.js'

/** 写入边时：目标为结束节点则强制顶心入边 */
function normalizeTargetDock(targetNode, dock) {
  if (targetNode?.nodeType === NODE_TYPE.END || targetNode?.nodeType === 'end')
    return { ...END_INCOMING_DOCK }
  return dock ?? null
}

const GATEWAY_TYPES = new Set([NODE_TYPE.CONDITION, NODE_TYPE.PARALLEL, NODE_TYPE.INCLUSIVE])

/** 结束节点写入紧凑宽高，布局才能识别已校正坐标，避免箭头悬空。 */
function buildUiPosition(node, position = {}) {
  const next = {
    x: Number(position.x),
    y: Number(position.y),
  }
  if (node?.nodeType === NODE_TYPE.END) {
    next.w = Number.isFinite(position.width) ? Number(position.width) : (Number.isFinite(position.w) ? Number(position.w) : 20)
    next.h = Number.isFinite(position.height) ? Number(position.height) : (Number.isFinite(position.h) ? Number(position.h) : 20)
  }
  return next
}

export function createEmptyFlow() {
  const start = buildNode(NODE_TYPE.START, { id: 'StartEvent_1', bpmnElementId: 'StartEvent_1', name: '发起人' })
  const end = buildNode(NODE_TYPE.END, { id: 'EndEvent_1', bpmnElementId: 'EndEvent_1', name: '结束' })
  return {
    processId: 'Process_1',
    processName: '新流程',
    config: {
      allowSubmitterWithdraw: true,
      autoApprovalMode: 'none',
      rejectStrategy: 'TO_INITIATOR_MODIFY',
    },
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
    if (GATEWAY_TYPES.has(nodeType))
      return addGatewayNode(afterId, nodeType, override)

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

    if (isFreeLayout(next)) {
      markFreeLayout(next, true)
      placeNodeNearAnchor(next, newNode.id, afterId)
    }

    commit(next)
    return newNode.id
  }

  function addGatewayNode(afterId, nodeType, override = {}) {
    const next = cloneJson(flowJson.value)
    const after = next.nodes.find(n => n.id === afterId)
    if (!after)
      throw new Error(`addNode: afterId=${afterId} 不存在`)

    const outgoing = next.edges.filter(e => e.source === afterId)
    if (outgoing.length > 1)
      throw new Error('addNode: 网关后不能直接插入新的网关，请在具体分支内添加')

    const anchor = outgoing[0] || null
    const mergeTargetId = anchor?.target || null

    const gateway = buildNode(nodeType, override)
    gateway.id = idGen.nextNodeId()
    gateway.bpmnElementId = gateway.id
    next.nodes.push(gateway)

    if (anchor)
      next.edges = next.edges.filter(e => e.id !== anchor.id)

    next.edges.push(makeEdge(idGen.nextEdgeId(), afterId, gateway.id, {
      condition: anchor?.condition || '',
      conditionType: anchor?.conditionType || null,
      isDefault: anchor?.isDefault || false,
      branchId: anchor?.branchId || null,
    }))

    const defaultBranchIndex = nodeType === NODE_TYPE.PARALLEL ? -1 : 1
    const branchIds = []
    for (let i = 0; i < 2; i += 1) {
      const branchNode = buildNode(NODE_TYPE.APPROVER, {
        name: `分支${i + 1}审批`,
      })
      branchNode.id = idGen.nextNodeId()
      branchNode.bpmnElementId = branchNode.id
      next.nodes.push(branchNode)
      branchIds.push(branchNode.id)

      const branchEdgeId = idGen.nextEdgeId()
      const isDefault = i === defaultBranchIndex
      next.edges.push(makeEdge(branchEdgeId, gateway.id, branchNode.id, {
        branchId: idGen.nextBranchId(),
        isDefault,
        condition: '',
        conditionType: null,
      }))
      if (isDefault)
        gateway.config.defaultFlowId = branchEdgeId

      if (mergeTargetId)
        next.edges.push(makeEdge(idGen.nextEdgeId(), branchNode.id, mergeTargetId))
    }

    if (mergeTargetId) {
      const mergeTarget = next.nodes.find(n => n.id === mergeTargetId)
      if (mergeTarget)
        mergeTarget.config = { ...(mergeTarget.config || {}), mergeNode: true }
    }

    if (isFreeLayout(next)) {
      markFreeLayout(next, true)
      placeNodeNearAnchor(next, gateway.id, afterId, { x: 0, y: 110 })
      placeNodeNearAnchor(next, branchIds[0], gateway.id, { x: -180, y: 120 })
      placeNodeNearAnchor(next, branchIds[1], gateway.id, { x: 180, y: 120 })
    }

    commit(next)
    return gateway.id
  }

  function addBranch(gatewayId, override = {}) {
    const next = cloneJson(flowJson.value)
    const gateway = next.nodes.find(n => n.id === gatewayId)
    if (!gateway)
      throw new Error(`addBranch: gatewayId=${gatewayId} 不存在`)
    if (!GATEWAY_TYPES.has(gateway.nodeType))
      throw new Error('addBranch: 仅支持网关节点添加分支')

    const outgoing = next.edges.filter(e => e.source === gatewayId)
    const mergeTargetId = findGatewayMergeTarget(next, gatewayId)
    const branchNode = buildNode(NODE_TYPE.APPROVER, {
      name: `分支${outgoing.length + 1}审批`,
      ...override,
    })
    branchNode.id = idGen.nextNodeId()
    branchNode.bpmnElementId = branchNode.id
    next.nodes.push(branchNode)

    const branchEdgeId = idGen.nextEdgeId()
    next.edges.push(makeEdge(branchEdgeId, gatewayId, branchNode.id, {
      branchId: idGen.nextBranchId(),
      isDefault: false,
      condition: '',
      conditionType: null,
    }))

    if (mergeTargetId) {
      next.edges.push(makeEdge(idGen.nextEdgeId(), branchNode.id, mergeTargetId))
      const mergeTarget = next.nodes.find(n => n.id === mergeTargetId)
      if (mergeTarget)
        mergeTarget.config = { ...(mergeTarget.config || {}), mergeNode: true }
    }

    normalizeGatewayDefault(next, gatewayId)
    if (isFreeLayout(next)) {
      markFreeLayout(next, true)
      const outs = next.edges.filter(e => e.source === gatewayId)
      const idx = Math.max(outs.length - 1, 0)
      placeNodeNearAnchor(next, branchNode.id, gatewayId, {
        x: (idx - (outs.length - 1) / 2) * 200,
        y: 120,
      })
    }
    commit(next)
    return { nodeId: branchNode.id, edgeId: branchEdgeId }
  }

  function findGatewayMergeTarget(json, gatewayId) {
    const inDegreeOf = id => json.edges.filter(e => e.target === id).length
    const nodeById = new Map(json.nodes.map(node => [node.id, node]))
    const outgoing = json.edges.filter(e => e.source === gatewayId)

    for (const edge of outgoing) {
      let cur = edge.target
      const guard = new Set([gatewayId])
      while (cur && !guard.has(cur)) {
        guard.add(cur)
        const out = json.edges.filter(e => e.source === cur)
        if (out.length !== 1)
          break

        const targetId = out[0].target
        const targetNode = nodeById.get(targetId)
        if ((inDegreeOf(targetId) >= 2 && targetId !== edge.target) || targetNode?.config?.mergeNode)
          return targetId

        cur = targetId
      }
    }

    return null
  }

  function normalizeGatewayDefault(json, gatewayId) {
    const gateway = json.nodes.find(n => n.id === gatewayId)
    if (!gateway || gateway.nodeType === NODE_TYPE.PARALLEL)
      return

    const outgoing = json.edges.filter(e => e.source === gatewayId)
    if (!outgoing.length) {
      gateway.config = { ...(gateway.config || {}), defaultFlowId: '' }
      return
    }

    const configuredDefault = outgoing.find(e => e.id === gateway.config?.defaultFlowId)
    const existingDefault = outgoing.find(e => e.isDefault)
    const defaultEdge = configuredDefault || existingDefault || outgoing[outgoing.length - 1]
    for (const edge of outgoing)
      edge.isDefault = edge.id === defaultEdge.id
    gateway.config = { ...(gateway.config || {}), defaultFlowId: defaultEdge.id }
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

  function moveNodeUp(id) {
    swapNeighbor(id, 'up')
  }

  function moveNodeDown(id) {
    swapNeighbor(id, 'down')
  }

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

  function mergeApprovalNodes(sourceId, targetId, beforeCommit = () => {}) {
    const next = buildApprovalMerge(flowJson.value, sourceId, targetId)
    // 校验/建图全部成功之后才记录历史，失败不能产生空撤销步骤。
    beforeCommit()
    commit(next)
    selectedNodeId.value = targetId
  }

  function reconnect(edgeId, newSource, newTarget, docks = {}) {
    if (!edgeId || !newSource || !newTarget || newSource === newTarget)
      throw new Error('reconnect: 源/目标无效')
    if (!getNode(newSource) || !getNode(newTarget))
      throw new Error('reconnect: 节点不存在')
    if (getOutgoingEdges(newSource).some(e => e.id !== edgeId && e.target === newTarget))
      throw new Error('reconnect: 连线已存在')
    const patch = {
      source: newSource,
      target: newTarget,
      uiWaypoints: [],
    }
    if (docks.sourceDock !== undefined)
      patch.uiSourceDock = docks.sourceDock
    else
      patch.uiSourceDock = null
    if (docks.targetDock !== undefined)
      patch.uiTargetDock = normalizeTargetDock(getNode(newTarget), docks.targetDock)
    else
      patch.uiTargetDock = normalizeTargetDock(getNode(newTarget), null)
    updateEdge(edgeId, patch)
  }

  function setEdgeWaypoints(edgeId, waypoints) {
    // toStoredWaypoints 已做简化与上限；此处再滤非法值
    const list = Array.isArray(waypoints)
      ? waypoints
          .filter(p => Number.isFinite(p?.x) && Number.isFinite(p?.y))
          .slice(0, 12)
          .map(p => ({ x: Number(p.x), y: Number(p.y) }))
      : []
    const next = cloneJson(flowJson.value)
    const edge = next.edges.find(e => e.id === edgeId)
    if (!edge)
      throw new Error(`setEdgeWaypoints: edgeId=${edgeId} 不存在`)
    edge.uiWaypoints = list
    markFreeLayout(next, true)
    commit(next)
  }

  /** 更新连线在节点边上的锚点（可换边、可非中心） */
  function setEdgeDock(edgeId, which, dock) {
    if (!edgeId || (which !== 'source' && which !== 'target'))
      throw new Error('setEdgeDock: 参数无效')
    const next = cloneJson(flowJson.value)
    const edge = next.edges.find(e => e.id === edgeId)
    if (!edge)
      throw new Error(`setEdgeDock: edgeId=${edgeId} 不存在`)
    if (which === 'source') {
      edge.uiSourceDock = dock || null
    }
    else {
      const target = next.nodes.find(n => n.id === edge.target)
      edge.uiTargetDock = normalizeTargetDock(target, dock)
    }
    markFreeLayout(next, true)
    commit(next)
  }

  /** 货架拖入：在画布坐标放置节点，默认不自动串链。 */
  function placeNode(nodeType, position = {}, override = {}) {
    if (nodeType === NODE_TYPE.START || nodeType === NODE_TYPE.END)
      throw new Error('placeNode: start / end 请使用流程已有节点')
    if (GATEWAY_TYPES.has(nodeType))
      throw new Error('placeNode: 网关请先选中前置节点，再用底部「＋」添加，以保证分支结构完整')

    const next = cloneJson(flowJson.value)
    const newNode = buildNode(nodeType, override)
    newNode.id = idGen.nextNodeId()
    newNode.bpmnElementId = newNode.id
    if (Number.isFinite(position.x) && Number.isFinite(position.y))
      newNode.uiPosition = { x: Number(position.x), y: Number(position.y) }
    next.nodes.push(newNode)
    markFreeLayout(next, true)
    commit(next)
    return newNode.id
  }

  function connectNodes(sourceId, targetId, extras = {}) {
    if (!sourceId || !targetId || sourceId === targetId)
      throw new Error('connectNodes: 源/目标无效')
    if (!getNode(sourceId) || !getNode(targetId))
      throw new Error('connectNodes: 节点不存在')
    if (getOutgoingEdges(sourceId).some(e => e.target === targetId))
      throw new Error('connectNodes: 连线已存在')

    const next = cloneJson(flowJson.value)
    const edge = makeEdge(idGen.nextEdgeId(), sourceId, targetId, {
      ...extras,
      uiTargetDock: normalizeTargetDock(getNode(targetId), extras.uiTargetDock),
    })
    next.edges.push(edge)
    commit(next)
    return edge.id
  }

  function disconnectEdge(edgeId) {
    const edge = getEdge(edgeId)
    if (!edge)
      throw new Error(`disconnectEdge: edgeId=${edgeId} 不存在`)
    const next = cloneJson(flowJson.value)
    next.edges = next.edges.filter(e => e.id !== edgeId)
    commit(next)
  }

  function moveNodePosition(nodeId, position) {
    if (!Number.isFinite(position?.x) || !Number.isFinite(position?.y))
      throw new Error('moveNodePosition: 需要有效的 x/y')
    const next = cloneJson(flowJson.value)
    const node = next.nodes.find(n => n.id === nodeId)
    if (!node)
      throw new Error(`moveNodePosition: nodeId=${nodeId} 不存在`)
    node.uiPosition = buildUiPosition(node, position)
    markFreeLayout(next, true)
    commit(next)
  }

  /** 将当前布局坐标冻结到 uiPosition，避免拖一个节点时其它节点被自动布局重排。 */
  function freezeLayoutPositions(positions) {
    if (!positions || typeof positions.get !== 'function')
      return
    const next = cloneJson(flowJson.value)
    let changed = false
    for (const node of next.nodes) {
      if (node.uiPosition)
        continue
      const pos = positions.get(node.id)
      if (!pos || !Number.isFinite(pos.x) || !Number.isFinite(pos.y))
        continue
      node.uiPosition = buildUiPosition(node, pos)
      changed = true
    }
    if (changed) {
      markFreeLayout(next, true)
      commit(next)
    }
  }

  function clearNodePositions() {
    const next = cloneJson(flowJson.value)
    let changed = false
    for (const node of next.nodes) {
      if (!node.uiPosition)
        continue
      delete node.uiPosition
      changed = true
    }
    for (const edge of next.edges) {
      if (!edge.uiWaypoints?.length)
        continue
      edge.uiWaypoints = []
      changed = true
    }
    if (next.config?.freeLayout) {
      markFreeLayout(next, false)
      changed = true
    }
    if (changed)
      commit(next)
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
    addBranch,
    placeNode,
    connectNodes,
    disconnectEdge,
    moveNodePosition,
    freezeLayoutPositions,
    clearNodePositions,
    deleteNode,
    updateNode,
    moveNodeUp,
    moveNodeDown,
    copyNode,
    mergeApprovalNodes,
    updateEdge,
    reconnect,
    setEdgeWaypoints,
    setEdgeDock,
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
