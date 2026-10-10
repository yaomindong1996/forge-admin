import assert from 'node:assert/strict'
import test from 'node:test'
import { createPinia, setActivePinia } from 'pinia'
import { useInitiatorSelectStore } from '../initiatorSelect.js'

function createStore() {
  setActivePinia(createPinia())
  return useInitiatorSelectStore()
}

const nodes = [
  { nodeKey: 'leader', nodeName: '部门负责人', multiple: false },
  { nodeKey: 'cc', nodeName: '抄送人' },
]

test('confirm resolves selected user ids per node', async () => {
  const store = createStore()
  const pending = store.open(nodes)
  assert.equal(store.isComplete, false)

  store.pick('leader')
  store.toggleMember({ userId: 1, realName: '张三' })
  assert.equal(store.activeNodeKey, '', 'single-select returns to the node list after picking')
  store.toggleMember({ userId: 9, realName: '无节点' })

  store.pick('cc')
  store.toggleMember({ userId: 2, realName: '李四' })
  store.toggleMember({ userId: 3, realName: '王五' })
  store.toggleMember({ userId: 3, realName: '王五' })
  store.back()

  assert.equal(store.isComplete, true)
  store.confirm()
  assert.deepEqual(await pending, { leader: ['1'], cc: ['2'] })
  assert.equal(store.visible, false)
})

test('single-select replaces the previous member and removal works', () => {
  const store = createStore()
  store.open(nodes)
  store.pick('leader')
  store.toggleMember({ userId: 1 })
  store.pick('leader')
  store.toggleMember({ userId: 2 })
  assert.deepEqual(store.selections.leader.map(member => member.userId), ['2'])
  store.removeMember('leader', 2)
  assert.deepEqual(store.selections.leader, [])
})

test('confirm with a missing node throws and keeps the sheet open', () => {
  const store = createStore()
  store.open(nodes)
  store.pick('leader')
  store.toggleMember({ userId: 1 })
  assert.throws(() => store.confirm(), /请选择「抄送人」的审批人/)
  assert.equal(store.visible, true)
})

test('cancel and reopening resolve the pending request with null', async () => {
  const store = createStore()
  const first = store.open(nodes)
  const second = store.open(nodes)
  assert.equal(await first, null)
  store.cancel()
  assert.equal(await second, null)
  assert.equal(store.visible, false)
})
