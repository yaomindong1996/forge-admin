import { readFileSync } from 'node:fs'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { downloadDay, downloadFileType, downloadScope, downloadTime, hasDownloadDock, progressPercentage, readHiddenDownloads } from '../recentDownloadsUtils'

afterEach(() => vi.useRealTimers())
describe('下载展示工具与布局边界', () => {
  it('时间格式化和分组，不泄漏 ISO T；非法日期不显示 Invalid Date', () => {
    vi.useFakeTimers().setSystemTime(new Date(2026, 9, 17, 12))
    expect(downloadDay('2026-10-17T09:12:00')).toBe('今天')
    expect(downloadDay('2026-10-16T09:12:00')).toBe('更早')
    expect(downloadTime('2026-10-17T09:12:00')).toBe('09:12')
    expect(downloadTime('invalid')).toBe('时间未知')
  })
  it('未知总大小不显示百分比，超界进度收敛', () => {
    expect(progressPercentage({ loaded: 50 })).toBeNull()
    expect(progressPercentage({ loaded: 50, total: 100 })).toBe(50)
    expect(progressPercentage({ loaded: 120, total: 100 })).toBe(100)
    expect(downloadFileType('合同.PDF').tone).toBe('pdf')
    expect(downloadFileType('附件.XLSX').tone).toBe('sheet')
  })
  it('身份缺失不建立共享缓存；脏缓存安全忽略', () => {
    expect(downloadScope(null, 1)).toBe('')
    localStorage.setItem('forge-recent-downloads-hidden:scope', 'invalid-json')
    expect(readHiddenDownloads('scope')).toEqual([])
  })
  it.each(['normal', 'simple', 'full', 'top-side-menu', 'bento', 'nexus', 'side-flyout'])(
    '%s 侧栏存在时使用嵌入入口',
    (layout) => {
      expect(hasDownloadDock(layout, { narrow: false, collapsed: false, showSidebar: true })).toBe(true)
    },
  )
  it.each(['normal', 'simple', 'full', 'top-side-menu', 'nexus', 'side-flyout'])(
    '%s 窄屏隐藏侧栏后必须启用兜底入口',
    (layout) => {
      expect(hasDownloadDock(layout, { narrow: true, collapsed: false, showSidebar: true })).toBe(false)
    },
  )
  it('全屏/顶侧无侧栏以及收起的沉浸式侧栏启用兜底', () => {
    for (const layout of ['full', 'top-side-menu'])
      expect(hasDownloadDock(layout, { narrow: false, collapsed: false, showSidebar: false })).toBe(false)
    for (const layout of ['nexus', 'side-flyout'])
      expect(hasDownloadDock(layout, { narrow: false, collapsed: true, showSidebar: true })).toBe(false)
    expect(hasDownloadDock('business-workbench', {})).toBe(false)
  })

  it('隐藏的浮岛侧栏不保留第二份浮层，入口判断与实际布局归一化保持一致', () => {
    const source = relative => readFileSync(new URL(relative, import.meta.url), 'utf8')
    expect(source('../../../layouts/nexus/index.vue')).toContain('!appStore.collapsed && !isNarrow')
    expect(source('../../../App.vue')).toContain('resolveShellLayoutName')
    expect(source('../../../App.vue')).toContain('activeShellLayout')
  })
})
