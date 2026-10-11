import { readFileSync } from 'node:fs'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { compileStyle, parse } from 'vue/compiler-sfc'
import ForgeSymbol from '@/components/common/ForgeSymbol.vue'
import { forgeSymbolColorways } from '@/components/common/forgeSymbolColors'
import { forgeSymbols } from '@/components/common/forgeSymbols'
import IconRenderer from '@/components/IconRenderer.vue'
import { usePermissionStore } from '@/store/modules/permission'
import { resolveNavigationIcon, withUnifiedNavigationIcons } from '../navigation-icons'

vi.mock('@/utils', () => ({ isExternal: value => /^https?:/.test(value || '') }))
vi.mock('@/components/common/AuthImage.vue', () => ({ default: { template: '<img data-auth-image>' } }))

beforeEach(() => setActivePinia(createPinia()))

function readSymbolSource(relativePath) {
  // 与现有结构测试一致：参数形式避免 Vite 将源码路径转换为浏览器资源 URL。
  return readFileSync(new URL(relativePath, import.meta.url), 'utf8')
}

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
    expect(resolveNavigationIcon({ name: 'AI 能力' })).toBe('forge-color:ai')
    expect(resolveNavigationIcon({ name: '平台新名称', path: '/platform' })).toBe('forge-color:platform')
    expect(resolveNavigationIcon({ name: '数据新名称', path: '/data-report' })).toBe('forge-color:analytics')
    expect(resolveNavigationIcon({ path: '/system/collaboration' })).toBe('forge-color:collaboration')
    expect(resolveNavigationIcon({ name: '插件中心', path: '/system/plugin' })).toBe('forge-color:plugins')
    expect(resolveNavigationIcon({ path: '/custom' })).toBe('forge-color:module')
    expect(resolveNavigationIcon({ icon: 'forge:__proto__' })).toBe('forge-color:module')
  })

  it('显示适配不修改原始数据，子菜单图标和权限/链接原样保留', () => {
    const child = { id: 'leaf', path: '/system/user', icon: 'PeopleOutline', perms: 'system:user:list' }
    const source = [{ id: 'root', path: '/system', icon: 'old', children: [child], order: 3 }]
    const result = withUnifiedNavigationIcons(source)
    expect(source[0].icon).toBe('old')
    expect(result[0]).toMatchObject({ id: 'root', path: '/system', order: 3, icon: 'forge-color:platform' })
    expect(result[0].children[0]).toBe(child)
    expect(withUnifiedNavigationIcons([{ type: 'subapp', children: source }])[0].children[0].icon)
      .toBe('forge-color:platform')
  })

  it('现行菜单接口只替换顶级展示图标，隐藏/按钮项仍被过滤', () => {
    const root = { id: 1, resourceType: 1, resourceName: '平台管理', path: '/system', icon: 'old', children: [
      { id: 2, resourceType: 2, resourceName: '用户管理', path: '/system/user', icon: 'PeopleOutline' },
      { id: 3, resourceType: 2, resourceName: '隐藏项', visible: 0 },
      { id: 4, resourceType: 3, resourceName: '按钮' },
    ] }
    const store = usePermissionStore()
    store.setMenuData([root])
    expect(store.menus[0].icon).toBe('forge-color:platform')
    expect(store.menus[0].children).toHaveLength(1)
    expect(store.menus[0].children[0].icon).toBe('PeopleOutline')
    expect(root.icon).toBe('old')
  })

  it('旧权限接口同样适配顶级导航，保持原始配置', () => {
    const permission = { id: 1, type: 'MENU', name: '首页', code: 'home', path: '/home', icon: 'old', show: true }
    const store = usePermissionStore()
    store.setPermissions([permission])
    expect(store.menus[0].icon).toBe('forge-color:home')
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

  it('旧 forge 配置迁移到多色模式，重复适配幂等且不改变子级', () => {
    const child = { path: '/custom/leaf', icon: 'forge:shield' }
    const source = [{ path: '/custom', icon: 'forge:database', children: [child] }]
    const once = withUnifiedNavigationIcons(source)
    expect(once[0].icon).toBe('forge-color:database')
    expect(withUnifiedNavigationIcons(once)).toEqual(once)
    expect(once[0].children[0]).toBe(child)
    expect(resolveNavigationIcon({ icon: 'forge-color:__proto__' })).toBe('forge-color:module')
  })

  it('多色图层覆盖所有白名单符号，使用固定双色而不继承菜单选中文字色', () => {
    expect(Object.keys(forgeSymbolColorways).sort()).toEqual(Object.keys(forgeSymbols).sort())
    const primaryColors = new Set()
    for (const name of Object.keys(forgeSymbols)) {
      const wrapper = mount(IconRenderer, { props: { icon: `forge-color:${name}`, size: 20, color: 'white' } })
      const svg = wrapper.get('svg')
      const strokes = new Set(wrapper.findAll('path').map(path => path.attributes('stroke')))
      expect(strokes).toEqual(new Set(['var(--forge-icon-primary)', 'var(--forge-icon-secondary)']))
      expect(svg.classes()).toContain('forge-symbol--multicolor')
      expect(svg.attributes('width')).toBe('20')
      expect(svg.element.style.getPropertyValue('--forge-icon-main-dark')).toMatch(/^#[\da-f]{6}$/i)
      primaryColors.add(svg.element.style.getPropertyValue('--forge-icon-main-light'))
      expect(wrapper.find('img').exists()).toBe(false)
      wrapper.unmount()
    }
    expect(primaryColors.size).toBe(6)
  })

  it('切回普通符号移除调色板，不污染插件/关于系统和二级图标', async () => {
    const wrapper = mount(IconRenderer, { props: { icon: 'forge-color:ai', color: 'white' } })
    await wrapper.setProps({ icon: 'forge:ai' })
    const svg = wrapper.get('svg')
    expect(svg.classes()).not.toContain('forge-symbol--multicolor')
    expect(svg.element.style.getPropertyValue('--forge-icon-main-light')).toBe('')
    expect(wrapper.findAll('path').every(path => !path.attributes('stroke'))).toBe(true)
    expect(svg.attributes('stroke')).toBe('currentColor')
    wrapper.unmount()
  })

  it('多色未知符号只使用安全回退路径', () => {
    const wrapper = mount(IconRenderer, { props: { icon: 'forge-color:<script>alert(1)</script>' } })
    expect(wrapper.get('svg').attributes('data-symbol')).toBe('module')
    expect(wrapper.findAll('path')).toHaveLength(forgeSymbols.module.length)
    expect(wrapper.find('script').exists()).toBe(false)
    expect(wrapper.find('img').exists()).toBe(false)
    wrapper.unmount()
  })

  it('深色样式编译后仍定位图标自身，而不是只把调色板写到根节点', () => {
    const { descriptor } = parse(readSymbolSource('../../components/common/ForgeSymbol.vue'))
    const result = compileStyle({ source: descriptor.styles[0].content, id: 'data-v-icon', scoped: true })
    expect(result.errors).toHaveLength(0)
    expect(result.code).toContain('.dark .forge-symbol--multicolor[data-v-icon]')
    expect(result.code).toContain('--forge-icon-primary: var(--forge-icon-main-dark)')
  })
})
