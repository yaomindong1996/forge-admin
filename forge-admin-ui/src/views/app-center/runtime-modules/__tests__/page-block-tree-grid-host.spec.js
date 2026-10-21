import { describe, expect, it } from 'vitest'
import { findGridCellHostForChild } from '../page-block-tree'

describe('findGridCellHostForChild', () => {
  it('locates nest chart cell span inside root grid-layout', () => {
    const items = [{
      id: 'grid_1',
      blockType: 'grid-layout',
      props: {
        columns: 24,
        cells: [
          { key: 'metrics', span: 24, children: [{ id: 'stats_1', blockType: 'stats-strip' }] },
          { key: 'bar', span: 12, children: [{ id: 'chart_bar', blockType: 'bar-chart' }] },
          { key: 'line', span: 12, children: [{ id: 'chart_line', blockType: 'line-chart' }] },
        ],
      },
    }]
    expect(findGridCellHostForChild(items, 'chart_line')).toEqual({
      gridId: 'grid_1',
      cellKey: 'line',
      span: 12,
      columns: 24,
    })
    expect(findGridCellHostForChild(items, 'missing')).toBeNull()
  })
})
