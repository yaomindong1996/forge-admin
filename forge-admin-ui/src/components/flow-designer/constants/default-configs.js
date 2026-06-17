/**
 * 每种 nodeType 的默认 config
 *
 * 用途：
 * - useFlowDesigner.addNode(afterId, type) 时构造新节点
 * - 与 user-task-parser / json-to-bpmn 字段保持一致，避免新增节点缺字段
 *
 * 注意：返回的对象通过 cloneConfig 深拷贝，避免不同节点共享引用导致互相污染。
 */

export const DEFAULT_CONFIGS = Object.freeze({
  start: () => ({
    documentation: '',
    initiator: 'initiator',
    formKey: '',
    formJson: '',
    formUrl: '',
  }),
  end: () => ({
    documentation: '',
    endType: 'normal',
  }),
  approver: () => ({
    documentation: '',
    taskType: 'assignee',
    assignee: '',
    assigneeExpr: '',
    assigneeUserName: '',
    candidateUsers: [],
    candidateUserNames: [],
    candidateGroups: [],
    candidateGroupNames: [],
    spelTemplate: '',
    priority: 50,
    dueDate: 0,
    formType: 'none',
    formKey: '',
    formJson: '',
    formUrl: '',
    multiInstanceType: 'none',
    completionCondition: 'all',
    passRate: 100,
    taskListeners: [],
    executionListeners: [],
    formFieldPermissions: [],
    allowApprove: true,
    allowReject: true,
    allowDelegate: true,
    allowReturn: false,
    allowTerminate: false,
    requireSignature: false,
    requireComment: true,
  }),
  carbonCopy: () => ({
    documentation: '',
    flowableType: 'cc',
    implementationType: 'expression',
    implementation: '',
    candidateUsers: [],
    candidateUserNames: [],
    async: false,
  }),
  condition: () => ({
    documentation: '',
    defaultFlowId: '',
  }),
  parallel: () => ({
    documentation: '',
  }),
  inclusive: () => ({
    documentation: '',
    defaultFlowId: '',
  }),
  service: () => ({
    documentation: '',
    implementationType: 'class',
    implementation: '',
    async: false,
  }),
  script: () => ({
    documentation: '',
    scriptFormat: 'javascript',
    script: '',
    async: false,
  }),
  subProcess: () => ({
    documentation: '',
    triggeredByEvent: false,
  }),
  callActivity: () => ({
    documentation: '',
    calledElement: '',
  }),
  advanced: () => ({
    documentation: '',
  }),
})

/** 节点类型默认显示名（NDrawer 标题、卡片标题兜底） */
export const DEFAULT_NODE_NAMES = Object.freeze({
  start: '发起人',
  end: '结束',
  approver: '审批人',
  carbonCopy: '抄送人',
  condition: '条件分支',
  parallel: '并行分支',
  inclusive: '包容分支',
  service: '服务任务',
  script: '脚本任务',
  subProcess: '子流程',
  callActivity: '调用活动',
  advanced: '高级节点',
})

const BPMN_ELEMENT_TYPE_MAP = Object.freeze({
  start: 'bpmn:StartEvent',
  end: 'bpmn:EndEvent',
  approver: 'bpmn:UserTask',
  carbonCopy: 'bpmn:ServiceTask',
  service: 'bpmn:ServiceTask',
  script: 'bpmn:ScriptTask',
  condition: 'bpmn:ExclusiveGateway',
  parallel: 'bpmn:ParallelGateway',
  inclusive: 'bpmn:InclusiveGateway',
  subProcess: 'bpmn:SubProcess',
  callActivity: 'bpmn:CallActivity',
  advanced: 'bpmn:Activity',
})

/**
 * 构造一个新节点骨架（不含 id，由调用方分配）。
 *
 * @param {string} nodeType
 * @param {object} [overrides]
 * @returns {object} 新节点对象
 */
export function buildNode(nodeType, overrides = {}) {
  const factory = DEFAULT_CONFIGS[nodeType]
  const config = factory ? factory() : { documentation: '' }
  return {
    id: '',
    nodeType,
    name: DEFAULT_NODE_NAMES[nodeType] || '节点',
    bpmnElementId: '',
    bpmnElementType: BPMN_ELEMENT_TYPE_MAP[nodeType] || 'bpmn:Activity',
    rawXml: null,
    config,
    ...overrides,
  }
}
