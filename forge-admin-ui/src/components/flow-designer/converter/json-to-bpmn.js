/**
 * JSON → BPMN XML 转换器（Task 5）
 *
 * 输入：flowJson；输出：BPMN 2.0 XML 字符串。
 *
 * pitfalls #8：BPMNPlane.bpmnElement 必须指向真实 process id；本实现总写入 root process id。
 */

import { NODE_TYPE, NODE_TYPE_TO_BPMN_LOCAL_NAME } from '../constants/node-types.js'
import { ensureRejectRoutes, normalizeRejectStrategy, REJECT_STRATEGY } from './ensure-reject-routes.js'
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

const DEFAULT_PROCESS_CONFIG = {
  allowSubmitterWithdraw: true,
  autoApprovalMode: 'none',
  // 未显式配置时不自动改写存量 BPMN；新流程由设计器写入 TO_INITIATOR_MODIFY。
  rejectStrategy: REJECT_STRATEGY.MANUAL,
}
const DEFAULT_CC_DELEGATE_EXPRESSION = '$' + '{flowCcNodeDelegate}'

export function convertJsonToBpmn(flowJson) {
  if (isBusinessProcessSchema(flowJson))
    throw new TypeError('businessProcessJson 不能转换为 BPMN；审批内部流程必须使用独立 flowJson')
  if (!flowJson || !Array.isArray(flowJson.nodes))
    return buildEmptyDefinitions()

  const processId = flowJson.processId || 'Process_1'
  const processName = flowJson.processName || ''
  const processConfig = normalizeProcessConfig(flowJson.config)
  // 按流程级驳回策略补齐缺失回路；不修改设计器内存图，只影响写出的 BPMN。
  const exportJson = ensureRejectRoutes(flowJson, processConfig.rejectStrategy)
  const layout = calculateLayout(exportJson)

  const nodeXml = exportJson.nodes.map(writeNode).filter(Boolean)
  const edgeXml = (exportJson.edges || []).map(writeEdge).filter(Boolean)
  const diagram = writeDiagram(processId, exportJson, layout)

  const procAttrs = [`id="${escapeXmlAttr(processId)}"`]
  if (processName)
    procAttrs.push(`name="${escapeXmlAttr(processName)}"`)
  procAttrs.push(`flowable:allowSubmitterWithdraw="${processConfig.allowSubmitterWithdraw}"`)
  procAttrs.push(`flowable:autoApprovalMode="${escapeXmlAttr(processConfig.autoApprovalMode)}"`)
  procAttrs.push(`flowable:rejectStrategy="${escapeXmlAttr(processConfig.rejectStrategy)}"`)
  if (processConfig.freeLayout || (flowJson.nodes || []).some(n => Number.isFinite(n?.uiPosition?.x)))
    procAttrs.push('flowable:freeLayout="true"')
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

function isBusinessProcessSchema(value) {
  return value?.schemaVersion === '1.0'
    && typeof value?.processCode === 'string'
    && value?.subject != null
    && Array.isArray(value?.nodes)
}

function normalizeProcessConfig(config = {}) {
  const autoApprovalMode = ['firstOnly', 'consecutive', 'none'].includes(config.autoApprovalMode)
    ? config.autoApprovalMode
    : DEFAULT_PROCESS_CONFIG.autoApprovalMode
  return {
    allowSubmitterWithdraw: config.allowSubmitterWithdraw !== false,
    autoApprovalMode,
    rejectStrategy: normalizeRejectStrategy(config.rejectStrategy),
    freeLayout: config.freeLayout === true,
  }
}

export { normalizeRejectStrategy, REJECT_STRATEGY }

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
      attrs.push('flowable:initiator="initiator"')
      break
    }
    case NODE_TYPE.END: {
      if (node.config?.endType === 'terminate')
        children.push('<bpmn:terminateEventDefinition/>')
      break
    }
    case NODE_TYPE.APPROVER: {
      const w = writeUserTaskConfig({ ...(node.config || {}), bpmnElementId: node.id })
      if (w.attrs)
        attrs.push(w.attrs)
      if (w.children)
        children.push(w.children)
      break
    }
    case NODE_TYPE.SERVICE:
    case NODE_TYPE.CARBON_COPY: {
      const c = node.config || {}
      if (node.nodeType === NODE_TYPE.CARBON_COPY) {
        const ccValues = normalizeCarbonCopyValues(c)
        attrs.push('flowable:type="cc"')
        attrs.push(`flowable:ccReceiverType="${escapeXmlAttr(ccValues.ccReceiverType)}"`)
        attrs.push(`flowable:ccExpressionTarget="${escapeXmlAttr(ccValues.ccExpressionTarget)}"`)
        if (ccValues.candidateUsers.length)
          attrs.push(`flowable:candidateUsers="${escapeXmlAttr(ccValues.candidateUsers.join(','))}"`)
        if (Array.isArray(c.candidateUserNames) && c.candidateUserNames.length)
          attrs.push(`flowable:candidateUserNames="${escapeXmlAttr(c.candidateUserNames.join(','))}"`)
        if (ccValues.candidateGroups.length)
          attrs.push(`flowable:candidateGroups="${escapeXmlAttr(ccValues.candidateGroups.join(','))}"`)
        if (Array.isArray(c.candidateGroupNames) && c.candidateGroupNames.length)
          attrs.push(`flowable:candidateGroupNames="${escapeXmlAttr(c.candidateGroupNames.join(','))}"`)
      }
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
      else if (node.nodeType === NODE_TYPE.CARBON_COPY) {
        attrs.push(`flowable:delegateExpression="${escapeXmlAttr(DEFAULT_CC_DELEGATE_EXPRESSION)}"`)
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

function normalizeCarbonCopyValues(config = {}) {
  const ccReceiverType = config.ccReceiverType || inferCarbonCopyReceiverType(config)
  const ccExpressionTarget = config.ccExpressionTarget || inferCarbonCopyExpressionTarget(config)
  const expression = normalizeExpression(config.ccExpression || inferCarbonCopyExpression(config))
  if (ccReceiverType === 'expression' && expression) {
    return {
      ccReceiverType,
      ccExpressionTarget,
      candidateUsers: ccExpressionTarget === 'roles' ? [] : [expression],
      candidateGroups: ccExpressionTarget === 'roles' ? [expression] : [],
    }
  }
  return {
    ccReceiverType,
    ccExpressionTarget,
    candidateUsers: Array.isArray(config.candidateUsers) ? config.candidateUsers : [],
    candidateGroups: Array.isArray(config.candidateGroups) ? config.candidateGroups : [],
  }
}

function inferCarbonCopyReceiverType(config = {}) {
  if (config.ccExpression || findExpressionValue(config.candidateUsers) || findExpressionValue(config.candidateGroups))
    return 'expression'
  if (Array.isArray(config.candidateGroups) && config.candidateGroups.length)
    return 'roles'
  return 'users'
}

function inferCarbonCopyExpressionTarget(config = {}) {
  return findExpressionValue(config.candidateGroups) ? 'roles' : 'users'
}

function inferCarbonCopyExpression(config = {}) {
  return findExpressionValue(config.candidateUsers) || findExpressionValue(config.candidateGroups) || ''
}

function findExpressionValue(values) {
  if (!Array.isArray(values))
    return ''
  return values.find(value => isExpressionValue(value)) || ''
}

function isExpressionValue(value) {
  const text = String(value || '').trim()
  return text.startsWith('${') && text.endsWith('}')
}

function normalizeExpression(value) {
  const text = String(value || '').trim()
  if (!text)
    return ''
  if (isExpressionValue(text))
    return text
  return '$' + `{${text}}`
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
