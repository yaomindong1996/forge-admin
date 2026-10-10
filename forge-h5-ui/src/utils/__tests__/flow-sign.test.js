import assert from 'node:assert/strict'
import test from 'node:test'
import {
  activeSignRelations,
  buildSignExcludeIds,
  canAddSign,
  canReduceSign,
  isTaskActor,
  signStatusText,
  signUserName,
} from '../flow-sign.js'

const relations = [
  { targetUserId: '21', targetUserName: '李四', status: 1 },
  { targetUserId: '22', status: 0 },
]

test('assignee or owner is the task actor', () => {
  assert.equal(isTaskActor({ assignee: '7' }, 7), true)
  assert.equal(isTaskActor({ assignee: '8', owner: '7' }, '7'), true)
  assert.equal(isTaskActor({ assignee: '8' }, '7'), false)
  assert.equal(isTaskActor({ assignee: '7' }, ''), false)
})

test('add sign follows node policy, readonly mode and actor', () => {
  const task = { assignee: '7' }
  assert.equal(canAddSign({ task, userId: '7', policy: {} }), true)
  assert.equal(canAddSign({ task, userId: '7', policy: { allowAddSign: true } }), true)
  assert.equal(canAddSign({ task, userId: '7', policy: { allowAddSign: false } }), false)
  assert.equal(canAddSign({ task, userId: '7', policy: {}, readonly: true }), false)
  assert.equal(canAddSign({ task, userId: '9', policy: {} }), false)
})

test('reduce sign requires an active relation but ignores add-sign policy', () => {
  const task = { assignee: '7' }
  assert.equal(canReduceSign({ task, userId: '7', relations }), true)
  assert.equal(canReduceSign({ task, userId: '7', relations: [relations[1]] }), false)
  assert.equal(canReduceSign({ task, userId: '7', relations, readonly: true }), false)
  assert.equal(canReduceSign({ task, userId: '9', relations }), false)
})

test('relation display and active filtering', () => {
  assert.deepEqual(activeSignRelations(relations).map(item => item.targetUserId), ['21'])
  assert.deepEqual(activeSignRelations(null), [])
  assert.equal(signUserName(relations[0]), '李四')
  assert.equal(signUserName(relations[1]), '22')
  assert.equal(signUserName({}), '未知人员')
  assert.equal(signStatusText(relations[0]), '有效')
  assert.equal(signStatusText(relations[1]), '已撤回')
})

test('picker excludes self, assignee and active signers', () => {
  assert.deepEqual(buildSignExcludeIds({ userId: '7', task: { assignee: '7' }, relations }), ['7', '21'])
})
