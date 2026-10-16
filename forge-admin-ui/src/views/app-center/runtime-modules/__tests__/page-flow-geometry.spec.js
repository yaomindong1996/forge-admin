import { describe, expect, it } from 'vitest'
import {
  applyMeasuredPageBlockHeights,
  migratePageFlowToContentCoords,
  normalizePagePadding,
  resolveDefaultPageBlockHeight,
  resolvePageBlockFlowGeometry,
  resolvePageBlockShellStyle,
  resolvePagePaddingCss,
  resolveRootPageBlockCollisions,
} from '../page-flow-geometry'

describe('page-flow-geometry', () => {
  it('uses compact defaults for title-like blocks', () => {
    expect(resolveDefaultPageBlockHeight({ blockType: 'text-title' })).toBe(56)
    expect(resolveDefaultPageBlockHeight({ blockType: 'page-title' })).toBe(72)
    expect(resolveDefaultPageBlockHeight({ blockType: 'info-panel' })).toBe(88)
  })

  it('keeps list CRUD default height aligned with portal runtime', () => {
    expect(resolveDefaultPageBlockHeight({ blockType: 'AiCrudPage' })).toBe(560)
    expect(resolveDefaultPageBlockHeight({ blockType: 'AiTable' })).toBe(560)
    expect(resolveDefaultPageBlockHeight({ blockType: 'search-form' })).toBe(220)
  })

  it('keeps fixed-height shells shrinkable and in sync with anchors', () => {
    const block = {
      id: 't1',
      blockType: 'text-title',
      props: {
        style: {
          widthMode: 'full',
          heightMode: 'fixed',
          pageFlowHeight: 48,
          pageFlowY: 20,
        },
      },
    }
    const style = resolvePageBlockShellStyle(block, [block], { pageId: 'page_1' })
    expect(style.height).toBe('48px')
    expect(style.minHeight).toBe('48px')
    expect(style.position).toBe('relative')
    expect(style.overflow).toBe('visible')
  })

  it('lets container auto-height grow with content while keeping minHeight', () => {
    const block = {
      id: 'g1',
      blockType: 'grid-layout',
      props: {
        style: {
          widthMode: 'full',
          heightMode: 'auto',
          pageFlowHeight: 180,
        },
      },
    }
    const style = resolvePageBlockShellStyle(block, [block])
    expect(style.height).toBe('auto')
    expect(style.minHeight).toBe('180px')
    expect(style.overflow).toBe('visible')
  })

  it('keeps grid containers content-sized even when heightMode is fixed', () => {
    const block = {
      id: 'g2',
      blockType: 'grid-layout',
      props: {
        style: {
          widthMode: 'full',
          heightMode: 'fixed',
          pageFlowHeight: 200,
          pageFlowX: 0,
          pageFlowY: 40,
        },
      },
    }
    const style = resolvePageBlockShellStyle(block, [block], { pageId: 'custom_page' })
    expect(style.height).toBe('auto')
    expect(style.minHeight).toBe('200px')
    expect(style.overflow).toBe('visible')
  })

  it('positions full-width shells inside canvas padding without double inset', () => {
    const block = {
      id: 't2',
      blockType: 'text-title',
      props: {
        style: {
          widthMode: 'full',
          heightMode: 'fixed',
          pageFlowHeight: 48,
          pageFlowY: 20,
        },
      },
    }
    const style = resolvePageBlockShellStyle(block, [{
      ...block,
      props: { style: { ...block.props.style, widthMode: 'fixed', pageFlowWidth: '400px' } },
    }, block], { pagePadding: { top: 12, right: 12, bottom: 12, left: 12 } })
    // mixed widthMode disables flow-stack → absolute；通栏吃 pagePadding
    expect(style.left).toBe('12px')
    expect(style.width).toBe('calc(100% - 24px)')
  })

  it('applies top/bottom padding via content-relative coords', () => {
    const block = {
      id: 't3',
      blockType: 'text-title',
      props: {
        style: {
          widthMode: 'full',
          heightMode: 'fixed',
          pageFlowHeight: 48,
          pageFlowX: 0,
          pageFlowY: 16,
        },
      },
    }
    const peer = {
      id: 't4',
      blockType: 'text-title',
      props: { style: { widthMode: 'fixed', pageFlowWidth: '320px', pageFlowX: 40, pageFlowY: 16 } },
    }
    const style = resolvePageBlockShellStyle(block, [peer, block], {
      pagePadding: { top: 40, right: 24, bottom: 32, left: 24 },
      pageFlowCoordSpace: 'content',
    })
    expect(style.top).toBe('56px') // 40 + 16
    expect(style.left).toBe('24px')
  })

  it('migrates canvas coords to content coords', () => {
    const layout = migratePageFlowToContentCoords({
      pagePadding: { top: 24, right: 24, bottom: 24, left: 24 },
      items: [{
        id: 'a',
        blockType: 'text-title',
        props: { style: { widthMode: 'full', pageFlowX: 24, pageFlowY: 48 } },
      }],
    })
    expect(layout.pageFlowCoordSpace).toBe('content')
    expect(layout.items[0].props.style.pageFlowX).toBe(0)
    expect(layout.items[0].props.style.pageFlowY).toBe(24)
  })

  it('floors free-layout list CRUD shell width and height so preview is usable', () => {
    const block = {
      id: 'crud',
      blockType: 'AiCrudPage',
      props: {
        style: {
          widthMode: 'fixed',
          heightMode: 'fixed',
          width: 360,
          height: 200,
          pageFlowWidth: '360px',
          pageFlowHeight: 200,
          pageFlowX: 0,
          pageFlowY: 0,
        },
      },
    }
    const style = resolvePageBlockShellStyle(block, [block], {
      pagePadding: { top: 8, right: 8, bottom: 8, left: 8 },
      pageFlowCoordSpace: 'content',
    })
    expect(style.width).toMatch(/^min\(640px/)
    expect(style.height).toBe('420px')
    expect(style.minHeight).toBe('420px')
  })

  it('normalizes page padding defaults and css', () => {
    expect(normalizePagePadding(null)).toEqual({ top: 8, right: 8, bottom: 8, left: 8 })
    expect(normalizePagePadding(12)).toEqual({ top: 12, right: 12, bottom: 12, left: 12 })
    expect(normalizePagePadding({ top: 8, left: 16 })).toEqual({ top: 8, right: 8, bottom: 8, left: 16 })
    expect(resolvePagePaddingCss({ top: 8, right: 12, bottom: 16, left: 20 })).toBe('8px 12px 16px 20px')
  })

  it('collision geometry prefers the larger of pageFlowHeight and height for grow blocks', () => {
    const tabs = {
      id: 'tabs_1',
      blockType: 'tabs',
      props: {
        style: {
          widthMode: 'full',
          heightMode: 'auto',
          pageFlowX: 0,
          pageFlowY: 100,
          pageFlowHeight: 180,
          height: 252,
        },
      },
    }
    const geo = resolvePageBlockFlowGeometry(tabs, 0, [tabs])
    expect(geo.height).toBe(252)
    expect(geo.bottom).toBe(352)
  })

  it('measured heights sync then collision pushes the lower block down', () => {
    const tabs = {
      id: 'tabs_1',
      blockType: 'tabs',
      props: {
        style: {
          widthMode: 'full',
          heightMode: 'auto',
          pageFlowX: 0,
          pageFlowY: 100,
          pageFlowHeight: 180,
          height: 180,
        },
      },
    }
    const grid = {
      id: 'grid_1',
      blockType: 'grid-layout',
      props: {
        style: {
          widthMode: 'full',
          heightMode: 'auto',
          pageFlowX: 0,
          pageFlowY: 296,
          pageFlowHeight: 180,
          height: 180,
        },
      },
    }
    const measured = applyMeasuredPageBlockHeights([tabs, grid], new Map([['tabs_1', 250]]))
    expect(measured.changed).toBe(true)
    expect(measured.items[0].props.style.pageFlowHeight).toBe(250)
    const resolved = resolveRootPageBlockCollisions(measured.items, 'tabs_1')
    expect(resolved[1].props.style.pageFlowY).toBe(366) // 100 + 250 + 16
  })
})
