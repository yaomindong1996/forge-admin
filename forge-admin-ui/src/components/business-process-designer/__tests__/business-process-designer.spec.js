import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { convertJsonToBpmn } from '../../flow-designer/converter/json-to-bpmn.js'
import {
  buildConditionExpression,
  parseConditionExpression,
} from '../../flow-designer/panel/condition-expression.js'
import { layoutBusinessProcess } from '../business-process-layout.js'
import {
  BUSINESS_PROCESS_NODE_TYPE,
  getBusinessProcessNodeDefinition,
} from '../business-process-node-types.js'
import {
  businessProcessHashInput,
  createBusinessProcessSchema,
  normalizeBusinessProcessSchema,
  validateBusinessProcessGraph,
} from '../business-process-schema.js'
import BusinessProcessCanvas from '../BusinessProcessCanvas.vue'
import BusinessProcessConditionConfig from '../BusinessProcessConditionConfig.vue'
import BusinessProcessNodeRenderer from '../BusinessProcessNodeRenderer.vue'
import { useBusinessProcessDesigner } from '../useBusinessProcessDesigner.js'

const objectRef = {
  objectId: '1900000000000001001',
  objectCode: 'sample_purchase_order',
}

describe('businessProcessJson 1.0 protocol', () => {
  it('migrates legacy alphabetic approval ports back to business result order', () => {
    const schema = createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef })
    schema.nodes.splice(1, 0, {
      id: 'approval_legacy',
      type: 'APPROVAL',
      name: '采购审批',
      ports: ['APPROVED', 'CANCELED', 'FAILED', 'REJECTED'],
      config: {},
    })

    expect(normalizeBusinessProcessSchema(schema).nodes
      .find(node => node.id === 'approval_legacy').ports)
      .toEqual(['APPROVED', 'REJECTED', 'CANCELED', 'FAILED'])
  })

  it('creates an independent manual-start to success-end draft with string IDs', () => {
    const schema = createBusinessProcessSchema({
      processCode: 'purchase_submit',
      objectRef,
      startType: BUSINESS_PROCESS_NODE_TYPE.START_MANUAL,
    })

    expect(schema).toMatchObject({
      schemaVersion: '1.0',
      processCode: 'purchase_submit',
      subject: {
        objectId: '1900000000000001001',
        objectCode: 'sample_purchase_order',
        recordIdSource: 'RUNTIME_RECORD',
      },
    })
    expect(schema.nodes.map(node => node.type)).toEqual(['START_MANUAL', 'END'])
    expect(schema.nodes.every(node => typeof node.id === 'string')).toBe(true)
    expect(schema.nodes.some(node => 'nodeType' in node || 'bpmnElementId' in node)).toBe(false)
    expect(validateBusinessProcessGraph(schema).isValid).toBe(true)
  })

  it('normalizes node, edge, port and dependency ordering for stable dirty checks', () => {
    const source = createBusinessProcessSchema({
      processCode: 'purchase_submit',
      objectRef,
    })
    source.dependencies.objects.push('aaa_object', 'sample_purchase_order')
    source.nodes.push({
      id: 'condition_amount',
      type: 'CONDITION',
      name: '金额判断',
      ports: ['MATCHED', 'BRANCH_1', 'OTHERWISE'],
      config: {
        branches: [
          { port: 'MATCHED', label: '条件 1', condition: { operator: 'AND', rules: [] } },
          { port: 'BRANCH_1', label: '条件 2', condition: { operator: 'AND', rules: [] } },
          { port: 'OTHERWISE', label: '其他情况', isDefault: true },
        ],
      },
    })
    source.nodes.reverse()
    source.edges[0].sourcePort = ' next '

    const normalized = normalizeBusinessProcessSchema(source)

    expect(normalized.nodes.map(node => node.id)).toEqual(['condition_amount', 'end_success', 'start_manual'])
    expect(normalized.nodes[0].ports).toEqual(['MATCHED', 'BRANCH_1', 'OTHERWISE'])
    expect(normalized.edges[0].sourcePort).toBe('NEXT')
    expect(normalized.dependencies.objects).toEqual(['aaa_object', 'sample_purchase_order'])
    expect(businessProcessHashInput(source)).toBe(businessProcessHashInput(normalized))
  })

  it('rejects numeric IDs and BPMN flowJson instead of coercing them', () => {
    const numericId = createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef })
    numericId.subject.objectId = Number('1900000000000001001')

    expect(() => normalizeBusinessProcessSchema(numericId)).toThrow(/ID.*字符串/)
    expect(() => normalizeBusinessProcessSchema({
      processId: 'Process_1',
      nodes: [{ id: 'S', nodeType: 'start' }],
      edges: [],
    })).toThrow(/BPMN|flowJson/)
  })

  it('business process schema is explicitly rejected by the BPMN converter', () => {
    const schema = createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef })
    expect(() => convertJsonToBpmn(schema)).toThrow(/businessProcessJson/)
  })
})

describe('business process graph validation', () => {
  it('reports multiple starts, dangling edges and cycles with stable issue codes', () => {
    const schema = createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef })
    schema.nodes.push({
      id: 'start_event',
      type: 'START_EVENT',
      name: '记录新增',
      ports: [],
      config: { eventType: 'RECORD_CREATED' },
    })
    schema.edges.push({
      id: 'edge_cycle',
      source: 'end_success',
      target: 'start_manual',
      sourcePort: 'NEXT',
      condition: {},
      isDefault: false,
    }, {
      id: 'edge_dangling',
      source: 'missing',
      target: 'end_success',
      sourcePort: 'NEXT',
      condition: {},
      isDefault: false,
    })

    const validation = validateBusinessProcessGraph(schema)
    const codes = validation.issues.map(issue => issue.code)

    expect(validation.isValid).toBe(false)
    expect(codes).toContain('START_NODE_COUNT')
    expect(codes).toContain('EDGE_SOURCE_MISSING')
    expect(codes).toContain('GRAPH_CYCLE')
  })

  it('node registry keeps business labels and ports independent from BPMN types', () => {
    expect(getBusinessProcessNodeDefinition('APPROVAL')).toMatchObject({
      label: '审批流程',
      ports: ['APPROVED', 'REJECTED', 'CANCELED', 'FAILED'],
    })
    expect(getBusinessProcessNodeDefinition('ACTION').bpmnType).toBeUndefined()
  })

  it('requires an independent flow status for low-code approval forms', () => {
    const designer = useBusinessProcessDesigner(
      createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef }),
    )
    const approvalId = designer.addNode('start_manual', 'APPROVAL', {
      config: {
        flowModelKey: 'purchase_approval',
        formAsset: { formKey: 'purchase_form', formMode: 'BUSINESS_OBJECT_FORM' },
        statusField: 'status',
      },
    })

    expect(validateBusinessProcessGraph(designer.schema.value).issues).toEqual(expect.arrayContaining([
      expect.objectContaining({ code: 'APPROVAL_FLOW_STATUS_REQUIRED', nodeId: approvalId }),
    ]))

    designer.updateNode(approvalId, {
      config: {
        ...designer.getNode(approvalId).config,
        statusField: 'flowStatus',
      },
    })

    expect(validateBusinessProcessGraph(designer.schema.value).issues
      .some(item => item.code === 'APPROVAL_FLOW_STATUS_REQUIRED')).toBe(false)
  })
})

describe('useBusinessProcessDesigner', () => {
  it('replaces a complete node config so deleted visibility conditions are not merged back', () => {
    const designer = useBusinessProcessDesigner(
      createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef }),
    )
    designer.updateNode('start_manual', {
      config: {
        positions: ['ROW'],
        visibleCondition: {
          operator: 'AND',
          rules: [{ field: 'status', operator: 'EQ', value: 'DRAFT' }],
        },
      },
    })

    designer.updateNode('start_manual', {
      config: { positions: ['ROW'] },
    }, { replaceConfig: true })

    expect(designer.getNode('start_manual').config).toEqual({ positions: ['ROW'] })
    expect(designer.getNode('start_manual').config).not.toHaveProperty('visibleCondition')
  })

  it('inserts, copies and deletes a business action while preserving the DAG', () => {
    const designer = useBusinessProcessDesigner(
      createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef }),
    )

    const actionId = designer.addNode('start_manual', 'ACTION', {
      name: '更新采购单',
      config: { actionType: 'UPDATE_RECORD', objectCode: 'sample_purchase_order' },
    })
    const copiedId = designer.copyNode(actionId)

    expect(designer.getNode(copiedId).name).toBe('更新采购单 副本')
    expect(validateBusinessProcessGraph(designer.schema.value).isValid).toBe(true)

    designer.deleteNode(actionId)
    expect(designer.getNode(actionId)).toBeNull()
    expect(validateBusinessProcessGraph(designer.schema.value).isValid).toBe(true)
  })

  it('condition insertion creates business branches rather than approval nodes', () => {
    const designer = useBusinessProcessDesigner(
      createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef }),
    )

    const conditionId = designer.addNode('start_manual', 'CONDITION', { name: '判断金额' })
    const condition = designer.getNode(conditionId)
    const outgoing = designer.getOutgoingEdges(conditionId)

    expect(condition.config.branches).toHaveLength(2)
    expect(outgoing).toHaveLength(2)
    expect(outgoing.filter(edge => edge.isDefault)).toHaveLength(1)
    expect(designer.schema.value.nodes.filter(node => node.type === 'APPROVAL')).toHaveLength(0)
    expect(validateBusinessProcessGraph(designer.schema.value).isValid).toBe(true)
  })

  it('inserts a governed node into one concrete condition branch edge', () => {
    const designer = useBusinessProcessDesigner(
      createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef }),
    )
    const conditionId = designer.addNode('start_manual', 'CONDITION', { name: '判断金额' })
    const matchedEdge = designer.getOutgoingEdges(conditionId)
      .find(edge => edge.sourcePort === 'MATCHED')

    const actionId = designer.insertNodeOnEdge(matchedEdge.id, 'ACTION', {
      name: '记录命中结果',
    })

    expect(designer.getEdge(matchedEdge.id).target).toBe(actionId)
    expect(designer.getOutgoingEdges(actionId)).toHaveLength(1)
    expect(designer.getOutgoingEdges(conditionId)
      .find(edge => edge.sourcePort === 'OTHERWISE').target).toBe('end_success')
    expect(validateBusinessProcessGraph(designer.schema.value).isValid).toBe(true)
  })

  it('keeps condition branches, ports and real outgoing edges synchronized', () => {
    const designer = useBusinessProcessDesigner(
      createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef }),
    )
    const conditionId = designer.addNode('start_manual', 'CONDITION', { name: '判断金额' })
    const originalMatchedEdge = designer.getOutgoingEdges(conditionId)
      .find(edge => edge.sourcePort === 'MATCHED')
    const branches = [
      {
        port: 'AMOUNT_HIGH',
        label: '金额较高',
        condition: {
          operator: 'AND',
          rules: [{ field: 'amount', operator: 'gt', value: '1000', endValue: '' }],
          expression: '$' + '{amount > 1000}',
        },
      },
      {
        port: 'BRANCH_1',
        label: '普通金额',
        condition: {
          operator: 'AND',
          rules: [{ field: 'amount', operator: 'le', value: '1000', endValue: '' }],
          expression: '$' + '{amount <= 1000}',
        },
      },
      { port: 'OTHERWISE', label: '其他情况', isDefault: true },
    ]

    designer.updateNode(conditionId, { config: { branches } })

    expect(designer.getNode(conditionId).ports).toEqual(['AMOUNT_HIGH', 'BRANCH_1', 'OTHERWISE'])
    expect(designer.getOutgoingEdges(conditionId).map(edge => edge.sourcePort))
      .toEqual(['AMOUNT_HIGH', 'BRANCH_1', 'OTHERWISE'])
    expect(designer.getOutgoingEdges(conditionId)
      .find(edge => edge.sourcePort === 'AMOUNT_HIGH').id).toBe(originalMatchedEdge.id)
    expect(designer.getOutgoingEdges(conditionId).filter(edge => edge.isDefault)).toHaveLength(1)

    designer.updateNode(conditionId, {
      config: { branches: branches.filter(branch => branch.port !== 'BRANCH_1') },
    })
    expect(designer.getOutgoingEdges(conditionId).map(edge => edge.sourcePort).sort())
      .toEqual(['AMOUNT_HIGH', 'OTHERWISE'])
    expect(validateBusinessProcessGraph(designer.schema.value).isValid).toBe(true)
  })

  it('deletes shared-successor condition and approval nodes without leaving broken edges', () => {
    for (const type of ['CONDITION', 'APPROVAL']) {
      const designer = useBusinessProcessDesigner(
        createBusinessProcessSchema({ processCode: `purchase_${type.toLowerCase()}`, objectRef }),
      )
      const nodeId = designer.addNode('start_manual', type)

      expect(designer.deleteNode(nodeId)).toBe(true)
      expect(designer.schema.value.edges).toHaveLength(1)
      expect(designer.schema.value.edges[0]).toMatchObject({
        source: 'start_manual',
        target: 'end_success',
      })
      expect(validateBusinessProcessGraph(designer.schema.value).isValid).toBe(true)
    }
  })

  it('refuses to delete a multi-branch node before its branches share one successor', () => {
    const designer = useBusinessProcessDesigner(
      createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef }),
    )
    const conditionId = designer.addNode('start_manual', 'CONDITION')
    const matchedEdge = designer.getOutgoingEdges(conditionId)
      .find(edge => edge.sourcePort === 'MATCHED')
    designer.insertNodeOnEdge(matchedEdge.id, 'ACTION')

    expect(() => designer.deleteNode(conditionId)).toThrow(/多个分支尚未汇合/)
  })

  it('supports undo, redo, dirty baseline and deep-cloned export', () => {
    const designer = useBusinessProcessDesigner(
      createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef }),
    )
    const actionId = designer.addNode('start_manual', 'ACTION')

    expect(designer.isDirty.value).toBe(true)
    expect(designer.undo()).toBe(true)
    expect(designer.getNode(actionId)).toBeNull()
    expect(designer.redo()).toBe(true)
    expect(designer.getNode(actionId)).toBeTruthy()

    designer.markSaved()
    expect(designer.isDirty.value).toBe(false)
    const exported = designer.exportSchema()
    exported.nodes.push({ id: 'mutated' })
    expect(designer.getNode('mutated')).toBeNull()
  })

  it('keeps governed dependencies synchronized with configured nodes', () => {
    const designer = useBusinessProcessDesigner(
      createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef }),
    )
    const actionId = designer.addNode('start_manual', 'ACTION', {
      config: {
        actionType: 'SEND_MESSAGE',
        messageTemplateCode: 'purchase_approved_notice',
      },
    })

    expect(designer.schema.value.dependencies.messageTemplates).toEqual(['purchase_approved_notice'])

    designer.updateNode(actionId, {
      config: {
        actionType: 'INVOKE_CAPABILITY',
        messageTemplateCode: null,
        capabilityCode: 'erp.purchase.sync',
      },
    })
    expect(designer.schema.value.dependencies.messageTemplates).toEqual([])
    expect(designer.schema.value.dependencies.capabilities).toEqual(['erp.purchase.sync'])
  })
})

describe('business process canvas', () => {
  it('reuses the shared viewport and edge layers while rendering business nodes', async () => {
    const schema = createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef })
    const wrapper = mount(BusinessProcessCanvas, {
      props: { schema, selectedNodeId: 'start_manual' },
    })

    expect(wrapper.find('.flow-canvas').exists()).toBe(true)
    expect(wrapper.find('.edge-layer').exists()).toBe(true)
    expect(wrapper.findAll('[data-business-process-node]')).toHaveLength(2)
    expect(wrapper.find('[data-node-id="start_manual"]').classes()).toContain('is-selected')

    await wrapper.find('[data-node-id="end_success"]').trigger('click')
    expect(wrapper.emitted('nodeSelect')[0][0].id).toBe('end_success')
  })

  it('renders an inline insertion control for every graph edge', async () => {
    const schema = createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef })
    const wrapper = mount(BusinessProcessCanvas, {
      props: { schema, selectedNodeId: 'start_manual' },
    })

    const insertion = wrapper.find('[data-business-insert-edge]')
    expect(wrapper.findAll('[data-business-insert-edge]')).toHaveLength(schema.edges.length)

    await insertion.find('button').trigger('click')
    await insertion.find('[data-business-insert-type="APPROVAL"]').trigger('click')

    expect(wrapper.emitted('insertNode')[0][0]).toEqual({
      edgeId: schema.edges[0].id,
      type: 'APPROVAL',
    })
  })

  it('keeps a shared approval successor centered with distinct result routes', () => {
    const designer = useBusinessProcessDesigner(
      createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef }),
    )
    const approvalId = designer.addNode('start_manual', 'APPROVAL')
    const wrapper = mount(BusinessProcessCanvas, {
      props: { schema: designer.schema.value, selectedNodeId: approvalId },
    })
    const layout = wrapper.vm.layoutResult
    const start = layout.nodePositions.get('start_manual')
    const approval = layout.nodePositions.get(approvalId)
    const end = layout.nodePositions.get('end_success')
    const resultEdges = designer.getOutgoingEdges(approvalId)
    const routeSignatures = resultEdges.map(edge => JSON.stringify(
      layout.edgePaths.get(edge.id)?.points || [],
    ))
    const insertionPositions = resultEdges.map((edge) => {
      const target = wrapper.find(`[data-edge-id="${edge.id}"]`)
      return `${target.element.style.left}:${target.element.style.top}`
    })

    expect(start.x).toBe(approval.x)
    expect(end.x).toBe(approval.x)
    expect(end.y).toBeGreaterThan(approval.y)
    expect(new Set(routeSignatures)).toHaveLength(4)
    expect(new Set(insertionPositions)).toHaveLength(4)
  })

  it('keeps three condition branches centered with distinct routes and insertion targets', () => {
    const designer = useBusinessProcessDesigner(
      createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef }),
    )
    const conditionId = designer.addNode('start_manual', 'CONDITION')
    const branches = [
      { port: 'MATCHED', label: '金额较高', condition: { operator: 'AND', rules: [{ field: 'amount' }] } },
      { port: 'BRANCH_1', label: '普通金额', condition: { operator: 'AND', rules: [{ field: 'amount' }] } },
      { port: 'OTHERWISE', label: '其他情况', isDefault: true },
    ]
    designer.updateNode(conditionId, { config: { branches } })
    const wrapper = mount(BusinessProcessCanvas, {
      props: { schema: designer.schema.value, selectedNodeId: conditionId },
    })
    const layout = wrapper.vm.layoutResult
    const condition = layout.nodePositions.get(conditionId)
    const end = layout.nodePositions.get('end_success')
    const resultEdges = designer.getOutgoingEdges(conditionId)

    expect(condition.x).toBe(end.x)
    expect(new Set(resultEdges.map(edge => JSON.stringify(
      layout.edgePaths.get(edge.id)?.points || [],
    )))).toHaveLength(3)
    expect(new Set(resultEdges.map((edge) => {
      const target = wrapper.find(`[data-edge-id="${edge.id}"]`)
      return `${target.element.style.left}:${target.element.style.top}`
    }))).toHaveLength(3)
  })

  it('lays out independent condition branches as card lanes and merges them without crossed endpoints', () => {
    const designer = useBusinessProcessDesigner(
      createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef }),
    )
    const conditionId = designer.addNode('start_manual', 'CONDITION')
    designer.updateNode(conditionId, {
      config: {
        branches: [
          { port: 'AMOUNT_HIGH', label: '金额较高', condition: { operator: 'AND', rules: [{ field: 'amount' }] } },
          { port: 'AMOUNT_NORMAL', label: '普通金额', condition: { operator: 'AND', rules: [{ field: 'amount' }] } },
          { port: 'OTHERWISE', label: '其他情况', isDefault: true },
        ],
      },
    })

    const actionIds = designer.getOutgoingEdges(conditionId).map((edge, index) =>
      designer.insertNodeOnEdge(edge.id, 'ACTION', { name: `分支动作 ${index + 1}` }),
    )
    const layout = layoutBusinessProcess(designer.schema.value)
    const conditionPosition = layout.nodePositions.get(conditionId)
    const actionPositions = actionIds.map(id => layout.nodePositions.get(id))
    const endPosition = layout.nodePositions.get('end_success')
    const branchEdges = designer.getOutgoingEdges(conditionId)

    expect(conditionPosition.width).toBe(288)
    expect(conditionPosition.height).toBe(92)
    expect(new Set(actionPositions.map(position => position.x))).toHaveLength(3)
    expect(new Set(actionPositions.map(position => position.y))).toHaveLength(1)
    expect(endPosition.y).toBeGreaterThan(actionPositions[0].y)
    expect(new Set(branchEdges.map(edge => JSON.stringify(layout.edgePaths.get(edge.id).points))))
      .toHaveLength(3)

    branchEdges.forEach((edge) => {
      const target = layout.nodePositions.get(edge.target)
      const points = layout.edgePaths.get(edge.id).points
      expect(points[0].y).toBe(conditionPosition.y + conditionPosition.height)
      expect(points.at(-1).y).toBe(target.y)
      expect(points.at(-1).x).toBeGreaterThanOrEqual(target.x)
      expect(points.at(-1).x).toBeLessThanOrEqual(target.x + target.width)
    })
  })

  it('keeps every business node at card size when a condition has different downstream targets', () => {
    const schema = createBusinessProcessSchema({ processCode: 'leave_route', objectRef })
    schema.nodes.push(
      {
        id: 'condition_department',
        type: 'CONDITION',
        name: '部门判断',
        ports: ['TECH', 'OTHERWISE'],
        config: {
          branches: [
            { port: 'TECH', label: '技术部门', condition: { operator: 'AND', rules: [] } },
            { port: 'OTHERWISE', label: '其他部门', isDefault: true },
          ],
        },
      },
      { id: 'tech_action', type: 'ACTION', name: '技术负责人处理', ports: [], config: {} },
      { id: 'other_action', type: 'ACTION', name: '行政负责人处理', ports: [], config: {} },
    )
    schema.edges = [
      { id: 'e_start', source: 'start_manual', target: 'condition_department', sourcePort: 'NEXT', condition: {}, isDefault: null },
      { id: 'e_tech', source: 'condition_department', target: 'tech_action', sourcePort: 'TECH', condition: {}, isDefault: null },
      { id: 'e_other', source: 'condition_department', target: 'other_action', sourcePort: 'OTHERWISE', condition: {}, isDefault: true },
      { id: 'e_tech_end', source: 'tech_action', target: 'end_success', sourcePort: 'NEXT', condition: {}, isDefault: null },
      { id: 'e_other_end', source: 'other_action', target: 'end_success', sourcePort: 'NEXT', condition: {}, isDefault: null },
    ]

    const layout = layoutBusinessProcess(schema)

    expect([...layout.nodePositions.values()].every(position =>
      position.width === 288 && position.height === 92)).toBe(true)
    expect(layout.nodePositions.get('tech_action').x).not.toBe(layout.nodePositions.get('other_action').x)
    expect(layout.edgePaths.size).toBe(schema.edges.length)
  })

  it('routes a branch that skips an intermediate rank around unrelated cards', () => {
    const schema = createBusinessProcessSchema({ processCode: 'leave_route', objectRef })
    const branchNodes = [
      {
        id: 'condition_days',
        type: 'CONDITION',
        name: '请假天数判断',
        ports: ['LONG_LEAVE', 'OTHERWISE'],
        config: {
          branches: [
            { port: 'LONG_LEAVE', label: '三天以上', condition: { operator: 'AND', rules: [] } },
            { port: 'OTHERWISE', label: '其他情况', isDefault: true },
          ],
        },
      },
      { id: 'manager_approval', type: 'ACTION', name: '经理处理', ports: ['NEXT'], config: {} },
    ]
    schema.nodes.splice(1, 0, ...branchNodes)
    schema.edges = [
      { id: 'e_start', source: 'start_manual', target: 'condition_days', sourcePort: 'NEXT' },
      { id: 'e_long', source: 'condition_days', target: 'manager_approval', sourcePort: 'LONG_LEAVE' },
      { id: 'e_default', source: 'condition_days', target: 'end_success', sourcePort: 'OTHERWISE', isDefault: true },
      { id: 'e_approval_end', source: 'manager_approval', target: 'end_success', sourcePort: 'NEXT' },
    ]

    const layout = layoutBusinessProcess(schema)
    const bypassPath = layout.edgePaths.get('e_default').points
    const mergePath = layout.edgePaths.get('e_approval_end').points
    const intermediateCard = layout.nodePositions.get('manager_approval')

    expect(polylineIntersectsRect(bypassPath, intermediateCard)).toBe(false)
    expect(orthogonalPathsOverlap(bypassPath, mergePath)).toBe(false)
    expect(bypassPath.length).toBeGreaterThan(2)
    expect(Math.max(...bypassPath.map(point => point.x)))
      .toBeGreaterThan(intermediateCard.x + intermediateCard.width)
  })
})

describe('business process condition experience', () => {
  it('reuses the condition expression compiler and parser for structured business rules', () => {
    const fields = [
      { field: 'amount', label: '申请金额', dataType: 'number' },
      { field: 'department', label: '申请部门', dataType: 'string' },
    ]
    const expression = buildConditionExpression([
      { field: 'amount', operator: 'ge', value: '1000' },
      { field: 'department', operator: 'eq', value: '研发部' },
    ], 'all', fields)

    expect(expression).toBe('$' + '{amount >= 1000 && department == \'研发部\'}')
    expect(parseConditionExpression(expression, fields)).toEqual({
      logic: 'all',
      rules: [
        { field: 'amount', operator: 'ge', value: '1000', endValue: '' },
        { field: 'department', operator: 'eq', value: '研发部', endValue: '' },
      ],
    })
  })

  it('shows Chinese branch meaning and never exposes technical port codes', async () => {
    const branches = [
      { port: 'MATCHED', label: '金额较高', condition: { operator: 'AND', rules: [] } },
      { port: 'OTHERWISE', label: '其他情况', isDefault: true },
    ]
    const wrapper = mount(BusinessProcessConditionConfig, {
      props: {
        branches,
        fields: [{ fieldCode: 'amount', fieldName: '申请金额', dataType: 'number' }],
      },
    })

    const branchNameInputs = wrapper.findAll('.branch-name-field input')
    expect(branchNameInputs.map(input => input.element.value)).toEqual(['金额较高', '其他情况'])
    expect(wrapper.text()).not.toMatch(/MATCHED|OTHERWISE|BRANCH_/)
    expect(wrapper.findAll('[data-condition-rule]')).toHaveLength(0)

    await wrapper.find('[data-condition-add-branch]').trigger('click')
    const emittedBranches = wrapper.emitted('update:branches')[0][0]
    expect(emittedBranches).toHaveLength(3)
    expect(emittedBranches[1]).toMatchObject({ label: '条件 2' })
  })

  it('removes the last judgment rule instead of recreating an empty one', async () => {
    const branches = [
      {
        port: 'BRANCH_1',
        label: '条件 1',
        condition: {
          operator: 'AND',
          logic: 'all',
          rules: [{ field: 'name', operator: 'eq', value: '张三', endValue: '' }],
        },
      },
      { port: 'OTHERWISE', label: '其他情况', isDefault: true },
    ]
    const wrapper = mount(BusinessProcessConditionConfig, {
      props: {
        branches,
        fields: [{ fieldCode: 'name', fieldName: '姓名', dataType: 'string' }],
      },
    })

    expect(wrapper.findAll('[data-condition-rule]')).toHaveLength(1)
    await wrapper.find('[aria-label="删除判断规则"]').trigger('click')

    const nextBranches = wrapper.emitted('update:branches')[0][0]
    expect(nextBranches[0].condition.rules).toEqual([])
    expect(nextBranches[0].condition.expression).toBe('')

    await wrapper.setProps({ branches: nextBranches })
    expect(wrapper.findAll('[data-condition-rule]')).toHaveLength(0)
    expect(wrapper.text()).toContain('暂无判断规则')
  })

  it('renders approval and condition ports in Chinese and exposes a card delete action', async () => {
    const approval = {
      id: 'approval_1',
      type: 'APPROVAL',
      name: '请假审批',
      ports: ['APPROVED', 'REJECTED', 'CANCELED', 'FAILED'],
      config: {},
    }
    const wrapper = mount(BusinessProcessNodeRenderer, {
      props: {
        node: approval,
        position: { x: 0, y: 0, width: 288, height: 92 },
        selected: true,
      },
    })

    expect(wrapper.text()).toContain('审批通过')
    expect(wrapper.text()).toContain('审批驳回')
    expect(wrapper.text()).toContain('审批取消')
    expect(wrapper.text()).toContain('执行失败')
    expect(wrapper.text()).not.toMatch(/APPROVED|REJECTED|CANCELED|FAILED/)

    await wrapper.find('[data-business-node-delete]').trigger('click')
    expect(wrapper.emitted('delete')[0][0]).toEqual(approval)
    expect(wrapper.emitted('select')).toBeUndefined()
  })
})

function polylineIntersectsRect(points, rect) {
  for (let index = 1; index < points.length; index += 1) {
    const start = points[index - 1]
    const end = points[index]
    if (start.x === end.x) {
      const minY = Math.min(start.y, end.y)
      const maxY = Math.max(start.y, end.y)
      if (start.x > rect.x && start.x < rect.x + rect.width
        && maxY > rect.y && minY < rect.y + rect.height) {
        return true
      }
    }
    else if (start.y === end.y) {
      const minX = Math.min(start.x, end.x)
      const maxX = Math.max(start.x, end.x)
      if (start.y > rect.y && start.y < rect.y + rect.height
        && maxX > rect.x && minX < rect.x + rect.width) {
        return true
      }
    }
  }
  return false
}

function orthogonalPathsOverlap(first, second) {
  for (let firstIndex = 1; firstIndex < first.length; firstIndex += 1) {
    const firstStart = first[firstIndex - 1]
    const firstEnd = first[firstIndex]
    for (let secondIndex = 1; secondIndex < second.length; secondIndex += 1) {
      const secondStart = second[secondIndex - 1]
      const secondEnd = second[secondIndex]
      if (firstStart.x === firstEnd.x && secondStart.x === secondEnd.x
        && firstStart.x === secondStart.x) {
        const overlap = Math.min(Math.max(firstStart.y, firstEnd.y), Math.max(secondStart.y, secondEnd.y))
          - Math.max(Math.min(firstStart.y, firstEnd.y), Math.min(secondStart.y, secondEnd.y))
        if (overlap > 0.5)
          return true
      }
      if (firstStart.y === firstEnd.y && secondStart.y === secondEnd.y
        && firstStart.y === secondStart.y) {
        const overlap = Math.min(Math.max(firstStart.x, firstEnd.x), Math.max(secondStart.x, secondEnd.x))
          - Math.max(Math.min(firstStart.x, firstEnd.x), Math.min(secondStart.x, secondEnd.x))
        if (overlap > 0.5)
          return true
      }
    }
  }
  return false
}
