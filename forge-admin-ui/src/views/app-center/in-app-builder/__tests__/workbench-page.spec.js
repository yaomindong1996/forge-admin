import { describe, expect, it } from 'vitest'
import {
  createDefaultWorkbenchPage,
  ensureWorkbenchPageInBuilder,
  isWorkbenchPageId,
  WORKBENCH_LAYOUT_VERSION,
  WORKBENCH_PAGE_ID,
} from '../workbench-page'

describe('workbench-page', () => {
  it('recognizes the system workbench page id', () => {
    expect(isWorkbenchPageId(WORKBENCH_PAGE_ID)).toBe(true)
    expect(isWorkbenchPageId('page_home')).toBe(false)
  })

  it('creates a richer default layout for all apps', () => {
    const page = createDefaultWorkbenchPage()
    const items = page.layout.gridLayout.items
    expect(page.layout.workbenchLayoutVersion).toBe(WORKBENCH_LAYOUT_VERSION)
    expect(items.map(item => item.blockType)).toEqual([
      'workspace-summary-metrics',
      'info-panel',
    ])
    const metrics = items.find(item => item.blockType === 'workspace-summary-metrics')
    expect(Number(metrics.props.style.pageFlowHeight)).toBeGreaterThanOrEqual(200)
    expect(metrics.props.style.heightMode).toBe('auto')
  })

  it('upgrades v3 default layout that still has empty-state filler', () => {
    const legacy = {
      pages: {
        [WORKBENCH_PAGE_ID]: {
          title: '个人工作台',
          layout: {
            workbenchLayoutVersion: 3,
            gridLayout: {
              items: [
                { id: 'm1', blockType: 'workspace-summary-metrics', props: { style: { pageFlowHeight: 200, heightMode: 'auto' } } },
                { id: 't1', blockType: 'info-panel', props: { title: '使用提示' } },
                { id: 'e1', blockType: 'empty-state', props: { title: '从这里开始' } },
              ],
            },
          },
        },
      },
    }
    const result = ensureWorkbenchPageInBuilder(legacy)
    expect(result.upgraded).toBe(true)
    expect(result.page.layout.gridLayout.items.map(item => item.blockType)).toEqual([
      'workspace-summary-metrics',
      'info-panel',
    ])
    expect(result.page.layout.workbenchLayoutVersion).toBe(WORKBENCH_LAYOUT_VERSION)
  })

  it('upgrades legacy short metrics-only layout', () => {
    const legacy = {
      pages: {
        [WORKBENCH_PAGE_ID]: {
          title: '个人工作台',
          layout: {
            gridLayout: {
              items: [{
                id: 'm1',
                blockType: 'workspace-summary-metrics',
                props: {
                  style: { pageFlowHeight: 148, height: 148, heightMode: 'fixed' },
                  visibleKeys: ['todo', 'done', 'started', 'cc'],
                },
              }],
            },
          },
        },
      },
    }
    const result = ensureWorkbenchPageInBuilder(legacy)
    expect(result.upgraded).toBe(true)
    expect(result.page.layout.gridLayout.items.length).toBeGreaterThan(1)
    expect(result.page.layout.workbenchLayoutVersion).toBe(WORKBENCH_LAYOUT_VERSION)
  })

  it('keeps customized workbench pages intact', () => {
    const custom = {
      pages: {
        [WORKBENCH_PAGE_ID]: {
          title: '个人工作台',
          layout: {
            workbenchLayoutVersion: WORKBENCH_LAYOUT_VERSION,
            gridLayout: {
              items: [
                { id: 'a', blockType: 'card', props: { style: { pageFlowHeight: 120 } } },
                { id: 'b', blockType: 'workspace-summary-metrics', props: { style: { pageFlowHeight: 220, heightMode: 'auto' } } },
              ],
            },
          },
        },
      },
    }
    const result = ensureWorkbenchPageInBuilder(custom)
    expect(result.created).toBe(false)
    expect(result.page.layout.gridLayout.items).toHaveLength(2)
    expect(result.page.layout.gridLayout.items[0].blockType).toBe('card')
  })

  it('strips the default canvas page-title and shifts remaining blocks up', () => {
    const withTitle = {
      pages: {
        [WORKBENCH_PAGE_ID]: {
          title: '个人工作台',
          layout: {
            workbenchLayoutVersion: 2,
            gridLayout: {
              items: [
                {
                  id: 't1',
                  blockType: 'page-title',
                  props: { title: '个人工作台', style: { pageFlowY: 20, pageFlowHeight: 88 } },
                },
                {
                  id: 'm1',
                  blockType: 'workspace-summary-metrics',
                  props: { style: { pageFlowY: 124, pageFlowHeight: 220, heightMode: 'auto' } },
                },
                {
                  id: 'i1',
                  blockType: 'info-panel',
                  props: { style: { pageFlowY: 360, pageFlowHeight: 112, heightMode: 'auto' } },
                },
              ],
            },
          },
        },
      },
    }
    const result = ensureWorkbenchPageInBuilder(withTitle)
    const items = result.page.layout.gridLayout.items
    expect(items.map(item => item.blockType)).toEqual([
      'workspace-summary-metrics',
      'info-panel',
    ])
    expect(Number(items[0].props.style.pageFlowY)).toBe(20)
    expect(Number(items[1].props.style.pageFlowY)).toBe(256)
    expect(result.page.layout.workbenchLayoutVersion).toBe(WORKBENCH_LAYOUT_VERSION)
  })
})
