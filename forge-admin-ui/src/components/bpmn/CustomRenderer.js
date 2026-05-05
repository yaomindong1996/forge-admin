import { is } from 'bpmn-js/lib/util/ModelUtil'
import BaseRenderer from 'diagram-js/lib/draw/BaseRenderer'
import inherits from 'inherits-browser'
import { append as svgAppend, attr as svgAttr, create as svgCreate } from 'tiny-svg'

const HIGH_PRIORITY = 2000

const TASK_WIDTH = 100
const TASK_HEIGHT = 80
const EVENT_SIZE = 36
const GATEWAY_SIZE = 50

const COLORS = {
  start: { from: '#34d399', to: '#10b981' },
  end: { from: '#f87171', to: '#ef4444' },
  userTask: { from: '#60a5fa', to: '#3b82f6' },
  serviceTask: { from: '#a78bfa', to: '#8b5cf6' },
  scriptTask: { from: '#fbbf24', to: '#f59e0b' },
  callActivity: { from: '#818cf8', to: '#6366f1' },
  exclusive: { from: '#fcd34d', to: '#fbbf24' },
  parallel: { from: '#67e8f9', to: '#06b6d4' },
  inclusive: { from: '#bef264', to: '#84cc16' },
  subProcess: { from: '#e0e7ff', to: '#c7d2fe' },
}

const LABEL_STYLE = {
  fontSize: 12,
  fontFamily: 'Arial, sans-serif',
  fontWeight: 500,
  fill: '#334155',
}

function ensureDefs(svgDoc) {
  if (!svgDoc)
    return null
  let defs = svgDoc.querySelector('defs')
  if (!defs) {
    defs = svgCreate('defs')
    svgDoc.insertBefore(defs, svgDoc.firstChild)
  }
  return defs
}

function hasDef(defs, id) {
  return !!defs.querySelector(`#${id}`)
}

function createGradient(defs, id, fromColor, toColor) {
  if (hasDef(defs, id))
    return
  const grad = svgCreate('linearGradient')
  svgAttr(grad, { id, x1: '0%', y1: '0%', x2: '100%', y2: '100%' })
  const s1 = svgCreate('stop')
  svgAttr(s1, { 'offset': '0%', 'stop-color': fromColor })
  const s2 = svgCreate('stop')
  svgAttr(s2, { 'offset': '100%', 'stop-color': toColor })
  grad.appendChild(s1)
  grad.appendChild(s2)
  defs.appendChild(grad)
}

function createShadowFilter(defs) {
  const id = 'forge-node-shadow'
  if (hasDef(defs, id))
    return
  const filter = svgCreate('filter')
  svgAttr(filter, { id, x: '-20%', y: '-20%', width: '140%', height: '140%' })
  const dropShadow = svgCreate('feDropShadow')
  svgAttr(dropShadow, { 'dx': '0', 'dy': '2', 'stdDeviation': '3', 'flood-color': 'rgba(0,0,0,0.12)', 'flood-opacity': '1' })
  filter.appendChild(dropShadow)
  defs.appendChild(filter)
}

function drawRoundRect(parent, x, y, w, h, rx, attrs) {
  const rect = svgCreate('rect')
  svgAttr(rect, { x, y, width: w, height: h, rx, ry: rx, ...attrs })
  svgAppend(parent, rect)
  return rect
}

function drawCircle(parent, cx, cy, r, attrs) {
  const circle = svgCreate('circle')
  svgAttr(circle, { cx, cy, r, ...attrs })
  svgAppend(parent, circle)
  return circle
}

function drawText(parent, x, y, text, attrs) {
  const t = svgCreate('text')
  svgAttr(t, { x, y, 'text-anchor': 'middle', ...LABEL_STYLE, ...attrs })
  t.textContent = text
  svgAppend(parent, t)
  return t
}

function drawIcon(parent, cx, cy, iconPath, boxSize, attrs) {
  const scale = boxSize / 24
  const g = svgCreate('g')
  svgAttr(g, { transform: `translate(${cx - boxSize / 2}, ${cy - boxSize / 2}) scale(${scale})` })
  const path = svgCreate('path')
  svgAttr(path, {
    'd': iconPath,
    'fill': 'none',
    'stroke': '#fff',
    'stroke-width': '2.5',
    'stroke-linecap': 'round',
    'stroke-linejoin': 'round',
    ...attrs,
  })
  g.appendChild(path)
  svgAppend(parent, g)
}

const ICONS = {
  play: 'M6 4l14 8-14 8z',
  stop: 'M6 6h12v12H6z',
  person: 'M12 12c2.2 0 4-1.8 4-4s-1.8-4-4-4-4 1.8-4 4 1.8 4 4 4zm0 2c-2.7 0-8 1.3-8 4v2h16v-2c0-2.7-5.3-4-8-4z',
  gear: 'M12 15a3 3 0 100-6 3 3 0 000 6z M20.3 8.6l-1.1-2L17 7.8a7 7 0 00-2 0l-2.1-1.3-1.1 2 1.5 1.9c-.4.7-.6 1.4-.6 2.1s.2 1.4.6 2.1l-1.5 2 1.1 1.9 2.1-1.2a7 7 0 002 0l2.1 1.2 1.1-1.9-1.5-2c.4-.7.6-1.4.6-2.1s-.2-1.4-.6-2.1l1.5-1.9z',
  code: 'M8 7l-3 5 3 5 M16 7l3 5-3 5 M14 4l-4 16',
  external: 'M14 10h4v4 M15 3l6 6 M21 3h-6 M10 14l11-11',
  x: 'M12 12l-6 6 M12 12l6-6 M12 12l-6-6 M12 12l6 6',
  plus: 'M8 12h8 M12 8v8',
  circle: 'M12 12m-6 0a6 6 0 106 0 6 6 0 10-6 0',
}

function CustomRenderer(eventBus, styles, textRenderer, pathMap) {
  BaseRenderer.call(this, eventBus, HIGH_PRIORITY)
  this._svgCache = null
}

inherits(CustomRenderer, BaseRenderer)
CustomRenderer.$inject = ['eventBus', 'styles', 'textRenderer', 'pathMap']

CustomRenderer.prototype.canRender = function (element) {
  return !is(element, 'bpmn:Process')
    && !is(element, 'bpmn:Collaboration')
    && !is(element, 'bpmn:Participant')
    && !is(element, 'bpmn:Lane')
    && !is(element, 'bpmn:Group')
    && !is(element, 'bpmn:TextAnnotation')
}

CustomRenderer.prototype.drawShape = function (parentNode, element) {
  const svgDoc = parentNode.ownerSVGElement || parentNode.ownerDocument?.documentElement
  if (svgDoc && svgDoc !== this._svgCache) {
    this._svgCache = svgDoc
    const defs = ensureDefs(svgDoc)
    if (defs) {
      createShadowFilter(defs)
      for (const [key, colors] of Object.entries(COLORS)) {
        createGradient(defs, `grad-${key}`, colors.from, colors.to)
      }
    }
  }

  const { width: elW, height: elH } = element
  const w = elW || TASK_WIDTH
  const h = elH || TASK_HEIGHT
  const cx = w / 2
  const cy = h / 2

  const g = svgCreate('g')
  svgAttr(g, { 'class': 'djs-element', 'data-element-id': element.id })

  const type = element.type

  if (type === 'bpmn:StartEvent') {
    drawCircle(g, cx, cy, EVENT_SIZE / 2, {
      'fill': 'url(#grad-start)',
      'stroke': '#059669',
      'stroke-width': '2',
      'filter': 'url(#forge-node-shadow)',
    })
    drawCircle(g, cx, cy, EVENT_SIZE / 2 - 3, {
      'fill': 'none',
      'stroke': 'rgba(255,255,255,0.3)',
      'stroke-width': '1',
    })
    drawIcon(g, cx, cy, ICONS.play, 14, { fill: '#fff', stroke: 'none' })
  }
  else if (type === 'bpmn:EndEvent') {
    drawCircle(g, cx, cy, EVENT_SIZE / 2, {
      'fill': 'url(#grad-end)',
      'stroke': '#dc2626',
      'stroke-width': '3',
      'filter': 'url(#forge-node-shadow)',
    })
    drawIcon(g, cx, cy, ICONS.stop, 12, { fill: '#fff', stroke: 'none' })
  }
  else if (type === 'bpmn:UserTask') {
    drawRoundRect(g, 0, 0, w, h, 8, {
      fill: 'url(#grad-userTask)',
      filter: 'url(#forge-node-shadow)',
    })
    drawRoundRect(g, 0, 0, 6, h, 3, {
      fill: '#1d4ed8',
    })
    drawIcon(g, cx, cy, ICONS.person, 22, { fill: '#fff', stroke: 'none' })
  }
  else if (type === 'bpmn:ServiceTask') {
    drawRoundRect(g, 0, 0, w, h, 8, {
      fill: 'url(#grad-serviceTask)',
      filter: 'url(#forge-node-shadow)',
    })
    drawIcon(g, cx, cy, ICONS.gear, 22, { fill: '#fff', stroke: 'none' })
  }
  else if (type === 'bpmn:ScriptTask') {
    drawRoundRect(g, 0, 0, w, h, 8, {
      fill: 'url(#grad-scriptTask)',
      filter: 'url(#forge-node-shadow)',
    })
    drawIcon(g, cx, cy, ICONS.code, 18, { fill: '#fff', stroke: 'none' })
  }
  else if (type === 'bpmn:CallActivity') {
    drawRoundRect(g, 0, 0, w, h, 8, {
      'fill': 'url(#grad-callActivity)',
      'stroke': '#4f46e5',
      'stroke-width': '3',
      'filter': 'url(#forge-node-shadow)',
    })
    drawIcon(g, cx, cy, ICONS.external, 18, { fill: '#fff', stroke: 'none' })
  }
  else if (type === 'bpmn:SubProcess') {
    if (element.collapsed || element.isCollapsed) {
      drawRoundRect(g, 0, 0, w, h, 8, {
        'fill': 'rgba(199,210,254,0.5)',
        'stroke': '#818cf8',
        'stroke-width': '2',
        'stroke-dasharray': '6,3',
        'filter': 'url(#forge-node-shadow)',
      })
      drawRoundRect(g, w - 22, 2, 20, 16, 3, {
        fill: '#6366f1',
        opacity: '0.8',
      })
      const plus = svgCreate('path')
      svgAttr(plus, { 'd': 'M4 8h12 M10 2v12', 'stroke': '#fff', 'stroke-width': '2', 'stroke-linecap': 'round' })
      const g2 = svgCreate('g')
      svgAttr(g2, { transform: `translate(${w - 18}, 0)` })
      g2.appendChild(plus)
      g.appendChild(g2)
    }
    else {
      drawRoundRect(g, 0, 0, w, h, 8, {
        'fill': 'rgba(199,210,254,0.3)',
        'stroke': '#818cf8',
        'stroke-width': '2',
        'stroke-dasharray': '6,3',
      })
    }
  }
  else if (type === 'bpmn:ExclusiveGateway') {
    const diamond = svgCreate('polygon')
    svgAttr(diamond, {
      'points': `${cx},${cy - GATEWAY_SIZE / 2} ${cx + GATEWAY_SIZE / 2},${cy} ${cx},${cy + GATEWAY_SIZE / 2} ${cx - GATEWAY_SIZE / 2},${cy}`,
      'fill': 'url(#grad-exclusive)',
      'stroke': '#d97706',
      'stroke-width': '2',
      'filter': 'url(#forge-node-shadow)',
    })
    g.appendChild(diamond)
    drawIcon(g, cx, cy, ICONS.x, 14, { 'fill': 'none', 'stroke': '#92400e', 'stroke-width': '2.5' })
  }
  else if (type === 'bpmn:ParallelGateway') {
    const diamond = svgCreate('polygon')
    svgAttr(diamond, {
      'points': `${cx},${cy - GATEWAY_SIZE / 2} ${cx + GATEWAY_SIZE / 2},${cy} ${cx},${cy + GATEWAY_SIZE / 2} ${cx - GATEWAY_SIZE / 2},${cy}`,
      'fill': 'url(#grad-parallel)',
      'stroke': '#0891b2',
      'stroke-width': '2',
      'filter': 'url(#forge-node-shadow)',
    })
    g.appendChild(diamond)
    drawIcon(g, cx, cy, ICONS.plus, 12, { 'fill': 'none', 'stroke': '#155e75', 'stroke-width': '2.5' })
  }
  else if (type === 'bpmn:InclusiveGateway') {
    const diamond = svgCreate('polygon')
    svgAttr(diamond, {
      'points': `${cx},${cy - GATEWAY_SIZE / 2} ${cx + GATEWAY_SIZE / 2},${cy} ${cx},${cy + GATEWAY_SIZE / 2} ${cx - GATEWAY_SIZE / 2},${cy}`,
      'fill': 'url(#grad-inclusive)',
      'stroke': '#65a30d',
      'stroke-width': '2',
      'filter': 'url(#forge-node-shadow)',
    })
    g.appendChild(diamond)
    drawIcon(g, cx, cy, ICONS.circle, 12, { 'fill': 'none', 'stroke': '#3f6212', 'stroke-width': '2.5' })
  }
  else if (type === 'label') {
    return g
  }
  else {
    drawRoundRect(g, 0, 0, w, h, 8, {
      'fill': '#f1f5f9',
      'stroke': '#94a3b8',
      'stroke-width': '1.5',
    })
    const label = element.businessObject?.name || element.id
    drawText(g, cx, cy + 4, truncateLabel(label, 14))
  }

  svgAppend(parentNode, g)
  return g
}

function truncateLabel(label, maxLen) {
  if (!label)
    return ''
  return label.length > maxLen ? `${label.slice(0, maxLen - 1)}…` : label
}

export default CustomRenderer
