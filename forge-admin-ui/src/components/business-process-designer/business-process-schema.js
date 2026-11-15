import {
  BUSINESS_PROCESS_NODE_TYPE,
  BUSINESS_PROCESS_START_TYPES,
  createBusinessProcessNodeTemplate,
  getBusinessProcessNodeDefinition,
  isBusinessProcessStartType,
} from './business-process-node-types.js'

export const BUSINESS_PROCESS_SCHEMA_VERSION = '1.0'

const ROOT_KEYS = new Set([
  'schemaVersion',
  'processCode',
  'subject',
  'nodes',
  'edges',
  'policies',
  'dependencies',
  'metadata',
])
const SUBJECT_KEYS = new Set(['objectId', 'objectCode', 'objectVersionId', 'recordIdSource'])
const NODE_KEYS = new Set(['id', 'type', 'name', 'ports', 'config'])
const EDGE_KEYS = new Set(['id', 'source', 'target', 'sourcePort', 'condition', 'isDefault'])
const POLICY_KEYS = new Set(['approvalConcurrency', 'maxSubProcessDepth', 'retry'])
const RETRY_KEYS = new Set(['mode', 'maxAttempts', 'backoffSeconds'])
const DEPENDENCY_KEYS = Object.freeze([
  'objects',
  'flowModels',
  'formAssets',
  'businessActions',
  'messageTemplates',
  'capabilities',
  'subProcesses',
])
const DEPENDENCY_KEY_SET = new Set(DEPENDENCY_KEYS)

const RECORD_ID_SOURCE = Object.freeze({
  [BUSINESS_PROCESS_NODE_TYPE.START_MANUAL]: 'RUNTIME_RECORD',
  [BUSINESS_PROCESS_NODE_TYPE.START_EVENT]: 'EVENT_RECORD',
  [BUSINESS_PROCESS_NODE_TYPE.START_SCHEDULE]: 'SCHEDULE_SCAN_RECORD',
})

const DEFAULT_START_ID = Object.freeze({
  [BUSINESS_PROCESS_NODE_TYPE.START_MANUAL]: 'start_manual',
  [BUSINESS_PROCESS_NODE_TYPE.START_EVENT]: 'start_event',
  [BUSINESS_PROCESS_NODE_TYPE.START_SCHEDULE]: 'start_schedule',
})
const APPROVAL_PORT_ORDER = Object.freeze(['APPROVED', 'REJECTED', 'CANCELED', 'FAILED'])

export function createBusinessProcessSchema({
  processCode,
  objectRef,
  startType = BUSINESS_PROCESS_NODE_TYPE.START_MANUAL,
} = {}) {
  const normalizedStartType = normalizeUpperString(startType)
  if (!BUSINESS_PROCESS_START_TYPES.includes(normalizedStartType))
    throw new Error('业务流程必须使用手动、事件或定时开始节点')

  if (!isPlainObject(objectRef))
    throw new Error('业务流程必须绑定主业务对象')
  assertStringIds(objectRef)

  const objectId = normalizeRequiredString(objectRef.objectId, '主业务对象 ID')
  const objectCode = normalizeRequiredString(objectRef.objectCode, '主业务对象编码')
  const start = createBusinessProcessNodeTemplate(normalizedStartType)
  const end = createBusinessProcessNodeTemplate(BUSINESS_PROCESS_NODE_TYPE.END)
  const startId = DEFAULT_START_ID[normalizedStartType]

  return {
    schemaVersion: BUSINESS_PROCESS_SCHEMA_VERSION,
    processCode: normalizeRequiredString(processCode, '业务流程编码'),
    subject: {
      objectId,
      objectCode,
      objectVersionId: objectRef.objectVersionId ?? null,
      recordIdSource: RECORD_ID_SOURCE[normalizedStartType],
    },
    nodes: [
      { id: startId, ...start },
      { id: 'end_success', ...end, name: '完成' },
    ],
    edges: [
      {
        id: 'edge_start_end',
        source: startId,
        target: 'end_success',
        sourcePort: 'NEXT',
        condition: {},
        isDefault: null,
      },
    ],
    policies: {
      approvalConcurrency: 'ONE_ACTIVE_PER_BUSINESS_KEY',
      maxSubProcessDepth: 5,
      retry: {
        mode: 'LIMITED',
        maxAttempts: 3,
        backoffSeconds: [30, 120, 600],
      },
    },
    dependencies: {
      objects: [objectCode],
      flowModels: [],
      formAssets: [],
      businessActions: [],
      messageTemplates: [],
      capabilities: [],
      subProcesses: [],
    },
    metadata: {},
  }
}

export function normalizeBusinessProcessSchema(input) {
  if (!isPlainObject(input))
    throw new Error('businessProcessJson 必须是 JSON 对象')
  if (looksLikeBpmnFlowJson(input))
    throw new Error('BPMN flowJson 不能作为 businessProcessJson 使用')

  assertOnlyKeys(input, ROOT_KEYS, 'businessProcessJson')
  assertStringIds(input)

  const subject = normalizeSubject(input.subject)
  const nodes = normalizeArray(input.nodes, 'nodes')
    .map((node, index) => normalizeNode(node, index))
    .sort(compareById)
  const edges = normalizeArray(input.edges, 'edges')
    .map((edge, index) => normalizeEdge(edge, index))
    .sort(compareById)

  return {
    schemaVersion: normalizeOptionalString(input.schemaVersion),
    processCode: normalizeOptionalString(input.processCode),
    subject,
    nodes,
    edges,
    policies: normalizePolicies(input.policies),
    dependencies: normalizeDependencies(input.dependencies),
    metadata: normalizeJsonObject(input.metadata, 'metadata'),
  }
}

export function cloneBusinessProcessSchema(input) {
  return deepClone(normalizeBusinessProcessSchema(input))
}

export function synchronizeBusinessProcessDependencies(input) {
  const schema = normalizeBusinessProcessSchema(input)
  const dependencies = Object.fromEntries(DEPENDENCY_KEYS.map(key => [key, new Set()]))
  if (schema.subject?.objectCode)
    dependencies.objects.add(schema.subject.objectCode)

  for (const node of schema.nodes) {
    const config = node.config || {}
    if (node.type === BUSINESS_PROCESS_NODE_TYPE.ACTION) {
      if (['UPDATE_RECORD', 'CREATE_RECORD'].includes(config.actionType) && config.objectCode)
        dependencies.objects.add(config.objectCode)
      if (['BUSINESS_ACTION', 'EXECUTE_BUSINESS_ACTION', 'DOMAIN_ACTION'].includes(config.actionType)) {
        const actionCode = config.businessActionCode || config.actionCode
        if (actionCode)
          dependencies.businessActions.add(actionCode)
      }
      if (config.actionType === 'SEND_MESSAGE' && config.messageTemplateCode)
        dependencies.messageTemplates.add(config.messageTemplateCode)
      if (config.actionType === 'INVOKE_CAPABILITY' && config.capabilityCode)
        dependencies.capabilities.add(config.capabilityCode)
    }
    if (node.type === BUSINESS_PROCESS_NODE_TYPE.APPROVAL) {
      if (config.flowModelKey)
        dependencies.flowModels.add(config.flowModelKey)
      if (config.formAsset?.formKey)
        dependencies.formAssets.add(config.formAsset.formKey)
    }
    if (node.type === BUSINESS_PROCESS_NODE_TYPE.SUB_PROCESS) {
      const processCode = config.processCode || config.subProcessCode
      if (processCode)
        dependencies.subProcesses.add(processCode)
    }
  }

  schema.dependencies = Object.fromEntries(DEPENDENCY_KEYS.map(key => [
    key,
    [...dependencies[key]],
  ]))
  return normalizeBusinessProcessSchema(schema)
}

export function businessProcessHashInput(input) {
  return stableStringify(normalizeBusinessProcessSchema(input))
}

export function validateBusinessProcessGraph(input) {
  let schema
  try {
    schema = normalizeBusinessProcessSchema(input)
  }
  catch (error) {
    return validationResult([
      issue('SCHEMA_NORMALIZATION_FAILED', error.message, null, '$'),
    ])
  }

  const issues = []
  const nodesById = new Map()
  const startNodes = []
  const endNodes = []

  for (const node of schema.nodes) {
    if (!node.id) {
      issues.push(issue('NODE_ID_REQUIRED', '节点 ID 不能为空', null, 'nodes'))
      continue
    }
    if (nodesById.has(node.id))
      issues.push(issue('NODE_ID_DUPLICATE', '节点 ID 重复', node.id, 'nodes'))
    else
      nodesById.set(node.id, node)

    if (!getBusinessProcessNodeDefinition(node.type))
      issues.push(issue('NODE_TYPE_UNKNOWN', '节点类型不在业务流程注册表中', node.id, 'nodes'))
    if (node.type === BUSINESS_PROCESS_NODE_TYPE.CONDITION)
      validateConditionNode(node, issues)
    if (node.type === BUSINESS_PROCESS_NODE_TYPE.ACTION)
      validateActionNode(node, issues)
    if (node.type === BUSINESS_PROCESS_NODE_TYPE.APPROVAL) {
      const formMode = String(node.config?.formAsset?.formMode || node.config?.formAsset?.type || '').toUpperCase()
      const statusField = String(node.config?.statusField || '')
      if (formMode === 'BUSINESS_OBJECT_FORM' && !['flowStatus', 'flow_status'].includes(statusField)) {
        // 降级为 WARNING：flowStatus 会在保存时由前后端自动补齐，不再阻塞编辑
        const flowStatusWarning = issue(
          'APPROVAL_FLOW_STATUS_REQUIRED',
          '低代码审批需要绑定独立流程状态字段 flowStatus（保存时将自动创建）',
          node.id,
          `nodes.${node.id}.config.statusField`,
        )
        flowStatusWarning.level = 'WARNING'
        issues.push(flowStatusWarning)
      }
    }
    if (isBusinessProcessStartType(node.type))
      startNodes.push(node)
    if (node.type === BUSINESS_PROCESS_NODE_TYPE.END)
      endNodes.push(node)
  }

  if (startNodes.length !== 1) {
    issues.push(issue(
      'START_NODE_COUNT',
      '每个业务流程必须且只能有一个开始节点',
      null,
      'nodes',
    ))
  }
  if (endNodes.length === 0)
    issues.push(issue('END_NODE_REQUIRED', '业务流程至少需要一个结束节点', null, 'nodes'))

  const edgesBySource = new Map()
  const edgesByTarget = new Map()
  const validEdges = []
  const edgeIds = new Set()
  const sourcePorts = new Set()

  for (const edge of schema.edges) {
    if (!edge.id) {
      issues.push(issue('EDGE_ID_REQUIRED', '连线 ID 不能为空', null, 'edges'))
    }
    else if (edgeIds.has(edge.id)) {
      issues.push(issue('EDGE_ID_DUPLICATE', '连线 ID 重复', null, `edges.${edge.id}`))
    }
    else {
      edgeIds.add(edge.id)
    }

    const source = nodesById.get(edge.source)
    const target = nodesById.get(edge.target)
    if (!source)
      issues.push(issue('EDGE_SOURCE_MISSING', '连线来源节点不存在', null, `edges.${edge.id}.source`))
    if (!target)
      issues.push(issue('EDGE_TARGET_MISSING', '连线目标节点不存在', edge.source, `edges.${edge.id}.target`))
    if (source && target && source.id === target.id)
      issues.push(issue('EDGE_SELF_LOOP', '节点不能连接到自身', source.id, `edges.${edge.id}`))

    if (source) {
      const validPorts = allowedPorts(source)
      if (!validPorts.includes(edge.sourcePort)) {
        issues.push(issue(
          'EDGE_PORT_INVALID',
          '连线出口不属于来源节点注册表',
          source.id,
          `edges.${edge.id}.sourcePort`,
        ))
      }
      const sourcePortKey = `${source.id}\u0000${edge.sourcePort}`
      if (sourcePorts.has(sourcePortKey)) {
        issues.push(issue(
          'EDGE_PORT_DUPLICATE',
          '同一节点出口只能连接一个后继节点',
          source.id,
          `edges.${edge.id}.sourcePort`,
        ))
      }
      sourcePorts.add(sourcePortKey)
    }

    if (source && target) {
      validEdges.push(edge)
      appendMapArray(edgesBySource, source.id, edge)
      appendMapArray(edgesByTarget, target.id, edge)
    }
  }

  for (const node of nodesById.values()) {
    const incoming = edgesByTarget.get(node.id) || []
    const outgoing = edgesBySource.get(node.id) || []
    if (isBusinessProcessStartType(node.type) && incoming.length > 0)
      issues.push(issue('START_NODE_HAS_INCOMING', '开始节点不能有入边', node.id, `nodes.${node.id}`))
    if (node.type === BUSINESS_PROCESS_NODE_TYPE.END && outgoing.length > 0)
      issues.push(issue('END_NODE_HAS_OUTGOING', '结束节点不能有出边', node.id, `nodes.${node.id}`))
    if (node.type !== BUSINESS_PROCESS_NODE_TYPE.END && outgoing.length === 0)
      issues.push(issue('NODE_SUCCESSOR_REQUIRED', '非结束节点必须连接后继节点', node.id, `nodes.${node.id}`))
    if (!isBusinessProcessStartType(node.type) && incoming.length === 0)
      issues.push(issue('NODE_PREDECESSOR_REQUIRED', '非开始节点必须有入边', node.id, `nodes.${node.id}`))
    if (node.type === BUSINESS_PROCESS_NODE_TYPE.CONDITION) {
      const outgoingPorts = new Set(outgoing.map(edge => edge.sourcePort))
      const declaredPorts = new Set(node.ports || [])
      if (outgoingPorts.size !== declaredPorts.size
        || [...declaredPorts].some(port => !outgoingPorts.has(port))) {
        issues.push(issue(
          'CONDITION_EDGE_MISMATCH',
          '条件分支与画布连线不一致，请重新应用节点配置',
          node.id,
          `nodes.${node.id}.ports`,
        ))
      }
    }
  }

  if (containsCycle(nodesById, validEdges))
    issues.push(issue('GRAPH_CYCLE', '业务画布必须是有向无环图', null, 'edges'))

  if (startNodes.length === 1) {
    const reachable = collectReachable(startNodes[0].id, edgesBySource, edge => edge.target)
    for (const node of nodesById.values()) {
      if (!reachable.has(node.id))
        issues.push(issue('NODE_UNREACHABLE', '节点无法从开始节点到达', node.id, `nodes.${node.id}`))
    }
  }

  if (endNodes.length > 0) {
    const canReachEnd = new Set()
    for (const end of endNodes) {
      const reachable = collectReachable(end.id, edgesByTarget, edge => edge.source)
      reachable.forEach(id => canReachEnd.add(id))
    }
    for (const node of nodesById.values()) {
      if (!canReachEnd.has(node.id))
        issues.push(issue('NODE_WITHOUT_END_PATH', '节点没有通向结束节点的路径', node.id, `nodes.${node.id}`))
    }
  }

  return validationResult(issues)
}

function normalizeSubject(input) {
  if (!isPlainObject(input))
    return null
  assertOnlyKeys(input, SUBJECT_KEYS, 'subject')
  return {
    objectId: normalizeOptionalString(input.objectId),
    objectCode: normalizeOptionalString(input.objectCode),
    objectVersionId: input.objectVersionId == null
      ? null
      : normalizeOptionalString(input.objectVersionId),
    recordIdSource: normalizeUpperString(input.recordIdSource),
  }
}

function normalizeNode(input, index) {
  if (!isPlainObject(input))
    throw new Error(`nodes[${index}] 必须是 JSON 对象`)
  assertOnlyKeys(input, NODE_KEYS, `nodes[${index}]`)
  const type = normalizeUpperString(input.type)
  const config = normalizeJsonObject(input.config, `nodes[${index}].config`)
  const ports = normalizeStringList(input.ports, `nodes[${index}].ports`, true, true)
  return {
    id: normalizeOptionalString(input.id),
    type,
    name: normalizeOptionalString(input.name),
    // 旧草稿曾按字母排序端口；审批使用注册表顺序，条件使用用户配置的分支顺序迁移。
    ports: normalizeNodePorts(type, ports, config),
    config,
  }
}

function normalizeNodePorts(type, ports, config) {
  if (type === BUSINESS_PROCESS_NODE_TYPE.APPROVAL
    && sameStringSet(ports, APPROVAL_PORT_ORDER)) {
    return [...APPROVAL_PORT_ORDER]
  }
  if (type !== BUSINESS_PROCESS_NODE_TYPE.CONDITION)
    return ports

  const branchPorts = []
  for (const branch of Array.isArray(config?.branches) ? config.branches : []) {
    if (typeof branch?.port !== 'string')
      continue
    const port = branch.port.trim().toUpperCase()
    if (port && !branchPorts.includes(port))
      branchPorts.push(port)
  }
  return branchPorts.length && sameStringSet(branchPorts, ports) ? branchPorts : ports
}

function sameStringSet(left, right) {
  return left.length === right.length && left.every(value => right.includes(value))
}

function normalizeEdge(input, index) {
  if (!isPlainObject(input))
    throw new Error(`edges[${index}] 必须是 JSON 对象`)
  assertOnlyKeys(input, EDGE_KEYS, `edges[${index}]`)
  if (input.isDefault != null && typeof input.isDefault !== 'boolean')
    throw new Error(`edges[${index}].isDefault 必须是布尔值`)
  return {
    id: normalizeOptionalString(input.id),
    source: normalizeOptionalString(input.source),
    target: normalizeOptionalString(input.target),
    sourcePort: normalizeUpperString(input.sourcePort),
    condition: normalizeJsonObject(input.condition, `edges[${index}].condition`),
    isDefault: input.isDefault == null ? null : input.isDefault,
  }
}

function normalizePolicies(input) {
  if (!isPlainObject(input))
    return null
  assertOnlyKeys(input, POLICY_KEYS, 'policies')
  const retry = input.retry
  if (retry != null && !isPlainObject(retry))
    throw new Error('policies.retry 必须是 JSON 对象')
  if (retry)
    assertOnlyKeys(retry, RETRY_KEYS, 'policies.retry')

  return {
    approvalConcurrency: normalizeUpperString(input.approvalConcurrency),
    maxSubProcessDepth: normalizeOptionalInteger(input.maxSubProcessDepth, 'policies.maxSubProcessDepth'),
    retry: retry
      ? {
          mode: normalizeUpperString(retry.mode),
          maxAttempts: normalizeOptionalInteger(retry.maxAttempts, 'policies.retry.maxAttempts'),
          backoffSeconds: normalizeIntegerList(retry.backoffSeconds, 'policies.retry.backoffSeconds'),
        }
      : null,
  }
}

function normalizeDependencies(input) {
  const source = isPlainObject(input) ? input : {}
  assertOnlyKeys(source, DEPENDENCY_KEY_SET, 'dependencies')
  return Object.fromEntries(DEPENDENCY_KEYS.map(key => [
    key,
    normalizeStringList(source[key], `dependencies.${key}`),
  ]))
}

function normalizeJsonObject(input, path) {
  if (input == null)
    return {}
  if (!isPlainObject(input))
    throw new Error(`${path} 必须是 JSON 对象`)
  return deepClone(input)
}

function normalizeArray(input, path) {
  if (input == null)
    return []
  if (!Array.isArray(input))
    throw new Error(`${path} 必须是数组`)
  return input
}

function normalizeStringList(input, path, uppercase = false, preserveOrder = false) {
  const values = normalizeArray(input, path)
  const result = new Set()
  for (const value of values) {
    if (typeof value !== 'string')
      throw new Error(`${path} 只能包含字符串`)
    const normalized = uppercase ? normalizeUpperString(value) : normalizeOptionalString(value)
    if (normalized)
      result.add(normalized)
  }
  return preserveOrder ? [...result] : [...result].sort(compareStrings)
}

function normalizeIntegerList(input, path) {
  const values = normalizeArray(input, path)
  return values.map((value, index) => normalizeRequiredInteger(value, `${path}[${index}]`))
}

function normalizeOptionalInteger(value, path) {
  if (value == null)
    return null
  return normalizeRequiredInteger(value, path)
}

function normalizeRequiredInteger(value, path) {
  if (!Number.isInteger(value))
    throw new Error(`${path} 必须是整数`)
  return value
}

function normalizeRequiredString(value, label) {
  const normalized = normalizeOptionalString(value)
  if (!normalized)
    throw new Error(`${label}不能为空`)
  return normalized
}

function normalizeOptionalString(value) {
  if (value == null)
    return null
  if (typeof value !== 'string')
    throw new Error('businessProcessJson 字符串字段不能隐式转换类型')
  return value.trim() || null
}

function normalizeUpperString(value) {
  const normalized = normalizeOptionalString(value)
  return normalized ? normalized.toUpperCase() : null
}

function looksLikeBpmnFlowJson(input) {
  return Object.hasOwn(input, 'processId')
    || (Array.isArray(input.nodes) && input.nodes.some(node =>
      isPlainObject(node)
      && (Object.hasOwn(node, 'nodeType') || Object.hasOwn(node, 'bpmnElementId')),
    ))
}

function assertStringIds(value) {
  if (Array.isArray(value)) {
    value.forEach(assertStringIds)
    return
  }
  if (!isPlainObject(value))
    return
  for (const [key, child] of Object.entries(value)) {
    if (isIdKey(key) && typeof child === 'number')
      throw new Error('businessProcessJson 中的 ID 必须使用字符串')
    assertStringIds(child)
  }
}

function isIdKey(key) {
  return key === 'id'
    || key.endsWith('Id')
    || key.endsWith('Ids')
    || key.endsWith('_id')
    || key.endsWith('_ids')
}

function assertOnlyKeys(input, allowed, path) {
  for (const key of Object.keys(input)) {
    if (!allowed.has(key))
      throw new Error(`${path} 包含不支持的字段：${key}`)
  }
}

function allowedPorts(node) {
  const item = getBusinessProcessNodeDefinition(node.type)
  if (!item)
    return []
  if (node.type === BUSINESS_PROCESS_NODE_TYPE.CONDITION)
    return node.ports
  return item.ports
}

function validateActionNode(node, issues) {
  const actionType = String(node.config?.actionType || '').toUpperCase()
  if (!['UPDATE_RECORD', 'CREATE_RECORD'].includes(actionType))
    return
  const mappings = Array.isArray(node.config?.fieldMappings) ? node.config.fieldMappings : []
  const hasField = mappings.some(item => String(item?.field || '').trim())
  if (!hasField) {
    issues.push(issue(
      'ACTION_FIELD_MAPPING_REQUIRED',
      '更新/创建记录动作至少配置一个有效字段赋值',
      node.id,
      `nodes.${node.id}.config.fieldMappings`,
    ))
  }
}

function validateConditionNode(node, issues) {
  const branches = Array.isArray(node.config?.branches) ? node.config.branches : []
  if (branches.length < 2) {
    issues.push(issue(
      'CONDITION_BRANCH_COUNT_INVALID',
      '条件节点至少需要一个判断分支和一个默认分支',
      node.id,
      `nodes.${node.id}.config.branches`,
    ))
    return
  }

  const ports = branches.map(branch => branch?.port).filter(Boolean)
  if (ports.length !== branches.length || new Set(ports).size !== ports.length) {
    issues.push(issue(
      'CONDITION_PORT_INVALID',
      '条件分支存在重复或无效出口',
      node.id,
      `nodes.${node.id}.config.branches`,
    ))
  }
  const defaults = branches.filter(branch => branch?.isDefault === true)
  if (defaults.length !== 1) {
    issues.push(issue(
      'CONDITION_DEFAULT_INVALID',
      '条件节点必须保留一个默认分支',
      node.id,
      `nodes.${node.id}.config.branches`,
    ))
  }
  if (new Set(node.ports || []).size !== ports.length
    || ports.some(port => !(node.ports || []).includes(port))) {
    issues.push(issue(
      'CONDITION_PORTS_MISMATCH',
      '条件分支出口与节点配置不一致',
      node.id,
      `nodes.${node.id}.ports`,
    ))
  }
}

function containsCycle(nodesById, edges) {
  const indegree = new Map([...nodesById.keys()].map(id => [id, 0]))
  const outgoing = new Map()
  for (const edge of edges) {
    indegree.set(edge.target, (indegree.get(edge.target) || 0) + 1)
    appendMapArray(outgoing, edge.source, edge.target)
  }
  const queue = [...indegree.entries()]
    .filter(([, count]) => count === 0)
    .map(([id]) => id)
    .sort()
  let visited = 0
  while (queue.length) {
    const id = queue.shift()
    visited += 1
    for (const target of outgoing.get(id) || []) {
      const next = indegree.get(target) - 1
      indegree.set(target, next)
      if (next === 0) {
        queue.push(target)
        queue.sort()
      }
    }
  }
  return visited !== nodesById.size
}

function collectReachable(startId, edgeMap, nextId) {
  const reachable = new Set()
  const queue = [startId]
  while (queue.length) {
    const id = queue.shift()
    if (!id || reachable.has(id))
      continue
    reachable.add(id)
    for (const edge of edgeMap.get(id) || [])
      queue.push(nextId(edge))
  }
  return reachable
}

function appendMapArray(map, key, value) {
  if (!map.has(key))
    map.set(key, [])
  map.get(key).push(value)
}

function issue(code, message, nodeId, path) {
  return {
    level: 'ERROR',
    code,
    message,
    nodeId,
    path,
  }
}

function validationResult(issues) {
  return {
    isValid: issues.length === 0,
    issues,
  }
}

function compareById(left, right) {
  return compareStrings(left.id || '\uFFFF', right.id || '\uFFFF')
}

function compareStrings(left, right) {
  if (left === right)
    return 0
  return left < right ? -1 : 1
}

function stableStringify(value) {
  return JSON.stringify(sortObjectKeys(value))
}

function sortObjectKeys(value) {
  if (Array.isArray(value))
    return value.map(sortObjectKeys)
  if (!isPlainObject(value))
    return value
  return Object.fromEntries(
    Object.keys(value)
      .sort()
      .map(key => [key, sortObjectKeys(value[key])]),
  )
}

function deepClone(value) {
  return value == null ? value : JSON.parse(JSON.stringify(value))
}

function isPlainObject(value) {
  return value != null && typeof value === 'object' && !Array.isArray(value)
}
