import assert from 'node:assert/strict'
import { test } from 'node:test'
import { isUrgentFlowPriority, resolveFlowPriorityLevel } from '../flow-priority.js'

test('flowable default priority 50 is normal, not urgent', () => {
  assert.equal(resolveFlowPriorityLevel(50), 1)
  assert.equal(isUrgentFlowPriority(50), false)
})

test('business levels and high flowable values map like admin', () => {
  assert.equal(isUrgentFlowPriority(3), true)
  assert.equal(isUrgentFlowPriority(80), true)
  assert.equal(resolveFlowPriorityLevel(60), 2)
  assert.equal(resolveFlowPriorityLevel(10), 0)
})

test('missing priority is not urgent', () => {
  assert.equal(resolveFlowPriorityLevel(null), null)
  assert.equal(isUrgentFlowPriority(undefined), false)
  assert.equal(isUrgentFlowPriority('abc'), false)
})
