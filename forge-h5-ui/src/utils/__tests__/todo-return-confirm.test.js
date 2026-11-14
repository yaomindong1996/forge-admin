import assert from 'node:assert/strict'
import test from 'node:test'
import { readTodoDetailSource } from './todo-detail-source.js'

test('return and delegate are confirmed once in their sheets', () => {
  const source = readTodoDetailSource()
  assert.match(source, /@click="submitAction\('return', approvalSignatureRef\)">确认驳回</)
  assert.match(source, /emit\('confirm', delegateSignatureRef\)">确认转办</)
  assert.match(source, /const SHEET_CONFIRMED_ACTIONS = new Set\(\['return', 'delegate'\]\)/)
  assert.match(source, /const confirmed = SHEET_CONFIRMED_ACTIONS\.has\(action\) \|\| await showConfirmDialog\(/)
})
