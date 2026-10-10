import assert from 'node:assert/strict'
import test from 'node:test'
import {
  MENU_TONES,
  buildMobileMenuGroups,
  flattenMobileMenus,
  resolveMobileMenuIcon,
  resolveMobileMenuTarget,
  resolveMobileMenuTone,
} from '../mobile-menu.js'

test('only authorized visible H5 routes and low-code pages become app entries', () => {
  const menus = [
    { id: 1, resourceType: 1, resourceName: '业务', children: [
      { id: 11, resourceType: 2, clientCode: 'h5', resourceName: '印章申请', path: '/pages/lowcode-runtime?configKey=seal&appId=42' },
      { id: 12, resourceType: 2, clientCode: 'h5', resourceName: '待办', path: '/pages/todo' },
      { id: 13, resourceType: 2, clientCode: 'pc', resourceName: 'PC 页面', path: '/pages/lowcode-runtime?configKey=pc' },
      { id: 14, resourceType: 2, resourceName: '占位页', path: '/pages/app-entry' },
      { id: 15, resourceType: 2, resourceName: '演示页', path: '/pages/demo/loading/index' },
      { id: 16, resourceType: 3, resourceName: '按钮', path: '/pages/todo' },
      { id: 17, resourceType: 2, visible: 0, resourceName: '隐藏', path: '/pages/todo' },
      { id: 18, resourceType: 2, resourceName: '有按钮的页面', path: '/pages/lowcode-runtime?configKey=with_actions', children: [
        { id: 181, resourceType: 3, resourceName: '创建', path: '/pages/todo' },
      ] },
    ] },
    { id: 2, resourceType: 1, resourceName: '空目录', children: [
      { id: 21, resourceType: 2, path: '/pc-only', resourceName: 'PC 专用' },
    ] },
  ]
  assert.deepEqual(flattenMobileMenus(menus).map(item => item.label), ['印章申请', '有按钮的页面'])
  assert.equal(buildMobileMenuGroups(menus).length, 1)
  assert.equal(flattenMobileMenus(menus)[0].target.url, '/pages/lowcode-runtime?configKey=seal&appId=42')
})

test('menu entries preserve meaningful icons and replace generic duplicates semantically', () => {
  const menus = [{
    id: 1,
    resourceType: 2,
    resourceName: '印章申请',
    path: '/pages/lowcode-runtime?configKey=approval',
    icon: 'AppsOutline',
  }]
  assert.equal(flattenMobileMenus(menus)[0].icon, '/static/app-icons/seal.png')
  assert.equal(resolveMobileMenuIcon({ id: 12, resourceName: '店铺', icon: 'AppsOutline' }), '/static/app-icons/shop.png')
  assert.equal(resolveMobileMenuIcon({ id: 13, resourceName: '测试', icon: 'i-ai-icon:workflow' }), '/static/app-icons/tool.png')
  assert.equal(resolveMobileMenuIcon({ id: 14, resourceName: '店铺', icon: 'ionicons5:StorefrontOutline' }), '/static/app-icons/shop.png')
  assert.equal(resolveMobileMenuIcon({ icon: 'https://cdn.example.com/app.png' }), 'https://cdn.example.com/app.png')
  assert.equal(resolveMobileMenuIcon({ icon: '/static/custom/seal.png' }), '/static/custom/seal.png')
  assert.match(resolveMobileMenuIcon({ id: 12, resourceName: '未知应用', icon: '-1' }), /^\/static\/app-icons\/[a-z]+\.png$/)
})

test('menu routes preserve configured queries and never use placeholder pages', () => {
  assert.equal(resolveMobileMenuTarget({ path: '/pages/app-entry?configKey=demo' }).url, '/pages/app-entry?configKey=demo')
  assert.equal(resolveMobileMenuTarget({ path: '/pages/lowcode-runtime' }), null)
  assert.equal(resolveMobileMenuTarget({ path: '/pages/app-entry' }), null)
  assert.equal(resolveMobileMenuTarget({ path: '/pages/todo?foo=bar' }).tab, true)
  assert.equal(resolveMobileMenuTarget({ path: '/pages/message/index' }).tab, true)
  assert.equal(resolveMobileMenuTarget({ path: '/pages/contacts/index' }).tab, true)
  assert.equal(resolveMobileMenuTarget({ path: '/ai/crud-page/orders' }).url, '/pages/lowcode-runtime?configKey=orders')
})

test('app icon tones follow menu semantics and stay stable for the same menu', () => {
  assert.equal(resolveMobileMenuTone({ id: 1, resourceName: '费用报销' }).key, 'orange')
  assert.equal(resolveMobileMenuTone({ id: 2, resourceName: '采购审批流程' }).key, 'orange')
  assert.equal(resolveMobileMenuTone({ id: 3, resourceName: '销售报表' }).key, 'purple')
  assert.deepEqual(resolveMobileMenuTone({ id: 3, resourceName: '销售报表' }), { key: 'purple', ...MENU_TONES.purple })

  const unknown = { id: 99, resourceName: '未知应用', path: '/pages/lowcode-runtime?configKey=x' }
  const first = resolveMobileMenuTone(unknown)
  assert.ok(Object.keys(MENU_TONES).includes(first.key))
  assert.deepEqual(resolveMobileMenuTone({ ...unknown }), first)

  const menus = [{ id: 7, resourceType: 2, resourceName: '印章申请', path: '/pages/lowcode-runtime?configKey=seal' }]
  const [entry] = flattenMobileMenus(menus)
  assert.equal(entry.tone, 'red')
  assert.equal(entry.color, MENU_TONES.red.color)
  assert.equal(entry.toneBg, MENU_TONES.red.bg)
  assert.deepEqual(flattenMobileMenus(menus)[0], entry)
})
