import fs from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

const source = file => fs.readFileSync(path.resolve(process.cwd(), 'src', file), 'utf8')

function styleRule(css, selector) {
  const rules = [...css.replace(/\/\*[\s\S]*?\*\//g, '').matchAll(/([^{}]+)\{([^{}]*)\}/g)]
  const rule = rules.find(match => match[1].split(',').some(part => part.trim() === selector))
  expect(rule, `缺少状态样式 ${selector}`).toBeDefined()
  return rule[2]
}

describe('布局主题覆盖与工具接入契约', () => {
  it('通知表面没有独立绿色品牌或写死白底，审批按钮沿用 Naive 主题', () => {
    const content = source('layouts/components/MessageNotification.vue')
    expect(content).not.toMatch(/#(?:6a8355|627e4f|5f774c|526a42|e8f0fe|1a73e8)\b/i)
    expect(content).not.toMatch(/background:\s*#(?:fff|ffffff)\b/i)
    expect(content).not.toContain('--n-color:')
    expect(content).toContain('background: var(--bg-primary)')
    expect(content).toContain('color: var(--primary-color)')
  })
  it('nexus 暗色不再盖过导航 Token，布局不染色业务页', () => {
    const content = source('layouts/nexus/index.vue')
    expect(content).not.toMatch(/\.dark\s+\.nexus-(?:header|sidebar-inner)/)
    expect(content).not.toContain('.cache-management-page')
    expect(content).not.toContain('--primary-300:')
    expect(content).not.toContain(':root {')
    expect(content).toContain('background: var(--layout-header-bg-color)')
  })
  it('内容区使用语义表面色而非浅色灰度，避免深色切换残留白底', () => {
    for (const layout of ['normal', 'full', 'top-menu', 'top-side-menu']) {
      const content = source(`layouts/${layout}/index.vue`)
      expect(content).not.toContain('background: var(--gray-100)')
      expect(content).toContain('background: var(--bg-secondary)')
    }
  })
  it('标签页与全部菜单搜索含义分离，深色内容区选中文字可见', () => {
    const tabs = source('layouts/components/tab/index.vue')
    expect(tabs).toContain('aria-label="查找已打开的标签页"')
    expect(tabs).toContain('--forge-tab-accent: var(--text-primary)')
    expect(source('layouts/components/MenuSearch.vue')).toContain('aria-label="搜索全部菜单"')
  })
  it.each([
    'normal/header/index.vue',
    'full/header/index.vue',
    'nexus/header/index.vue',
    'immersive/header/index.vue',
    'business-workbench/components/WorkbenchHeader.vue',
    'top-menu/index.vue',
    'top-side-menu/index.vue',
  ])('%s 复用同一个工具区，不重复菜单搜索监听器', (file) => {
    const content = source(`layouts/${file}`)
    expect(content).toContain('<HeaderTools')
    expect(content).not.toContain('<MenuSearch')
  })
  it('工作台共用工具区保留身份入口，全屏不再藏进单项浮层', () => {
    const content = source('layouts/components/HeaderTools.vue')
    expect(content).toContain('<TenantSwitcher />')
    expect(content).toContain('<OrgSwitcher />')
    expect(content).toContain('<Fullscreen />')
    expect(content).not.toContain('aria-label="更多工具"')
    expect(content).not.toContain('<n-popover')
    expect(content).toContain('v-if="isCompact" placement="bottom-end"')
    expect(source('layouts/business-workbench/components/WorkbenchHeader.vue')).toContain('show-appearance')
  })

  it('无顶栏布局收纳账户与工具，便当盒不再另写注销/全屏逻辑', () => {
    const panel = source('layouts/components/CompactLayoutTools.vue')
    for (const component of ['TenantSwitcher', 'OrgSwitcher', 'AccountIdentity', 'ToggleTheme', 'Fullscreen']) {
      expect(panel).toContain(`<${component}`)
    }
    expect(panel).toContain('appStore.appearanceOpen = true')
    expect(panel).not.toContain('<UserAvatar')
    expect(panel).toContain('handleAccountAction(\'logout\')')
    expect(panel).toContain('handleAccountAction(\'profile\')')
    expect(source('layouts/simple/sidebar/index.vue')).toContain('<CompactLayoutTools')
    const rail = source('layouts/bento/components/BentoRail.vue')
    expect(rail).toContain('<CompactLayoutTools')
    expect(rail).not.toContain('requestFullscreen')
    expect(rail).not.toContain('userDropdownVisible')
  })

  it('简约窄屏入口和桌面侧栏互斥，Nexus 不再使用离屏菜单或拖拽条', () => {
    const simple = source('layouts/simple/index.vue')
    expect(simple).toContain('v-if="!isNarrow"')
    expect(simple).toContain('v-if="isNarrow"')
    expect(simple).toContain('<ResponsiveMenuToggle')
    expect(source('layouts/nexus/header/index.vue')).toContain('<ResponsiveMenuToggle')
    const nexus = source('layouts/nexus/index.vue')
    expect(nexus).not.toContain('translateX(-100%)')
    expect(nexus).not.toContain('nexus-expand-bar')
    expect(nexus).not.toContain('handleBarMouseDown')
  })

  it.each(['normal', 'full', 'top-menu', 'top-side-menu'])('%s 窄屏复用授权抽屉，不残留固定侧栏', (layout) => {
    const content = source(`layouts/${layout}/index.vue`)
    const header = ['normal', 'full'].includes(layout)
      ? source(`layouts/${layout}/header/index.vue`)
      : content
    expect(content).toContain('!isNarrow')
    expect(content).toContain('max-width: 768px')
    expect(content).not.toContain('translateX(-100%)')
    expect(header).toContain('<ResponsiveMenuToggle')
    expect(content).not.toContain('transition: width')
    if (['top-menu', 'top-side-menu'].includes(layout)) {
      expect(content).toContain('<TopMenu v-if="!isNarrow"')
    }
  })

  it('沉浸式 scoped 表面与租户前景一致，不用正文背景覆盖顶栏', () => {
    const header = source('layouts/immersive/header/index.vue')
    expect(header).toContain('background: var(--layout-header-bg-color)')
    expect(header).toContain('color: var(--layout-header-text-color)')
    expect(header).toContain('background: var(--layout-header-hover-color)')
    expect(header).not.toContain('background: var(--bg-primary)')
    expect(header).not.toContain('background: var(--bg-secondary)')
    expect(header).toMatch(/@media \(max-width: 1100px\)[\s\S]*?\.header-center\s*\{\s*display: none/)
  })

  it('便当盒通知和账户使用同一热区并消除包装层横向偏移', () => {
    const rail = source('layouts/bento/components/BentoRail.vue')
    expect(rail).toContain('.bento-tools :deep(.message-notification-wrapper)')
    expect(rail).toMatch(/\.bento-tools :deep\(\.message-notification-wrapper\)[\s\S]*?margin: 0/)
    expect(rail).toContain('.bento-tools :deep(.compact-tools-trigger)')
    expect(rail).toContain('width: 36px')
    expect(rail).toContain('font-size: 20px')
  })

  it.each(['full', 'simple'])('%s 展开侧栏至少 240px，收起仍沿用用户偏好', (layout) => {
    const content = source(`layouts/${layout}/index.vue`)
    expect(content).toContain('width: max(240px, var(--side-menu-width))')
    expect(content).toContain('width: var(--side-menu-collapsed-width)')
    expect(content).toContain('!isNarrow')
  })

  it('全屏顶栏保持 48px，不被父壳改回 60px', () => {
    expect(source('layouts/full/index.vue')).toContain('--layout-header-height: 48px')
    expect(source('layouts/full/index.vue')).toContain('height: 48px')
    expect(source('layouts/full/header/index.vue')).toContain('height: 48px')
  })

  it('移除全局指引，不遗留入口、状态或空工具浮层', () => {
    for (const file of [
      'App.vue',
      'store/modules/app.js',
      'layouts/components/HeaderTools.vue',
      'layouts/components/CompactLayoutTools.vue',
      'layouts/components/index.js',
    ]) {
      expect(source(file)).not.toMatch(/BeginnerGuide|OperationGuideDialog|guideOpen|show-guide/)
    }
  })

  it('顶部和混合共用横条 Token，工作台基础样式及覆盖规则也保持一致', () => {
    const token = '--top-menu-active-bar-color'
    expect(source('layouts/components/TopMenuBar.vue')).toContain(`background: var(${token}, var(--primary-color))`)
    const chrome = source('styles/layout-chrome.css')
    expect(chrome).toMatch(/business-mega-triggers > button::after\s*\{\s*background: var\(--top-menu-active-bar-color/)
    expect(source('layouts/business-workbench/workbench.css')).toContain(`background: var(${token}, var(--workbench-primary))`)
    for (const layout of ['top-menu', 'top-side-menu']) {
      expect(source(`layouts/${layout}/components/TopMenu.vue`)).toContain('<TopMenuBar')
    }
  })

  it('工作台隐藏横向滚动条但保留滚动容器、左右按钮和键盘定位', () => {
    const css = source('layouts/business-workbench/workbench.css')
    const track = css.match(/\.business-mega-triggers\s*\{([^}]+)\}/)[1]
    expect(track).toContain('overflow-x: auto')
    expect(track).toContain('scrollbar-width: none')
    expect(css).toMatch(/\.business-mega-triggers::-webkit-scrollbar\s*\{\s*display: none/)
    const header = source('layouts/business-workbench/components/WorkbenchHeader.vue')
    expect(header).toContain('@wheel="scrollMenuWithWheel"')
    expect(header).toContain('向右查看更多菜单')
    expect(header).toContain('向左查看更多菜单')
    expect(header).toContain('@keydown.right.prevent')
    expect(header).toContain('scrollIntoView')
  })

  it('工作台品牌强调色不借用可能为白色的侧栏选中文字色', () => {
    const panel = styleRule(source('styles/layout-chrome.css'), '.layout-chrome-header .business-mega-panel')
    expect(panel).toContain('--workbench-primary: var(--primary-color)')
    expect(panel).not.toContain('--workbench-primary: var(--side-menu-text-color-active)')
  })

  it.each([
    ['.mega-section-nav > button:hover', 'hover'],
    ['.mega-section-nav > button:focus-visible', 'hover'],
    ['.mega-section-nav > button.is-active', 'active'],
    ['.workbench-menu-link:hover', 'hover'],
    ['.workbench-menu-link:focus-visible', 'hover'],
    ['.workbench-menu-link.is-current', 'active'],
  ])('%s 前景和背景成对使用侧栏 %s 配色', (selector, state) => {
    const rule = styleRule(source('layouts/business-workbench/workbench.css'), selector)
    expect(rule).toContain(`color: var(--side-menu-text-color-${state},`)
    expect(rule).toContain(`background: var(--side-menu-bg-color-${state},`)
    expect(rule).not.toContain('color-mix')
  })

  it('选中状态后置以覆盖悬停/聚焦，箭头随父按钮状态而非品牌色', () => {
    const css = source('layouts/business-workbench/workbench.css')
    expect(css.indexOf('.workbench-menu-link.is-current {'))
      .toBeGreaterThan(css.indexOf('.workbench-menu-link:focus-visible {'))
    expect(css.indexOf('.mega-section-nav > button.is-active {'))
      .toBeGreaterThan(css.indexOf('.mega-section-nav > button:focus-visible {'))
    expect(styleRule(css, '.mega-section-nav__arrow')).toContain('color: currentColor')
    expect(source('styles/layout-chrome.css')).not.toContain('button:is(:hover, .is-active)')
  })
})
