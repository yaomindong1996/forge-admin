/**
 * JSON → BPMN XML 转换器（Task 5）
 *
 * 输入：flowJson；输出：BPMN 2.0 XML 字符串。
 *
 * pitfalls #8：BPMNPlane.bpmnElement 必须指向真实 process id；本实现总写入 root process id。
 */

import { NODE_TYPE, NODE_TYPE_TO_BPMN_LOCAL_NAME } from '../constants/node-types.js'
import { calculateLayout } from './layout-algorithm.js'
import { writeUserTaskConfig } from './user-task-writer.js'
import { escapeXmlAttr, escapeXmlText } from './xml-escape.js'

const NS_DECLS = [
  'xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"',
  'xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"',
  'xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"',
  'xmlns:di="http://www.omg.org/spec/DD/20100524/DI"',
  'xmlns:flowable="http://flowable.org/bpmn"',
  'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"',
].join(' ')

export function convertJsonToBpmn(flowJson) {
  if (!flowJson || !Array.isArray(flowJson.nodes))
    return buildEmptyDefinitions()

  const processId = flowJson.processId || 'Process_1'
  const processName = flowJson.processName || ''
  const layout = calculateLayout(flowJson)

  const nodeXml = flowJson.nodes.map(writeNode).filter(Boolean)
  const edgeXml = flowJson.edges.map(writeEdge).filter(Boolean)
  const diagram = writeDiagram(processId, flowJson, layout)

  const procAttrs = [`id="${escapeXmlAttr(processId)}"`]
  if (processName)
    procAttrs.push(`name="${escapeXmlAttr(processName)}"`)
  procAttrs.push('isExecutable="true"')

  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    `<bpmn:definitions ${NS_DECLS} id="Definitions_1" targetNamespace="http://flowable.org/bpmn">`,
    `  <bpmn:process ${procAttrs.join(' ')}>`,
    ...nodeXml.map(s => `    ${s}`),
    ...edgeXml.map(s => `    ${s}`),
    '  </bpmn:process>',
    `  ${diagram}`,
    '</bpmn:definitions>',
  ].join('\n')
}

function writeNode(node) {
  if (node.nodeType === NODE_TYPE.ADVANCED && node.rawXml)
    return node.rawXml

  const local = NODE_TYPE_TO_BPMN_LOCAL_NAME[node.nodeType]
  if (!local)
    return ''

  const tag = `bpmn:${local}`
  const attrs = [`id="${escapeXmlAttr(node.id)}"`]
  if (node.name)
    attrs.push(`name="${escapeXmlAttr(node.name)}"`)
  const children = []

  if (node.config?.documentation)
    children.push(`<bpmn:documentation>${escapeXmlText(node.config.documentation)}</bpmn:documentation>`)

  switch (node.nodeType) {
    case NODE_TYPE.START: {
      const c = node.config || {}
      if (c.initiator)
        attrs.push(`flowable:initiator="${escapeXmlAttr(c.initiator)}"`)
      if (c.formKey)
        attrs.push(`flowable:formKey="${escapeXmlAttr(c.formKey)}"`)
      if (c.formJson)
        attrs.push(`flowable:formJson="${escapeXmlAttr(c.formJson)}"`)
      if (c.formUrl)
        attrs.push(`flowable:formUrl="${escapeXmlAttr(c.formUrl)}"`)
      break
    }
    case NODE_TYPE.END: {
      if (node.config?.endType === 'terminate')
        children.push('<bpmn:terminateEventDefinition/>')
      break
    }
    case NODE_TYPE.APPROVER: {
      const w = writeUserTaskConfig(node.config || {})
      if (w.attrs)
        attrs.push(w.attrs)
      if (w.children)
        children.push(w.children)
      break
    }
    case NODE_TYPE.SERVICE:
    case NODE_TYPE.CARBON_COPY: {
      const c = node.config || {}
      if (node.nodeType === NODE_TYPE.CARBON_COPY)
        attrs.push('flowable:type="cc"')
      if (c.implementationType && c.implementation) {
        const t = c.implementationType
        const v = escapeXmlAttr(c.implementation)
        if (t === 'class')
          attrs.push(`flowable:class="${v}"`)
        else if (t === 'expression')
          attrs.push(`flowable:expression="${v}"`)
        else if (t === 'delegateExpression')
          attrs.push(`flowable:delegateExpression="${v}"`)
      }
      if (c.async)
        attrs.push('flowable:async="true"')
      break
    }
    case NODE_TYPE.SCRIPT: {
      const c = node.config || {}
      if (c.scriptFormat)
        attrs.push(`scriptFormat="${escapeXmlAttr(c.scriptFormat)}"`)
      if (c.async)
        attrs.push('flowable:async="true"')
      if (c.script)
        children.push(`<bpmn:script>${escapeXmlText(c.script)}</bpmn:script>`)
      break
    }
    case NODE_TYPE.CALL_ACTIVITY: {
      if (node.config?.calledElement)
        attrs.push(`calledElement="${escapeXmlAttr(node.config.calledElement)}"`)
      break
    }
    case NODE_TYPE.CONDITION:
    case NODE_TYPE.PARALLEL:
    case NODE_TYPE.INCLUSIVE: {
      if (node.config?.defaultFlowId)
        attrs.push(`default="${escapeXmlAttr(node.config.defaultFlowId)}"`)
      break
    }
    default:
      break
  }

  if (children.length)
    return `<${tag} ${attrs.join(' ')}>${children.join('')}</${tag}>`
  return `<${tag} ${attrs.join(' ')}/>`
}

function writeEdge(edge) {
  const attrs = [
    `id="${escapeXmlAttr(edge.id)}"`,
    `sourceRef="${escapeXmlAttr(edge.source)}"`,
    `targetRef="${escapeXmlAttr(edge.target)}"`,
  ]
  if (edge.condition && !edge.isDefault) {
    return `<bpmn:sequenceFlow ${attrs.join(' ')}>`
      + `<bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${escapeXmlText(edge.condition)}</bpmn:conditionExpression>`
      + `</bpmn:sequenceFlow>`
  }
  return `<bpmn:sequenceFlow ${attrs.join(' ')}/>`
}

function writeDiagram(processId, flowJson, layout) {
  const shapes = []
  const edges = []
  for (const node of flowJson.nodes) {
    const pos = layout.nodePositions.get(node.id)
    if (!pos)
      continue
    shapes.push(
      `<bpmndi:BPMNShape id="${escapeXmlAttr(node.id)}_di" bpmnElement="${escapeXmlAttr(node.id)}">`
      + `<dc:Bounds x="${pos.x}" y="${pos.y}" width="${pos.width}" height="${pos.height}"/>`
      + `</bpmndi:BPMNShape>`,
    )
  }
  for (const edge of flowJson.edges) {
    const wp = layout.edgeWaypoints.get(edge.id)
    if (!wp || wp.length === 0)
      continue
    const wpXml = wp.map(p => `<di:waypoint x="${p.x}" y="${p.y}"/>`).join('')
    edges.push(
      `<bpmndi:BPMNEdge id="${escapeXmlAttr(edge.id)}_di" bpmnElement="${escapeXmlAttr(edge.id)}">${wpXml}</bpmndi:BPMNEdge>`,
    )
  }
  return [
    '<bpmndi:BPMNDiagram id="BPMNDiagram_1">',
    `    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="${escapeXmlAttr(processId)}">`,
    ...shapes.map(s => `      ${s}`),
    ...edges.map(e => `      ${e}`),
    '    </bpmndi:BPMNPlane>',
    '  </bpmndi:BPMNDiagram>',
  ].join('\n')
}

function buildEmptyDefinitions() {
  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    `<bpmn:definitions ${NS_DECLS} id="Definitions_1" targetNamespace="http://flowable.org/bpmn">`,
    '  <bpmn:process id="Process_1" isExecutable="true"/>',
    '  <bpmndi:BPMNDiagram id="BPMNDiagram_1">',
    '    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1"/>',
    '  </bpmndi:BPMNDiagram>',
    '</bpmn:definitions>',
  ].join('\n')
}
