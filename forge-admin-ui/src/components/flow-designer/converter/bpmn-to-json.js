/**
 * BPMN XML → flowJson 转换器（基础节点骨架）
 *
 * 范围（本任务）：
 * - 解析 process 元素 + 所有 flowNode 子节点 + sequenceFlow
 * - 基础节点字段：name、documentation、async、implementation 等
 * - 未识别 BPMN 元素 → advanced 节点（rawXml 兜底）
 *
 * 后续任务补充：
 * - Task 3：UserTask 完整属性（assignee / multiInstance / listeners / 权限 / 表单）
 * - Task 4：网关分支（branchId / default / condition）与汇合点识别
 *
 * flowJson 结构：
 * {
 *   processId: 'Process_1',
 *   processName: '请假流程',
 *   nodes: [
 *     { id, nodeType, name, bpmnElementId, bpmnElementType, rawXml, config }
 *   ],
 *   edges: [
 *     { id, source, target, bpmnElementId, conditionType, condition, isDefault, branchId }
 *   ]
 * }
 */

import { bpmnTypeToNodeType, NODE_TYPE } from '../constants/node-types.js'
import { markBranches } from './branch-parser.js'
import { parseUserTaskConfig } from './user-task-parser.js'
import {
  getAttr,
  getChild,
  getChildren,
  getDocumentation,
  getFlowableAttr,
  getFlowableBoolAttr,
  getLocalName,
  getRootProcess,
  parseBpmnXml,
} from './xml-utils.js'

/**
 * 不参与节点解析的子标签（这些属于流程结构性元素而非节点本身）。
 */
const NON_NODE_TAGS = new Set([
  'sequenceFlow',
  'documentation',
  'extensionElements',
  'laneSet',
  'lane',
  'multiInstanceLoopCharacteristics',
])

/**
 * 解析 BPMN XML 字符串为 flowJson。
 *
 * @param {string} xmlString
 * @returns {{ processId: string, processName: string, nodes: any[], edges: any[] }}
 */
export function convertBpmnToJson(xmlString) {
  const doc = parseBpmnXml(xmlString)
  const proc = getRootProcess(doc)

  if (!proc) {
    return {
      processId: '',
      processName: '',
      nodes: [],
      edges: [],
    }
  }

  const nodes = []
  const edges = []

  for (const child of getChildren(proc)) {
    const localName = getLocalName(child)
    if (NON_NODE_TAGS.has(localName))
      continue
    nodes.push(parseNode(child))
  }

  for (const flow of getChildren(proc, 'sequenceFlow'))
    edges.push(parseEdge(flow))

  const flowJson = {
    processId: getAttr(proc, 'id') || '',
    processName: getAttr(proc, 'name') || '',
    nodes,
    edges,
  }

  // Task 4：网关 branchId / default / 汇合节点识别（mutate edges + nodes.config）
  markBranches(flowJson)

  return flowJson
}

/**
 * 解析单个 BPMN 节点元素 → flowNode。
 * UserTask / 网关详细属性在 Task 3 / Task 4 补齐，本任务只填 nodeType + 基础信息。
 */
export function parseNode(element) {
  const nodeType = bpmnTypeToNodeType(element)
  const id = getAttr(element, 'id') || ''
  const localName = getLocalName(element)
  const bpmnElementType = `bpmn:${capitalize(localName)}`

  const baseNode = {
    id,
    nodeType,
    name: getAttr(element, 'name') || '',
    bpmnElementId: id,
    bpmnElementType,
    rawXml: null,
    config: {},
  }

  // advanced 兜底：原样保留 outerHTML（serializeXml 单元素子树）
  if (nodeType === NODE_TYPE.ADVANCED)
    return buildAdvancedNode(element, baseNode)

  // 基础节点 config 提取
  baseNode.config = parseBaseConfig(element, nodeType)

  return baseNode
}

/**
 * 把元素序列化回 XML 字符串作为 advanced 节点的 rawXml 兜底。
 */
export function buildAdvancedNode(element, baseNode) {
  const rawXml = serializeElement(element)
  return {
    ...baseNode,
    rawXml,
    config: {
      // 仅暴露能编辑的字段：节点名 + documentation
      documentation: getDocumentation(element),
    },
  }
}

/**
 * 单元素 → 字符串。XMLSerializer.serializeToString 直接接受 Element。
 */
function serializeElement(element) {
  if (!element)
    return ''
  // jsdom 与浏览器都支持 outerHTML，但 outerHTML 在某些 XML 文档下可能丢前缀；
  // 用 XMLSerializer 更安全。
  try {
    return new XMLSerializer().serializeToString(element)
  }
  catch {
    return element.outerHTML || ''
  }
}

/**
 * 节点公共 config 提取（按 nodeType 分支）。
 * UserTask 详细属性留到 Task 3 的 parseUserTaskConfig。
 */
function parseBaseConfig(element, nodeType) {
  const config = {
    documentation: getDocumentation(element),
  }

  switch (nodeType) {
    case NODE_TYPE.START: {
      // 发起人节点：initiator + formKey 等基础信息（StartConfig 后续在 Task 24 完善）
      config.initiator = getFlowableAttr(element, 'initiator') || 'initiator'
      config.formKey = getFlowableAttr(element, 'formKey') || ''
      config.formJson = getFlowableAttr(element, 'formJson') || ''
      config.formUrl = getFlowableAttr(element, 'formUrl') || ''
      break
    }
    case NODE_TYPE.END: {
      // EndConfig：normal / terminate（terminate 通过子节点 terminateEventDefinition 识别）
      config.endType = getChild(element, 'terminateEventDefinition') ? 'terminate' : 'normal'
      break
    }
    case NODE_TYPE.SERVICE:
    case NODE_TYPE.CARBON_COPY: {
      const impl = parseImplementation(element)
      config.implementationType = impl.implementationType
      config.implementation = impl.implementation
      config.async = getFlowableBoolAttr(element, 'async')
      // carbonCopy 默认带 flowable:type='cc'
      if (nodeType === NODE_TYPE.CARBON_COPY)
        config.flowableType = 'cc'
      break
    }
    case NODE_TYPE.SCRIPT: {
      config.scriptFormat = getAttr(element, 'scriptFormat') || ''
      const scriptEl = getChild(element, 'script')
      config.script = scriptEl ? (scriptEl.textContent || '').trim() : ''
      config.async = getFlowableBoolAttr(element, 'async')
      break
    }
    case NODE_TYPE.SUB_PROCESS: {
      // 子流程内部结构暂不递归（Task 28 / 后续迭代），仅记录 triggeredByEvent 等基本属性
      config.triggeredByEvent = getAttr(element, 'triggeredByEvent') === 'true'
      break
    }
    case NODE_TYPE.CALL_ACTIVITY: {
      config.calledElement = getAttr(element, 'calledElement') || ''
      // Flowable 风格的输入/输出参数映射在 extensionElements 中（暂不展开，Task 24 完善）
      break
    }
    case NODE_TYPE.APPROVER: {
      // UserTask 完整属性提取（Task 3）
      const userTaskConfig = parseUserTaskConfig(element)
      Object.assign(config, userTaskConfig)
      // documentation 在 parseBaseConfig 顶部已写入；UserTask 也复用同一字段
      break
    }
    case NODE_TYPE.CONDITION:
    case NODE_TYPE.PARALLEL:
    case NODE_TYPE.INCLUSIVE: {
      // 网关 default 属性 → 在 Task 4 markBranches 时再标记到 edges
      config.defaultFlowId = getAttr(element, 'default') || ''
      break
    }
    default:
      break
  }

  return config
}

/**
 * 解析 ServiceTask 的实现方式：class / expression / delegateExpression。
 * 与 NodePropertiesPanel.vue:1733-1751 保持一致。
 */
function parseImplementation(element) {
  const classVal = getFlowableAttr(element, 'class')
  if (classVal)
    return { implementationType: 'class', implementation: classVal }

  const exprVal = getFlowableAttr(element, 'expression')
  if (exprVal)
    return { implementationType: 'expression', implementation: exprVal }

  const delegateVal = getFlowableAttr(element, 'delegateExpression')
  if (delegateVal)
    return { implementationType: 'delegateExpression', implementation: delegateVal }

  return { implementationType: 'class', implementation: '' }
}

/**
 * 解析 sequenceFlow → flowEdge 骨架。
 * branchId / default / condition 的精细化分组留到 Task 4 markBranches。
 */
export function parseEdge(flowElement) {
  const id = getAttr(flowElement, 'id') || ''
  const source = getAttr(flowElement, 'sourceRef') || ''
  const target = getAttr(flowElement, 'targetRef') || ''

  const condEl = getChild(flowElement, 'conditionExpression')
  let condition = ''
  let conditionType = null
  if (condEl) {
    condition = (condEl.textContent || '').trim()
    // language 属性表示脚本：language="javascript" / "groovy"
    const language = getAttr(condEl, 'language')
    conditionType = language ? 'script' : 'expression'
  }

  return {
    id,
    source,
    target,
    bpmnElementId: id,
    conditionType,
    condition,
    isDefault: false, // Task 4 在 markBranches 中按网关 default 属性回填
    branchId: null, // Task 4 在 markBranches 中分配
  }
}

/**
 * 工具：首字母大写（用于构造 bpmnElementType）。
 */
function capitalize(s) {
  if (!s)
    return ''
  return s.charAt(0).toUpperCase() + s.slice(1)
}
