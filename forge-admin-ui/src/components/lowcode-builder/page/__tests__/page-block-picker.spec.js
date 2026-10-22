import { describe, expect, it } from 'vitest'
import {
  DEFAULT_NEST_EXCLUDE_BLOCK_TYPES,
  FREQUENT_PAGE_BLOCK_TYPES,
  PAGE_BLOCK_PICKER_GROUPS,
  buildPageBlockPickerGroups,
  listInsertablePageBlocks,
  resolvePageBlockPickerGroup,
} from '@/components/lowcode-builder/page/page-block-picker'

describe('page-block-picker', () => {
  it('groups charts / todo / notice like the + picker', () => {
    expect(resolvePageBlockPickerGroup({ blockType: 'pie-chart', title: '饼图' })).toBe('chart')
    expect(resolvePageBlockPickerGroup({ blockType: 'area-chart', title: '面积图' })).toBe('chart')
    expect(resolvePageBlockPickerGroup({ blockType: 'workflow-task-list', title: '我的待办/已办' })).toBe('list')
    expect(resolvePageBlockPickerGroup({ blockType: 'notice-board', title: '通知公告' })).toBe('list')
    expect(resolvePageBlockPickerGroup({ blockType: 'welcome-banner', title: '欢迎条' })).toBe('list')
    expect(resolvePageBlockPickerGroup({ blockType: 'top-ranking', title: '排行榜' })).toBe('chart')
  })

  it('right-click nest insert includes new widgets under the same groups as +', () => {
    const groups = buildPageBlockPickerGroups({
      excludeBlockTypes: DEFAULT_NEST_EXCLUDE_BLOCK_TYPES,
    })
    expect(groups.map(g => g.key)).toEqual(
      PAGE_BLOCK_PICKER_GROUPS.map(g => g.key).filter(key => groups.some(g => g.key === key)),
    )

    const byType = new Map()
    groups.forEach((group) => {
      group.items.forEach((item) => {
        byType.set(item.blockType, group.key)
      })
    })

    for (const type of ['pie-chart', 'area-chart', 'bar-chart', 'line-chart', 'top-ranking', 'workflow-task-list', 'notice-board', 'welcome-banner', 'card', 'panel-box', 'box-layout', 'grid-layout']) {
      expect(byType.has(type), `${type} missing from insert catalog`).toBe(true)
    }
    expect(byType.get('pie-chart')).toBe('chart')
    expect(byType.get('area-chart')).toBe('chart')
    expect(byType.get('top-ranking')).toBe('chart')
    expect(byType.get('workflow-task-list')).toBe('list')
    expect(byType.get('notice-board')).toBe('list')
    expect(byType.get('welcome-banner')).toBe('list')
    expect(byType.get('card')).toBe('view')
    expect(byType.get('panel-box')).toBe('view')
    expect(byType.get('grid-layout')).toBe('view')
    for (const excluded of DEFAULT_NEST_EXCLUDE_BLOCK_TYPES) {
      expect(byType.has(excluded)).toBe(false)
    }
  })

  it('keyword filter matches title / blockType', () => {
    const items = listInsertablePageBlocks({ keyword: '饼图' })
    expect(items.some(item => item.blockType === 'pie-chart')).toBe(true)
    const cards = listInsertablePageBlocks({ keyword: '卡片' })
    expect(cards.some(item => item.blockType === 'card')).toBe(true)
    const panels = listInsertablePageBlocks({ keyword: '面板' })
    expect(panels.some(item => item.blockType === 'panel-box')).toBe(true)
    const welcome = listInsertablePageBlocks({ keyword: '欢迎' })
    expect(welcome.some(item => item.blockType === 'welcome-banner')).toBe(true)
  })

  it('puts frequent widgets ahead within each group', () => {
    const groups = buildPageBlockPickerGroups()
    const listGroup = groups.find(group => group.key === 'list')
    expect(listGroup?.items?.[0]?.blockType).toBe('workflow-task-list')
    const chartGroup = groups.find(group => group.key === 'chart')
    const chartTypes = (chartGroup?.items || []).map(item => item.blockType)
    const firstFrequent = FREQUENT_PAGE_BLOCK_TYPES.find(type => chartTypes.includes(type))
    expect(chartTypes[0]).toBe(firstFrequent)
  })
})
