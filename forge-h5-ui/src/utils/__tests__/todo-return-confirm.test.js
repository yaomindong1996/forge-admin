import assert from 'node:assert/strict'
import test from 'node:test'
import { readTodoDetailSource } from './todo-detail-source.js'

test('return to a chosen node is confirmed once in the target sheet', () => {
  const source = readTodoDetailSource()
  assert.match(source, /@click="submitAction\('return', approvalSignatureRef\)">确认驳回</)
  assert.match(source, /const confirmed = action === 'return' \|\| await showConfirmDialog\(/)
})
