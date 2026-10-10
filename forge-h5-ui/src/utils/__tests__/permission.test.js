import assert from 'node:assert/strict'
import test from 'node:test'
import { FLOW_PERMISSIONS, hasPermission } from '../permission.js'

test('exact permission codes match', () => {
  assert.equal(hasPermission(['ai:businessFlow:start'], FLOW_PERMISSIONS.start), true)
  assert.equal(hasPermission(['ai:businessFlow:start'], FLOW_PERMISSIONS.withdraw), false)
})

test('super admin wildcard grants everything', () => {
  assert.equal(hasPermission(['*:*:*'], FLOW_PERMISSIONS.runtime), true)
})

test('empty or invalid input is denied', () => {
  assert.equal(hasPermission([], FLOW_PERMISSIONS.start), false)
  assert.equal(hasPermission(null, FLOW_PERMISSIONS.start), false)
  assert.equal(hasPermission(['*:*:*'], ''), false)
})
