import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { reactive } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { findGuideElement, guideLocation, locateAssistantGuide } from '../assistant-guide-location'

const component = { template: '<div />' }
const menus = [{ key: '1', type: 'menu', label: '用户管理', path: '/system/user' }]
const user = { userId: '11', tenantId: '1' }
const option = { id: '1' }

let router
let permission

function button(anchor, parent = document.body) {
  const element = document.createElement('button')
  element.dataset.assistantGuide = anchor
  element.getClientRects = () => [{ width: 20, height: 20 }]
  parent.append(element)
  return element
}

describe('页面操作标记', () => {
  beforeEach(async () => {
    permission = reactive({ menuDataLoaded: true, menus, accessRoutes: [{ path: '/system/user' }] })
    router = createRouter({
      history: createMemoryHistory(),
      routes: ['/', '/system/user', '/system/role'].map(path => ({ path, component })),
    })
    await router.push('/')
  })

  afterEach(async () => {
    await router.push('/')
    document.body.replaceChildren()
  })

  it('跳到授权页面，只标记并聚焦可见按钮，离开页面移除标记', async () => {
    const control = button('add')
    expect(await locateAssistantGuide(router, permission, user, option, 'add')).toBe('marked')
    expect(router.currentRoute.value.path).toBe('/system/user')
    expect(control.classList.contains('assistant-guide-highlight')).toBe(true)
    expect(document.activeElement).toBe(control)
    await router.push('/')
    expect(control.classList.contains('assistant-guide-highlight')).toBe(false)
  })

  it('撤权或上下文变化后，旧卡片不能继续跳转', async () => {
    expect(guideLocation(router, permission, user, option)?.path).toBe('/system/user')
    permission.accessRoutes = []
    expect(await locateAssistantGuide(router, permission, user, option, 'add')).toBe('unavailable')
    expect(await locateAssistantGuide(router, permission, user, { currentScope: '/system/role' }, 'save')).toBe('unavailable')
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('业务菜单经原入口跳转后，在保留同一菜单身份的正式页面定位', async () => {
    const entry = '/app-center/app/100'
    const runtime = '/app-center/application/equipment/runtime'
    permission.menus = [{ key: '2', type: 'menu', label: '设备登记', path: entry }]
    permission.accessRoutes = [{ path: entry }]
    router.addRoute({ path: entry, component })
    router.addRoute({ path: runtime, component })
    router.beforeEach(to => to.path === entry ? { path: runtime, query: to.query } : undefined)
    const control = button('add')
    expect(await locateAssistantGuide(router, permission, user, { id: '2' }, 'add')).toBe('marked')
    expect(router.currentRoute.value.query.menuKey).toBe('2')
    expect(control.classList.contains('assistant-guide-highlight')).toBe(true)
  })

  it('略过隐藏和禁用控件，多个表单时优先当前弹窗', () => {
    const region = document.createElement('div')
    const control = button('save')
    document.body.append(region)
    const modalControl = button('save', region)
    region.hidden = true
    expect(findGuideElement('save')).toBe(control)
    region.hidden = false
    expect(findGuideElement('save')).toBeNull()
    region.setAttribute('role', 'dialog')
    expect(findGuideElement('save')).toBe(modalControl)
    modalControl.disabled = true
    expect(findGuideElement('save')).toBe(control)
    expect(findGuideElement('save"] button')).toBeNull()
  })
})
