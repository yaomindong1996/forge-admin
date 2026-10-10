import assert from 'node:assert/strict'
import test from 'node:test'
import { buildStartUrl, buildStartableGroups, indexMenusByConfigKey } from '../startable-objects.js'

const menus = [
  { key: 'm1', label: '请假', target: { url: '/pages/lowcode-runtime?configKey=leave&applicationId=10' } },
  { key: 'm2', label: '报销', target: { url: '/pages/app-entry?runtimeConfigKey=expense' } },
  { key: 'm3', label: '公告', target: { url: '/pages/notice/index' } },
]

const objects = [
  { objectCode: 'leave', objectName: '请假单', configKey: 'leave', applicationId: '10', applicationName: '人事' },
  { objectCode: 'trip', objectName: '出差单', configKey: 'trip', applicationId: '10', applicationName: '人事' },
  { objectCode: 'expense', objectName: '报销单', configKey: 'expense', applicationId: '20', applicationName: '财务' },
  { objectCode: 'misc', objectName: '杂项', configKey: 'misc' },
]

test('menu index reads every config key alias', () => {
  const index = indexMenusByConfigKey(menus)
  assert.deepEqual([...index.keys()], ['leave', 'expense'])
  assert.equal(index.get('leave').query.applicationId, '10')
})

test('only objects the user has a menu for are listed, grouped in order', () => {
  const groups = buildStartableGroups(objects, indexMenusByConfigKey(menus))
  assert.deepEqual(groups.map(group => group.label), ['人事', '财务'])
  assert.deepEqual(groups[0].items.map(item => item.label), ['请假单'])
})

test('objects without an application go to the trailing other group', () => {
  const index = indexMenusByConfigKey([...menus, { key: 'm4', target: { url: '/pages/lowcode-runtime?configKey=misc' } }])
  const groups = buildStartableGroups([objects[3], ...objects], index)
  assert.equal(groups.at(-1).label, '其他')
  assert.deepEqual(groups.at(-1).items.map(item => item.configKey), ['misc'])
})

test('search matches application name or object name', () => {
  const index = indexMenusByConfigKey(menus)
  assert.deepEqual(buildStartableGroups(objects, index, '财务').map(group => group.label), ['财务'])
  assert.deepEqual(buildStartableGroups(objects, index, '请假')[0].items.map(item => item.label), ['请假单'])
  assert.equal(buildStartableGroups(objects, index, '不存在').length, 0)
})

test('start url opens the runtime in create mode with app context', () => {
  const url = buildStartUrl(objects[0], { query: { applicationId: '10', appId: '7' } })
  assert.equal(url, '/pages/lowcode-runtime?configKey=leave&mode=create&flow=1&appId=7&applicationId=10&title=%E8%AF%B7%E5%81%87%E5%8D%95')
  assert.match(buildStartUrl(objects[2], {}), /applicationId=20/)
})
