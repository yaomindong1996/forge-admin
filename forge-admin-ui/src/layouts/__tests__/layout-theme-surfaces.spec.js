import fs from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

const source = file => fs.readFileSync(path.resolve(process.cwd(), 'src', file), 'utf8')

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
  it('工作台共用工具区保留身份入口，低频工具进入浮层', () => {
    const content = source('layouts/components/HeaderTools.vue')
    expect(content).toContain('<TenantSwitcher />')
    expect(content).toContain('<OrgSwitcher />')
    expect(content).toContain('aria-label="更多工具"')
    expect(content).toContain('<Fullscreen with-label />')
    expect(content).toContain('<BeginnerGuide with-label />')
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
})
