import dagre from 'dagre'

/**
 * Auto-arrange BPMN diagram elements using dagre layout algorithm.
 * @param {object} modeler - bpmn-js modeler instance
 */
export default function autoLayout(modeler) {
  const elementRegistry = modeler.get('elementRegistry')
  const modeling = modeler.get('modeling')
  const canvas = modeler.get('canvas')

  const elements = elementRegistry.getAll()

  const shapes = []
  const edges = []

  // 分离节点和连接线
  for (const el of elements) {
    if (el.waypoints) {
      // 连接线
      const bo = el.businessObject
      if (bo && bo.sourceRef && bo.targetRef) {
        edges.push({
          id: el.id,
          sourceId: bo.sourceRef.id,
          targetId: bo.targetRef.id,
        })
      }
    }
    else if (el.type !== 'bpmn:Process' && el.type !== 'label') {
      // 节点（排除 Process 和 label）
      shapes.push(el)
    }
  }

  if (shapes.length === 0) {
    console.warn('没有可布局的节点')
    return
  }

  // 创建 dagre 图
  const g = new dagre.graphlib.Graph()
  g.setGraph({
    rankdir: 'LR', // 从左到右
    nodesep: 100, // 节点间距
    ranksep: 150, // 层级间距
    marginx: 50,
    marginy: 50,
  })
  g.setDefaultEdgeLabel(() => ({}))

  // 根据节点类型确定大小
  const getNodeSize = (type) => {
    if (!type)
      return { width: 120, height: 80 }
    if (type.includes('Task') || type.includes('CallActivity') || type.includes('SubProcess'))
      return { width: 120, height: 80 }
    if (type.includes('Event'))
      return { width: 36, height: 36 }
    if (type.includes('Gateway'))
      return { width: 50, height: 50 }
    return { width: 120, height: 80 }
  }

  // 添加节点到图
  for (const shape of shapes) {
    const size = getNodeSize(shape.type)
    g.setNode(shape.id, size)
  }

  // 添加边到图
  for (const edge of edges) {
    if (g.hasNode(edge.sourceId) && g.hasNode(edge.targetId)) {
      g.setEdge(edge.sourceId, edge.targetId)
    }
  }

  // 执行布局
  dagre.layout(g)

  // 移动节点到新位置
  const updates = []
  for (const shape of shapes) {
    const node = g.node(shape.id)
    if (!node)
      continue

    const size = getNodeSize(shape.type)
    // dagre 返回的是中心点坐标，需要转换为左上角坐标
    const newX = Math.round(node.x - size.width / 2)
    const newY = Math.round(node.y - size.height / 2)

    updates.push({
      element: shape,
      newPosition: { x: newX, y: newY },
    })
  }

  // 批量更新位置
  if (updates.length > 0) {
    for (const { element, newPosition } of updates) {
      modeling.moveShape(element, newPosition)
    }
  }

  // 适应视口
  setTimeout(() => {
    canvas.zoom('fit-viewport')
  }, 100)
}
