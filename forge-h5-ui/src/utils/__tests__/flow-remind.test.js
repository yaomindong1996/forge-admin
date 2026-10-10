import assert from 'node:assert/strict'
import test from 'node:test'
import {
  canRemindTask,
  isRemindCooling,
  isRemindThrottledMessage,
  markRemindCooldown,
  REMIND_COOLDOWN_MS,
  remindTaskId,
} from '../flow-remind.js'

test('only running tasks with an assignee can be reminded', () => {
  assert.equal(canRemindTask({ status: 1 }), true)
  assert.equal(canRemindTask({ status: 0, assignee: '7' }), true)
  assert.equal(canRemindTask({ status: 'RUNNING', assignee: '7' }), true)
  assert.equal(canRemindTask({ status: 0 }), false)
  assert.equal(canRemindTask({ status: 2, assignee: '7' }), false)
  assert.equal(canRemindTask(null), false)
})

test('remind task id prefers flowable task id', () => {
  assert.equal(remindTaskId({ taskId: 't1', id: 'row-1' }), 't1')
  assert.equal(remindTaskId({ id: 'row-1' }), 'row-1')
  assert.equal(remindTaskId(undefined), '')
})

test('cooldown lasts ten minutes and then expires', () => {
  const cooldowns = {}
  const now = 1_000_000
  markRemindCooldown(cooldowns, 't1', now)
  assert.equal(isRemindCooling(cooldowns, 't1', now + REMIND_COOLDOWN_MS - 1), true)
  assert.equal(isRemindCooling(cooldowns, 't1', now + REMIND_COOLDOWN_MS), false)
  assert.equal(isRemindCooling(cooldowns, 't2', now), false)
  markRemindCooldown(cooldowns, '', now)
  assert.deepEqual(Object.keys(cooldowns), ['t1'])
})

test('backend throttle message is recognized', () => {
  assert.equal(isRemindThrottledMessage('已催办过，请 10 分钟后再试'), true)
  assert.equal(isRemindThrottledMessage('任务尚未被签收，暂时无法催办'), false)
})
