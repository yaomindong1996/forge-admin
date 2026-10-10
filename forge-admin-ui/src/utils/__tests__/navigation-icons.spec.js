import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ForgeSymbol from '@/components/common/ForgeSymbol.vue'
import { forgeSymbols } from '@/components/common/forgeSymbols'
import IconRenderer from '@/components/IconRenderer.vue'
import { usePermissionStore } from '@/store/modules/permission'
import { resolveNavigationIcon, withUnifiedNavigationIcons } from '../navigation-icons'

vi.mock('@/utils', () => ({ isExternal: value => /^https?:/.test(value || '') }))
vi.mock('@/components/common/AuthImage.vue', () => ({ default: { template: '<img data-auth-image>' } }))

beforeEach(() => setActivePinia(createPinia()))

describe('一级导航统一符号', () => {
  it('内置一级入口按语义区分，名称/排序变化不改变路径语义', () => {
    const paths = [
      '/home',
      '/app-center',
      '/flow',
      '/data',
      '/ai',
      '/system',
      '/collaboration',
      '/open-platform',
      '/system/plugin',
      '/generator',
      '/monitor',
      '/message',
      '/print',
      '/job',
    ]
    const icons = paths.map(path => resolveNavigationIcon({ path, icon: 'local-image:icon01.png' }))
    expect(new Set(icons).size).toBe(paths.length)
    expect(resolveNavigationIcon({ name: 'AI 能力' })).toBe('forge:ai')
    expect(resolveNavigationIcon({ name: '平台新名称', path: '/platform' })).toBe('forge:platform')
    expect(resolveNavigationIcon({ name: '数据新名称', path: '/data-report' })).toBe('forge:analytics')
    expect(resolveNavigationIcon({ path: '/system/collaboration' })).toBe('forge:collaboration')
    expect(resolveNavigationIcon({ name: '插件中心', path: '/system/plugin' })).toBe('forge:plugins')
    expect(resolveNavigationIcon({ path: '/custom' })).toBe('forge:module')
    expect(resolveNavigationIcon({ icon: 'forge:__proto__' })).toBe('forge:module')
  })

  it('显示适配不修改原始数据，子菜单图标和权限/链接原样保留', () => {
    const child = { id: 'leaf', path: '/system/user', icon: 'PeopleOutline', perms: 'system:user:list' }
    const source = [{ id: 'root', path: '/system', icon: 'old', children: [child], order: 3 }]
    const result = withUnifiedNavigationIcons(source)
    expect(source[0].icon).toBe('old')
    expect(result[0]).toMatchObject({ id: 'root', path: '/system', order: 3, icon: 'forge:platform' })
    expect(result[0].children[0]).toBe(child)
    expect(withUnifiedNavigationIcons([{ type: 'subapp', children: source }])[0].children[0].icon)
      .toBe('forge:platform')
  })

  it('现行菜单接口只替换顶级展示图标，隐藏/按钮项仍被过滤', () => {
    const root = { id: 1, resourceType: 1, resourceName: '平台管理', path: '/system', icon: 'old', children: [
      { id: 2, resourceType: 2, resourceName: '用户管理', path: '/system/user', icon: 'PeopleOutline' },
      { id: 3, resourceType: 2, resourceName: '隐藏项', visible: 0 },
      { id: 4, resourceType: 3, resourceName: '按钮' },
    ] }
    const store = usePermissionStore()
    store.setMenuData([root])
    expect(store.menus[0].icon).toBe('forge:platform')
    expect(store.menus[0].children).toHaveLength(1)
    expect(store.menus[0].children[0].icon).toBe('PeopleOutline')
    expect(root.icon).toBe('old')
  })

  it('旧权限接口同样适配顶级导航，保持原始配置', () => {
    const permission = { id: 1, type: 'MENU', name: '首页', code: 'home', path: '/home', icon: 'old', show: true }
    const store = usePermissionStore()
    store.setPermissions([permission])
    expect(store.menus[0].icon).toBe('forge:home')
    expect(permission.icon).toBe('old')
  })

  it('统一 SVG 使用当前文字色、同网格/线宽，支持尺寸且不请求旧图片', () => {
    for (const name of Object.keys(forgeSymbols)) {
      const wrapper = mount(IconRenderer, { props: { icon: `forge:${name}`, size: 22, color: 'white' } })
      const svg = wrapper.get('svg')
      expect(svg.attributes()).toMatchObject({
        'viewBox': '0 0 24 24',
        'stroke': 'currentColor',
        'stroke-width': '1.7',
        'width': '22',
        'height': '22',
        'aria-hidden': 'true',
      })
      expect(svg.attributes('style')).toContain('color: white')
      expect(wrapper.find('img').exists()).toBe(false)
      wrapper.unmount()
    }
    const fallback = mount(ForgeSymbol, { props: { name: '<script>alert(1)</script>' } })
    expect(fallback.attributes('data-symbol')).toBe('module')
    expect(fallback.find('script').exists()).toBe(false)
    fallback.unmount()
  })
})
