import { describe, expect, it } from 'vitest'
import { findDirectParentPageBlock, findGridCellHostForChild } from '../page-block-tree'

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

describe('findDirectParentPageBlock', () => {
  it('resolves card parent for chart nested in grid cell card', () => {
    const items = [{
      id: 'grid_1',
      blockType: 'grid-layout',
      props: {
        cells: [{
          key: 'c1',
          span: 12,
          children: [{
            id: 'card_1',
            blockType: 'card',
            children: [{ id: 'chart_1', blockType: 'bar-chart' }],
          }],
        }],
      },
    }]
    expect(findDirectParentPageBlock(items, 'chart_1')?.id).toBe('card_1')
    expect(findDirectParentPageBlock(items, 'card_1')?.id).toBe('grid_1')
    expect(findDirectParentPageBlock(items, 'grid_1')).toBeNull()
  })
})
