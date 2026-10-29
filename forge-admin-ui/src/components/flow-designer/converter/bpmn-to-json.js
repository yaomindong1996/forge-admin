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
const DEFAULT_CC_DELEGATE_EXPRESSION = '$' + '{flowCcNodeDelegate}'

/**
 * 解析 BPMN XML 字符串为 flowJson。
 *
 * @param {string} xmlString
 * @returns {{ processId: string, processName: string, nodes: any[], edges: any[] }} 流程设计器 JSON。
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
    config: parseProcessConfig(proc),
    nodes,
    edges,
  }

  // Task 4：网关 branchId / default / 汇合节点识别（mutate edges + nodes.config）
  markBranches(flowJson)
  applyDiagramPositions(doc, flowJson)

  return flowJson
}

/**
 * 仅在自由布局模式下回填 uiPosition。
 * 普通自动布局的 Diagram Bounds 不能当成手工坐标，否则保存后再开会整图“散开”。
 */
function applyDiagramPositions(doc, flowJson) {
  if (!flowJson?.config?.freeLayout)
    return
  const shapes = collectDiagramShapes(doc)
  if (!shapes.size)
    return
  for (const node of flowJson.nodes) {
    const bounds = shapes.get(node.id) || shapes.get(node.bpmnElementId)
    if (!bounds)
      continue
    node.uiPosition = { x: bounds.x, y: bounds.y }
  }
}

function collectDiagramShapes(doc) {
  const map = new Map()
  const root = doc?.documentElement || doc
  if (!root)
    return map
  const stack = [root]
  while (stack.length) {
    const cur = stack.pop()
    const name = getLocalName(cur)
    if (name === 'BPMNShape') {
      const bpmnElement = getAttr(cur, 'bpmnElement')
      const bounds = getChild(cur, 'Bounds')
      if (bpmnElement && bounds) {
        const x = Number(getAttr(bounds, 'x'))
        const y = Number(getAttr(bounds, 'y'))
        if (Number.isFinite(x) && Number.isFinite(y))
          map.set(bpmnElement, { x, y })
      }
    }
    for (const child of getChildren(cur))
      stack.push(child)
  }
  return map
}

function parseProcessConfig(processElement) {
  return {
    allowSubmitterWithdraw: parseBooleanWithDefault(
      getFlowableAttr(processElement, 'allowSubmitterWithdraw'),
      true,
    ),
    autoApprovalMode: normalizeAutoApprovalMode(
      getFlowableAttr(processElement, 'autoApprovalMode'),
    ),
    rejectStrategy: normalizeRejectStrategyAttr(
      getFlowableAttr(processElement, 'rejectStrategy'),
    ),
    freeLayout: parseBooleanWithDefault(
      getFlowableAttr(processElement, 'freeLayout'),
      false,
    ),
  }
}

function normalizeRejectStrategyAttr(value) {
  const text = String(value || '').trim().toUpperCase()
  if (!text)
    return 'MANUAL'
  if (text === 'TO_END' || text === 'END' || text === 'TERMINATE')
    return 'TO_END'
  if (text === 'MANUAL' || text === 'NONE' || text === 'OFF')
    return 'MANUAL'
  if (text === 'TO_INITIATOR_MODIFY' || text === 'TO_START' || text === 'MODIFY')
    return 'TO_INITIATOR_MODIFY'
  return 'MANUAL'
}

function parseBooleanWithDefault(value, fallback) {
  if (value == null)
    return fallback
  const normalized = String(value).trim().toLowerCase()
  if (['true', '1', 'y', 'yes'].includes(normalized))
    return true
  if (['false', '0', 'n', 'no'].includes(normalized))
    return false
  return fallback
}

function normalizeAutoApprovalMode(value) {
  return ['firstOnly', 'consecutive', 'none'].includes(value) ? value : 'none'
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
      // 发起人变量是系统内置变量，固定为 initiator；表单配置走流程模型全局配置。
      config.initiator = 'initiator'
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
      if (nodeType === NODE_TYPE.CARBON_COPY) {
        config.flowableType = 'cc'
        config.candidateUsers = splitCsv(getFlowableAttr(element, 'candidateUsers'))
        config.candidateUserNames = splitCsv(getFlowableAttr(element, 'candidateUserNames'))
        config.candidateGroups = splitCsv(getFlowableAttr(element, 'candidateGroups'))
        config.candidateGroupNames = splitCsv(getFlowableAttr(element, 'candidateGroupNames'))
        config.ccReceiverType = getFlowableAttr(element, 'ccReceiverType') || inferCarbonCopyReceiverType(config)
        config.ccExpressionTarget = getFlowableAttr(element, 'ccExpressionTarget') || inferCarbonCopyExpressionTarget(config)
        config.ccExpression = inferCarbonCopyExpression(config)
        if (config.implementationType === 'delegateExpression' && config.implementation === DEFAULT_CC_DELEGATE_EXPRESSION) {
          config.implementation = ''
        }
      }
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

function splitCsv(value) {
  if (!value)
    return []
  return String(value).split(',').map(item => item.trim()).filter(Boolean)
}

function inferCarbonCopyReceiverType(config = {}) {
  if (inferCarbonCopyExpression(config))
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
  return values.find((value) => {
    const text = String(value || '').trim()
    return text.startsWith('${') && text.endsWith('}')
  }) || ''
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
