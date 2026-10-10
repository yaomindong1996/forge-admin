import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildNoticeDetailUrl,
  filterNotices,
  formatFileSize,
  isNoticeTop,
  isNoticeUnread,
  normalizeNoticePage,
} from '../notice.js'

test('page payload is normalized and invalid rows dropped', () => {
  const page = normalizeNoticePage({ records: [{ noticeId: '1' }, null, { title: 'x' }], total: 9 })
  assert.equal(page.records.length, 1)
  assert.equal(page.total, 9)
  assert.deepEqual(normalizeNoticePage(null), { records: [], total: 0 })
})

test('only explicit isRead = 1 counts as read, session reads override', () => {
  assert.equal(isNoticeUnread({ noticeId: 1, isRead: 1 }), false)
  assert.equal(isNoticeUnread({ noticeId: 1, isRead: 0 }), true)
  assert.equal(isNoticeUnread({ noticeId: 1 }), true)
  assert.equal(isNoticeUnread({ noticeId: 1, isRead: 0 }, ['1']), false)
})

test('unread tab filters locally', () => {
  const rows = [{ noticeId: 1, isRead: 1 }, { noticeId: 2, isRead: 0 }, { noticeId: 3, isRead: 0 }]
  assert.deepEqual(filterNotices(rows, 'unread', ['3']).map(item => item.noticeId), [2])
  assert.equal(filterNotices(rows, 'all').length, 3)
})

test('helpers format size, top flag and url', () => {
  assert.equal(formatFileSize(0), '')
  assert.equal(formatFileSize(512), '512 B')
  assert.equal(formatFileSize(2048), '2.0 KB')
  assert.equal(formatFileSize(3 * 1024 * 1024), '3.0 MB')
  assert.equal(isNoticeTop({ isTop: '1' }), true)
  assert.equal(buildNoticeDetailUrl('12 3'), '/pages/notice/detail?noticeId=12%203')
})
