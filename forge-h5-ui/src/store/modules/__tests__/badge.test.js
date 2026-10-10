import assert from 'node:assert/strict'
import test from 'node:test'
import { createPinia, setActivePinia } from 'pinia'
import { formatBadgeCount, normalizeUnreadCount, useBadgeStore } from '../badge.js'

function createStore() {
  setActivePinia(createPinia())
  return useBadgeStore()
}

test('concurrent refresh calls share one request', async () => {
  const store = createStore()
  let calls = 0
  let release
  const loader = () => {
    calls += 1
    return new Promise((resolve) => { release = () => resolve({ todoCount: 3, unreadCount: 120 }) })
  }

  const first = store.refresh(loader)
  const second = store.refresh(loader)
  await Promise.resolve()
  release()
  await Promise.all([first, second])

  assert.equal(calls, 1)
  assert.equal(store.todoCount, 3)
  assert.equal(store.unreadCount, 120)
  assert.equal(store.todoText, '3')
  assert.equal(store.unreadText, '99+')
})

test('a failed count keeps the previous value', async () => {
  const store = createStore()
  await store.refresh(async () => ({ todoCount: 5, unreadCount: 2 }))
  await store.refresh(async () => ({ todoCount: undefined, unreadCount: 0 }))
  assert.equal(store.todoCount, 5)
  assert.equal(store.unreadCount, 0)
  assert.equal(store.unreadText, '')

  await store.refresh(async () => { throw new Error('network') })
  assert.equal(store.todoCount, 5)
})

test('notice unread counts toward the message tab only', async () => {
  const store = createStore()
  await store.refresh(async () => ({ todoCount: 1, unreadCount: 3, noticeUnreadCount: 2 }))
  assert.equal(store.unreadText, '3')
  assert.equal(store.messageTabText, '5')

  store.setNoticeUnreadCount(98)
  assert.equal(store.messageTabText, '99+')
})

test('a failed notice count does not block the message count', async () => {
  const store = createStore()
  await store.refresh(async () => ({ unreadCount: 1, noticeUnreadCount: 4 }))
  await store.refresh(async () => ({ unreadCount: 6, noticeUnreadCount: undefined }))
  assert.equal(store.unreadCount, 6)
  assert.equal(store.noticeUnreadCount, 4)
  assert.equal(store.messageTabText, '10')
})

test('badge text and unread payload normalization', () => {
  assert.equal(formatBadgeCount(0), '')
  assert.equal(formatBadgeCount(-1), '')
  assert.equal(formatBadgeCount('7'), '7')
  assert.equal(formatBadgeCount(100), '99+')
  assert.equal(normalizeUnreadCount(4), 4)
  assert.equal(normalizeUnreadCount({ totalCount: 6 }), 6)
  assert.equal(normalizeUnreadCount({ unreadCount: 2 }), 2)
  assert.equal(normalizeUnreadCount(null), 0)
})
