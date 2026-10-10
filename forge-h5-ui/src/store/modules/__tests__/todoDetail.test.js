import assert from 'node:assert/strict'
import test from 'node:test'
import { createPinia, setActivePinia } from 'pinia'
import { useTodoDetailStore } from '../todoDetail.js'

function createStore(options) {
  setActivePinia(createPinia())
  const store = useTodoDetailStore()
  store.init(options)
  return store
}

test('init resets previous task state and resolves page mode', () => {
  const store = createStore({ taskId: 't1', mode: 'todo' })
  store.comment = '同意'
  store.moreVisible = true
  store.init({ taskId: 't2', mode: 'readonly', messageId: '9' })
  assert.equal(store.taskId, 't2')
  assert.equal(store.sourceMessageId, '9')
  assert.equal(store.readonlyMode, true)
  assert.equal(store.comment, '')
  assert.equal(store.moreVisible, false)
})

test('flow form snapshot overrides business context policy only where returned', () => {
  const store = createStore({ taskId: 't1' })
  store.businessContext = { allowDelegate: false, allowTerminate: true, requireSignature: true }
  store.formInfo = { allowDelegate: true }
  assert.equal(store.canDelegate, true)
  assert.equal(store.canTerminate, true)
  assert.equal(store.requireSignature, true)
  assert.equal(store.requireComment, true)
  assert.equal(store.canRejectToStart, false)
  assert.equal(store.hasMoreActions, true)
})

test('return target selection requires multi return and targets', () => {
  const store = createStore({ taskId: 't1' })
  store.formInfo = { allowMultiReturn: true, returnTargets: [] }
  assert.equal(store.canChooseReturnTarget, false)
  store.formInfo = { allowMultiReturn: true, returnTargets: [{ activityId: 'a1' }, { activityId: 'a2', activityName: '经理' }] }
  assert.equal(store.canChooseReturnTarget, true)
  assert.deepEqual(store.returnTargetOptions, [{ label: 'a1', value: 'a1' }, { label: '经理', value: 'a2' }])
})

test('candidate task is unassigned and pending', () => {
  const store = createStore({ taskId: 't1' })
  store.task = { status: 0 }
  assert.equal(store.isCandidateTask, true)
  store.task = { status: 0, assignee: '7' }
  assert.equal(store.isCandidateTask, false)
})

test('approval points are seeded unchecked and readonly mode cannot toggle', () => {
  const store = createStore({ taskId: 't1' })
  store.formInfo = { approvalPoints: [{ id: 'p1', required: true }, { id: 'p2' }] }
  store.seedApprovalPointChecks(store.formInfo)
  assert.equal(store.hasUncheckedRequiredPoints(), true)
  store.toggleApprovalPoint({ id: 'p1' })
  assert.equal(store.hasUncheckedRequiredPoints(), false)

  store.pageMode = 'readonly'
  store.toggleApprovalPoint({ id: 'p2' })
  assert.equal(store.approvalPointChecks.p2, false)
})

test('opening delegate closes more actions and clears the previous choice', () => {
  const store = createStore({ taskId: 't1' })
  store.moreVisible = true
  store.delegateUser = { id: 1 }
  store.delegateComment = '旧说明'
  store.openDelegate()
  assert.equal(store.moreVisible, false)
  assert.equal(store.delegateVisible, true)
  assert.equal(store.delegateUser, null)
  assert.equal(store.delegateComment, '')
})

test('sign actions follow actor, node policy and active relations', () => {
  const store = createStore({ taskId: 't1' })
  store.init({ taskId: 't1' }, '7')
  store.task = { assignee: '7' }
  assert.equal(store.canAddSign, true)
  assert.equal(store.canReduceSign, false)
  assert.equal(store.hasMoreActions, true)

  store.formInfo = { allowAddSign: false, allowDelegate: false }
  store.signRelations = [{ targetUserId: '21', status: 1 }, { targetUserId: '22', status: 0 }]
  assert.equal(store.canAddSign, false)
  assert.equal(store.canReduceSign, true)
  assert.deepEqual(store.activeSignRelations.map(item => item.targetUserId), ['21'])

  store.init({ taskId: 't1', mode: 'readonly' }, '7')
  store.task = { assignee: '7' }
  store.signRelations = [{ targetUserId: '21', status: 1 }]
  assert.equal(store.canAddSign, false)
  assert.equal(store.canReduceSign, false)
})

test('open sign resets the previous selection', () => {
  const store = createStore({ taskId: 't1' })
  store.moreVisible = true
  store.signUser = { id: '9' }
  store.signComment = '旧原因'
  store.openSign('reduceSign')
  assert.equal(store.moreVisible, false)
  assert.equal(store.signVisible, true)
  assert.equal(store.signAction, 'reduceSign')
  assert.equal(store.signUser, null)
  assert.equal(store.signComment, '')
  assert.equal(store.actionLoadingText, '提交中...')
  store.pendingAction = 'addSign'
  assert.equal(store.actionLoadingText, '加签中...')
})
