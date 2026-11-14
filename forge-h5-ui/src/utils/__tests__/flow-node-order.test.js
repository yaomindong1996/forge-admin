import assert from 'node:assert/strict'
import test from 'node:test'
import { orderProcessNodes } from '../flow-node-order.js'

const node = (nodeId, nodeType = 'UserTask') => ({ nodeId, nodeName: nodeId, nodeType })

test('orders nodes along sequence flows instead of BPMN element order', () => {
  const nodes = [node('Start', 'StartEvent'), node('End', 'EndEvent'), node('A'), node('B'), node('CC', 'ServiceTask')]
  const flows = [
    { sourceRef: 'Start', targetRef: 'A' },
    { sourceRef: 'A', targetRef: 'B' },
    { sourceRef: 'B', targetRef: 'CC' },
    { sourceRef: 'CC', targetRef: 'End' },
  ]
  assert.deepEqual(orderProcessNodes(nodes, flows).map(item => item.nodeId), ['Start', 'A', 'B', 'CC', 'End'])
})

test('lists gateway branches layer by layer and keeps unreachable nodes at the end', () => {
  const nodes = [node('End', 'EndEvent'), node('Orphan'), node('Join', 'ParallelGateway'), node('B2'), node('B1'), node('Fork', 'ParallelGateway'), node('Start', 'StartEvent')]
  const flows = [
    { sourceRef: 'Start', targetRef: 'Fork' },
    { sourceRef: 'Fork', targetRef: 'B1' },
    { sourceRef: 'Fork', targetRef: 'B2' },
    { sourceRef: 'B1', targetRef: 'Join' },
    { sourceRef: 'B2', targetRef: 'Join' },
    { sourceRef: 'Join', targetRef: 'End' },
  ]
  const ids = orderProcessNodes(nodes, flows).map(item => item.nodeId)
  assert.deepEqual(ids.slice(0, 4), ['Start', 'Fork', 'B1', 'B2'])
  assert.equal(ids.indexOf('Join') < ids.indexOf('End'), true)
  assert.equal(ids.includes('Orphan'), true)
  assert.equal(ids.length, nodes.length)
})

test('falls back to the original order without flows', () => {
  assert.deepEqual(orderProcessNodes([node('X'), node('Y')], undefined).map(item => item.nodeId), ['X', 'Y'])
  assert.deepEqual(orderProcessNodes(undefined), [])
})
