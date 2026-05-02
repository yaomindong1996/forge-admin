import dagre from 'dagre'

/**
 * Auto-arrange BPMN diagram elements using dagre layout algorithm.
 * @param {Object} modeler - bpmn-js modeler instance
 */
export default function autoLayout(modeler) {
  const elementRegistry = modeler.get('elementRegistry')
  const modeling = modeler.get('modeling')
  const commandStack = modeler.get('commandStack')
  const canvas = modeler.get('canvas')

  const elements = elementRegistry.getAll()

  const shapes = []
  const edges = []

  for (const el of elements) {
    if (el.waypoints) {
      // Sequence flow / connection
      const bo = el.businessObject
      if (bo && bo.sourceRef && bo.targetRef) {
        edges.push({
          id: el.id,
          sourceId: bo.sourceRef.id || (bo.sourceRef && bo.sourceRef.id),
          targetId: bo.targetRef.id || (bo.targetRef && bo.targetRef.id),
        })
      }
    }
    else {
      shapes.push(el)
    }
  }

  const g = new dagre.graphlib.Graph()
  g.setGraph({
    rankdir: 'LR',
    nodesep: 80,
    ranksep: 120,
    marginx: 40,
    marginy: 40,
  })
  g.setDefaultEdgeLabel(() => ({}))

  const nodeSize = (type) => {
    if (!type) return [120, 60]
    if (type.includes('Task') || type.includes('CallActivity') || type.includes('SubProcess'))
      return [120, 60]
    if (type.includes('Event'))
      return [50, 50]
    if (type.includes('Gateway'))
      return [50, 50]
    return [120, 60]
  }

  for (const shape of shapes) {
    const [w, h] = nodeSize(shape.type)
    g.setNode(shape.id, { width: w, height: h })
  }

  for (const edge of edges) {
    if (g.hasNode(edge.sourceId) && g.hasNode(edge.targetId)) {
      g.setEdge(edge.sourceId, edge.targetId)
    }
  }

  dagre.layout(g)

  const moves = []
  for (const shape of shapes) {
    const dagreNode = g.node(shape.id)
    if (!dagreNode) continue

    const [w] = nodeSize(shape.type)
    const targetX = dagreNode.x - w / 2
    const targetY = dagreNode.y - (nodeSize(shape.type)[1] / 2)

    const dx = targetX - shape.x
    const dy = targetY - shape.y

    if (Math.abs(dx) > 0.5 || Math.abs(dy) > 0.5) {
      moves.push({ element: shape, dx, dy })
    }
  }

  if (moves.length > 0) {
    commandStack.execute('elements.move', {
      elements: moves.map(m => m.element),
      delta: {
        x: moves.reduce((s, m) => s + m.dx, 0) / moves.length,
        y: moves.reduce((s, m) => s + m.dy, 0) / moves.length,
      },
    })
  }

  for (const { element, dx, dy } of moves) {
    modeling.moveElements([element], { x: dx, y: dy })
  }

  canvas.zoom('fit-viewport')
}
