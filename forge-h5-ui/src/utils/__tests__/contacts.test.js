import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildContactMemberUrl,
  buildContactOrgUrl,
  canDial,
  contactInitials,
  contactSubtitle,
  normalizeContactPage,
  safeDecode,
} from '../contacts.js'

test('头像文字：中文取后两字，英文取前两字母', () => {
  assert.equal(contactInitials('张三丰'), '三丰')
  assert.equal(contactInitials('李四'), '李四')
  assert.equal(contactInitials('john doe'), 'JO')
  assert.equal(contactInitials(''), '?')
})

test('副标题取主部门和主岗位', () => {
  assert.equal(contactSubtitle({ orgNames: ['研发部', '产品部'], postNames: ['工程师'] }), '研发部 · 工程师')
  assert.equal(contactSubtitle({ orgNames: [], postNames: ['工程师'] }), '工程师')
  assert.equal(contactSubtitle({}), '')
})

test('脱敏号码不可拨打', () => {
  assert.equal(canDial('13800138000'), true)
  assert.equal(canDial('+86 138-0013-8000'), true)
  assert.equal(canDial('138****8000'), false)
  assert.equal(canDial(''), false)
})

test('分页结果兼容 records 与数组', () => {
  assert.deepEqual(normalizeContactPage({ records: [{ userId: 1 }], total: '12' }), { records: [{ userId: 1 }], total: 12 })
  assert.deepEqual(normalizeContactPage([{ userId: 2 }]), { records: [{ userId: 2 }], total: 1 })
  assert.deepEqual(normalizeContactPage(null), { records: [], total: 0 })
})

test('路由参数解码失败时保留原文', () => {
  assert.equal(safeDecode('%E7%A0%94%E5%8F%91'), '研发')
  assert.equal(safeDecode('100%完成'), '100%完成')
  assert.equal(safeDecode(undefined), '')
})

test('通讯录跳转地址会编码参数', () => {
  assert.equal(buildContactOrgUrl({ orgId: 3, orgName: '研发 部' }), '/pages/contacts/org?orgId=3&orgName=%E7%A0%94%E5%8F%91%20%E9%83%A8')
  assert.equal(buildContactOrgUrl(), '/pages/contacts/org')
  assert.equal(buildContactMemberUrl(9), '/pages/contacts/member?userId=9')
})
