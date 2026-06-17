/**
 * 钉钉样式流程设计器 - 节点类型枚举与 BPMN 元素类型映射
 *
 * 12 种节点类型对应 BPMN 元素：
 *   start         → bpmn:StartEvent
 *   end           → bpmn:EndEvent
 *   approver      → bpmn:UserTask
 *   carbonCopy    → bpmn:ServiceTask + flowable:type='cc'
 *   condition     → bpmn:ExclusiveGateway
 *   parallel      → bpmn:ParallelGateway
 *   inclusive     → bpmn:InclusiveGateway
 *   service       → bpmn:ServiceTask（非抄送）
 *   script        → bpmn:ScriptTask
 *   subProcess    → bpmn:SubProcess
 *   callActivity  → bpmn:CallActivity
 *   advanced      → 其他无法识别类型（IntermediateEvent / BoundaryEvent / ManualTask 等）
 */

export const NODE_TYPE = Object.freeze({
  START: 'start',
  END: 'end',
  APPROVER: 'approver',
  CARBON_COPY: 'carbonCopy',
  CONDITION: 'condition',
  PARALLEL: 'parallel',
  INCLUSIVE: 'inclusive',
  SERVICE: 'service',
  SCRIPT: 'script',
  SUB_PROCESS: 'subProcess',
  CALL_ACTIVITY: 'callActivity',
  ADVANCED: 'advanced',
})

/** 所有合法 nodeType 集合，便于校验。 */
export const NODE_TYPE_SET = new Set([
  NODE_TYPE.START,
  NODE_TYPE.END,
  NODE_TYPE.APPROVER,
  NODE_TYPE.CARBON_COPY,
  NODE_TYPE.CONDITION,
  NODE_TYPE.PARALLEL,
  NODE_TYPE.INCLUSIVE,
  NODE_TYPE.SERVICE,
  NODE_TYPE.SCRIPT,
  NODE_TYPE.SUB_PROCESS,
  NODE_TYPE.CALL_ACTIVITY,
  NODE_TYPE.ADVANCED,
])

/**
 * 获取元素的不带命名空间前缀的 localName。
 * - DOM Element 的 localName 在 XML 文档下不区分前缀，返回值为 'startEvent' 这类裸 tag。
 * - 兜底使用 nodeName.split(':') 兼容 HTML/无命名空间解析场景。
 */
function getLocalName(element) {
  if (!element)
    return ''
  if (element.localName)
    return element.localName
  const name = element.nodeName || ''
  const idx = name.indexOf(':')
  return idx >= 0 ? name.slice(idx + 1) : name
}

/**
 * BPMN 元素类型 → 钉钉 nodeType。
 *
 * @param {Element} element BPMN DOM 元素，例如 `<bpmn:userTask>`、`<bpmn:serviceTask>` 等
 * @returns {string} NODE_TYPE 之一，无法识别返回 'advanced'
 *
 * 识别规则（与 spec 10.3 节 + Task 1 任务卡一致）：
 *   - bpmn:StartEvent → start
 *   - bpmn:EndEvent   → end
 *   - bpmn:UserTask   → approver
 *   - bpmn:ServiceTask + flowable:type='cc' → carbonCopy（抄送）
 *   - bpmn:ServiceTask 其他 → service
 *   - bpmn:ScriptTask → script
 *   - bpmn:ExclusiveGateway → condition
 *   - bpmn:ParallelGateway  → parallel
 *   - bpmn:InclusiveGateway → inclusive
 *   - bpmn:SubProcess  → subProcess
 *   - bpmn:CallActivity → callActivity
 *   - 其他（IntermediateEvent / BoundaryEvent / ManualTask / BusinessRuleTask / ReceiveTask /
 *     ComplexGateway / EventBasedGateway / Transaction / AdHocSubProcess 等）→ advanced
 */
export function bpmnTypeToNodeType(element) {
  if (!element)
    return NODE_TYPE.ADVANCED

  const localName = getLocalName(element)

  switch (localName) {
    case 'startEvent':
      return NODE_TYPE.START
    case 'endEvent':
      return NODE_TYPE.END
    case 'userTask':
      return NODE_TYPE.APPROVER
    case 'serviceTask': {
      // 抄送节点用 flowable:type="cc" 区分（与现有 NodePropertiesPanel 约定一致）
      const flowableType = readFlowableType(element)
      if (flowableType === 'cc')
        return NODE_TYPE.CARBON_COPY
      return NODE_TYPE.SERVICE
    }
    case 'scriptTask':
      return NODE_TYPE.SCRIPT
    case 'exclusiveGateway':
      return NODE_TYPE.CONDITION
    case 'parallelGateway':
      return NODE_TYPE.PARALLEL
    case 'inclusiveGateway':
      return NODE_TYPE.INCLUSIVE
    case 'subProcess':
      return NODE_TYPE.SUB_PROCESS
    case 'callActivity':
      return NODE_TYPE.CALL_ACTIVITY
    default:
      return NODE_TYPE.ADVANCED
  }
}

/**
 * 读取 flowable:type 属性，兼容三种解析路径：
 * 1. 命名空间感知（XML mode）下 getAttributeNS('http://flowable.org/bpmn', 'type')
 * 2. 直接读取带前缀的 attribute 'flowable:type'
 * 3. fallback 读取无前缀 'type' 属性（极少数 XML 命名空间被默认化时出现）
 */
function readFlowableType(element) {
  const FLOWABLE_NS = 'http://flowable.org/bpmn'
  if (typeof element.getAttributeNS === 'function') {
    const v = element.getAttributeNS(FLOWABLE_NS, 'type')
    if (v != null && v !== '')
      return v
  }
  const prefixed = element.getAttribute?.('flowable:type')
  if (prefixed != null && prefixed !== '')
    return prefixed
  return null
}

/**
 * 钉钉 nodeType → BPMN local element name（用于 JSON→XML 写出）。
 * carbonCopy 与 service 都映射到 serviceTask，区别在于 flowable:type 属性，调用方负责追加。
 */
export const NODE_TYPE_TO_BPMN_LOCAL_NAME = Object.freeze({
  [NODE_TYPE.START]: 'startEvent',
  [NODE_TYPE.END]: 'endEvent',
  [NODE_TYPE.APPROVER]: 'userTask',
  [NODE_TYPE.CARBON_COPY]: 'serviceTask',
  [NODE_TYPE.SERVICE]: 'serviceTask',
  [NODE_TYPE.SCRIPT]: 'scriptTask',
  [NODE_TYPE.CONDITION]: 'exclusiveGateway',
  [NODE_TYPE.PARALLEL]: 'parallelGateway',
  [NODE_TYPE.INCLUSIVE]: 'inclusiveGateway',
  [NODE_TYPE.SUB_PROCESS]: 'subProcess',
  [NODE_TYPE.CALL_ACTIVITY]: 'callActivity',
  // advanced 不参与默认映射，由 rawXml 直接写回
})
