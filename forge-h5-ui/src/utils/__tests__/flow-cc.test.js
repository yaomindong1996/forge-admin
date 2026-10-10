import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildCcDetailUrl,
  ccSender,
  ccSummary,
  ccTitle,
  formatCcUnread,
  isCcUnread,
  normalizeCcPage,
  normalizeCcUnreadCount,
} from '../flow-cc.js'

test('cc page normalization drops records without id', () => {
  const page = normalizeCcPage({ records: [{ id: 'c1' }, null, { title: 'no id' }], total: '7' })
  assert.deepEqual(page.records, [{ id: 'c1' }])
  assert.equal(page.total, 7)
  assert.deepEqual(normalizeCcPage(null), { records: [], total: 0 })
})

test('cc display fields fall back in order', () => {
  assert.equal(ccTitle({ title: '采购单 PO-1', processName: '采购审批' }), '采购单 PO-1')
  assert.equal(ccTitle({ processName: '采购审批' }), '采购审批')
  assert.equal(ccTitle({}), '流程抄送')
  assert.equal(ccSummary({ businessSummary: '金额 1200', content: '请知悉' }), '金额 1200')
  assert.equal(ccSummary({ content: '请知悉' }), '请知悉')
  assert.equal(ccSender({ sendUserName: '张三' }), '张三')
  assert.equal(ccSender({}), '系统')
})

test('unread state honors session read ids', () => {
  assert.equal(isCcUnread({ id: 'c1', isRead: 0 }), true)
  assert.equal(isCcUnread({ id: 'c1', isRead: 1 }), false)
  assert.equal(isCcUnread({ id: 'c1', isRead: 0 }, ['c1']), false)
  assert.equal(isCcUnread(null), false)
})

test('unread count formatting', () => {
  assert.equal(normalizeCcUnreadCount({ count: 3 }), 3)
  assert.equal(normalizeCcUnreadCount({ count: -1 }), 0)
  assert.equal(normalizeCcUnreadCount(undefined), 0)
  assert.equal(formatCcUnread(0), '')
  assert.equal(formatCcUnread({ count: 12 }), '12')
  assert.equal(formatCcUnread(100), '99+')
})

test('detail url encodes id and process instance', () => {
  assert.equal(buildCcDetailUrl({ id: 'a b', processInstanceId: 'p&1' }), '/pages/flow/cc-detail?id=a%20b&processInstanceId=p%261')
  assert.equal(buildCcDetailUrl({ id: 'c1' }), '/pages/flow/cc-detail?id=c1')
})
