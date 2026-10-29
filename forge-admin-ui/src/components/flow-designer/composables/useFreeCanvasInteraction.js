/**
 * 自由画布交互（对齐 bpmn.io）
 *
 * - 「连线」工具：点源 → 蓝虚线跟手 → 点目标；四边锚点可指定出边/入边
 * - 选中连线：点哪里加拐点、拖拐点改线；端点拖到节点另一边可换锚点
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { computeAlignmentGuides } from '../canvas/alignment-guides.js'
import {
  END_INCOMING_DOCK,
  dockOnSidePreferCenter,
  facingCenterDocks,
  nearestDockOnBox,
  sideCenterDock,
} from '../canvas/edge-dock.js'
import {
  findNearestSegment,
  insertBendAt,
  moveOrthoSegment,
  toStoredWaypoints,
} from '../canvas/edge-segment-edit.js'

const DROP_SNAP_PX = 56
const RECONNECT_SNAP_PX = 80
const ALIGN_THRESHOLD = 6

export function useFreeCanvasInteraction({
  designer,
  history,
  layoutResult,
  canvasRef,
  readonly,
  onNodePlaced,
}) {
  const linking = ref(null)
  const draggingNodeId = ref(null)
  /** @type {import('vue').Ref<Array<{ orientation:'v'|'h', position:number, start:number, end:number }>>} */
  const alignmentGuides = ref([])
  const selectedEdgeId = ref(null)
  const selectedWaypointIndex = ref(null)
  const tool = ref('select')
  let dragOrigin = null
  let linkMoveHandler = null
  let linkUpHandler = null
  let connectSourceId = null
  /** @type {{ side: string, t: number } | null} */
  let pendingSourceDock = null
  /** 用户是否点了源节点某条边锚点（锁定边，完成时不再按相对位置改源边） */
  let pendingSourceDockLocked = false
  let suppressNextNodeClick = false
  let connectPreviewMove = null

  function isReadonly() {
    return typeof readonly === 'function' ? readonly() : readonly?.value
  }

  function freezeIfNeeded() {
    designer.freezeLayoutPositions(layoutResult.value.nodePositions)
  }

  function clientToCanvas(clientX, clientY) {
    const canvas = canvasRef.value
    const el = canvas?.containerRef
    if (!canvas?.screenToCanvas || !el)
      return null
    const rect = el.getBoundingClientRect()
    return canvas.screenToCanvas(clientX - rect.left, clientY - rect.top)
  }

  function findNodeIdAt(clientX, clientY) {
    const stack = document.elementsFromPoint?.(clientX, clientY) || []
    for (const el of stack) {
      const host = el?.closest?.('[data-node-id]')
      const id = host?.getAttribute?.('data-node-id')
      if (id)
        return id
    }
    return null
  }

  function findDropNodeId(clientX, clientY, excludeId, snapPx = DROP_SNAP_PX, preferId = null) {
    const direct = findNodeIdAt(clientX, clientY)
    if (direct && direct !== excludeId)
      return direct
    const pt = clientToCanvas(clientX, clientY)
    if (!pt)
      return null
    // 改接时优先吸附「当前端点所属节点」，方便拖到同一节点另一条边
    if (preferId && preferId !== excludeId) {
      const pos = layoutResult.value.nodePositions.get(preferId)
      if (pos) {
        const dx = Math.max(pos.x - pt.x, 0, pt.x - (pos.x + pos.width))
        const dy = Math.max(pos.y - pt.y, 0, pt.y - (pos.y + pos.height))
        if (Math.hypot(dx, dy) <= snapPx)
          return preferId
      }
    }
    let bestId = null
    let bestDist = snapPx
    for (const [id, pos] of layoutResult.value.nodePositions.entries()) {
      if (!pos || id === excludeId)
        continue
      const dx = Math.max(pos.x - pt.x, 0, pt.x - (pos.x + pos.width))
      const dy = Math.max(pos.y - pt.y, 0, pt.y - (pos.y + pos.height))
      const boxDist = Math.hypot(dx, dy)
      const cx = pos.x + pos.width / 2
      const cy = pos.y + pos.height / 2
      const dist = Math.min(boxDist, Math.hypot(pt.x - cx, pt.y - cy))
      if (dist < bestDist) {
        bestDist = dist
        bestId = id
      }
    }
    return bestId
  }

  function warn(msg) {
    window.$message?.warning?.(msg)
  }

  function tip(msg) {
    window.$message?.info?.(msg)
  }

  function clearLinkDragListeners() {
    if (linkMoveHandler) {
      window.removeEventListener('pointermove', linkMoveHandler)
      window.removeEventListener('pointermove', linkMoveHandler, true)
    }
    if (linkUpHandler) {
      window.removeEventListener('pointerup', linkUpHandler)
      window.removeEventListener('pointerup', linkUpHandler, true)
    }
    linkMoveHandler = null
    linkUpHandler = null
  }

  function stopConnectPreviewFollow() {
    if (connectPreviewMove) {
      window.removeEventListener('pointermove', connectPreviewMove, true)
      connectPreviewMove = null
    }
  }

  /** 点选连线：蓝虚线跟手（capture，避免被节点挡住收不到 move） */
  function startConnectPreviewFollow() {
    stopConnectPreviewFollow()
    connectPreviewMove = (e) => {
      if (!linking.value || linking.value.mode !== 'new')
        return
      const cur = clientToCanvas(e.clientX, e.clientY)
      if (!cur)
        return
      const prev = linking.value
      if (Math.abs(prev.x - cur.x) < 0.5 && Math.abs(prev.y - cur.y) < 0.5)
        return
      linking.value = { ...prev, x: cur.x, y: cur.y }
    }
    window.addEventListener('pointermove', connectPreviewMove, true)
  }

  function cancelConnect() {
    connectSourceId = null
    pendingSourceDock = null
    pendingSourceDockLocked = false
    stopConnectPreviewFollow()
    if (linking.value?.mode === 'new')
      linking.value = null
  }

  /**
   * 落点规则：
   * - 点了某条边锚点 → 默认该边中心（只有明显偏到两侧才偏移）
   * - 点卡片主体 → 按源/目标相对位置选对边中心，禁止点下半就挂到底边穿心
   */
  function dockFromPointer(nodeId, pointerEvent, preferredSide) {
    const pos = layoutResult.value.nodePositions.get(nodeId)
    if (!pos)
      return preferredSide ? sideCenterDock(preferredSide) : null
    if (preferredSide) {
      if (!pointerEvent)
        return sideCenterDock(preferredSide)
      const cur = clientToCanvas(pointerEvent.clientX, pointerEvent.clientY)
      if (!cur)
        return sideCenterDock(preferredSide)
      return dockOnSidePreferCenter(pos, preferredSide, cur.x, cur.y)
    }
    if (!pointerEvent)
      return null
    const cur = clientToCanvas(pointerEvent.clientX, pointerEvent.clientY)
    if (!cur)
      return null
    const raw = nearestDockOnBox(pos, cur.x, cur.y)
    return dockOnSidePreferCenter(pos, raw.side, cur.x, cur.y)
  }

  function beginConnectFrom(node, pointerEvent, side = null) {
    if (!node || node.nodeType === 'end') {
      warn('请先点其它节点作为连线起点')
      return
    }
    connectSourceId = node.id
    pendingSourceDockLocked = Boolean(side)
    // 点边锚点：该边中心；点卡片：先记底边中心，完成时再按相对位置校正
    pendingSourceDock = side
      ? dockFromPointer(node.id, pointerEvent, side)
      : sideCenterDock('bottom')
    selectedEdgeId.value = null
    designer.selectedNodeId.value = null
    const pos = layoutResult.value.nodePositions.get(node.id)
    const tip = pointerEvent
      ? (clientToCanvas(pointerEvent.clientX, pointerEvent.clientY) || null)
      : null
    linking.value = {
      mode: 'new',
      sourceId: node.id,
      sourceDock: pendingSourceDock,
      x: tip?.x ?? ((pos?.x || 0) + (pos?.width || 0) / 2),
      y: tip?.y ?? ((pos?.y || 0) + (pos?.height || 0) + 32),
    }
    startConnectPreviewFollow()
  }

  function completeConnectTo(node, pointerEvent = null, side = null) {
    if (!node || !connectSourceId)
      return
    if (node.id === connectSourceId) {
      cancelConnect()
      return
    }
    if (node.nodeType === 'start') {
      warn('开始节点不能作为连线目标')
      return
    }
    const existed = designer.getOutgoingEdges(connectSourceId)?.some(e => e.target === node.id)
    if (existed) {
      tip('这两个节点之间已有连线')
      cancelConnect()
      return
    }
    const sourcePos = layoutResult.value.nodePositions.get(connectSourceId)
    const targetPos = layoutResult.value.nodePositions.get(node.id)
    const endTarget = node.nodeType === 'end'
    let sourceDock = pendingSourceDock
    let targetDock = null

    if (endTarget) {
      // 结束节点只有顶心锚点，禁止挂左/底导致线偏或绕圈
      targetDock = { ...END_INCOMING_DOCK }
      if (!pendingSourceDockLocked && sourcePos && targetPos) {
        const facing = facingCenterDocks(sourcePos, targetPos, { endTarget: true })
        sourceDock = facing.sourceDock
      }
    }
    else if (side) {
      // 用户点了目标某条边 → 该边中心（可轻微偏移）
      targetDock = dockFromPointer(node.id, pointerEvent, side)
    }
    else if (sourcePos && targetPos) {
      // 点卡片：按朝向选四边中心，绝不能从上往下却挂目标底边
      const facing = facingCenterDocks(sourcePos, targetPos)
      targetDock = facing.targetDock
      if (!pendingSourceDockLocked)
        sourceDock = facing.sourceDock
    }
    else {
      targetDock = sideCenterDock('top')
    }

    history.snapshot()
    try {
      designer.connectNodes(connectSourceId, node.id, {
        uiSourceDock: sourceDock,
        uiTargetDock: targetDock,
      })
      cancelConnect()
    }
    catch (err) {
      console.warn(err)
      warn(String(err?.message || '连线失败').replace(/^connectNodes:\s*/, ''))
    }
  }

  function clearReconnect() {
    if (linking.value?.mode === 'reconnect')
      linking.value = null
  }

  function setTool(next) {
    tool.value = next || 'select'
    cancelConnect()
    linking.value = null
    clearLinkDragListeners()
    selectedWaypointIndex.value = null
    if (next !== 'delete')
      selectedEdgeId.value = null
    if (next === 'connect')
      tip('请点击起始节点，移动鼠标出现蓝虚线，再点击结束节点完成连线')
  }

  function isReconnectDragging() {
    return linking.value?.mode === 'reconnect'
  }

  function onNodePointerDown(event, node) {
    if (isReadonly() || !node || event.button !== 0)
      return
    if (event.target?.closest?.('.node-port'))
      return
    if (tool.value === 'pan')
      return
    if (isReconnectDragging())
      return

    if (tool.value === 'delete') {
      event.preventDefault()
      event.stopPropagation()
      if (node.nodeType === 'start' || node.nodeType === 'end')
        return
      history.snapshot()
      try {
        designer.deleteNode(node.id)
      }
      catch (err) {
        console.warn(err)
      }
      return
    }

    if (tool.value === 'connect') {
      event.preventDefault()
      event.stopPropagation()
      // 点选连线只走 pointerdown；吞掉随后 click，避免「点源被二次 click 取消」或弹出配置
      suppressNextNodeClick = true
      if (!connectSourceId)
        beginConnectFrom(node, event)
      else
        completeConnectTo(node, event)
      return
    }

    const pos = layoutResult.value.nodePositions.get(node.id)
    const start = clientToCanvas(event.clientX, event.clientY)
    if (!start)
      return
    dragOrigin = {
      nodeId: node.id,
      pointerX: start.x,
      pointerY: start.y,
      nodeX: pos?.x ?? 0,
      nodeY: pos?.y ?? 0,
      moved: false,
      snapshotted: false,
    }

    const onMove = (e) => {
      if (!dragOrigin)
        return
      const cur = clientToCanvas(e.clientX, e.clientY)
      if (!cur)
        return
      const dx = cur.x - dragOrigin.pointerX
      const dy = cur.y - dragOrigin.pointerY
      if (!dragOrigin.moved && Math.hypot(dx, dy) < 4)
        return
      if (!dragOrigin.moved) {
        freezeIfNeeded()
        if (!dragOrigin.snapshotted) {
          history.snapshot()
          dragOrigin.snapshotted = true
        }
        dragOrigin.moved = true
        draggingNodeId.value = dragOrigin.nodeId
      }
      const selfPos = layoutResult.value.nodePositions.get(dragOrigin.nodeId)
      const nextBox = {
        x: dragOrigin.nodeX + dx,
        y: dragOrigin.nodeY + dy,
        width: selfPos?.width || 200,
        height: selfPos?.height || 56,
      }
      const others = []
      for (const [id, pos] of layoutResult.value.nodePositions.entries()) {
        if (id === dragOrigin.nodeId || !pos)
          continue
        others.push({ id, x: pos.x, y: pos.y, width: pos.width, height: pos.height })
      }
      const guides = computeAlignmentGuides(nextBox, others, ALIGN_THRESHOLD)
      alignmentGuides.value = guides.lines
      designer.moveNodePosition(dragOrigin.nodeId, {
        x: guides.snapX ?? nextBox.x,
        y: guides.snapY ?? nextBox.y,
      })
    }
    const onUp = () => {
      draggingNodeId.value = null
      alignmentGuides.value = []
      dragOrigin = null
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('pointerup', onUp)
    }
    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerup', onUp)
  }

  function onNodeClick(node) {
    if (isReadonly() || !node)
      return false
    if (suppressNextNodeClick) {
      suppressNextNodeClick = false
      return true
    }
    // 连线工具已在 pointerdown 处理，这里只拦截，避免打开配置抽屉
    if (tool.value === 'connect' || linking.value?.mode === 'new')
      return true
    return false
  }

  /**
   * 四边锚点：带 side 指定从哪条边出/入
   */
  function startLink(event, node, side = null) {
    if (isReadonly() || !node)
      return
    if (tool.value === 'pan' || tool.value === 'delete')
      return
    event.preventDefault()
    event.stopPropagation()
    suppressNextNodeClick = true

    if (tool.value === 'connect' || linking.value?.mode === 'new') {
      if (!connectSourceId)
        beginConnectFrom(node, event, side)
      else
        completeConnectTo(node, event, side)
      return
    }

    if (node.nodeType === 'end')
      return
    const start = clientToCanvas(event.clientX, event.clientY)
    if (!start)
      return
    clearLinkDragListeners()
    clearReconnect()
    connectSourceId = node.id
    pendingSourceDockLocked = Boolean(side)
    pendingSourceDock = side
      ? dockFromPointer(node.id, event, side)
      : sideCenterDock('bottom')
    linking.value = {
      mode: 'new',
      sourceId: node.id,
      sourceDock: pendingSourceDock,
      x: start.x,
      y: start.y,
    }
    startConnectPreviewFollow()
    linkUpHandler = (e) => {
      finishNewLinkDrag(e)
      clearLinkDragListeners()
    }
    window.addEventListener('pointerup', linkUpHandler)
  }

  function finishNewLinkDrag(event) {
    const state = linking.value
    if (!state?.sourceId || state.mode !== 'new') {
      cancelConnect()
      return
    }
    const targetId = findDropNodeId(event.clientX, event.clientY, state.sourceId)
    if (!targetId) {
      cancelConnect()
      return
    }
    const target = designer.getNode(targetId)
    completeConnectTo(target || { id: targetId }, event)
  }

  /** 拖起/终点：蓝虚线跟手，松在任意节点边上改接（可换到同一节点另一条边） */
  function onEndpointDown(edge, which, event) {
    if (isReadonly() || !edge?.id || (which !== 'source' && which !== 'target'))
      return
    if (tool.value === 'pan' || tool.value === 'delete')
      return
    event.preventDefault()
    event.stopPropagation()
    const fixedId = which === 'target' ? edge.source : edge.target
    const movingNodeId = which === 'target' ? edge.target : edge.source
    const start = clientToCanvas(event.clientX, event.clientY)
    if (!start || !fixedId)
      return
    clearLinkDragListeners()
    stopConnectPreviewFollow()
    selectedEdgeId.value = edge.id
    selectedWaypointIndex.value = null
    designer.selectedNodeId.value = null
    linking.value = {
      mode: 'reconnect',
      edgeId: edge.id,
      which,
      fixedId,
      movingNodeId,
      x: start.x,
      y: start.y,
    }
    tip('拖到节点任意边上松开即可改接（可换到上/下/左/右）')
    linkMoveHandler = (e) => {
      if (!linking.value || linking.value.mode !== 'reconnect')
        return
      const cur = clientToCanvas(e.clientX, e.clientY)
      if (!cur)
        return
      linking.value = { ...linking.value, x: cur.x, y: cur.y }
    }
    linkUpHandler = (e) => {
      finishReconnectDrag(e)
      clearLinkDragListeners()
    }
    window.addEventListener('pointermove', linkMoveHandler, true)
    window.addEventListener('pointerup', linkUpHandler, true)
  }

  function finishReconnectDrag(event) {
    const state = linking.value
    linking.value = null
    if (state?.mode !== 'reconnect' || !state.edgeId)
      return
    const edge = designer.getEdge(state.edgeId)
    if (!edge)
      return
    const preferId = state.movingNodeId || (state.which === 'target' ? edge.target : edge.source)
    const dropId = findDropNodeId(
      event.clientX,
      event.clientY,
      null,
      RECONNECT_SNAP_PX,
      preferId,
    )
    if (!dropId) {
      warn('请拖到节点边上松开以改接锚点')
      return
    }
    const dropPos = layoutResult.value.nodePositions.get(dropId)
    const dropNode = designer.getNode(dropId)
    const cur = clientToCanvas(event.clientX, event.clientY)
    // 改接到结束节点：无论松在哪一侧，一律顶心
    const dock = (state.which === 'target' && dropNode?.nodeType === 'end')
      ? { ...END_INCOMING_DOCK }
      : (dropPos && cur
        ? dockOnSidePreferCenter(dropPos, nearestDockOnBox(dropPos, cur.x, cur.y).side, cur.x, cur.y)
        : sideCenterDock('top'))

    // 拖到同一端点节点的另一条边 → 只换锚点
    if (state.which === 'source' && dropId === edge.source) {
      suppressNextNodeClick = true
      history.snapshot()
      designer.setEdgeDock(edge.id, 'source', dock)
      return
    }
    if (state.which === 'target' && dropId === edge.target) {
      suppressNextNodeClick = true
      history.snapshot()
      designer.setEdgeDock(edge.id, 'target', dock)
      return
    }

    const newSource = state.which === 'source' ? dropId : edge.source
    const newTarget = state.which === 'target' ? dropId : edge.target
    if (!newSource || !newTarget || newSource === newTarget) {
      warn('不能接到同一节点')
      return
    }
    if (newSource === edge.source && newTarget === edge.target) {
      // 同节点同端：仍可能只是换了边，上面分支已处理；此处兜底写 dock
      suppressNextNodeClick = true
      history.snapshot()
      designer.setEdgeDock(edge.id, state.which, dock)
      return
    }
    suppressNextNodeClick = true
    history.snapshot()
    try {
      const targetIsEnd = designer.getNode(newTarget)?.nodeType === 'end'
      designer.reconnect(state.edgeId, newSource, newTarget, {
        sourceDock: state.which === 'source' ? dock : (edge.uiSourceDock || null),
        targetDock: targetIsEnd
          ? { ...END_INCOMING_DOCK }
          : (state.which === 'target' ? dock : (edge.uiTargetDock || null)),
      })
    }
    catch (err) {
      console.warn(err)
      warn(err?.message || '改接失败')
      suppressNextNodeClick = false
    }
  }

  /**
   * 已选中连线：
   * - 单击：不加拐点
   * - 最近段为纯横/竖：段平移（蓝块）；斜线不出蓝块
   * - 最近段为斜线：按住拖动才插入拐点（避免横竖段逻辑抢走）
   */
  function onEdgeHitPointerDown(edge, event) {
    if (isReadonly() || !edge?.id || event.button !== 0)
      return
    if (tool.value === 'pan' || tool.value === 'delete')
      return
    if (selectedEdgeId.value !== edge.id)
      return
    if (event.target?.closest?.('.edge-endpoint, .edge-bend, .edge-seg-handle, .edge-endpoint-group'))
      return
    const pt = clientToCanvas(event.clientX, event.clientY)
    const path = layoutResult.value.edgePaths.get(edge.id)
    if (!pt || !path?.points?.length)
      return
    // 以「整线最近段」为准：斜线优先留给拐点，不能被邻近横竖段抢走
    const nearAny = findNearestSegment(path.points, pt.x, pt.y, 18, false)
    if (!nearAny)
      return
    event.preventDefault()
    event.stopPropagation()

    // 纯横/竖段：进入段平移（与蓝块拖动一致）
    if (nearAny.horizontal || nearAny.vertical) {
      startSegmentDrag(edge, nearAny.index, path.points)
      return
    }

    // 斜线段：按住拖动才生成拐点；松开未移动则不加
    clearLinkDragListeners()
    clearReconnect()
    freezeIfNeeded()
    let points = path.points.map(p => ({ x: p.x, y: p.y }))
    let fullIndex = -1
    let inserted = false

    linkMoveHandler = (e) => {
      const cur = clientToCanvas(e.clientX, e.clientY)
      if (!cur)
        return
      if (!inserted) {
        if (Math.hypot(cur.x - pt.x, cur.y - pt.y) < 5)
          return
        history.snapshot()
        const res = insertBendAt(points, pt.x, pt.y, 24)
        points = res.points
        fullIndex = res.bendIndex + 1
        inserted = true
        designer.setEdgeWaypoints(edge.id, toStoredWaypoints(points))
        selectedWaypointIndex.value = res.bendIndex
        const refreshed0 = layoutResult.value.edgePaths.get(edge.id)?.points
        if (refreshed0?.length)
          points = refreshed0.map(p => ({ x: p.x, y: p.y }))
      }
      if (fullIndex < 1 || !points[fullIndex])
        return
      points[fullIndex] = { x: cur.x, y: cur.y }
      designer.setEdgeWaypoints(edge.id, toStoredWaypoints(points))
      const refreshed = layoutResult.value.edgePaths.get(edge.id)?.points
      if (refreshed?.length) {
        points = refreshed.map(p => ({ x: p.x, y: p.y }))
        let nearest = fullIndex
        let best = Infinity
        for (let i = 1; i < points.length - 1; i++) {
          const d = Math.hypot(points[i].x - cur.x, points[i].y - cur.y)
          if (d < best) {
            best = d
            nearest = i
          }
        }
        fullIndex = nearest
        selectedWaypointIndex.value = Math.max(0, fullIndex - 1)
      }
    }
    linkUpHandler = () => {
      // 单击不加拐点
      clearLinkDragListeners()
    }
    window.addEventListener('pointermove', linkMoveHandler)
    window.addEventListener('pointerup', linkUpHandler)
  }

  function startSegmentDrag(edge, segmentIndex, initialPoints) {
    clearLinkDragListeners()
    clearReconnect()
    freezeIfNeeded()
    let points = initialPoints.map(p => ({ x: p.x, y: p.y }))
    let activeSeg = segmentIndex
    const seed = points[segmentIndex]
    const seedNext = points[segmentIndex + 1]
    const preferHorizontal = seed && seedNext && Math.abs(seed.y - seedNext.y) < 1.5
    let snapshotted = false
    selectedWaypointIndex.value = null

    linkMoveHandler = (e) => {
      const cur = clientToCanvas(e.clientX, e.clientY)
      if (!cur)
        return
      if (!snapshotted) {
        history.snapshot()
        snapshotted = true
      }
      points = moveOrthoSegment(points, activeSeg, cur)
      designer.setEdgeWaypoints(edge.id, toStoredWaypoints(points))
      const refreshed = layoutResult.value.edgePaths.get(edge.id)?.points
      if (refreshed?.length)
        points = refreshed.map(p => ({ x: p.x, y: p.y }))
      // 首/末段插入拐点后下标会变：按指针重锚定同向最近段，避免反复 splice 爆点
      const again = findNearestSegment(points, cur.x, cur.y, 48)
      if (again && again.horizontal === preferHorizontal)
        activeSeg = again.index
    }
    linkUpHandler = () => clearLinkDragListeners()
    window.addEventListener('pointermove', linkMoveHandler)
    window.addEventListener('pointerup', linkUpHandler)
  }

  /** 拖拐角蓝点 */
  function onBendDown(edge, bendIndex, event) {
    if (isReadonly() || !edge?.id || !Number.isInteger(bendIndex))
      return
    if (tool.value === 'pan' || tool.value === 'delete')
      return
    event.preventDefault()
    event.stopPropagation()
    const path = layoutResult.value.edgePaths.get(edge.id)
    if (!path?.points || path.points.length < 3)
      return
    clearLinkDragListeners()
    clearReconnect()
    freezeIfNeeded()
    selectedEdgeId.value = edge.id
    selectedWaypointIndex.value = bendIndex
    designer.selectedNodeId.value = null

    let points = path.points.map(p => ({ x: p.x, y: p.y }))
    let fullIndex = bendIndex + 1
    let snapshotted = false

    linkMoveHandler = (e) => {
      const cur = clientToCanvas(e.clientX, e.clientY)
      if (!cur || !points[fullIndex])
        return
      if (!snapshotted) {
        history.snapshot()
        snapshotted = true
      }
      // 拐点随意放置（允许斜线），与 bpmn.io 一致
      points[fullIndex] = { x: cur.x, y: cur.y }
      designer.setEdgeWaypoints(edge.id, toStoredWaypoints(points))
      const refreshed = layoutResult.value.edgePaths.get(edge.id)?.points
      if (refreshed?.length) {
        points = refreshed.map(p => ({ x: p.x, y: p.y }))
        let nearest = fullIndex
        let best = Infinity
        for (let i = 1; i < points.length - 1; i++) {
          const d = Math.hypot(points[i].x - cur.x, points[i].y - cur.y)
          if (d < best) {
            best = d
            nearest = i
          }
        }
        fullIndex = nearest
        selectedWaypointIndex.value = Math.max(0, fullIndex - 1)
      }
    }
    linkUpHandler = () => clearLinkDragListeners()
    window.addEventListener('pointermove', linkMoveHandler)
    window.addEventListener('pointerup', linkUpHandler)
  }

  /** 拖线段中点胶囊：平移整段 */
  function onSegmentDown(edge, segmentIndex, event) {
    if (isReadonly() || !edge?.id || !Number.isInteger(segmentIndex))
      return
    if (tool.value === 'pan' || tool.value === 'delete')
      return
    event.preventDefault()
    event.stopPropagation()
    const path = layoutResult.value.edgePaths.get(edge.id)
    if (!path?.points?.length)
      return
    selectedEdgeId.value = edge.id
    selectedWaypointIndex.value = null
    designer.selectedNodeId.value = null
    startSegmentDrag(edge, segmentIndex, path.points)
  }

  function onCanvasDrop(event) {
    if (isReadonly())
      return
    const type = event.dataTransfer?.getData('application/x-forge-flow-node')
    if (!type)
      return
    event.preventDefault()
    freezeIfNeeded()
    const point = clientToCanvas(event.clientX, event.clientY)
    if (!point)
      return
    history.snapshot()
    try {
      const id = designer.placeNode(type, {
        x: point.x - 140,
        y: point.y - 38,
      })
      onNodePlaced?.(id)
    }
    catch (err) {
      console.warn(err)
    }
  }

  function onCanvasDragOver(event) {
    if (isReadonly())
      return
    if (event.dataTransfer?.types?.includes?.('application/x-forge-flow-node')) {
      event.preventDefault()
      event.dataTransfer.dropEffect = 'copy'
    }
  }

  function autoLayout() {
    if (isReadonly())
      return
    history.snapshot()
    designer.clearNodePositions()
    selectedEdgeId.value = null
    selectedWaypointIndex.value = null
    clearReconnect()
  }

  function selectEdge(edge) {
    if (isReadonly() || !edge?.id)
      return
    if (tool.value === 'delete') {
      history.snapshot()
      try {
        designer.disconnectEdge(edge.id)
      }
      catch (err) {
        console.warn(err)
      }
      selectedEdgeId.value = null
      selectedWaypointIndex.value = null
      clearReconnect()
      return
    }
    selectedEdgeId.value = edge.id
    selectedWaypointIndex.value = null
    designer.selectedNodeId.value = null
    clearReconnect()
  }

  function clearEdgeSelection() {
    clearReconnect()
    selectedEdgeId.value = null
    selectedWaypointIndex.value = null
    // 点空白：若正在点选连线则取消
    if (tool.value === 'connect' && connectSourceId)
      cancelConnect()
  }

  function removeSelectedWaypoint() {
    const edgeId = selectedEdgeId.value
    const idx = selectedWaypointIndex.value
    if (!edgeId || idx == null)
      return false
    const path = layoutResult.value.edgePaths.get(edgeId)
    if (!path?.points || path.points.length < 3)
      return false
    history.snapshot()
    const points = path.points.map(p => ({ x: p.x, y: p.y }))
    points.splice(idx + 1, 1)
    designer.setEdgeWaypoints(edgeId, toStoredWaypoints(points))
    selectedWaypointIndex.value = null
    return true
  }

  function removeSelectedEdge() {
    if (isReadonly() || !selectedEdgeId.value)
      return
    if (removeSelectedWaypoint())
      return
    history.snapshot()
    try {
      designer.disconnectEdge(selectedEdgeId.value)
    }
    catch (err) {
      console.warn(err)
    }
    finally {
      selectedEdgeId.value = null
      selectedWaypointIndex.value = null
      clearReconnect()
    }
  }

  function onKeyDown(event) {
    if (isReadonly())
      return
    if (event.target?.closest?.('input, textarea, select, [contenteditable="true"]'))
      return
    if (event.key === 'Delete' || event.key === 'Backspace') {
      if (!selectedEdgeId.value)
        return
      event.preventDefault()
      removeSelectedEdge()
    }
    if (event.key === 'Escape') {
      if (connectSourceId || linking.value?.mode === 'new') {
        cancelConnect()
        return
      }
      clearEdgeSelection()
      clearLinkDragListeners()
      if (tool.value !== 'select')
        setTool('select')
    }
  }

  onMounted(() => {
    window.addEventListener('keydown', onKeyDown)
  })
  onBeforeUnmount(() => {
    window.removeEventListener('keydown', onKeyDown)
    clearLinkDragListeners()
    stopConnectPreviewFollow()
  })

  return {
    tool,
    setTool,
    linking,
    draggingNodeId,
    alignmentGuides,
    selectedEdgeId,
    selectedWaypointIndex,
    isReconnectDragging,
    onNodePointerDown,
    onNodeClick,
    startLink,
    onEndpointDown,
    onEdgeHitPointerDown,
    onBendDown,
    onSegmentDown,
    onCanvasDrop,
    onCanvasDragOver,
    autoLayout,
    selectEdge,
    clearEdgeSelection,
    removeSelectedEdge,
    cancelConnect,
  }
}
