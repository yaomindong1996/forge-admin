import assert from 'node:assert/strict'
import test from 'node:test'
import { createPinia, setActivePinia } from 'pinia'
import { useCcStore } from '../cc.js'

function createStore() {
  setActivePinia(createPinia())
  return useCcStore()
}

test('mark read decrements unread count once and never below zero', async () => {
  const store = createStore()
  const calls = []
  store.unreadCount = 1
  store.setCurrent({ id: 'c1', isRead: 0 })

  assert.equal(await store.markRead('c1', id => calls.push(id)), true)
  assert.equal(await store.markRead('c1', id => calls.push(id)), false)
  assert.equal(await store.markRead('c2', id => calls.push(id)), true)

  assert.deepEqual(calls, ['c1', 'c2'])
  assert.equal(store.unreadCount, 0)
  assert.equal(store.current.isRead, 1)
})

test('mark read failure keeps unread state', async () => {
  const store = createStore()
  store.unreadCount = 2
  await assert.rejects(store.markRead('c1', async () => { throw new Error('denied') }))
  assert.equal(store.unreadCount, 2)
  assert.deepEqual(store.readIds, [])
})

test('mark all read clears unread count', async () => {
  const store = createStore()
  store.unreadCount = 5
  await store.markAllRead(async () => ({ data: 5 }))
  assert.equal(store.unreadCount, 0)
  assert.equal(store.unreadText, '')
})

test('unread count failure keeps the previous value', async () => {
  const store = createStore()
  await store.loadUnreadCount(async () => ({ data: { count: 120 } }))
  assert.equal(store.unreadText, '99+')
  await store.loadUnreadCount(async () => { throw new Error('network') })
  assert.equal(store.unreadCount, 120)
})
