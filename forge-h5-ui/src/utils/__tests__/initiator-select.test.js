import assert from 'node:assert/strict'
import test from 'node:test'
import {
  collectInitiatorSelectSelections,
  normalizeInitiatorSelectIds,
  normalizeInitiatorSelectNodes,
} from '../initiator-select.js'

const nodes = [
  { nodeKey: 'leader', nodeName: '部门负责人', multiple: false },
  { nodeKey: 'countersign', nodeName: '会签人' },
]

test('every node must have an approver', () => {
  assert.throws(
    () => collectInitiatorSelectSelections(nodes, { leader: ['1'] }),
    /请选择「会签人」的审批人/,
  )
})

test('single-select nodes keep only the first id, multi-select keeps all', () => {
  const result = collectInitiatorSelectSelections(nodes, { leader: ['1', '2'], countersign: ['3', '4', '3'] })
  assert.deepEqual(result, { leader: ['1'], countersign: ['3', '4'] })
})

test('nodes without key are skipped and names fall back to key', () => {
  const normalized = normalizeInitiatorSelectNodes([{ nodeName: '无键' }, { nodeKey: ' cfo ' }, null])
  assert.deepEqual(normalized, [{ nodeKey: 'cfo', nodeName: 'cfo', multiple: true }])
  assert.deepEqual(collectInitiatorSelectSelections([{ nodeName: '无键' }], {}), {})
})

test('ids are normalized to unique non-empty strings', () => {
  assert.deepEqual(normalizeInitiatorSelectIds([1, '1', ' ', null, 2]), ['1', '2'])
  assert.deepEqual(normalizeInitiatorSelectIds('9'), ['9'])
  assert.deepEqual(normalizeInitiatorSelectIds(undefined), [])
})
