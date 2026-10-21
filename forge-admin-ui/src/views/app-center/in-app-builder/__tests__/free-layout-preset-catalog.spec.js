import { describe, expect, it } from 'vitest'
import {
  buildFreeLayoutPresetGrid,
  freeLayoutPresetCatalog,
  resolveFreeLayoutPreset,
} from '../free-layout-preset-catalog'

describe('free-layout-preset-catalog', () => {
  it('exposes three grid-based presets', () => {
    expect(freeLayoutPresetCatalog.map(item => item.key)).toEqual([
      'ops-overview',
      'entry-workbench',
      'analytics-board',
    ])
  })

  it.each(freeLayoutPresetCatalog.map(item => item.key))(
    'builds %s as a single full-width grid-layout with components in cells',
    (key) => {
      const layout = buildFreeLayoutPresetGrid(key, { pageTitle: '演示页' })
      expect(layout).toBeTruthy()
      expect(layout.layoutType).toBe('custom')
      expect(layout.items).toHaveLength(1)
      const root = layout.items[0]
      expect(root.blockType).toBe('grid-layout')
      expect(root.props.columns).toBe(24)
      expect(root.props.showCellBorder).toBe(false)
      expect(root.props.style.widthMode).toBe('full')
      expect(root.props.style.heightMode).toBe('auto')
      expect(Array.isArray(root.props.cells)).toBe(true)
      expect(root.props.cells.length).toBeGreaterThan(1)
      const spans = root.props.cells.map(cell => cell.span)
      expect(spans.every(span => span >= 1 && span <= 24)).toBe(true)
      const children = root.props.cells.flatMap(cell => cell.children || [])
      expect(children.length).toBeGreaterThan(1)
      children.forEach((child) => {
        expect(child.blockType).toBeTruthy()
        expect(child.props?.style?.widthMode).toBe('full')
      })
    },
  )

  it('ops-overview places stats, quick-entry and twin charts', () => {
    const layout = buildFreeLayoutPresetGrid('ops-overview')
    const types = layout.items[0].props.cells.flatMap(cell => (cell.children || []).map(child => child.blockType))
    expect(types).toEqual(['stats-strip', 'quick-entry', 'bar-chart', 'line-chart'])
    expect(layout.items[0].props.cells.map(cell => cell.span)).toEqual([24, 24, 12, 12])
  })

  it('keeps chart nest shells tall enough so canvases do not overflow into neighbors', () => {
    const layout = buildFreeLayoutPresetGrid('ops-overview')
    const charts = layout.items[0].props.cells
      .flatMap(cell => cell.children || [])
      .filter(child => ['bar-chart', 'line-chart'].includes(child.blockType))
    expect(charts.length).toBe(2)
    charts.forEach((chart) => {
      expect(chart.props.style.heightMode).toBe('fixed')
      expect(Number(chart.props.style.height)).toBeGreaterThanOrEqual(280)
      expect(Number(chart.props.style.minHeight)).toBeGreaterThanOrEqual(280)
      expect(Number(chart.props.height)).toBeGreaterThanOrEqual(260)
      expect(chart.props.style.height).not.toBe('100%')
    })
  })

  it('entry-workbench includes list block for data binding', () => {
    const layout = buildFreeLayoutPresetGrid('entry-workbench', { pageTitle: '请假中心' })
    const types = layout.items[0].props.cells.flatMap(cell => (cell.children || []).map(child => child.blockType))
    expect(types).toContain('quick-entry')
    expect(types).toContain('AiCrudPage')
    expect(types).toContain('page-title')
  })

  it('analytics-board uses full-width trend then split distribution', () => {
    const layout = buildFreeLayoutPresetGrid('analytics-board')
    const cells = layout.items[0].props.cells
    expect(cells.find(cell => cell.key === 'trend')?.span).toBe(24)
    expect(cells.find(cell => cell.key === 'bar')?.span).toBe(16)
    expect(cells.find(cell => cell.key === 'note')?.span).toBe(8)
  })

  it('blank / unknown preset resolves to null', () => {
    expect(resolveFreeLayoutPreset('blank')).toBeNull()
    expect(resolveFreeLayoutPreset('')).toBeNull()
    expect(buildFreeLayoutPresetGrid('nope')).toBeNull()
  })

  it('ships demo metrics / chart categories so presets look usable out of the box', () => {
    const ops = buildFreeLayoutPresetGrid('ops-overview')
    const stats = ops.items[0].props.cells
      .flatMap(cell => cell.children || [])
      .find(child => child.blockType === 'stats-strip')
    expect(stats.props.metrics.length).toBeGreaterThanOrEqual(4)
    const bar = ops.items[0].props.cells
      .flatMap(cell => cell.children || [])
      .find(child => child.blockType === 'bar-chart')
    expect(bar.props.categories.length).toBeGreaterThan(0)
    expect(bar.props.series[0].data.length).toBe(bar.props.categories.length)

    const entry = buildFreeLayoutPresetGrid('entry-workbench')
    const quick = entry.items[0].props.cells
      .flatMap(cell => cell.children || [])
      .find(child => child.blockType === 'quick-entry')
    expect(quick.props.items.length).toBe(4)
    expect(quick.props.items.every(item => item.title && item.icon)).toBe(true)
  })

  it('keeps each row span sum within 24 columns', () => {
    for (const key of freeLayoutPresetCatalog.map(item => item.key)) {
      const cells = buildFreeLayoutPresetGrid(key).items[0].props.cells
      let row = 0
      for (const cell of cells) {
        if (row + cell.span > 24)
          row = 0
        row += cell.span
        expect(row).toBeLessThanOrEqual(24)
        if (row === 24)
          row = 0
      }
    }
  })
})
