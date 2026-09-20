import assert from 'node:assert/strict'
import test from 'node:test'
import { createFlowActionCredentials } from '../flow-action-idempotency.js'

test('creates a stable digest and a fresh key for the same flow action', async () => {
  const payload = { comment: '同意', variables: { amount: 1200 } }
  const first = await createFlowActionCredentials('approve', '9223372036854775807', payload)
  const second = await createFlowActionCredentials('approve', '9223372036854775807', payload)

  assert.equal(first.requestDigest, second.requestDigest)
  assert.notEqual(first.idempotencyKey, second.idempotencyKey)
  assert.match(first.idempotencyKey, /^flow:approve:9223372036854775807:/)
  assert.ok(first.idempotencyKey.length <= 128)
  assert.ok(first.requestDigest.length <= 71)
})

test('changes the digest when the protected action payload changes', async () => {
  const approved = await createFlowActionCredentials('approve', 'task-1', { comment: '同意' })
  const rejected = await createFlowActionCredentials('reject', 'task-1', { comment: '驳回' })

  assert.notEqual(approved.requestDigest, rejected.requestDigest)
})
